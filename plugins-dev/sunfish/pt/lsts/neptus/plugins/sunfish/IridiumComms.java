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
 * Jun 30, 2013
 */
package pt.lsts.neptus.plugins.sunfish;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.apache.commons.codec.binary.Hex;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IridiumMsgRx;
import pt.lsts.imc.IridiumMsgTx;
import pt.lsts.imc.IridiumTxStatus;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.imc.RemoteSensorInfo;
import pt.lsts.imc.TextMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.iridium.ActivateSubscription;
import pt.lsts.neptus.comm.iridium.DeactivateSubscription;
import pt.lsts.neptus.comm.iridium.DesiredAssetPosition;
import pt.lsts.neptus.comm.iridium.IridiumCommand;
import pt.lsts.neptus.comm.iridium.IridiumManager;
import pt.lsts.neptus.comm.iridium.IridiumMessage;
import pt.lsts.neptus.comm.iridium.TargetAssetPosition;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Iridium Communications Plug-in", icon = "pt/lsts/neptus/plugins/sunfish/iridium.png")
public class IridiumComms extends SimpleRendererInteraction implements IPeriodicUpdates {

    private static final long serialVersionUID = -8535642303286049869L;
    protected long lastMessageReceivedTime = System.currentTimeMillis() - 3600000;
    protected LinkedHashMap<String, RemoteSensorInfo> sensorData = new LinkedHashMap<>();

    protected final int HERMES_ID = 0x08c1;
    protected Vector<VirtualDrifter> drifters = new Vector<>();
    protected LinkedHashMap<String, Image> systemImages = new LinkedHashMap<String, Image>();

    @NeptusProperty(name = "Wave Glider", description = "Imc id of neptus console of wave glider operator", category = "IMC id", userLevel = NeptusProperty.LEVEL.REGULAR)
    public int wgOpImcId = 0;

    @NeptusProperty(name = "Remote neptus", description = "Imc id of neptus console operating remotely", category = "IMC id", userLevel = NeptusProperty.LEVEL.REGULAR)
    public int neptusOpImcId = 0;

    @Override
    public long millisBetweenUpdates() {
        return 60000;
    }
    
    @Subscribe
    public void on(IridiumMsgRx msg) {
        try {
            byte[] data = msg.getData();
            NeptusLog.pub().info(msg.getSourceName()+" received iridium message with data "+new String(Hex.encodeHex(data)));
            IridiumMessage m = IridiumMessage.deserialize(data);
            NeptusLog.pub().info("Resulting message: "+m);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }
    
    @Subscribe
    public void on(IridiumMsgTx msg) {
        try {
            byte[] data = msg.getData();
            NeptusLog.pub().info(msg.getSourceName()+" request sending of iridium message with data "+new String(Hex.encodeHex(data)));
            IridiumMessage m = IridiumMessage.deserialize(data);
            NeptusLog.pub().info("Encoded message: "+m);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }

    @Override
    public boolean update() {
        for (VirtualDrifter d : drifters) {
            RemoteSensorInfo rsi = new RemoteSensorInfo();
            rsi.setId(d.id);
            LocationType loc = d.getLocation();
            rsi.setLat(loc.getLatitudeRads());
            rsi.setLon(loc.getLongitudeRads());
            rsi.setTimestampMillis(System.currentTimeMillis());
            rsi.setSensorClass("drifter");
            ImcMsgManager.getManager().postInternalMessage("IridiumComms", rsi);
            post(rsi);            
        }

        return true;
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    private void commandPlanExecution() {
       Collection<String> planNames = getConsole().getMission().getIndividualPlansList().keySet();
       if (planNames.isEmpty())
           return;
       String selectedPlan = planNames.iterator().next();
       if (getConsole().getPlan() != null) {
           selectedPlan = getConsole().getPlan().getId();
       }
       
       final Object selection = JOptionPane.showInputDialog(getConsole(), "Select plan to be commanded via Iridium", "Start plan", JOptionPane.QUESTION_MESSAGE, null, planNames.toArray(), selectedPlan);
       if (selection == null)
           return;
       
       Thread send = new Thread("Send plan via iridium") {
           @Override
        public void run() {
               String selectedPlan = selection.toString();
               PlanType toSend = getConsole().getMission().getIndividualPlansList().get(selectedPlan);
               IMCMessage msg = toSend.asIMCPlan();
               PlanControl pc = new PlanControl();
               pc.setArg(msg);
               pc.setOp(OP.START);
               pc.setType(TYPE.REQUEST);
               pc.setPlanId(selectedPlan);
               sendViaIridium(getMainVehicleId(), pc);
           };
       };
       send.setDaemon(true);
       send.start();       
    }
    
    private void sendIridiumCommand() {
        String cmd = JOptionPane.showInputDialog(getConsole(),
                I18n.textf("Enter command to be sent to %vehicle", getMainVehicleId()));
        if (cmd == null || cmd.isEmpty())
            return;

        IridiumCommand command = new IridiumCommand();
        command.setCommand(cmd);

        VehicleType vt = VehiclesHolder.getVehicleById(getMainVehicleId());
        if (vt == null) {
            GuiUtils.errorMessage(getConsole(), "Send Iridium Command",
                    "Could not calculate destination's IMC identifier");
            return;
        }
        command.setDestination(vt.getImcId().intValue());
        command.setSource(ImcMsgManager.getManager().getLocalId().intValue());
        try {
            IridiumManager.getManager().send(command);
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
        }
        getConsole().post(Notification.success("Iridium message sent", "1 Iridium messages were sent using "+IridiumManager.getManager().getCurrentMessenger().getName()));        
    }

    private void setWaveGliderTargetPosition(LocationType loc) {
        TargetAssetPosition pos = new TargetAssetPosition();
        pos.setLocation(loc);
        pos.setDestination(neptusOpImcId);
        pos.setAssetImcId(HERMES_ID);
        pos.setSource(ImcMsgManager.getManager().getLocalId().intValue());
        try {
            IridiumManager.getManager().send(pos);
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
        }
    }

    private void setWaveGliderDesiredPosition(LocationType loc) {
        DesiredAssetPosition pos = new DesiredAssetPosition();
        pos.setAssetImcId(HERMES_ID);
        pos.setLocation(loc);
        pos.setDestination(wgOpImcId);
        pos.setSource(ImcMsgManager.getManager().getLocalId().intValue());
        try {
            IridiumManager.getManager().send(pos);
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
        }
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if (event.getButton() != MouseEvent.BUTTON3) {
            super.mouseClicked(event, source);
            return;
        }

        final LocationType loc = source.getRealWorldLocation(event.getPoint());
        loc.convertToAbsoluteLatLonDepth();

        JPopupMenu popup = new JPopupMenu();

        if (IridiumManager.getManager().isActive()) {
            popup.add(I18n.text("Deactivate Polling")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    IridiumManager.getManager().stop();
                }
            });
        }
        else {
            popup.add(I18n.text("Activate Polling")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    IridiumManager.getManager().start();
                }
            });
        }

