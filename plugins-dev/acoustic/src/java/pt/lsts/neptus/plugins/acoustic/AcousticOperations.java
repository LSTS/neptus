/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Eduardo Ramos
 * Apr 24, 2019
 */
package pt.lsts.neptus.plugins.acoustic;

import com.google.common.eventbus.Subscribe;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import pt.lsts.imc.AcousticOperation;
import pt.lsts.imc.AcousticSystems;
import pt.lsts.imc.AcousticSystemsQuery;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.MessagePart;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanDB;
import pt.lsts.imc.TextMessage;
import pt.lsts.imc.net.IMCFragmentHandler;
import pt.lsts.imc.sender.MessageEditor;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMessageSenderPanel;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author Eduardo Ramos
 * 
 */
@PluginDescription(name = "New Acoustic Operations", author = "Eduardo Ramos", icon = "pt/lsts/neptus/plugins/acoustic/manta.png")
@LayerPriority(priority = 40)
@Popup(name = "New Acoustic Operations", accelerator = KeyEvent.VK_M, width = 650, height = 600, pos = POSITION.CENTER, icon = "pt/lsts/neptus/plugins/acoustic/manta.png")
public class AcousticOperations extends ConsolePanel implements ConfigurationListener, Renderer2DPainter {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Systems listing", description = "Use commas to separate system identifiers")
    public String sysListing = "";

    @NeptusProperty(name = "Display ranges in the map")
    public boolean showRanges = true;

    @NeptusProperty(name = "Use system discovery", description = "Instead of a static list, receive supported systems from gateway")
    public boolean sysDiscovery = true;

    @NeptusProperty(name = "Separate ranging when using \"any\" gateway", category = "Any Gateway", userLevel = LEVEL.ADVANCED,
            description = "Introduces a time separation between messages when \"any\" gateway..")
    private boolean separateRangingForAnyGateway = true;

    @NeptusProperty(name = "Separate ranging when using \"any\" gateway time", category = "Any Gateway", userLevel = LEVEL.ADVANCED,
            description = "Time in seconds")
    private short separateRangingForAnyGatewaySeconds = 2;

    //SYSTEMS INFO
    private Vector<LocationType> rangeSources = new Vector<>();
    private Vector<Double> rangeDistances = new Vector<>();
    private HashSet<String> knownSystems = new HashSet<>();
    private String selectedGateway = null;
    private String selectedTarget = null;

    //COMPONENTS
    private LinkedHashMap<String, JButton> cmdButtons = new LinkedHashMap<>();
    private JComboBox<String> gatewaysCombo;
    private JComboBox<String> targetsCombo;
    private JTextArea infoArea = null;

    protected ImcMessageSenderPanel editor;
    protected boolean initialized = false;

