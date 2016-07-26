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
package pt.lsts.neptus.plugins.mvplanning.monitors;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.FuelLevel;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;
import pt.lsts.neptus.plugins.mvplanning.interfaces.PlanTask;
import pt.lsts.neptus.plugins.mvplanning.interfaces.TaskConstraint;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * Class responsible for keeping a list of available and
 * unavailable vehicles.
 * It listens to {@link ConsoleEventVehicleStateChanged} events
 * to have a sense of what the vehicles' current state is.
 **/
public class VehicleAwareness {
    public enum VEHICLE_STATE {
        Available("Available"),
        Unavailable("Unavailable"),
        Replanning("Replanning");

        protected String value;
        VEHICLE_STATE(String value) {
            this.value = value;
        }
    };

    private ConsoleAdapter console;
    private Map<String, LocationType> startLocations;
    private ConcurrentMap<String, VEHICLE_STATE> vehiclesState;
    private ConcurrentMap<String, Double> vehiclesFuel;

    public VehicleAwareness(ConsoleAdapter console) {
        this.console = console;
        startLocations = new ConcurrentHashMap<>();
        vehiclesState = new ConcurrentHashMap<>();
        vehiclesFuel = new ConcurrentHashMap<>();
    }


    public void setVehicleStartLocation(String vehicleId, LocationType startLocation) {
        startLocations.put(vehicleId, startLocation);
        LocationType tmp = startLocation.getNewAbsoluteLatLonDepth();
        String message = "[" + vehicleId + "]" + " start location's set in " + tmp.getLatitudeAsPrettyString() +
                " " + tmp.getLongitudeAsPrettyString();
        NeptusLog.pub().info(message);
        NeptusLog.pub().debug(message);
    }

    public LocationType getVehicleStartLocation(String vehicleId) {
        return startLocations.getOrDefault(vehicleId, null);
    }

    @Subscribe
    public void on(ConsoleEventVehicleStateChanged event) {
        if(event == null || event.getState() == null) {
            NeptusLog.pub().error("I'm receiving null ConsoleEventVehicleStateChanged events");
            return;
        }

        String id = event.getVehicle();
        ConsoleEventVehicleStateChanged.STATE newState = event.getState();

        synchronized(vehiclesState) {
            VEHICLE_STATE vState;
            if (newState == STATE.FINISHED || newState == STATE.SERVICE)
                vState = VEHICLE_STATE.Available;
            else
                vState = VEHICLE_STATE.Unavailable;

            vehiclesState.put(id, vState);
            console.notifiySuccess("MvPlanning: [" + id + "] " + vState.value, "");
        }
    }

    @Subscribe
    public void consume(FuelLevel msg) {
        vehiclesFuel.put(msg.getSourceName(), msg.getValue());
    }

    /**
     * If the given vehicle is considered as
     * available
     * */
    public boolean isVehicleAvailable(String vehicle) {
        VEHICLE_STATE state = vehiclesState.get(vehicle);

        return state != null && state == VEHICLE_STATE.Available;
    }

    /**
     * Given a vehicle and task checks if
     * the vehicle meets/validates all of the
     * task's constraints, hence being able to
     * execute it.
     * */
    public boolean isVehicleAvailable(String vehicleId, PlanTask task) {
        ImcSystem sys = ImcSystemsHolder.getSystemWithName(vehicleId);
        for(TaskConstraint constraint : task.getConstraints()) {
            boolean validated = true;
            switch (constraint.getName()) {
                case BatteryLevel:
                    if(!sys.isSimulated() && !constraint.isValidated(vehiclesFuel.get(vehicleId)))
                        validated = false;
                    break;
                case HasPayload:
                    if(!constraint.isValidated(vehicleId))
                        validated = false;
                    break;
                case HasSafeLocationSet:
                    if(!constraint.isValidated(startLocations.get(vehicleId) != null))
                        validated = false;
                    break;
                case HasTcpOn:
                    if(!constraint.isValidated(vehicleId))
                        validated = false;
                    break;
                case IsActive:
                    if(!constraint.isValidated(vehicleId))
                        validated = false;
                    break;
                case IsAvailable:
                    if(!constraint.isValidated(vehiclesState.get(vehicleId)))
                        validated = false;
                    break;
                default:
                    NeptusLog.pub().warn("Unhandled constraint: " + constraint.getName().name());
                    validated = false;
                    break;
            }

            if(!validated)
                return false;
        }
        return true;
    }
}
