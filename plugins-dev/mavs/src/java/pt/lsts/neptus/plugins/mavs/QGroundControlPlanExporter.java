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
 * Author: pdias
 * 24/04/2017
 */
package pt.lsts.neptus.plugins.mavs;

import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import javax.swing.ProgressMonitor;

import org.apache.commons.io.FileUtils;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.editor.NumberEditor;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.Loiter;
import pt.lsts.neptus.mp.maneuvers.ManeuversUtil;
import pt.lsts.neptus.mp.maneuvers.PathProvider;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.mavs.enumeration.MAV_FRAME;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.mission.plan.IPlanFileExporter;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author pdias
 *
 */
public class QGroundControlPlanExporter implements IPlanFileExporter {

    private static final int MPL_FORMAT_VERSION = 120;
    
    private static final String NEW_LINE = "\r\n";
    private static final String SPACER_CHAR = "\t";
    private static final String COMMENT_CHAR = "#";
    @SuppressWarnings("unused")
    private static final String COMMENT_CHAR_WITH_SPACE = COMMENT_CHAR + " ";
    
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'.0Z'", Locale.US);
    
    @NeptusProperty (name = "Acceptance Radius", userLevel = LEVEL.REGULAR, editorClass = NumberEditor.UInteger.class)
    private int acceptanceRadius = 10;
    @NeptusProperty (name = "MAV Frames", userLevel = LEVEL.REGULAR)
    private MAV_FRAME mavFrame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT; 
    
    private long wpCounter = 0;
    
    private int holdTimeCopter = 0;
    
