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
 * Author: José Pinto
 * Oct 11, 2011
 */
package pt.lsts.neptus.mp.preview;

import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.maneuvers.Elevator;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class ElevatorPreview implements IManeuverPreview<Elevator> {

    protected ManeuverLocation destination;
    protected double speed, radius, startZ, endZ;
    protected boolean finished = false, clockwise = true;
    protected String loiterType;
    UnicycleModel model = new UnicycleModel();
    @Override
    public boolean init(String vehicleId, Elevator man, SystemPositionAndAttitude state, Object manState) {
        
        if (man.startFromCurrentPosition) {
            LocationType loc = new LocationType(state.getPosition());
            loc.translatePosition(0, man.getRadius()+5, 0);
            destination = new ManeuverLocation();
            destination.setLocation(loc);
            destination.setZ(man.getEndLocation().getZ());
            destination.setZUnits(man.getEndLocation().getZUnits());
            startZ = endZ = man.getEndZ();
        }
        else {
            destination = man.getEndLocation().clone();
            startZ = man.getStartZ();
            endZ = man.getEndZ();                            
        }
        
        if (destination.getZUnits() == ManeuverLocation.Z_UNITS.DEPTH)
            destination.setDepth(startZ);
        else if (destination.getZUnits() == ManeuverLocation.Z_UNITS.ALTITUDE)
            destination.setDepth(-startZ);
        else
            destination.setDepth(0);
                
        clockwise = true;
        radius = man.getRadius();
        speed = man.getSpeed().getMPS();

        speed = Math.min(speed, SpeedConversion.MAX_SPEED);

        model.setState(state);        
        return true;
    }

    @Override
    public SystemPositionAndAttitude step(SystemPositionAndAttitude state, double timestep, double ellapsedTime) {
        model.setState(state);
        double distToDestination = state.getPosition().getHorizontalDistanceInMeters(destination);
        if (distToDestination-2 > radius) {
            model.guide(destination, speed, destination.getDepth() >= 0 ? null : - destination.getDepth());
        }
        else {
            double perpend = state.getPosition().getXYAngle(destination) + Math.PI/2;
            LocationType loc = new LocationType(state.getPosition());
            loc.setDepth(endZ);
            loc.translatePosition(Math.cos(perpend) * -20, Math.sin(perpend) * -20, 0);
            model.guide(loc, speed, destination.getDepth() >= 0 ? null : - destination.getDepth());
        }

        model.advance(timestep);

        if (Math.abs(state.getPosition().getDepth()-endZ) < 0.5)
            finished = true;
        
        return model.getState();
    }
    
    @Override
    public Object getState() {
        return null;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void reset(SystemPositionAndAttitude state) {
        model.setState(state);
    }
}
