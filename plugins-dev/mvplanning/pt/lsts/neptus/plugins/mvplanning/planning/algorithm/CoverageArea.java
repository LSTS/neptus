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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.FollowPath;
import pt.lsts.neptus.plugins.mvplanning.MvPlanningUtils;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapDecomposition;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Profile;
import pt.lsts.neptus.plugins.mvplanning.planning.PlanTask;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;
import pt.lsts.neptus.types.mission.GraphType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author tsmarques
 *
 */
public class CoverageArea {

    private Profile planProfile;
    private List<PlanType> plans;
    private GraphType planGraph;
    private List<ManeuverLocation> path;

    public CoverageArea(String id, Profile planProfile, MapDecomposition areaToCover, MissionType mt) {
        this.planProfile = planProfile;

        path = getPath(areaToCover);
        plans = getPlan(mt, path, id);
    }


    private List<ManeuverLocation> getPath(MapDecomposition areaToCover) {
        if(areaToCover.getClass().getSimpleName().toString().equals("GridArea"))
            return new SpiralSTC((GridArea) areaToCover).getPath();

        /* TODO implement for other types of decompositions */
        return null;
    }

    /**
     * Generates a PlanType for a coverage area plan
     * */
    private List<PlanType> getPlan(MissionType mt, List<ManeuverLocation> path, String id) {
        /* returns an empty plan type */
        if(path.isEmpty())
            return new ArrayList<>(0);

        List<FollowPath> fpaths = asFollowPathManeuver(path);
        List<PlanType> plans = new ArrayList<>(fpaths.size());

        int i = 0;
        for(FollowPath followPath : fpaths) {
            PlanType ptype = new PlanType(mt);
            ptype.getGraph().addManeuver(followPath);
            ptype.setId(id + "_" + i);
            plans.add(ptype);
            i++;
        }

        return plans;
    }

    /**
     * Generates a GraphType for a coverage area plan
     * */
    public GraphType asGraphType() {
        return planGraph;
    }

    public List<FollowPath> asFollowPathManeuver() {
        return asFollowPathManeuver(this.path);
    }

    private List<FollowPath> asFollowPathManeuver(List<ManeuverLocation> path) {
        List<FollowPath> fpaths = new ArrayList<>();
        ManeuverLocation loc = path.get(0);
        FollowPath fpath = (FollowPath) MvPlanningUtils.buildManeuver(planProfile, loc, PlanTask.TASK_TYPE.COVERAGE_AREA);

        Vector<double[]> offsets = new Vector<>();
        for(ManeuverLocation point : path) {
            double[] newPoint = getNewPoint(loc, point);
            offsets.add(newPoint);
        }

        fpath.setOffsets(offsets);

        /* if completion time is bigger than 30 minutes, split the path */
        if(fpath.getCompletionTime(loc) >= 1800) {
            fpaths = splitPath(fpath, path);
            NeptusLog.pub().debug("Splitting the coverage area plan");
        }
        else
            fpaths.add(fpath);

        return fpaths;
    }

    private List<FollowPath> splitPath(FollowPath path, List<ManeuverLocation> points) {
        List<FollowPath> paths = new ArrayList<>();

        FollowPath fpath = null;
        Vector<double[]> offsets = null;
        ManeuverLocation initialLoc = null;
        boolean newManeuver = true;

        for(ManeuverLocation point : points) {
            if(newManeuver) {
                offsets = new Vector<>();
                initialLoc = point;
                fpath = (FollowPath) MvPlanningUtils.buildManeuver(planProfile, initialLoc, PlanTask.TASK_TYPE.COVERAGE_AREA);

                newManeuver = false;
            }

            double[] newPoint = getNewPoint(initialLoc, point);
            offsets.add(newPoint);
            fpath.setOffsets(offsets);

            /* split in plans of 15 minutes (maximum) each */
            if(fpath.getCompletionTime(initialLoc) >= 900) {
                newManeuver = true;
                paths.add(fpath);
            }
        }

        paths.add(fpath);
        return paths;
    }

    private double[] getNewPoint(ManeuverLocation initialLoc, ManeuverLocation currentLoc) {
        double[] newPoint = new double[4];
        double[] pOffsets = currentLoc.getOffsetFrom(initialLoc);

        newPoint[0] = pOffsets[0];
        newPoint[1] = pOffsets[1];
        newPoint[2] = pOffsets[2];

        return newPoint;
    }

    public List<PlanType> asPlanType() {
        return plans;
    }

    /**
     * Returns a list of PlanSpecification's of the plan.
     * If the plan is empty returns and empty list
     * */
    public List<PlanSpecification> asPlanSpecification() {
        if(plans.isEmpty())
            return new ArrayList<>(0);

        List<PlanSpecification> pSpecs = new ArrayList<>(plans.size());
        for(PlanType ptype : plans) {
            PlanSpecification planSpec = (PlanSpecification) ptype.asIMCPlan();
            planSpec.setValue("description", "Coverage plan automatically generated by MVPlanning");

            pSpecs.add(planSpec);
        }

        return pSpecs;
    }
}
