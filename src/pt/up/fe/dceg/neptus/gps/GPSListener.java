/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.gps;

public interface GPSListener {
	/**
	 * Every time the GPS has a new state, this method is invoked
	 * @param oldState The last state that was read
	 * @param newState The new state being reported
	 */
	public void GPSStateChanged(GPSState oldState, GPSState newState);
}
