/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Mar 31, 2014
 */
package pt.lsts.neptus.console.plugins.planning;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.TreeMap;

import javax.swing.JOptionPane;
import javax.swing.tree.TreePath;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBAdapter;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBState;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ByteUtil;

/**
 * @author zp
 * 
 */
public class MissionTreePlanDbAdapter extends PlanDBAdapter {

    private ConsoleLayout console;
    private MissionTreePanel missionTree;
    private boolean debugOn = true;
    
    public MissionTreePlanDbAdapter(ConsoleLayout console, MissionTreePanel missionTree) {
        this.console = console;
        this.missionTree = missionTree;
    }
    
    // Called only if Type == SUCCESS in received PlanDB message
    @Override
    public void dbCleared() {
    }

    @Override
    public void dbInfoUpdated(PlanDBState updatedInfo) {
        // PlanDB Operation = GET_STATE
        // Get state of the entire DB. Successful replies will yield a
        // 'PlanDBState' message in the 'arg' field but without
        // individual plan information (in the 'plans_info' field of
        // 'PlanDBState').
        TreePath[] selectedNodes = missionTree.browser.getSelectionPath();

        // browser.transUpdateElapsedTime(); //TODO

        TreeMap<String, PlanType> localPlans;
        try {
            localPlans = console.getMission().getIndividualPlansList();
        }
        catch (NullPointerException e) {
            NeptusLog.pub().warn("I cannot find local plans for " + console.getMainSystem());
            localPlans = new TreeMap<String, PlanType>();
        }
        missionTree.browser.updatePlansStateEDT(localPlans, console.getMainSystem());
        missionTree.browser.setSelectedNodes(selectedNodes);
        // System.out.println("dbInfoUpdated");
    }

    @Override
    public void dbPlanReceived(PlanType spec) {
        // PlanDB Operation = GET
        // Get a plan stored in the DB.The 'plan_id' field identifies
        // the plan. Successful replies will yield a
        // 'PlanSpecification' message in the 'arg' field.
        // Update when received remote plan into our system
        // Put plan in mission
        PlanType lp = console.getMission().getIndividualPlansList().get(spec.getId());
        spec.setMissionType(console.getMission());

        boolean alreadyLocal = console.getMission().getIndividualPlansList().containsKey(spec.getId());

        if (alreadyLocal) {
            PlanSpecification remote = (PlanSpecification) spec.asIMCPlan();
            PlanSpecification local = (PlanSpecification) console.getMission().getIndividualPlansList()
                    .get(spec.getId()).asIMCPlan();
            if (!ByteUtil.equal(local.payloadMD5(), remote.payloadMD5())) {
                int option = JOptionPane.showConfirmDialog(console,
                        I18n.text("Replace plan '" + spec.getId() + "' with received version?"));
                if (option != JOptionPane.YES_OPTION)
                    return;
            }
        }

        console.getMission().addPlan(spec);
        // Save mission
        console.getMission().save(true);
        // Alert listeners
        console.updateMissionListeners();

        if (console.getPlan().getId().equals(spec.getId())) {
            console.setPlan(spec);
        }

        console.post(Notification.success(I18n.text("Plan Dissemination"),
                I18n.textf("Received plan '%plan' from vehicle.", spec.getId())));

        if (debugOn && lp != null) {
            try {
                IMCMessage p1 = lp.asIMCPlan(), p2 = spec.asIMCPlan();
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IMCOutputStream imcOs = new IMCOutputStream(baos);
                IMCDefinition.getInstance().serializeFields(p1, imcOs);
                ByteUtil.dumpAsHex(baos.toByteArray(), System.out);
                ByteUtil.dumpAsHex(p1.payloadMD5(), System.out);
                
                baos = new ByteArrayOutputStream();
                imcOs = new IMCOutputStream(baos);
                IMCDefinition.getInstance().serializeFields(p2, imcOs);
                ByteUtil.dumpAsHex(baos.toByteArray(), System.out);
                ByteUtil.dumpAsHex(p2.payloadMD5(), System.out);
                
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        // System.out.println("dbPlanReceived");
    }

    @Override
    public void dbPlanRemoved(String planId) {
        // PlanDB Operation = DEL
        // Delete a plan from the DB. The 'plan_id' field identifies
        // the plan to delete.
        missionTree.browser.removeCurrSelectedNodeRemotely();
        // System.out.println("dbPlanRemoved");
    }

    @Override
    public void dbPlanSent(String planId) {
        // PlanDB Operation = SET message.
        // Set a plan in the DB. The 'plan_id' field identifies the
        // plan, and a pre-existing plan with the same identifier, if
        // any will be overwritten. For requests, the 'arg' field must
        // contain a 'PlanSpecification' message.
        // System.out.println("dbPlanSent");
        // sent plan confirmation, is now in sync
        missionTree.browser.setPlanAsSync(planId);
    }

    /**
     * @param debugOn the debugOn to set
     */
    public void setDebugOn(boolean debugOn) {
        this.debugOn = debugOn;
    }
}
