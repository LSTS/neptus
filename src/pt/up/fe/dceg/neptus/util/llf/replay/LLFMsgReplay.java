/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias and zepinto
 * 2007/09/25
 */
package pt.up.fe.dceg.neptus.util.llf.replay;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.swingx.JXLabel;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.ImagePanel;
import pt.up.fe.dceg.neptus.gui.InfiniteProgressPanel;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.gui.swing.NeptusFileView;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfoImpl;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.SystemImcMsgCommInfo;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.util.llf.LogUtils;

/**
 * @author pdias
 * @author ZP
 */
@SuppressWarnings("serial")
public class LLFMsgReplay extends JPanel implements MRAVisualization, ActionListener {

    private VehicleType vehicle = null;
    private IMraLogGroup source;

    private ToolbarButton play, restart, forward, rewind;
    private JLabel curTimeLbl = new JLabel("0.0");
    private JSlider timeline;

    private JPanel infoPanel = null;

    private LinkedList<IMraLog> logsParser;
    private Timer timer = null;
    private double currentTime = 0;
    private double jumpToTime = -1;
    private double minTime, maxTime;
    private double speed = 1.0;
    private double startTimeFull;
    private double minTimeFull;
    private double maxTimeFull;

    private JLabel idVehicleText;
    private JLabel startTimeText;
    private JLabel endTimeText;
    private ImagePanel vehicleImg;
    private JLabel deltaTimeText;
    private JLabel genInfoLabel;

    private boolean cleanCalled = false;
    InfiniteProgressPanel loader;
    
    public LLFMsgReplay(MRAPanel panel) {
        this.source = panel.getSource();
        this.loader = panel.getLoader();
        
        setLayout(new BorderLayout());
    }

    public void startLLFReplay() {
        loader.setText(I18n.text("Loading mission messages replay"));
        if (cleanCalled)
            return;
        try {
            vehicle = LogUtils.getVehicle(source);

            loadLogParsers();
            calcMinMaxStarTimes();
            loadLogParsers();

            setLayout(new BorderLayout(3, 3));
            add(getInfoPanel(), BorderLayout.CENTER);
            add(buildControls(), BorderLayout.NORTH);
        }
        catch (Exception e) {
            e.printStackTrace();
            JXLabel errLb = new JXLabel("<html><b>" + I18n.textf("Not possible to replay log %log.", source.name()));
            errLb.setHorizontalAlignment(JLabel.CENTER);
            errLb.setVerticalAlignment(JLabel.CENTER);
            setLayout(new BorderLayout(3, 3));
            add(errLb, BorderLayout.CENTER);
        }
    }

