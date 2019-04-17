/*
 * Copyright (c) 2004-2019 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Jul 21, 2011
 */
package pt.lsts.neptus.plugins.acoustic;

import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import javax.swing.*;
import pt.lsts.imc.*;
import pt.lsts.imc.sender.MessageEditor;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.*;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.txtmsg.SendTxtMessage;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "New Acoustic Operations", author = "ZP", icon = "pt/lsts/neptus/plugins/acoustic/manta.png")
@LayerPriority(priority = 40)
@Popup(name = "New Acoustic Operations", accelerator = KeyEvent.VK_M, width = 600, height = 400, pos = POSITION.CENTER, icon = "pt/lsts/neptus/plugins/acoustic/manta.png")
public class AcousticOperations extends ConsolePanel implements ConfigurationListener, Renderer2DPainter {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Systems listing", description = "Use commas to separate system identifiers")
    public String sysListing = "benthos-1,benthos-2,benthos-3,benthos-4,lauv-xtreme-2,lauv-noptilus-1,lauv-noptilus-2,lauv-noptilus-3";

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

    private final String TXT_PREFIX = "TXT";
    protected MessageEditor editor = new MessageEditor();

    private String selectedGateway = null;
    private String selectedTarget = null;

    //PANEL INFORMATION
    private LinkedHashMap<String, JButton> cmdButtons = new LinkedHashMap<>();
    private JTextArea infoArea = null;


    protected boolean initialized = false;

