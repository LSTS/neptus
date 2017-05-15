///*
// * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
// * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
// * All rights reserved.
// * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
// *
// * This file is part of Neptus, Command and Control Framework.
// *
// * Commercial Licence Usage
// * Licencees holding valid commercial Neptus licences may use this file
// * in accordance with the commercial licence agreement provided with the
// * Software or, alternatively, in accordance with the terms contained in a
// * written agreement between you and Universidade do Porto. For licensing
// * terms, conditions, and further information contact lsts@fe.up.pt.
// *
// * Modified European Union Public Licence - EUPL v.1.1 Usage
// * Alternatively, this file may be used under the terms of the Modified EUPL,
// * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
// * included in the packaging of this file. You may not use this work
// * except in compliance with the Licence. Unless required by applicable
// * law or agreed to in writing, software distributed under the Licence is
// * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
// * ANY KIND, either express or implied. See the Licence for the specific
// * language governing permissions and limitations at
// * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
// * and http://ec.europa.eu/idabc/eupl.html.
// *
// * For more information please see <http://lsts.fe.up.pt/neptus>.
// *
// * Author: edrdo
// * May 14, 2017
// */
package pt.lsts.neptus.plugins.nvl_runtime;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractAction;
import javax.swing.JButton;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.VehicleState;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.nvl.dsl.NVLEngine;
import pt.lsts.nvl.runtime.NVLExecutionException;
import pt.lsts.nvl.runtime.NVLPlatform;
import pt.lsts.nvl.runtime.NVLVehicle;
import pt.lsts.nvl.runtime.tasks.PlatformTask;



@PluginDescription(name = "NVL Runtime Feature", author = "Keila Lima")
@Popup(pos = Popup.POSITION.BOTTOM_RIGHT, width=300, height=300)
@SuppressWarnings("serial")
public class NeptusRuntime extends ConsolePanel implements NVLPlatform {

    private final Map<String,IMCPlanTask> imcPlanTasks;
    
    public NeptusRuntime(ConsoleLayout layout) {
        super(layout);
        imcPlanTasks =  new ConcurrentHashMap<>();
    }

    @Subscribe
    public void on(PlanControlState pcs) {
      NeptusEvents.post(pcs); 
    }
    @Override
    public void initSubPanel() {
        //initialize existing plans in the console
        for(PlanType plan: getConsole().getMission().getIndividualPlansList().values()){
            imcPlanTasks.put(plan.getId(), new IMCPlanTask(plan));
            //System.out.println("P " + plan.getId());

        }
        pt.lsts.nvl.util.Debug.enable();
        NVLEngine.create(this);
        test();
    }

    
    private void test() {
        JButton testButton = new JButton(
                new AbstractAction(I18n.text("Test!")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {   
                        new Thread(() -> {
                          
                            NVLEngine.getInstance().run(imcPlanTasks.get("test"));
                        }).start();
                    }
                });

        add(testButton);

    }


    @Override
    public void cleanSubPanel() {

    }

    @Subscribe
    public void on(ConsoleEventPlanChange changedPlan) {
        PlanType oldPlan = changedPlan.getOld();
        PlanType newPlan = changedPlan.getCurrent();
        if (newPlan == null){
            // TODO: porquê este check?
            if(! getConsole().getMission().getIndividualPlansList().containsKey(oldPlan.getId())){
                imcPlanTasks.remove(changedPlan.getOld().getId());
            }
        }
        else{
            imcPlanTasks.put(newPlan.getId(), new IMCPlanTask(changedPlan.getCurrent()));
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.nvl.runtime.NVLPlatform#getConnectedVehicles()
     */
    @Override
    public List<NVLVehicle> getConnectedVehicles() {
        LinkedList<NVLVehicle> list = new LinkedList<>();
        for(ImcSystem vec: ImcSystemsHolder.lookupActiveSystemVehicles()){
            VehicleState state  = ImcMsgManager.getManager().getState(vec.getName()).last(VehicleState.class);
            if (state != null && state.getOpMode() == VehicleState.OP_MODE.SERVICE) {
                d("Adding %s", vec.getName());
                list.add(new NeptusVehicleAdapter(vec));
            }


        }
        return list;
    }

    /* (non-Javadoc)
     * @see pt.lsts.nvl.runtime.NVLPlatform#getPlatformTask(java.lang.String)
     */
    @Override
    public PlatformTask getPlatformTask(String id) {
        PlatformTask task = imcPlanTasks.get(id);
        if (task == null) {
            throw new NVLExecutionException("No such IMC plan: " + id);
        }
        return task;
    }

}
