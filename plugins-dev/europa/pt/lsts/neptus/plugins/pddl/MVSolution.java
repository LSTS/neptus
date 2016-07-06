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
 * Author: zp
 * Nov 27, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.maneuvers.Loiter;
import pt.lsts.neptus.mp.maneuvers.PopUp;
import pt.lsts.neptus.mp.maneuvers.RowsManeuver;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;
import pt.lsts.neptus.params.ManeuverPayloadConfig;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.params.SystemProperty.ValueTypeEnum;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

/**
 * @author zp
 *
 */
public class MVSolution {

    private static final double DEFAULT_DEPTH = 3;
    private ArrayList<Action> actions = new ArrayList<MVSolution.Action>();
    private final Pattern pat = Pattern.compile("(.*)\\:.*\\((.*)\\).* \\[(.*)\\]");
    private LinkedHashMap<String, LocationType> locations = null;
    private LinkedHashMap<String, MVPlannerTask> tasks = new LinkedHashMap<String, MVPlannerTask>();

    public MVSolution(LinkedHashMap<String, LocationType> locations, String pddlSolution, List<MVPlannerTask> tasks) {
        for (MVPlannerTask t : tasks)
            this.tasks.put(t.getName(), t);

        this.locations = locations;

        for (String line : pddlSolution.split("\n")) {
            Action act = createAction(line);
            if (act != null)
                actions.add(act);
        }
    }

