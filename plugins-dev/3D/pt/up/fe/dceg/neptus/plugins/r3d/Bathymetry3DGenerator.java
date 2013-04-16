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
 * Author: Margarida Faria
 * May 31, 2012
 */
package pt.up.fe.dceg.neptus.plugins.r3d;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.colormap.ColorMapUtils;
import pt.up.fe.dceg.neptus.colormap.DataDiscretizer;
import pt.up.fe.dceg.neptus.colormap.DataDiscretizer.DataPoint;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.NeptusMRA;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.plugins.mraplots.XYZUtils;
import pt.up.fe.dceg.neptus.plugins.r3d.dto.BathymetryLogInfo;
import pt.up.fe.dceg.neptus.plugins.r3d.dto.VehicleInfoAtPointDTO;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.llf.LogUtils;
import pt.up.fe.dceg.plugins.tidePrediction.Harbors;
import pt.up.fe.dceg.plugins.tidePrediction.TidePredictionFinder;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

/**
 * @author meg
 * 
 *         Here the log data is converted to a bathymetry image
 */
public class Bathymetry3DGenerator {
    private enum BDEntities {
        ECHO_SOUNDER("Echo Sounder"), DVL("DVL"), DEPTH_HEADING_CONTROL("Depth & Heading Control"), ALL("ALL");

        public final String name;

        private BDEntities(String name) {
            this.name = name;
        }
    }

    private static final int DISTANCE_WINDOW = 2;
    private static final int MESSAGE_WINDOW = 50;
    private static final int INVALID_ENTITY_ID = 255;
    private final int MIN_NUM_SATALLITES = 8; // one minute

    private final String SRC_ENT = "src_ent";

    public int targetImageSize;
    public final int GRID_SIZE = 10;
    BathymetryLogInfo data;
    // from Log
    private final IMraLogGroup source;
    private DataDiscretizer xyzNEDmeters;
    private Vector<Double> xVec, yVec, waterColumnVec, vehicleDepthVec;
    private LocationType imageCornerRef;
    private int idSourceBottomDistance;
    private final Harbors harbor;

    /**
     * Sets the source of the log and the timestamp. Initializes the object where to store the collected data.
     * 
     * @param source the log
     * @param harbor
     * @param timestamp
     * @throws IOException when the source entity of the bottom distance information is invalid
     */
    public Bathymetry3DGenerator(IMraLogGroup source, Harbors harbor) {
        super();
        this.source = source;
        data = new BathymetryLogInfo();
        this.harbor = harbor;
    }

    /**
     * Swipes all the log to see if there is a DEPTH_HEADING_CONTROL or a DVL present.
     * 
     * @return the id of the found entity or INVALID_ENTITY_ID if none was found
     */
    private Integer getIdOfBottomDistanceQuick() {
        // Get the entities in the source
        final LinkedHashMap<Integer, String> entities = LogUtils.getEntities(source);
        final IMraLog bottomDistLogs = source.getLog("BottomDistance");
        // Go through all the log or until you find a message from DEPTH_HEADING_CONTROL
        Integer selectedEntity = INVALID_ENTITY_ID;
        int entityId;
        String entityName;
        IMCMessage logEntry = bottomDistLogs.nextLogEntry();
        while (logEntry != null) {
            entityId = logEntry.getInteger("src_ent");
            entityName = entities.get(entityId);
            if (entityName.equals(BDEntities.DEPTH_HEADING_CONTROL.name)) {
                return entityId;
            }
            else if (entityName.equals(BDEntities.DVL.name)) {
                selectedEntity = entityId;
            }
            logEntry = bottomDistLogs.nextLogEntry();
        }
        return selectedEntity;
    }

