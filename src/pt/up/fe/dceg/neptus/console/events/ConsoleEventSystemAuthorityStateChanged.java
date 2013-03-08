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
 * $Id:: ConsoleEventSystemAuthorityStateChanged.java 10019 2013-02-25 03:31:42#$:
 */
package pt.up.fe.dceg.neptus.console.events;

import pt.up.fe.dceg.neptus.console.plugins.SystemsList;
import pt.up.fe.dceg.neptus.console.plugins.VehicleStateMonitor;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem.IMCAuthorityState;

/**
 * This event is triggered whenever the authority over a vehicle changes
 * The class {@link SystemsList} is the one generating these events.
 * @author zp
 */
public class ConsoleEventSystemAuthorityStateChanged {

    protected String system;
    protected IMCAuthorityState state;
    protected IMCAuthorityState previousState;
    
    /**
     * Class constructor
     * @param system The name of the system
     * @param newAuth The new authority state
     * @param prevAuth The previous authority
     * @see {@link VehicleStateMonitor}
     */
    public ConsoleEventSystemAuthorityStateChanged(String system, IMCAuthorityState prevAuth, IMCAuthorityState newAuth) {
        this.system = system;
        this.state = newAuth;
        this.previousState = prevAuth;
    }

    /**
     * @return The name of the system
     */
    public String getSystem() {
        return system;
    }


    /**
     * @return The current authority state
     */
    public IMCAuthorityState getAuthorityState() {
        return state;
    }
    
    /**
     * @return The previous authority state
     */
    public IMCAuthorityState getPreviousAuthorityState() {
        return previousState;
    }
    
}
