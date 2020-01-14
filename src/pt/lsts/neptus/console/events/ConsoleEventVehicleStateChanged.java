/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Nov 22, 2012
 */
package pt.lsts.neptus.console.events;

import pt.lsts.neptus.console.plugins.VehicleStateMonitor;

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
        CONNECTED(7),
        BOOT(8);

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
