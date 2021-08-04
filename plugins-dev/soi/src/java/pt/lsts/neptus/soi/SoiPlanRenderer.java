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
 * Jan 22, 2018
 */
package pt.lsts.neptus.soi;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Date;

import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.endurance.Plan;
import pt.lsts.neptus.endurance.Waypoint;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class SoiPlanRenderer implements Renderer2DPainter {

    private Plan plan;
    private Color color;
    private String vehicle;

    public SoiPlanRenderer() {
        plan = null;
        color = Color.white;
        vehicle = null;
    }

    @Override
    public void paint(Graphics2D g0, StateRenderer2D renderer) {
        if (plan != null && !plan.waypoints().isEmpty()) {
            LocationType lastLoc = null;
            if (getVehicle() != null && !plan.waypoints().isEmpty() && plan.waypoint(0).getArrivalTime().after(new Date())) {
                lastLoc = ImcSystemsHolder.getSystemWithName(getVehicle()).getLocation();
            }
            Graphics2D g = (Graphics2D) g0.create();
            for (Waypoint wpt : plan.waypoints()) {
                LocationType loc = new LocationType(wpt.getLatitude(), wpt.getLongitude());
                Point2D pt2d = renderer.getScreenPosition(loc);

                if (lastLoc != null) {
                    Point2D lastPT = renderer.getScreenPosition(lastLoc);
                    g.setColor(color.darker());
                    g.setStroke(new BasicStroke(2.5f));
                    g.draw(new Line2D.Double(lastPT.getX(), lastPT.getY(), pt2d.getX(), pt2d.getY()));
                }
                lastLoc = loc;
            }
            
            for (Waypoint wpt : plan.waypoints()) {
                LocationType loc = new LocationType(wpt.getLatitude(), wpt.getLongitude());
                Point2D pt2d = renderer.getScreenPosition(loc);
                wpt.paint(g, wpt, pt2d, color, 8, false);
            }
            g.dispose();
        }
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * @return the vehicle
     */
    public String getVehicle() {
        return vehicle;
    }

    /**
     * @param vehicle the vehicle to set
     */
    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

}
