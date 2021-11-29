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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jan 29, 2014
 */
package pt.lsts.neptus.mra.replay;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.LblBeacon;
import pt.lsts.imc.LblConfig;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.plots.LogMarkerListener;
import pt.lsts.neptus.mra.replay.LogReplayComponent.Context;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.llf.LogUtils;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Mission Replay", icon = "pt/lsts/neptus/mra/replay/replay.png")
public class MRALogReplay extends SimpleMRAVisualization implements LogMarkerListener, MissionChangeListener {

    private static final long serialVersionUID = 1L;
    private LsfIndex index;
    private IMraLogGroup source;
    private final Vector<LogReplayLayer> layers = new Vector<>();
    private final Vector<LogReplayPanel> panels = new Vector<>();
    private final AsyncEventBus replayBus = new AsyncEventBus("Replay Event bus", Executors.newFixedThreadPool(2));
    private StateRenderer2D r2d;
    private JToolBar layersToolbar;
    private MRALogReplayTimeline timeline;
    private final LinkedHashMap<String, Vector<LogReplayComponent>> observers = new LinkedHashMap<>();
    private final LinkedHashMap<LogReplayPanel, JDialog> popups = new LinkedHashMap<>();
    private final MRAPanel panel;
    

    public MRALogReplay(MRAPanel panel) {
        super(panel);
        this.panel = panel;
    }
    
    
   
    @Override
    public void onShow() {
        super.onShow();
    }

    @Override
    public Type getType() {
        return MRAVisualization.Type.VISUALIZATION;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        this.source = source;
        return true;
    }
    
    @Subscribe
    public synchronized void on(LblConfig m) {
        
        for (LblBeacon b: m.getBeacons()) {
            String id = b.getBeacon();
            double lat = Math.toDegrees(b.getLat());
            double lon = Math.toDegrees(b.getLon());
            double depth = b.getDepth();
            LocationType lt = new LocationType();
            lt.setLatitudeDegs(lat);
            lt.setLongitudeDegs(lon);
            lt.setDepth(depth);
            try {
                TransponderElement el = (TransponderElement) r2d.getMapGroup().getMapObjectsByID(id)[0];
                el.setCenterLocation(lt);
                r2d.mapChanged(new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED));
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
            
        }
        
    }

