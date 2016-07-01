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

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.mvplanning.allocation.RoundRobinAllocator;
import pt.lsts.neptus.plugins.mvplanning.exceptions.BadPlanTaskException;
import pt.lsts.neptus.plugins.mvplanning.interfaces.AbstractAllocator;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;
import pt.lsts.neptus.plugins.mvplanning.monitors.VehicleAwareness;
import pt.lsts.neptus.plugins.mvplanning.interfaces.PlanTask;
import pt.lsts.neptus.plugins.mvplanning.planning.tasks.ToSafety;
import pt.lsts.neptus.plugins.mvplanning.planning.tasks.VisitPoint;
import pt.lsts.neptus.types.coord.LocationType;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for plan allocation and allocation strategy.
 * Holds an object, the allocator, that should extend the
 * {@link AbstractAllocator} class and implement an allocation
 * strategy.
 * */
public class PlanAllocator {
    private ConsoleAdapter console;
    private AbstractAllocator allocator = null;
    private VehicleAwareness vawareness;
    private PlanGenerator pgen;

    public enum AllocationStrategy {
        ROUND_ROBIN
    }

    public PlanAllocator(VehicleAwareness vawareness, ConsoleAdapter console, PlanGenerator pgen) {
        this.pgen = pgen;
        this.vawareness = vawareness;
        this.console = console;
    }

    public PlanAllocator(AllocationStrategy allocStrat, VehicleAwareness vawareness) {
        this.vawareness = vawareness;
        setAllocationStrategy(allocStrat);
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

        } catch (BadPlanTaskException e) {
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

    public void replan(String vehicle) {
        /* if the vehicle is currently doing any task */
        if(allocator != null && !vawareness.isVehicleAvailable(vehicle)) {
            LocationType safeLoc = vawareness.getVehicleStartLocation(vehicle);

            if(safeLoc == null) {
                NeptusLog.pub().info("Something went wrong...");
                return;
            }

            allocate(new ToSafety(safeLoc, vehicle));
        }
    }
}