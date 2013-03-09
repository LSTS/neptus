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
 * Nov 1, 2012
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Vector;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.MissionChangeListener;
import pt.up.fe.dceg.neptus.imc.PlanControlState;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.PathProvider;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 *
 */
@PluginDescription(name="Multi-Vehicle plan overlay")
public class MultiVehiclePlanOverlay extends SimpleSubPanel implements Renderer2DPainter, MissionChangeListener {

    private static final long serialVersionUID = 1L;
    protected LinkedHashMap<String, String> vehicleToPlanIds = new LinkedHashMap<>();
    protected LinkedHashMap<String, Vector<LocationType>> planToLocations = new LinkedHashMap<>();    
    protected LinkedHashMap<String, Vector<Point2D>> planPoints = new LinkedHashMap<>();
    protected double lastZoom;
    protected LocationType lastCenter;
    protected double lastRotation = 0;
    protected Vector<StateRenderer2D> renderers = new Vector<>();

    /**
     * @param console
     */
    public MultiVehiclePlanOverlay(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void initSubPanel() {
      
    }

    @Override
    public void missionReplaced(MissionType mission) {
        planToLocations.clear();
        planPoints.clear();
    }

    @Override
    public void missionUpdated(MissionType mission) {
        planToLocations.clear();
        planPoints.clear();
    }

    @Subscribe
    public void consume(PlanControlState pcstate) {
        ImcSystem system = ImcSystemsHolder.lookupSystem(pcstate.getSrc());
        if (system == null)
            return;
        String sysId = system.getName();
        switch (pcstate.getState()) {
            case INITIALIZING:
            case EXECUTING:
                vehicleToPlanIds.put(sysId, pcstate.getPlanId());
                break;
            case BLOCKED:
            case READY:
                vehicleToPlanIds.remove(sysId);
                break;
        }
    }

    protected static final Vector<LocationType> planPath(PlanType plan) {
        Vector<LocationType> locations = new Vector<>();
        LinkedList<Maneuver> mans = plan.getGraph().getGraphAsManeuversList();

        for (Maneuver man : mans) {

            if (!(man instanceof LocatedManeuver))
                continue;

            LocationType destTo = ((LocatedManeuver) man).getManeuverLocation();                         
            if (man instanceof PathProvider)
                locations.addAll(((PathProvider) man).getPathLocations());
            else
                locations.add(destTo);
        }        
        return locations;
    }


    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!renderers.contains(renderer)) {
            renderers.add(renderer);

            renderer.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    planPoints.clear();
                }
            });
        }

        if (renderer.getZoom() != lastZoom || !renderer.getCenter().equals(lastCenter) || renderer.getRotation() != lastRotation) {
            planPoints.clear();
        }

        for (Entry<String, String> entry : vehicleToPlanIds.entrySet()) {
            VehicleType vt = VehiclesHolder.getVehicleById(entry.getKey());
            Color c = Color.GRAY;

            if (vt != null)
                c = vt.getIconColor();

            if (!planToLocations.containsKey(entry.getValue())) {
                PlanType pt = getConsole().getMission().getIndividualPlansList().get(entry.getValue());
                if (pt != null)
                    planToLocations.put(pt.getId(), planPath(pt));

            }

            Vector<Point2D> points = planPoints.get(entry.getValue());

            if (points == null) {
                points = new Vector<>();

                Vector<LocationType> planLocs = planToLocations.get(entry.getValue());
                if (planLocs == null)
                    continue;
                for (LocationType l : planLocs)
                    points.add(renderer.getScreenPosition(l));
                lastZoom = renderer.getZoom();
                lastCenter = new LocationType(renderer.getCenter());
            }
            planPoints.put(entry.getValue(), points);

            Point2D prev = null;
            for (Point2D cur : planPoints.get(entry.getValue())) {
                g.setColor(c.darker());
                if (prev != null)
                    g.draw(new Line2D.Double(prev, cur));
                prev = cur;
                g.setColor(c.brighter());
                g.fill(new Ellipse2D.Double(cur.getX()-2, cur.getY()-2, 4, 4));
            }            
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}