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
 * Author: zp
 * 28/01/2017
 */
package pt.lsts.neptus.plugins.pddl;

import java.util.Map.Entry;
import java.util.Vector;

import pt.lsts.neptus.types.vehicle.VehicleType;

/**
 * @author zp
 *
 */
public class LstsDomainModelV2 extends LstsDomainModel {

    @Override
    protected String vehicleDetails(VehicleType v, MVProblemSpecification problem) {

        StringBuilder sb = new StringBuilder();

        double timeToStart = (states.get(v).getTime() - System.currentTimeMillis()) / 1000.0;
        if (timeToStart < 10)
            timeToStart = 0;

        sb.append("\n  ;" + v.getId() + ":\n");
        sb.append("  (= (speed " + v.getNickname() + ") " + MVProblemSpecification.constantSpeed + ")\n");
        sb.append("  (base " + v.getNickname() + " " + v.getNickname() + "_depot)\n\n");
        sb.append("  (at " + v.getNickname() + " " + v.getNickname() + "_depot" + ")\n");

        for (Entry<String, Vector<String>> entry : payloadNames.entrySet()) {
            for (String n : entry.getValue()) {
                if (n.startsWith(v.getNickname() + "_")) {
                    sb.append("  (having " + n + " " + v.getNickname() + ")\n");
                }
            }
        }

        sb.append("  (at " + timeToStart + " (can-move " + v.getNickname()+"))\n");

        //sb.append("  (can-move " + v.getNickname() + ") ;required always\n");
        sb.append("  (= (from-base " + v.getNickname() + ") 0) ;how long the vehicle is away from its depot \n"); // FIXME
        sb.append("  (= (max-to-base " + v.getNickname() + ") " + problem.secondsAwayFromDepot
                + ") ;the maximum time before returning to the depot\n");
        sb.append("\n");

        return sb.toString();
    }

    @Override
    protected String goals(MVProblemSpecification problem) {

        StringBuilder sb = new StringBuilder();

        sb.append("\n(:goal (and\n");
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
        for (SurveyPolygonTask t : problem.surveyPolygon) {
            for (PayloadRequirement r : t.getRequiredPayloads()) {
                sb.append("  (communicated_data " + t.getName() + "_" + r.name() + ")\n");
            }
        }
        
        sb.append("))\n\n");

        sb.append("(:metric minimize (+ (total-time)(base-returns))))\n");

        return sb.toString();
    }

    @Override
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

        sb.append("  (= (base-returns) 0) ; \"cost\" of returning to the depots \n");

        // details of all vehicles
        for (VehicleType v : states.keySet()) {
            sb.append(vehicleDetails(v, problem));
        }

        // survey tasks
        sb.append(surveyTasks(problem));

        // sample tasks
        sb.append(sampleTasks(problem));
        sb.append(")\n");

        // goals to solve
        sb.append(goals(problem));

        return sb.toString();
    }

}
