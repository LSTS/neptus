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
 * $Id:: TCPNotification.java 9616 2012-12-30 23:23:22Z pdias             $:
 */
package pt.up.fe.dceg.neptus.util.comm.transports.tcp;

import java.net.InetSocketAddress;

import pt.up.fe.dceg.neptus.util.comm.transports.Notification;

/**
 * @author pdias
 *
 */
public class TCPNotification extends Notification {

	private boolean eosReceived = false;
	
	/**
	 * @param isReception
	 * @param address
	 * @param buffer
	 */
	public TCPNotification(boolean isReception, InetSocketAddress address, byte[] buffer) {
		super(isReception, address, buffer);
	}
	
	/**
	 * @param reception
	 * @param socketAddress
	 * @param recBytes
	 * @param currentTimeMillis
	 */
	public TCPNotification(boolean isReception, InetSocketAddress address,
			byte[] buffer, long timeMillis) {
		super(isReception, address, buffer, timeMillis);
	}

	/**
	 * @param isReception
	 * @param address
	 * @param eos
	 * @param timeMillis
	 */
	public TCPNotification(boolean isReception, InetSocketAddress address,
			boolean eos, long timeMillis) {
		super(isReception, address, new byte[0], timeMillis);
		this.eosReceived = eos;
	}

	/**
	 * @return the eosReceived
	 */
	public boolean isEosReceived() {
		return eosReceived;
	}
	
	/**
	 * @param eosReceived the eosReceived to set
	 */
	public void setEosReceived(boolean eosReceived) {
		this.eosReceived = eosReceived;
	}
}
