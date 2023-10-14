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
 * 23/02/2017
 */
package pt.lsts.neptus.colormap;

import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * @author zp
 *
 */
public class ConvexHull {

    // Returns the determinant of the point matrix
    // This determinant tells how far p3 is from vector p1p2 and on which side
    // it is
    private static double distance(Point2D p1, Point2D p2, Point2D p3) {
        double x1 = p1.getX();
        double x2 = p2.getX();
        double x3 = p3.getX();
        double y1 = p1.getY();
        double y2 = p2.getY();
        double y3 = p3.getY();
        return x1 * y2 + x3 * y1 + x2 * y3 - x3 * y2 - x2 * y1 - x1 * y3;
    }

    // Returns the points of convex hull in the correct order
    public static ArrayList<Point2D> compute(ArrayList<Point2D> array) {
        int size = array.size();
        if (size < 2)
            return null;
        Point2D l = array.get(0);
        Point2D r = array.get(size - 1);
        ArrayList<Point2D> path = new ArrayList<Point2D>();
        path.add(l);
        cHull(array, l, r, path);
        path.add(r);
        cHull(array, r, l, path);
        return path;
    }

    private static void cHull(ArrayList<Point2D> points, Point2D l, Point2D r, ArrayList<Point2D> path) {
        if (points.size() < 3)
            return;
        double maxDist = 0;
        double tmp;
        Point2D p = null;
        for (Point2D pt : points) {
            if (pt != l && pt != r) {
                tmp = distance(l, r, pt);
                if (tmp > maxDist) {
                    maxDist = tmp;
                    p = pt;
                }
            }
        }
        if (p == null)
            return;
        ArrayList<Point2D> left = new ArrayList<Point2D>();
        ArrayList<Point2D> right = new ArrayList<Point2D>();
        left.add(l);
        right.add(p);
        for (Point2D pt : points) {
            if (distance(l, p, pt) > 0)
                left.add(pt);
            else if (distance(p, r, pt) > 0)
                right.add(pt);
        }
        left.add(p);
        right.add(r);
        cHull(left, l, p, path);
        path.add(p);
        cHull(right, p, r, path);
    }
}
