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
public class VehicleAwareness implements IPeriodicUpdates {
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
    private Map<String, STATE> consoleStates;

    private long startTime = -1;

    public VehicleAwareness(ConsoleAdapter console) {
        this.console = console;
        startLocations = new ConcurrentHashMap<>();
        vehiclesState = new ConcurrentHashMap<>();
        consoleStates = new HashMap<>();
        vehiclesFuel = new ConcurrentHashMap<>();
    }

    /**
     * Check vehicles' state and contraints
     * and update if necessary
     * */
    @Override
    public boolean update() {
        boolean restartCounter = false;
        if(startTime == -1)
            startTime = System.currentTimeMillis();

        for(String vehicle : vehiclesState.keySet()) {
            VEHICLE_STATE newState = VEHICLE_STATE.Unavailable;
            if(validConstraints(vehicle))
                newState = VEHICLE_STATE.Available;

            if(newState != vehiclesState.get(vehicle)) {
                vehiclesState.put(vehicle, newState);
                notify(vehicle, newState);
            }

            /* Every 30 seconds display status info */
            if(System.currentTimeMillis() - startTime >= 60000) {
                NeptusLog.pub().info(getStatusMessage(vehicle));
                restartCounter = true;
            }
        }

        if(restartCounter) {
            startTime = System.currentTimeMillis();
            System.out.println("\n");
        }
        return true;
    }

    /**
     * Posts a new notification to the console
     * */
    private void notify(String vehicle, VEHICLE_STATE newState) {
        String message = "MvPlanning: " + getStatusMessage(vehicle);
        if(newState == VEHICLE_STATE.Available)
            console.notifiySuccess(message, "");
        else
            console.notifyWarning(message, "");
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
        NeptusLog.pub().info(getStatusMessage(id));
        ConsoleEventVehicleStateChanged.STATE newState = event.getState();

        synchronized(vehiclesState) {
            if (!vehiclesState.containsKey(id))
                vehiclesState.put(id, VEHICLE_STATE.Unavailable);
        }

        consoleStates.put(id, newState);
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
     * Constraints that dictate if a vehicle is
     * to be considered as available for allocation
     * */
    private boolean validConstraints(String vehicle) {
        boolean isAvailable = true;
        ImcSystem sys = ImcSystemsHolder.getSystemWithName(vehicle);

        STATE consoleState = consoleStates.get(vehicle);
        isAvailable = isAvailable &&
                consoleState != null && (consoleState == STATE.FINISHED || consoleState == STATE.SERVICE);
        isAvailable = isAvailable &&
                (sys.isActive() && (sys.isTCPOn() || sys.isSimulated()));
        isAvailable = isAvailable && (getVehicleStartLocation(vehicle) != null);

        return isAvailable;

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
            switch (constraint.getName()) {
                case BatteryLevel:
                    if(!sys.isSimulated() && !constraint.isValidated(vehiclesFuel.get(vehicleId)))
                        return false;
                case HasPayload:
                    if(!constraint.isValidated(vehicleId))
                        return false;
                case HasSafeLocationSet:
                    if(!constraint.isValidated(startLocations.get(vehicleId) != null))
                        return false;
                case HasTcpOn:
                    if(!constraint.isValidated(vehicleId))
                        return false;
                case IsActive:
                    if(!constraint.isValidated(vehicleId))
                        return false;
                case IsAvailable:
                    if(!constraint.isValidated(vehiclesState.get(vehicleId)))
                        return false;
                default:
                    NeptusLog.pub().warn("Unknown constraint");
                    return false;
            }
        }
        return true;
    }


    @Override
    public long millisBetweenUpdates() {
        return 1000;
    }

    /**
     * Method that generates a debug message
     * concerning the status of vehicles's
     * constraints (tcp on, vehicle state, etc)
     * for a given vehicle.
     * */
    private String getStatusMessage(String vehicle) {
        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(vehicle);
        boolean isActive = sys.isActive();
        boolean isTCPOn = sys.isTCPOn();
        boolean isSimulated = sys.isSimulated();
        VEHICLE_STATE state = vehiclesState.get(vehicle);
        boolean hasSafeLocation = (getVehicleStartLocation(vehicle) != null);
        String debugMsg = "";

        if(state != null)
            debugMsg += state.name() + " | ";
        else
            debugMsg += "Unavailable | ";

        if(isActive)
            debugMsg += "ACTIVE | ";
        else
            debugMsg += "NOT ACTIVE | ";

        if (isTCPOn)
            debugMsg += "TCP ON| ";
        else
            debugMsg += "TCP OFF | ";

        if(isSimulated)
            debugMsg += "SIMULATED | ";
        else
            debugMsg += "NOT SIMULATED | ";

        if(hasSafeLocation)
            debugMsg += "SAFE LOC | ";
        else
            debugMsg += "NO SAFE LOC";

        return debugMsg;
    }
}
