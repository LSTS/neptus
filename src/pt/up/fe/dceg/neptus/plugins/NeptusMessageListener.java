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
package pt.up.fe.dceg.neptus.plugins;

import pt.up.fe.dceg.neptus.imc.IMCMessage;

public interface NeptusMessageListener {

	/**
	 * @return The abbreviated names of the messages to listen to
	 */
	public String[] getObservedMessages();
	
	/**
	 * This method is called when a message of the observed type has arrived
	 * @param message The message to be parsed
	 */
	public void messageArrived(IMCMessage message);
	
}