    public QGroundControlPlanExporter() {
        resetLocalData();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.mission.plan.IPlanFileExporter#getExporterName()
     */
    @Override
    public String getExporterName() {
        return "QGroundControl Waypoint File List";
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.mission.plan.IPlanFileExporter#validExtensions()
     */
    @Override
    public String[] validExtensions() {
        return new String[] { "mission" };
    }

    private void resetLocalData() {
        resetWPCounter();
    }
    
    private long resetWPCounter() {
        return wpCounter = 0;
    }

    private long nextWPCounter() {
        return wpCounter++;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.mission.plan.IPlanFileExporter#exportToFile(pt.lsts.neptus.types.mission.plan.PlanType, java.io.File, javax.swing.ProgressMonitor)
     */
    @Override
    public void exportToFile(PlanType plan, File out, ProgressMonitor monitor) throws Exception {
        resetLocalData();
        
        boolean isCancelled = PluginUtils.editPluginProperties(this, true);
        if (isCancelled)
            return;
        
        StringBuilder sb = new StringBuilder();
        
        // Writing plan name comment line (At the moment not supported)
//        sb.append(COMMENT_CHAR_WITH_SPACE);
//        sb.append("Plan: ").append(plan.getId());
//        sb.append(NEW_LINE);
//
//        sb.append(COMMENT_CHAR_WITH_SPACE);
//        sb.append(getTimeStamp());
//        sb.append(NEW_LINE);
//
//        // Writing comment lines
//        sb.append(COMMENT_CHAR_WITH_SPACE);
//        sb.append("QGC WPL <VERSION>");
//        sb.append(NEW_LINE).append(COMMENT_CHAR_WITH_SPACE);
//        sb.append("<INDEX>\t<CURRENT WP>\t<COORD FRAME>\t<COMMAND>\t<PARAM1>\t<PARAM2>\t<PARAM3>\t<PARAM4>\t");
//        sb.append("<PARAM5/X/LONGITUDE>\t<PARAM6/Y/LATITUDE>\t<PARAM7/Z/ALTITUDE>\t<AUTOCONTINUE>");
//        sb.append(NEW_LINE).append(NEW_LINE);
        
        sb.append("QGC WPL ").append(MPL_FORMAT_VERSION).append(NEW_LINE);
        
        processManeuvers(plan, sb);
        
        FileUtils.write(out, sb.toString(), (Charset) null);
    }
    
    @SuppressWarnings("unused")
    private String getTimeStamp() {
        return dateFormatter.format(new Date());
    }

    private void processManeuvers(PlanType plan, StringBuilder sb) throws Exception {
        for (Maneuver m : plan.getGraph().getManeuversSequence()) {
            double speedMS = ManeuversUtil.getSpeedMps(m);

            if (m instanceof PathProvider) {
                Collection<ManeuverLocation> waypoints = ((LocatedManeuver) m).getWaypoints();
                waypoints.stream().forEach(wp -> wp.convertToAbsoluteLatLonDepth());
                int ct = 0;
                for (ManeuverLocation wp : ((LocatedManeuver) m).getWaypoints()) {
                    if (ct++ > 0)
                        sb.append(NEW_LINE);
                    sb.append(getCommandGoto(wp.getLatitudeDegs(), wp.getLongitudeDegs(), wp.getZ(), wp.getZUnits(),
                            speedMS));
                }
            }
            else if (m instanceof Loiter) {
//              if (Double.isNaN(speedMS))
//                  continue;
                ManeuverLocation wp = ((Loiter) m).getManeuverLocation();
                wp.convertToAbsoluteLatLonDepth();
                sb.append(getCommandLoiter(wp.getLatitudeDegs(), wp.getLongitudeDegs(), wp.getZ(), wp.getZUnits(),
                        ((Loiter) m).getRadius(), ((Loiter) m).getDirection(), ((Loiter) m).getBearing(), speedMS));
            }
            else if (m instanceof Goto) { // Careful with ordering because of extensions
//                if (Double.isNaN(speedMS))
//                    continue;
                ManeuverLocation wp = ((Goto) m).getManeuverLocation();
                wp.convertToAbsoluteLatLonDepth();
                sb.append(getCommandGoto(wp.getLatitudeDegs(), wp.getLongitudeDegs(), wp.getZ(), wp.getZUnits(),
                        speedMS));
            }
            else {
                NeptusLog.pub().warn(
                        String.format("Unsupported maneuver found \"%s\" in plan \"%s\".", m.getId(), plan.getId()));
            }

            sb.append(NEW_LINE);
        }
    }

    private String getIndexAndCurWP() {
        long c = nextWPCounter();
        return String.format("%d%s%d", c, SPACER_CHAR, (c == 0 ? 1 : 0));
    }

    /**
     * @return
     */
    private long getMAVFrame() {
        return mavFrame.value();
    }

    /**
     * 16  MAV_CMD_NAV_WAYPOINT    Navigate to MISSION.
     *     Mission Param #1    Hold time in decimal seconds. (ignored by fixed wing, time to stay at MISSION for rotary wing)
     *     Mission Param #2    Acceptance radius in meters (if the sphere with this radius is hit, the MISSION counts as reached)
     *     Mission Param #3    0 to pass through the WP, if > 0 radius in meters to pass by WP. Positive value for clockwise orbit, negative value for counter-clockwise orbit. Allows trajectory control.
     *     Mission Param #4    Desired yaw angle at MISSION (rotary wing). NaN for unchanged.
     *     Mission Param #5    Latitude
     *     Mission Param #6    Longitude
     *     Mission Param #7    Altitude 
     * @throws Exception 
     *     
     */
    private String getCommandGoto(double latDeg, double lonDeg, double z, Z_UNITS zUnits, double speedMS)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndexAndCurWP()).append(SPACER_CHAR);
        sb.append(getMAVFrame()).append(SPACER_CHAR);
        sb.append(16).append(SPACER_CHAR);
        sb.append(holdTimeCopter).append(SPACER_CHAR); // param 1
        sb.append(acceptanceRadius).append(SPACER_CHAR); // param 2
        sb.append(0).append(SPACER_CHAR); // param 3
        sb.append(0).append(SPACER_CHAR); // param 4
        sb.append(formatReal(latDeg, (short) 8)).append(SPACER_CHAR); // param 5
        sb.append(formatReal(lonDeg, (short) 8)).append(SPACER_CHAR); // param 6
        sb.append(formatZAndZUnits(z, zUnits)).append(SPACER_CHAR); // param 7
        sb.append(1); // AUTOCONTINUE=1
        return sb.toString();
    }

    /**
     * @param latDegs
     * @param lonDegs
     * @param z
     * @param zUnits
     * @param radius
     * @param direction
     * @param bearingDegs
     * @param speedMS
     * @return
     */
    private Object getCommandLoiter(double latDeg, double lonDeg, double z, Z_UNITS zUnits, double radius,
            String direction, double bearingDegs, double speedMS) throws Exception {

        long radiusAround = Math.abs(Math.round(radius));
        switch (direction) {
            case "Counter-Clockwise":
                radiusAround = -radiusAround;
                break;
            case "Vehicle Dependent":
            case "Clockwise":
            case "Into the Wind":
            default:
                break;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getIndexAndCurWP()).append(SPACER_CHAR);
        sb.append(getMAVFrame()).append(SPACER_CHAR);
        sb.append(17).append(SPACER_CHAR);
        sb.append(0).append(SPACER_CHAR); // param 1
        sb.append(0).append(SPACER_CHAR); // param 2
        sb.append(radiusAround).append(SPACER_CHAR); // param 3
        sb.append(Math.round(bearingDegs)).append(SPACER_CHAR); // param 4
        sb.append(formatReal(latDeg, (short) 8)).append(SPACER_CHAR); // param 5
        sb.append(formatReal(lonDeg, (short) 8)).append(SPACER_CHAR); // param 6
        sb.append(formatZAndZUnits(z, zUnits)).append(SPACER_CHAR); // param 7
        sb.append(1); // AUTOCONTINUE=1
        return sb.toString();
    }

    /**
     * @param value
     * @return
     */
    private String formatReal(double value) {
        return String.format(Locale.US, "%f", value);
    }

    /**
     * @param value
     * @param decimalPlaces
     * @return
     */
    private String formatReal(double value, short decimalPlaces) {
        if (decimalPlaces < 0)
            return formatReal(value);
        
        return String.format(Locale.US, "%." + decimalPlaces + "f", value);
    }

    /**
     * @param value
     * @return
     */
    private String formatInteger(long value) {
        return "" + value;
    }

    /**
     * @param z
     * @param zUnit
     * @return
     * @throws Exception
     */
    private String formatZAndZUnits(double z, Z_UNITS zUnit) throws Exception {
        Z_UNITS valid = Z_UNITS.NONE;
        switch (zUnit) {
            case HEIGHT:
                if (z > 0 && mavFrame == MAV_FRAME.MAV_FRAME_GLOBAL) {
                    valid = Z_UNITS.HEIGHT;
                    return formatInteger(Math.round(z));
                }
            case ALTITUDE:
                if (z > 0 && (mavFrame == MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT
                        || mavFrame == MAV_FRAME.MAV_FRAME_GLOBAL_TERRAIN_ALT)) {
                    valid = Z_UNITS.HEIGHT;
                    return formatInteger(Math.round(z));
                }
            case DEPTH:
            case NONE:
            default:
                String validStr = valid != Z_UNITS.NONE ? valid.toString() : Z_UNITS.HEIGHT + " or " + Z_UNITS.ALTITUDE;
                throw new Exception(
                        "Unsupported Z unit " + zUnit + ", valid are " + validStr);
        }
    }
}
