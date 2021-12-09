package pt.lsts.neptus.comm.transports.dtls;


import java.io.*;
import java.nio.*;
import java.net.*;
import java.util.*;
import java.security.*;
import javax.net.ssl.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.xml.bind.DatatypeConverter;


import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.transports.udp.UDPNotification;
import pt.lsts.neptus.comm.transports.udp.UDPTransport;



public class DTLSTransport {

    private static int MAX_HANDSHAKE_LOOPS = 200;
    private static int MAX_APP_READ_LOOPS = 60;
//    private static int SOCKET_TIMEOUT = 10 * 1000; // in millis
    private static int BUFFER_SIZE = 4096;
//    private static int MAXIMUM_PACKET_SIZE = 4096;
    private static String SERVER_ADDRESS = "10.0.10.60";
    private static int SERVER_PORT = 6003;
    private static int CLIENT_PORT = 6004;

    /*
     * The following is to set up the keystores.
     */
    // starting point for the path is the current execution directory
    // which is home directory of neptus repository
    private static String pathToStores = "../etc";
    private static String lstsKeyStoreFile = "lsts-keystore.jks";
    private static String lstsTrustStoreFile = "lsts-truststore.jks";
    private static String lstsKeyFilename =
            System.getProperty("test.src", ".") + "/" + pathToStores +
                    "/" + lstsKeyStoreFile;
    private static String lstsTrustFilename =
            System.getProperty("test.src", ".") + "/" + pathToStores +
                    "/" + lstsTrustStoreFile;

    private static Exception clientException = null;
    private static Exception serverException = null;

    private static ByteBuffer serverApp =
            ByteBuffer.wrap("Hi Client, I'm Server".getBytes());
    private static ByteBuffer clientApp =
            ByteBuffer.wrap("Hi Server, I'm Client".getBytes());

    private int bindPort;
    private int serverPort;
    private String serverAddress = null;
    private boolean isOnBindError = false;
    private Thread sockedListenerThread = null;
    private int maxBufferSize = 65507;
    private DatagramSocket sock;
    private static int timeoutMillis = 10 * 1000; // in millis
    private InetSocketAddress serverSocketAddr = null;
    private SSLEngine engine = null;
    private static int maximumPacketSize = 4096;
    //IMC list of received DTLS messages?? i think
    private LinkedBlockingQueue<UDPNotification> receptionMessageList = new LinkedBlockingQueue<UDPNotification>();
    private boolean purging = false;
    /**
     * This will bind to port 6001.
     */
    public DTLSTransport() {
        initialize();
    }

    /**
     * @param numberOfSenderThreads
     * @param inetAddress: Server address to connect to
     */
    public DTLSTransport(int numberOfSenderThreads, String inetAddress) {
//        setNumberOfSenderThreads(numberOfSenderThreads);
        Pattern p = Pattern.compile("^\\s*(.*?):(\\d+)\\s*$");
        Matcher m = p.matcher(inetAddress);
        if (m.matches()) {
            setServerPort(Integer.parseInt(m.group(2)));
            setServerAddress(m.group(1));
        }

        initialize();
    }

    /**
     * @param bindPort
     */
    public void setBindPort(int bindPort) {
        this.bindPort = bindPort;
    }

