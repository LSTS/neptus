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
 * Jun 30, 2013
 */
package pt.lsts.neptus.plugins.sunfish;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.*;

import org.apache.commons.codec.binary.Hex;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.*;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.iridium.ActivateSubscription;
import pt.lsts.neptus.comm.iridium.DeactivateSubscription;
import pt.lsts.neptus.comm.iridium.DesiredAssetPosition;
import pt.lsts.neptus.comm.iridium.DeviceUpdate;
import pt.lsts.neptus.comm.iridium.ExtendedDeviceUpdate;
import pt.lsts.neptus.comm.iridium.ImcIridiumMessage;
import pt.lsts.neptus.comm.iridium.IridiumCommand;
import pt.lsts.neptus.comm.iridium.IridiumManager;
import pt.lsts.neptus.comm.iridium.IridiumMessage;
import pt.lsts.neptus.comm.iridium.Position;
import pt.lsts.neptus.comm.iridium.TargetAssetPosition;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.params.ConfigurationManager;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Iridium Communications Plug-in", icon = "images/iridium/iridium-logo.png")
@SuppressWarnings("unused")
public class IridiumComms extends SimpleRendererInteraction {

    private static final long serialVersionUID = -8535642303286049869L;
    protected long lastMessageReceivedTime = System.currentTimeMillis() - 3600000;
    protected LinkedHashMap<String, RemoteSensorInfo> sensorData = new LinkedHashMap<>();
    private static final String[] iridiumDestinations = new String[] { "broadcast", "manta-1", "manta-11",
            "lauv-xplore-1", "lauv-seacon-2", "lauv-seacon-3" };

    protected final int HERMES_ID = 0x08c1;
    protected Vector<VirtualDrifter> drifters = new Vector<>();
    protected LinkedHashMap<String, Image> systemImages = new LinkedHashMap<String, Image>();

    @NeptusProperty(name = "Wave Glider", description = "Imc id of neptus console of wave glider operator", category = "IMC id", userLevel = NeptusProperty.LEVEL.REGULAR)
    public int wgOpImcId = 0;

    @NeptusProperty(name = "Remote Neptus", description = "Imc id of neptus console operating remotely", category = "IMC id", userLevel = NeptusProperty.LEVEL.REGULAR)
    public int neptusOpImcId = 0;

    /** System restart types **/
    final String targetMainDUNE = "Main CPU: DUNE";
    final String targetMainCpu = "Main CPU";
    final String targetAuxCpu = "Auxiliary CPU";

    public void setPosition(Position p) {
        String name = IMCDefinition.getInstance().getResolver().resolve(p.id);
        LocationType loc = new LocationType();
        loc.setLatitudeRads(p.latRads);
        loc.setLongitudeRads(p.lonRads);

        ImcSystem system = ImcSystemsHolder.lookupSystem(p.id);
        if (system != null) {
            system.setLocation(loc);
            return;
        }

        ExternalSystem sys = ExternalSystemsHolder.lookupSystem(name);
        if (sys == null) {
            sys = new ExternalSystem(name);

            switch (IMCUtils.getSystemType(p.id)) {
                case "UUV":
                case "ROV":
                case "USV":
                case "UAV":
                case "UXV":
                    sys.setType(SystemTypeEnum.VEHICLE);
                    break;
                case "CCU":
                    sys.setType(SystemTypeEnum.CCU);
                    break;
                default:
                    sys.setType(SystemTypeEnum.MOBILESENSOR);
                    break;
            }
            ExternalSystemsHolder.registerSystem(sys);
        }
        sys.setLocation(loc);

    }

