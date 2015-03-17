/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Nov 1, 2012
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;

import pt.lsts.imc.PlanControlState;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Multi-Vehicle plan overlay")
public class MultiVehiclePlanOverlay extends ConsolePanel implements Renderer2DPainter, MissionChangeListener {

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

        if (pcstate.getPlanId().isEmpty())
            return;

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

        if (renderer.getZoom() != lastZoom || !renderer.getCenter().equals(lastCenter)
                || renderer.getRotation() != lastRotation) {
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
                    planToLocations.put(pt.getId(), pt.planPath());

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
                g.fill(new Ellipse2D.Double(cur.getX() - 2, cur.getY() - 2, 4, 4));
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
}