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

import pt.up.fe.dceg.neptus.imc.DesiredSpeed;
import pt.up.fe.dceg.neptus.imc.DesiredSpeed.SPEED_UNITS;
import pt.up.fe.dceg.neptus.imc.DesiredZ;
import pt.up.fe.dceg.neptus.imc.DesiredZ.Z_UNITS;
import pt.up.fe.dceg.neptus.imc.Reference;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

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
        this.reference = ref;
        loc = new ManeuverLocation();
        loc.setLatitudeRads(ref.getLat());
        loc.setLongitudeRads(ref.getLon());
        loc.setZ(ref.getZ().getValue());
        loc.setZUnits(pt.up.fe.dceg.neptus.mp.ManeuverLocation.Z_UNITS.valueOf(ref.getZ().getZUnits().name()));        
    }

    public void setHorizontalLocation(LocationType newLoc) {
        newLoc.convertToAbsoluteLatLonDepth();
        loc.setLatitude(newLoc.getLatitudeAsDoubleValue());
        loc.setLongitude(newLoc.getLongitudeAsDoubleValue());
        reference.setLat(newLoc.getLatitudeAsDoubleValueRads());
        reference.setLon(newLoc.getLongitudeAsDoubleValueRads());
    }
    
    public void setZ(Z_UNITS units, double value) {
        loc.setZ(value);
        loc.setZUnits(pt.up.fe.dceg.neptus.mp.ManeuverLocation.Z_UNITS.valueOf(units.name()));        
        reference.setZ(new DesiredZ((float)value, units));
    }
    
    public void setSpeed(double value) {
        reference.setSpeed(new DesiredSpeed(value, SPEED_UNITS.METERS_PS));
    }
    
    public final Reference getReference() {
        return reference;
    }
    
    public final ManeuverLocation getManeuverLocation() {
        return loc;
    }
    
}
