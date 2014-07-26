/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jul 24, 2014
 */
package pt.lsts.neptus.plugins.alliance;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashSet;

import javax.swing.JMenuItem;

import com.google.common.eventbus.Subscribe;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import pt.lsts.imc.DevDataText;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.NMEAUtils;
import pt.lsts.neptus.util.llf.NeptusMessageLogger;
import de.baderjene.aistoolkit.aisparser.AISParser;
import de.baderjene.aistoolkit.aisparser.message.Message05;


/**
 * @author zp
 *
 */
@PluginDescription(name="NMEA Plotter")
public class NmeaPlotter extends ConsoleLayer {
    
    @NeptusProperty(name = "Connect to the serial port")
    public boolean serialListen = false;

    @NeptusProperty(name = "Serial Port Device")
    public String uartDevice = "/dev/ttyUSB0";

    @NeptusProperty(name = "Serial Port Baud Rate")
    public int uartBaudRate = 38400;

    @NeptusProperty(name = "Serial Port Data Bits")
    public int dataBits = 8;

    @NeptusProperty(name = "Serial Port Stop Bits")
    public int stopBits = 1;

    @NeptusProperty(name = "Serial Port Parity Bits")
    public int parity = 0;

    @NeptusProperty(name = "UDP port to bind")
    public int udpPort = 7878;

    @NeptusProperty(name = "Listen for incoming UDP packets")
    public boolean udpListen = true;

    @NeptusProperty(name = "Maximum age in for AIS contacts (seconds)")
    public int maximumAisAge = 600;

    @NeptusProperty(name = "Use Neptus external systems API", userLevel=LEVEL.ADVANCED)
    public boolean useExternalSystemsApi = true;
    
    @NeptusProperty(name = "Retransmit to other Neptus consoles", userLevel=LEVEL.ADVANCED)
    public boolean retransmitToNeptus = true;
    
    @NeptusProperty(name = "Log received data", userLevel=LEVEL.ADVANCED)
    public boolean logReceivedData = true;
        
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

    private SerialPort serialPort = null;
    private HashSet<NmeaListener> listeners = new HashSet<>();
    private AisContactDb contactDb = new AisContactDb();
    private AISParser parser = new AISParser();