    public AcousticOperations(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void propertiesChanged() {
        if(gatewaysCombo == null || targetsCombo == null){
            return;
        }

        //UPDATE GATEWAYS COMBO
        String gwMemory = selectedGateway;
        gatewaysCombo.removeAllItems();
        for(ImcSystem sys : ImcSystemsHolder.lookupSystemByService("acoustic/operation", VehicleType.SystemTypeEnum.ALL, true)){
            knownSystems.add(sys.getName().trim());
            gatewaysCombo.addItem(sys.getName().trim());
        }
        if (gwMemory != null){
            gatewaysCombo.setSelectedItem(gwMemory);
        }

        //UPDATE TARGETS COMBO
        for (String s : sysListing.split(",")) {
            if (!s.isEmpty() && !s.endsWith(" list"))
                knownSystems.add(s.trim());
        }

        ArrayList<String> systems = new ArrayList<>(knownSystems);
        Collections.sort(systems);

        String targetMemory = selectedTarget;
        targetsCombo.removeAllItems();
        for (String s : systems) {
            targetsCombo.addItem(s);
        }
        if (targetMemory != null){
            targetsCombo.setSelectedItem(targetMemory);
        }
    }

    @Override
    public void initSubPanel() {
        if (initialized)
            return;
        initialized = true;

        getConsole().getImcMsgManager().addListener(this);

        setLayout(new BorderLayout());

        add(getSelectionPanel(), BorderLayout.NORTH);
        add(getControlPanel(),BorderLayout.CENTER);
        add(new JScrollPane(getInfoArea()),BorderLayout.SOUTH);

        updateButtons();
    }

    //SUB PANELS
    private JPanel getSelectionPanel() {
        final JPanel selectionPanel = new JPanel();
        selectionPanel.setLayout(new GridLayout(2, 2, 2, 2));
        selectionPanel.add(new JLabel(I18n.text("Gateway:")));
        selectionPanel.add(new JLabel(I18n.text("Target:")));
        selectionPanel.add(getGatewaysSelect());
        selectionPanel.add(getTargetSelect());

        return selectionPanel;
    }

    private JPanel getControlPanel() {
        final JPanel ctrlPanel = new JPanel();
        ctrlPanel.setLayout(new GridLayout(0, 1, 2, 2));
        ctrlPanel.add(getRangeButton());
        ctrlPanel.add(getMessageButton());
        ctrlPanel.add(getStartPlanButton());
        ctrlPanel.add(getCtrlsBottomRow());
        ctrlPanel.add(getAbortButton());

        return ctrlPanel;
    }

    private JTextArea getInfoArea() {
        final JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setBackground(Color.white);
        infoArea.setRows(6);
        this.infoArea = infoArea;
        
        return infoArea;
    }

    //SELECTION BOXES
    private JComboBox<String> getGatewaysSelect() {
        if (gatewaysCombo != null)
            return gatewaysCombo;
        final JComboBox<String> gatewaySelect = new JComboBox<>();
        gatewaySelect.insertItemAt("Any", 0);
        gatewaySelect.setEditable(true);
        gatewaySelect.setSelectedItem("Select Gateway...");
        gatewaySelect.setEditable(false);
        gatewaySelect.addActionListener(e -> {
            if (gatewaysCombo.getSelectedItem() != null) {
                selectedGateway = gatewaysCombo.getSelectedItem().toString();
                AcousticSystemsQuery asq = new AcousticSystemsQuery();
                send(selectedGateway, asq);
            }
            updateButtons();
        });
        for (ImcSystem sys : ImcSystemsHolder.lookupSystemByService("acoustic/operation",
                VehicleType.SystemTypeEnum.ALL, true)) {
            gatewaySelect.addItem(sys.getName().trim());
        }
        gatewaysCombo = gatewaySelect;
        return gatewaySelect;
    }

    private JComboBox<String> getTargetSelect() {
        if (targetsCombo != null)
            return targetsCombo;
        final JComboBox<String> targetSelect = new JComboBox<>();
        targetSelect.setEditable(true);
        targetSelect.setSelectedItem("Select Target...");
        targetSelect.setEditable(false);
        targetSelect.addActionListener(e -> {
            if (targetsCombo.getSelectedItem() != null)
                selectedTarget = targetsCombo.getSelectedItem().toString();
            updateButtons();
        });
        for (ImcSystem sys : ImcSystemsHolder.lookupSystemByService("acoustic/operation",
                VehicleType.SystemTypeEnum.ALL, true)) {
            targetSelect.addItem(sys.getName().trim());
        }
        targetsCombo = targetSelect;
        return targetSelect;
    }

    //CONTROL BUTTONS
    private JButton getRangeButton() {
        final JButton rangeBtn = new JButton(I18n.text("Range system"));
        rangeBtn.setActionCommand("range");
        cmdButtons.put("range", rangeBtn);
        rangeBtn.addActionListener(event -> {
            ImcSystem[] sysLst;

            if (selectedGateway.equals(I18n.text("Any")))
                sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation", VehicleType.SystemTypeEnum.ALL,
                        true);
            else {
                ImcSystem sys = ImcSystemsHolder.lookupSystemByName(selectedGateway);
                if (sys != null)
                    sysLst = new ImcSystem[] { sys };
                else
                    sysLst = new ImcSystem[] {};
            }

            if (sysLst.length == 0) {
                post(Notification
                        .error(I18n.text("Range System"),
                                I18n.text("No acoustic device is capable of sending this request"))
                        .src(I18n.text("Console")));
            }

            if (selectedTarget == null) {
                addText(I18n.text("Please select a system."));
            }
            else {
                IMCMessage m = IMCDefinition.getInstance().create("AcousticOperation", "op", "RANGE", "system",
                        selectedTarget);

                cmdButtons.get("range").setEnabled(false);
                SwingWorker<Integer, Void> sWorker = new SwingWorker<Integer, Void>() {
                    @Override
                    protected Integer doInBackground() {
                        int successCount = 0;
                        for (ImcSystem sys : sysLst) {
                            if (ImcMsgManager.getManager().sendMessage(m.cloneMessage(), sys.getId(), null))
                                successCount++;
                            if (separateRangingForAnyGateway && sysLst.length > 1) {
                                try {
                                    Thread.sleep(separateRangingForAnyGatewaySeconds * 1000);
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().warn(e);
                                }
                            }
                        }
                        return successCount;
                    }

                    @Override
                    protected void done() {
                        int successCount = 0;
                        try {
                            successCount = get();
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e);
                        }

                        if (successCount > 0) {
                            addText(I18n.textf("Range %systemName commanded to %systemCount systems", selectedTarget,
                                    successCount));
                        }
                        else {
                            post(Notification
                                    .error(I18n.text("Range System"), I18n.text("Unable to range selected system"))
                                    .src(I18n.text("Console")));
                        }

                        cmdButtons.get("range").setEnabled(true);
                    }
                };
                sWorker.execute();
            }
        });
        return rangeBtn;
    }

    private JButton getMessageButton() {
        final JButton messageBtn = new JButton(I18n.text("Send Message"));
        messageBtn.setActionCommand("message");
        cmdButtons.put("message", messageBtn);
        messageBtn.addActionListener(event -> {
            JDialog dialog = new JDialog(getConsole(), I18n.text("Send message acoustically"));
            dialog.setLayout(new BorderLayout());
            JButton btn = new JButton(I18n.text("Send"));
            editor = new ImcMessageSenderPanel(btn);
//            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.TRAILING));
            btn.addActionListener(e -> {
                ImcSystem gw = ImcSystemsHolder.lookupSystemByName(selectedGateway);
                IMCSendMessageUtils.sendMessageByAcousticModem(editor.getMessage(), selectedTarget, true,
                        new ImcSystem[] { gw });
            });
            dialog.getContentPane().add(editor, BorderLayout.CENTER);
//            bottom.add(btn);
//            dialog.getContentPane().add(bottom, BorderLayout.SOUTH);
            dialog.setSize(600, 500);
            dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
            GuiUtils.centerParent(dialog, getConsole());
            dialog.setVisible(true);
        });
        return messageBtn;
    }

    private JButton getAbortButton() {
        final JButton abortButton = new JButton(I18n.text("Abort"));
        abortButton.setBackground(Color.red);
        abortButton.setActionCommand("abort");
        cmdButtons.put("abort", abortButton);
        JPanel context = this;
        abortButton.addActionListener(event -> {
            int dialogResult = JOptionPane.showConfirmDialog(context,
                    I18n.text("Are you sure you want to abort the current plan"), I18n.text("Abort Plan"),
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (dialogResult != JOptionPane.YES_OPTION) {
                addText(I18n.text("Abort Cancelled"));
                return;
            }
            ImcSystem[] sysLst;

            if (selectedGateway.equals(I18n.text("Any"))) {
                sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation", VehicleType.SystemTypeEnum.ALL,
                        true);
            }
            else {
                ImcSystem sys = ImcSystemsHolder.lookupSystemByName(selectedGateway);
                if (sys != null)
                    sysLst = new ImcSystem[] { sys };
                else
                    sysLst = new ImcSystem[] {};
            }

            if (sysLst.length == 0) {
                post(Notification
                        .error(I18n.text("Abort"), I18n.text("No acoustic device is capable of sending this request"))
                        .src(I18n.text("Console")));
            }

            IMCMessage m = IMCDefinition.getInstance().create("AcousticOperation", "op", "ABORT", "system",
                    selectedTarget);

            int successCount = 0;
            for (ImcSystem sys : sysLst)
                if (ImcMsgManager.getManager().sendMessage(m.cloneMessage(), sys.getId(), null))
                    successCount++;

            if (successCount > 0) {
                addText(I18n.textf("Abort %systemName commanded to %systemCount systems", selectedGateway,
                        successCount));
            }
            else {
                post(Notification.error(I18n.text("Abort"), I18n.text("Unable to abort selected system"))
                        .src(I18n.text("Console")));
            }

        });
        return abortButton;
    }

    private JButton getStartPlanButton() {
        final JButton startPlanButton = new JButton(I18n.text("Start/Resume Plan"));
        startPlanButton.setActionCommand("start");
        cmdButtons.put("start", startPlanButton);
        startPlanButton.addActionListener(event -> {
            SendPlanDialog dialog = SendPlanDialog.sendPlan(getConsole());

            if (dialog == null)
                return;

            boolean ret = true;

            if (dialog.isResume) {
                ret = sendPlanResumeMessage(dialog.planId, dialog.startingManeuver);
            }
            else if (dialog.justSendPlan) {
                ret = sendPlan(dialog.planId, dialog.selectedVehicle);
            }
            else {
                PlanControl pc = new PlanControl();
                pc.setType(PlanControl.TYPE.REQUEST);
                pc.setOp(PlanControl.OP.START);
                pc.setPlanId(dialog.planId);
                pc.setFlags((dialog.skipCalibration ? 0 : PlanControl.FLG_CALIBRATE)
                        | (dialog.ignoreErrors ? PlanControl.FLG_IGNORE_ERRORS : 0));
                pc.setSrc(ImcMsgManager.getManager().getLocalId().intValue());
                pc.setDst(ImcSystemsHolder.getSystemWithName(dialog.selectedVehicle).getId().intValue());
                if (dialog.sendDefinition) {
                    try {
                        PlanType pt = getConsole().getMission().getIndividualPlansList().get(dialog.planId);
                        pc.setArg(pt.asIMCPlan());
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error("Error retrieving plan from mission", e);
                        return;
                    }
                }

                if (pc.getPayloadSize() > 1020) {
                    IMCFragmentHandler handler = new IMCFragmentHandler(IMCDefinition.getInstance());
                    try {
                        MessagePart[] parts = handler.fragment(pc, 1020);
                        for (MessagePart part : parts)
                            ret &= IMCSendMessageUtils.sendMessageByAcousticModem(part, dialog.selectedVehicle, true,
                                    gatewaysLookup());
                        NeptusLog.pub().info(
                                "Plan Control message with definition resulted in " + parts.length + " fragments");
                    }
                    catch (Exception ex) {
                        ret = false;
                        NeptusLog.pub().error(ex);
                        ex.printStackTrace();
                    }
                }
                else {
                    ret = IMCSendMessageUtils.sendMessageByAcousticModem(pc, dialog.selectedVehicle, true,
                            gatewaysLookup());
                }
            }

            if (!ret) {
                String errorTextForDialog = I18n.textf("Error sending message to %sys.", dialog.selectedVehicle);
                post(Notification.error(I18n.text("Send Message"), errorTextForDialog).src(I18n.text("Console")));
            }
        });
        return startPlanButton;
    }

    private JPanel getCtrlsBottomRow() {
        final JPanel row = new JPanel();
        row.setLayout(new GridLayout(0, 2, 2, 2));

        row.add(getRefreshStateButton());
        row.add(getClearRangesButton());

        return row;
    }

    private JButton getClearRangesButton() {
        final JButton clearRangesButton = new JButton(I18n.text("Clear Ranges"));
        clearRangesButton.setActionCommand("clear");
        cmdButtons.put("clear", clearRangesButton);
        clearRangesButton.addActionListener(e -> {
            rangeDistances.clear();
            rangeSources.clear();
        });
        return clearRangesButton;
    }

    private JButton getRefreshStateButton() {
        final JButton refreshStateButton = new JButton(I18n.text("Refresh Systems"));
        refreshStateButton.setActionCommand("refresh");
        cmdButtons.put("refresh", refreshStateButton);
        refreshStateButton.addActionListener(e -> {
            knownSystems.clear();
            requestSysListing();
        });
        return refreshStateButton;
    }

    //UTILITIES
    private void updateButtons() {
        if (selectedGateway == null || selectedTarget == null)
            for (String key : cmdButtons.keySet())
                cmdButtons.get(key).setEnabled(false);
        else {
            for (String key : cmdButtons.keySet())
                cmdButtons.get(key).setEnabled(true);

            if (selectedGateway.startsWith("lsts"))
                cmdButtons.get("abort").setEnabled(false);
        }
        cmdButtons.get("refresh").setEnabled(true);
    }

    private ImcSystem[] gatewaysLookup() {
        ImcSystem[] sysLst;
        if (selectedGateway == null || selectedGateway.equals(I18n.text("Any"))) {
            sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation", VehicleType.SystemTypeEnum.ALL, true);
        }
        else {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(selectedGateway);
            if (sys != null)
                sysLst = new ImcSystem[] { sys };
            else
                sysLst = new ImcSystem[] {};
        }
        return sysLst;
    }

    private boolean sendPlan(String planId, String selectedVehicle) {
        boolean ret = true;
        PlanType pt = getConsole().getMission().getIndividualPlansList().get(planId);
        if (pt == null) {
            GuiUtils.errorMessage(getConsole(), I18n.text("Send Plan acoustically"),
                    I18n.textf("Plan with id %id could not be found ", planId));
            return false;
        }
        PlanDB pdb = new PlanDB();
        pdb.setRequestId(IMCSendMessageUtils.getNextRequestId());
        pdb.setArg(pt.asIMCPlan());
        pdb.setOp(PlanDB.OP.SET);
        pdb.setPlanId(planId);

        if (pdb.getPayloadSize() > 1020) {
            IMCFragmentHandler handler = new IMCFragmentHandler(IMCDefinition.getInstance());
            try {
                MessagePart[] parts = handler.fragment(pdb, 1020);
                for (MessagePart part : parts)
                    ret &= IMCSendMessageUtils.sendMessageByAcousticModem(part, selectedVehicle, true,
                            gatewaysLookup());
                NeptusLog.pub().info("PlanDB message resulted in " + parts.length + " fragments");
            }
            catch (Exception ex) {
                NeptusLog.pub().error(ex);
                ex.printStackTrace();
            }
        }
        else {
            ret = IMCSendMessageUtils.sendMessageByAcousticModem(pdb, selectedVehicle, true, gatewaysLookup());
        }
        return ret;
    }

    private boolean sendPlanResumeMessage(String planId, String maneuverId) {
        TextMessage msgTxt = new TextMessage();
        String msgStr = "resume " + planId.trim() + " " + maneuverId.trim();
        msgTxt.setOrigin(GeneralPreferences.imcCcuName.toLowerCase());
        msgTxt.setText(msgStr);

        return IMCSendMessageUtils.sendMessageByAcousticModem(msgTxt, selectedTarget, true, gatewaysLookup());
    }

    public void addText(String text) {
        infoArea.setText(infoArea.getText() + " \n" + text);
        infoArea.scrollRectToVisible(new Rectangle(0, infoArea.getHeight() + 22, 1, 1));
    }

    //MESSAGE SUBSCRIPTIONS
    @Subscribe
    public void on(AcousticOperation msg) {
        switch (msg.getOp()) {
            case RANGE_RECVED:
                LocationType loc = new LocationType(MyState.getLocation());
                if (ImcSystemsHolder.getSystemWithName(msg.getSourceName()) != null)
                    loc = ImcSystemsHolder.getSystemWithName(msg.getSourceName()).getLocation();

                rangeDistances.add(msg.getRange());
                rangeSources.add(loc);

                addText(I18n.textf("Distance to %systemName is %distance", msg.getSystem(),
                        GuiUtils.getNeptusDecimalFormat(1).format(msg.getRange())));
                break;
            case ABORT_ACKED:
                addText(I18n.textf("%systemName has acknowledged abort command", msg.getSystem()));
                break;
            case BUSY:
                addText(I18n.textf("%manta is busy. Try again in a few moments", msg.getSourceName()));
                break;
            case NO_TXD:
                addText(I18n.textf("%manta has no acoustic transducer connected. Connect a transducer.",
                        msg.getSourceName()));
                break;
            case ABORT_IP:
                addText(I18n.textf("Aborting %systemName acoustically (via %manta)...", msg.getSystem(),
                        msg.getSourceName()));
                break;
            case ABORT_TIMEOUT:
                addText(I18n.textf("%manta timed out while trying to abort %systemName", msg.getSourceName(),
                        msg.getSystem()));
                break;
            case MSG_DONE:
                addText(I18n.textf("Message to %systemName has been sent successfully.", msg.getSystem()));
                break;
            case MSG_FAILURE:
                addText(I18n.textf("Failed to send message to %systemName.", msg.getSystem()));
                break;
            case MSG_IP:
                addText(I18n.textf("Sending message to %systemName...", msg.getSystem()));
                break;
            case MSG_QUEUED:
                addText(I18n.textf("Message to %systemName has been queued in %manta.", msg.getSystem(),
                        msg.getSourceName()));
                break;
            case RANGE_IP:
                addText(I18n.textf("Ranging of %systemName is in progress...", msg.getSystem()));
                break;
            case RANGE_TIMEOUT:
                addText(I18n.textf("Ranging of %systemName timed out.", msg.getSystem()));
                break;
            case UNSUPPORTED:
                addText(I18n.textf("The command is not supported by %manta.", msg.getSourceName()));
                break;
            default:
                addText(I18n.textf("[%manta]: %status", msg.getSourceName(), msg.getOp().toString()));
                break;
        }
    }

    @Subscribe
    public void on(AcousticSystems systems) {
        String acSystems = systems.getString("list", false);
        boolean newSystem = false;
        for (String s : acSystems.split(","))
            newSystem |= knownSystems.add(s);

        if (newSystem)
            propertiesChanged();
    }

    //Every 5 minutes, update listing of all reachable systems
    @Periodic(millisBetweenUpdates = 300000)
    private void requestSysListing() {
        if (sysDiscovery) {
            AcousticSystemsQuery asq = new AcousticSystemsQuery();
            for (ImcSystem s : gatewaysLookup()){
                send(s.getName(), asq);
            }
        }
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!showRanges)
            return;
        for (int i = 0; i < rangeSources.size(); i++) {
            double radius = rangeDistances.get(i) * renderer.getZoom();
            Point2D pt = renderer.getScreenPosition(rangeSources.get(i));

            if (i < rangeSources.size() - 1)
                g.setColor(new Color(255, 128, 0, 128));
            else
                g.setColor(new Color(255, 128, 0, 255));

            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(pt.getX() - radius, pt.getY() - radius, radius * 2, radius * 2));
        }
    }

    @Override
    public void cleanSubPanel() {
        getConsole().getImcMsgManager().removeListener(this);
    }

    public static void main(String[] args) {
        ConsoleParse.testSubPanel(AcousticOperations.class);
    }
}
