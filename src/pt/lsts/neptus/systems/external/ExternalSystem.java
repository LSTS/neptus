/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 24 de Jun de 2012
 */
package pt.lsts.neptus.systems.external;

import pt.lsts.neptus.types.coord.CoordinateSystem;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;

/**
 * @author pdias
 *
 */
public class ExternalSystem implements Comparable<ExternalSystem> {

    public static enum ExternalTypeEnum {
        UNKNOWN,
        VEHICLE,
        CCU,
        STATICSENSOR,
        MOBILESENSOR,
        MANNED_SHIP,
        MANNED_CAR,
        MANNED_AIRPLANE,
        PERSON,
        ALL
    };
    
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
        return active || (locationTimeMillis - System.currentTimeMillis() < 10000);
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

    /**
     * Only is override if locationTimeMillis is newer than already there.
     * 
     * @param location
     * @param locationTimeMillis
     */
    public boolean setLocation(LocationType location, long locationTimeMillis) {
        if (locationTimeMillis < getLocationTimeMillis())
            return false;
        this.location.setLocation(location);
        this.location.convertToAbsoluteLatLonDepth();
        setLocationTimeMillis(locationTimeMillis);
        return true;
    }

    public void setAttitudeDegrees(double rollDegrees, double pitchDegrees, double yawDegrees) {
        location.setRoll(rollDegrees);
        location.setPitch(pitchDegrees);
        location.setYaw(yawDegrees);
        setAttitudeTimeMillis(System.currentTimeMillis());
    }

    /**
     * Only is override if attitudeTimeMillis is newer than already there.
     * 
     * @param rollDegrees
     * @param pitchDegrees
     * @param yawDegrees
     * @param locationTimeMillis
     */
    public boolean setAttitudeDegrees(double rollDegrees, double pitchDegrees, double yawDegrees, long attitudeTimeMillis) {
        if (attitudeTimeMillis < getAttitudeTimeMillis())
            return false;
        location.setRoll(rollDegrees);
        location.setPitch(pitchDegrees);
        location.setYaw(yawDegrees);
        setAttitudeTimeMillis(attitudeTimeMillis);
        return true;
    }

    public void setAttitudeDegrees(double yawDegrees) {
        location.setRoll(0);
        location.setPitch(0);
        location.setYaw(yawDegrees);
        setAttitudeTimeMillis(System.currentTimeMillis());
    }

    /**
     * Only is override if attitudeTimeMillis is newer than already there.
     * 
     * @param yawDegrees
     * @param locationTimeMillis
     */
    public boolean setAttitudeDegrees(double yawDegrees, long attitudeTimeMillis) {
        if (attitudeTimeMillis < getAttitudeTimeMillis())
            return false;
        location.setRoll(0);
        location.setPitch(0);
        location.setYaw(yawDegrees);
        setAttitudeTimeMillis(attitudeTimeMillis);
        return true;
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
