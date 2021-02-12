/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * 14/05/2016
 */
package pt.lsts.neptus.mp.preview.payloads;

import java.util.ArrayList;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.plugins.GroundHeight;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class CameraFOV {

    private double hAOV, vAOV, roll, pitch, yaw, altitude, tilt = 0;
    private LocationType loc = new LocationType();
    private Transform3D[] quad = new Transform3D[4];

    /**
     * Create a Camera FOV instance
     * @param horAOVrads This camera's horizontal angle of view, in radians
     * @param verAOVrads This camera's vertical angle of view, in radians
     */
    public CameraFOV(double horAOVrads, double verAOVrads) {
        this.hAOV = horAOVrads;
        this.vAOV = verAOVrads;

        for (int i = 0; i < 4; i++)
            quad[i] = new Transform3D();
        quad[0].setEuler(new Vector3d(-getDiagAOV() / 2, 0, getDiagAngle()));
        quad[1].setEuler(new Vector3d(-getDiagAOV() / 2, 0, Math.PI - getDiagAngle()));
        quad[2].setEuler(new Vector3d(-getDiagAOV() / 2, 0, Math.PI + getDiagAngle()));
        quad[3].setEuler(new Vector3d(-getDiagAOV() / 2, 0, -getDiagAngle()));
    }
    
    public static  CameraFOV defaultFov() {
        CameraFOV fov = new CameraFOV(Math.toRadians(60), Math.toRadians(45));
        fov.setTilt(-60);
        return fov;
    }

    /**
     * Retrieve world coordinates from (normalized) camera coords
     * @param normalizedScreenX The horizontal position in the screen. 0 is center, -1 is all the way left and 1 is all the way right
     * @param normalizedScreenY The vertical position in the screen. 0 is center, -1 is all the way down and 1 is all the way up
     * @return The World location on the direction of the clicked position
     */
    public LocationType getLookAt(double normalizedScreenX, double normalizedScreenY) {
        double hDist = Math.tan(hAOV / 2);
        double vDist = Math.tan(vAOV / 2);
        double x = normalizedScreenX * hDist;
        double y = normalizedScreenY * vDist;
        double pitch = Math.atan(Math.sqrt(x*x + y*y));
        double yaw = (Math.PI/2)-Math.atan2(y, x);
        LocationType loc = new LocationType(this.loc);
        Point3d lookDown = new Point3d(0, 0, 1);
        Transform3D euler = new Transform3D();
        euler.setEuler(new Vector3d(0, pitch, yaw));
        euler.transform(lookDown);
        euler.setEuler(new Vector3d(getRoll(), getPitch()+tilt, getYaw()));
        euler.transform(lookDown);
        Transform3D scale = new Transform3D();
        scale.setScale(getAltitude() / lookDown.z);
        scale.transform(lookDown);
        loc.translatePosition(lookDown.x, lookDown.y, 0);        
        return loc;
        
    }
    
    /**
     * Calculate this camera's diagonal angle of view
     * @return this camera's diagonal angle of view, in radians
     */
    public double getDiagAOV() {
        double hDist = Math.tan(hAOV / 2);
        double vDist = Math.tan(vAOV / 2);
        double hyp = Math.sqrt(hDist * hDist + vDist * vDist);
        return Math.atan(hyp) * 2;
    }

    private double getDiagAngle() {
        double hDist = Math.tan(hAOV / 2);
        double vDist = Math.tan(vAOV / 2);
        return Math.atan2(vDist, hDist);
    }

    /**
     * Set the camera roll angle
     * @param rollRads roll angle, in radians
     */
    public void setRoll(double rollRads) {
        this.roll = rollRads;
    }

    /**
     * Set the camera pitch angle
     * @param pitch pitch angle, in radians
     * @see #setTilt(double)
     */
    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    /**
     * Set the camera yaw angle
     * @param pitch yaw angle, in radians
     */    
    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    /**
     * Set this camera altitude to the ground
     * @param alt altitude, in meters
     */
    public void setAltitude(double alt) {
        this.altitude = alt;
    }

    /**
     * Set this camera's location in the world
     * @param loc camera's world location
     */
    public void setLocation(LocationType loc) {
        this.loc.setLocation(loc);
    }

    /**
     * @return camera's altitude, in meters
     */
    public double getAltitude() {
        return altitude;
    }

    /**
     * @return camera's roll, in radians
     */
    public double getRoll() {
        return roll;
    }

    /**
     * @return camera's pitch, in radians
     */
    public double getPitch() {
        return pitch;
    }

    /**
     * @return camera's yaw, in radians
     */
    public double getYaw() {
        return yaw;
    }

    /**
     * @return camera's position in the world
     */
    public LocationType getLocation() {
        return loc;
    }
    
    /**
     * Set the camera's tilt angle (pitch mount angle)
     * @param tiltRads tilt angle, in radians
     */
    public void setTilt(double tiltRads) {
        this.tilt = tiltRads;
    }

    /**
     * @return the tilt
     */
    public double getTilt() {
        return tilt;
    }

    /**
     * Set the state of this camera (will update position, altitude and all euler angles)
     * @param state The state of this camera
     */
    public void setState(SystemPositionAndAttitude state) {
        setLocation(state.getPosition());
        setAltitude(state.getAltitude());
        setRoll(state.getRoll());
        setPitch(state.getPitch());
        setYaw(state.getYaw());
    }
    
    /**
     * Set the state of this camera from a vehicle's position (will update position, altitude and all euler angles)
     * @param state The state to apply to this camera
     */
    public void setState(EstimatedState state) {
        setLocation(IMCUtils.parseLocation(state));
        setAltitude(state.getHeight() - GroundHeight.instance().getHeight());
        setRoll(state.getPhi());
        setPitch(state.getTheta());
        setYaw(state.getPsi());
    }

    /**
     * Calculate the world coordinate that this camera is pointing at, in the ground
     * @return Coordinates where this camera is pointing to 
     */
    public LocationType getLookAt() {
        LocationType loc = new LocationType(this.loc);
        Point3d lookDown = new Point3d(0, 0, 1);
        Transform3D euler = new Transform3D();
        euler.setEuler(new Vector3d(getRoll(), getPitch()+tilt, getYaw()));
        euler.transform(lookDown);
        Transform3D scale = new Transform3D();
        scale.setScale(getAltitude() / lookDown.z);
        scale.transform(lookDown);
        loc.translatePosition(lookDown.x, lookDown.y, 0);
        return loc;
    }
    
    /**
     * Calculate the area, in the world that this camera is capturing
     * @return a list with the 4 corners of this camera's footprint in the ground 
     */
    public ArrayList<LocationType> getFootprintQuad() {
        ArrayList<LocationType> result = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            Point3d pt = new Point3d(0, 0, 1);
            Transform3D trans = new Transform3D();
            trans.setEuler(new Vector3d(getRoll(), getPitch()+tilt, getYaw()));
            trans.mul(quad[i]);
            trans.transform(pt);
            trans = new Transform3D();
            trans.setScale(getAltitude() / pt.z);
            trans.transform(pt);
            LocationType loc = new LocationType(this.loc);
            loc.translatePosition(pt.x, pt.y, 0);
            result.add(loc);
        }

        return result;
    }    
    
    @Override
    public String toString() {
        return String.format("CameraFOV{hAOV=%.2f, vAOV=%.2f, tilt=%.2f, %s -> %s (%.2f)", Math.toDegrees(hAOV),
                Math.toDegrees(vAOV), Math.toDegrees(tilt), getLocation(), getLookAt(),
                getLocation().getDistanceInMeters(getLookAt()));
    }
}
