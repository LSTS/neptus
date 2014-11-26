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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Nov 25, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.HashSet;

import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.ManeuversUtil;
import pt.lsts.neptus.mp.maneuvers.RowsManeuver;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.ParallelepipedElement;
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
        area.setCenterLocation(clickedLocation);
        area.setWidth(175);
        area.setLength(230);
        area.setYawDeg(-32);
        entry.setId(getName()+"_entry");
        exit.setId(getName()+"_exit");
        updateManeuver();
    }
    
    private LocationType getEntryPoint() {
        pivot.endManeuver();
        return pivot.getStartLocation().convertToAbsoluteLatLonDepth();
    }
    
    private LocationType getEndPoint() {
        return pivot.getEndLocation().convertToAbsoluteLatLonDepth();
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
        double minHorStep = area.getWidth();
        double minDepth = Double.MAX_VALUE;
        double maxDepth = -Double.MAX_VALUE;
        for (PayloadRequirement p : requiredPayloads) {
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
        
        offsetNorth = (-15+area.getLength()/2) * Math.cos(area.getYawRad()+Math.PI);
        offsetEast = (-15+area.getLength()/2) * Math.sin(area.getYawRad()+Math.PI);
        start.translatePosition(offsetNorth, offsetEast, 0);
        start.convertToAbsoluteLatLonDepth();
        pivot.setManeuverLocation(new ManeuverLocation(start));
        pivot.setParams(area.getWidth(), area.getLength(), 25, pivot.getAlternationPercent(), pivot.getCurvOff(), pivot.isSquareCurve(), area.getYawRad(), pivot.getCrossAngleRadians(), true, false, (short) pivot.getSsRangeShadow());
        
        entry.setCenterLocation(getEntryPoint());
        exit.setCenterLocation(getEndPoint());
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        area.paint((Graphics2D)g.create(), renderer, renderer.getRotation());
        Graphics2D copy = (Graphics2D)g.create();
        Point2D pt = renderer.getScreenPosition(pivot.getStartLocation());
        copy.translate(pt.getX(), pt.getY());
        copy.rotate(-renderer.getRotation()-Math.PI/2);
        ManeuversUtil.paintPointLineList(copy, renderer.getZoom(), pivot.getPathPoints(), false, 0);
        entry.paint((Graphics2D)g.create(), renderer, renderer.getRotation());
        exit.paint((Graphics2D)g.create(), renderer, renderer.getRotation());
    }
    
    public static void main(String[] args) {
        SurveyAreaTask task = new SurveyAreaTask(new LocationType(41, -8));
        StateRenderer2D renderer = new StateRenderer2D(new LocationType(41, -8));
        renderer.addPostRenderPainter(task, "task");
        GuiUtils.testFrame(renderer);
    }


}
