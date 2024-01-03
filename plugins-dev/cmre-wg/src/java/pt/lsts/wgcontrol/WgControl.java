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
 * Jul 14, 2015
 */
package pt.lsts.wgcontrol;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;

import javax.swing.JButton;
import javax.swing.JToggleButton;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FuelLevel;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
@PluginDescription(name="CMRE WG Commander")
@Popup(width=200,height=500,name="CMRE WG Commander", pos=POSITION.BOTTOM_RIGHT)
public class WgControl extends ConsolePanel implements ActionListener, ConfigurationListener {

    private static final long serialVersionUID = 1L;

    LinkedHashMap<String, String> payloads = new LinkedHashMap<String, String>();
    {
        payloads.put("AIS", "payload_3");
        payloads.put("Lights", "light");
        payloads.put("Weather Station", "weather_station");
        payloads.put("Payload 2", "payload_2");
    }

    private LinkedHashMap<String, String> commands = new LinkedHashMap<String, String>();
    {
        commands.put("Rudder Left", "<cmre><nav><mode>202</mode></nav></cmre>");
        commands.put("Rudder Centre", "<cmre><nav><mode>200</mode></nav></cmre>");
        commands.put("Rudder Right", "<cmre><nav><mode>201</mode></nav></cmre>");
        commands.put("All Off", "<cmre><nav><light>2</light><payload_2>2</payload_2><payload_3>2</payload_3></nav></cmre>");
        commands.put("Channel Serial", "<cmre><nav><channel>3</channel></nav></cmre>");
        commands.put("Hand Brake", "<cmre><nav><mode>1</mode></nav></cmre>");
        //commands.put("Protect All",  "<cmre><nav><over_ride>1</over_ride><light>2</light><payload_1>2</payload_1><payload_2>2</payload_2><payload_3>2</payload_3></nav></cmre>");
    }

    @Override
    public void propertiesChanged() {
        stopListening();
        startListening();
    }

    private LinkedHashMap<String, String> toggles = new LinkedHashMap<String, String>();
    {
        toggles.put("Lights", "<cmre><nav><light>$mode</light></nav></cmre>");
        toggles.put("AIS", "<cmre><nav><payload_3>$mode</payload_3></nav></cmre>");
        toggles.put("Payload 2", "<cmre><nav><payload_2>$mode</payload_2></nav></cmre>");
        toggles.put("Weather Station", "<cmre><nav><weather_station>$mode</weather_station></nav></cmre>" );
        toggles.put("All Systems", "<cmre><nav><light>$mode</light><payload_2>$mode</payload_2><payload_3>$mode</payload_3></nav></cmre>");
        toggles.put("Payload CPU", "<cmre><igep><computer>$mode</computer></igep></cmre");
    }

    private LinkedHashMap<String, JButton> buttons = new LinkedHashMap<String, JButton>();
    private LinkedHashMap<String, JToggleButton> toggleButtons = new LinkedHashMap<String, JToggleButton>();
    private DatagramSocket telemetryListener, replyListener;
    private Thread telemetryThread, replyThread;

    @NeptusProperty(userLevel=LEVEL.REGULAR, name="Wave Glider Host Name")
    public String wg_hostname = "";

    @NeptusProperty
    public int command_port = 5983;

    @NeptusProperty
    public int reply_port = 5001;

    @NeptusProperty
    public int telemetry_port = 5984;

    public WgControl(ConsoleLayout cl) {
        super(cl);
    }




    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = null;

        if (e.getSource() instanceof JButton)
            command = commands.get(((JButton)e.getSource()).getText());
        else if (e.getSource() instanceof JToggleButton) {
            JToggleButton source = (JToggleButton)e.getSource();
            command = toggles.get(source.getText());
            if (source.isSelected())
                command = command.replaceAll("\\$mode", "1");
            else
                command = command.replaceAll("\\$mode", "2");
        }

