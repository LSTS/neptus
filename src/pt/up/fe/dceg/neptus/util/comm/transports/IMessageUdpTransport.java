/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * 2009/03/29 by pdias
 * $Id:: IMessageUdpTransport.java 9616 2012-12-30 23:23:22Z pdias        $:
 */
package pt.up.fe.dceg.neptus.util.comm.transports;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.messages.IMessage;
import pt.up.fe.dceg.neptus.messages.IMessageProtocol;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfoImpl;
import pt.up.fe.dceg.neptus.messages.listener.MessageListener;
import pt.up.fe.dceg.neptus.util.ByteUtil;
import pt.up.fe.dceg.neptus.util.comm.transports.udp.UDPMessageListener;
import pt.up.fe.dceg.neptus.util.comm.transports.udp.UDPNotification;
import pt.up.fe.dceg.neptus.util.comm.transports.udp.UDPTransport;

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
