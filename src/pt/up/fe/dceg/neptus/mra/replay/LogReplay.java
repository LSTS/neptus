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
 * Author: 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.mra.replay;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.InfiniteProgressPanel;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.plots.LogMarkerListener;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.plugins.multibeam.MultibeamReplay;
import pt.up.fe.dceg.neptus.plugins.oplimits.OperationLimits;
import pt.up.fe.dceg.neptus.plugins.sss.SidescanOverlay;
import pt.up.fe.dceg.neptus.renderer2d.MissionRenderer;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;
import pt.up.fe.dceg.neptus.util.llf.LogUtils;

/**
 * @author ZP
 */
@SuppressWarnings("serial")
public class LogReplay extends JPanel implements MRAVisualization, ActionListener, LogMarkerListener {

    private VehicleType vehicle = null;
    private IMraLogGroup source;
    private MissionRenderer renderer;
    private MissionType mt;
    private PlanType plan;
    private double minTime, maxTime, speed = 1.0, currentTime = 0;
    private IMraLog parser;
    private Timer timer = null;
    private SimpleDateFormat format = (SimpleDateFormat) DateTimeUtil.timeFormaterUTC.clone(); // new
    private LogMarkersReplay markersReplay = new LogMarkersReplay();

    private ToolbarButton play, restart, forward, rewind;
    private JLabel curTimeLbl = new JLabel("");
    private JSlider timeline;

    private int jumpToTime = -1;
    private double startTime = 0;

    private Vector<LogReplayLayer> layers = new Vector<LogReplayLayer>();
    {
        layers.add(new GPSFixReplay());
        layers.add(new EstimatedStateReplay());
        layers.add(new SimulatedStateReplay());
        layers.add(new LBLRangesReplay());
        layers.add(new SidescanOverlay());
//        layers.add(new SidescanReplay());
        layers.add(new MultibeamReplay());
        layers.add(new TrexReplay());
        layers.add(markersReplay);
        layers.add(new BathymetryReplay());
    }
    
    protected LinkedHashMap<String, IMraLog> replayParsers = new LinkedHashMap<String, IMraLog>();
    private Vector<LogReplayLayer> renderedLayers = new Vector<LogReplayLayer>();
    private Vector<LogReplayLayer> replayLayers = new Vector<LogReplayLayer>();
    InfiniteProgressPanel loader;
    
