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
package pt.lsts.neptus.plugins.mvplanning.planning.algorithm;

import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.FollowPath;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapDecomposition;
import pt.lsts.neptus.plugins.mvplanning.jaxb.Profile;
import pt.lsts.neptus.plugins.mvplanning.planning.MapCell;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.GraphType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.TransitionType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author tsmarques
 *
 */
public class CoverageArea {
    private Profile planProfile;
    private PlanType plan;
    private GraphType planGraph;

    public CoverageArea(String id, Profile planProfile, MapDecomposition areaToCover, MissionType mt) {
        this.planProfile = planProfile;
        planGraph = getGraph(areaToCover);
        plan = getPlan(mt);
        setId(id);
    }

    private void setId(String id) {
        plan.setId(id);
    }

    private GraphType getGraph(MapDecomposition areaToCover) {
        if(areaToCover.getClass().getSimpleName().toString().equals("GridArea"))
            return graphFromGrid((GridArea) areaToCover);

        /* TODO implement for other types of decompositions */
        return null;
    }

    private GraphType graphFromGrid(GridArea areaToCover) {
        /* TODO Improve */
        MapCell[][] cells = areaToCover.getAllCells();
        int nrows = cells.length;
        int ncols = cells[0].length;

        /* Build graph */
        GraphType planGraph = new GraphType();
        /* Add nodes to the graph */
        for(int i = 0; i < nrows; i++)
            for(int j = 0; j < ncols; j++) {
                Goto waypoint = new Goto();
                String wpId = i + " " + j;
                waypoint.setManeuverLocation(new ManeuverLocation(cells[i][j].getLocation()));
                waypoint.setId(wpId);

                if(i == 0 && j == 0)
                    waypoint.setInitialManeuver(true);

                planGraph.addManeuver(waypoint);
            }

        /* Add graph edges */
        for(int i = 0; i < nrows; i++) {
            for(int j = 0; j < ncols; j++) {
                TransitionType edge;
                /* edge's start node */
                String wpId = i + " " + j;
                /* edges's end node */
                if(j == ncols - 1 && i != nrows -1) {
                    String neighbourId = (i+1) + " " + 0;
                    edge = new TransitionType(wpId, neighbourId);
                    planGraph.addTransition(edge);
                }
                else if(j != ncols - 1){
                    String neighbourId = i + " " + (j+1);
                    edge = new TransitionType(wpId, neighbourId);
                    planGraph.addTransition(edge);
                }
            }
        }
        return planGraph;
    }

    /**
     * Generates a PlanType for a coverage area plan
     * */
    private PlanType getPlan(MissionType mt) {
        FollowPath fpath = asFollowPathManeuver();
        PlanType ptype = new PlanType(mt);
        ptype.getGraph().addManeuver(fpath);

        return ptype;
    }

    public ManeuverLocation getManeuverLocation(Profile planProfile, LocationType lt) {
        ManeuverLocation manLoc = new ManeuverLocation(lt);
        manLoc.setZ(planProfile.getProfileZ());

        /* TODO set according to profile's parameters */
        manLoc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
        return manLoc;
    }

    /**
     * Generates a GraphType for a coverage area plan
     * */
    public GraphType asGraphType() {
        return planGraph;
    }

    public FollowPath asFollowPathManeuver() {
        FollowPath fpath = new FollowPath(planGraph);
        ManeuverLocation loc = ((LocatedManeuver) planGraph.getManeuversSequence()[0]).getManeuverLocation();

        fpath.setManeuverLocation(getManeuverLocation(planProfile, loc));
        fpath.setSpeed(planProfile.getProfileSpeed());

        /* TODO set according to profile's parameters */
        fpath.setSpeedUnits(ManeuverLocation.Z_UNITS.DEPTH.toString());

        return fpath;
    }

    public PlanType asPlanType() {
        return plan;
    }

    public PlanSpecification asPlanSpecification() {
        PlanSpecification planSpec = (PlanSpecification) plan.asIMCPlan();
        planSpec.setValue("description", "Coverage plan automatically generated by MVPlanning");

        return planSpec;
    }
}
