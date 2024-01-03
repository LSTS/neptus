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
 * Oct 29, 2017
 */
package pt.lsts.neptus.soi;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import pt.lsts.imc.Maneuver;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.PlanTransition;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.endurance.Plan;
import pt.lsts.neptus.endurance.Waypoint;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.util.WGS84Utilities;

/**
 * @author zp
 *
 */
public class SoiUtils {

    public Date nextCommunication(ImcSystem vehicle, Plan plan) {
        if (vehicle == null || plan == null)
            return new Date();
        
        Date nextComm = new Date(vehicle.getLocationTimeMillis());
        
        for (Waypoint wpt : plan.waypoints()) {
            if (wpt.getArrivalTime().after(nextComm)) {
                nextComm = wpt.getArrivalTime();
            }
        }
        
        return nextComm;
    }
    
    public static SystemPositionAndAttitude futureState(ImcSystem vehicle, Plan plan) {
        
        if (vehicle == null || plan == null)
            return null;
        
        SystemPositionAndAttitude last = new SystemPositionAndAttitude(vehicle.getLocation(),0,0,0);
        
        Date now = new Date();
        Waypoint next = null;
        
        for (Waypoint wpt : plan.waypoints()) {
            if (wpt.getArrivalTime() != null && wpt.getArrivalTime().after(now)) {
                next = wpt;
                break;
            }
        }
        if (next == null)
            return null;
        
        last.setTime(next.getArrivalTime().getTime());
        last.getPosition().setLatitudeDegs(next.getLatitude());
        last.getPosition().setLongitudeDegs(next.getLongitude());
        
        return last;
    }
    
    public static SystemPositionAndAttitude estimatedState(ImcSystem system, Plan plan) {
        if (system == null || plan == null)
            return null;
        Date now = new Date();
        Waypoint previous = null;
        Waypoint next = null;       
        
        // find previous and following waypoint
        for (Waypoint wpt : plan.waypoints()) {
            if (previous == null || wpt.getArrivalTime().before(now)) {
                previous = wpt;
            }
            if (wpt.getArrivalTime() == null || wpt.getArrivalTime().after(now)) {
                next = wpt;
                break;
            }
        }
                
        if (previous == null || next == null || previous.compareTo(next) > 0
                || previous.getArrivalTime() == null || next.getArrivalTime() == null)
            return null;
        
        // calculate where should the vehicle be between these two waypoints
        double totalTime = (next.getArrivalTime().getTime() - previous.getArrivalTime().getTime());
        double timeSincePrevious = (now.getTime() - previous.getArrivalTime().getTime());
        
        double offsets[] = WGS84Utilities.WGS84displacement(previous.getLatitude(), previous.getLongitude(), 0,
                next.getLatitude(), next.getLongitude(), 0);
        
        offsets[0] *= timeSincePrevious / totalTime;
        offsets[1] *= timeSincePrevious / totalTime;
        
        
        SystemPositionAndAttitude ret = new SystemPositionAndAttitude(
                new LocationType(previous.getLatitude(), previous.getLongitude()), 0, 0, 0);
        
        
        double[] pos = WGS84Utilities.WGS84displace(previous.getLatitude(), previous.getLongitude(), 0, offsets[0], offsets[1], 0);
        ret.getPosition().setLatitudeDegs(pos[0]);
        ret.getPosition().setLongitudeDegs(pos[1]);
        ret.setTime(now.getTime());
        ret.setYaw(Math.toDegrees(Math.atan2(offsets[1], offsets[0])));
        
        return ret;
    }
    
    /**
     * This method calculates the maneuver sequence present in a plan. In case
     * of cyclic plans, it will retrieve the first non-repeating sequence of
     * maneuvers.
     * 
     * @param plan
     *            The plan to parsed.
     * @return a maneuver sequence.
     */
    public static List<Maneuver> getFirstManeuverSequence(PlanSpecification plan) {
        ArrayList<Maneuver> ret = new ArrayList<Maneuver>();

        LinkedHashMap<String, Maneuver> maneuvers = new LinkedHashMap<String, Maneuver>();
        LinkedHashMap<String, String> transitions = new LinkedHashMap<String, String>();

        for (PlanManeuver m : plan.getManeuvers())
            maneuvers.put(m.getManeuverId(), m.getData());

        for (PlanTransition pt : plan.getTransitions()) {
            if (transitions.containsKey(pt.getSourceMan())) {
                System.err.println("This should be used only in sequential plans");
                continue;
            }
            transitions.put(pt.getSourceMan(), pt.getDestMan());
        }

        Vector<String> visited = new Vector<String>();
        String man = plan.getStartManId();

        while (man != null) {
            if (visited.contains(man)) {
                return ret;
            }
            visited.add(man);
            Maneuver m = maneuvers.get(man);
            ret.add(m);
            man = transitions.get(man);
        }

        return ret;
    }
}