    public LogReplay(MRAPanel panel) {
        this.source = panel.getSource();
        this.loader = panel.getLoader();
        
        setLayout(new BorderLayout());

        for (LogReplayLayer layer : layers) {

            if (layer.canBeApplied(source))
                renderedLayers.add(layer);

            if (layer.canBeApplied(source) && layer.getObservedMessages() != null
                    && layer.getObservedMessages().length > 0)
                replayLayers.add(layer);
        }
    }

    
    public void startLogReplay() {
        try {
            loader.setText("Loading mission replay");

            // parse all mission features
            loader.setText(I18n.text("Generating mission"));
            mt = LogUtils.generateMission(source);
            loader.setText(I18n.text("Generating plan"));
            plan = LogUtils.generatePlan(mt, source);

            loader.setText(I18n.text("Generating Operational Limits"));
            OperationLimits limits = LogUtils.getOperationLimits(source);

            // max and min time are calculated from the EstimatedState log
            parser = source.getLog("EstimatedState");
            loader.setText(I18n.text("Calculating total time"));
            minTime = parser.firstLogEntry().getTimestamp();
            maxTime = parser.getLastEntry().getTimestamp();
            parser.firstLogEntry();

            loader.setText(I18n.text("Starting renderers"));
            // if (NeptusMRA.show3D) {
            // try {
            // renderer = new MissionRenderer(mt, MissionRenderer.R2D_AND_R3D1CAM);
            // }
            // catch (Exception e) {
            // renderer = new MissionRenderer(mt, MissionRenderer.R2D_ONLY);
            // }
            // }
            // else
            renderer = new MissionRenderer(mt, MissionRenderer.R2D_ONLY);
            renderer.getRenderer2d().setWorldMapShown(true);
            renderer.getRenderer2d().setShowWorldMapOnScreenControls(true);
            renderer.setInterpolateStatesVisible(false);

            // set plan
            if (plan != null)
                renderer.setPlan(plan);

            // set op limits
            if (limits != null)
                renderer.getRenderer2d().addPostRenderPainter(limits, I18n.text("Operational Limits"));

            // add all layers to the map
            loader.setText(I18n.text("Starting renderer layers"));
            
           

            //loader.setText(I18n.text("Adding marks"));
//            synchronized (toBeAdded) {
//                for (LogMarker marker : toBeAdded) {
//                    addMarkToRenderer(marker);
//                }
//                toBeAdded.clear();
//            }

            Thread t = new Thread() {
                public void run() {
                    for (LogReplayLayer layer : renderedLayers) {
                        try {
                            loader.setText("Loading " + layer.getName());
                            layer.parse(source);
                            renderer.getRenderer2d().addPostRenderPainter(layer, layer.getName());
                            renderer.getRenderer2d().setPainterActive(layer.getName(), layer.getVisibleByDefault());
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            t.setDaemon(true);
            t.start();
            
            // add the map and controls to the interface
            setLayout(new BorderLayout(3, 3));
            add(renderer, BorderLayout.CENTER);
            add(buildControls(), BorderLayout.NORTH);
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        // re-layout everything
        invalidate();
        revalidate();
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

        startTime = minTime;

        // timeline values are from 0 to the total mission time
        timeline = new JSlider(0, (int) (maxTime - startTime));

        // we start at the beginning of the mission
        timeline.setValue(0);

        // whenever the timeline slider is moved by the user
        timeline.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {

                if (timeline.getValue() != (int) (currentTime - startTime)) {
                    if (timer == null) {
                        currentTime = timeline.getValue() + startTime;
                    }

                    IMCMessage m = parser.getCurrentEntry();
                    if (m == null || m.getTimestamp() > currentTime)
                        parser.firstLogEntry();

                    curTimeLbl.setText(getTime((long) (currentTime * 1000)) + " (" + speed + "x)");
                    if (parser.currentTimeMillis() > currentTime) {
                        parser = source.getLog("EstimatedState");
                    }

                    // if (timer != null)
                    // jumpToTime = currentTime;
                    // else
                    // setState(parser.getEntryAtOrAfter((long)(currentTime*1000)));
                }
            }
        });

        controlsPanel.add(timeline);
        controlsPanel.add(curTimeLbl);
        curTimeLbl.setText("");

        return controlsPanel;
    }

    LocationType tmp = null;

    private void setState(IMCMessage entry) {
        currentTime = entry.getTimestamp();

        int sec = (int) (currentTime - startTime);
        // NeptusLog.pub().info("<###>>"+sec);
        if (sec != timeline.getValue()) {
            timeline.setValue(sec);
            curTimeLbl.setText(format.format(new Date((long) (currentTime * 1000))) + " (" + speed + "x)");
        }

        tmp = LogUtils.getLocation(entry);

        SystemPositionAndAttitude state = new SystemPositionAndAttitude(tmp, entry.getDouble("phi"),
                entry.getDouble("theta"), entry.getDouble("psi"));

        vehicle = VehiclesHolder.getVehicleWithImc(new ImcId16(entry.getSrc()));
        renderer.setVehicleState(vehicle, state);
    }

    

    public TimerTask buildReplayTask() {

        replayParsers.put("EstimatedState", parser);

        for (LogReplayLayer layer : replayLayers) {
            for (String msg : layer.getObservedMessages()) {
                replayParsers.put(msg, source.getLog(msg));
                replayParsers.get(msg).nextLogEntry();
            }
        }

        TimerTask tt = new TimerTask() {
            long elapsedTime = 0;
            long lastRunMillis = System.currentTimeMillis();
            IMCMessage entry = parser.getCurrentEntry();

            @Override
            public void run() {

                elapsedTime += (System.currentTimeMillis() - lastRunMillis) * speed;
                lastRunMillis = System.currentTimeMillis();

                if (jumpToTime != -1) {
                    elapsedTime = (long) (jumpToTime - minTime) * 1000;
                    jumpToTime = -1;
                }

                double replayTime = elapsedTime / 1000.0;
                double missionTime = startTime + replayTime;

                if (timeline.getValue() != (int) replayTime) {
                    timeline.setValue((int) replayTime);
                }

                if (parser.currentTimeMillis() > missionTime * 1000)
                    parser.firstLogEntry();
                
                if ((entry = parser.getEntryAtOrAfter((long) (missionTime * 1000))) != null) {
                    NeptusLog.pub().info("<###> "+entry.getSrc());
                    currentTime = entry.getTimestamp();
                    setState(entry);

                    for (LogReplayLayer layer : replayLayers) {
                        for (String msg : layer.getObservedMessages()) {

                            IMCMessage entry = replayParsers.get(msg).getEntryAtOrAfter((long) (currentTime * 1000));

                            if (entry != null) {
                                try {
                                    layer.onMessage(entry);
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().warn(e);
                                }
                            }
                                
                        }
                    }
                }
                else {
                    currentTime = minTime;
                    timer.cancel();
                    timer = null;
                    play.setActionCommand("play");
                    play.setToolTipText("play");
                    play.setIcon(ImageUtils.getIcon("images/buttons/play.png"));
                }
            }
        };

        return tt;
    }

    public void restart() {
        if (timer != null)
            timer.cancel();
        parser = source.getLog("EstimatedState");
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
        play.setToolTipText("play");
        play.setIcon(ImageUtils.getIcon("images/buttons/play.png"));
    }

    public void play() {
        NeptusLog.pub().info("<###>play");
        if (timer != null) {
            timer.purge();
            timer.cancel();
        }

        // parser = source.getLog("EstimatedState");
        parser.firstLogEntry();
        parser.getEntryAtOrAfter((long) (currentTime * 1000));
        currentTime = parser.getLastEntry().getTimestampMillis();
        timer = new Timer("Replay Timer");
        timer.scheduleAtFixedRate(buildReplayTask(), 0, 33);

        play.setActionCommand("pause");
        play.setToolTipText("pause");
        play.setIcon(ImageUtils.getIcon("images/buttons/pause.png"));
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    protected String getTime(long timeInMillis) {

        if (parser.getCurrentEntry() == null)
            return I18n.text("finished");

        return format.format(new Date(timeInMillis));
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("ff")) {
            if (speed >= 16)
                return;

            speed *= 2;

            curTimeLbl.setText(getTime((long) (currentTime * 1000)) + " (" + speed + "x)");
        }

        else if (e.getActionCommand().equals("rew")) {
            if (speed <= 0.125)
                return;

            speed *= 0.5;

            curTimeLbl.setText(getTime((long) (currentTime * 1000)) + " (" + speed + "x)");
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
    }


    public void setMission(MissionType mt) {
        this.mt = mt;
        LogUtils.generatePath(mt, source);
        renderer.setMission(mt);
        renderer.setPlan(plan);
    }

    public MissionType getMt() {
        return mt;
    }

    // --- MRAVisualization ---
    @Override
    public JComponent getComponent(IMraLogGroup source, double timestep) {
        startLogReplay();
        return this;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLog("EstimatedState") != null;
    }

    @Override
    public String getName() {
        return I18n.text("Replay");
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
    
    public void onCleanup() {
        if (timer != null)
            timer.cancel();
        if (renderer != null)
            renderer.cleanup();
        for (LogReplayLayer layer : layers) {
            try {
                layer.cleanup();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // --- LogMarkerListener ---
    @Override
    public void removeLogMarker(LogMarker marker) {
        markersReplay.removeMarker(marker);
    }

    @Override
    public void addLogMarker(LogMarker marker) {
        markersReplay.addMarker(marker);
    }
    
    @Override
    public void GotoMarker(LogMarker marker) {
        
    }
}


