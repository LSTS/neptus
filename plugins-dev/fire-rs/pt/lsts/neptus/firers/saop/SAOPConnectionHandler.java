/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Author: keila
 * 02/04/2018
 */
package pt.lsts.neptus.firers.saop;

import java.util.HashMap;
import java.util.Map;

import com.google.common.eventbus.Subscribe;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.DevDataBinary;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Heartbeat;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanSpecification;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.comm.transports.ImcTcpTransport;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author keila
 *
 */
@PluginDescription(name = "SAOP Server Interaction", description = "IMC Message exchange with SAOP IMC TCP Server")
public class SAOPConnectionHandler extends SimpleRendererInteraction {

    /**
     * @param console
     */
    public SAOPConnectionHandler(ConsoleLayout console) {
        super(console);
    }

    private static final long serialVersionUID = 1L;
    private final String prefix = "saop_";
    private volatile boolean sendHeartbeat = false;
    private ImcTcpTransport imctt;
    private MessageDeliveryListener deliveryListener;
    private MessageListener<MessageInfo, IMCMessage> msgListener;
    private Map<String, Integer> plans_reqId = new HashMap<>();

    @NeptusProperty(name = "IP ADDRESS", userLevel = LEVEL.REGULAR, description = "IP ADDRESS to SAOP server")
    public String ipAddr = "127.0.0.1";
    @NeptusProperty(name = "PORT", userLevel = LEVEL.REGULAR, description = "Port to SAOP server")
    public int serverPort = 8888;
    @NeptusProperty(name = "Bind PORT", userLevel = LEVEL.ADVANCED, description = "Neptus internal configuration")
    public int bindPort = 8000;

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.renderer2d.StateRendererInteraction#isExclusive()
     */
    @Override
    public boolean isExclusive() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        imctt.removeListener(msgListener);
        imctt.purge();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        initializeListeners();
        imctt = new ImcTcpTransport(bindPort, IMCDefinition.getInstance());
        imctt.addListener(msgListener);
        sendHeartbeat = true;
    }

    /**
     * Send periodically heartbeat to keep connection open
     */
    @Periodic(millisBetweenUpdates = 1000)
    public void sendHeartbeat() {
        if (sendHeartbeat) {
            Heartbeat hb = new Heartbeat();
            imctt.sendMessage(ipAddr, serverPort, hb, deliveryListener);
        }
    }

    public void initializeListeners() {

        msgListener = new MessageListener<MessageInfo, IMCMessage>() {
            @Override
            public void onMessage(MessageInfo info, IMCMessage msg) {
              //  if (info.getPublisherInetAddress().equalsIgnoreCase(ipAddr) && info.getPublisherPort() == port) {
                    if (msg.getMgid() == PlanControl.ID_STATIC) {
                        NeptusLog.pub().info("RECEIVED: "+msg.getAbbrev()+" from: "+info.getPublisher()+":"+info.getPublisherPort());
                        PlanControl pc = (PlanControl) msg;
                        addNewPlanControl(pc);
                    }
                //}
            }
        };

        deliveryListener = new MessageDeliveryListener() {

            @Override
            public void deliveryUnreacheable(IMCMessage msg) {
                NeptusLog.pub().error(I18n.text("Destination " + ipAddr + ":" + serverPort
                        + "unreacheable. Unable to delivery " + msg.getAbbrev() + " to SAOP IMC TCP Server."));
            }

            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
                // TODO Auto-generated method stub
            }

            @Override
            public void deliveryTimeOut(IMCMessage msg) {
                // TODO retry??
                if (checkMsgId(msg.getMgid())) {
                    imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
                }
            }

            @Override
            public void deliverySuccess(IMCMessage msg) {
                // TODO remove from hash
                //System.err.println("DELIVERED: "+msg.getAbbrev());
            }

            @Override
            public void deliveryError(IMCMessage msg, Object error) {
                // TODO retry??
                if (checkMsgId(msg.getMgid())) {
                    imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
                }
            }
        };
    }

    @Subscribe
    public void on(IMCMessage msg) {
        String system = IMCDefinition.getInstance().getResolver().resolve(msg.getSrc());
        ImcSystem imcSys = ImcSystemsHolder.lookupSystemByName(system);
        // Filter specific IMC Messages from UAVs
        if (imcSys != null
                && (imcSys.getType() == SystemTypeEnum.VEHICLE && imcSys.getTypeVehicle() == VehicleTypeEnum.UAV)) {

            if (msg.getMgid() == PlanControl.ID_STATIC) {
                sendHeartbeat = false;
                PlanControl pc = (PlanControl) msg;

                if (pc.getType().equals(pt.lsts.imc.PlanControl.TYPE.SUCCESS) && pc.getOp().equals(OP.START)) { // ACK
                    String newName = pc.getPlanId();
                    String oldName = originalName(newName);
                    pc.setPlanId(oldName);
                    pc.setRequestId(plans_reqId.get(newName));
                    imctt.sendMessage(ipAddr, serverPort, pc, deliveryListener);
                    // plans_reqId.remove(newName); plan Id is needed for PCS
                    sendHeartbeat = true;
                }
            }
            else if (msg.getMgid() == PlanControlState.ID_STATIC) {
                PlanControlState pcs = (PlanControlState) msg;
                if (plans_reqId.containsKey(pcs.getPlanId())) {
                    sendHeartbeat = false;
                    String newName = pcs.getPlanId();
                    String oldName = originalName(newName);
                    pcs.setPlanId(oldName);
                    imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
                    sendHeartbeat = true;
                }
            }
            else if (msg.getMgid() == DevDataBinary.ID_STATIC) {
                // TODO Filter src entity to IR Cam/FireMapper/AUXCPU
                imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
            }
            else if (msg.getMgid() == EstimatedState.ID_STATIC) {
                imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
            }
//            else if (msg.getMgid() == PlanDB.ID_STATIC) {
//                imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
//            }
        }
    }

    public boolean checkMsgId(int msgId) {

        return msgId == PlanControl.ID_STATIC || msgId == PlanControlState.ID_STATIC || msgId == DevDataBinary.ID_STATIC
                || msgId == EstimatedState.ID_STATIC;
    }

    /**
     * Add new incoming PlanControl from the SAOP server to mission plan tree. The added plan has the format:
     * saop_[timestamp]_planId
     */
    public void addNewPlanControl(PlanControl pc) {
        PlanSpecification ps;
        if (pc.getArg() != null) {
            if (pc.getArg().getClass().equals(PlanSpecification.class)) {
                ps = (PlanSpecification) pc.getArg();
                PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(), ps);
                String newPlanId = prefix + System.currentTimeMillis() + "_" + ps.getPlanId();
                plan.setId(newPlanId);
                plans_reqId.put(newPlanId, pc.getRequestId());
                getConsole().getMission().addPlan(plan);
                getConsole().getMission().save(true);
                getConsole().updateMissionListeners();
                getConsole().post(Notification.success(I18n.text("SAOP IMC TCP SERVER"),
                        I18n.textf("Received PlanSpecifition: %plan.", plan.getId())));
            }
        }
        else {
            GuiUtils.errorMessage(this.getConsole(), "Error Parsing PlanControl",
                    "Error parsing IMC PlanSpecification received from " + ipAddr + ":" + serverPort);
            NeptusLog.pub()
                    .error(I18n.text("Error parsing IMC PlanSpecification received from " + ipAddr + ":" + serverPort));
        }
    }

    /**
     * @param newName in the form: saop_`TIMESTAMP`_oldName which can contain ``_'' (underscore)
     * @return Retrieves original name from the plan
     */
    public String originalName(String newName) {
        StringBuilder sb = new StringBuilder();
        String[] names = newName.split("_");
        for (int i = 2; i < names.length; i++)
            sb.append(names[i]);
        return sb.toString();
    }

    /**
     * Updated plugins parameters
     */
    @Override
    public void setProperties(Property[] properties) {
        if ((String) properties[0].getValue() != ipAddr) {
            ipAddr = (String) properties[0].getValue();
        }
        if ((int) properties[1].getValue() != serverPort) {
            serverPort = (int) properties[1].getValue();
        }
        if ((int) properties[2].getValue() != bindPort) {
            sendHeartbeat = false;
            bindPort = (int) properties[2].getValue();
            imctt.reStart();
            imctt.setBindPort(bindPort);
            imctt.addListener(msgListener);
            sendHeartbeat = true;
        }

        super.setProperties(properties);
    }
}