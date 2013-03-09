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
 * 2010/01/16
 */
package pt.up.fe.dceg.neptus.util.comm.transports.udp;

import java.net.InetSocketAddress;

import pt.up.fe.dceg.neptus.util.comm.transports.Notification;

/**
 * @author pdias
 *
 */
public class UDPNotification extends Notification{

	/**
	 * @param isReception
	 * @param address
	 * @param buffer
	 */
	public UDPNotification(boolean isReception, InetSocketAddress address, byte[] buffer) {
		super(isReception, address, buffer);
	}
	

	/**
	 * @param reception
	 * @param socketAddress
	 * @param recBytes
	 * @param currentTimeMillis
	 */
	public UDPNotification(boolean isReception, InetSocketAddress address,
			byte[] buffer, long timeMillis) {
		super(isReception, address, buffer, timeMillis);
	}
}
