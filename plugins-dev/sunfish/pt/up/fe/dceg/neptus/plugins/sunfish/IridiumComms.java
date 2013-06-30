/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 30, 2013
 */
package pt.up.fe.dceg.neptus.plugins.sunfish;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.comm.iridium.IridiumCommand;
import pt.up.fe.dceg.neptus.comm.iridium.IridiumFacade;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.RemoteSensorInfo;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleRendererInteraction;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 *
 */
@PluginDescription(name="Iridium Communications Plug-in")
public class IridiumComms extends SimpleRendererInteraction implements Renderer2DPainter {

    private static final long serialVersionUID = -8535642303286049869L;
    protected long lastMessageReceivedTime = System.currentTimeMillis() - 3600000;
    protected LinkedHashMap<String, RemoteSensorInfo> sensorData = new LinkedHashMap<>();
    
    @Override
    public boolean isExclusive() {
        return true;
    }
    
    private void sendIridiumCommand() {
        String cmd = JOptionPane.showInputDialog(getConsole(), I18n.textf("Enter command to be sent to %vehicle", getMainVehicleId()));
        if (cmd == null || cmd.isEmpty())
            return;
        
        IridiumCommand command = new IridiumCommand();
        command.setCommand(cmd);
        
        VehicleType vt = VehiclesHolder.getVehicleById(getMainVehicleId());
        if (vt == null) {
            GuiUtils.errorMessage(getConsole(), "Send Iridium Command", "Could not calculate destination's IMC identifier");
            return;
        }
        command.setDestination(vt.getImcId().intValue());
        command.setSource(ImcMsgManager.getManager().getLocalId().intValue());
        try {
            IridiumFacade.getInstance().sendMessage(command);    
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
        }
    }
    
    private void setWaveGliderTargetPosition(LocationType loc) {
        
    }

    private void setWaveGliderDesiredPosition(LocationType loc) {
        
    }    
    
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (event.getButton() != MouseEvent.BUTTON3)
            super.mouseClicked(event,source);
        
        final LocationType loc = source.getRealWorldLocation(event.getPoint());
        loc.convertToAbsoluteLatLonDepth();
        
        JPopupMenu popup = new JPopupMenu();
        popup.add(I18n.textf("Send %vehicle a command via Iridium", getMainVehicleId())).addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e) {
                sendIridiumCommand();                
            }
        });

        popup.add("Set this as actual wave glider target").addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e) {
                setWaveGliderTargetPosition(loc);
            }
        });
        
        popup.add("Set this as desired wave glider target").addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e) {
                setWaveGliderDesiredPosition(loc);
            }
        });
        
        popup.addSeparator();
        
        popup.add("Settings").addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(IridiumComms.this, getConsole(), true);
            }
        });
        
        popup.show(source, event.getX(), event.getY());
    }
    
    @Subscribe
    public void on(RemoteSensorInfo msg) {
        NeptusLog.pub().info("Got device update from "+msg.getId()+": "+sensorData);        
        sensorData.put(msg.getId(), msg);
    }
    
    public IridiumComms(ConsoleLayout console) {
        super(console);
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        for (RemoteSensorInfo sinfo : sensorData.values()) {
            switch (sinfo.getId()) {
                case "wg-desired-pos":
                case "wg-target-pos":
                case "lauv-desired-pos":
                case "lauv-target-pos":
                    break;
                default:
                    
                    break;
            }
        }
    }

    @Override
    public void initSubPanel() {
        IridiumFacade.getInstance();          
    }

    @Override
    public void cleanSubPanel() {

    }

}
