/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Jul 14, 2015
 */
package pt.lsts.wgcontrol;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FuelLevel;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;

/**
 * @author zp
 *
 */
@PluginDescription(name="CMRE WG Commander")
@Popup(width=200,height=500,name="CMRE WG Commander", pos=POSITION.BOTTOM_RIGHT)
public class WgControl extends ConsolePanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    
    private LinkedHashMap<String, String> commands = new LinkedHashMap<String, String>();
    {
        commands.put("Rudder Left", "<nav><mode>202</mode></nav>");
        commands.put("AIS Off", "<nav><payload_3>2</payload_3></nav>");
        commands.put("AIS On", "<nav><payload_3>1</payload_3></nav>");
        commands.put("All Off", "<nav><light>2</light><payload_2>2</payload_2><payload_3>2</payload_3></nav>");
        commands.put("Channel Serial", "<nav><channel>3</channel></nav>");
        commands.put("Hand Brake", "<nav><mode>1</mode></nav>");
        commands.put("Payload 2 Off", "<nav><payload_2>2</payload_2></nav>");
        commands.put("Payload 2 On",  "<nav><payload_2>1</payload_2></nav>" );
        commands.put("Protect All",  "<nav><over_ride>1</over_ride><light>2</light><payload_1>2</payload_1><payload_2>2</payload_2><payload_3>2</payload_3></nav>");
        commands.put("Rudder Centre", "<nav><mode>200</mode></nav>");
        commands.put("Rudder Right", "<nav><mode>201</mode></nav>");
        commands.put("Lights On", "<nav><light>1</light></nav>");
        commands.put("Lights Off", "<nav><light>2</light></nav>");
        commands.put("Weather Station On",  "<nav>weather_station>1</weather_station></nav>" );
    }
    
    private LinkedHashMap<String, JButton> buttons = new LinkedHashMap<String, JButton>();
    
    
    @NeptusProperty
    public String wg_hostname = "";
    
    @NeptusProperty
    public int command_port = 5983;
    
    @NeptusProperty
    public int reply_port = 5984;
    
    @NeptusProperty
    public int telemetry_port = 5989;
    
    public WgControl(ConsoleLayout cl) {
        super(cl);
    }
    
    
    
    @Override
    public void cleanSubPanel() {
        
    }
    
    
    @Override
    public void actionPerformed(ActionEvent e) {
        JButton source = (JButton)e.getSource();
        String cmd = source.getText();
        try {
            sendCommand(cmd, wg_hostname, command_port);
        }
        catch (Exception ex) {
            getConsole().post(
                    Notification.error("WG Commander",
                            ex.getClass().getSimpleName() + " while sending command: " + ex.getMessage()));
            ex.printStackTrace();
        }
        getConsole().post(Notification.success("WG Commander", cmd + " sent."));        
    }

    @Override
    public void initSubPanel() {
        
        BoxLayout layout = new BoxLayout(this, BoxLayout.PAGE_AXIS);
        setLayout(layout);
        
        for (String cmd : commands.keySet()) {
            JButton button = new JButton(cmd);
            button.addActionListener(this);
            add(button);
            buttons.put(cmd, button);            
        }
    }
    
    public void process(String telemetry) {
        //parse telemetry
        EstimatedState state = new EstimatedState();
        //state.setLat(lat);
        //ImcMsgManager.getManager().postInternalMessage(srcName, message);
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

}
