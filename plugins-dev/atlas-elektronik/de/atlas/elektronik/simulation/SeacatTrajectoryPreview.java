/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * 05/05/2017
 */
package de.atlas.elektronik.simulation;

import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.maneuvers.FollowTrajectory;
import pt.lsts.neptus.mp.preview.FollowTrajectoryPreview;
import pt.lsts.neptus.mp.preview.controller.EightLoopController;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public class SeacatTrajectoryPreview extends FollowTrajectoryPreview {

    boolean arrived = false;
    private static final double EIGHT_DIST = 10;

    private EightLoopController eightCtrl = null;
    private LocationType targetPoint = null;
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.preview.GotoPreview#init(java.lang.String, pt.lsts.neptus.mp.maneuvers.Goto, pt.lsts.neptus.mp.SystemPositionAndAttitude, java.lang.Object)
     */
    @Override
    public boolean init(String vehicleId, FollowTrajectory man, SystemPositionAndAttitude state, Object manState) {
        boolean ret = super.init(vehicleId, man, state, manState);
        targetPoint = new LocationType(locs.firstElement());
        return ret;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mp.preview.GotoPreview#reset(pt.lsts.neptus.mp.SystemPositionAndAttitude)
     */
    @Override
    public void reset(SystemPositionAndAttitude state) {
        super.reset(state);
        eightCtrl = null;
    }

    @Override
    public SystemPositionAndAttitude step(SystemPositionAndAttitude state, double timestep, double ellapsedTime) {

        // first point?
        if (locIndex == 0) {
            LocationType destination = locs.firstElement();
            model.setState(state);
            if (!arrived)
                arrived = model.guide(destination, speed, destination.getDepth() >= 0 ? null : -destination.getDepth());

            double zDistance = Math.abs(destination.getDepth() - state.getDepth());
            if (destination.getDepth() < 0)
                zDistance = Math.abs(-destination.getDepth() - state.getAltitude());
            
            if (arrived && (zDistance > 0.1 || eightCtrl != null && (targetPoint.getHorizontalDistanceInMeters(state.getPosition()) >= speed))) {
                double angle = state.getYaw();
                if (eightCtrl == null) {
                    eightCtrl = new EightLoopController(destination.getLatitudeRads(), destination.getLongitudeRads(), angle + Math.PI / 2.0,
                            EIGHT_DIST * 2, 15, destination.getDepth(), true, speed);
                }
                LocationType tmp = eightCtrl.step(model, state, timestep, ellapsedTime);
                System.out.println("tmpDest= " + tmp);
                model.guide(tmp, speed, destination.getDepth() >= 0 ? null : -destination.getDepth());
            }
            if (arrived && zDistance < 0.1 && (eightCtrl == null
                    || eightCtrl != null && (targetPoint.getHorizontalDistanceInMeters(state.getPosition()) < speed)))
                locIndex++;
            model.advance(timestep);
            return model.getState();
        }
        else
            return super.step(state, timestep, ellapsedTime);
    }
}
