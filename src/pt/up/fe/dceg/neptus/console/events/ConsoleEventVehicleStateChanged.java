/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Nov 22, 2012
 */
package pt.up.fe.dceg.neptus.console.events;

import pt.up.fe.dceg.neptus.console.plugins.VehicleStateMonitor;

/**
 * This event is triggered whenever a vehicle changes its mode or gets disconnected.
 * The class {@link VehicleStateMonitor} is the one generating these events.
 * @author zp
 */
public class ConsoleEventVehicleStateChanged {

    public enum STATE {
        SERVICE(0),
        CALIBRATION(1),
        ERROR(2),
        MANEUVER(3),
        EXTERNAL(4),
        TELEOPERATION(5),
        DISCONNECTED(6),
        CONNECTED(7);

        protected long value;

        public long value() {
            return value;
        }

        STATE(long value) {
            this.value = value;
        }
    }
    
    protected String vehicle;
    protected String description;
    protected STATE state;
    
    /**
     * Class constructor
     * @param vehicle The name of the vehicle
     * @param errorDesc The error description (only applicable for error states)
     * @param mode The new mode of the vehicle
     * @see {@link VehicleStateMonitor}
     */
    public ConsoleEventVehicleStateChanged(String vehicle, String description, STATE mode){
        this.vehicle = vehicle;
        this.description = description;
        this.state = mode;
    }

    /**
     * @return The name of the vehicle
     */
    public String getVehicle() {
        return vehicle;
    }

    /**
     * @return The error description (only applicable for error states)
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return The new mode of the vehicle
     */
    public STATE getState() {
        return state;
    }
}
