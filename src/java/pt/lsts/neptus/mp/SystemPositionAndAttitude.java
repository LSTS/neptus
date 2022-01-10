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
 * Author: 
 * 14/Set/2004
 */
package pt.lsts.neptus.mp;

import java.io.Serializable;

import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author Ze Carlos This class represents a vehicle state in a single moment
 */
public class SystemPositionAndAttitude implements Cloneable, Comparable<SystemPositionAndAttitude>, Serializable {

    private static final long serialVersionUID = -5241496476923632391L;
    private double roll = 0;
    private double pitch = 0; 
    private double yaw = 0;
    private double altitude = 0;
    //private double depth = -1;

    private Vector3Dimension pqr = new Vector3Dimension();
    private Vector3Dimension uvw = new Vector3Dimension();
    private Vector3Dimension vxyz = new Vector3Dimension();

    private long time = 0;

    private LocationType position = new LocationType(), guine = new LocationType();

    /**
     * Empty constructor
     */
    public SystemPositionAndAttitude() {
    }

    /**
     * Creates a new VehicleState from the given position and axis attitudes
     * 
     * @param position The vehicle position
     * @param roll The vehicle rotation over the X (South->North) axis (radians)
     * @param pitch The vehicle rotation over the Y (West->East) axis (radians)
     * @param yaw The vehicle rotation over the Z (Down->Up) axis (radians)
     */
    public SystemPositionAndAttitude(LocationType position, double roll, double pitch, double yaw) {
        setPosition(position);
        setRoll(roll);
        setPitch(pitch);
        setYaw(yaw);
    }

    public EstimatedState toEstimatedState() {
        EstimatedState estate = new EstimatedState();
        LocationType loc = new LocationType(getPosition());
        loc.convertToAbsoluteLatLonDepth();
        estate.setLat(loc.getLatitudeRads());
        estate.setLon(loc.getLongitudeRads());
        estate.setDepth(loc.getDepth());
        estate.setAlt(getAltitude());

        estate.setPhi(getRoll());
        estate.setTheta(getPitch());
        estate.setPsi(getYaw());

        estate.setP(getP());
        estate.setQ(getQ());
        estate.setR(getR());

        estate.setU(getU());
        estate.setV(getV());
        estate.setW(getW());
        estate.setVx(getVx());
        estate.setVy(getVy());
        estate.setVz(getVz());

        return estate;

    }

    public SystemPositionAndAttitude(SystemPositionAndAttitude vs) {
        setPosition(new LocationType(vs.getPosition()));
        setRoll(vs.getRoll());
        setPitch(vs.getPitch());
        setYaw(vs.getYaw());
        setPQR(vs.getP(), vs.getQ(), vs.getR());
        setUVW(vs.getU(), vs.getV(), vs.getW());
        setVxyz(vs.getVx(), vs.getVy(), vs.getVz());
        
        setAltitude(vs.altitude);
    }

    public SystemPositionAndAttitude(EstimatedState state) {
        LocationType loc = new LocationType();
        loc.setLatitudeRads(state.getLat());
        loc.setLongitudeRads(state.getLon());
        loc.setDepth(0);
        loc.translatePosition(state.getX(), state.getY(), 0);
        setPosition(loc);
        setRoll(state.getPhi());
        setPitch(state.getTheta());
        setYaw(state.getPsi());
        setPQR(state.getP(), state.getQ(), state.getR());
        setUVW(state.getU(), state.getV(), state.getW());
        setVxyz(state.getVx(), state.getVy(), state.getVz());
        
        setAltitude(state.getAlt());
    }

    /**
     * This function changes this state by rotating the vehicle according to the angle given This rotation will be done
     * only in the horizontal plane (x,y)
     * 
     * @param radians The rotation angle
     * 
     * @uml.property name="psi"
     */
    public void rotateXY(double radians) {
        setYaw(getYaw() + radians);
        if (getYaw() >= (Math.PI) * 2)
            setYaw(getYaw() - Math.PI * 2);
        else if (getYaw() <= -Math.PI * 2)
            setYaw(getYaw() + Math.PI * 2);
    }

    public void addNEDOffsets(double n, double e, double d) {
        position.translatePosition(n, e, d);
    }

    /**
     * This function changes the current state by moving the vehicle forward If the value of meters is negative, the
     * vehicle will actually move back
     * 
     * @param meters The number of meters to advance or go back (if negative)
     */
    public void moveForward(double meters) {
        getPosition().setOffsetEast(getPosition().getOffsetEast() + Math.sin(getYaw()) * meters);
        getPosition().setOffsetNorth(getPosition().getOffsetNorth() + Math.cos(getYaw()) * meters);
        // this.vehiclePosition.y += (float)Math.sin((double) psi) * meters;
    }

