/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsm
 * 23 May 2018
 */
package pt.lsts.neptus.plugins.telemetrycontrol;

import com.google.common.eventbus.Subscribe;
import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.Announce;
import pt.lsts.imc.CommSystemsQuery;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanDB;
import pt.lsts.imc.TelemetryMsg;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;


@Popup(width = 200, height = 100)
@PluginDescription(name = "Telemetry Control", author = "Tiago Sá Marques", description = "Telemetry control panel")
public class TelemetryControlPanel extends ConsolePanel implements PlanChangeListener {

    @NeptusProperty(name = "Ack timeout", description = "Ack timeout in seconds", userLevel = NeptusProperty.LEVEL.REGULAR)
    public int ackTimeoutSeconds = 3;

    private final ImageIcon ICON_UP = ImageUtils.getIcon("images/planning/up.png");
    private final ImageIcon ICON_START = ImageUtils.getIcon("images/planning/start.png");
    private final ImageIcon ICON_STOP = ImageUtils.getIcon("images/planning/stop.png");

    private final Color COLOR_GREEN = new Color(0, 200, 125);
    private final Color COLOR_RED = Color.RED;

    private final String announceRadioFormat = "radio/telemetry";

    private ToolbarButton sendPlan;
    private ToolbarButton startPlan;
    private ToolbarButton stopPlan;

    private final JToggleButton toggleTelemetry = new JToggleButton("OFF");
    private final JLabel sourcesLabel = new JLabel("From");
    private final JComboBox sourcesList = new JComboBox();

    private AbstractAction sendPlanAction;
    private AbstractAction startPlanAction;
    private AbstractAction stopPlanAction;

    private long requestId = 0;

    private String currSys = null;
    private PlanType currSelectedPlan = null;

    /** Known telemetry systems **/
    private final HashSet<String> availableTelemetrySystems = new HashSet<>();

    /** Telemetry binds for known **/
    private final HashMap<String, HashSet<String>> telemetryBinds = new HashMap<>();

    /** Received acks **/
    private final HashSet<Long> acks = new HashSet<>();

    public TelemetryControlPanel(ConsoleLayout console) {
        super(console);
        buildPlanel();
        discoverTelemetrySystems();
    }

    /** Send message to known systems requesting radio model, systems bound, etc **/
    private void requestTelemetryInfo(String targetSys) {
        CommSystemsQuery query = new CommSystemsQuery();
        query.setCommInterface(CommSystemsQuery.CIQ_RADIO);
        query.setType(CommSystemsQuery.CIQ_QUERY);

        // TODO dispatch
        NeptusLog.pub().info("Requesting telemetry info from " + targetSys);
        IMCSendMessageUtils.sendMessage(query, I18n.text("Error querying info from " + targetSys), false, targetSys);
    }

    /**
     * Discover known telemetry systems
     * */
    private void discoverTelemetrySystems() {
        ImcSystem[] telemetrySystems = ImcSystemsHolder.lookupSystemByService(announceRadioFormat, VehicleType.SystemTypeEnum.ALL, true);

        synchronized (availableTelemetrySystems) {
            List<String> newSystems = Arrays.stream(telemetrySystems)
                    .filter(ts -> !availableTelemetrySystems.contains(ts.getName()))
                    .map(ImcSystem::getName)
                    .collect(Collectors.toList());

            newSystems.stream().forEach(newSystem -> {
                availableTelemetrySystems.add(newSystem);
                requestTelemetryInfo(newSystem);
            });
        }
    }

    /**
     * Check announce for new telemetry systems
     * */
    @Subscribe
    public void consume(Announce msg) {
        if (availableTelemetrySystems.contains(msg.getSysName()))
            return;

        String[] services = msg.getServices().split(";");

        // no radio service
        if (!Arrays.stream(services).anyMatch(s -> s.contains(announceRadioFormat)))
            return;

        String srcName = msg.getSysName();
        NeptusLog.pub().info("Discovered " + srcName + " telemetry system");

        synchronized (availableTelemetrySystems) {
            availableTelemetrySystems.add(srcName);
            sourcesList.addItem(srcName);
        }

        requestTelemetryInfo(srcName);
    }

    @Subscribe
    public void consume(CommSystemsQuery msg) {
        if ((msg.getType() & CommSystemsQuery.CIQ_QUERY) != 0)
            return;

        if ((msg.getCommInterface() & CommSystemsQuery.CIQ_RADIO) == 0)
            return;

        String[] telemetryBinds = msg.getList().split(",");

        if (telemetryBinds.length == 0) {
            NeptusLog.pub().info("There are no bindings for " + msg.getSourceName());
            return;
        }

        HashSet<String> binds = this.telemetryBinds.getOrDefault(msg.getSourceName(), new HashSet<>());
        Arrays.stream(telemetryBinds).forEach(s -> binds.add(s));
    }

    @Subscribe
    public void consume(TelemetryMsg msg) {
        if (msg.getType() != TelemetryMsg.TYPE.TXSTATUS)
            return;

        NeptusLog.pub().info("Got ack from " + msg.getSourceName() + " for request " + msg.getReqId());
        // register successful sending of message
        if (msg.getStatus() == TelemetryMsg.STATUS.DONE)
            synchronized (acks) {
                acks.add(msg.getReqId());
            }
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange ev) {
        currSys = ev.getCurrent();
    }

