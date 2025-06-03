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
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.MessagePart;
import pt.lsts.imc.MessagePartControl;
import pt.lsts.imc.PlanDB;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.net.IMCFragmentHandler;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.admin.CommsAdmin;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.conf.GeneralPreferences;

import javax.swing.SwingWorker;
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
    private static ImcMessageFragmentManager instance;

    private final IMCFragmentHandler imcFragmentHandler;

    // Lock instance
    private final Object lockSent = new Object();
    private final Object lockReceived = new Object();

    // Map to hold the sent fragments insert time. Pair is frag id and system id of the destination of the fragments
    private final Map<Pair<Integer, Integer>, Long> sentFragmentsInsertTimeHolder = Collections.synchronizedMap(new HashMap<>());
    // Map to hold the sent fragments
    private final Map<Pair<Integer, Integer>, List<MessagePart>> sentFragmentsHolder = Collections.synchronizedMap(new HashMap<>());
    // Map to hold the sent fragments note holder
    private final Map<Pair<Integer, Integer>, String> sentFragmentsNoteHolder = Collections.synchronizedMap(new HashMap<>());

    // Map to hold the received fragments. Pair is frag id and system id of the destination of the fragments
    private final Map<Pair<Integer, Integer>, List<MessagePart>> receivedFragmentsHolder = Collections.synchronizedMap(new HashMap<>());
    // Map to hold the received fragments insert time. Pair is frag id and system id of the destination of the fragments
    private final Map<Pair<Integer, Integer>, Long> receivedFragmentsInsertTimeHolder = Collections.synchronizedMap(new HashMap<>());

    public ImcMessageFragmentManager() {
        this(ImcMsgManager.getManager());
    }

    public ImcMessageFragmentManager(ImcMsgManager imcMsgManager) {
        // Constructor logic here
        imcMsgManager.registerBusListener(this);
        PeriodicUpdatesService.registerPojo(this);
        imcFragmentHandler = new IMCFragmentHandler(imcMsgManager.imcDefinition);
    }

    public synchronized static ImcMessageFragmentManager getInstance() {
        return getInstance(ImcMsgManager.getManager());
    }

    public synchronized static ImcMessageFragmentManager getInstance(ImcMsgManager imcMsgManager) {
        if (instance == null) {
            instance = new ImcMessageFragmentManager(imcMsgManager);
        }
        return instance;
    }

    public void addSentFragments(int fragmentId, int systemId, List<MessagePart> fragmentList) {
        if (fragmentList == null || fragmentList.isEmpty()) {
            return;
        }

        String systemName = ImcSystemsHolder.translateImcIdToSystemName(systemId);
        if (systemName == null || systemName.isEmpty()) {
            VehicleType veh = VehiclesHolder.getVehicleWithImc(new ImcId16(systemId));
            if (veh != null) {
                systemName = veh.getName();
            }
        }
        if (systemName == null || systemName.isEmpty()) {
            systemName = new ImcId16(systemId).toPrettyString();
        }

        String note = "Fragments for frag id " + fragmentId + " to system id " + systemName;
        try {
            IMCMessage originalMsg = imcFragmentHandler.reassemble(fragmentList);
            if (originalMsg == null) {
                throw new Exception("Original message is null after reassembling fragments");
            }

            String msgNote = "";
            switch (originalMsg.getMgid()) {
                case PlanSpecification.ID_STATIC:
                    msgNote = String.format(" plan '%s'", ((PlanSpecification) originalMsg).getPlanId());
                    break;
                case PlanDB.ID_STATIC:
                    msgNote = String.format(" plan db '%s' of type '%s' doing operation '%s' [rqst %s]", ((PlanDB) originalMsg).getPlanId(),
                            ((PlanDB) originalMsg).getType().name(), ((PlanDB) originalMsg).getOp().name(),
                            ((PlanDB) originalMsg).getRequestId());
                    break;
                default:
                    msgNote = String.format(" message '%s'", originalMsg.getAbbrev());
            }

            note = "Fragments for" + msgNote +" frag id " + fragmentId + " to system id " + systemName;
        }
        catch (Exception e) {
            System.out.println("Error reassembling sent fragments: " + e.getMessage());
            NeptusLog.pub().warn("Error reassembling sent fragments: {}", e.getMessage());
        }

        synchronized (lockSent) {
            int fragId = fragmentList.get(0).getUid();
            Pair<Integer, Integer> idPair = Pair.create(fragId, systemId);
            System.out.println("Adding sent fragments to " + idPair + ": " + fragmentList);
            NeptusLog.pub().warn("Adding sent fragments to {}: {}", idPair, fragmentList);
            sentFragmentsInsertTimeHolder.put(idPair, System.currentTimeMillis());
            List<MessagePart> fl = new ArrayList<>(fragmentList);
            sentFragmentsHolder.put(idPair, fl);
            sentFragmentsNoteHolder.put(idPair, note);
        }
    }

    public void addReceivedFragments(MessagePart... fragmentList) {
        if (fragmentList == null || fragmentList.length == 0) {
            return;
        }

        List<MessagePart> fragments = Arrays.asList(fragmentList);
        addReceivedFragments(fragments);
    }

    public void addReceivedFragments(List<MessagePart> fragmentList) {
        if (fragmentList == null || fragmentList.isEmpty()) {
            return;
        }

        synchronized (lockReceived) {
            int systemId = fragmentList.get(0).getSrc();
            int fragId = fragmentList.get(0).getUid();
            Pair<Integer, Integer> idPair = Pair.create(fragId, systemId);
            System.out.println("Adding received fragments to " + idPair + ": " + fragmentList);
            NeptusLog.pub().warn("Adding received fragments to {}: {}", idPair, fragmentList);
            receivedFragmentsInsertTimeHolder.put(idPair, System.currentTimeMillis());
            List<MessagePart> allFragmentList = receivedFragmentsHolder.get(idPair);
            if (allFragmentList == null) {
                allFragmentList = new ArrayList<>();
            }
            receivedFragmentsHolder.put(idPair, allFragmentList);

            // Add the fragments to the list, avoiding duplicates
            for (MessagePart fragment : fragmentList) {
                boolean matched = allFragmentList.stream().anyMatch(mp -> mp.getFragNumber() == fragment.getFragNumber());
                if (!matched) {
                    allFragmentList.add(fragment);
                }
            }
        }
    }

    @Subscribe
    public void onMessageSent(MessagePartControl msg) {
        int systemId = msg.getSrc();
        int fragId = msg.getUid();
        Pair<Integer, Integer> idPair = Pair.create(fragId, systemId);

        System.out.println("Message Frag Control sent: with frag id " + fragId + " and system id " + systemId);
        NeptusLog.pub().warn("Message Frag Control request: with frag id {} and system id {}", fragId, systemId);

        if (!sentFragmentsHolder.containsKey(idPair)) {
            System.out.println("Not a known fragment: " + idPair);
            NeptusLog.pub().warn("Not a known fragment: {}", idPair);
            return; // Not a known fragment
        }

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
        final String fragNote;
        synchronized (lockSent) {
            if (msg.getOp() == MessagePartControl.OP.STATUS_RECEIVED) {
                removeReceivedFragments(idPair, isPositiveConsidered, fragIdsList);
                sentFragmentsInsertTimeHolder.put(idPair, System.currentTimeMillis());
                fragNote = "";
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
                fragNote = sentFragmentsNoteHolder.get(idPair);
            }
            else {
                fragNote = "";
            }
        }

        if (fragmentsToSend.isEmpty()) {
            return; // No fragments to send
        }

        final List<MessagePart> fragmentsToSendFinal = fragmentsToSend;
        final String fragsIdsStr = fragmentsToSendFinal.stream().map(
                m -> "" + m.getFragNumber()).collect(Collectors.joining(", "));
        final String fragsTotal = "" + fragmentsToSendFinal.get(0).getNumFrags();
        final String systemName = ImcSystemsHolder.translateImcIdToSystemName(systemId);

        final Notification sendNotificationAction = Notification.info(I18n.textf("Resend Message Fragments to %name", systemName),
                        I18n.textf("%note.\n Resend %n of %t fragments for parts: %frags?",
                                fragNote, fragmentsToSendFinal.size(), fragsTotal, fragsIdsStr))
                .requireHumanAction(true);
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                NeptusLog.pub().warn("Resending requested fragments to {} with ids: {}", msg.getSourceName(), fragIdsList);
                System.out.println("Resending requested fragments to " + msg.getSourceName() + " with ids: " + fragIdsList);
                // FIXME: Make the actual sending of the fragments be done in a separate thread with a delay, and to be validate with the operator

                for (MessagePart fragment : fragmentsToSendFinal) {
                    // Send the fragments
                    //ImcMsgManager.getManager().sendMessage(fragmentsToSend);
                    String[] channelsToUse = new String[] {CommsAdmin.CommChannelType.WIFI.name, CommsAdmin.CommChannelType.IRIDIUM.name};
                    boolean ret =  IMCSendMessageUtils.sendMessage(fragment, ImcMsgManager.TRANSPORT_TCP,
                            (MessageDeliveryListener) null, null, I18n.text("Error sending msg part requested by receiver"),
                            false, "", true, true,
                            true, false, channelsToUse, msg.getSourceName());
                    if (!ret) {
                        NeptusEvents.post(Notification.error(I18n.textf("Resent Message Fragments to %name", systemName),
                                I18n.textf("%note'.\n Resent ERROR %n of %t fragments for parts: %frags.",
                                        fragNote, fragmentsToSendFinal.size(), fragsTotal, fragsIdsStr)));
                        sendNotificationAction.setActionTriggered(false);
                        return null; // Error sending the message, aborting the rest
                    }
                }

                NeptusEvents.post(Notification.success(I18n.textf("Resent Message Fragments to %name", systemName),
                        I18n.textf("%note'.\n Resent OK %n of %t fragments for parts: %frags.",
                                fragNote, fragmentsToSendFinal.size(), fragsTotal, fragsIdsStr)));
                return null;
            }
        };

        if (GeneralPreferences.isAutomaticallyResendMissingSentFragments) {
            worker.execute();
            return; // No need to ask for confirmation, just execute the worker
        }

        sendNotificationAction.setActionListener(e -> {
            worker.execute();
        });
        NeptusEvents.post(sendNotificationAction);
    }

    private void removeReceivedFragments(Pair<Integer, Integer> idPair, boolean isPositiveConsidered, List<Integer> fragIdsList) {
        synchronized (lockSent) {
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

    @Subscribe
    public void onReceivedFragments(MessagePart fragment) {
        if (fragment == null || fragment.getSrc() == GeneralPreferences.imcCcuId.intValue())
            return;

        System.out.println("Received " + ((fragment.getUid() <= 0 || fragment.getSrc() <= 0) ? "invalid " : "")
                + "fragment: " + fragment.getUid() + " from system " + fragment.getSrc());
        NeptusLog.pub().warn("Received {}fragment: {} from system {}",
                (fragment.getUid() <= 0 || fragment.getSrc() <= 0) ? "invalid " : "",
                fragment.getUid(), fragment.getSrc());

        if (fragment.getUid() <= 0 || fragment.getSrc() <= 0) {
            return; // Invalid fragment
        }

        int systemId = fragment.getSrc();
        int fragId = fragment.getUid();
        Pair<Integer, Integer> idPair = Pair.create(fragId, systemId);

        // Add the fragment to the received fragments holder
        addReceivedFragments(fragment);
    }

    @Periodic(millisBetweenUpdates = 20_000)
    public void checkSentFragments() {
        long currentTimeMillis = System.currentTimeMillis();
        synchronized (lockSent) {
            List<Pair<Integer, Integer>> toRemove = new ArrayList<>();
            for (Map.Entry<Pair<Integer, Integer>, Long> entry : sentFragmentsInsertTimeHolder.entrySet()) {
                Pair<Integer, Integer> fragmentIdPair = entry.getKey();
                long insertTimeMillis = entry.getValue();
                if (currentTimeMillis - insertTimeMillis > GeneralPreferences.minutesToDumpAllFragments * 60 * 1_000) {
                    toRemove.add(fragmentIdPair);
                }
            }
            for (Pair<Integer, Integer> fragmentIdPair : toRemove) {
                sentFragmentsHolder.remove(fragmentIdPair);
                sentFragmentsInsertTimeHolder.remove(fragmentIdPair);
                sentFragmentsNoteHolder.remove(fragmentIdPair);
            }

            System.out.println("Checking sent fragments. Current time: " + currentTimeMillis +
                    ", Sent fragments: " + sentFragmentsInsertTimeHolder.size() + ", Removed fragments: " + toRemove);
            NeptusLog.pub().warn("Checking sent fragments. Current time: {}, Sent fragments: {}, Removed fragments: {}",
                    currentTimeMillis, sentFragmentsInsertTimeHolder.size(), toRemove);
        }
    }

    @Periodic(millisBetweenUpdates = 20_000)
    public void checkReceivedFragments() {
        long currentTimeMillis = System.currentTimeMillis();
        System.out.println("Checking received fragments. Current time: " + currentTimeMillis +
                ", received fragments: " + receivedFragmentsInsertTimeHolder.size());
        NeptusLog.pub().warn("Checking received fragments. Current time: {}, received fragments: {}",
                currentTimeMillis, receivedFragmentsInsertTimeHolder.size());

        synchronized (lockReceived) {
            List<Pair<Integer, Integer>> toRemove = new ArrayList<>();
            for (Map.Entry<Pair<Integer, Integer>, Long> entry : receivedFragmentsInsertTimeHolder.entrySet()) {
                Pair<Integer, Integer> fragmentIdPair = entry.getKey();
                long insertTimeMillis = entry.getValue();
                if (currentTimeMillis - insertTimeMillis > GeneralPreferences.minutesToDumpAllFragments * 60 * 1_000) {
                    toRemove.add(fragmentIdPair);
                    continue;
                }

                // If the fragment is still valid, check if it is all received
                List<MessagePart> fragmentsAlreadyReceived = receivedFragmentsHolder.get(fragmentIdPair);
                if (fragmentsAlreadyReceived == null || fragmentsAlreadyReceived.isEmpty()) {
                    continue; // No fragments to request
                }
                int nFrags = fragmentsAlreadyReceived.get(0).getNumFrags();
                if (fragmentsAlreadyReceived.size() >= nFrags) {
                    // All fragments received, remove from the holder
                    toRemove.add(fragmentIdPair);
                    System.out.println("All fragments received for " + fragmentIdPair + ", removing from holder.");
                    NeptusLog.pub().warn("All fragments received for {}, removing from holder.", fragmentIdPair);
                }
            }
            for (Pair<Integer, Integer> fragmentIdPair : toRemove) {
                receivedFragmentsHolder.remove(fragmentIdPair);
                receivedFragmentsInsertTimeHolder.remove(fragmentIdPair);
            }

            for (Map.Entry<Pair<Integer, Integer>, Long> entry : receivedFragmentsInsertTimeHolder.entrySet()) {
                Pair<Integer, Integer> fragmentIdPair = entry.getKey();
                long insertTimeMillis = entry.getValue();
                if (currentTimeMillis - insertTimeMillis <= GeneralPreferences.minutesToRequestMissingReceivedFragments * 60 * 1_000) {
                    continue; // Still within the time to request missing fragments
                }

                List<MessagePart> fragmentsAlreadyReceived = receivedFragmentsHolder.get(fragmentIdPair);
                if (fragmentsAlreadyReceived == null || fragmentsAlreadyReceived.isEmpty()) {
                    continue; // No fragments to request
                }

                MessagePart firstFrag = fragmentsAlreadyReceived.get(0);
                int fragUid = fragmentIdPair.getFirst();
                int systemId = fragmentIdPair.getSecond();
                int totalNFrag = firstFrag.getNumFrags();
                List<Integer> receivedFragNumbers = fragmentsAlreadyReceived.stream()
                        .map(MessagePart::getFragNumber)
                        .map(Integer::valueOf)
                        .collect(Collectors.toList());
                List<Integer> missingFragNumbers = new ArrayList<>();
                for (int i = 0; i < totalNFrag; i++) {
                    if (!receivedFragNumbers.contains(i)) {
                        missingFragNumbers.add(i);
                    }
                }

                if (missingFragNumbers.isEmpty()) {
                    continue; // No missing fragments to request
                }

                // Request the missing fragments
                MessagePartControl requestMsg = new MessagePartControl();
                requestMsg.setDst(systemId);
                requestMsg.setUid((short) fragUid);
                requestMsg.setOpVal((short) MessagePartControl.OP.REQUEST_RETRANSMIT.value());
                requestMsg.setFragIds((missingFragNumbers.size() <= receivedFragNumbers.size() ? "" : "!")
                        + (missingFragNumbers.size() <= receivedFragNumbers.size()
                            ? missingFragNumbers : receivedFragNumbers).stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")));

                System.out.println("Requesting missing fragments " + requestMsg.getFragIds() +
                        " from " + requestMsg.getSourceName() + " for frag id " + fragUid);
                NeptusLog.pub().warn("Requesting missing fragments {} from {} for frag id {}",
                        requestMsg.getFragIds(), requestMsg.getSourceName(), fragUid);

                String systemName = ImcSystemsHolder.translateImcIdToSystemName(systemId);
                String[] channelsToUse = new String[] {CommsAdmin.CommChannelType.WIFI.name, CommsAdmin.CommChannelType.IRIDIUM.name};
                boolean ret =  IMCSendMessageUtils.sendMessage(requestMsg, ImcMsgManager.TRANSPORT_TCP,
                        (MessageDeliveryListener) null, null, I18n.text("Error sending msg part retransmit requested for sender"),
                        false, "", true, true,
                        true, false, channelsToUse, systemName);

                if (ret) {
                    receivedFragmentsInsertTimeHolder.put(fragmentIdPair, System.currentTimeMillis());
                }
            }
        }
    }
}