    @Subscribe
    public void on(IridiumMsgRx msg) {
        try {
            byte[] data = msg.getData();
            NeptusLog.pub().info(
                    msg.getSourceName() + " received iridium message with data " + new String(Hex.encodeHex(data)));
            IridiumMessage m = IridiumMessage.deserialize(data);

            getConsole().post(Notification.info("Iridium Comms",
                    "Received message of type " + m.getClass().getSimpleName() + " from "
                            + IMCDefinition.getInstance().getResolver().resolve(m.getSource()) + " (0x"
                            + String.format("%04X", m.getSource()) + ")"));

            if (m instanceof ExtendedDeviceUpdate) {
                ExtendedDeviceUpdate upd = (ExtendedDeviceUpdate) m;
                getConsole().post(Notification.info("Iridium Communications",
                        "Received " + upd.getPositions().size() + " position updates."));
                for (Position p : upd.getPositions().values()) {
                    RemoteSensorInfo rsi = new RemoteSensorInfo();
                    rsi.setTimestamp(p.timestamp);
                    rsi.setLat(p.latRads);
                    rsi.setLon(p.lonRads);
                    String name = ImcSystemsHolder.translateImcIdToSystemName(p.id);
                    if (name != null)
                        rsi.setId(ImcSystemsHolder.translateImcIdToSystemName(p.id));
                    else
                        rsi.setId(String.format("Unknown (%X)", p.id));

                    rsi.setSensorClass(IMCUtils.getSystemType(p.id));
                    ImcMsgManager.getManager().postInternalMessage("IridiumComms", rsi);
                }
            }
            else if (m instanceof DeviceUpdate) {
                DeviceUpdate upd = (DeviceUpdate) m;
                getConsole().post(Notification.info("Iridium Communications",
                        "Received " + upd.getPositions().size() + " position updates."));
                for (Position p : upd.getPositions().values()) {
                    RemoteSensorInfo rsi = new RemoteSensorInfo();
                    rsi.setTimestamp(p.timestamp);
                    rsi.setLat(p.latRads);
                    rsi.setLon(p.lonRads);
                    String name = IMCDefinition.getInstance().getResolver().resolve(p.id);
                    if (name != null)
                        rsi.setId(IMCDefinition.getInstance().getResolver().resolve(p.id));
                    else
                        rsi.setId(String.format("Unknown (%X)", p.id));

                    EstimatedState state = new EstimatedState();
                    state.setSrc(p.id);
                    state.setLat(p.latRads);
                    state.setLon(p.lonRads);
                    state.setTimestamp(p.timestamp);

                    ImcMsgManager.getManager().postInternalMessage("IridiumComms", state);
                }
            }
            else if (m instanceof ImcIridiumMessage) {
                for (IMCMessage message : m.asImc()) {
                    message.setSrc(msg.getSrc());
                    message.setTimestampMillis(msg.getTimestampMillis());
                    message.setDst(msg.getDst());
                    NeptusLog.pub().info("Posting incoming message to bus: " + message);
                    ImcMsgManager.getManager().postInternalMessage("IridiumComms", message);
                }
            }
            else if (m instanceof IridiumCommand) {
                IridiumCommand cmd = (IridiumCommand) m;
                if (cmd.getCommand().toLowerCase().startsWith("error"))
                    getConsole()
                            .post(Notification.warning("Iridium Report", msg.getSourceName() + ": " + cmd.getCommand())
                                    .requireHumanAction(true));
                else
                    getConsole().post(Notification.info("Iridium Text", msg.getSourceName() + ": " + cmd.getCommand())
                            .requireHumanAction(true));
            }
            NeptusLog.pub().info("Resulting message: " + m);
        }
        catch (Exception e) {
            NeptusLog.pub().info(
                    "Received a custom Iridium text from " + msg.getSourceName() + ": " + new String(msg.getData()));
        }
    }

    @Subscribe
    public void on(IridiumMsgTx msg) {
        try {
            byte[] data = msg.getData();
            IridiumMessage m = IridiumMessage.deserialize(data);
            NeptusLog.pub().info(msg.getSourceName() + " request sending of " + m.getClass().getSimpleName()
                    + " message with data " + new String(Hex.encodeHex(data)));
            m.setSource(msg.getSrc());
            m.setDestination(msg.getDst());
            m.timestampMillis = msg.getTimestampMillis();
            NeptusLog.pub().debug("Encoded message: " + m);
        }
        catch (Exception e) {
            NeptusLog.pub().info(
                    msg.getSourceName() + " sent a custom iridium message with text: " + new String(msg.getData()));
        }
    }

