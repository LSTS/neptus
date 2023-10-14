/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 2009/05/01
 */
package pt.lsts.neptus.gui;

import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultFormatter;

import pt.lsts.neptus.gui.ClockCounter.ClockState;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.speech.SpeechUtil;

/**
 * @author pdias
 * 
 */

public class ChronometerPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    public static Color COLOR_OK = ClockCounter.COLOR_BACK;
    public static Color COLOR_NOK = new Color(255, 50, 0);

    private final String ACTION_START_STOP = "StartStop";
    private final String ACTION_PAUSE_RESUME = "PauseResume";
    private final String ACTION_COUNTDOWN = "Countdown";
    private final String ACTION_RESET = "Reset";
    private final String ACTION_ALARM = "Alarm";

    private final ImageIcon ICON_START = ImageUtils.getScaledIcon("images/chronometer/play.png", 16, 16);
    private final ImageIcon ICON_STOP = ImageUtils.getScaledIcon("images/chronometer/stop.png", 16, 16);
    private final ImageIcon ICON_PAUSE = ImageUtils.getScaledIcon("images/chronometer/pause.png", 16, 16);
    private final ImageIcon ICON_RESUME = ImageUtils.getScaledIcon("images/chronometer/fwd.png", 16, 16);
    private final ImageIcon ICON_DOWN = ImageUtils.getScaledIcon("images/chronometer/down.png", 16, 16);
    private final ImageIcon ICON_UP = ImageUtils.getScaledIcon("images/chronometer/up.png", 16, 16);
    private final ImageIcon ICON_RESET = ImageUtils.getScaledIcon("images/chronometer/restart.png", 16, 16);
    private final ImageIcon ICON_ALARM_ON = ImageUtils.getScaledIcon("images/chronometer/alarm.png", 16, 16);
    private final ImageIcon ICON_ALARM_OFF = ImageUtils.getScaledIcon("images/chronometer/alarm_disabled.png",
            16, 16);

    public static enum CronState {
        STOPED,
        STARTED,
        PAUSED
    };

    public static enum CronEvent {
        NONE,
        STOP,
        START,
        PAUSE,
        RESUME
    };

    private long msStart = 0, msEnd = -1, msStop = 0, msAcum = 0;
    private long msTime = 0;

    private long maxSecs = -1;

    protected CronState cState = CronState.STOPED;

