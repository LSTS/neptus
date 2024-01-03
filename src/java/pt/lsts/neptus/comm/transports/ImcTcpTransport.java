/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2011/01/17
 */
package pt.lsts.neptus.comm.transports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.comm.transports.DeliveryListener.ResultEnum;
import pt.lsts.neptus.comm.transports.tcp.TCPMessageListener;
import pt.lsts.neptus.comm.transports.tcp.TCPNotification;
import pt.lsts.neptus.comm.transports.tcp.TCPTransport;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageInfoImpl;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 *
 */
public class ImcTcpTransport {
	
	private LinkedHashSet<MessageListener<MessageInfo, IMCMessage>> listeners = new LinkedHashSet<MessageListener<MessageInfo, IMCMessage>>();
    private IMCDefinition imcDefinition;

	private TCPTransport tcpTransport = null;
	
	private int bindPort = 7011;

    final HashMap<String, TCPMessageProcessor> listProc = new HashMap<String, TCPMessageProcessor>();

	public ImcTcpTransport(int bindPort, IMCDefinition imcDefinition) {
        this.imcDefinition = imcDefinition;
		this.bindPort = bindPort;
		getTcpTransport();
		setTCPListener();
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
		getTcpTransport().setBindPort(bindPort);
	}
	
	/**
	 * @return
	 */
	public boolean isRunning() {
		return getTcpTransport().isRunning();
	}	
	
    /**
     * @return
     */
    public boolean isRunningNormally() {
        return getTcpTransport().isRunningNormally();
    }

	/**
	 * 
	 */
	private void setTCPListener() {
		getTcpTransport().addListener(new TCPMessageListener() {
			@Override
			public void onTCPMessageNotification(TCPNotification req) {
//	              NeptusLog.pub().info("<###>ssssssssssssssssssss "+req.getTimeMillis());
			    String id = req.getAddress().toString();
//	              NeptusLog.pub().info("<###>---id: "+id);
			    TCPMessageProcessor proc = listProc.get(id);
			    if (proc == null) {
			        proc = new TCPMessageProcessor(id, listeners, imcDefinition);
			        listProc.put(id, proc);
			    }
			    if (req.isEosReceived())
			        listProc.remove(id);
			    proc.onTCPMessageNotification(req);
			}
		});
	}

	/**
	 * @return the udpTransport
	 */
	public TCPTransport getTcpTransport() {
		if (tcpTransport == null) {
			tcpTransport = new TCPTransport(bindPort);
		}
		return tcpTransport;
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

	
    /**
     * @param destination
     * @param port
     * @param message
     * @param deliveryListener
     */
    public boolean sendMessage(String destination, int port, final IMCMessage message,
            final MessageDeliveryListener deliveryListener) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IMCOutputStream imcOs = new IMCOutputStream(baos);
        try {
            @SuppressWarnings("unused")
            int size = message.serialize(imcOs);
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
            DeliveryResult retResult = getTcpTransport().sendMessage(IdPair.from(destination, port), baos.toByteArray()).get();
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
            if (!ret) {
                if (deliveryListener != null) {
                    deliveryListener.deliveryError(message, new Exception("Delivery "
                            + ResultEnum.UnFinished + " due to closing transport!"));
                }
            }
            return ret;
        } catch (Exception e) {
            NeptusLog.pub().error(e);
            if (deliveryListener != null) {
                deliveryListener.deliveryError(message, e);
            }
            return false;
        }
    }
	
	/**
	 * 
	 */
	public void stop() {
		getTcpTransport().stop();
		stopAndCleanReceiveProcessors();
	}
	
	/**
	 * 
	 */
	public void purge() {
		getTcpTransport().purge();
		stopAndCleanReceiveProcessors();
	}
	
	/**
     * Call only after stop or purge.
     */
    private void stopAndCleanReceiveProcessors() {
        for (TCPMessageProcessor proc : listProc.values()) {
            proc.cleanup();
        }
        listProc.clear();
    }

    public void reStart() {
		getTcpTransport().reStart();
	}
	
	/**
	 * 
	 */
	public boolean isOnBindError() {
		return getTcpTransport().isOnBindError();
	}

    /**
     * @return
     */
    public long getActiveNumberOfConnections() {
        return getTcpTransport().getActiveNumberOfConnections();
    }
    
