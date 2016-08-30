/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsmarques
 * 15 Dec 2015
 */

package pt.lsts.neptus.plugins.mvplanning;

import com.google.common.eventbus.Subscribe;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControlState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.plugins.mvplanning.allocation.RoundRobinAllocator;
import pt.lsts.neptus.plugins.mvplanning.events.MvPlanningEventPlanAllocated;
import pt.lsts.neptus.plugins.mvplanning.exceptions.BadPlanTaskException;
import pt.lsts.neptus.plugins.mvplanning.exceptions.SafePathNotFoundException;
import pt.lsts.neptus.plugins.mvplanning.interfaces.AbstractAllocator;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;
import pt.lsts.neptus.plugins.mvplanning.interfaces.PlanTask;
import pt.lsts.neptus.plugins.mvplanning.jaxb.PlanTaskMarshaler;
import pt.lsts.neptus.plugins.mvplanning.monitors.StateMonitor;
import pt.lsts.neptus.plugins.mvplanning.monitors.VehicleAwareness;
import pt.lsts.neptus.plugins.mvplanning.planning.tasks.ToSafety;
import pt.lsts.neptus.types.coord.LocationType;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Responsible for plan allocation and allocation strategy.
 * Holds an object, the allocator, that should extend the
 * {@link AbstractAllocator} class and implement an allocation
 * strategy.
 * It's also responsible for saving and loading any unfinished tasks
 * from a previous session
 * */
public class PlanAllocator {
    private ConsoleAdapter console;
    private AbstractAllocator allocator = null;
    private VehicleAwareness vawareness;
    private PlanGenerator pgen;

    private final PlanTaskMarshaler pTaskMarsh = new PlanTaskMarshaler();
    private ConcurrentMap<String, Double> plansCompletion = null;
    private ConcurrentMap<String, PlanTask> plans = null;

    public enum AllocationStrategy {
        ROUND_ROBIN
    }

    public PlanAllocator(VehicleAwareness vawareness, ConsoleAdapter console, PlanGenerator pgen) {
        this.pgen = pgen;
        this.vawareness = vawareness;
        this.console = console;
        plansCompletion = new ConcurrentHashMap<>();
        plans = new ConcurrentHashMap<>();

    }

    public PlanAllocator(AllocationStrategy allocStrat, VehicleAwareness vawareness) {
        this.vawareness = vawareness;
        setAllocationStrategy(allocStrat);
        plansCompletion = new ConcurrentHashMap<>();
        plans = new ConcurrentHashMap<>();
    }

    /**
     * Given a task, asks the plan generator to
     * generate the corresponding plan(s) and
     * sets them for allocation
     * */
    public List<PlanTask> allocate(PlanTask task) {
        /* default allocation strategy is round-robin */
        if(allocator == null)
            setAllocationStrategy(AllocationStrategy.ROUND_ROBIN);

        try {
            List<PlanTask> tasks = pgen.generatePlan(task);

            if(task.getTaskType() == PlanTask.TASK_TYPE.SAFETY)
                allocator.allocateSafetyTask((ToSafety) tasks.get(0));
            else
                for (PlanTask ptask : tasks)
                    allocator.addNewPlan(ptask);

            return tasks;

        } catch (BadPlanTaskException | SafePathNotFoundException e) {
            e.printStackTrace();
            NeptusLog.pub().warn("No plan has been allocated");

            return new ArrayList<>(0);
        }
    }

    public void setAllocationStrategy(AllocationStrategy allocStrat) {
        /* for now just round-robin */
        NeptusLog.pub().info("Using Round-Robin allocation strategy");
        allocator = new RoundRobinAllocator(true, false, vawareness, console, pgen);
    }

    /**
     * If the given vehicle is currently executing a
     * task this method puts it "on hold" by allocating
     * a plan that moves it to its safe position.
     * <p>
     * This is called, for instance, when an external
     * system gets too close to the given vehicle
     * */
    public void replan(String vehicle) {
        /* if the vehicle is currently doing any task */
        if(allocator != null && !vawareness.isVehicleAvailable(vehicle)) {
            LocationType currLoc = ImcSystemsHolder.lookupSystemByName(vehicle).getLocation();
            LocationType safeLoc = vawareness.getVehicleStartLocation(vehicle);

            if(currLoc == null || safeLoc == null) {
                NeptusLog.pub().info("[" + vehicle + "]" + "Can't replan because some location is null!");
                return;
            }
            allocate(new ToSafety(currLoc, safeLoc, vehicle));
        }
    }

    @Subscribe
    public void on(MvPlanningEventPlanAllocated event) {
        if(StateMonitor.isPluginClosing())
            return;

        plansCompletion.putIfAbsent(event.getPlanId(), 100.0);
        plans.putIfAbsent(event.getPlanId(), event.getPlan());
    }

    @Subscribe
    public void on(PlanControlState msg) {
        if(StateMonitor.isPluginClosing())
            return;

        String id = msg.getPlanId();
        /* put() and containsKeys() are not thread-safe */
        synchronized(plansCompletion) {
            if(plans.containsKey(id)) {
                double progress = msg.getPlanProgress();
                if(progress < 0)
                    return;

                if(progress == 0) {
                    plansCompletion.put(id, progress);
                    plans.get(id).updatePlanCompletion(progress);
                }
                // if the task was started
                else {
                    PlanControlState.LAST_OUTCOME last = msg.getLastOutcome();
                    PlanControlState.STATE current = msg.getState();

                    // task interrupted
                    if (current != PlanControlState.STATE.EXECUTING && last == PlanControlState.LAST_OUTCOME.FAILURE) {
                        plansCompletion.put(id, 0.0); // task has to be restarted
                        plans.get(id).updatePlanCompletion(0.0);

                        // add the plan for allocation, again
                        allocator.addNewPlan(plans.get(id));
                        console.notifiyError("Task " + id + " has been interrupted, trying to re-allocate","");
                    }
                    // everything is ok; task has finished
                    else if(Math.round(progress) == 100) {
                        plansCompletion.remove(id);
                        plans.remove(id);

                        console.notifiySuccess("Task " + id + "has been completed with success", "");
                    }
                    // everything is ok; update its completion
                    else {
                        plansCompletion.put(id, progress);
                        plans.get(id).updatePlanCompletion(progress);
                    }
                }
            }
        }
    }

    public void cleanup() {
        List<PlanTask> plansList = new ArrayList<PlanTask>(plans.values());
        try {
            pTaskMarsh.marshalAll(plansList);
        }
        catch (JAXBException e) {
            NeptusLog.pub().warn("Couldn't save unfinished plans...");
            e.printStackTrace();
        }
    }

    public List<PlanTask> loadPlans() throws JAXBException {
        return pTaskMarsh.unmarshalAll(console.getMission());
    }
}