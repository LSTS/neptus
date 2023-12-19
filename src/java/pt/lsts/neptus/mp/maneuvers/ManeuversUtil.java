/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 11/09/2011
 */
package pt.lsts.neptus.mp.maneuvers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.plugins.PluginProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.AngleUtils;

/**
 * @author pdias
 *
 */
public class ManeuversUtil {
    
    protected static final int X = 0, Y = 1, Z = 2, T = 3;

    public static final Color noEditBoxColor = new Color(255, 160, 0, 100);
    public static final Color editBoxColor = new Color(255, 125, 255, 200);

    private static GeneralPath arrow1 = new GeneralPath();
    private static GeneralPath arrow2 = new GeneralPath();
    static {
        arrow1.moveTo(0, -5);
        arrow1.lineTo(-3.5, -11);
        arrow1.lineTo(3.5, -11);
        arrow1.closePath();

        arrow2.moveTo(0, -6);
        arrow2.lineTo(-4.5, -12);
        arrow2.lineTo(4.5, -12);
        arrow2.closePath();
    }

    private ManeuversUtil() {
    }
    
    /**
     * @param width
     * @param hstep
     * @param alternationPercent
     * @param curvOff
     * @param squareCurve
     * @param bearingRad
     * @return
     */
    public static Vector<double[]> calcRIPatternPoints(double width, double hstep,
            double alternationPercent, double curvOff, boolean squareCurve, double bearingRad) {
        
        Vector<double[]> newPoints = new Vector<double[]>();
        
        double length = width;
        double[] pointBaseB = {-length/2., -width/2., 0, -1};
        double[] res = AngleUtils.rotate(bearingRad, pointBaseB[X], pointBaseB[Y], false);
        double[] pointBase1 = new double[] {res[X], res[Y], 0, -1};
        res = AngleUtils.rotate(bearingRad+Math.toRadians(-60), pointBaseB[X], pointBaseB[Y], false);
        double[] pointBase2 = {res[X], res[Y], 0, -1};
        res = AngleUtils.rotate(bearingRad+Math.toRadians(-120), pointBaseB[X], pointBaseB[Y], false);
        double[] pointBase3 = {res[X], res[Y], 0, -1};
        
        Vector<double[]> points1 = calcRowsPoints(width, width, hstep, 2-alternationPercent, curvOff,
                squareCurve, bearingRad, 0);
        for (double[] pt : points1) {
            pt[X] += pointBase1[X];
            pt[Y] += pointBase1[Y];
        }

        Vector<double[]> points2 = calcRowsPoints(width, width, hstep, 2-alternationPercent, curvOff,
                squareCurve, bearingRad + Math.toRadians(-60), 0);
        for (double[] pt : points2) {
            pt[X] += pointBase2[X];
            pt[Y] += pointBase2[Y];
        }

        Vector<double[]> points3 = calcRowsPoints(width, width, hstep, 2-alternationPercent, curvOff,
                squareCurve, bearingRad + Math.toRadians(-120), 0);
        for (double[] pt : points3) {
            pt[X] += pointBase3[X];
            pt[Y] += pointBase3[Y];
        }

        newPoints.addAll(points1);
        newPoints.addAll(points2);
        newPoints.addAll(points3);
        return newPoints;
    }
    
    
    /**
     * @param width
     * @param hstep
     * @param curvOff
     * @param squareCurve
     * @param bearingRad
     * @return
     */
    public static Vector<double[]> calcCrossHatchPatternPoints(double width, double hstep,
            double curvOff, boolean squareCurve, double bearingRad) {
        
        Vector<double[]> newPoints = new Vector<double[]>();
        
        double length = width;
        double[] pointBase1 = {-length/2., -width/2., 0, -1};
        double[] pointBase2 = {-length/2., width/2., 0, -1};
        double[] res = AngleUtils.rotate(bearingRad, pointBase1[X], pointBase1[Y], false);
        pointBase1 = new double[] {res[X], res[Y], 0, -1};
        res = AngleUtils.rotate(bearingRad, pointBase2[X], pointBase2[Y], false);
        pointBase2 = new double[] {res[X], res[Y], 0, -1};

        Vector<double[]> points1 = calcRowsPoints(width, width, hstep, 1, curvOff,
                squareCurve, bearingRad, 0);
        for (double[] pt : points1) {
            pt[X] += pointBase1[X];
            pt[Y] += pointBase1[Y];
        }

        Vector<double[]> points2 = calcRowsPoints(width, width, hstep, 1, curvOff,
                squareCurve, bearingRad + Math.toRadians(-90), 0);
        for (double[] pt : points2) {
            pt[X] += pointBase2[X];
            pt[Y] += pointBase2[Y];
        }

        newPoints.addAll(points1);
        newPoints.addAll(points2);
        return newPoints;
    }

    
    /**
     * @param width
     * @param length
     * @param hstep
     * @param alternationPercent
     * @param curvOff
     * @param squareCurve
     * @param bearingRad
     * @param crossAngleRadians
     * @return
     */
    public static Vector<double[]> calcRowsPoints(double width, double length, double hstep,
            double alternationPercent, double curvOff, boolean squareCurve, double bearingRad,
            double crossAngleRadians) {
        return calcRowsPoints(width, length, hstep, alternationPercent, curvOff, squareCurve,
                bearingRad, crossAngleRadians, false);
    }
    
