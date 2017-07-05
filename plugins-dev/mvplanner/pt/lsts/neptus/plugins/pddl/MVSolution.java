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
 * Nov 27, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TimeZone;

import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.ScheduledGoto.DELAYED;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.imc.TemporalAction;
import pt.lsts.imc.TemporalAction.STATUS;
import pt.lsts.imc.TemporalAction.TYPE;
import pt.lsts.imc.TemporalPlan;
import pt.lsts.imc.VehicleDepot;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.maneuvers.AreaSurvey;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.IMCSerialization;
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
import pt.lsts.util.PlanUtilities;

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
            PddlAction[] act = createAction(line.toLowerCase());
            if (act != null) {
                for (PddlAction a : act)
                    actions.add(a);
            }
        }
    }

    private PddlAction[] createAction(String line) throws Exception {
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
            e.printStackTrace();
            throw new Exception("Unrecognized PDDL syntax on line '" + line + "'", e);
        }

        MVPlannerTask task = tasks.get(action.name);

        double minDepth = Double.MAX_VALUE;
        double maxDepth = -Double.MAX_VALUE;

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
            case "collab-survey-one-payload":
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 4].split("_")[1]));
                PddlAction locate = new PddlAction();
                locate.location = new ManeuverLocation(locations.get(parts[parts.length - 2]));
                locate.endTime = action.endTime;
                locate.startTime = action.startTime;
                locate.type = "locate";
                locate.vehicle = VehicleParams.getVehicleFromNickname(parts[parts.length - 3]);
                locate.name = action.name;
                return new PddlAction[] { action, locate };
            default:
                break;
        }
        return new PddlAction[] { action };
    }
    
    public Collection<IMCMessage> getPayloadParams(ArrayList<PayloadRequirement> requiredPayloads) {
        
        ArrayList<IMCMessage> ret = new ArrayList<>();
        
        for (PayloadRequirement r : requiredPayloads) {
            SetEntityParameters msg = new SetEntityParameters();
            msg.setParams(Arrays.asList(new EntityParameter("Active", "true")));
            switch (r) {
                case sidescan:
                    msg.setName("Sidescan");
                    ret.add(msg);
                    break;
                case camera:
                    msg.setName("Camera");
                    ret.add(msg);
                    break;
                case multibeam:
                    msg.setName("Multibeam");
                    ret.add(msg);
                    break;
                default:
                    break;
            }
        }
        
        return ret;
    }

    private void enablePayloads(ManeuverPayloadConfig manPayloads, ArrayList<PayloadRequirement> requiredPayloads) {

        for (SystemProperty e : manPayloads.getProperties()) {

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
                if (requiredPayloads.contains(PayloadRequirement.sidescan))
                    e.setValue(true);
                else
                    e.setValue(false);
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
     * This method translates the solution into an IMC's TemporalPlan message.
     * @return The temporal plan
     */
    public TemporalPlan generateTemporalPlan(int secondsAway) {
        ArrayList<TemporalAction> planActions = new ArrayList<>();
        int i = 1;
        
        LinkedHashMap<String, VehicleDepot> depots = new LinkedHashMap<>();
        
        for (Entry<String, LocationType> loc : locations.entrySet()) {
            if (loc.getKey().endsWith("depot")) {
                String nick = loc.getKey().substring(0, loc.getKey().indexOf("_"));
                lastLocations.put(VehicleParams.getVehicleFromNickname(nick).getId(), new LocationType(loc.getValue()));
                VehicleDepot depot = new VehicleDepot();
                LocationType l = new LocationType(loc.getValue());
                l.convertToAbsoluteLatLonDepth();
                depot.setLat(l.getLatitudeRads());
                depot.setLon(l.getLongitudeRads());
                depot.setVehicle(VehicleParams.getVehicleFromNickname(nick).getImcId().intValue());
                depots.put(VehicleParams.getVehicleFromNickname(nick).getId(), depot);
            }
        }
        
        for (PddlAction act : actions) {
            Maneuver maneuver = generateManeuver(act);
            String manId = "" + (i++);
            maneuver.setId(manId);
            
            TemporalAction action = new TemporalAction();
            action.setSystemId(act.vehicle.getImcId().intValue());
            action.setDuration((act.endTime - act.startTime) / 1000.0);
            action.setStartTime(act.startTime / 1000.0);
            String id = maneuver.getId()+"_"+act.type;
            if (act.type.equals("survey") || act.type.equals("sample"))
                id = act.name;
            else if (act.type.equals("move")) {
               VehicleDepot vdepot = depots.get(act.vehicle.getId());
               if (vdepot.getDeadline() == 0) {
                   Date d = new Date(act.startTime + secondsAway * 1000);
                   vdepot.setDeadline(d.getTime()/1000.0);
                   System.out.println("Deadline for "+act.vehicle.getId()+" is now "+d);
               }
                
            }
            action.setActionId(id);
            
            PlanManeuver pm = new PlanManeuver();
            pm.setManeuverId(manId);
            pm.setData((pt.lsts.imc.Maneuver) ((IMCSerialization) maneuver).serializeToIMC());
            pm.setStartActions(getPayloadParams(act.payloads));
            PlanSpecification spec = new PlanSpecification();
            spec.setManeuvers(Arrays.asList(pm));
            spec.setPlanId(id);
            spec.setStartManId(manId);
            action.setAction(spec);
            action.setType(TYPE.valueOf(act.type.toUpperCase()));
            action.setStatus(STATUS.SCHEDULED);            
            planActions.add(action);
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-DD_HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        TemporalPlan plan = new TemporalPlan();
        plan.setDepots(depots.values());

        plan.setPlanId("plan_"+sdf.format(new Date()));
        plan.setActions(planActions);
        return plan;
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

        for (Entry<String, LocationType> loc : locations.entrySet()) {
            if (loc.getKey().endsWith("depot")) {
                String nick = loc.getKey().substring(0, loc.getKey().indexOf("_"));
                lastLocations.put(VehicleParams.getVehicleFromNickname(nick).getId(), new LocationType(loc.getValue()));
            }
        }
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

            // System.out.println(act.name+" matches? "+act.name.matches("t[0-9]+(_p[0-9]+)?"));
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
                popup.setSpeed(1.0);
                popup.setSpeedUnits(Maneuver.SPEED_UNITS.METERS_PS);
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

    LinkedHashMap<String, LocationType> lastLocations = new LinkedHashMap<>();

    public Maneuver generateManeuver(PddlAction action) {

        Maneuver m = null;
        //System.out.println(action.type + ", " + action.name + ", " + action.vehicle.getId() + ", " + action.location);
        switch (action.type) {
            case "locate": {
                StationKeeping sk = new StationKeeping();
                ManeuverLocation loc = sk.getManeuverLocation();
                loc.setLocation(action.location);
                loc.setZ(0);
                loc.setZUnits(Z_UNITS.DEPTH);
                sk.setManeuverLocation(loc);
                sk.setSpeedUnits(Maneuver.SPEED_UNITS.METERS_PS);
                sk.setDuration(((int) ((action.endTime - action.startTime) / 1000)));
                sk.setSpeed(1.0);
                sk.setRadius(20);
                m = sk;
                break;
            }
            case "communicate":
                action.location.setZ(0);
                action.location.setZUnits(Z_UNITS.DEPTH);
                StationKeeping tmpSk = new StationKeeping();
                tmpSk.setManeuverLocation(action.location);
                tmpSk.setSpeed(1.0);
                tmpSk.setSpeedUnits(Maneuver.SPEED_UNITS.METERS_PS);
                tmpSk.setDuration(60);
                m = tmpSk;
                break;
            case "move":
                if (useScheduledGoto) {
                    ScheduledGoto tmpMove = new ScheduledGoto();
                    tmpMove.setSpeed(1.0);
                    tmpMove.setSpeedUnits(Maneuver.SPEED_UNITS.METERS_PS);
                    tmpMove.setManeuverLocation(action.location);
                    tmpMove.setArrivalTime(new Date(action.endTime));
                    tmpMove.setDelayedBehavior(DELAYED.RESUME);
                    m = tmpMove;
                    break;
                }
                else {
                    Goto tmpMove = new Goto();
                    tmpMove.setSpeed(1.0);
                    tmpMove.setSpeedUnits(Maneuver.SPEED_UNITS.METERS_PS);
                    tmpMove.setManeuverLocation(action.location);
                    m = tmpMove;
                    break;
                }
            case "sample": {
                Loiter tmpLoiter = new Loiter();
                tmpLoiter.setManeuverLocation(action.location);
                tmpLoiter.setSpeed(1.0);
                tmpLoiter.setSpeedUnits(Maneuver.SPEED_UNITS.METERS_PS);
                tmpLoiter.setLoiterDuration((int) ((action.endTime - action.startTime) / 1000));
                ManeuverPayloadConfig payloadConfig = new ManeuverPayloadConfig(action.vehicle.getId(), tmpLoiter,
                        null);
                enablePayloads(payloadConfig, action.payloads);
                m = tmpLoiter;
                break;
            }
            case "survey":
            case "collab": {
                if (tasks.get(action.name) instanceof SurveyAreaTask) {
                    SurveyAreaTask surveyTask = (SurveyAreaTask) tasks.get(action.name);
                    RowsManeuver rows = (RowsManeuver) surveyTask.getPivot().clone();
                    rows.setManeuverLocation(action.location);
                    rows.setSpeed(1.0);
                    rows.setSpeedUnits(Maneuver.SPEED_UNITS.METERS_PS);
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
                    rows.setSpeed(1.0);
                    rows.setSpeedUnits(Maneuver.SPEED_UNITS.METERS_PS);
                    ManeuverPayloadConfig payloadConfig = new ManeuverPayloadConfig(action.vehicle.getId(), rows, null);
                    enablePayloads(payloadConfig, action.payloads);
                    m = rows;
                }

                break;
            }
            case "surface": {
                LocationType last = lastLocations.get(action.vehicle.getId());
                StationKeeping sk = new StationKeeping();

                ManeuverLocation loc = sk.getManeuverLocation();
                if (last != null)
                    loc.setLocation(last);
                
                loc.setZ(0);
                loc.setZUnits(Z_UNITS.DEPTH);
                sk.setManeuverLocation(loc);
                sk.setSpeedUnits(Maneuver.SPEED_UNITS.METERS_PS);
                sk.setDuration(((int) ((action.endTime - action.startTime) / 1000)));
                sk.setSpeed(1.0);
                sk.setRadius(20);
                m = sk;
                break;
            }
            default:
                System.err.println("Unrecognized action type: " + action.type);
                return null;
        }
        if (m instanceof LocatedManeuver)
            lastLocations.put(action.vehicle.getId(), ((LocatedManeuver) m).getEndLocation());
        
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