    /**
     * IMC4 Extracts the information on log about the vehicle path and bottom distance. Sets it in the internal object
     * that holds information from the log but also returns it.
     * <p>
     * This gives the raw information of latitude(degrees), longitude(degrees), depth(m) and bottom distance(m).
     * 
     * @return an object that holds the collected information.
     */
    public BathymetryLogInfo extractBathymetryInfoRawIMC4() throws NoBottomDistanceEntitiesException {
        // rules out unreliable sensors
        idSourceBottomDistance = getIdOfBottomDistanceQuick();
        if (idSourceBottomDistance == INVALID_ENTITY_ID) {
            // TODO refactor so that when there is no bottom distance only the vehicle path is rendered
            throw new NoBottomDistanceEntitiesException(I18n.text("No valid entity of bottom distance."));
        }
        ArrayList<VehicleInfoAtPointDTO> vehicleInfo = new ArrayList<VehicleInfoAtPointDTO>();
        // get raw info from log and init associated vars
        final IMraLog bParser = source.getLog("BottomDistance");
        final IMraLog stateParser = source.getLog("EstimatedState");

        VehicleInfoAtPointDTO currPointInfo;
        IMCMessage currBottDistMsg = nextBottDistMsg(bParser);
        EstimatedState currEstStateMsg = (EstimatedState) stateParser.nextLogEntry();
        IMCMessage lastEstStateMsgsSync;
        LocationType lastEstStateMsgsSyncLocation, estStateMsgLocation;
        float distance = Float.MAX_VALUE;
        long currEstStateTime, currBottDistTime;
        lastEstStateMsgsSync = null;
        // Will advance through the log advancing the estimated state parser, the bottom distance parser will only be
        // advanced when the entry is before the estimated state one
        while (currEstStateMsg != null) {
            currPointInfo = new VehicleInfoAtPointDTO();
            currEstStateTime = currEstStateMsg.getTimestampMillis();
            if (currBottDistMsg != null) {
                currBottDistTime = currBottDistMsg.getTimestampMillis();
                // check if should advance bottom distance
                if (currEstStateTime > currBottDistTime) {
                    currBottDistMsg = nextBottDistMsg(bParser);
                }
                // set last estimated state msg synced with bottom distance msg (for location)
                if (FastMath.abs(currEstStateTime - currBottDistTime) < MESSAGE_WINDOW) {
                    lastEstStateMsgsSync = currEstStateMsg;
                }
                // Take depth info if in range
                if (lastEstStateMsgsSync != null) {
                    // Calc location of estimated state
                    estStateMsgLocation = getLocationIMC4(currEstStateMsg); // Caso haja erro verificar a estrutura do
                    // EstimatedState
                    estStateMsgLocation.convertToAbsoluteLatLonDepth();
                    // calc distance
                    lastEstStateMsgsSyncLocation = new LocationType();
                    lastEstStateMsgsSyncLocation.setLatitude(Math.toDegrees(lastEstStateMsgsSync.getFloat("lat")));
                    lastEstStateMsgsSyncLocation.setLatitude(Math.toDegrees(lastEstStateMsgsSync.getFloat("lon")));
                    distance = (float) lastEstStateMsgsSyncLocation.getDistanceInMeters(estStateMsgLocation);
                }
                if (distance < DISTANCE_WINDOW) {
                    // bottom of sea = depth of vehicle + bottom distance
                    currPointInfo.setBottomDist((currBottDistMsg.getFloat("value")));
                }
            }
            // Take vehicle path info
            estStateMsgLocation = getLocationIMC4(currEstStateMsg); // Caso haja erro verificar a estrutura do
            // EstimatedState
            estStateMsgLocation.convertToAbsoluteLatLonDepth();
            currPointInfo.setLatLonDepth((float) estStateMsgLocation.getLatitudeAsDoubleValue(),
                    (float) estStateMsgLocation.getLongitudeAsDoubleValue(), (float) estStateMsgLocation.getDepth());
            // Take yaw pitch roll
            currPointInfo.setYawPitchRoll(new Vector3f(currEstStateMsg.getFloat("psi"), currEstStateMsg
                    .getFloat("theta"), currEstStateMsg.getFloat("phi")));
            // Save data
            vehicleInfo.add(currPointInfo);
            currEstStateMsg = (EstimatedState) stateParser.nextLogEntry();
        }

        data = new BathymetryLogInfo(vehicleInfo);
        return data;
    }

