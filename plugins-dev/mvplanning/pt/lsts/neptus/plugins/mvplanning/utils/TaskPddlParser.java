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
 * 27 Jul 2016
 */


package pt.lsts.neptus.plugins.mvplanning.utils;

import com.google.common.io.Files;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.mvplanning.interfaces.PlanTask;
import pt.lsts.neptus.plugins.mvplanning.interfaces.TaskConstraint;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class responsible for parsing a PDDL domain file,
 * searching for actions/tasks specifications such as
 * what constraints need to be met by a vehicle for a given
 * task to be allocated to it (Battery level > n, IsAvailable, etc)
 */
public class TaskPddlParser {
    private static Map<String, List<String>> tasksPddlSpecs = loadPddlActions();
    private static Map<String, Map<String, String>> tasksContraints = loadTaksConstraints();

    /**
     * Returns a mapping from a TaskConstraint name to its
     * PDDL specification
     * */
    public static Map<String, String> getTaskConstraints(String taskName) {
        return tasksContraints.get(taskName);
    }

    /**
     * Parses a PDDL domain file and returns a
     * mapping from Action/Task name to its
     * pddl specification (in plain text)
     * */
    private static Map<String, List<String>> loadPddlActions() {
        File pddlFile = new File("conf/pddl/MvPlanning_domain.pddl");

        if(!pddlFile.exists() || pddlFile.isDirectory())
            return null;


        Map<String, List<String>> actions = new HashMap<>();
        Pattern actionPattern = Pattern.compile("\\(:([durative-]*)action(.*?)\\s\\)", Pattern.DOTALL);
        StringBuilder allLines = new StringBuilder();

        try {
            /* Merge all PDDL file's lines */
            for(String line : Files.readLines(pddlFile, Charset.defaultCharset()))
                allLines.append(line + "\n");

            Matcher match = actionPattern.matcher(allLines.toString());

            /* fetch defined actions/tasks */
            while(match.find()) {
                String action = match.group();
                String actionName = action.split(" ")[1].trim();
                actions.put(actionName, Arrays.asList(action.split("\n")));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return actions;
    }

    /**
     * Parses the constraints of all action/task defined
     * in the PDDL's ":condition" block
     * */
    private static Map<String, Map<String, String>> loadTaksConstraints() {
        Map<String, Map<String, String>> tasksConstraints = new HashMap<>();
        for(PlanTask.TASK_TYPE taskType : PlanTask.TASK_TYPE.values()) {
            String taskName = taskType.value;
            NeptusLog.pub().info("[" + taskName + "] " +  "loading specification from PDDL domain");
            tasksConstraints.put(taskName, loadTaskConstrains(taskName));
        }

        return tasksConstraints;
    }

    /**
     * Returns the constrains' specification for
     * a specific action/task, by looking for
     * "at start" string
     * */
    private static Map<String, String> loadTaskConstrains(String taskName) {
        List<String> taskPddlSpec = tasksPddlSpecs.get(taskName);
        if(taskPddlSpec == null) {
            NeptusLog.pub().warn("[" + taskName + "] failed to load from PDDL domain");
            return null;
        }

        Map<String, String> constraints = new HashMap<>();

        Pattern pat = Pattern.compile("\\(at start(.*?)\\((.*)\\){2}");
        for(String line : taskPddlSpec) {
            Matcher m = pat.matcher(line);

            while (m.find()) {
                String constrainSpec = m.group(2);

                /* Check if this constraint is defined in TaskContraint.NAME's enum */
                Optional<TaskConstraint.NAME> entry= Arrays.asList(TaskConstraint.NAME.values())
                        .stream()
                        .filter(c -> constrainSpec.contains(c.name()))
                        .findFirst();

                if (!entry.isPresent()) {
                    NeptusLog.pub().warn("[" + taskName + "] Constraint name in \"" + constrainSpec + "\" unknown. Ignoring...");
                    continue;
                }

                NeptusLog.pub().info("[" + taskName + "] " + entry.get().name());
               constraints.put(entry.get().name(), constrainSpec);
            }
        }
        System.out.println();
        return constraints;
    }
}
