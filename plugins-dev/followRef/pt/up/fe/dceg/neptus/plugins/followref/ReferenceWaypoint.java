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
 * Jun 5, 2013
 */
package pt.up.fe.dceg.neptus.plugins.followref;

import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.imc.DesiredSpeed;
import pt.lsts.imc.DesiredSpeed.SPEED_UNITS;
import pt.lsts.imc.DesiredZ;
import pt.lsts.imc.DesiredZ.Z_UNITS;
import pt.lsts.imc.Reference;

/**
 * @author zp
 *
 */
public class ReferenceWaypoint {

    private Reference reference;
    private ManeuverLocation loc;
    
    public ReferenceWaypoint(ManeuverLocation loc, double speed) {
        loc.convertToAbsoluteLatLonDepth();
        this.loc = loc.clone();
        reference = new Reference();        
        reference.setLat(loc.getLatitudeAsDoubleValueRads());
        reference.setLon(loc.getLongitudeAsDoubleValueRads());
        reference.setZ(new DesiredZ((float)loc.getZ(), Z_UNITS.valueOf(loc.getZUnits().name())));
        reference.setSpeed(new DesiredSpeed(speed, SPEED_UNITS.METERS_PS));
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
        loc.setZ(ref.getZ().getValue());
        loc.setZUnits(ManeuverLocation.Z_UNITS.valueOf(ref.getZ().getZUnits().name()));        
    }

    public void setHorizontalLocation(LocationType newLoc) {
        newLoc.convertToAbsoluteLatLonDepth();
        loc.setLatitude(newLoc.getLatitudeAsDoubleValue());
        loc.setLongitude(newLoc.getLongitudeAsDoubleValue());
        reference.setLat(newLoc.getLatitudeAsDoubleValueRads());
        reference.setLon(newLoc.getLongitudeAsDoubleValueRads());
    }
    
    public void setZ(DesiredZ desiredZ) {        
        if (desiredZ == null) {
            reference.setFlags((short)(reference.getFlags() ^ Reference.FLAG_Z));
            reference.setZ(null);
            loc.setZUnits(ManeuverLocation.Z_UNITS.NONE);
        }
        else {
            reference.setZ(new DesiredZ((float)desiredZ.getValue(), desiredZ.getZUnits()));
            reference.setFlags((short)(reference.getFlags() | Reference.FLAG_Z));
            loc.setZ(desiredZ.getValue());
            loc.setZUnits(ManeuverLocation.Z_UNITS.valueOf(desiredZ.getZUnits().name()));
        }
    }
    
    public void setSpeed(DesiredSpeed speed) {
        if (speed == null) {
            reference.setFlags((short)(reference.getFlags() ^ Reference.FLAG_SPEED));
            reference.setSpeed(null);            
        }
        else {
            reference.setFlags((short)(reference.getFlags() | Reference.FLAG_SPEED));
            reference.setSpeed(new DesiredSpeed(speed.getValue(), speed.getSpeedUnits()));
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
