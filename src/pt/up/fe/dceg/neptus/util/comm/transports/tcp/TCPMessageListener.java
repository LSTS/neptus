/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2010/05/09
 * $Id:: TCPMessageListener.java 9616 2012-12-30 23:23:22Z pdias          $:
 */
package pt.up.fe.dceg.neptus.util.comm.transports.tcp;

/**
 * @author pdias
 *
 */
public interface TCPMessageListener {

	/**
	 * @param req
	 */
	public void onTCPMessageNotification(TCPNotification req);
	
}
