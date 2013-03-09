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
 * Jun 28, 2012
 */
package pt.up.fe.dceg.neptus.plugins.r3d.dto;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Vector;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * Class with all the data extracted from logs that is needed from the bathymetry 3D plugin
 * 
 * @author Margarida Faria
 * 
 */
public class BathymetryLogInfo {
    public static final int INVALID_HEIGHT = -1000;
    private float waterHeightM = INVALID_HEIGHT;
    private float minWaterColumnM = Float.MAX_VALUE;
    private float maxWaterColumnM = Float.MIN_VALUE;
    private float deltaDepth;

    // set on generateBufferedImage()
    private BufferedImage buffImageHeightMap;
    private float scaleImageToMetersX, scaleImageToMetersY;
    private LocationType referenceLocation;
    private double[] referenceTopLeftCornerOffsets;
    private Vector<Double> northVec, eastVec, depthVec;
    private Vector<Double> rollVector, yawVector;

    private final ArrayList<VehicleInfoAtPointDTO> vehicleInfo;

    /**
     * @return the vehicleInfo
     */
    public ArrayList<VehicleInfoAtPointDTO> getVehicleInfo() {
        return vehicleInfo;
    }

    /**
     * @param vehicleInfo
     */
    public BathymetryLogInfo(ArrayList<VehicleInfoAtPointDTO> vehicleInfo) {
        super();
        this.vehicleInfo = vehicleInfo;
    }

    /**
     * Is the smallest scale between latitude scale and longitude scale
     * 
     * @return the slected scale
     */
    // public float getDepthScale() {
    // return (scaleImageToMetersX > scaleImageToMetersY) ? (scaleImageToMetersY) : (scaleImageToMetersX);
    // }

    /**
     * @return the deltaDepth
     */
    public float getDeltaDepth() {
        return deltaDepth;
    }

    /**
     * @param deltaDepth the deltaDepth to set
     */
    public void setDeltaDepth(float deltaDepth) {
        this.deltaDepth = deltaDepth;
    }

    /**
     * 
     */
    public BathymetryLogInfo() {
        vehicleInfo = null;
    }

    /**
     * @return the xVec
     */
    public Vector<Double> getNorthVec() {
        return northVec;
    }

    /**
     * @param northVec the xVec to set
     */
    public void setNorthVec(Vector<Double> northVec) {
        this.northVec = northVec;
    }

    /**
     * @return the yVec
     */
    public Vector<Double> getEastVec() {
        return eastVec;
    }

    /**
     * @param eastVec the yVec to set
     */
    public void setEastVec(Vector<Double> eastVec) {
        this.eastVec = eastVec;
    }

    /**
     * @return the zVec
     */
    public Vector<Double> getDepthVec() {
        return depthVec;
    }

    /**
     * @param depthVec the zVec to set
     */
    public void setDepthVec(Vector<Double> depthVec) {
        this.depthVec = depthVec;
    }

    /**
     * @param referenceTopLeftCornerOffsets the referenceTopLeftCornerOffsets to set
     */
    public void setReferenceTopLeftCornerOffsets(double[] referenceTopLeftCornerOffsets) {
        this.referenceTopLeftCornerOffsets = referenceTopLeftCornerOffsets;
    }

    /**
     * @return the referenceLocation
     */
    public LocationType getReferenceLocation() {
        return referenceLocation;
    }

    /**
     * @param referenceLocation the referenceLocation to set
     */
    public void setReferenceLocation(LocationType referenceLocation) {
        this.referenceLocation = referenceLocation;
    }

    /**
     * @return the scaleImageToMetersX
     */
    public float getScaleImageToMetersX() {
        return scaleImageToMetersX;
    }

    /**
     * @param scaleImageToMetersX the scaleImageToMetersX to set
     */
    public void setScaleImageToMetersX(float scaleImageToMetersX) {
        this.scaleImageToMetersX = scaleImageToMetersX;
    }

    /**
     * @return the scaleImageToMetersY
     */
    public float getScaleImageToMetersY() {
        return scaleImageToMetersY;
    }

    /**
     * @param scaleImageToMetersY the scaleImageToMetersY to set
     */
    public void setScaleImageToMetersY(float scaleImageToMetersY) {
        this.scaleImageToMetersY = scaleImageToMetersY;
    }

    /**
     * @return the waterHeight
     */
    public float getWaterHeight() {
        return waterHeightM;
    }

    /**
     * @param waterHeight the waterHeight to set
     */
    public void setWaterHeight(float waterHeight) {
        this.waterHeightM = waterHeight;
    }

    /**
     * @return the minDepth
     */
    public float getMinWaterColumn() {
        return minWaterColumnM;
    }

    /**
     * @return the maxDepth
     */
    public float getMaxWaterColumn() {
        return maxWaterColumnM;
    }

    /**
     * @return the buffImageHeightMap
     */
    public BufferedImage getBuffImageHeightMap() {
        return buffImageHeightMap;
    }

    /**
     * @param buffImageHeightMap the buffImageHeightMap to set
     */
    public void setBuffImageHeightMap(BufferedImage buffImageHeightMap) {
        this.buffImageHeightMap = buffImageHeightMap;
    }

    /**
     * @return BathymetryLogInfo [waterHeight=waterHeight, minDepth=minDepth, maxDepth=maxDepth, latitude=latitude,
     *         longitude=longitude]
     */
    @Override
    public String toString() {
        return "BathymetryLogInfo [" + I18n.text("waterHeight") + "=" + waterHeightM + ", " + I18n.text("minDepth=")
                + minWaterColumnM + ", " + I18n.text("maxDepth=") + maxWaterColumnM + ", "
                + I18n.text("referenceLocation=") + referenceLocation.toString() + ", # "
                + I18n.text("independent vehicle path points=") + vehicleInfo.size() + "]";
    }

    /**
     * Sets maximum.
     * 
     * @param max depth in meters
     * @param min depth in meters
     */
    public void setMaxWaterColumn(float max) {
        maxWaterColumnM = max;
    }

    /**
     * Sets minimum depth
     * 
     * @param min depth in meters
     */
    public void setMinWaterColumn(float min) {
        minWaterColumnM = min;
    }


    /**
     * @return the referenceTopLeftCornerOffsets
     */
    public double[] getReferenceTopLeftCornerOffsets() {
        return referenceTopLeftCornerOffsets;
    }


    /**
     * 
     * @return the rollVector
     */
    public Vector<Double> getRollVector() {
        return rollVector;
    }

    /**
     * @param rollVector the rollVector to set
     */
    public void setRollVector(Vector<Double> rollVector) {
        this.rollVector = rollVector;
    }

    /**
     * @return the yawVector
     */
    public Vector<Double> getYawVector() {
        return yawVector;
    }

    /**
     * @param yawVector the yawVector to set
     */
    public void setYawVector(Vector<Double> yawVector) {
        this.yawVector = yawVector;
    }

}
