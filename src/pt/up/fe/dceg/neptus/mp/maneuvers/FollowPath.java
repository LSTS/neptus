/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: FollowPath.java 9616 2012-12-30 23:23:22Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.mp.maneuvers;

import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.GraphType;
import pt.up.fe.dceg.neptus.util.FileUtil;


/**
 * @author pdias
 *
 */
public class FollowPath extends FollowTrajectory {

    
    public FollowPath() {
        super();
        hasTime = false;
    }
    
    public FollowPath(GraphType gotoSequence) {
        super();
        hasTime = false;
        Goto gotoF = ((Goto) gotoSequence.getManeuver(gotoSequence.getInitialManeuverId()));
        LocationType trajectoryStart = gotoF.destination;
        setId(gotoSequence.getManeuver(gotoSequence.getInitialManeuverId()).getId());
        setSpeed(gotoF.getSpeed());
        setUnits(gotoF.getUnits());
        Maneuver[] mans = gotoSequence.getAllManeuvers();
        for (Maneuver m : mans) {
            Goto g = (Goto)m;
            double[] ret = g.destination.getOffsetFrom(trajectoryStart);
            double[] point = new double[] {ret[0], ret[1], ret[2], -1};
            points.add(point);
        }
        getManeuverLocation().setLocation(trajectoryStart);
    }

    @Override
    public String getName() {
        return "FollowPath";
    }

    @Override
    public double getCompletionTime(LocationType initialPosition) {
        double speed = this.speed;
        if (this.speed_units.equalsIgnoreCase("RPM")) {
            speed = speed/769.230769231; //1.3 m/s for 1000 RPMs
        }
        else if (this.speed_units.equalsIgnoreCase("%")) {
            speed = speed/76.923076923; //1.3 m/s for 100% speed
        }

        return getDistanceTravelled(initialPosition) / speed;
    }

    /**
     * @param msg
     * @return
     */
    public static Maneuver createFollowPathOrPattern(IMCMessage message) {
        String customValues = (String) message.getValue("custom");
        if (customValues != null) {
            LinkedHashMap<String, String> customValuesTL = IMCMessage.decodeTupleList(customValues);
            String pattern = customValuesTL.get("Pattern");
            if (pattern == null || pattern.length() == 0)
                return new FollowPath();
            if ("RowsPatterns".equalsIgnoreCase(pattern))
                return new RowsPattern();
            else if ("RIPattern".equalsIgnoreCase(pattern))
                return new RIPattern();
            else if ("CrossHatchPattern".equalsIgnoreCase(pattern))
                return new CrossHatchPattern();
            else
                return new FollowPath();
        }
        else
            return new FollowPath();
    }

    public static void main(String[] args) {
        FollowPath traj = new FollowPath();
        traj.loadFromXML("<FollowPath kind=\"automatic\"><basePoint type=\"pointType\"><point><id>id_53802104</id><name>id_53802104</name><coordinate><latitude>0N0'0''</latitude><longitude>0E0'0''</longitude><depth>0.0</depth></coordinate></point><radiusTolerance>0.0</radiusTolerance></basePoint><path><nedOffsets northOffset=\"0.0\" eastOffset=\"1.0\" depthOffset=\"2.0\" timeOffset=\"3.0\"/><nedOffsets northOffset=\"4.0\" eastOffset=\"5.0\" depthOffset=\"6.0\" timeOffset=\"7.0\"/></path><speed unit=\"RPM\">1000.0</speed></FollowPath>");
        //System.out.println(FileUtil.getAsPrettyPrintFormatedXMLString(traj.getManeuverAsDocument("FollowTrajectory")));
        traj.setSpeed(1);
        traj.setUnits("m/s");        
        System.out.println(FileUtil.getAsPrettyPrintFormatedXMLString(traj.getManeuverAsDocument("FollowPath")));

        traj.setSpeed(2);
        traj.setUnits("m/s");        
        System.out.println(FileUtil.getAsPrettyPrintFormatedXMLString(traj.getManeuverAsDocument("FollowPath")));

    }
}
