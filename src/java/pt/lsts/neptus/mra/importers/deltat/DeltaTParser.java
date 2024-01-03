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
 * Author: jqcorreia
 * Apr 2, 2013
 */
package pt.lsts.neptus.mra.importers.deltat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Date;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.api.BathymetryInfo;
import pt.lsts.neptus.mra.api.BathymetryParser;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * @author jqcorreia
 * @author hfq
 * @author pdias
 */
public class DeltaTParser implements BathymetryParser {
    public boolean debugOn = false;
    
    private final IMraLogGroup source;
    private CorrectedPosition position = null;
    
    private boolean isLoaded = false;
    
    private File file = null;
    private FileInputStream fis;
    private final FileChannel channel;
    private ByteBuffer buf;
    private long curPos = 0;
    private DeltaTHeader header;

    public BathymetryInfo info;

    private int realNumberOfBeams = 0;
    private int totalNumberPoints = 0;

    private boolean hasIntensity = false;

    private boolean generateProcessReport = false;
    private String processResultOutputFileName;
    private Writer processResultOutputWriter;
    
    private long firstTimestamp = 0;
    private long lastTimestamp = 0;
    
    public DeltaTParser(IMraLogGroup source) {
        this.source = source;
        file = findDataSource(source);

        try {
            fis = new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        channel = fis.getChannel();
        //stateParser = source.getLog("EstimatedState");
        
        initialize();
    }

    private synchronized void initResultOutputFile() {
        if (processResultOutputWriter != null || new File(processResultOutputFileName).exists()) {
            generateProcessReport = false;
            return;
        }
        try {
            try {
                processResultOutputWriter = new OutputStreamWriter(new FileOutputStream(processResultOutputFileName), "UTF-8");
            }
            catch (UnsupportedEncodingException UEe) {
                System.err.println("\n-- UnsupportedEncodingException\n");
                System.err.flush();
                processResultOutputWriter = new OutputStreamWriter(new FileOutputStream(processResultOutputFileName), "iso8859-1");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            generateProcessReport = false;
        }
    }

    private void cleanupResultOutputFile() {
        if (processResultOutputWriter != null) {
            try {
                processResultOutputWriter.flush();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            try {
                processResultOutputWriter.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void recordMsgln() {
        recordMsg("\r\n");
    }

    private void recordMsgln(String string) {
        recordMsg(string + "\r\n");
    }

    private void recordMsg(String string) {
        if (generateProcessReport && processResultOutputWriter != null) {
            try {
                processResultOutputWriter.write(string);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (generateProcessReport && processResultOutputWriter == null) {
            generateProcessReport = false;
        }
    }

    /**
     * Used to gather bathymetry info and generate BathymetryInfo object
     */
    private void initialize() {
        File f = new File(source.getFile("Data.lsf").getParent() + "/mra/bathy.info");
        File folder = new File(source.getFile("Data.lsf").getParent() + "/mra/");

        if (!folder.exists())
            folder.mkdirs();

        processResultOutputFileName = folder.getAbsolutePath() + "/deltaT-process.txt";
        if (MRAProperties.generateDeltaTProcessReport)
            generateProcessReport = true;
        if (generateProcessReport)
            initResultOutputFile();
        
        if (!f.exists()) {
            info = new BathymetryInfo();

            double maxLat = -90;
            double minLat = 90;
            double maxLon = -180;
            double minLon = 180;

            BathymetrySwath bs;

            boolean firstTimestampSet = false;
            while ((bs = nextSwath()) != null) {
                LocationType loc = bs.getPose().getPosition().convertToAbsoluteLatLonDepth();
                double lat = loc.getLatitudeDegs();
                double lon = loc.getLongitudeDegs();

                maxLat = Math.max(lat, maxLat);
                maxLon = Math.max(lon, maxLon);
                minLat = Math.min(lat, minLat);
                minLon = Math.min(lon, minLon);
                
                if (!firstTimestampSet) {
                    firstTimestamp = bs.getTimestamp();
                    firstTimestampSet = true;
                }
                lastTimestamp = bs.getTimestamp();

                for (int c = 0; c < bs.getNumBeams(); c++) {
                    BathymetryPoint p = bs.getData()[c];

                    if(p == null)
                        continue;

                    info.minDepth = Math.min(info.minDepth, p.depth);
                    info.maxDepth = Math.max(info.maxDepth, p.depth);
                }

                totalNumberPoints = totalNumberPoints + bs.getNumBeams();
                realNumberOfBeams = 0;
            }

            info.topLeft = new LocationType(maxLat, minLon).translatePosition(info.maxDepth, -info.maxDepth, 0)
                    .convertToAbsoluteLatLonDepth();
            info.bottomRight = new LocationType(minLat, maxLon).translatePosition(-info.maxDepth, info.maxDepth, 0)
                    .convertToAbsoluteLatLonDepth();
            info.totalNumberOfPoints = totalNumberPoints;

            try {
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));
                out.writeObject(info);
                out.close();
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            curPos = 0;
        }
        else {
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
                info = (BathymetryInfo) in.readObject();
                in.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        // NeptusLog.pub().info("<###> "+info.maxDepth);
        isLoaded = true;

        if (processResultOutputWriter != null) {
            generateProcessReport = false;
            cleanupResultOutputFile();
            
            StringBuilder dataToSave = new StringBuilder();
            dataToSave.append("% Log                   : " + folder.getParentFile().getName() + "\r\n");
            dataToSave.append("% Box top left          : " + info.topLeft + "\r\n");
            dataToSave.append("% Box bottom right      : " + info.bottomRight + "\r\n");
            dataToSave.append("% Total number of points: " + info.totalNumberOfPoints + "\r\n");
            dataToSave.append("% Depths                : [" + info.minDepth + ", " + info.maxDepth + "]" + "\r\n");
            if (MRAProperties.timestampMultibeamIncrement != 0)
                dataToSave.append("% Added milliseconds    :" + MRAProperties.timestampMultibeamIncrement + "\r\n");
            if (MRAProperties.soundSpeedCorrection)
                dataToSave.append("% Sound speed correction applied to data" + "\r\n");

            dataToSave.append("% -------------------------------------------------------------------------------" + "\r\n");
            try {
                File mainFx = new File(processResultOutputFileName);
                File bkpFx = new File(processResultOutputFileName + ".bak");
                if (bkpFx.exists())
                    bkpFx.delete();
                FileUtils.moveFile(mainFx, bkpFx);
                FileUtil.saveToFile(processResultOutputFileName, dataToSave.toString());
                FileUtil.concatFiles(mainFx, bkpFx);
                bkpFx.delete();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            boolean firstTimestampSet = false;
            try {
                while(curPos < channel.size()) {
                    buf = channel.map(MapMode.READ_ONLY, curPos, 256);
                    header = new DeltaTHeader();
                    header.parse(buf);
                    if (!firstTimestampSet) {
                        firstTimestamp = header.timestamp;
                        firstTimestampSet = true;
                    }
                    lastTimestamp = header.timestamp;
                    curPos += header.numBytes;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            curPos = 0;
        }
    }

    public static boolean canBeApplied(IMraLogGroup source) {
        File file = findDataSource(source);
        if (file != null && file.exists())
            return true;
        return false;
    }
    
    /**
     * @param source
     */
    public static File findDataSource(IMraLogGroup source) {
        if (source.getFile("data.83P") != null)
            return source.getFile("data.83P");
        else if (source.getFile("Data.83P") != null)
            return source.getFile("Data.83P");
        else if (source.getFile("multibeam.83P") != null)
            return source.getFile("multibeam.83P");
        else
            return null;
    }

    public boolean isLoaded() {
        return isLoaded;
    }
    
    /**
     * @return the position
     */
    public CorrectedPosition getCorrectedPosition() {
        if (position == null)
            position = new CorrectedPosition(source);

        return position;
    }
    
    @Override
    public long getFirstTimestamp() {
        return firstTimestamp;
    }

    @Override
    public long getLastTimestamp() {
        return lastTimestamp;
    }

    @Override
    public BathymetryInfo getBathymetryInfo() {
        return info;
    }

    @Override
    public BathymetrySwath getSwathAt(long timestamp) {
        if (this.header.timestamp == timestamp) {
            curPos -= header.numBytes;
            return nextSwath(1);
        }
        
        long oldCurPos = curPos;
        long posSearch = oldCurPos;
        if (timestamp < this.header.timestamp)
            posSearch = 0;
        
        try {
            DeltaTHeader headerTest = null;
            while(posSearch < channel.size()) {
                buf = channel.map(MapMode.READ_ONLY, posSearch, 256);
                headerTest = new DeltaTHeader();
                headerTest.parse(buf);
                if (headerTest.timestamp >= timestamp) {
                    curPos = posSearch;
                    return nextSwath(1);
                }
                posSearch += headerTest.numBytes;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public BathymetrySwath nextSwath() {
        return nextSwath(1);
    }

    @Override
    public BathymetrySwath nextSwath(double prob) {

        if (position == null)
            position = new CorrectedPosition(source);
        
        try {
            if (curPos >= channel.size()) {
//                cleanupResultOutputFile();
                return null;
            }

            BathymetryPoint data[];
            realNumberOfBeams = 0;

            buf = channel.map(MapMode.READ_ONLY, curPos, 256);
            header = new DeltaTHeader();
            header.parse(buf);

            hasIntensity = header.hasIntensity;
//            if (hasIntensity)
//                NeptusLog.pub().info("LOG has intensity");
//            else
//                NeptusLog.pub().info("Log doesn't have intensity");

            // Parse and process data ( no need to create another structure for this )
            if (header.hasIntensity)
                buf = channel.map(MapMode.READ_ONLY, curPos + 256, header.numBeams * 4);
            else
                buf = channel.map(MapMode.READ_ONLY, curPos + 256, header.numBeams * 2);

            data = new BathymetryPoint[header.numBeams];

            long timestamp = header.timestamp + MRAProperties.timestampMultibeamIncrement;

            boolean poseFromCorrected = true;
            SystemPositionAndAttitude pose = position.getPosition(timestamp/1000.0);
            if (pose == null) {
                
                poseFromCorrected = false;
                pose = new SystemPositionAndAttitude();
                LocationType loc = new LocationType();
                loc.setLatitudeDegs(CoordinateUtil.latFrom83PFormatWorker(header.gnssShipPosLat));
                loc.setLongitudeDegs(CoordinateUtil.lonFrom83PFormatWorker(header.gnssShipPosLon));
                loc.setAbsoluteDepth(-1);
                pose.setPosition(loc);
                
                pose.setTime(timestamp);
                pose.setAltitude(header.altitude);

                pose.setRoll(Math.toRadians(header.rollAngleDegreesOrientModule));
                pose.setPitch(Math.toRadians(header.pitchAngleDegreesOrientModule));
                pose.setYaw(Math.toRadians(header.headingAngleDegreesOrientModule));
                
                NeptusLog.pub().debug("No position found on navigation, using partial data from Sonar");
            }

            boolean doSpeedCorrection = MRAProperties.soundSpeedCorrection;

            if (generateProcessReport) {
                recordMsgln();
                recordMsgln("% Swath type & version : " + header.fileType + ", " + header.fileVersion);
                recordMsgln("% Swath time           : " + DateTimeUtil.dateTimeFileNameFormatterMillis.format(new Date(timestamp)));
                recordMsgln("% Swath position       : " + pose.getPosition().toString().replaceAll("\n", " ") + 
                        "m depth  :: " + MathMiscUtils.round(pose.getAltitude(), 2) + "m altitude" + 
                        (poseFromCorrected ? " from corrected position" : " from data"));
                recordMsgln("% Swath attitude       : R" + MathMiscUtils.round(Math.toDegrees(pose.getRoll()), 1) +
                        "\u00B0 P" + MathMiscUtils.round(Math.toDegrees(pose.getPitch()), 1) +
                        "\u00B0 Y" + MathMiscUtils.round(Math.toDegrees(pose.getYaw()), 1) + "\u00B0");
                recordMsgln("% Orient. module       : R" + MathMiscUtils.round(Math.toDegrees(header.rollAngleDegreesOrientModule), 1) +
                        "\u00B0 P" + MathMiscUtils.round(Math.toDegrees(header.pitchAngleDegreesOrientModule), 1) +
                        "\u00B0 H" + MathMiscUtils.round(Math.toDegrees(header.headingAngleDegreesOrientModule), 1) + "\u00B0");
                
                recordMsgln("% Ship Course          : " + header.gnssShipCourse + "\u00B0");
                recordMsgln("% Ship Lat/Lon         : " + header.gnssShipPosLat + "  " + header.gnssShipPosLon);
                recordMsgln("% Sonar XYZ offsets    : " + header.sonarXOffset + "m, " + header.sonarYOffset + "m, " + header.sonarZOffset + "m");
                
                recordMsgln("% Angle start/increment: " + header.startAngle + "\u00B0" + ", " + header.angleIncrement + "\u00B0");
                recordMsgln("% Beams                : " + header.numBeams);
                recordMsgln("% Samples per beam     : " + header.samplesPerBeam);
                recordMsgln("% Number of pings avg  : " + header.numberOfPingsAveraged);
                recordMsgln("% Sample rate high/std : " + (header.sampleRateHigh ? "high" : "std") + " [std(1 in 500)/high (1 in 5000)]");
                recordMsgln("% Range                : " + header.range + "m");
                recordMsgln("% Range resolution     : " + header.rangeResolution + "mm");
                recordMsgln("% Sonar Freq.          : " + header.sonarFreqKHz + "kHz");
                recordMsgln("% Pulse length         : " + header.pulseLength + "\u03BCs");
                recordMsg(  "% 1/PRF                : " + header.pulseRepetingRate + "ms");
                recordMsgln(" (" + MathMiscUtils.parseToEngineeringNotation(1. / (header.pulseRepetingRate / 1E3), 1) + "Hz)");
                recordMsgln("% Ping number          : " + header.pingNumber);
                recordMsgln("% Sector size          : " + header.sectorSize + "\u00B0 :: " +
                        (header.angleIncrement * header.numBeams) + "\u00B0 calculated");
                recordMsgln("% Speed                : " + MathMiscUtils.round(header.speed, 1) + "m/s");
                recordMsgln("% Sound speed          : " + header.soundVelocity + "m/s" + (doSpeedCorrection ? "" : " (1500m/s used for calculation)"));
                recordMsgln("% Roll correction      : " + (header.dataIsCorrectedForRoll ? "yes" : "no"));
                recordMsgln("% RayBending correction: " + (header.dataIsCorrectedForRayBending ? "yes" : "no"));
                recordMsgln("% Op overlap mode      : " + (header.sonarIsOperatingInOverlappedMode ? "yes" : "no"));
                recordMsgln("% Altitude             : " + header.altitude + "m");
                recordMsgln("% ---------------------");
            }
            
            StringBuilder rangesStr = new StringBuilder();
            StringBuilder heightStr = new StringBuilder();
            StringBuilder intensityStr = new StringBuilder();
            StringBuilder oxStr = new StringBuilder();
            StringBuilder oyStr = new StringBuilder();
            StringBuilder deltasStr = new StringBuilder();
            float prevX = Float.NaN;
            float prevY = Float.NaN;
            
            for (int c = 0; c < header.numBeams; c++) {
                double range = buf.getShort(c * 2) * (header.rangeResolution / 1000.0); // rangeResolution in mm 

                if (range == 0.0 || Math.random() > prob) {
                    if (generateProcessReport || debugOn) {
                        if (range != 0) {
                            recordMsgln("% Skip swath beam " + c + " range=" + range);
                        }
                        else { 
                            rangesStr.append(" " + MathMiscUtils.round(range, 3));
                            heightStr.append(" " + Double.NaN);
                            intensityStr.append(" " + Double.NaN);
                            oxStr.append(" " + Double.NaN);
                            oyStr.append(" " + Double.NaN);
                            deltasStr.append(" " + Float.NaN);
                            prevX = Float.NaN;
                            prevY = Float.NaN;
                        }
                    }
                    continue;
                }

                if (doSpeedCorrection && header.soundVelocity != 1500f) {
                    range = range * header.soundVelocity / 1500f;
                }

                if (generateProcessReport || debugOn) {
                    rangesStr.append(" " + MathMiscUtils.round(range, 3));
                }
                
                double angle = header.startAngle + header.angleIncrement * c;
                float height = (float) (range * Math.cos(Math.toRadians(MRAProperties.rollBias+angle)) + pose.getPosition().getDepth());
                
                double x = range * Math.sin(Math.toRadians(angle));
                double yawAngle = -pose.getYaw();

                float ox = (float) (x * Math.sin(yawAngle));
                float oy = (float) (x * Math.cos(yawAngle));

                if (header.hasIntensity) {
                    short intensity = buf.getShort(480 + (c * 2) - 1); // sometimes there's a return = 0
                    int intensityInt = 0xffff & intensity;
                    data[c] = new BathymetryPoint(ox, oy, height, intensityInt);
                    data[c].intensityMaxValue = 65535;
                    if (generateProcessReport || debugOn)
                        intensityStr.append(" " + intensityInt);
                }
                else {
                    data[c] = new BathymetryPoint(ox, oy, height);
                    data[c].intensityMaxValue = 65535;
                    if (generateProcessReport || debugOn)
                        intensityStr.append(" " + Double.NaN);
                }
                realNumberOfBeams++;
                
                if (generateProcessReport || debugOn) {
                    heightStr.append(" " + MathMiscUtils.round(height, 3));
                    oxStr.append(" " + MathMiscUtils.round(ox, 3));
                    oyStr.append(" " + MathMiscUtils.round(oy, 3));
                    if (!Float.isNaN(prevX) && !Float.isNaN(prevY)) {
                        float delta = (float) Math.sqrt((ox - prevX) * (ox - prevX) + (oy - prevY) * (oy - prevY));
                        deltasStr.append(" " + MathMiscUtils.round(delta, 3));
                    }
                    else {
                        deltasStr.append(" " + Float.NaN);
                    }
                    prevX = ox;
                    prevY = oy;
                }
            }

            if (generateProcessReport) {
                recordMsgln("% Ranges:");
                recordMsgln(rangesStr.toString());
                recordMsgln("% Heights:");
                recordMsgln(heightStr.toString());
                recordMsgln("% Intensities:");
                recordMsgln(intensityStr.toString());
                recordMsgln("% Offsets X:");
                recordMsgln(oxStr.toString());
                recordMsgln("% Offsets Y:");
                recordMsgln(oyStr.toString());
                recordMsgln("% Deltas:");
                recordMsgln(deltasStr.toString());
                recordMsgln("% Number of beams vs read: " + header.numBeams + " vs " + realNumberOfBeams);
            }
            
            curPos += header.numBytes; // Advance current position

            BathymetrySwath swath = new BathymetrySwath(header.timestamp, pose, data);
            realNumberOfBeams = header.numBeams;
            swath.setNumBeams(realNumberOfBeams);

            if (debugOn) {
                StringBuilder sb = new StringBuilder();
                sb.append("----------------------------------------------------------------\n");
                sb.append("% Swath position       : " + pose.getPosition().toString().replaceAll("\n", " ") + 
                        "m depth  :: " + MathMiscUtils.round(pose.getAltitude(), 2) + "m altitude" + 
                        (poseFromCorrected ? " from corrected position" : " from data"));
                sb.append("\n");
                sb.append("% Swath attitude       : R" + MathMiscUtils.round(Math.toDegrees(pose.getRoll()), 1) +
                        "\u00B0 P" + MathMiscUtils.round(Math.toDegrees(pose.getPitch()), 1) +
                        "\u00B0 Y" + MathMiscUtils.round(Math.toDegrees(pose.getYaw()), 1) + "\u00B0");
                sb.append("\n");
                
                sb.append("% Angle start/increment: " + header.startAngle + "\u00B0" + ", " + header.angleIncrement + "\u00B0");
                sb.append("\n");
                sb.append("% Beams                : " + header.numBeams);
                sb.append("\n");
                sb.append("% Range                : " + header.range + "m");
                sb.append("\n");

                sb.append("% Ping number          : " + header.pingNumber);
                sb.append("\n");
                sb.append("% Sector size          : " + header.sectorSize + "\u00B0 :: " +
                        (header.angleIncrement * header.numBeams) + "\u00B0 calculated");
                sb.append("\n");
                sb.append("% Speed                : " + MathMiscUtils.round(header.speed, 1) + "m/s");
                sb.append("\n");
                sb.append("% Altitude             : " + header.altitude + "m");
                sb.append("\n");
                sb.append(heightStr.toString());
                sb.append("\n");
                
                System.out.println(sb.toString());
            }
            
            return swath;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public BathymetrySwath nextSwathRealData() {
        return null;
    }

    public BathymetrySwath nextSwathNoData() {
        if (position == null)
            position = new CorrectedPosition(source);
        
        try {
            if (curPos >= channel.size())
                return null;

            realNumberOfBeams = 0;

            buf = channel.map(MapMode.READ_ONLY, curPos, 256);
            DeltaTHeader header = new DeltaTHeader();
            header.parse(buf);

            long timestamp = header.timestamp + MRAProperties.timestampMultibeamIncrement;

            SystemPositionAndAttitude pose = position.getPosition(timestamp/1000.0);
            curPos += header.numBytes; // Advance current position

            BathymetrySwath swath = new BathymetrySwath(header.timestamp, pose, null);
            swath.setNumBeams(realNumberOfBeams);

            return swath;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets the current position
     * @return 
     */
    public long getCurrentPosition() {
        return curPos;
    }
    
    /**
     * @return the header
     */
    public DeltaTHeader getCurrentHeader() {
        return header;
    }
    
    @Override
    public void rewind() {
        curPos = 0;
        //stateParser.firstLogEntry();
    }

    @Override
    public boolean getHasIntensity() {
        return hasIntensity;
    }

    public static void main(String[] args) {
        try {
            LsfLogSource source = new LsfLogSource(new File(
                    // "/home/lsts/Desktop/to_upload_20130715/lauv-noptilus-1/20130715/122455_out_survey/Data.lsf"
                    "D:\\LSTS-Logs\\2014-11-09-Madeira\\2014-11-12-Madeira_115528_rows_maneuver_cais2_day3\\Data.lsf.gz"
                    ), null);
            File fxB = source.getFile("mra/bathy.info");
            if (fxB != null && fxB.exists())
                fxB.delete();
            fxB = source.getFile("mra/deltaT-process.txt");
            if (fxB != null && fxB.exists())
                fxB.delete();
            MRAProperties.generateDeltaTProcessReport = true;
            MRAProperties.soundSpeedCorrection = true;
            DeltaTParser p = new DeltaTParser(source);

            int c = 0;
            BathymetrySwath s;
            while ((s = p.nextSwath()) != null) {
                // // for(BathymetryPoint bp : bs.getData()) {
                // // double r[] = CoordinateUtil.latLonAddNE2(bp.lat, bp.lon, bp.north, bp.east);
                // // float f[] = new float[2];
                // //
                // // f[0] = (float) (r[0] * 1000000f);
                // // f[1] = Double.valueOf(r[1]).floatValue();
                // //
                // // NeptusLog.pub().info("<###> "+r[0]);
                // // NeptusLog.pub().info("<###> " + f[0]);
                // // }
                // c++;
                // // kryo.writeObject(output, bs);

                System.out.println(Math.toDegrees(s.getPose().getYaw()));
                c++;
            }
            System.out.println(c);
            
            long fTs = p.getFirstTimestamp();
            long lTs = p.getLastTimestamp();
            long sTs = lTs - fTs;
            Random rand = new Random();
            for (int i = 0; i < 4; i++) {
                double perc = rand.nextDouble();
                long searchTs = (long) (sTs * perc + fTs);
                long markStart = System.nanoTime();
                BathymetrySwath swath = p.getSwathAt(searchTs);
                long markEnd = System.nanoTime();
                System.out.printf("Search swath at '%s' and got one for time '%s' and took %.3fms\n", 
                        DateTimeUtil.dateTimeFormatterUTC.format(new Date(searchTs)),
                        (swath != null ? DateTimeUtil.dateTimeFormatterUTC.format(new Date(swath.getTimestamp())) : "NONE"),
                        (markEnd - markStart) / 1E6);
            }
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
