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
 * Author: Paulo Dias
 * 2010/05/01
 */
package pt.lsts.neptus.comm.transports.tcp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.transports.DeliveryListener.ResultEnum;
import pt.lsts.neptus.comm.transports.DeliveryResult;
import pt.lsts.neptus.comm.transports.IdPair;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 *
 */
public class TCPTransport {

	protected Set<TCPMessageListener> listeners = new LinkedHashSet<>();
	
	private BlockingQueue<TCPNotification> receptionMessageList = new LinkedBlockingQueue<>();
	private BlockingQueue<TCPNotification> sendMessageList = new LinkedBlockingQueue<>();
	
	private Thread sockedListenerThread = null;
	private Thread dispatcherThread = null;
	private Thread senderThread = null;
	
	
	private Map<String, InetAddress> solvedAddresses = new LinkedHashMap<>();

	private int bindPort = 7011;

    private boolean keepAlive = true;
    private int timeoutMillis = 5000;
	private int timeoutSelectorsMillis = 100;
	private int maxBufferSize = 65506;

	private boolean purging = false;
	
	/**
	 * Server channel for "select" operation.
	 */
	private ServerSocketChannel serverCh;

	/**
	 * Selector for "select" operation.
	 */
	private Selector selector;

	/**
	 * List of client SocketChannel handles.
	 */
	private final List<SocketChannel> clients = new ArrayList<>();

	private boolean isOnBindError = false;

	/**
	 * 
	 */
	public TCPTransport() {
		initialize();
	}

	public TCPTransport(int bindPort) {
		setBindPort(bindPort);
		initialize();
	}

	public TCPTransport(int bindPort, boolean keepAlive) {
		setKeepAlive(keepAlive);
		setBindPort(bindPort);
		initialize();
	}

