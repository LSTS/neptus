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
 * Author: José Pinto
 * Nov 21, 2012
 */
package pt.lsts.neptus.mp.preview;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;

import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.preview.payloads.PayloadFactory;
import pt.lsts.neptus.mp.preview.payloads.PayloadFingerprint;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class PlanSimulationOverlay implements Renderer2DPainter {

    protected LocationType ref;
    protected Vector<SystemPositionAndAttitude> states = new Vector<>();
    protected Vector<Color> colors = new Vector<>();
    protected Vector<SimulationState> simStates = new Vector<>();
    protected Vector<LinkedHashMap<Area, Color>> swaths = new Vector<>();
    
    public boolean simulationFinished = false;
    public static double bottomDepth = 10;
    private HashSet<PlanSimulationListener> listeners = new HashSet<>();
    private double totalTime = 0;
    protected LinkedHashMap<String, Collection<PayloadFingerprint>> payloads;

    protected PlanType plan;
    
    public PlanSimulationOverlay(PlanType plan, final double usedBattHours, final double remainingBattHours, SystemPositionAndAttitude start) {
        this.ref = new LocationType(plan.getMissionType().getHomeRef());
        this.plan = plan;
        final SimulationEngine engine = new SimulationEngine(plan);
        payloads = PayloadFactory.getPayloads(plan);
        if (start == null) {
            start = new SystemPositionAndAttitude(plan.getMissionType().getHomeRef(), 0, 0, 0);
            Vector<LocatedManeuver> manLocs = PlanUtil.getLocationsAsSequence(plan);
            
            if (!manLocs.isEmpty()) 
                start.setPosition(manLocs.firstElement().getStartLocation());            
        }
        engine.setState(start);

        Thread t = new Thread("Plan simulation overlay") {
            public void run() {

                double ellapsedTime = 0;
                double lastPoint = 0;
                ColorMap cmap = ColorMapFactory.createRedYellowGreenColorMap();
                double totalBattTime = (usedBattHours + remainingBattHours) * 3600;
                double usedBatt = usedBattHours * 3600;

                while (!engine.isFinished() && ellapsedTime < 10 * 3600) {
                    ellapsedTime += engine.getTimestep();
                    engine.simulationStep();
                    if (ellapsedTime - lastPoint >= 1) {
                        Color c = cmap.getColor(1 - ((ellapsedTime + usedBatt) / totalBattTime));
                        SystemPositionAndAttitude state = engine.getState();
                        addPoint(state, c,
                                new SimulationState(engine.getManId(), engine.getCurPreview() == null ? null : engine
                                        .getCurPreview().getState(), state, ellapsedTime));
                        
                        lastPoint = ellapsedTime;
                    }
                    Thread.yield();
                }
                simulationFinished = true;
                totalTime = ellapsedTime;
                for (PlanSimulationListener l : listeners)
                    l.simulationFinished(PlanSimulationOverlay.this);
            };
        };
        t.setDaemon(true);
        t.start();
    }
    
    public void addListener(PlanSimulationListener l) {
        listeners.add(l);
    }
    
    public void removeListener(PlanSimulationListener l) {
        listeners.remove(l);
    }
    
    protected void addPoint(double northing, double easting, Color color) {
        LocationType loc = new LocationType(ref);        
        loc.translatePosition(northing, easting, 0);
        addPoint(loc, color);         
    }

    protected void addPoint(LocationType loc, Color color) {
        addPoint(loc, color, null);
    }    

    protected void addPoint(LocationType loc) {
        if (colors.size() > 0)
            addPoint(loc, colors.lastElement());
        else
            addPoint(loc, Color.white);       
    }

    public void addPoint(SystemPositionAndAttitude state, Color color, SimulationState simState) {
        states.add(state);
        colors.add(color);
        simStates.add(simState);
        
        String man = simState.getCurrentManeuver();
        if (man == null)
            return;
        LinkedHashMap<Area, Color> swath = new LinkedHashMap<>();
        for (PayloadFingerprint pf : payloads.get(man)) {
            double altitude = SimulationEngine.simBathym.getSimulatedDepth(state.getPosition());
           if (state.getDepth() == -1)
               new Exception().printStackTrace();
            altitude -= state.getDepth();
            state.setAltitude(altitude);
            Area a = pf.getFingerprint(state);
            swath.put(a, pf.getColor());
        }
        synchronized (swaths) {
            swaths.add(swath);    
        }        
    }

    public void addPoint(LocationType loc, Color color, SimulationState simState) {
        addPoint(new SystemPositionAndAttitude(loc, 0,0,0), color, simState);
    }

    /**
     * @return the simStates
     */
    public final Vector<SimulationState> getSimStates() {
        return simStates;
    }

    /**
     * @return the states
     */
    public final Vector<SystemPositionAndAttitude> getStates() {
        return states;
    }
    
    public Pair<Integer, SimulationState> nearestState(SystemPositionAndAttitude state, double minDistThreshold) {
        int nearest = 0;
        double nearestDistance = Double.MAX_VALUE;
        
        for (int i = 0; i < simStates.size(); i++) {
            LocationType center = states.get(i).getPosition();
            double dist = center.getHorizontalDistanceInMeters(state.getPosition()) + 10 * Math.abs(state.getYaw() - states.get(i).getYaw());
            if (dist < nearestDistance) {
                nearestDistance = dist;
                nearest = i;
            }
        }

        if (minDistThreshold == 0 || nearestDistance < minDistThreshold)
            return new Pair<Integer, SimulationState>(nearest, simStates.get(nearest));
        else
            return null;
    }

    public LinkedHashMap<String, String> statistics(SystemPositionAndAttitude state) {
        LinkedHashMap<String, String> stats = new LinkedHashMap<>();  
        
        if (!simStates.isEmpty()) {
            SimulationState nearest = simStates.get(0);

            if (state != null)
                nearest = nearestState(state, Integer.MAX_VALUE).second();

            int pos = simStates.indexOf(nearest);

            int time = states.size() - pos;
            String timeUnits = I18n.text("seconds");
            if (time > 300) {
                timeUnits = I18n.text("minutes");
                time = time / 60;
            }
            
            stats.put(I18n.text("Completion status"), I18n.textf("%percent % complete", (pos * 1000 / simStates.size())/10.0));
            stats.put(I18n.text("Time until completion"), time + " " + timeUnits);
            
        }

        return stats;
    }

    /**
     * @return the totalTime
     */
    public double getTotalTime() {
        return totalTime;
    }

    /**
     * @param totalTime the totalTime to set
     */
    public void setTotalTime(double totalTime) {
        this.totalTime = totalTime;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        Point2D center = renderer.getScreenPosition(ref);
        g.setColor(Color.white);

        for (int i = 0; i < states.size(); i++) {
            Graphics2D g2 = (Graphics2D)g.create();
            Point2D pt = renderer.getScreenPosition(states.get(i).getPosition());
            g2.translate(pt.getX(), pt.getY());
            g2.scale(renderer.getZoom(), renderer.getZoom());
            g2.rotate(-renderer.getRotation()+states.get(i).getYaw());
            try {
                for (Entry<Area, Color> swath : swaths.get(i).entrySet()) {
                    g2.setColor(swath.getValue());
                    g2.fill(swath.getKey());
                }   
            }
            catch (ArrayIndexOutOfBoundsException e) {
                // still being generated...
            }
        }
        g.translate(center.getX(), center.getY());
        g.rotate(-renderer.getRotation());
        for (int i = 0; i < states.size(); i++) {
            g.setColor(colors.get(i));
            double zoom = renderer.getZoom();
            double[] neOffsets = states.get(i).getPosition().getOffsetFrom(ref);
            Graphics2D g3 = (Graphics2D) g.create();
            g3.translate((int)(neOffsets[1] * zoom), -(int)(neOffsets[0] * zoom));
            g3.rotate(states.get(i).getYaw());
            g3.fillRect(-4, -1, 8, 2);
            g3.dispose();
        }
    }
}
