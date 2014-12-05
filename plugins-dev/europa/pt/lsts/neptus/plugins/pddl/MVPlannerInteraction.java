/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Nov 25, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JPopupMenu;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBAdapter;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBControl;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBState;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.pddl.MVSolution.MVPlans;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="Multi-Vehicle Planner Interaction")
public class MVPlannerInteraction extends ConsoleInteraction {
    
    private Vector<MVPlannerTask> tasks = new Vector<MVPlannerTask>();
    private MVPlannerTask selectedTask = null;    
    private Point2D lastPoint = null;
    
    MVProblemSpecification problem = null;
    MVExecutor executor = null;
    
    protected PlanDBControl pdbControl;
    
    protected PlanDBAdapter planDBListener = new PlanDBAdapter() {
        @Override
        public void dbCleared() {
        }

        @Override
        public void dbInfoUpdated(PlanDBState updatedInfo) {
        }

        @Override
        public void dbPlanReceived(PlanType spec) {
            spec.setMissionType(getConsole().getMission());
            getConsole().getMission().addPlan(spec);
            getConsole().getMission().save(true);
            getConsole().updateMissionListeners();

        }

        @Override
        public void dbPlanRemoved(String planId) {
        }

        @Override
        public void dbPlanSent(String planId) {
        }
    };
    
    private void removePlanDBListener() {
        if (pdbControl != null)
            pdbControl.removeListener(planDBListener);
    }
    
    private void planControlUpdate(String id) {
        removePlanDBListener();
        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(id);

        if (sys == null) {
            pdbControl = null;
            NeptusLog.pub().error(
                    "The main vehicle selected " + id
                    + " is not in the vehicles-defs folder. Please add definitions file for this vehicle.");
        }
        else {
            pdbControl = sys.getPlanDBControl();
            if (pdbControl == null) {
                pdbControl = new PlanDBControl();
                pdbControl.setRemoteSystemId(id);
            }

            pdbControl.addListener(planDBListener);
        }
    }
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        
        g.setTransform(new AffineTransform());
        for (MVPlannerTask t : tasks) {
            t.paint((Graphics2D)g.create(), source);
        }
        
        super.paintInteraction(g, source);
    }
    
    @Override
    public void mouseClicked(final MouseEvent event, final StateRenderer2D source) {
        if (event.getButton() != MouseEvent.BUTTON3) {
            super.mouseClicked(event, source);
            return;
        }
        MVPlannerTask clicked = null;
        LocationType lt = source.getRealWorldLocation(event.getPoint());
        for (MVPlannerTask t : tasks) {
            if (t.containsPoint(lt, source)) {
                clicked = t;
                break;
            }
        }

//        if (clicked == null) {
//            super.mouseClicked(event, source);
//            return;
//        }
        
        JPopupMenu popup = new JPopupMenu();
        final MVPlannerTask clickedTask = clicked;
        
        if (clicked != null) {
            popup.add("Remove "+clicked.getName()).addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    tasks.remove(clickedTask);        
                    source.repaint();
                }
            });
            
            popup.add("Set payloads for "+clickedTask.getName()).addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    PropertiesEditor.editProperties(clickedTask, true);                 
                }
            });
            
            popup.addSeparator();
            
        }
        
        popup.add("Add survey task").addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                SurveyAreaTask task = new SurveyAreaTask(source.getRealWorldLocation(event.getPoint()));
                if (!PropertiesEditor.editProperties(task, true))
                    tasks.add(task);
                source.repaint();
            }
        });
        popup.add("Add sample task").addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                SamplePointTask task = new SamplePointTask(source.getRealWorldLocation(event.getPoint()));
                if (!PropertiesEditor.editProperties(task, true))
                    tasks.add(task);
                source.repaint();
            }
        });
        

        
        popup.add("Generate").addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                Vector<VehicleType> activeVehicles = new Vector<VehicleType>();
                for (ImcSystem s : ImcSystemsHolder.lookupActiveSystemVehicles()) {
                    if (s.getTypeVehicle() == VehicleTypeEnum.UUV)
                        activeVehicles.addElement(VehiclesHolder.getVehicleById(s.getName()));
                }
                
                problem = new MVProblemSpecification(activeVehicles, tasks, null);
                System.out.println(problem.asPDDL());
                FileUtil.saveToFile("initial_state.pddl", problem.asPDDL());
                try {
                   String solution = problem.solve();
                    if (solution.isEmpty()) 
                        throw new Exception("No solution has been found.");
                    GuiUtils.htmlMessage(getConsole(), "found solution", "", "<html><pre>"+solution+"</pre></html>");
                    System.out.println(solution);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    GuiUtils.errorMessage(getConsole(), ex);
                }