	/**
	 * 
	 */
	private void initialize() {
		serverCh = null;
		selector = null;
		createReceivers();
		createSenders();
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	private void createSenders() {
        getSenderThread();
    }

    private void createReceivers() {
//		setOnBindError(false);
		getSockedListenerThread();
		getDispatcherThread();
	}


	public boolean reStart() {
		if (isConnected())
			return false;
		purging = false;
		initialize();
		return true;
	}
	
	   /**
     * @param multicastAddress
     * @return
     */
    protected InetAddress resolveAddress(String multicastAddress)
            throws UnknownHostException {
        if (!solvedAddresses.containsKey(multicastAddress)) {
            solvedAddresses.put(multicastAddress, InetAddress
                    .getByName(multicastAddress));
        }
        return solvedAddresses.get(multicastAddress);
    }


	/**
	 * Interrupts all the sending threads abruptly.
	 * @see {@link #purge()}
	 */
	public void stop() {
		if (isConnected()) {
			purging = true;
			
			if (sockedListenerThread != null) {
				sockedListenerThread.interrupt();
				sockedListenerThread = null;
			}
			synchronized (receptionMessageList) {
				receptionMessageList.clear();
			}
			if (dispatcherThread != null) {
				dispatcherThread.interrupt();
				dispatcherThread = null;
			}
            if (senderThread != null) {
                senderThread.interrupt();
                senderThread = null;
            }
			
//			int size = senderThreads.size();
//			for (int i = 0; i < size; i++) {
//				senderThreads.get(0).interrupt();
//				senderThreads.remove(0); //shifts the right elements to the left 
//			}
            
            List<TCPNotification> toClearSen = new ArrayList<>();
            sendMessageList.drainTo(toClearSen);
            for (TCPNotification req : toClearSen) {
                informDeliveryListener(req, ResultEnum.Error, new Exception("Server shutdown!!"));
            }
		}
	}

   /**
     * Stops accepting new messages but waits until all the buffered 
     * messages are sent to the network before stopping the sending thread(s).
     */
    public void purge() {
        purging = true;
        while (!receptionMessageList.isEmpty() || !sendMessageList.isEmpty()) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        stop();
    }

    /**
     * @param host
     * @param port
     * @return
     */
    private SocketChannel getEstablishedConnectionOrConnect (String host, int port) {
        SocketChannel channel = getEstablishedConnection(host, port);
        if (channel == null) {
            try {
                channel = createAndConnectSocketChannel(new InetSocketAddress(resolveAddress(host), port));
            }
            catch (UnknownHostException e) {
                NeptusLog.pub().error(e.getMessage());
                channel = createAndConnectSocketChannel(new InetSocketAddress(host, port));
            }
            if (channel != null) {
                synchronized (clients) {
                    clients.add(channel);                    
                }
            }
        }
        return channel;
    }
    

    /**
     * @param host
     * @param port
     * @return
     */
    private SocketChannel getEstablishedConnection(String host, int port) {
        SocketChannel channel = null;
        synchronized (clients) {
            for (SocketChannel channelTmp : clients) {
                try {
                    //NeptusLog.pub().info("<###> "+resolveAddress(host) + " | " + resolveAddress(host).toString().replaceFirst("^[a-zA-Z0-9._-]*(/)", "$1") + "  " +port + "   " +channelTmp.socket().getInetAddress() + " " + channelTmp.socket().getPort());
                    if ((resolveAddress(host).toString().equalsIgnoreCase(channelTmp.socket().getInetAddress().toString()) ||
                            resolveAddress(host).toString().replaceFirst("^[a-zA-Z0-9._-]*(/)", "$1").
                                    equalsIgnoreCase(channelTmp.socket().getInetAddress().toString()))
                            && port == channelTmp.socket().getPort()) {
                        channel = channelTmp;
                        break;
                    }
                }
                catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
//        if (channel != null) NeptusLog.pub().info("<###> "+host +"@" +port + "   " + channel + "   " + channel.socket().getInetAddress());
        return channel;
    }

    /**
     * @param host
     * @param port
     * @return
     */
    public boolean connectIfNotConnected (String host, int port) {
        return getEstablishedConnectionOrConnect(host, port) != null ? true : false;
    }
    
    /**
     * @param host
     * @param port
     * @return
     */
    public boolean isConnectionEstablished(String host, int port) {
        SocketChannel channel = getEstablishedConnection(host, port);
        if (channel == null)
            return false;
        else
            return true;
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
	}
	
	
	protected void connect() {
		try {
			serverCh = ServerSocketChannel.open();
			selector = Selector.open();
			serverCh.configureBlocking(false);
			serverCh.socket().setSoTimeout(timeoutMillis);
			// serverCh.socket().setReuseAddress(true); // Possible problem in reusing socket address!!!
			serverCh.socket().bind(new InetSocketAddress(getBindPort()));
			serverCh.register(selector, SelectionKey.OP_ACCEPT);
            isOnBindError = false;
		}
		catch (Exception e) {
		    if (e instanceof IOException)
		        isOnBindError = true;
			if (serverCh != null)
				disconnect();
		}
	}

	/**
	 * @param address
	 * @return
	 */
	private SocketChannel createAndConnectSocketChannel(InetSocketAddress address) {
	    try {
	        SocketChannel channel = SocketChannel.open();
            channel.socket().setSoTimeout(timeoutMillis);
            try {
                channel.socket().setKeepAlive(true);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
	        channel.configureBlocking(false);
	        channel.connect(address);
//	        boolean connect = channel.connect(address);
//	        if (!connect)
//	            return null;
	        while (!channel.finishConnect()) {
	            try { Thread.sleep(10); } catch (InterruptedException e1) { NeptusLog.pub().error(e1.getMessage()); }
	        }
            selector.wakeup();
//	        NeptusLog.pub().info("<###>rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr");
	        channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//	        NeptusLog.pub().info("<###>RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR");
	        return channel;
	    }
	    catch (Exception e) {
	        if (e instanceof NoRouteToHostException || e instanceof ConnectException) {
//	            System.err.print(e.toString() + ": " + address + "  ");
//	            System.err.flush();
	            NeptusLog.pub().debug(e.toString() + ": " + address);
	        }
	        else
	            NeptusLog.pub().error(e);
	        return null;
	    }
	}

	private boolean checkConnected() {
		return isConnected();
	}

	
	/**
	 * Disconnect and close the TCP server socket.
	 * 
	 * @throws Exception
	 *             if a network error occurs
	 */
	protected void disconnect() {
		isConnected();
		try {
			if (selector != null)
				selector.close();
            synchronized (clients) {
                for (SocketChannel channel : clients) {
                    try {
                        channel.close();
                        // safelyAlertAndCloseChannelToListeners(channel);
                    }
                    catch (IOException e) {
                        NeptusLog.pub().error(e.getStackTrace());
                        // Ignore error when closing client socket
                    }
                }
            }
			serverCh.close();
		} catch (Exception e) {
		    NeptusLog.pub().error(e);
		}
		serverCh = null;
		selector = null;
		synchronized (clients) {
		    clients.clear();
		}
		purging = true;
	}

//	private void safelyAlertAndCloseChannelToListeners(SocketChannel channel) {
//        System.err.println("111111111111111111");
//	    InetAddress dd = channel.socket().getInetAddress();
//        System.err.println("222222222222222222");
//        int pp = channel.socket().getPort();
//        System.err.println("333333333333333333");
//        TCPNotification info = new TCPNotification(
//                TCPNotification.RECEPTION,
//                new InetSocketAddress(dd,
//                        pp), true,
//                System.currentTimeMillis());
//        System.err.println("444444444444444444 "+clients.size());
//        //clients.remove(channel);
//        System.err.println("555555555555555555 "+clients.remove(channel));
//        try {
//            channel.close();
//        }
//        catch (IOException e) {
//         // Ignore error when closing client socket
////            e.printStackTrace();
//        }
//        System.err.println("666666666666666666");
//        receptionMessageList.offer(info);
//        System.err.println("777777777777777777");
//    }

	
	/**
	 * @return 
	 * 
	 */
	private boolean isConnected() {
		if (serverCh == null && sockedListenerThread == null
				&& dispatcherThread == null)
			return false;
		return true;
	}

	   /**
     * @return
     */
    public boolean isRunning() {
        return isConnected();
    }

    /**
     * @return
     */
    public boolean isRunningNormally() {
        if (isOnBindError())
            return false;
        if (serverCh == null || sockedListenerThread == null
                || dispatcherThread == null)
            return false;

        return true;
    }

	
	/**
     * @return the isOnBindError
     */
    public boolean isOnBindError() {
        return isOnBindError;
    }
	
	/**
	 * @param listener
	 * @return
	 */
	public boolean addListener(TCPMessageListener listener) {
		synchronized (listeners) {
			boolean ret = listeners.add(listener);
			return ret;
		}
	}

	/**
	 * @param listener
	 * @return
	 */
	public boolean removeListener(
			TCPMessageListener listener) {
		synchronized (listeners) {
			boolean ret = listeners.remove(listener);
			return ret;
		}
	}
	
	
	private Thread getSockedListenerThread() {
		if (sockedListenerThread == null) {
			Thread listenerThread = new Thread(TCPTransport.class.getSimpleName() + ": Listener Thread " + this.hashCode()) {			
				byte[] sBuffer = new byte[maxBufferSize];
				
				public synchronized void start() {
					NeptusLog.pub().debug("Listener Thread Started");
					try {
						connect();
					} catch (Exception e) {
						NeptusLog.pub().error(e);
						//setOnBindError(true);
						return;
					}
					super.start();			
				}
				
				public void run() {
					try {
					    long time = System.currentTimeMillis();
					    long previousConnectedClients = -1;
						while (!purging) {
							checkConnected();
							int lengthReceived = 0;
							try {
							    if (System.currentTimeMillis() - time > 10000 && previousConnectedClients != clients.size()) {
							        //NeptusLog.pub().info("<###> "+getBindPort() + " clients " + clients.size());
                                    NeptusLog.pub().debug(TCPTransport.class.getSimpleName()
                                            + ": Listener Thread " + getBindPort() + " now " + clients.size()
                                            + " clients");
							        time = System.currentTimeMillis();
							        previousConnectedClients = clients.size();
							    }
							    
							    // To clear and detect closed channels
							    synchronized (clients) {
							        for (SocketChannel channel : clients.toArray(new SocketChannel[0])) {
                                        if (!channel.isOpen()) {
                                            channel.close();
                                            clients.remove(channel);
                                        }
                                        else {
    							            try {
    							                Object obj = channel.keyFor(selector).attachment();
    							                if (obj != null) {
    
    							                    long lastTime = (Long) obj;
    							                    if (System.currentTimeMillis() - lastTime > 20000) {
    							                        channel.close();
    							                        channel.keyFor(selector).cancel();
//    							                        NeptusLog.pub().info("<###>CLEAN  ...........................................");
    							                    }
    							                }
    							            }
    							            catch (Exception e) {
    							                e.printStackTrace();
    							            }
                                        }
//	                                    NeptusLog.pub().info("<###> "+channel.socket() + "  c " + channel.socket().isConnected() + "  " + channel.isOpen() + "    " + channel.isConnected());
							        }
							    }
							    
								if (selector.select(timeoutSelectorsMillis) == 0)
									continue;
								
								Iterator<SelectionKey> it = selector.selectedKeys().iterator();
								while (it.hasNext()) {
									SelectionKey key = it.next();
									it.remove();
									if (!key.isValid()) {
										if (key.channel() instanceof SocketChannel) {
											key.channel().close();
											synchronized (clients) {
											    clients.remove(key.channel());
											}
										}
									}
									else if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
										// New client connection
										SocketChannel channel = serverCh.accept();
										if (channel == null)
											continue;
										channel.configureBlocking(false);
										channel.socket().setSoTimeout(timeoutMillis);
										try {
                                            channel.socket().setKeepAlive(true);
                                        }
                                        catch (Exception e) {
                                            e.printStackTrace();
                                        }
										channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//										InetAddress dd = channel.socket().getInetAddress();
//										int pp = channel.socket().getPort();
//										NeptusLog.pub().info("<###> "+getBindPort()+"======== " + dd.getHostAddress() + "@" + pp);
                                        key.attach(System.currentTimeMillis());
										synchronized (clients) {
										    clients.add(channel);
										}
									}
									else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
										// Data available
										SocketChannel channel = (SocketChannel) key.channel();
										InetAddress dd = channel.socket().getInetAddress();
										int pp = channel.socket().getPort();
										TCPNotification info;
										try {
											lengthReceived = channel.read(ByteBuffer.wrap(sBuffer, 0, sBuffer.length));
											
											if (lengthReceived != -1) {
												byte[] recBytes = Arrays.copyOf(sBuffer, lengthReceived);
                                                info = new TCPNotification(TCPNotification.RECEPTION,
                                                        new InetSocketAddress(dd, pp), recBytes,
                                                        System.currentTimeMillis());
											}
											else {
                                                info = new TCPNotification(TCPNotification.RECEPTION,
                                                        new InetSocketAddress(dd, pp), true, System.currentTimeMillis());
												synchronized (clients) {
												    clients.remove(channel);
												}
												channel.close();
											}
											receptionMessageList.offer(info);
		                                    //NeptusLog.pub().info("<###>>>> Channel: "+channel+ " READ " + lengthReceived + "B");
                                            NeptusLog.pub().debug(TCPTransport.class.getSimpleName()
                                                    + ": Listener Thread " + ">>> Channel: " + channel + " READ "
                                                    + lengthReceived + "B");
										} catch (IOException e) {
										    synchronized (clients) {
										        clients.remove(channel);
										    }
//											System.err.println("Lost touch with " + channel
//													+ " -> " + e.getMessage());
                                            NeptusLog.pub().info(TCPTransport.class.getSimpleName()
                                                    + ": Listener Thread " + "Lost touch with " + channel + " -> "
                                                    + e.getMessage());
                                            channel.close();
                                            info = new TCPNotification(TCPNotification.RECEPTION,
                                                    new InetSocketAddress(dd, pp), true, System.currentTimeMillis());
											receptionMessageList.offer(info);
										}
                                        key.attach(System.currentTimeMillis());
									}
                                    else if ((key.readyOps() & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                                        try {
                                            boolean sendAck = true;
                                            Object obj = key.attachment();
                                            if (obj != null) {
                                                try {
                                                    long lastTime = (Long) obj;
                                                    if (System.currentTimeMillis() - lastTime < 2000) {
                                                        sendAck = false;
                                                    }
                                                }
                                                catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            if (sendAck && isKeepAlive()) {
                                                SocketChannel channel = (SocketChannel) key.channel();
                                                //ByteBuffer bf = ByteBuffer.wrap(new byte[] { (byte) 0xFFFF });
                                                ByteBuffer bf = ByteBuffer.wrap(new byte[0]);
                                                channel.write(bf);
                                                key.attach(System.currentTimeMillis());
                                            }
                                        }
                                        catch (IOException e) {
                                            NeptusLog.pub().error(e.getMessage());
                                            SocketChannel channel = (SocketChannel) key.channel();
                                            channel.close();
                                            key.cancel();
                                        }
                                    }
									try { Thread.sleep(10); } catch (Exception e) { NeptusLog.pub().error(e.getMessage());}
								}
							} 
							catch (IOException e) {
		                        NeptusLog.pub().error(e);
							}
						}
					}
					catch (Exception e) {
						NeptusLog.pub().error(e);
						e.printStackTrace();
						//NeptusLog.pub().warn(this+" Thread interrupted");
					}
					NeptusLog.pub().warn(this + " Thread Stopped");
					disconnect();
				}
				
			};
			listenerThread.setPriority(Thread.MIN_PRIORITY);
			listenerThread.setDaemon(true);
			listenerThread.start();
			sockedListenerThread = listenerThread;
		}
		return sockedListenerThread;
	}

	
	private Thread getDispatcherThread() {
		if (dispatcherThread == null) {
			Thread listenerThread = new Thread(TCPTransport.class.getSimpleName() + ": Dispacher Thread " + this.hashCode()) {			
				public synchronized void start() {
					NeptusLog.pub().debug("Dispacher Thread Started");
					super.start();				
				}
				
				public void run() {
					try {
						while (!(purging && receptionMessageList.isEmpty())) {
//							TCPNotification req = receptionMessageList.take();
							TCPNotification req = receptionMessageList.poll(1, TimeUnit.SECONDS);
                            if (req == null)
                                continue;
							synchronized (listeners) {
								for (TCPMessageListener lst : listeners) {
									try {
//								    ByteUtil.dumpAsHex("" + req.getAddress(), req.getBuffer(), System.out);
										lst.onTCPMessageNotification(req);
									} catch (Exception e) {
										e.printStackTrace();
									}
									catch (Error e) {
										e.printStackTrace();
									}
								}
							}
						}
					}
					catch (InterruptedException e) {
						NeptusLog.pub().warn(e.getMessage());
					}
					
					NeptusLog.pub().info(this + " Thread Stopped");
				}
			};
			listenerThread.setPriority(Thread.MIN_PRIORITY+1);
			listenerThread.setDaemon(true);
			listenerThread.start();
			dispatcherThread = listenerThread;
		}
		return dispatcherThread;
	}

	private Thread getSenderThread() {
	    if (senderThread == null) {
	        senderThread = new Thread(TCPTransport.class.getSimpleName() + ": Sender Thread " + this.hashCode()) {

	            TCPNotification req;

	            public synchronized void start() {
	                NeptusLog.pub().info("Sender Thread Started");
	                super.start();
	            }

	            public void run() {
	                try {
	                    while (!(purging && sendMessageList.isEmpty())) {
//	                        req = sendmessageList.take();
                            req = sendMessageList.poll(1, TimeUnit.SECONDS);
                            if (req == null)
                                continue;
	                        SocketChannel channel = null;
	                        try {
                                channel = getEstablishedConnectionOrConnect(req
                                        .getAddress().getHostName(), req.getAddress().getPort());
	                            if (channel == null) {
                                    informDeliveryListener(req, ResultEnum.Error, new IOException(
                                            "Not able to get a connection to "
                                                    + req.getAddress().getHostName() + ":"
                                                    + req.getAddress().getPort()));
	                                continue; //FIXME
	                            }
	                            
	                            ByteBuffer bbuf = ByteBuffer.wrap(req.getBuffer());
	                            int writtenBytes = channel.write(bbuf);
//	                            NeptusLog.pub().info("<###>......... " + writtenBytes + "   ");
	                            if (writtenBytes != req.getBuffer().length) {
	                                informDeliveryListener(req, ResultEnum.Error, null);
                                    synchronized (clients) {
                                        try {
                                            channel.close();
                                        }
                                        catch (IOException e1) {
                                            NeptusLog.pub().warn(e1.getMessage());
                                        }
                                        clients.remove(channel);
                                    }
	                            }
	                            else {
	                                informDeliveryListener(req, ResultEnum.Success, null);
	                            }
	                        }
	                        catch (Exception e) {
	                            NeptusLog.pub().error(e);
	                            //e.printStackTrace();
	                            informDeliveryListener(req, ResultEnum.Error, e);
                                if (channel != null) {
                                    synchronized (clients) {
                                        try {
                                            channel.close();
                                        }
                                        catch (IOException e1) {
                                            e1.printStackTrace();
                                        }
                                        clients.remove(channel);
                                    }
                                }
	                        }
	                    }
	                }
	                catch (InterruptedException e) {
	                    NeptusLog.pub().warn(this + " Thread interrupted");
	                    informDeliveryListener(req, ResultEnum.Error, e);
	                }

	                NeptusLog.pub().debug(this + " Sender Thread Stopped");
	            }
	        };
	        senderThread.setPriority(Thread.MIN_PRIORITY);
	        senderThread.setDaemon(true);
	        senderThread.start();
	    }
	    return senderThread;
	}
	
   /**
     * Sends a message to the network
     * @param buffer
     * @return true meaning that the message was put on the send queue, and 
     *          false if it was not put on the send queue.
     */
	public CompletableFuture<Void> sendMessage(byte[] buffer) {
		List<CompletableFuture<DeliveryResult>> listFutures = new ArrayList<>();
		clients.stream().forEach(sc -> {
			if (!sc.isOpen())
				return;
			try {
				SocketAddress rAddr = sc.getRemoteAddress();
				if (rAddr instanceof InetSocketAddress) {
					CompletableFuture<DeliveryResult> cf = sendMessage(IdPair.from((InetSocketAddress) rAddr), buffer);
					listFutures.add(cf);
				}
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		return CompletableFuture.allOf(listFutures.toArray(new CompletableFuture[0]));
	}

	public CompletableFuture<DeliveryResult> sendMessage(IdPair destination, byte[] buffer) {
		return sendMessage(destination.getHost(), destination.getPort(), buffer);
	}

	/**
	  * Sends a message to the network
	  * @param destination A valid hostname like "dummy.com" or "127.0.0.1"
	  * @param port The destination's port
	  * @param buffer
	  * @return true meaning that the message was put on the send queue, and
	  *          false if it was not put on the send queue.
	  */
    public CompletableFuture<DeliveryResult> sendMessage(String destination, int port, byte[] buffer) {
        if (purging) {
            String txt = "Not accepting any more messages. IMCMessenger is terminating";
            NeptusLog.pub().warn(txt);
            return CompletableFuture.completedFuture(DeliveryResult.from(
					ResultEnum.UnFinished, new IOException(txt)));
        }
        CompletableFuture<DeliveryResult> deliveryListener = new CompletableFuture<>();
        try {
            TCPNotification req = new TCPNotification(TCPNotification.SEND,
                    new InetSocketAddress(resolveAddress(destination), port),
                    buffer);
            req.setDeliveryListener(deliveryListener);
            sendMessageList.add(req);
        } 
        catch (UnknownHostException e) {
            NeptusLog.pub().warn(e.getMessage());
            if (deliveryListener != null)
                deliveryListener.complete(DeliveryResult.from(ResultEnum.Unreachable, e));
        }

        return deliveryListener;
    }

    /**
     * @param req 
     * @param e
     */
    private void informDeliveryListener(TCPNotification req, ResultEnum result, Exception e) {
        if (req != null && req.getDeliveryListener() != null) {
            req.getDeliveryListener().complete(DeliveryResult.from(result, e));
        }
    }

    /**
     * @return
     */
    public long getActiveNumberOfConnections() {
        return clients.size();
    }
    
    /**
     * Used in main only for test
     */
    private static class TCPMessageProcessor implements TCPMessageListener, Comparable<TCPMessageProcessor> {
	    String id = "";
        PipedOutputStream pos;
        PipedInputStream pis;
        
        // Needed because the pis.available() not always when return '0' means end of stream
        boolean isInputClosed = false;

        /**
         * 
         */
        public TCPMessageProcessor(String id) {
            this.id = id;
            pos = new PipedOutputStream();
            try {
                pis = new PipedInputStream(pos);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            final IMCDefinition imcDef = IMCDefinition.getInstance();
            new Thread() {
                @Override
                public void run() {
                    try {
                        while(!isInputClosed && pis.available() >= 0) { // the pis.available() not always when return '0' means end of stream
//                            NeptusLog.pub().info("<###>pis.available()" + pis.available());
                            if (pis.available() == 0) {
                                try { Thread.sleep(20); } catch (InterruptedException e) { }
                                continue;
                            }
                            try {
                                IMCMessage msg = imcDef.nextMessage(pis);
                                if (msg != null) {
                                    msg.dump(System.out);
                                    //double timeMillis = msg.getTimestampMillis();
                                    msgArrived(/*(long) timeMillis,*/ msg);
                                }
                            }
//                            catch (EOFException e) {
//                                if (isInputClosed)
//                                    break;
//                            }
                            catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
//                            byte[] ba = new byte[pis.available()];
//                            if (ba.length > 0) {
//                            pis.read(ba);
//                            ByteUtil.dumpAsHex(ba, System.out);
//                            }
                        }
                    }
                    catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    NeptusLog.pub().info("<###>pis.available()------------");
                    try {
                        NeptusLog.pub().info("<###>pis.available()" + pis.available());
                    }
                    catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        
//        /**
//         * @return the id
//         */
//        public String getId() {
//            return id;
//        }
        
        @Override
        public void onTCPMessageNotification(TCPNotification req) {
            try {
                if (req.isEosReceived()) {
                    pos.flush();
                    pos.close();
                    isInputClosed = true;
                    NeptusLog.pub().info("<###>POS Closed");
                }
                else
                    pos.write(req.getBuffer());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        @Override
        public void msgArrived(/*long timeStampMillis,*/ IMCMessage message) {
            message.dump(System.out);
        }

        @Override
        public int compareTo(TCPMessageProcessor o) {
            return id.compareTo(o.id);
        }
	}
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception {
		ConfigFetch.initialize();
		
        final HashMap<String, TCPMessageProcessor> listProc = new HashMap<String, TCPTransport.TCPMessageProcessor>();
		
//		final PipedOutputStream pos = new PipedOutputStream();
//		PipedInputStream pis = new PipedInputStream(pos);
		TCPTransport tcp = new TCPTransport(8082);
		tcp.addListener( new TCPMessageListener() {
			
			@Override
			public void onTCPMessageNotification(TCPNotification req) {
//				NeptusLog.pub().info("<###>ssssssssssssssssssss "+req.getTimeMillis());
			    String id = req.getAddress().toString();
//			    NeptusLog.pub().info("<###>---id: "+id);
			    TCPMessageProcessor proc = listProc.get(id);
			    if (proc == null) {
			        proc = new TCPMessageProcessor(id);
			        listProc.put(id, proc);
			    }
			    if (req.isEosReceived())
			        listProc.remove(id);
			    proc.onTCPMessageNotification(req);
                ByteUtil.dumpAsHex(req.getBuffer(), System.out);
			}
		});
		
		TCPTransport tcp2 = new TCPTransport(8083);
		tcp2.addListener( new TCPMessageListener() {
		    @Override
		    public void onTCPMessageNotification(TCPNotification req) {
		        String id = req.getAddress().toString();
                TCPMessageProcessor proc = listProc.get(id);
		        if (proc == null) {
		            proc = new TCPMessageProcessor(id);
		            listProc.put(id, proc);
		        }
		        if (req.isEosReceived())
		            listProc.remove(id);
		        proc.onTCPMessageNotification(req);
		        ByteUtil.dumpAsHex(req.getBuffer(), System.err);
		    }
		});

		IMCMessage msg = IMCDefinition.getInstance().create("Heartbeat");
        IMCMessage msgES = IMCDefinition.getInstance().create("EstimatedState");
//        StandardSerializationBuffer sb = new StandardSerializationBuffer(300);
//        StandardSerializationBuffer sb2 = new StandardSerializationBuffer(300);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IMCOutputStream imcos = new IMCOutputStream(baos); 
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        IMCOutputStream imcos2 = new IMCOutputStream(baos2); 
        msg.getHeader().setValue("src", 0x3c22);
        msgES.getHeader().setValue("src", 0x0015);
        int size = msg.serialize(imcos);
        int size2 = msgES.serialize(imcos2);
//
//        try { Thread.sleep(10000); } catch (InterruptedException e1) { }
//
//        NeptusLog.pub().info("<###>Start --------------------------------------------");
//		try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//		tcp.sendMessage("127.0.0.1", 8083, baos.toByteArray(), null);
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcp.sendMessage("127.0.0.1", 8083, baos.toByteArray(), null);
//
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcp2.sendMessage("127.0.0.1", 8082, baos2.toByteArray(), null);
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcp2.sendMessage("127.0.0.1", 8082, baos2.toByteArray(), null);
//
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcp.sendMessage("127.0.0.1", 8083, baos.toByteArray(), null);
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcp.sendMessage("127.0.0.1", 8083, baos.toByteArray(), null);
//
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcp.sendMessage("127.0.0.1", 8083, baos.toByteArray(), null);
//        tcp2.sendMessage("127.0.0.1", 8082, baos2.toByteArray(), null);
//        tcp.sendMessage("127.0.0.1", 8083, baos.toByteArray(), null);
//
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcp2.stop();
//
//        try { Thread.sleep(5000); } catch (InterruptedException e1) { }
//        tcp.sendMessage("127.0.0.1", 8083, baos.toByteArray(), null);
	}
}
