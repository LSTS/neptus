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
 * Aug 20, 2012
 * $Id:: VehicleInfoAtPointDTO.java 9615 2012-12-30 23:08:28Z pdias             $:
 */
package pt.up.fe.dceg.neptus.plugins.r3d.dto;

import com.jme3.math.Vector3f;

/**
 * Coordinate information about a point of the vehicle path.
 * <p> Always has latitude, longitude, depth, yaw, pitch and roll.
 * <p> Has bottom distance if available on log in that position.
 * <p> When log information is processed stores position in 3D world coordinates as well as wing points, as needed.
 * 
 * @author Margarida Faria
 *
 */
public class VehicleInfoAtPointDTO {
    private Vector3f positionLatLonDepth;
    private Vector3f positionXYZ;
    private Vector3f yawPitchRoll;
    private Vector3f wingPoints[];

    private float bottomDist;

    /**
     * Initializes bottom distance to NaN, to signal if it's not assigned meaningful value.
     */
    public VehicleInfoAtPointDTO() {
        super();
        this.bottomDist = Float.NaN;
        this.wingPoints = null;
    }

    /**
     * 
     * @param latitude
     * @param longitude
     * @param depth
     */
    public void setLatLonDepth(float latitude, float longitude, float depth) {
        this.positionLatLonDepth = new Vector3f(latitude, longitude, depth);
    }

    /**
     * 
     * @param orientationUnitX
     * @param negate
     */
    public void setWings(Vector3f orientationUnitX, Vector3f negate) {
        wingPoints = new Vector3f[2];
        wingPoints[0] = orientationUnitX;
        wingPoints[1] = negate;
    }

    /**
     * 
     * @return
     */
    public Vector3f getWingLeft() {
        return wingPoints[0];
    }

    /**
     * 
     * @return
     */
    public Vector3f getWingRight() {
        return wingPoints[1];
    }

    /**
     * @return
     */
    public float getLatitude() {
        return positionLatLonDepth.x;
    }

    /**
     * @return
     */
    public float getLongitude() {
        return positionLatLonDepth.y;
    }

    /**
     * @return
     */
    public Vector3f getLatLonDepth() {
        return positionLatLonDepth;
    }

    /**
     * @return
     */
    public float getYaw() {
        return yawPitchRoll.x;
    }

    /**
     * @return
     */
    public float getRoll() {
        return yawPitchRoll.z;
    }

    /**
     * @param yawPitchRoll the yawPitchRoll to set
     */
    public void setYawPitchRoll(Vector3f yawPitchRoll) {
        this.yawPitchRoll = yawPitchRoll;
    }


    /**
     * @return the positionXYZ
     */
    public Vector3f getPositionXYZ() {
        return positionXYZ;
    }

    /**
     * @param positionXYZ the positionXYZ to set
     */
    public void setPositionXYZ(Vector3f positionXYZ) {
        this.positionXYZ = positionXYZ;
    }

    /**
     * @return the bottomDist
     */
    public float getBottomDist() {
        return bottomDist;
    }

    /**
     * @param bottomDist the bottomDist to set
     */
    public void setBottomDist(float bottomDist) {
        this.bottomDist = bottomDist;
    }

    /**
     * @param angleBetween
     */
    public void setYaw(float angle) {
        yawPitchRoll.x = angle;
    }

}