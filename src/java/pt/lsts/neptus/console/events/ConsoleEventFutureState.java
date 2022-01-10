/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * 29/01/2017
 */
package pt.lsts.neptus.console.events;

import java.util.Date;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.preview.SimulatedFutureState;

/**
 * @author zp
 *
 */
public class ConsoleEventFutureState {

    protected String vehicle;
    protected Date date;
    protected SystemPositionAndAttitude state;
    
    public ConsoleEventFutureState(SimulatedFutureState future) {
        this.vehicle = future.getVehicle();
        this.date = future.getDate();
        this.state = future.getState();
    }
    
    public ConsoleEventFutureState(String vehicle, Date date, SystemPositionAndAttitude state) {
        this.vehicle = vehicle;
        this.date = date;
        this.state = state;
    }

    /**
     * @return the vehicle
     */
    public String getVehicle() {
        return vehicle;
    }

    /**
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    /**
     * @return the state
     */
    public SystemPositionAndAttitude getState() {
        return state;
    }
    
    @Override
    public String toString() {
        return String.format("%s will finish around %s in about %.1f seconds.", vehicle, state.getPosition(), ((date.getTime() - System.currentTimeMillis())/1000.0));
    }
    
}
