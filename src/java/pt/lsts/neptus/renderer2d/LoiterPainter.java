/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author:
 * 20??/??/??
 */
package pt.lsts.neptus.renderer2d;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.maneuvers.Loiter;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.util.GuiUtils;

public class LoiterPainter implements Renderer2DPainter {

    private Loiter loiterManeuver = null;

    public LoiterPainter() {
        setLoiterManeuver(new Loiter());
    }

    public LoiterPainter(Loiter loiterManeuver) {
        setLoiterManeuver(loiterManeuver);
    }

    public static void paint(Loiter loiterManeuver, Graphics2D g, double zoom, double rotation) {
        //               x marks the spot...
        g.drawLine(-4, -4, 4, 4);
        g.drawLine(-4, 4, 4, -4);

        double bearing = -loiterManeuver.getBearing() - rotation;
        double radius = loiterManeuver.getRadius() * zoom;
        double length = loiterManeuver.getLength() * zoom;

        // display bearing
        g.rotate(bearing);
        //g.draw(new Line2D.Double(0, 0, 0, -radius));


        boolean isClockwise = loiterManeuver.getDirection().equalsIgnoreCase("Clockwise") ? true : false;

        if (loiterManeuver.getLoiterType().equalsIgnoreCase("circular")) {
            double rt = loiterManeuver.getRadiusTolerance() * zoom;

            g.setColor(new Color(255, 255, 255, 100));
            Area outer = new Area(new Ellipse2D.Double(-radius - rt, -radius - rt, (radius + rt) * 2, (radius + rt) * 2));
            Area inner = new Area(new Ellipse2D.Double(-radius + rt, -radius + rt, (radius - rt) * 2, (radius - rt) * 2));

            outer.subtract(inner);

            g.fill(outer);
            g.setColor(Color.RED);

            g.draw(new Ellipse2D.Double(-radius,-radius,radius*2, radius*2));

            g.translate(0, -radius);
            if (isClockwise) {
                g.drawLine(-5, 5, 0, 0);
                g.drawLine(-5, -5, 0, 0);
            }
            else {
                g.drawLine(5, 5, 0, 0);
                g.drawLine(5, -5, 0, 0);
            }
            return;
        }

        if (loiterManeuver.getLoiterType().equalsIgnoreCase("racetrack")) {

            double rt = loiterManeuver.getRadiusTolerance() * zoom;

            g.setColor(new Color(255,255,255,100));

            Area outer = new Area(new Rectangle2D.Double(-length/2, -radius-rt, length, (radius+rt)*2));
            outer.add(new Area(new Ellipse2D.Double(-radius-rt-length/2, -radius-rt, (radius+rt)*2, (radius+rt)*2)));
            outer.add(new Area(new Ellipse2D.Double(-radius-rt+length/2, -radius-rt, (radius+rt)*2, (radius+rt)*2)));

            Area inner = new Area(new Rectangle2D.Double(-length/2, -radius+rt, length, (radius-rt)*2));
            inner.add(new Area(new Ellipse2D.Double(-radius+rt-length/2, -radius+rt, (radius-rt)*2, (radius-rt)*2)));
            inner.add(new Area(new Ellipse2D.Double(-radius+rt+length/2, -radius+rt, (radius-rt)*2, (radius-rt)*2)));

            outer.subtract(inner);

            g.fill(outer);
            g.setColor(Color.RED);

            Area a = new Area();
            a.add(new Area(new Ellipse2D.Double(-radius-length/2,-radius,radius*2, radius*2)));
            a.add(new Area(new Ellipse2D.Double(-radius+length/2,-radius,radius*2, radius*2)));
            a.add(new Area(new Rectangle2D.Double(-length/2, -radius, length, radius*2)));

            g.draw(a);

            g.translate(0, -radius);

            if (isClockwise) {
                g.drawLine(-5, 5, 0, 0);
                g.drawLine(-5, -5, 0, 0);
            }
            else {
                g.drawLine(5, 5, 0, 0);
                g.drawLine(5, -5, 0, 0);
            }
            return;
        }

        if (loiterManeuver.getLoiterType().equalsIgnoreCase("Figure 8")) {

            double rt = loiterManeuver.getRadiusTolerance() * zoom;

            g.setColor(new Color(255,255,255,100));

            Area outer = new Area();

            outer.add(new Area(new Ellipse2D.Double(-radius-rt-length/2, -radius-rt, (radius+rt)*2, (radius+rt)*2)));
            outer.add(new Area(new Ellipse2D.Double(-radius-rt+length/2, -radius-rt, (radius+rt)*2, (radius+rt)*2)));

            outer.subtract(new Area(new Rectangle2D.Double(-length/2, -radius-rt, length, (radius+rt)*2)));

            Area inner = new Area(new Rectangle2D.Double(-length/2, -radius+rt, length, (radius-rt)*2));
            inner.add(new Area(new Ellipse2D.Double(-radius+rt-length/2, -radius+rt, (radius-rt)*2, (radius-rt)*2)));
            inner.add(new Area(new Ellipse2D.Double(-radius+rt+length/2, -radius+rt, (radius-rt)*2, (radius-rt)*2)));

            outer.subtract(inner);



            GeneralPath p = new GeneralPath();
            p.moveTo(-length/2, -radius-rt);
            p.lineTo(length/2, radius-rt);
            p.lineTo(length/2, radius+rt);
            p.lineTo(-length/2, -radius+rt);
            p.closePath();

            outer.add(new Area(p));

            p = new GeneralPath();
            p.moveTo(-length/2, radius-rt);
            p.lineTo(length/2, -radius-rt);
            p.lineTo(length/2, -radius+rt);
            p.lineTo(-length/2, radius+rt);
            p.closePath();

            outer.add(new Area(p));

            g.fill(outer);

            g.setColor(Color.RED);

            Area a = new Area();
            a.add(new Area(new Ellipse2D.Double(-radius-length/2,-radius,radius*2, radius*2)));
            a.add(new Area(new Ellipse2D.Double(-radius+length/2,-radius,radius*2, radius*2)));
            a.subtract(new Area(new Rectangle2D.Double(-length/2, -radius, length, radius*2)));

            p = new GeneralPath();
            p.moveTo(-length/2-1, -radius);
            p.lineTo(length/2+1, radius);
            p.lineTo(length/2+1, -radius);
            p.lineTo(-length/2-1, radius);
            p.closePath();
            a.add(new Area(p));

            g.draw(a);

            g.translate(0, -radius);
            if (isClockwise) {
                g.drawLine(-5, 5, 0, 0);
                g.drawLine(-5, -5, 0, 0);
            }
            else {
                g.drawLine(5, 5, 0, 0);
                g.drawLine(5, -5, 0, 0);
            }
            return;
        }

        if (loiterManeuver.getLoiterType().equalsIgnoreCase("Hover")) {
            g.setColor(new Color(255,255,255,100));
            g.fill(new Ellipse2D.Double(-radius,-radius,radius*2, radius*2));
            return;
        }

        g.setColor(new Color(255,255,255,100));
        g.fill(new Ellipse2D.Double(-radius,-radius,radius*2, radius*2));
        g.rotate(-bearing);
        g.setColor(Color.RED);
        g.drawString("?", 5, 10);

    }