        try {
            sendCommand(command, wg_hostname, command_port);
        }
        catch (Exception ex) {
            getConsole().post(
                    Notification.error("WG Commander",
                            ex.getClass().getSimpleName() + " while sending command: " + ex.getMessage()));
            ex.printStackTrace();
        }
        getConsole().post(Notification.success("WG Commander", "Command sent via UDP"));        
    }

    @Override
    public void initSubPanel() {

        GridLayout grid = new GridLayout(0, 1);
        setLayout(grid);

        for (String cmd : commands.keySet()) {
            JButton button = new JButton(cmd);
            button.addActionListener(this);
            add(button);
            buttons.put(cmd, button);            
        }

        for (String cmd : toggles.keySet()) {
            JToggleButton button = new JToggleButton(cmd);
            button.addActionListener(this);
            //button.setEnabled(false);
            add(button);
            toggleButtons.put(cmd, button);            
        }       
    }

    public void stopListening() {
        if (telemetryThread != null)
            telemetryThread.interrupt();
        if (telemetryListener != null)
            telemetryListener.close();
        if (replyThread != null)
            replyThread.interrupt();
        if (replyListener != null)
            replyListener.close();
    }

    public void startListening() {
        try {
            telemetryListener = new DatagramSocket(telemetry_port);
            telemetryListener.setSoTimeout(0);
            getConsole().post(Notification.info("CMRE WG Commander", "Listening for telemetry on port "+telemetry_port));
            telemetryThread = new Thread() {
                public void run() {
                    byte[] buf = new byte[65535];
                    while (true) {
                        DatagramPacket packet = new DatagramPacket (buf, buf.length);
                        try {
                            telemetryListener.receive(packet);
                            String data = new String(packet.getData(), 0, packet.getLength()).trim();
                            processTelemetry(data);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            telemetryListener.close();
                            break;
                        }
                    }
                };
            };
            telemetryThread.setDaemon(true);
            telemetryThread.start();
        }
        catch (Exception e) {
            e.printStackTrace();
            getConsole().post(
                    Notification.error("CMRE WG Commander", "Error binding to telemetry port: " + e.getMessage()));
        }

        try {
            replyListener = new DatagramSocket(reply_port);
            replyListener.setSoTimeout(0);
            getConsole().post(Notification.info("CMRE WG Commander", "Listening for replies on port "+reply_port));
            replyThread = new Thread() {

                public void run() {
                    byte[] buf = new byte[65535];
                    while (true) {
                        DatagramPacket packet = new DatagramPacket (buf, buf.length);
                        try {
                            replyListener.receive(packet);
                            String data = new String(packet.getData(), 0, packet.getLength()).trim();
                            processReply(data);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            replyListener.close();
                            break;
                        }
                    }
                };
            };
            replyThread.setDaemon(true);
            replyThread.start();
        }
        catch (Exception e) {
            e.printStackTrace();
            getConsole().post(
                    Notification.error("CMRE WG Commander", "Error binding to telemetry port: " + e.getMessage()));
        }
    }

    public void processReply(String reply) throws Exception {
        Document doc = DocumentHelper.parseText(reply);
        System.out.println("GOT Reply: "+doc.asXML());

        int light_state = Integer.parseInt(doc.selectSingleNode("cmre/nav/requested/light").getText());
        
        for (String toggleName : payloads.keySet()) {
            String payload_name = payloads.get(toggleName);
            int state = Integer.parseInt(doc.selectSingleNode("cmre/nav/requested/"+payload_name).getText());
            if (state == 1)
                toggleButtons.get(toggleName).setForeground(Color.green);
            else if (state == 2)
                toggleButtons.get(toggleName).setForeground(Color.red);
            else {
                toggleButtons.get(toggleName).setForeground(Color.gray);
                System.out.println("State for "+toggleName+" is "+state);
            }
            
            
        }
        
        
        System.out.println(light_state);
        
        
        /*
        int light_state = Integer.parseInt(doc.selectSingleNode("cmre/nav/VehicleRequest/@waveglider_light").getText());
        int ais_state = Integer.parseInt(doc.selectSingleNode("cmre/nav/VehicleRequest/@waveglider_ais").getText());
        int p2_state = Integer.parseInt(doc.selectSingleNode("cmre/nav/VehicleRequest/@waveglider_payload_2").getText());
        int wstation_state = Integer.parseInt(doc.selectSingleNode("cmre/nav/VehicleRequest/@waveglider_payload_2").getText());

        if (light_state == 1)
            toggleButtons.get("Lights").setSelected(true);
        else
            toggleButtons.get("Lights").setSelected(false);
        
        if (ais_state == 1)
            toggleButtons.get("AIS").setSelected(true);
        else
            toggleButtons.get("AIS").setSelected(false);
        
        if (wstation_state == 1)
            toggleButtons.get("Weather Station").setSelected(true);
        else
            toggleButtons.get("Weather Station").setSelected(false);
        
        if (p2_state == 1)
            toggleButtons.get("Payload 2").setSelected(true);
        else
            toggleButtons.get("Payload 2").setSelected(false);
        */
    }

    public void processTelemetry(String telemetry) throws Exception {
        Document doc = DocumentHelper.parseText(telemetry);

        System.out.println("GOT State: "+doc.asXML());
        String vehicle_name = doc.selectSingleNode("nav/ns").getText().substring(2);
        LinkedHashMap<String, Double> nav_parameters = new LinkedHashMap<String, Double>();
        String parameters[] = new String[] {"latitude", "longitude", "distance_to_target", "heading", "speed", "time_fix"};
        for (String param : parameters) {
            double value = Double.parseDouble(doc.selectSingleNode("nav/VehicleNavigation/@parameters_"+param).getText());
            nav_parameters.put(param, value);
        }
        
       

        ExternalSystem s = ExternalSystemsHolder.lookupSystem(vehicle_name);
        if (s == null) {
            s = new ExternalSystem(vehicle_name);
            ExternalSystemsHolder.registerSystem(s);
        }

        LocationType loc = new LocationType(nav_parameters.get("latitude"), nav_parameters.get("longitude"));
        s.setLocation(loc, (long) (nav_parameters.get("time_fix") * 1000));
        
        EstimatedState state = new EstimatedState();
        state.setLat(loc.getLatitudeRads());
        state.setLon(loc.getLongitudeRads());
        state.setPsi(Math.toRadians(nav_parameters.get("heading")));
        state.setU(nav_parameters.get("speed"));
        state.setTimestamp(nav_parameters.get("time_fix"));
        state.setSrc(655535); // FIXME set correct id
        ImcMsgManager.getManager().postInternalMessage("lisa", state);
        
        FuelLevel fl = new FuelLevel();
        fl.setValue(0); //  battery state, percent
        state.setTimestamp(nav_parameters.get("time_fix"));
        state.setSrc(655535); // FIXME set correct id
        
        
        
    }


    public void sendCommand(String cmd, String host, int cmd_port) throws IOException {
        if (commands.containsKey(cmd))
            cmd = commands.get(cmd);

        cmd = cmd.trim();
        DatagramSocket ds = new DatagramSocket();
        ds.connect(new InetSocketAddress(wg_hostname, cmd_port));
        byte[] data = cmd.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length);
        ds.send(packet);
        ds.close();
    }


    public static void main(String[] args) throws Exception {

        BufferedReader r = new BufferedReader(new FileReader(new File("/home/zp/Desktop/lisa.xml")));
        Document doc = DocumentHelper.parseText(r.readLine());

        System.out.println(doc.asXML());

        @SuppressWarnings("unused")
        String vehicle_name = doc.selectSingleNode("nav/ns").getText().substring(2);

        r.close();
    }
}
