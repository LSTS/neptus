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
package pt.lsts.neptus.mra.replay;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.tree.TreePath;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.gui.Timeline;
import pt.lsts.neptus.gui.TimelineChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.plots.LogMarkerListener;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.plugins.mraplots.ReplayPlot;
import pt.lsts.neptus.plugins.multibeam.MultibeamReplay;
import pt.lsts.neptus.plugins.oplimits.OperationLimits;
import pt.lsts.neptus.renderer2d.MissionRenderer;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.llf.LogUtils;
import pt.lsts.neptus.util.llf.LsfTree;

/**
 * @author ZP
 */
@SuppressWarnings("serial")
public class LogReplay extends JPanel implements MRAVisualization, LogMarkerListener {
    private MissionRenderer renderer;

    private VehicleType vehicle = null;
    private LocationType loc = null;
    
    private IMraLogGroup source;
    
    private MissionType mt;
    private PlanType plan;
    
    private double minTime, maxTime;
    private IMraLog parser;
    private Timer timer = null;

    private LogMarkersReplay markersReplay = new LogMarkersReplay();

    private Timeline timeline;

    private JButton plotButton;
    
    private Vector<LogReplayLayer> layers = new Vector<LogReplayLayer>();
    {
        layers.add(new GPSFixReplay());
        layers.add(new EstimatedStateReplay());
        layers.add(new SimulatedStateReplay());
        layers.add(new LBLRangesReplay());
//        layers.add(new SidescanOverlay());
//        layers.add(new SidescanReplay());
        layers.add(new MultibeamReplay());
        layers.add(new TrexReplay());
        layers.add(markersReplay);
        layers.add(new BathymetryReplay());
        layers.add(new AnnouncesReplay());
    }
    
    protected LinkedHashMap<String, IMraLog> replayParsers = new LinkedHashMap<String, IMraLog>();
    private Vector<LogReplayLayer> renderedLayers = new Vector<LogReplayLayer>();
    private Vector<LogReplayLayer> replayLayers = new Vector<LogReplayLayer>();
    
    private ArrayList<ReplayPlot> replayPlots = new ArrayList<ReplayPlot>();
    
    private InfiniteProgressPanel loader;
    
    
    int currentValue = 0; // This value will be used to know if we regressed in timeline
    
    MRAPanel panel;
    LsfTree tree;
    
