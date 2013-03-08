/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Nov 21, 2012
 * $Id:: PlanSimulationOverlay.java 9880 2013-02-07 15:23:52Z jqcorreia         $:
 */
package pt.up.fe.dceg.neptus.mp.preview;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Vector;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class PlanSimulationOverlay implements Renderer2DPainter {

    protected LocationType ref;
    protected Vector<SystemPositionAndAttitude> states = new Vector<>();
    
    protected Vector<Color> colors = new Vector<>();
    protected Vector<SimulationState> simStates = new Vector<>();
    public boolean simulationFinished = false;
    public double bottomDepth = 10;
    
    public PlanSimulationOverlay(PlanType plan, final double usedBattHours, final double remainingBattHours, SystemPositionAndAttitude start) {
        this(plan.getMissionType().getHomeRef());
        final SimulationEngine engine = new SimulationEngine(plan);
        
        if (start != null)
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
                    if (ellapsedTime - lastPoint > 1) {
                        Color c = cmap.getColor(1 - ((ellapsedTime + usedBatt) / totalBattTime));
                        addPoint(engine.getState(), c, new SimulationState(
                                engine.getManId(), engine.getCurPreview() == null? null : engine.getCurPreview().getState(), engine.getState()));
                        lastPoint = ellapsedTime;
                    }
                    Thread.yield();
                }
                simulationFinished = true;
            };
        };
        t.setDaemon(true);
        t.start();
    }
    
    

    public PlanSimulationOverlay(LocationType ref) {
        this.ref = new LocationType(ref);
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



    public SimulationState nearestState(SystemPositionAndAttitude state, double minDistThreshold) {
        int nearest = 0;
        double nearestDistance = Double.MAX_VALUE;
        
        for (int i = 0; i < simStates.size(); i++) {
            LocationType center = states.get(i).getPosition();
            double dist = center.getHorizontalDistanceInMeters(state.getPosition()) + 2 * Math.abs(state.getYaw() - states.get(i).getYaw());
            if (dist < nearestDistance) {
                nearestDistance = dist;
                nearest = i;
            }
        }

        if (nearestDistance < minDistThreshold)
            return simStates.get(nearest);
        else
            return null;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        Point2D center = renderer.getScreenPosition(ref);
        g.setColor(Color.white);
        int time = states.size();
        String timeUnits = "seconds";
        if (time > 300) {
            timeUnits = "minutes";
            time = time / 60;
        }

        g.drawString("Plan takes aproximately "+time+" "+timeUnits, 10, renderer.getHeight()-40);
        g.translate(center.getX(), center.getY());
        g.rotate(-renderer.getRotation());
        for (int i = 0; i < states.size(); i++) {
            g.setColor(colors.get(i));
            double zoom = renderer.getZoom();
            double[] neOffsets = states.get(i).getPosition().getOffsetFrom(ref); 
            g.fillRect((int)(neOffsets[1]*zoom)-1, (int)-(neOffsets[0]*zoom)-1, 2, 2);
        }
    }
}
