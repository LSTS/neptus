/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jan 29, 2014
 */
package pt.lsts.neptus.mra.replay;

import java.awt.BorderLayout;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.Executors;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.plots.LogMarkerListener;
import pt.lsts.neptus.mra.replay.LogReplayLayer.Context;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.renderer2d.StateRenderer2D;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 *
 */
@PluginDescription(name="Mission Replay")
public class MRALogReplay extends SimpleMRAVisualization implements LogMarkerListener {

    private static final long serialVersionUID = 1L;
    private LsfIndex index;
    private IMraLogGroup source;
    private Vector<LogReplayLayer> layers = new Vector<>();
    private final AsyncEventBus replayBus = new AsyncEventBus("Replay Event bus", Executors.newFixedThreadPool(2));
    private StateRenderer2D r2d;
    private JToolBar layersToolbar;
    private MRALogReplayTimeline timeline;
    private LinkedHashMap<String, Vector<LogReplayLayer>> observersTable = new LinkedHashMap<>();

    public MRALogReplay(MRAPanel panel) {
        super(panel);
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
    public synchronized void on(IMCMessage m) {
        if (observersTable.containsKey(m.getAbbrev())) {
            for (LogReplayLayer l : observersTable.get(m.getAbbrev()))
                l.onMessage(m);
            r2d.repaint();
        }
        
        if (m.getAbbrev().equals("EstimatedState")) {
            SystemPositionAndAttitude state = IMCUtils.parseState(m);
            r2d.vehicleStateChanged(m.getSourceName(), state);
        }
    }
    
    @Override
    public JComponent getVisualization(final IMraLogGroup source, double timestep) {
        this.source = source;
        this.index = source.getLsfIndex();
        timeline = new MRALogReplayTimeline(this);
        if (index.containsMessagesOfType("EstimatedState")) {
            r2d.setCenter(IMCUtils.getLocation(index.nextMessageOfType(EstimatedState.class, 0)));
        }

        Thread t = new Thread("Starting replay") {
            public void run() {
                for (LogReplayLayer l : layers) {
                    try {
                        if (l.canBeApplied(source, Context.MRA)) {
                            l.parse(source);
                            replayBus.register(l);
                            r2d.addPostRenderPainter(l, l.getName());
                            String[] msgs = l.getObservedMessages();
                            if (msgs != null) {
                                for (String m : msgs) {
                                    if (!observersTable.containsKey(m))
                                        observersTable.put(m, new Vector<LogReplayLayer>());
                                    observersTable.get(m).add(l);
                                }
                            }
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
                ((LogMarkerListener)l).addLogMarker(marker);
        }
    }

    @Override
    public void removeLogMarker(LogMarker marker) {
        for (LogReplayLayer l : layers) {
            if (l instanceof LogMarkerListener)
                ((LogMarkerListener)l).removeLogMarker(marker);
        }
    }

    @Override
    public void GotoMarker(LogMarker marker) {
        for (LogReplayLayer l : layers) {
            if (l instanceof LogMarkerListener)
                ((LogMarkerListener)l).GotoMarker(marker);
        }
    }

    @Override
    public void onCleanup() {
        super.onCleanup();
        for (LogReplayLayer l : layers) {
            l.cleanup();
            replayBus.unregister(l);
        }

    }

    /**
     * @return the index
     */
    public LsfIndex getIndex() {
        return index;
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
