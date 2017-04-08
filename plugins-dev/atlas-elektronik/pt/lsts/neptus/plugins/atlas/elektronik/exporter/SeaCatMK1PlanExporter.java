/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * 06/04/2017
 */
package pt.lsts.neptus.plugins.atlas.elektronik.exporter;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import javax.swing.ProgressMonitor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.actions.PlanActions;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.ManeuversUtil;
import pt.lsts.neptus.mp.maneuvers.PathProvider;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;
import pt.lsts.neptus.types.mission.plan.IPlanFileExporter;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.UnitsUtil;

/**
 * @author pdias
 *
 */
public class SeaCatMK1PlanExporter implements IPlanFileExporter {

    private static final String NEW_LINE = "\r\n";
    /** Tue Dec 15 13:34:50 2009 */
    public static final SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
    public static HashMap<String, String> activeReplacementStringForPayload = new HashMap<>();
    static {
        try {
            String mapperTxt = IOUtils.toString(FileUtil.getResourceAsStream("payload-active-replacement.txt"));
            String[] lines = mapperTxt.split("[\r\n]");
            for (String ln : lines) {
                if (ln.startsWith("#") || ln.startsWith("%") || ln.startsWith(";"))
                    continue;
                try {
                    if (ln.isEmpty())
                        continue;
                    String[] pair = ln.trim().split(" {1,}");
                    if (pair.length < 2)
                        continue;
                    activeReplacementStringForPayload.put(pair[0].trim(), pair[1].trim());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // This shall be reseted at the beginning of exporting
    private long commandLineCounter = 1;
    private ArrayList<String> payloadsInPlan = new ArrayList<>();

    public SeaCatMK1PlanExporter() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.mission.plan.IPlanFileExporter#getExporterName()
     */
    @Override
    public String getExporterName() {
        return "SeaCat-MK1 Mission File";
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.mission.plan.IPlanFileExporter#validExtensions()
     */
    @Override
    public String[] validExtensions() {
        return new String[] { "seacat" };
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.types.mission.plan.IPlanFileExporter#exportToFile(pt.lsts.neptus.types.mission.plan.PlanType, java.io.File, javax.swing.ProgressMonitor)
     */
    @Override
    public void exportToFile(PlanType plan, File out, ProgressMonitor monitor) throws Exception {
        commandLineCounter = 1;
        payloadsInPlan.clear();
        
        String template = IOUtils.toString(FileUtil.getResourceAsStream("template.mis"));

        template = replaceTokenWithKey(template, "GenDate", getTimeStamp());
        template = replaceTokenWithKey(template, "LowBatteryState", getSectionLowBatteryState());
        template = replaceTokenWithKey(template, "EmergencyRendezvousPoint", getSectionEmergencyRendezvousPoint());
        template = replaceTokenWithKey(template, "KeepPosition", getSectionKeepPositionAtMissionEnd());
        template = replaceTokenWithKey(template, "AutonomyArea", getSectionAutonomyArea());
        template = replaceTokenWithKey(template, "ExplorationArea", getSectionExplorationArea());
        template = replaceTokenWithKey(template, "SafeAltitude", getSectionSafeAltitude());
        template = replaceTokenWithKey(template, "SystemPayload", getSectionSystemPayload());
        template = replaceTokenWithKey(template, "SwapPayload", getSectionSwapPayload());
        template = replaceTokenWithKey(template, "Body", getSectionBody(plan));

        FileUtils.write(out, template);
    }
    
    /**
     * Tue Dec 15 13:34:50 2009
     * 
     * @return
     */
    private String getTimeStamp() {
        return dateFormatter.format(new Date());
    }
    
    /**
     * @return
     */
    private String getSectionLowBatteryState() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

    /**
     * @return
     */
    private String getSectionEmergencyRendezvousPoint() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

    /**
     * @return
     */
    private String getSectionKeepPositionAtMissionEnd() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

    /**
     * @return
     */
    private String getSectionAutonomyArea() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

    /**
     * @return
     */
    private String getSectionExplorationArea() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

    /**
     * @return
     */
    private String getSectionSafeAltitude() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

    /**
     * @return
     */
    private String getSectionSystemPayload() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

    /**
     * @return
     */
    private String getSectionSwapPayload() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

    /**
     * @param plan 
     * @return
     * @throws Exception 
     */
    private String getSectionBody(PlanType plan) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Maneuver m : plan.getGraph().getManeuversSequence()) {
            double speedKnots = ManeuversUtil.getSpeedMps(m);
            if (!Double.isNaN(speedKnots))
                speedKnots *= UnitsUtil.MS_TO_KNOT;
            
            if (m instanceof PathProvider) {
                sb.append(getPayloadSettingsFromManeuver(m));

                Collection<ManeuverLocation> waypoints = ((LocatedManeuver) m).getWaypoints();
                waypoints.stream().forEach(wp -> wp.convertToAbsoluteLatLonDepth());

                ManeuverLocation prevWp = null;
                double curHeadingRad = Double.NaN;
                for (ManeuverLocation wp : ((LocatedManeuver) m).getWaypoints()) {
                    if (prevWp != null) {
                        curHeadingRad = AngleUtils.nomalizeAngleRadsPi(wp.getXYAngle(prevWp));
                    }
                    
                    sb.append(getCommandGoto(wp.getLatitudeDegs(), wp.getLongitudeDegs(), wp.getZ(), wp.getZUnits(),
                            speedKnots));
                    prevWp = wp;
                }
            }
            else if (m instanceof StationKeeping) {
                sb.append(getPayloadSettingsFromManeuver(m));
                
                ManeuverLocation wp = ((StationKeeping) m).getManeuverLocation();
                wp.convertToAbsoluteLatLonDepth();
                sb.append(getCommandKeepPosition(wp.getLatitudeDegs(), wp.getLongitudeDegs(), wp.getZ(), wp.getZUnits(),
                        ((StationKeeping) m).getDuration()));
            }
            else if (m instanceof Goto) { // Careful with ordering because of extensions
                if (Double.isNaN(speedKnots))
                    continue;
                
                sb.append(getPayloadSettingsFromManeuver(m));
                
                ManeuverLocation wp = ((Goto) m).getManeuverLocation();
                wp.convertToAbsoluteLatLonDepth();
                sb.append(getCommandGoto(wp.getLatitudeDegs(), wp.getLongitudeDegs(), wp.getZ(), wp.getZUnits(),
                        speedKnots));
            }
            sb.append(NEW_LINE);
        }
        

        sb.append(getCommandsBeforeEnd());
        sb.append(getCommandEnd(true));
        
        return sb.toString();
    }

    /**
     * @param m
     * @return
     */
    private String getPayloadSettingsFromManeuver(Maneuver m) {
        StringBuilder sb = new StringBuilder();
        PlanActions pActions = m.getStartActions();
        for (IMCMessage msg : pActions.getAllMessages()) {
            try {
                if (msg instanceof SetEntityParameters) {
                    SetEntityParameters sep = (SetEntityParameters) msg;
                    switch (sep.getName()) {
                        case "ObstacleAvoidance":
                            sb.append(getSettingObstacleAvoidance(
                                    Boolean.parseBoolean(sep.getParams().get(0).getValue())));
                            break;
                        case "ExternalControl":
                            sb.append(
                                    getSettingExternalControl(Boolean.parseBoolean(sep.getParams().get(0).getValue())));
                            break;
                        case "Acoms":
                            Vector<EntityParameter> params = sep.getParams();
                            if (Boolean.parseBoolean(params.get(0).getValue())) { // "Auto Send" parameter
                                EntityParameter pRep = getParamWithName(params, "Repetitions");
                                EntityParameter pInt = getParamWithName(params, "Interval");
                                sb.append(getSetting('Q', "Acoms", pRep.getValue(), pInt.getValue()));
                            }
                            else {
                                sb.append(getSetting('Q', "Acoms", "0"));
                            }
                            break;
                        default:
                            sb.append(processPayload(sep.getName(), sep.getParams()));
                            break;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return sb.toString();
    }

    /**
     * Example: Edgetech2205 RANGE:50;GAIN:100;OPMODE:HF;PING:ON_LOG
     * 
     * @param name
     * @param params
     * @return
     */
    private String processPayload(String payloadName, Vector<EntityParameter> params) {
        StringBuilder sb = new StringBuilder();
        for (EntityParameter ep : params) {
            String name = ep.getName();
            boolean activeKey = false;
            boolean activeValue = false;
            if (name.equalsIgnoreCase("Active")) {
                name = translatePayloadActiveFor(payloadName);
                activeKey = true;
            }
            else {
                name = name.replaceAll(" {1,}", "-").toUpperCase();
            }
            String value = ep.getValue();
            if (value.equalsIgnoreCase("true")) {
                value = "ON";
                activeValue = true;
            }
            else if (value.equalsIgnoreCase("false")) {
                value = "OFF";
                activeValue = false;
            }
            sb.append(name.toUpperCase());
            sb.append(":");
            sb.append(value.toUpperCase());
            if (activeKey && !activeValue) {
                // Mark payload for mission end switch off
                if (!payloadsInPlan.contains(payloadName))
                    payloadsInPlan.add(payloadName);

                break;
            }
            sb.append(";");
        }
        
        return getSetting('P', payloadName, sb.toString());
    }

    /**
     * @param payloadName
     * @param name
     * @return
     */
    private String translatePayloadActiveFor(String payloadName) {
        String name = activeReplacementStringForPayload.get(payloadName);
        return name == null ? "ACTIVE" : name;
    }

    /**
     * @param params
     * @param name
     * @return
     */
    private EntityParameter getParamWithName(Vector<EntityParameter> params, String name) {
        for (EntityParameter ep : params) {
            if (ep.getName().equalsIgnoreCase(name))
                return ep;
        }
        return null;
    }

    /**
     * @param original
     * @param key
     * @param replacement
     * @return
     */
    private String replaceTokenWithKey(String original, String key, String replacement) {
        return original.replaceAll("\\$\\{" + key + "\\}", replacement);
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
     * The depth mode might read as ‘D’ – constant diving depth – or ‘A’ – constant altitude mode
     * 
     * @param depthUnit
     * @return
     * @throws Exception 
     */
    private String formatDepthUnit(Z_UNITS depthUnit) throws Exception {
        switch (depthUnit) {
            case ALTITUDE:
                return "A";
            case DEPTH:
                return "D";
            case HEIGHT:
            case NONE:
            default:
                throw new Exception("Unsupported Z unit " + depthUnit + ", valid are " + 
                        Z_UNITS.DEPTH + " or " + Z_UNITS.ALTITUDE);
        }
    }

    /**
     * C [Line_Number] [Identifier] [Manoeuvre_Descriptor] [Parameter]
     * 
     * Each command starts with a “C” character followed by a line number. Line numbers must be in ascending order, but
     * not necessary of step-size one. A following string helps to make it human readable, it is not evaluated. The
     * string must not exceed 20 characters and may not contain blanks. The command is identified by a character which
     * is the identifier of the command and determines which parameter will follow.
     * 
     * @return
     */
    private String getCommand(Character command, String humanReadableCommand, String... parameter) {
        StringBuilder sb = new StringBuilder();
        sb.append("C ");
        sb.append(commandLineCounter++);
        sb.append(" ");
        sb.append(Character.toUpperCase(command));
        sb.append(" ");
        sb.append(humanReadableCommand.substring(0, Math.min(20, humanReadableCommand.length())).replace(" ", "_"));
        for (String st : parameter) {
            sb.append(" ");
            sb.append(st);
        }
        sb.append(NEW_LINE);
        return sb.toString();
    }

    private String getSetting(Character command, String humanReadableCommand, String... parameter) {
        StringBuilder sb = new StringBuilder();
        sb.append("S ");
        sb.append(commandLineCounter++);
        sb.append(" ");
        sb.append(Character.toUpperCase(command));
        sb.append(" ");
        sb.append(humanReadableCommand.substring(0, Math.min(20, humanReadableCommand.length())).replace(" ", "_"));
        for (String st : parameter) {
            sb.append(" ");
            sb.append(st);
        }
        sb.append(NEW_LINE);
        return sb.toString();
    }

    /**
     * @param active
     * @return
     */
    private String getSettingExternalControl(boolean active) {
        return getSetting('R', "ExternalControl", active ? "1" : "0");
    }

    /**
     * @param active
     * @return
     */
    private String getSettingObstacleAvoidance(boolean active) {
        return getSetting('O', "ObstacleAvoidance", active ? "1" : "0");
    }

    /**
     * C n A Goto Latitude Longitude depth depth-mode speed
     * 
     * @param latDegs
     * @param lonDegs
     * @param depth
     * @param depthUnit
     * @param speedKnots
     * @return
     * @throws Exception 
     */
    private String getCommandGoto(double latDegs, double lonDegs, double depth, ManeuverLocation.Z_UNITS depthUnit,
            double speedKnots) throws Exception {
        return getCommand('A', "Goto", formatReal(latDegs), formatReal(lonDegs), formatReal(depth, (short) 1),
                formatDepthUnit(depthUnit), formatReal(speedKnots, (short) 1));
    }

    /**
     * C n B Goto direction distance depth depth-mode speed
     * 
     * @param directionDegs
     * @param distanceMeters
     * @param depth
     * @param depthUnit
     * @param speedKnots
     * @return
     * @throws Exception 
     */
    private String getCommandGotoDirection(double directionDegs, double distanceMeters, double depth, ManeuverLocation.Z_UNITS  depthUnit,
            double speedKnots) throws Exception {
        double dir = AngleUtils.nomalizeAngleDegrees360(directionDegs);
        return getCommand('B', "Goto", formatReal(dir, (short) 1), formatReal(distanceMeters, (short) 1), formatReal(depth, (short) 1),
                formatDepthUnit(depthUnit), formatReal(speedKnots, (short) 1));
    }


    /**
     * C n C Curve target-latitude target-longitude center-latitude center-longitude direction depth depth-mode speed
     * 
     * The direction may be either ‘R’ for clockwise turning or ‘L’ for counter-clockwise turnings. All other
     * parameters are the same as in the straight Goto command.
     *
     * @param targetLatDegs
     * @param targetLonDegs
     * @param centerLatDegs
     * @param centerLonDegs
     * @param direction
     * @param depth
     * @param depthUnit
     * @param speedKnots
     * @return
     * @throws Exception 
     */
    private String getCommandCurve(double targetLatDegs, double targetLonDegs, double centerLatDegs,
            double centerLonDegs, Character direction, double depth, ManeuverLocation.Z_UNITS  depthUnit, double speedKnots) throws Exception {
        return getCommand('C', "Curve", formatReal(targetLatDegs), formatReal(targetLonDegs),
                formatReal(centerLatDegs), formatReal(centerLonDegs), direction == 'L' ? "L" : "R",
                formatReal(depth, (short) 1), formatDepthUnit(depthUnit), formatReal(speedKnots, (short) 1));
    }

    /**
     * The command ‘K’makes the AUV to keep its position at the desired latitude an longitude in a desired depth and
     * depth-mode for a desired time in seconds. 
     * 
     * C n K KeepPosition latitude longitude depth deph-mode time
     * @throws Exception 
     */
    private String getCommandKeepPosition(double latDegs, double lonDegs, double depth, ManeuverLocation.Z_UNITS  depthUnit,
            long timeSeconds) throws Exception {
        return getCommand('K', "KeepPosition", formatReal(latDegs), formatReal(lonDegs),
                formatReal(depth, (short) 1), formatDepthUnit(depthUnit), formatInteger(timeSeconds));
    }

    /**
     * @return
     */
    private Object getCommandsBeforeEnd() {
        StringBuilder sb = new StringBuilder();

        // Disabled settings
        sb.append(getSettingObstacleAvoidance(false));
        sb.append(getSettingExternalControl(false));
        
        // Switch off payloads
        for (String payloadName : payloadsInPlan) {
            String name = translatePayloadActiveFor(payloadName);
            sb.append(getSetting('P', payloadName, name.toUpperCase() + ":OFF"));
        }
        
        return sb.toString();
    }

    /**
     * C 100 Z MissionEnd P
     * 
     * @param keepPosOrDrift true to keep the position at the end or false for drift.
     * @return
     */
    private String getCommandEnd(boolean keepPosOrDrift) {
       return getCommand('Z', "MissionEnd", keepPosOrDrift ? "P" : "D"); 
    }
}