    @Periodic(millisBetweenUpdates = 60000)
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

        final Object selection = JOptionPane.showInputDialog(getConsole(), "Select plan to be commanded via Iridium",
                "Start plan", JOptionPane.QUESTION_MESSAGE, null, planNames.toArray(), selectedPlan);
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
                selectedPlan = selectedPlan.replaceAll("(_)*[^\\w\\d]*", "");
                pc.setPlanId(selectedPlan);
                sendViaIridium(getMainVehicleId(), pc);
            };
        };
        send.setDaemon(true);
        send.start();
    }

    private void sendTextNote() {
        String note = JOptionPane.showInputDialog(getConsole(), I18n.text("Enter note to be published"));

        if (note == null || note.isEmpty())
            return;

        LogBookEntry entry = new LogBookEntry();
        entry.setText(note);
        entry.setTimestampMillis(System.currentTimeMillis());
        entry.setSrc(ImcMsgManager.getManager().getLocalId().intValue());

        // (Component parentComponent, Object message, String title, int messageType, Icon icon, Object[]
        // selectionValues, Object initialSelectionValue)
        Object selection = JOptionPane.showInputDialog(getConsole(), "Please enter destination of this message",
                "Send Text Note", JOptionPane.QUESTION_MESSAGE, null, iridiumDestinations, "manta-1");
        if (selection == null)
            return;
        else if (selection.equals("broadcast"))
            entry.setDst(65535);
        else
            entry.setDst(IMCDefinition.getInstance().getResolver().resolve("" + selection));
        entry.setContext("Iridium logbook");
        ImcIridiumMessage msg = new ImcIridiumMessage();
        msg.setSource(ImcMsgManager.getManager().getLocalId().intValue());

        msg.setDestination(65535);
        msg.setMsg(entry);
        try {
            IridiumManager.getManager().send(msg);
            getConsole().post(Notification.success("Iridium message sent", "1 Iridium messages were sent using "
                    + IridiumManager.getManager().getCurrentMessenger().getName()));
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), "Send Iridium Text", e.getMessage());
        }
    }

    private void sendIridiumCommand() {
        String cmd = JOptionPane.showInputDialog(getConsole(),
                I18n.textf("Enter command to be sent to %vehicle", getMainVehicleId()));
        if (cmd == null || cmd.isEmpty())
            return;

        IridiumCommand command = new IridiumCommand();
        command.setCommand(cmd);

        ImcSystem sys = ImcSystemsHolder.getSystemWithName(getMainVehicleId());

        // VehicleType vt = VehiclesHolder.getVehicleById(getMainVehicleId());
        if (sys == null) {
            GuiUtils.errorMessage(getConsole(), "Send Iridium Command",
                    "Could not calculate destination's IMC identifier");
            return;
        }
        command.setDestination(sys.getId().intValue());
        command.setSource(ImcMsgManager.getManager().getLocalId().intValue());
        try {
            IridiumManager.getManager().send(command);
            getConsole().post(Notification.success("Iridium message sent", "1 Iridium messages were sent using "
                    + IridiumManager.getManager().getCurrentMessenger().getName()));
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), "Send Iridium Command", e.getMessage());
        }

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
            GuiUtils.errorMessage(getConsole(), "Send Iridium Wave Glider Target Position", e.getMessage());
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
            GuiUtils.errorMessage(getConsole(), "Send Iridium Wave Glider Desired Position", e.getMessage());
        }
    }

    private void sendSystemRestart(String selectedSysName, String restartType) {
        int sysId = ImcSystemsHolder.lookupSystemByName(selectedSysName).getId().intValue();

        IMCMessage msg = null;
        final ImcIridiumMessage iridiumMsg = new ImcIridiumMessage();

        switch (restartType) {
            case targetMainDUNE: {
                RestartSystem m = new RestartSystem();
                m.setType(RestartSystem.TYPE.DUNE);
                msg = m;
                break;
            }
            case targetMainCpu: {
                RestartSystem m = new RestartSystem();
                m.setType(RestartSystem.TYPE.SYSTEM);
                msg = m;
                break;
            }
            case targetAuxCpu: {
                ArrayList<SystemProperty> props = ConfigurationManager.getInstance()
                        .getPropertiesByEntity(selectedSysName,
                                "Power Supply",
                                SystemProperty.Visibility.DEVELOPER,
                                SystemProperty.Scope.GLOBAL);

                Optional<SystemProperty> res = props.stream()
                        .filter(p -> p.getValueType() == SystemProperty.ValueTypeEnum.STRING
                                && ((String) p.getValue()).contains("Auxiliary CPU"))
                        .findFirst();

                if(!res.isPresent()) {
                    getConsole().post(Notification.warning("Restart System", "Could not find Auxiliary Cpu"));
                    return;
                }

                PowerChannelControl pcc = new PowerChannelControl();
                pcc.setOp(PowerChannelControl.OP.RESTART);
                pcc.setName((String) res.get().getValue());
                msg = pcc;
                break;
            }
            default:
                NeptusLog.pub().warn("Restart type " + restartType + " not supported");
        }

        if (msg == null) {
            getConsole().post(Notification.warning("Restart system command", "Could not send system restart"));
            return;
        }

        msg.setDst(sysId);
        iridiumMsg.setSource(ImcMsgManager.getManager().getLocalId().intValue());
        iridiumMsg.setDestination(sysId);
        iridiumMsg.timestampMillis = msg.getTimestampMillis();
        iridiumMsg.setMsg(msg);

        try {
            IridiumManager.getManager().send(iridiumMsg);
            getConsole().post(Notification.success("Restart system command",
                    "Sent following system restart -> id: " +
                    selectedSysName + "(" + sysId + ") " +
                    "Type: " + restartType));
        } catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), "Send Iridium CPU Restart", e.getMessage());
            NeptusLog.pub().warn("Failed to send system restart");
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

        popup.add(I18n.textf("Send %vehicle a command via Iridium", getMainVehicleId()))
                .addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        sendIridiumCommand();
                    }
                });

        popup.add(I18n.textf("Command %vehicle a plan via Iridium", getMainVehicleId()))
                .addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        commandPlanExecution();
                    }
                });

        popup.add(I18n.textf("Subscribe to iridium device updates", getMainVehicleId()))
                .addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ActivateSubscription activate = new ActivateSubscription();
                        activate.setDestination(0xFF);
                        activate.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                        try {
                            IridiumManager.getManager().send(activate);
                            getConsole().post(
                                    Notification.success("Iridium message sent", "1 Iridium messages were sent using "
                                            + IridiumManager.getManager().getCurrentMessenger().getName()));
                        }
                        catch (Exception ex) {
                            GuiUtils.errorMessage(getConsole(), "Subscribe to iridium device updates", ex.getMessage());
                        }
                    }
                });

        popup.add(I18n.textf("Unsubscribe to iridium device updates", getMainVehicleId()))
                .addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        DeactivateSubscription deactivate = new DeactivateSubscription();
                        deactivate.setDestination(0xFF);
                        deactivate.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                        try {
                            IridiumManager.getManager().send(deactivate);
                        }
                        catch (Exception ex) {
                            GuiUtils.errorMessage(getConsole(), "Unsubscribe to iridium device updates", ex.getMessage());
                        }
                    }
                });

        popup.add(I18n.text("Send an Iridium text")).addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String text = JOptionPane.showInputDialog(getConsole(), "Enter text to send via Iridium");
                if (text == null || text.isEmpty())
                    return;

                IridiumCommand command = new IridiumCommand();
                command.setCommand(text);

                // send to broadcast
                command.setDestination(0xFFFF);
                command.setSource(ImcMsgManager.getManager().getLocalId().intValue());
                try {
                    IridiumManager.getManager().send(command);
                    getConsole().post(Notification.success("Iridium message sent", "Iridium message sent using "
                            + IridiumManager.getManager().getCurrentMessenger().getName()));
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), "Iridium message sent", ex.getMessage());
                }

            }
        });

        popup.addSeparator();

        popup.add(I18n.text("Simulate incoming message")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String res = JOptionPane.showInputDialog(getConsole(), "Paste the message data here (hex)");

                if (res == null)
                    return;

                else {
                    IridiumMsgRx rx = new IridiumMsgRx();
                    try {
                        byte[] data = Hex.decodeHex(res.toCharArray());
                        rx.setData(data);
                        rx.setSrc(ImcSystemsHolder.getSystemWithName(getConsole().getMainSystem()).getId().intValue());
                        rx.setDst(0xFFFF);
                        on(rx);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
            }
        });

        popup.add("Restart system").addActionListener(e -> {
            JComboBox<String> availableVehicles = new JComboBox<>();
            Arrays.stream(ImcSystemsHolder.lookupSystemByType(SystemTypeEnum.VEHICLE))
                    .filter(s -> s.getTypeVehicle() == VehicleType.VehicleTypeEnum.UUV)
                    .forEach(s -> availableVehicles.addItem(s.getName()));


            JComboBox<String> restartTypes = new JComboBox<>(new String[]{
                    targetMainDUNE,
                    targetMainCpu,
                    targetAuxCpu});

            final JComponent[] inputs = new JComponent[] {
                    new JLabel("System"),
                    availableVehicles,
                    new JLabel("Type"),
                    restartTypes};

            int res = JOptionPane.showConfirmDialog(null, inputs, "System restart", JOptionPane.OK_CANCEL_OPTION);
            if (res != JOptionPane.OK_OPTION)
                return;


            String selectedSysName = ((String) availableVehicles.getSelectedItem());
            String restartType = (String) restartTypes.getSelectedItem();

            sendSystemRestart(selectedSysName, restartType);
        });

        popup.show(source, event.getX(), event.getY());
    }

    @Subscribe
    public void on(IridiumTxStatus status) {
        try {

            switch (status.getStatus()) {
                case ERROR:
                    post(Notification
                            .warning(I18n.text("Iridium communications"), I18n.text("Error sending iridium message"))
                            .src(status.getSourceName()));
                    break;
                case EXPIRED:
                    post(Notification
                            .warning(I18n.text("Iridium communications"),
                                    I18n.text("Iridium message transmission time has expired"))
                            .src(status.getSourceName()));
                    break;
                case OK:
                    post(Notification.success(I18n.text("Iridium communications"),
                            I18n.text("Iridium message sent successfully")).src(status.getSourceName()));
                    break;
                case QUEUED:
                    post(Notification
                            .warning(I18n.text("Iridium communications"),
                                    I18n.text("Iridium message was queued for later transmission"))
                            .src(status.getSourceName()));
                    break;
                case TRANSMIT:
                    post(Notification.warning(I18n.text("Iridium communications"),
                            I18n.text("Iridium message is being transmited")).src(status.getSourceName()));
                    break;
                default:
                    break;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IridiumComms(ConsoleLayout console) {
        super(console);
    }

    @Subscribe
    public void on(TextMessage msg) {
        NeptusLog.pub().info("Received text message from "+msg.getSourceName()+": "+msg.getText());
        if (msg.getText().startsWith("ERROR"))
            post(Notification.error("Iridium Error", msg.getSourceName() + ": " + msg.getText())
                    .requireHumanAction(true));
        else
            post(Notification.info("Text message", msg.getSourceName() + ": " + msg.getText())
                    .requireHumanAction(true));
    }

    @Override
    public void initSubPanel() {

    }

    @Override
    public void cleanSubPanel() {

    }

    public static void main(String[] args) throws Exception {
        String s = "sk_rendez-({;vous";
        System.out.println(s.replaceAll("(_)*[^\\w\\d]*", ""));
        IridiumComms comms = new IridiumComms(ConsoleParse.dummyConsole());
        IridiumMsgRx rx = new IridiumMsgRx();
        String plan = "2100ffffd50738004552524f523a205061746820436f6e74726f6c3a206578706563746564206e6577207061746820636f6e74726f6c207265666572656e6365";
        byte[] data = Hex.decodeHex(plan.toCharArray());
        rx.setData(data);
        rx.setSrc(30);
        rx.setDst(0xFFFF);
        comms.on(rx);
    }
}