    private JPanel getInfoPanel() {
        if (infoPanel == null) {
            infoPanel = new JPanel();
            // infoPanel.setLayout(new FlowLayout());
            vehicleImg = new ImagePanel();
            if (vehicle != null) {
                if (vehicle.getPresentationImageHref().equalsIgnoreCase(""))
                    vehicleImg.setImage(vehicle.getSideImageHref());
                else
                    vehicleImg.setImage(vehicle.getPresentationImageHref());
            }
            vehicleImg.setSize(174, 92);
            vehicleImg.setPreferredSize(new java.awt.Dimension(174, 92));
            vehicleImg.setMinimumSize(new java.awt.Dimension(174, 92));
            if (vehicle != null)
                vehicleImg.adjustImageSizeToPanelSize();
            // infoPanel.add(vehicleImg);

            // infoPanel.add(new JLabel("<html><b>Vehicle: "));
            JLabel idVehicleLabel = new JLabel("<html><b>" + I18n.text("Vehicle") + ": ");
            idVehicleText = new JLabel((vehicle != null) ? vehicle.getId() : "<<Unknown>>");
            // infoPanel.add(idVehicleText);

            Date[] estStateMinMaxTimesSeconds = null;
            IMraLog logES = source.getLog("EstimatedState");
            if (logES != null) {
                estStateMinMaxTimesSeconds = LogUtils.getMessageMinMaxDates(logES);
            }

            JLabel startTimeLabel = new JLabel("<html><b>" + I18n.text("Start time") + ": ");
            // infoPanel.add(startTimeLabel);
            startTimeText = new JLabel(DateTimeUtil.dateTimeFormater2UTC.format(
                    estStateMinMaxTimesSeconds != null ? estStateMinMaxTimesSeconds[0] : new Date(Double.valueOf(
                            startTimeFull * 1000).longValue())).toString());
            // infoPanel.add(startTimeText);
            JLabel endTimeLabel = new JLabel("<html><b>" + I18n.text("End time") + ": ");
            // infoPanel.add(endTimeLabel);
            endTimeText = new JLabel(DateTimeUtil.dateTimeFormater2UTC.format(
                    estStateMinMaxTimesSeconds != null ? estStateMinMaxTimesSeconds[1] : new Date(Double.valueOf(
                            maxTimeFull * 1000).longValue())).toString());
            // infoPanel.add(endTimeText);
            JLabel durationTimeLabel = new JLabel("<html><b>" + I18n.text("Mission duration") + ": ");
            // infoPanel.add(durationTimeLabel);
            deltaTimeText = new JLabel(
                    DateTimeUtil
                            .milliSecondsToFormatedString((long) ((estStateMinMaxTimesSeconds != null ? estStateMinMaxTimesSeconds[1]
                                    .getTime() - estStateMinMaxTimesSeconds[0].getTime()
                                    : (maxTimeFull - startTimeFull) * 1000.0))));
            // infoPanel.add(deltaTimeText);

            LinkedHashMap<String, String> genInfoHT = LogUtils.generateStatistics(source);
            String infoStr = "<html>";
            for (String infoKey : genInfoHT.keySet()) {
                if (infoKey.startsWith("Mission start time") || infoKey.startsWith("Mission end time")
                        || infoKey.startsWith("Mission duration"))
                    continue;
                infoStr += "<b>" + I18n.text(infoKey) + ":</b> " + genInfoHT.get(infoKey) + "<br>";
            }
            genInfoLabel = new JLabel(infoStr);

            JPanel holder = new JPanel();
            GroupLayout layout = new GroupLayout(holder);
            holder.setLayout(layout);

            layout.setAutoCreateGaps(true);
            layout.setHorizontalGroup(layout
                    .createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addGroup(
                            layout.createSequentialGroup()
                                    .addComponent(vehicleImg)
                                    .addGroup(
                                            layout.createSequentialGroup()
                                                    .addGroup(
                                                            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                                    .addComponent(idVehicleLabel)
                                                                    .addComponent(startTimeLabel)
                                                                    .addComponent(endTimeLabel)
                                                                    .addComponent(durationTimeLabel))
                                                    .addGroup(
                                                            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                                    .addComponent(idVehicleText)
                                                                    .addComponent(startTimeText)
                                                                    .addComponent(endTimeText)
                                                                    .addComponent(deltaTimeText))))
                    .addComponent(genInfoLabel));

            layout.setVerticalGroup(layout
                    .createSequentialGroup()
                    .addGroup(
                            layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                    .addComponent(vehicleImg)
                                    .addGroup(
                                            layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                    .addGroup(
                                                            layout.createSequentialGroup().addComponent(idVehicleLabel)
                                                                    .addComponent(startTimeLabel)
                                                                    .addComponent(endTimeLabel)
                                                                    .addComponent(durationTimeLabel))
                                                    .addGroup(
                                                            layout.createSequentialGroup().addComponent(idVehicleText)
                                                                    .addComponent(startTimeText)
                                                                    .addComponent(endTimeText)
                                                                    .addComponent(deltaTimeText))))
                    .addComponent(genInfoLabel));

            infoPanel.setLayout(new FlowLayout());
            infoPanel.add(holder);
        }
        return infoPanel;
    }

