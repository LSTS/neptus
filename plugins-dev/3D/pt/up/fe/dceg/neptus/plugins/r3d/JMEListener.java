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
 * Feb 6, 2013
 * $Id:: JMEListener.java 9890 2013-02-08 03:32:15Z robot                       $:
 */
package pt.up.fe.dceg.neptus.plugins.r3d;

import java.util.EventListener;

/**
 * @author Margarida Faria
 *
 */

public interface JMEListener extends EventListener {
    public void badDriversOccurred(BadDriversEvent evt);
}