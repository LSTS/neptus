/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Author: edrdo
 * May 16, 2017
 */
package pt.lsts.neptus.plugins.nvl;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import pt.lsts.imc.VehicleState;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.nvl.dsl.NVLEngine;
import pt.lsts.nvl.runtime.NVLExecutionException;
import pt.lsts.nvl.runtime.NVLPlatform;
import pt.lsts.nvl.runtime.NVLVehicle;
import pt.lsts.nvl.runtime.tasks.PlatformTask;

public enum NeptusNVLPlatform implements NVLPlatform {
    INSTANCE;

    private final Map<String,IMCPlanTask> imcPlanTasks = new ConcurrentHashMap<>();

    public static final NeptusNVLPlatform getInstance() {
        return INSTANCE;
    }

    private NVLConsolePanel consolePanel;

    private NeptusNVLPlatform() {
        pt.lsts.nvl.util.Debug.enable();
        NVLEngine.create(this);
        consolePanel = null;
        d("initialized");
    }

    public void associateTo(NVLConsolePanel cp) {
        detach();
        d("attached to console");
        consolePanel = cp;
        for(PlanType plan: cp.getConsole().getMission().getIndividualPlansList().values()){
            nvlInfoMessage("IMC plan available: %s", plan.getId());
            imcPlanTasks.put(plan.getId(), new IMCPlanTask(plan));
        }
    }

    public void detach() {
        imcPlanTasks.clear();
        if (consolePanel != null) {
            consolePanel = null;
            d("detach from console");
        }
    }

    @Override
    public List<NVLVehicle> getConnectedVehicles() {
        LinkedList<NVLVehicle> list = new LinkedList<>();
        for(ImcSystem vec: ImcSystemsHolder.lookupActiveSystemVehicles()){
            VehicleState state  = ImcMsgManager.getManager().getState(vec.getName()).last(VehicleState.class);
            if (state != null && state.getOpMode() == VehicleState.OP_MODE.SERVICE) {
                nvlInfoMessage("%s is available!", vec.getName());
                list.add(new NeptusNVLVehicle(vec));
            }


        }
        return list;
    }

    public void onPlanChanged(ConsoleEventPlanChange changedPlan) {
        PlanType oldPlan = changedPlan.getOld();
        PlanType newPlan = changedPlan.getCurrent();

        if (newPlan == null){
            if(! consolePanel.getConsole().getMission().getIndividualPlansList().containsKey(oldPlan.getId())){
                nvlInfoMessage("removing IMC plan %s", oldPlan.getId());
                imcPlanTasks.remove(oldPlan.getId());
            }
        }
        else{
            nvlInfoMessage("replacing IMC plan %s", oldPlan.getId());
            imcPlanTasks.put(newPlan.getId(), new IMCPlanTask(newPlan));
        }

    }
    @Override
    public PlatformTask getPlatformTask(String id) {
        PlatformTask task = imcPlanTasks.get(id);
        if (task == null) {
            throw new NVLExecutionException("No such IMC plan: " + id);
        }
        return task;
    }


    public void run(String planId) {
        if (imcPlanTasks.containsKey(planId)) {
            d("will run %s", planId);
            NVLEngine.getInstance().run(imcPlanTasks.get(planId));
        }
    }

    public void run(File scriptFile) {
        if (scriptFile.exists()) {
            nvlInfoMessage("will run %s", scriptFile.getAbsolutePath());
            NVLEngine.getInstance().run(scriptFile);
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.nvl.runtime.NVLPlatform#nvlInfoMessage(java.lang.String, java.lang.Object[])
     */
    @Override
    public void nvlInfoMessage(String fmt, Object... args) {
      d(fmt, args);
      if (consolePanel != null) {
          consolePanel.displayMessage(fmt, args); 
      }
    }

 



}