//    private Timer timer = new Timer(this.getClass().getSimpleName() + ": " + this.hashCode(), true);
//    private TimerTask tTask = null;
    private Thread guiUpdateThread = null;

    // Visual components
    private ClockCounter display = null;
    private MiniButton startStopToggleButton = null;
    private MiniButton pauseResumeToggleButton = null;
    private MiniButton alarmValueButton = null;
    private MiniButton countdownToggleButton = null;
    private MiniButton resetButton = null;
    private JLabel labelPanel = null;
    
    private boolean audioAlertOnZero = false;
    private boolean alreadyReported = false;

    public ChronometerPanel() {
        this.removeAll();
        initialize();
    }

    private void initialize() {
        setBackground(COLOR_OK);
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setAutoCreateGaps(false);
        layout.setAutoCreateContainerGaps(false);

        layout.setHorizontalGroup(layout
                .createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup().addComponent(getDisplay()))
                .addGroup(
                        layout.createSequentialGroup().addComponent(getStartStopToggleButton())
                                .addComponent(getPauseResumeToggleButton()).addComponent(getAlarmValueButton())
                                .addComponent(getCountdownToggleButton()).addGap(10)
                                .addComponent(getLabelPanel())));

        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER).addGroup(
                layout.createSequentialGroup()
                        .addComponent(getDisplay())
                        .addGroup(
                                layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(getStartStopToggleButton())
                                        .addComponent(getPauseResumeToggleButton()).addComponent(getAlarmValueButton())
                                        .addComponent(getCountdownToggleButton())
                                        .addComponent(getLabelPanel()))));

        layout.linkSize(SwingConstants.HORIZONTAL, getStartStopToggleButton(), getPauseResumeToggleButton(),
                getAlarmValueButton(), getCountdownToggleButton()/* , getResetButton() */);
        layout.linkSize(SwingConstants.VERTICAL, getStartStopToggleButton(), getPauseResumeToggleButton(),
                getAlarmValueButton(), getCountdownToggleButton()/* , getResetButton() */);
    }

    public void hideButtons() {
        getStartStopToggleButton().setVisible(false);
        getPauseResumeToggleButton().setVisible(false);
        getAlarmValueButton().setVisible(false);
        getCountdownToggleButton().setVisible(false);
        getResetButton().setVisible(false);
    }

    public void unHideButtons() {
        getStartStopToggleButton().setVisible(true);
        getPauseResumeToggleButton().setVisible(true);
        getAlarmValueButton().setVisible(true);
        getCountdownToggleButton().setVisible(true);
        // getResetButton().setVisible(true);
    }

    protected ClockCounter getDisplay() {
        if (display == null) {
            display = new ClockCounter();
            final JPopupMenu popup = new JPopupMenu();
            popup.add(new JMenuItem(new AbstractAction(I18n.text("Copy time")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    ClipboardOwner owner = new ClipboardOwner() {
                        public void lostOwnership(Clipboard clipboard, Transferable contents) {
                        }
                    };
                    Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new StringSelection(ChronometerPanel.this.getFormattedTime()), owner);
                }
            }));
            popup.add(new JMenuItem(new AbstractAction(I18n.text("Set label")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    String newLabel = JOptionPane.showInputDialog(
                            SwingUtilities.windowForComponent(ChronometerPanel.this), I18n.text("Label text"),
                            getLabelPanel().getText());
                    getLabelPanel().setText(newLabel);
                }
            }));
            display.add(popup);
            display.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    popup.show(display, e.getX(), e.getY());
                    super.mouseClicked(e);
                }
            });
        }
        return display;
    }

    protected MiniButton getStartStopToggleButton() {
        if (startStopToggleButton == null) {
            startStopToggleButton = new MiniButton();
            startStopToggleButton.setToggle(true);
            startStopToggleButton.setIcon(ICON_START);
            startStopToggleButton.setActionCommand(ACTION_START_STOP);
            startStopToggleButton.addActionListener(this);
            startStopToggleButton.setToolTipText(I18n.text("Start/Stop"));
        }
        return startStopToggleButton;
    }

    protected MiniButton getPauseResumeToggleButton() {
        if (pauseResumeToggleButton == null) {
            pauseResumeToggleButton = new MiniButton();
            pauseResumeToggleButton.setToggle(true);
            pauseResumeToggleButton.setIcon(ICON_PAUSE);
            pauseResumeToggleButton.setActionCommand(ACTION_PAUSE_RESUME);
            pauseResumeToggleButton.addActionListener(this);
            pauseResumeToggleButton.setToolTipText(I18n.text("Pause/Resume"));
            // pauseResumeToggleButton.setEnabled(false);
        }
        return pauseResumeToggleButton;
    }

    protected MiniButton getCountdownToggleButton() {
        if (countdownToggleButton == null) {
            countdownToggleButton = new MiniButton();
            countdownToggleButton.setToggle(true);
            countdownToggleButton.setIcon(ICON_UP);
            countdownToggleButton.setActionCommand(ACTION_COUNTDOWN);
            countdownToggleButton.addActionListener(this);
            countdownToggleButton.setToolTipText(I18n.text("Count Up/Down"));
        }
        return countdownToggleButton;
    }

    protected MiniButton getResetButton() {
        if (resetButton == null) {
            resetButton = new MiniButton();
            resetButton.setIcon(ICON_RESET);
            resetButton.setActionCommand(ACTION_RESET);
            resetButton.addActionListener(this);
            resetButton.setToolTipText(I18n.text("Reset"));
            resetButton.setVisible(false);
        }
        return resetButton;
    }

    protected MiniButton getAlarmValueButton() {
        if (alarmValueButton == null) {
            alarmValueButton = new MiniButton();
            alarmValueButton.setIcon(ICON_ALARM_OFF);
            alarmValueButton.setActionCommand(ACTION_ALARM);
            alarmValueButton.addActionListener(this);
            alarmValueButton.setToolTipText(I18n.text("Alarm Value"));
        }
        return alarmValueButton;
    }

    /**
     * @return the labelPanel
     */
    public JLabel getLabelPanel() {
        if (labelPanel == null) {
            labelPanel = new JLabel("");
        }
        return labelPanel;
    }
    
    /**
     * Start the timer
     */
    public void start() {
        if (!getStartStopToggleButton().getState())
            getStartStopToggleButton().doClick();
    }

    /**
     * Stop the timer.
     */
    public void stop() {
        if (getStartStopToggleButton().getState())
            getStartStopToggleButton().doClick();
    }

    /**
     * Pause the timer
     */
    public void pause() {
        // updateState(CronEvent.PAUSE);
        if (!getPauseResumeToggleButton().getState())
            getPauseResumeToggleButton().doClick();
    }

    /**
     * Resume the timer, that is stopped by <code>stop()</code>.
     */
    public void resume() {
        // updateState(CronEvent.RESUME);
        if (getPauseResumeToggleButton().getState())
            getPauseResumeToggleButton().doClick();
    }

    /**
     * Count down
     */
    public void countDown() {
        if (!getCountdownToggleButton().getState())
            getCountdownToggleButton().doClick();
    }

    /**
     * Count up
     */
    public void countUp() {
        if (getCountdownToggleButton().getState())
            getCountdownToggleButton().doClick();
    }

    /**
     * Return the elapsed time measured in milliseconds.
     * 
     * @return long, the time.
     */
    public long getMilliSecTime() {
        if (cState == CronState.STARTED)
            msTime = System.currentTimeMillis() - msStart + msAcum;
        else if (cState == CronState.PAUSED)
            msTime = msStop - msStart + msAcum;
        else if (cState == CronState.STOPED)
            msTime = msEnd - msStart + msAcum;
        return msTime;
    }

    public long getSecTime() {
        return getMilliSecTime() / 1000;
    }

    public long getMaxSecs() {
        return maxSecs;
    }

    public void setMaxSecs(long maxSecs) {
        this.maxSecs = maxSecs;
        if (maxSecs <= 0)
            alarmValueButton.setIcon(ICON_ALARM_OFF);
        else
            alarmValueButton.setIcon(ICON_ALARM_ON);
    }

    @Override
    public void actionPerformed(ActionEvent a) {
        String aCommand = a.getActionCommand();
        if (aCommand.equalsIgnoreCase(ACTION_START_STOP)) {
            boolean state = getStartStopToggleButton().getState();
            if (state) { // Start
                updateState(CronEvent.START);
                getStartStopToggleButton().setIcon(ICON_STOP);
                if (getPauseResumeToggleButton().getState()) {
                    getPauseResumeToggleButton().setState(false);
                    getPauseResumeToggleButton().setIcon(ICON_PAUSE);
                }
            }
            else { // Stop
                updateState(CronEvent.STOP);
                getStartStopToggleButton().setIcon(ICON_START);
                if (getPauseResumeToggleButton().getState()) {
                    getPauseResumeToggleButton().setState(false);
                    getPauseResumeToggleButton().setIcon(ICON_PAUSE);
                }
            }
        }
        else if (aCommand.equalsIgnoreCase(ACTION_PAUSE_RESUME)) {
            boolean state = getPauseResumeToggleButton().getState();
            if (state) { // Pause
                updateState(CronEvent.PAUSE);
                getPauseResumeToggleButton().setIcon(ICON_RESUME);
            }
            else { // Resume
                updateState(CronEvent.RESUME);
                getPauseResumeToggleButton().setIcon(ICON_PAUSE);
            }
        }
        else if (aCommand.equalsIgnoreCase(ACTION_COUNTDOWN)) {
            boolean state = getCountdownToggleButton().getState();
            if (state) {
                getCountdownToggleButton().setIcon(ICON_DOWN);
                if (msEnd > 0)
                    updateDisplay();
            }
            else {
                getCountdownToggleButton().setIcon(ICON_UP);
                if (msEnd > 0)
                    updateDisplay();
            }
        }
        else if (aCommand.equalsIgnoreCase(ACTION_RESET)) {
            if (cState == CronState.STOPED) {
                setMaxSecs(0L);
            }
        }
        else if (aCommand.equalsIgnoreCase(ACTION_ALARM)) {
            final JFormattedTextField value = new JFormattedTextField(new HMSFormatter());
            value.setColumns(9);
            value.setValue(getMaxSecs() <= 0 ? 0 : getMaxSecs());
            value.setSelectionStart(0);
            value.setSelectionEnd(value.getText().length());
            final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), I18n.text("Enter the max. time"));
            JPanel jp = new JPanel();
            jp.setLayout(new FlowLayout());
            jp.add(value);
            final JButton okButton = new JButton(I18n.text("Ok"));
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setMaxSecs((Long) value.getValue());
                    dialog.setVisible(false);
                }
            });
            value.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    super.keyReleased(e);
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        okButton.doClick(50);
                    }
                }
            });
            jp.add(okButton);
            dialog.add(jp);
            dialog.setSize(200, 100);
            // dialog.setModal(true);
            dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
            dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
            // value.setSelectionEnd(value.getText().length());
            // value.setSelectionStart(0);
            // value.setCaretPosition(value.getSelectionEnd());
            dialog.setVisible(true);
        }
    }

    protected void updateState(CronEvent event) {
        if (cState == CronState.STOPED) {
            if (event == CronEvent.START) {
                msTime = 0;
                msStart = System.currentTimeMillis();
                msEnd = -1;
                msAcum = 0;
                msStop = -1;
                cState = CronState.STARTED;
                startDisplayUpdate();
            }
        }
        else if (cState == CronState.STARTED) {
            if (event == CronEvent.PAUSE) {
                msStop = System.currentTimeMillis();
                cState = CronState.PAUSED;
            }
            else if (event == CronEvent.STOP) {
                msEnd = System.currentTimeMillis();
                cState = CronState.STOPED;
                stopDisplayUpdate();
                updateDisplay();
                alreadyReported = false;
            }
        }
        else if (cState == CronState.PAUSED) {
            if (event == CronEvent.RESUME) {
                msAcum += msStop - msStart;
                msStart = System.currentTimeMillis();
                cState = CronState.STARTED;
            }
            else if (event == CronEvent.STOP) {
                msEnd = msStop;
                cState = CronState.STOPED;
                stopDisplayUpdate();
                updateDisplay();
            }
        }
    }

    protected void startDisplayUpdate() {
//        if (tTask != null)
//            stopDisplayUpdate();
//        tTask = new TimerTask() {
//            @Override
//            public void run() {
//                updateDisplay();
//            }
//        };
//        timer.scheduleAtFixedRate(tTask, 0, 250);
        
        if (guiUpdateThread == null) {
            guiUpdateThread = new Thread(this.getClass().getSimpleName() + ":" + Integer.toHexString(this.hashCode())) {
                @Override
                public void run() {
                    try {
                        while (true) {
                            updateDisplay();
                            Thread.sleep(250);
                            Thread.yield();
                        }
                    }
                    catch (Exception e) {
                        // Nothing to do
                    }
                    guiUpdateThread = null;
                }
            };
            guiUpdateThread.setDaemon(true);
            guiUpdateThread.start();
        }
    }

    protected void stopDisplayUpdate() {
//        if (tTask != null) {
//            tTask.cancel();
//            tTask = null;
//        }
        if (guiUpdateThread != null) {
            guiUpdateThread.interrupt();
        }
    }

    public void updateDisplay() {
        long t = getMilliSecTime() / 1000;
        if (getMaxSecs() <= 0 && getCountdownToggleButton().getState()) {
            // if (getCountdownToggleButton().getState())
            getCountdownToggleButton().doClick();
            getDisplay().setSecs(t);
        }
        else if (getMaxSecs() > 0 && getCountdownToggleButton().getState()) {
            long tm = getMaxSecs() - t;
            getDisplay().setSecs((tm < 0) ? 0 : tm);
        }
        else {
            getDisplay().setSecs(t);
        }

        if (cState == CronState.STARTED)
            getDisplay().setState(ClockState.START);
        else if (cState == CronState.STOPED)
            getDisplay().setState(ClockState.STOP);
        else if (cState == CronState.PAUSED)
            getDisplay().setState(ClockState.PAUSE);
        else
            getDisplay().setState(ClockState.NONE);

        if (t >= getMaxSecs() && getMaxSecs() > 0) {
            getDisplay().setBackground(COLOR_NOK);
            if (audioAlertOnZero && !alreadyReported)
                SpeechUtil.readSimpleText("Chronometer Alarm");
            alreadyReported = true;
        }
        else {
            getDisplay().setBackground(COLOR_OK);
        }
    }

    public class HMSFormatter extends DefaultFormatter {
        private static final long serialVersionUID = 1L;
        Pattern regex = Pattern.compile("^(((((\\d?\\d" + ClockCounter.HOURS_SEPARATOR + ")?\\d)?\\d"
                + ClockCounter.MINUTES_SEPARATOR + ")?)?\\d)?\\d" + ClockCounter.SECONDS_SEPARATOR + "?$");
        Matcher matcher;

        public HMSFormatter() {
            setValueClass(Long.class);
            setOverwriteMode(false);
            matcher = regex.matcher(""); // create a Matcher for the regular expression
        }

        @Override
        public Object stringToValue(String string) throws java.text.ParseException {
            if (string == null)
                return null;
            matcher.reset(string); // set 'string' as the matcher's input

            if (!matcher.matches()) // Does 'string' match the regular expression?
                throw new java.text.ParseException("does not match regex", 0);

            // If we get this far, then it did match.
            // return super.stringToValue(string); // will honor the 'valueClass' property

            String[] sv = string.split("[" + ClockCounter.HOURS_SEPARATOR + ClockCounter.MINUTES_SEPARATOR + ClockCounter.SECONDS_SEPARATOR + "]");
            Long[] lv = new Long[] { 0L, 0L, 0L }; // sec, min, hour
            for (int i = 0; i < sv.length; i++) {
                int k = sv.length - i - 1;
                lv[i] = Long.parseLong(sv[k]);
            }

            long ret = (long) (lv[0] + lv[1] * 60.0 + lv[2] * 60.0 * 60.0);

            return ret;
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (value == null) {
                return "0";
            }

            long secVal;
            try {
                secVal = (Long) value;
            }
            catch (Exception e) {
                secVal = 0;
            }
            long hr = (long) ((secVal / 60.0 / 60.0) % 24);
            long mi = (long) ((secVal / 60.0) % 60.0);
            long sec = (long) (secVal % 60.0);
            String hrS = Long.toString(hr);
            String miS = Long.toString(mi);
            String secS = Long.toString(sec);
            if (hrS.length() == 1)
                hrS = "0" + hrS;
            if (miS.length() == 1)
                miS = "0" + miS;
            if (secS.length() == 1)
                secS = "0" + secS;
            String time = "" + hrS + ClockCounter.HOURS_SEPARATOR + miS + ClockCounter.MINUTES_SEPARATOR + secS + ClockCounter.SECONDS_SEPARATOR;

            return time;
        }
    }

    public String getFormattedTime() {
        // return DateTimeUtil.timeFormater.format(new Date(getMilliSecTime()));
        double millisVal = getMilliSecTime();
        long hr = (long) ((millisVal / 1000 / 60.0 / 60.0) % 24);
        long mi = (long) ((millisVal / 1000 / 60.0) % 60.0);
        double sec = millisVal / 1000 % 60.0;
        String hrS = Long.toString(hr);
        String miS = Long.toString(mi);
        String secS = Double.toString(MathMiscUtils.round(sec, 3));
        if (hrS.length() == 1)
            hrS = "0" + hrS;
        if (miS.length() == 1)
            miS = "0" + miS;
        if (Long.toString((long) sec).length() == 1)
            secS = "0" + secS;
        String time = "" + hrS + ClockCounter.HOURS_SEPARATOR + miS + ClockCounter.MINUTES_SEPARATOR + secS + ClockCounter.SECONDS_SEPARATOR;

        return time;
    }

    /**
     * @return the audioAlertOnZero
     */
    public boolean isAudioAlertOnZero() {
        return audioAlertOnZero;
    }
    
    /**
     * @param audioAlertOnZero the audioAlertOnZero to set
     */
    public void setAudioAlertOnZero(boolean audioAlertOnZero) {
        this.audioAlertOnZero = audioAlertOnZero;
    }
    
    public static void main(String[] args) {
        ChronometerPanel chronometerPanel = new ChronometerPanel();
        chronometerPanel.setAudioAlertOnZero(true);
        GuiUtils.testFrame(chronometerPanel, ChronometerPanel.class.getSimpleName(), 400, 204);
    }
}
