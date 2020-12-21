/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.util;

import java.awt.geom.Point2D;
import java.util.Vector;

import javax.vecmath.Vector2d;

/**
 * This class holds some Angle related utilities.
 * 
 * @author zecarlos
 * @author RJPG
 * @author pdias
 */
public class AngleUtils {
	/** 2Pi constant */
    public static final double TWO_PI_RADS = Math.PI * 2.0;

    /** To avoid initialization */
    private AngleUtils() {
    }
    
    /**
     * This method linearizes an vector of 2D points.
     * 
     * @param points The points to linearize
     * @param angleTolerance The acceptable angle to consider a straight line
     * @return
     */
    public static Vector<Point2D> linearizePoint2D(Vector<Point2D> points, double angleTolerance) {
        Point2D[] aux = (Point2D[]) points.toArray(new Point2D[] {});
        Vector<Point2D> aux2 = new Vector<Point2D>();

        boolean reduce = true;
        while (reduce) { // process while there are reductions to process
            aux2 = null;
            aux2 = new Vector<Point2D>();
            reduce = false;
            int nPointsProcess = aux.length;
            if (((double) nPointsProcess % 3.) != 0.) {
                nPointsProcess--;
                if (((double) nPointsProcess % 3.) != 0.)
                    nPointsProcess--;
            }

            int i = 0;
            while (i < nPointsProcess) { // remove the points if considered linear (process 3 by 3)
                Vector2d a = new Vector2d();
                Vector2d b = new Vector2d();

                a.x = aux[i + 1].getX() - aux[i].getX();
                a.y = aux[i + 1].getY() - aux[i].getY();

                b.x = aux[i + 2].getX() - aux[i + 1].getX();
                b.y = aux[i + 2].getY() - aux[i + 1].getY();

                if (a.angle(b) > angleTolerance) {
                    aux2.add(aux[i]);
                    aux2.add(aux[i + 1]);
                    aux2.add(aux[i + 2]);
                }
                else {
                    aux2.add(aux[i]);
                    aux2.add(aux[i + 2]);
                    reduce = true;
                }
                i += 3;
            }

            while (i < aux.length) { // adds the remainder
                aux2.add(aux[i]);
                i++;
            }
            aux = (Point2D[]) aux2.toArray(new Point2D[0]);
        }

        if (aux2.size() > 3) {
            aux = (Point2D[]) aux2.toArray(new Point2D[0]);
            aux2 = null;

            aux2 = new Vector<Point2D>();
            aux2.add(aux[0]);
            for (int i = 1; i < aux.length - 1; i++) {
                Vector2d a = new Vector2d();
                Vector2d b = new Vector2d();

                a.x = aux[i].getX() - aux[i - 1].getX();
                a.y = aux[i].getY() - aux[i - 1].getY();

                b.x = aux[i + 1].getX() - aux[i].getX();
                b.y = aux[i + 1].getY() - aux[i].getY();
                if (a.angle(b) > angleTolerance)
                    aux2.add(aux[i]);
            }
            aux2.add(aux[aux.length - 1]);
        }
        return aux2;
    }

    /**
     * Calculates the angle between two 2D points.
     * 
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    public static double calcAngle(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double angle = 0.0d;

        // Calculate angle
        if (dx == 0.0) {
            if (dy == 0.0)
                angle = 0.0;
            else if (dy > 0.0)
                angle = Math.PI / 2.0;
            else
                angle = Math.PI * 3.0 / 2.0;
        }
        else if (dy == 0.0) {
            if (dx > 0.0)
                angle = 0.0;
            else
                angle = Math.PI;
        }
        else {
            if (dx < 0.0)
                angle = Math.atan(dy / dx) + Math.PI;
            else if (dy < 0.0)
                angle = Math.atan(dy / dx) + TWO_PI_RADS;
            else
                angle = Math.atan(dy / dx);
        }

        // Return
        return -1 * (angle - (Math.PI / 2.0));
    }
    
    /**
     * Normalizes an angle in radians.
     * 
     * @param angle The angle to normalize
     * @return The angle between 0 and 2pi
     */
    public static double nomalizeAngleRads2Pi(double angle) {
    	double ret = angle;
    	ret = ret % TWO_PI_RADS;
        if (ret < 0.0)
            ret += TWO_PI_RADS;
        return ret;
    }

