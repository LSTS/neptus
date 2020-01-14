/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 27, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import pt.lsts.imc.ScheduledGoto.DELAYED;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.mp.maneuvers.AreaSurvey;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.Loiter;
import pt.lsts.neptus.mp.maneuvers.PopUp;
import pt.lsts.neptus.mp.maneuvers.RowsManeuver;
import pt.lsts.neptus.mp.maneuvers.ScheduledGoto;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;
import pt.lsts.neptus.params.ManeuverPayloadConfig;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.params.SystemProperty.ValueTypeEnum;
import pt.lsts.neptus.plugins.mvplanner.api.ConsoleEventPlanAllocation;
import pt.lsts.neptus.plugins.mvplanner.api.ConsoleEventPlanAllocation.Operation;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;

/**
 * @author zp
 *
 */
public class MVSolution {

    private static final double DEFAULT_DEPTH = 3;
    private static int allocationCounter = 1;
    private ArrayList<PddlAction> actions = new ArrayList<MVSolution.PddlAction>();
    private LinkedHashMap<String, LocationType> locations = null;
    private LinkedHashMap<String, MVPlannerTask> tasks = new LinkedHashMap<String, MVPlannerTask>();

    private boolean generatePopups = false;
    private boolean useScheduledGoto = false;

    public MVSolution(LinkedHashMap<String, LocationType> locations, String pddlSolution, List<MVPlannerTask> tasks)
            throws Exception {
        for (MVPlannerTask t : tasks)
            this.tasks.put(t.getName(), t);

        this.locations = locations;

        for (String line : pddlSolution.split("\n")) {
            if (line.trim().isEmpty() || line.trim().startsWith(";"))
                continue;
            PddlAction act = createAction(line.toLowerCase());
            if (act != null)
                actions.add(act);
        }
    }

