/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.mp.maneuvers;

import java.util.LinkedHashMap;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.GraphType;
import pt.lsts.neptus.util.FileUtil;


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
        setSpeedUnits(gotoF.getUnits());
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
        //NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(traj.getManeuverAsDocument("FollowTrajectory")));
        traj.setSpeed(1);
        traj.setSpeedUnits("m/s");        
        NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(traj.getManeuverAsDocument("FollowPath")));

        traj.setSpeed(2);
        traj.setSpeedUnits("m/s");        
        NeptusLog.pub().info("<###> "+FileUtil.getAsPrettyPrintFormatedXMLString(traj.getManeuverAsDocument("FollowPath")));

    }
}
