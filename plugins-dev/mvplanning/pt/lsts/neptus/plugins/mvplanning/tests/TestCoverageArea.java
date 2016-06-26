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
 * 18 Apr 2016
 */
package pt.lsts.neptus.plugins.mvplanning.tests;

import pt.lsts.neptus.plugins.mvplanning.jaxb.ProfileMarshaler;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Profile;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author tsmarques
 *
 */
public class TestCoverageArea {
    public static void testCoverageFromGrid(Profile planProfile, GridArea areaToCover) {
        System.out.println("--- Testing coverage area from a grid ---");

//        CoverageAreaFactory covArea = new CoverageAreaFactory("bla bla", planProfile, areaToCover, new MissionType());
//        GraphType planGraph = covArea.asGraphType();
//        FollowPath covManeuver = covArea.asFollowPathManeuver();
//
//        int nNodes = planGraph.getAllManeuvers().length;
//        int nEdges = planGraph.getAllEdges().length;
//        double manLat = covManeuver.getManeuverLocation().getAbsoluteLatLonDepth()[0];
//        double manLon = covManeuver.getManeuverLocation().getAbsoluteLatLonDepth()[1];
//        double speed = covManeuver.getSpeed();
//        double z = covManeuver.getManeuverLocation().getZ();
//        String zUnits = covManeuver.getManeuverLocation().getZUnits().toString();
//
//        System.out.println("* Maneuver's graph has " + nNodes + " nodes and " + nEdges + " edges");
//        System.out.println("* Start maneuver is " + planGraph.getInitialManeuverId());
//        System.out.println("* End maneuver is " + planGraph.getLastManeuver().getId());
//        System.out.println("* Maneuver's location is " + manLat + " " + manLon);
//        System.out.println("* Maneuver's speed is " + speed);
//        System.out.println("* Maneuver's Z is " + z + " " + zUnits);
//        System.out.println();
//        System.out.println("* Displaying transitions:");
//
//        if(nEdges == 0)
//            System.out.println("** There are no edges");
//        else
//            for(TransitionType edge : planGraph.getAllEdges())
//                System.out.println("** " + "(" + edge.getSourceManeuver() + ")" + " -> " + "(" + edge.getTargetManeuver() + ")");
    }

    public static void main(String[] args) {
        ProfileMarshaler pMarsh = new ProfileMarshaler();
        Profile planProfile = pMarsh.getAllProfiles().get("Batimetria");

        System.out.println("--- Test 1 ---");
        GridArea grid1 = new GridArea(60, 100, 100, 0, LocationType.FEUP);

        TestCoverageArea.testCoverageFromGrid(planProfile, grid1);

        System.out.println();

        System.out.println("--- Test 2 ---");
        GridArea grid2 = new GridArea(60, 500, 500, 0, LocationType.FEUP);

        TestCoverageArea.testCoverageFromGrid(planProfile, grid2);
    }
}
