/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * 2009/03/29
 * $Id:: ImcUdpTransport.java 9616 2012-12-30 23:23:22Z pdias             $:
 */
package pt.up.fe.dceg.neptus.util.comm.transports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfoImpl;
import pt.up.fe.dceg.neptus.messages.listener.MessageListener;
import pt.up.fe.dceg.neptus.util.comm.CommUtil;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.MessageDeliveryListener;
import pt.up.fe.dceg.neptus.util.comm.transports.DeliveryListener.ResultEnum;
import pt.up.fe.dceg.neptus.util.comm.transports.udp.UDPMessageListener;
import pt.up.fe.dceg.neptus.util.comm.transports.udp.UDPNotification;
import pt.up.fe.dceg.neptus.util.comm.transports.udp.UDPTransport;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 *
 */
public class ImcUdpTransport {
	
	private LinkedHashSet<MessageListener<MessageInfo, IMCMessage>> listeners = new LinkedHashSet<MessageListener<MessageInfo, IMCMessage>>();
	private IMCDefinition imcDefinition;
	
	private UDPTransport udpTransport = null;
	
	private int bindPort = 6001;
	private String multicastAddress = "";
	
	private boolean broadcastEnable = false;

	
	public ImcUdpTransport(int bindPort, IMCDefinition imcDefinition) {
	    this.imcDefinition = imcDefinition;
		this.bindPort = bindPort;
		this.multicastAddress = "";
		getUdpTransport();
		setUDPListener();
	}

	public ImcUdpTransport(int bindPort, String multicastAddress, IMCDefinition imcDefinition) {
        this.imcDefinition = imcDefinition;
		this.bindPort = bindPort;
		this.multicastAddress = multicastAddress;
		getUdpTransport();
		setUDPListener();
	}

	public ImcUdpTransport(int bindPort, boolean broadcastEnable, IMCDefinition imcDefinition) {
        this.imcDefinition = imcDefinition;
		this.bindPort = bindPort;
		this.broadcastEnable = broadcastEnable;
		getUdpTransport();
		setUDPListener();
	}

	/**
	 * @return the bindPort
	 */
	public int getBindPort() {
		return bindPort;
	}
	
	/**
	 * @param bindPort the bindPort to set
	 */
	public void setBindPort(int bindPort) {
		this.bindPort = bindPort;
		getUdpTransport().setBindPort(bindPort);
	}
	
	/**
	 * @return
	 */
	public boolean isRunnning() {
		return getUdpTransport().isRunning();
	}
	
	/**
	 * @return the multicastAddress
	 */
	public String getMulticastAddress() {
		return multicastAddress;
	}
	
	/**
     * @param multicastAddress the multicastAddress to set
     */
    public void setMulticastAddress(String multicastAddress) {
        this.multicastAddress = multicastAddress;
    }
	
    /**
     * @return the broadcastEnable
     */
    public boolean isBroadcastEnable() {
        return broadcastEnable;
    }
    
    /**
     * @param broadcastEnable the broadcastEnable to set
     */
    public void setBroadcastEnable(boolean broadcastEnable) {
        this.broadcastEnable = broadcastEnable;
        getUdpTransport().setBroadcastEnable(broadcastEnable);
    }
    
	/**
	 * 
	 */
	private void setUDPListener() {
		getUdpTransport().addListener(new UDPMessageListener() {
			@Override
			public void onUDPMessageNotification(UDPNotification req) {
					//ByteUtil.dumpAsHex(req.getBuffer(),System.out);
//					long start = System.nanoTime();
			    IMCMessage msg;
			    
			    try {
			        msg = imcDefinition.parseMessage(req.getBuffer());			        
			    } 
                catch (IOException e) {
                    NeptusLog.pub().warn(e.getMessage()+" while unpacking message sent from " + req.getAddress().getHostString());
                    return;
                }

                MessageInfo info = new MessageInfoImpl();
                info.setPublisher(req.getAddress().getAddress().getHostAddress());
                info.setPublisherInetAddress(req.getAddress().getAddress().getHostAddress());
                info.setPublisherPort(req.getAddress().getPort());
                info.setTimeReceivedNanos(req.getTimeMillis() * (long)1E6);
                info.setTimeSentNanos((long)msg.getTimestamp() * (long)1E9);
                info.setProperty(MessageInfo.TRANSPORT_MSG_KEY, "UDP");
                for (MessageListener<MessageInfo, IMCMessage> lst : listeners) {
						try {
							//req.getMessage().dump(System.err);
							lst.onMessage(info , msg);
						} 
						catch (Exception e) {
							NeptusLog.pub().error(e);
						}
						catch (Error e) {
							NeptusLog.pub().error(e);
						}
					}
				}
		});
	}

	/**
	 * @return the udpTransport
	 */
	public UDPTransport getUdpTransport() {
		if (udpTransport == null) {
			if (broadcastEnable) {
				udpTransport = new UDPTransport(true, bindPort, 1);
			} else if (multicastAddress == null || "".equalsIgnoreCase(multicastAddress)) {
				udpTransport = new UDPTransport(bindPort, 1);
			} else {
				udpTransport = new UDPTransport(multicastAddress, bindPort, 1);
			}
		}
		return udpTransport;
	}
	
	
	/**
	 * @param listener
	 * @return
	 */
	public boolean addListener(MessageListener<MessageInfo, IMCMessage> listener) {
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
			MessageListener<MessageInfo, IMCMessage> listener) {
		boolean ret = false;
		synchronized (listeners) {
			ret = listeners.remove(listener);
		}
		return ret;
	}
	
	/**
     * @param destination
     * @param port
     * @param message
     */
    public boolean sendMessage(String destination, int port, IMCMessage message) {
        return sendMessage(destination, port, message, null);
    }