    /**
     * This function changes the current state by moving the vehicle up If the value of meters is negative, the vehicle
     * will actually move down
     * 
     * @param meters The number of meters to move up or down (if negative)
     */
    public void moveUp(float meters) {
        getPosition().setOffsetDown(getPosition().getOffsetDown() - meters);
        // this.vehiclePosition.z += meters;
    }

    /**
     * Creates a new cloned SystemPositionAndAttitudeState
     */
    public SystemPositionAndAttitude clone() {

        SystemPositionAndAttitude cloneState = new SystemPositionAndAttitude(getPosition(), getRoll(),
                getPitch(), getYaw());

        LocationType lt = new LocationType();
        lt.setLocation(getPosition());
        cloneState.setPitch(getPitch());
        cloneState.setRoll(getRoll());
        cloneState.setYaw(getYaw());
        cloneState.setAltitude(getAltitude());
        cloneState.setDepth(getDepth());

        return cloneState;
    }

    /**
     * Returns the offset from absolute 0 as double[3]:
     * <p>
     * <blockquote>
     * <li>0 - The offset (in the North direction) from absolut (0,0,0)
     * <li>1 - The offset (in the East direction) from absolut (0,0,0)
     * <li>2 - The offset (in the Down direction) from absolut (0,0,0) </blockquote>
     * <p>
     * 
     * @return A 3 component absolute offset
     */
    public double[] getNEDPosition() {
        return getPosition().getOffsetFrom(guine);
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public LocationType getPosition() {
        return position;
    }

    public void setPosition(LocationType position) {
        this.position.setLocation(position);
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    /**
     * @return the depth
     */
    public double getDepth() {
        return position.getDepth();        
    }
    
    /**
     * @param depth the depth to set
     */
    public void setDepth(double depth) {
        this.position.setDepth(depth);
    }
    
    public double getRoll() {
        return roll;
    }

    public void setRoll(double roll) {
        this.roll = roll;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public String toString() {
        return "System Position and Attitude State:\nNorth: " + getPosition().getOffsetNorth() + ", East: "
                + getPosition().getOffsetEast() + ", Down: " + getPosition().getOffsetDown() + "\n" + "Roll: "
                + getRoll() + ", Pitch: " + getPitch() + ", Yaw: " + getYaw();
    }

    public double getP() {
        return pqr.x;
    }

    public void setP(double p) {
        this.pqr.x = p;
    }

    public double getQ() {
        return pqr.y;
    }

    public void setQ(double q) {
        this.pqr.y = q;
    }

    public double getR() {
        return pqr.z;
    }

    public void setR(double r) {
        this.pqr.z = r;
    }

    public double getU() {
        return uvw.x;
    }

    public void setU(double u) {
        this.uvw.x = u;
    }

    public double getV() {
        return uvw.y;
    }

    public void setV(double v) {
        uvw.y = v;
    }

    public double getW() {
        return uvw.z;
    }

    public void setW(double w) {
        uvw.z = w;
    }

    public void setUVW(double u, double v, double w) {
        uvw.x = u;
        uvw.y = v;
        uvw.z = w;
    }

    public void setPQR(double p, double q, double r) {
        pqr.x = p;
        pqr.y = q;
        pqr.z = r;
    }

    public Vector3Dimension getPqr() {
        return pqr;
    }

    public void setPqr(Vector3Dimension pqr) {
        this.pqr = pqr;
    }

    public Vector3Dimension getUvw() {
        return uvw;
    }

    public void setUvw(Vector3Dimension uvw) {
        this.uvw = uvw;
    }

    public Vector3Dimension getVxyz() {
        return vxyz;
    }

    public void setVxyz(Vector3Dimension vxyz) {
        this.vxyz = vxyz;
    }

    public void setVxyz(double vx, double vy, double vz) {
        vxyz.x = vx;
        vxyz.y = vy;
        vxyz.z = vz;
    }

    public double getVx() {
        return vxyz.x;
    }

    public double getVy() {
        return vxyz.y;
    }

    public double getVz() {
        return vxyz.z;
    }

    /**
     * @return the time
     */
    public long getTime() {
        return time;
    }

    /**
     * @param time the time to set
     */
    public void setTime(long time) {
        this.time = time;
    }

    private static class Vector3Dimension implements Serializable { 
        private static final long serialVersionUID = 3751192816949152977L;
        public double x = 0;
        public double y = 0;
        public double z = 0;
    }

    @Override
    public int compareTo(SystemPositionAndAttitude o) {
        return ((Long)time).compareTo(o.time);
    }    
}
