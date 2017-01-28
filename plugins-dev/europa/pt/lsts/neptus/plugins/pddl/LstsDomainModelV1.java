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
 * 28/01/2017
 */
package pt.lsts.neptus.plugins.pddl;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Vector;
import java.util.Map.Entry;

import pt.lsts.imc.FuelLevel;
import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;

/**
 * @author zp
 *
 */
public class LstsDomainModelV1 {

    LinkedHashMap<String, LocationType> locations = new LinkedHashMap<String, LocationType>();
    LinkedHashMap<String, Integer> vehicleBattery = new LinkedHashMap<String, Integer>();
    LinkedHashMap<String, Vector<String>> payloadNames = new LinkedHashMap<String, Vector<String>>();

    protected void init(MVProblemSpecification problem) {

        locations.clear();
        vehicleBattery.clear();
        payloadNames.clear();
        
        // calculate all positions to be given to the planner, first from vehicles
        for (VehicleType v : problem.vehicles) {

            ImcSystemState state = ImcMsgManager.getManager().getState(v);
            LocationType depot = new LocationType(problem.defaultLoc);
            try {
                depot = ImcSystemsHolder.getSystemWithName(v.getId()).getLocation();
            }
            catch (Exception e) {
            }
            ;

            locations.put(v.getNickname() + "_depot", depot);

            double fuelPercent = 100.0;
            FuelLevel fuel = state.last(FuelLevel.class);
            if (fuel != null && (System.currentTimeMillis() - fuel.getTimestampMillis()) < 600) {
                fuelPercent = state.last(FuelLevel.class).getValue();
            }
            int fuelUnits = (int) (VehicleParams.maxBattery(v) * (fuelPercent / 100.0));
            vehicleBattery.put(v.getId(), fuelUnits);
        }

        // and then tasks
        for (SurveyAreaTask task : problem.surveyTasks) {
            locations.put(task.getName() + "_entry", task.getEntryPoint());
            locations.put(task.getName() + "_exit", task.getEndPoint());
        }
        for (SamplePointTask task : problem.sampleTasks) {
            locations.put(task.getName() + "_oi", task.getLocation());
        }

        // calculate all payload names
        for (VehicleType v : problem.vehicles) {
            for (PayloadRequirement pr : VehicleParams.payloadsFor(v)) {
                if (!payloadNames.containsKey(pr.name()))
                    payloadNames.put(pr.name(), new Vector<String>());
                payloadNames.get(pr.name()).add(v.getNickname() + "_" + pr.name());
            }
        }
    }

    protected String locationNames(MVProblemSpecification problem) {
        StringBuilder sb = new StringBuilder();

        for (String loc : locations.keySet()) {
            sb.append(" " + loc);
        }
        sb.append(" - location\n  ");

        return sb.toString();
    }

    protected String vehicles(MVProblemSpecification problem) {
        StringBuilder sb = new StringBuilder();

        for (VehicleType v : problem.vehicles)
            sb.append(" " + v.getNickname());
        sb.append(" - auv\n  ");

        return sb.toString();
    }

    protected String payloadNames(MVProblemSpecification problem) {
        StringBuilder sb = new StringBuilder();

        for (String ptype : payloadNames.keySet()) {
            for (String name : payloadNames.get(ptype)) {
                sb.append(" " + name);
            }
            sb.append(" - " + ptype + "\n  ");
        }

        return sb.toString();
    }

    protected String taskNames(MVProblemSpecification problem) {
        StringBuilder sb = new StringBuilder();

        if (!problem.surveyTasks.isEmpty()) {
            sb.append(" ");
            for (SurveyAreaTask t : problem.surveyTasks) {
                sb.append(" " + t.getName() + "_area");
            }
            sb.append(" - area\n");
        }
        else {
            sb.append(" dummy_area - area\n");
        }

        if (!problem.sampleTasks.isEmpty()) {
            sb.append("  ");
            for (SamplePointTask t : problem.sampleTasks) {
                sb.append(" " + t.getName() + "_obj");
            }
            sb.append(" - oi\n");
        }
        else {
            sb.append(" dummy_obj - oi\n");
        }

        sb.append("  ");
        for (SurveyAreaTask t : problem.surveyTasks) {
            if (t.getRequiredPayloads().size() >= 1)
                for (PayloadRequirement pr : t.getRequiredPayloads())
                    sb.append(" " + t.getName() + "_" + pr.name());
        }
        for (SamplePointTask t : problem.sampleTasks) {
            if (t.getRequiredPayloads().size() >= 1)
                for (PayloadRequirement pr : t.getRequiredPayloads())
                    sb.append(" " + t.getName() + "_" + pr.name());
        }
        sb.append(" - task\n");

        return sb.toString();
    }

