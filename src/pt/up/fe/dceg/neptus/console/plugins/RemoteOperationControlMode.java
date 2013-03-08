/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2009/10/07
 * $Id:: RemoteOperationControlMode.java 9616 2012-12-30 23:23:22Z pdias  $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.StatusLed;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.PlanControl;
import pt.up.fe.dceg.neptus.imc.Teleoperation;
import pt.up.fe.dceg.neptus.imc.TeleoperationDone;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.IMCSendMessageUtils;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Paulo Dias", name = "Remote Operation Control Panel", icon = "images/control-mode/cmode.png", version = "1.3.0", description = "To enter and leave remote operation.", documentation = "plan-control/remoteoperationcontrolmode.html")
public class RemoteOperationControlMode extends SimpleSubPanel implements MainVehicleChangeListener, IPeriodicUpdates,
        LockableSubPanel, ConfigurationListener, NeptusMessageListener {

    public static final ImageIcon CM_ICON = new ImageIcon(ImageUtils.getImage("images/control-mode/cmode.png"));

    public static final ImageIcon CM_CLEAN = new ImageIcon(ImageUtils.getScaledImage("images/led_not.png", 32, 32));
    public static final ImageIcon CM_UNKNOWN = new ImageIcon(ImageUtils.getScaledImage(
            "images/control-mode/unknown.png", 32, 32)); // 48,48
    public static final ImageIcon CM_NONE = new ImageIcon(ImageUtils.getScaledImage("images/control-mode/wait.png", 32,
            32)); // 44x44
    public static final ImageIcon CM_DIAG = new ImageIcon(ImageUtils.getScaledImage(
            "images/control-mode/diagnostic.png", 32, 32));
    public static final ImageIcon CM_MANUAL = new ImageIcon(ImageUtils.getScaledImage(
            "images/control-mode/teleoperation.png", 32, 32));
    public static final ImageIcon CM_AUTO = new ImageIcon(ImageUtils.getScaledImage(
            "images/control-mode/autonomous.png", 32, 32));
    public static final ImageIcon CM_ALERT = new ImageIcon(ImageUtils.getScaledImage("images/control-mode/alert.png",
            32, 32));
    public static final ImageIcon CM_CALIB = new ImageIcon(ImageUtils.getScaledImage(
            "images/control-mode/calibration.png", 32, 32));
    public static final ImageIcon CM_EXTER = new ImageIcon(ImageUtils.getScaledImage(
            "images/control-mode/external.png", 32, 32));

    public static final String CM_NOINFO_TEXT = I18n.text("No Information");
    public static final String CM_UNKNOWN_TEXT = I18n.text("Unknown Mode");
    public static final String CM_NONE_TEXT = I18n.text("Idle Mode");
    public static final String CM_DIAG_TEXT = I18n.text("Diagnostic Mode");
    public static final String CM_MANUAL_TEXT = I18n.text("Tele-operation Mode");
    public static final String CM_AUTO_TEXT = I18n.text("Autonomous Mode");
    public static final String CM_ALERT_TEXT = I18n.text("Error Mode");
    public static final String CM_SERVICE_TEXT = I18n.text("Service Mode");
    public static final String CM_CALIB_TEXT = I18n.text("Calibration Mode");
    public static final String CM_MANEUV_TEXT = I18n.text("Maneuver Mode");
    public static final String CM_EXTER_TEXT = I18n.text("External Mode");

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
    // / Maintain lenght
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
     * @see pt.up.fe.dceg.neptus.plugins.ConfigurationListener#propertiesChanged()
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

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#mainVehicleChange(java.lang.String)
     */
    @Override
    public void mainVehicleChangeNotification(String id) {
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
                        PlanControl msg = new PlanControl("type", "REQUEST", "op", "START", "request_id",
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
     * @see pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */

    @Override
    public long millisBetweenUpdates() {
        return 1000;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates#update()
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
     * @see pt.up.fe.dceg.neptus.consolebase.LockableSubPanel#lock()
     */
    @Override
    public void lock() {
        setControlMode.setEnabled(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.consolebase.LockableSubPanel#unLock()
     */
    @Override
    public void unLock() {
        setControlMode.setEnabled(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.consolebase.LockableSubPanel#isLocked()
     */
    @Override
    public boolean isLocked() {
        return !setControlMode.isEnabled();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }

   
}
