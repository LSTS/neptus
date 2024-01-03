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
 * 2010/01/16
 */
package pt.lsts.neptus.comm.transports.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.transports.DeliveryListener.ResultEnum;
import pt.lsts.neptus.comm.transports.DeliveryResult;
import pt.lsts.neptus.comm.transports.IdPair;
import pt.lsts.neptus.util.ByteUtil;

/**
 * @author pdias
 * 
 */
public class UDPTransport {
    protected Set<UDPMessageListener> listeners = new LinkedHashSet<>();

    private final BlockingQueue<UDPNotification> receptionMessageList = new LinkedBlockingQueue<>();
    private final BlockingQueue<UDPNotification> sendMessageList = new LinkedBlockingQueue<>();

    private Thread sockedListenerThread = null;
    private Thread dispatcherThread = null;
    private final List<Thread> senderThreads = new ArrayList<>();
    private int numberOfSenderThreads = 1;

    private DatagramSocket sock;

    private final Map<String, InetAddress> solvedAddresses = new LinkedHashMap<>();

    private int bindPort = 6001;

    private int timeoutMillis = 1000;
    private int maxBufferSize = 65507;

    private boolean purging = false;

    private boolean broadcastEnable = false;
    private boolean broadcastActive = false;

    private boolean multicastEnable = false;
    private boolean multicastActive = false;

    private String multicastAddress = "224.0.75.69";

    private boolean isOnBindError = false;

    /**
	 * This will bind to port 6001. 
	 */
    public UDPTransport() {
        initialize();
    }

    /**
     * @param bindPort
     */
    public UDPTransport(int bindPort) {
        this(bindPort, 1);
    }

    /**
     * @param bindPort
     * @param numberOfSenderThreads
     */
    public UDPTransport(int bindPort, int numberOfSenderThreads) {
        setNumberOfSenderThreads(numberOfSenderThreads);
        setBindPort(bindPort);
        initialize();
    }

    public UDPTransport(boolean isBroadcastEnable, int bindPort, int numberOfSenderThreads) {
        setNumberOfSenderThreads(numberOfSenderThreads);
        setBindPort(bindPort);
        setBroadcastEnable(isBroadcastEnable);
        initialize();
    }

    public UDPTransport(boolean isBroadcastEnable, int bindPort) {
        this(isBroadcastEnable, bindPort, 1);
    }

    /**
     * @param multicastAddress
     * @param bindPort
     * @param numberOfSenderThreads
     */
    public UDPTransport(String multicastAddress, int bindPort, int numberOfSenderThreads) {
        setNumberOfSenderThreads(numberOfSenderThreads);
        setBindPort(bindPort);
        setMulticastAddress(multicastAddress);
        setMulticastEnable(true);
        initialize();
    }

    /**
     * @param multicastAddress
     * @param bindPort
     */
    public UDPTransport(String multicastAddress, int bindPort) {
        this(multicastAddress, bindPort, 1);
    }

    /**
	 * 
	 */
    private void initialize() {
        createReceivers();
        createSenders();
    }

    /**
     * @return the isOnBindError
     */
    public boolean isOnBindError() {
        return isOnBindError;
    }

    /**
     * @param isOnBindError the isOnBindError to set
     */
    private void setOnBindError(boolean isOnBindError) {
        this.isOnBindError = isOnBindError;
    }

    /**
     * @return
     */
    public int getBindPort() {
        return bindPort;
    }

