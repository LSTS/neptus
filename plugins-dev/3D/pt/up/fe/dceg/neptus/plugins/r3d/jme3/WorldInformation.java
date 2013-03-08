/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Jul 9, 2012
 * $Id:: WorldInformation.java 9924 2013-02-14 10:37:31Z mfaria                 $:
 */
package pt.up.fe.dceg.neptus.plugins.r3d.jme3;

import pt.up.fe.dceg.neptus.plugins.r3d.dto.BathymetryLogInfo;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.coord.egm96.EGM96Util;

import com.jme3.math.Vector3f;

/**
 * Object with general information about the world. Most of it is set by the data on log and need on multiple 3D
 * elements.
 * <p>
 * Handles all the conversions between meters and world scale.
 * 
 * @author Margarida Faria
 * 
 */
public class WorldInformation {
    public static final Vector3f LIGHT_DIR = new Vector3f(0.1f, -1f, -0.1f);
    public static final int PATCH_SIZE = 65;
    public final int mapSize;

    private final float scaleImageToMetersX, scaleImageToMetersY;
    private final float deltaDepthM;
    private final LocationType referenceLocation;
    private final float referenceTopLeftCornerOffsetsM[];
    private final float terrainLvl;
    private final float geoidtLvl;
    private final float waterLvl;
    private final float waterHeightM;
    private final float geoidHeightM;
    private final float maxWaterColumnM;
    private final float minWaterColumnM;

    public final int SCALE_METERS2JME_DIRECT_FROM_LOG = 10;
    private final LocationType origin_directFromLog;

    /**
     * @param bathyInfo
     * @param cornerOffsetsM
     */
    /**
     * @param bathyInfo
     * @param cornerOffsetsM
     */
    public WorldInformation(BathymetryLogInfo bathyInfo, float cornerOffsetsM[]) {
        mapSize = bathyInfo.getBuffImageHeightMap().getWidth();
        // variables for representations on scale with height map
        deltaDepthM = bathyInfo.getDeltaDepth();
        maxWaterColumnM = bathyInfo.getMaxWaterColumn();
        minWaterColumnM = bathyInfo.getMinWaterColumn();
        scaleImageToMetersX = bathyInfo.getScaleImageToMetersX();
        scaleImageToMetersY = bathyInfo.getScaleImageToMetersY();
        referenceLocation = bathyInfo.getReferenceLocation();
        referenceTopLeftCornerOffsetsM = cornerOffsetsM;
        if (bathyInfo.getWaterHeight() != BathymetryLogInfo.INVALID_HEIGHT) {
            waterHeightM = bathyInfo.getWaterHeight();
            waterLvl = convertHeightMeter2Px_heightMapScale(waterHeightM);
        }
        else {
            waterLvl = BathymetryLogInfo.INVALID_HEIGHT;
            waterHeightM = BathymetryLogInfo.INVALID_HEIGHT;
        }
        geoidHeightM = calcGeoidHeightOnLocation();
        terrainLvl = convertDepthMeter2Px_heightMapScale(bathyInfo.getMaxWaterColumn());
        geoidtLvl = convertHeightMeter2Px_heightMapScale(geoidHeightM);

        if (JmeComponent.TEST_CURVE) {
        // variables on scale with independent lat lon depth from log
        origin_directFromLog = new LocationType(Math.toDegrees(bathyInfo.getVehicleInfo().get(0).getLatitude()),
                Math.toDegrees(bathyInfo.getVehicleInfo().get(0).getLongitude()));
        }
        else {
            origin_directFromLog = null;
        }
        // NeptusLog.pub().debug(
        // "waterM:" + waterHeightM + "; geoidM:" + geoidHeightM + "; deltaDepthM: " + deltaDepthM
        // + "; deltaDepthJME: " + convertHeightMeter2Px_heightMapScale(deltaDepthM)
        // + "; terrainLvl == maxDepth; ");
    }

    /**
     * Calculate offsets from reference location to desired location
     * 
     * @param location
     * @return
     */
    public double[] calcOffSets(LocationType location) {
        return location.getOffsetFrom(referenceLocation); // in NED
    }