    protected String distances(MVProblemSpecification problem) {

        StringBuilder sb = new StringBuilder();

        // distance between all locations
        Vector<String> locNames = new Vector<String>();
        locNames.addAll(locations.keySet());
        for (int i = 0; i < locNames.size(); i++) {
            for (int j = 0; j < locNames.size(); j++) {
                if (i == j)
                    continue;

                String loc1 = locNames.get(i);
                String loc2 = locNames.get(j);
                double dist = Math.max(0.01, locations.get(loc1).getHorizontalDistanceInMeters(locations.get(loc2)));
                sb.append("  (=(distance " + loc1 + " " + loc2 + ") " + String.format(Locale.US, "%.2f", dist) + ")\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    protected String vehicleDetails(MVProblemSpecification problem) {

        StringBuilder sb = new StringBuilder();

        // details of all vehicles
        for (VehicleType v : problem.vehicles) {
            sb.append("\n;" + v.getId() + ":\n");
            double moveConsumption = VehicleParams.moveConsumption(v) * MVProblemSpecification.powerUnitMultiplier
                    / 3600.0;
            sb.append("  (=(speed " + v.getNickname() + ") " + MVProblemSpecification.constantSpeed + ")\n");
            sb.append("  (= (battery-consumption-move " + v.getNickname() + ") "
                    + String.format(Locale.US, "%.2f", moveConsumption) + ")\n");
            sb.append("  (= (battery-level " + v.getNickname() + ") "
                    + vehicleBattery.get(v.getId()) * MVProblemSpecification.powerUnitMultiplier + ")\n");
            sb.append("  (base " + v.getNickname() + " " + v.getNickname() + "_depot)\n\n");
            sb.append("  (at " + v.getNickname() + " " + v.getNickname() + "_depot" + ")\n");
            for (Entry<String, Vector<String>> entry : payloadNames.entrySet()) {
                for (String n : entry.getValue()) {
                    if (n.startsWith(v.getNickname() + "_")) {
                        double consumption = ((PayloadRequirement.valueOf(entry.getKey()).getConsumptionPerHour()
                                / 3600.0) * MVProblemSpecification.powerUnitMultiplier);
                        sb.append("  (= (battery-consumption-payload " + n + ") "
                                + String.format(Locale.US, "%.2f", consumption) + ")\n");
                        sb.append("  (having " + n + " " + v.getNickname() + ")\n");
                    }
                }
            }
        }

        sb.append("\n");
        return sb.toString();
    }
    
    protected String sampleTasks(MVProblemSpecification problem) {

        StringBuilder sb = new StringBuilder();
        for (SamplePointTask t : problem.sampleTasks) {
            sb.append("\n;" + t.getName() + " object of interest:\n");
            sb.append("  (free " + t.getName() + "_oi)\n");
            sb.append("  (at_oi " + t.getName() + "_obj " + t.getName() + "_oi" + ")\n");
            for (PayloadRequirement r : t.getRequiredPayloads()) {

                if (!payloadNames.containsKey(r.name())) {
                    System.err.println("No vehicle is capable of executing task " + t.getName() + " with " + r.name());
                    continue;
                }

                for (String alternative : payloadNames.get(r.name())) {
                    sb.append("  (task_desc " + t.getName() + "_" + r.name() + " " + t.getName() + "_obj " + alternative
                            + ")\n");
                }
            }
        }
        sb.append("\n");
        return sb.toString();
    }
    
    protected String surveyTasks(MVProblemSpecification problem) {

        StringBuilder sb = new StringBuilder();
        for (SurveyAreaTask t : problem.surveyTasks) {
            sb.append("\n;" + t.getName() + " survey:\n");
            sb.append("  (available " + t.getName() + "_area)\n");
            sb.append("  (free " + t.getName() + "_entry" + ")\n");
            sb.append("  (free " + t.getName() + "_exit" + ")\n");
            sb.append("  (entry " + t.getName() + "_area " + t.getName() + "_entry" + ")\n");
            sb.append("  (exit " + t.getName() + "_area " + t.getName() + "_exit" + ")\n");
            sb.append("  (=(surveillance_distance " + t.getName() + "_area) "
                    + String.format(Locale.US, "%.2f", t.getLength()) + ")\n");

            for (PayloadRequirement r : t.getRequiredPayloads()) {
                if (!payloadNames.containsKey(r.name())) {
                    System.err.println("No vehicle is capable of executing task " + t.getName() + " with " + r.name());
                    continue;
                }
                for (String alternative : payloadNames.get(r.name())) {
                    sb.append("  (task_desc " + t.getName() + "_" + r.name() + " " + t.getName() + "_area "
                            + alternative + ")\n");
                }
            }
        }
        return sb.toString();
    }

    protected String goals(MVProblemSpecification problem) {
        StringBuilder sb = new StringBuilder();
        sb.append("(:goal (and\n");
        for (SamplePointTask t : problem.sampleTasks) {
            for (PayloadRequirement r : t.getRequiredPayloads()) {
                sb.append("  (communicated_data " + t.getName() + "_" + r.name() + ")\n");
            }
        }
        for (SurveyAreaTask t : problem.surveyTasks) {
            for (PayloadRequirement r : t.getRequiredPayloads()) {
                sb.append("  (communicated_data " + t.getName() + "_" + r.name() + ")\n");
            }
        }
        sb.append("))\n");
        sb.append("(:metric minimize (total-time)))\n");
        return sb.toString();
    }
    
    public String getInitialState(MVProblemSpecification problem) {

        init(problem);

        // start printing...
        StringBuilder sb = new StringBuilder();
        sb.append("(define (problem LSTSprob)(:domain LSTS)\n(:objects\n  ");

        // print location names
        sb.append(locationNames(problem));

        // print vehicle names
        sb.append(vehicles(problem));

        // print payload names
        sb.append(payloadNames(problem));

        // print task names
        sb.append(taskNames(problem));

        sb.append(")\n(:init\n");

        // distance between all locations
        sb.append(distances(problem));

        // details of all vehicles
        sb.append(vehicleDetails(problem));

        // survey tasks
        sb.append(surveyTasks(problem));
        
        // sample tasks
        sb.append(sampleTasks(problem));
        sb.append(")\n");
        
        // goals to solve
        sb.append(goals(problem));

        return sb.toString();
    }

    public MVSolution parseSolution(MVProblemSpecification problem, String solution) {
        Vector<MVPlannerTask> tasks = new Vector<MVPlannerTask>();
        tasks.addAll(problem.sampleTasks);
        tasks.addAll(problem.surveyTasks);
        return new MVSolution(problem.calculateLocations(), solution, tasks);
    }
}