    public void paint(Graphics2D g, StateRenderer2D renderer) {
        //g.setTransform(new AffineTransform());
        //Point2D pt = renderer.getScreenPosition(loiterManeuver.getLocation());
        //g.translate(pt.getX(), pt.getY());
        paint(loiterManeuver, g, renderer.getZoom(), renderer.getRotation());
    }

    public void setLoiterManeuver(Loiter loiterManeuver) {
        this.loiterManeuver = loiterManeuver;
    }

    private static MapLegend legend = new MapLegend();

    public static Image previewLoiter(Loiter loiter, int width, int height) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        NeptusLog.pub().info("<###>width="+width);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        g.translate(width/2.0, height/2.0);
        paint(loiter, g, 1.0, 0.0);

        g.setTransform(new AffineTransform());
        legend.paint(g, width, height, true);

        return bi;
    }


    public static void main(String[] args) {
        StateRenderer2D r2d = new StateRenderer2D(MapGroup.getNewInstance(new CoordinateSystem()));
        Loiter loiter = new Loiter();
        LocationType lt = new LocationType();
        lt.setOffsetEast(150);
        loiter.getManeuverLocation().setLocation(lt);
        loiter.setLength(50);
        loiter.setLoiterType("Circular");
        loiter.setBearing(Math.toRadians(45));
        loiter.setDirection("Clockwise");
        r2d.addPostRenderPainter(new LoiterPainter(loiter), "Loiter Painter");

        GuiUtils.testFrame(r2d, "Testing loiter painter...");

    }

}
