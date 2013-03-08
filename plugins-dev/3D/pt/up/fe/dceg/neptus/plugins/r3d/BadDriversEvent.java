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
 * $Id:: BadDriversEvent.java 9615 2012-12-30 23:08:28Z pdias                   $:
 */
package pt.up.fe.dceg.neptus.plugins.r3d;

import java.util.EventObject;

/**
 * @author Margarida Faria
 *
 */
public class BadDriversEvent extends EventObject {
    private static final long serialVersionUID = -4905792399600262841L;

    public BadDriversEvent(Object source) {
        super(source);
    }

}


