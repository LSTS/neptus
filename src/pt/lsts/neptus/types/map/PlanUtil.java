/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 16/09/2011
 */
package pt.lsts.neptus.types.map;

import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JMenu;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.StatisticsProvider;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.StringUtils;

/**
 * Initially moved from {@link pt.lsts.neptus.plugins.planning.PlanStatistics}
 * @author pdias
 *
 */
public class PlanUtil {

    private static NumberFormat format = GuiUtils.getNeptusDecimalFormat(0);
    
    public static double speedRpmRatioSpeed = 1.3;
    
    public static double speedRpmRatioRpms = 1000;

    private PlanUtil() {
    }
    
    public static Vector<LocatedManeuver> getLocationsAsSequence(PlanType plan) {
        Vector<LocatedManeuver> mans = new Vector<LocatedManeuver>();
        for (Maneuver m : plan.getGraph().getManeuversSequence()) {
            if (m instanceof LocatedManeuver)
                mans.add((LocatedManeuver) m);
        }
        return mans;
    }

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
                    String units = (String) m.getClass().getMethod("getUnits").invoke(m);
                    if (units.equalsIgnoreCase("%"))
                        speed = speed/100 * speedRpmRatioSpeed;
                    else if (units.equalsIgnoreCase("rpm"))
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
        ret += (simpleTextOrHTML?"":(asHTMLFragment?"":"<html>")+"<h1>") + title + (simpleTextOrHTML?"\n":"</h1><ul>");
        ret += (simpleTextOrHTML ? "" : "<li><b>") + I18n.text("ID") + ":" + (simpleTextOrHTML ? " " : "</b> ")
                + plan.getId() + (simpleTextOrHTML ? "\n" : "</li>");
        ret += (simpleTextOrHTML ? "" : "<li><b>") + I18n.text("Length") + ":" + (simpleTextOrHTML ? " " : "</b> ")
                + MathMiscUtils.parseToEngineeringNotation(PlanUtil.getPlanLength(mans), 2)
                + "m" + (simpleTextOrHTML ? "\n" : "</li>");
        ret += (simpleTextOrHTML ? "" : "<li><b>") + I18n.text("Est. Time") + ":" + (simpleTextOrHTML ? " " : "</b> ")
                + PlanUtil.estimatedTime(mans, speedRpmRatioSpeed, speedRpmRatioRpms) + ""
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
        ret += (simpleTextOrHTML ? "" : "<li><b>") + I18n.text("Using Speed/RPM ratio") + ":"
                + (simpleTextOrHTML ? " " : "</b> ") + speedRpmRatioSpeed + "m/s (" + speedRpmRatioRpms + I18n.text("RPM") + ")"
                + (simpleTextOrHTML ? "\n" : "</li>");

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
