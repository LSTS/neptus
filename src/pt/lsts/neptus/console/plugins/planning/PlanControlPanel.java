/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 21/06/2011
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.AcousticOperation;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.LblBeacon;
import pt.lsts.imc.LblConfig;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.imc.PlanDB;
import pt.lsts.imc.PlanDB.OP;
import pt.lsts.imc.Teleoperation;
import pt.lsts.imc.TeleoperationDone;
import pt.lsts.imc.VehicleState;
import pt.lsts.imc.VehicleState.OP_MODE;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.console.plugins.IPlanSelection;
import pt.lsts.neptus.console.plugins.ITransponderSelection;
import pt.lsts.neptus.console.plugins.LockableSubPanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.SystemsList;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBState;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.gui.system.btn.SystemsSelectionAction;
import pt.lsts.neptus.gui.system.btn.SystemsSelectionAction.SelectionType;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.map.TransponderUtils;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Plan Control", author = "Paulo Dias", version = "1.2.3", documentation = "plan-control/plan-control.html#PlanControl", category = CATEGORY.INTERFACE)
public class PlanControlPanel extends ConsolePanel implements ConfigurationListener, MainVehicleChangeListener,
LockableSubPanel, IPeriodicUpdates, NeptusMessageListener {

    protected static final boolean DONT_USE_ACOUSTICS = true;
    protected static final boolean USE_ACOUSTICS = false;

    private final ImageIcon ICON_BEACONS = ImageUtils
            .getIcon("images/planning/uploadBeacons.png");
    //private final ImageIcon ICON_BEACONS_ZERO = ImageUtils
    //        .getIcon("images/planning/uploadBeaconsZero.png");
    private final ImageIcon ICON_UP = ImageUtils.getIcon("images/planning/up.png");
    private final ImageIcon ICON_DOWN_R = ImageUtils.getIcon("images/planning/fileimport.png");
    private final ImageIcon ICON_START = ImageUtils.getIcon("images/planning/start.png");
    private final ImageIcon ICON_STOP = ImageUtils.getIcon("images/planning/stop.png");
    private final ImageIcon ICON_TELEOP_ON = ImageUtils.getScaledIcon(
            "images/planning/teleoperation.png", 32, 32);
    private final ImageIcon ICON_TELEOP_OFF = ImageUtils.getScaledIcon(
            "images/planning/teleoperation-off.png", 32, 32);

    private final String startTeleOperationStr = I18n.text("Start Tele-Operation");
    private final String stopTeleOperationStr = I18n.text("Stop Tele-Operation");
    private final String startPlanStr = I18n.text("Start Plan");
    private final String stopPlanStr = I18n.text("Stop Plan");
    private final String sendAcousticBeaconsStr = I18n.text("Send Acoustic Beacons");
    private final String sendSelectedPlanStr = I18n.text("Send Selected Plan");
    private final String downloadActivePlanStr = I18n.text("Download Active Plan");

    @NeptusProperty(name = "Font Size Multiplier", description = "The font size. Use '1' for default.")
    public int fontMultiplier = 1;

    @NeptusProperty(name = "Verify plans for island nodes", userLevel = LEVEL.ADVANCED,
            description = "Always runs a verification "
                    + "on the plan for maneuvers that have no input edges. If you choose to switch off here "
                    + "you can allways click Alt when sending the plan that this verification will run.")
    public boolean allwaysVerifyAllManeuversUsed = true;


    @NeptusProperty(name = "Service name for acoustic message sending", userLevel = LEVEL.ADVANCED, 
            distribution = DistributionEnum.DEVELOPER)
    public String acousticOpServiceName = "acoustic/operation";

    @NeptusProperty(name = "Use only active systems for acoustic message sending", userLevel = LEVEL.ADVANCED, 
            distribution = DistributionEnum.DEVELOPER)
    public boolean acousticOpUseOnlyActive = false;

    @NeptusProperty(name = "Use Full Mode or Teleoperation Mode", userLevel = LEVEL.ADVANCED, 
            description = "By default this value is true and makes "
                    + "it display all buttons, if false only teleoperation button is shown.")
    public boolean useFullMode = true;

    @NeptusProperty(name = "Enable teleoperation button", userLevel = LEVEL.ADVANCED, description = "Configures if send beacons button is shown or not in Full Mode.")
    public boolean enableTeleopButton = true;

    @NeptusProperty(name = "Enable console actions", editable = false)
    public boolean enableConsoleActions = true;

    // @NeptusProperty(name = "Use PlanDB to send plan", description = "For current vehicles set to true.")
    // public boolean usePlanDBToSendPlan = true;

    @NeptusProperty(name = "Enable selection button", userLevel = LEVEL.ADVANCED,
            description = "Configures if system selection button is active or not")
    public boolean enableSelectionButton = false;

    @NeptusProperty(name = "Enable beacons button", userLevel = LEVEL.ADVANCED,
            description = "Configures if send beacons button is active or not")
    public boolean enableBeaconsButton = true;

    @NeptusProperty(name = "Use Calibration on Start Plan", userLevel = LEVEL.ADVANCED)
    public boolean useCalibrationOnStartPlan = true;

    @NeptusProperty(name = "Use TCP To Send Messages", userLevel = LEVEL.ADVANCED)
    public boolean useTcpToSendMessages = true;

    // GUI
    private JPanel holder;
    private JLabel titleLabel;
    private JLabel planIdLabel;
    private ToolbarButton selectionButton, sendAcousticsButton, sendUploadPlanButton, sendDownloadPlanButton,
    sendStartButton, sendStopButton, teleOpButton;

    private SystemsSelectionAction selectionAction;
    private AbstractAction sendAcousticsAction, sendUploadPlanAction, sendDownloadPlanAction, sendStartAction,
    sendStopAction, teleOpAction;

    private int teleoperationManeuver = -1;
    {
        IMCMessage tomsg = IMCDefinition.getInstance().create("Teleoperation");
        if (tomsg != null)
            teleoperationManeuver = tomsg.getMgid();
    }

    private boolean locked = false;
    private final LinkedHashMap<Integer, Long> registerRequestIdsTime = new LinkedHashMap<Integer, Long>();
    private final LinkedHashMap<Integer, PlanControl> requests = new LinkedHashMap<>();
    private final String[] messagesToObserve = new String[] { "PlanControl", "PlanControlState", "VehicleState",
            "PlanDB", "LblConfig", "AcousticOperation" };

    public PlanControlPanel(ConsoleLayout console) {
        super(console);
        initialize();
    }

    @Override
    public String[] getObservedMessages() {
        return messagesToObserve;
    }

    private void initialize() {
        initializeActions();

        removeAll();
        setSize(new Dimension(255, 60));
        setLayout(new BorderLayout());
        // FIXI18N
        titleLabel = new JLabel(I18n.text(getName()));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9 * fontMultiplier));
        add(titleLabel, BorderLayout.NORTH);
        holder = new JPanel();
        holder.setLayout(new BoxLayout(holder, BoxLayout.LINE_AXIS));
        add(holder, BorderLayout.CENTER);

        planIdLabel = new JLabel("");
        add(planIdLabel, BorderLayout.SOUTH);

        selectionButton = new ToolbarButton(selectionAction);
        sendAcousticsButton = new ToolbarButton(sendAcousticsAction);
        sendUploadPlanButton = new ToolbarButton(sendUploadPlanAction);
        sendDownloadPlanButton = new ToolbarButton(sendDownloadPlanAction);
        sendStartButton = new ToolbarButton(sendStartAction);
        sendStartButton.setActionCommand(startPlanStr);
        sendStopButton = new ToolbarButton(sendStopAction);
        sendStopButton.setActionCommand(stopPlanStr);
        teleOpButton = new ToolbarButton(teleOpAction);
        teleOpButton.setActionCommand(startTeleOperationStr);

        holder.add(selectionButton);
        // holder.add(sendNavStartPointButton);
        holder.add(sendAcousticsButton);
        holder.add(sendUploadPlanButton);
        // holder.add(sendDownloadPlanButton);
        holder.add(sendStartButton);
        holder.add(sendStopButton);
        holder.add(teleOpButton);

        setModeComponentsVisibility();
    }

    /**
     * Parameter {@link #fontMultiplier} validator.
     * 
     * @param value
     * @return
     */
    public String validateFontMultiplier(int value) {
        if (value <= 0)
            return I18n.text("Values lower than zero are not valid!");
        if (value > 10)
            return I18n.text("Values bigger than 10 are not valid!");
        return null;
    }

    /**
     * 
     */
    private void setModeComponentsVisibility() {
        for (Component comp : holder.getComponents()) {
            comp.setVisible(useFullMode);
        }
        titleLabel.setVisible(useFullMode);
        if (useFullMode) {
            if (enableTeleopButton) {
                teleOpButton.setVisible(true);
            }
            else {
                teleOpButton.setVisible(false);
            }
        }
        else {
            teleOpButton.setVisible(true);
        }
        selectionButton.setVisible(enableSelectionButton && useFullMode);
        sendAcousticsButton.setVisible(enableBeaconsButton && useFullMode);
    }

    /**
     * 
     */
    private void initializeActions() {
        selectionAction = new SystemsSelectionAction(I18n.text("Using") + ":", 20);

        sendAcousticsAction = new AbstractAction(sendAcousticBeaconsStr, ICON_BEACONS) {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                final Object action = getValue(Action.NAME);
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        NeptusLog.action().info(action);

                        sendAcousticsButton.setEnabled(false);
                        sendAcoustics(false, getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(ev)));

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
                        sendAcousticsButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        };

        sendUploadPlanAction = new AbstractAction(sendSelectedPlanStr, ICON_UP) {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                final Object action = getValue(Action.NAME);

                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        NeptusLog.action().info(action);
                        try {
                            sendUploadPlanButton.setEnabled(false);
                            boolean verifyAllManeuversUsed = false;
                            if (allwaysVerifyAllManeuversUsed
                                    || (ev.getModifiers() & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK)
                                verifyAllManeuversUsed = true;
                            sendPlan(verifyAllManeuversUsed,
                                    getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(ev)));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
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
                        sendUploadPlanButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        };

        sendDownloadPlanAction = new AbstractAction(downloadActivePlanStr, ICON_DOWN_R) {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                final Object action = getValue(Action.NAME);
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        NeptusLog.action().info(action);
                        try {
                            sendDownloadPlanButton.setEnabled(false);
                            sendDownLoadPlan(getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(ev)));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
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
                        sendDownloadPlanButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        };

        sendStartAction = new AbstractAction(startPlanStr, ICON_START) {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                final Object action = getValue(Action.NAME);
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        NeptusLog.action().info(action);

                        sendStartButton.setEnabled(false);
                        boolean ignoreErrors = ((ev.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK);
                        if (ignoreErrors)
                            System.out.println("Control pressed, ignoring errors");
                        sendStartPlan(ignoreErrors, getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(ev)));

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
                        sendStartButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        };

        sendStopAction = new AbstractAction(stopPlanStr, ICON_STOP) {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                final Object action = getValue(Action.NAME);
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        NeptusLog.action().info(action);

                        sendStopButton.setEnabled(false);
                        sendStopPlan(false, getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(ev)));

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
                        sendStopButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        };

        teleOpAction = new AbstractAction(startTeleOperationStr, ICON_TELEOP_ON) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Object action = getValue(Action.NAME);
                NeptusLog.action().info(action);
                String[] systems = getSystemsToSendTo(SystemsSelectionAction.getClearSelectionOption(e));

                if (testAndShowWarningForNoSystemSelection(systems))
                    return;

                if (startTeleOperationStr.equalsIgnoreCase(e.getActionCommand())) {
                    Teleoperation teleop = new Teleoperation();
                    teleop.setCustom("src="+ImcMsgManager.getManager().getLocalId().intValue());

                    int reqId = IMCSendMessageUtils.getNextRequestId();
                    PlanControl pc = new PlanControl();
                    pc.setType(PlanControl.TYPE.REQUEST);
                    pc.setOp(PlanControl.OP.START);
                    pc.setRequestId(reqId);
                    pc.setPlanId("teleoperation-mode");
                    pc.setFlags(0);
                    pc.setArg(teleop);

                    boolean ret = IMCSendMessageUtils.sendMessage(pc,
                            (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP : null),
                            createDefaultMessageDeliveryListener(),
                            PlanControlPanel.this,
                            I18n.text("Error Initializing Tele-Operation"), DONT_USE_ACOUSTICS,
                            "", false, true, true, systems);
                    if (!ret) {
                        post(Notification.error(I18n.text("Tele-Operation"),
                                I18n.text("Error sending Tele-Operation message!")));
                    }
                    else {
                        registerPlanControlRequest(reqId);
                    }
                }
                else {
                    boolean ret = IMCSendMessageUtils.sendMessage(new TeleoperationDone(), 
                            (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP : null),
                            createDefaultMessageDeliveryListener(),
                            PlanControlPanel.this,
                            I18n.text("Error sending exiting Tele-Operation message!"), DONT_USE_ACOUSTICS,
                            "", false, true, true, systems);
                    if (!ret) {
                        post(Notification.error(I18n.text("Tele-Op"),
                                I18n.text("Error sending exiting Tele-Operation message!")));
                    }
                }
            }
        };
    }

    private MessageDeliveryListener createDefaultMessageDeliveryListener() {
        return (!useTcpToSendMessages || false ? null : new MessageDeliveryListener() {

            private String  getDest(IMCMessage message) {
                ImcSystem sys = message != null ? ImcSystemsHolder.lookupSystem(message.getDst()) : null;
                String dest = sys != null ? sys.getName() : I18n.text("unknown destination");
                return dest;
            }

            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery destination unreacheable",
                                message.getAbbrev(), getDest(message))));
            }

            @Override
            public void deliveryTimeOut(IMCMessage message) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery timeout",
                                message.getAbbrev(), getDest(message))));
            }

            @Override
            public void deliveryError(IMCMessage message, Object error) {
                post(Notification.error(
                        I18n.text("Delivering Message"),
                        I18n.textf("Message %messageType to %destination delivery error. (%error)",
                                message.getAbbrev(), getDest(message), error)));
            }

            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
            }

            @Override
            public void deliverySuccess(IMCMessage message) {
                //                post(Notification.success(
                //                        I18n.text("Delivering Message"),
                //                        I18n.textf("Message %messageType to %destination delivery success",
                //                                message.getAbbrev(), getDest(message))));
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.SubPanel#postLoadInit()
     */
    @Override
    public void initSubPanel() {
        // addMenuItems();
        setModeComponentsVisibility();
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
        planIdLabel.setFont(new Font("Arial", Font.BOLD, 9 * fontMultiplier));
        setModeComponentsVisibility();
        this.revalidate();
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange ev) {
        update();
        refreshUI();
    }

    /**
     * 
     */
    private void refreshUI() {
        boolean bEnable = true;
        if (isLocked())
            bEnable = false;
        if (bEnable != sendAcousticsButton.isEnabled()) {
            for (Component comp : holder.getComponents()) {
                comp.setEnabled(bEnable);
            }
        }

        // if ("INITIALIZING".equalsIgnoreCase(state) || "EXECUTING".equalsIgnoreCase(state)) {
        // sendStartButton.setActionCommand("Stop Plan");
        // sendStartButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_STOP);
        // sendStartButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION, "Stop Plan");
        // }
        // else {
        // sendStartButton.setActionCommand("Start Plan");
        // sendStartButton.getAction().putValue(AbstractAction.SMALL_ICON, ICON_START);
        // sendStartButton.getAction().putValue(AbstractAction.SHORT_DESCRIPTION, "Start Plan");
        // }
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

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        return 1000;
    }

    private short requestsCleanupFlag = 0;

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
        ImcSystem tmp = ImcSystemsHolder.lookupSystemByName(getMainVehicleId());
        if (tmp != null) {
            refreshUI();
        }

        requestsCleanupFlag++;
        if (requestsCleanupFlag > 20 * 5) {
            requestsCleanupFlag = 0;
            try {
                for (Integer key : registerRequestIdsTime.keySet().toArray(new Integer[0])) {
                    if (System.currentTimeMillis() - registerRequestIdsTime.get(key) > 10000)
                        registerRequestIdsTime.remove(key);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private String[] getSystemsToSendTo(boolean clearSelection) {
        if (sendToMainOrSelection())
            return new String[] { getMainVehicleId() };
        else {
            Vector<String> selectedSystems = getSelectedSystems(clearSelection);
            return selectedSystems.toArray(new String[selectedSystems.size()]);
        }
    }

    private boolean sendToMainOrSelection() {
        if (selectionAction.getSelectionType() == SelectionType.MAIN)
            return true;
        return false;
    }

    /**
     * @param clearSelection
     * 
     */
    private Vector<String> getSelectedSystems(boolean clearSelection) {
        Vector<SystemsList> sysLst = getConsole().getSubPanelsOfClass(SystemsList.class);
        Vector<String> selectedSystems = new Vector<String>();
        for (SystemsList systemsList : sysLst)
            selectedSystems.addAll(systemsList.getSelectedSystems(clearSelection));
        return selectedSystems;
    }

    private boolean sendAcoustics(boolean sendBlancTranspondersList, String... systems) {
        if (!checkConditionToRun(this, true, false))
            return false;
        if (testAndShowWarningForNoSystemSelection(systems))
            return false;

        MissionType miss = getConsole().getMission();

        ArrayList<TransponderElement> transpondersList = new ArrayList<TransponderElement>();

        if (!sendBlancTranspondersList) {
            LinkedHashMap<String, MapMission> mapList = miss.getMapsList();
            for (MapMission mpm : mapList.values()) {
                transpondersList.addAll(mpm.getMap().getTranspondersList().values());
            }
            // Let us order the beacons in alphabetic order (case insensitive)
            TransponderUtils.orderTransponders(transpondersList);

            TransponderElement[] selTransponders = getSelectedTransponderElementsFromExternalComponents();
            if (selTransponders.length > 0 && selTransponders.length < transpondersList.size()) {
                String beaconsToSend = "";
                boolean hideComma = true;
                for (TransponderElement tElnt : selTransponders) {
                    beaconsToSend += hideComma ? "" : ", ";
                    beaconsToSend += tElnt.getDisplayName();
                    hideComma = false;
                }
                int resp = GuiUtils.confirmDialog(SwingUtilities.windowForComponent(this), I18n.text("LBL Beacons"),
                        I18n.textf("Are you sure you want to send only %beaconsToSend?", beaconsToSend));

                if (resp == JOptionPane.YES_OPTION) {
                    transpondersList.clear();
                    transpondersList.addAll(Arrays.asList(selTransponders));
                }
                else {
                    if (resp == JOptionPane.NO_OPTION) {
                        return false;
                    }
                }
            }
        }

        // For new LBL Beacon Configuration
        Vector<LblBeacon> lblBeaconsList = new Vector<LblBeacon>();

        if (!sendBlancTranspondersList) {
            for (int i = 0; i < transpondersList.size(); i++) {
                TransponderElement transp = transpondersList.get(i);
                LblBeacon msgLBLBeaconSetup = TransponderUtils.getTransponderAsLblBeaconMessage(transp);
                if (msgLBLBeaconSetup == null) {
                    post(Notification.error(sendAcousticsButton.getName(),
                            I18n.textf("Bad configuration parsing for transponder %transponderid!", transp.getId())));
                    return false;
                }
                lblBeaconsList.add(msgLBLBeaconSetup);
            }
        }

        // if (lblBeaconsList.size() > 0) {
        // // Let us order the beacons in alphabetic order (case insensitive)
        // Collections.sort(lblBeaconsList, new Comparator<LblBeacon>() {
        // @Override
        // public int compare(LblBeacon o1, LblBeacon o2) {
        // return o1.getBeacon().compareTo(o2.getBeacon());
        // }
        // });
        // }
        LblConfig msgLBLConfiguration = new LblConfig();
        msgLBLConfiguration.setOp(LblConfig.OP.SET_CFG);
        msgLBLConfiguration.setBeacons(lblBeaconsList);

        IMCSendMessageUtils.sendMessage(msgLBLConfiguration,
                (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP : null), createDefaultMessageDeliveryListener(),
                this, I18n.text("Error sending acoustic beacons"), DONT_USE_ACOUSTICS, acousticOpServiceName,
                acousticOpUseOnlyActive, true, true, systems);
        // NeptusLog.pub().error("Sending beacons to vehicle: " + lblBeaconsList.toString());

        final String[] dest = systems;
        SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    Thread.sleep(1000);
                    LblConfig msgLBLConfiguration = new LblConfig();
                    msgLBLConfiguration.setOp(LblConfig.OP.GET_CFG);

                    // for (String sysName : dest) {
                    // ImcSystem sys = ImcSystemsHolder.getSystemWithName(sysName);
                    // if (sys != null)
                    // sys.removeData(ImcSystem.LBL_CONFIG_KEY);
                    // }

                    IMCSendMessageUtils.sendMessage(msgLBLConfiguration,
                            (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP : null),
                            createDefaultMessageDeliveryListener(), PlanControlPanel.this,
                            I18n.text("Error sending acoustic beacons"), DONT_USE_ACOUSTICS, acousticOpServiceName,
                            acousticOpUseOnlyActive, true, true, dest);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        sw.run();

        if (transpondersList.size() > 0) {
            String missionlog = GuiUtils.getLogFileName("mission_state", "zip");
            miss.asZipFile(missionlog, true);
        }

        return true;
    }

    /**
     * @param verifyAllManeuversUsed
     * 
     */
    private boolean sendPlan(boolean verifyAllManeuversUsed, String... systems) {
        if (!checkConditionToRun(this, true, true))
            return false;
        if (testAndShowWarningForNoSystemSelection(systems))
            return false;

        PlanType[] plans = getSelectedPlansFromExternalComponents();
        PlanType plan;
        int iSent = 0;

        for (int i = 0; i < plans.length; i++) {
            plan = plans[i];
            try {
                if (verifyAllManeuversUsed)
                    plan.validatePlan();
            }
            catch (Exception e) {
                // GuiUtils.errorMessage(getConsole(), e);
                post(Notification.error(I18n.text("Send Plan"), e.getMessage()));
                return false;
            }
            IMCMessage planSpecificationMessage = IMCUtils.generatePlanSpecification(plan);
            if (planSpecificationMessage == null) {
                // GuiUtils.errorMessage(this, I18n.text("Send Plan"),
                // I18n.text("Error sending plan message!\nNo plan spec. valid!"));
                post(Notification.error(I18n.text("Send Plan"),
                        I18n.text("Error sending plan message!\nNo plan spec. valid!")));
            }
            int reqId = IMCSendMessageUtils.getNextRequestId();
            PlanDB pdb = new PlanDB();
            pdb.setType(PlanDB.TYPE.REQUEST);
            pdb.setOp(OP.SET);
            pdb.setRequestId(reqId);
            pdb.setPlanId(plan.getId());
            pdb.setArg(planSpecificationMessage);
            
            pdb.setInfo("Plan sent by Neptus version " + ConfigFetch.getNeptusVersion());
            registerPlanControlRequest(reqId);
            boolean ret = IMCSendMessageUtils.sendMessage(pdb, (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP
                    : null), createDefaultMessageDeliveryListener(), this, I18n.text("Error sending plan"),
                    DONT_USE_ACOUSTICS, acousticOpServiceName, acousticOpUseOnlyActive, true, true, systems);
            if (ret) {
                iSent++;
            }
        }
        if (iSent > 0) {
            String missionlog = GuiUtils.getLogFileName("mission_state", "zip");
            getConsole().getMission().asZipFile(missionlog, true);
        }
        return true;
    }

    /**
     * @return
     * 
     */
    private boolean sendDownLoadPlan(String... systems) {
        if (!checkConditionToRun(this, true, false))
            return false;
        if (testAndShowWarningForNoSystemSelection(systems))
            return false;

        IMCMessage planControlMessage = IMCDefinition.getInstance().create("PlanControl");
        planControlMessage.setValue("type", 0); // REQUEST

        planControlMessage.setValue("op", "GET");

        int reqId = IMCSendMessageUtils.getNextRequestId();
        planControlMessage.setValue("request_id", reqId);

        // planControlMessage.setValue("plan_id", plan.getId());

        // boolean ret = sendTheMessage(planControlMessage, "Error sending plan download request", systems);
        boolean ret = IMCSendMessageUtils.sendMessage(planControlMessage,
                (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP : null), 
                createDefaultMessageDeliveryListener(), this,
                I18n.text("Error sending plan download request"), DONT_USE_ACOUSTICS,
                acousticOpServiceName, acousticOpUseOnlyActive, true, true, systems);

        if (ret) {
            registerPlanControlRequest(reqId);
            PlanControl pc = new PlanControl(planControlMessage);
            try {
                pc.copyFrom(planControlMessage);
                requests.put(reqId, pc);
            }
            catch (Exception e) {
                e.printStackTrace();               
            }
        }

        return ret;
    }

    private boolean sendStartPlan(boolean ignoreErrors, String... systems) {
        return sendStartStop(ignoreErrors, PlanControl.OP.START, systems);
    }

    private boolean sendStopPlan(boolean ignoreErrors, String... systems) {
        return sendStartStop(ignoreErrors, PlanControl.OP.STOP, systems);
    }

    private boolean sendStartStop(boolean ignoreErrors, PlanControl.OP cmd, String... systems) {
        if (!checkConditionToRun(this, cmd == PlanControl.OP.START ? false : true, cmd == PlanControl.OP.STOP ? false
                : true))
            return false;
        if (testAndShowWarningForNoSystemSelection(systems))
            return false;

        int reqId = IMCSendMessageUtils.getNextRequestId();
        PlanControl pc = new PlanControl();
        pc.setType(PlanControl.TYPE.REQUEST);
        pc.setRequestId(reqId);
        
        String cmdStrMsg = "";
        try {
            switch (cmd) {
                case START:
                    cmdStrMsg += I18n.text("Error sending start plan");
                    PlanType[] plans = getSelectedPlansFromExternalComponents();
                    PlanType plan = plans[0];

                    if (!verifyIfPlanIsInSyncOnTheSystem(plan, systems)) {
                        if (systems.length == 1)
                            post(Notification.error(I18n.text("Send Start Plan"),"Plan not in sync on system!"));
                        else
                            post(Notification.error(I18n.text("Send Start Plan"),"Plan not in sync on systems!"));

                        return false;
                    }

                    pc.setPlanId(plan.getId());
                    if (useCalibrationOnStartPlan)
                        pc.setFlags(PlanControl.FLG_CALIBRATE);
                    break;
                case STOP:
                    cmdStrMsg += I18n.text("Error sending stopping plan");
                    break;
                default:
                    return false;
            }
        }
        catch (Exception ex) {
            NeptusLog.pub().error(this, ex);
        }
        pc.setOp(cmd);

        if(ignoreErrors) {
            pc.setFlags(pc.getFlags() | PlanControl.FLG_IGNORE_ERRORS);
            post(Notification.warning(I18n.text("Send Plan"), "Ignoring any errors during execution of this plan"));
        }
        boolean dontSendByAcoustics = DONT_USE_ACOUSTICS;
        if (cmd == PlanControl.OP.START) {
            String planId = pc.getPlanId();
            if (planId.length() <= GeneralPreferences.maximumSizePlanNameForAcoustics) {
                dontSendByAcoustics = USE_ACOUSTICS;
            }
        }

        boolean ret = IMCSendMessageUtils.sendMessage(pc, (useTcpToSendMessages ? ImcMsgManager.TRANSPORT_TCP : null),
                createDefaultMessageDeliveryListener(), getConsole(), cmdStrMsg, dontSendByAcoustics,
                acousticOpServiceName, acousticOpUseOnlyActive, true, true, systems);

        if (!ret) {
            post(Notification.error(I18n.text("Send Plan"), I18n.text("Error sending PlanControl message!")));
            return false;
        }
        else {
            registerPlanControlRequest(reqId);
            requests.put(reqId, pc);
        }

        return true;
    }

    /**
     * @param plan
     * @param systems
     * @return
     */
    private boolean verifyIfPlanIsInSyncOnTheSystem(PlanType plan, String... systems) {
        boolean planInSync = true;
        String systemsNotInSync = "";
        for (String sysStr : systems) {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(sysStr);
            if (sys == null)
                continue;
            PlanDBState prs = sys.getPlanDBControl().getRemoteState();
            if (prs == null || !prs.matchesRemotePlan(plan)) {
                planInSync = false;
                systemsNotInSync += (systemsNotInSync.length() > 0 ? ", " : "") + sysStr;
            }
        }
        if (!planInSync) {//Synchronized 
            int resp = GuiUtils.confirmDialog(SwingUtilities.windowForComponent(this),
                    I18n.text("Plan not synchronized"),
                    I18n.textf("The plan '%plan' is not synchronized on %system.\nYou should resend the plan.\nDo you still want to start the plan?",
                            plan.getId(), systemsNotInSync));
            planInSync = (resp == JOptionPane.YES_OPTION);
        }

        return planInSync;
    }


    /**
     * @param reqId
     */
    private void registerPlanControlRequest(int reqId) {
        registerRequestIdsTime.put(reqId, System.currentTimeMillis());
    }

    /**
     * @param component
     * @param checkMission
     * @param checkPlan
     * @return
     */
    protected boolean checkConditionToRun(Component component, boolean checkMission, boolean checkPlan) {
        if (!ImcMsgManager.getManager().isRunning()) {
            post(Notification.error(component.getName(), I18n.text("IMC comms. are not running!")));
            return false;
        }

        ConsoleLayout cons = getConsole();
        if (cons == null) {
            post(Notification.error(component.getName(), I18n.text("Missing console attached!")));
            return false;
        }

        if (checkMission) {
            MissionType miss = cons.getMission();
            if (miss == null) {
                post(Notification.error(component.getName(), I18n.text("Missing attached mission!")));
                return false;
            }
        }

        if (checkPlan) {
            PlanType[] plans = getSelectedPlansFromExternalComponents();
            if (plans == null || plans.length == 0) {
                post(Notification.error(component.getName(), I18n.text("Missing attached plan!")));
                return false;
            }
        }
        return true;
    }

    private PlanType[] getSelectedPlansFromExternalComponents() {
        if (getConsole() == null)
            return new PlanType[0];
        Vector<IPlanSelection> psel = getConsole().getSubPanelsOfInterface(IPlanSelection.class);
        if (psel.size() == 0) {
            if (getConsole().getPlan() != null)
                return new PlanType[] { getConsole().getPlan() };
            else
                return new PlanType[0];
        }
        else {
            Vector<PlanType> vecPlans = psel.get(0).getSelectedPlans();
            return vecPlans.toArray(new PlanType[vecPlans.size()]);
        }
    }

    private TransponderElement[] getSelectedTransponderElementsFromExternalComponents() {
        if (getConsole() == null)
            return new TransponderElement[0];
        Vector<ITransponderSelection> psel = getConsole().getSubPanelsOfInterface(ITransponderSelection.class);
        Collection<TransponderElement> vecTrans = psel.get(0).getSelectedTransponders();
        return vecTrans.toArray(new TransponderElement[vecTrans.size()]);
    }

    /**
     * @param systems
     */
    private boolean testAndShowWarningForNoSystemSelection(String... systems) {
        if (systems.length < 1) {
            // getConsole().warning(this.getName() + ": " + I18n.text("No systems selected to send to!"));
            return true;
        }
        return false;
    }

    int lastTeleopState = 0;

    private String convertTimeSecondsToFormatedStringMillis(double timeSeconds) {
        String tt = "";
        if (timeSeconds < 60)
            tt = MathMiscUtils.parseToEngineeringNotation(timeSeconds, 3) + "s";
        else
            tt = DateTimeUtil.milliSecondsToFormatedString((long) (timeSeconds * 1000.0));
        return tt;
    }

    @Override
    public void messageArrived(IMCMessage message) {

        // message.dump(System.out);

        switch (message.getMgid()) {
            case PlanControl.ID_STATIC:
                PlanControl msg = (PlanControl) message;
                try {
                    PlanControl.TYPE type = msg.getType();
                    if (type != PlanControl.TYPE.IN_PROGRESS) {
                        int reqId = msg.getRequestId();
                        if (registerRequestIdsTime.containsKey(reqId)) {
                            boolean cleanReg = false;

                            if (type == TYPE.SUCCESS) {
                                PlanControl request = requests.get(reqId);
                                String text = I18n.textf("Request %d completed successfully.", reqId);
                                String src = ImcSystemsHolder.translateImcIdToSystemName(msg.getSrc());

                                if (request != null) {
                                    switch (request.getOp()) {
                                        case START:
                                            text = I18n.textf("Starting of %plan was acknowledged by %system.",
                                                    request.getPlanId(), src);
                                            break;
                                        case STOP:
                                            text = I18n.textf("Stopping of %plan was acknowledged by %system.",
                                                    request.getPlanId(), src);
                                            break;
                                        default:
                                            break;
                                    }
                                }

                                post(Notification.success("Plan Control", text));                                
                                cleanReg = true;
                            }
                            else if (type == TYPE.FAILURE) {
                                cleanReg = true;
                                long requestTimeMillis = registerRequestIdsTime.get(reqId);
                                String utcStr = " " + I18n.text("UTC");
                                double deltaTime = (msg.getTimestampMillis() - requestTimeMillis) / 1E3;
                                post(Notification.error(I18n.text("Plan Control Error"),
                                        I18n.textf("The following error arrived at @%timeArrived for a request @%timeRequested (\u2206t %deltaTime): %msg",
                                                DateTimeUtil.timeFormatterNoMillis2UTC.format(msg.getDate())
                                                + utcStr,
                                                DateTimeUtil.timeFormatterNoMillis2UTC.format(new Date(
                                                        requestTimeMillis)) + utcStr, deltaTime < 0 ? "-"
                                                                : convertTimeSecondsToFormatedStringMillis(deltaTime),
                                                                msg.getInfo())).src(
                                                                        ImcSystemsHolder.translateImcIdToSystemName(msg.getSrc())));
                            }
                            if (cleanReg)
                                registerRequestIdsTime.remove(reqId);
                        }
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
                break;
            case VehicleState.ID_STATIC:
                VehicleState vstate = (VehicleState) message;

                OP_MODE mode = vstate.getOpMode();
                int manType = vstate.getManeuverType();

                int teleopState = new String(mode.hashCode() + "," + manType).hashCode();

                if (teleopState != lastTeleopState) {
                    if (manType == teleoperationManeuver && mode == OP_MODE.MANEUVER) {
                        teleOpButton.setActionCommand(stopTeleOperationStr);
                        teleOpButton.setIcon(ICON_TELEOP_OFF);
                        teleOpButton.setToolTipText(I18n.text(stopTeleOperationStr));
                    }
                    else {
                        teleOpButton.setActionCommand(startTeleOperationStr);
                        teleOpButton.setIcon(ICON_TELEOP_ON);
                        teleOpButton.setToolTipText(startTeleOperationStr);
                    }
                }
                lastTeleopState = teleopState;
                break;
            case PlanDB.ID_STATIC:
                PlanDB planDb = (PlanDB) message;
                try {
                    PlanDB.TYPE type = planDb.getType();

                    if (type != PlanDB.TYPE.IN_PROGRESS) {
                        int reqId = planDb.getRequestId();
                        if (registerRequestIdsTime.containsKey(reqId)) {
                            // Date date = new Date(registerRequestIdsTime.get(reqId));
                            boolean cleanReg = false;
                            if (type == PlanDB.TYPE.SUCCESS) {
                                cleanReg = true;
                            }
                            else if (type == PlanDB.TYPE.FAILURE) {
                                cleanReg = true;
                                long requestTimeMillis = registerRequestIdsTime.get(reqId);
                                String utcStr = " " + I18n.text("UTC");
                                double deltaTime = (planDb.getTimestampMillis() - requestTimeMillis) / 1E3;
                                post(Notification.error(I18n.text("Plan DB Error"),
                                        I18n.textf("The following error arrived at @%timeArrived for a request @%timeRequested (\u2206t %deltaTime): %msg",
                                                DateTimeUtil.timeFormatterNoMillis2UTC.format(planDb.getDate())
                                                + utcStr,
                                                DateTimeUtil.timeFormatterNoMillis2UTC.format(new Date(
                                                        requestTimeMillis)) + utcStr, deltaTime < 0 ? "-"
                                                                : convertTimeSecondsToFormatedStringMillis(deltaTime),
                                                                planDb.getInfo())).src(
                                                                        ImcSystemsHolder.translateImcIdToSystemName(planDb.getSrc())));
                            }
                            if (cleanReg)
                                registerRequestIdsTime.remove(reqId);
                        }
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e, e);
                }
                break;
            case AcousticOperation.ID_STATIC:
                if (message.getDst() != getConsole().getImcMsgManager().getLocalId().intValue())
                    break;
                AcousticOperation aoMsg = (AcousticOperation) message;
                switch (aoMsg.getOp()) {
                    case MSG_DONE:
                        post(Notification.success("Acoustic Message Send", I18n.textf(
                                "Message to %systemName has been sent successfully.", aoMsg.getSystem().toString())));
                        break;
                    case MSG_FAILURE:
                        post(Notification.error("Acoustic Message Send",
                                I18n.textf("Failed to send message to %systemName.", aoMsg.getSystem().toString())));
                        break;
                    case MSG_IP:
                        post(Notification.info("Acoustic Message Send",
                                I18n.textf("Sending message to %systemName...", aoMsg.getSystem().toString())));
                        break;
                    case MSG_QUEUED:
                        post(Notification.warning("Acoustic Message Send", I18n.textf(
                                "Message to %systemName has been queued in %manta.", aoMsg.getSystem().toString(),
                                aoMsg.getSourceName())));
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
}
