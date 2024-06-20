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
 * Author: José Pinto
 * Jun 1, 2011
 */
package pt.lsts.neptus.console.plugins.planning.plandb;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * @author zp
 * 
 */

public class PlanDBControl implements MessageListener<MessageInfo, IMCMessage> {

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
        IMCMessage imc_PlanDB = IMCDefinition.getInstance().create("PlanDB", "type", "REQUEST", "op", "CLEAR",
                "request_id", IMCSendMessageUtils.getNextRequestId());

        return ImcMsgManager.getManager().sendMessageToSystem(imc_PlanDB, remoteSystemId);
    }

    public boolean sendPlan(PlanType plan) {
        IMCMessage imc_PlanDB = IMCDefinition.getInstance().create("PlanDB", "type", "REQUEST", "op", "SET",
                "request_id", IMCSendMessageUtils.getNextRequestId(), "plan_id", plan.getId(), "arg", plan.asIMCPlan(),
                "info", "");

        return ImcMsgManager.getManager().sendMessageToSystem(imc_PlanDB, remoteSystemId);
    }

    public boolean requestPlan(String plan_id) {
        IMCMessage imc_PlanDB = IMCDefinition.getInstance().create("PlanDB", "type", "REQUEST", "op", "GET",
                "request_id", IMCSendMessageUtils.getNextRequestId(), "plan_id", plan_id);
        return ImcMsgManager.getManager().sendMessageToSystem(imc_PlanDB, remoteSystemId);
    }

    public boolean requestActivePlan() {
        return requestPlan(null);
    }

    public boolean requestPlanInfo(String plan_id) {
        IMCMessage imc_PlanDB = IMCDefinition.getInstance().create("PlanDB", "type", "REQUEST", "op", "GET_INFO",
                "request_id", IMCSendMessageUtils.getNextRequestId(), "plan_id", plan_id);
        return ImcMsgManager.getManager().sendMessageToSystem(imc_PlanDB, remoteSystemId);
    }

    public boolean deletePlan(String plan_id) {
        IMCMessage imc_PlanDB = IMCDefinition.getInstance().create("PlanDB", "type", "REQUEST", "op", "DEL",
                "request_id", IMCSendMessageUtils.getNextRequestId(), "plan_id", plan_id);
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
                    e.printStackTrace();
                }
            }

            else if (msg.getString("op").equals("GET")) {
                PlanType pt = IMCUtils.parsePlanSpecification(new MissionType(),
                        msg.getMessage("arg"));
               
                for (IPlanDBListener l : listeners.toArray(new IPlanDBListener[0]))
                    l.dbPlanReceived(pt);
            }
            else if (msg.getString("op").equals("GET_INFO")) {
                PlanDBInfo pinfo = new PlanDBInfo();
                pinfo.parseIMCMessage(msg.getMessage("arg"));
                remoteState.storedPlans.put(msg.getAsString("plan_id"), pinfo);

                try {
                    for (IPlanDBListener l : listeners)
                        l.dbInfoUpdated(remoteState);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
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
                for (IPlanDBListener l : listeners)
                    l.dbPlanSent(msg.getAsString("plan_id"));
            }
        }
    }
}