    /**
     * @param bindPort
     */
    public void setBindPort(int bindPort) {
        this.bindPort = bindPort;
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
     * @return the multicastEnable
     */
    public boolean isMulticastEnable() {
        return multicastEnable;
    }

    /**
     * @param multicastEnable the multicastEnable to set
     */
    public void setMulticastEnable(boolean multicastEnable) {
        this.multicastEnable = multicastEnable;
    }

    /**
     * @return the multicastActive
     */
    protected boolean isMulticastActive() {
        return multicastActive;
    }

    /**
     * @param multicastActive the multicastActive to set
     */
    protected void setMulticastActive(boolean multicastActive) {
        this.multicastActive = multicastActive;
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
    }

    /**
     * @return the broadcastActive
     */
    protected boolean isBroadcastActive() {
        return broadcastActive;
    }

    /**
     * @param broadcastActive the broadcastActive to set
     */
    protected void setBroadcastActive(boolean broadcastActive) {
        this.broadcastActive = broadcastActive;
    }

    /**
     * @return the numberOfSenderThreads
     */
    public int getNumberOfSenderThreads() {
        return numberOfSenderThreads;
    }

    /**
     * @param numberOfSenderThreads the numberOfSenderThreads to set
     */
    public void setNumberOfSenderThreads(int numberOfSenderThreads) {
        this.numberOfSenderThreads = numberOfSenderThreads;
    }

    /**
     * @return the timeoutMillis
     */
    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * @param timeoutMillis the timeoutMillis to set
     */
    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * @return the maxBufferSize
     */
    public int getMaxBufferSize() {
        return maxBufferSize;
    }

    /**
     * @param maxBufferSize the maxBufferSize to set
     */
    public void setMaxBufferSize(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }

    /**
     * @param multicastAddress
     * @return
     */
    protected InetAddress resolveAddress(String multicastAddress) throws UnknownHostException {
        if (!solvedAddresses.containsKey(multicastAddress)) {
            solvedAddresses.put(multicastAddress, InetAddress.getByName(multicastAddress));
        }
        return solvedAddresses.get(multicastAddress);
    }

    /**
     * @return
     */
    public boolean reStart() {
        if (!(!isStopping() && !isRunning()))
            return false;
        purging = false;
        createReceivers();
        createSenders();
        return true;
    }

    /**
     * Interrupts all the sending threads abruptly.
     * 
     * @see {@link #purge()}
     */
    public void stop() {
        if (isRunning()) {
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

            int size = senderThreads.size();
            for (int i = 0; i < size; i++) {
                try {
                    senderThreads.get(0).interrupt();
                    senderThreads.remove(0); // shifts the right elements to the left
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e.getMessage());
                }
            }

            List<UDPNotification> toClearSen = new ArrayList<>();
            sendMessageList.drainTo(toClearSen);
            for (UDPNotification req : toClearSen) {
                informDeliveryListener(req, ResultEnum.Error, new Exception("Server shutdown!!"));
            }
        }
    }