        popup.add(I18n.text("Select Iridium gateway")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                IridiumManager.getManager().selectMessenger(getConsole());
            }
        });

        popup.addSeparator();

        popup.add(I18n.textf("Send %vehicle a command via Iridium", getMainVehicleId())).addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        sendIridiumCommand();
                    }
                });
        
        popup.add(I18n.textf("Command %vehicle a plan via Iridium", getMainVehicleId())).addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        commandPlanExecution();
                    }
                });

        popup.add(I18n.textf("Subscribe to iridium device updates", getMainVehicleId())).addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ActivateSubscription activate = new ActivateSubscription();
                        activate.setDestination(0xFF);
                        activate.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                        try {
                            IridiumManager.getManager().send(activate);
                        }
                        catch (Exception ex) {
                            GuiUtils.errorMessage(getConsole(), ex);
                        }
                    }
                });

        popup.add(I18n.textf("Unsubscribe to iridium device updates", getMainVehicleId())).addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        DeactivateSubscription deactivate = new DeactivateSubscription();
                        deactivate.setDestination(0xFF);
                        deactivate.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                        try {
                            IridiumManager.getManager().send(deactivate);
                        }
                        catch (Exception ex) {
                            GuiUtils.errorMessage(getConsole(), ex);
                        }
                    }
                });

        popup.add("Set this as actual wave glider target").addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setWaveGliderTargetPosition(loc);
            }
        });

        popup.add("Set this as desired wave glider target").addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setWaveGliderDesiredPosition(loc);
            }
        });

        popup.addSeparator();

        popup.add("Add virtual drifter").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                VirtualDrifter d = new VirtualDrifter(loc, 0, 0.1);
                PropertiesEditor.editProperties(d, true);
                drifters.add(d);
                update();
            }
        });

        popup.show(source, event.getX(), event.getY());
    }


    @Subscribe
    public void on(IridiumTxStatus status) {
        switch (status.getStatus()) {
            case ERROR:
                post(Notification.warning(I18n.text("Iridium communications"),
                        I18n.text("Error sending iridium message")).src(status.getSourceName()));
                break;
            case EXPIRED:
                post(Notification.warning(I18n.text("Iridium communications"),
                        I18n.text("Iridium message transmission time has expired")).src(status.getSourceName()));
                break;
            case OK:
                post(Notification.success(I18n.text("Iridium communications"),
                        I18n.text("Iridium message sent successfully")).src(status.getSourceName()));
                break;
            case QUEUED:
                post(Notification.warning(I18n.text("Iridium communications"),
                        I18n.text("Iridium message was queued for later transmission")).src(status.getSourceName()));
                break;
            case TRANSMIT:
                post(Notification.warning(I18n.text("Iridium communications"),
                        I18n.text("Iridium message is being transmited")).src(status.getSourceName()));
                break;
        }
    }

    public IridiumComms(ConsoleLayout console) {
        super(console);
    }

    @Subscribe
    public void on(TextMessage msg) {
        NeptusLog.pub().info("Received text message");
        post(Notification.info("Text message", msg.getSourceName() + ": " + msg.getText()));
    }

    @Override
    public void initSubPanel() {
        
    }

    @Override
    public void cleanSubPanel() {

    }
}
