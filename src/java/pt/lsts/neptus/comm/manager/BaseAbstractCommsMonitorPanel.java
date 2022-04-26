/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 2008/11/26
 */
package pt.lsts.neptus.comm.manager;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.l2fprod.common.swing.JOutlookBar;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.StatusLed;
import pt.lsts.neptus.gui.swing.JRoundButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.IMessage;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.conf.PreferencesListener;

/**
 * @author Paulo Dias
 * 
 */
public abstract class BaseAbstractCommsMonitorPanel<W extends CommBaseManager<M, Mi, C, I, CommManagerStatusChangeListener>, C extends SystemCommBaseInfo<M, Mi, I>, M extends IMessage, Mi extends MessageInfo, I>
        extends JPanel implements CommManagerStatusChangeListener {
    private static final long serialVersionUID = 4617120125187818738L;

    protected I selectedSystem = null;

    // public static ImageIcon ICON_ON = new ImageIcon(GuiUtils.getImage(
    // "images/imc.png").getScaledInstance(16, 16,
    // Image.SCALE_SMOOTH));
    // public static ImageIcon ICON_OFF = new ImageIcon(GuiUtils.getImage(
    // "images/buttons/noimc.png").getScaledInstance(16, 16,
    // Image.SCALE_SMOOTH));
    // private static ImageIcon DUMMY_ICON = new ImageIcon(GuiUtils.getImage(
    // "images/menus/render.png").getScaledInstance(16, 16,
    // Image.SCALE_SMOOTH)); // @jve:decl-index=0:

    private static ImageIcon CLEAR_ICON = new ImageIcon(ImageUtils.getImage("images/buttons/clear.png")
            .getScaledInstance(16, 16, Image.SCALE_SMOOTH));

    private W commManager = null;

    // Top Component
    private JOutlookBar topOutlookBar = null;

    // Top MonitorTab
    private JTabbedPane monitorTabbedPane = null;

    // MonitorTab Status
    private JPanel statusPanel = null;

    private JLabel statusLabel = null;
    private StatusLed statusLed = null;

    private JLabel statusNrSysLabel = null;
    private JTextField statusNrSysTextField = null;

    private JLabel queueSizeTextLabel = null;
    private JTextField queueSizeTextField = null;
    private StatusLed queueSizeStatusLed = null;
    private JRoundButton queueClearButton = null;

    private JLabel commonLastMsgArrivedLabel = null;
    private JTextField commonArrivalTimeText = null;
    private JTextField commonFreqArrivalTextField = null;
    private JTextField commonArrivalTxRxTimeNanosTextField = null;

    private JLabel commonLastMsgProcessLabel = null;
    private JTextField commonProcessTimeTextField = null;
    private JTextField commonFreqProcessTextField = null;
    private JTextField commonProcessTxRxTimeNanosTextField = null;

    private JRoundButton startCommButton = null;
    private JRoundButton stopCommButton = null;

    private JSeparator smallSeparator = null;

    private JLabel statusListenersLabel = null;
    private JTextField statusListenersSizeTextField = null;

    private JSeparator bigSeparator = null;

    private JScrollPane statusSysListScrollPane = null;
    private JList<String> statusSysList = null;
    private DefaultListModel<String> statusSysListModel = null;

    // SystemCommsPanel
    private JPanel systemCommsPanel = null;

    private JLabel sysTitleLabel = null;
    private JLabel sysIDLabel = null;
    private JLabel sysNameLabel = null;

    private JLabel sysActiveLabel = null;
    private StatusLed sysActiveLed = null;
    private JLabel activeListenerNameLabel = null;

    private JLabel sysLastMsgArrivedLabel = null;
    private JTextField sysArrivalTimeText = null;
    private JTextField sysFreqArrivalTextField = null;
    private JTextField sysArrivalTxRxTimeNanosTextField = null;

    private JLabel sysLastMsgProcessLabel = null;
    private JTextField sysProcessTimeTextField = null;
    private JTextField sysFreqProcessTextField = null;
    private JTextField sysProcessTxRxTimeNanosTextField = null;

    private JLabel sysQueueSizeLabel = null;
    private JTextField sysQueueSizeTextField = null;
    private StatusLed sysQueueSizeStatusLed = null;
    private JRoundButton sysQueueClearButton = null;
    private JLabel sysListenersSizeLabel = null;
    private JTextField sysListenersSizeTextField = null;

    // AddVehicleCommPanel()
    private JPanel addSystemCommPanel = null;
    // private JTextField addNewVehTextField = null;
    // private JRoundButton addNewVehRoundButton = null;

    // PROBATION
    // private JComboBox vehMsgComboBox = null;

    private Timer updateGuiTimer = null;
    private TimerTask updateGuiTask = null;
    private TimerTask updateGuiTask2 = null;

    protected int queueMaxSize = 1024;

    protected PreferencesListener preferencesListener = new PreferencesListener() {
        public void preferencesUpdated() {
//            try {
//                queueMaxSize = GeneralPreferences.getPropertyInteger(GeneralPreferences.COMMS_QUEUE_SIZE);
//            }
//            catch (GeneralPreferencesException e) {
//                e.printStackTrace();
//            }
            queueMaxSize = GeneralPreferences.commsQueueSize;
        }
    };

    public BaseAbstractCommsMonitorPanel(W commManager) {
        super();
        setCommManager(commManager);
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setLayout(new BorderLayout());
        this.setSize(new Dimension(396, 411));
        this.setPreferredSize(new Dimension(396, 411));
        this.add(getTopOutlookBar(), BorderLayout.CENTER);
        updateGuiTimer = new Timer(BaseAbstractCommsMonitorPanel.class.getSimpleName(), true);
        updateGuiTask = new TimerTask() {
            @Override
            public void run() {
                boolean running = getCommManager().isRunning();
                if (running)
                    statusLed.setLevel((short) 0, I18n.text("Running."));
                else
                    statusLed.setLevel((short) -1, I18n.text("Not running."));

                if (!running)
                    return;

                updateSystemList();
            }
        };

        updateGuiTask2 = new TimerTask() {
            @Override
            public void run() {
                int queueSize = getCommManager().getMsgQueueLength();
                getQueueSizeTextField().setText("" + queueSize);
                getStatusListenersSizeTextField().setText("" + getCommManager().getStatusListenersSize());
                if (queueSize < queueMaxSize)
                    getQueueSizeStatusLed().setLevel(StatusLed.LEVEL_0, I18n.text("Queue OK"));
                else
                    getQueueSizeStatusLed().setLevel(StatusLed.LEVEL_1, I18n.text("Queue full"));
                updateSystemCommData();
                
                updateSystemList();
            }
        };

        // updateGuiTimer.scheduleAtFixedRate(updateGuiTask, 500, 200);
        updateGuiTimer.schedule(updateGuiTask, 200);
        updateGuiTimer.scheduleAtFixedRate(updateGuiTask2, 500, 200);
        getCommManager().addStatusListener(this);

        preferencesListener.preferencesUpdated();
        GeneralPreferences.addPreferencesListener(preferencesListener);
    }

    /**
     * If overwrite call this also
     */
    public void cleanup() {
        if (updateGuiTask != null)
            updateGuiTask.cancel();
        if (updateGuiTask2 != null)
            updateGuiTask2.cancel();
        if (updateGuiTimer != null)
            updateGuiTimer.cancel();

        // getVehicleDummyComms().shutdown();
        getCommManager().removeStatusListener(this);

        GeneralPreferences.removePreferencesListener(preferencesListener);
    }

    /* ----------------- */

    /**
     * @return the commManager
     */
    public final W getCommManager() {
        return commManager;
    }

    /**
     * @param commManager the commManager to set
     */
    public final void setCommManager(W commManager) {
        this.commManager = commManager;
    }

    /* ----------------- */

    public abstract ImageIcon getOnIcon();

    public abstract ImageIcon getOffIcon();

    /* ----------------- */

    private void updateSystemList() {
        W sm = getCommManager();
        statusNrSysTextField.setText("" + sm.getNumberOfSystems());

        // statusVehListModel.removeAllElements();
        String[] exel = new String[statusSysListModel.size()];
        statusSysListModel.copyInto(exel);
        Vector<String> exelVRemove = new Vector<String>();
        Vector<String> exelVAdd = new Vector<String>();
        for (String st1 : exel) {
            exelVRemove.add(st1);
            exelVAdd.add(st1);
        }
        for (I str : sm.getCommInfo().keySet()) {
            if (exelVRemove.contains(convertStringIdAndNameToStringIdName(translateIdToStringId(str),
                    translateSystemIdToName(str)))) { // str.toString(); {
                exelVRemove.remove(convertStringIdAndNameToStringIdName(translateIdToStringId(str),
                        translateSystemIdToName(str))); // str.toString();
            }
        }
        for (I str : sm.getCommInfo().keySet()) {
            if (!exelVAdd.contains(convertStringIdAndNameToStringIdName(translateIdToStringId(str),
                    translateSystemIdToName(str)))) // str.toString();
                statusSysListModel.addElement(convertStringIdAndNameToStringIdName(translateIdToStringId(str),
                        translateSystemIdToName(str))); // str.toString();
        }
        for (String str : exelVRemove)
            statusSysListModel.removeElement(str);
        statusSysList.repaint();
    }

    private String convertStringIdNameToStringId(String val) {
        if (val == null)
            return val;
        
        return val.replaceFirst(" \\[[^\\]]*\\]$", "");
    }

    private String convertStringIdAndNameToStringIdName(String id, String name) {
        return id + " [" + name + "]";
    }

    /**
     * @param timeSeconds
     * @return
     */
    protected String convertTimeSecondsToFormatedString(double timeSeconds) {
        String tt = "";
        tt = DateTimeUtil.milliSecondsToFormatedString((long) (timeSeconds * 1000.0));
        return tt;
    }

    /**
     * @param timeSeconds
     * @return
     */
    private String convertTimeSecondsToFormatedStringNanos(double timeSeconds) {
        String tt = "";
        if (timeSeconds < 60)
            tt = MathMiscUtils.parseToEngineeringNotation(timeSeconds, 3) + "s";
        else
            tt = DateTimeUtil.milliSecondsToFormatedString((long) (timeSeconds * 1000.0));
        return tt;
    }

    /**
     * 
     */
    private void updateSystemCommData() {
        W sman = getCommManager();
        // / This should be a very short text label like this one. Tx=transmission Rx=reception
        String deltaStr = I18n.text("\u2206tTxRx") + " ";// IncrementSign //"\u0394;"//DeltaSign
        // / This should be a very short text label like this one. Rx=reception Hdl=handled
        String deltaStr2 = "\u2206tRxHdl" + " ";
        String unknownStr = "? s";
        double tmpMillis = sman.getProcessTimeMillisLastMsg();
        double time = (System.currentTimeMillis() - (long) tmpMillis) / 1000.0;
        if (tmpMillis < 0)
            getCommonProcessTimeTextField().setText(unknownStr);
        else
            getCommonProcessTimeTextField().setText(convertTimeSecondsToFormatedStringNanos(time));
        double tmpNanos = sman.getProcessDeltaTxRxTimeNanosLastMsg();
        String tmpConvNanos = unknownStr;
        if (tmpNanos >= 0)
            tmpConvNanos = convertTimeSecondsToFormatedStringNanos(tmpNanos / 1E9);
        getCommonProcessTxRxTimeNanosText().setText(deltaStr2 + tmpConvNanos);

        if (time < 2)
            getCommonFreqProcessTextField().setText(
                    (MathMiscUtils.parseToEngineeringNotation(sman.getProcessMessageFreq(), 0)) + " Hz");
        else
            getCommonFreqProcessTextField().setText("- Hz");

        tmpMillis = sman.getArrivalTimeMillisLastMsg();
        time = (System.currentTimeMillis() - (long) tmpMillis) / 1000.0;
        if (tmpMillis < 0)
            getCommonArrivalTimeText().setText(unknownStr);
        else
            getCommonArrivalTimeText().setText(convertTimeSecondsToFormatedStringNanos(time));
        tmpNanos = sman.getArrivalDeltaTxRxTimeNanosLastMsg();
        tmpConvNanos = unknownStr;
        if (tmpNanos >= 0)
            tmpConvNanos = convertTimeSecondsToFormatedStringNanos(tmpNanos / 1E9);
        getCommonArrivalTxRxTimeNanosText().setText(deltaStr + tmpConvNanos);

        if (time < 2)
            getCommonFreqArrivalTextField().setText(
                    (MathMiscUtils.parseToEngineeringNotation(sman.getArrivalMessageFreq(), 0)) + " Hz");
        else
            getCommonFreqArrivalTextField().setText("- Hz");

        try {
            updateVehicleCommDataPeriodicCall();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (selectedSystem == null /* || "".equalsIgnoreCase(selectedSystem) */)
            return;
        C vci = sman.getCommInfo().get(selectedSystem /* translateStringIdToId(selectedSystem) */);
        if (vci == null)
            return;

        if (!getSystemCommsPanel().isShowing())
            return;

        getSysActiveLed().setLevel((vci.isActive()) ? (short) 0 : (short) 1);
        int queueSize = vci.getMsgQueueLength();
        getSysQueueSizeTextField().setText("" + queueSize);
        if (queueSize < queueMaxSize)
            getSysQueueSizeStatusLed().setLevel(StatusLed.LEVEL_0, I18n.text("Queue OK"));
        else
            getSysQueueSizeStatusLed().setLevel(StatusLed.LEVEL_1, I18n.text("Queue full"));

        sysIDLabel.setText(translateIdToStringId(selectedSystem) /* selectedSystem.toString() */);
        try {
            // FIXME
            // vehNameLabel.setText(VehiclesHolder.getVehicleById((String)vci.getVehicleCommId()).getName());
            sysNameLabel.setText(translateSystemIdToName(vci.getSystemCommId()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        tmpMillis = vci.getProcessTimeMillisLastMsg();
        time = (System.currentTimeMillis() - (long) tmpMillis) / 1000.0;
        if (tmpMillis < 0)
            getSysProcessTimeTextField().setText(unknownStr);
        else
            getSysProcessTimeTextField().setText(convertTimeSecondsToFormatedStringNanos(time));
        tmpNanos = vci.getProcessDeltaTxRxTimeNanosLastMsg();
        tmpConvNanos = unknownStr;
        if (tmpNanos >= 0)
            tmpConvNanos = convertTimeSecondsToFormatedStringNanos(tmpNanos / 1E9);
        getSysProcessTxRxTimeNanosTextField().setText(deltaStr2 + tmpConvNanos);

        if (time < 2)
            getSysFreqProcessTextField().setText(
                    (MathMiscUtils.parseToEngineeringNotation(vci.getProcessMessageFreq(), 0)) + " Hz");
        else
            getSysFreqProcessTextField().setText("- Hz");

        tmpMillis = vci.getArrivalTimeMillisLastMsg();
        time = (System.currentTimeMillis() - (long) tmpMillis) / 1000.0;
        if (tmpMillis < 0)
            getSysArrivalTimeText().setText(unknownStr);
        else
            getSysArrivalTimeText().setText(convertTimeSecondsToFormatedStringNanos(time));
        tmpNanos = vci.getArrivalDeltaTxRxTimeNanosLastMsg();
        tmpConvNanos = unknownStr;
        if (tmpNanos >= 0)
            tmpConvNanos = convertTimeSecondsToFormatedStringNanos(tmpNanos / 1E9);
        getSysArrivalTxRxTimeNanosTextField().setText(deltaStr + tmpConvNanos);

        if (time < 2)
            getSysFreqArrivalTextField().setText(
                    (MathMiscUtils.parseToEngineeringNotation(vci.getArrivalMessageFreq(), 0)) + " Hz");
        else
            getSysFreqArrivalTextField().setText("- Hz");

        getSysListenersSizeTextField().setText("" + vci.getListenersSize());

//        changeSystemTree(((SystemImcMsgCommInfo)vci).getImcState());
//        systemTreeLabel.setText(I18n.text("System:") + " " + translateSystemIdToName(selectedSystem) + " ["
//                + translateIdToStringId(selectedSystem) + "]" /* selectedSystem.toString() */);

        MessageListener<? extends Mi, ? extends M> lastListener = vci.getLastListener();
        if (lastListener == null)
            activeListenerNameLabel.setText("");
        else {
            String name = lastListener.getClass().getSimpleName();
            activeListenerNameLabel.setText(name);
        }
    }

    /**
     * This is an empty body function that is call periodically and should be override if you want to update some GUI.
     * This is call on the GUI update.
     */
    protected void updateVehicleCommDataPeriodicCall() {
    }

    protected abstract String translateSystemIdToName(I id);

    protected abstract I translateStringIdToId(String id);

    protected abstract String translateIdToStringId(I id);

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.util.comm.manager.CommManagerStatusChangeListener#managerStatusChanged(int,
     * java.lang.String)
     */
    public final void managerStatusChanged(int status, String msg) {
        if (status == W.MANAGER_START)
            statusLed.setLevel((short) 0, I18n.text("Running"));
        else if (status == W.MANAGER_STOP)
            statusLed.setLevel((short) -1, I18n.text("Not running"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.util.comm.manager.CommManagerStatusChangeListener#managerVehicleAdded(pt.lsts.neptus
     * .types.vehicle.VehicleType)
     */
    public final void managerVehicleAdded(VehicleType vehicle) {
        updateSystemList();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.util.comm.manager.CommManagerStatusChangeListener#managerVehicleRemoved(pt.lsts.neptus
     * .types.vehicle.VehicleType)
     */
    public final void managerVehicleRemoved(VehicleType vehicle) {
        updateSystemList();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.util.comm.manager.CommManagerStatusChangeListener#managerVehicleStatusChanged(pt.lsts
     * .neptus.types.vehicle.VehicleType, int)
     */
    public final void managerVehicleStatusChanged(VehicleType vehicle, int status) {
        updateSystemList();
    }

    @Override
    public void managerSystemAdded(String systemId) {
        updateSystemList();
    }

    @Override
    public void managerSystemRemoved(String systemId) {
        updateSystemList();
    }

    @Override
    public void managerSystemStatusChanged(String systemId, int status) {
        updateSystemList();
    }

    /**
     * This method initializes jTabbedPane
     * 
     * @return javax.swing.JTabbedPane
     */
    private JTabbedPane getMonitorTabbedPane() {
        if (monitorTabbedPane == null) {
            monitorTabbedPane = new JTabbedPane();
            monitorTabbedPane.setEnabled(true);
            monitorTabbedPane.addTab(I18n.text("Status"), null, getStatusPanel(), null);
            // jTabbedPane.addTab("Send Msg.", null, getPanel2(), null);
            // / Rec.=reception
//            monitorTabbedPane.addTab(I18n.text("Common Tree Rec."), null, getCommonTreePanel(), null);
            // / Rec.=reception
//            monitorTabbedPane.addTab(I18n.text("System Tree Rec."), null, getSystemTreePanel(), null);
        }
        return monitorTabbedPane;
    }

    /**
     * @return
     */
    protected final JTabbedPane getMonitorTabHolder() {
        return getMonitorTabbedPane();
    }

    /**
     * @param title
     * @param icon
     * @param component
     * @param tip
     */
    protected final void addMonitorTab(String title, Icon icon, Component component, String tip) {
        getMonitorTabbedPane().addTab(title, icon, component, tip);
    }

    /**
     * This method initializes panel1
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getStatusPanel() {
        if (statusPanel == null) {
            statusListenersLabel = new JLabel();
            // statusListenersLabel.setLocation(new Point(104, 106));
            statusListenersLabel.setSize(new Dimension(94, 15));
            statusListenersLabel.setText(I18n.text("Status listeners:"));
            queueSizeTextLabel = new JLabel();
            queueSizeTextLabel.setText(I18n.text("Common queue:"));
            queueSizeTextLabel.setLocation(new Point(15, 75));
            queueSizeTextLabel.setSize(new Dimension(92, 16));
            statusLabel = new JLabel();
            statusLabel.setLocation(new Point(15, 15));
            statusLabel.setSize(new Dimension(91, 16));
            statusLabel.setText(I18n.text("Status (on/off):"));
            statusNrSysLabel = new JLabel();
            // / Nr.=number
            statusNrSysLabel.setText(I18n.text("Nr. systems:"));
            statusNrSysLabel.setLocation(new Point(15, 45));
            statusNrSysLabel.setSize(new Dimension(91, 16));
            statusPanel = new JPanel();
            statusPanel.setName("");
            commonLastMsgArrivedLabel = new JLabel();
            commonLastMsgArrivedLabel.setSize(new Dimension(108, 15));
            commonLastMsgArrivedLabel.setText(I18n.text("Last msg arrived:"));
            commonLastMsgProcessLabel = new JLabel();
            commonLastMsgProcessLabel.setText(I18n.text("Last msg processed:"));
            commonLastMsgProcessLabel.setSize(new Dimension(113, 16));

            GroupLayout layout = new GroupLayout(statusPanel);
            statusPanel.setLayout(layout);
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);

            layout.setHorizontalGroup(layout
                    .createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addGroup(
                            layout.createSequentialGroup()
                                    .addGroup(
                                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                    .addGroup(
                                                            layout.createSequentialGroup()
                                                                    .addComponent(statusLabel)
                                                                    .addComponent(getStatusLed(),
                                                                            getStatusLed().getWidth(),
                                                                            getStatusLed().getWidth(),
                                                                            getStatusLed().getWidth()))
                                                    .addGroup(
                                                            layout.createSequentialGroup()
                                                                    .addComponent(statusNrSysLabel)
                                                                    .addComponent(getStatusNrSysTextField(),
                                                                            getStatusNrSysTextField().getWidth(),
                                                                            getStatusNrSysTextField().getWidth(),
                                                                            Short.MAX_VALUE))
                                                    .addGroup(
                                                            layout.createSequentialGroup()
                                                                    .addComponent(queueSizeTextLabel)
                                                                    .addComponent(getQueueSizeTextField(),
                                                                            getQueueSizeTextField().getWidth(),
                                                                            getQueueSizeTextField().getWidth(),
                                                                            Short.MAX_VALUE)
                                                                    .addComponent(getQueueSizeStatusLed(),
                                                                            getQueueSizeStatusLed().getWidth(),
                                                                            getQueueSizeStatusLed().getWidth(),
                                                                            getQueueSizeStatusLed().getWidth())
                                                                    .addComponent(getQueueClearButton(),
                                                                            getQueueClearButton().getWidth(),
                                                                            getQueueClearButton().getWidth(),
                                                                            getQueueClearButton().getWidth()))
                                                    .addGroup(
                                                            layout.createSequentialGroup()
                                                                    .addComponent(getStartCommButton(),
                                                                            getStartCommButton().getWidth(),
                                                                            getStartCommButton().getWidth(),
                                                                            getStartCommButton().getWidth())
                                                                    .addComponent(getStopCommButton(),
                                                                            getStopCommButton().getWidth(),
                                                                            getStopCommButton().getWidth(),
                                                                            getStopCommButton().getWidth())
                                                                    .addComponent(getSmallSeparator(),
                                                                            getSmallSeparator().getWidth(),
                                                                            getSmallSeparator().getWidth(),
                                                                            getSmallSeparator().getWidth())
                                                                    .addGroup(
                                                                            layout.createParallelGroup(
                                                                                    GroupLayout.Alignment.LEADING)
                                                                                    .addComponent(statusListenersLabel)
                                                                                    .addComponent(
                                                                                            getStatusListenersSizeTextField()))))
                                    .addComponent(getBigSeparator(), getBigSeparator().getWidth(),
                                            getBigSeparator().getWidth(), getBigSeparator().getWidth())
                                    .addComponent(getStatusSysListScrollPane(),
                                            getStatusSysListScrollPane().getWidth(),
                                            getStatusSysListScrollPane().getWidth(), Short.MAX_VALUE))
                    .addGroup(
                            layout.createSequentialGroup().addComponent(commonLastMsgArrivedLabel)
                                    .addComponent(getCommonArrivalTimeText())
                                    .addComponent(getCommonFreqArrivalTextField())
                                    .addComponent(getCommonArrivalTxRxTimeNanosText()))
                    .addGroup(
                            layout.createSequentialGroup().addComponent(commonLastMsgProcessLabel)
                                    .addComponent(getCommonProcessTimeTextField())
                                    .addComponent(getCommonFreqProcessTextField())
                                    .addComponent(getCommonProcessTxRxTimeNanosText()))
                    .addComponent(getSystemCommsPanel(), getSystemCommsPanel().getWidth(),
                            getSystemCommsPanel().getWidth(), Short.MAX_VALUE).addComponent(getAddSystemCommPanel()));

            layout.setVerticalGroup(layout
                    .createSequentialGroup()
                    .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                    .addGroup(
                                            layout.createSequentialGroup()
                                                    .addGroup(
                                                            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                                    .addComponent(statusLabel)
                                                                    .addComponent(getStatusLed()))
                                                    .addGroup(
                                                            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                                    .addComponent(statusNrSysLabel)
                                                                    .addComponent(getStatusNrSysTextField(),
                                                                            getStatusNrSysTextField().getHeight(),
                                                                            getStatusNrSysTextField().getHeight(),
                                                                            getStatusNrSysTextField().getHeight()))
                                                    .addGroup(
                                                            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                                    .addComponent(queueSizeTextLabel)
                                                                    .addComponent(getQueueSizeTextField(),
                                                                            getQueueSizeTextField().getHeight(),
                                                                            getQueueSizeTextField().getHeight(),
                                                                            getQueueSizeTextField().getHeight())
                                                                    .addComponent(getQueueSizeStatusLed(),
                                                                            getQueueSizeStatusLed().getHeight(),
                                                                            getQueueSizeStatusLed().getHeight(),
                                                                            getQueueSizeStatusLed().getHeight())
                                                                    .addComponent(getQueueClearButton(),
                                                                            getQueueClearButton().getHeight(),
                                                                            getQueueClearButton().getHeight(),
                                                                            getQueueClearButton().getHeight()))
                                                    .addGroup(
                                                            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                                    .addComponent(getStartCommButton(),
                                                                            getStartCommButton().getHeight(),
                                                                            getStartCommButton().getHeight(),
                                                                            getStartCommButton().getHeight())
                                                                    .addComponent(getStopCommButton(),
                                                                            getStopCommButton().getHeight(),
                                                                            getStopCommButton().getHeight(),
                                                                            getStopCommButton().getHeight())
                                                                    .addComponent(getSmallSeparator())
                                                                    .addGroup(
                                                                            layout.createSequentialGroup()
                                                                                    .addComponent(statusListenersLabel)
                                                                                    .addComponent(
                                                                                            getStatusListenersSizeTextField(),
                                                                                            getStatusListenersSizeTextField()
                                                                                                    .getHeight(),
                                                                                            getStatusListenersSizeTextField()
                                                                                                    .getHeight(),
                                                                                            getStatusListenersSizeTextField()
                                                                                                    .getHeight()))))
                                    .addComponent(getBigSeparator(), GroupLayout.Alignment.CENTER)
                                    .addComponent(getStatusSysListScrollPane()))
                    .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(commonLastMsgArrivedLabel).addComponent(getCommonArrivalTimeText())
                                    .addComponent(getCommonFreqArrivalTextField())
                                    .addComponent(getCommonArrivalTxRxTimeNanosText()))
                    .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(commonLastMsgProcessLabel)
                                    .addComponent(getCommonProcessTimeTextField())
                                    .addComponent(getCommonFreqProcessTextField())
                                    .addComponent(getCommonProcessTxRxTimeNanosText()))
                    .addComponent(getSystemCommsPanel(), getSystemCommsPanel().getHeight(),
                            getSystemCommsPanel().getHeight(), Short.MAX_VALUE).addComponent(getAddSystemCommPanel()));

            layout.linkSize(SwingConstants.HORIZONTAL, statusLabel, statusNrSysLabel, queueSizeTextLabel);
            layout.linkSize(SwingConstants.HORIZONTAL, commonLastMsgArrivedLabel, commonLastMsgProcessLabel);

            layout.linkSize(SwingConstants.VERTICAL, getCommonArrivalTimeText(), getCommonProcessTimeTextField(),
                    getCommonFreqArrivalTextField(), getCommonFreqProcessTextField(),
                    getCommonArrivalTxRxTimeNanosText(), getCommonProcessTxRxTimeNanosText());

        }
        return statusPanel;
    }

    /**
     * This method initializes statusNrVehTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getStatusNrSysTextField() {
        if (statusNrSysTextField == null) {
            statusNrSysTextField = new JTextField();
            statusNrSysTextField.setBounds(new Rectangle(120, 45, 48, 20));
            statusNrSysTextField.setEditable(false);
        }
        return statusNrSysTextField;
    }

    /**
     * This method initializes statusLed
     * 
     * @return pt.lsts.neptus.gui.StatusLed
     */
    private StatusLed getStatusLed() {
        if (statusLed == null) {
            statusLed = new StatusLed();
            statusLed.setBounds(new Rectangle(120, 15, 14, 14));
            statusLed.setLevel((short) -1);
            statusLed.made2LevelIndicator();
        }
        return statusLed;
    }

    /**
     * @return the queueSizeStatusLed
     */
    private StatusLed getQueueSizeStatusLed() {
        if (queueSizeStatusLed == null) {
            queueSizeStatusLed = new StatusLed();
            queueSizeStatusLed.setSize(new Dimension(14, 14));
            queueSizeStatusLed.setLevel((short) -1);
            queueSizeStatusLed.made2LevelIndicator();
        }
        return queueSizeStatusLed;
    }

    /**
     * @return the vehQueueSizeStatusLed
     */
    private StatusLed getSysQueueSizeStatusLed() {
        if (sysQueueSizeStatusLed == null) {
            sysQueueSizeStatusLed = new StatusLed();
            sysQueueSizeStatusLed.setSize(new Dimension(14, 14));
            sysQueueSizeStatusLed.setLevel((short) -2);
            sysQueueSizeStatusLed.made2LevelIndicator();
        }
        return sysQueueSizeStatusLed;
    }

    /**
     * This method initializes statusVehList
     * 
     * @return javax.swing.JList
     */
    private JList<String> getStatusSysList() {
        if (statusSysList == null) {
            statusSysListModel = new DefaultListModel<String>();
            statusSysList = new JList<String>(statusSysListModel);
            statusSysList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            statusSysList.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting())
                        return;
                    String sid = (String) statusSysList.getSelectedValue();

                    try {
                        selectedSystem = translateStringIdToId(convertStringIdNameToStringId(sid)); // sid;
                    }
                    catch (NumberFormatException e1) {
                        selectedSystem = null; // "";
                    }
                }
            });
        }
        return statusSysList;
    }

    /**
     * This method initializes statusVehListScrollPane
     * 
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getStatusSysListScrollPane() {
        if (statusSysListScrollPane == null) {
            statusSysListScrollPane = new JScrollPane();
            statusSysListScrollPane.setBounds(new Rectangle(220, 14, 151, 129));
            statusSysListScrollPane.setViewportView(getStatusSysList());
        }
        return statusSysListScrollPane;
    }

    /**
     * This method initializes queuesizeTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getQueueSizeTextField() {
        if (queueSizeTextField == null) {
            queueSizeTextField = new JTextField();
            queueSizeTextField.setSize(new Dimension(48, 20));
            queueSizeTextField.setEditable(false);
            queueSizeTextField.setLocation(new Point(120, 75));
        }
        return queueSizeTextField;
    }

    /**
     * This method initializes vehicleCommsPanel
     * 
     * @return javax.swing.JPanel
     */
    protected JPanel getSystemCommsPanel() {
        if (systemCommsPanel == null) {
            activeListenerNameLabel = new JLabel();
            activeListenerNameLabel.setText("");
            activeListenerNameLabel.setLocation(new Point(99, 33));
            activeListenerNameLabel.setSize(new Dimension(245, 16));
            sysLastMsgArrivedLabel = new JLabel();
            sysLastMsgArrivedLabel.setLocation(new Point(14, 58));
            sysLastMsgArrivedLabel.setSize(new Dimension(108, 15));
            sysLastMsgArrivedLabel.setText(I18n.text("Last msg arrived:"));
            sysListenersSizeLabel = new JLabel();
            sysListenersSizeLabel.setLocation(new Point(189, 109));
            sysListenersSizeLabel.setSize(new Dimension(72, 15));
            sysListenersSizeLabel.setText(I18n.text("Listeners:"));
            sysTitleLabel = new JLabel();
            sysTitleLabel.setLocation(new Point(13, 11));
            sysTitleLabel.setSize(new Dimension(65, 16));
            sysTitleLabel.setText("<html><b>" + I18n.text("System:"));
            sysLastMsgProcessLabel = new JLabel();
            sysLastMsgProcessLabel.setText(I18n.text("Last msg processed:"));
            sysLastMsgProcessLabel.setLocation(new Point(14, 81));
            sysLastMsgProcessLabel.setSize(new Dimension(113, 16));
            sysNameLabel = new JLabel();
            sysNameLabel.setText("");
            sysNameLabel.setLocation(new Point(225, 11));
            sysNameLabel.setSize(new Dimension(119, 16));
            sysIDLabel = new JLabel();
            sysIDLabel.setText("");
            sysIDLabel.setLocation(new Point(97, 11));
            sysIDLabel.setSize(new Dimension(110, 16));
            sysActiveLabel = new JLabel();
            sysActiveLabel.setLocation(new Point(13, 31));
            sysActiveLabel.setSize(new Dimension(51, 16));
            sysActiveLabel.setText(I18n.text("Activity:"));
            sysQueueSizeLabel = new JLabel();
            sysQueueSizeLabel.setText(I18n.text("Queue size:"));
            sysQueueSizeLabel.setLocation(new Point(14, 105));
            sysQueueSizeLabel.setSize(new Dimension(83, 16));

            systemCommsPanel = new JPanel();
            systemCommsPanel.setBorder(BorderFactory.createLineBorder(SystemColor.windowBorder, 1));
            systemCommsPanel.setLocation(new Point(15, 148));
            systemCommsPanel.setSize(new Dimension(355, 131));

            GroupLayout layout = new GroupLayout(systemCommsPanel);
            systemCommsPanel.setLayout(layout);
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);

            layout.setHorizontalGroup(layout
                    .createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(
                            layout.createSequentialGroup()
                                    .addComponent(sysTitleLabel)
                                    .addComponent(sysIDLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
                                            Short.MAX_VALUE)
                                    .addComponent(sysNameLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE,
                                            Short.MAX_VALUE))
                    .addGroup(
                            layout.createSequentialGroup()
                                    .addComponent(sysActiveLabel)
                                    .addComponent(getSysActiveLed(), getSysActiveLed().getWidth(),
                                            getSysActiveLed().getWidth(), getSysActiveLed().getWidth())
                                    .addComponent(activeListenerNameLabel))
                    .addGroup(
                            layout.createSequentialGroup().addComponent(sysLastMsgArrivedLabel)
                                    .addComponent(getSysArrivalTimeText()).addComponent(getSysFreqArrivalTextField())
                                    .addComponent(getSysArrivalTxRxTimeNanosTextField()))
                    .addGroup(
                            layout.createSequentialGroup().addComponent(sysLastMsgProcessLabel)
                                    .addComponent(getSysProcessTimeTextField())
                                    .addComponent(getSysFreqProcessTextField())
                                    .addComponent(getSysProcessTxRxTimeNanosTextField()))
                    .addGroup(
                            layout.createSequentialGroup()
                                    .addComponent(sysQueueSizeLabel)
                                    .addComponent(getSysQueueSizeTextField())
                                    .addComponent(getSysQueueSizeStatusLed(), getSysQueueSizeStatusLed().getWidth(),
                                            getSysQueueSizeStatusLed().getWidth(),
                                            getSysQueueSizeStatusLed().getWidth())
                                    .addComponent(getSysQueueClearButton(), getSysQueueClearButton().getWidth(),
                                            getSysQueueClearButton().getWidth(), getSysQueueClearButton().getWidth())
                                    .addComponent(sysListenersSizeLabel).addComponent(getSysListenersSizeTextField())));

            layout.linkSize(SwingConstants.HORIZONTAL, sysTitleLabel, sysActiveLabel);
            layout.linkSize(SwingConstants.HORIZONTAL, sysIDLabel, sysNameLabel);
            layout.linkSize(SwingConstants.HORIZONTAL, sysLastMsgArrivedLabel, sysLastMsgProcessLabel);
            // layout.linkSize(SwingConstants.HORIZONTAL, getVehArrivalTimeText(), getVehProcessTimeTextField());
            // layout.linkSize(SwingConstants.HORIZONTAL, getFreqArrivalTextField(), getFreqProcessTextField(),
            // getVehListenersSizeTextField());
            layout.linkSize(SwingConstants.HORIZONTAL, sysQueueSizeLabel, sysListenersSizeLabel);

            layout.setVerticalGroup(layout
                    .createSequentialGroup()
                    .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(sysTitleLabel)
                                    .addComponent(sysIDLabel).addComponent(sysNameLabel))
                    .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(sysActiveLabel)
                                    .addComponent(getSysActiveLed(), getSysActiveLed().getHeight(),
                                            getSysActiveLed().getHeight(), getSysActiveLed().getHeight())
                                    .addComponent(activeListenerNameLabel))
                    .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(sysLastMsgArrivedLabel)
                                    .addComponent(getSysArrivalTimeText(), getSysArrivalTimeText().getHeight(),
                                            getSysArrivalTimeText().getHeight(), getSysArrivalTimeText().getHeight())
                                    .addComponent(getSysFreqArrivalTextField())
                                    .addComponent(getSysArrivalTxRxTimeNanosTextField(),
                                            getSysArrivalTxRxTimeNanosTextField().getHeight(),
                                            getSysArrivalTxRxTimeNanosTextField().getHeight(),
                                            getSysArrivalTimeText().getHeight()))
                    .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(sysLastMsgProcessLabel).addComponent(getSysProcessTimeTextField())
                                    .addComponent(getSysFreqProcessTextField())
                                    .addComponent(getSysProcessTxRxTimeNanosTextField()))
                    .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(sysQueueSizeLabel)
                                    .addComponent(getSysQueueSizeTextField())
                                    .addComponent(getSysQueueSizeStatusLed(), getSysQueueSizeStatusLed().getHeight(),
                                            getSysQueueSizeStatusLed().getHeight(),
                                            getSysQueueSizeStatusLed().getHeight())
                                    .addComponent(getSysQueueClearButton(), getSysQueueClearButton().getHeight(),
                                            getSysQueueClearButton().getHeight(), getSysQueueClearButton().getHeight())
                                    .addComponent(sysListenersSizeLabel).addComponent(getSysListenersSizeTextField())));

            layout.linkSize(SwingConstants.VERTICAL, getSysArrivalTimeText(), getSysProcessTimeTextField(),
                    getSysFreqArrivalTextField(), getSysFreqProcessTextField(), getSysQueueSizeTextField(),
                    getSysListenersSizeTextField(), getSysArrivalTxRxTimeNanosTextField(),
                    getSysProcessTxRxTimeNanosTextField());

        }
        return systemCommsPanel;
    }

    /**
     * This method initializes vehActiveLed
     * 
     * @return pt.lsts.neptus.gui.StatusLed
     */
    private StatusLed getSysActiveLed() {
        if (sysActiveLed == null) {
            sysActiveLed = new StatusLed();
            sysActiveLed.setBounds(new Rectangle(72, 33, 14, 14));
            sysActiveLed.made2LevelIndicator();
        }
        return sysActiveLed;
    }

    /**
     * This method initializes vehQueueSizeTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getSysQueueSizeTextField() {
        if (sysQueueSizeTextField == null) {
            sysQueueSizeTextField = new JTextField();
            sysQueueSizeTextField.setLocation(new Point(113, 105));
            sysQueueSizeTextField.setEditable(false);
            sysQueueSizeTextField.setSize(new Dimension(53, 20));
        }
        return sysQueueSizeTextField;
    }

    /**
     * This method initializes vehTimeTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getSysProcessTimeTextField() {
        if (sysProcessTimeTextField == null) {
            sysProcessTimeTextField = new JTextField();
            sysProcessTimeTextField.setBounds(new Rectangle(137, 81, 127, 20));
            sysProcessTimeTextField.setEditable(false);
        }
        return sysProcessTimeTextField;
    }

    /**
     * @return the commonProcessTxRxTimeNanosText
     */
    public JTextField getSysProcessTxRxTimeNanosTextField() {
        if (sysProcessTxRxTimeNanosTextField == null) {
            sysProcessTxRxTimeNanosTextField = new JTextField();
            sysProcessTxRxTimeNanosTextField.setBounds(new Rectangle(137, 81, 127, 20));
            sysProcessTxRxTimeNanosTextField.setEditable(false);
            sysProcessTxRxTimeNanosTextField.setToolTipText("<html>"
                    + I18n.text("The time difference between reception and handling time"));
        }
        return sysProcessTxRxTimeNanosTextField;
    }

    private JTextField getCommonProcessTimeTextField() {
        if (commonProcessTimeTextField == null) {
            commonProcessTimeTextField = new JTextField();
            commonProcessTimeTextField.setBounds(new Rectangle(137, 81, 127, 20));
            commonProcessTimeTextField.setEditable(false);
        }
        return commonProcessTimeTextField;
    }

    /**
     * @return the commonProcessTxRxTimeNanosText
     */
    public JTextField getCommonProcessTxRxTimeNanosText() {
        if (commonProcessTxRxTimeNanosTextField == null) {
            commonProcessTxRxTimeNanosTextField = new JTextField();
            commonProcessTxRxTimeNanosTextField.setBounds(new Rectangle(137, 81, 127, 20));
            commonProcessTxRxTimeNanosTextField.setEditable(false);
            commonProcessTxRxTimeNanosTextField.setToolTipText("<html>"
                    + I18n.text("The time difference between reception and handling time"));
        }
        return commonProcessTxRxTimeNanosTextField;
    }

    /**
     * This method initializes startStanagButton
     * 
     * @return pt.lsts.neptus.gui.swing.JRoundButton
     */
    private JRoundButton getStartCommButton() {
        if (startCommButton == null) {
            startCommButton = new JRoundButton();
            startCommButton.setToolTipText(I18n.textf("Start %commName comms.", getCommName()));
            startCommButton.setSize(new Dimension(34, 34));
            startCommButton.setLocation(new Point(16, 103));
            startCommButton.setIcon(getOnIcon());
            startCommButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    startCommButton.setEnabled(false);
                    NeptusLog.action().warn("Start comm. manager '" + getCommName() + "' request");
                    SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() throws Exception {
                            return getCommManager().start();
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                            startCommButton.setEnabled(true);
                        }
                    };
                    worker.execute();
                }
            });
        }
        return startCommButton;
    }

    /**
     * This method initializes stopStanagButton
     * 
     * @return pt.lsts.neptus.gui.swing.JRoundButton
     */
    private JRoundButton getStopCommButton() {
        if (stopCommButton == null) {
            stopCommButton = new JRoundButton();
            stopCommButton.setToolTipText(I18n.textf("Stop %commName comms.", getCommName()));
            stopCommButton.setSize(new Dimension(34, 34));
            stopCommButton.setLocation(new Point(56, 103));
            stopCommButton.setIcon(getOffIcon());
            stopCommButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    stopCommButton.setEnabled(false);
                    NeptusLog.action().warn("Stop comm. manager '" + getCommName() + "' request");
                    SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() throws Exception {
                            return getCommManager().stop();
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                            stopCommButton.setEnabled(true);
                        }
                    };
                    worker.execute();
                }
            });
        }
        return stopCommButton;
    }

    /**
     * @return the queueClearButton
     */
    private JRoundButton getQueueClearButton() {
        if (queueClearButton == null) {
            queueClearButton = new JRoundButton();
            queueClearButton.setToolTipText(I18n.text("Clear queue"));
            queueClearButton.setSize(new Dimension(20, 20));
            // queueClearButton.setLocation(new Point(56, 103));
            queueClearButton.setIcon(CLEAR_ICON);
            queueClearButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    queueClearButton.setEnabled(false);
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            getCommManager().clearMsgQueue();
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
                            queueClearButton.setEnabled(true);
                        }
                    };
                    worker.execute();
                }
            });
        }
        return queueClearButton;
    }

    /**
     * @return the vehQueueClearButton
     */
    private JRoundButton getSysQueueClearButton() {
        if (sysQueueClearButton == null) {
            sysQueueClearButton = new JRoundButton();
            sysQueueClearButton.setToolTipText(I18n.text("Clear queue"));
            sysQueueClearButton.setSize(new Dimension(20, 20));
            // queueClearButton.setLocation(new Point(56, 103));
            sysQueueClearButton.setIcon(CLEAR_ICON);
            sysQueueClearButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    sysQueueClearButton.setEnabled(false);
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            W sman = getCommManager();
                            C vci = sman.getCommInfo().get(selectedSystem /* translateStringIdToId(selectedSystem) */);
                            if (vci == null)
                                return null;
                            vci.clearMsgQueue();
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
                            sysQueueClearButton.setEnabled(true);
                        }
                    };
                    worker.execute();
                }
            });
        }
        return sysQueueClearButton;
    }

    /**
     * This method initializes jPanel1
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getAddSystemCommPanel() {
        if (addSystemCommPanel == null) {
            addSystemCommPanel = new JPanel();
            addSystemCommPanel.setLayout(new FlowLayout());
            addSystemCommPanel.setBorder(BorderFactory.createLineBorder(SystemColor.windowBorder, 1));
            addSystemCommPanel.setSize(new Dimension(355, 42));
            addSystemCommPanel.setLocation(new Point(15, 284));
            // addVehicleCommPanel.add(getAddNewVehTextField(), null);
            // addVehicleCommPanel.add(getAddNewVehRoundButton(), null);
        }
        return addSystemCommPanel;
    }

    protected final void addNewActivateCommPanel(JPanel newComponent) {
        GroupLayout grpL = (GroupLayout) getStatusPanel().getLayout();
        grpL.replace(getAddSystemCommPanel(), newComponent);
        addSystemCommPanel = newComponent;
        newComponent.setBorder(BorderFactory.createLineBorder(SystemColor.windowBorder, 1));
    }

    /**
     * This method initializes bigSeparator
     * 
     * @return javax.swing.JSeparator
     */
    private JSeparator getBigSeparator() {
        if (bigSeparator == null) {
            bigSeparator = new JSeparator();
            bigSeparator.setBounds(new Rectangle(203, 11, 9, 131));
            bigSeparator.setBackground(SystemColor.windowBorder);
            bigSeparator.setOrientation(SwingConstants.VERTICAL);
        }
        return bigSeparator;
    }

    /**
     * This method initializes smallSeparator
     * 
     * @return javax.swing.JSeparator
     */
    private JSeparator getSmallSeparator() {
        if (smallSeparator == null) {
            smallSeparator = new JSeparator();
            smallSeparator.setBounds(new Rectangle(96, 98, 10, 43));
            smallSeparator.setOrientation(SwingConstants.VERTICAL);
        }
        return smallSeparator;
    }

    /**
     * This method initializes jOutlookBar
     * 
     * @return com.l2fprod.common.swing.JOutlookBar
     */
    private JOutlookBar getTopOutlookBar() {
        if (topOutlookBar == null) {
            topOutlookBar = new JOutlookBar();
            topOutlookBar.setPreferredSize(new Dimension(396, 411));
            topOutlookBar.addTab(I18n.textf("%commName Monitor", getCommName()), getOnIcon(), getMonitorTabbedPane(),
                    null);
            // jOutlookBar.addTab("Dummy Vehicles", DUMMY_ICON, getVehicleDummyComms(), null);
        }
        return topOutlookBar;
    }

    /**
     * @return
     */
    protected final JOutlookBar getTopTabHolder() {
        return getTopOutlookBar();
    }

    protected abstract String getCommName();

    /**
     * @param title
     * @param icon
     * @param component
     * @param tip
     */
    protected final void addTopTab(String title, Icon icon, Component component, String tip) {
        getTopOutlookBar().addTab(title, icon, component, tip);
    }

    /**
     * This method initializes freqTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getSysFreqProcessTextField() {
        if (sysFreqProcessTextField == null) {
            sysFreqProcessTextField = new JTextField();
            sysFreqProcessTextField.setEditable(false);
            sysFreqProcessTextField.setLocation(new Point(269, 81));
            sysFreqProcessTextField.setSize(new Dimension(77, 19));
        }
        return sysFreqProcessTextField;
    }

    private JTextField getCommonFreqProcessTextField() {
        if (commonFreqProcessTextField == null) {
            commonFreqProcessTextField = new JTextField();
            commonFreqProcessTextField.setEditable(false);
            commonFreqProcessTextField.setLocation(new Point(269, 81));
            commonFreqProcessTextField.setSize(new Dimension(77, 19));
        }
        return commonFreqProcessTextField;
    }

    /**
     * This method initializes vehListenersSizeTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getSysListenersSizeTextField() {
        if (sysListenersSizeTextField == null) {
            sysListenersSizeTextField = new JTextField();
            sysListenersSizeTextField.setEditable(false);
            sysListenersSizeTextField.setLocation(new Point(269, 105));
            sysListenersSizeTextField.setSize(new Dimension(75, 19));
        }
        return sysListenersSizeTextField;
    }

    /**
     * This method initializes statusListenersSizeTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getStatusListenersSizeTextField() {
        if (statusListenersSizeTextField == null) {
            statusListenersSizeTextField = new JTextField();
            statusListenersSizeTextField.setBounds(new Rectangle(124, 123, 55, 19));
            statusListenersSizeTextField.setEditable(false);
        }
        return statusListenersSizeTextField;
    }

    // .. ..................... ..//

    /**
     * This method initializes vehArrivalTimeText
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getSysArrivalTimeText() {
        if (sysArrivalTimeText == null) {
            sysArrivalTimeText = new JTextField();
            sysArrivalTimeText.setEditable(false);
            sysArrivalTimeText.setSize(new Dimension(127, 20));
            sysArrivalTimeText.setLocation(new Point(137, 59));
        }
        return sysArrivalTimeText;
    }

    /**
     * @return the commonArrivalTxRxTimeNanosText
     */
    public JTextField getSysArrivalTxRxTimeNanosTextField() {
        if (sysArrivalTxRxTimeNanosTextField == null) {
            sysArrivalTxRxTimeNanosTextField = new JTextField();
            sysArrivalTxRxTimeNanosTextField.setEditable(false);
            sysArrivalTxRxTimeNanosTextField.setToolTipText(I18n
                    .text("<html>The time difference between sent and reception time"
                            + "<br>(clock difference may invalidate it)"));
            sysArrivalTxRxTimeNanosTextField.setSize(new Dimension(127, 20));
            sysArrivalTxRxTimeNanosTextField.setLocation(new Point(137, 59));
        }
        return sysArrivalTxRxTimeNanosTextField;
    }

    private JTextField getCommonArrivalTimeText() {
        if (commonArrivalTimeText == null) {
            commonArrivalTimeText = new JTextField();
            commonArrivalTimeText.setEditable(false);
            commonArrivalTimeText.setSize(new Dimension(127, 20));
            commonArrivalTimeText.setLocation(new Point(137, 59));
        }
        return commonArrivalTimeText;
    }

    /**
     * @return the commonArrivalTxRxTimeNanosText
     */
    public JTextField getCommonArrivalTxRxTimeNanosText() {
        if (commonArrivalTxRxTimeNanosTextField == null) {
            commonArrivalTxRxTimeNanosTextField = new JTextField();
            commonArrivalTxRxTimeNanosTextField.setEditable(false);
            commonArrivalTxRxTimeNanosTextField.setToolTipText(I18n
                    .text("<html>The time difference between sent and reception time"
                            + "<br>(clock difference may invalidate it)"));
            commonArrivalTxRxTimeNanosTextField.setSize(new Dimension(127, 20));
            commonArrivalTxRxTimeNanosTextField.setLocation(new Point(137, 59));
        }
        return commonArrivalTxRxTimeNanosTextField;
    }

    /**
     * This method initializes freqArrivalTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getSysFreqArrivalTextField() {
        if (sysFreqArrivalTextField == null) {
            sysFreqArrivalTextField = new JTextField();
            sysFreqArrivalTextField.setEditable(false);
            sysFreqArrivalTextField.setLocation(new Point(269, 59));
            sysFreqArrivalTextField.setSize(new Dimension(77, 19));
        }
        return sysFreqArrivalTextField;
    }

    private JTextField getCommonFreqArrivalTextField() {
        if (commonFreqArrivalTextField == null) {
            commonFreqArrivalTextField = new JTextField();
            commonFreqArrivalTextField.setEditable(false);
            commonFreqArrivalTextField.setLocation(new Point(269, 59));
            commonFreqArrivalTextField.setSize(new Dimension(77, 19));
        }
        return commonFreqArrivalTextField;
    }
}