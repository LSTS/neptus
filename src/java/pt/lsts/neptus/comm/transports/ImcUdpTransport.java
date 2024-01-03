/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * 2009/03/29
 */
package pt.lsts.neptus.comm.transports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.CommUtil;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.comm.transports.DeliveryListener.ResultEnum;
import pt.lsts.neptus.comm.transports.udp.UDPMessageListener;
import pt.lsts.neptus.comm.transports.udp.UDPNotification;
import pt.lsts.neptus.comm.transports.udp.UDPTransport;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageInfoImpl;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.util.conf.ConfigFetch;

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
                                deliveryListener.deliveryUncertain(message, new Exception("Message delivered via UDP"));
                                break;
                            case Error:
                                deliveryListener.deliveryError(message, error);
                                break;
                            case TimeOut:
                                deliveryListener.deliveryTimeOut(message);
                                break;
                            case Unreachable:
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
            DeliveryResult retResult = getUdpTransport().sendMessage(IdPair.from(destination, port), baos.toByteArray()).get();
            listener.deliveryResult(retResult.result, retResult.exception);
            boolean ret = false;
            switch (retResult.result) {
                case Success:
                    ret = true;
                    break;
                case UnFinished:
                case TimeOut:
                case Unreachable:
                case Error:
                    break;
            }
//            message.dump(System.err);
//            if (message.getAbbrev().equalsIgnoreCase("LblConfig")) {
//                NeptusLog.pub().info("<###> sissssssssssss" + baos.toByteArray().length);
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
	 * @throws Exception
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
//		NeptusLog.pub().info("<###>STOP_______________________" + udpT2.getUdpTransport().isRunnning() + udpT2.getUdpTransport().isStopping());
//		try {
//			Thread.sleep(5000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		udpT2.reStartAll();
//		NeptusLog.pub().info("<###>START______________________" + udpT2.getUdpTransport().isRunnning() + udpT2.getUdpTransport().isStopping());
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
