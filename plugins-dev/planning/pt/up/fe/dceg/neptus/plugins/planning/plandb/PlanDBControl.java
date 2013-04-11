/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Jun 1, 2011
 */
package pt.up.fe.dceg.neptus.plugins.planning.plandb;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageListener;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.ByteUtil;
import pt.up.fe.dceg.neptus.util.comm.IMCSendMessageUtils;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */                

public class PlanDBControl implements MessageListener<MessageInfo, IMCMessage>{

    protected String remoteSystemId = null;
    protected PlanDBState remoteState = null;
    
    protected final int planDB_id = IMCDefinition.getInstance().getMessageId("PlanDB");
    
    protected Vector<IPlanDBListener> listeners = new Vector<IPlanDBListener>();
    
    public boolean addListener(IPlanDBListener listener) {
        if (!listeners.contains(listener))
            return listeners.add(listener);
        return false;
    }
    
    public boolean removeListener(IPlanDBListener listener) {
        return listeners.remove(listener);
    }
    
    /**
     * @return the remoteSystemId
     */
    public String getRemoteSystemId() {
        return remoteSystemId;
    }
    
    /**
     * @param remoteSystemId the remoteSystemId to set
     */
    public void setRemoteSystemId(String remoteSystemId) {
        if (remoteState == null || this.remoteSystemId == null || !remoteSystemId.equals(this.remoteSystemId)) {
            remoteState = new PlanDBState();
        }
        this.remoteSystemId = remoteSystemId;
    }
    
    /**
     * @return the remoteState
     */
    public PlanDBState getRemoteState() {
        return remoteState;
    }
    
    public boolean clearDatabase() {
        IMCMessage imc_PlanDB = IMCDefinition.getInstance().create("PlanDB",
                "type", "REQUEST",
                "op", "CLEAR",
                "request_id", IMCSendMessageUtils.getNextRequestId()
        );
        
        return ImcMsgManager.getManager().sendMessageToSystem(imc_PlanDB, remoteSystemId);
    }
    
    public boolean sendPlan(PlanType plan) {
        IMCMessage imc_PlanDB = IMCDefinition.getInstance().create("PlanDB",
                "type", "REQUEST",
                "op", "SET",
                "request_id", IMCSendMessageUtils.getNextRequestId(),
                "plan_id", plan.getId(),
                "arg", plan.asIMCPlan(),
                "info", "Plan sent by Neptus version "+ConfigFetch.getNeptusVersion()
        );
        
//        NeptusLog.pub().info("<###>Sending "+imc_PlanDB.toString() +" to "+remoteSystemId);
        return ImcMsgManager.getManager().sendMessageToSystem(imc_PlanDB, remoteSystemId);
    }
    
    public boolean requestPlan(String plan_id) {
        IMCMessage imc_PlanDB = IMCDefinition.getInstance().create("PlanDB",
                "type", "REQUEST",
                "op", "GET",
                "request_id", IMCSendMessageUtils.getNextRequestId(),
                "plan_id", plan_id
        );
        
//        NeptusLog.pub().info("<###>Sending "+imc_PlanDB.toString() +" to "+remoteSystemId);
        return ImcMsgManager.getManager().sendMessageToSystem(imc_PlanDB, remoteSystemId);
    }
    
    public boolean requestActivePlan() {
        return requestPlan(null);
    }
    
    public boolean requestPlanInfo(String plan_id) {
        IMCMessage imc_PlanDB = IMCDefinition.getInstance().create("PlanDB",
                "type", "REQUEST",
                "op", "GET_INFO",
                "request_id", IMCSendMessageUtils.getNextRequestId(),
                "plan_id", plan_id
        );
//        NeptusLog.pub().info("<###>Sending "+imc_PlanDB.toString() +" to "+remoteSystemId);
        return ImcMsgManager.getManager().sendMessageToSystem(imc_PlanDB, remoteSystemId);
    }
    
    
    public boolean deletePlan(String plan_id) {
        IMCMessage imc_PlanDB = IMCDefinition.getInstance().create("PlanDB",
                "type", "REQUEST",
                "op", "DEL",
                "request_id", IMCSendMessageUtils.getNextRequestId(),
                "plan_id", plan_id
        );
        NeptusLog.pub().debug("Sending to " + remoteSystemId);
        return ImcMsgManager.getManager().sendMessageToSystem(imc_PlanDB, remoteSystemId);
    }
    
    
    public void updateKnownState(IMCMessage imc_PlanDBState) {
        if (remoteState == null)
            remoteState = new PlanDBState();
        remoteState.parseIMCMessage(imc_PlanDBState);
    }
    
