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
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanDB;
import pt.lsts.imc.TelemetryMsg;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.params.ConfigurationManager;
import pt.lsts.neptus.params.SystemProperty;
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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
    /** Name given to the DUNE parameter used for telemetry bind **/
    private final String bindParamStr = "Vehicle to bind";

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

            newSystems.stream().forEach(newSystem -> requestTelemetryInfo(newSystem));
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
        requestTelemetryInfo(srcName);
    }

    /**
     * Check given system's DUNE parameters for the system to bind
     * and register it. Used when radio model is 3DR or RDFXXXXPTP
     * @param sys System's name (e.g. manta-3)
     * */
    private boolean registerSystemBinds(String sys) {
        ArrayList<SystemProperty> params = ConfigurationManager.getInstance().getProperties(sys,
                SystemProperty.Visibility.USER, SystemProperty.Scope.GLOBAL);

        Optional<SystemProperty> res = params.stream().filter(param -> param.getName().equals(bindParamStr)).findAny();

        if (!res.isPresent()) {
            post(Notification.error("Telemetry Control", "Could not find parameter \"" + bindParamStr + "\""));
            return false;
        }

        SystemProperty bindParam = res.get();
        String systemToBind = (String) bindParam.getValue();
        NeptusLog.pub().info("Binding " + sys + " to " + systemToBind);

        // register bind
        this.telemetryBinds.getOrDefault(sys, new HashSet<>()).add(systemToBind);
        return true;
    }

    /**
     * Register given mesh as system's bindings.
     * Used when radio model "is mesh"
     * @param sys System's name (e.g. manta-3)
     * @param mesh The given system's bindings
     * */
    private boolean registerSystemMeshBindings(String sys, String[] mesh) {
        if (mesh.length == 0) {
            NeptusLog.pub().info("There are no bindings for " + sys);
            return false;
        }

        HashSet<String> binds = this.telemetryBinds.getOrDefault(sys, new HashSet<>());
        Arrays.stream(mesh).forEach(s -> binds.add(s));

        return true;
    }

    @Subscribe
    public void consume(CommSystemsQuery msg) {
        if ((msg.getType() & CommSystemsQuery.CIQ_QUERY) != 0)
            return;

        if ((msg.getCommInterface() & CommSystemsQuery.CIQ_RADIO) == 0)
            return;

        NeptusLog.pub().info("Checking bindings for " + msg.getSourceName());
        boolean ret;
        // point to point
        if (msg.getModel() == CommSystemsQuery.MODEL.M3DR || msg.getModel() == CommSystemsQuery.MODEL.RDFXXXXPTP)
            ret = registerSystemBinds(msg.getSourceName());
        // mesh
        else
            ret = registerSystemMeshBindings(msg.getSourceName(), msg.getList().split(","));

        // failed to find bindings
        if (!ret)
            return;

        synchronized (availableTelemetrySystems) {
            availableTelemetrySystems.add(msg.getSourceName());
            sourcesList.addItem(msg.getSourceName());
        }
    }

    @Subscribe
    public void consume(TelemetryMsg msg) {
        switch (msg.getType()) {
            case TXSTATUS:
                consumeTelemetryMsgTxStatus(msg);
            case RX:
                consumeTelemetryMsgRx(msg);
            default:
                NeptusLog.pub().debug("TelemetryMsg.TX no handled");
        }
    }

    /**
     * Handle TXSTATUS type TelemetryMsg
     * */
    public void consumeTelemetryMsgTxStatus(TelemetryMsg msg) {
        NeptusLog.pub().info("Got ack from " + msg.getSourceName() + " for request " + msg.getReqId());
        // register successful sending of message
        if (msg.getStatus() == TelemetryMsg.STATUS.DONE)
            synchronized (acks) {
                acks.add(msg.getReqId());
            }
    }

    public void consumeTelemetryMsgRx(TelemetryMsg msg) {

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

        ByteBuffer bfr = ByteBuffer.allocate(pdb.getPayloadSize());

        int nbytes = pdb.serializePayload(bfr, 0);
        if (nbytes != pdb.getPayloadSize()) {
            GuiUtils.errorMessage(I18n.text("Send Plan Error"), "Wrong serialization size");
            return;
        }

        long reqId = dispatchTelemetry(telemetryTarget, imcTarget, TelemetryMsg.CODE.CODE_IMC, TelemetryMsg.STATUS.EMPTY, true, bfr.array());

        if (reqId == -1)
            return;

        scheduleAction(() -> {
            synchronized (acks) {
                if (acks.contains(reqId))
                    post(Notification.success("Telemetry Control", "Uploaded plan " + currSelectedPlan.getDisplayName()));
                else
                    post(Notification.error("Telemetry Control", "Failed to upload plan " + currSelectedPlan.getDisplayName()));
                acks.remove(reqId);
            }
        }, ackTimeoutSeconds);
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
        pc.setDst(ImcSystemsHolder.lookupSystemByName(imcTarget).getId().intValue());

        ByteBuffer bfr = ByteBuffer.allocate(pc.getPayloadSize());

        int nbytes = pc.serializePayload(bfr, 0);
        if (nbytes != pc.getPayloadSize()) {
            GuiUtils.errorMessage(I18n.text("Start Plan Error"), "Wrong serialization size");
            return;
        }

        long reqId = dispatchTelemetry(telemetryTarget, imcTarget, TelemetryMsg.CODE.CODE_IMC, TelemetryMsg.STATUS.EMPTY, true, bfr.array());

        if (reqId == -1)
            return;

        scheduleAction(() -> {
            synchronized (acks) {
                if (acks.contains(reqId))
                    post(Notification.success("Telemetry Control", "Sent plan start " + currSelectedPlan.getDisplayName()));
                else
                    post(Notification.error("Telemetry Control", "Failed to send plan start " + currSelectedPlan.getDisplayName()));
                acks.remove(reqId);
            }
        }, ackTimeoutSeconds);
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
        pc.setDst(ImcSystemsHolder.lookupSystemByName(imcTarget).getId().intValue());

        ByteBuffer bfr = ByteBuffer.allocate(pc.getPayloadSize());

        int nbytes = pc.serializePayload(bfr, 0);
        if (nbytes != pc.getPayloadSize()) {
            GuiUtils.errorMessage(I18n.text("Stop Plan Error"), "Wrong serialization size");
            return;
        }

        long reqId = dispatchTelemetry(telemetryTarget, imcTarget, TelemetryMsg.CODE.CODE_IMC, TelemetryMsg.STATUS.EMPTY, true, bfr.array());

        if (reqId == -1)
            return;

        scheduleAction(() -> {
            synchronized (acks) {
                if (acks.contains(reqId))
                    post(Notification.success("Telemetry Control", "Sent plan stop " + currSelectedPlan.getDisplayName()));
                else
                    post(Notification.error("Telemetry Control", "Failed to send plan stop " + currSelectedPlan.getDisplayName()));
                acks.remove(reqId);
            }
        }, ackTimeoutSeconds);
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
     * Schedule the given action to be started after timeoutSeconds
     * */
    private void scheduleAction(Runnable action, int timeoutSeconds) {
        new Thread(() -> {
            try {
                Thread.sleep(timeoutSeconds * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                GuiUtils.errorMessage("TelemetryControl", "Failed to schedule action due to \n" + e.getMessage());
            }

            action.run();
        }).start();
    }
}
