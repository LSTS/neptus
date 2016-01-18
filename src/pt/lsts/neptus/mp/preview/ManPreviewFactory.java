/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Oct 11, 2011
 */
package pt.lsts.neptus.mp.preview;

import java.util.LinkedHashMap;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.maneuvers.Elevator;
import pt.lsts.neptus.mp.maneuvers.FollowTrajectory;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.Loiter;
import pt.lsts.neptus.mp.maneuvers.PopUp;
import pt.lsts.neptus.mp.maneuvers.RowsManeuver;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;
import pt.lsts.neptus.mp.maneuvers.YoYo;

/**
 * @author zp
 *
 */
public class ManPreviewFactory {

    protected static LinkedHashMap<Class<Maneuver>, Class<IManeuverPreview<Maneuver>>> registeredPreviews = new LinkedHashMap<Class<Maneuver>, Class<IManeuverPreview<Maneuver>>>();

    public static IManeuverPreview<?> getPreview(Maneuver maneuver, String vehicleId, SystemPositionAndAttitude state, Object manState) {
        if (maneuver == null)
            return null;
        
        if (maneuver.getClass() == Goto.class) {
            GotoPreview prev = new GotoPreview();
            prev.init(vehicleId, (Goto)maneuver, state, manState);
            return prev;
        }
        else if (maneuver.getClass() == PopUp.class) {
            PopupPreview prev = new PopupPreview();
            prev.init(vehicleId, (PopUp)maneuver, state, manState);
            return prev;
        }
        else if (maneuver.getClass() == StationKeeping.class) {
            StationKeepingPreview prev = new StationKeepingPreview();
            prev.init(vehicleId, (StationKeeping)maneuver, state, manState);
            return prev;
        }
        else if (maneuver.getClass() == Loiter.class) {
            LoiterPreview prev = new LoiterPreview();
            prev.init(vehicleId, (Loiter)maneuver, state, manState);
            return prev;
        }
        else if (maneuver.getClass() == RowsManeuver.class) {
            RowsManeuverPreview prev = new RowsManeuverPreview();
            prev.init(vehicleId, (RowsManeuver)maneuver, state, manState);
            return prev;
        }
        else if (maneuver.getClass() == Elevator.class) {
            ElevatorPreview prev = new ElevatorPreview();
            prev.init(vehicleId, (Elevator)maneuver, state, manState);
            return prev;
        }
        else if (FollowTrajectory.class.isAssignableFrom(maneuver.getClass())) {
            FollowTrajectoryPreview prev = new FollowTrajectoryPreview();
            prev.init(vehicleId, (FollowTrajectory)maneuver, state, manState);
            return prev;
        }
        else if (YoYo.class.isAssignableFrom(maneuver.getClass())) {
            YoYoPreview prev = new YoYoPreview();
            prev.init(vehicleId, (YoYo)maneuver, state, manState);
            return prev;
        }
        
        
        return null;
    }    
}


