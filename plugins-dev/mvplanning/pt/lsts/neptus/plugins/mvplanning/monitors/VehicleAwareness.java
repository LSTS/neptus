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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.events.ConsoleEventNewNotification;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;

/**
 * Class responsible for keeping a list of available and
 * unavailable vehicles.
 * It listens to {@link ConsoleEventVehicleStateChanged} events
 * to have a sense of what the vehicles' current state is.
 **/
public class VehicleAwareness implements IPeriodicUpdates {
    private final ReadWriteLock RW_LOCK = new ReentrantReadWriteLock();

    private enum VEHICLE_STATE {
        AVAILABLE("Available"),
        UNAVAILABLE("Unavailable"),
        REPLANNING("Replanning");

        protected String value;
        VEHICLE_STATE(String value) {
            this.value = value;
        }
    };

    private ConsoleAdapter console;
    private Map<String, LocationType> startLocations;
    private ConcurrentMap<String, VEHICLE_STATE> vehiclesState;
    private Map<String, STATE> consoleStates;

    private long startTime = -1;

    public VehicleAwareness(ConsoleAdapter console) {
        this.console = console;
        startLocations = new ConcurrentHashMap<>();
        vehiclesState = new ConcurrentHashMap<>();
        consoleStates = new HashMap<>();

        /* Fetch available vehicles, on plugin start-up */
        for(ImcSystem vehicle : ImcSystemsHolder.lookupActiveSystemByType(SystemTypeEnum.VEHICLE))
            setVehicleStartLocation(vehicle.getName(), vehicle.getLocation());
    }

    /**
     * Check vehicles' state and contraints
     * and update if necessary
     * */
    @Override
    public boolean update() {
        RW_LOCK.writeLock().lock();
        boolean restartCounter = false;
        if(startTime == -1)
            startTime = System.currentTimeMillis();

        for(String vehicle : vehiclesState.keySet()) {
            VEHICLE_STATE newState = VEHICLE_STATE.UNAVAILABLE;
            if(validConstraints(vehicle))
                newState = VEHICLE_STATE.AVAILABLE;

            if(newState == null || newState != vehiclesState.get(vehicle)) {
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
        RW_LOCK.writeLock().unlock();
        return true;
    }

    /**
     * Posts a new notification to the console
     * */
    private void notify(String vehicle, VEHICLE_STATE newState) {
        String message = "MvPlanning: " + getStatusMessage(vehicle);
        if(newState == VEHICLE_STATE.AVAILABLE)
            console.post(Notification.success(message, ""));
        else
            console.post(Notification.warning(message, ""));
    }

    public void setVehicleStartLocation(String vehicleId, LocationType startLocation) {
        startLocations.put(vehicleId, startLocation);
        NeptusLog.pub().info("[" + vehicleId + "]" + " start location's set");
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

        if(newState == STATE.SERVICE && !startLocations.containsKey(id)) {
            ImcSystem vehicle = ImcSystemsHolder.getSystemWithName(id);
            setVehicleStartLocation(vehicle.getName(), vehicle.getLocation());
        }

        RW_LOCK.writeLock().lock();
        if(!vehiclesState.containsKey(id))
            vehiclesState.put(id, VEHICLE_STATE.UNAVAILABLE);
        consoleStates.put(id, newState);
        RW_LOCK.writeLock().unlock();
    }

    /**
     * If the given vehicle is considered as
     * available
     * */
    public boolean isVehicleAvailable(String vehicle) {
        RW_LOCK.readLock().lock();
        VEHICLE_STATE state = vehiclesState.get(vehicle);
        RW_LOCK.readLock().unlock();

        return state != null && state == VEHICLE_STATE.AVAILABLE;
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

        return isAvailable;

    }


    @Override
    public long millisBetweenUpdates() {
        return 1000;
    }

    private String getStatusMessage(String vehicle) {
        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(vehicle);
        boolean isActive = sys.isActive();
        boolean isTCPOn = sys.isTCPOn();
        boolean isSimulated = sys.isSimulated();
        VEHICLE_STATE state = vehiclesState.get(vehicle);
        String debugMsg = "";

        if(state != null)
            debugMsg += state.name() + " | ";
        else
            debugMsg += "UNAVAILABLE | ";

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

        return debugMsg;
    }
}
