/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Paulo Dias
 * 2011/01/17
 */
package pt.up.fe.dceg.neptus.util.comm.transports;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfoImpl;
import pt.up.fe.dceg.neptus.messages.listener.MessageListener;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.MessageDeliveryListener;
import pt.up.fe.dceg.neptus.util.comm.transports.DeliveryListener.ResultEnum;
import pt.up.fe.dceg.neptus.util.comm.transports.tcp.TCPMessageListener;
import pt.up.fe.dceg.neptus.util.comm.transports.tcp.TCPNotification;
import pt.up.fe.dceg.neptus.util.comm.transports.tcp.TCPTransport;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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
//	              System.out.println("ssssssssssssssssssss "+req.getTimeMillis());
			    String id = req.getAddress().toString();
//	              System.out.println("---id: "+id);
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
            boolean ret = getTcpTransport().sendMessage(destination, port,
                    baos.toByteArray(), listener);
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
//	                GzLsf2Llf.transformLSFStream("./conf/messages/IMC.xml", 
//	                        pis, TCPMessageProcessor.this, null);
	                try {
	                    //IMCInputStream iis = new IMCInputStream(pis);
	                    
                        while(!isInputClosed && pis.available() >= 0) { // the pis.available() not always when return '0' means end of stream
                            
                            if (pis.available() == 0) {
                                try { Thread.sleep(20); } catch (InterruptedException e) { }
                                continue;
                            }
                            try {
                                IMCMessage msg = imcDef.nextMessage(pis);
//                                msg.dump(System.out);
                                //double timeMillis = msg.getTimestampMillis();
                                msgArrived(/*(long) timeMillis,*/ msg);
                            }
                            catch (IOException e) {
                                if (!"Unrecognized Sync word: 00".equalsIgnoreCase(e.getMessage()))
                                        e.printStackTrace();
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
	     * @see pt.up.fe.dceg.neptus.util.comm.transports.tcp.TCPMessageListener#onTCPMessageNotification(pt.up.fe.dceg.neptus.util.comm.transports.tcp.TCPNotification)
	     */
	    @Override
	    public void onTCPMessageNotification(TCPNotification req) {
	        //    System.out.println("ssssssssssssssssssss "+req.getTimeMillis());
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
	        } catch (IOException e) {
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
//	     * @see pt.up.fe.dceg.neptus.util.lsf.gz.GzLsf2Llf.MessageListener#msgArrived(long, pt.up.fe.dceg.neptus.imc.IMCMessage)
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
                System.out.println(">>> deliveryUnreacheable: "+ message.getAbbrev());
            }
            @Override
            public void deliveryTimeOut(IMCMessage message) {
                System.out.println(">>> deliveryTimeOut: "+ message.getAbbrev());
            }
            @Override
            public void deliverySuccess(IMCMessage message) {
                System.out.println(">>> deliverySuccess: "+ message.getAbbrev());
            }
            @Override
            public void deliveryError(IMCMessage message, Object error) {
                System.out.println(">>> deliveryError: "+ message.getAbbrev() + " " + error);
            }
            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
                System.out.println(">>> deliveryUncertain: "+ message.getAbbrev() + " " + msg);                
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
        
        try { Thread.sleep(10000); } catch (InterruptedException e1) { }
        
        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
        tcpT.sendMessage(server2, portServer2, msg, mdlT);
        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
        tcpT.sendMessage(server2, portServer2, msg, mdlT);

        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
        tcpT2.sendMessage(server, portServer, msgES, mdlT2);
        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
        tcpT2.sendMessage(server, portServer, msgES, mdlT2);

        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
        tcpT.sendMessage(server2, portServer2, msg, mdlT);
        tcpT2.sendMessage(server, portServer, msgES, mdlT2);
        tcpT.sendMessage(server2, portServer2, msg, mdlT);

        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
        tcpT2.stop();

        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
        tcpT.sendMessage(server2, portServer2, msg, mdlT);
	}
}
