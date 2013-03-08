/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo
 * Nov 23, 2012
 * $Id:: ConsoleEventMissionChanged.java 9615 2012-12-30 23:08:28Z pdias        $:
 */
package pt.up.fe.dceg.neptus.console.events;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.types.mission.MissionType;

/**
 * @author Hugo
 * 
 */
public class ConsoleEventMissionChanged {
    private final MissionType old;
    private final MissionType current;
    
    /**
     * This triggers when {@link ConsoleLayout#setMission(MissionType)} is called
     * @param old
     * @param current
     */
    public ConsoleEventMissionChanged(MissionType old, MissionType current){
        this.old = old;
        this.current = current;
    }

    /**
     * @return the current
     */
    public MissionType getCurrent() {
        return current;
    }

    /**
     * @return the old
     */
    public MissionType getOld() {
        return old;
    }

}
