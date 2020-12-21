/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Oct 16, 2012
 */
package pt.lsts.neptus.plugins.noptilus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import pt.lsts.imc.FollowPath;
import pt.lsts.imc.Goto;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.Maneuver;
import pt.lsts.imc.PathPoint;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.imc.types.PlanSpecificationAdapter;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * This class provides a set of utilitary methods for generating IMC plans from waypoints lists
 * @author Noptilus
 */
public class PlanUtils {

    /**
     * Given a text file, this method loads a list of waypoints into a vector structure
     * @param inFile The file where to read the waypoints from
     * @return The list of loaded waypoints (lat, lon, depth)
     * @throws Exception If there is an error reading the file or an error with the file format
     */
    public static Vector<double[]> loadWaypoints(File inFile) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(inFile));
        
        String line = reader.readLine();
        Vector<double[]> locs = new Vector<double[]>();
        while (line != null) {
            line = line.trim();
            line = line.replaceAll("\\s+", "\t");
            String[] parts = line.trim().split("\\t");
            double[] loc = new double[3];
            loc[0] = Double.parseDouble(parts[0]);
            loc[1] = Double.parseDouble(parts[1]);
            loc[2] = Double.parseDouble(parts[2]);            
            locs.add(loc);
            line = reader.readLine(); 
        }
        
        reader.close();
        return locs;        
    }
    
    /**
     * Given a list of read locations, this method can be used to normalize them 
     * @param points A list of points (previously read with {@link #loadWaypoints(File)}
     * @param multFactor A multiplication factor
     * @param addFactor An addiction factor
     */
    public static void normalizeZ(Vector<double[]> points, double multFactor, double addFactor) {
        for (int i = 0 ; i < points.size(); i++)
            points.get(i)[2] = points.get(i)[2] * multFactor + addFactor; 
    }
    
    public static void filterShortDistances(Vector<double[]> points, double minDistance) {
        LocationType previousManeuver = null;
        
        Vector<double[]> filtered = new Vector<double[]>();
        
        for (double[] pt : points) {
            LocationType loc = new LocationType(pt[0], pt[1]);
            loc.setAbsoluteDepth(pt[2]);
            
            if (previousManeuver == null || loc.getDistanceInMeters(previousManeuver) > minDistance) {
                filtered.add(pt);
                previousManeuver = loc;
            }
        }
        
        points.clear();
        points.addAll(filtered);
    }
    
    /**
     * This method generates a plan specification containing a single FollowPath maneuver that goes through a list of waypoints
     * @param plan_id The id to be given to the plan
     * @param lld_locations The locations encoded as a vector of LatLonDepth points
     * @param speed The speed to be used in the plan
     * @param units The speed units as defined in {@link FollowPath.SPEED_UNITS}
     * @return The resulting PlanSpecification message
     */
    public static PlanSpecification trajectoryPlan(String plan_id, Vector<double[]> lld_locations, double speed, SpeedUnits units) {
        FollowPath maneuver = new FollowPath();
        
        LocationType firstLoc = new LocationType(lld_locations.get(0)[0], lld_locations.get(0)[1]);

        maneuver.setLat(firstLoc.getLatitudeRads());
        maneuver.setLon(firstLoc.getLongitudeRads());
        maneuver.setSpeed(speed);
        maneuver.setSpeedUnits(units);
        
        Vector<PathPoint> points = new Vector<PathPoint>();
        for (int i = 0; i < lld_locations.size(); i++) {
            PathPoint point = new PathPoint();
            LocationType curLoc = new LocationType(lld_locations.get(i)[0], lld_locations.get(i)[1]);
            curLoc.setAbsoluteDepth(lld_locations.get(i)[2]);
            double[] offsets = curLoc.getOffsetFrom(firstLoc);
            point.setX(offsets[0]);
            point.setY(offsets[1]);
            point.setZ(offsets[2]);            
            points.add(point);
        }

        maneuver.setPoints(points);
        
        PlanSpecificationAdapter plan = new PlanSpecificationAdapter();
        plan.setPlanId(plan_id);
        plan.addManeuver("path", maneuver);
        plan.setDescription("Trajectory plan generated from a list of waypoints");
        return (PlanSpecification)plan.getData(IMCDefinition.getInstance());
    }
    
    /**
     * This method generates an IMC plan specification given a list of locations and speed
     * @param plan_id The id to be given to the plan
     * @param lld_locations The list of locations
     * @param speed The speed to be used in the plan
     * @param units The speed units as defined in {@link Goto.SPEED_UNITS}
     * @return The corresponding IMC message
     */
    public static PlanSpecification planFromWaypoints(String plan_id, Vector<double[]> lld_locations, double speed, SpeedUnits units) {
        Vector<Maneuver> maneuvers = new Vector<Maneuver>();
        
        PlanSpecificationAdapter plan = new PlanSpecificationAdapter();
        plan.setPlanId(plan_id);
        plan.setDescription("Plan generated from a list of waypoints");

        for (int i = 0; i < lld_locations.size(); i++) {
            Goto gotoManeuver = new Goto();
            gotoManeuver.setLat(Math.toRadians(lld_locations.get(i)[0]));
            gotoManeuver.setLon(Math.toRadians(lld_locations.get(i)[1]));
            gotoManeuver.setZ(lld_locations.get(i)[2]);
            gotoManeuver.setSpeed(speed);
            gotoManeuver.setSpeedUnits(units);
            
            maneuvers.add(gotoManeuver);
            plan.addManeuver("" + (i + 1), gotoManeuver);
        }
        
        for (int i = 1; i < maneuvers.size(); i++)
            plan.addTransition("" + i, "" + (i + 1), "ManeuverIsDone", null);
        
        return (PlanSpecification) plan.getData(IMCDefinition.getInstance());
    }
}