    @Override
    public void onMessage(MessageInfo info, IMCMessage msg) {
        
        if (msg.getMgid() != planDB_id)
            return;
        
        if (remoteState == null) {
            try {
                setRemoteSystemId(ImcSystemsHolder.lookupSystem(new ImcId16(msg.getHeaderValue("src"))).getName());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        //msg.dump(System.err);
        //NeptusLog.pub().info("<###> "+msg.getString("op"));
        if (msg.getString("type").equals("SUCCESS")) {
            if (msg.getString("op").equals("GET_STATE")) {
                if (remoteState == null)
                    remoteState = new PlanDBState();
                remoteState.parseIMCMessage(msg.getMessage("arg"));         
                
                try {
                    for (IPlanDBListener l : listeners)
                        l.dbInfoUpdated(remoteState);
                }
                catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            else if (msg.getString("op").equals("GET")) {
//                if (console != null) {
                    PlanType pt = IMCUtils.parsePlanSpecification(new MissionType()/*console.getMission()*/, msg.getMessage("arg"));
                    IMCMessage p0 = msg.getMessage("arg");
                    NeptusLog.pub().info("<###>Plan received        " + pt.getId() + " with MD5 " + ByteUtil.encodeAsString(p0.payloadMD5()));
                    IMCMessage p1 = pt.asIMCPlan();
                    NeptusLog.pub().info("<###>Plan from plan       " + pt.getId() + " with MD5 " + ByteUtil.encodeAsString(p1.payloadMD5()));
                    IMCMessage p2 = pt.clonePlan().asIMCPlan();
                    NeptusLog.pub().info("<###>Plan from clone plan " + pt.getId() + " with MD5 " + ByteUtil.encodeAsString(p2.payloadMD5()));

                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        IMCOutputStream imcOs = new IMCOutputStream(baos);
                        p0.serialize(imcOs);
                        ByteUtil.dumpAsHex(baos.toByteArray(), System.out);
                        baos = new ByteArrayOutputStream();
                        imcOs = new IMCOutputStream(baos);
                        p1.serialize(imcOs);
                        ByteUtil.dumpAsHex(baos.toByteArray(), System.out);
                        baos = new ByteArrayOutputStream();
                        imcOs = new IMCOutputStream(baos);
                        p2.serialize(imcOs);
                        ByteUtil.dumpAsHex(baos.toByteArray(), System.out);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                    
                    for (IPlanDBListener l : listeners.toArray(new IPlanDBListener[0]))
                        l.dbPlanReceived(pt.clonePlan());
//                }
            }
            else if (msg.getString("op").equals("GET_INFO")) {
                PlanDBInfo pinfo = new PlanDBInfo();
                pinfo.parseIMCMessage(msg.getMessage("arg"));
                remoteState.storedPlans.put(msg.getAsString("plan_id"), pinfo);                
            }
            else if (msg.getString("op").equals("DEL")) {
                remoteState.storedPlans.remove(msg.getAsString("plan_id"));
                
                for (IPlanDBListener l : listeners)
                    l.dbPlanRemoved(msg.getAsString("plan_id"));

            }
            else if (msg.getString("op").equals("CLEAR")) {                
                remoteState.storedPlans.clear();
            }
            else if (msg.getString("op").equals("SET")) {
                requestPlanInfo(msg.getAsString("plan_id"));
//                if (console != null)
//                    console.info("[PlanDB] The plan '"+msg.getAsString("plan_id")+"' has been successfully received.");
                
                for (IPlanDBListener l : listeners)
                    l.dbPlanSent(msg.getAsString("plan_id"));
            }
        }
    }

//    /**
//     * @return the console
//     */
//    public ConsoleLayout getConsole() {
//        return console;
//    }
//
//    /**
//     * @param console the console to set
//     */
//    public void setConsole(ConsoleLayout console) {
//        this.console = console;
//        setRemoteSystemId(console.getMainVehicle());
//    }
}