    /**
     * @param width
     * @param length
     * @param hstep
     * @param alternationPercent
     * @param curvOff
     * @param squareCurve
     * @param bearingRad
     * @param crossAngleRadians
     * @param invertY
     * @return
     */
    public static Vector<double[]> calcRowsPoints(double width, double length, double hstep,
            double alternationPercent, double curvOff, boolean squareCurve, double bearingRad,
            double crossAngleRadians, boolean invertY) {
        width = Math.abs(width);
        length = Math.abs(length);
        hstep = Math.abs(hstep);
        hstep = hstep == 0 ? 0.1 : hstep;
        
        boolean direction = true;
        Vector<double[]> newPoints = new Vector<double[]>();
        double[] point = {-curvOff, 0, 0, -1};
        newPoints.add(point);
        
        double x2;
        for (double y = 0; y - (!direction ? hstep * (1 - alternationPercent) : 0) <= width; y += hstep) {
            if (direction)
                x2 = length + curvOff;
            else
                x2 = -curvOff;
            direction = !direction;

            double hstepDelta = 0;
            if (direction)
                hstepDelta = hstep * (1 - alternationPercent);
            point = new double[] { x2, y - hstepDelta, 0, -1 };
            newPoints.add(point);

            if (y + hstep * (!direction ? alternationPercent : 1) <= width) {
                double hstepAlt = hstep;
                if (!direction)
                    hstepAlt = hstep * alternationPercent;
                point = new double[] { x2 + (squareCurve ? 0 : 1) * (direction ? curvOff : -curvOff), y + hstepAlt, 0, -1 };
                newPoints.add(point);
            }
        }
        for (double[] pt : newPoints) {
            double[] res = AngleUtils.rotate(-crossAngleRadians, pt[X], 0, false);
            pt[X] = res[0];
            pt[Y] = pt[Y] + res[1];
            if (invertY)
                pt[Y] = -pt[Y];
            res = AngleUtils.rotate(bearingRad + (!invertY ? -1 : 1) * -crossAngleRadians, pt[X], pt[Y], false);
            pt[X] = res[0];
            pt[Y] = res[1];
        }
        return newPoints;
    }

    public static Vector<double[]> calcExpansiveSquarePatternPointsMaxBox(
            double width, double hstep, double bearingRad, boolean invertY) {
        width = Math.abs(width);
        hstep = Math.abs(hstep);
        hstep = hstep == 0 ? 0.1 : hstep;

        Vector<double[]> newPoints = new Vector<double[]>();
        
        final short left = 0, up = 1, right = 2, down = 3;
        
        double[] point;;
        
        double x = 0;
        double y = 0;
        short stepDir = left;
        int stepX = 1;
        int stepY = 1;
        do {
            point = new double[] { x, y, 0, -1 };
            newPoints.add(point);

            switch (stepDir) {
                case left:
                    x += hstep * stepX;
                    // y = y;
                    stepX++;
                    break;
                case up:
                    // x = x;
                    y += hstep * stepY;
                    stepY++;
                    break;
                case right:
                    x -= hstep * stepX;
                    // y = y;
                    stepX++;
                    break;
                case down:
                    // x = x;;
                    y -= hstep * stepY;
                    stepY++;
                    break;
                default:
                    throw new RuntimeException("Something went wrong!!");
            }
            stepDir = (short) (++stepDir % 4);
        } while (Math.abs(x) <= width / 2 || Math.abs(y) <= width / 2);

        double[] le = newPoints.lastElement();
        le[0] = Math.signum(le[0]) * Math.min(Math.abs(le[0]), width / 2);
        le[1] = Math.signum(le[1]) * Math.min(Math.abs(le[1]), width / 2);
        
        for (double[] pt : newPoints) {
            double[] res = AngleUtils.rotate(0, pt[X], 0, false);
            pt[X] = res[0];
            pt[Y] = pt[Y] + res[1];
            if (invertY)
                pt[Y] = -pt[Y];
            res = AngleUtils.rotate(bearingRad, pt[X], pt[Y], false);
            pt[X] = res[0];
            pt[Y] = res[1];
        }

        return newPoints;
    }

