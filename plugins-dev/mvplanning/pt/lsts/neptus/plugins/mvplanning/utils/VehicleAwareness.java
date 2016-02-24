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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.lsts.neptus.plugins.mvplanning.MVPlanning;
import pt.lsts.neptus.plugins.mvplanning.utils.jaxb.Profile;
import pt.lsts.neptus.plugins.mvplanning.utils.jaxb.ProfileMarshaler;

import com.google.common.eventbus.Subscribe;

/*
 * FIXME: Only detects vehicles when they change state.
 *        Should fetch list of available vehicles at startup
 */
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

        if(newState == STATE.FINISHED)
            setVehicleAvailable(id);
        else if(newState == STATE.SERVICE) {
            ImcSystem vehicle = ImcSystemsHolder.getSystemWithName(id);

            if(vehicle.isInOperationMedium())
                setVehicleAvailable(id);
            else /* In SERVICE mode, but on the ground */
                setVehicleUnavailable(id);
        }
        else
            setVehicleUnavailable(id);
    }

    public boolean isVehicleAvailable(String vehicle) {
        return availableVehicles.containsKey(vehicle);
    }
    
    private void setVehicleAvailable(String id) {
        /* if vehicle is not already set as available */
        if(!availableVehicles.containsKey(id)) {
            VehicleInfo vehicle;
            if(unavailableVehicles.containsKey(id))
                vehicle = unavailableVehicles.remove(id);
            else
                vehicle = new VehicleInfo(id, MVPlanning.availableProfiles); /* first time in service mode */
            availableVehicles.put(id, vehicle);

            /* logging */
            logDebugInfo("Vehicle " + id + " set as AVAILABLE");
            vehicle.printProfiles();
        }
    }
    
    private void setVehicleUnavailable(String id) {
        /* if vehicle is not already set as unavailable */
        if(!unavailableVehicles.containsKey(id)) {
            VehicleInfo vehicle;
            if(availableVehicles.containsKey(id))
                vehicle = availableVehicles.remove(id);
            else
                vehicle = new VehicleInfo(id, MVPlanning.availableProfiles); /* first time in service mode */
            unavailableVehicles.put(id, vehicle);
            logDebugInfo("Vehicle " + id + " set as UNAVAILABLE");
        }
    }

    private void logDebugInfo(String msg) {
        System.out.println("[mvplanning/VehicleAwareness] " + msg);
    }

    public VehicleInfo getVehicleInfo(String vid) {
        return availableVehicles.get(vid);
    }

    public void printVehicleProfiles(String vid) {
        getVehicleInfo(vid).printProfiles();
    }
}