    public boolean isConnectionEstablished(String host, int port) {
        return getTcpTransport().isConnectionEstablished(host, port);
    }

	/**
	 * @author pdias
	 *
	 */
    static class TCPMessageProcessor implements TCPMessageListener, // GzLsf2Llf.MessageListener,
            Comparable<TCPMessageProcessor> {
        String id = "";
	    PipedOutputStream pos;
	    PipedInputStream pis;

	    // Needed because the pis.available() not always when return '0' means end of stream
        boolean isInputClosed = false;

	    String host = "";
	    int port = 0;
	    
	    LinkedHashSet<MessageListener<MessageInfo, IMCMessage>> listeners;
	    IMCDefinition imcDefinition;
	    
	    /**
	     * 
	     */
	    public TCPMessageProcessor(String id, LinkedHashSet<MessageListener<MessageInfo, IMCMessage>> listeners,
	            IMCDefinition imcDefinition) {
	        this.imcDefinition = imcDefinition;
	        this.id = id;
	        this.listeners = listeners;
	        pos = new PipedOutputStream();
	        try {
	            pis = new PipedInputStream(pos);
	        }
	        catch (IOException e) {
	            e.printStackTrace();
	        }
	        final IMCDefinition imcDef = imcDefinition;
	        new Thread(ImcTcpTransport.class.getSimpleName() + " :: " + TCPMessageProcessor.class.getSimpleName() + "(" + TCPMessageProcessor.this.hashCode() + ")") {
	            @Override
	            public void run() {
	                try {
	                    //IMCInputStream iis = new IMCInputStream(pis);
	                    
                        while(!isInputClosed && pis.available() >= 0) { // the pis.available() not always when return '0' means end of stream
                            
                            if (pis.available() == 0) {
                                try { Thread.sleep(20); } catch (InterruptedException e) { }
                                continue;
                            }
                            try {
                                IMCMessage msg = imcDef.nextMessage(pis);
                                if (msg != null)
                                    msgArrived(/*(long) timeMillis,*/ msg);
                            }
                            catch (IOException e) {
                                if (!"Unrecognized Sync word: 00".equalsIgnoreCase(e.getMessage()))
                                    NeptusLog.pub().debug(e);
                            }
//                            byte[] ba = new byte[pis.available()];
//                            if (ba.length > 0) {
//                                pis.read(ba);
//                                ByteUtil.dumpAsHex(ba, System.out);
//                            }
                        }
                    }
                    catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
	            }
	        }.start();
	    }

	    /**
	     * @return the id
	     */
	    public String getId() {
	        return id;
	    }

	    /* (non-Javadoc)
	     * @see pt.lsts.neptus.util.comm.transports.tcp.TCPMessageListener#onTCPMessageNotification(pt.lsts.neptus.util.comm.transports.tcp.TCPNotification)
	     */
	    @Override
	    public void onTCPMessageNotification(TCPNotification req) {
	        //    NeptusLog.pub().info("<###>ssssssssssssssssssss "+req.getTimeMillis());
//	        ByteUtil.dumpAsHex(req.getAddress()+"", req.getBuffer(), System.out);
	        host = req.getAddress().getAddress().getHostAddress();
	        port = req.getAddress().getPort();
	        
	        try {
	            if (req.isEosReceived()) {
                    isInputClosed = true;
	                pos.flush();
	                pos.close();
	            }
	            else {
	                pos.write(req.getBuffer());
	            }
	        }
	        catch (IOException e) {
	            e.printStackTrace();
	        }
	    }

	    /**
         * Calling this will invalidate the instance for future use.
         */
        public void cleanup() {
            try {
                pos.flush();
                pos.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            isInputClosed = true;
        }
	    
//	    /* (non-Javadoc)
//	     * @see pt.lsts.neptus.util.lsf.gz.GzLsf2Llf.MessageListener#msgArrived(long, pt.lsts.neptus.imc.IMCMessage)
//	     */
//	    @Override
	    public void msgArrived(/*long timeStampMillis,*/ IMCMessage msg) {
	        //msg.dump(System.out);
	        
            MessageInfo info = new MessageInfoImpl();
            info.setPublisher(host);
            info.setPublisherInetAddress(host);
            info.setPublisherPort(port);
            //FIXME time here is in milliseconds and MiddlewareMessageInfo is in nanoseconds
//            info.setTimeReceivedNanos(req.getTimeMillis() * (long)1E6);
            info.setTimeReceivedNanos(System.currentTimeMillis() * (long)1E6);
            info.setTimeSentNanos((long)msg.getTimestamp() * (long)1E9);
            info.setProperty(MessageInfo.TRANSPORT_MSG_KEY, "TCP");

	        for (MessageListener<MessageInfo, IMCMessage> lst : listeners) {
                try {
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

	    @Override
	    public int compareTo(TCPMessageProcessor o) {
	        return id.compareTo(o.id);
	    }
	}

	
	/**
	 * @param args
	 * @throws MiddlewareException 
	 */
	@SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
		ConfigFetch.initialize();

        String server = "127.0.0.1";
        int portServer = 6001;
        String server2 = "127.0.0.1";
        int portServer2 = 6002;

		ImcTcpTransport tcpT = new ImcTcpTransport(portServer, IMCDefinition.getInstance());
        ImcTcpTransport tcpT2 = new ImcTcpTransport(portServer2, IMCDefinition.getInstance());
		
		tcpT.addListener(new MessageListener<MessageInfo, IMCMessage>() {
		    @Override
		    public void onMessage(MessageInfo info, IMCMessage msg) {
		        info.dump(System.out);
		        msg.dump(System.out);
		    }
		});
        tcpT2.addListener(new MessageListener<MessageInfo, IMCMessage>() {
            @Override
            public void onMessage(MessageInfo info, IMCMessage msg) {
                info.dump(System.err);
                msg.dump(System.err);
            }
        });

        MessageDeliveryListener mdlT = new MessageDeliveryListener() {
            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                NeptusLog.pub().info("<###>>>> deliveryUnreacheable: "+ message.getAbbrev());
            }
            @Override
            public void deliveryTimeOut(IMCMessage message) {
                NeptusLog.pub().info("<###>>>> deliveryTimeOut: "+ message.getAbbrev());
            }
            @Override
            public void deliverySuccess(IMCMessage message) {
                NeptusLog.pub().info("<###>>>> deliverySuccess: "+ message.getAbbrev());
            }
            @Override
            public void deliveryError(IMCMessage message, Object error) {
                NeptusLog.pub().info("<###>>>> deliveryError: "+ message.getAbbrev() + " " + error);
            }
            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
                NeptusLog.pub().info("<###>>>> deliveryUncertain: "+ message.getAbbrev() + " " + msg);                
            }
        };
        MessageDeliveryListener mdlT2 = new MessageDeliveryListener() {
            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                System.err.println(">>> deliveryUnreacheable: "+ message.getAbbrev());
            }
            @Override
            public void deliveryTimeOut(IMCMessage message) {
                System.err.println(">>> deliveryTimeOut: "+ message.getAbbrev());
            }
            @Override
            public void deliverySuccess(IMCMessage message) {
                System.err.println(">>> deliverySuccess: "+ message.getAbbrev());
            }
            @Override
            public void deliveryError(IMCMessage message, Object error) {
                System.err.println(">>> deliveryError: "+ message.getAbbrev() + " " + error);
            }
            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
                System.err.println(">>> deliveryUncertain: "+ message.getAbbrev() + " " + msg);                
            }
        };


        IMCMessage msg=null;
        IMCMessage msgES=null;
        try {
            msg = IMCDefinition.getInstance().create("Abort");
            msgES = IMCDefinition.getInstance().create("EstimatedState");
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     
        msg.getHeader().setValue("src", 0x3c22);
        msgES.getHeader().setValue("src", 0x0015);
        
//        try { Thread.sleep(10000); } catch (InterruptedException e1) { }
//        
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcpT.sendMessage(server2, portServer2, msg, mdlT);
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcpT.sendMessage(server2, portServer2, msg, mdlT);
//
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcpT2.sendMessage(server, portServer, msgES, mdlT2);
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcpT2.sendMessage(server, portServer, msgES, mdlT2);
//
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcpT.sendMessage(server2, portServer2, msg, mdlT);
//        tcpT2.sendMessage(server, portServer, msgES, mdlT2);
//        tcpT.sendMessage(server2, portServer2, msg, mdlT);
//
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcpT2.stop();
//
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcpT.sendMessage(server2, portServer2, msg, mdlT);
	}
}
