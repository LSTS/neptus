/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Date;

import pt.lsts.neptus.endurance.Plan;
import pt.lsts.neptus.endurance.Waypoint;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * @author zp
 *
 */
public class SoiPlanRenderer implements Renderer2DPainter {

    private Plan plan;
    private Color color;

    public SoiPlanRenderer() {
        plan = null;
        color = Color.white;
    }

    @Override
    public void paint(Graphics2D g0, StateRenderer2D renderer) {
        if (plan != null && !plan.waypoints().isEmpty()) {
            LocationType lastLoc = null;
            Graphics2D g = (Graphics2D) g0.create();
            for (Waypoint wpt : plan.waypoints()) {
                LocationType loc = new LocationType(wpt.getLatitude(), wpt.getLongitude());
                Point2D pt2d = renderer.getScreenPosition(loc);

                if (lastLoc != null) {
                    Point2D lastPT = renderer.getScreenPosition(lastLoc);
                    g.setColor(color);
                    g.draw(new Line2D.Double(lastPT.getX(), lastPT.getY(), pt2d.getX(), pt2d.getY()));
                }
                lastLoc = loc;

                if (wpt.getArrivalTime().before(new Date())) {
                    g.setColor(Color.gray);
                    g.fill(new Ellipse2D.Double(pt2d.getX() - 4, pt2d.getY() - 4, 8, 8));
                }
                else {
                    String minsToEta = "ETA: ?";
                    if (wpt.getArrivalTime() != null)
                        minsToEta = "ETA: " + DateTimeUtil
                                .milliSecondsToFormatedString(wpt.getArrivalTime().getTime() - System.currentTimeMillis());

                    g.setColor(Color.green.darker());
                    g.fill(new Ellipse2D.Double(pt2d.getX() - 4, pt2d.getY() - 4, 8, 8));
                    g.drawString(minsToEta, (int) pt2d.getX() + 6, (int) pt2d.getY() - 3);
                }
                g.setColor(color);
                g.draw(new Ellipse2D.Double(pt2d.getX() - 4, pt2d.getY() - 4, 8, 8));
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

}
