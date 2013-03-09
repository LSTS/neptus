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
 * Nov 26, 2012
 */
package pt.up.fe.dceg.neptus.console.events;

import pt.up.fe.dceg.neptus.console.ConsoleSystem;

/**
 * @author Hugo
 * 
 */
public class ConsoleEventNewSystem {
    private final ConsoleSystem system;

    public ConsoleEventNewSystem(ConsoleSystem system) {
        this.system = system;
    }

    /**
     * @return the system
     */
    public ConsoleSystem getSystem() {
        return system;
    }
}
