/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsm
 * 31 Jan 2017
 */
package pt.lsts.neptus.plugins.mvplanner;


import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanControlState.LAST_OUTCOME;
import pt.lsts.imc.PlanControlState.STATE;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager.SendResult;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.mvplanner.api.ConsoleEventPlanAllocation;
import pt.lsts.neptus.plugins.mvplanner.tasks.NeptusTask;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.mission.plan.PlanType;

@PluginDescription(author = "José Pinto, Tiago Marques", name = "MvPlannerAllocator",
        icon = "pt/lsts/neptus/plugins/map/map-edit.png",
        description="This plug-in does allocations of the tasks planned by LPG",
        version = "0.1", category = PluginDescription.CATEGORY.INTERFACE)
public class MvPlannerTaskAllocator extends ConsolePanel implements Renderer2DPainter {
    private static final long serialVersionUID = -7052505428515767469L;

    /** Maintain to-be-allocated tasks sorted by start time **/
    private final PriorityQueue<PlanTask> taskHeap = new PriorityQueue<>(Comparator.comparing(PlanTask::getStartTime));

    /** Mapping of all tasks **/
    private final ConcurrentHashMap<String, PlanTask> tasks = new ConcurrentHashMap<>();

    private ConsoleLayout console;

    public MvPlannerTaskAllocator(ConsoleLayout console) {
        super(console);
        this.console = console;
    }


    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        for(PlanTask task : tasks.values())
            task.paintTask(g, renderer);
    }

    @Subscribe
    public void consume(ConsoleEventPlanAllocation event) {
        if(event == null) {
            NeptusLog.pub().error("Null ConsoleEventPlanAllocation");
            return;
        }

        PlanTask task;
        switch (event.getOp()) {
            case ALLOCATED:
                task = new NeptusTask(event.getPlan());
                task.associateVehicle(event.getVehicle());
                task.setStartDate(event.getStartTime());
                taskHeap.add(task);
                tasks.put(task.getId(), task);

                NeptusLog.pub().info("Allocation request for task " + task.getId() + " at " + task.getStartTime().toString());
                break;
            case CANCELLED:
                tasks.remove(event.getId());
                ArrayList<PlanTask> existing = new ArrayList<>();
                synchronized (taskHeap) {
                    while(!taskHeap.isEmpty()) {
                        PlanTask t = taskHeap.poll();
                        if (!t.plan.getId().equals(event.getId()))
                            existing.add(t);
                    }
                    taskHeap.addAll(existing);
                }
                
                NeptusLog.pub().info("Allocation cancelled for task " + event.getId());
                break;
            case FINISHED:
                task = tasks.get(event.getPlan().getId());

                if(task == null)
                    break;

                task.setCompleted();
                NeptusLog.pub().info("Task " + task.getId() + " has been completed");
                break;

            case INTERRUPTED:
                task = tasks.get(event.getPlan().getId());

                if(task == null)
                    break;

                task.setInterrupted();
                NeptusLog.pub().warn("Task " + task.getId() + " has been interrupted");
                break;
            default:
                NeptusLog.pub().error("Unknown ConsoleEventPlanAllocation operation");
        }
    }

    @Subscribe
    public void consume(PlanControlState state) {
        if(state == null)
            return;

        PlanTask task = tasks.get(state.getPlanId());
        if (task == null)
            return;
        
        boolean success = false;
        boolean failure = false;
        if (state.getState() == STATE.READY || state.getState() == STATE.BLOCKED) {
            success = state.getLastOutcome() == LAST_OUTCOME.SUCCESS;
            failure = !success;
        }
        else if (state.getState() == STATE.EXECUTING) {
            if (state.getPlanProgress() == 100)
                success = true;
        }
        
        if (success) {
            console.post(new ConsoleEventPlanAllocation(task.asPlanType(), task.getStartTime(),
                    ConsoleEventPlanAllocation.Operation.FINISHED));
            tasks.remove(state.getPlanId());
        }
        if (failure) {
            console.post(new ConsoleEventPlanAllocation(task.asPlanType(), task.getStartTime(),
                    ConsoleEventPlanAllocation.Operation.INTERRUPTED));            
            tasks.remove(state.getPlanId());
        }
        
    }

    @Periodic(millisBetweenUpdates = 5000)
    private void doAllocation() {
        int ntries = 5;
        Date currDate = new Date();
        synchronized (taskHeap) {
            while(!taskHeap.isEmpty() && currDate.compareTo(taskHeap.peek().getStartTime()) >= 0 && ntries >= 0)
                if (allocateTask(taskHeap.peek())) {
                    taskHeap.poll();
                    ntries = 5;
                }
                else
                    ntries--;
        }       
    }

    /**
     * Send plan to a vehicle
     * */
    private boolean allocateTask(PlanTask task) {
        int reqId = IMCSendMessageUtils.getNextRequestId();
        PlanControl pc = new PlanControl();
        pc.setType(PlanControl.TYPE.REQUEST);
        pc.setRequestId(reqId);
        pc.setPlanId(task.getId());
        pc.setOp(PlanControl.OP.START);

        PlanSpecification plan = task.asIMCPlan();
        pc.setArg(plan);

        String vehicle = task.getAllocatedVehicle();
        Future<SendResult> cmdRes = console.getImcMsgManager().sendMessageReliably(pc, vehicle);

        try {
            if(cmdRes.get() == SendResult.SUCCESS || cmdRes.get() == SendResult.UNCERTAIN_DELIVERY) {
                NeptusLog.pub().info("Allocated task " + task.getId() + " with start time " + task.getStartTime().toString()
                        + " to " + vehicle);

                return true;
            }
            else {
                NeptusLog.pub().info("Failed allocation of task " + task.getId() + " with start time " + task.getStartTime().toString()
                        + " to " + vehicle);

                return false;
            }

        }
        catch (InterruptedException | ExecutionException e) {
            NeptusLog.pub().error("Failed allocation of task " + task.getId() + " with start time " + task.getStartTime().toString()
                    + " to " + vehicle + " due to exception");
            NeptusLog.pub().debug(e.getMessage());

            return false;
        }
    }

    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {

    }

    public void runTests() {
        Calendar tmp = Calendar.getInstance();

        tmp.set(Calendar.MINUTE, tmp.get(Calendar.MINUTE) + 2);
        Date d1 = tmp.getTime();
        PlanType p1 = new PlanType(console.getMission());
        p1.setId("--test-- p1");

        tmp = Calendar.getInstance();
        tmp.set(Calendar.MINUTE, tmp.get(Calendar.MINUTE) + 4);
        Date d2 = tmp.getTime();
        PlanType p2 = new PlanType(console.getMission());
        p2.setId("--test-- p2");

        tmp = Calendar.getInstance();
        tmp.set(Calendar.MINUTE, tmp.get(Calendar.MINUTE) + 1);
        Date d3 = tmp.getTime();
        PlanType p3 = new PlanType(console.getMission());
        p3.setId("--test-- p3");

        NeptusLog.pub().info("Plan 1 " + d1.toString());
        NeptusLog.pub().info("Plan 2 " + d2.toString());
        NeptusLog.pub().info("Plan 3 " + d3.toString());
        System.out.println("\n");

        console.post(new ConsoleEventPlanAllocation(p1, d1, ConsoleEventPlanAllocation.Operation.ALLOCATED));
        console.post(new ConsoleEventPlanAllocation(p2, d2, ConsoleEventPlanAllocation.Operation.ALLOCATED));
        console.post(new ConsoleEventPlanAllocation(p3, d3, ConsoleEventPlanAllocation.Operation.ALLOCATED));
    }
}
