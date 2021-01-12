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
 * Author: Paulo Dias
 * 21 de Jul de 2010
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.jdesktop.swingx.JXLabel;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.LockableSubPanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.gui.system.EmergencyTaskSymbol;
import pt.lsts.neptus.gui.system.EmergencyTaskSymbol.EmergencyStatus;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.MultiSystemIMCMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Emergency Control", description = "This panel controls the emergency plan for the vehicle.", version = "1.0.0", author = "Paulo Dias")
public class EmergencyControlPanel extends ConsolePanel implements ConfigurationListener, MainVehicleChangeListener,
        LockableSubPanel, IPeriodicUpdates {

    // <message id="557" name="Emergency Control" abbrev="EmergencyControl" source="ccu">
    // <field name="Command" abbrev="command" type="uint8_t" prefix="ECTL" unit="Enumerated">
    // <enum id="0" name="Enable" abbrev="ENABLE"/>
    // <enum id="1" name="Disable" abbrev="DISABLE"/>
    // <enum id="2" name="Start" abbrev="START"/>
    // <enum id="3" name="Stop" abbrev="STOP"/>
    // <enum id="4" name="Query" abbrev="QUERY"/>
    // <enum id="5" name="Set Plan" abbrev="SET_PLAN"/>
    // </field>
    // <field type="message" name="Plan Specification" abbrev="plan"/>
    // </message>
    //
    // <message id="558" name="Emergency Control State" abbrev="EmergencyControlState" source="vehicle">
    // <field name="State" abbrev="state" type="uint8_t" prefix="ECS" unit="Enumerated">
    // <enum id="0" name="Not Configured" abbrev="NOT_CONFIGURED"/>
    // <enum id="1" name="Disabled" abbrev="DISABLED"/>
    // <enum id="2" name="Enabled" abbrev="ENABLED"/>
    // <enum id="3" name="Armed" abbrev="ARMED"/>
    // <enum id="4" name="Active" abbrev="ACTIVE"/>
    // <enum id="5" name="Stopping" abbrev="STOPPING"/>
    // </field>
    // <field name="Mission Id" abbrev="plan_id" type="plaintext"/>
    // <field name="Communications Level" abbrev="comm_level" type="uint8_t" unit="%" min="0" max="100"/>
    // </message>

    @NeptusProperty(name = "Font Size Multiplier", description = "The font size. Use '1' for default.")
    public int fontMultiplier = 1;

    private final ImageIcon ICON_UP = ImageUtils.getIcon("images/planning/up.png");
    private final ImageIcon ICON_START = ImageUtils.getIcon("images/planning/start.png");
    private final ImageIcon ICON_STOP = ImageUtils.getIcon("images/planning/stop.png");
    private final ImageIcon ICON_ENABLE = ImageUtils.getIcon("images/planning/enable.png");
    private final ImageIcon ICON_DISABLE = ImageUtils.getIcon("images/planning/disable.png");
    private final ImageIcon ICON_QUERY = ImageUtils.getIcon("images/planning/sendquery.png");

    public enum EmergencyStateEnum {
        UNKNOWN(-1, "Unknown"),
        NOT_CONFIGURED(0, "Not Configured"),
        DISABLED(1, "Desabled"),
        ENABLED(2, "Enabled"),
        ARMED(3, "Armed"),
        ACTIVE(4, "Active"),
        STOPPING(5, "Stopping");
        int id;
        String desc;

        EmergencyStateEnum(int id, String desc) {
            this.id = id;
            this.desc = desc;
        }

        public static EmergencyStateEnum translate(int id) {
            for (EmergencyStateEnum tmp : EmergencyStateEnum.values()) {
                if (tmp.id == id)
                    return tmp;
            }
            return UNKNOWN;
        }
    };

    private MultiSystemIMCMessageListener mainVehicleMessageListener = null;
    private EmergencyStateEnum reportedState = EmergencyStateEnum.UNKNOWN;
    private String reportedPlanId = "";
    private short reportedCommLevel = -1;
    private boolean locked = false;

    // GUI
    private JLabel titleLabel = null;
    private JXLabel planIdLabel = null;
    private Font planIdLabelFont = null;
    private ToolbarButton uploadPlanButton = null;
    private ToolbarButton sendQueryButton = null;
    private ToolbarButton sendEnableDisableButton = null;
    private ToolbarButton sendStartStopButton = null;
    private EmergencyTaskSymbol stateSymbolLabel = null;

    /**
	 * 
	 */
    public EmergencyControlPanel(ConsoleLayout console) {
        super(console);
        initialize();
    }

    /**
	 * 
	 */
    private void initialize() {
        removeAll();
        setSize(new Dimension(190, 60));
        setLayout(new BorderLayout());
        titleLabel = new JLabel(PluginUtils.getPluginName(this.getClass()));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9 * fontMultiplier));
        add(titleLabel, BorderLayout.NORTH);
        JPanel tmpPanel = new JPanel();
        tmpPanel.setLayout(new BoxLayout(tmpPanel, BoxLayout.LINE_AXIS));
        add(tmpPanel, BorderLayout.CENTER);
        planIdLabel = new JXLabel("");
        planIdLabelFont = planIdLabel.getFont();
        planIdLabel.setFont(planIdLabelFont.deriveFont((float) (planIdLabelFont.getSize() * fontMultiplier)));
        add(planIdLabel, BorderLayout.SOUTH);

        uploadPlanButton = new ToolbarButton(new AbstractAction("Upload Plan", ICON_UP) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        uploadPlanButton.setEnabled(false);
                        sendPlanSpecAsEmergencyPlan();
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e);
                        }
                        uploadPlanButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        });
        tmpPanel.add(uploadPlanButton);

        sendQueryButton = new ToolbarButton(new AbstractAction("Query", ICON_QUERY) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        sendQueryButton.setEnabled(false);
                        sendQuery();
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e);
                        }
                        sendQueryButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        });
        tmpPanel.add(sendQueryButton);

        sendEnableDisableButton = new ToolbarButton(new AbstractAction("Enable Plan", ICON_ENABLE) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String cmd = e.getActionCommand();
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        sendEnableDisableButton.setEnabled(false);
                        if ("Enable Plan".equalsIgnoreCase(cmd))
                            sendEnableDisableStartStop(0);
                        else if ("Disable Plan".equalsIgnoreCase(cmd))
                            sendEnableDisableStartStop(1);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e);
                        }
                        sendEnableDisableButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        });
        sendEnableDisableButton.setActionCommand("Enable Plan");
        tmpPanel.add(sendEnableDisableButton);

        sendStartStopButton = new ToolbarButton(new AbstractAction("Start Plan", ICON_START) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String cmd = e.getActionCommand();
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        sendStartStopButton.setEnabled(false);
                        if ("Start Plan".equalsIgnoreCase(cmd))
                            sendEnableDisableStartStop(2);
                        else if ("Stop Plan".equalsIgnoreCase(cmd))
                            sendEnableDisableStartStop(3);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e);
                        }
                        sendStartStopButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        });
        sendStartStopButton.setActionCommand("Start Plan");
        tmpPanel.add(sendStartStopButton);

        stateSymbolLabel = new EmergencyTaskSymbol();
        stateSymbolLabel.setSize(32, 32);
        stateSymbolLabel.setMaximumSize(new Dimension(32, 32));
        stateSymbolLabel.setSymbolHeight(32);
        stateSymbolLabel.setSymbolWidth(32);
        stateSymbolLabel.setActive(true);
        updateStatusSymbol();
        tmpPanel.add(stateSymbolLabel);

        initializeMainVehicleMessageListener();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        if (fontMultiplier < 1)
            return;
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9 * fontMultiplier));
        planIdLabel.setFont(planIdLabelFont.deriveFont((float) (planIdLabelFont.getSize() * fontMultiplier)));
        this.revalidate();
    }

    /**
	 * 
	 */
    private void refreshUI() {
        updateStatusSymbol();

        if (isLocked()) {
            planIdLabel.setEnabled(false);
            uploadPlanButton.setEnabled(false);
            sendQueryButton.setEnabled(false);
            sendEnableDisableButton.setEnabled(false);
            sendStartStopButton.setEnabled(false);
            stateSymbolLabel.setEnabled(false);
            return;
        }
        else {
            planIdLabel.setEnabled(true);
            uploadPlanButton.setEnabled(true);
            sendQueryButton.setEnabled(true);
            sendEnableDisableButton.setEnabled(true);
            sendStartStopButton.setEnabled(true);
            stateSymbolLabel.setEnabled(true);
        }

        if (reportedState == EmergencyStateEnum.NOT_CONFIGURED || reportedState == EmergencyStateEnum.UNKNOWN) {
            sendEnableDisableButton.setEnabled(false);
            sendStartStopButton.setEnabled(false);
        }
        else {
            sendEnableDisableButton.setEnabled(true);
            if (reportedState != EmergencyStateEnum.DISABLED) {
                sendEnableDisableButton.setActionCommand("Disable Plan");
                sendEnableDisableButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_DISABLE);
                sendEnableDisableButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION, "Disable Plan");
            }
            else {
                sendEnableDisableButton.setActionCommand("Enable Plan");
                sendEnableDisableButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_ENABLE);
                sendEnableDisableButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION, "Enable Plan");
            }

            if (reportedState == EmergencyStateEnum.DISABLED)
                sendStartStopButton.setEnabled(false);
            else
                sendStartStopButton.setEnabled(true);
            if ((reportedState == EmergencyStateEnum.ENABLED || reportedState == EmergencyStateEnum.ARMED)
                    && reportedState != EmergencyStateEnum.DISABLED
                    && (reportedState != EmergencyStateEnum.ACTIVE || reportedState != EmergencyStateEnum.STOPPING)) {
                sendStartStopButton.setActionCommand("Start Plan");
                sendStartStopButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_START);
                sendStartStopButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION, "Start Plan");
            }
            else {
                sendStartStopButton.setActionCommand("Stop Plan");
                sendStartStopButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_STOP);
                sendStartStopButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION, "Stop Plan");
            }
        }
    }

    /**
	 * 
	 */
    private void updateStatusSymbol() {
        try {
            stateSymbolLabel.setStatus(EmergencyStatus.valueOf(reportedState.name()));
            stateSymbolLabel.setActive(true);
        }
        catch (Exception e) {
            e.printStackTrace();
            stateSymbolLabel.setStatus(EmergencyStatus.NOT_CONFIGURED); // this happens on unknown state
            stateSymbolLabel.setActive(false);
        }
    }

    /**
	 * 
	 */
    private void initializeMainVehicleMessageListener() {
        if (mainVehicleMessageListener != null)
            return;
        mainVehicleMessageListener = new MultiSystemIMCMessageListener(this.getClass().getSimpleName() + " ["
                + Integer.toHexString(hashCode()) + "]") {
            @Override
            public void messageArrived(ImcId16 id, IMCMessage msg) {
                if ("EmergencyControlState".equalsIgnoreCase(msg.getAbbrev())) {
                    String strPlanId;
                    strPlanId = msg.getString("plan_id");
                    
                    if(strPlanId == null)
                        strPlanId = msg.getString("mission_id");
                    
                    int stateEn = msg.getInteger("state");
                    int commLevel = (int) msg.getDouble("comm_level");

                    setReportedState(EmergencyStateEnum.translate(stateEn));
                    setReportedPlanId(strPlanId);
                    setReportedCommLevel((short) commLevel);
                    refreshUI();
                }
            }
        };
        mainVehicleMessageListener.setMessagesToListen("EmergencyControlState");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.SubPanel#postLoadInit()
     */
    @Override
    public void initSubPanel() {
        mainVehicleMessageListener.setSystemToListenStrings(getMainVehicleId());
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#clean()
     */
    @Override
    public void cleanSubPanel() {
        mainVehicleMessageListener.clean();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#mainVehicleChange(java.lang.String)
     */
    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange evt) {
        mainVehicleMessageListener.setSystemToListenStrings(evt.getCurrent());
        setReportedState(EmergencyStateEnum.UNKNOWN);
        setReportedPlanId("");
        setReportedCommLevel((short) -1);
        update();
        refreshUI();
    }

    /**
     * @return the reportedPlanId
     */
    public String getReportedPlanId() {
        return reportedPlanId;
    }

    /**
     * @param reportedPlanId the reportedPlanId to set
     */
    private void setReportedPlanId(String reportedPlanId) {
        this.reportedPlanId = reportedPlanId;
        planIdLabel.setText("<html><b>Plan: </b>" + reportedPlanId);
        planIdLabel.setToolTipText("Plan: " + reportedPlanId);
        planIdLabel.repaint();
    }

    /**
     * @return the reportedState
     */
    public EmergencyStateEnum getReportedState() {
        return reportedState;
    }

    /**
     * @param reportedState the reportedState to set
     */
    private void setReportedState(EmergencyStateEnum reportedState) {
        this.reportedState = reportedState;
    }

    /**
     * @return the reportedCommLevel
     */
    public short getReportedCommLevel() {
        return reportedCommLevel;
    }

    /**
     * @param reportedCommLevel the reportedCommLevel to set
     */
    private void setReportedCommLevel(short reportedCommLevel) {
        this.reportedCommLevel = reportedCommLevel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        return 5000;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
        ImcSystem tmp = ImcSystemsHolder.lookupSystemByName(getMainVehicleId());
        if (tmp != null) {
            reportedPlanId = tmp.getEmergencyPlanId();
            try {
                setReportedState(EmergencyStateEnum.valueOf(tmp.getEmergencyStatusStr()));
                setReportedPlanId(tmp.getEmergencyPlanId());
            }
            catch (Exception e) {
                 e.printStackTrace();
            }
            refreshUI();
        }
        return true;
    }

    protected boolean sendPlanSpecAsEmergencyPlan() {
        if (!checkConditionToRun(this, true, true))
            return false;

        IMCMessage msgEmergencyControl = IMCDefinition.getInstance().create("EmergencyControl");

        msgEmergencyControl.setValue("command", "SET_PLAN");

        IMCMessage msgPlanSpecification = getPlanAsSpecification();

        if (msgPlanSpecification == null) {
            GuiUtils.errorMessage(EmergencyControlPanel.this, "Send Emergency Control Mission Plan",
                    "Error sending MissionSpecification message!\n" + "No Mission Plan Spec. valid!");
        }

        if (msgEmergencyControl.getMessageType().getFieldType("plan") != null)
            msgEmergencyControl.setValue("plan", msgPlanSpecification);
        else
            msgEmergencyControl.setValue("mission", msgPlanSpecification);

        // String missionlog = GuiUtils.getLogFileName("mission_state", "zip");
        // getConsole().getMission().asZipFile(missionlog, true);

        // msgEmergencyControl.dump(System.out);
        return sendTheMessage(msgEmergencyControl, "Error sending Plan message!");
    }

    protected boolean sendQuery() {
        IMCMessage msgEmergencyControl = IMCDefinition.getInstance().create("EmergencyControl");

        msgEmergencyControl.setValue("command", "QUERY");

        return sendTheMessage(msgEmergencyControl, "Error sending Query Command message!");
    }

    protected boolean sendEnableDisableStartStop(int enableOrDisableOrStartOrStopCmd) {
        IMCMessage msgEmergencyControl = IMCDefinition.getInstance().create("EmergencyControl");

        String enumerated = "";
        String cmdStr = "Unknown";
        try {
            switch (enableOrDisableOrStartOrStopCmd) {
                case 0:
                    enumerated = "ENABLE";
                    cmdStr = "Enable";
                    break;
                case 1:
                    enumerated = "DISABLE";
                    cmdStr = "Disable";
                    break;
                case 2:
                    enumerated = "START";
                    cmdStr = "Start";
                    break;
                case 3:
                    enumerated = "STOP";
                    cmdStr = "Stop";
                    break;
                default:
                    break;
            }
        }
        catch (Exception ex) {
            NeptusLog.pub().error(this, ex);
        }

        msgEmergencyControl.setValue("command", enumerated);

        return sendTheMessage(msgEmergencyControl, "Error sending " + cmdStr + " Command message!");
    }

    /**
     * @return
     * 
     */
    private boolean sendTheMessage(IMCMessage msg, String errorTextForDialog) {
        String vid = getConsole().getMainSystem();

        boolean ret = ImcMsgManager.getManager().sendMessageToSystem(msg, vid, null, null);

        if (!ret) {
            GuiUtils.errorMessage(EmergencyControlPanel.this, "Send Emergency Control Message", errorTextForDialog);
            return false;
        }
        return true;
    }

    /**
     * @return
     */
    protected IMCMessage getPlanAsSpecification() {
        if (!checkConditionToRun(this, true, true))
            return null;
        ConsoleLayout cons = getConsole();
        // MissionType miss = cons.getMission();
        PlanType plan = cons.getPlan();

        if (!(plan instanceof PlanType))
            return null;

        PlanType iPlan = (PlanType) plan;
        return iPlan.asIMCPlan();
    }

    /**
     * @param component
     * @param checkMission
     * @param checkPlan
     * @return
     */
    protected boolean checkConditionToRun(Component component, boolean checkMission, boolean checkPlan) {
        if (!ImcMsgManager.getManager().isRunning()) {
            GuiUtils.errorMessage(EmergencyControlPanel.this, component.getName(), "IMC comms. are not running!");
            return false;
        }

        ConsoleLayout cons = getConsole();
        if (cons == null) {
            GuiUtils.errorMessage(EmergencyControlPanel.this, component.getName(), "Missing console attached!");
            return false;
        }

        if (checkMission) {
            MissionType miss = cons.getMission();
            if (miss == null) {
                GuiUtils.errorMessage(EmergencyControlPanel.this, component.getName(), "Missing attached mission!");
                return false;
            }
        }

        if (checkPlan) {
            PlanType plan = cons.getPlan();
            if (plan == null) {
                GuiUtils.errorMessage(EmergencyControlPanel.this, component.getName(), "Missing attached plan!");
                return false;
            }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.LockableSubPanel#lock()
     */
    @Override
    public void lock() {
        locked = true;
        refreshUI();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.LockableSubPanel#unLock()
     */
    @Override
    public void unLock() {
        locked = false;
        refreshUI();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.LockableSubPanel#isLocked()
     */
    @Override
    public boolean isLocked() {
        return locked;
    }

    public static void main(String[] args) {
        GuiUtils.setLookAndFeel();
        ConsoleParse.dummyConsole(new ConsolePanel[] { new EmergencyControlPanel(null) });
    }
}
