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
 * $Id:: IPeriodicUpdates.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.plugins.update;

public interface IPeriodicUpdates {

	/**
	 * Use this method to return the desired update interval
	 * @return The desired update interval, in milliseconds
	 */
	public long millisBetweenUpdates();
	
	/**
	 * This method is called periodically (same period as specified by {@link #millisBetweenUpdates()}
	 * @return When not interested in being updated anymore, this method should return <strong>false</strong>. Return <strong>true</strong> otherwise.
	 */
	public boolean update();
}
