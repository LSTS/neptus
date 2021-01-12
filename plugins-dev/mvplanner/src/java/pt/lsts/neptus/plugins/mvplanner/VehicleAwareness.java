/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsm
 * 30 Jan 2017
 */
package pt.lsts.neptus.plugins.mvplanner;

import pt.lsts.imc.Loiter;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.StationKeeping;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsoleSystem;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;

import java.util.concurrent.ConcurrentHashMap;

public class VehicleAwareness {
    private enum VehicleStateEnum {
        Available,
        Unavailable
    }

    private ConcurrentHashMap<String, VehicleStateEnum> vehiclesState = null;
    private ConsoleLayout console;

    public VehicleAwareness() {
        vehiclesState = new ConcurrentHashMap<>();
    }

    public void setConsole(ConsoleLayout console) {
        this.console = console;
    }

    public boolean isVehicleAvailable(String id) {
        VehicleStateEnum state = vehiclesState.getOrDefault(id, null);
        return state != null && state == VehicleStateEnum.Available;
    }

    public void onVehicleStateChange(ConsoleEventVehicleStateChanged event) {
        if(event.getState() == null) {
            NeptusLog.pub().error("I'm receiving null ConsoleEventVehicleStateChanged events");
            return;
        }

        if(vehiclesState == null)
            return;

        String id = event.getVehicle();
        ConsoleEventVehicleStateChanged.STATE newState = event.getState();

        VehicleStateEnum vState;
        if (newState == STATE.SERVICE)
            vState = VehicleStateEnum.Available;
        else
            vState = VehicleStateEnum.Unavailable;

        vehiclesState.put(id, vState);
        NeptusLog.pub().info(" * Vehicle " + id + " is " + vState.name());
    }

    public void onPlanControlState(PlanControlState msg) {
        if(vehiclesState == null || console == null)
            return;

        boolean vehicleIsAvailable = false;
        if (msg.getManEta() == 0xFFFF && msg.getManType() == Loiter.ID_STATIC)
            vehicleIsAvailable = true;
        else if (msg.getManEta() == 0xFFFF && msg.getManType() == StationKeeping.ID_STATIC )
            vehicleIsAvailable = true;

        String src = msg.getSourceName();
        ConsoleSystem vehicle = console.getSystem(src);
        if(vehicleIsAvailable) {
            vehiclesState.put(vehicle.getVehicleId(), VehicleStateEnum.Available);
            System.out.println(" ** Vehicle " + vehicle.getVehicleId() + " is available");
        }
    }
}
