/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Rui Gonçalves
 * 2010/04/15
 */

package pt.up.fe.dceg.neptus.plugins.vrp.planning;

import java.applet.Applet;
import java.awt.Color;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Enumeration;
import java.util.Vector;

import pt.up.fe.dceg.neptus.NeptusLog;
import drasys.or.geom.GeomException;
import drasys.or.geom.geo.proj.Albers;
import drasys.or.geom.geo.proj.LambertConic;
import drasys.or.geom.geo.proj.Mercator;
import drasys.or.geom.geo.proj.ProjectionI;
import drasys.or.geom.rect2.Point;
import drasys.or.geom.rect2.PointI;
import drasys.or.geom.rect2.RangeI;
import drasys.or.geom.rect2.Transform;
import drasys.or.geom.rect2.TransformI;
import drasys.or.graph.DuplicateVertexException;
import drasys.or.graph.EdgeI;
import drasys.or.graph.GraphI;
import drasys.or.graph.MatrixGraph;
import drasys.or.graph.PointGraph;
import drasys.or.graph.VertexNotFoundException;
import drasys.or.graph.vrp.BestOf;
import drasys.or.graph.vrp.ClarkeWright;
import drasys.or.graph.vrp.Composite;
import drasys.or.graph.vrp.ConstructI;
import drasys.or.graph.vrp.GillettMiller;
import drasys.or.graph.vrp.ImproveI;
import drasys.or.graph.vrp.ImproveWithTSP;
import drasys.or.graph.vrp.SolutionNotFoundException;
import drasys.or.graph.vrp.VRPException;
import drasys.or.prob.DiscreteUniformDistribution;
import drasys.or.prob.DistributionI;
import drasys.or.prob.UniformDistribution;

/**
 * @author Rui Gonçalves
 * 
 */

public class TutorialApplet extends Applet {
    private static final long serialVersionUID = 1L;

    void newButton_Clicked(Event event) {
        newCustomers(sizeOfStops);
        clear();
    }

    void Applet1_MouseUp(Event event) {
        mouseClick(event.x, event.y);
    }

    void showButton_Clicked(Event event) {
        showTours();
    }

    void clearButton_Clicked(Event event) {
        clear();
    }

    void projectionChoice_Action(Event event) {
        setProjection(projectionChoice.getSelectedItem());
        clear();
    }

    void capacityChoice_Action(Event event) {
        capacityConstraint = new Integer(capacityChoice.getSelectedItem()).intValue();
        newAlgorithm(constructType, subalgorithmType, improveType);
    }

    void rangeChoice_Action(Event event) {
        rangeConstraint = new Integer(rangeChoice.getSelectedItem()).intValue();
        newAlgorithm(constructType, subalgorithmType, improveType);
    }

    void iterationChoice_Action(Event event) {
        iterations = new Integer(iterationChoice.getSelectedItem()).intValue();
        newAlgorithm(constructType, subalgorithmType, improveType);
    }

    void strengthChoice_Action(Event event) {
        strength = new Integer(strengthChoice.getSelectedItem()).intValue();
        newAlgorithm(constructType, subalgorithmType, improveType);
    }

    void stopChoice_Action(Event event) {
        sizeOfStops = new Integer(stopChoice.getSelectedItem()).intValue();
        newCustomers(sizeOfStops);
        clear();
    }

    void tourChoice_Action(Event event) {
        tourType = tourChoice.getSelectedItem();
    }

    void improveChoice_Action(Event event) {
        improveType = improveChoice.getSelectedItem();
        newAlgorithm(constructType, subalgorithmType, improveType);
    }

    void constructChoice_Action(Event event) {
        constructType = constructChoice.getSelectedItem();
        newAlgorithm(constructType, subalgorithmType, improveType);
    }

    void subalgorithmChoice_Action(Event event) {
        subalgorithmType = subalgorithmChoice.getSelectedItem();
        newAlgorithm(constructType, subalgorithmType, improveType);
    }

