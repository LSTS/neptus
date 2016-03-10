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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleSystem;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.lsts.neptus.plugins.mvplanning.events.MvPlanningEventAvailableVehicle;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;

import com.google.common.eventbus.Subscribe;

/**
 * Class responsible for keeping a list of available and
 * unavailable vehicles.
 * It listens to {@link ConsoleEventVehicleStateChanged} events
 * to have a sense of what the vehicles' current state is. 
 **/
public class VehicleAwareness {
    private ConsoleAdapter console;
    private List<String> availableVehicles;
    private List<String> unavailableVehicles;

    public VehicleAwareness(ConsoleAdapter console) {
        this.console = console;
        availableVehicles = new ArrayList<>();
        unavailableVehicles = new ArrayList<>();

        /* check vehicles' state at startup */
        for(Entry<String, ConsoleSystem> entry : console.getSystems().entrySet())
            checkVehicleState(entry.getKey(), entry.getValue().getVehicleState());
    }

    @Subscribe
    public void on(ConsoleEventVehicleStateChanged event) {
        onVehicleStateChanged(event);
    }

    private synchronized void onVehicleStateChanged(ConsoleEventVehicleStateChanged event) {
        String id = event.getVehicle();
        ConsoleEventVehicleStateChanged.STATE newState = event.getState();

        checkVehicleState(id, newState);
    }

    private void checkVehicleState(String vehicle, STATE state) {
        if(state == STATE.FINISHED)
            setVehicleAvailable(vehicle);
        else if(state == STATE.SERVICE) {
//            ImcSystem vehicle = ImcSystemsHolder.getSystemWithName(id);

//            if(vehicle.isInOperationMedium())
            setVehicleAvailable(vehicle);
//            else /* In SERVICE mode, but on the ground */
//                setVehicleUnavailable(id);
        }
        else
            setVehicleUnavailable(vehicle);
    }

    public boolean isVehicleAvailable(String vehicle) {
        if(availableVehicles.contains(vehicle)) {
            ImcSystem sys = ImcSystemsHolder.getSystemWithName(vehicle);

            if(sys.isActive())
                return true;
            else {
                setVehicleUnavailable(vehicle);
                return false;
            }
        }
        else
            return false;
    }
    
    private void setVehicleAvailable(String id) {
        /* if vehicle is not already set as available */
        if(!availableVehicles.contains(id)) {
            /* if vehicle was set as unavailable, unset */
            if(unavailableVehicles.contains(id))
                unavailableVehicles.remove(id);

            availableVehicles.add(id);

            console.post(new MvPlanningEventAvailableVehicle(id));
            /* logging */
            logDebugInfo("Vehicle " + id + " set as AVAILABLE");
        }
    }
    
    private void setVehicleUnavailable(String id) {
        /* if vehicle is not already set as unavailable */
        if(!unavailableVehicles.contains(id)) {
            /* if vehicle was set as available, unset */
            if(availableVehicles.contains(id))
                availableVehicles.remove(id);

            unavailableVehicles.add(id);
            logDebugInfo("Vehicle " + id + " set as UNAVAILABLE");
        }
    }

    private void logDebugInfo(String msg) {
        System.out.println("[mvplanning/VehicleAwareness] " + msg);
    }
}
