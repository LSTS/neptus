/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jul 24, 2014
 */
package pt.lsts.neptus.plugins.alliance;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;

import com.google.common.eventbus.Subscribe;

import de.baderjene.aistoolkit.aisparser.AISParser;
import de.baderjene.aistoolkit.aisparser.message.Message05;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import pt.lsts.aismanager.api.AisContactManager;
import pt.lsts.imc.DevDataText;
import pt.lsts.imc.lsf.LsfMessageLogger;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.gui.OrientationIcon;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.alliance.ais.CmreAisCsvParser;
import pt.lsts.neptus.plugins.alliance.ais.CmreAisCsvParser.DistressPosition;
import pt.lsts.neptus.plugins.alliance.ais.CmreAisCsvParser.DistressStatus;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ScatterPointsElement;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.NMEAUtils;
import pt.lsts.neptus.util.nmea.NmeaListener;
import pt.lsts.neptus.util.nmea.NmeaProvider;

/**
 * @author zp
 * @author pdias
 */
@PluginDescription(name = "NMEA Plotter", icon = "pt/lsts/neptus/plugins/alliance/nmea-ais.png")
public class NmeaPlotter extends ConsoleLayer implements NmeaProvider {

    private static final int RECT_WIDTH = 228;
    private static final int RECT_HEIGHT = 85;
    private static final int MARGIN = 5;
    
    private static final Color COLOR_GREEN_DARK_100 = new Color(155, 255, 155, 250);
    private static final Color COLOR_RED_DARK_100 = new Color(255, 155, 155, 250);

    @NeptusProperty(name = "Connect to the serial port", category = "Serial Port", userLevel = LEVEL.REGULAR)
    public boolean serialListen = false;

    @NeptusProperty(name = "Serial Port Device", category = "Serial Port", userLevel = LEVEL.REGULAR)
    public String uartDevice = "/dev/ttyUSB0";

    @NeptusProperty(name = "Serial Port Baud Rate", category = "Serial Port", userLevel = LEVEL.ADVANCED)
    public int uartBaudRate = 38400;

    @NeptusProperty(name = "Serial Port Data Bits", category = "Serial Port", userLevel = LEVEL.ADVANCED)
    public int dataBits = 8;

    @NeptusProperty(name = "Serial Port Stop Bits", category = "Serial Port", userLevel = LEVEL.ADVANCED)
    public int stopBits = 1;

    @NeptusProperty(name = "Serial Port Parity Bits", category = "Serial Port", userLevel = LEVEL.ADVANCED)
    public int parity = 0;

    @NeptusProperty(name = "UDP port to bind", category = "UDP", userLevel = LEVEL.REGULAR)
    public int udpPort = 7878;

    @NeptusProperty(name = "Listen for incoming UDP packets", category = "UDP", userLevel = LEVEL.REGULAR)
    public boolean udpListen = true;

    @NeptusProperty(name = "Connect via TCP", category = "TCP Client", userLevel = LEVEL.REGULAR)
    public boolean tcpConnect = false;

    @NeptusProperty(name = "TCP Host", category = "TCP Client", userLevel = LEVEL.REGULAR)
    public String tcpHost = "127.0.0.1";

    @NeptusProperty(name = "TCP Port", category = "TCP Client", userLevel = LEVEL.REGULAR)
    public int tcpPort = 13000;

    @NeptusProperty(name = "Maximum age in for AIS contacts (minutes)", userLevel = LEVEL.REGULAR,
            description = "0 for disable filter")
    public int maximumAisAgeMinutes = 10;

    @NeptusProperty(name = "Retransmit to other Neptus consoles", userLevel = LEVEL.ADVANCED)
    public boolean retransmitToNeptus = true;

    @NeptusProperty(name = "Log received data", userLevel = LEVEL.ADVANCED)
    public boolean logReceivedData = true;

    @NeptusProperty(name = "Number of track points", userLevel = LEVEL.ADVANCED)
    public int trackPoints = 100;

    @NeptusProperty(name = "Minutes to Show Distress Signal", category = "Distress Test", userLevel = LEVEL.ADVANCED)
    private int minutesToShowDistress = 5; 
    