    public void init() {
        super.init();

        // {{INIT_CONTROLS
        setLayout(null);
        setBackground(java.awt.Color.lightGray);
        setForeground(java.awt.Color.black);
        setSize(481, 302);
        showButton.setLabel("Show Tours");
        add(showButton);
        showButton.setBounds(384, 252, 84, 33);
        clearButton.setLabel("Clear");
        add(clearButton);
        clearButton.setBounds(312, 252, 60, 33);
        label3.setText("Tour Type");
        add(label3);
        label3.setBounds(264, 24, 84, 24);
        label2.setText("Improvement algorithm");
        label2.setAlignment(java.awt.Label.CENTER);
        add(label2);
        label2.setBounds(240, 192, 228, 24);
        label1.setText("Construction Algorithm");
        label1.setAlignment(java.awt.Label.CENTER);
        add(label1);
        label1.setBounds(12, 192, 228, 24);
        tourChoice.addItem("Closed");
        tourChoice.addItem("Inbound");
        tourChoice.addItem("Outbound");
        try {
            tourChoice.select(2);
        }
        catch (IllegalArgumentException e) {
        }
        add(tourChoice);
        tourChoice.setBounds(360, 24, 108, 25);
        improveChoice.addItem("Improve With Tsp: 2-Opt");
        improveChoice.addItem("Improve With Tsp: 3-Opt");
        improveChoice.addItem("Improve With Tsp: US-5");
        improveChoice.addItem("None");
        try {
            improveChoice.select(0);
        }
        catch (IllegalArgumentException e) {
        }
        add(improveChoice);
        improveChoice.setBounds(252, 216, 216, 25);
        constructChoice.addItem("Clarke&Wright - savings list");
        constructChoice.addItem("Gillett&Miller - polar sweep");
        constructChoice.addItem("Best Of Above");
        try {
            constructChoice.select(0);
        }
        catch (IllegalArgumentException e) {
        }
        add(constructChoice);
        constructChoice.setBounds(12, 216, 228, 25);
        messageLabel.setAlignment(java.awt.Label.CENTER);
        add(messageLabel);
        messageLabel.setBounds(252, 156, 228, 12);
        newButton.setLabel("New");
        add(newButton);
        newButton.setBounds(252, 252, 48, 33);
        capacityChoice.addItem("300");
        capacityChoice.addItem("400");
        capacityChoice.addItem("500");
        capacityChoice.addItem("700");
        capacityChoice.addItem("900");
        capacityChoice.addItem("1100");
        capacityChoice.addItem("1300");
        capacityChoice.addItem("1500");
        try {
            capacityChoice.select(2);
        }
        catch (IllegalArgumentException e) {
        }
        add(capacityChoice);
        capacityChoice.setBounds(360, 72, 108, 25);
        label4.setText("Max Capacity");
        add(label4);
        label4.setBounds(264, 72, 84, 24);
        label5.setText("Customers");
        add(label5);
        label5.setBounds(264, 96, 84, 24);
        stopChoice.addItem("15");
        stopChoice.addItem("20");
        stopChoice.addItem("30");
        stopChoice.addItem("50");
        try {
            stopChoice.select(0);
        }
        catch (IllegalArgumentException e) {
        }
        add(stopChoice);
        stopChoice.setBounds(360, 96, 108, 25);
        label6.setText("Iterations");
        label6.setAlignment(java.awt.Label.CENTER);
        add(label6);
        label6.setBounds(108, 240, 72, 24);
        iterationChoice.addItem("0");
        iterationChoice.addItem("5");
        iterationChoice.addItem("10");
        iterationChoice.addItem("15");
        try {
            iterationChoice.select(1);
        }
        catch (IllegalArgumentException e) {
        }
        add(iterationChoice);
        iterationChoice.setBounds(108, 264, 72, 25);
        strengthChoice.addItem("1");
        strengthChoice.addItem("2");
        strengthChoice.addItem("3");
        strengthChoice.addItem("4");
        strengthChoice.addItem("5");
        strengthChoice.addItem("6");
        strengthChoice.addItem("7");
        try {
            strengthChoice.select(4);
        }
        catch (IllegalArgumentException e) {
        }
        add(strengthChoice);
        strengthChoice.setBounds(180, 264, 60, 25);
        label7.setText("Strength");
        label7.setAlignment(java.awt.Label.CENTER);
        add(label7);
        label7.setBounds(180, 240, 60, 24);
        rangeChoice.addItem("100");
        rangeChoice.addItem("150");
        rangeChoice.addItem("200");
        rangeChoice.addItem("250");
        rangeChoice.addItem("300");
        rangeChoice.addItem("325");
        rangeChoice.addItem("350");
        rangeChoice.addItem("375");
        rangeChoice.addItem("400");
        rangeChoice.addItem("425");
        rangeChoice.addItem("450");
        rangeChoice.addItem("475");
        rangeChoice.addItem("500");
        try {
            rangeChoice.select(7);
        }
        catch (IllegalArgumentException e) {
        }
        add(rangeChoice);
        rangeChoice.setBounds(360, 48, 108, 25);
        label8.setText("Max Range");
        add(label8);
        label8.setBounds(264, 48, 84, 24);
        subalgorithmChoice.addItem("2-Opt");
        subalgorithmChoice.addItem("3-Opt");
        subalgorithmChoice.addItem("US-5");
        subalgorithmChoice.addItem("None");
        try {
            subalgorithmChoice.select(3);
        }
        catch (IllegalArgumentException e) {
        }
        add(subalgorithmChoice);
        subalgorithmChoice.setBounds(12, 264, 96, 25);
        label9.setText("Subalgorithm");
        label9.setAlignment(java.awt.Label.CENTER);
        add(label9);
        label9.setBounds(12, 240, 96, 24);
        projectionChoice.addItem("Albers");
        projectionChoice.addItem("Mercator");
        projectionChoice.addItem("Lambert Conic");
        try {
            projectionChoice.select(1);
        }
        catch (IllegalArgumentException e) {
        }
        add(projectionChoice);
        projectionChoice.setBounds(360, 120, 108, 25);
        label10.setText("Map Projection");
        add(label10);
        label10.setBounds(264, 120, 84, 24);
        // }}

        initTutorial();

    }