    /**
     * IMC5
     * 
     * Extract information from log to build terrain and vehicle path (as line)
     * 
     * @throws Exception
     */
    public BathymetryLogInfo extractBathymetryInfoIMC5(boolean tideAdjust) {
        if (data == null) {
            data = new BathymetryLogInfo();
        }
        // set water height
        EstimatedState firstEstStateMsg = source.getLsfIndex().getFirst(EstimatedState.class);
        data.setWaterHeight((float) firstEstStateMsg.getHeight());
        // set reference for corner of image
        imageCornerRef = getLocationIMC5(firstEstStateMsg);
        data.setReferenceLocation(imageCornerRef);

        LsfIndex lsfIndex = source.getLsfIndex();
        // find water height at start of log
        TidePredictionFinder tidePrediction = null;

        if (harbor != null && tideAdjust) {
            tidePrediction = new TidePredictionFinder();
        }

        // process whole log
        xyzNEDmeters = new DataDiscretizer(1);
        xVec = new Vector<Double>();
        yVec = new Vector<Double>();
        waterColumnVec = new Vector<Double>();
        vehicleDepthVec = new Vector<Double>();
        LocationType estStateMsgLocation;
        double waterColumn;
        double[] offs;
        double depth, terrainAltitude, alt, vehAltitude;
        float currPrediction;
        for (EstimatedState currEstStateMsg : lsfIndex.getIterator(EstimatedState.class)) {
            depth = currEstStateMsg.getDepth();
            if (depth < 0) {
                continue;
            }
            // Take vehicle path info
            estStateMsgLocation = getLocationIMC5(currEstStateMsg);
            offs = estStateMsgLocation.getOffsetFrom(imageCornerRef); // in NED
            // Save data
            xVec.add(offs[0]);
            yVec.add(offs[1]);
            alt = currEstStateMsg.getAlt();
            if (harbor != null && tideAdjust) {
                if (alt < 0 || depth < NeptusMRA.minDepthForBathymetry) {
                    vehicleDepthVec.add((double) 0);
                    continue;
                }
                try {
                    currPrediction = tidePrediction.getTidePrediction(currEstStateMsg.getDate(), harbor, false);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }

                waterColumn = depth + alt;
                terrainAltitude = waterColumn - currPrediction;
                vehAltitude = depth - currPrediction;
                vehicleDepthVec.add(vehAltitude);
                addBathymetryData(terrainAltitude, offs);
            }
            else {
                if (alt < 0 || depth < NeptusMRA.minDepthForBathymetry) {
                    vehicleDepthVec.add(depth);
                    continue;
                }
                waterColumn = depth + alt;
                addBathymetryData(waterColumn, offs);
            }
        }
        data.setNorthVec(xVec);
        data.setEastVec(yVec);
        data.setDepthVec(vehicleDepthVec);
        // prepare data for image
        generateBufferedImage(ColorMapFactory.createStoreDataColormap());
        // graphToFile(data.getBuffImageHeightMap(), "/home/meg/LSTS/heighMap/heightMapout.jpg");

        return data;
    }

    private void addBathymetryData(double waterColumn, double[] offs) {
        xyzNEDmeters.addPoint(offs[1], -offs[0], -waterColumn);
        waterColumnVec.add(-waterColumn);
        // Distance between
        if (waterColumn < data.getMinWaterColumn()) {
            data.setMinWaterColumn((float) waterColumn);
        }
        if (waterColumn > data.getMaxWaterColumn()) {
            data.setMaxWaterColumn((float) waterColumn);
        }
    }

    public static LocationType getLocationIMC5(EstimatedState firstEstStateMsg) {
        double lat = firstEstStateMsg.getLat();
        double lon = firstEstStateMsg.getLon();
        double depth = firstEstStateMsg.getDepth();
        lat = Math.toDegrees(lat);
        lon = Math.toDegrees(lon);

        LocationType loc = new LocationType();
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        loc.setDepth(depth);
        loc.setOffsetNorth(firstEstStateMsg.getX());
        loc.setOffsetEast(firstEstStateMsg.getY());
        loc.setOffsetDown(firstEstStateMsg.getZ());
        return loc;
    }

    /**
     * Returns the next valid message of the given entity.
     * 
     * @param parserBottDist the parser for Bottom distance
     * @param idSourceBottomDistance the id of the analyzed sensor
     * @return the next msg or null if there are none
     */
    private IMCMessage nextBottDistMsg(IMraLog parserBottDist) {
        // Conditions:
        // - idSourceBottomDistance != bEntry.getInteger(SRC_ENT)
        // - !"VALID".equalsIgnoreCase(bEntry.getString("validity"))
        IMCMessage nextMsg;
        do {
            nextMsg = parserBottDist.nextLogEntry();

        } while (nextMsg != null
                && ((idSourceBottomDistance != nextMsg.getSrcEnt() || !nextMsg.getString("validity")
                        .equalsIgnoreCase("VALID"))));
        return nextMsg;
    }

    private IMCMessage advanceLogParserIrrelevantEntries(IMraLog logParser, IMCMessage bEntry) {
        if (bEntry != null
                && (idSourceBottomDistance != INVALID_ENTITY_ID && idSourceBottomDistance != bEntry.getInteger(SRC_ENT))
                || (bEntry != null && !"VALID".equalsIgnoreCase(bEntry.getString("validity"))))
            while ((bEntry != null && bEntry.getDouble(SRC_ENT) != idSourceBottomDistance)
                    || (bEntry != null && !"VALID".equalsIgnoreCase(bEntry.getString("validity"))))
                bEntry = logParser.nextLogEntry();
        return bEntry;
    }

