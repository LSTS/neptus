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
 * $Id:: NeptusAction.java 9616 2012-12-30 23:23:22Z pdias                $:
 */
package pt.up.fe.dceg.neptus.plugins;


/**
 * This interface is to be used by pluggable actions that can be added to Console interfaces and other applications
 * @author zp
 * @version 1.0
 */
public interface NeptusAction {

	/**
	 * Executes the action, a separate thread is used if the call {@link #runInOwnThread()} returns <b>true</b>
	 */
	public void execute();
	
	/**
	 * @return <b>true</b> if this action should be run in a separate thread or <b>false</b> otherwise
	 */
	public boolean runInOwnThread();
	
	/**
	 * If this action is to be executed periodically (as a daemon), this method should return a value greater than 0
	 * @return A value greater than 0 corresponding to the interval between executions or <b>0</b> if this action is not to be run periodically
	 */
	public int getPeriodicityMillis();
}
