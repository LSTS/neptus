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
 * Author: zp
 * Jun 5, 2013
 */
package pt.lsts.neptus.plugins.followref;

import pt.lsts.imc.DesiredSpeed;
import pt.lsts.imc.DesiredZ;
import pt.lsts.imc.Reference;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class ReferenceWaypoint implements ConfigurationListener {

    private Reference reference;
    
    private ManeuverLocation loc;
    
    protected double latitude, longitude;
    
    //@NeptusProperty(name="Specify Z", category="Z")
    //private boolean defineZ = true;

    @NeptusProperty(name="Z Reference", category="Z")
    private pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS zUnits = ManeuverLocation.Z_UNITS.DEPTH;
    
    @NeptusProperty(name="Z value", category="Z")
    private double z = 0;
    
    @NeptusProperty(name="Specify speed", category="Speed")
    private boolean defineSpeed = true;

    @NeptusProperty(name="Speed units", category="Speed")
    private SpeedUnits speedUnits = SpeedUnits.METERS_PS;

    @NeptusProperty(name="Speed value", category="Speed")
    private double speed = 1.3;
    
    @NeptusProperty(name="Loiter radius (m)", category="Loiter")
    public double loiterRadius = 15;
    
    @NeptusProperty(name="Loiter", category="Loiter")
    public boolean loiter = false;
    
    @NeptusProperty(name="Time (seconds)", description="Time to stay at this location. -1 for infinity.", category="Time")
    public double time = 0;
    
    private double startTime = Double.NaN;
    
    @Override
    public void propertiesChanged() {
        loc.setZ(z);
        loc.setZUnits(zUnits);
     
        reference = new Reference();
        if (loiter)
            reference.setRadius(loiterRadius);
        
        reference.setLat(loc.getLatitudeRads());
        reference.setLon(loc.getLongitudeRads());
        if (loc.getZUnits() != ManeuverLocation.Z_UNITS.NONE)
            reference.setZ(new DesiredZ((float)loc.getZ(), ZUnits.valueOf(loc.getZUnits().name())));
        if (defineSpeed)
            reference.setSpeed(new DesiredSpeed(speed, speedUnits));
        reference.setFlags((short)(Reference.FLAG_LOCATION | 
                (defineSpeed?   Reference.FLAG_SPEED : 0) | 
                (loc.getZUnits() != ManeuverLocation.Z_UNITS.NONE?       Reference.FLAG_Z : 0) |
                (loiter?       Reference.FLAG_RADIUS : 0)));
    }
    
    public double timeLeft() {
        if (time == 0 || Double.isNaN(startTime))
            return Double.NaN;
        return time - (System.currentTimeMillis() / 1000.0 - startTime);
    }
    
    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }
    
    public ReferenceWaypoint(ManeuverLocation loc, double speed) {
        loc.convertToAbsoluteLatLonDepth();
        this.loc = loc.clone();
        this.latitude = loc.getLatitudeDegs();
        this.longitude = loc.getLongitudeDegs();
        this.speed = speed;
        this.speedUnits = SpeedUnits.METERS_PS;
        this.z = loc.getZ();
        this.zUnits = loc.getZUnits();
        
        reference = new Reference();        
        reference.setLat(loc.getLatitudeRads());
        reference.setLon(loc.getLongitudeRads());
        reference.setZ(new DesiredZ((float)loc.getZ(), ZUnits.valueOf(loc.getZUnits().name())));
        reference.setSpeed(new DesiredSpeed(speed, SpeedUnits.METERS_PS));
        reference.setFlags((short)(Reference.FLAG_LOCATION | Reference.FLAG_SPEED | Reference.FLAG_Z));
    }
    
    public ReferenceWaypoint(Reference ref) {
        try {
            this.reference = Reference.clone(ref);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        loc = new ManeuverLocation();
        loc.setLatitudeRads(ref.getLat());
        loc.setLongitudeRads(ref.getLon());
        if (ref.getZ() != null) {
            loc.setZ(ref.getZ().getValue());
            loc.setZUnits(ManeuverLocation.Z_UNITS.valueOf(ref.getZ().getZUnits().name()));
        }    
        boolean defineZ = (ref.getFlags() & Reference.FLAG_Z) != 0;
        if (!defineZ)
            loc.setZUnits(ManeuverLocation.Z_UNITS.NONE);
        
        loiter = (ref.getFlags() & Reference.FLAG_RADIUS) != 0;
        this.loiterRadius = ref.getRadius();
        
        this.latitude = loc.getLatitudeDegs();
        this.longitude = loc.getLongitudeDegs();
        this.z = loc.getZ();
        this.zUnits = loc.getZUnits();
        if (ref.getSpeed() != null) {
            this.speedUnits = ref.getSpeed().getSpeedUnits();
            this.speed = ref.getSpeed().getValue();
        }
        defineSpeed = (ref.getFlags() & Reference.FLAG_SPEED) != 0;
        
    }

    public void setHorizontalLocation(LocationType newLoc) {
        newLoc.convertToAbsoluteLatLonDepth();
        loc.setLatitudeDegs(newLoc.getLatitudeDegs());
        loc.setLongitudeDegs(newLoc.getLongitudeDegs());
        reference.setLat(newLoc.getLatitudeRads());
        reference.setLon(newLoc.getLongitudeRads());
        latitude = newLoc.getLatitudeDegs();
        longitude = newLoc.getLongitudeDegs();
    }
    
    public void setZ(DesiredZ desiredZ) {        
        if (desiredZ == null) {
            reference.setFlags((short)(reference.getFlags() ^ Reference.FLAG_Z));
            reference.setZ(null);
            loc.setZUnits(ManeuverLocation.Z_UNITS.NONE);
            zUnits = ManeuverLocation.Z_UNITS.NONE;
        }
        else {
            reference.setZ(new DesiredZ((float)desiredZ.getValue(), desiredZ.getZUnits()));
            reference.setFlags((short)(reference.getFlags() | Reference.FLAG_Z));
            loc.setZ(desiredZ.getValue());
            loc.setZUnits(ManeuverLocation.Z_UNITS.valueOf(desiredZ.getZUnits().name()));
            zUnits = loc.getZUnits();
            z = loc.getZ();
        }
    }
    
    public void setSpeed(DesiredSpeed speed) {
        if (speed == null) {
            reference.setFlags((short)(reference.getFlags() ^ Reference.FLAG_SPEED));
            reference.setSpeed(null);  
            defineSpeed = false;
        }
        else {
            reference.setFlags((short)(reference.getFlags() | Reference.FLAG_SPEED));
            reference.setSpeed(new DesiredSpeed(speed.getValue(), speed.getSpeedUnits()));
            defineSpeed = true;
            this.speed = speed.getValue();
            this.speedUnits = speed.getSpeedUnits();
        }
    }
    
    public void setLoiterRadius(double radius) {
        if (radius <= 0) {
            reference.setFlags((short)(reference.getFlags() ^ Reference.FLAG_RADIUS));
            reference.setRadius(radius);
        }
        else {
            reference.setFlags((short)(reference.getFlags() | Reference.FLAG_RADIUS));
            reference.setRadius(radius);
        }
    }    
    
    public final Reference getReference() {
        return reference;
    }
    
    public final ManeuverLocation getManeuverLocation() {
        return loc;
    }
    
}
