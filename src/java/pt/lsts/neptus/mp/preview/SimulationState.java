/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 26, 2012
 */
package pt.lsts.neptus.mp.preview;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;


/**
 * @author zp
 *
 */
public class SimulationState {

    protected String currentManeuver;
    protected Object maneuverState;    
    protected SystemPositionAndAttitude sysState;
    protected double timeSinceStart;
    
    public SimulationState(String maneuver, Object maneuverState, SystemPositionAndAttitude sysState, double timeSinceStart) {
        this.currentManeuver = maneuver;
        this.maneuverState = maneuverState;
        this.sysState = sysState;
        this.timeSinceStart = timeSinceStart;
    }

    /**
     * @return the currentManeuver
     */
    public String getCurrentManeuver() {
        return currentManeuver;
    }

    /**
     * @param currentManeuver the currentManeuver to set
     */
    public void setCurrentManeuver(String currentManeuver) {
        this.currentManeuver = currentManeuver;
    }

    /**
     * @return the maneuverState
     */
    public Object getManeuverState() {
        return maneuverState;
    }

    /**
     * @param maneuverState the maneuverState to set
     */
    public void setManeuverState(Object maneuverState) {
        this.maneuverState = maneuverState;
    }

    /**
     * @return the sysState
     */
    public SystemPositionAndAttitude getSysState() {
        return sysState;
    }

    /**
     * @param sysState the sysState to set
     */
    public void setSysState(SystemPositionAndAttitude sysState) {
        this.sysState = sysState;
    }

    /**
     * @return the timeSinceStart
     */
    public final double getTimeSinceStart() {
        return timeSinceStart;
    }

    /**
     * @param timeSinceStart the timeSinceStart to set
     */
    public final void setTimeSinceStart(double timeSinceStart) {
        this.timeSinceStart = timeSinceStart;
    }
    
}
