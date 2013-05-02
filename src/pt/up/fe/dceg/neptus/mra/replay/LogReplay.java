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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.InfiniteProgressPanel;
import pt.up.fe.dceg.neptus.gui.Timeline;
import pt.up.fe.dceg.neptus.gui.TimelineChangeListener;
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
    
    private SimpleDateFormat format = (SimpleDateFormat) DateTimeUtil.timeFormaterUTC.clone(); // new
    private LogMarkersReplay markersReplay = new LogMarkersReplay();

    private Timeline timeline;

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
    private InfiniteProgressPanel loader;
    
    public LogReplay(MRAPanel panel) {
        this.source = panel.getSource();
        this.loader = panel.getLoader();
        
        setLayout(new MigLayout());
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

                if ((entry = parser.getEntryAtOrAfter((long) ((minTime + value)))) != null) {
                    System.out.println(value + " " + (minTime + value) + " " + entry.getAbbrev());
                    setState(entry);
                    
                    timeline.setTime((long) (minTime + value));
                    
                    for (LogReplayLayer layer : replayLayers) {
                        for (String msg : layer.getObservedMessages()) {

                            System.out.println(replayParsers.get(msg) + " " + msg);
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
            }
        });
        
        timeline.getSlider().setValue(0);
        controlsPanel.add(timeline);

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

    protected String getTime(long timeInMillis) {

        if (parser.getCurrentEntry() == null)
            return I18n.text("finished");

        return format.format(new Date(timeInMillis));
    }

    public void setMission(MissionType mt) {
        this.mt = mt;
        LogUtils.generatePath(mt, source);
        renderer.setMission(mt);
        renderer.setPlan(plan);
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