    /**
     * @param g2d
     * @param zoom
     * @param points
     * @param paintSSRange
     * @param sRange
     */
    public static void paintPointLineList(Graphics2D g2d, double zoom, List<double[]> points,
            boolean paintSSRange, double sRange) {
        paintPointLineList(g2d, zoom, points, paintSSRange, sRange, false);
    }
    
    
    /**
     * @param g2d
     * @param zoom
     * @param points
     * @param paintSSRange
     * @param sRange
     * @param editMode
     */
    public static void paintPointLineList(Graphics2D g2d, double zoom, List<double[]> points,
            boolean paintSSRange, double sRange, boolean editMode) {
        zoom = zoom <= 0 ? 0.01 : zoom;
        sRange = sRange < 0 ? 0 : sRange;
        
        double[] pointI, pointF, pointN;
        
        Color oColor = g2d.getColor();
        
        Stroke sO = g2d.getStroke();
        Stroke s1 = new BasicStroke(1);
        Stroke s2 = new BasicStroke(2);
        Stroke s3 = new BasicStroke(3);
        Stroke sR = new BasicStroke((float) (2 * sRange * zoom), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        
        for (int i = 0; i < points.size(); i += 2) {
            pointI = points.get(i);
            //NeptusLog.pub().info("<###>[" + pointI[X] + ", " + pointI[Y] + "]");
            try {
                pointF = points.get(i + 1);
            }
            catch (Exception e1) {
                pointF = null;
            }
            try {
                pointN = points.get(i + 2);
            }
            catch (Exception e) {
                pointN = null;
            }
            int ellisRadius = !editMode ? 3 : 4;
            int factor = 2;
            Ellipse2D el = new Ellipse2D.Double(-ellisRadius, -ellisRadius, ellisRadius * 2, ellisRadius * 2);
            Ellipse2D ex = new Ellipse2D.Double(-ellisRadius * factor, -ellisRadius * factor, ellisRadius * factor * 2, ellisRadius * factor * 2);
            if (i == 0) {
                g2d.translate(pointI[X] * zoom, pointI[Y] * zoom);
                g2d.setColor(new Color(0, 0, 255));
                g2d.fill(ex);
                g2d.translate(-pointI[X] * zoom, -pointI[Y] * zoom);
            }
            if (pointF != null) {
                Line2D.Double l = new Line2D.Double(pointI[X] * zoom, pointI[Y] * zoom, pointF[X] * zoom, pointF[Y] * zoom);
                if (paintSSRange) {
                    g2d.setColor(new Color(0, 0, 0, 30));
                    g2d.setStroke(sR);
                    g2d.draw(l);
                }
                g2d.setColor(!editMode ? Color.yellow : Color.orange);
                g2d.setStroke(!editMode ? s2 : s3);
                g2d.draw(l);
                g2d.setColor(Color.black);
                g2d.setStroke(s1);
                if (!editMode)
                    g2d.draw(l);
                g2d.setStroke(sO);

                g2d.translate(pointF[X] * zoom, pointF[Y] * zoom);
                
                g2d.setColor(new Color(255, 0, 0));
                if (i == points.size() - 2) {
                    g2d.setColor(new Color(0, 0, 255));
                    g2d.fill(ex);
                    g2d.setColor(Color.WHITE);
                    g2d.fill(el);
                }
                else {
                    g2d.fill(el);
                }
                
                if (zoom > 0.4) {
                    double angle = Math.atan2(pointF[Y] - pointI[Y], pointF[X] - pointI[X]);
                    Graphics2D gArrow = (Graphics2D) g2d.create();
                    gArrow.rotate(angle - Math.PI / 2);
                    if (editMode)
                        gArrow.scale(1.5, 1.5);
                    gArrow.setColor(!editMode ? Color.gray : Color.black);
                    gArrow.fill(arrow2);
                    gArrow.setColor(!editMode ? Color.yellow : Color.orange);
                    gArrow.fill(arrow1);
                    gArrow.dispose();
                }
                
                g2d.translate(-pointF[X] * zoom, -pointF[Y] * zoom);

                if (pointN != null) {
                    l = new Line2D.Double(pointF[X] * zoom, pointF[Y] * zoom, pointN[X] * zoom, pointN[Y] * zoom);
                    g2d.setColor(!editMode ? Color.yellow : Color.orange);
                    g2d.setStroke(!editMode ? s2 : s3);
                    g2d.draw(l);
                    g2d.setColor(Color.black);
                    g2d.setStroke(s1);
                    if (!editMode)
                        g2d.draw(l);
                    g2d.setStroke(sO);
                    
                    g2d.translate(pointN[X] * zoom, pointN[Y] * zoom);
                    
                    g2d.setColor(new Color(0, 255, 0));
                    if (i == points.size() - 3) {
                        g2d.setColor(new Color(0, 0, 255));
                        g2d.fill(ex);
                        g2d.setColor(Color.WHITE);
                        g2d.fill(el);
                    }
                    else {
                        g2d.fill(el);
                    }
                    
                    if (zoom > 0.4) {
                        double angle = Math.atan2(pointN[Y] - pointF[Y], pointN[X] - pointF[X]);
                        Graphics2D gArrow = (Graphics2D) g2d.create();
                        gArrow.rotate(angle - Math.PI / 2);
                        if (editMode)
                            gArrow.scale(1.5, 1.5);
                        gArrow.setColor(!editMode ? Color.gray : Color.black);
                        gArrow.fill(arrow2);
                        gArrow.setColor(!editMode ? Color.yellow : Color.orange);
                        gArrow.fill(arrow1);
                        gArrow.dispose();
                    }
                    
                    g2d.translate(-pointN[X] * zoom, -pointN[Y] * zoom);
                }
            }
        }
        g2d.setColor(oColor);
    }

    /**
     * @param g2d
     * @param zoom
     * @param width
     * @param length
     * @param x0
     * @param y0
     * @param bearingRad
     * @param crossAngleRadians
     * @param fill
     * @param invertY
     * @param editMode
     */
    public static void paintBox(Graphics2D g, double zoom, double width, double length, double x0, double y0,
            double bearingRad, double crossAngleRadians, boolean fill, boolean invertY, boolean editMode) {
        zoom = zoom <= 0 ? 0.01 : zoom;
        width = Math.abs(width);
        length = Math.abs(length);
        
        Graphics2D g2d = (Graphics2D) g.create();
        double mult = !invertY ? 1 : -1;
        GeneralPath sp = new GeneralPath();
        sp.moveTo(x0 * zoom, y0 * zoom);
        double[] resT = AngleUtils.rotate(-crossAngleRadians, length, 0, false);
        sp.lineTo(x0 * zoom + resT[0] * zoom, mult * (y0 * zoom + resT[1] * zoom));
        sp.lineTo(x0 * zoom + resT[0] * zoom, mult * (y0 * zoom + (width + resT[1]) * zoom));
        sp.lineTo(x0 * zoom, mult * (y0 * zoom + width * zoom));
        sp.closePath();
        g2d.setColor(!editMode ? noEditBoxColor : editBoxColor);
        g2d.rotate(bearingRad + (!invertY ? -1 : 1) * -crossAngleRadians);
        Stroke s1 = new BasicStroke(2);
        Stroke s3 = new BasicStroke(3);
        g2d.setStroke(!editMode ? s1 : s3);
        g2d.draw(sp);
        if (fill) {
            g2d.setColor(editMode ? editBoxColor : noEditBoxColor);
            g2d.fill(sp);
        }
        g2d.dispose();
    }
    
    public static double getSpeedMps(Maneuver man) {
        if (!(man instanceof ManeuverWithSpeed))
            return Double.NaN;
        
        return ((ManeuverWithSpeed)man).getSpeed().getMPS();        
    }
    
    /**
     * @param man
     * @return
     */
    public static Vector<DefaultProperty> getPropertiesFromManeuver(Maneuver man) {
        Vector<DefaultProperty> properties = new Vector<DefaultProperty>();
        PluginProperty[] prop = PluginUtils.getPluginProperties(man);
        properties.addAll(Arrays.asList(prop));
        return properties;
    }

    /**
     * @param man
     * @param properties
     */
    public static void setPropertiesToManeuver(Maneuver man, Property[] properties) {
        PluginUtils.setPluginProperties(man, properties);
    }
    
    public static <M extends Maneuver> Class<M> getManeuverFromType(String type) {
        return IMCUtils.getManeuverFromType(type);
    }
}
