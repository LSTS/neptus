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
 * Nov 21, 2012
 * $Id:: ConsoleEventMainSystemChange.java 9615 2012-12-30 23:08:28Z pdias      $:
 */
package pt.up.fe.dceg.neptus.console.events;

/**
 * @author Hugo
 * 
 */
public class ConsoleEventMainSystemChange {
    private final String old;
    private final String current;

    public ConsoleEventMainSystemChange(String old, String current) {
        this.old = old;
        this.current = current;
    }

    /**
     * @return the old
     */
    public String getOld() {
        return old;
    }

    /**
     * @return the current
     */
    public String getCurrent() {
        return current;
    }

}
