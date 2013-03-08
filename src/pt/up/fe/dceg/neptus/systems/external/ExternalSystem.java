/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 24 de Jun de 2012
 * $Id:: ExternalSystem.java 9615 2012-12-30 23:08:28Z pdias                    $:
 */
package pt.up.fe.dceg.neptus.systems.external;

import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.VehicleTypeEnum;

/**
 * @author pdias
 *
 */
public class ExternalSystem implements Comparable<ExternalSystem> {

    public static enum ExternalTypeEnum {UNKNOWN, VEHICLE, CCU, STATICSENSOR, MOBILESENSOR, MANNED_SHIP, MANNED_CAR, MANNED_AIRPLANE, PERSON, ALL};
    
    protected String id;
    protected SystemTypeEnum type = SystemTypeEnum.UNKNOWN;
    protected VehicleTypeEnum typeVehicle = VehicleTypeEnum.UNKNOWN;
    protected ExternalTypeEnum typeExternal = ExternalTypeEnum.UNKNOWN;
    
    protected boolean active = false;
//    protected PlanType activePlan = null;
    protected final CoordinateSystem location = new CoordinateSystem();
    protected long locationTimeMillis = -1;
    protected long attitudeTimeMillis = -1;

    /**
     * 
     */
    public ExternalSystem(String id) {
        this.id = id;
    }
    
    /**
     * @return the id
     */
    public String getId() {
        return id;
    }
    
    /**
     * @return
     */
    public String getName() {
        return getId();
    }
    
    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }
    
    
    /**
     * @return the location
     */
    public LocationType getLocation() {
        return location.getNewAbsoluteLatLonDepth();
    }
    
    /**
     * @param location the location to set
     */
    public void setLocation(LocationType location) {
        this.location.setLocation(location);
        this.location.convertToAbsoluteLatLonDepth();
        setLocationTimeMillis(System.currentTimeMillis());
    }

    public void setLocation(LocationType location, long locationTimeMillis) {
        this.location.setLocation(location);
        this.location.convertToAbsoluteLatLonDepth();
        setLocationTimeMillis(locationTimeMillis);
    }

    public void setAttitudeDegrees(double rollDegrees, double pitchDegrees, double yawDegrees) {
        location.setRoll(rollDegrees);
        location.setPitch(pitchDegrees);
        location.setYaw(yawDegrees);
        setAttitudeTimeMillis(System.currentTimeMillis());
    }

    public void setAttitudeDegrees(double rollDegrees, double pitchDegrees, double yawDegrees, long locationTimeMillis) {
        location.setRoll(rollDegrees);
        location.setPitch(pitchDegrees);
        location.setYaw(yawDegrees);
        setAttitudeTimeMillis(locationTimeMillis);
    }

    public void setAttitudeDegrees(double yawDegrees) {
        location.setRoll(0);
        location.setPitch(0);
        location.setYaw(yawDegrees);
        setAttitudeTimeMillis(System.currentTimeMillis());
    }

    public void setAttitudeDegrees(double yawDegrees, long locationTimeMillis) {
        location.setRoll(0);
        location.setPitch(0);
        location.setYaw(yawDegrees);
        setAttitudeTimeMillis(locationTimeMillis);
    }

    public double getRollDegrees() {
        return location.getRoll();
    }

    public double getPitchDegrees() {
        return location.getPitch();
    }

    public double getYawDegrees() {
        return location.getYaw();
    }

    /**
     * @return the locationTime
     */
    public long getLocationTimeMillis() {
        return locationTimeMillis;
    }
    
    /**
     * @param locationTimeMillis the locationTimeMillis to set
     */
    public void setLocationTimeMillis(long locationTimeMillis) {
        this.locationTimeMillis = locationTimeMillis;
    }
    
    /**
     * @return the attitudeTimeMillis
     */
    public long getAttitudeTimeMillis() {
        return attitudeTimeMillis;
    }
    
    /**
     * @param attitudeTimeMillis the attitudeTimeMillis to set
     */
    public void setAttitudeTimeMillis(long attitudeTimeMillis) {
        this.attitudeTimeMillis = attitudeTimeMillis;
    }
    
    /**
     * @return the type
     */
    public SystemTypeEnum getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(SystemTypeEnum type) {
        this.type = type;
    }

    /**
     * @return the typeVehicle
     */
    public VehicleTypeEnum getTypeVehicle() {
        return typeVehicle;
    }
    
    /**
     * @param typeVehicle the typeVehicle to set
     */
    public void setTypeVehicle(VehicleTypeEnum typeVehicle) {
        this.typeVehicle = typeVehicle;
    }
    
    /**
     * @return the typeExternal
     */
    public ExternalTypeEnum getTypeExternal() {
        return typeExternal;
    }
    
    /**
     * @param typeExternal the typeExternal to set
     */
    public void setTypeExternal(ExternalTypeEnum typeExternal) {
        this.typeExternal = typeExternal;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(ExternalSystem o) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getName();
    }
}