    private void calcMinMaxStarTimes() {
        startTimeFull = 0.0;
        minTimeFull = Double.POSITIVE_INFINITY;
        maxTimeFull = Double.NEGATIVE_INFINITY;
        try {
            for (IMraLog parser : logsParser.toArray(new IMraLog[0])) {
                IMCMessage firstEntry = parser.nextLogEntry();

                if (firstEntry == null) {
                    logsParser.remove(parser);
                    continue;
                }

                IMCMessage lastEntry = parser.getLastEntry();
                double startTime = 0; // Now the time is absolute : firstEntry.getTimestamp();
                double minTime = ((Double) firstEntry.getDouble("timestamp"));
                double maxTime = ((Double) lastEntry.getDouble("timestamp"));
                if (startTime + minTime < minTimeFull) {
                    minTimeFull = startTime + minTime;
                    startTimeFull = minTime; // startTime;
                }
                if (startTime + maxTime > maxTimeFull) {
                    maxTimeFull = startTime + maxTime;
                }

            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        Date date = new Date(Double.valueOf(startTimeFull * 1000).longValue());
        NeptusLog.pub().info("<###> "+date + " [" + (minTimeFull - startTimeFull) + ":" + (maxTimeFull - startTimeFull) + "]");

        minTime = minTimeFull - startTimeFull;
        maxTime = maxTimeFull - startTimeFull;
    }

    private void loadLogParsers() {
        logsParser = new LinkedList<IMraLog>();
        for (String str : source.listLogs()) {
            IMraLog parser = source.getLog(str);
            logsParser.add(parser);
        }
    }

    private JToolBar buildControls() {

        JToolBar controlsPanel = new JToolBar();

        play = new ToolbarButton("images/buttons/play.png", I18n.text("Play"), "play");
        play.addActionListener(this);
        controlsPanel.add(play);

        restart = new ToolbarButton("images/buttons/restart.png", I18n.text("Restart"), "restart");
        restart.addActionListener(this);
        controlsPanel.add(restart);

        rewind = new ToolbarButton("images/buttons/rew.png", I18n.text("Slower"), "rew");
        rewind.addActionListener(this);
        controlsPanel.add(rewind);

        forward = new ToolbarButton("images/buttons/fwd.png", I18n.text("Faster"), "ff");
        forward.addActionListener(this);
        controlsPanel.add(forward);

        // timeline = new JSlider((int) minTime, (int) maxTime);
        // timeline.setValue((int) minTime);
        timeline = new JSlider(0, (int) (maxTime - minTime));
        timeline.setValue(0);

        timeline.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {

                // if (timeline.getValue() != (int) currentTime) {
                if ((int) (timeline.getValue() + minTime) != (int) currentTime) {
                    if (timer != null)
                        jumpToTime = timeline.getValue() + minTime; // jumpToTime = timeline.getValue();
                    else {
                        currentTime = timeline.getValue() + minTime; // currentTime = timeline.getValue();
                        updateCurTimeLabelText(); // curTimeLbl.setText((int) (timeline.getValue()) + ".0 s (" + speed +
                                                  // "x)"); //curTimeLbl.setText(timeline.getValue() + ".0 s (" + speed
                                                  // + "x)");
                    }
                }
            }
        });

        timeline.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() > 1)
                    timeline.setValue(0);
            }
        });

        controlsPanel.add(timeline);
        controlsPanel.add(curTimeLbl);
        updateCurTimeLabelText(); // curTimeLbl.setText(GuiUtils.getNeptusDecimalFormat(1).format(minTime) + " s");

        if (vehicle == null) {
            play.setEnabled(false);
            restart.setEnabled(false);
            rewind.setEnabled(false);
            forward.setEnabled(false);
            timeline.setEnabled(false);
        }

        return controlsPanel;
    }

    private void updateCurTimeLabelText() {
        curTimeLbl.setText(DateTimeUtil.timeFormaterUTC.format(new Date((long) ((startTimeFull + currentTime) * 1000)))
                + " (" + speed + "x)");
    }

    public TimerTask buildReplayTask() {
        TimerTask tt = new TimerTask() {
            // LLFEntry entry = parser.nextLogEntry();
            double lastTimeFull = (double) System.currentTimeMillis() / 1000.0;
            double ellapsedTime = Math.max(currentTime, minTime);

            @Override
            public void run() {
                if (ellapsedTime <= maxTime/* entry != null */) {

                    if (((int) (currentTime - minTime)) != timeline.getValue()) { // if (((int) currentTime) !=
                                                                                  // timeline.getValue()) {
                        timeline.setValue((int) (currentTime - minTime)); // timeline.setValue((int) currentTime);
                    }
                    updateCurTimeLabelText(); // curTimeLbl.setText(GuiUtils.getNeptusDecimalFormat(1).format(currentTime
                                              // - minTime) + " s (" + speed + "x)");
                                              // //curTimeLbl.setText(GuiUtils.getNeptusDecimalFormat(1).format(currentTime)
                                              // + " s (" + speed + "x)");

                    double thisTimeFull = (double) System.currentTimeMillis() / 1000.0;

                    double deltaPassedTime = speed * (thisTimeFull - lastTimeFull);
                    double maxCurrentTime = currentTime + deltaPassedTime;

                    // NeptusLog.pub().info("<###>>" + currentTime +" till " + maxCurrentTime);

                    Vector<IMCMessage> entriesVector = new Vector<IMCMessage>();
                    Vector<Double> entriesTimeAbsVector = new Vector<Double>();
                    for (IMraLog parser : logsParser) {
                        double logStartTime = parser.firstLogEntry().getTimestamp();
                        double dST = startTimeFull - logStartTime;
                        double curFixTime = currentTime - dST;
                        double curMaxFixTime = maxCurrentTime - dST;
                        // NeptusLog.pub().info("<###> "+logStartTime + "\t" + startTimeFull + "  dt:" + dST + "  " + currentTime +
                        // " :: " +curFixTime +" till " + curMaxFixTime);
                        if (curFixTime < 0)
                            curFixTime = 0;
                        if (curMaxFixTime < 0)
                            continue;
                        while (true) {
                            double curET = parser.currentTimeMillis() / 1000.0;
                            // NeptusLog.pub().info("<###>curET= " + curET + "   " + (curET > curFixTime + startTimeFull) +
                            // " && " + (curET <= curMaxFixTime + startTimeFull));
                            if (curET > curFixTime + startTimeFull && curET <= curMaxFixTime + startTimeFull) {
                                entriesVector.add(parser.getCurrentEntry()); // TODO check...
                                entriesTimeAbsVector.add(logStartTime + curET);
                            }
                            if (curET > curMaxFixTime + startTimeFull)
                                break;
                            if (parser.nextLogEntry() == null)
                                break;
                        }
                        // if (debug) NeptusLog.pub().info("<###> "+i + " " + parser.getLogFormat().getSimpleName());
                    }

                    // @FIXME IMC3
                    // VehicleNepMsgCommInfo vci = Imc3MsgManager.getManager().getCommInfoById(vehicle.getId());
                    SystemImcMsgCommInfo vci = ImcMsgManager.getManager().getCommInfoById(vehicle.getId());

                    for (int i = 0; i < entriesVector.size(); i++) {
                        if (vci == null) // FIXME
                            break;
                        // NeptusLog.pub().info("<###> "+entry.getAsMessage());
                        IMCMessage entry = entriesVector.get(i);
                        double timeStp = entriesTimeAbsVector.get(i);
                        IMCMessage msg;
                        msg = entry;

                        MessageInfo info = new MessageInfoImpl();
                        info.setTimeSentNanos((long) (timeStp * 1E6));
                        info.setTimeReceivedNanos(System.currentTimeMillis() * (long) 1E6);
                        info.setProperty(MessageInfo.NOT_TO_LOG_MSG_KEY, "true");
                        if (vci != null)
                            vci.onMessage(info, msg);
                    }

                    if (jumpToTime != -1) {
                        if (jumpToTime < ellapsedTime) {

                            loadLogParsers();
                        }
                        ellapsedTime = jumpToTime;
                        jumpToTime = -1;
                    }
                    else {
                        ellapsedTime += speed * (thisTimeFull - lastTimeFull);
                    }

                    lastTimeFull = thisTimeFull;
                    currentTime = ellapsedTime;

                    // Get all entries

                }
                else {
                    currentTime = minTime;
                    timer.cancel();
                    timer = null;
                    play.setActionCommand("play");
                    play.setToolTipText(I18n.text("play"));
                    play.setIcon(ImageUtils.getIcon("images/buttons/play.png"));
                }
            }
        };

        return tt;
    }

    public void restart() {
        if (timer != null)
            timer.cancel();
        // parser = source.getParser("EstimatedState");
        loadLogParsers();
        currentTime = minTime;
        play();
    }

    public void pause() {
        if (timer != null) {
            timer.purge();
            timer.cancel();
        }

        timer = null;

        play.setActionCommand("play");
        play.setToolTipText(I18n.text("play"));
        play.setIcon(ImageUtils.getIcon("images/buttons/play.png"));
    }

    public void play() {
        if (timer != null) {
            timer.purge();
            timer.cancel();
        }

        // parser = source.getParser("EstimatedState");
        // parser.getEntryAfter(currentTime);
        // currentTime = parser.getLastEntryTime();

        timer = new Timer("Replay Timer", true);
        timer.scheduleAtFixedRate(buildReplayTask(), 0, 33);

        play.setActionCommand("pause");
        play.setToolTipText(I18n.text("pause"));
        play.setIcon(ImageUtils.getIcon("images/buttons/pause.png"));
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("ff")) {
            if (speed >= 16)
                return;

            speed *= 2;
            updateCurTimeLabelText(); // curTimeLbl.setText(GuiUtils.getNeptusDecimalFormat(1).format(currentTime -
                                      // minTime) + " s (" + speed + "x)");
        }
        else if (e.getActionCommand().equals("rew")) {
            if (speed <= 0.125)
                return;

            speed *= 0.5;

            updateCurTimeLabelText(); // curTimeLbl.setText(GuiUtils.getNeptusDecimalFormat(1).format(currentTime -
                                      // minTime) + " s (" + speed + "x)");
        }
        else if (e.getActionCommand().equals("pause")) {
            pause();
        }
        else if (e.getActionCommand().equals("play")) {
            play();
        }
        else if (e.getActionCommand().equals("restart")) {
            restart();
        }
        else if (e.getActionCommand().equals("open")) {
            JFileChooser chooser = new JFileChooser(ConfigFetch.getConfigFile());
            chooser.setFileFilter(GuiUtils.getCustomFileFilter("LLF files", new String[] { "llf" }));
            chooser.setFileView(new NeptusFileView());
            chooser.showDialog(SwingUtilities.getWindowAncestor(LLFMsgReplay.this), I18n.text("Open"));
        }

    }

    public void onCleanup() {
        cleanCalled = true;
        if (timer != null)
            timer.cancel();
        // if (renderer != null)
        // renderer.cleanup();
    }

    @Override
    public String getName() {
        return I18n.text("Statistics/Replay Msg");
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        startLLFReplay();
        return this;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("images/menus/replay.png");
    }

    @Override
    public Double getDefaultTimeStep() {
        return null;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }

    public Type getType() {
        return Type.VISUALIZATION;
    }

    @Override
    public void onHide() {

    }

    public void onShow() {

    }
}