    private void buildPlanel() {
        setLayout(new MigLayout());

        setupButtons();

        this.add(sourcesLabel, "grow");
        this.add(sourcesList, "grow");
        this.add(toggleTelemetry, "grow,wrap");
        this.add(sendPlan);
        this.add(startPlan);
        this.add(stopPlan);
        this.setVisibility(true);

        toogleTelemetry();
        toogleControolButtons();
    }

    private void toogleControolButtons() {
        boolean enabled = false;
        if (currSelectedPlan != null)
            enabled = true;

        sendPlan.setEnabled(enabled);
        startPlan.setEnabled(enabled);
        stopPlan.setEnabled(enabled);
    }

    private void setupButtons() {
        sendPlanAction = new AbstractAction("Send Plan", ICON_UP) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String sourceSys = (String) sourcesList.getSelectedItem();

                if(sourceSys == null || sourceSys.equals(""))
                    return;

                if(!allowedCommunication(sourceSys, currSys)) {
                    GuiUtils.errorMessage("Telemetry Control", "There is no bind between " + sourceSys + " and " + currSys);
                    return;
                }

                syncPlan(sourceSys, currSys);
            }
        };

        startPlanAction = new AbstractAction("Start Plan", ICON_START) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String sourceSys = (String) sourcesList.getSelectedItem();

                if(sourceSys == null || sourceSys.equals(""))
                    return;

                if(!allowedCommunication(sourceSys, currSys)) {
                    GuiUtils.errorMessage("Telemetry Control", "There is no bind between " + sourceSys + " and " + currSys);
                    return;
                }

                sendPlanStart(sourceSys, currSys);
            }
        };

        stopPlanAction = new AbstractAction("Stop plan", ICON_STOP) {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String sourceSys = (String) sourcesList.getSelectedItem();

                if(sourceSys == null || sourceSys.equals(""))
                    return;

                if(!allowedCommunication(sourceSys, currSys)) {
                    GuiUtils.errorMessage("Telemetry Control", "There is no bind between " + sourceSys + " and " + currSys);
                    return;
                }

                sendPlanStop(sourceSys, currSys);
            }
        };

        sendPlan = new ToolbarButton(sendPlanAction);
        startPlan = new ToolbarButton(startPlanAction);
        stopPlan = new ToolbarButton(stopPlanAction);

        // wtf java
        UIManager.put("ToggleButton.select", new ColorUIResource( COLOR_GREEN ));
        toggleTelemetry.setSelected(false);
        toggleTelemetry.addItemListener(itemEvent -> toogleTelemetry());
    }

    /**
     * Check if there is a bind between sourceSys and targetSys
     * */
    private boolean allowedCommunication(String sourceSys, String targetSys) {
        if (!telemetryBinds.containsKey(sourceSys))
            return true;

        return telemetryBinds.get(sourceSys).contains(targetSys);
    }

    private void syncPlan(String telemetryTarget, String imcTarget) {
        if (currSelectedPlan == null) {
            NeptusLog.pub().warn("Currently selected plan is null");
            GuiUtils.infoMessage(this, "Send Plan Warning", "Currently selected plan is null");
            return;
        }

        NeptusLog.pub().info("Sync plan " + currSelectedPlan.getDisplayName() + " to " + imcTarget + " through " + telemetryTarget);

        PlanDB pdb = new PlanDB();
        pdb.setType(PlanDB.TYPE.REQUEST);
        pdb.setOp(PlanDB.OP.SET);
        pdb.setRequestId(IMCSendMessageUtils.getNextRequestId());
        pdb.setPlanId(currSelectedPlan.getId());
        pdb.setArg(currSelectedPlan.asIMCPlan());
        pdb.setDst(ImcSystemsHolder.lookupSystemByName(imcTarget).getId().intValue());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            pdb.serialize(new IMCOutputStream(baos));
        } catch (IOException e) {
            GuiUtils.errorMessage(I18n.text("Send Plan Error"), e.getMessage());
            return;
        }

        long reqId = dispatchTelemetry(telemetryTarget, imcTarget, TelemetryMsg.CODE.CODE_IMC, TelemetryMsg.STATUS.EMPTY, true, baos.toByteArray());

        if (reqId == -1)
            return;

        waitForAck(reqId,
                () -> post(Notification.success("Telemetry Control", "Uploaded plan " + currSelectedPlan.getDisplayName())),
                () -> post(Notification.error("Telemetry Control", "Failed to upload plan " + currSelectedPlan.getDisplayName())));
    }

    private void sendPlanStart(String telemetryTarget, String imcTarget) {
        if (currSelectedPlan == null) {
            NeptusLog.pub().warn("Currently selected plan is null");
            GuiUtils.infoMessage(this, "Stop Plan Warning", "Currently selected plan is null");
            return;
        }

        NeptusLog.pub().info("Start plan " + currSelectedPlan.getDisplayName() + " to " + imcTarget + "( " + ImcSystemsHolder.lookupSystemByName(imcTarget).getId().intValue() + ") through " + telemetryTarget);

        PlanControl pc = new PlanControl();
        pc.setType(PlanControl.TYPE.REQUEST);
        pc.setOp(PlanControl.OP.START);
        pc.setRequestId(IMCSendMessageUtils.getNextRequestId());
        pc.setPlanId(currSelectedPlan.getId());
        pc.setArg(currSelectedPlan.asIMCPlan());
        pc.setDst(ImcSystemsHolder.lookupSystemByName(imcTarget).getId().intValue());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            pc.serialize(new IMCOutputStream(baos));
        } catch (IOException e) {
            GuiUtils.errorMessage(I18n.text("Start Plan Error"), e.getMessage());
            return;
        }

        long reqId = dispatchTelemetry(telemetryTarget, imcTarget, TelemetryMsg.CODE.CODE_IMC, TelemetryMsg.STATUS.EMPTY, true, baos.toByteArray());

        if (reqId == -1)
            return;

        waitForAck(reqId,
                () -> post(Notification.success("Telemetry Control", "Sent plan start " + currSelectedPlan.getDisplayName())),
                () -> post(Notification.error("Telemetry Control", "Failed to send plan start " + currSelectedPlan.getDisplayName())));
    }

    private void sendPlanStop(String telemetryTarget, String imcTarget) {
        if (currSelectedPlan == null) {
            NeptusLog.pub().warn("Currently selected plan is null");
            GuiUtils.infoMessage(this, "Stop Plan Warning", "Currently selected plan is null");
            return;
        }

        NeptusLog.pub().info("Send plan stop " + currSelectedPlan.getDisplayName() + " to " + imcTarget + " through " + telemetryTarget);

        PlanControl pc = new PlanControl();
        pc.setType(PlanControl.TYPE.REQUEST);
        pc.setOp(PlanControl.OP.STOP);
        pc.setRequestId(IMCSendMessageUtils.getNextRequestId());
        pc.setPlanId(currSelectedPlan.getId());
        pc.setArg(currSelectedPlan.asIMCPlan());
        pc.setDst(ImcSystemsHolder.lookupSystemByName(imcTarget).getId().intValue());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            pc.serialize(new IMCOutputStream(baos));
        } catch (IOException e) {
            GuiUtils.errorMessage(I18n.text("Send Plan Error"), e.getMessage());
            return;
        }

        long reqId = dispatchTelemetry(telemetryTarget, imcTarget, TelemetryMsg.CODE.CODE_IMC, TelemetryMsg.STATUS.EMPTY, true, baos.toByteArray());

        if (reqId == -1)
            return;

        waitForAck(reqId,
                () -> post(Notification.success("Telemetry Control", "Sent plan stop " + currSelectedPlan.getDisplayName())),
                () -> post(Notification.error("Telemetry Control", "Failed to send plan stop " + currSelectedPlan.getDisplayName())));
    }

    /**
     * Send a telemetry message to the given system.
     * @param targetSystem Receiver of this message
     * @param code Telemetry message code
     * @param status Telemetry message status
     * @param data Aditional raw data to send (e.g. IMC messages). Can be null
     * */
    long dispatchTelemetry(String gatewaySystem, String targetSystem, TelemetryMsg.CODE code, TelemetryMsg.STATUS status, boolean requestAck, byte[] data) {
        TelemetryMsg msg = new TelemetryMsg();
        msg.setType(TelemetryMsg.TYPE.TX);
        msg.setDestination(targetSystem);
        msg.setDst(ImcSystemsHolder.lookupSystemByName(gatewaySystem).getId().intValue());
        msg.setCode(code);
        msg.setStatus(status);
        msg.setAcknowledge(requestAck ? TelemetryMsg.TM_AK : TelemetryMsg.TM_NAK);
        msg.setTtl(ackTimeoutSeconds);
        msg.setReqId(requestId+1);

        if (data != null)
            msg.setData(data);

        boolean ret = IMCSendMessageUtils.sendMessage(msg, I18n.text("Error sending plan"), false, gatewaySystem);

        if (ret)
            return ++requestId;

        return -1;
    }

    private void toogleTelemetry() {
        if (toggleTelemetry.isSelected())
            toggleTelemetry.setText(I18n.text("ON"));
        else {
            toggleTelemetry.setBackground(COLOR_RED);
            toggleTelemetry.setText(I18n.text("OFF"));
        }
    }

    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {

    }

    @Override
    public void planChange(PlanType plan) {
        currSelectedPlan = plan;
        toogleControolButtons();
    }

    /**
     * Create thread to wait to ACK
     * Execute onSuccess if ACK is received; onError otherwise
     * */
    private void waitForAck(long reqId, Runnable onSuccess, Runnable onError) {
        new Thread(() -> {
            try {
                Thread.sleep(ackTimeoutSeconds * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                onError.run();
            }

            synchronized (acks) {
                if (acks.contains(reqId))
                    onSuccess.run();
                else
                    onError.run();

                acks.remove(reqId);
            }
        }).start();
    }
}