    @SuppressWarnings("deprecation")
    public boolean handleEvent(Event event) {
        if (event.target == constructChoice && event.id == Event.ACTION_EVENT) {
            constructChoice_Action(event);
            return true;
        }
        if (event.target == subalgorithmChoice && event.id == Event.ACTION_EVENT) {
            subalgorithmChoice_Action(event);
            return true;
        }
        if (event.target == improveChoice && event.id == Event.ACTION_EVENT) {
            improveChoice_Action(event);
            return true;
        }
        if (event.target == tourChoice && event.id == Event.ACTION_EVENT) {
            tourChoice_Action(event);
            return true;
        }
        if (event.target == capacityChoice && event.id == Event.ACTION_EVENT) {
            capacityChoice_Action(event);
            return true;
        }
        if (event.target == projectionChoice && event.id == Event.ACTION_EVENT) {
            projectionChoice_Action(event);
            return true;
        }
        if (event.target == rangeChoice && event.id == Event.ACTION_EVENT) {
            rangeChoice_Action(event);
            return true;
        }
        if (event.target == iterationChoice && event.id == Event.ACTION_EVENT) {
            iterationChoice_Action(event);
            return true;
        }
        if (event.target == strengthChoice && event.id == Event.ACTION_EVENT) {
            strengthChoice_Action(event);
            return true;
        }
        if (event.target == stopChoice && event.id == Event.ACTION_EVENT) {
            stopChoice_Action(event);
            return true;
        }
        if (event.target == clearButton && event.id == Event.ACTION_EVENT) {
            clearButton_Clicked(event);
            return true;
        }
        if (event.target == showButton && event.id == Event.ACTION_EVENT) {
            showButton_Clicked(event);
            return true;
        }
        if (event.target == this && event.id == Event.MOUSE_UP) {
            Applet1_MouseUp(event);
            return true;
        }
        if (event.target == newButton && event.id == Event.ACTION_EVENT) {
            newButton_Clicked(event);
            return true;
        }
        return super.handleEvent(event);
    }

