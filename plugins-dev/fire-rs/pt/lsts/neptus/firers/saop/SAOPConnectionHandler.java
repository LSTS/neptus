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

import java.awt.Graphics2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import javax.swing.JOptionPane;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.DevDataBinary;
import pt.lsts.imc.DevDataText;
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
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.comm.transports.ImcTcpTransport;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.util.GuiUtils;

@PluginDescription(name = "SAOP Server Interaction", description = "IMC Message exchange with SAOP IMC TCP Server")
public class SAOPConnectionHandler extends ConsoleLayer {


    private final String prefix = "saop-";
    private volatile boolean sendHeartbeat = false, established = false;
    private ImcTcpTransport imctt;
    private MessageDeliveryListener deliveryListener;
    private MessageListener<MessageInfo, IMCMessage> msgListener;
    private Map<String, Integer> plans_reqId = Collections.synchronizedMap(new HashMap<>());

    @NeptusProperty(name = "Debug Mode", userLevel = LEVEL.REGULAR, description = "Request operators permission to start/stop plan")
    public boolean debugMode = true;
    @NeptusProperty(name = "IP ADDRESS", userLevel = LEVEL.REGULAR, description = "IP ADDRESS to SAOP server")
    public String ipAddr = "127.0.0.1";
    @NeptusProperty(name = "PORT", userLevel = LEVEL.REGULAR, description = "Port to SAOP server")
    public int serverPort = 8888;
    @NeptusProperty(name = "Bind PORT", userLevel = LEVEL.ADVANCED, description = "Neptus internal configuration")
    public int bindPort = 8000;

    /**
     * Send periodically heartbeat to keep connection open
     */
    @Periodic(millisBetweenUpdates = 3000)
    public void sendHeartbeat() {
        boolean isConnected = imctt.getTcpTransport().connectIfNotConnected(ipAddr, serverPort);
        if( isConnected && !established){
          established = true;
          sendHeartbeat = true;
        }
        else if(!isConnected && established){
          established = false;
          sendHeartbeat = false;
        }
            
        if (sendHeartbeat) {
            Heartbeat hb = new Heartbeat();
            imctt.sendMessage(ipAddr, serverPort, hb, deliveryListener);
        }
    }

    public void initializeListeners() {

        msgListener = new MessageListener<MessageInfo, IMCMessage>() {
            @Override
            public void onMessage(MessageInfo info, IMCMessage msg) {
                NeptusLog.pub().info("Received: " + msg.getAbbrev() + " from: " + info.getPublisher() + ":"
                        + info.getPublisherPort());
                // if (info.getPublisherInetAddress().equalsIgnoreCase(ipAddr) && info.getPublisherPort() == port) {
                if (msg.getMgid() == PlanControl.ID_STATIC) {
                    PlanControl pc = msg.cloneMessageTyped();
                    addNewPlanControl(pc);
                }
                // }
                else if(msg.getMgid() == DevDataBinary.ID_STATIC) {
                    DevDataBinary data = msg.cloneMessageTyped();
                    //TODO insert deserialization code here
                }

                else if (msg.getMgid() == DevDataText.ID_STATIC) {
                    //Parse Json Object from IMC msg
                    JsonParser parser   = new JsonParser();
                    DevDataText data    = msg.cloneMessageTyped();
                    JsonElement element = parser.parse(data.getValue());
                    
                    //TODO create Polygon and paint it according to rgb code -> add it to Map
                    
                }
            }
        };

        deliveryListener = new MessageDeliveryListener() {

            @Override
            public void deliveryUnreacheable(IMCMessage msg) {
                NeptusLog.pub().debug(I18n.text("Destination " + ipAddr + ":" + serverPort
                        + "unreacheable. Unable to delivery " + msg.getAbbrev() + " to SAOP IMC TCP Server."));
            }

            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
            }

            @Override
            public void deliveryTimeOut(IMCMessage msg) {
                if (checkMsgId(msg.getMgid())) {
                    imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
                }
            }

            @Override
            public void deliverySuccess(IMCMessage msg) {
                // TODO remove from hash
            }

            @Override
            public void deliveryError(IMCMessage msg, Object error) {
                NeptusLog.pub().debug(I18n.text("Error delivering " + msg.getAbbrev() + " to SAOP IMC TCP Server."));
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
            if (established) {
                if (msg.getMgid() == PlanControl.ID_STATIC) {
                    sendHeartbeat = false;
                    PlanControl pc = (PlanControl) msg;
                    pc = (PlanControl) pc.cloneMessage();
                    if (!pc.getType().equals(pt.lsts.imc.PlanControl.TYPE.REQUEST)) { // REPLY SUCCESS or FAILURE or
                        // IN_PROGRESS

                        if (pc.getPlanId() != null) { // reset original SAOP data
                            String newName = pc.getPlanId();
                            String oldName = getOriginalName(newName);
                            pc.setPlanId(oldName);
                            pc.setRequestId(plans_reqId.get(newName));
                        }
                    }
                    imctt.sendMessage(ipAddr, serverPort, pc, deliveryListener);
                    // plans_reqId.remove(newName); plan Id is needed for PCS

                    sendHeartbeat = true;
                }
                else if (msg.getMgid() == PlanControlState.ID_STATIC) {
                    PlanControlState pcs = (PlanControlState) msg;
                    pcs = (PlanControlState) pcs.cloneMessage();
                    String newName = pcs.getPlanId();
                    String oldName = getOriginalName(newName);
                    if (plans_reqId.containsKey(pcs.getPlanId())) {
                        sendHeartbeat = false;
                        pcs.setPlanId(oldName);
                        imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
                        sendHeartbeat = true;
                    }
                }
                else if (msg.getMgid() == DevDataBinary.ID_STATIC) {
                    imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
                }
                else if (msg.getMgid() == EstimatedState.ID_STATIC) {
                    imctt.sendMessage(ipAddr, serverPort, msg, deliveryListener);
                }
            }
        }
    }

