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
 * Aug 20, 2012
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