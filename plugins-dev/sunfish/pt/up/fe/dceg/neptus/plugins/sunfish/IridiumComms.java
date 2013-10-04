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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.LinkedHashMap;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.comm.iridium.ActivateSubscription;
import pt.up.fe.dceg.neptus.comm.iridium.DeactivateSubscription;
import pt.up.fe.dceg.neptus.comm.iridium.DesiredAssetPosition;
import pt.up.fe.dceg.neptus.comm.iridium.DeviceUpdate;
import pt.up.fe.dceg.neptus.comm.iridium.HubIridiumMessenger;
import pt.up.fe.dceg.neptus.comm.iridium.IridiumCommand;
import pt.up.fe.dceg.neptus.comm.iridium.IridiumFacade;
import pt.up.fe.dceg.neptus.comm.iridium.IridiumMessage;
import pt.up.fe.dceg.neptus.comm.iridium.IridiumMessageListener;
import pt.up.fe.dceg.neptus.comm.iridium.TargetAssetPosition;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.RemoteSensorInfo;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleRendererInteraction;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 *
 */
@PluginDescription(name="Iridium Communications Plug-in", icon="pt/up/fe/dceg/neptus/plugins/sunfish/iridium.png")
public class IridiumComms extends SimpleRendererInteraction implements IPeriodicUpdates, ConfigurationListener, Renderer2DPainter, IridiumMessageListener {

    private static final long serialVersionUID = -8535642303286049869L;
    protected long lastMessageReceivedTime = System.currentTimeMillis() - 3600000;
    protected LinkedHashMap<String, RemoteSensorInfo> sensorData = new LinkedHashMap<>();
    protected Image spot, desired, target, unknown;
    protected final int HERMES_ID = 0x08c1;
    
    @NeptusProperty(name="Iridium communications device", description="The name of Iridium comms provider. Examples: lauv-xtreme-2, manta-1, hub, ...")
    public String messengerName = null;
    
    @Override
    public void propertiesChanged() {
        IridiumFacade.getInstance().setIridiumSystemProvider(messengerName);
    }
    
    @Override
    public String getName() {
        return "Iridium Communications plug-in";
    }
    
    @Override
    public long millisBetweenUpdates() {
        return 60000;
    }
    
    @Override
    public boolean update() {
        
        HubIridiumMessenger m = IridiumFacade.getInstance().getFirstMessengerOfType(HubIridiumMessenger.class);
        if (m != null && m.isAvailable()) {
            try {
                DeviceUpdate update = m.pollActiveDevices();
                post(update);
                for (IMCMessage msg : update.asImc()) {
                    post(msg);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }
    
    
    @Override
    public boolean isExclusive() {
        return true;
    }
    
    public void loadImages() {
        spot = ImageUtils.getImage("pt/up/fe/dceg/neptus/plugins/sunfish/spot.png");
        desired = ImageUtils.getImage("pt/up/fe/dceg/neptus/plugins/sunfish/desired.png");
        target = ImageUtils.getImage("pt/up/fe/dceg/neptus/plugins/sunfish/target.png");
        unknown = ImageUtils.getImage("pt/up/fe/dceg/neptus/plugins/sunfish/unknown.png");
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
        TargetAssetPosition pos = new TargetAssetPosition();
        pos.setLocation(loc);
        pos.setDestination(0);
        pos.setAssetImcId(HERMES_ID);
        pos.setSource(ImcMsgManager.getManager().getLocalId().intValue());
        try {
            IridiumFacade.getInstance().sendMessage(pos);    
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
        }
    }

    private void setWaveGliderDesiredPosition(LocationType loc) {
        DesiredAssetPosition pos = new DesiredAssetPosition();
        pos.setAssetImcId(HERMES_ID);
        pos.setLocation(loc);
        pos.setDestination(0);
        pos.setSource(ImcMsgManager.getManager().getLocalId().intValue());
        try {
            IridiumFacade.getInstance().sendMessage(pos);    
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
        }
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
        
        popup.add(I18n.textf("Subscribe to device updates", getMainVehicleId())).addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e) {
                ActivateSubscription activate = new ActivateSubscription();
                activate.setDestination(0xFF);
                activate.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                try {
                    IridiumFacade.getInstance().sendMessage(activate);    
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                }
            }
        });
        
        popup.add(I18n.textf("Unsubscribe to device updates", getMainVehicleId())).addActionListener(new ActionListener() {            
            public void actionPerformed(ActionEvent e) {
                DeactivateSubscription deactivate = new DeactivateSubscription();
                deactivate.setDestination(0xFF);
                deactivate.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                try {
                    IridiumFacade.getInstance().sendMessage(deactivate);    
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                }
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
            LocationType loc = new LocationType();
            loc.setLatitudeRads(sinfo.getLat());
            loc.setLongitudeRads(sinfo.getLon());
            Point2D pt = renderer.getScreenPosition(loc);
            Image img = null;
            if (sinfo.getId().startsWith("DP_")) {
                img = desired;
            }
            else if (sinfo.getId().startsWith("TP_")) {
                img = target;
            }
            else if (sinfo.getId().startsWith("spot") || sinfo.getId().startsWith("SPOT")) {
                img = spot;
              g.drawImage(spot, (int)(pt.getX()-spot.getWidth(this)/2), (int) (pt.getY()-spot.getHeight(this)/2), this);    
            }
            else {
                if (ImcSystemsHolder.getSystemWithName(sinfo.getId()) != null) {
                    VehicleType vt = ImcSystemsHolder.getSystemWithName(sinfo.getId()).getVehicle();
                    if (vt != null) {
                        try {
                            img = ImageUtils.getScaledImage(ImageIO.read(new File(vt.getTopImageHref())), 16, 16, false);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (img == null)
                img = unknown;
            g.drawImage(img, (int) (pt.getX()-img.getWidth(this)/2), (int) (pt.getY()-img.getHeight(this)/2), this);
            
            g.setColor(Color.black);
            g.drawString(sinfo.getId(), (int)(pt.getX()+img.getWidth(this)/2 + 3),  (int)(pt.getY() + 5));
         
        }
    }
    
    @Subscribe
    public void on(DesiredAssetPosition desiredPos) {
        NeptusLog.pub().info("Received desired position");
        RemoteSensorInfo rsi = new RemoteSensorInfo("DP_hermes", "Wave Glider", desiredPos.getLocation().getLatitudeAsDoubleValue(), desiredPos.getLocation().getLongitudeAsDoubleValue(), 0, 0, "");
        post(rsi);
    }
    
    @Subscribe
    public void on(TargetAssetPosition targetPos) {
        NeptusLog.pub().info("Received target position");
        RemoteSensorInfo rsi = new RemoteSensorInfo("TP_hermes", "Wave Glider", targetPos.getLocation().getLatitudeAsDoubleValue(), targetPos.getLocation().getLongitudeAsDoubleValue(), 0, 0, "");
        post(rsi);
    }
        
    @Override
    public void messageReceived(IridiumMessage msg) {
        NeptusLog.pub().info("Iridium message received asynchronously: "+msg);
        getConsole().post(msg);
        for (IMCMessage m : msg.asImc())
            getConsole().post(m);
    }

    @Override
    public void initSubPanel() {
        IridiumFacade.getInstance().addListener(this);
        loadImages();
    }

    @Override
    public void cleanSubPanel() {
        IridiumFacade.getInstance().removeListener(this);
    }
}