    /**
     * @param console
     */
    public AcousticOperations(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void propertiesChanged() {
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
        ctrlPanel.add(getAbortButton());

        return ctrlPanel;
    }
    private JTextArea getInfoArea() {
        final JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setBackground(Color.white);
        infoArea.setRows(6);
        return infoArea;
    }

    //SELECTION BOXES
    private JComboBox<String> getGatewaysSelect(){
        final JComboBox<String> gatewaySelect = new JComboBox<>(sysListing.split(","));
        gatewaySelect.insertItemAt("Any",0);
        gatewaySelect.setEditable(true);
        gatewaySelect.setSelectedItem("Select Gateway...");
        gatewaySelect.setEditable(false);
        gatewaySelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedGateway = e.getActionCommand();
                updateButtons();
            }
        });
        return gatewaySelect;
    }
    private JComboBox<String> getTargetSelect(){
        final JComboBox<String> targetSelect = new JComboBox<>();
        targetSelect.addItem("Test Target");
        targetSelect.setEditable(true);
        targetSelect.setSelectedItem("Select Target...");
        targetSelect.setEditable(false);
        targetSelect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedTarget = e.getActionCommand();
                updateButtons();
            }
        });
        return targetSelect;
    }

    //CONTROL BUTTONS
    private JButton getRangeButton() {
        final JButton rangeBtn = new JButton(I18n.text("Range system"));
        rangeBtn.setActionCommand("range");
        cmdButtons.put("range", rangeBtn);
        rangeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                ImcSystem[] sysLst;

                if (selectedGateway.equals(I18n.text("Any")))
                    sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation", VehicleType.SystemTypeEnum.ALL, true);
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
                    infoArea.setText(I18n.text("Please select a system."));
                }
                else {
                    IMCMessage m = IMCDefinition.getInstance().create("AcousticOperation", "op", "RANGE", "system",
                            selectedTarget);

                    cmdButtons.get("range").setEnabled(false);
                    SwingWorker<Integer, Void> sWorker = new SwingWorker<Integer, Void>() {
                        @Override
                        protected Integer doInBackground() throws Exception {
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
                                infoArea.setText(I18n.textf("Range %systemName commanded to %systemCount systems",
                                        selectedTarget, successCount));
                            }
                            else {
                                post(Notification.error(I18n.text("Range System"), I18n.text("Unable to range selected system"))
                                        .src(I18n.text("Console")));
                            }

                            cmdButtons.get("range").setEnabled(true);
                        }
                    };
                    sWorker.execute();
                }
            }
        });
        return rangeBtn;
    }
    private JButton getMessageButton() {
        final JButton messageBtn = new JButton(I18n.text("Send Message"));
        messageBtn.setActionCommand("message");
        cmdButtons.put("message", messageBtn);
        messageBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JDialog dialog = new JDialog(getConsole(), I18n.text("Send message acoustically"));
                dialog.setLayout(new BorderLayout());
                dialog.getContentPane().add(editor, BorderLayout.CENTER);
                JPanel bottom = new JPanel(new FlowLayout(FlowLayout.TRAILING));
                JButton btn = new JButton(I18n.text("Send"));
                btn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        sendAcoustically(selectedGateway, editor.getMessage());
                    }
                });
                bottom.add(btn);
                dialog.getContentPane().add(bottom, BorderLayout.SOUTH);
                dialog.setSize(600, 500);
                dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                GuiUtils.centerParent(dialog, getConsole());
                dialog.setVisible(true);
            }
        });
        return messageBtn;
    }
    private JButton getAbortButton() {
        final JButton abortButton = new JButton(I18n.text("Abort"));
        abortButton.setBackground(Color.red);
        abortButton.setActionCommand("abort");
        cmdButtons.put("abort", abortButton);
        JPanel context = this;
        abortButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int dialogResult = JOptionPane.showConfirmDialog(
                        context,
                        I18n.text("Are you sure you want to abort the current plan"),
                        I18n.text("Abort Plan"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if(dialogResult != JOptionPane.YES_OPTION){
                    return;
                }
                ImcSystem[] sysLst;

                if (selectedGateway.equals(I18n.text("Any"))) {
                    sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation", VehicleType.SystemTypeEnum.ALL, true);
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
                            .error(I18n.text("Abort"),
                                    I18n.text("No acoustic device is capable of sending this request"))
                            .src(I18n.text("Console")));
                }

                IMCMessage m = IMCDefinition.getInstance().create("AcousticOperation", "op", "ABORT", "system",
                        selectedTarget);

                int successCount = 0;
                for (ImcSystem sys : sysLst)
                    if (ImcMsgManager.getManager().sendMessage(m.cloneMessage(), sys.getId(), null))
                        successCount++;

                if (successCount > 0) {
                    infoArea.setText(I18n.textf("Abort %systemName commanded to %systemCount systems", selectedGateway,
                            successCount));
                }
                else {
                    post(Notification.error(I18n.text("Abort"), I18n.text("Unable to abort selected system"))
                            .src(I18n.text("Console")));
                }

            }
        });
        return abortButton;
    }
    private JButton getStartPlanButton() {
        final JButton startPlanButton = new JButton(I18n.text("Start/Resume Plan"));
        startPlanButton.setActionCommand("start");
        cmdButtons.put("start", startPlanButton);
        startPlanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                SendPlanDialog dialog = SendPlanDialog.sendPlan(getConsole());

                if (dialog == null)
                    return;

                boolean ret;

                dialog.selectedVehicle = getConsole().getMainSystem();
                if (dialog.isResume) {
                    ret = sendPlanResumeMessage(dialog.planId, dialog.startingManeuver);
                }
                else {
                    PlanControl pc = new PlanControl();
                    pc.setType(PlanControl.TYPE.REQUEST);
                    pc.setOp(PlanControl.OP.START);
                    pc.setPlanId(dialog.planId);
                    if (dialog.sendDefinition) {
                        try {
                            PlanType pt = getConsole().getMission().getIndividualPlansList().get(dialog.planId);
                            pc.setArg(pt.asIMCPlan(false));
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error("Error retriveing plan from mission", e);
                            return;
                        }
                    }

                    ret = IMCSendMessageUtils.sendMessageByAcousticModem(pc, dialog.selectedVehicle, true, gatewaysLookup());
                }

                if (!ret) {
                    String errorTextForDialog = I18n.textf("Error sending message to %sys.", dialog.selectedVehicle);
                    post(Notification.error(I18n.text("Send Message"), errorTextForDialog).src(I18n.text("Console")));
                }
            }
        });
        return startPlanButton;
    }

    //UTILITIES
    private boolean sendAcoustically(String destination, IMCMessage msg) {
        ImcSystem[] sysLst = gatewaysLookup();
        AcousticOperation op = new AcousticOperation(AcousticOperation.OP.MSG, destination, 0, msg);

        if (sysLst.length == 0) {
            post(Notification
                    .error(I18n.text("Send message"),
                            I18n.text("No acoustic device is capable of sending this message"))
                    .src(I18n.text("Console")));
            return false;
        }

        int successCount = 0;
        for (ImcSystem sys : sysLst)
            if (ImcMsgManager.getManager().sendMessage(op.cloneMessage(), sys.getId(), null))
                successCount++;

        if (successCount > 0) {
            infoArea.setText(I18n.textf(
                    "Request to send message to %systemName via %systemCount acoustic gateways", destination,
                    successCount));
            return true;
        }
        else {
            post(Notification.error(I18n.text("Send message"),
                    I18n.textf("Unable to send message to system %systemName", destination))
                    .src(I18n.text("Console")));
            return false;
        }
    }
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
    }
    private ImcSystem[] gatewaysLookup() {
        ImcSystem[] sysLst;
        if (selectedGateway.equals(I18n.text("Any")))
            sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation", VehicleType.SystemTypeEnum.ALL, true);
        else {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(selectedGateway);
            if (sys != null)
                sysLst = new ImcSystem[] { sys };
            else
                sysLst = new ImcSystem[] {};
        }
        return sysLst;
    }
    private boolean sendPlanResumeMessage(String planId, String maneuverId){
        TextMessage msgTxt = new TextMessage();
        String msgStr = "resume " + planId.trim() + " " + maneuverId.trim();
        msgTxt.setOrigin(GeneralPreferences.imcCcuName.toLowerCase());
        msgTxt.setText(msgStr);

        return IMCSendMessageUtils.sendMessageByAcousticModem(msgTxt, selectedTarget, true, gatewaysLookup());
    }

    @Periodic(millisBetweenUpdates = 1000000000)
    public void updateGateways(){
        ImcSystem[] sysList;
        sysList = ImcSystemsHolder.lookupSystemByService("acoustic/operation", VehicleType.SystemTypeEnum.ALL, true);
        System.out.println("Arrays.toString(sysList) = " + Arrays.toString(sysList));
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!showRanges)
            return;
    }

    /*
     * (non-Javadoc)
     *
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        getConsole().getImcMsgManager().removeListener(this);
    }

    public static void main(String[] args) {
        ConsoleParse.testSubPanel(AcousticOperations.class);
    }
}