    private Action createAction(String line) {
        Matcher m = pat.matcher(line);
        if (!m.matches())
            System.err.println("Bad output format: " + line);
        double timestamp = Double.parseDouble(m.group(1).trim());
        String a = m.group(2).trim();

        Action action = new Action();
        action.startTimestamp = (long) (1000 * timestamp);

        String[] parts = a.split(" ");
        action.vehicle = VehicleParams.getVehicleFromNickname(parts[1]);
        ManeuverLocation where = new ManeuverLocation(locations.get(parts[2]));
        String taskName = parts[2].split("_")[0];
        MVPlannerTask task = null;
        
        int i = 0;
        while (task == null) {
            taskName = parts[i++].split("_")[0];
            task = tasks.get(taskName);
        }
        
        double minDepth = Double.MAX_VALUE;
        double maxDepth = -Double.MAX_VALUE;
        
        if (parts[0].contains("survey") || parts[0].contains("sample")) {
            if (task.requiredPayloads == null || task.requiredPayloads.isEmpty()) {
                minDepth = maxDepth = DEFAULT_DEPTH;            
            }
            for (PayloadRequirement r : task.requiredPayloads) {
                minDepth = Math.min(minDepth, r.getMinDepth());
                maxDepth = Math.max(maxDepth, r.getMaxDepth());
            }
            
            if (minDepth < 0) {
                where.setZUnits(Z_UNITS.ALTITUDE);
                where.setZ(-minDepth);
            }
            else {
                where.setZUnits(Z_UNITS.DEPTH);
                where.setZ(DEFAULT_DEPTH);    
            }                
        }
        else {
            where.setZUnits(Z_UNITS.DEPTH);
            where.setZ(DEFAULT_DEPTH);    
        }
        
        switch (parts[0]) {
            case "move":
                Goto tmpMove = new Goto();
                tmpMove.setSpeed(1.0);
                tmpMove.setSpeedUnits(Maneuver.SPEED_UNITS.METERS_PS);
                tmpMove.setManeuverLocation(where);
                action.man = tmpMove;
                break;
            case "communicate":
                StationKeeping tmpSk = new StationKeeping();
                tmpSk.setManeuverLocation(where);
                tmpSk.setSpeed(1.0);
                tmpSk.setSpeedUnits("m/s");
                where.setZ(0);
                where.setZUnits(Z_UNITS.DEPTH);
                tmpSk.setDuration(60); // FIXME
                action.man = tmpSk;
                break;
            case "sample":
                Loiter tmpLoiter = new Loiter();
                tmpLoiter.setManeuverLocation(where);
                tmpLoiter.setSpeed(1.0);
                tmpLoiter.setSpeedUnits("m/s");
                tmpLoiter.setLoiterDuration(60); // FIXME
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 1].split("_")[1]));
                action.man = tmpLoiter;
                break;
            case "survey-one-payload":
                SurveyAreaTask onep = (SurveyAreaTask) tasks.get(taskName);
                onep.getPivot().setManeuverLocation(where);
                onep.getPivot().setSpeed(1.0);
                onep.getPivot().setSpeedUnits("m/s");
                action.man = onep.getPivot();
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 1].split("_")[1]));
                break;
            case "survey-two-payload":
                SurveyAreaTask twop = (SurveyAreaTask) tasks.get(taskName);
                twop.getPivot().setManeuverLocation(where);
                twop.getPivot().setSpeed(1.0);
                twop.getPivot().setSpeedUnits("m/s");
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 1].split("_")[1]));
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 2].split("_")[1]));
                action.man = twop.getPivot();
                break;
            case "survey-three-payload":
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 1].split("_")[1]));
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 2].split("_")[1]));
                action.payloads.add(PayloadRequirement.valueOf(parts[parts.length - 3].split("_")[1]));
                SurveyAreaTask threep = (SurveyAreaTask) tasks.get(taskName);
                threep.getPivot().setManeuverLocation(where);
                threep.getPivot().setSpeed(1.0);
                threep.getPivot().setSpeedUnits("m/s");
                action.man = threep.getPivot();
                break;
            default:
                System.err.println("Unrecognized action: " + line);
                break;
        }
        return action;
    }

    private void enablePayloads(ManeuverPayloadConfig manPayloads, ArrayList<PayloadRequirement> requiredPayloads) {

        for (SystemProperty e : manPayloads.getProperties()) {
            System.out.println(e.toString());

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

    public Collection<PlanType> generatePlans() {

        LinkedHashMap<String, PlanType> plansPerVehicle = new LinkedHashMap<String, PlanType>();

        for (Action act : actions) {
            ManeuverPayloadConfig payload = new ManeuverPayloadConfig(act.vehicle.getId(), act.man, null);
            enablePayloads(payload, act.payloads);
            Maneuver maneuver = (Maneuver) act.man.clone();

            if (!plansPerVehicle.containsKey(act.vehicle.getId())) {
                PlanType newPlan = new PlanType(null);
                newPlan.setId("mvplanner_"+act.vehicle.getId());
                newPlan.setVehicle(act.vehicle.getId());
                plansPerVehicle.put(act.vehicle.getId(), newPlan);                
            }
            
            if (maneuver instanceof RowsManeuver || maneuver instanceof Loiter) {
                LocatedManeuver m = (LocatedManeuver) maneuver;
                PopUp popup = new PopUp();
                popup.setDuration(120);
                popup.setSpeed(1.0);
                popup.setSpeedUnits("m/s");
                ManeuverLocation loc = new ManeuverLocation(m.getStartLocation());
                loc.setZ(DEFAULT_DEPTH);
                loc.setZUnits(Z_UNITS.DEPTH);
                popup.setManeuverLocation(loc);
                plansPerVehicle.get(act.vehicle.getId()).getGraph().addManeuverAtEnd(popup);                    
            }
            plansPerVehicle.get(act.vehicle.getId()).getGraph().addManeuverAtEnd(maneuver);        
        }
        
        return plansPerVehicle.values();
    }

    public ArrayList<MVPlans> generatePlansPerAction() {

        TreeMap<String, ArrayList<Maneuver>> manListperVehicle = new TreeMap<>();
        HashMap<String, ArrayList<Long>> tsPerVehicle = new HashMap<>();

        for (Action act : actions) {
            ManeuverPayloadConfig payload = new ManeuverPayloadConfig(act.vehicle.getId(), act.man, null);
            enablePayloads(payload, act.payloads);

            if (!manListperVehicle.containsKey(act.vehicle.getId())) {

                Maneuver maneuver = (Maneuver) act.man.clone();

                // store maneuvers
                ArrayList<Maneuver> manList = new ArrayList<Maneuver>();
                manList.add(maneuver);
                manListperVehicle.put(act.vehicle.getId(), manList);
                // store timestamps
                ArrayList<Long> tsList = new ArrayList<>();
                tsList.add(act.startTimestamp);
                tsPerVehicle.put(act.vehicle.getId(), tsList);
            }
            else {
                ArrayList<Maneuver> storedManeuvers = manListperVehicle.get(act.vehicle.getId());
                ArrayList<Long> storedTimestamps = tsPerVehicle.get(act.vehicle.getId());

                storedManeuvers.add((Maneuver) act.man.clone());
                storedTimestamps.add(act.startTimestamp);

            }
        }
        TreeMap<String, ArrayList<PlanType>> planListperVehicle = new TreeMap<String, ArrayList<PlanType>>();

        for (Entry<String, ArrayList<Maneuver>> v : manListperVehicle.entrySet()) {

            for (Maneuver m : v.getValue()) {
                if (planListperVehicle.get(v.getKey()) == null) {
                    PlanType plan = new PlanType(null);
                    plan.getGraph().addManeuver(m);
                    ArrayList<PlanType> planos = new ArrayList<PlanType>();
                    planos.add(plan);
                    planListperVehicle.put(v.getKey(), planos);
                }
                else {
                    PlanType plan = new PlanType(null);
                    plan.getGraph().addManeuver(m);

                    planListperVehicle.get(v.getKey()).add(plan);
                }
            }
        }

        ArrayList<MVPlans> list = new ArrayList<>();

        for (Entry<String, ArrayList<PlanType>> e : planListperVehicle.entrySet()) {

            int i = 0;
            VehicleType veh = VehiclesHolder.getVehicleById(e.getKey());
            MVPlans vehiclePlans = new MVPlans(veh);

            for (PlanType plan : e.getValue()) {
                plan.setVehicle(e.getKey());

                if (plan.getGraph().getAllManeuvers().length > 0) // ensure that plan has maneuver
                    plan.setId("pl_" + e.getKey() + "_" + plan.getGraph().getAllManeuvers()[0].getType() + i);

                vehiclePlans.addPlan(tsPerVehicle.get(veh.getId()).get(i), plan);
                i++;

            }

            list.add(vehiclePlans);
        }
        for (int i = 0; i < list.size(); i++)
            System.out.println("tamanho lista : " + list.get(i).planList.toString());

        return list;

    }

    static class Action {
        public long startTimestamp;
        public ArrayList<PayloadRequirement> payloads = new ArrayList<PayloadRequirement>();
        public Maneuver man;
        public VehicleType vehicle;

        @Override
        public String toString() {
            return man.getType() + " with payloads " + payloads + ", using vehicle " + vehicle + " at time "
                    + startTimestamp;
        }

    }

    class MVPlans {
        private VehicleType vehicle;
        private LinkedHashMap<Long, PlanType> planList;

        public MVPlans(VehicleType vehicle) {
            this.setVehicle(vehicle);
            this.planList = new LinkedHashMap<>();

        }

        public void addPlan(long ts, PlanType planToAdd) {
            planList.put((Long) ts, planToAdd);
        }

        public VehicleType getVehicle() {
            return vehicle;
        }

        public void setVehicle(VehicleType vehicle) {
            this.vehicle = vehicle;
        }

        /**
         * @return the planList
         */
        public HashMap<Long, PlanType> getPlanList() {
            return planList;
        }

    }

}
