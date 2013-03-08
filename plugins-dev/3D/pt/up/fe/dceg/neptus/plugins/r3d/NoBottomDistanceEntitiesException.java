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
 * $Id:: NoBottomDistanceEntitiesException.java 9615 2012-12-30 23:08:28Z pdias $:
 */
package pt.up.fe.dceg.neptus.plugins.r3d;

/**
 * @author Margarida Faria
 *
 */
public class NoBottomDistanceEntitiesException extends Exception {
    private static final long serialVersionUID = -1342306115502379876L;

    public NoBottomDistanceEntitiesException(String message) {
        super(message);
    }
}
