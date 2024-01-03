/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
            for (int i = 0; i < sum.length; i++) {
                ret[i] = (numValues > 0)? sum[i]/numValues : Double.NaN;
            }

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

    public ArrayList<Point2D> computeConvexHull() {
        ArrayList<Point2D> array = new ArrayList<Point2D>();
        for (DataPoint dp : points.values()) {
            array.add(new Point(dp.x, dp.y));
        }
        Collections.sort(array, new Comparator<Point2D>() {
            @Override
            public int compare(Point2D pt1, Point2D pt2) {
                double r = pt1.getX() - pt2.getX();
                if (r != 0)
                    return Double.valueOf(pt1.getX()).compareTo(pt2.getX());
                else
                    return Double.valueOf(pt1.getY()).compareTo(pt2.getY());
            }
        });

        return ConvexHull.compute(array);
    }

    /**
     * @return the cHullShape
     */
    public GeneralPath getCHullShape() {

        GeneralPath cHullShape = new GeneralPath();
        ArrayList<Point2D> chull = computeConvexHull();

        cHullShape.reset();
        if (chull.size() > 3) {
            cHullShape.reset();
            for (int i = 0; i < chull.size(); i++) {
                if (i == 0)
                    cHullShape.moveTo(chull.get(i).getX(), chull.get(i).getY());
                else
                    cHullShape.lineTo(chull.get(i).getX(), chull.get(i).getY());
            }
            cHullShape.closePath();
        }  

        return cHullShape;
    }
    // THIS CLASS AS A VERSIONED main() METHOD

}
