/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * 16/4/2024
 */
package pt.lsts.neptus.comm.admin;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.MessagePart;
import pt.lsts.imc.TransmissionRequest;
import pt.lsts.imc.net.IMCFragmentHandler;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.iridium.ImcIridiumMessage;
import pt.lsts.neptus.comm.iridium.IridiumManager;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.conf.GeneralPreferences;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class CommsAdmin {

    public static final int COMM_TIMEOUT_MILLIS = 20000;
    public static final int MAX_ACOMMS_PAYLOAD_SIZE = 998;
    public static final double TIMEOUT_ACOMMS_SECS = 60;

    public enum CommChannelType {
        WIFI("WiFi", "Wi-Fi channel", "images/channels/wifi.png",
                "images/channels/wifi_selected.png", "images/channels/wifi_disabled.png", true, false, false),
        ACOUSTIC("Acoustic", "Acoustic channel", "images/channels/acoustic.png",
                "images/channels/acoustic_selected.png", "images/channels/acoustic_disabled.png", true, false, false),
        IRIDIUM("Iridium", "Iridium channel", "images/channels/iridium.png",
                "images/channels/iridium_selected.png", "images/channels/iridium_disabled.png", true, false, false),
        GSM("GSM", "GSM channel", "images/channels/gsm.png",
                "images/channels/gsm_selected.png", "images/channels/gsm_disabled.png", true, false, false),
        ;

        public final String name;
        public final String description;
        public final String icon;
        public final String iconSelected;
        public final String iconDisabled;
        private boolean enabled;
        private boolean active;
        private boolean reliable;

        private CommChannelType(String name, String description, String icon, String iconSelected,
                                String iconDisabled, boolean enabled, boolean active, boolean reliable) {
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.iconSelected = iconSelected;
            this.iconDisabled = iconDisabled;
            this.enabled = enabled;
            this.active = active;
            this.reliable = reliable;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isReliable() {
            return reliable;
        }

        public void setReliable(boolean reliable) {
            this.reliable = reliable;
        }
    } // CommChannelType

    private final String acousticOpServiceName = "acoustic/operation";
    private final String iridiumOpServiceName = "iridium";

    private ImcMsgManager imcMsgManager = null;
    private List<CommChannelType> channels = new ArrayList<>();

    public CommsAdmin(ImcMsgManager imcMsgManager) {
        this.imcMsgManager = imcMsgManager;

        Collections.addAll(channels, CommChannelType.values());
    }

    //public static boolean sendMessage(IMCMessage msg, String sendProperties, MessageDeliveryListener listener,
    //                                  Component parent, String errorTextForDialog, String... destinationIds) {

    public Future<ImcMsgManager.SendResult> sendMessage(IMCMessage message, String destinationName, int timeoutMillis,
                                                        Component parentComponentForAlert,
                                                        boolean requireUserConfirmOtherThanWifi, String... channelsToSend) {
        final ResultWaiter waiter = new ResultWaiter(timeoutMillis); // COMM_TIMEOUT_MILLIS
        FutureTask<ImcMsgManager.SendResult> result = new FutureTask<ImcMsgManager.SendResult>(waiter) {
            private long start = System.currentTimeMillis();

            @Override
            public ImcMsgManager.SendResult get() throws InterruptedException, ExecutionException {
                try {
                    return waiter.call();
                } catch (Exception e) {
                    throw new ExecutionException(e);
                }
            }

            @Override
            public ImcMsgManager.SendResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
                    TimeoutException {
                long end = start + unit.toMillis(timeout);
                while (System.currentTimeMillis() < end) {
                    if (waiter.result == null) {
                        Thread.sleep(100);
                    } else {
                        try {
                            return waiter.call();
                        } catch (Exception e) {
                            throw new ExecutionException(e);
                        }
                    }
                }
                throw new TimeoutException("Time out exceeded");
            }
        };

        String channelsToUseConf = GeneralPreferences.imcChannelsToUse;

        //sendMessageToSystem(message, systemName, "TCP", waiter);
        // Let us try to send the message to the destination

        // First check which channels are enabled here
        List<CommChannelType> channelsToUse = new ArrayList<>();
        if (channelsToSend == null || channelsToSend.length == 0) {
            channelsToUse.addAll(channels);
        } else {
            for (String channelName : channelsToSend) {
                for (CommChannelType channel : channels) {
                    if (channel.name.equalsIgnoreCase(channelName)) {
                        channelsToUse.add(channel);
                        break;
                    }
                }
            }
        }

        if (channelsToUse.isEmpty()) {
            waiter.deliveryUnreacheable(message);
            return result;
        }

        final AtomicReference<ImcSystem> proxyAcousticSystem = new AtomicReference<>();
        final AtomicReference<ImcSystem> proxyGsmSystem = new AtomicReference<>();
        final AtomicReference<ImcSystem> proxyIridiumSystem = new AtomicReference<>();

        ImcSystem system = ImcSystemsHolder.lookupSystemByName(destinationName);

        if (system == null) {
            waiter.deliveryUnreacheable(message);
            return result;
        }

        // From the enabled channels, check which are active for the system
        channelsToUse = channelsToUse.stream().filter(channel -> {
            switch (channel) {
                case WIFI:
                    if (system.isActive()) {
                        return true;
                    }
                    break;
                case ACOUSTIC:
                    // Let's check if the system has this comm mean

                    ImcSystem[] acousticOpSysLst = ImcSystemsHolder.lookupSystemByService(
                            acousticOpServiceName, VehicleType.SystemTypeEnum.ALL, true);
                    List<ImcSystem> canditatesList = Arrays.asList(acousticOpSysLst);
                    Collections.shuffle(canditatesList);
                    proxyAcousticSystem.set(canditatesList.stream().filter(sys -> IMCSendMessageUtils
                                    .doesSystemWithAcousticCanReachSystem(sys, destinationName))
                            .findFirst().orElse(null));
                    if (proxyAcousticSystem.get() != null) {
                        return true;
                    }
                    break;
                case IRIDIUM:
                    if (IridiumManager.getManager().isAvailable())
                        return true;
                    break;
                case GSM:
                default:
                    break;
            }
            return false;
        }).collect(Collectors.toList());

        // We should now filter by the ones active for the system


        // Now send the message to the first available channel
        if (channelsToUse.isEmpty()) {
            waiter.deliveryUnreacheable(message);
            return result;
        }

        // Try to check if the distance to vehícle is too far for the comm mean to be used
        // ****************************************************************************************


        // Send the message
        for (CommChannelType channel : channelsToUse) {
            switch (channel) {
                case WIFI:
                    if (system.isActive()) {
                        imcMsgManager.sendMessage(message, system.getId(), null, waiter);
                        return result;
                    }
                    break;
                case ACOUSTIC:
                    if (proxyAcousticSystem.get() != null && proxyAcousticSystem.get().isActive()) {
                        try {
                            ArrayList<TransmissionRequest> requests = new ArrayList<TransmissionRequest>();
                            if (message.getPayloadSize() > MAX_ACOMMS_PAYLOAD_SIZE) {
                                IMCFragmentHandler handler = new IMCFragmentHandler(IMCDefinition.getInstance());

                                MessagePart[] parts = handler.fragment(message, MAX_ACOMMS_PAYLOAD_SIZE);
                                NeptusLog.pub().info("PlanDB message resulted in " + parts.length + " fragments");
                                for (MessagePart part : parts) {
                                    TransmissionRequest request = getAcousticTransmissionRequestForImcMessage(part, system);
                                    requests.add(request);
                                }
                            } else {
                                TransmissionRequest request = getAcousticTransmissionRequestForImcMessage(message, system);
                                requests.add(request);
                            }

                            for (TransmissionRequest request : requests) {
                                ImcMsgManager.getManager().sendMessageToSystem(request, proxyAcousticSystem.get().getName(), waiter);
                            }

                            return result;
                        } catch (Exception e) {
                            NeptusLog.pub().error(this, e);
                        }
                    }
                    break;
                case IRIDIUM:
                        sendViaIridium(destinationName, message, waiter);
                        return result;
                case GSM:
                default:
                    break;
            }
        }

        waiter.deliveryUnreacheable(message);
        return result;
    }

    private TransmissionRequest getAcousticTransmissionRequestForImcMessage(IMCMessage part, ImcSystem system) {
        TransmissionRequest request = new TransmissionRequest();
        request.setCommMean(TransmissionRequest.COMM_MEAN.ACOUSTIC);
        request.setReqId(Long.valueOf(imcMsgManager.getNextSeqInstanceNr()).intValue());
        request.setDataMode(TransmissionRequest.DATA_MODE.INLINEMSG);
        request.setMsgData(part);
        request.setDestination(system.getName());
        request.setDeadline(System.currentTimeMillis() / 1000.0 + TIMEOUT_ACOMMS_SECS);
        return request;
    }

    static class ResultWaiter implements Callable<ImcMsgManager.SendResult>, MessageDeliveryListener {

        public ImcMsgManager.SendResult result = null;
        private long timeoutMillis = 10000;
        private long start;

        public ResultWaiter(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            this.start = System.currentTimeMillis();
        }

        @Override
        public ImcMsgManager.SendResult call() throws Exception {
            while (true) {
                synchronized (this) {
                    if (result != null) {
                        return result;
                    }
                    if (System.currentTimeMillis() - start > timeoutMillis) {
                        return ImcMsgManager.SendResult.TIMEOUT;
                    }
                }
                Thread.sleep(100);
            }
        }

        @Override
        public void deliveryError(IMCMessage message, Object error) {
            result = ImcMsgManager.SendResult.ERROR;
        }

        @Override
        public void deliverySuccess(IMCMessage message) {
            result = ImcMsgManager.SendResult.SUCCESS;
        }

        @Override
        public void deliveryTimeOut(IMCMessage message) {
            result = ImcMsgManager.SendResult.TIMEOUT;
        }

        @Override
        public void deliveryUncertain(IMCMessage message, Object msg) {
            result = ImcMsgManager.SendResult.UNCERTAIN_DELIVERY;
        }

        @Override
        public void deliveryUnreacheable(IMCMessage message) {
            result = ImcMsgManager.SendResult.UNREACHABLE;
        }
    }

    public void sendViaIridium(String destination, IMCMessage message, ResultWaiter waiter) {
        if (message.getTimestamp() == 0)
            message.setTimestampMillis(System.currentTimeMillis());
        Collection<ImcIridiumMessage> irMsgs;
        try {
            irMsgs = IridiumManager.iridiumEncode(message);
        }
        catch (Exception e) {
            NeptusLog.pub().warn( "Send by Iridium :: " + e.getMessage());
            waiter.deliveryError(message, e);
            return;
        }
        int src = ImcMsgManager.getManager().getLocalId().intValue();
        int dst = IMCDefinition.getInstance().getResolver().resolve(destination);
        int count = 0;
        try {
            NeptusLog.pub().warn(message.getAbbrev() + " resulted in " + irMsgs.size() + " iridium SBD messages.");
            for (ImcIridiumMessage irMsg : irMsgs) {
                irMsg.setDestination(dst);
                irMsg.setSource(src);
                irMsg.timestampMillis = message.getTimestampMillis();
                if (irMsg.timestampMillis == 0)
                    irMsg.timestampMillis = System.currentTimeMillis();
                IridiumManager.getManager().send(irMsg);
                count++;
            }

            NeptusLog.pub().warn("Iridium message sent", count + " Iridium messages were sent using "
                    + IridiumManager.getManager().getCurrentMessenger().getName());
            waiter.deliverySuccess(message);
        }
        catch (Exception e) {
            NeptusLog.pub().warn("Send by Iridium :: " + e.getMessage());
            waiter.deliveryError(message, e);
        }
    }
}
