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
 * $Id:: ConsoleEventNewNotification.java 9615 2012-12-30 23:08:28Z pdias       $:
 */
package pt.up.fe.dceg.neptus.console.events;

import pt.up.fe.dceg.neptus.console.notifications.Notification;

/**
 * @author Hugo
 * 
 */
public class ConsoleEventNewNotification {
    private Notification noty;

    public ConsoleEventNewNotification(Notification noty) {
        this.noty = noty;

    }

    /**
     * @return the noty
     */
    public Notification getNoty() {
        return noty;
    }

}
