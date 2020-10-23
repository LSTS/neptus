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
 * Nov 25, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.ProgressMonitor;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventFutureState;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.mvplanner.api.ConsoleEventPlanAllocation;
import pt.lsts.neptus.plugins.mvplanner.api.ConsoleEventPlanAllocation.Operation;
import pt.lsts.neptus.plugins.update.Periodic;
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
@PluginDescription(name = "Multi-Vehicle Planner Interaction", icon = "pt/lsts/neptus/plugins/pddl/wizard.png",
description="This plug-in uses LPG for multi-vehicle task allocation")
public class MVPlannerInteraction extends ConsoleInteraction {

    private static final File TASKS_FILE = new File("conf/mvplanner.tasks");
    private ArrayList<MVPlannerTask> tasks = new ArrayList<MVPlannerTask>();
    private MVPlannerTask selectedTask = null;
    private MVProblemSpecification problem = null;
    private boolean allocationInProgress = false;
    private LinkedHashMap<String, ConsoleEventFutureState> futureStates = new LinkedHashMap<String, ConsoleEventFutureState>();
    private long lastAllocation = System.currentTimeMillis();

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

    @NeptusProperty(category = "Plan Generation", name = "Maximum length permitted for a survey task.")
    private double surveyMaxLength = 600;

    @NeptusProperty(category = "Plan Execution", name = "Automatic execution.")
    private boolean autoExec = false;

    @NeptusProperty(category = "Plan Execution", name = "Seconds between automatic allocations.")
    private int secsBetweenAllocations = 30;

    @Subscribe
    public void on(ConsoleEventFutureState future) {
        synchronized (futureStates) {
            if (future.getState() == null)
                futureStates.remove(future.getVehicle());
            else {
                futureStates.put(future.getVehicle(), future);
            }
        }
    }