    // {{DECLARE_CONTROLS
    java.awt.Button showButton = new java.awt.Button();
    java.awt.Button clearButton = new java.awt.Button();
    java.awt.Label label3 = new java.awt.Label();
    java.awt.Label label2 = new java.awt.Label();
    java.awt.Label label1 = new java.awt.Label();
    java.awt.Choice tourChoice = new java.awt.Choice();
    java.awt.Choice improveChoice = new java.awt.Choice();
    java.awt.Choice constructChoice = new java.awt.Choice();
    java.awt.Label messageLabel = new java.awt.Label();
    java.awt.Button newButton = new java.awt.Button();
    java.awt.Choice capacityChoice = new java.awt.Choice();
    java.awt.Label label4 = new java.awt.Label();
    java.awt.Label label5 = new java.awt.Label();
    java.awt.Choice stopChoice = new java.awt.Choice();
    java.awt.Label label6 = new java.awt.Label();
    java.awt.Choice iterationChoice = new java.awt.Choice();
    java.awt.Choice strengthChoice = new java.awt.Choice();
    java.awt.Label label7 = new java.awt.Label();
    java.awt.Choice rangeChoice = new java.awt.Choice();
    java.awt.Label label8 = new java.awt.Label();
    java.awt.Choice subalgorithmChoice = new java.awt.Choice();
    java.awt.Label label9 = new java.awt.Label();
    java.awt.Choice projectionChoice = new java.awt.Choice();
    java.awt.Label label10 = new java.awt.Label();
    // }}

    Image image;
    GraphI graph;
    Composite vrp;
    String tourType;
    TransformI screenTransform;
    RangeI xyRange;
    int sizeOfStops, iterations, strength;
    double rangeConstraint;
    double capacityConstraint;
    String improveType;
    String constructType;
    String subalgorithmType;
    Customer[] customers = null;
    ProjectionI projection;
    drasys.or.geom.geo.RangeI geoRange;
    DistributionI lonDist, latDist, loadDist;

    public void initTutorial() {
        image = getImage(getDocumentBase(), "images/menus/tip.png");
        tourType = "Outbound";
        improveType = "Improve With Tsp: 2-Opt";
        constructType = "Clarke&Wright - savings list";
        subalgorithmType = "None";
        strength = 5;
        iterations = 10;
        sizeOfStops = 15;
        rangeConstraint = 475;
        capacityConstraint = 500;
        messageLabel.setText("Click to show Lon,Lat");
        newAlgorithm(constructType, subalgorithmType, improveType);

        geoRange = new drasys.or.geom.geo.Range(160, -6.5, 162, -5);
        lonDist = new UniformDistribution(geoRange.west(), geoRange.east(), 123);
        latDist = new UniformDistribution(geoRange.south(), geoRange.north(), 321);
        loadDist = new DiscreteUniformDistribution(100, 100, 3);
        setProjection("Mercator");

    }

    public void setProjection(String projectionStr) {
        try {

            if (projectionStr.equals("Mercator"))
                projection = new Mercator(geoRange);
            else if (projectionStr.equals("Albers"))
                projection = new Albers(geoRange);
            else if (projectionStr.equals("Lambert Conic"))
                projection = new LambertConic(geoRange);
            else
                NeptusLog.pub().info("<###>Can't find: " + projectionStr);

            xyRange = projection.forward(geoRange);
            PointI xyLowerLeft = projection.forward(geoRange.southwest());
            PointI xyUpperRight = projection.forward(geoRange.northeast());
            PointI screenLowerLeft = new Point(10, 150);
            PointI screenUpperRight = new Point(240, 10);
            screenTransform = new Transform(xyLowerLeft, xyUpperRight, screenLowerLeft, screenUpperRight);
            newCustomers(sizeOfStops);

        }
        catch (GeomException e) {
        }
    }

