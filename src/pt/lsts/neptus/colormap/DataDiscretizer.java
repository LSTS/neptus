/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.colormap;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Vector;

/**
 * @author ZP
 */
public class DataDiscretizer {

    private final LinkedHashMap<String, DataPoint> points = new LinkedHashMap<String, DataPoint>();
    //private final GeneralPath cHullShape = new GeneralPath();
    public double maxX = Double.NaN, maxY = Double.NaN, minX = Double.NaN, minY = Double.NaN;
    public double minVal[] = null, maxVal[] = null;
    private double cellWidth = 5;
    //private ArrayList<Point> chull = new ArrayList<Point>();

    public DataDiscretizer(double cellWidth) {
        this.cellWidth = cellWidth;		
    }

    public void addPoint(Number x, Number y, Number value) {
        addPoint(x.doubleValue(), y.doubleValue(), value.doubleValue());
    }

    public void addPoint(Number x, Number y, Number[] values) {
        double[] vals = new double[values.length];
        for (int i = 0; i < vals.length; i++)
            vals[i] = values[i].doubleValue();
        addPoint(x.doubleValue(), y.doubleValue(), vals);
    }

    public void addPoint(double x, double y, double[] values) {
        if (Double.isNaN(maxX)) {
            maxX = minX = x;
            maxY = minY = y;
            minVal = new double[values.length];
            maxVal = new double[values.length];
            for (int i = 0; i < values.length; i++)
                minVal[i] = maxVal[i] = values[i];					
        }
        else {
            if (x > maxX) maxX = x;
            if (x < minX) minX = x;
            if (y > maxY) maxY = y;
            if (y < minY) minY = y;
            for (int i = 0; i < minVal.length; i++) {
                if (values[i] < minVal[i]) minVal[i] = values[i];
                if (values[i] > maxVal[i]) maxVal[i] = values[i];
            }
        }

        int x_ =  (int)(Math.floor(x/cellWidth)*cellWidth);
        int y_ =  (int)(Math.floor(y/cellWidth)*cellWidth);

        String id = x_+","+y_;

        if (points.containsKey(id))
            points.get(id).addValue(values);
        else
            points.put(id, new DataPoint(x_, y_, values));	
        //		
        //		ArrayList<Point> array = new ArrayList<Point>();
        //		for (DataPoint dp : points.values()) {
        //            array.add(new Point(dp.x, dp.y));
        //        }
        //        Collections.sort(array, new Comparator<Point>() {
        //            @Override
        //            public int compare(Point pt1, Point pt2) {
        //                int r = pt1.x - pt2.x;
        //                if (r != 0)
        //                    return r;
        //                else
        //                    return pt1.y - pt2.y;
        //            }
        //        });
        //        if (points.size() > 3) {
        //            chull = CHull.cHull(array);
        //            cHullShape.reset();
        //            for (int i = 0; i < chull.size(); i++) {
        //                if (i == 0)
        //                    cHullShape.moveTo(chull.get(i).x, chull.get(i).y);
        //                else
        //                    cHullShape.lineTo(chull.get(i).x, chull.get(i).y);
        //            }
        //            cHullShape.closePath();
        //        }
    }

    public void addPoint(double x, double y, double value) {
        addPoint(x, y, new double[] {value});
    }

    public DataPoint[] getDataPoints(int minValues) {		
        Vector<DataPoint> dps = new Vector<DataPoint>();

        for (DataPoint dp : points.values()) {
            if (dp.numValues >= minValues)
                dps.add(dp);
        }		
        return dps.toArray(new DataPoint[0]);
    }

    public DataPoint[] getDataPoints() {
        return points.values().toArray(new DataPoint[0]);
    }

    public int getAmountDataPoints() {
        return points.size();
    }

    public class DataPoint {
        private final int x, y;
        private int numValues;
        double sum[];
        public DataPoint(int x, int y) {
            this.x = x; this.y = y;
        }

        public DataPoint(int x, int y, double value) {
            this(x,y); addValue(value);
        }

        public DataPoint(int x, int y, double[] values) {
            this(x,y); addValue(values);
        }

        public void addValue(double value) {
            addValue(new double[]{value});
        }

        public void addValue(double values[]) {
            if (numValues == 0)
                sum = new double[values.length];
            numValues++;
            for (int i = 0; i < values.length; i++)
                sum[i] += values[i];
        }

        public double getValue() {
            return (numValues > 0)? sum[0]/numValues : Double.NaN; 
        }

        public double[] getValues() {
            double[] ret = new double[sum.length];
            for (int i = 0; i < sum.length; i++)
                ret[i] = (numValues > 0)? sum[i]/numValues : Double.NaN;

                return ret; 
        }

        public Point2D getPoint2D() {
            return new Point2D.Double(x,y);
        }

        @Override
        public String toString() {
            return "("+x+","+y+")="+getValue();
        }
    }

    public ArrayList<Point> computeConvexHull() {
        ArrayList<Point> array = new ArrayList<Point>();
        for (DataPoint dp : points.values()) {
            array.add(new Point(dp.x, dp.y));
        }
        Collections.sort(array, new Comparator<Point>() {
            @Override
            public int compare(Point pt1, Point pt2) {
                int r = pt1.x - pt2.x;
                if (r != 0)
                    return r;
                else
                    return pt1.y - pt2.y;
            }
        });

        return CHull.cHull(array);
    }

    /**
     * @return the cHullShape
     */
    public GeneralPath getCHullShape() {

        GeneralPath cHullShape = new GeneralPath();
        ArrayList<Point> chull = computeConvexHull();

        cHullShape.reset();
        if (chull.size() > 3) {
            cHullShape.reset();
            for (int i = 0; i < chull.size(); i++) {
                if (i == 0)
                    cHullShape.moveTo(chull.get(i).x, chull.get(i).y);
                else
                    cHullShape.lineTo(chull.get(i).x, chull.get(i).y);
            }
            cHullShape.closePath();
        }  

        return cHullShape;
    }
    // THIS CLASS AS A VERSIONED main() METHOD
}

class CHull {

    // Returns the determinant of the point matrix
    // This determinant tells how far p3 is from vector p1p2 and on which side
    // it is
    private static int distance(Point p1, Point p2, Point p3) {
        int x1 = p1.x;
        int x2 = p2.x;
        int x3 = p3.x;
        int y1 = p1.y;
        int y2 = p2.y;
        int y3 = p3.y;
        return x1 * y2 + x3 * y1 + x2 * y3 - x3 * y2 - x2 * y1 - x1 * y3;
    }

    // Returns the points of convex hull in the correct order
    static ArrayList<Point> cHull(ArrayList<Point> array) {
        int size = array.size();
        if (size < 2)
            return null;
        Point l = array.get(0);
        Point r = array.get(size - 1);
        ArrayList<Point> path = new ArrayList<Point>();
        path.add(l);
        cHull(array, l, r, path);
        path.add(r);
        cHull(array, r, l, path);
        return path;
    }

    static void cHull(ArrayList<Point> points, Point l, Point r, ArrayList<Point> path) {
        if (points.size() < 3)
            return;
        int maxDist = 0;
        int tmp;
        Point p = null;
        for (Point pt : points) {
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
        ArrayList<Point> left = new ArrayList<Point>();
        ArrayList<Point> right = new ArrayList<Point>();
        left.add(l);
        right.add(p);
        for (Point pt : points) {
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
