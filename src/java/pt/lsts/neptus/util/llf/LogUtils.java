/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 200?/??/??
 */
package pt.lsts.neptus.util.llf;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jfree.chart.JFreeChart;

import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.GpsFix;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.LblBeacon;
import pt.lsts.imc.PathControlState;
import pt.lsts.imc.SonarData;
import pt.lsts.imc.VehicleCommand;
import pt.lsts.imc.VehicleCommand.COMMAND;
import pt.lsts.imc.VehicleCommand.TYPE;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.imc.lsf.LsfIterator;
import pt.lsts.imc.types.PlanSpecificationAdapter;
import pt.lsts.imc.types.PlanSpecificationAdapter.Transition;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.OperationLimits;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.maneuvers.Unconstrained;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;

/** 
 * @author pdias
 * @author ZP
 * 
 */
public class LogUtils {

    public enum LogValidity {
       VALID, NO_DIRECTORY, NO_XML_DEFS, NO_VALID_LOG_FILE 
    };
    
    public static LinkedHashMap<String, String> generateStatistics(IMraLogGroup source) {
        if (source.getLog("EstimatedState") == null) {
            return new LinkedHashMap<String, String>();
        }

        IMraLog parser = source.getLog("EstimatedState");
        IMCMessage entry = parser.firstLogEntry();

        long startMillis = parser.getCurrentEntry().getTimestampMillis();
        long startMillis2 = parser.firstLogEntry().getTimestampMillis();
        
        NeptusLog.pub().info("<###> "+startMillis + "" + startMillis2);
        double lastTime = 0;

        double maxDepth = 0;
        double avgDepth = entry.getDouble("depth"); // z

        double maxRoll = 0;
        double minRoll = 0;

        double maxPitch = 0;
        double minPitch = 0;

        long numStates = 1;

        double avgRoll = entry.getDouble("phi");
        double avgPitch = entry.getDouble("theta");

        double distance = 0;
        IMCMessage prevEntry = null;
        
        CorrectedPosition corPosition = new CorrectedPosition(source);
        
        LocationType lastLoc = null;

        Iterator<SystemPositionAndAttitude> iter = corPosition.iterator();
        while (iter.hasNext()) {
            SystemPositionAndAttitude loc = iter.next();
            if (lastLoc != null)
                distance += loc.getPosition().getDistanceInMeters(lastLoc);
            lastLoc = loc.getPosition();
        }
        
        while ((entry = parser.nextLogEntry()) != null) {
            
            double depth = entry.getDouble("depth"); // z
            maxDepth = Math.max(maxDepth, entry.getDouble("depth")); // z
            double phi = entry.getDouble("phi");
            double theta = entry.getDouble("theta");

            maxRoll = Math.max(maxRoll, phi);
            minRoll = Math.min(minRoll, phi);
            maxPitch = Math.max(maxPitch, theta);
            minPitch = Math.min(minPitch, theta);

            avgDepth = (avgDepth * numStates + depth) / (numStates + 1);

            avgRoll = (avgRoll * numStates + phi) / (numStates + 1);
            avgPitch = (avgPitch * numStates + theta) / (numStates + 1);

            numStates++;
            
            prevEntry = entry;
            lastTime = prevEntry.getTimestamp();
        }

        lastTime = parser.getLastEntry().getTimestamp();
        
        LinkedHashMap<String, String> stats = new LinkedHashMap<String, String>();

        long endMillis = (long) (lastTime * 1000.0); //(long) (startMillis + lastTime * 1000.0);

        Date ds = new Date(startMillis);
        Date df = new Date(endMillis);

        stats.put(I18n.text("Vehicle"), "" + LogUtils.getVehicle(source));
        stats.put(I18n.text("Mission start time"), "" + ds);
        stats.put(I18n.text("Mission end time"), "" + df);
        stats.put(I18n.text("Mission duration"), DateTimeUtil.milliSecondsToFormatedString(endMillis - startMillis));
        stats.put(I18n.text("Maximum depth"), GuiUtils.getNeptusDecimalFormat(2).format(maxDepth) + " " + I18n.textc("m", "meters"));
        stats.put(I18n.text("Avg depth"), GuiUtils.getNeptusDecimalFormat(2).format(avgDepth) + " " + I18n.textc("m", "meters"));

        stats.put(I18n.text("Roll min/max/amp/avg"), GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(minRoll)) + "\u00B0 / "
                + GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(maxRoll)) + "\u00B0 / "
                + GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(maxRoll - minRoll)) + "\u00B0 / "
                + GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(avgRoll)) + "\u00B0");

        stats.put(I18n.text("Pitch min/max/amp/avg"), GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(minPitch)) + "\u00B0 / "
                + GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(maxPitch)) + "\u00B0 / "
                + GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(maxPitch - minPitch)) + "\u00B0 / "
                + GuiUtils.getNeptusDecimalFormat(2).format(Math.toDegrees(avgPitch)) + "\u00B0");

        stats.put(I18n.text("Distance travelled"), GuiUtils.getNeptusDecimalFormat(2).format(distance) + " " + I18n.textc("m", "meters"));
        stats.put(I18n.text("Mean speed"),
                GuiUtils.getNeptusDecimalFormat(2).format(distance / ((endMillis - startMillis) / 1000.0)) + " " + I18n.text("m/s"));

        LocationType loc = LogUtils.getHomeRef(source);
        if (loc != null) {
            stats.put(I18n.text("Home Latitude"), loc.getLatitudeAsPrettyString());
            stats.put(I18n.text("Home Longitude"), loc.getLongitudeAsPrettyString());
        }
        return stats;
    }

    public static Date getStartDate(IMraLogGroup source) {

        if (source.getLog("EstimatedState") == null)
            return null;

        long startMillis = source.getLog("EstimatedState").currentTimeMillis();
        return new Date(startMillis);

    }

    public static Date[] getMessageMinMaxDates(IMraLog msgLog) {
        if (msgLog == null)
            return null;

        long startMillis = msgLog.firstLogEntry().getTimestampMillis();
        long endMillis = msgLog.getLastEntry().getTimestampMillis();
        return new Date[] { new Date(startMillis), new Date(endMillis) };
    }
    
    public static LocationType getFirstValidLocation(IMraLogGroup source) {
        LsfIndex index = source.getLsfIndex();
        
        if (index.containsMessagesOfType(EstimatedState.class.getSimpleName())) {
            return IMCUtils.getLocation(index.getFirst(EstimatedState.class));
        }
        else if (index.containsMessagesOfType(GpsFix.class.getSimpleName())) {
            LsfIterator<GpsFix> it = index.getIterator(GpsFix.class);
            
            while (it.hasNext()) {
                GpsFix fix = it.next();
                if ((fix.getValidity() & GpsFix.GFV_VALID_POS) != 0) {
                    LocationType loc = new LocationType();
                    loc.setLatitudeRads(fix.getLat());
                    loc.setLongitudeRads(fix.getLon());
                    return loc;
                }                
            }
        }        
        return null;
    }

    public static MissionType generateMission(IMraLogGroup source) {

        MissionType mission = new MissionType();

        // home ref
        LocationType lt = getHomeRef(source);
        if (lt != null) {
            CoordinateSystem cs = new CoordinateSystem();
            cs.setLocation(lt);
            mission.setHomeRef(cs);
        }

        MapType map = new MapType();

        MapMission mm = new MapMission();
        mm.setId(map.getId());
        mm.setMap(map);

        MarkElement start = new MarkElement();
        start.setId("start");
        map.addObject(start);

        LocationType sloc = getStartupPoint(source);
        if (sloc != null)
            start.setCenterLocation(sloc);
        else
            start.setCenterLocation(new LocationType(mission.getHomeRef()));

        mission.addMap(mm);

        TransponderElement[] te = getTransponders(source);
        for (TransponderElement t : te) {
            t.setParentMap(map);
            t.setMapGroup(map.getMapGroup());
            map.addObject(t);
        }

        return mission;
    }
    
    public static LocationType getHomeRef(IMraLogGroup source) {
        LocationType loc = getFirstValidLocation(source);
        
        if (loc == null)
            loc = new LocationType();
        
        return loc;
    }

    public static OperationLimits getOperationLimits(IMraLogGroup source) {
        IMraLog parser = source.getLog("OperationalLimits");
        if (parser == null)
            return null;

        IMCMessage lastEntry = parser.getLastEntry();
        if (lastEntry == null)
            return null;

        OperationLimits limits = new OperationLimits();
        LinkedHashMap<String, Boolean> bitmask = lastEntry.getBitmask("mask");
        if (bitmask.get("MAX_DEPTH"))
            limits.setMaxDepth(lastEntry.getDouble("max_depth"));
        if (bitmask.get("MIN_ALT"))
            limits.setMinAltitude(lastEntry.getDouble("min_altitude"));
        if (bitmask.get("MAX_ALT"))
            limits.setMaxAltitude(lastEntry.getDouble("max_altitude"));
        if (bitmask.get("MIN_SPEED"))
            limits.setMinSpeed(lastEntry.getDouble("min_speed"));
        if (bitmask.get("MAX_SPEED"))
            limits.setMaxSpeed(lastEntry.getDouble("max_speed"));
        if (bitmask.get("MAX_VRATE"))
            limits.setMaxVertRate(lastEntry.getDouble("max_vrate"));
        if (bitmask.get("AREA")) {
            limits.setOpAreaLat(Math.toDegrees(lastEntry.getDouble("lat")));
            limits.setOpAreaLon(Math.toDegrees(lastEntry.getDouble("lon")));
            limits.setOpRotationRads(lastEntry.getDouble("orientation"));
            limits.setOpAreaWidth(lastEntry.getDouble("width"));
            limits.setOpAreaLength(lastEntry.getDouble("length"));
        }
        return limits;
    }
    
    public static LocationType getStartupPoint(IMraLogGroup source, int src) {
        IMraLog parser = source.getLog("NavigationStartupPoint");
        if (parser != null) {
            IMCMessage entry = parser.getCurrentEntry();
            while (entry != null) {
                if (entry.getHeader().getInteger("src") != src) {
                    entry = parser.nextLogEntry();
                    continue;
                }
                double lat = entry.getDouble("lat");
                double lon = entry.getDouble("lon");
                double depth = entry.getDouble("depth");
                lat = Math.toDegrees(lat);
                lon = Math.toDegrees(lon);

                LocationType center = new LocationType();
                center.setLatitudeDegs(lat);
                center.setLongitudeDegs(lon);
                center.setDepth(depth);
                return center;
            }
        }

        return null;
    }
    
    public static LocationType getStartupPoint(IMraLogGroup source) {
        IMraLog parser = source.getLog("NavigationStartupPoint");
        if (parser != null) {
            IMCMessage lastEntry = parser.getLastEntry();

            double lat = lastEntry.getDouble("lat");
            double lon = lastEntry.getDouble("lon");
            double depth = lastEntry.getDouble("depth");
            lat = Math.toDegrees(lat);
            lon = Math.toDegrees(lon);

            LocationType center = new LocationType();
            center.setLatitudeDegs(lat);
            center.setLongitudeDegs(lon);
            center.setDepth(depth);
            return center;
        }
        return null;
    }
    
    public static TransponderElement[] getTransponders(IMraLogGroup source) {
        IMraLog parser = source.getLog("LblConfig");
        if (parser == null)
            return new TransponderElement[0];
        
        Vector<TransponderElement> transp = new Vector<TransponderElement>();
        
        try {
            IMCMessage config = parser.getLastEntry();
            
            if(config.getMessageList("beacons") != null) {
                for (IMCMessage lblBeacon : config.getMessageList("beacons")) {
                    String beacon = lblBeacon.getString("beacon");
                    double lat = Math.toDegrees(lblBeacon.getDouble("lat"));
                    double lon = Math.toDegrees(lblBeacon.getDouble("lon"));
                    double depth = lblBeacon.getDouble("depth");
                    TransponderElement el = new TransponderElement();
                    LocationType lt = new LocationType();
                    lt.setLatitudeDegs(lat);
                    lt.setLongitudeDegs(lon);
                    lt.setDepth(depth);
                    el.setId(beacon);
                    el.setCenterLocation(lt);
                    transp.add(el);
                }
            } 
            else {
                for (int i = 0; i < 6; i++) {
                    IMCMessage msg = config.getMessage("beacon" + i);
                    if (msg == null)
                        continue;
                    LblBeacon lblBeacon = LblBeacon.clone(msg);
                    String beacon = lblBeacon.getBeacon();
                    double lat = Math.toDegrees(lblBeacon.getLat());
                    double lon = Math.toDegrees(lblBeacon.getLon());
                    double depth = lblBeacon.getDepth();
                    TransponderElement el = new TransponderElement();
                    LocationType lt = new LocationType();
                    lt.setLatitudeDegs(lat);
                    lt.setLongitudeDegs(lon);
                    lt.setDepth(depth);
                    el.setId(beacon);
                    el.setCenterLocation(lt);
                    transp.add(el);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();            
        }
        return transp.toArray(new TransponderElement[0]);
    }

    @Deprecated
    public static boolean isValidLogFolder(File dir) {
        if (!dir.isDirectory() || !dir.canRead())
            return false;

        for (File f : dir.listFiles()) {
            if (FileUtil.getFileExtension(f).equalsIgnoreCase("llf"))
                return f.canRead();
            if (f.getName().equalsIgnoreCase("data.bsf"))
                return f.canRead();
        }
        return false;
    }

    @Deprecated
    public static boolean isValidZipSource(File zipFile) {
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                // if (ze.getName().equals("EstimatedState.llf"))
                if (FileUtil.getFileExtension(ze.getName()).equalsIgnoreCase("llf")) {
                    zis.close();
                    return true;
                }
                ze = zis.getNextEntry();
            }
            zis.close();
        }
        catch (Exception e) {
        }
        return false;
    }

    public static LogValidity isValidLSFSource(File dir) {
        if (!dir.isDirectory() || !dir.canRead())
            return LogValidity.NO_DIRECTORY;

        short lsfFx = 0, lsfGzFx = 0, lsfBZip2Fx = 0, defXmlFx = 0;
        for (File f : dir.listFiles()) {
            switch (FileUtil.getFileExtension(f)) {
                case "lsf":
                    lsfFx++;
                    break;
                case "gz":
                    String fex = FileUtil.getFileNameWithoutExtension(f.getName());
                    if (FileUtil.getFileExtension(fex).equalsIgnoreCase("lsf"))
                        lsfGzFx++;
                    else if (FileUtil.getFileExtension(fex).equalsIgnoreCase("xml"))
                        defXmlFx++;
                    break;
                case "xml":
                    defXmlFx++;
                    break;
                case "bz2":
                    fex = FileUtil.getFileNameWithoutExtension(f.getName());
                    if (FileUtil.getFileExtension(fex).equalsIgnoreCase("lsf"))
                        lsfBZip2Fx++;
                    break;
                default:
                    break;
            }
        }
        if ((lsfFx + lsfGzFx + lsfBZip2Fx) > 0 && defXmlFx > 0)
            return LogValidity.VALID;
        
        if ((lsfFx + lsfGzFx + lsfBZip2Fx) == 0)
            return LogValidity.NO_VALID_LOG_FILE;
        
        return LogValidity.NO_XML_DEFS;
    }

    /**
     * Return from a log folder a valid {@link FileUtil#FILE_TYPE_LSF} (compressed or not)
     * 
     * @param logFolder
     * @return
     */
    public static File getValidLogFileFromLogFolder(File logFolder) {
        if (!logFolder.exists())
            return null;

        File logLsf = null;
        File logLsfGz = null;
        File logLsfBz2 = null;
        ArrayList<File> files = new ArrayList<File>(Arrays.asList(logFolder.listFiles()));
        Collections.sort(files);
        
        File fxLogFound = null;
        String fxBase = "Data";
        List<String> acceptableFiles = Arrays.asList(fxBase + "." + FileUtil.FILE_TYPE_LSF,
                fxBase + "." + FileUtil.FILE_TYPE_LSF_COMPRESSED,
                fxBase + "." + FileUtil.FILE_TYPE_LSF_COMPRESSED_BZIP2);
        List<File> rLogs = files.stream().filter(f -> acceptableFiles.contains(f.getName()))
                .collect(Collectors.toList());
        if (!rLogs.isEmpty()) {
            rLogs.sort(new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    String e1 = FileUtil.getFileExtension(f1);
                    String e2 = FileUtil.getFileExtension(f2);
                    if (e1.equalsIgnoreCase(e2)) {
                        return f1.compareTo(f2);
                    }
                    else {
                        if (e1.endsWith("lsf"))
                            return -1;
                        else if (e2.endsWith("lsf"))
                            return 1;
                        else if (e1.endsWith("gz"))
                            return -1;
                        else if (e2.endsWith("gz"))
                            return 1;
                        else if (e1.endsWith("bz2"))
                            return -1;
                        else if (e2.endsWith("bz2"))
                            return 1;
                    }
                    return 0;
                }
            });
            fxLogFound = rLogs.get(0);
        }
        
        if (fxLogFound == null) {
            sel : 
                for (File fx : files) {
                    switch (FileUtil.getFileExtension(fx)) {
                        case FileUtil.FILE_TYPE_LSF:
                            logLsf = fx;
                            break sel;
                        case "gz":
                            String fex = FileUtil.getFileNameWithoutExtension(fx.getName());
                            if (FileUtil.getFileExtension(fex).equalsIgnoreCase(FileUtil.FILE_TYPE_LSF))
                                logLsfGz = fx;
                            break sel;
                        case "bz2":
                            fex = FileUtil.getFileNameWithoutExtension(fx.getName());
                            if (FileUtil.getFileExtension(fex).equalsIgnoreCase(FileUtil.FILE_TYPE_LSF))
                                logLsfBz2 = fx;
                            break sel;
                        default:
                            break;
                    }
                }
        }

        File ret = null;
        if (fxLogFound != null)
            ret = fxLogFound;
        else if (logLsf != null)
            ret = logLsf;
        else if (logLsfGz != null)
            ret = logLsfGz;
        else if (logLsfBz2 != null)
            ret = logLsfBz2;
        
        return ret;
    }
    
    /**
     * @author zp
     * @param mt
     * @param source
     * @return
     */
    public static PlanType generatePlan(MissionType mt, IMraLogGroup source) {
        try {
            IMraLog log = source.getLog("PlanSpecification");
            if (log == null || log.getNumberOfEntries() > 1)
                return generatePlanFromVehicleCommands(mt, source);
            
            PlanSpecificationAdapter imcPlan = new PlanSpecificationAdapter(log.getLastEntry());
            
            
            PlanType plan = new PlanType(mt);
            plan.setId(imcPlan.getPlanId());
            
            for (String manId : imcPlan.getAllManeuvers().keySet()) {
                IMCMessage maneuver = imcPlan.getAllManeuvers().get(manId);
                Maneuver man = parseManeuver(manId, maneuver);
                plan.getGraph().addManeuver(man);
            }

            for (Transition imcTransition: imcPlan.getAllTransitions()) {
                plan.getGraph().addTransition(imcTransition.getSourceManeuver(), imcTransition.getDestManeuver(), imcTransition.getConditions());                
            }
            
            plan.getGraph().setInitialManeuver(imcPlan.getFirstManeuverId());
            
            return plan;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * @author zp
     * @param mt
     * @param source
     * @return
     */
    public static PlanType generatePlanFromVehicleCommands(MissionType mt, IMraLogGroup source) {
        try {
            IMraLog log = source.getLog("VehicleCommand");
            
            if (log == null)
                return null;
            
            PlanType pt = new PlanType(mt);
            pt.setId("Executed");

            IMCMessage msg = log.nextLogEntry();
            
            int count = 1;
            
            while (msg != null) {
                VehicleCommand cmd = VehicleCommand.clone(msg);
                if (cmd.getType() == TYPE.REQUEST && cmd.getCommand() == COMMAND.EXEC_MANEUVER) {
                    IMCMessage maneuver = cmd.getManeuver();
                    if (maneuver != null) {
                        String id = ""+(count++);
                        Maneuver man = parseManeuver(id, maneuver);
                        pt.getGraph().addManeuver(man);   
                    }
                }
                
                msg = log.nextLogEntry();
                
            }
            
            return pt;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * See {@link IMCUtils#parseManeuver(IMCMessage)}
     */
    protected static Maneuver parseManeuver(String manId, IMCMessage msg) {
        Maneuver maneuver = IMCUtils.parseManeuver(msg);
        if (maneuver != null) {
            maneuver.setId(manId);
        }
        else {
            String manType = msg.getAbbrev();
            if (!"Teleoperation".equalsIgnoreCase(manType) && (maneuver instanceof Unconstrained))
                maneuver = null;
        }
        
        return maneuver;
    }

    public static String parseInlineName(String data) {
        if (data.startsWith("%INLINE{")) {
            return data.substring("%INLINE{".length(), data.length() - 1);
        }
        return null;
    }

    public static LocationType getLocation(IMCMessage estimatedStateMessage) {
        try {
            if (estimatedStateMessage != null) {
                LocationType loc = new LocationType();

                // 0 -> NED ONLY, 1 -> LLD ONLY, 2 -> NED_LLD
                long refMode = estimatedStateMessage.getLong("ref");
                
                // IMC5 Compatibility
                if(!estimatedStateMessage.getMessageType().getFieldNames().contains("ref")) {
                    refMode = 2;
                }
                
                if (refMode == 1 || refMode == 2) {
                    loc.setLatitudeRads(estimatedStateMessage.getDouble("lat"));
                    loc.setLongitudeRads(estimatedStateMessage.getDouble("lon"));
                    loc.setDepth(estimatedStateMessage.getDouble("depth"));
                }
                if (refMode == 0 || refMode == 2) {
                    loc.translatePosition(estimatedStateMessage.getDouble("x"), estimatedStateMessage.getDouble("y"),
                            estimatedStateMessage.getDouble("z"));
                }
                loc.convertToAbsoluteLatLonDepth();
                return loc;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    

    /**
     * @param estimatedStateEntry
     * 
     */
    public static LocationType getLocation(LocationType baseLoc, IMCMessage estimatedStateEntry) {
        LocationType loc = getLocation(estimatedStateEntry);
        if (loc == null)
            return null;

        long refMode = estimatedStateEntry.getLong("ref");
        if (refMode == 0) {
            loc.setLatitudeDegs(baseLoc.getLatitudeDegs());
            loc.setLongitudeDegs(baseLoc.getLongitudeDegs());
            loc.setDepth(baseLoc.getDepth());
        }
        return loc;
    }

    public static PathElement generatePath(MissionType mission, IMraLogGroup source) {
        MapType mt = new MapType();
        LocationType lt = new LocationType(mission.getStartLocation());
        PathElement pe = new PathElement(MapGroup.getMapGroupInstance(mission), mt, lt);
        pe.setParentMap(mt);
        mt.addObject(pe);

        IMraLog parser = source.getLog("EstimatedState");
        if (parser == null)
            return pe;

        IMCMessage entry = parser.nextLogEntry();

        // entry = parser.getEntryAfter(11.0);
        LocationType tmp = new LocationType();

        while (entry != null) {
            parser.advance(100);
            entry = parser.nextLogEntry();
            if (entry != null) {
                long refMode = entry.getLong("ref");
                if (refMode == 0) {
                    pe.addPoint(entry.getDouble("y"),entry.getDouble("x"), entry.getDouble("z"),false);
                }
                else if (refMode == 1) {
                    double lat = entry.getDouble("lat");
                    double lon = entry.getDouble("lon");
                    double depth = entry.getDouble("depth");

                    if (lat != tmp.getLatitudeDegs() && lon != tmp.getLongitudeDegs()) {
                        tmp.setLatitudeDegs(Math.toDegrees(lat));
                        tmp.setLongitudeDegs(Math.toDegrees(lon));
                        tmp.setDepth(depth);
                        double[] offs = tmp.getOffsetFrom(mission.getStartLocation());
                        pe.addPoint(offs[1], offs[0], offs[2], false);
                    }
                }
                else if (refMode == 2) {
                    double lat = entry.getDouble("lat");
                    double lon = entry.getDouble("lon");
                    double depth = entry.getDouble("depth");
                    double x = entry.getDouble("x");
                    double y = entry.getDouble("y");
                    double z = entry.getDouble("z");

                    tmp.setLatitudeDegs(Math.toDegrees(lat));
                    tmp.setLongitudeDegs(Math.toDegrees(lon));
                    tmp.setDepth(depth);

                    double[] offs = tmp.getOffsetFrom(mission.getStartLocation());
                    // if (!(xVals.contains(offs[0]+x) && yVals.contains(offs[1]+y))) {
                    // xVals.add(offs[0]+x);
                    // yVals.add(offs[1]+y);
                    // }
                    pe.addPoint(offs[1] + y, offs[0] + x, offs[2] + z, false);
                }
            }
        }
        pe.setMyColor(Color.green);
        pe.setFilled(false);
        pe.setFinished(true);

        MapMission mm = new MapMission();
        mm.setId(mt.getId());
        mm.setMap(mt);
        mission.addMap(mm);
        MapGroup.getMapGroupInstance(mission).addMap(mt);

        return pe;
    }

    public static PathElement generatePath(MissionType mission, Vector<LocationType> locations) {
        MapType mt = new MapType();
        LocationType first = locations.firstElement();
        locations.remove(0);
        // LocationType lt = new LocationType(mission.getHomeRef());
        PathElement pe = new PathElement(MapGroup.getMapGroupInstance(mission), mt, first);
        pe.setParentMap(mt);
        mt.addObject(pe);

        for (LocationType l : locations) {
            double[] offsets = l.getOffsetFrom(first);
            pe.addPoint(offsets[0], offsets[1], offsets[2], false);

        }
        pe.setMyColor(Color.green);
        pe.setFinished(true);

        MapMission mm = new MapMission();
        mm.setId(mt.getId());
        mm.setMap(mt);
        mission.addMap(mm);
        MapGroup.getMapGroupInstance(mission).addMap(mt);

        return pe;
    }

    public static VehicleType getVehicle(IMraLogGroup source) {

        // logs that won't be logged by external systems
        String privateLogs[] = new String[] { "Voltage", "CpuUsage", "Temperature" };

        for (String privateLog : privateLogs) {
            IMraLog log = source.getLog(privateLog);
            if (log != null) {
                IMCMessage msg = log.nextLogEntry();
                if (msg == null)
                    continue;
                int src_id = msg.getHeader().getInteger("src");

                VehicleType vt = VehiclesHolder.getVehicleWithImc(new ImcId16(src_id));

                if (vt != null)
                    return vt;
            }
        }

        return null;
    }

    public static void saveCharAsPdf(JFreeChart chart, File outFile) {
        Rectangle pageSize = new Rectangle(1024, 768);
        try {
            FileOutputStream out = new FileOutputStream(outFile);

            Document doc = new Document(pageSize);

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPdfVersion('1');

            doc.open();

            PdfContentByte cb = writer.getDirectContent();

            int width = (int) pageSize.getWidth();
            int height = (int) pageSize.getHeight();

            PdfTemplate tp = cb.createTemplate(width, height);

            java.awt.Graphics2D g2 = tp.createGraphicsShapes(width, height);
            chart.draw(g2, new Rectangle2D.Double(0, 0, width, height));
            // chart.paint(g2);

            g2.dispose();
            cb.addTemplate(tp, 0, 0);

            doc.close();
            out.flush();
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void savePlanAsPdf(StateRenderer2D renderer, File outFile) {
        Rectangle pageSize = new Rectangle(800, 600);
        try {
            FileOutputStream out = new FileOutputStream(outFile);

            Document doc = new Document(pageSize);

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPdfVersion('1');

            doc.open();

            PdfContentByte cb = writer.getDirectContent();

            int width = (int) pageSize.getWidth();
            int height = (int) pageSize.getHeight();

            PdfTemplate tp = cb.createTemplate(width, height);

            java.awt.Graphics2D g2 = tp.createGraphicsShapes(width, height);
            renderer.setSize(width, height);
            renderer.update(g2);
            // chart.draw(g2, new Rectangle2D.Double(0,0,width,height));
            // chart.paint(g2);

            g2.dispose();
            cb.addTemplate(tp, 0, 0);

            doc.close();
            out.flush();
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static LinkedHashMap<Integer, String> getEntities(IMraLogGroup source) {
        LinkedHashMap<Integer, String> entities = new LinkedHashMap<Integer, String>();

        entities.put(255, "Unknown");

        IMraLog parser = source.getLog("EntityInfo");

        if (parser != null) {
            IMCMessage entry = parser.nextLogEntry();

            while (entry != null) {
                try {
                    entities.put(entry.getInteger("id"), entry.getString("label"));
                }
                catch (Exception e) {
                }
                entry = parser.nextLogEntry();
            }
        }

        return entities;
    }
    /**
     * Returns the entity map for a given IMC node source id
     * @param source Log source
     * @param srcId Source id number 
     * @return The entity map
     */
    public static LinkedHashMap<Integer, String> getEntities(IMraLogGroup source, int srcId) {
        LinkedHashMap<Integer, String> entities = new LinkedHashMap<Integer, String>();

        entities.put(255, "Unknown");

        IMraLog parser = source.getLog("EntityInfo");

        if (parser != null) {
            IMCMessage entry = parser.nextLogEntry();

            while (entry != null) {
                if(entry.getHeader().getInteger("src")==srcId) {
                    try {
                        entities.put(entry.getInteger("id"), entry.getString("label"));
                    }
                    catch (Exception e) {
                    }
                }
                entry = parser.nextLogEntry();
            }
        }
        return entities;
    }
    
    
    public static ArrayList<LogMarker> getMarkersFromSource(IMraLogGroup source) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(source.getFile("Data.lsf").getParent()+"mra/marks.dat"));
            @SuppressWarnings("unchecked")
            ArrayList<LogMarker> markers = (ArrayList<LogMarker>)ois.readObject();
            ois.close();
            return markers;
        } catch(Exception e) {
            return new ArrayList<>();
        }
    }
    
    public static Vector<Double> lineSegments(IMraLogGroup source) {
        
        Vector<Double> result = new Vector<Double>();
        
        LsfIndex index = source.getLsfIndex();
        PathControlState lastState = index.getFirst(PathControlState.class);
        
        for (PathControlState pcs : index.getIterator(PathControlState.class)) {
            if (pcs.getEndLat() != lastState.getEndLat() || pcs.getEndLon() != pcs.getEndLon()) {
                result.add(pcs.getTimestamp());
                lastState = pcs;
            }
        }
        return result;
    }
    
   
    public static boolean hasIMCSidescan(IMraLogGroup source) {
        LsfIndex index = source.getLsfIndex();
        LsfIterator<SonarData> it = index.getIterator(SonarData.class);
        SonarData sd = it.next();
        
        if(sd == null)
            return false;
        
        while(sd != null) {
            if(sd.getType() == SonarData.TYPE.SIDESCAN)
                return true;
            sd = it.next();
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        IMraLogGroup source = new LsfLogSource(new File("/home/zp/workspace/logs/160736_mvplanner_lauv-noptilus-1/Data.lsf"), null);
        System.out.println(LogUtils.lineSegments(source));
    }
}
