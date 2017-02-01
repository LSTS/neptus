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


import com.google.common.eventbus.Subscribe;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.mvplanner.api.ConsoleEventPlanAllocation;
import pt.lsts.neptus.plugins.mvplanner.tasks.NeptusTask;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager.SendResult;

import java.awt.Graphics2D;
import java.util.Comparator;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@PluginDescription(author = "José Pinto, Tiago Marques", name = "MvPlannerAllocator",
        icon = "pt/lsts/neptus/plugins/map/map-edit.png",
        version = "0.1", category = PluginDescription.CATEGORY.INTERFACE)
public class MvPlannerTaskAllocator extends ConsolePanel implements Renderer2DPainter {
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

                taskHeap.add(task);
                tasks.put(task.getId(), task);
                break;

            case FINISHED:
                task = tasks.get(event.getPlan().getId());

                if(task != null)
                    task.setCompleted();
                break;

            case INTERRUPTED:
                task = tasks.get(event.getPlan().getId());

                if(task != null)
                    task.setInterrupted();
                break;
            default:
                NeptusLog.pub().error("Unknown ConsoleEventPlanAllocation operation");
        }
    }

    @Periodic(millisBetweenUpdates = 5000)
    private void doAllocation() {
        if(taskHeap.isEmpty())
            return;

        int ntries = 5;
        Date currDate = new Date();
        while(currDate.compareTo(taskHeap.peek().getStartTime()) >= 0 && ntries >= 0)
            if(allocateTask(taskHeap.peek())) {
                taskHeap.poll();
                ntries = 5;
            }
            else
                ntries--;
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
}
