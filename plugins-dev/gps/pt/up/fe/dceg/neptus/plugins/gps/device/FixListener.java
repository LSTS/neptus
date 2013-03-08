/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by rasm
 * Apr 13, 2011
 * $Id:: FixListener.java 9615 2012-12-30 23:08:28Z pdias                       $:
 */
package pt.up.fe.dceg.neptus.plugins.gps.device;

/**
 * @author Ricardo Martins
 * 
 */
public interface FixListener {
    /**
     * Called when a new GPS fix is available.
     * 
     * @param fix
     *            GPS fix.
     */
    public abstract void onFix(Fix fix);
}
