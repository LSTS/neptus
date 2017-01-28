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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ProgressMonitor;

import pt.lsts.imc.PlanControl;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Multi-Vehicle Planner Interaction", icon="pt/lsts/neptus/plugins/pddl/wizard.png")
public class MVPlannerInteraction extends ConsoleInteraction {

    private Vector<MVPlannerTask> tasks = new Vector<MVPlannerTask>();
    private MVPlannerTask selectedTask = null;
    private Point2D lastPoint = null;
    private MVProblemSpecification problem = null;
    private LinkedHashMap<String, PlanType> generatedPlans = new LinkedHashMap<String, PlanType>();
    
    @NeptusProperty(name = "Domain Model to use")
    private MVDomainModel domainModel = MVDomainModel.V1;
    
    @NeptusProperty(name = "Seconds to search for optimal solution. Use 0 for multiple fast solutions.")
    private int seconds = 0;
    
    @NeptusProperty(name = "Number of fast solutions (if not optimizing).")
    private int numTries = 50;
        
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {

        g.setTransform(new AffineTransform());
        for (MVPlannerTask t : tasks) {
            t.paint((Graphics2D) g.create(), source);
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
        
        JPopupMenu popup = new JPopupMenu();
        final MVPlannerTask clickedTask = clicked;

        if (clicked != null) {
            popup.add("Remove " + clicked.getName()).addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    tasks.remove(clickedTask);
                    source.repaint();
                }
            });

            popup.add("Set payloads for " + clickedTask.getName()).addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    PropertiesEditor.editProperties(clickedTask, true);
                }
            });
            
            if (clickedTask instanceof SurveyAreaTask) {
                popup.add("Split " + clickedTask.getName()).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String answer = JOptionPane.showInputDialog(source, "Enter maximum time, in minutes, per task", (int)((SurveyAreaTask) clickedTask).getLength() / 60)+1;
                        if (answer != null) {
                            
                        }
                    }
                });
            }

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

        popup.add("Generate").addActionListener(this::generate);
        popup.addSeparator();
        popup.add("Start execution").addActionListener(this::startExecution);
        popup.addSeparator();
        popup.add("Settings").addActionListener(this::settings);
        
        popup.show(source, event.getX(), event.getY());
    }
    
    private void generate(ActionEvent action) {
        Thread t = new Thread("Generating Multi-Vehicle plan...") {
            public void run() {
                ProgressMonitor pm = new ProgressMonitor(getConsole(), "Searching for solutions...", "Generating initial state", 0, 5+numTries);
                
                Vector<VehicleType> activeVehicles = new Vector<VehicleType>();
                for (ImcSystem s : ImcSystemsHolder.lookupActiveSystemVehicles()) {
                    if (s.getTypeVehicle() == VehicleTypeEnum.UUV)
                        activeVehicles.addElement(VehiclesHolder.getVehicleById(s.getName()));
                }
                
                problem = new MVProblemSpecification(domainModel, activeVehicles, tasks, null);
                FileUtil.saveToFile("initial_state.pddl", problem.asPDDL());
                pm.setProgress(5);
                pm.setMillisToPopup(0);
                double bestYet = 0;
                String bestSolution = null;
                
                if (seconds == 0) {
                    for (int i = 0; i < numTries; i++) {
                        String best = "N/A";
                        if (bestSolution != null)
                            best = DateTimeUtil.milliSecondsToFormatedString((long)(bestYet * 1000));
                        if (pm.isCanceled())
                            return;
                        pm.setNote("Current best solution time: "+best);
                        pm.setProgress(5+i);
                        
                        try {
                            String solution = problem.solve(0);
                            if (solution.isEmpty())
                                continue;                
                            if (bestSolution == null) {
                                bestSolution = solution;
                                bestYet = solutionCost(solution);
                            }
                            else {
                                if (solutionCost(solution) < bestYet) {
                                    bestYet = solutionCost(solution);
                                    bestSolution = solution;
                                    
                                }
                            }                                
                        }
                        catch (Exception ex) {
                            NeptusLog.pub().error(ex);
                        }
                        pm.setProgress(5+numTries);                        
                    }
                    pm.close();
                }
                else {
                    try {                        
                        bestSolution = problem.solve(seconds);
                        
                        Thread progress = new Thread("Progress updater") {
                            public void run() {
                                pm.setMaximum(seconds*10);
                                pm.setProgress(0);
                                
                                for (int i = 0; i < seconds * 10; i++) {
                                    pm.setNote("Seconds ellapsed: "+i);
                                    pm.setProgress(i);
                                    try {
                                        Thread.sleep(100);
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                pm.setProgress(pm.getMaximum());
                                pm.close();
                            };
                        };
                        progress.setDaemon(true);
                        progress.start();
                    }
                    catch (Exception ex) {
                        NeptusLog.pub().error(ex);
                    }
                }

                generatedPlans.clear();
                if (bestSolution != null) {
                    MVSolution solution = problem.getSolution();
                    if (solution != null) {
                        Collection<PlanType> plans = solution.generatePlans();
                        for (PlanType pt : plans) {
                            pt.setMissionType(getConsole().getMission());
                            getConsole().getMission().getIndividualPlansList().put(pt.getId(), pt);
                            generatedPlans.put(pt.getVehicle(), pt);
                        }
                        getConsole().warnMissionListeners();
                        getConsole().getMission().save(true);
                    }
                    GuiUtils.htmlMessage(getConsole(), "Multi-Vehicle Planner", "Valid solution found", "<html><pre>" + bestSolution
                            + "</pre></html>");
                    System.out.println(bestSolution);
                }
                else
                    GuiUtils.errorMessage(getConsole(), new Exception("No solution has been found."));
            }
        };
        t.setDaemon(true);
        t.start();
    }
    
    private void startExecution(ActionEvent action) {
        for (Entry<String, PlanType> generated : generatedPlans.entrySet()) {
            PlanControl startPlan = new PlanControl();
            startPlan.setType(pt.lsts.imc.PlanControl.TYPE.REQUEST);
            startPlan.setOp(pt.lsts.imc.PlanControl.OP.START);
            startPlan.setPlanId(generated.getValue().getId());
            startPlan.setArg(generated.getValue().asIMCPlan(true));
            int reqId = IMCSendMessageUtils.getNextRequestId();
            startPlan.setRequestId(reqId);
            ImcMsgManager.getManager().sendMessageToVehicle(startPlan, generated.getKey(), null);                    
        }
    }
    
    private void settings(ActionEvent action) {
        PluginUtils.editPluginProperties(this, true);
    }

    private double solutionCost(String solution) {
        String parts[] = solution.split("\n");
        if (parts.length == 0)
            return Double.MAX_VALUE;
        String line = parts[parts.length-1]; 
        return Double.parseDouble(line.substring(0, line.indexOf(':')-1));
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
            selectedTask.growLength(-yamount * 5 / source.getZoom());
            selectedTask.growWidth(xamount * 5 / source.getZoom());
        }
        else if (event.isShiftDown()) {
            double angle = selectedTask.getCenterLocation().getXYAngle(now);
            selectedTask.setYaw(angle);
            
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
        inter.init(ConsoleLayout.forge());
        renderer.setActiveInteraction(inter);
        GuiUtils.testFrame(renderer);
    }
}
