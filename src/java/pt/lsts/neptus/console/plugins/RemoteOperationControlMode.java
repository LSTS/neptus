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
 * 2009/10/07
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.Teleoperation;
import pt.lsts.imc.TeleoperationDone;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.gui.StatusLed;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Paulo Dias", name = "Remote Operation Control Panel", icon = "images/control-mode/cmode.png", version = "1.3.0", description = "To enter and leave remote operation.", documentation = "plan-control/remoteoperationcontrolmode.html")
public class RemoteOperationControlMode extends ConsolePanel implements MainVehicleChangeListener, IPeriodicUpdates,
        LockableSubPanel, ConfigurationListener, NeptusMessageListener {

    public final ImageIcon CM_ICON = new ImageIcon(ImageUtils.getImage("images/control-mode/cmode.png"));

    private final ImageIcon CM_CLEAN = new ImageIcon(ImageUtils.getScaledImage("images/led_not.png", 32, 32));
    private final ImageIcon CM_UNKNOWN = new ImageIcon(ImageUtils.getScaledImage(
            "images/control-mode/unknown.png", 32, 32)); // 48,48
    private final ImageIcon CM_NONE = new ImageIcon(ImageUtils.getScaledImage("images/control-mode/wait.png", 32,
            32)); // 44x44
    private final ImageIcon CM_MANUAL = new ImageIcon(ImageUtils.getScaledImage(
            "images/control-mode/teleoperation.png", 32, 32));
    private final ImageIcon CM_AUTO = new ImageIcon(ImageUtils.getScaledImage(
            "images/control-mode/autonomous.png", 32, 32));
    private final ImageIcon CM_ALERT = new ImageIcon(ImageUtils.getScaledImage("images/control-mode/alert.png",
            32, 32));
    private final ImageIcon CM_CALIB = new ImageIcon(ImageUtils.getScaledImage(
            "images/control-mode/calibration.png", 32, 32));
    private final ImageIcon CM_EXTER = new ImageIcon(ImageUtils.getScaledImage(
            "images/control-mode/external.png", 32, 32));

    private final String CM_NOINFO_TEXT = I18n.text("No Information");
    private final String CM_UNKNOWN_TEXT = I18n.text("Unknown Mode");
    private final String CM_MANUAL_TEXT = I18n.text("Tele-operation Mode");
    private final String CM_ALERT_TEXT = I18n.text("Error Mode");
    private final String CM_SERVICE_TEXT = I18n.text("Service Mode");
    private final String CM_CALIB_TEXT = I18n.text("Calibration Mode");
    private final String CM_MANEUV_TEXT = I18n.text("Maneuver Mode");
    private final String CM_EXTER_TEXT = I18n.text("External Mode");

    private static final String ENTER_REMOTE_TEXT = I18n.text("set remote op.");
    private static final String ENTER_REMOTE_TOOL = I18n.text("request enter remote operation");
    private static final String LEAVE_REMOTE_TEXT = I18n.text("exit remote op.");
    private static final String LEAVE_REMOTE_TOOL = I18n.text("request exit remote operation");

    @NeptusProperty(name = "Use PlanControl for Teleoperation or old VehicleCommand")
    public boolean usePlanControlForTeleoperaton = true;
    {
    }

    @NeptusProperty(name = "Enable teleoperation button")
    public boolean enableTeleoperationButton = false;

    @NeptusProperty(name = "Enable console actions")
    public boolean enableConsoleActions = false;

    @NeptusProperty(name = "Font Size")
    public int modeLabelFontSize = 12;

    protected LinkedHashMap<Short, String> cmMessage = new LinkedHashMap<Short, String>();

    protected String controlModeVar = "SetControlMode.state";
    protected String controlModeVar1 = "VehicleState.control_mode";
    protected String opModeVar = "VehicleState.op_mode";
    protected String opModeVar1 = "VehicleState.maneuver_type";

    protected short controlModeValue = 0;

    private long lastMessageReceived = -1;

    private LinkedHashMap<Short, ImageIcon> oldControlModeIcons = new LinkedHashMap<Short, ImageIcon>();
    private LinkedHashMap<Short, Color> oldControlModeColors = new LinkedHashMap<Short, Color>();
    private LinkedHashMap<Short, String> oldControlModeMessages = new LinkedHashMap<Short, String>();
    private LinkedHashMap<Short, ImageIcon> opModeIcons = new LinkedHashMap<Short, ImageIcon>();
    private LinkedHashMap<Short, Color> opModeColors = new LinkedHashMap<Short, Color>();
    private LinkedHashMap<Short, String> opModeMessages = new LinkedHashMap<Short, String>();
    private boolean opModeOrControlMode = true;

    // UI
    protected StatusLed state = new StatusLed();
    // / Maintain length
    protected JButton setControlMode = new JButton(I18n.text("remote operation"));
    protected JXLabel modeLabel = new JXLabel("", SwingConstants.CENTER);

    protected ActionListener enterRemoteOperationAction, leaveRemoteOperationAction;

    /**
	 * 
	 */
    public RemoteOperationControlMode(ConsoleLayout console) {
        super(console);
        initialize();
    }

    private void initialize() {
        // Setup interface
        removeAll();
        this.state.made3LevelIndicatorBig();

        opModeIcons.put((short) -2, CM_CLEAN);
        opModeMessages.put((short) -2, CM_NOINFO_TEXT);
        opModeIcons.put((short) -1, CM_UNKNOWN);
        opModeMessages.put((short) -1, CM_UNKNOWN_TEXT);
        opModeIcons.put((short) 0, CM_NONE);
        opModeMessages.put((short) 0, CM_SERVICE_TEXT);
        opModeIcons.put((short) 1, CM_CALIB);
        opModeMessages.put((short) 1, CM_CALIB_TEXT);
        opModeIcons.put((short) 2, CM_ALERT);
        opModeMessages.put((short) 2, CM_ALERT_TEXT);
        opModeIcons.put((short) 3, CM_AUTO);
        opModeMessages.put((short) 3, CM_MANEUV_TEXT);
        opModeIcons.put((short) 4, CM_EXTER);
        opModeMessages.put((short) 4, CM_EXTER_TEXT);
        opModeIcons.put((short) 30, CM_MANUAL);
        opModeMessages.put((short) 30, CM_MANUAL_TEXT);
        for (Short s : opModeIcons.keySet())
            opModeColors.put(s, Color.BLUE);
        state.changeLevels(opModeIcons, opModeColors);

        cmMessage = opModeMessages;

        setControlMode.addActionListener(getRemoteOperationAction());
        setControlMode.setText(ENTER_REMOTE_TEXT);
        setControlMode.setToolTipText(ENTER_REMOTE_TOOL);
        setControlMode.setActionCommand(LEAVE_REMOTE_TEXT);
        setControlMode.setVisible(enableTeleoperationButton);

        JXPanel holder = new JXPanel();
        holder.setLayout(new MigLayout("hidemode 3", "", ""));
        holder.add(state, "w 100%, center, wrap");

        modeLabel.setFont(new Font("Arial", Font.BOLD, modeLabelFontSize));
        holder.add(modeLabel, "grow, center");
        state.setLevel(state.getLevel());
        this.setLayout(new BorderLayout());

        holder.add(setControlMode);

        add(holder);
        // setBounds(new Rectangle(0, 0, 182, 121));
        resetGUI();
        setDoubleBuffered(true);
    }

    private void resetGUI() {
        setControlMode.setText(ENTER_REMOTE_TEXT);
        setControlMode.setToolTipText(ENTER_REMOTE_TOOL);
        setControlMode.setActionCommand(LEAVE_REMOTE_TEXT);

        // state.setLevel((short) -2);
        // modeLabel.setText(cmMessage.get(-2));
        setControlModeValue((short) -2); // Reset the control mode value to the minimum (No Information)
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        modeLabel.setFont(new Font("Arial", Font.BOLD, modeLabelFontSize));
        setControlMode.setVisible(enableTeleoperationButton);
    }

    private void setOpModeOrControlMode(boolean opModeOrControlMode) {
        if (!(opModeOrControlMode ^ this.opModeOrControlMode))
            return;
        if (opModeOrControlMode) {
            this.opModeOrControlMode = true;
            state.changeLevels(opModeIcons, opModeColors);
            cmMessage = opModeMessages;
        }
        else {
            this.opModeOrControlMode = false;
            state.changeLevels(oldControlModeIcons, oldControlModeColors);
            cmMessage = oldControlModeMessages;
        }
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange ev) {
        resetGUI(); // initialize();
        lastMessageReceived = -1;
    }

    /**
     * @return the remoteOperationAction
     */
    public ActionListener getRemoteOperationAction() {
        if (enterRemoteOperationAction == null) {
            enterRemoteOperationAction = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (ENTER_REMOTE_TEXT.equals(e.getActionCommand())) {
                        PlanControl msg = PlanControl.create("type", "REQUEST", "op", "START", "request_id",
                                IMCSendMessageUtils.getNextRequestId(), "plan_id", "teleoperation-mode", "flags", 0,
                                "arg", new Teleoperation());
                        send(msg);
                    }
                    else {
                        send(new TeleoperationDone());
                    }
                }
            };
        }
        return enterRemoteOperationAction;
    }

    protected short getControlModeValue() {
        return controlModeValue;
    }

    protected void setControlModeValue(short controlModeValue) {
        this.controlModeValue = controlModeValue;
        this.state.setLevel(controlModeValue, cmMessage.get(controlModeValue));
        this.modeLabel.setText(cmMessage.get(controlModeValue));

        short compValue;
        if (opModeOrControlMode)
            compValue = 30;
        else
            compValue = 2;

        if (controlModeValue == compValue) {
            setControlMode.setText(LEAVE_REMOTE_TEXT);
            setControlMode.setToolTipText(LEAVE_REMOTE_TOOL);
            setControlMode.setActionCommand(LEAVE_REMOTE_TEXT);
        }
        else {
            setControlMode.setText(ENTER_REMOTE_TEXT);
            setControlMode.setToolTipText(ENTER_REMOTE_TOOL);
            setControlMode.setActionCommand(ENTER_REMOTE_TEXT);
        }
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "VehicleState" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        setOpModeOrControlMode(true);
        this.setControlModeValue((short) message.getInteger("op_mode"));
        lastMessageReceived = System.currentTimeMillis();
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

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
        if (lastMessageReceived != -1) {
            if (System.currentTimeMillis() - lastMessageReceived > 5000) {
                setControlModeValue((short) -1);
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
        setControlMode.setEnabled(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.LockableSubPanel#unLock()
     */
    @Override
    public void unLock() {
        setControlMode.setEnabled(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.LockableSubPanel#isLocked()
     */
    @Override
    public boolean isLocked() {
        return !setControlMode.isEnabled();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }

   
}
