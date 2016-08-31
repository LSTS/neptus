/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias
 * 16/09/2011
 */
package pt.lsts.neptus.types.map;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JMenu;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.Maneuver.SPEED_UNITS;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.StatisticsProvider;
import pt.lsts.neptus.mp.preview.PlanSimulator;
import pt.lsts.neptus.mp.preview.SpeedConversion;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.StringUtils;

/**
 * Initially moved from {@link pt.lsts.neptus.console.plugins.planning.PlanStatistics}
 * @author pdias, zp
 *
 */
public class PlanUtil {

    private static NumberFormat format = GuiUtils.getNeptusDecimalFormat(0);
    
    public static double speedRpmRatioSpeed = 1.3;
    
    public static double speedRpmRatioRpms = 1000;

    private PlanUtil() {
    }
    
    /**
     * This method will sequence all maneuvers and return those whose positions are known
     * @param plan A given plan
     * @return A sequence of maneuvers whose locations are known
     */
    public static Vector<LocatedManeuver> getLocationsAsSequence(PlanType plan) {
        Vector<LocatedManeuver> mans = new Vector<LocatedManeuver>();
        for (Maneuver m : plan.getGraph().getManeuversSequence()) {
            if (m instanceof LocatedManeuver)
                mans.add((LocatedManeuver) m);
        }
        return mans;
    }