    public void newCustomers(int n) {
        try {

            customers = new Customer[n + 1];
            PointGraph pointGraph = new PointGraph();
            drasys.or.geom.geo.PointI geoPoint;
            PointI xyPoint, screenPoint;

            Object key = "Depot";
            double load = 0;

            double x = lonDist.getRandomScaler();
            double y = latDist.getRandomScaler();

            for (int i = 0; i <= 3;) {
                load = 0;// loadDist.getRandomScaler();
                geoPoint = new drasys.or.geom.geo.Point(x, y += 0.1);
                // geoPoint = new drasys.or.geom.geo.Point(lonDist.getRandomScaler(), latDist.getRandomScaler());
                xyPoint = projection.forward(geoPoint);
                xyPoint.x();
                if (!xyRange.includes(xyPoint))
                    continue;
                screenPoint = screenTransform.forward(xyPoint);
                customers[i] = new Customer(load, xyPoint, screenPoint);
                pointGraph.addVertex(key, customers[i]);
                key = new String("Depot" + i);// Integer(i);

                i++;
            }

            for (int i = 4; i <= 7;) {
                // geoPoint = new drasys.or.geom.geo.Point(x, y+=0.1);
                load = 2;// loadDist.getRandomScaler();
                geoPoint = new drasys.or.geom.geo.Point(lonDist.getRandomScaler(), latDist.getRandomScaler());
                xyPoint = projection.forward(geoPoint);
                xyPoint.x();
                if (!xyRange.includes(xyPoint))
                    continue;
                screenPoint = screenTransform.forward(xyPoint);
                customers[i] = new Customer(load, xyPoint, screenPoint);

                pointGraph.addVertex(key, customers[i]);
                key = new String("Depot" + i);// Integer(i);

                i++;
            }

            for (int i = 8; i < n;) {
                load = 0;// loadDist.getRandomScaler();
                geoPoint = new drasys.or.geom.geo.Point(lonDist.getRandomScaler(), latDist.getRandomScaler());
                xyPoint = projection.forward(geoPoint);
                if (!xyRange.includes(xyPoint))
                    continue;
                screenPoint = screenTransform.forward(xyPoint);
                customers[i] = new Customer(load, xyPoint, screenPoint);
                pointGraph.addVertex(key, customers[i]);
                key = new String("Depot" + i);// Integer(i);

                i++;
            }
            graph = new MatrixGraph(pointGraph, null);
            graph.setSymmetric(false);

        }

        catch (GeomException e) {
        }
        catch (DuplicateVertexException e) {
        }
    }

    public void newAlgorithm(String constructStr, String subalgorithmStr, String improveStr) {
        try {

            drasys.or.graph.tsp.ImproveI subalgorithm = null;
            if (subalgorithmStr.equals("None"))
                subalgorithm = null;
            else if (subalgorithmStr.equals("2-Opt"))
                subalgorithm = new drasys.or.graph.tsp.TwoOpt();
            else if (subalgorithmStr.equals("3-Opt"))
                subalgorithm = new drasys.or.graph.tsp.ThreeOpt();
            else if (subalgorithmStr.equals("US-5"))
                subalgorithm = new drasys.or.graph.tsp.Us(5);
            else
                NeptusLog.pub().info("<###>Can't find: " + subalgorithmStr);

            ConstructI construct = null;
            if (constructStr.equals("Clarke&Wright - savings list"))
                construct = new ClarkeWright(iterations, strength, subalgorithm);
            else if (constructStr.equals("Gillett&Miller - polar sweep"))
                construct = new GillettMiller(iterations, strength, subalgorithm);
            else if (constructStr.equals("Best Of Above")) {
                BestOf bestOf = new BestOf();
                bestOf.addConstruct(new ClarkeWright(iterations, strength, subalgorithm));
                bestOf.addConstruct(new GillettMiller(iterations, strength, subalgorithm));
                construct = bestOf;
            }
            else
                NeptusLog.pub().info("<###>Can't find: " + constructStr);

            ImproveI improve = null;
            if (improveStr.equals("None"))
                improve = null;
            else if (improveStr.equals("Improve With Tsp: 2-Opt"))
                improve = new ImproveWithTSP(new drasys.or.graph.tsp.TwoOpt());
            else if (improveStr.equals("Improve With Tsp: 3-Opt"))
                improve = new ImproveWithTSP(new drasys.or.graph.tsp.ThreeOpt());
            else if (improveStr.equals("Improve With Tsp: US-5"))
                improve = new ImproveWithTSP((drasys.or.graph.tsp.ImproveI) new drasys.or.graph.tsp.Us(5));
            else
                NeptusLog.pub().info("<###>Can't find: " + improveStr);

            vrp = new Composite(construct, improve);
            vrp.setCostConstraint(1000);
            vrp.setCapacityConstraint(2 /* capacityConstraint */);

        }
        catch (VRPException e) {
        }
    }

    public void clear() {
        update(getGraphics());
        messageLabel.setText("Click to show Lon,Lat");
    }

