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
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.HashSet;

import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.AreaSurvey;
import pt.lsts.neptus.mp.maneuvers.ManeuversUtil;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;
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
        pivot.setManeuverLocation(new ManeuverLocation(clickedLocation));
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
        // FIXME
        return false;
    }

    @Override
    public void translate(double offsetNorth, double offsetEast) {
        area.translate(offsetNorth, offsetEast);
        updateManeuver();
    }

    @Override
    public void setYaw(double yawRads) {
        updateManeuver();
    }

    @Override
    public void rotate(double amountRads) {
        area.rotate(amountRads);
        System.out.println("rotate");
        updateManeuver();
    }

    @Override
    public void growWidth(double amount) {
        System.out.println("grow width");
        updateManeuver();
    }

    @Override
    public void growLength(double amount) {
        System.out.println("grow length");
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
        LocationType loc = new LocationType(getCenterLocation());
        loc.convertToAbsoluteLatLonDepth();
        // FIXME
        // double length = area.getLength(), width = area.getWidth(), bearing = area.getYawDeg();
        return "";//String.format("polygon %s %s %f %f %f %f %f %s", getName(), isFirstPriority(),
                  // loc.getLatitudeDegs(),
                  // loc.getLongitudeDegs(), length, width, bearing, getRequiredPayloads());
    }

    @Override
    public void unmarshall(String data) throws IOException {
        // FIXME
        // Scanner input = new Scanner(data);
        // input.next("[\\w]+");
        // this.name = input.next("[\\w]+");
        // this.firstPriority = input.nextBoolean();
        // double latDegs = input.nextDouble();
        // double lonDegs = input.nextDouble();
        // area.setCenterLocation(new LocationType(latDegs, lonDegs));
        // area.setLength(input.nextDouble());
        // area.setWidth(input.nextDouble());
        // area.setYawDeg(input.nextDouble());
        // String[] payloads = input.nextLine().replaceAll("[\\[\\]]", "").trim().split("[, ]+");
        // getRequiredPayloads().clear();
        // for (String p : payloads)
        // getRequiredPayloads().add(PayloadRequirement.valueOf(p));
        // updateManeuver();
        // input.close();
    }

    public static void main(String[] args) throws Exception {
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

}