    /**
     * This method will change the speed of all maneuvers in a plan
     * @param plan A given plan
     * @param speedMps The speed to be set to all maneuvers (that accept a speed parameter) in meters per second
     */
    public static void setPlanSpeed(PlanType plan, double speedMps) {
        DefaultProperty units = PropertiesEditor.getPropertyInstance("Speed units", Maneuver.SPEED_UNITS.class, Maneuver.SPEED_UNITS.METERS_PS, true);
        units.setDisplayName(I18n.text("Speed units"));
        units.setShortDescription(I18n.text("The speed units"));
        
        DefaultProperty propertySpeed = PropertiesEditor.getPropertyInstance("Speed", Double.class, speedMps, true);
        propertySpeed.setDisplayName(I18n.text("Speed"));
        Property[] props = new Property[] {units, propertySpeed};
        
        for (Maneuver man : plan.getGraph().getAllManeuvers()) {
            try {
                man.setProperties(props);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e, e);
            }
        }
    }
    
    /**
     * This method will change the depth of all maneuvers in a plan
     * @param plan A given plan
     * @param depth The depth to be set to all maneuvers (that accept a depth parameter) in meters
     * @see #setPlanZ(PlanType, double, pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS)
     */
    public static void setPlanDepth(PlanType plan, double depth) {
       setPlanZ(plan, depth, ManeuverLocation.Z_UNITS.DEPTH);
    }
    
    /**
     * This method will change the altitude of all maneuvers in a plan
     * @param plan A given plan
     * @param depth The altitude to be set to all maneuvers (that accept a depth parameter) in meters
     * @see #setPlanZ(PlanType, double, pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS)
     */
    public static void setPlanAltitude(PlanType plan, double altitude) {
        setPlanZ(plan, altitude, ManeuverLocation.Z_UNITS.ALTITUDE);
    }

    /**
     * This method will change the depth/altitude of all maneuvers in a plan
     * @param plan A given plan
     * @param z The z value to be set in the plan
     * @param units The z units to be used
     * @see ManeuverLocation.Z_UNITS
     */
    public static void setPlanZ(PlanType plan, double z, ManeuverLocation.Z_UNITS units) {
        for (LocatedManeuver man : getLocationsAsSequence(plan)) {
            ManeuverLocation lt = ((LocatedManeuver) man).getManeuverLocation();
            lt.setZ(z);
            lt.setZUnits(units);
            ((LocatedManeuver) man).setManeuverLocation(lt);
        }
    }
    
    /**
     * This method will compute the initial location of the given plan
     * @param plan A plan
     * @return The first known location of the plan
     * @throws Exception In case no maneuvers of the plan have a known location
     */
    public static LocationType getFirstLocation(PlanType plan) throws Exception {
        for (Maneuver m : plan.getGraph().getManeuversSequence()) {
            if (m instanceof LocatedManeuver)
                return ((LocatedManeuver) m).getStartLocation();
        }
        throw new Exception("The plan doesn't have any located maneuvers");
    }
    
    /**
     * This method will compute the final location of the given plan
     * @param plan A plan
     * @return The last known location of the plan
     * @throws Exception In case no maneuvers of the plan have a known location
     */
    public static LocationType getEndLocation(PlanType plan) throws Exception {
        Maneuver[] mans = plan.getGraph().getManeuversSequence();
        for (int i = mans.length - 1; i >= 0; i--) {
            if (mans[i] instanceof LocatedManeuver)
                return ((LocatedManeuver) mans[i]).getEndLocation();
        }
        throw new Exception("The plan doesn't have any located maneuvers");
    }
    
    /**
     * This method will simulate a given plan and return a series of system states
     * @param plan The plan to be simulated
     * @return A sequence of vehicle poses that are expected to happen when executing this plan
     * @throws Exception In case the initial position of the plan cannot be computed
     */
    public Vector<SystemPositionAndAttitude> simulatePlan(PlanType plan) throws Exception {
        PlanSimulator ps = new PlanSimulator(plan, new SystemPositionAndAttitude(getFirstLocation(plan), 0,0,0));
        
        while(!ps.getSimulationOverlay().simulationFinished)
            Thread.sleep(100);
        
        return ps.getSimulationOverlay().getStates();        
    }
    
    /**
     * This method will simulate a given plan and return a series of system states
     * @param plan The plan to be simulated
     * @param loc The initial location to be used for the simulation
     * @return A sequence of vehicle poses that are expected to happen when executing this plan
     * @throws Exception In case the initial position of the plan cannot be computed
     */
    public Vector<SystemPositionAndAttitude> simulatePlan(LocationType loc, PlanType plan) throws Exception {
        PlanSimulator ps = new PlanSimulator(plan, new SystemPositionAndAttitude(loc, 0,0,0));
        
        while(!ps.getSimulationOverlay().simulationFinished)
            Thread.sleep(100);
        
        return ps.getSimulationOverlay().getStates();        
    }
    
    
    public static ArrayList<ManeuverLocation> getPlanWaypoints(PlanType plan) {
        ArrayList<ManeuverLocation> locs = new ArrayList<>();
        for (Maneuver m : plan.getGraph().getManeuversSequence()) {
            if (m instanceof LocatedManeuver)
                locs.addAll(((LocatedManeuver) m).getWaypoints());
        }
        
        for (ManeuverLocation l : locs)
            l.convertToAbsoluteLatLonDepth();
        return locs;
    }
    
    /**
     * This method computes the total length of a plan, in meters
     * @param plan A given plan
     * @return The total length of the plan, in meters
     * @see #getPlanLength(Vector)
     */
    public static double getPlanLength(PlanType plan) {
        return getPlanLength(getLocationsAsSequence(plan));
    }
    
    /**
     * This method computes the total length of maneuver sequence, in meters
     * @param mans A sequence of located maneuvers
     * @return The total length of the maneuver sequence
     */
    public static double getPlanLength(Vector<LocatedManeuver> mans) {
        double length = 0;
        if (mans.size() == 0)
            return 0;
        if (mans.get(0) instanceof StatisticsProvider)
            length += ((StatisticsProvider)mans.get(0)).getDistanceTravelled((LocationType) mans.get(0).getStartLocation());
        for (int i = 1; i < mans.size(); i++) {
              if (mans.get(i) instanceof StatisticsProvider)
                  length += ((StatisticsProvider)mans.get(i)).getDistanceTravelled((LocationType) mans.get(i-1).getEndLocation());
              else
                  length += mans.get(i).getManeuverLocation().getDistanceInMeters(mans.get(i-1).getManeuverLocation());
        }
        return length;
    }
    
    /**
     * This method estimates the total time 
     * @param previousPos
     * @param plan
     * @return
     * @throws Exception
     */
    public static double getEstimatedDelay(LocationType previousPos, PlanType plan) throws Exception {
        double time = 0;
        try {
            if (previousPos == null)
                previousPos = getFirstLocation(plan);
        }
        catch (Exception e) {
            // Impossible to get any location for this plan...
            NeptusLog.pub().debug(e);
            if (previousPos == null)
                return 0;
        }
        
        for (Maneuver m : plan.getGraph().getManeuversSequence()) {
            if (m instanceof StatisticsProvider) {
                time += ((StatisticsProvider)m).getCompletionTime(previousPos);                
            }
            else {
                try {
                    double speed = (Double) m.getClass().getMethod("getSpeed").invoke(m);
                    SPEED_UNITS units = (Maneuver.SPEED_UNITS) m.getClass().getMethod("getSpeedUnits").invoke(m);
                    switch (units) {
                        case PERCENTAGE:
                            speed = SpeedConversion.convertPercentageToMps(speed);
                            break;
                        case RPM:
                            speed = SpeedConversion.convertRpmtoMps(speed);
                        default:
                            break;
                    }
                    if (m instanceof LocatedManeuver) {
                        LocationType start = ((LocatedManeuver) m).getStartLocation();
                        LocationType end = ((LocatedManeuver) m).getEndLocation();
                        time += start.getDistanceInMeters(previousPos) / speed;
                        time += end.getDistanceInMeters(start) / speed;                        
                    }
                }
                catch (Exception e) {
                    //e.printStackTrace();
                }
            }

            if (m instanceof LocatedManeuver) {
                previousPos = ((LocatedManeuver) m).getEndLocation();
            }            
        }

        return time;
    }

    public static double getEstimatedDelay(LocationType previousPos, Maneuver m) throws Exception {
        double time = 0;

        if (previousPos == null)
            return 0;


        if (m instanceof StatisticsProvider) {
            time = ((StatisticsProvider)m).getCompletionTime(previousPos);
        }
        else {
            try {
                double speed = (Double) m.getClass().getMethod("getSpeed").invoke(m);
                SPEED_UNITS units = (Maneuver.SPEED_UNITS) m.getClass().getMethod("getSpeedUnits").invoke(m);
                switch (units) {
                    case PERCENTAGE:
                        speed = SpeedConversion.convertPercentageToMps(speed);
                        break;
                    case RPM:
                        speed = SpeedConversion.convertRpmtoMps(speed);
                    default:
                        break;
                }
                if (m instanceof LocatedManeuver) {
                    LocationType start = ((LocatedManeuver) m).getStartLocation();
                    LocationType end = ((LocatedManeuver) m).getEndLocation();
                    time += start.getDistanceInMeters(previousPos) / speed;
                    time += end.getDistanceInMeters(start) / speed;
                }
            }
            catch (Exception e) {
                //e.printStackTrace();
            }
        }

        return time;
    }

    public static String getDelayStr(LocationType previousPos, PlanType plan) throws Exception {
        return DateTimeUtil.milliSecondsToFormatedString((long)(getEstimatedDelay(previousPos, plan) * 1000));
    }

    public static String estimatedTime(Vector<LocatedManeuver> mans, double speedRpmRatioSpeed, double speedRpmRatioRpms) {
        double timeSecs = 0;

        for (int i = 0; i < mans.size(); i++) {
            LocatedManeuver m = mans.get(i);
            LocationType previousPos = (i > 0)? new LocationType(mans.get(i-1).getManeuverLocation()) : new LocationType(m.getManeuverLocation());
            double speed = speedRpmRatioSpeed;
            if (m instanceof StatisticsProvider)
                timeSecs += ((StatisticsProvider)m).getCompletionTime(previousPos);
            else {
                try {
                    speed = (Double) m.getClass().getMethod("getSpeed").invoke(m);
                    SPEED_UNITS units = (Maneuver.SPEED_UNITS) m.getClass().getMethod("getSpeedUnits").invoke(m);
                    if (units == SPEED_UNITS.PERCENTAGE)
                        speed = speed/100 * speedRpmRatioSpeed;
                    else if (units == SPEED_UNITS.RPM)
                        speed = (speed / speedRpmRatioRpms) * speedRpmRatioSpeed;
                }
                catch (Exception e) {
                    //e.printStackTrace();
                }
                double dist = mans.get(i).getManeuverLocation().getDistanceInMeters(previousPos);
            
                timeSecs += dist / speed;
            }
        }
//      int minutes = (int)timeSecs / 60;
//      int seconds = (int)timeSecs % 60;
        
        //return minutes+"m "+seconds+"s";
        return DateTimeUtil.milliSecondsToFormatedString((long) (timeSecs * 1E3));
    }
    
    public static double getMaxPlannedDepth(Vector<LocatedManeuver> mans) {
        double depth = 0;
        
        if (!mans.isEmpty())
            depth = mans.get(0).getManeuverLocation().getAllZ();
        
        for (int i = 1; i < mans.size(); i++) {
            double d = mans.get(i).getManeuverLocation().getAllZ();
            if (d > depth)
                depth = d;
        }
        return depth;
    }

    public static double getMinPlannedDepth(Vector<LocatedManeuver> mans) {
        double depth = 0;
        
        if (!mans.isEmpty())
            depth = mans.get(0).getManeuverLocation().getAllZ();
        
        for (int i = 1; i < mans.size(); i++) {
            double d = mans.get(i).getManeuverLocation().getAllZ();
            if (d < depth)
                depth = d;
        }
        return depth;
    }

    public static int numManeuvers(PlanType plan) {
        return plan.getGraph().getAllManeuvers().length;
    }

    public static JMenu getPlanStatisticsAsJMenu (PlanType plan, String title) {
        return getPlanStatisticsAsJMenu(plan, title, speedRpmRatioSpeed, speedRpmRatioRpms);
    }

    public static JMenu getPlanStatisticsAsJMenu (PlanType plan, double speedRpmRatioSpeed, double speedRpmRatioRpms) {
        return getPlanStatisticsAsJMenu(plan, null, speedRpmRatioSpeed, speedRpmRatioRpms);
    }

    public static JMenu getPlanStatisticsAsJMenu (PlanType plan, String title, double speedRpmRatioSpeed, double speedRpmRatioRpms) {
        if (title == null || title.length() == 0)
            title = I18n.text("Plan Statistics");
        JMenu menu = new JMenu(title);
        
        String txt = getPlanStatisticsAsText(plan, title, speedRpmRatioSpeed, speedRpmRatioRpms, true, false);
        boolean titleBool = true;
        for (String str : txt.split("\n")) {
            if (titleBool) {
                titleBool = false;
                continue;
            }
            if (str.equalsIgnoreCase(""))
                menu.addSeparator();
            else
                menu.add(new JLabel(str));
        }
        return menu;
    }

    public static String getPlanStatisticsAsText(PlanType plan, String title,
            boolean simpleTextOrHTML, boolean asHTMLFragment) {
        return getPlanStatisticsAsText(plan, title, speedRpmRatioSpeed, speedRpmRatioRpms, simpleTextOrHTML,
                asHTMLFragment);
    }
    
    public static String getPlanStatisticsAsText(PlanType plan, String title, double speedRpmRatioSpeed,
            double speedRpmRatioRpms, boolean simpleTextOrHTML, boolean asHTMLFragment) {
        if (title == null || title.length() == 0)
            title = I18n.text("Plan Statistics");
        Vector<LocatedManeuver> mans = PlanUtil.getLocationsAsSequence(plan);
        String ret = "";
        String estDelay = PlanUtil.estimatedTime(mans, speedRpmRatioSpeed, speedRpmRatioRpms);
        try {
            estDelay = PlanUtil.getDelayStr(null, plan);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
        ret += (simpleTextOrHTML?"":(asHTMLFragment?"":"<html>")+"<h1>") + title + (simpleTextOrHTML?"\n":"</h1><ul>");
        ret += (simpleTextOrHTML ? "" : "<li><b>") + I18n.text("ID") + ":" + (simpleTextOrHTML ? " " : "</b> ")
                + plan.getId() + (simpleTextOrHTML ? "\n" : "</li>");
        ret += (simpleTextOrHTML ? "" : "<li><b>") + I18n.text("Length") + ":" + (simpleTextOrHTML ? " " : "</b> ")
                + MathMiscUtils.parseToEngineeringNotation(PlanUtil.getPlanLength(mans), 2)
                + "m" + (simpleTextOrHTML ? "\n" : "</li>");
        ret += (simpleTextOrHTML ? "" : "<li><b>") + I18n.text("Est. Time") + ":" + (simpleTextOrHTML ? " " : "</b> ")
                //+ PlanUtil.estimatedTime(mans, speedRpmRatioSpeed, speedRpmRatioRpms) + ""
                + estDelay + ""
                + (simpleTextOrHTML ? "\n" : "</li>");
        ret += (simpleTextOrHTML ? "" : "<li><b>") + I18n.text("Max. Depth") + ":"
                + (simpleTextOrHTML ? " " : "</b> ")
                + format.format(PlanUtil.getMaxPlannedDepth(mans)) + "m"
                + (simpleTextOrHTML ? "\n" : "</li>");
        ret += (simpleTextOrHTML ? "" : "<li><b>") + I18n.text("Min. Depth") + ":"
                + (simpleTextOrHTML ? " " : "</b> ")
                + format.format(PlanUtil.getMinPlannedDepth(mans)) + "m"
                + (simpleTextOrHTML ? "\n" : "</li>");
        ret += (simpleTextOrHTML ? "" : "<li><b>") + I18n.text("# Maneuvers") + ":"
                + (simpleTextOrHTML ? " " : "</b> ")
                + PlanUtil.numManeuvers(plan) + ""
                + (simpleTextOrHTML ? "\n" : "</li>");
        ret += (simpleTextOrHTML) ? "\n" : "";        

        Vector<VehicleType> vehs = plan.getVehicles();
        String vehiclesListStr = "";
        if (vehs.size() > 0) {
            for (VehicleType vehicleType : vehs)
                vehiclesListStr += vehicleType.getName();  
            vehiclesListStr = StringUtils.wrapEveryNChars(vehiclesListStr, (short) 40, -1, true);
            String vehicleTitle = I18n.text("Vehicle") + (vehs.size() > 1 ? "s" : "");
            ret += (simpleTextOrHTML) ? "\n" : "";
            ret += (simpleTextOrHTML ? "" : "<li><b>") + vehicleTitle + ":"
                    + (simpleTextOrHTML ? " " : "</b> ") + vehiclesListStr
                    + (simpleTextOrHTML ? "\n" : "</li>");
        }
        
        ret += simpleTextOrHTML ? "" : "</ul>" + (asHTMLFragment?"":"</html>");

        return ret;
    }
}
