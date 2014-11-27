/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 26, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FuelLevel;
import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;

/**
 * @author zp
 *
 */
public class MVProblemSpecification {
    
    private static final int constantSpeed = 1;
    private static final int powerUnitMultiplier = 1000;
    private Vector<SamplePointTask> sampleTasks = new Vector<SamplePointTask>();
    private Vector<SurveyAreaTask> surveyTasks = new Vector<SurveyAreaTask>();
    private Vector<VehicleType> vehicles = new Vector<VehicleType>();

    public MVProblemSpecification(Collection<VehicleType> vehicles, Collection<MVPlannerTask> tasks) {
        for (MVPlannerTask t : tasks) {
            if (t instanceof SurveyAreaTask) 
                surveyTasks.add((SurveyAreaTask)t);
            else if (t instanceof SamplePointTask) {
                sampleTasks.add((SamplePointTask)t);
            }
        }
        
        this.vehicles.addAll(vehicles);
    }
    
    public String asPDDL() {
        
        LinkedHashMap<String, LocationType> locations = new LinkedHashMap<String, LocationType>();
        LinkedHashMap<String, Integer> vehicleBattery = new LinkedHashMap<String, Integer>();        
        LinkedHashMap<String, Vector<String>> payloadNames = new LinkedHashMap<String, Vector<String>>();
        
        // calculate all positions to be given to the planner, first from vehicles
        for (VehicleType v : vehicles) {
            ImcSystemState state = ImcMsgManager.getManager().getState(v);
            LocationType depot = IMCUtils.parseLocation(state.last(EstimatedState.class)).convertToAbsoluteLatLonDepth();
            locations.put(v.getNickname()+"_depot", depot);
            double fuelPercent = 100.0;
            FuelLevel fuel = state.last(FuelLevel.class);
            if (fuel != null && (System.currentTimeMillis() - fuel.getTimestampMillis()) < 600) {
                fuelPercent = state.last(FuelLevel.class).getValue();
            }
            int fuelUnits = (int) (PowerConsumptions.maxBattery(v) * (fuelPercent/100.0));
            vehicleBattery.put(v.getId(), fuelUnits);            
        }
        
        // and then tasks
        for (SurveyAreaTask task : surveyTasks) {
            locations.put(task.getName()+"_entry", task.getEntryPoint());
            locations.put(task.getName()+"_exit", task.getEndPoint());
        }
        for (SamplePointTask task : sampleTasks) {
            locations.put(task.getName()+"_oi", task.getLocation());            
        }
        
        // calculate all payload names
        for (VehicleType v : vehicles) {
            for (PayloadRequirement pr : PowerConsumptions.payloadsFor(v)) {
                if (!payloadNames.containsKey(pr.name()))
                    payloadNames.put(pr.name(), new Vector<String>());
                payloadNames.get(pr.name()).add(v.getNickname()+"_"+pr.name());                
            }
        }
        
        // start printing...
        StringBuilder sb = new StringBuilder();
        sb.append("(define (problem LSTSprob)(:domain LSTS)\n(:objects\n  ");

        // print location names
        for (String loc : locations.keySet()) {
            sb.append(" "+loc);
        }
        sb.append(" - location\n  ");
        
        // print vehicle names
        for (VehicleType v : vehicles)
            sb.append(" "+v.getNickname());
        sb.append(" - auv\n  ");
        
        // print payload names 
        for (String ptype : payloadNames.keySet()) {
            for (String name: payloadNames.get(ptype)) {
                sb.append(" "+name);
            }
            sb.append(" - "+ptype+"\n  ");
        }        
        
        if (!surveyTasks.isEmpty()) {
            sb.append(" ");
            for (SurveyAreaTask t : surveyTasks) {
                sb.append(" "+t.getName()+"_area");
            }
            sb.append(" - area\n");
        }
        
        if (!sampleTasks.isEmpty()) {
            sb.append("  ");
            for (SamplePointTask t : sampleTasks) {
                sb.append(" "+t.getName()+"_obj");
            }
            sb.append(" - oi\n");
        }
        
        sb.append("  ");
        for (SurveyAreaTask t : surveyTasks) {
            if (t.getRequiredPayloads().size() >= 1)
                for (PayloadRequirement pr : t.getRequiredPayloads()) 
                    sb.append(" "+t.getName()+"_"+pr.name());            
        }
        for (SamplePointTask t : sampleTasks) {
            if (t.getRequiredPayloads().size() >= 1)
                for (PayloadRequirement pr : t.getRequiredPayloads()) 
                    sb.append(" "+t.getName()+"_"+pr.name());
        }
        sb.append(" - task\n");
        
        sb.append(")\n(:init\n");
        
        // distance between all locations
        Vector<String> locNames = new Vector<String>();
        locNames.addAll(locations.keySet());
        for (int i = 0; i < locNames.size(); i++) {
            for (int j = 0; j < locNames.size(); j++) {
                if (i == j)
                    continue;
                
                String loc1 = locNames.get(i);
                String loc2 = locNames.get(j);
                long dist = Math.round(locations.get(loc1).getHorizontalDistanceInMeters(locations.get(loc2)));
                sb.append("  (=(distance "+loc1+" "+loc2+") "+dist+")\n");
            }
        }
        sb.append("\n");
        
        // details of all vehicles
        for (VehicleType v : vehicles) {
            sb.append("\n;"+v.getId()+":\n");
            int moveConsumption = (int) (PowerConsumptions.moveConsumption(v) * powerUnitMultiplier / 3600);
            sb.append("  (=(speed "+v.getNickname()+") "+constantSpeed+")\n");
            sb.append("  (= (battery-consumption-move "+v.getNickname()+") "+moveConsumption+")\n");
            sb.append("  (= (battery-level "+v.getNickname()+") "+(int)(vehicleBattery.get(v.getId())*powerUnitMultiplier)+")\n");
            sb.append("  (base "+v.getNickname()+" "+v.getNickname()+"_depot)\n\n");
            sb.append("  (at "+v.getNickname()+" "+v.getNickname()+"_depot"+")\n");
            for (Entry<String, Vector<String>> entry : payloadNames.entrySet()) {
                for (String n : entry.getValue()) {
                    if (n.startsWith(v.getNickname()+"_")) {
                        int consumption = (int) ((PayloadRequirement.valueOf(entry.getKey()).getConsumptionPerHour() / 3600.0) * powerUnitMultiplier);
                        sb.append("  (= (battery-consumption-payload "+n+") "+consumption+")\n");
                        sb.append("  (having "+n+" "+v.getNickname()+")\n");        
                    }
                }
            }
            
        }
        sb.append("\n");
        
        
        for (SurveyAreaTask t : surveyTasks) {
            sb.append("\n;"+t.getName()+" survey:\n");
            sb.append("  (available "+t.getName()+"_area)\n");
            sb.append("  (free "+t.getName()+"_entry"+")\n");
            sb.append("  (free "+t.getName()+"_exit"+")\n");
            sb.append("  (entry "+t.getName()+"_area "+t.getName()+"_entry"+")\n");                    
            sb.append("  (exit "+t.getName()+"_area "+t.getName()+"_exit"+")\n");

            for (PayloadRequirement r : t.getRequiredPayloads()) {
                if (!payloadNames.containsKey(r.name())) {
                    continue;
                }
                for (String alternative : payloadNames.get(r.name())) {
                    sb.append("  (task_desc "+t.getName()+"_"+r.name()+" "+t.getName()+"_area "+alternative+")\n");
                }
            }
        }
        
        for (SamplePointTask t : sampleTasks) {
            sb.append("\n;"+t.getName()+" object of interest:\n");
            sb.append("  (free "+t.getName()+"_oi)\n");
            sb.append("  (at_oi "+t.getName()+"_obj "+t.getName()+"_oi"+")\n");
            for (PayloadRequirement r : t.getRequiredPayloads()) {
                for (String alternative : payloadNames.get(r.name())) {
                    sb.append("  (task_desc "+t.getName()+"_"+r.name()+" "+t.getName()+"_obj "+alternative+")\n");
                }
            }
        }
        
        sb.append("\n)");
        sb.append("(:goal (and\n");
        for (SamplePointTask t :sampleTasks) {
            for (PayloadRequirement r : t.getRequiredPayloads()) {
                sb.append("  (communicated_data "+t.getName()+"_"+r.name()+")\n");                
            }
        }
        for (SurveyAreaTask t :surveyTasks) {
            for (PayloadRequirement r : t.getRequiredPayloads()) {
                sb.append("  (communicated_data "+t.getName()+"_"+r.name()+")\n");                
            }
        }
        sb.append("))\n");
        sb.append("(:metric minimize (total-time)))\n");
        
        //TODO
        return sb.toString();
    }
    
}