    @Subscribe
    public void on(ConsoleEventPlanAllocation allocation) {
        try {
            switch (allocation.getOp()) {
                case FINISHED:
                    synchronized (tasks) {
                        Iterator<MVPlannerTask> it = tasks.iterator();
                        while (it.hasNext()) {
                            MVPlannerTask t = it.next();
                            if (t.getAssociatedAllocation() != null && t.getAssociatedAllocation().equals(allocation.getId()))
                                it.remove();
                        }
                    }
                    break;
                case INTERRUPTED:
                    synchronized (tasks) {
                        HashSet<String> allocationsToCancel = new HashSet<>();
                        Iterator<MVPlannerTask> it = tasks.iterator();
                        while (it.hasNext()) {
                            MVPlannerTask t = it.next();
                            if (t.getAssociatedAllocation() != null && t.getAssociatedVehicle().equals(allocation.getVehicle())) {
                                if (!t.getAssociatedAllocation().equals(allocation.getId()))
                                    allocationsToCancel.add(t.getAssociatedAllocation());
                                t.setAllocation(null);
                            }
                        }

                        for (String cancelledAlloc : allocationsToCancel) {
                            PlanType plan = new PlanType(getConsole().getMission());
                            plan.setId(cancelledAlloc);
                            ConsoleEventPlanAllocation cancel = new ConsoleEventPlanAllocation(plan, new Date(), Operation.CANCELLED);
                            getConsole().post(cancel);
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

    private Thread planner = null;

    @Periodic(millisBetweenUpdates=1000) 
    private void doAllocation() {
        if ((System.currentTimeMillis() - lastAllocation) < secsBetweenAllocations * 1000)
            return;

        lastAllocation = System.currentTimeMillis();
        boolean needsToPlan = false;

        // Check if there are any tasks left to allocate
        for (MVPlannerTask t : tasks) {
            if (t.getAssociatedAllocation() == null) {
                needsToPlan = true;
                break;
            }
        }
        
        if (!needsToPlan) {
            NeptusLog.pub().info("Not planning because no tasks are left for allocation.");
            return;
        }
        
        // Check if there are any vehicles available        
        ArrayList<String> available = new ArrayList<>();
        for (ImcSystem s : ImcSystemsHolder.lookupActiveSystemVehicles()) {
            if (s.getTypeVehicle() == VehicleTypeEnum.UUV)
                available.add(s.getName());
        }

        // For the available vehicles check which are free for being allocated
        synchronized (futureStates) {
            for (ConsoleEventFutureState future : futureStates.values()) {
                // this vehicle won't be ready in time...
                if ((future.getDate().getTime() - System.currentTimeMillis()) > secsBetweenAllocations * 1000) {
                    available.remove(future.getVehicle());
                }
            }
        }
        
        if (available.isEmpty()) {
            needsToPlan = false;
            NeptusLog.pub().info("Not planning because no vehicles would be available for this allocation.");            
        }

        if (!autoExec || !needsToPlan)
            return;

        if (planner != null) {
            planner.interrupt();
        }

        planner = new Thread("Planning thread") {
            public void run() {
                NeptusLog.pub().info("Generating plan...");
                String solution = createPlan(null);
                if (solution != null) {
                    NeptusLog.pub().info("Solution: "+solution);
                    allocatePlan(solution);
                }
                else {
                    NeptusLog.pub().warn("Solution is null");
                }
            };
        };

        planner.setDaemon(true);
        planner.start();
    }

    @NeptusMenuItem("File>Multi-Vehicle Planner>Load Tasks")
    public void loadTasks() {
        JFileChooser chooser = GuiUtils.getFileChooser(".", "Multi-Vehicle Planner Tasks (.tasks)", "tasks");
        if (chooser.showOpenDialog(getConsole()) == JFileChooser.APPROVE_OPTION) {
            File tasksFile = chooser.getSelectedFile();
            try {
                ArrayList<MVPlannerTask> readTasks = MVPlannerTask.loadFile(tasksFile);
                clear(null);
                tasks = readTasks;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @NeptusMenuItem("File>Multi-Vehicle Planner>Save Tasks")
    public void saveTasks() {
        JFileChooser chooser = GuiUtils.getFileChooser(".", "Multi-Vehicle Planner Tasks (.tasks)", "tasks");
        if (chooser.showSaveDialog(getConsole()) == JFileChooser.APPROVE_OPTION) {
            File tasksFile = chooser.getSelectedFile();
            try {
                MVPlannerTask.saveFile(tasksFile, tasks);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {

        g.setTransform(new AffineTransform());
        
        if (allocationInProgress) {
            
            long t = (System.currentTimeMillis() / 1000) % 4;
            
            String str = "planning";
            
            for (int i = 0; i < t; i++)
                str += ".";
            g.setColor(new Color(0,0,0,200));
            g.drawString(str, 10.5f, 40.5f);
            g.setColor(Color.red);
            g.drawString(str, 10, 40);
        }
        else if (autoExec) {
            
            long ellapsed = (System.currentTimeMillis() - lastAllocation);
            long missing = (secsBetweenAllocations * 1000)  - ellapsed;
            
            String str = "planning in "+(missing/1000)+"...";
            g.setColor(new Color(0,0,0,200));
            g.drawString(str, 10.5f, 40.5f);
            g.setColor(Color.green.darker());
            g.drawString(str, 10, 40);
            
        }
        
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
                    saveState();
                }
            });

            popup.add("Set payloads for " + clickedTask.getName()).addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    PropertiesEditor.editProperties(clickedTask, true);
                    saveState();
                }
            });

            popup.addSeparator();

        }
        
        popup.add("<html>Add <b>Survey</b> task").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SurveyPolygonTask task = null;
                while (true) {
                    task = new SurveyPolygonTask(source.getRealWorldLocation(event.getPoint()));
                    boolean found = false;
                    for (MVPlannerTask t : tasks) {
                        if (t.getName().equals(task.getName())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        break;
                }
                if (!PropertiesEditor.editProperties(task, true)) {
                    tasks.add(task);
                    saveState();
                }
                source.repaint();
            }
        });

        popup.add("<html>Add <b>Sample</b> task").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SamplePointTask task = null;
                while (true) {
                    task = new SamplePointTask(source.getRealWorldLocation(event.getPoint()));
                    boolean found = false;
                    for (MVPlannerTask t : tasks) {
                        if (t.getName().equals(task.getName())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        break;
                }

                if (!PropertiesEditor.editProperties(task, true)) {
                    tasks.add(task);
                    saveState();
                }
                source.repaint();
            }
        });

        popup.add("Clear Tasks").addActionListener(this::clear);
        popup.add("Cancel Allocations").addActionListener(this::cancelAllocations);

        popup.addSeparator();

        if (!this.autoExec) {
            popup.add("Create plans").addActionListener(this::generate);
            popup.addSeparator();
        }

        popup.add("Settings").addActionListener(this::settings);

        popup.show(source, event.getX(), event.getY());
    }

    private void clear(ActionEvent action) {
        HashSet<String> allocationsToCancel = new HashSet<>();
        Iterator<MVPlannerTask> it = tasks.iterator();
        while (it.hasNext()) {
            MVPlannerTask t = it.next();
            if (t.getAssociatedAllocation() != null)
                allocationsToCancel.add(t.getAssociatedAllocation());
            it.remove();
        }

        for (String cancelledAlloc : allocationsToCancel) {
            PlanType plan = new PlanType(getConsole().getMission());
            plan.setId(cancelledAlloc);
            ConsoleEventPlanAllocation cancel = new ConsoleEventPlanAllocation(plan, new Date(), Operation.CANCELLED);
            getConsole().post(cancel);
        }

        saveState();
    }
    
    private void cancelAllocations(ActionEvent action) {
        Iterator<MVPlannerTask> it = tasks.iterator();
        HashSet<String> allocationsToCancel = new HashSet<>();
        while (it.hasNext()) {
            MVPlannerTask t = it.next();
            if (t.getAssociatedAllocation() != null)
                allocationsToCancel.add(t.getAssociatedAllocation());
            t.setAllocation(null);
        }
        for (String cancelledAlloc : allocationsToCancel) {
            PlanType plan = new PlanType(getConsole().getMission());
            plan.setId(cancelledAlloc);
            ConsoleEventPlanAllocation cancel = new ConsoleEventPlanAllocation(plan, new Date(), Operation.CANCELLED);
            getConsole().post(cancel);
        }
    }

    private void generate(ActionEvent action) {
        Thread t = new Thread("Generating Multi-Vehicle plan...") {
            public void run() {
                
                ProgressMonitor pm = new ProgressMonitor(getConsole(), "Searching for solutions...",
                        "Generating initial state", 0, 5 + numTries);
                String bestSolution = createPlan(pm);

                if (bestSolution != null) {
                    allocatePlan(bestSolution);
                }
                else
                    GuiUtils.errorMessage(getConsole(), new Exception("No solution has been found."));
            }
        };
        t.setDaemon(true);
        t.start();
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
            selected.mousePressed(event, source);
            selectedTask = selected;
        }
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        if (selectedTask != null) {
            saveState();
            selectedTask.mouseReleased(event, source);
        }
        
        selectedTask = null;
        super.mouseReleased(event, source);
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        
        synchronized (tasks) {
            for (MVPlannerTask t : tasks) {
                if (t.containsPoint(source.getRealWorldLocation(event.getPoint()), source)) {
                    t.mouseMoved(event, source);
                    break;
                }
            }
        }
    }
    
    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        
        if (selectedTask == null) {
            super.mouseDragged(event, source);
            return;
        }
        
        selectedTask.mouseDragged(event, source);
    }