    private double[] calcOffsets(float[] a, float[] b) {
        // Top left location
        LocationType topLeft = new LocationType(referenceLocation);
        topLeft.translatePosition(a[0], a[1], a[2]);
        topLeft.convertToAbsoluteLatLonDepth();
        // Point location
        LocationType point = new LocationType(referenceLocation);
        point.translatePosition(b[0], b[1], b[2]);
        point.convertToAbsoluteLatLonDepth();
        // Offset between them
        double offsetTopPoint[] = point.getOffsetFrom(topLeft);
        return offsetTopPoint;
    }
    
    /**
     * Scales the height to the height represented in the 255 of the height map.
     * 
     * @param meters
     * @return pixels
     */
    public float convertHeightMeter2Px_heightMapScale(float meters) {
        return meters * 255 / deltaDepthM;
    }
    
    /**
     * Transforms depth into height from geoid then converts from meters to pixels.
     * 
     * @param meters
     * @return pixels
     */
    public float convertDepthMeter2Px_heightMapScale(float meters) {
        return convertHeightMeter2Px_heightMapScale(waterHeightM - meters);
    }

    /**
     * Calculates the height of the reference location using EGM96 and sets it on geoidHeightM.
     */
    private float calcGeoidHeightOnLocation() {
        double[] absoluteLatLonDepth = referenceLocation.getAbsoluteLatLonDepth();
        // NeptusLog.pub().debug(I18n.text("location") + ": " + absoluteLatLonDepth[0] + " " + absoluteLatLonDepth[1]);
        return (float) EGM96Util.calcHeight(absoluteLatLonDepth[0], absoluteLatLonDepth[1]);
    }

    @Deprecated
    public Vector3f convertLatLonDepth2XYZ(Vector3f posLatLonDepth) {
        // the water is 0 y jME
        LocationType newPos = new LocationType(Math.toDegrees(posLatLonDepth.x), Math.toDegrees(posLatLonDepth.y));
        double offsets[] = origin_directFromLog.getOffsetFrom(newPos);
        // I'm keeping the translation of axis used for the image to be compatible with that kind of terrain
        // generation
        Vector3f vectorXYZ = new Vector3f(((float) offsets[1]) / SCALE_METERS2JME_DIRECT_FROM_LOG, (-posLatLonDepth.z)
                / SCALE_METERS2JME_DIRECT_FROM_LOG, ((float) -offsets[0]) / SCALE_METERS2JME_DIRECT_FROM_LOG);
        return vectorXYZ;
    }
    
    /**
     * Converts from NED to jME coordinates, having in consideration the height map scale and the offset between the
     * original reference location and the top left corner location.
     * 
     * @param pointOffset in NED (depth is not used)
     * @return array with [y Image px][ x Image px]
     */
    public float[] convertNED2jME_heightMapScale(float[] pointOffset) {
        // Calc offset between top left and point
        float[] a = { referenceTopLeftCornerOffsetsM[0], referenceTopLeftCornerOffsetsM[1], 0 };
        float[] b = { pointOffset[0], pointOffset[1], 0 };
        double[] offsetTopPoint = calcOffsets(a, b); // [North, East]
        // Convert from meters into pixels of the greyscale image
        float scaleOffset2Pixel[] = new float[2];
        scaleOffset2Pixel[1] = (float) Math.abs(offsetTopPoint[1]) * scaleImageToMetersX; // x off image correspondes to
                                                                                          // east
        scaleOffset2Pixel[0] = (float) Math.abs(offsetTopPoint[0]) * scaleImageToMetersY; // y off image correspondes to
                                                                                          // north
        return scaleOffset2Pixel;
    }


    /**
     * This only should be need for help screen information
     * 
     * @return the geoidHeightM
     */
    public float getGeoidHeightM() {
        return geoidHeightM;
    }

    /**
     * @return the terrainOffset
     */
    public float getTerrainLvl() {
        return terrainLvl;
    }

    /**
     * @return the geoidtLvl
     */
    public float getGeoidtLvl() {
        return geoidtLvl;
    }

    /**
     * @return the waterLvl
     */
    public float getWaterLvl() {
        return waterLvl;
    }

    /**
     * @return the mapSize
     */
    public int getMapSize() {
        return mapSize;
    }

    /**
     * @return the minTerrainDepth
     */
    public float getMaxWaterColumnM() {
        return maxWaterColumnM;
    }

    /**
     * @return the minTerrainDepth
     */
    public float getMinWaterColumnM() {
        return minWaterColumnM;
    }
}
