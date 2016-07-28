package pt.lsts.neptus.plugins.mvplanning.utils;

import com.google.common.io.Files;
import info.necsave.msgs.Plan;
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
 * Created by tsmarques on 27/07/16.
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

    private static Map<String, List<String>> loadPddlActions() {
        File pddlFile = new File("conf/pddl/MvPlanning_domain.pddl");

        if(!pddlFile.exists() || pddlFile.isDirectory())
            return null;


        Map<String, List<String>> actions = new HashMap<>();
        Pattern actionPattern = Pattern.compile("\\(:([durative-]*)action(.*?)\\s\\)", Pattern.DOTALL);
        StringBuilder allLines = new StringBuilder();

        try {
            for(String line : Files.readLines(pddlFile, Charset.defaultCharset()))
                allLines.append(line + "\n");

            Matcher match = actionPattern.matcher(allLines.toString());

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

    private static Map<String, Map<String, String>> loadTaksConstraints() {
        Map<String, Map<String, String>> tasksConstraints = new HashMap<>();
        for(PlanTask.TASK_TYPE taskType : PlanTask.TASK_TYPE.values()) {
            String taskName = taskType.value;
            NeptusLog.pub().info("[" + taskName + "] " +  "loading specification from PDDL domain");
            tasksConstraints.put(taskName, loadTaskConstrains(taskName));
        }

        return tasksConstraints;
    }

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
