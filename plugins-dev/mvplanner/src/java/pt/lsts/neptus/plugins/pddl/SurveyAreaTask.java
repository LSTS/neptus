/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Scanner;

import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.ManeuversUtil;
import pt.lsts.neptus.mp.maneuvers.RowsManeuver;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class SurveyAreaTask extends MVPlannerTask {

    private RowsManeuver pivot = new RowsManeuver();
    private ParallelepipedElement area = null;
    MarkElement entry = new MarkElement(), exit = new MarkElement();
    
    public SurveyAreaTask(LocationType clickedLocation) {
        area = new ParallelepipedElement();
        area.setColor(Color.yellow);
        area.setCenterLocation(clickedLocation);
        area.setWidth(75);
        area.setLength(75);
        area.setYawDeg(0);
        entry.setId(getName()+"_entry");
        exit.setId(getName()+"_exit");
        updateManeuver();
    }

    private SurveyAreaTask(ParallelepipedElement area, String name) {
        this.area = area;
        this.name = name;
        entry.setId(getName() + "_entry");
        exit.setId(getName() + "_exit");
        updateManeuver();
    }
    
    public void setSize(double width, double length, double bearingRads) {
        area.setLength(length);
        area.setWidth(width);
        area.setYaw(bearingRads);
        updateManeuver();
    }
    
    public LocationType getEntryPoint() {
        pivot.endManeuver();
        return pivot.getStartLocation().convertToAbsoluteLatLonDepth();
    }
    
    public LocationType getEndPoint() {
        return pivot.getEndLocation().convertToAbsoluteLatLonDepth();
    }

    public double getWidth() {
        return area.getWidth();
    }

    public double getHeight() {
        return area.getWidth();
    }

    /**
     * Split this survey in n other areas
     * */
    @Override
    public Collection<MVPlannerTask> splitTask(double newLength) {
        int n = (int) Math.round(this.getLength() / newLength);
        ArrayList<MVPlannerTask> surveys = new ArrayList<>();

        // compute new area dimensions
        double newAreaWidth = area.getWidth();
        double newAreaLength = area.getLength();

        boolean horizontalSplit = false;
        if(area.getWidth() > area.getLength())
            newAreaWidth = area.getWidth() / n;
        else {
            newAreaLength = area.getLength() / n;
            horizontalSplit = true;
        }

        LocationType pivot = getEntryPoint();
        LocationType origin = new LocationType(pivot);
        origin.translatePosition(newAreaLength/2, newAreaWidth/2, 0);


        for(int i = 0; i < n; i++) {
            LocationType newCenter = new LocationType(origin);
            if(horizontalSplit)
                newCenter.translatePosition(newAreaLength * i, 0, 0);
            else
                newCenter.translatePosition(0, newAreaWidth * i, 0);

            double offsets[] = pivot.getOffsetFrom(newCenter);
            double deltas[] = AngleUtils.rotate(area.getYawRad(), offsets[0], offsets[1], false);
            newCenter.translatePosition(offsets[0] - deltas[0], offsets[1] -deltas[1], 0);

            ParallelepipedElement newArea = new ParallelepipedElement();
            newArea.setCenterLocation(newCenter);
            newArea.setWidth(newAreaWidth);
            newArea.setLength(newAreaLength);
            newArea.setYawDeg(area.getYawRad());

            String id;
            if(i == 0)
                id = getName();
            else
                id = getName() + "_p" + i;

            SurveyAreaTask newTask = new SurveyAreaTask(newArea, id);
            newTask.setYaw(area.getYawRad());
            newTask.updateManeuver();
            newTask.setRequiredPayloads(newTask.getRequiredPayloads());
            newTask.setProperties(getProperties());

            surveys.add(newTask);
        }

        this.area = ((SurveyAreaTask)surveys.get(0)).area;
        this.updateManeuver();

        return surveys;
    }
    
    @Override
    public boolean containsPoint(LocationType lt, StateRenderer2D renderer) {
        return area.containsPoint(lt, renderer);
    }
    
    @Override
    public void translate(double offsetNorth, double offsetEast) {
        LocationType cur = area.getCenterLocation();
        cur.translatePosition(offsetNorth, offsetEast, 0);
        area.setCenterLocation(cur.convertToAbsoluteLatLonDepth());
        updateManeuver();
    }
    
    @Override
    public void setYaw(double yawRads) {
        area.setYaw(Math.toDegrees(yawRads));
        updateManeuver();
    }
    
    @Override
    public void rotate(double amountRads) {
        area.setYaw(area.getYaw()+amountRads);
        updateManeuver();
    }
    
    @Override
    public void growWidth(double amount) {
        area.setWidth(area.getWidth()+amount);
        area.setWidth(Math.max(area.getWidth(), pivot.getHstep()));
        updateManeuver();
    }
    
    @Override
    public void growLength(double amount) {
        area.setLength(area.getLength()+amount);
        area.setLength(Math.max(area.getLength(), pivot.getHstep()));
        updateManeuver();
    }
    
    @Override
    public void setRequiredPayloads(HashSet<PayloadRequirement> payloads) {
        super.setRequiredPayloads(payloads);
        updateManeuver();
    }
    
    private void updateManeuver() {
        double minHorStep = area.getWidth()/3;
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
        LocationType start = new LocationType(area.getCenterLocation());
        double offsetNorth = area.getWidth()/2 * Math.cos(area.getYawRad()-Math.PI/2);
        double offsetEast = area.getWidth()/2 * Math.sin(area.getYawRad()-Math.PI/2);
        start.translatePosition(offsetNorth, offsetEast, 0);
        start.convertToAbsoluteLatLonDepth();
        
        offsetNorth = (area.getLength()/2) * Math.cos(area.getYawRad()+Math.PI);
        offsetEast = (area.getLength()/2) * Math.sin(area.getYawRad()+Math.PI);
        start.translatePosition(offsetNorth, offsetEast, 0);
        start.convertToAbsoluteLatLonDepth();
        pivot.setManeuverLocation(new ManeuverLocation(start));
        pivot.setParams(area.getWidth(), area.getLength(), minHorStep, pivot.getAlternationPercent(), 0, pivot.isSquareCurve(), area.getYawRad(), pivot.getCrossAngleRadians(), true, false, (short) pivot.getSsRangeShadow());
        
        entry.setCenterLocation(getEntryPoint());
        exit.setCenterLocation(getEndPoint());
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        area.setColor(new Color(128,255,128,64));
        area.paint((Graphics2D)g.create(), renderer, renderer.getRotation());
        Graphics2D copy = (Graphics2D)g.create();
        Point2D pt = renderer.getScreenPosition(pivot.getStartLocation());
        copy.translate(pt.getX(), pt.getY());
        copy.rotate(-renderer.getRotation()-Math.PI/2);
        ManeuversUtil.paintPointLineList(copy, renderer.getZoom(), pivot.getPathPoints(), false, 0);
        String payloads = getPayloadsAbbreviated();
        loadImages();
        if (getAssociatedAllocation() == null)
            g.drawImage(orangeLed, (int)pt.getX()-8, (int)pt.getY()-8, null);
        else
            g.drawImage(greenLed, (int)pt.getX()-8, (int)pt.getY()-8, null);        
        g.setColor(Color.black);
        g.drawString(getName()+" ("+payloads+")", (int)pt.getX()+8, (int)pt.getY()+8);
        if(getAssociatedAllocation() != null)
            g.setColor(Color.green.brighter().brighter());
        else
            g.setColor(Color.orange);
        g.drawString(getName()+" ("+payloads+")", (int)pt.getX()+7, (int)pt.getY()+7);
        
    }
    
    public double getLength() {
        return pivot.getDistanceTravelled(getEntryPoint());
    }
    
    @Override
    public LocationType getCenterLocation() {
        return area.getCenterLocation();
    }
      
    /**
     * @return the pivot
     */
    public final RowsManeuver getPivot() {
        return pivot;
    }

    @Override
    public String marshall() throws IOException {
        LocationType loc = new LocationType(getCenterLocation());
        loc.convertToAbsoluteLatLonDepth();
        double length = area.getLength(), width = area.getWidth(), bearing = area.getYawDeg();
        return String.format("survey %s %s %f %f %f %f %f %s", getName(), isFirstPriority(), loc.getLatitudeDegs(),
                loc.getLongitudeDegs(), length, width, bearing, getRequiredPayloads());
    }

    @Override
    public void unmarshall(String data) throws IOException {
        Scanner input = new Scanner(data);
        input.next("[\\w]+");
        this.name = input.next("[\\w]+");
        this.firstPriority = input.nextBoolean();
        double latDegs = input.nextDouble();
        double lonDegs = input.nextDouble();
        area.setCenterLocation(new LocationType(latDegs, lonDegs));
        area.setLength(input.nextDouble());
        area.setWidth(input.nextDouble());
        area.setYawDeg(input.nextDouble());
        String[] payloads = input.nextLine().replaceAll("[\\[\\]]", "").trim().split("[, ]+");
        getRequiredPayloads().clear();
        for (String p : payloads)
            getRequiredPayloads().add(PayloadRequirement.valueOf(p));
        updateManeuver();
        input.close();
    }
    
    public static void main(String[] args) throws Exception {
        SurveyAreaTask task = new SurveyAreaTask(new LocationType(41, -8));
        task.area.setWidth(200);
        task.area.setLength(120);
        task.setYaw(Math.PI/4);
        task.requiredPayloads.add(PayloadRequirement.sidescan);
        task.updateManeuver();
        System.out.println(task.marshall());
        SurveyAreaTask task2 = new SurveyAreaTask(new LocationType());
        task2.unmarshall(task.marshall());
        System.out.println(task2.marshall());
        StateRenderer2D renderer = new StateRenderer2D(new LocationType(41, -8));
        renderer.addPostRenderPainter(task, "task");
        GuiUtils.testFrame(renderer);
        
        
    }
    
    
}
