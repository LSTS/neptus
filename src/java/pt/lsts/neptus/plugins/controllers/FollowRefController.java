/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Oct 6, 2013
 */
package pt.lsts.neptus.plugins.controllers;

import pt.lsts.imc.DesiredZ;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FollowRefState;
import pt.lsts.imc.Reference;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;

/**
 * @author zp
 *
 */
public abstract class FollowRefController implements IController {

   protected VehicleType controlledVehicle = null; 
   protected LocationType destination = null;
      
    public abstract LocationType getDestination(LocationType curLoc, double curDepth, double curAlt);
   
    @Override
    public Reference guide(VehicleType vehicle, EstimatedState estate, FollowRefState frefState) {
        LocationType loc = IMCUtils.parseLocation(estate);
        double depth = estate.getDepth();
        double alt = estate.getAlt();
        LocationType dest = getDestination(loc, depth, alt);
        Reference ref = new Reference();
        if (dest == null) // completed
            return null;
        
        dest.convertToAbsoluteLatLonDepth();
        ref.setLat(dest.getLatitudeRads());
        ref.setLon(dest.getLongitudeRads());
        DesiredZ z;
        if (dest.getDepth() >= 0)
            z = new DesiredZ((float)dest.getDepth(), ZUnits.DEPTH);
        else
            z = new DesiredZ((float)-dest.getDepth(), ZUnits.ALTITUDE);
        
        ref.setZ(z);        
        ref.setFlags((short)(Reference.FLAG_Z | Reference.FLAG_LOCATION));
        
        return ref;
    }
    
    @Override
    public String getControllerName() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean supportsVehicle(VehicleType vehicle, EstimatedState state) {
        return vehicle.getFeasibleManeuvers().containsKey("FollowReference");
    }

    @Override
    public void startControlling(VehicleType vehicle, EstimatedState state) {
        this.controlledVehicle = vehicle;
    }

    @Override
    public void vehicleTimedOut(VehicleType vehicle) {
        // nothing        
    }

    @Override
    public void stopControlling(VehicleType vehicle) {
        // nothing
    }

}
