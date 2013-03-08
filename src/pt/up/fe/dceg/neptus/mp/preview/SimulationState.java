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
 * Nov 26, 2012
 * $Id:: SimulationState.java 9880 2013-02-07 15:23:52Z jqcorreia               $:
 */
package pt.up.fe.dceg.neptus.mp.preview;

import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;


/**
 * @author zp
 *
 */
public class SimulationState {

    protected String currentManeuver;
    protected Object maneuverState;    
    protected SystemPositionAndAttitude sysState;
    
    public SimulationState(String maneuver, Object maneuverState, SystemPositionAndAttitude sysState) {
        this.currentManeuver = maneuver;
        this.maneuverState = maneuverState;
        this.sysState = sysState;
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
    
}
