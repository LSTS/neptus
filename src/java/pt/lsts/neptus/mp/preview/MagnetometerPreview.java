/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Nov 20, 2012
 */

package pt.lsts.neptus.mp.preview;

import java.util.Vector;

import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.maneuvers.Magnetometer;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 */
public class MagnetometerPreview implements IManeuverPreview<Magnetometer> {
    protected Vector<LocationType> locs = new Vector<>();
    private int locIndex = 0;
    protected String vehicleId = null;
    protected double speed;
    protected UnicycleModel model = new UnicycleModel();
    protected boolean finished = false;

    @Override
    public boolean init(String vehicleId, Magnetometer man, SystemPositionAndAttitude state, Object manState) {
        locs.addAll(man.getPathLocations());
        for (LocationType loc : locs) {
            if (man.getManeuverLocation().getZUnits() == Z_UNITS.DEPTH)
                loc.setDepth(man.getManeuverLocation().getZ());
            else if (man.getManeuverLocation().getZUnits() == Z_UNITS.ALTITUDE)
                loc.setDepth(-man.getManeuverLocation().getZ());
        }
        this.vehicleId = vehicleId;
        this.locIndex = 0;
        speed = man.getSpeed().getMPS();

        model.setState(state);

        if (manState != null && manState instanceof Integer)
            locIndex = (Integer) manState;

        return true;
    }

    @Override
    public SystemPositionAndAttitude step(SystemPositionAndAttitude state, double time, double timestep) {
        if (locIndex >= locs.size()) {
            finished = true;
            return state;
        }
        model.setState(state);
        LocationType destination = locs.get(locIndex);

        if (model.guide(destination, speed, destination.getDepth() >= 0 ? null : -destination.getDepth()))
            locIndex++;

        return model.getState();
    }

    public void reset(SystemPositionAndAttitude state) {
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public Object getState() {
        return locIndex;
    }
}