//                try {
//                    String result = p.solve();
//                    GuiUtils.infoMessage(getConsole(), "Solution", result);                    
//                }
//                catch (Exception ex) {
//                    GuiUtils.errorMessage(getConsole(), ex);
//                }
                
                
            }
        });
        popup.addSeparator();
        popup.add("Send solution to vehicles").addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
               if (problem!=null && problem.getSolution()!=null) {
                   //generate plans from the found solution
                    ArrayList<MVPlans> genPlans = problem.getSolution().generatePlans();
                    
                    for (MVPlans entry : genPlans) {
                        VehicleType vehicle = entry.getVehicle();
                        String vehicleId = vehicle.getId();
                        planControlUpdate(vehicleId);
                        
                        for (Entry<Long, PlanType> pl : entry.getPlanList().entrySet()) {                       
                            PlanType plan = pl.getValue();
                            String planId = plan.getId();
                            
                            plan.setMissionType(getConsole().getMission());
                            
                            // add plans to mission
                            getConsole().getMission().addPlan(plan);
                            
                            /* FIXME SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                                @Override
                                protected Void doInBackground() throws Exception {
                                    getConsole().getMission().save(true);
                                    return null;
                                }
                            };
                            worker.execute();*/
                            
                            /*//send plans to vehicle
                            pdbControl.setRemoteSystemId(vehicleId);
                            pdbControl.sendPlan(plan);
                            System.out.println(">> Sending plan '"+planId+ "' to vehicle "+vehicleId);*/
                        }
                    }
//                    if (pdbControl == null) {
//                        return;
//                    }
                    executor = new MVExecutor(genPlans);
                }
            }
        });
        
        
        popup.add("Start Execution").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                executor.init();
            }
        });

        popup.show(source, event.getX(), event.getY());
    }
    
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        MVPlannerTask selected = null;
        LocationType lt = source.getRealWorldLocation(event.getPoint());
        for (MVPlannerTask t : tasks) {
            if (t.containsPoint(lt, source)) {
                selected = t;
                break;
            }
        }
        if (selected == null)
            super.mousePressed(event, source);
        else {
            selectedTask = selected;
            lastPoint = event.getPoint();
        }
    }
        
    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        selectedTask = null;
        lastPoint = null;
        super.mouseReleased(event, source);
    }
    
    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (selectedTask == null) {
            super.mouseDragged(event, source);
            return;
        }
       
        LocationType prev = source.getRealWorldLocation(lastPoint);
        LocationType now = source.getRealWorldLocation(event.getPoint());
        
        double xamount = event.getX() - lastPoint.getX();
        double yamount = event.getY() - lastPoint.getY();
        
        if (event.isControlDown()) {
            selectedTask.growLength(-yamount*5 / source.getZoom());
            selectedTask.growWidth(xamount*5 / source.getZoom());
        }
        else if (event.isShiftDown()) {
            selectedTask.rotate(Math.toRadians((yamount+xamount)*3));
        }
        else {
            double offsets[] = now.getOffsetFrom(prev);
            selectedTask.translate(offsets[0], offsets[1]);
        }
        
        // change selected task
        lastPoint = event.getPoint();
    }
    
    @Override
    public void initInteraction() { 
        
    }

    @Override
    public void cleanInteraction() {

    }
    
    /**
     * @return the tasks
     */
    public Vector<MVPlannerTask> getTasks() {
        return tasks;
    }

    public static void main(String[] args) {
        StateRenderer2D renderer = new StateRenderer2D();
        MVPlannerInteraction inter = new MVPlannerInteraction();
        inter.init(new ConsoleLayout());
        renderer.setActiveInteraction(inter);
        GuiUtils.testFrame(renderer);
    }
}
