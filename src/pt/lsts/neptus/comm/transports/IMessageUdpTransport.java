/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * 2009/03/29 by pdias
 */
package pt.lsts.neptus.comm.transports;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.transports.udp.UDPMessageListener;
import pt.lsts.neptus.comm.transports.udp.UDPNotification;
import pt.lsts.neptus.comm.transports.udp.UDPTransport;
import pt.lsts.neptus.messages.IMessage;
import pt.lsts.neptus.messages.IMessageProtocol;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageInfoImpl;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.util.ByteUtil;

/**
 * @author pdias
 *
 */
public class IMessageUdpTransport<M extends IMessage, P extends IMessageProtocol<M>> {
	
	private LinkedHashSet<MessageListener<MessageInfo, M>> listeners = new LinkedHashSet<MessageListener<MessageInfo, M>>();

	private P protocol = null;
	
	private UDPTransport udpTransport = null;
	
	int bindPort = 52001;
	String multicastAddress = null;

	public IMessageUdpTransport(String multicastAddress, int bindPort, P protocol) {
		this.multicastAddress = multicastAddress;
		this.protocol = protocol;
		this.bindPort = bindPort;
		getUdpTransport();
		setUDPListener();
	}

	public IMessageUdpTransport(int bindPort, P protocol) {
		this.protocol = protocol;
		this.bindPort = bindPort;
		getUdpTransport();
		setUDPListener();
	}
	
	/**
	 * 
	 */
	private void setUDPListener() {
		getUdpTransport().addListener(new UDPMessageListener() {
			@Override
			public void onUDPMessageNotification(UDPNotification req) {
				try {
					ByteUtil.dumpAsHex(req.getBuffer(), System.out);
					M msg = protocol.unserialize(new ByteArrayInputStream(req.getBuffer()));
					for (MessageListener<MessageInfo, M> lst : listeners) {
						MessageInfo info = new MessageInfoImpl();
						info.setPublisher(req.getAddress().getAddress().getHostAddress());
						info.setPublisherInetAddress(req.getAddress().getAddress().getHostAddress());
						info.setPublisherPort(req.getAddress().getPort());
						info.setTimeReceivedNanos(req.getTimeMillis() * (long)1E6);
						try {
							info.setTimeSentNanos((long) ((Double)msg.getValue("TimeStamp") * 1E9));
						} catch (Exception e) {
							info.setTimeSentNanos(req.getTimeMillis() * (long)1E6);
						}
						try {
							//req.getMessage().dump(System.err);
							lst.onMessage(info , msg);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						catch (Error e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * @return the udpTransport
	 */
	public UDPTransport getUdpTransport() {
		if (udpTransport == null) {
			if (multicastAddress == null)
				udpTransport = new UDPTransport(bindPort, 1);
			else
				udpTransport = new UDPTransport(multicastAddress, bindPort, 1);
		}
		return udpTransport;
	}
	
	
	/**
	 * @param listener
	 * @return
	 */
	public boolean addListener(MessageListener<MessageInfo, M> listener) {
		boolean ret = false;
		synchronized (listeners) {
			ret = listeners.add(listener);
		}
		return ret;
	}

	/**
	 * @param listener
	 * @return
	 */
	public boolean removeListener(
			MessageListener<MessageInfo, M> listener) {
		boolean ret = false;
		synchronized (listeners) {
			ret = listeners.remove(listener);
		}
		return ret;
	}
	
	
	public void sendMessage(String destination, int port, M message) {
		ByteArrayOutputStream sb = new ByteArrayOutputStream();
		try {
			protocol.serialize(message, sb);
			byte[] buffer = sb.toByteArray();
			getUdpTransport().sendMessage(destination, port,
					Arrays.copyOf(buffer, buffer.length));
			ByteUtil.dumpAsHex(message.getAbbrev(), buffer, System.out);
		} catch (Exception e) {
			NeptusLog.pub().error(e);
		}
	}

	
	public void stopAll() {
		getUdpTransport().stop();
	}
	
	public void purgeAll() {
		getUdpTransport().purge();
	}
	
	public void reStartAll() {
		getUdpTransport().reStart();
	}
}
