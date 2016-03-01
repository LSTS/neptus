/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * 26 Feb 2016
 */
package pt.lsts.neptus.plugins.mvplanning.allocation;

import java.util.ArrayList;
import java.util.List;

import pt.lsts.neptus.plugins.mvplanning.PlanTask;
import pt.lsts.neptus.plugins.mvplanning.interfaces.AbstractAllocator;
import pt.lsts.neptus.plugins.mvplanning.utils.VehicleAwareness;

/**
 * @author tsmarques
 *
 */
public class RoundRobinAllocator extends AbstractAllocator {
    private List<PlanTask> plans;
    private List<String> vehicles;
    private VehicleAwareness vawareness;

    public RoundRobinAllocator(boolean isPeriodic, boolean listenToEvents, VehicleAwareness vawareness) {
        super(isPeriodic, listenToEvents);
        setVehicleAwareness(vawareness);
        plans = new ArrayList<>();
        vehicles = new ArrayList<>();
    }
        
    private void updateVehiclesList(List<String> profileVehicles) {
        synchronized(vehicles) {
            for(String vehicle : profileVehicles)
                if(!vehicles.contains(vehicle))
                    vehicles.add(vehicle);
        }
    }
    
    @Override
    public void addNewPlan(PlanTask ptask) {
        synchronized(plans) {
            System.out.println("[mvplanning/RoundRobinAllocator] Received a new plan.");
            plans.add(ptask);
            updateVehiclesList(ptask.getProfile().getProfileVehicles());
            doAllocation();
        }
    }
    

    @Override
    public void doAllocation() {
        synchronized(plans) {
            if(plans.isEmpty()) {
                System.out.println("[mvplanning/RoundRobinAllocator] No plans to allocate.");
                return;
            }

            int i = 0;
            boolean allocated = false;
            PlanTask ptask = plans.get(0);
            /* iterate over profile's vehicles and find the first one available */
            while(!allocated && (i < vehicles.size())) {
                /* fetch, supposedly, available vehicle of the profile */
                String vehicle = vehicles.get(i);
                if(vawareness.isVehicleAvailable(vehicle) && ptask.containsVehicle(vehicle)) {
                    allocated = allocateTo(vehicle, ptask);

                    if(allocated) {
                        System.out.println("[mvplanning/RoundRobinAllocator] Allocating " + ptask.getPlanId() + " to " + vehicle);
                        /* plan has been allocated */
                        plans.remove(0);

                        /* move vehicle to the end of the queue */
                        vehicles.remove(i);
                        vehicles.add(vehicle);
                    }
                    else
                        System.out.println("[mvplanning/RoundRobinAllocator] Vehicle " + vehicle + " no available or not in profile " + ptask.getProfile().getId());
                }
                i++;
            }
        }
    }
    
    @Override
    public boolean allocateTo(String vehicle, PlanTask ptask) {
        /* TODO: Implement. Use sendMessage() from ConsoleAdapter */
        System.out.println("[mvplanning/RoundRobinAllocator] Sending message");
        return false;
    }
    
    @Override
    public void setVehicleAwareness(VehicleAwareness vawareness) {
        this.vawareness = (VehicleAwareness) vawareness;        
    }
}
