/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Nov 13, 2012
 */
package pt.up.fe.dceg.neptus.plugins.r3d;

import javax.swing.event.EventListenerList;

/**
 * @author Margarida Faria
 *
 */
public class BadDrivers {
    protected EventListenerList listenerList = new EventListenerList();

    public void addBadDriversListener(JMEListener listener) {
        listenerList.add(JMEListener.class, listener);
    }

    public void removeBadDriversListener(JMEListener listener) {
        listenerList.remove(JMEListener.class, listener);
    }

    public void fireBadDrivers(BadDriversEvent evt) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = 0; i < listeners.length; i = i + 2) {
            if (listeners[i] == JMEListener.class) {
                ((JMEListener) listeners[i + 1]).badDriversOccurred(evt);
            }
        }
    }
}