    private LocationType getLocationIMC4(EstimatedState estimatedStateMessage) {
        try {
            if (estimatedStateMessage != null) {
                LocationType loc = new LocationType();

                // 0 -> NED ONLY, 1 -> LLD ONLY, 2 -> NED_LLD
                long refMode = estimatedStateMessage.getLong("ref");
                if (refMode == 0) {
                    loc = new LocationType(data.getReferenceLocation());
                }
                if (refMode == 1 || refMode == 2) {
                    loc.setLatitude(Math.toDegrees(estimatedStateMessage.getDouble("lat")));
                    loc.setLongitude(Math.toDegrees(estimatedStateMessage.getDouble("lon")));
                    loc.setDepth(estimatedStateMessage.getDouble("depth"));
                }
                if (refMode == 0 || refMode == 2) {
                    loc.translatePosition(estimatedStateMessage.getDouble("x"), estimatedStateMessage.getDouble("y"),
                            estimatedStateMessage.getDouble("z"));
                }
                return loc;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * IMC4 Extracts mean water height, minimum depth as well as depth and bottom distance values from log. The
     * interpolated bathymetry is then calculated and stored as a BufferedImage
     * 
     * @return water height, minimum depth and the bufferedImage inside the BathymetryLogInfo object
     */
    public BathymetryLogInfo extractBathymetryInfoIMC4() throws NoBottomDistanceEntitiesException {
        // rules out unreliable sensors
        idSourceBottomDistance = getIdOfBottomDistanceQuick();
        if (idSourceBottomDistance == INVALID_ENTITY_ID) {
            // TODO refactor so that when there is no bottom distance only the vehicle path is rendered
            throw new NoBottomDistanceEntitiesException(I18n.text("No valid entity of bottom distance."));
        }
        if (data == null) {
            data = new BathymetryLogInfo();
        }
        // extract data from log
        calculateWaterHeightFromGPSFix();
        calcBathymetryData();
        generateBufferedImage(ColorMapFactory.createStoreDataColormap());
        // graphToFile(data.getBuffImageHeightMap(), "/home/meg/LSTS/heighMap/heightMapout.jpg");
        return data;
    }

    /**
     * IMC 5
     * 
     * @return
     */
    public BathymetryLogInfo extractBathymetryInfoRawIMC5() {
        LsfIndex lsfIndex = source.getLsfIndex();
        ArrayList<VehicleInfoAtPointDTO> vehicleInfo = new ArrayList<VehicleInfoAtPointDTO>();
        LocationType estStateMsgLocation;
        VehicleInfoAtPointDTO currPointInfo;
        // IMCMessage currEstStateMsg;

        for (EstimatedState currEstStateMsg : lsfIndex.getIterator(EstimatedState.class)) {
            currPointInfo = new VehicleInfoAtPointDTO();
            // Take vehicle path info
            estStateMsgLocation = getLocationIMC4(currEstStateMsg); // Caso haja erro verificar a estrutura do
            // EstimatedState
            estStateMsgLocation.convertToAbsoluteLatLonDepth();
            currPointInfo.setLatLonDepth((float) estStateMsgLocation.getLatitudeAsDoubleValue(),
                    (float) estStateMsgLocation.getLongitudeAsDoubleValue(), (float) estStateMsgLocation.getDepth());
            // Take yaw pitch roll
            currPointInfo.setYawPitchRoll(new Vector3f((float) currEstStateMsg.getPhi(), (float) currEstStateMsg
                    .getTheta(), (float) currEstStateMsg.getPhi()));
            // Save data
            vehicleInfo.add(currPointInfo);
        }
        data = new BathymetryLogInfo(vehicleInfo);
        return data;
    }

    private java.awt.geom.Rectangle2D.Double calculateBounds(DataDiscretizer dd, int targetImageWidth,
            int targetImageHeight) {
        // the points that are further apart in x and y
        final double maxX = dd.maxX + 5;
        final double maxY = dd.maxY + 5;
        final double minX = dd.minX - 5;
        final double minY = dd.minY - 5;

        // width/height (covered area)
        double dimensionX = maxX - minX;
        double dimensionY = maxY - minY;

        final double ratioWanted = (double) targetImageWidth / (double) targetImageHeight;
        final double ratioReal = dimensionX / dimensionY;

        if (ratioReal < ratioWanted)
            dimensionX = dimensionY * ratioWanted;
        else
            dimensionY = dimensionX / ratioWanted;

        // center of the covered area
        final double centerX = (maxX + minX) / 2;
        final double centerY = (maxY + minY) / 2;

        final double originalX = centerX - dimensionX / 2;
        final double originalY = centerY - dimensionY / 2;
        // in meters
        return new Rectangle2D.Double(originalX, originalY, dimensionX, dimensionY);
    }

    private LocationType calculateHomeRef(EstimatedState entry) {
        // try to generate homeref from logs
        LocationType homeRef = LogUtils.getHomeRef(source);
        // in the case homeRef is null, the first position is copied to homeref
        if (homeRef == null) {
            homeRef = getLocationIMC4(entry);
        }
        return homeRef;
    }

    /**
     * Gathers info from log about bottom distance (for the sea floor measurements) and estimated state (for vehicle
     * position) for bathymetry.
     * Also saves the highest point for bathymetry to have a reference for sea level as well
     * as the reference location for the offsets in the data.
     */
    private void calcBathymetryData() {
        // get raw info from log and init associated vars
        final IMraLog bParser = source.getLog("BottomDistance");
        final IMraLog stateParser = source.getLog("EstimatedState");
        EstimatedState stateEntry = null;
        IMCMessage bEntry = bParser.nextLogEntry();
        // init vars for points
        xyzNEDmeters = new DataDiscretizer(1);
        xVec = new Vector<Double>();
        yVec = new Vector<Double>();
        waterColumnVec = new Vector<Double>();
        vehicleDepthVec = new Vector<Double>();
        LocationType tmp = new LocationType();
        imageCornerRef = calculateHomeRef((EstimatedState) stateParser.nextLogEntry());
        data.setReferenceLocation(imageCornerRef);
        // rules out unreliable sensors
        double waterColumn;
        double[] offs;
        float minDepth = Float.MAX_VALUE; // impossible high value
        float maxDepth = Float.MIN_VALUE; // impossible low value


        while (bEntry != null) {
            stateEntry = (EstimatedState) stateParser.getEntryAtOrAfter(bParser.currentTimeMillis());
            if (stateEntry == null) {
                // bParser.advance((long) (timestep * 1000));
                // bEntry = bParser.getCurrentEntry();
                // continue;

                // In case this estimated state isn't valid use last one
                stateEntry = (EstimatedState) stateParser.getLastEntry();
            }
            bEntry = advanceLogParserIrrelevantEntries(bParser, bEntry);

            tmp = getLocationIMC4(stateEntry); // Caso haja erro verificar a estrutura do EstimatedState
            tmp.convertToAbsoluteLatLonDepth();

            // Distance between
            waterColumn = tmp.getDepth() + bEntry.getDouble("value");
            if (waterColumn < minDepth) {
                minDepth = (float) waterColumn;
            }
            if (waterColumn > maxDepth) {
                maxDepth = (float) waterColumn;
            }
            offs = tmp.getOffsetFrom(imageCornerRef); // in NED
            xVec.add(offs[0]);
            yVec.add(offs[1]);
            waterColumnVec.add(-waterColumn);
            vehicleDepthVec.add(tmp.getDepth());
            // (x,y,z) => (E,-N,D)
            xyzNEDmeters.addPoint(offs[1], -offs[0], -waterColumn);
            bEntry = bParser.nextLogEntry();
            bEntry = advanceLogParserIrrelevantEntries(bParser, bEntry);
        }
        data.setNorthVec(xVec);
        data.setEastVec(yVec);
        data.setDepthVec(vehicleDepthVec);
        data.setMaxWaterColumn(maxDepth);
        data.setMinWaterColumn(minDepth);
    }

    /**
     * GPS heigh varies a lot so the mean should be calculated with as many values as possible. All values with enough
     * satallites are considered even if they are in different moments. Sets in global waterHeight the height from the
     * ellipsoid in meters
     * 
     * @return true if there are values for GPSFix, false otherwise
     */
    private boolean calculateWaterHeightFromGPSFix() {
        final IMraLog gpsParser = source.getLog("GpsFix");
        if (gpsParser == null)
            return false;

        IMCMessage gpsFixMsg;
        float countValid = 0;
        float height = 0;
        double tempSatellites = 0;
        float mean = 0;
        gpsFixMsg = gpsParser.nextLogEntry();
        do {
            // check if there is enough satellite signal to get a position
            tempSatellites = gpsFixMsg.getDouble("satellites");
            if (tempSatellites >= MIN_NUM_SATALLITES) {
                countValid++;
                height += gpsFixMsg.getDouble("height");
            }
            gpsFixMsg = gpsParser.nextLogEntry();
        } while (gpsFixMsg != null);
        // if there was any valid message the mean was calculated
        if (countValid > 0) {
            mean = height / countValid;
            data.setWaterHeight(mean);
            return true;
        }
        else {
            NeptusLog.pub()
                    .info("Cannot capture height of vehicle from GPS. All functions dependent on this information will be disabled.");
            data.setWaterHeight(BathymetryLogInfo.INVALID_HEIGHT);
            return false;
        }
    }

    /**
     * Generates image according to interpolation data and ColorMap and sets to BathymetryLogInfo object. Also sets
     * ScaleImageToMetersX, ScaleImageToMetersY in BathymetryLogInfo to enable conversion to Location.
     * 
     * @param xyzData interpolation data (see interpolateData())
     * @param colorMap colors for the image
     */
    private void generateBufferedImage(ColorMap colorMap) {
        final DataPoint[] dps = xyzNEDmeters.getDataPoints();
        java.awt.geom.Rectangle2D.Double bounds;
        float depthScale, scaleImageToMetersX, scaleImageToMetersY;
        int imageSize;
        int iImageSize = 0;
        int imageSizes[] = { 1024 };
        do {
            imageSize = imageSizes[iImageSize];
            // calculates best image boundaries for target image size
            bounds = calculateBounds(xyzNEDmeters, imageSize, imageSize);
            // Calculate ratio to convert from pixels to meters
            scaleImageToMetersX = (float) (imageSize / bounds.getWidth());
            scaleImageToMetersY = (float) (imageSize / bounds.getHeight());
            depthScale = (scaleImageToMetersX > scaleImageToMetersY) ? (scaleImageToMetersY) : (scaleImageToMetersX);
            iImageSize++;
        } while (depthScale > 1 && iImageSize < imageSizes.length);

        // find amount of meters to add to heigt encoded into height map to get the same scale as the smallest between
        // lat and lon scales
        float terrainDepthDiference = data.getMaxWaterColumn() - data.getMinWaterColumn();
        float extraDepth = 255 / depthScale - terrainDepthDiference;
        data.setDeltaDepth(extraDepth + terrainDepthDiference);

        setOffsetHomeRef2TopLeftInMeters(bounds);
        data.setScaleImageToMetersX(scaleImageToMetersX); // Px/m
        data.setScaleImageToMetersY(scaleImageToMetersY);
        // generate the actual image
        final BufferedImage buffImage = new BufferedImage(imageSize, imageSize,
                BufferedImage.TYPE_INT_RGB);
        ColorMapUtils.generateInterpolatedColorMap(bounds, dps, 0, buffImage.createGraphics(), buffImage.getWidth(),
                buffImage.getHeight(), 255, colorMap, xyzNEDmeters.minVal[0], xyzNEDmeters.maxVal[0] + extraDepth);
        // graphToFile(buffImage, "/home/meg/depthScale.jpg");
        data.setBuffImageHeightMap(buffImage);
    }

    private void setOffsetHomeRef2TopLeftInMeters(final java.awt.geom.Rectangle2D.Double bounds) {
        LocationType heightMapOrigin = new LocationType(data.getReferenceLocation());
        // (x,y,z) >> (E,-N,D)
        // North was stored inverted as y
        // East was stored as x
        // (N,E,D) >> (-maxY, minX, -)
        heightMapOrigin.translatePosition(-bounds.getMaxY(), bounds.getMinX(), 0);
        heightMapOrigin.convertToAbsoluteLatLonDepth();
        double[] offsetFromOrigin = heightMapOrigin.getOffsetFrom(imageCornerRef);
        data.setReferenceTopLeftCornerOffsets(offsetFromOrigin);
    }

    /**
     * Saves the image to the designated path
     * 
     * @param heightImage the image
     * @param path the path where to save
     */
    @SuppressWarnings("unused")
    private void graphToFile(BufferedImage heightImage, String path) {
        final File image = new File(path);
        XYZUtils.saveImageToJPG(heightImage, image);
        NeptusLog.pub().info("Image generated to " + path);
    }
}
