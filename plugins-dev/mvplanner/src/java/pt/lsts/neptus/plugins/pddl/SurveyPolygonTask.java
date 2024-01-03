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
 * Nov 25, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.bind.JAXB;

import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.AreaSurvey;
import pt.lsts.neptus.mp.maneuvers.ManeuversUtil;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;
import pt.lsts.neptus.types.coord.PolygonType.Vertex;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class SurveyPolygonTask extends MVPlannerTask {

    private AreaSurvey pivot = new AreaSurvey();
    private PolygonType area = pivot.getPolygon();
    MarkElement entry = new MarkElement(), exit = new MarkElement();

    public SurveyPolygonTask(LocationType clickedLocation) {
        pivot = new AreaSurvey();
        ManeuverLocation loc = new ManeuverLocation(clickedLocation);
        loc.setDepth(2);
        pivot.setManeuverLocation(loc);
        area = new PolygonType();
        area.setColor(Color.yellow);

        LocationType nw = new LocationType(clickedLocation), ne = new LocationType(clickedLocation),
                sw = new LocationType(clickedLocation), se = new LocationType(clickedLocation);

        nw.translatePosition(60, -60, 0);
        ne.translatePosition(60, 60, 0);
        sw.translatePosition(-60, -60, 0);
        se.translatePosition(-60, 60, 0);
        area.addVertex(nw);
        area.addVertex(ne);
        area.addVertex(se);
        area.addVertex(sw);
        area.recomputePath();

        entry.setId(getName() + "_entry");
        exit.setId(getName() + "_exit");
        updateManeuver();
    }

    public LocationType getEntryPoint() {
        pivot.endManeuver();
        return pivot.getStartLocation().convertToAbsoluteLatLonDepth();
    }

    public LocationType getEndPoint() {
        return pivot.getEndLocation().convertToAbsoluteLatLonDepth();
    }

    @Override
    public boolean containsPoint(LocationType lt, StateRenderer2D renderer) {
        if (area.containsPoint(lt))
            return true;

        Point2D screen = renderer.getScreenPosition(lt);
        for (PolygonType.Vertex v : area.getVertices()) {
            Point2D pt = renderer.getScreenPosition(v.getLocation());

            if (pt.distance(screen) < 10) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void translate(double offsetNorth, double offsetEast) {
        pivot.translate(offsetNorth, offsetEast, 0);
    }

    @Override
    public void setYaw(double yawRads) {
        updateManeuver();
    }

    @Override
    public void rotate(double amountRads) {
        area.rotate(amountRads);
        updateManeuver();
    }

    @Override
    public void growWidth(double amount) {
        updateManeuver();
    }

    @Override
    public void growLength(double amount) {
        updateManeuver();
    }

    @Override
    public void setRequiredPayloads(HashSet<PayloadRequirement> payloads) {
        super.setRequiredPayloads(payloads);
        updateManeuver();
    }

    private void updateManeuver() {
        double minHorStep = area.getDiameter() / 3;
        double minDepth = Double.MAX_VALUE;
        double maxDepth = -Double.MAX_VALUE;
        for (PayloadRequirement p : getRequiredPayloads()) {
            minHorStep = Math.min(minHorStep, p.getSwathWidth());
            if (p.getMinDepth() < 0)
                minDepth = p.getMinDepth();
            else
                minDepth = Math.min(minDepth, p.getMinDepth());

            if (p.getMaxDepth() < 0)
                maxDepth = p.getMaxDepth();
            else
                maxDepth = Math.max(maxDepth, p.getMaxDepth());
        }
        entry.setCenterLocation(getEntryPoint());
        exit.setCenterLocation(getEndPoint());
        pivot.setPolygon(area);
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        Graphics2D copy = (Graphics2D) g.create();
        area.setColor(new Color(128, 180, 128, 64));
        area.paint((Graphics2D) g.create(), renderer);
        g.setColor(Color.magenta);
        for (Vertex v : area.getVertices()) {
            Point2D pt = renderer.getScreenPosition(v.getLocation());
            g.fill(new Ellipse2D.Double(pt.getX() - 5, pt.getY() - 5, 10, 10));
        }

        Point2D pt = renderer.getScreenPosition(pivot.getManeuverLocation());

        copy.translate(pt.getX(), pt.getY());
        copy.rotate(-renderer.getRotation() - Math.PI / 2);
        ManeuversUtil.paintPointLineList(copy, renderer.getZoom(), pivot.getPathPoints(), false, 0);
        String payloads = getPayloadsAbbreviated();
        loadImages();
        if (getAssociatedAllocation() == null)
            g.drawImage(orangeLed, (int) pt.getX() - 8, (int) pt.getY() - 8, null);
        else
            g.drawImage(greenLed, (int) pt.getX() - 8, (int) pt.getY() - 8, null);
        g.setColor(Color.black);
        g.drawString(getName() + " (" + payloads + ")", (int) pt.getX() + 8, (int) pt.getY() + 8);
        if (getAssociatedAllocation() != null)
            g.setColor(Color.green.brighter().brighter());
        else
            g.setColor(Color.orange);
        g.drawString(getName() + " (" + payloads + ")", (int) pt.getX() + 7, (int) pt.getY() + 7);

    }

    public double getLength() {
        return pivot.getDistanceTravelled(getEntryPoint());
    }

    @Override
    public LocationType getCenterLocation() {
        return area.getCentroid();
    }

    /**
     * @return the pivot
     */
    public final AreaSurvey getPivot() {
        return pivot;
    }

    @Override
    public String marshall() throws IOException {

        SurveyPolygonModel model = new SurveyPolygonModel();
        model.firstPriority = isFirstPriority();
        model.name = getName();
        model.payloads.addAll(getRequiredPayloads());
        LocationType loc = new LocationType(pivot.getManeuverLocation()).convertToAbsoluteLatLonDepth();
        model.startLocation = new Point2D.Double(loc.getLatitudeDegs(), loc.getLongitudeDegs());
        area.getVertices().stream().forEach(v -> {
            model.points.add(new Point2D.Double(v.getLatitudeDegs(), v.getLongitudeDegs()));
        });
        StringWriter writer = new StringWriter();
        JAXB.marshal(model, writer);
        return writer.toString();
    }

    @Override
    public Collection<MVPlannerTask> splitTask(double maxLength) {
        ArrayList<MVPlannerTask> surveys = new ArrayList<>();
        double horStep = area.getDiameter() / 3;
        for (PayloadRequirement p : getRequiredPayloads())
            horStep = Math.min(horStep, p.getSwathWidth());

        final double swathWidth = horStep;

        int numAreas = 1;
        double curLength = area.getPathLength(horStep, 0);
        double angle = area.getDiameterAndAngle().second();
        ArrayList<PolygonType> polygons = new ArrayList<>();

        if (curLength < maxLength) {
            surveys.add(this);
            return surveys;
        }

        while (curLength > maxLength) {
            numAreas++;
            polygons.clear();
            int horSplits = numAreas / 3 + 1;

            if (horSplits > 1) {
                ArrayList<PolygonType> polygonsHor = area.subAreas(horSplits, angle + Math.PI / 2);

                for (PolygonType p : polygonsHor) {
                    polygons.addAll(p.subAreas((int) Math.ceil((double) numAreas / horSplits), angle));
                }
            }
            else {
                polygons.addAll(area.subAreas(numAreas, angle));
            }

            curLength = 0;
            for (PolygonType p : polygons)
                curLength = Math.max(curLength, p.getPathLength(swathWidth, 0));
        }

        for (int i = 0; i < polygons.size(); i++) {
            PolygonType p = polygons.get(i);
            SurveyPolygonTask task = new SurveyPolygonTask(p.getCentroid());
            task.area = p;
            task.requiredPayloads = getRequiredPayloads();
            task.updateManeuver();
            surveys.add(task);
        }

        return surveys;
    }

    @Override
    public void unmarshall(String data) throws IOException {
        SurveyPolygonModel model = JAXB.unmarshal(new StringReader(data), SurveyPolygonModel.class);

        setFirstPriority(model.firstPriority);
        name = model.name;
        requiredPayloads.clear();
        requiredPayloads.addAll(model.payloads);
        area.clearVertices();
        model.points.stream().forEach(p -> {
            area.addVertex(new LocationType(p.x, p.y));
        });
        area.recomputePath();
        pivot.setManeuverLocation(new ManeuverLocation(new LocationType(model.startLocation.x, model.startLocation.y)));
        pivot.setPolygon(area);
        pivot.recalcPoints();
    }

    LocationType lastPoint = null;
    Point2D lastScreenPoint = null;
    Vertex clickedVertex = null;

    @Override
    public void mouseDragged(MouseEvent e, StateRenderer2D renderer) {
        double xamount = e.getX() - lastScreenPoint.getX();
        if (e.isShiftDown()) {
            rotate(xamount / 40.0);
            lastScreenPoint = e.getPoint();
            return;
        }
        if (clickedVertex != null) {
            clickedVertex.setLocation(renderer.getRealWorldLocation(e.getPoint()));
            area.recomputePath();
            pivot.recalcPoints();
        }
        else if (e.isControlDown() && clickedVertex == null) {

        }
        else if (lastPoint != null) {
            LocationType loc = renderer.getRealWorldLocation(e.getPoint());
            double offsets[] = loc.getOffsetFrom(lastPoint);
            translate(offsets[0], offsets[1]);
        }

        lastPoint = renderer.getRealWorldLocation(e.getPoint());
        lastScreenPoint = e.getPoint();
    }

    @Override
    public void mouseMoved(MouseEvent e, StateRenderer2D renderer) {
        // System.out.println(getName() + " Mouse moved");
    }

    @Override
    public void mousePressed(MouseEvent e, StateRenderer2D renderer) {
        // System.out.println(getName() + " Mouse pressed");
        lastPoint = renderer.getRealWorldLocation(e.getPoint());
        lastScreenPoint = e.getPoint();

        for (int i = 0; i < area.getVertices().size(); i++) {
            PolygonType.Vertex v = area.getVertices().get(i);
            Point2D pt = renderer.getScreenPosition(v.getLocation());
            if (pt.distance(e.getPoint()) < 15) {
                clickedVertex = v;
                
                if (e.isControlDown()) {
                    area.addVertex(i, v.getLocation());
                }
                return;
            }
        }

        clickedVertex = null;

    }

    @Override
    public void mouseReleased(MouseEvent e, StateRenderer2D renderer) {

        System.out.println(clickedVertex);
//        if (clickedVertex != null && e.isControlDown()) {
//            area.removeVertex(clickedVertex);
//        }
            
        
        lastPoint = null;
        lastScreenPoint = null;
        clickedVertex = null;
    }

    public static void main(String[] args) throws Exception {
        SurveyPolygonModel model = new SurveyPolygonModel();
        model.startLocation = new Point2D.Double(41, -8);
        model.name = "This is the name!";
        model.payloads.add(PayloadRequirement.sidescan);
        model.payloads.add(PayloadRequirement.camera);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JAXB.marshal(model, baos);
        SurveyPolygonModel m2 = JAXB.unmarshal(new ByteArrayInputStream(baos.toByteArray()), SurveyPolygonModel.class);
        JAXB.marshal(model, System.out);
        JAXB.marshal(m2, System.out);

        SurveyPolygonTask task = new SurveyPolygonTask(new LocationType(41, -8.0002));
        task.rotate(Math.toDegrees(10));
        task.requiredPayloads.add(PayloadRequirement.sidescan);
        task.updateManeuver();
        System.out.println(task.marshall());
        SurveyPolygonTask task2 = new SurveyPolygonTask(new LocationType());
        task2.unmarshall(task.marshall());
        System.out.println(task2.marshall());
        StateRenderer2D renderer = new StateRenderer2D(new LocationType(41, -8));
        renderer.addPostRenderPainter(task, "task");
        GuiUtils.testFrame(renderer);

    }

    private static class SurveyPolygonModel implements Serializable {
        private static final long serialVersionUID = 4771888511610759920L;
        public ArrayList<Point2D.Double> points = new ArrayList<>();
        public Point2D.Double startLocation = null;
        public String name = "";
        public boolean firstPriority = false;
        public ArrayList<PayloadRequirement> payloads = new ArrayList<>();
    }

}
