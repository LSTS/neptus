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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.ProgressMonitor;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.PlanControl;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventFutureState;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.mvplanner.api.ConsoleEventPlanAllocation;
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
@PluginDescription(name = "Multi-Vehicle Planner Interaction", icon = "pt/lsts/neptus/plugins/pddl/wizard.png")
public class MVPlannerInteraction extends ConsoleInteraction {

    private ArrayList<MVPlannerTask> tasks = new ArrayList<MVPlannerTask>();
    private MVPlannerTask selectedTask = null;
    private Point2D lastPoint = null;
    private MVProblemSpecification problem = null;
    private LinkedHashMap<String, PlanType> generatedPlans = new LinkedHashMap<String, PlanType>();
    private LinkedHashMap<String, ConsoleEventFutureState> futureStates = new LinkedHashMap<String, ConsoleEventFutureState>();

    @NeptusProperty(category = "Problem Specification", name = "Domain Model to use")
    private MVDomainModel domainModel = MVDomainModel.V2;

    @NeptusProperty(category = "Problem Specification", name = "Time (seconds) vehicles can stay away from depot")
    private int secondsAway = 1000;

    @NeptusProperty(category = "Plan Generation", name = "Time (seconds) to search for optimal solution.")
    private int searchSeconds = 10;

    @NeptusProperty(category = "Plan Generation", name = "Number of alternative solutions (if not timed).", userLevel = LEVEL.ADVANCED)
    private int numTries = 50;

    @NeptusProperty(category = "Plan Generation", name = "Include pop-ups in generated plans.")
    private boolean generatePopups = false;

    @NeptusProperty(category = "Plan Generation", name = "Use scheduled waypoints.")
    private boolean useScheduledGotos = true;

    @NeptusProperty(category = "Plan Generation", name = "Manual execution.")
    private boolean manualExec = false;

    @Subscribe
    public void on(ConsoleEventFutureState future) {
        synchronized (futureStates) {
            if (future.getState() == null)
                futureStates.remove(future.getVehicle());
            else {
                futureStates.put(future.getVehicle(), future);
                // System.out.println(future);
            }
        }
    }

