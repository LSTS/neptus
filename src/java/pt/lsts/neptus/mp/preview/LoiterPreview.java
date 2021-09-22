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
import pt.lsts.neptus.mp.maneuvers.Loiter;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class LoiterPreview implements IManeuverPreview<Loiter> {

    protected LocationType destination;
    protected double speed, radius, duration;
    protected boolean finished = false, clockwise = true;
    protected String loiterType;
    protected double loiterTime = 0;
    protected boolean enteredLoiter = false;
    
    UnicycleModel model = new UnicycleModel();
    @Override
    public boolean init(String vehicleId, Loiter man, SystemPositionAndAttitude state, Object manState) {
        enteredLoiter = false;
        destination = new LocationType(man.getManeuverLocation());
        if (man.getManeuverLocation().getZUnits() == ManeuverLocation.Z_UNITS.DEPTH)
            destination.setDepth(man.getManeuverLocation().getZ());
        else if (man.getManeuverLocation().getZUnits() == ManeuverLocation.Z_UNITS.ALTITUDE)
            destination.setDepth(-man.getManeuverLocation().getZ());
        else
            destination.setDepth(0);
        
        clockwise = man.getDirection().equalsIgnoreCase("Clockwise");
        radius = man.getRadius();
        loiterType = man.getLoiterType();
        duration = man.getLoiterDuration();
        speed = man.getSpeed().getMPS();

        speed = Math.min(speed, SpeedConversion.MAX_SPEED);

        if (manState != null && manState instanceof Double) {
            loiterTime = (Double) manState;
        }
        model.setState(state);        
        return true;
    }

    
    
    @Override
    public SystemPositionAndAttitude step(SystemPositionAndAttitude state, double timestep, double ellapsedTime) {
        model.setState(state);
        double distToDestination = state.getPosition().getDistanceInMeters(destination);
       if (loiterType.equalsIgnoreCase("circular")) {

            if (distToDestination-2 > radius) {
                model.guide(destination, speed, destination.getDepth() >= 0 ? null : - destination.getDepth());
                if (!enteredLoiter)
                    loiterTime = 0;
            }
            else {
                enteredLoiter = true;
                double perpend = state.getPosition().getXYAngle(destination) + Math.PI/2;
                LocationType loc = new LocationType(state.getPosition());
                loc.setDepth(destination.getDepth());
                if (clockwise)
                    loc.translatePosition(Math.cos(perpend) * -20, Math.sin(perpend) * -20, 0);
                else
                    loc.translatePosition(Math.cos(perpend) * 20, Math.sin(perpend) * 20, 0);
                model.guide(loc, speed, destination.getDepth() >= 0 ? null : - destination.getDepth());
                loiterTime += timestep;
            }            
        }
        else {
            if (distToDestination < speed * 2)
                loiterTime += timestep;            
            else
                model.guide(destination, speed, destination.getDepth() >= 0 ? null : - destination.getDepth());
        }

        model.advance(timestep);

        if (loiterTime > duration)
            finished = true;

        return model.getState();
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void reset(SystemPositionAndAttitude state) {
        model.setState(state);
    }
    
    @Override
    public Object getState() {
        return Double.valueOf(loiterTime);
    }
}
