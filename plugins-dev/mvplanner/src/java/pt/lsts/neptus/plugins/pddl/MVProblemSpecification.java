/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 26, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.events.ConsoleEventFutureState;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.FileUtil;

/**
 * @author zp
 *
 */
public class MVProblemSpecification {

    public static final int constantSpeed = 1;
    public static final int powerUnitMultiplier = 1000;
    Vector<SamplePointTask> sampleTasks = new Vector<SamplePointTask>();
    Vector<SurveyAreaTask> surveyTasks = new Vector<SurveyAreaTask>();
    Vector<SurveyPolygonTask> surveyPolygon = new Vector<SurveyPolygonTask>();
    LinkedHashMap<VehicleType, SystemPositionAndAttitude> vehicleStates = new LinkedHashMap<>();
    LocationType defaultLoc = null;
    int secondsAwayFromDepot = 1000;
    private String solutionStr = "";

    private List<String> commandBase = Arrays.asList("lpg -o DOMAIN -f INITIAL_STATE -out OUTFILE".split(" "));
    private List<String> commandSpeed = Arrays.asList("-speed");
    private List<String> commandSecs = Arrays.asList("-n 10 -cputime".split(" "));

    private MVDomainModel domainModel = MVDomainModel.V1;

    LinkedHashMap<String, LocationType> calculateLocations() {
        LinkedHashMap<String, LocationType> locations = new LinkedHashMap<String, LocationType>();

        // Vehicle depots (present and future)
        for (Entry<VehicleType, SystemPositionAndAttitude> entry : vehicleStates.entrySet())
            locations.put(entry.getKey().getNickname() + "_depot", entry.getValue().getPosition());

        // and then tasks
        for (SurveyAreaTask task : surveyTasks) {
            locations.put(task.getName() + "_entry", task.getEntryPoint());
            locations.put(task.getName() + "_exit", task.getEndPoint());
        }
        for (SurveyPolygonTask task : surveyPolygon) {
            locations.put(task.getName() + "_entry", task.getEntryPoint());
            locations.put(task.getName() + "_exit", task.getEndPoint());
        }
        
        for (SamplePointTask task : sampleTasks) {
            locations.put(task.getName() + "_oi", task.getLocation());
        }

        return locations;
    }

    public boolean solve(int secs) throws Exception {
        solutionStr = "";
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd-HHmmss");
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = fmt.format(new Date());
        String input_file = "log/pddl/problem_" + timestamp + ".pddl";
        String output_file = "log/pddl/solution_" + timestamp + ".SOL";

        FileUtil.saveToFile(input_file, asPDDL());
        System.out.println(asPDDL());
        if (secs == 0) {
            commandBase.addAll(commandSpeed);
        }
        else {
            commandBase.addAll(commandSecs);
            commandBase.add(String.format("%d", secs));
        }

        int idx = commandBase.indexOf("DOMAIN");
        commandBase.remove(idx);
        commandBase.add(idx, domainModel.file().getAbsolutePath()
                .replaceAll("/", System.getProperty("file.separator")));
        idx = commandBase.indexOf("INITIAL_STATE");
        commandBase.remove(idx);
        commandBase.add(idx, String.format("problem_%s.pddl", timestamp)
                .replaceAll("/", System.getProperty("file.separator")));
        idx = commandBase.indexOf("OUTFILE");
        commandBase.remove(idx);
        commandBase.add(idx, (String.format("solution_%s.SOL", timestamp))
                .replaceAll("/", System.getProperty("file.separator")));

        String[] cmd = commandBase.toArray(new String[commandBase.size()]);
        Process p = Runtime.getRuntime().exec(cmd, null, new File("log/pddl"));
        
        Thread monitor = new Thread() {
            public void run() {
                try {
                    if (!p.waitFor(secs+1, TimeUnit.SECONDS)) {
                        NeptusLog.pub().error("LPG is taking too long to plan. Killing process...");
                        p.destroyForcibly();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            };
        };
        monitor.setDaemon(true);
        monitor.start();
        
        StringBuilder result = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        
        while (line != null) {
            result.append(line.toLowerCase().trim() + "\n");
            System.out.println(line);
            NeptusLog.pub().debug(line);
            line = reader.readLine();
        }
        File outputFile = new File(output_file);
        if (outputFile.canRead()) {
            solutionStr = FileUtil.getFileAsString(outputFile);
            return true;
        }
        return false;
    }

    public MVProblemSpecification(MVDomainModel model, Collection<VehicleType> vehicles,
            Collection<MVPlannerTask> tasks, Collection<ConsoleEventFutureState> futureStates,
            LocationType defaultLoc, int secondsAwayFromDepot) {

        this.domainModel = model;
        this.defaultLoc = defaultLoc;
        this.secondsAwayFromDepot = secondsAwayFromDepot;
        
        // connected vehicles
        for (VehicleType v : vehicles) {
            LocationType loc = defaultLoc;
            try {
                loc = ImcSystemsHolder.getSystemWithName(v.getId()).getLocation();
            }
            catch (Exception e) {
            }
            SystemPositionAndAttitude pos = new SystemPositionAndAttitude(loc, 0, 0, 0);
            vehicleStates.put(v, pos);
        }

        // vehicles currently executing plans (connected or not)
        for (ConsoleEventFutureState futureState : futureStates) {
            SystemPositionAndAttitude state = new SystemPositionAndAttitude(futureState.getState());
            state.setTime(futureState.getDate().getTime());
            vehicleStates.put(VehiclesHolder.getVehicleById(futureState.getVehicle()), state);
        }

        for (MVPlannerTask t : tasks) {
            if (t.getAssociatedAllocation() != null)
                continue;
            if (t instanceof SurveyAreaTask)
                surveyTasks.add((SurveyAreaTask) t);
            else if (t instanceof SurveyPolygonTask)
                surveyPolygon.add((SurveyPolygonTask) t);
            else if (t instanceof SamplePointTask)
                sampleTasks.add((SamplePointTask) t);            
        }
    }

    public String asPDDL() {
        return domainModel.translator().getInitialState(this);
    }

    public MVSolution getSolution() throws Exception {
        return domainModel.translator().parseSolution(this, solutionStr);
    }

    /**
     * @return the solutionStr
     */
    public String toString() {
        return solutionStr;
    }

}