    public boolean checkMsgId(int msgId) {

        return msgId == PlanControl.ID_STATIC || msgId == PlanControlState.ID_STATIC || msgId == DevDataBinary.ID_STATIC
                || msgId == EstimatedState.ID_STATIC;
    }

    /**
     * Add new incoming PlanControl from the SAOP server to mission plan tree. The added plan has the format:
     * saop-planId
     */
    public void addNewPlanControl(PlanControl pc) {
        String pcsName = null;
        PlanSpecification ps;
        if(pc.getPlanId() != null){
            StringJoiner sj = new StringJoiner("", prefix, pc.getPlanId());
            pcsName = sj.toString();
            pc.setPlanId(pcsName);
            synchronized (plans_reqId) {
                plans_reqId.put(pcsName, pc.getRequestId());
            }
        }
        if (pc.getArg() != null) {
            if (pc.getArg().getClass().equals(PlanSpecification.class)) {
                ps = (PlanSpecification) pc.getArg();
                ps.setPlanId(pcsName);
                PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(), ps);
                plan.setVehicle("x8-02");
                synchronized (plans_reqId) {
                    plans_reqId.put(pcsName, pc.getRequestId());
                }
                pc.setArg(ps);
                
                getConsole().getMission().addPlan(plan);
                getConsole().getMission().save(true);
                getConsole().updateMissionListeners();
                getConsole().post(Notification.success(I18n.text("SAOP IMC TCP SERVER"),
                        I18n.textf("Received PlanSpecifition: %plan.", pcsName)));
            }

        }
        String system = IMCDefinition.getInstance().getResolver().resolve(pc.getDst());
        ImcSystem imcSys = ImcSystemsHolder.lookupSystemByName(system);
        if (debugMode) {
            if (pc.getOp().equals(OP.STOP) || pc.getOp().equals(OP.START) || pc.getOp().equals(OP.LOAD)) {
                String message = pc.getOpStr() + " from SAOP Server to " + imcSys;
                if (pc.getOp().equals(OP.START) && pcsName != null) {
                    message = message + " to plan: " + pc.getPlanId();
                }
                else if (pc.getOp().equals(OP.START) && pcsName != null) {
                    message = message + " to plan: " + pcsName;
                }
                else if (pc.getOp().equals(OP.START) && (pcsName == null && pc.getArg() == null))
                    return;
                int answer = GuiUtils.confirmDialog(this.getConsole(), "SAOP IMC TCP SERVER", "Allow " + message);
               if (answer == JOptionPane.OK_OPTION){
                    ImcMsgManager.getManager().sendMessage(pc);
                }
                else if (answer == JOptionPane.NO_OPTION) {
                    getConsole().post(Notification.warning(I18n.text("SAOP IMC TCP SERVER"),
                            I18n.text("Intercepted " + message)));
                }
            }

        }
        else {
            ImcMsgManager.getManager().sendMessage(pc);
            getConsole().post(Notification.success(I18n.text("SAOP IMC TCP SERVER"),
                    I18n.textf("Forwarded PlanControl %op.", pc.getOpStr())));
        }
    }

    /**
     * @param newName in the form: saop_oldName which can contain ``-'' (underscore) DEPREAC: saop_`TIMESTAMP`_oldName
     * @return Retrieves original name from the plan
     */
    public String getOriginalName(String newName) {
        StringBuilder sb = new StringBuilder();
        String[] names = newName.split("-");
        for (int i = 1; i < names.length; i++)
            sb.append(names[i]);
        return sb.toString();
    }

    /**
     * Updated plugins parameters
     */
    @Override
    public void setProperties(Property[] properties) {
        if ((boolean) properties[0].getValue() != debugMode) {
            debugMode = (boolean) properties[0].getValue();
        }
        if ((String) properties[1].getValue() != ipAddr) {
            ipAddr = (String) properties[1].getValue();
        }
        if ((int) properties[2].getValue() != serverPort) {
            serverPort = (int) properties[2].getValue();
        }
        if ((int) properties[3].getValue() != bindPort) {
            established = false;
            sendHeartbeat = false;
            bindPort = (int) properties[3].getValue();
            imctt.reStart();
            imctt.setBindPort(bindPort);
            imctt.addListener(msgListener);
            sendHeartbeat = true;
        }

        super.setProperties(properties);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#userControlsOpacity()
     */
    @Override
    public boolean userControlsOpacity() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#initLayer()
     */
    @Override
    public void initLayer() {
        initializeListeners();
        imctt = new ImcTcpTransport(bindPort, IMCDefinition.getInstance());
        imctt.addListener(msgListener);
        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsoleLayer#cleanLayer()
     */
    @Override
    public void cleanLayer() {
        established = false;
        sendHeartbeat = false;
        imctt.removeListener(msgListener);
        imctt.purge();
        
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        //TODO paint raster
    }
}