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
 * Author: pdias
 * 08/06/2016
 */
package pt.lsts.neptus.mp.preview;

import java.util.Vector;

import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.maneuvers.RowsCoverage;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author pdias
 *
 */
public class RowsCoveragePreview implements IManeuverPreview<RowsCoverage> {

    public static class RowsCoverageState {
        public int locIndex = 0;
        public double altMin;
        public double covPred;
        public double covActualMin;
        public double curHstep;
        
        /* (non-Javadoc)
         * @see java.lang.Object#clone()
         */
        @Override
        public RowsCoverageState clone() {
            RowsCoverageState clone = new RowsCoverageState();
            clone.locIndex = locIndex;
            clone.altMin = altMin;
            clone.covPred = covPred;
            clone.covActualMin = covActualMin;
            clone.curHstep = curHstep;
            return clone;
        }
    }

    protected RowsCoverageState rowsState = new RowsCoverageState();
    protected Vector<LocationType> locs = new Vector<>();
    protected String vehicleId = null;
    protected double speed;
    protected UnicycleModel model = new UnicycleModel();
    protected boolean finished = false;
    protected RowsCoverage man = null;
    
    
    @Override
    public boolean init(String vehicleId, RowsCoverage man, SystemPositionAndAttitude state, Object manState) {
        this.man = man;
        
        if (manState == null || !(manState instanceof RowsCoverageState)) {
            rowsState = new RowsCoverageState();
            double hstep;
            if (man.getAngleApertureDegs() <= 0)
                hstep = 2 * man.getRange();
            else
                hstep = 2 * man.getRange() * Math.sin(Math.toRadians(man.getAngleApertureDegs() / 2));
            
            rowsState.altMin = -1;
            rowsState.covPred = hstep * (1 - man.getOverlapPercent() / 200.);
            rowsState.covActualMin = rowsState.covPred;
            rowsState.curHstep = rowsState.covPred;
            rowsState.locIndex = 0;
        }
        else {
            rowsState = (RowsCoverageState) manState;
        }
        
        locs.addAll(man.getPathLocations());
        for (LocationType loc : locs) {
            if (man.getManeuverLocation().getZUnits() == Z_UNITS.DEPTH)
                loc.setDepth(man.getManeuverLocation().getZ());
            else if (man.getManeuverLocation().getZUnits() == Z_UNITS.ALTITUDE)
                loc.setDepth(-man.getManeuverLocation().getZ());
        }        

        this.vehicleId = vehicleId;
        speed = man.getSpeed().getMPS();
        speed = Math.min(speed, SpeedConversion.MAX_SPEED);    
        
        model.setState(state);

        return true;
    }
    
    @Override
    public boolean isFinished() {
        return finished;
    }
    
    public void reset(SystemPositionAndAttitude state) {
        //not supported for this maneuver
    };

    @Override
    public SystemPositionAndAttitude step(SystemPositionAndAttitude state, double timestep, double ellapsedTime) {
        if (rowsState.locIndex >= locs.size()) {
            finished = true;
            return state;
        }
        model.setState(state);
        LocationType destination = locs.get(rowsState.locIndex);
        
        if (model.guide(destination, speed, destination.getDepth() >= 0 ? null : - destination.getDepth())) {
            rowsState.locIndex++;
        }
        else
            model.advance(timestep);            
        
        return model.getState();
    }
    
    @Override
    public Object getState() {
        return rowsState.clone();
    }
}
