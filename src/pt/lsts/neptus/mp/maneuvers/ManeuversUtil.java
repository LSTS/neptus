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
import java.util.Vector;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.preview.SpeedConversion;
import pt.lsts.neptus.plugins.PluginProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.AngleCalc;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author pdias
 *
 */
public class ManeuversUtil {
    
    protected static final int X = 0, Y = 1, Z = 2, T = 3;

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
        double[] res = AngleCalc.rotate(bearingRad, pointBaseB[X], pointBaseB[Y], false);
        double[] pointBase1 = new double[] {res[X], res[Y], 0, -1};
        res = AngleCalc.rotate(bearingRad+Math.toRadians(-60), pointBaseB[X], pointBaseB[Y], false);
        double[] pointBase2 = {res[X], res[Y], 0, -1};
        res = AngleCalc.rotate(bearingRad+Math.toRadians(-120), pointBaseB[X], pointBaseB[Y], false);
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
        double[] res = AngleCalc.rotate(bearingRad, pointBase1[X], pointBase1[Y], false);
        pointBase1 = new double[] {res[X], res[Y], 0, -1};
        res = AngleCalc.rotate(bearingRad, pointBase2[X], pointBase2[Y], false);
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
        
        boolean direction = true;
        Vector<double[]> newPoints = new Vector<double[]>();
        double[] point = {-curvOff, 0, 0, -1};
        newPoints.add(point);
        
//        double x1; 
        double x2;
        for (double y = 0; y <= width; y += hstep) {
            if (direction) {
//                x1 = -curvOff; 
                x2 = length + curvOff;
            }
            else {
//                x1 = length + curvOff;
                x2 = -curvOff;
            }
            direction = !direction;

            double hstepDelta = 0;
            if (direction)
                hstepDelta = hstep * (1 - alternationPercent);
            point = new double[] { x2, y - hstepDelta, 0, -1 };
            newPoints.add(point);

            if (y + hstep <= width) {
                double hstepAlt = hstep;
                if (!direction)
                    hstepAlt = hstep * alternationPercent;
                point = new double[] { x2 + (squareCurve ? 0 : 1) * (direction ? curvOff : -curvOff), y + hstepAlt, 0, -1 };
                newPoints.add(point);
            }
        }
        for (double[] pt : newPoints) {
            double[] res = AngleCalc.rotate(-crossAngleRadians, pt[X], 0, false);
            pt[X] = res[0];
            pt[Y] = pt[Y] + res[1];
            if (invertY)
                pt[Y] = -pt[Y];
            res = AngleCalc.rotate(bearingRad + (!invertY ? -1 : 1) * -crossAngleRadians, pt[X], pt[Y], false);
            pt[X] = res[0];
            pt[Y] = res[1];
        }
        
//        NeptusLog.pub().info("<###>Points");
//        for (double[] pt : newPoints) {
//            NeptusLog.pub().info("<###>[" + pt[X] + ", " + pt[Y] + "]");
//        }
        return newPoints;
    }

    /**
     * @param g2d
     * @param zoom
     * @param points
     * @param paintSSRange
     * @param sRange
     */
    public static void paintPointLineList(Graphics2D g2d, double zoom, Vector<double[]> points,
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
    public static void paintPointLineList(Graphics2D g2d, double zoom, Vector<double[]> points,
            boolean paintSSRange, double sRange, boolean editMode) {
        double[] pointI, pointF, pointN;
        
        Color oColor = g2d.getColor();
        
        Stroke sO = g2d.getStroke();
        Stroke s1 = new BasicStroke(1);
        Stroke s2 = new BasicStroke(2);
        Stroke s3 = new BasicStroke(3);
        Stroke sR = new BasicStroke((float) (2 * sRange * zoom), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        
        for (int i = 0; i < points.size(); i+=2) {
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
            Ellipse2D el = new Ellipse2D.Double(-ellisRadius, -ellisRadius, ellisRadius * 2, ellisRadius * 2);
            if (i == 0) {
                g2d.translate(pointI[X] * zoom, pointI[Y] * zoom);
                g2d.setColor(new Color(0, 255, 0));
                g2d.fill(el);
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
                g2d.fill(el);
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
                    g2d.fill(el);
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
     * @param bearingRad
     * @param crossAngleRadians
     * @param editMode
     */
    public static void paintBox(Graphics2D g2d, double zoom, double width, double length,
            double x0, double y0,
            double bearingRad, double crossAngleRadians, boolean editMode) {
        paintBox(g2d, zoom, width, length, x0, y0, bearingRad, crossAngleRadians, false, editMode);
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
     * @param invertY
     * @param editMode
     */
    public static void paintBox(Graphics2D g2d, double zoom, double width, double length, double x0, double y0,
            double bearingRad, double crossAngleRadians, boolean invertY, boolean editMode) {
        double mult = !invertY ? 1 : -1;
        GeneralPath sp = new GeneralPath();
        sp.moveTo(x0 * zoom, y0 * zoom);
        double[] resT = AngleCalc.rotate(-crossAngleRadians, length, 0, false);
        sp.lineTo(x0 * zoom + resT[0] * zoom, mult * (y0 * zoom + resT[1] * zoom));
//        resT = AngleCalc.rotate(crossAngleRadians, length, 0, false);
        sp.lineTo(x0 * zoom + resT[0] * zoom, mult * (y0 * zoom + (width + resT[1]) * zoom));
        sp.lineTo(x0 * zoom, mult * (y0 * zoom + width * zoom));
        sp.closePath();
        g2d.setColor(!editMode ? new Color(255, 255, 255, 100) : new Color(255, 125, 255, 200));
        g2d.rotate(bearingRad + (!invertY ? -1 : 1) * -crossAngleRadians);
        Stroke sO = g2d.getStroke();
        Stroke s1 = new BasicStroke(1);
        Stroke s3 = new BasicStroke(2);
        g2d.setStroke(!editMode ? s1 : s3);
        g2d.draw(sp);
//        if (editMode) {
//            g2d.setColor(new Color(255, 125, 255, 100));
//            g2d.fill(sp);
//        }
        g2d.setStroke(sO);
        g2d.rotate(-bearingRad + (!invertY ? 1 : -1) * -crossAngleRadians);
    }
    
    public static double getSpeedMps(Maneuver man) {
        
        DefaultProperty speedProp = null, unitsProp = null;
        
        for (DefaultProperty dp : man.getProperties()) {
            if (dp.getName().equalsIgnoreCase("Speed"))
                speedProp = dp;
            else if (dp.getName().equalsIgnoreCase("Speed units"))
                unitsProp = dp;
        }
        
        if (speedProp == null || unitsProp == null)
            return Double.NaN;
        
        switch (""+unitsProp.getValue()) {
            case "m/s":
                return Double.parseDouble(""+speedProp.getValue());
            case "RPM":
                return SpeedConversion.convertRpmtoMps(Double.parseDouble(""+speedProp.getValue()));
            case "%":
                return SpeedConversion.convertPercentageToMps(Double.parseDouble(""+speedProp.getValue()));
            default:
                NeptusLog.pub().error("Unrecognized speed units: "+unitsProp.getValue());
                return Double.NaN;
        }
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
}
