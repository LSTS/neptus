/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias
 * 2025/May/14
 */
package pt.lsts.neptus.comm.manager.imc;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.math3.util.Pair;
import pt.lsts.imc.MessagePart;
import pt.lsts.imc.MessagePartControl;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.admin.CommsAdmin;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.conf.GeneralPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class manages the fragments of messages that are sent and received.
 * It keeps track of the fragments that have been sent and received, and allows
 * for retransmission of fragments if requested.
 */
public class ImcMessageFragmentManager {
    private static final ImcMessageFragmentManager instance = new ImcMessageFragmentManager();

    // Lock instance
    private final Object lock = new Object();
    // Map to hold the sent fragments insert time. Pair is frag id and system id of the destination of the fragments
    private final Map<Pair<Integer, Integer>, Long> sentFragmentsInsertTimeHolder = Collections.synchronizedMap(new HashMap<>());
    // Map to hold the sent fragments
    private final Map<Pair<Integer, Integer>, List<MessagePart>> sentFragmentsHolder = Collections.synchronizedMap(new HashMap<>());

    public ImcMessageFragmentManager() {
        // Constructor logic here
        ImcMsgManager.getManager().registerBusListener(this);
    }

    public static ImcMessageFragmentManager getInstance() {
        return instance;
    }

    public void addSentFragments(int fragmentId, List<MessagePart> fragmentList) {
        if (fragmentList == null || fragmentList.isEmpty()) {
            return;
        }

        synchronized (lock) {
            int systemId = fragmentList.get(0).getDst();
            int fragId = fragmentList.get(0).getUid();
            Pair<Integer, Integer> idPair = Pair.create(fragId, systemId);
            System.out.println("Adding sent fragments to " + idPair + ": " + fragmentList);
            NeptusLog.pub().warn("Adding sent fragments to {}: {}", idPair, fragmentList);
            sentFragmentsInsertTimeHolder.put(idPair, System.currentTimeMillis());
            sentFragmentsHolder.put(idPair, fragmentList);
        }
    }

    @Subscribe
    private void onMessageSent(MessagePartControl msg) {
        int systemId = msg.getDst();
        int fragId = msg.getUid();
        Pair<Integer, Integer> idPair = Pair.create(fragId, systemId);

        if (!sentFragmentsHolder.containsKey(idPair))
            return; // Not a known fragment

        String fragIdStr = msg.getFragIds();
        boolean isPositiveConsidered = true; // If positive is to consider  the elements, otherwise the negative
        if (fragIdStr.startsWith("!")) {
            isPositiveConsidered = false;
            fragIdStr = fragIdStr.substring(1);
        }

        List<Integer> fragIdsList;
        if (fragIdStr.trim().equalsIgnoreCase("!") || fragIdStr.trim().isEmpty()) {
            fragIdsList = Collections.emptyList();
        } else {
            try {
                fragIdsList = Arrays.stream(fragIdStr.split("[!,; ]"))
                        .map(s -> Integer.parseInt(s.trim())).collect(Collectors.toList());
            } catch (NumberFormatException e) {
                return; // Invalid fragment ids. Returning without doing anything
            }
        }

        List<MessagePart> fragmentsToSend = Collections.emptyList();
        synchronized (lock) {
            if (msg.getOp() == MessagePartControl.OP.STATUS_RECEIVED) {
                removeReceivedFragments(idPair, isPositiveConsidered, fragIdsList);
                sentFragmentsInsertTimeHolder.put(idPair, System.currentTimeMillis());
            }
            else if (msg.getOp() == MessagePartControl.OP.REQUEST_RETRANSMIT) {
                sentFragmentsInsertTimeHolder.put(idPair, System.currentTimeMillis());
                removeReceivedFragments(idPair, !isPositiveConsidered, fragIdsList); // here the positive consideration is reversed
                // If the message is a request for retransmit, we need to send the fragments that are in the list
                List<MessagePart> fragments = sentFragmentsHolder.get(idPair);
                if (fragments == null || fragments.isEmpty()) {
                    return; // No fragments to send
                }

                fragmentsToSend = new ArrayList<>(fragments);
            }
        }

        NeptusLog.pub().warn("Resending requested fragments to {} with ids: {}", msg.getSourceName(), fragIdsList);
        System.out.println("Resending requested fragments to " + msg.getSourceName() + " with ids: " + fragIdsList);
        // FIXME: Make the actual sending of the fragments be done in a separate thread with a delay, and to be validate with the operator

        for (MessagePart fragment : fragmentsToSend) {
            // Send the fragments
            //ImcMsgManager.getManager().sendMessage(fragmentsToSend);
            String[] channelsToUse = new String[] {CommsAdmin.CommChannelType.WIFI.name, CommsAdmin.CommChannelType.IRIDIUM.name};
            boolean ret =  IMCSendMessageUtils.sendMessage(fragment, ImcMsgManager.TRANSPORT_TCP,
                    (MessageDeliveryListener) null, null, I18n.text("Error sending msg part requested by receiver"),
                    false, "", true, true,
                    true, false, channelsToUse, msg.getSourceName());
            if (!ret) {
                return; // Error sending the message, aborting the rest
            }
        }
    }

    private void removeReceivedFragments(Pair<Integer, Integer> idPair, boolean isPositiveConsidered, List<Integer> fragIdsList) {
        synchronized (lock) {
            if (isPositiveConsidered) {
                // Remove the fragments that are in the list
                List<MessagePart> fragments = sentFragmentsHolder.get(idPair);
                if (fragments != null) {
                    fragments.removeIf(frag -> fragIdsList.contains(Short.valueOf(frag.getFragNumber()).intValue()));
                }
            } else {
                // Remove the fragments that are not in the list
                List<MessagePart> fragments = sentFragmentsHolder.get(idPair);
                if (fragments != null) {
                    fragments.removeIf(frag -> !fragIdsList.contains(Short.valueOf(frag.getFragNumber()).intValue()));
                }
            }
        }
    }

    @Periodic(millisBetweenUpdates = 1000)
    public void checkSentFragments() {
        long currentTimeMillis = System.currentTimeMillis();
        synchronized (lock) {
            for (Map.Entry<Pair<Integer, Integer>, Long> entry : sentFragmentsInsertTimeHolder.entrySet()) {
                Pair<Integer, Integer> fragmentIdPair = entry.getKey();
                long insertTimeMillis = entry.getValue();
                if (currentTimeMillis - insertTimeMillis > GeneralPreferences.minutesToDumpAllFragments * 60 * 1_000) {
                    sentFragmentsHolder.remove(fragmentIdPair);
                    sentFragmentsInsertTimeHolder.remove(fragmentIdPair);
                }
            }
        }
    }
}