    public Vector<?>[] buildTour(GraphI graph) throws VertexNotFoundException, SolutionNotFoundException {
        vrp.setGraph(graph);
        if (tourType.equals("Closed"))
            vrp.constructClosedTours("Depot");
        else if (tourType.equals("Inbound"))
            vrp.constructInboundTours("Depot");
        else if (tourType.equals("Outbound")) {
            vrp.constructOutboundTours("Depot");
        }
        return vrp.getTours();
    }

    public void showTours() {
        messageLabel.setText("-- Thinking --");
        update(this.getGraphics());

        try {
            Vector<?>[] tours = buildTour(graph);
            paintTours(tours);
        }
        catch (VertexNotFoundException e) {
            messageLabel.setText("Can't find the depot.");
            System.out.println(e.getMessage());
        }
        catch (SolutionNotFoundException e) {
            messageLabel.setText("Can't find a solution (see console msg).");
            System.out.println(e.getMessage());
        }
    }

    public void mouseClick(int x, int y) {
        try {
            PointI projectedPoint = screenTransform.inverse(new Point(x, y));
            projectedPoint = xyRange.bound(projectedPoint);
            drasys.or.geom.geo.PointI geoPoint = projection.inverse(projectedPoint);
            String lon = ("" + (geoPoint.longitude() + 5.0E-7)).substring(0, 10);
            String lat = ("" + (geoPoint.latitude() - 5.0E-7)).substring(0, 9);
            messageLabel.setText("Lon,Lat = " + lon + ", " + lat);
        }
        catch (GeomException e) {
        }
    }

    public void paint(Graphics g) {
        if (image != null)
            g.drawImage(image, 0, 0, this);
        paintcustomers(g);
    }

    public int totalDist(Vector<?>[] tours) throws SolutionNotFoundException {
        int meters = 0;
        for (int i = 0; i < tours.length; i++) {
            Enumeration<?> e = tours[i].elements();
            e.nextElement(); // Skip Vertex
            while (e.hasMoreElements()) {
                EdgeI edge = (EdgeI) e.nextElement();
                Customer customer1 = (Customer) edge.getToVertex().getValue();
                Customer customer2 = (Customer) edge.getFromVertex().getValue();
                meters += customer1.distanceTo(customer2);
                e.nextElement(); // Skip Vertex
            }
        }
        String msg = "Vehicles - " + tours.length + ", ";
        msg += "Distance(Km) - " + meters / 1000;
        messageLabel.setText(msg);

        return meters / 1000;
    }

    public void paintTours(Vector<?>[] tours) throws SolutionNotFoundException {
        int meters = 0;
        Graphics g = this.getGraphics();
        g.setColor(Color.blue);
        for (int i = 0; i < tours.length; i++) {
            Enumeration<?> e = tours[i].elements();
            e.nextElement(); // Skip Vertex
            while (e.hasMoreElements()) {
                EdgeI edge = (EdgeI) e.nextElement();
                Customer customer1 = (Customer) edge.getToVertex().getValue();
                Customer customer2 = (Customer) edge.getFromVertex().getValue();
                meters += customer1.distanceTo(customer2);
                int x1 = (int) customer1.screenPoint.x();
                int y1 = (int) customer1.screenPoint.y();
                int x2 = (int) customer2.screenPoint.x();
                int y2 = (int) customer2.screenPoint.y();
                g.drawLine(x1, y1, x2, y2);
                e.nextElement(); // Skip Vertex
            }
        }
        String msg = "Vehicles - " + tours.length + ", ";
        msg += "Distance(Km) - " + meters / 1000;
        messageLabel.setText(msg);

    }

    public void paintcustomers(Graphics g) {
        Color[] colors = { Color.black, Color.green, Color.yellow, Color.red };

        // 1º
        Customer customer = customers[0];
        g.setColor(colors[3]);
        int x = (int) customer.screenPoint.x();
        int y = (int) customer.screenPoint.y();
        g.fillOval(x - 2, y - 2, 4, 4);

        for (int i = 1; i < customers.length; i++) {
            customer = customers[i];
            g.setColor(colors[(int) customer.load/* 100 */]);
            x = (int) customer.screenPoint.x();
            y = (int) customer.screenPoint.y();
            g.fillOval(x - 2, y - 2, 4, 4);
        }
    }

    public void update(Graphics g) {
        paint(g);
    }
}
