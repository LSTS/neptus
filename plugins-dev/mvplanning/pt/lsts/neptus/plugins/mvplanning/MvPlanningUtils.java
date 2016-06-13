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
 * Author: tsmarques
 * 8 Jun 2016
 */
package pt.lsts.neptus.plugins.mvplanning;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.FollowPath;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Profile;
import pt.lsts.neptus.plugins.mvplanning.planning.PlanTask;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author tsmarques
 *
 */
public class MvPlanningUtils {

    /**
     * Builds a new maneuver for a task/plan of type taskType
     * according to the given profile
     * */
    public static Maneuver buildManeuver(Profile planProfile, LocationType manLoc, PlanTask.TASK_TYPE taskType) {
        Maneuver newMan;
        ManeuverLocation loc = new ManeuverLocation(manLoc);

        loc.setZ(planProfile.getProfileZ());
        /* TODO set according to profile's parameters */
        loc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);

        if(taskType == PlanTask.TASK_TYPE.COVERAGE_AREA) {
            newMan = new FollowPath();

            ((FollowPath) newMan).setManeuverLocation(loc);
            ((FollowPath) newMan).setSpeed(planProfile.getProfileSpeed());

            /* TODO set according to profile's parameters */
            ((FollowPath) newMan).setSpeedUnits(ManeuverLocation.Z_UNITS.DEPTH.toString());
        }
        else {
            newMan = new Goto();

            ((Goto) newMan).setManeuverLocation(loc);
            ((Goto) newMan).setSpeed(planProfile.getProfileSpeed());

            /* TODO set according to profile's parameters */
            ((Goto) newMan).setSpeedUnits("m/s");
        }

        return newMan;
    }

    /**
     * Setup and add the payloads defined in the profile, to the given
     * maneuver
     * */
    private static void setupPayloads(Profile planProfile, Maneuver man) {

    }
}