    @NeptusProperty(name = "Connect via Ripples", category = "TCP Client", userLevel = LEVEL.REGULAR)
    public boolean ripplesConnection = false;


    private JLabel distressLabelToPaint = new JLabel();

    private JMenuItem connectItem = null;
    private boolean connected = false;

    GeneralPath ship = new GeneralPath();
    {
        ship.moveTo(0, 1.0);
        ship.lineTo(1.0, 0.6);
        ship.lineTo(1.0, -1.0);
        ship.lineTo(-1.0, -1.0);
        ship.lineTo(-1.0, 0.6);
        ship.lineTo(0, 1.0);
    }

    private final OrientationIcon icon = new OrientationIcon(40, 2) {{ 
        setBackgroundColor(COLOR_RED_DARK_100);
        setForegroundColor(COLOR_GREEN_DARK_100);
    }};

    private SerialPort serialPort = null;
    private DatagramSocket udpSocket = null;
    private Socket tcpSocket = null;
    
    private boolean isSerialConnected = false;
    private boolean isUdpConnected = false;
    private boolean isTcpConnected = false;

    private HashSet<NmeaListener> listeners = new HashSet<>();
    private AisContactDb contactDb = new AisContactDb();
    private AISParser parser = new AISParser();
    private final AisContactManager aisManager = AisContactManager.getInstance();

    private LinkedHashMap<String, LocationType> lastLocs = new LinkedHashMap<>();
    private LinkedHashMap<String, ScatterPointsElement> tracks = new LinkedHashMap<>();

    @Periodic(millisBetweenUpdates = 5000)
    public void updateTracks() {
        for (AisContact c : contactDb.getContacts()) {
            LocationType l = c.getLocation();
            String name = c.getLabel();

            if (lastLocs.get(name) == null || !lastLocs.get(name).equals(l)) {
                if (!tracks.containsKey(name)) {
                    ScatterPointsElement sc = new ScatterPointsElement();
                    sc.setCenterLocation(l);
                    sc.setColor(Color.black, Color.gray.brighter());
                    sc.setNumberOfPoints(trackPoints);
                    tracks.put(name, sc);
                }
                tracks.get(name).addPoint(l);
                updateAisManager(c);
            }
        }
    }
    
    @Periodic(millisBetweenUpdates = 30_000)
    public void ripplesUpdate() {
        if (!ripplesConnection)
            return;
        try {
            RipplesAisParser.getShips().forEach(contactDb::setMTShip);    
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error downloading AIS data from Ripples: "+e.getMessage(), e);
        }        
    }

    /**
     * Set/Update AIS contact's new information on AIS Manager
     * */
    private void updateAisManager(AisContact contact) {
        int mmsi = contact.getMmsi();
        double cog = Math.toRadians(contact.getCog());
        double sog = contact.getSog();
        double hdg = Math.toRadians(contact.getHdg());

        LocationType loc = contact.getLocation().getNewAbsoluteLatLonDepth();
        double latRads = loc.getLatitudeRads();
        double lonRads = loc.getLongitudeRads();
        long timestamp = System.currentTimeMillis();
        String label = contact.getLabel();

        aisManager.setShipPosition(mmsi, sog, cog, hdg, latRads, lonRads, timestamp, label);
    }