    /**
     * Stops accepting new messages but waits until all the buffered messages are sent to the network before stopping
     * the sending thread(s).
     */
    public void purge() {
        purging = true;
        while (!receptionMessageList.isEmpty() || !sendMessageList.isEmpty()) {
            try {
                Thread.sleep(1000);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        stop();
    }

    /**
     * @return
     */
    public boolean isRunning() {
        if (senderThreads.size() > 0)
            return true;

        if (sockedListenerThread == null && dispatcherThread == null)
            return false;
        return true;
    }

    /**
     * @return
     */
    public boolean isRunningNormally() {
        if (isOnBindError())
            return false;
        if (senderThreads.size() == 0)
            return false;
        if (sockedListenerThread == null)
            return false;
        if (dispatcherThread == null)
            return false;

        return true;
    }

    /**
     * @return
     */
    public boolean isStopping() {
        if (isRunning() && purging)
            return true;
        return false;
    }

    /**
	 * 
	 */
    private void createSenders() {
        senderThreads.clear();
        for (int i = 0; i < this.numberOfSenderThreads; i++) {
            if (i == 0)
                senderThreads.add(getSenderThread(sock));
            else
                senderThreads.add(getSenderThread(null));
        }
    }

    /**
	 * 
	 */
    private void createReceivers() {
        setOnBindError(false);
        getSockedListenerThread();
        getDispatcherThread();
    }

    /**
     * @return
     */
    private Thread getSockedListenerThread() {
        if (sockedListenerThread == null) {
            Thread listenerThread = new Thread(UDPTransport.class.getSimpleName() + ": Listener Thread "
                    + this.hashCode()) {
                byte[] sBuffer = new byte[maxBufferSize];
                String multicastGroup = "";

                public synchronized void start() {
                    NeptusLog.pub().debug("Listener Thread Started");
                    try {
                        boolean useMulticast = isMulticastEnable();
                        sock = (!useMulticast) ? new DatagramSocket(null) : new MulticastSocket(null);
//                        if (useMulticast) {
//                            sock.setReuseAddress(true); // This may be a potential problem when opening two Neptus instances, we don't detect a bind error  
//                        }
                        sock.setReuseAddress(false);
                        if (bindPort != 0) {
                            sock.bind(new InetSocketAddress(bindPort));
                        }
                        else {
                            sock.bind(new InetSocketAddress(0));
                        }

                        try {
                            if (useMulticast) {
                                ((MulticastSocket) sock).joinGroup(resolveAddress(getMulticastAddress()));
                                multicastGroup = getMulticastAddress();
                            }
                            setMulticastActive(useMulticast);
                        }
                        catch (Exception e) {
                            NeptusLog.pub().warn("Multicast socket join :: " + e.getMessage());
                            setMulticastActive(false);
                        }

                        sock.setSoTimeout(timeoutMillis);
                        if (isBroadcastEnable()) {
                            try {
                                sock.setBroadcast(true);
                                setBroadcastActive(true);
                            }
                            catch (Exception e) {
                                NeptusLog.pub().warn(e.getMessage());
                                setBroadcastActive(false);
                            }
                        }
                    }
                    catch (Exception e) {
                        NeptusLog.pub().warn(e);
                        setOnBindError(true);
                        return;
                    }
                    finally {
                        if (isOnBindError()) {
                            try {
                                sock.disconnect();
                                sock.close();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().warn(e.getStackTrace());
                            }
                        }
                    }
                    super.start();
                }

                public void run() {
                    try {
                        while (!purging) {
                            DatagramPacket packet = new DatagramPacket(sBuffer, sBuffer.length);
                            try {
                                sock.receive(packet);
                                int lengthReceived = packet.getLength();
                                try {
                                    byte[] recBytes = Arrays.copyOf(sBuffer, lengthReceived);
                                    UDPNotification info = new UDPNotification(UDPNotification.RECEPTION,
                                            (InetSocketAddress) packet.getSocketAddress(), recBytes,
                                            System.currentTimeMillis());
                                    receptionMessageList.offer(info);
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                    // FIXME treat better this exception (pdias)
                                }
                            }
                            catch (SocketTimeoutException e) {
                                // NeptusLog.pub().warn(this + " Thread SocketTimeoutException");
                                // try { Thread.sleep(1500); } catch (Exception e1) { }
                                continue;
                            }
                            catch (Exception e) {
                                NeptusLog.pub().warn(e.getMessage());
                                e.printStackTrace();
                                // continue;
                            }
                            catch (Error e) {
                                NeptusLog.pub().warn(e.getMessage());
                                e.printStackTrace();
                                // continue;
                            }
                            // Thread.sleep(1);
                            // try { Thread.sleep(10); } catch (Exception e) { }
                        }
                    }
                    catch (Exception e) {
                        NeptusLog.pub().warn(e.getMessage());
                        // NeptusLog.pub().warn(this+" Thread interrupted");
                    }

                    NeptusLog.pub().info(this + " Thread Stopped");

                    if (isMulticastActive()) {
                        try {
                            // ((MulticastSocket)sock).leaveGroup(((MulticastSocket)sock).getInetAddress());
                            ((MulticastSocket) sock).leaveGroup(resolveAddress(multicastGroup));
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    sock.disconnect();
                    sock.close();
                    sock = null;
                    sockedListenerThread = null;
                }

            };
            listenerThread.setPriority(Thread.MIN_PRIORITY);
            listenerThread.setDaemon(true);
            listenerThread.start();
            sockedListenerThread = listenerThread;
        }
        return sockedListenerThread;
    }

    /**
     * @return
     */
    private Thread getDispatcherThread() {
        if (dispatcherThread == null) {
            Thread listenerThread = new Thread(UDPTransport.class.getSimpleName() + ": Dispacher Thread "
                    + this.hashCode()) {
                public synchronized void start() {
                    NeptusLog.pub().debug("Dispacher Thread Started");
                    super.start();
                }

                public void run() {
                    try {
                        while (!(purging && receptionMessageList.isEmpty())) {
                            UDPNotification req;
                            req = receptionMessageList.poll(1, TimeUnit.SECONDS);
                            if (req == null)
                                continue;

                            for (UDPMessageListener lst : listeners) {
                                try {
                                    lst.onUDPMessageNotification(req);
                                }
                                catch (ArrayIndexOutOfBoundsException e) {
                                    NeptusLog.pub().debug(
                                            "Dispacher Thread: ArrayIndexOutOfBoundsException: "
                                                    + "onUDPMessageNotification " + e.getMessage());
                                }
                                catch (Exception e) {
                                    String addStr = "";
                                    if (req != null) {
                                        if (req.getBuffer() != null) {
                                            addStr = ByteUtil.dumpAsHexToString(req.getAddress().toString(), req.getBuffer());
                                            if (addStr != null && !"".equalsIgnoreCase(addStr))
                                                addStr = "Buffer:\n" + addStr;
                                        }
                                    }
                                    NeptusLog.pub().warn(
                                            "Dispacher Thread: Exception: " + "onUDPMessageNotification "
                                                    + e.getMessage() + addStr, e);
                                }
                                catch (Error e) {
                                    NeptusLog.pub().fatal(
                                            "Dispacher Thread: Error: " + "onUDPMessageNotification " + e.getMessage(),
                                            e);
                                }
                            }
                        }
                    }
                    catch (InterruptedException e) {
                        NeptusLog.pub().debug(this + " Thread interrupted");
                    }

                    NeptusLog.pub().info(this + " Thread Stopped");
                    dispatcherThread = null;
                }
            };
            listenerThread.setPriority(Thread.MIN_PRIORITY + 1);
            listenerThread.setDaemon(true);
            listenerThread.start();
            dispatcherThread = listenerThread;
        }
        return dispatcherThread;
    }

    /**
     * @return
     */
    private Thread getSenderThread(final DatagramSocket sockToUseAlreadyOpen) {
        Thread senderThread = new Thread(UDPTransport.class.getSimpleName() + ": Sender Thread " + this.hashCode()) {

            DatagramSocket sock;
            DatagramPacket dgram;
            UDPNotification req;

            public synchronized void start() {
                NeptusLog.pub().debug("Sender Thread Started");
                try {
                    if (sockToUseAlreadyOpen != null)
                        sock = sockToUseAlreadyOpen;
                    else
                        sock = new DatagramSocket();
                    super.start();
                }
                catch (SocketException e) {
                    e.printStackTrace();
                }
            }

            public void run() {
                try {
                    while (!(purging && sendMessageList.isEmpty())) {
                        // req = sendmessageList.take();
                        req = sendMessageList.poll(1, TimeUnit.SECONDS);
                        if (req == null)
                            continue;
                        try {
                            dgram = new DatagramPacket(req.getBuffer(), req.getBuffer().length, req.getAddress());
                            if (req.getAddress().getPort() != 0) {
                                sock.send(dgram);
                                informDeliveryListener(req, ResultEnum.Success, null);
                            }
                            else
                                throw new Exception(req.getAddress() + " port is not valid");
                        }
                        catch (IOException e) {
                            NeptusLog.pub().debug(e + " :: " + req.getAddress());
                            // e.printStackTrace();
                            informDeliveryListener(req, ResultEnum.Error, e);
                        }
                        catch (Exception e) {
                            NeptusLog.pub().warn(e + " :: " + req.getAddress());
                            // e.printStackTrace();
                            informDeliveryListener(req, ResultEnum.Error, e);
                        }
                    }
                }
                catch (InterruptedException e) {
                    NeptusLog.pub().debug(this + " Thread interrupted");
                    informDeliveryListener(req, ResultEnum.Error, e);
                }

                NeptusLog.pub().info(this + " Sender Thread Stopped");
                senderThreads.remove(this);
            }
        };
        senderThread.setPriority(Thread.MIN_PRIORITY);
        senderThread.setDaemon(true);
        senderThread.start();
        return senderThread;
    }

    /**
     * @param listener
     * @return
     */
    public boolean addListener(UDPMessageListener listener) {
        synchronized (listeners) {
            boolean ret = listeners.add(listener);
            return ret;
        }
    }

    /**
     * @param listener
     * @return
     */
    public boolean removeListener(UDPMessageListener listener) {
        synchronized (listeners) {
            boolean ret = listeners.remove(listener);
            return ret;
        }
    }

    /**
     * Sends a message to the network
     * 
     * @param destination A valid hostname like "whale.fe.up.pt" or "127.0.0.1"
     * @param buffer
     * @return true meaning that the message was put on the send queue, and false if it was not put on the send queue.
     */
    public CompletableFuture<DeliveryResult> sendMessage(IdPair destination, byte[] buffer) {
        return sendMessage(destination.getHost(), destination.getPort(), buffer);
    }

    /**
     * Sends a message to the network
     * 
     * @param destination A valid hostname like "whale.fe.up.pt" or "127.0.0.1"
     * @param port The destination's port
     * @param buffer
     * @return true meaning that the message was put on the send queue, and false if it was not put on the send queue.
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
            UDPNotification req = new UDPNotification(UDPNotification.SEND, new InetSocketAddress(
                    resolveAddress(destination), port), buffer);
            req.setDeliveryListener(deliveryListener);
            sendMessageList.add(req);
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            if (deliveryListener != null)
                deliveryListener.complete(DeliveryResult.from(ResultEnum.Unreachable, e));
        }
        return deliveryListener;
    }

    /**
     * @param req
     * @param e
     */
    private void informDeliveryListener(UDPNotification req, ResultEnum result, Exception e) {
        if (req != null && req.getDeliveryListener() != null) {
            req.getDeliveryListener().complete(DeliveryResult.from(result, e));
        }
    }

    /**
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
        // Multicast Test
        // UDPTransport udpT = new UDPTransport("224.0.75.69", 6969);
        // udpT.addListener(new UDPMessageListener() {
        // @Override
        // public void onUDPMessageNotification(UDPNotification req) {
        // System.err
        // .println("Received "
        // + req.getBuffer().length
        // + " bytes from "
        // + req.getAddress()
        // + " :: "
        // + new String(req.getBuffer(), 0, req
        // .getBuffer().length));
        // }
        // });
        //
        // while (true) {
        // udpT.sendMessage("224.0.75.69", 6969, new String("Ptah is a God!!!").getBytes());
        // Thread.sleep(1000);
        // }

        // //Multicast Test2
        // final String multicastAddress = "224.0.75.69";
        // String multicastRangePortsStr = "6969-6972";
        // final int localport = 6969;
        // final int[] multicastPorts = CommUtil
        // .parsePortRangeFromString(multicastRangePortsStr);
        // class ThreadTest extends Thread {
        // String id = "";
        // public ThreadTest(String id) {
        // super();
        // this.id = id;
        // }
        // @Override
        // public void run() {
        // UDPTransport multicastUdpTransport;
        //
        // multicastUdpTransport = new UDPTransport(multicastAddress,
        // (multicastPorts.length == 0) ? localport
        // : multicastPorts[0]);
        // multicastUdpTransport.reStart();
        // if (multicastUdpTransport.isOnBindError()) {
        // for (int i = 1; i < multicastPorts.length; i++) {
        // multicastUdpTransport.stop();
        // multicastUdpTransport.setBindPort(multicastPorts[i]);
        // multicastUdpTransport.reStart();
        // if (!multicastUdpTransport.isOnBindError())
        // break;
        // }
        // }
        // multicastUdpTransport.addListener(new UDPMessageListener() {
        // @Override
        // public void onUDPMessageNotification(UDPNotification req) {
        // System.err.println("Received "
        // + req.getBuffer().length
        // + " bytes from "
        // + req.getAddress()
        // + " :: "
        // + new String(req.getBuffer(), 0, req
        // .getBuffer().length));
        // }
        // });
        //
        // for (int i = 0; i < 10; i++) {
        // for (int port : multicastPorts)
        // multicastUdpTransport.sendMessage(multicastAddress,
        // port, id.concat(" :: "+
        // multicastUdpTransport.getBindPort()).getBytes());
        // }
        //
        // }
        // };
        //
        // UDPTransport uup = new UDPTransport(6969, 1);
        //
        // ThreadTest tt1 = new ThreadTest("P1");
        // ThreadTest tt2 = new ThreadTest("P2");
        // tt1.start();
        // tt2.start();
        // for (int i = 0; i < 10; i++) {
        // for (int port : multicastPorts)
        // uup.sendMessage(multicastAddress,
        // port, "Bind obstructor".concat(" :: "+
        // uup.getBindPort()).getBytes());
        // }

        UDPTransport udpTB1 = new UDPTransport(7969, 1);
        final UDPTransport udpTB = new UDPTransport(true, 6001, 1);
        udpTB.addListener(new UDPMessageListener() {
            @Override
            public void onUDPMessageNotification(UDPNotification req) {
                System.err.println(udpTB.isBroadcastActive() + " Received " + req.getBuffer().length + " bytes from "
                        + req.getAddress() + " :: ");
                // + new String(req.getBuffer(), 0, req
                // .getBuffer().length));
                NeptusLog.pub().info("<###> "+udpTB.receptionMessageList.size());
            }
        });

        // while (true) {
        // udpTB1.sendMessage(null, 7901, new String("Ptah is a God!!!").getBytes());
        // Thread.sleep(1000);
        // }

    }

}
