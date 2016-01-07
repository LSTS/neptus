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
package pt.lsts.neptus.plugins.mvplanning.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.params.ConfigurationManager;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.params.SystemProperty.Scope;
import pt.lsts.neptus.params.SystemProperty.Visibility;
import pt.lsts.neptus.types.mission.plan.PlanType;

public class VehicleAwareness {
    private ConcurrentHashMap<String, VehicleInfo> availableVehicles;
    private ConcurrentHashMap<String, VehicleInfo> unavailableVehicles;

    public VehicleAwareness() {
        availableVehicles = new ConcurrentHashMap<>();
        unavailableVehicles = new ConcurrentHashMap<>();
    }


    @Subscribe
    public void on(ConsoleEventVehicleStateChanged event) {
        onVehicleStateChanged(event);
    }
    
    private synchronized void onVehicleStateChanged(ConsoleEventVehicleStateChanged event) {
        String id = event.getVehicle();
        ConsoleEventVehicleStateChanged.STATE newState = event.getState();

        if(newState == ConsoleEventVehicleStateChanged.STATE.SERVICE) {
            VehicleInfo vehicle;
            if(unavailableVehicles.containsKey(id))
                vehicle = unavailableVehicles.remove(id);
            else
                vehicle = new VehicleInfo(id); /* first time in service mode */
            availableVehicles.put(id, vehicle);
        }
        else {
            if(availableVehicles.containsKey(id)) {
                VehicleInfo vehicle = availableVehicles.remove(id);
                unavailableVehicles.put(id, vehicle);
            }
        }
    }
    
    public VehicleInfo getVehicleInfo(String vid) {
        return availableVehicles.get(vid);
    }
    
    public void printVehicleCapabilities(String vid) {
        getVehicleInfo(vid).printCapabilities();
    }
    
    
    /* for debugging
     * 0 for unavailabe 1 for available */
    private void printVehicles(int t, ConcurrentHashMap<String, VehicleInfo> vehicles) {
        if(t == 0)
            System.out.println("## Unavailable vehicles ##");
        else
            System.out.println("## Available Vehicles ##");
        
        for(Map.Entry<String, VehicleInfo> entry : vehicles.entrySet()) {
            System.out.println("# Vehicle: " + entry.getKey());
            entry.getValue().printCapabilities();
        }
        System.out.println("\n");
    }


    private class VehicleInfo {
        private String vId;
        private ArrayList<String> capabilities;

        /* Properties variables */
        private final Scope scope = Scope.GLOBAL;
        private final Visibility vis = Visibility.USER;

        public VehicleInfo(String id) {
            vId = id;
            capabilities = getVehicleCapabilities(vId);
        }

        public String vehicleId() {
            return vId;
        }

        public ArrayList<String> getVehicleCapabilities(String vId) {
            ArrayList<SystemProperty> prList = ConfigurationManager.getInstance().getProperties(vId, vis, scope);
            ArrayList<String> capabilities = new ArrayList<String>(prList.size());
            for(SystemProperty pr : prList) {
                String cap = pr.getCategory();
                if(!capabilities.contains(cap))
                    capabilities.add(cap);
            }

            return capabilities;
        }

        public ArrayList<String> vehicleCapabilities() {
            return capabilities;
        }

        public boolean hasCapabilities(LinkedList<String> neededCapabilities) {
            return capabilities.containsAll(neededCapabilities);
        }
        
        /* for debugging */
        public void printCapabilities() {
            for(String cap : capabilities)
                System.out.println("-> " + cap);
        }
    }
}