    private PddlAction createAction(String line) throws Exception {
        line = line.trim();
        String regex = "[\\:\\(\\)\\[\\] ]+";
        String[] parts = line.split(regex);
        PddlAction action = new PddlAction();
        String actionStr;

            
        try {
            actionStr = parts[1];
            action.type = actionStr;
            if (actionStr.contains("-"))
                action.type = actionStr.substring(0, actionStr.indexOf('-'));

            if (action.type.equals("getready"))
                return null;
           
            
            action.startTime = (long) (1000 * Double.parseDouble(parts[0]) + System.currentTimeMillis());
            action.endTime = (long) (1000 * Double.parseDouble(parts[parts.length - 1])) + action.startTime;
            action.name = parts[3];            
            action.name = action.name.replaceAll("_entry", "");
            action.name = action.name.replaceAll("_exit", "");
            action.name = action.name.replaceAll("_oi", "");
            action.name = action.name.replaceAll("_depot", "");
            action.vehicle = VehicleParams.getVehicleFromNickname(parts[2]);
            if (action.type.equals("move"))
                action.location = new ManeuverLocation(locations.get(parts[4]));
            else
                action.location = new ManeuverLocation(locations.get(parts[3]));

        }
        catch (Exception e) {
            throw new Exception("Unrecognized PDDL syntax on line '" + line + "'", e);
        }

        MVPlannerTask task = tasks.get(action.name);

        double minDepth = Double.MAX_VALUE;
        double maxDepth = -Double.MAX_VALUE;

        System.out.println("Parsing "+actionStr);
        if (actionStr.contains("survey") || actionStr.contains("sample")) {
            if (task.getRequiredPayloads() == null || task.getRequiredPayloads().isEmpty()) {
                minDepth = maxDepth = DEFAULT_DEPTH;
            }
            for (PayloadRequirement r : task.getRequiredPayloads()) {
                minDepth = Math.min(minDepth, r.getMinDepth());
                maxDepth = Math.max(maxDepth, r.getMaxDepth());
            }

            if (minDepth < 0) {
                action.location.setZUnits(Z_UNITS.ALTITUDE);
                action.location.setZ(-minDepth);
            }
            else {
                action.location.setZUnits(Z_UNITS.DEPTH);
                action.location.setZ(DEFAULT_DEPTH);
            }
        }
        else {
            action.location.setZUnits(Z_UNITS.DEPTH);
            action.location.setZ(DEFAULT_DEPTH);
        }

        switch (actionStr) {
            case "survey-one-payload":
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 2].split("_")[1]));
                break;
            case "survey-two-payload":
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 3].split("_")[1]));
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 2].split("_")[1]));
                break;
            case "survey-three-payload":
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 4].split("_")[1]));
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 3].split("_")[1]));
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 2].split("_")[1]));
                break;
            default:
                break;
        }
        return action;
    }

    private void enablePayloads(ManeuverPayloadConfig manPayloads, ArrayList<PayloadRequirement> requiredPayloads) {

        for (SystemProperty e : manPayloads.getProperties()) {

            if (requiredPayloads.contains(PayloadRequirement.ctd)) {
                // System.out.println("I need CTD : "+ true);
            }

            if (e.getCategory().startsWith("UAN") && e.getValueType().equals(ValueTypeEnum.BOOLEAN)) {
                e.setValue(true);
            }

            if (e.getCategory().startsWith("Camera") && e.getValueType().equals(ValueTypeEnum.BOOLEAN)) {

                if (requiredPayloads.contains(PayloadRequirement.camera))
                    e.setValue(true);
                else
                    e.setValue(false);
            }

            if (e.getCategory().startsWith("Multibeam") && e.getValueType().equals(ValueTypeEnum.BOOLEAN)) {
                if (requiredPayloads.contains(PayloadRequirement.multibeam))
                    e.setValue(true);
                else
                    e.setValue(false);
            }

            if (e.getCategory().equals("Sidescan") && e.getValueType().equals(ValueTypeEnum.BOOLEAN)) {
                if (requiredPayloads.contains(PayloadRequirement.sidescan)
                        || requiredPayloads.contains(PayloadRequirement.edgetech))
                    e.setValue(true);
                else
                    e.setValue(false);
            }

            if (e.getCategory().equalsIgnoreCase("rhodamine") && e.getValueType().equals(ValueTypeEnum.BOOLEAN)) {
                if (requiredPayloads.contains(PayloadRequirement.rhodamine)) {
                    e.setValue(true);
                }
                else {
                    e.setValue(false);
                }
            }
        }

        manPayloads.setProperties(manPayloads.getProperties());

    }

    /**
     * @param generatePopups If <code>true</code>, move actions will be appended a Popup maneuver
     */
    public void setGeneratePopups(boolean generatePopups) {
        this.generatePopups = generatePopups;
    }

    /**
     * @param useScheduledGoto If <code>true</code>, move actions will be scheduled in time
     */
    public void setScheduledGotosUsed(boolean useScheduledGoto) {
        this.useScheduledGoto = useScheduledGoto;
    }

    /**
     * Generate allocations for this solution
     * 
     * @return For each allocation, a pair with its allocated action IDs and the event to execute the allocation
     */
    public ArrayList<Pair<ArrayList<String>, ConsoleEventPlanAllocation>> allocations() {
        LinkedHashMap<String, PlanType> plansPerVehicle = new LinkedHashMap<String, PlanType>();
        LinkedHashMap<String, Date> startTimes = new LinkedHashMap<String, Date>();
        LinkedHashMap<String, ArrayList<String>> actionsPerVehicle = new LinkedHashMap<>();

        int i = 1;
        for (PddlAction act : actions) {
            Maneuver maneuver = generateManeuver(act);
            maneuver.setId("" + (i++));
            ManeuverPayloadConfig payload = new ManeuverPayloadConfig(act.vehicle.getId(), maneuver, null);
            enablePayloads(payload, act.payloads);

            if (!plansPerVehicle.containsKey(act.vehicle.getId())) {
                PlanType newPlan = new PlanType(null);
                newPlan.setId("mvplan_" + act.vehicle.getNickname());
                newPlan.setVehicle(act.vehicle.getId());
                plansPerVehicle.put(act.vehicle.getId(), newPlan);
                actionsPerVehicle.put(act.vehicle.getId(), new ArrayList<>());
                startTimes.put(act.vehicle.getId(), new Date(act.startTime));
            }

            
            System.out.println(act.name+" matches? "+act.name.matches("t[0-9]+(_p[0-9]+)?"));
            // just account for valid actions and not movements to depots...
            if (act.name.matches("t[0-9]+(_p[0-9]+)?")) {
                ArrayList<String> actions = actionsPerVehicle.get(act.vehicle.getId());
                if (!actions.contains(act.name))
                    actions.add(act.name);
            }

            int numMans = plansPerVehicle.get(act.vehicle.getId()).getGraph().getAllManeuvers().length;

            if (generatePopups && (maneuver instanceof RowsManeuver || maneuver instanceof Loiter)) {
                LocatedManeuver m = (LocatedManeuver) maneuver;
                PopUp popup = new PopUp();
                popup.setDuration(120);
                popup.setSpeed(new SpeedType(1.0, Units.MPS));
                ManeuverLocation loc = new ManeuverLocation(m.getStartLocation());
                loc.setZ(DEFAULT_DEPTH);
                loc.setZUnits(Z_UNITS.DEPTH);
                popup.setManeuverLocation(loc);
                popup.setId("" + (++numMans));
                plansPerVehicle.get(act.vehicle.getId()).getGraph().addManeuverAtEnd(popup);
            }
            maneuver.setId("" + (++numMans));
            plansPerVehicle.get(act.vehicle.getId()).getGraph().addManeuverAtEnd(maneuver);
        }

        ArrayList<Pair<ArrayList<String>, ConsoleEventPlanAllocation>> result = new ArrayList<>();
        
        for (String s : plansPerVehicle.keySet()) {
            PlanType plan = plansPerVehicle.get(s);
            plan.setId(String.format("mvplanner-%03d-%s", allocationCounter++, plan.getVehicleType().getNickname()));
            Pair<ArrayList<String>, ConsoleEventPlanAllocation> p = new Pair<>(actionsPerVehicle.get(s),
                    new ConsoleEventPlanAllocation(plansPerVehicle.get(s), startTimes.get(s), Operation.ALLOCATED));
            result.add(p);
        }

        return result;
    }

    public Maneuver generateManeuver(PddlAction action) {

        Maneuver m = null;

        switch (action.type) {
            case "communicate":
                action.location.setZ(0);
                action.location.setZUnits(Z_UNITS.DEPTH);
                StationKeeping tmpSk = new StationKeeping();
                tmpSk.setManeuverLocation(action.location);
                tmpSk.setSpeed(new SpeedType(1.0, Units.MPS));
                tmpSk.setDuration(60);
                m = tmpSk;
                break;
            case "move":
                if (useScheduledGoto) {
                    ScheduledGoto tmpMove = new ScheduledGoto();
                    tmpMove.setSpeed(new SpeedType(1.0, Units.MPS));
                    tmpMove.setManeuverLocation(action.location);
                    tmpMove.setArrivalTime(new Date(action.endTime));
                    tmpMove.setDelayedBehavior(DELAYED.RESUME);
                    m = tmpMove;
                    break;
                }
                else {
                    Goto tmpMove = new Goto();
                    tmpMove.setSpeed(new SpeedType(1.0, Units.MPS));
                    tmpMove.setManeuverLocation(action.location);
                    m = tmpMove;
                    break;
                }
            case "sample": {
                Loiter tmpLoiter = new Loiter();
                tmpLoiter.setManeuverLocation(action.location);
                tmpLoiter.setSpeed(new SpeedType(1.0, Units.MPS));
                tmpLoiter.setLoiterDuration((int) ((action.endTime - action.startTime) / 1000));
                ManeuverPayloadConfig payloadConfig = new ManeuverPayloadConfig(action.vehicle.getId(), tmpLoiter,
                        null);
                enablePayloads(payloadConfig, action.payloads);
                m = tmpLoiter;
                break;
            }
            case "survey": {
                if (tasks.get(action.name) instanceof SurveyAreaTask) {
                    SurveyAreaTask surveyTask = (SurveyAreaTask) tasks.get(action.name);
                    RowsManeuver rows = (RowsManeuver) surveyTask.getPivot().clone();
                    rows.setManeuverLocation(action.location);
                    rows.setSpeed(new SpeedType(1.0, Units.MPS));
                    ManeuverPayloadConfig payloadConfig = new ManeuverPayloadConfig(action.vehicle.getId(), rows, null);
                    enablePayloads(payloadConfig, action.payloads);
                    m = rows;
                }
                else {
                    SurveyPolygonTask surveyTask = (SurveyPolygonTask) tasks.get(action.name);
                    AreaSurvey rows = (AreaSurvey) surveyTask.getPivot().clone();
                    ManeuverLocation manLoc = rows.getManeuverLocation();
                    
                    manLoc.setZ(action.location.getZ());
                    manLoc.setZUnits(action.location.getZUnits());
                    rows.setManeuverLocation(manLoc);
                    rows.setSpeed(new SpeedType(1.0, Units.MPS));
                    ManeuverPayloadConfig payloadConfig = new ManeuverPayloadConfig(action.vehicle.getId(), rows, null);
                    enablePayloads(payloadConfig, action.payloads);
                    m = rows;
                }
                
                break;
            }
            default:
                System.err.println("Unrecognized action type: " + action.type);
                return null;
        }
        // m.setId(action.name);
        return m;
    }
    

    /**
     * @return the actions
     */
    public ArrayList<PddlAction> getActions() {
        return actions;
    }

    public static class PddlAction {
        public long startTime, endTime;
        public ArrayList<PayloadRequirement> payloads = new ArrayList<PayloadRequirement>();
        public ManeuverLocation location;
        // public Maneuver man;
        public VehicleType vehicle;
        public String type;
        public String name;

        @Override
        public String toString() {
            return type + " @" + location + " with payloads " + payloads + ", using vehicle " + vehicle + " on "
                    + new Date(startTime);
        }

    }
    
    
    public static void main(String[] args) {
        String pattern = "t[0-9]+(_p[0-9]+)?";
        String act1 = "t23";
        String act2 = "t34_p3";
        
        System.out.println(act1.matches(pattern));
        System.out.println(act2.matches(pattern));
        
    }
}