    /**
     * @param serverPort
     */
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * @param serverAddress
     */
    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    /**
     *
     */
    private void initialize() {
        createReceivers();
//        createSenders();
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
     *
     */
    private void createReceivers() {
        setOnBindError(false);
        getSockedListenerThread();
//        getDispacherThread();
    }

    // get DTSL context
    SSLContext getDTLSContext() throws Exception {
        /*keystore is where the client certificates and private keys are stored*/
        KeyStore ks = KeyStore.getInstance("JKS");
        /*truststore is where CA certificates are stored*/
        KeyStore ts = KeyStore.getInstance("JKS");

        char[] passphrase = "mtlsts".toCharArray();

        try (FileInputStream fis = new FileInputStream(lstsKeyFilename)) {
            ks.load(fis, passphrase);
        }

        try (FileInputStream fis = new FileInputStream(lstsTrustFilename)) {
            ts.load(fis, passphrase);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        SSLContext sslCtx = SSLContext.getInstance("DTLS");

        sslCtx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslCtx;
    }

    /* Create ssl engine
     * @isClient define if it is the client or server side
     */
    SSLEngine createSSLEngine(boolean isClient) throws Exception {
        SSLContext context = getDTLSContext();
        SSLEngine engine = context.createSSLEngine();

        SSLParameters paras = engine.getSSLParameters();
        paras.setMaximumPacketSize(maximumPacketSize);

        engine.setUseClientMode(isClient);
        engine.setSSLParameters(paras);

        return engine;
    }

    // run delegated tasks
    void runDelegatedTasks(SSLEngine engine) throws Exception {
        Runnable runnable;
        while ((runnable = engine.getDelegatedTask()) != null) {
            runnable.run();
        }

        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
        if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            throw new Exception("handshake shouldn't need additional tasks");
        }
    }

    DatagramPacket createHandshakePacket(byte[] ba, SocketAddress socketAddr) {
        return new DatagramPacket(ba, ba.length, socketAddr);
    }

    // produce handshake packets
    boolean produceHandshakePackets(SSLEngine engine, SocketAddress socketAddr,
                                    String side, List<DatagramPacket> packets) throws Exception {

        boolean endLoops = false;
        int loops = MAX_HANDSHAKE_LOOPS / 2;
        while (!endLoops &&
                (serverException == null) && (clientException == null)) {

            if (--loops < 0) {
                throw new RuntimeException(
                        "Too much loops to produce handshake packets");
            }

            ByteBuffer oNet = ByteBuffer.allocate(32768);
            ByteBuffer oApp = ByteBuffer.allocate(0);
            SSLEngineResult r = engine.wrap(oApp, oNet);
            oNet.flip();

            SSLEngineResult.Status rs = r.getStatus();
            SSLEngineResult.HandshakeStatus hs = r.getHandshakeStatus();
            NeptusLog.pub().info(side, "----produce handshake packet(" +
                    loops + ", " + rs + ", " + hs + ")----");
            if (rs == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                // the client maximum fragment size config does not work?
                throw new Exception("Buffer overflow: " +
                        "incorrect server maximum fragment size");
            } else if (rs == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                NeptusLog.pub().info(side,
                        "Produce handshake packets: BUFFER_UNDERFLOW occured");
                NeptusLog.pub().info(side,
                        "Produce handshake packets: Handshake status: " + hs);
                // bad packet, or the client maximum fragment size
                // config does not work?
                if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                    throw new Exception("Buffer underflow: " +
                            "incorrect server maximum fragment size");
                } // otherwise, ignore this packet
            } else if (rs == SSLEngineResult.Status.CLOSED) {
                throw new Exception("SSLEngine has closed");
            } else if (rs == SSLEngineResult.Status.OK) {
                // OK
            } else {
                throw new Exception("Can't reach here, result is " + rs);
            }

            // SSLEngineResult.Status.OK:
            if (oNet.hasRemaining()) {
                byte[] ba = new byte[oNet.remaining()];
                oNet.get(ba);
                DatagramPacket packet = createHandshakePacket(ba, socketAddr);
                packets.add(packet);
            }

            if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
                NeptusLog.pub().info(side, "Produce handshake packets: "
                        + "Handshake status is FINISHED, finish the loop");
                return true;
            }

            boolean endInnerLoop = false;
            SSLEngineResult.HandshakeStatus nhs = hs;
            while (!endInnerLoop) {
                if (nhs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                    runDelegatedTasks(engine);
                } else if (nhs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP ||
                        nhs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN ||
                        nhs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {

                    endInnerLoop = true;
                    endLoops = true;
                } else if (nhs == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                    endInnerLoop = true;
                } else if (nhs == SSLEngineResult.HandshakeStatus.FINISHED) {
                    throw new Exception(
                            "Unexpected status, SSLEngine.getHandshakeStatus() "
                                    + "shouldn't return FINISHED");
                } else {
                    throw new Exception("Can't reach here, handshake status is "
                            + nhs);
                }
                nhs = engine.getHandshakeStatus();
            }
        }

        return false;
    }