    /**
     * Normalizes an angle in radians.
     * 
     * @param angle The angle to normalize
     * @return The angle between -pi and pi
     */
    public static double nomalizeAngleRadsPi(double angle) {
        double ret = angle;
        while (ret > Math.PI)
            ret -= TWO_PI_RADS;
        while (ret < -Math.PI)
            ret += TWO_PI_RADS;
        return ret;
    }
    
    /**	
     * Normalizes an angle in degrees.
     * 
     * @param angle The angle to normalize
     * @return The angle between 0 and 360
     */
    public static double nomalizeAngleDegrees360(double angle) {
    	double ret = angle;
    	ret = ret % 360.0;
    	if(ret < 0.0)
    		ret+= 360.0;
    	return ret;
    }

    /**
     * Normalizes an angle in degrees.
     * 
     * @param angle The angle to normalize
     * @return The angle between -180 and 180
     */
    public static double nomalizeAngleDegrees180(double angle) {
        double ret = angle;
        while (ret > 180)
            ret -= 360;
        while (ret < -180)
            ret += 360;
        return ret;
    }

    /**
     * XY Coordinate conversion considering a rotation angle.
     * 
     * @param angleRadians Angle
     * @param x original x value on entry, rotated x value on exit.
     * @param y original y value on entry, rotated y value on exit.
     * @param clockwiseRotation ClockwiseRotation rotation or not
     */
    public static double[] rotate(double angleRadians, double x, double y, boolean clockwiseRotation) {
        double sina = Math.sin(angleRadians), cosa = Math.cos(angleRadians);
        double[] xy = { 0, 0 };
        if (clockwiseRotation) {
            xy[0] = x * cosa + y * sina;
            xy[1] = -x * sina + y * cosa;
        }
        else {
            xy[0] = x * cosa - y * sina;
            xy[1] = x * sina + y * cosa;
        }
        return xy;
    }

    public static void main(String[] args) {
        System.out.println(Math.toDegrees(nomalizeAngleRads2Pi(Math.toRadians(360 + 120))));
        System.out.println(Math.toDegrees(nomalizeAngleRads2Pi(Math.toRadians(-120))));
        System.out.println(Math.toDegrees(nomalizeAngleRads2Pi(Math.PI * 4.2)));
        System.out.println(Math.toDegrees(nomalizeAngleRads2Pi(Math.PI * 3)));
		
        System.out.println();

        System.out.println(Math.toDegrees(calcAngle(0, 0, 1, 0)));
        System.out.println(Math.toDegrees(calcAngle(0, 0, 1, 0.2)));
		
        System.out.println();
        
		double[] xy = rotate(Math.PI/4, 1, .2, true);
        System.out.println("[" + xy[0] + ", " + xy[1] + "]");
        xy = rotate(Math.PI/4, 1, 0.2, false);
        System.out.println("[" + xy[0] + ", " + xy[1] + "]");
        
        System.out.println();
        
        xy = rotate(Math.toRadians(-2), 8233.212457347916, 3936.711000673984, false);
        System.out.println("[" + xy[0] + ", " + xy[1] + "]");
        xy = rotate(Math.toRadians(-2), xy[0], xy[1], false);
        System.out.println("[" + xy[0] + ", " + xy[1] + "]");
        xy = rotate(Math.toRadians(-2), xy[0], xy[1], false);
        System.out.println("[" + xy[0] + ", " + xy[1] + "]");
        xy = rotate(Math.toRadians(-2), xy[0], xy[1], false);
        System.out.println("[" + xy[0] + ", " + xy[1] + "]");
        xy = rotate(Math.toRadians(-2), xy[0], xy[1], false);
        System.out.println("[" + xy[0] + ", " + xy[1] + "]");
        xy = rotate(Math.toRadians(-2), xy[0], xy[1], false);
        System.out.println("[" + xy[0] + ", " + xy[1] + "]");
        xy = rotate(Math.toRadians(-2), xy[0], xy[1], false);
        System.out.println("[" + xy[0] + ", " + xy[1] + "]");
        xy = rotate(Math.toRadians(-2), xy[0], xy[1], false);
        System.out.println("[" + xy[0] + ", " + xy[1] + "]");
	}
}
