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
 * $Id:: ConsoleEventEntityStateChanged.java 9615 2012-12-30 23:08:28Z pdias    $:
 */
package pt.up.fe.dceg.neptus.console.events;

import pt.up.fe.dceg.neptus.console.plugins.VehicleStateMonitor;
import pt.up.fe.dceg.neptus.imc.EntityState;
import pt.up.fe.dceg.neptus.imc.EntityState.STATE;

/**
 * This event is triggered whenever a Entity state has changed
 * @author zp
 */
public class ConsoleEventEntityStateChanged {
    
    protected String system;
    protected String entity;
    protected EntityState.STATE previousState;
    protected EntityState.STATE newState;
    
    /**
     * Class constructor
     * @param system The name of the entity owner
     * @param entity The name of the entity that has changed
     * @param prev The previous state
     * @param now The new state
     * @see {@link EntityState}
     * @see {@link VehicleStateMonitor}
     */
    public ConsoleEventEntityStateChanged(String system, String entity, STATE prev, STATE now) {
        this.system = system;
        this.entity = entity;
        this.previousState = prev;
        this.newState = now;                
    }

    /**
     * @return The name of the entity owner
     */
    public String getSystem() {
        return system;
    }

    /**
     * @return The name of the entity that has changed
     */
    public String getEntity() {
        return entity;
    }

    /**
     * @return The previous state
     */
    public EntityState.STATE getPreviousState() {
        return previousState;
    }

    /**
     * @return The new state
     */
    public EntityState.STATE getNewState() {
        return newState;
    }  
}