    @Subscribe
    public synchronized void on(IMCMessage m) {
        try {
            if (observers.containsKey(m.getAbbrev())) {
                for (LogReplayComponent c : observers.get(m.getAbbrev())) {
                    try {
                        c.onMessage(m);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(
                                "Error occured while sending replayed message '" + m.getAbbrev() + "' to component '"
                                        + c.getName() + "'");
                        e.printStackTrace();
                    }
                }
                r2d.repaint();
            }
    
            if (m.getAbbrev().equals("EstimatedState")) {
                SystemPositionAndAttitude state = IMCUtils.parseState(m);
                r2d.vehicleStateChanged(m.getSourceName(), state);
                r2d.repaint();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bootstrapComponent(final LogReplayComponent comp) {
        comp.parse(source);
        replayBus.register(comp);
        
        String[] msgs = comp.getObservedMessages();
        if (msgs != null) {
            for (String m : msgs) {
                if (!observers.containsKey(m))
                    observers.put(m, new Vector<LogReplayComponent>());
                observers.get(m).add(comp);
            }
        }

    }
    
    private void bootstrap(final LogReplayLayer l) {
        bootstrapComponent(l);
        
        if (l.getVisibleByDefault())
            r2d.addPostRenderPainter(l, l.getName());
        ToolbarSwitch ts = new ToolbarSwitch(ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(l.getClass()), 16, 16),
                l.getName(), l.getName());
        ts.setSelected(l.getVisibleByDefault());
        ts.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected())
                    r2d.addPostRenderPainter(l, l.getName());
                else
                    r2d.removePostRenderPainter(l);
                r2d.repaint();
            }
        });

        if (l.getVisibleByDefault())
            layersToolbar.add(ts, 0);
        else
            layersToolbar.add(ts, layersToolbar.getComponentCount());

        layersToolbar.invalidate();
        layersToolbar.validate();
    }

    private JDialog getPopup(final LogReplayPanel p) {

        if (popups.containsKey(p))
            return popups.get(p);

        final JDialog d = new JDialog(SwingUtilities.getWindowAncestor(panel));
        d.setTitle(p.getName());
        d.getContentPane().add(p.getComponent());
        d.pack();
        GuiUtils.centerOnScreen(d);
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                popups.remove(p);
            }
        });
        popups.put(p, d);
        d.setVisible(true);
        return d;
    }

    private void bootstrapPanel(final LogReplayPanel p) {
        bootstrapComponent(p);
        

        final ToolbarSwitch ts = new ToolbarSwitch(ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(p.getClass()), 16, 16),
                p.getName(), p.getName());
        ts.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ToolbarSwitch ts = ((ToolbarSwitch) e.getSource());
                
                if (ts.isSelected()) {
                    JDialog d = getPopup(p);
                    d.setVisible(true);
                    d.toFront();
                    d.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosed(WindowEvent e) {
                            ts.setSelected(false);
                        };
                    });
                }
                else {
                    getPopup(p).setVisible(false);
                    getPopup(p).dispose();
                }
            }
        });

        if (p.getVisibleByDefault())
            layersToolbar.add(ts, 0);
        else
            layersToolbar.add(ts, layersToolbar.getComponentCount());

        layersToolbar.invalidate();
        layersToolbar.validate();
        if (p.getVisibleByDefault()) {
            ts.doClick();
        }
    }

    @Override
    public JComponent getVisualization(final IMraLogGroup source, double timestep) {
        this.source = source;
        this.index = source.getLsfIndex();
        
        replayBus.register(this);
        r2d = new StateRenderer2D();
        layersToolbar = new JToolBar("Layers", JToolBar.VERTICAL);
        for (Entry<String, Class<? extends LogReplayLayer>> entry : PluginsRepository.getReplayLayers().entrySet()) {
            try {
                LogReplayLayer layer = PluginsRepository.getPlugin(entry.getKey(), LogReplayLayer.class);
                layers.add(layer);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Entry<String, Class<? extends LogReplayPanel>> entry : PluginsRepository.listExtensions(
                LogReplayPanel.class).entrySet()) {
            try {
                LogReplayPanel p = PluginsRepository.getPlugin(entry.getKey(), LogReplayPanel.class);
                panels.add(p);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        timeline = new MRALogReplayTimeline(this);
        
        Thread t = new Thread("Starting replay") {
            @Override
            public void run() {
                MissionType mission = LogUtils.generateMission(source);
                r2d.setMapGroup(MapGroup.getMapGroupInstance(mission));

                ExecutorService executorService = Executors.newCachedThreadPool();
                
                for (final LogReplayLayer l : layers) {
                    try {
                        if (l.canBeApplied(source, Context.MRA)) {
                            executorService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    bootstrap(l);
                                }
                            }); 
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                for (final LogReplayPanel p : panels) {
                    try {
                        if (p.canBeApplied(source, pt.lsts.neptus.mra.replay.LogReplayPanel.Context.MRA)) {
                            executorService.execute(new Runnable() {
                                @Override
                                public void run() {
                                    bootstrapPanel(p);
                                }
                            });
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        };
        t.setDaemon(true);
        t.start();

        JPanel p = new JPanel(new BorderLayout());
        p.add(r2d, BorderLayout.CENTER);
        p.add(layersToolbar, BorderLayout.WEST);
        p.add(timeline, BorderLayout.NORTH);
        return p;

    }

    @Override
    public void addLogMarker(LogMarker marker) {
        for (LogReplayLayer l : layers) {
            if (l instanceof LogMarkerListener)
                ((LogMarkerListener) l).addLogMarker(marker);
        }
    }

    @Override
    public void removeLogMarker(LogMarker marker) {
        for (LogReplayLayer l : layers) {
            if (l instanceof LogMarkerListener)
                ((LogMarkerListener) l).removeLogMarker(marker);
        }
    }

    @Override
    public void goToMarker(LogMarker marker) {
        for (LogReplayLayer l : layers) {
            if (l instanceof LogMarkerListener)
                ((LogMarkerListener) l).goToMarker(marker);
        }
    }

    @Override
    public void onCleanup() {
        super.onCleanup();
        if (timeline != null)
            timeline.cleanup();
        for (JDialog d : popups.values()) {
            d.setVisible(false);
            d.dispose();
        }

        for (LogReplayLayer l : layers) {
            l.cleanup();
            try {
                replayBus.unregister(l);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
        for (LogReplayPanel p: panels) {
            p.cleanup();
            try {
                replayBus.unregister(p);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }        
    }
    
    
    @Override
    public void missionReplaced(MissionType mission) {
        missionUpdated(mission);
    }
    
    
    @Override
    public void missionUpdated(MissionType mission) {
        if (r2d != null) {
            r2d.setMapGroup(MapGroup.getMapGroupInstance(mission));
        }
    }

    /**
     * @return the index
     */
    public LsfIndex getIndex() {
        return index;
    }
    
    @Override
    public void onHide() {
        timeline.pause();
        for (JDialog d : popups.values()) {
            d.setVisible(false);
            d.dispose();
        }
        popups.clear();
    }

    /**
     * @return the source
     */
    public IMraLogGroup getSource() {
        return source;
    }

    /**
     * @return the replayBus
     */
    public EventBus getReplayBus() {
        return replayBus;
    }

}
