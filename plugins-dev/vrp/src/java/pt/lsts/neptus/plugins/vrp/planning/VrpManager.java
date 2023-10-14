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
 * Author: Rui Gonçalves
 * 2010/04/14
 */
package pt.lsts.neptus.plugins.vrp.planning;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import javax.vecmath.Point2d;

import drasys.or.graph.DuplicateVertexException;
import drasys.or.graph.EdgeI;
import drasys.or.graph.GraphI;
import drasys.or.graph.MatrixGraph;
import drasys.or.graph.PointGraph;
import drasys.or.graph.VertexNotFoundException;
import drasys.or.graph.vrp.BestOf;
import drasys.or.graph.vrp.ClarkeWright;
import drasys.or.graph.vrp.Composite;
import drasys.or.graph.vrp.ImproveI;
import drasys.or.graph.vrp.ImproveWithTSP;
import drasys.or.graph.vrp.SolutionNotFoundException;
import drasys.or.graph.vrp.VRPException;
import pt.lsts.neptus.NeptusLog;

/**
 * @author Rui Gonçalves
 * 
 */
public class VrpManager {

    public static double dist = 0;

    public static Vector<Vector<Point2d>> computePathsSingleDepot(Point2d depot, Vector<Point2d> pointList,
            int nVehicles) {

        Vector<Vector<Point2d>> returnVector = new Vector<>();

        if (nVehicles <= 0) { // Protection of divide by zero
            NeptusLog.pub().debug("number of vehicles cannot be <= 0solution not found");
            return returnVector;
        }

        int sizeVisitPoints = pointList.size();
        PointGraph pointGraph = new PointGraph();
        PointIdoubleI[] arrayVRP = new PointIdoubleI[sizeVisitPoints + 1];

        Object key = "Depot";
        arrayVRP[0] = new PointIdoubleI(1, depot);
        try {
            pointGraph.addVertex(key, arrayVRP[0]);
        }
        catch (DuplicateVertexException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            for (int i = 1; i < sizeVisitPoints + 1; i++) {

                pointGraph.addVertex(i, new PointIdoubleI(1, pointList.get(i - 1)));
            }
        }
        catch (DuplicateVertexException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        GraphI graph;
        graph = new MatrixGraph(pointGraph, null);
        graph.setSymmetric(false);

        Composite vrp;
        BestOf bestOf = new BestOf();
        int iterations = 10, strength = 4;
        drasys.or.graph.tsp.ImproveI subalgorithm = new drasys.or.graph.tsp.TwoOpt();
        try {
            bestOf.addConstruct(new ClarkeWright(iterations, strength, subalgorithm));
            // bestOf.addConstruct(new
            // GillettMiller(iterations,strength,subalgorithm));
        }
        catch (VRPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        ImproveI improve = new ImproveWithTSP(new drasys.or.graph.tsp.ThreeOpt());
        vrp = new Composite(bestOf, improve);
        vrp.setCostConstraint(Double.MAX_VALUE/* rangeConstraint*1000 */);

        vrp.setCapacityConstraint(5000 /* capacityConstraint */);

        vrp.setGraph(graph);

        Vector<?>[] tours = null;
        try {
            NeptusLog.pub().debug("solver called");
            vrp.constructClosedTours("Depot");
            NeptusLog.pub().debug("passed");
            tours = vrp.getTours();
        }
        catch (SolutionNotFoundException e) {
            // TODO Auto-generated catch block
            NeptusLog.pub().debug("solution not found");
            e.printStackTrace();
        }
        catch (VertexNotFoundException e) {
            NeptusLog.pub().debug("vertex not found");
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (tours == null) {
            tours = new Vector[0];
        }

        double meters = 0;
        if (nVehicles == tours.length)
            NeptusLog.pub().info("solved - One path for each vehicle");

        for (Vector<?> tour : tours) {
            Vector<Point2d> path = new Vector<>();
            Enumeration<?> e = tour.elements();
            e.nextElement(); // Skip Vertex
            // PointIdoubleI customer_aux=
            // (PointIdoubleI)edge_aux.getToVertex().getValue();
            // customer_aux.getPoint2d()
            while (e.hasMoreElements()) {
                EdgeI edge = (EdgeI) e.nextElement();
                PointIdoubleI customer1 = (PointIdoubleI) edge.getToVertex().getValue();

                PointIdoubleI customer2 = (PointIdoubleI) edge.getFromVertex().getValue();
                meters += customer1.distanceTo(customer2);

                path.add(customer1.getPoint2d());
                /*
                 * int x1 = (int)customer1.screenPoint.x(); int y1 = (int)customer1.screenPoint.y(); int x2 =
                 * (int)customer2.screenPoint.x(); int y2 = (int)customer2.screenPoint.y();
                 */
                // g.drawLine(x1, y1, x2, y2);
                e.nextElement(); // Skip Vertex
            }
            returnVector.add(path);
        }
        String msg = "Vehicles - " + tours.length + ", ";
        msg += "Distance(Km) - " + meters / 1000;
        NeptusLog.pub().info(msg);

        dist = meters;
        // ------------------------------------------------------------------
        double rangeConstraint = meters;
        double step = rangeConstraint / nVehicles;

        rangeConstraint -= step;

        int last = -1;

        while (returnVector.size() != nVehicles) {

            vrp.setCostConstraint(rangeConstraint/* rangeConstraint*1000 */);
            NeptusLog.pub().debug("range:" + rangeConstraint);
            NeptusLog.pub().debug("step:" + step);
            NeptusLog.pub().debug("Vehicles:" + returnVector.size());
            NeptusLog.pub().debug("Last:" + last);

            try {
                NeptusLog.pub().debug("solver was called");
                vrp.constructClosedTours("Depot");
                NeptusLog.pub().debug("passed");
                tours = vrp.getTours();

                if (last == 0)
                    last = 1;
            }
            catch (SolutionNotFoundException e) {
                // TODO Auto-generated catch block
                NeptusLog.pub().debug("solution not found : increasing distance");
                // e.printStackTrace();
                if (last != 0)
                    step /= 2;
                rangeConstraint += step;
                last = 0;

            }
            catch (VertexNotFoundException e) {
                NeptusLog.pub().debug("Vertex not found");
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            returnVector.clear();

            for (Vector<?> tour : tours) {
                Vector<Point2d> path = new Vector<>();
                Enumeration<?> e = tour.elements();
                e.nextElement(); // Skip Vertex
                // PointIdoubleI customer_aux=
                // (PointIdoubleI)edge_aux.getToVertex().getValue();
                // customer_aux.getPoint2d()
                while (e.hasMoreElements()) {
                    EdgeI edge = (EdgeI) e.nextElement();
                    PointIdoubleI customer1 = (PointIdoubleI) edge.getToVertex().getValue();

                    PointIdoubleI customer2 = (PointIdoubleI) edge.getFromVertex().getValue();
                    meters += customer1.distanceTo(customer2);

                    path.add(customer1.getPoint2d());
                    /*
                     * int x1 = (int)customer1.screenPoint.x(); int y1 = (int)customer1.screenPoint.y(); int x2 =
                     * (int)customer2.screenPoint.x(); int y2 = (int)customer2.screenPoint.y();
                     */
                    // g.drawLine(x1, y1, x2, y2);
                    e.nextElement(); // Skip Vertex
                }
                returnVector.add(path);
            }

            if (last != 0) {
                if (returnVector.size() > nVehicles) {
                    if (last < 0) {
                        step /= 2;
                    }

                    rangeConstraint += step;
                    last = 1;
                }

                if (returnVector.size() < nVehicles) {

                    if (last > 0) {
                        step /= 2;
                    }

                    rangeConstraint -= step;

                    last = -1;
                }
            }
        }

        dist = meters;

        // ------------------------------------------------------------------------
        /*
         * if(returnVector.size()!=n_vehicles) return null; else
         */
        return returnVector;
    }

    /*
     * public static Vector<Vector<Point2d>> computePathsSingleDepot2( Point2d depot, Vector<Point2d> pointList, int
     * n_vehicles)
     * 
     * {
     * 
     * Vector<Vector<Point2d>> returnVector = computePathsSingleDepot2(depot, pointList, n_vehicles, Double.MAX_VALUE);
     * 
     * double rangeConstraint = dist; double step = rangeConstraint / n_vehicles;
     * 
     * int last = 0;
     * 
     * while (returnVector.size() != n_vehicles) {
     * 
     * returnVector = computePathsSingleDepot2(depot, pointList, n_vehicles, rangeConstraint);
     * 
     * NeptusLog.pub().info("<###>Vehicles foud"+returnVector.size()); NeptusLog.pub().info("<###>Total dist"+dist);
     * 
     * if (returnVector.size() > n_vehicles) { if (last < 0) { step /= 2; }
     * 
     * rangeConstraint += step; last = 1; }
     * 
     * if (returnVector.size() < n_vehicles) {
     * 
     * if (last > 0) { step /= 2; }
     * 
     * rangeConstraint -= step;
     * 
     * last = -1; } }
     * 
     * return returnVector;
     * 
     * }
     */

    /**
     * @param depot Vehicle depot (start and end location)
     * @param pointList (points to be visited)
     * @param n_vehicles (Number of available vehicles)
     * @return A list of waypoint lists to be visited by each vehicle
     */
    public static Vector<Vector<Point2d>> computePathsSingleDepot3(Point2d depot, Vector<Point2d> pointList,
            int n_vehicles) {

        int sizeVisitPoints = pointList.size();
        PointGraph pointGraph = new PointGraph();
        PointIdoubleI[] arrayVRP = new PointIdoubleI[sizeVisitPoints + 1];

        ArrayList<Point2d> arrayCHull = new ArrayList<>(sizeVisitPoints + 1);
        arrayCHull.add(0, depot);

        for (int i = 1; i <= sizeVisitPoints; i++) {
            arrayCHull.add(i, pointList.get(i - 1));

        }
        arrayCHull.sort((pt1, pt2) -> {
            double r = pt1.x - pt2.x;
            if (r != 0) {
                if (r < 0)
                    return -1;
                else
                    return 1;
            }
            else {
                if ((pt1.y - pt2.y) < 0)
                    return -1;
                else
                    return 1;
            }
        });
        for (int i = 0; i <= sizeVisitPoints; i++) {
            if (arrayCHull.get(i) == depot)
                NeptusLog.pub().info("found depot");
        }

        ArrayList<Point2d> hull = CHull.cHull(arrayCHull);

        boolean depot_out_hull = false;
        if (hull != null) {
            for (Point2d point2d : hull) {
                if (point2d == depot) {
                    depot_out_hull = true;
                    break;
                }
            }
        }
        else {
            hull = new ArrayList<>();
        }

        Object key = "Depot";
        arrayVRP[0] = new PointIdoubleI(0, depot);
        try {
            pointGraph.addVertex(key, arrayVRP[0]);
        }
        catch (DuplicateVertexException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (depot_out_hull) {
            NeptusLog.pub().debug("depot is out");
            hull = CHull.resizePath(n_vehicles + 1, hull, arrayCHull, depot);

            // int index_aux=-1; // must find depot on hull
            // for (int i=0;i<hull.size();i++)
            // {
            // if( hull.get(i)==d)
            // {
            // NeptusLog.pub().info("<###>encontrei depot no resize hull");
            // index_aux=i; // FOUND
            // }
            // }
            try {
                for (int i = 1; i < sizeVisitPoints + 1; i++) {
                    arrayVRP = new PointIdoubleI[sizeVisitPoints + 1];
                    key = i;
                    arrayVRP[i] = new PointIdoubleI(0, pointList.get(i - 1));
                    for (Point2d point2d : hull) {
                        if (point2d == arrayVRP[i].getPoint2d()) {
                            NeptusLog.pub().debug("found listpoint in hull");
                            arrayVRP[i].setLoad(1);
                        }

                    }
                    pointGraph.addVertex(key, arrayVRP[i]);
                }
            }
            catch (DuplicateVertexException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        else {
            NeptusLog.pub().debug("depot is in");
            hull = CHull.resizePath(n_vehicles, hull, arrayCHull, null);

            arrayVRP = new PointIdoubleI[sizeVisitPoints + 1];
            try {
                for (int i = 1; i < sizeVisitPoints + 1; i++) {

                    key = i;
                    arrayVRP[i] = new PointIdoubleI(0, pointList.get(i - 1));
                    for (Point2d point2d : hull) {
                        if (point2d == arrayVRP[i].getPoint2d()) {
                            NeptusLog.pub().debug("found listpoint in hull");
                            arrayVRP[i].setLoad(1);
                        }

                    }
                    pointGraph.addVertex(key, arrayVRP[i]);
                }
            }
            catch (DuplicateVertexException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        GraphI graph;
        graph = new MatrixGraph(pointGraph, null);
        graph.setSymmetric(false);

        Composite vrp;
        BestOf bestOf = new BestOf();
        int iterations = 10, strength = 4;
        drasys.or.graph.tsp.ImproveI subalgorithm = new drasys.or.graph.tsp.Us(5);
        try {
            bestOf.addConstruct(new ClarkeWright(iterations, strength, subalgorithm));
            // bestOf.addConstruct(new GillettMiller(iterations,strength,subalgorithm));
        }
        catch (VRPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        ImproveI improve = new ImproveWithTSP(new drasys.or.graph.tsp.ThreeOpt());
        vrp = new Composite(bestOf, improve);
        vrp.setCostConstraint(Double.MAX_VALUE/* rangeConstraint*1000 */);

        vrp.setCapacityConstraint(1 /* capacityConstraint */);

        vrp.setGraph(graph);

        Vector<?>[] tours = null;
        try {
            NeptusLog.pub().debug("solver called");
            vrp.constructClosedTours("Depot");
            NeptusLog.pub().debug("passed");
            tours = vrp.getTours();
        }
        catch (SolutionNotFoundException | VertexNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (tours != null && n_vehicles == tours.length) {
            NeptusLog.pub().debug("Solved - One path for each vehicle");
        }

        Vector<Vector<Point2d>> returnVector = new Vector<>();

        if (tours != null) {
            for (Vector<?> tour : tours) {
                Vector<Point2d> path = new Vector<>();
                Enumeration<?> e = tour.elements();
                e.nextElement(); // Skip Vertex
                while (e.hasMoreElements()) {
                    EdgeI edge = (EdgeI) e.nextElement();
                    PointIdoubleI customer1 = (PointIdoubleI) edge.getToVertex().getValue();
                    path.add(customer1.getPoint2d());
                    e.nextElement(); // Skip Vertex
                }
                returnVector.add(path);
            }
        }
        if (returnVector.size() != n_vehicles)
            return null;
        else
            return returnVector;
    }

    public static int totalDist(Vector<?>[] tours) {
        int meters = 0;
        for (Vector<?> tour : tours) {
            Enumeration<?> e = tour.elements();
            e.nextElement(); // Skip Vertex
            while (e.hasMoreElements()) {
                EdgeI edge = (EdgeI) e.nextElement();
                Customer customer1 = (Customer) edge.getToVertex().getValue();
                Customer customer2 = (Customer) edge.getFromVertex().getValue();
                meters = Double.valueOf(customer1.distanceTo(customer2) + meters).intValue();
                e.nextElement(); // Skip Vertex
            }
        }
        return meters;
    }
}