    @Subscribe
    public void on(ConsoleEventPlanAllocation allocation) {

        System.out.println(allocation.getId()+" : "+allocation.getOp());
        try {
            switch (allocation.getOp()) {
                case FINISHED:
                    synchronized (tasks) {
                        Iterator<MVPlannerTask> it = tasks.iterator();
                        while (it.hasNext()) {
                            MVPlannerTask t = it.next();
                            if (t.associatedAllocation.equals(allocation.getId()))
                                it.remove();
                        }
                    }
                    break;
                case INTERRUPTED:
                    synchronized (tasks) {
                        Iterator<MVPlannerTask> it = tasks.iterator();
                        while (it.hasNext()) {
                            MVPlannerTask t = it.next();
                            if (t.associatedAllocation.equals(allocation.getId()))
                                t.associatedAllocation = null;                            
                        }
                    }
                default:
                    break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

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
            if (t.getAssociatedAllocation() != null)
                continue;
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
        if (manualExec) {
            popup.add("Execute now").addActionListener(this::startExecution);
            popup.addSeparator();
        }
        popup.add("Settings").addActionListener(this::settings);

        popup.show(source, event.getX(), event.getY());
    }

    private void generate(ActionEvent action) {
        Thread t = new Thread("Generating Multi-Vehicle plan...") {
            public void run() {
                ProgressMonitor pm = new ProgressMonitor(getConsole(), "Searching for solutions...",
                        "Generating initial state", 0, 5 + numTries);

                Vector<VehicleType> activeVehicles = new Vector<VehicleType>();
                for (ImcSystem s : ImcSystemsHolder.lookupActiveSystemVehicles()) {
                    if (s.getTypeVehicle() == VehicleTypeEnum.UUV)
                        activeVehicles.addElement(VehiclesHolder.getVehicleById(s.getName()));
                }

                ArrayList<ConsoleEventFutureState> futures = new ArrayList<>();
                synchronized (futureStates) {
                    futures.addAll(futureStates.values());
                }

                problem = new MVProblemSpecification(domainModel, activeVehicles, tasks, futures, null, secondsAway);
                FileUtil.saveToFile("initial_state.pddl", problem.asPDDL());
                pm.setProgress(5);
                pm.setMillisToPopup(0);
                double bestYet = 0;
                String bestSolution = null;

                if (searchSeconds == 0) {
                    for (int i = 0; i < numTries; i++) {
                        String best = "N/A";
                        if (bestSolution != null)
                            best = DateTimeUtil.milliSecondsToFormatedString((long) (bestYet * 1000));
                        if (pm.isCanceled())
                            return;
                        pm.setNote("Current best solution time: " + best);
                        pm.setProgress(5 + i);

                        try {
                            if (!problem.solve(0))
                                continue;

                            if (bestSolution == null) {
                                bestSolution = problem.toString();
                                bestYet = solutionCost(problem.toString());
                            }
                            else {
                                if (solutionCost(problem.toString()) < bestYet) {
                                    bestYet = solutionCost(problem.toString());
                                    bestSolution = problem.toString();
                                }
                            }
                        }
                        catch (Exception ex) {
                            getConsole().post(Notification.error("PDDL Solver", ex.getMessage()));
                            NeptusLog.pub().error(ex);
                        }
                        pm.setProgress(5 + numTries);
                    }
                    pm.close();
                }
                else {
                    Thread progress = new Thread("Progress updater") {
                        public void run() {
                            pm.setMaximum(searchSeconds * 10);
                            pm.setProgress(0);

                            for (int i = 0; i < searchSeconds * 10; i++) {
                                pm.setNote(String.format("Time left : %.1f seconds", (searchSeconds - i / 10.0)));
                                pm.setProgress(i);
                                try {
                                    Thread.sleep(100);
                                }
                                catch (InterruptedException e) {
                                    break;
                                }
                            }
                            pm.setProgress(pm.getMaximum());
                            pm.close();
                        };
                    };

                    try {
                        progress.setDaemon(true);
                        progress.start();
                        if (problem.solve(searchSeconds))
                            bestSolution = problem.toString();
                        progress.interrupt();
                        pm.setProgress(pm.getMaximum());
                        pm.close();
                    }
                    catch (Exception ex) {
                        bestSolution = null;
                        progress.interrupt();
                        pm.setProgress(pm.getMaximum());
                        pm.close();
                        ex.printStackTrace();
                        NeptusLog.pub().error(ex);
                        GuiUtils.errorMessage(getConsole(), new Exception("No solution has been found.", ex));
                        return;
                    }
                }

                generatedPlans.clear();
                if (bestSolution != null) {
                    try {
                        MVSolution solution = problem.getSolution();

                        if (solution != null) {
                            solution.setGeneratePopups(generatePopups);
                            solution.setScheduledGotosUsed(useScheduledGotos);

                            ArrayList<Pair<ArrayList<String>, ConsoleEventPlanAllocation>> allocations = solution
                                    .allocations();

                            for (Pair<ArrayList<String>, ConsoleEventPlanAllocation> entry : allocations) {
                                ArrayList<String> associatedActions = entry.first();
                                ConsoleEventPlanAllocation allocation = entry.second();
                                allocation.getPlan().setMissionType(getConsole().getMission());
                                getConsole().getMission().getIndividualPlansList().put(allocation.getPlan().getId(),
                                        allocation.getPlan());
                                generatedPlans.put(allocation.getVehicle(), allocation.getPlan());

                                for (String action : associatedActions) {
                                    for (MVPlannerTask task : tasks) {
                                        if (task.name.equals(action)) {
                                            task.setAssociatedAllocation(allocation.getId());
                                        }
                                    }
                                }

                                getConsole().post(allocation);
                            }
                            getConsole().warnMissionListeners();
                            getConsole().getMission().save(true);
                        }
                        GuiUtils.htmlMessage(getConsole(), "Multi-Vehicle Planner", "Valid solution found",
                                "<html><pre>" + bestSolution + "</pre></html>");
                        System.out.println(bestSolution);
                    }
                    catch (Exception e) {
                        GuiUtils.errorMessage(getConsole(), new Exception("Error parsing PDDL.", e));
                        return;
                    }
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
        String line = parts[parts.length - 1];
        return Double.parseDouble(line.substring(0, line.indexOf(':') - 1));
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        MVPlannerTask selected = null;
        LocationType lt = source.getRealWorldLocation(event.getPoint());
        for (MVPlannerTask t : tasks) {
            if (t.getAssociatedAllocation() != null)
                continue;
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
    public ArrayList<MVPlannerTask> getTasks() {
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
