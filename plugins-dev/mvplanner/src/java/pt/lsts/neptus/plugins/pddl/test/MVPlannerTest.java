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
package pt.lsts.neptus.plugins.pddl.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Vector;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.pddl.MVDomainModel;
import pt.lsts.neptus.plugins.pddl.MVPlannerInteraction;
import pt.lsts.neptus.plugins.pddl.MVPlannerTask;
import pt.lsts.neptus.plugins.pddl.MVProblemSpecification;
import pt.lsts.neptus.plugins.pddl.PayloadRequirement;
import pt.lsts.neptus.plugins.pddl.SamplePointTask;
import pt.lsts.neptus.plugins.pddl.SurveyAreaTask;
import pt.lsts.neptus.plugins.pddl.VehicleParams;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.FileUtil;

/**
 * @author zp
 *
 */
public class MVPlannerTest {

    private static MVProblemSpecification generateTest(int numberOfSurveys, int numberOfSamplings, String... vehicles) {
        Vector<VehicleType> vehiclTypes = new Vector<VehicleType>();
        Vector<MVPlannerTask> generatedTasks = new Vector<MVPlannerTask>();
        HashSet<PayloadRequirement> availablePayloads = new HashSet<PayloadRequirement>();
        for (String v : vehicles) {
            VehicleType vehicle = VehiclesHolder.getVehicleById(v);
            vehiclTypes.add(vehicle);
            availablePayloads.addAll(Arrays.asList(VehicleParams.payloadsFor(vehicle)));
        }

        LocationType center = new LocationType(41, -8);
        Random r = new Random(System.currentTimeMillis());

        for (int i = 0; i < numberOfSurveys; i++) {
            LocationType loc = new LocationType(center)
                    .translatePosition(r.nextDouble() * 2500 - 1250, r.nextDouble() * 2500 - 1250, 0)
                    .convertToAbsoluteLatLonDepth();
            SurveyAreaTask task = new SurveyAreaTask(loc);
            ArrayList<PayloadRequirement> reqs = new ArrayList<PayloadRequirement>();
            reqs.addAll(availablePayloads);
            int numPayloads = reqs.size();
            for (int j = 0; j < numPayloads; j++) {
                if (r.nextDouble() > 0.5 && reqs.size() > 1) {
                    reqs.remove(r.nextInt(reqs.size()));
                }
            }

            task.setSize(r.nextDouble() * 1000 + 20, r.nextDouble() * 1500 + 100, r.nextDouble() * 360);
            HashSet<PayloadRequirement> payloads = new HashSet<PayloadRequirement>();
            payloads.addAll(reqs);
            task.setRequiredPayloads(payloads);
            generatedTasks.add(task);
        }

        for (int i = 0; i < numberOfSamplings; i++) {
            LocationType loc = new LocationType(center)
                    .translatePosition(r.nextDouble() * 2500 - 1250, r.nextDouble() * 2500 - 1250, 0)
                    .convertToAbsoluteLatLonDepth();
            SamplePointTask task = new SamplePointTask(loc);
            ArrayList<PayloadRequirement> reqs = new ArrayList<PayloadRequirement>();
            reqs.addAll(availablePayloads);
            int numPayloads = reqs.size();
            for (int j = 0; j < numPayloads; j++) {
                if (r.nextDouble() > 0.5 && reqs.size() > 1) {
                    reqs.remove(r.nextInt(reqs.size()));
                }
            }
            HashSet<PayloadRequirement> payloads = new HashSet<PayloadRequirement>();
            payloads.addAll(reqs);
            task.setRequiredPayloads(payloads);
            generatedTasks.add(task);
        }

        LocationType defaultLoc = new LocationType(center).translatePosition(r.nextDouble() * 300 - 150,
                r.nextDouble() * 300 - 150, 0);
        return new MVProblemSpecification(MVDomainModel.V1, vehiclTypes, generatedTasks, new ArrayList<>(), defaultLoc, 1000);
    }

    /**
     * Unitary test used to generate a series of initial states to feed and test the planner...
     */
    public static void main(String[] args) {
        MVPlannerInteraction inter = new MVPlannerInteraction();
        inter.init(ConsoleLayout.forge());

        for (int i = 1; i <= 10; i++) {
            MVProblemSpecification spec = generateTest(i, i, "lauv-seacon-1", "lauv-xplore-1",
                    "lauv-noptilus-3", "lauv-xtreme-2");
            String pddl = spec.asPDDL();
            for (int j = 1; j <= 5; j++) {
                FileUtil.saveToFile("state" + i + "_" + j + ".pddl", pddl);
                System.out.println("Created " + "state" + i + ".pddl");
            }
        }
    }
}