    private void connectToSerial() throws Exception {
        serialPort = new SerialPort(uartDevice);

        boolean opened = serialPort.openPort();
        if (!opened)
            throw new Exception("Unable to open port "+uartDevice);

        serialPort.addEventListener(new SerialPortEventListener() {

            private String currentString = "";

            @Override
            public void serialEvent(SerialPortEvent arg0) {
                try {
                    String s = serialPort.readString();
                    if (s.contains("\n")) {
                        currentString += s.substring(0, s.indexOf('\n'));
                        if (!currentString.trim().isEmpty()) {
                            for (NmeaListener l :listeners)
                                l.nmeaSentence(currentString.trim());
                            parseSentence(currentString);
                            if (retransmitToNeptus)
                                retransmit(currentString);
                            if (logReceivedData)
                                NeptusMessageLogger.logMessage(new DevDataText(currentString));
                        }
                        currentString = s.substring(s.indexOf('\n')+1);                        
                    }
                    else {
                        currentString += s;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        serialPort.setParams(uartBaudRate, dataBits, stopBits, parity);
    }
    
    private void retransmit(String sentence) {
        DevDataText ddt = new DevDataText(sentence);
        for (ImcSystem s : ImcSystemsHolder.lookupSystemByType(SystemTypeEnum.CCU)) {
            ImcMsgManager.getManager().sendMessageToSystem(ddt, s.getName());
        }
    }
    
    @Subscribe
    public void on(DevDataText ddt) {
        //System.out.println("received dev data text from "+ddt.getSourceName());  
        parseSentence(ddt.getValue());
    }
    
    private void parseSentence(String s) {
        s = s.trim();
        String nmeaType = NMEAUtils.nmeaType(s);
        if (nmeaType.equals("$B-TLL") || nmeaType.equals("$A-TLL"))
            contactDb.processBtll(s);
        else if (nmeaType.equals("$GPGGA"))
            contactDb.processGGA(s);
        else if (nmeaType.equals("$RATTM"))
            contactDb.processRattm(s);
        else {
            synchronized (parser) {
                parser.process(s);    
            }
        }
            
    }
    
    private void connect() throws Exception {
        if (serialListen)
            connectToSerial();
        if (udpListen) {
            final DatagramSocket socket = new DatagramSocket(udpPort);
            Thread listener = new Thread("NmeaListener") {
                
                public void run() {
                    connected = true;
                    NeptusLog.pub().info("Listening to NMEA messages over UDP.");
                    while(connected) {
                        try {
                            DatagramPacket dp = new DatagramPacket(new byte[65507], 65507);
                            socket.receive(dp);
                            String sentence = new String(dp.getData());
                            sentence = sentence.substring(0, sentence.indexOf(0));
                            parseSentence(sentence);    
                            if (retransmitToNeptus)
                                retransmit(sentence);
                            if (logReceivedData)
                                NeptusMessageLogger.logMessage(new DevDataText(sentence));
                        }
                        catch (Exception e) {
                            e.printStackTrace();   
                            break;
                        }
                    }
                    NeptusLog.pub().info("UDP Socket closed.");
                    socket.close();
                };
            };
            listener.setDaemon(true);
            listener.start();
        }
    }

    public void disconnect() throws Exception {
        if (serialListen)
            serialPort.closePort();
        connected = false;
    }

    public void addListener(NmeaListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NmeaListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void cleanLayer() {
        if (serialPort != null) {
            try {
                serialPort.closePort();
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
    }

    public boolean userControlsOpacity() {
        return false;
    }

    @Periodic(millisBetweenUpdates=60000)
    public void purgeOldContacts() {
        contactDb.purge(maximumAisAge * 1000);
    }

    @Periodic(millisBetweenUpdates=120000)
    public void saveCache() {
        contactDb.saveCache();
    }


    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        for (AisContact c : contactDb.getContacts()) {
            LocationType l = c.getLocation();
            if (l.getLatitudeDegs() == 0 &&  l.getLongitudeDegs() == 0)
                continue;

            Point2D pt = renderer.getScreenPosition(l);
            g.setColor(new Color(64,124,192));
            g.drawString(c.getLabel(), (int)pt.getX()+17, (int)pt.getY()+2);

            if (c.getAdditionalProperties() != null) {
                g.setColor(new Color(64,124,192,128));
                Message05 m = c.getAdditionalProperties();
                Graphics2D copy = (Graphics2D)g.create();
                double width = m.getDimensionToPort() + m.getDimensionToStarboard();
                double length = m.getDimensionToStern() + m.getDimensionToBow();
                double centerX = pt.getX();//-m.getDimensionToPort() + width/2.0;
                double centerY = pt.getY();//-m.getDimensionToStern() + length/2.0;

                copy.translate(centerX, centerY);
                copy.rotate(Math.PI+Math.toRadians(c.getCog()) - renderer.getRotation());
                copy.scale(renderer.getZoom(), renderer.getZoom());
                copy.scale(width/2, length/2);
                copy.fill(ship);
                copy.scale(1.0/(width/2), 1.0/(length/2));
            }
            g.setColor(Color.black);
            g.fill(new Ellipse2D.Double((int)pt.getX()-3, (int)pt.getY()-3, 6, 6));            
        }            
    }

    @Override
    public void initLayer() {
        connectItem = getConsole().addMenuItem("Tools>NMEA Plotter>Connect", null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (!connected) {
                        connect();
                        connected = true;
                        connectItem.setText("Disconnect");
                    }
                    else {
                        disconnect();
                        connected = false;
                        connectItem.setText("Connect");
                    }
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                }
            }
        });

        getConsole().addMenuItem("Tools>NMEA Plotter>Settings", null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PluginUtils.editPluginProperties(NmeaPlotter.this, true);
            }
        });
        parser.register(contactDb);
    }
}