    // retransmission if timeout
    boolean onReceiveTimeout(SSLEngine engine, SocketAddress socketAddr,
                             String side, List<DatagramPacket> packets) throws Exception {

        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
        if (hs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            return false;
        } else {
            // retransmission of handshake messages
            return produceHandshakePackets(engine, socketAddr, side, packets);
        }
    }

    /* handshake
     * @param engine
     * @param socket Datagram socket
     * @param peerAddr IP + port from server
     * @param side: client or server, currently only client possible
     */
    void handshake(SSLEngine engine, DatagramSocket socket,
                   SocketAddress peerAddr, String side) throws Exception {

        boolean endLoops = false;
        int loops = MAX_HANDSHAKE_LOOPS;
        engine.beginHandshake();
        while (!endLoops &&
                (serverException == null) && (clientException == null)) {

            if (--loops < 0) {
                throw new RuntimeException(
                        "Too much loops to produce handshake packets");
            }

            SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
            NeptusLog.pub().info(side, "=======handshake(" + loops + ", " + hs + ")=======");
            if (hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP ||
                    hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN) {

                NeptusLog.pub().info(side, "Receive DTLS records, handshake status is " + hs);

                ByteBuffer iNet;
                ByteBuffer iApp;
                if (hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
                    byte[] buf = new byte[BUFFER_SIZE];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    try {
                        socket.receive(packet);
                    } catch (SocketTimeoutException ste) {
                        NeptusLog.pub().info(side, "Warning: " + ste);

                        List<DatagramPacket> packets = new ArrayList<>();
                        boolean finished = onReceiveTimeout(
                                engine, peerAddr, side, packets);

                        NeptusLog.pub().info(side, "Reproduced " + packets.size() + " packets");
                        for (DatagramPacket p : packets) {
                            // printHex("Reproduced packet",
                            //     p.getData(), p.getOffset(), p.getLength());
                            socket.send(p);
                        }

                        if (finished) {
                            NeptusLog.pub().info(side, "Handshake status is FINISHED "
                                    + "after calling onReceiveTimeout(), "
                                    + "finish the loop");
                            endLoops = true;
                        }

                        NeptusLog.pub().info(side, "New handshake status is "
                                + engine.getHandshakeStatus());

                        continue;
                    }

                    iNet = ByteBuffer.wrap(buf, 0, packet.getLength());
                    iApp = ByteBuffer.allocate(BUFFER_SIZE);
                } else {
                    iNet = ByteBuffer.allocate(0);
                    iApp = ByteBuffer.allocate(BUFFER_SIZE);
                }

                SSLEngineResult r = engine.unwrap(iNet, iApp);
                SSLEngineResult.Status rs = r.getStatus();
                hs = r.getHandshakeStatus();
                if (rs == SSLEngineResult.Status.OK) {
                    // OK
                } else if (rs == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                    NeptusLog.pub().info(side, "BUFFER_OVERFLOW, handshake status is " + hs);

                    // the client maximum fragment size config does not work?
                    throw new Exception("Buffer overflow: " +
                            "incorrect client maximum fragment size");
                } else if (rs == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                    NeptusLog.pub().info(side, "BUFFER_UNDERFLOW, handshake status is " + hs);

                    // bad packet, or the client maximum fragment size
                    // config does not work?
                    if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                        throw new Exception("Buffer underflow: " +
                                "incorrect client maximum fragment size");
                    } // otherwise, ignore this packet
                } else if (rs == SSLEngineResult.Status.CLOSED) {
                    throw new Exception(
                            "SSL engine closed, handshake status is " + hs);
                } else {
                    throw new Exception("Can't reach here, result is " + rs);
                }

                if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
                    NeptusLog.pub().info(side, "Handshake status is FINISHED, finish the loop");
                    endLoops = true;
                }
            } else if (hs == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                List<DatagramPacket> packets = new ArrayList<>();
                boolean finished = produceHandshakePackets(
                        engine, peerAddr, side, packets);

                NeptusLog.pub().info(side, "Produced " + packets.size() + " packets");
                for (DatagramPacket p : packets) {
                    socket.send(p);
                }

                if (finished) {
                    NeptusLog.pub().info(side, "Handshake status is FINISHED "
                            + "after producing handshake packets, "
                            + "finish the loop");
                    endLoops = true;
                }
            } else if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                runDelegatedTasks(engine);
            } else if (hs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
                NeptusLog.pub().info(side,
                        "Handshake status is NOT_HANDSHAKING, finish the loop");
                endLoops = true;
            } else if (hs == SSLEngineResult.HandshakeStatus.FINISHED) {
                throw new Exception(
                        "Unexpected status, SSLEngine.getHandshakeStatus() "
                                + "shouldn't return FINISHED");
            } else {
                throw new Exception(
                        "Can't reach here, handshake status is " + hs);
            }
        }

        SSLEngineResult.HandshakeStatus hs = engine.getHandshakeStatus();
        NeptusLog.pub().info(side, "Handshake finished, status is " + hs);

        if (engine.getHandshakeSession() != null) {
            throw new Exception(
                    "Handshake finished, but handshake session is not null");
        }

        SSLSession session = engine.getSession();
        if (session == null) {
            throw new Exception("Handshake finished, but session is null");
        }
        NeptusLog.pub().info(side, "Negotiated protocol is " + session.getProtocol());
        NeptusLog.pub().info(side, "Negotiated cipher suite is " + session.getCipherSuite());

        // handshake status should be NOT_HANDSHAKING
        //
        // According to the spec, SSLEngine.getHandshakeStatus() can't
        // return FINISHED.
        if (hs != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            throw new Exception("Unexpected handshake status " + hs);
        }
    }

    /**
     * @return
     */
    private Thread getSockedListenerThread() {
        if (sockedListenerThread == null) {
            Thread listenerThread = new Thread(DTLSTransport.class.getSimpleName() + ": Listener Thread "
                    + this.hashCode()) {
                byte[] sBuffer = new byte[maxBufferSize];

                public synchronized void start() {
                    NeptusLog.pub().info("Listener Thread Started");
                    try {
                        //set up DTLS socket
                        sock = new DatagramSocket(null);
                        sock.setReuseAddress(false);

                        serverSocketAddr = new InetSocketAddress(serverAddress, serverPort);
                        engine = createSSLEngine(true);

                        /* perform handshake */
                        handshake(engine, sock, serverSocketAddr, "Client");
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                        setOnBindError(true);
                        return;
                    }
                    finally {
                        if (isOnBindError()) {
                            try {

                                //close dtls connection
//                                sock.disconnect();
//                                sock.close();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e.getStackTrace());
                            }
                        }
                    }
                    super.start();
                }

                public void run() {
                    try {
                        // purging not changing yet
                        while (!purging) {
                            DatagramPacket packet = new DatagramPacket(sBuffer, sBuffer.length);
                            try {
                                sock.receive(packet);
                                int lengthReceived = packet.getLength();
                                ByteBuffer netBuffer = ByteBuffer.wrap(sBuffer, 0, lengthReceived);
                                ByteBuffer recBuffer = ByteBuffer.allocate(BUFFER_SIZE);

                                NeptusLog.pub().info("before unwrapping : " + Arrays.toString(netBuffer.array()));
                                SSLEngineResult rs = engine.unwrap(netBuffer, recBuffer);
                                recBuffer.flip();
                                NeptusLog.pub().info("after unwrapping : " + Arrays.toString(recBuffer.array()));
                                if (recBuffer.remaining() != 0) {
                                    System.out.println("Original ByteBuffer: "
                                            + Arrays.toString(netBuffer.array()));
                                    NeptusLog.pub().info("remaining buffer : " + Arrays.toString(recBuffer.array()));
                                    break;
                                }


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
                                NeptusLog.pub().error(e);
                                e.printStackTrace();
                                // continue;
                            }
                            catch (Error e) {
                                NeptusLog.pub().error(e);
                                e.printStackTrace();
                                // continue;
                            }
                            // Thread.sleep(1);
                            // try { Thread.sleep(10); } catch (Exception e) { }
                        }
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                        // NeptusLog.pub().warn(this+" Thread interrupted");
                    }

                    NeptusLog.pub().warn(this + " Thread Stopped");

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
}