    public LogReplay(MRAPanel panel) {
        this.source = panel.getSource();
        this.loader = panel.getLoader();
        this.panel = panel;
        
        this.tree = new LsfTree(this.source);
        
        setLayout(new MigLayout());
    }

    
    public void startLogReplay() {
        try {
            loader.setText(I18n.text("Loading mission replay"));

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
            minTime = parser.firstLogEntry().getTimestampMillis();
            maxTime = parser.getLastEntry().getTimestampMillis();
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

            for (LogReplayLayer layer : layers) {

                if (layer.canBeApplied(source))
                    renderedLayers.add(layer);

                if (layer.canBeApplied(source) && layer.getObservedMessages() != null
                        && layer.getObservedMessages().length > 0) {
                    replayLayers.add(layer);

                    for (String msg : layer.getObservedMessages()) {
                        replayParsers.put(msg, source.getLog(msg));
                    }
                }
            }

            replayParsers.put("EstimatedState", parser);
            Thread t = new Thread("Replay updater") {
                public void run() {
                    for (LogReplayLayer layer : renderedLayers) {
                        try {
                            loader.setText(I18n.textf("Loading %layerName", layer.getName()));
                            layer.parse(source);
                            renderer.getRenderer2d().addPostRenderPainter(layer, layer.getName());
                            renderer.getRenderer2d().setPainterActive(layer.getName(), layer.getVisibleByDefault());
                            renderer.getRenderer2d().repaint();                            
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    renderer.getRenderer2d().repaint();                                        
                }
            };
            t.setDaemon(true);
            t.start();

            TimerTask tt = new TimerTask() {
                
                @Override
                public void run() {
                    renderer.getRenderer2d().repaint();
                }
            };
            
            timer = new Timer("Log Replay renderer updater");
            timer.scheduleAtFixedRate(tt, 1000, 1000);
            
            // add the map and controls to the interface
            add(buildControls(), "w 100%, wrap");
            add(renderer, "w 100%, h 100%");
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

        timeline = new Timeline(0, (int)(maxTime - minTime), 32, 1000, false);
        
        timeline.addTimelineChangeListener(new TimelineChangeListener() {
            
            @Override
            public void timelineChanged(int value) {
                IMCMessage entry;
                
                if(value < currentValue) {
                    for(IMraLog l : replayParsers.values()) {
                        l.firstLogEntry();
                    }
                }
                
                if ((entry = parser.getEntryAtOrAfter((long) ((minTime + value)))) != null) {
                    setState(entry);
                    
                    timeline.setTime((long) (minTime + value));
                    
                    for (LogReplayLayer layer : replayLayers) {
                        for (String msg : layer.getObservedMessages()) {
                            if(replayParsers.get(msg) == null) 
                                continue;
                            
                            IMCMessage m = replayParsers.get(msg).getEntryAtOrAfter((long) ((minTime + value)));

                            if (m != null) {
                                try {
                                    layer.onMessage(m);
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().warn(e);
                                }
                            }
                        }
                    }
                }
                
                for(ReplayPlot plot : replayPlots) {
                    plot.timelineChanged(value);
                }
                
                currentValue = value;
            }
        });
        
        timeline.getSlider().setValue(0);
        controlsPanel.add(timeline);
        
        plotButton = new JButton(I18n.text("Plots"));
        
        plotButton.setAction(new AbstractAction(I18n.text("Plots")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Vector<String> fields = new Vector<String>();

                // Get fields to plot
                final JDialog fieldDialog = new JDialog();
                JScrollPane scroll = new JScrollPane(tree);
                fieldDialog.setModal(true);
                fieldDialog.setSize(300, 500);
                fieldDialog.setLayout(new MigLayout());
                fieldDialog.add(scroll, "w 100%, h 100%, wrap");
                fieldDialog.add(new JButton(new AbstractAction(I18n.text("Ok")) {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        fieldDialog.setVisible(false);
                    }
                }), "split");
                
                fieldDialog.add(new JButton(new AbstractAction(I18n.text("Cancel")) {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        fields.clear();
                        fieldDialog.setVisible(false);
                    }
                }));

                tree.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        TreePath[] path = tree.getSelectionPaths();
                        
                        if(path == null)
                            return;
                        
                        fields.clear();
                        for (int i = 0; i < path.length; i++) {
                            if (path[i].getPath().length == 3) {
                                String message = path[i].getPath()[1].toString();
                                String field = path[i].getPath()[2].toString();
                                
                                fields.add(message + "." + field);
                            }
                        }
                    }
                });
                
                // As fieldDialog is modal execution pauses here.
                fieldDialog.setVisible(true);
                
                ReplayPlot plot = new ReplayPlot(panel, fields.toArray(new String[0]));
                
                if(fields.size() != 0 && plot.canBeApplied(source)) {
                    replayPlots.add(plot);
                    
                    JDialog dialog = new JDialog();
                    dialog.setLayout(new MigLayout());
                    dialog.setSize(640, 480);
                    dialog.add(plot.getComponent(source, 0), "w 100%, h 100%");

                    plot.setTimelineVisible(false);
                    dialog.setVisible(true);
                }
            }
        });
        
        controlsPanel.add(plotButton);

        return controlsPanel;
        
    }

    private void setState(IMCMessage entry) {
//        currentTime = entry.getTimestamp();

//        int sec = (int) (currentTime - startTime);
        // NeptusLog.pub().info("<###>>"+sec);
//        if (sec != slider.getValue()) {
//            slider.setValue(sec);
//            curTimeLbl.setText(format.format(new Date((long) (currentTime * 1000))) + " (" + speed + "x)");
//        }

        loc = LogUtils.getLocation(entry);

        SystemPositionAndAttitude state = new SystemPositionAndAttitude(loc, entry.getDouble("phi"),
                entry.getDouble("theta"), entry.getDouble("psi"));

        vehicle = VehiclesHolder.getVehicleWithImc(new ImcId16(entry.getSrc()));
        renderer.setVehicleState(vehicle, state);
    }
    
    public void setMission(MissionType mt) {
        this.mt = mt;
        LogUtils.generatePath(mt, source);
        renderer.setMission(mt);
        renderer.setPlan(plan);
    }

    // --- MRAVisualization ---
    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
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