	public boolean sendMessage(String destination, int port, final IMCMessage message,
	        final MessageDeliveryListener deliveryListener) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IMCOutputStream imcOs = new IMCOutputStream(baos);

		try {
		    message.serialize(imcOs);
		    
		    DeliveryListener listener = null;
            if (deliveryListener != null) {
                listener = new DeliveryListener() {
                    @Override
                    public void deliveryResult(ResultEnum result, Exception error) {
                        switch (result) {
                            case Success:
                                deliveryListener.deliverySuccess(message);
                                break;
                            case Error:
                                deliveryListener.deliveryError(message, error);
                                break;
                            case TimeOut:
                                deliveryListener.deliveryTimeOut(message);
                                break;
                            case Unreacheable:
                                deliveryListener.deliveryUnreacheable(message);
                                break;
                            default:
                                deliveryListener.deliveryError(message, new Exception("Delivery "
                                        + ResultEnum.UnFinished));
                                break;
                        }
                    }
                };                
            }
            boolean ret = getUdpTransport().sendMessage(destination, port, baos.toByteArray(), listener);
//            message.dump(System.err);
//            if (message.getAbbrev().equalsIgnoreCase("LblConfig")) {
//                System.out.println(" sissssssssssss" + baos.toByteArray().length);
//                ByteUtil.dumpAsHex(message.getAbbrev(), baos.toByteArray(), System.out);
//            }
            if (!ret) {
                if (deliveryListener != null) {
                    deliveryListener.deliveryError(message, new Exception("Delivery "
                            + ResultEnum.UnFinished + " due to closing transport!"));
                }
            }
            return ret;
		} catch (Exception e) {
		    e.printStackTrace();
			NeptusLog.pub().error(e);
			if (deliveryListener != null) {
                deliveryListener.deliveryError(message, e);
            }
			return false;
		}
	}

	
	public void stop() {
		getUdpTransport().stop();
	}
	
	public void purge() {
		getUdpTransport().purge();
	}
	
	public void reStart() {
		getUdpTransport().reStart();
	}
	
	/**
	 * 
	 */
	public boolean isOnBindError() {
		return getUdpTransport().isOnBindError();
	}

	
	/**
	 * @param args
	 * @throws MiddlewareException 
	 */
	public static void main(String[] args) throws Exception {
		ConfigFetch.initialize();
//		MiddlewareOperations.startNeptusMiddleware();

		ImcUdpTransport udpT = new ImcUdpTransport(2550, IMCDefinition.getInstance());
		
		udpT.addListener(new MessageListener<MessageInfo, IMCMessage>() {
			@Override
			public void onMessage(MessageInfo info, IMCMessage msg) {
				info.dump(System.err);
				msg.dump(System.err);
			}
		});
				
//		for (int i = 0; i < 10; i++) {
//			udpT.sendMessage("127.0.0.1", 2550, IMCDefinition.getInstance().create("EstimatedState", "lat", new NativeDOUBLE(i)));
//		}
		
//		udpT.purgeAll();

//		udpT.reStartAll();
//		//udpT.getReceiver().purge();
//		for (int i = 0; i < 2; i++) {
//			udpT.sendMessage("127.0.0.1", 2550, IMCDefinition.getInstance().create("ReportedState", "lat", i));
//		}
//		
//		udpT.purgeAll();
//
//		udpT.reStartAll();
//		for (int i = 0; i < 2; i++) {
//			udpT.sendMessage("127.0.0.1", 2550, IMCDefinition.getInstance().create("SimulatedPosition", "x", new NativeDOUBLE(i)));
//		}
//		
//		//udpT.getSender().purge();
//		//udpT.getReceiver().purge();
//		udpT.purgeAll();
//		
//		ImcUdpTransport udpT2 = new ImcUdpTransport(6001);
//		
//		udpT2.addListener(new MessageListener<MessageInfo, IMCMessage>() {
//			@Override
//			public void onMessage(MessageInfo info, IMCMessage msg) {
//				msg.dump(System.err);
//			}
//		});
//
//		for (int i = 0; i < 1000; i++) {
//			udpT2.sendMessage("127.0.0.1", 6002, IMCDefinition.getInstance().create("Heartbeat"));
//		}
//
//		udpT2.purgeAll();
//		System.out.println("STOP_______________________" + udpT2.getUdpTransport().isRunnning() + udpT2.getUdpTransport().isStopping());
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		udpT2.reStartAll();
//		System.out.println("START______________________" + udpT2.getUdpTransport().isRunnning() + udpT2.getUdpTransport().isStopping());
//		udpT2.reStartAll();
		
		
		String multicastAddress = "224.0.75.69";
		String multicastRangePortsStr = "6969-6970";
		ImcUdpTransport multicastUdpTransport;
		int localport = 6969;
		int[] multicastPorts = CommUtil
				.parsePortRangeFromString(multicastRangePortsStr, new int[]{6969});

		multicastUdpTransport = new ImcUdpTransport(
				(multicastPorts.length == 0) ? localport
						: multicastPorts[0], multicastAddress, IMCDefinition.getInstance());
		multicastUdpTransport.reStart();
		if (!multicastUdpTransport.isOnBindError()) {
			for (int i = 1; i < multicastPorts.length; i++) {
				multicastUdpTransport.stop();
				multicastUdpTransport.setBindPort(multicastPorts[i]);
				multicastUdpTransport.reStart();
				if (multicastUdpTransport.isOnBindError())
					break;
			}
		}
		multicastUdpTransport.addListener(new MessageListener<MessageInfo, IMCMessage>() {
			@Override
			public void onMessage(MessageInfo info, IMCMessage msg) {
				// TODO Auto-generated method stub
				
			}
		});
	}
}
