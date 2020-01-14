/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
import pt.lsts.neptus.mp.maneuvers.YoYo;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zps
 * 
 */
public class YoYoPreview implements IManeuverPreview<YoYo> {

    protected LocationType destination;
    protected double speed;
    protected boolean finished = false;
    double amplitude = 0;
    boolean descending = true;
    boolean altitude = false;
    double maxZ, minZ;

    UnicycleModel model = new UnicycleModel();

    @Override
    public boolean init(String vehicleId, YoYo man, SystemPositionAndAttitude state, Object manState) {
        destination = new LocationType(man.getManeuverLocation());
        if (man.getManeuverLocation().getZUnits() == ManeuverLocation.Z_UNITS.DEPTH) {
            destination.setDepth(man.getManeuverLocation().getZ());
            altitude = false;
            amplitude = man.getAmplitude();
            minZ = man.getManeuverLocation().getZ() - amplitude;
            maxZ = man.getManeuverLocation().getZ() + amplitude;
        }
        else {
            destination.setDepth(Math.max(0.5, 10 - man.getManeuverLocation().getZ()));
            altitude = true;
            minZ = man.getManeuverLocation().getZ() + amplitude;
            maxZ = man.getManeuverLocation().getZ() - amplitude;
        }

        speed = man.getSpeed().getMPS();
        speed = Math.min(speed, SpeedConversion.MAX_SPEED);

        amplitude = man.getAmplitude();

        model.setState(state);
        return true;
    }

    @Override
    public SystemPositionAndAttitude step(SystemPositionAndAttitude state, double timestep, double ellapsedTime) {
        model.setState(state);
        boolean arrivedZ = false, arrivedXY = false;

        if (descending) {
            LocationType dest = new LocationType(destination);
            dest.translatePosition(0, 0, amplitude);
            dest.convertToAbsoluteLatLonDepth();
            arrivedXY = model.guide(dest, speed, altitude ? maxZ : null);

            if (!altitude) {
                arrivedZ = Math.abs(model.getCurrentPosition().getDepth() - maxZ) < 0.5;
            }
            else {
                arrivedZ = Math.abs(model.getCurrentAltitude() - maxZ) < 0.5;
            }
        }
        else {
            LocationType dest = new LocationType(destination);
            dest.translatePosition(0, 0, -amplitude);
            dest.convertToAbsoluteLatLonDepth();
            arrivedXY = model.guide(dest, speed, altitude ? minZ : null);
            
            if (!altitude) {
                arrivedZ = Math.abs(model.getCurrentPosition().getDepth() - minZ) < 0.25;
            }
            else {
                arrivedZ = Math.abs(model.getCurrentAltitude() - minZ) < 0.25;
            }
        }
        if (arrivedXY)
            finished = true;
        else if (arrivedZ)
            descending = !descending;
        
        if (!finished)
            model.advance(timestep);

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
        return null;
    }
}
