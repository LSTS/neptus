/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 15/09/2011
 * $Id:: RIPattern.java 9615 2012-12-30 23:08:28Z pdias                         $:
 */
package pt.up.fe.dceg.neptus.mp.maneuvers;

import java.awt.Graphics2D;
import java.util.Vector;

import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.PlanElement;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * Reacquire-Identify Pattern Maneuver
 * @author pdias
 *
 */
public class RIPattern extends RowsPattern {

    {
        ignoreLength = true;
        ignoreCrossAngle = true;
        ignoreFirstCurveRight = true;
        paintOnlyBasePoint = true;
    }
    
    @Override
    public String getName() {
        return "RIPattern";
    }
    
    @Override
    public void paintOnMap(Graphics2D g2d, PlanElement planElement, StateRenderer2D renderer) {
        double zoom = renderer.getZoom();
        g2d.rotate(-renderer.getRotation());
        g2d.rotate(-Math.PI/2);
        ManeuversUtil.paintBox(g2d, zoom, width, width, -width/2, -width/2, bearingRad, crossAngleRadians, editing);
        ManeuversUtil.paintBox(g2d, zoom, width, width, -width/2, -width/2, bearingRad+Math.toRadians(-60), crossAngleRadians, editing);
        ManeuversUtil.paintBox(g2d, zoom, width, width, -width/2, -width/2, bearingRad+Math.toRadians(-120), crossAngleRadians, editing);
//        ManeuversUtil.paintPointLineList(g2d, zoom, points, false, sRange);
        g2d.rotate(Math.PI/2);
        g2d.rotate(renderer.getRotation());

        super.paintOnMap(g2d, planElement, renderer);
    }

    /**
     * Call this to update the maneuver points.
     */
    @Override
    protected void recalcPoints() {
        Vector<double[]> newPoints = ManeuversUtil.calcRIPatternPoints(width, hstep,
                alternationPercentage, curvOff, squareCurve, bearingRad);
        points = newPoints;
    }

    /**
     * @param width
     * @param hstep
     * @param alternationPercent
     * @param curvOff
     * @param squareCurve
     * @param bearingRad
     * @param crossAngleRadians
     * @param firstCurveRight
     */
    public void setParams(double width, double hstep,
            double alternationPercent, double curvOff, boolean squareCurve, double bearingRad,
            double crossAngleRadians, boolean firstCurveRight) {
        this.width = width;
        this.hstep = hstep;
        this.alternationPercentage = (float) alternationPercent;
        this.curvOff = curvOff;
        this.squareCurve = squareCurve;
        this.bearingRad = bearingRad;
        this.crossAngleRadians = crossAngleRadians;
        this.firstCurveRight = firstCurveRight;
        
        recalcPoints();
    }

    public static void main(String[] args) {
        RIPattern man = new RIPattern();
        //man("<FollowPath kind=\"automatic\"><basePoint type=\"pointType\"><point><id>id_53802104</id><name>id_53802104</name><coordinate><latitude>0N0'0''</latitude><longitude>0E0'0''</longitude><depth>0.0</depth></coordinate></point><radiusTolerance>0.0</radiusTolerance></basePoint><path><nedOffsets northOffset=\"0.0\" eastOffset=\"1.0\" depthOffset=\"2.0\" timeOffset=\"3.0\"/><nedOffsets northOffset=\"4.0\" eastOffset=\"5.0\" depthOffset=\"6.0\" timeOffset=\"7.0\"/></path><speed unit=\"RPM\">1000.0</speed></FollowPath>");
        //System.out.println(FileUtil.getAsPrettyPrintFormatedXMLString(man.getManeuverAsDocument("FollowTrajectory")));
        man.setSpeed(1);
        man.setUnits("m/s");        
//        System.out.println(FileUtil.getAsPrettyPrintFormatedXMLString(man.getManeuverAsDocument("RIPattern")));

        man.setSpeed(2);
        man.setUnits("m/s");        
//        System.out.println(FileUtil.getAsPrettyPrintFormatedXMLString(man.getManeuverAsDocument("RIPattern")));

        
      MissionType mission = new MissionType("./missions/rep10/rep10.nmisz");
      StateRenderer2D r2d = new StateRenderer2D(MapGroup.getMapGroupInstance(mission));
      PlanElement pelem = new PlanElement(MapGroup.getMapGroupInstance(mission), null);
      PlanType plan = new PlanType(mission);
      man.getManeuverLocation().setLocation(r2d.getCenter());
//      man.setBearingRad(Math.toRadians(20));
//      man.setParams(200, 300, 27, .5, 15, true, Math.toRadians(20), Math.toRadians(10), true);
      plan.getGraph().addManeuver(man);        
      pelem.setPlan(plan);
      r2d.addPostRenderPainter(pelem, "Plan");
      GuiUtils.testFrame(r2d);
      RowsManeuver.unblockNewRows = true;
//      PropertiesEditor.editProperties(man, true);

    }
}