    @Override
    public void initInteraction() {
        // force automatic execution to be false at start.
        autoExec = false;
    }

    private void saveState() {
        try {
            MVPlannerTask.saveFile(TASKS_FILE, tasks);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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

    /**
     * Search for surveys than can be splitted
     * and split them
     * */
    private void searchAndSplitSurveys() {
        List<MVPlannerTask> toBeSplit = new ArrayList<>();
        
        synchronized (tasks) {
            // fetch surveys eligible to be split
            for(MVPlannerTask task : tasks) {
                if (task.getLength() > surveyMaxLength)
                    toBeSplit.add(task);
            }

            for(MVPlannerTask task : toBeSplit) {
                tasks.addAll(task.splitTask(surveyMaxLength));
                tasks.remove(task);
            }                        
        }
    }

    private String createPlan(ProgressMonitor pm) {
        searchAndSplitSurveys();
        
        
        System.out.println(tasks);
        allocationInProgress = true;
        
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
        if (pm != null) {
            pm.setProgress(5);
            pm.setMillisToPopup(0);
        }

        double bestYet = 0;
        String bestSolution = null;

        if (searchSeconds == 0) {
            for (int i = 0; i < numTries; i++) {
                String best = "N/A";
                if (bestSolution != null)
                    best = DateTimeUtil.milliSecondsToFormatedString((long) (bestYet * 1000));

                if (pm != null && pm.isCanceled()) {
                    allocationInProgress = false;
                    return null;
                }
                if (pm != null) {
                    pm.setNote("Current best solution time: " + best);
                    pm.setProgress(5 + i);
                }


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
                if (pm != null)
                    pm.setProgress(5 + numTries);
            }
            if (pm!= null)
                pm.close();
        }
        else {
            Thread progress = null;
            if (pm != null) {
                progress = new Thread("Progress updater") {
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
                progress.setDaemon(true);
                progress.start();
            }
            try {
                if (problem.solve(searchSeconds))
                    bestSolution = problem.toString();
                if (pm != null) {
                    progress.interrupt();
                    pm.setProgress(pm.getMaximum());
                    pm.close();                                                
                }
            }
            catch (Exception ex) {
                bestSolution = null;
                progress.interrupt();
                pm.setProgress(pm.getMaximum());
                pm.close();
                ex.printStackTrace();
                NeptusLog.pub().error(ex);
                GuiUtils.errorMessage(getConsole(), new Exception("No solution has been found.", ex));
                allocationInProgress = false;
                return null;
            }
        }
        allocationInProgress = false;
        return bestSolution;
    }

    private void allocatePlan(String pddlPlan) {
        if (pddlPlan != null) {
            
            Date nextAllocation = new Date((lastAllocation + (secsBetweenAllocations) * 1000));    
            
            try {
                MVSolution solution = problem.getSolution();

                if (solution != null) {
                    solution.setGeneratePopups(generatePopups);
                    solution.setScheduledGotosUsed(useScheduledGotos);

                    ArrayList<Pair<ArrayList<String>, ConsoleEventPlanAllocation>> allocations = solution
                            .allocations();

                    // when using automatic execution, only allocate those that should start before next allocation
                    if (autoExec) {
                        Iterator<Pair<ArrayList<String>, ConsoleEventPlanAllocation>> it = allocations.iterator();
                        while (it.hasNext()) {
                            Pair<ArrayList<String>, ConsoleEventPlanAllocation> val = it.next();    
                            System.out.println(val);
                            if (val.second().getStartTime().after(nextAllocation)) {
                                // ignore as it can be computed again before the allocation
                                NeptusLog.pub()
                                .info("Ignoring generated allocation for " + val.second().getVehicle()
                                        + " because it is after upcoming allocation: "
                                        + val.second().getStartTime() + " > " + nextAllocation);
                                it.remove();
                            }
                        }
                    }              

                    for (Pair<ArrayList<String>, ConsoleEventPlanAllocation> entry : allocations) {
                        ArrayList<String> associatedActions = entry.first();
                        ConsoleEventPlanAllocation allocation = entry.second();
                        allocation.getPlan().setMissionType(getConsole().getMission());
                        getConsole().getMission().getIndividualPlansList().put(allocation.getPlan().getId(),
                                allocation.getPlan());

                        for (String action : associatedActions) {
                            for (MVPlannerTask task : tasks) {
                                if (task.getName().equals(action))
                                    task.setAllocation(allocation);                                                                
                            }
                        }

                        getConsole().post(allocation);
                    }
                    getConsole().warnMissionListeners();
                    getConsole().getMission().save(true);
                }
                System.out.println(pddlPlan);
            }
            catch (Exception e) {
                if (!autoExec)
                    GuiUtils.errorMessage(getConsole(), new Exception("Error parsing PDDL.", e));
                e.printStackTrace();
                return;
            }
        }
    }

    public static void main(String[] args) {
        StateRenderer2D renderer = new StateRenderer2D();
        MVPlannerInteraction inter = new MVPlannerInteraction();
        inter.init(ConsoleLayout.forge());
        renderer.setActiveInteraction(inter);
        GuiUtils.testFrame(renderer);
    }
}