    private void connectToSerial() throws Exception {
        serialPort = new SerialPort(uartDevice);
        setSerialConnected(true);

        boolean opened = serialPort.openPort();
        if (!opened) {
            setSerialConnected(false);
            serialPort = null;
            Exception e = new Exception("Unable to open port " + uartDevice);
            NeptusLog.pub().error(e);
            getConsole().post(Notification.error("NMEA Plotter",
                    "Error connecting via serail to  \"" + serialPort + "\".").requireHumanAction(false));

            throw e;
        }

        serialPort.setParams(uartBaudRate, dataBits, stopBits, parity);
        serialPort.addEventListener(new SerialPortEventListener() {
            private String currentString = "";

            @Override
            public void serialEvent(SerialPortEvent serEvt) {
                try {
                    String s = serialPort.readString();
                    if (s == null|| s.isEmpty())
                        return; // If null nothing to do!
                    
                    if (s.contains("\n")) {
                        currentString += s.substring(0, s.indexOf('\n'));
                        processSentence();
                        currentString = s.substring(s.indexOf('\n') + 1);
                    }
                    else if (s.contains("$") || s.contains("!")) {
                        // For cases where the stream is not canonical (there is without new line at the end
                        if (s.contains("$"))
                            currentString += s.substring(0, s.indexOf('$'));
                        else
                            currentString += s.substring(0, s.indexOf('!'));
                        processSentence();
                        if (s.contains("$"))
                            currentString = s.substring(s.indexOf('$'));
                        else
                            currentString = s.substring(s.indexOf('!'));
                        // System.out.println(">>" + currentString);
                    }
                    else {
                        currentString += s;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private void processSentence() {
                if (!currentString.trim().isEmpty()) {
                    // System.out.println(">" + currentString);
                    if (hasNMEASentencePrefix(currentString)) {
                        for (NmeaListener l : listeners)
                            l.nmeaSentence(currentString.trim());
                    }
                    parseSentence(currentString);
                    if (retransmitToNeptus)
                        retransmit(currentString);
                    if (logReceivedData)
                        LsfMessageLogger.log(new DevDataText(currentString));
                }
            }
        });
        NeptusLog.pub().info("Listening to NMEA messages over serial \"" + serialPort + "\".");
        getConsole().post(Notification.success("NMEA Plotter", "Connected via serial to \"" + uartDevice + "\"."));
    }

    private boolean hasNMEASentencePrefix(String sentence) {
        return sentence.startsWith("$") || sentence.startsWith("!");
    }
    
    private void retransmit(String sentence) {
        DevDataText ddt = new DevDataText(sentence);
        for (ImcSystem s : ImcSystemsHolder.lookupSystemByType(SystemTypeEnum.CCU)) {
            ImcMsgManager.getManager().sendMessageToSystem(ddt.cloneMessage(), s.getName());
        }
    }

    @Subscribe
    public void on(DevDataText ddt) {
        parseSentence(ddt.getValue());
    }

    private void parseSentence(String s) {
        if (s == null || s.isEmpty())
            return;

        s = s.trim();
        if (hasNMEASentencePrefix(s)) {
            String nmeaType = NMEAUtils.nmeaType(s);
            if (nmeaType.equals("$B-TLL") || nmeaType.equals("$A-TLL"))
                contactDb.processBtll(s);
            else if (nmeaType.startsWith("GGA", 3)) // GP or GN
                contactDb.processGGA(s);
            else if (nmeaType.startsWith("TTM", 3)) // RA
                contactDb.processRattm(s);
            else if (nmeaType.startsWith("HDT", 3)) // GP
                contactDb.processGPHDT(s);
            else if (nmeaType.startsWith("GLL", 3)) // GP or NM
                contactDb.processGLL(s);
            else {
                synchronized (parser) {
                    parser.process(s); // Is AIS
                }
            }
        }
        else if (s.startsWith("{")) {
            contactDb.processJson(s);
        }
        else {
            CmreAisCsvParser.process(s, contactDb);
        }
        
        for (NmeaListener l : listeners)
            l.nmeaSentence(s);
    }

    private void connect() throws Exception {
        connected = true;
        if (serialListen) {
            connectToSerial();
        }
        if (udpListen) {
            connectToUDP();
        }
        if (tcpConnect) {
            connectToTCP();
        }
        connected = isSerialConnected || isUdpConnected || isTcpConnected;
    }

    private void connectToTCP() {
        final Socket socket = new Socket();
        this.tcpSocket = socket;
        Thread listener = new Thread("NMEA TCP Listener") {
            public void run() {
                BufferedReader reader = null;
                try {
                    socket.setSoTimeout(1000);
                }
                catch (SocketException e1) {
                    e1.printStackTrace();
                }
                try {
                    socket.connect(new InetSocketAddress(tcpHost, tcpPort));
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    setTcpConnected(true);
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                    getConsole().post(Notification.error("NMEA Plotter",
                            "Error connecting via TCP to " + tcpHost + ":" + tcpPort).requireHumanAction(false));
                    
                    setTcpConnected(false);

                    // if still connected, we need to reconnect
                    reconnect(socket);
                    
                    return;
                }
                NeptusLog.pub().info("Listening to NMEA messages over TCP.");
                getConsole().post(
                        Notification.success("NMEA Plotter", "Connected via TCP to " + tcpHost + ":" + tcpPort));
                while (connected && isTcpConnected) {
                    try {
                        String sentence = reader.readLine();
                        if (sentence == null)
                            break;
                        if (sentence.isEmpty())
                            continue;

                        String[] tks = sentence.split("\n");
                        for (String tk : tks) {
                            try {
                                parseSentence(tk);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (retransmitToNeptus)
                                retransmit(tk);
                            if (logReceivedData)
                                LsfMessageLogger.log(new DevDataText(tk));
                        }
                    }
                    catch (SocketTimeoutException e) {
                        continue;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
                NeptusLog.pub().info("TCP Socket closed.");
                getConsole().post(Notification.info("NMEA Plotter", "Disconnected via TCP."));
                try {
                    socket.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    setTcpConnected(false);
                }
                
                // if still connected, we need to reconnect
                reconnect(socket);
            }

            private void reconnect(final Socket socket) {
                if (connected) {
                    try {
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (connected && !isTcpConnected && socket == NmeaPlotter.this.tcpSocket) {
                        connectToTCP();
                    }
                }
            };
        };
        listener.setDaemon(true);
        setTcpConnected(true);
        listener.start();
    }

    private void connectToUDP() throws SocketException {
        final DatagramSocket socket = new DatagramSocket(udpPort);
        Thread udpListenerThread = new Thread("NMEA UDP Listener") {
            public void run() {
                setUdpConnected(true);

                try {
                    socket.setSoTimeout(1000);
                }
                catch (SocketException e1) {
                    e1.printStackTrace();
                }
                NeptusLog.pub().info("Listening to NMEA messages over UDP.");
                getConsole().post(Notification.success("NMEA Plotter", "Listening via UDP to port " + udpPort + "."));

                while (connected && isUdpConnected) {
                    try {
                        DatagramPacket dp = new DatagramPacket(new byte[65507], 65507);
                        socket.receive(dp);
                        String sentence = new String(dp.getData());
                        sentence = sentence.substring(0, sentence.indexOf(0));
                        if (sentence == null || sentence.isEmpty())
                            continue;

                        String[] tks = sentence.split("\n");
                        for (String tk : tks) {
                            try {
                                parseSentence(tk);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (retransmitToNeptus)
                                retransmit(tk);
                            if (logReceivedData)
                                LsfMessageLogger.log(new DevDataText(tk));
                        }
                    }
                    catch (SocketTimeoutException e) {
                        continue;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
                NeptusLog.pub().info("UDP Socket closed.");
                getConsole().post(Notification.info("NMEA Plotter", "Stop listening via UDP."));
                socket.close();
                setUdpConnected(false);
            };
        };
        udpListenerThread.setDaemon(true);
        setUdpConnected(true);
        udpListenerThread.start();
    }

    public void disconnect() throws Exception {
        connected = false;
        if (isSerialConnected && serialPort != null) {
            boolean res;
            try {
                res = serialPort.closePort();
            }
            catch (Exception e) {
                e.printStackTrace();
                res = true;
            }
            if (res)
                setSerialConnected(false);
        }
        if (udpSocket != null) {
            udpSocket.close();
            setUdpConnected(false);
            udpSocket = null;
        }
        if (udpSocket == null)
            setUdpConnected(false);

        if (tcpSocket != null) {
            tcpSocket.close();
            setTcpConnected(false);
            tcpSocket = null;
        }
        if (tcpSocket == null)
            setTcpConnected(false);

        connected = isSerialConnected || isUdpConnected || isTcpConnected;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.util.nmea.NmeaProvider#addListener(pt.lsts.neptus.util.nmea.NmeaListener)
     */
    public void addListener(NmeaListener listener) {
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.util.nmea.NmeaProvider#removeListener(pt.lsts.neptus.util.nmea.NmeaListener)
     */
    public void removeListener(NmeaListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void cleanLayer() {
        connected = false;
        try {
            disconnect();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        getConsole().removeMenuItem(I18n.text("Tools") + ">" + I18n.text("NMEA Plotter") + ">" + I18n.text("Connect"));
        getConsole().removeMenuItem(I18n.text("Tools") + ">" + I18n.text("NMEA Plotter") + ">" + I18n.text("Settings"));
    }

    public boolean userControlsOpacity() {
        return false;
    }

    @Periodic(millisBetweenUpdates = 60000)
    public void purgeOldContacts() {
        if (maximumAisAgeMinutes > 0)
            contactDb.purge(maximumAisAgeMinutes * 60 * 1000);
    }

    @Periodic(millisBetweenUpdates = 120000)
    public void saveCache() {
        contactDb.saveCache();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        ArrayList<ScatterPointsElement> els = new ArrayList<>();
        els.addAll(tracks.values());
        for (ScatterPointsElement el : els)
            el.paint((Graphics2D) g.create(), renderer, renderer.getRotation());

        Graphics2D g1 = (Graphics2D) g.create();
        for (AisContact c : contactDb.getContacts()) {
            LocationType l = c.getLocation();
            if (l.getLatitudeDegs() == 0 && l.getLongitudeDegs() == 0)
                continue;

            Point2D pt = renderer.getScreenPosition(l);
            g1.setColor(new Color(64, 124, 192));
            g1.drawString(c.getLabel(), (int) pt.getX() + 17, (int) pt.getY() + 2);

            if (c.getAdditionalProperties() != null) {
                g1.setColor(new Color(64, 124, 192, 128));
                Message05 m = c.getAdditionalProperties();
                Graphics2D copy = (Graphics2D) g1.create();
                double width = m.getDimensionToPort() + m.getDimensionToStarboard();
                double length = m.getDimensionToStern() + m.getDimensionToBow();
                double centerX = pt.getX();
                double centerY = pt.getY();

                double widthOffsetFromCenter = m.getDimensionToPort() - m.getDimensionToStarboard();
                double lenghtOffsetFromCenter = m.getDimensionToStern() - m.getDimensionToBow();

                copy.translate(centerX, centerY);
                double hdg = c.getHdg() > 360 ? c.getCog() : c.getHdg();
                copy.rotate(Math.PI + Math.toRadians(hdg) - renderer.getRotation());
                copy.scale(renderer.getZoom(), renderer.getZoom());
                copy.translate(widthOffsetFromCenter / 2., -lenghtOffsetFromCenter / 2.);
                copy.scale(width / 2, length / 2);
                copy.fill(ship);
                copy.dispose();
            }
            g1.setColor(Color.black);
            g1.fill(new Ellipse2D.Double((int) pt.getX() - 3, (int) pt.getY() - 3, 6, 6));
        }
        g1.dispose();
        
        paintDistress(g, renderer);
    }

    /**
     * @param g
     * @param renderer
     */
    private void paintDistress(Graphics2D g, StateRenderer2D renderer) {
        
        String txt = collectDistressTextToPaint();
        
        if (txt == null || txt.isEmpty())
            return;
        
        distressLabelToPaint.setText(txt);
        distressLabelToPaint.setForeground(Color.BLACK);
        distressLabelToPaint.setVerticalAlignment(JLabel.NORTH);
        distressLabelToPaint.setHorizontalTextPosition(JLabel.CENTER);
        distressLabelToPaint.setHorizontalAlignment(JLabel.LEFT);
        distressLabelToPaint.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setTransform(renderer.getIdentity());
        
        int width = RECT_WIDTH;
        int height = RECT_HEIGHT;
        height = (int) Math.max(height, distressLabelToPaint.getPreferredSize().getHeight());
        
        // Pull up for lat/lon label
        g2.translate(0, renderer.getHeight() - (height + MARGIN));
        g2.translate(0, -200);

        g2.setColor(new Color(0, 0, 0, 200));

        g2.drawRoundRect(MARGIN, MARGIN, width, height, 20, 20);

        g2.setColor(new Color(255, 155, 155, 230));

        g2.fillRoundRect(MARGIN, MARGIN, width, height, 20, 20);

        g2.translate(2.5, 2.5);
        distressLabelToPaint.setBounds(0, 0, width, height);
        distressLabelToPaint.paint(g2);
        
        LocationType loc = collectDistressLocation();
        if (loc != null) {
            Graphics2D g3 = (Graphics2D) g.create();
            Point2D pt = renderer.getScreenPosition(loc);
            g3.translate(pt.getX(), pt.getY());
            g3.setStroke(new BasicStroke(3));
            g3.setColor(new Color(255, 155, 155, 255));
            int s = 20;
            g3.fillOval(-s / 2, -s / 2, s, s);
            s = 40;
            g3.drawOval(-s / 2, -s / 2, s, s);
            s = 60;
            g3.drawOval(-s / 2, -s / 2, s, s);
            s = 80;
            g3.drawOval(-s / 2, -s / 2, s, s);
            g3.dispose();
            
            if (pt.getX() < 0 || pt.getY() < 0 || pt.getX() > renderer.getWidth() || pt.getY() > renderer.getHeight()) {
                double[] neb = CoordinateUtil.getNEBearingDegreesAndRange(renderer.getCenter(), loc);
                g2.translate(0, -40);
                icon.setBackgroundColor(new Color(255, 155, 155, 250));
                icon.setForegroundColor(new Color(155, 255, 155, 250));
                icon.setAngleRadians(Math.toRadians(neb[0]) - renderer.getRotation());
                icon.paintIcon(null, g2, 0, 0);
            }
        }

        g2.dispose();
    }

    /**
     * @return
     */
    private LocationType collectDistressLocation() {
        DistressPosition dPos = CmreAisCsvParser.distressPosition;
        if (dPos != null) {
            LocationType loc = new LocationType(dPos.latDegs, dPos.lonDegs);
            return loc;
        }
        return null;
    }

    /**
     * @return
     */
    private String collectDistressTextToPaint() {
        String ret = "";
        
        DistressPosition dPos = CmreAisCsvParser.distressPosition;
        DistressStatus dSta = CmreAisCsvParser.distressStatus;
        
        if (dPos == null && dSta == null)
            return "";
        
        long cur = System.currentTimeMillis();
        long lastMsg = 0;
        if (dPos != null)
            lastMsg = Math.max(lastMsg, dPos.timestamp);
        if (dSta != null)
            lastMsg = Math.max(lastMsg, dSta.timestamp);
        if (cur - lastMsg > minutesToShowDistress * DateTimeUtil.MINUTE)
            return "";
        
        StringBuilder sb = new StringBuilder("<html>");

        String nation = dPos != null ? dPos.nation : dSta.nation;
        sb.append("<font color=\"").append(String.format("#%02X%02X%02X", 228, 37, 58)).append("\">");
        sb.append("<b>").append("&gt;&gt;&gt; DISTRESS &lt;&lt;&lt;").append("</b>");
        sb.append("</font>");
        sb.append("&nbsp");
        if (lastMsg > 0) {
            sb.append("&nbsp;&nbsp;&nbsp;&nbsp;\u2206t ");
            sb.append(DateTimeUtil.milliSecondsToFormatedString(cur - lastMsg, true));
        }

        sb.append("<br/>");
        sb.append("<font color=\"").append(String.format("#%02X%02X%02X", 28, 37, 58)).append("\">");
        sb.append("<b>").append(nation.toUpperCase()).append("</b>");
        sb.append("</font>");

        if (dPos != null) {
            long lastPos = dPos.timestamp;
            String oldTxtSTag = "";
            String oldTxtETag = "";
            if (cur - lastPos > minutesToShowDistress * DateTimeUtil.MINUTE) {
                oldTxtSTag = "<font color=\"" + String.format("#%02X%02X%02X", 128, 128, 128) + "\">";
                oldTxtETag = "</font>"; 
            }
                
//            sb.append("<font size=\"2\">");

            sb.append("<br/>").append("<b>").append(I18n.textc("Pos", "Short for position!")).append(": ").append("</b>");
            sb.append(oldTxtSTag);
            sb.append(CoordinateUtil.latitudeAsPrettyString(dPos.latDegs))
                .append(" ").append(CoordinateUtil.longitudeAsPrettyString(dPos.lonDegs));
            sb.append(oldTxtETag);
            
            sb.append("<br/>").append("<b>").append(I18n.text("Speed")).append("/").append(I18n.text("Heading"))
                .append(": ").append("</b>");
            sb.append(oldTxtSTag);
            sb.append(MathMiscUtils.round(dPos.speedKnots, 1)).append(" knt");
            sb.append(oldTxtETag);
            sb.append("<b> | </b>");
            sb.append(oldTxtSTag);
            sb.append((int) MathMiscUtils.round(dPos.headingDegs, 0)).append("\u00B0");
            sb.append(oldTxtETag);

            sb.append("<br/>").append("<b>").append(I18n.text("Depth")).append(": ").append("</b>");
            sb.append(oldTxtSTag);
            sb.append((int) MathMiscUtils.round(dPos.depth, 0)).append(" m");
            sb.append(oldTxtETag);

//            sb.append("</font>");
        }
        if (dSta != null) {
            long lastSta = dSta.timestamp;
            String oldTxtSTag = "";
            String oldTxtETag = "";
            if (cur - lastSta > minutesToShowDistress * DateTimeUtil.MINUTE) {
                oldTxtSTag = "<font color=\"" + String.format("#%02X%02X%02X", 128, 128, 128) + "\">";
                oldTxtETag = "</font>"; 
            }

//            sb.append("<font size=\"2\">");

            sb.append("<br/>");
            sb.append("<b>").append("O2").append(": ").append("</b>");
            sb.append(oldTxtSTag);
            sb.append(MathMiscUtils.round(dSta.o2Percentage, 1)).append("%");
            sb.append(oldTxtETag);
            sb.append("<b> | </b>");
            sb.append("<b>").append("CO2").append(": ").append("</b>");
            sb.append(oldTxtSTag);
            sb.append(MathMiscUtils.round(dSta.co2Percentage, 1)).append("%");
            sb.append(oldTxtETag);
            sb.append("<br/>");
            sb.append("<b>").append("CO").append(": ").append("</b>");
            sb.append(oldTxtSTag);
            sb.append(MathMiscUtils.round(dSta.coPpm, 1)).append(" ppm");
            sb.append(oldTxtETag);
            sb.append("<b> | </b>");
            sb.append("<b>").append("H2").append(": ").append("</b>");
            sb.append(oldTxtSTag);
            sb.append(MathMiscUtils.round(dSta.h2Percentage, 1)).append("%");
            sb.append(oldTxtETag);
            sb.append("<br/>");
            sb.append("<b>").append(I18n.text("Pressure")).append(": ").append("</b>");
            sb.append(oldTxtSTag);
            sb.append(MathMiscUtils.round(dSta.presureAtm, 1)).append(" atm");
            sb.append(oldTxtETag);
            sb.append("<br/>");
            sb.append("<b>").append(I18n.text("Temperature")).append(": ").append("</b>");
            sb.append(oldTxtSTag);
            sb.append(MathMiscUtils.round(dSta.temperatureDegCentigrade, 1)).append("\u00B0C");
            sb.append(oldTxtETag);
            sb.append("<br/>");
            sb.append("<b>").append(I18n.text("Survivors")).append(": ").append("</b>");
            sb.append(oldTxtSTag);
            sb.append(dSta.survivors).append(" pax");
            sb.append(oldTxtETag);

//            sb.append("</font>");
        }

        sb.append("</html>");
        ret = sb.toString();
        return ret;
    }

    @Override
    public void initLayer() {
        connectItem = getConsole().addMenuItem(
                I18n.text("Tools") + ">" + I18n.text("NMEA Plotter") + ">" + I18n.text("Connect"), 
                getIcon(), new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            if (!connected)
                                connect();
                            else
                                disconnect();
                        }
                        catch (Exception ex) {
                            GuiUtils.errorMessage(getConsole(), ex);
                        }
                        updateConnectMenuText();
                    }
                });

        getConsole().addMenuItem(I18n.text("Tools") + ">" + I18n.text("NMEA Plotter") + ">" + I18n.text("Settings"),
                null, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PluginUtils.editPluginProperties(NmeaPlotter.this, getConsole(), true);
                    }
                });
        parser.register(contactDb);
    }

    private void updateConnectMenuText() {
        if (connected) {
            String comms = isSerialConnected ? "serial" : "";
            comms += isUdpConnected ? (comms.isEmpty() ? "" : ", ") + "UDP" : "";
            comms += isTcpConnected ? (comms.isEmpty() ? "" : ", ") + "TCP" : "";
            if (!comms.isEmpty())
                comms = " (" + comms + ")";
            connectItem.setText(I18n.text("Disconnect") + comms);
        }
        else {
            connectItem.setText(I18n.text("Connect"));
        }
    }

    /**
     * @param isSerialConnected the isSerialConnected to set
     */
    public void setSerialConnected(boolean isSerialConnected) {
        this.isSerialConnected = isSerialConnected;
        updateConnectMenuText();
    }
    
    /**
     * @param isUdpConnected the isUdpConnected to set
     */
    public void setUdpConnected(boolean isUdpConnected) {
        this.isUdpConnected = isUdpConnected;
        updateConnectMenuText();
    }
    
    /**
     * @param isTcpConnected the isTcpConnected to set
     */
    public void setTcpConnected(boolean isTcpConnected) {
        this.isTcpConnected = isTcpConnected;
        updateConnectMenuText();
    }
    
    public static void main(String[] args) throws Exception {
        File fx = null;
        if (args.length > 0) {
            if ("--help".equalsIgnoreCase(args[0]) 
                    || "-h".equalsIgnoreCase(args[0])
                    || "help".equalsIgnoreCase(args[0])
                    || "h".equalsIgnoreCase(args[0])) {
            
                System.out.println("Usage: NmeaPlotter SOURCE_FILE");
                System.out.println("Reads each line and provides to a single client.");
                System.out.println("Use for test purposes of NmeaPlotter.");
                return;
            }
            
            fx = new File(args[0]);
            if (!fx.exists())
                fx = null;
        }
        
        if (fx == null) {
            // fx = new File("CMRE-AIS_example.txt");
            fx = new File("CMRE-DISTRESS_example.txt");
        }
        
        if (!fx.exists()) {
            System.out.println("File \"" + fx.getPath() + "\" not found.");
            return;
        }
        
        System.out.println("Using source file \"" + fx.getPath() + "\".");
        
        @SuppressWarnings("resource")
        ServerSocket tcp = new ServerSocket(13000);
        BufferedReader br = null;
        while (true) {
            try {
                Socket con = tcp.accept();
                FileReader fr = new FileReader(fx);
                br = new BufferedReader(fr);
                while (con.isConnected()) {
                    OutputStream os = con.getOutputStream();
                    String line = br.readLine();
                    if (line == null)
                        break;
                    System.out.println(line);
                    os.write(line.getBytes("UTF-8"));
                    os.write("\n\r".getBytes());
                    Thread.sleep(5000);
                }
                System.out.println("END");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (br != null)
                    br.close();
            }
        }
    }
    
    public static class MTShip{
        double LAT, LON, SPEED, COURSE, HEADING, TIME;
        String SHIPNAME, TYPE_IMG, TYPE_NAME, STATUS_NAME,DESTINATION;
        long SHIP_ID,ELAPSED;
        int LENGTH,WIDTH,L_FORE,W_LEFT,ROT,SHIPTYPE,TYPE;
    }
}
