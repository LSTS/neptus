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
 * Jun 25, 2014
 */
package pt.lsts.neptus.plugins.europa;

import java.util.LinkedHashMap;
import java.util.Vector;

import psengine.PSEngine;
import psengine.PSObject;
import psengine.PSPlanDatabaseClient;
import psengine.PSSolver;
import psengine.PSToken;
import psengine.PSVarValue;
import psengine.PSVariable;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class NeptusSolver {

    private PSEngine europa;
    private PSSolver solver;
    private PSPlanDatabaseClient planDb;

    private LinkedHashMap<String, PSObject> vehicleObjects = new LinkedHashMap<>();
    private LinkedHashMap<String, PSObject> planObjects = new LinkedHashMap<>();
    
    public NeptusSolver() throws Exception {
        EuropaUtils.loadLibrary("Neptus");
        europa = EuropaUtils.createPlanner("neptus/auv_model.nddl");
        planDb = europa.getPlanDatabaseClient();
        
    }
    
    public PSObject addVehicle(String name, LocationType position) throws Exception {
        EuropaUtils.eval(europa, "Auv v_"+EuropaUtils.clearVarName(name)+" = new Auv();");
        position.convertToAbsoluteLatLonDepth();
        PSObject varVehicle = europa.getObjectByName("v_"+EuropaUtils.clearVarName(name)), vehiclePos;
        
        // Stupid europa require the full name of the attribute prefixed with the object name ...
        PSVariable varPosition = varVehicle.getMemberVariable(varVehicle.getEntityName() + ".position");
        // I need this to make sure that the location will ba associeted to xtreme1
        vehicleObjects.put(name, varVehicle);
        
        vehiclePos = PSObject.asPSObject(varPosition.getSingletonValue().asObject());
        
        // Create my factual position
        PSToken tok = planDb.createToken("Position.Pos", false, true);
        // specify that it is the position of xtreme1
        tok.getParameter("object").specifyValue(vehiclePos.asPSVarValue());
        // specify that this happens at the tick 0
        tok.getStart().specifyValue(PSVarValue.getInstance(0));

        // Make the vehicle starts at (-0.1, 0.02) which is 11km from (0.0,0.0)
        tok.getParameter("latitude").specifyValue(PSVarValue.getInstance(position.getLatitudeDegs()));
        tok.getParameter("longitude").specifyValue(PSVarValue.getInstance(position.getLongitudeDegs()));
        tok.getParameter("depth").specifyValue(PSVarValue.getInstance(position.getDepth()));
        
        return varVehicle;
    }
    
    public PSObject addTask(PlanType plan) throws Exception {
        String planName = plan.getId();
        //FIXME
        double planLength = 2000;
        
        Vector<LocatedManeuver> mans = PlanUtil.getLocationsAsSequence(plan);
        if (mans.isEmpty() )
            throw new Exception("Cannot compute plan locations");
        
        LocationType startLoc = new LocationType(mans.firstElement().getStartLocation()), 
                endLoc = new LocationType(mans.lastElement().getEndLocation());

        EuropaUtils.eval(europa, String.format("Task t_%s = new Task(%.7f, %.7f, %.7f, %.7f, %.1f);",
                EuropaUtils.clearVarName(planName),
                startLoc.getLatitudeDegs(),
                startLoc.getLongitudeDegs(),
                endLoc.getLatitudeDegs(),
                endLoc.getLongitudeDegs(),
                startLoc.getHorizontalDistanceInMeters(endLoc)+100));
        
        PSObject varTask = europa.getObjectByName("t_"+EuropaUtils.clearVarName(planName));
        
        
        
        planObjects.put(planName, varTask);
        return varTask;
    }
    
    public PSToken addGoal(String vehicle, String planId, double speed) throws Exception {
        
        PSToken g_tok = planDb.createToken("Auv.Execute", true, false);
        // If I wanted to force it to xtreme1 I would do:
        g_tok.getParameter("object").specifyValue(vehicleObjects.get(vehicle).asPSVarValue());
        // that task is the only one I defined
        g_tok.getParameter("task").specifyValue(planObjects.get(planId).asPSVarValue());
        // the speed is 1.5m/s
        g_tok.getParameter("speed").specifyValue(PSVarValue.getInstance(speed));
        
        //EuropaUtils.eval(europa, g_tok.getEntityName()+".end <= 26100;");

        return g_tok;
    }
    
    public void closeDomain() {
        planDb.close();
    }
    
    public void solve(int maxSteps) throws Exception {
       
        solver = EuropaUtils.createSolver(europa, 26100);
        
        while(solver.getStepCount() < maxSteps) {
            EuropaUtils.printFlaws(solver);
            if (!EuropaUtils.step(solver)) {
                if (EuropaUtils.failed(solver)) {
                    throw new Exception("Solver failed to find a plan");
                }
                else {
                    return;
                }
            }
        }
        throw new Exception("Solver could not find a plan in "+maxSteps+" steps");
    }
    
    public void resetSolver() {
        solver.reset();
    }
    
    public static void main(String[] args) throws Exception {
        MissionType mt = new MissionType("missions/APDL/missao-apdl.nmisz");
        
        LocationType loc1 = new LocationType(mt.getHomeRef());
        LocationType loc2 = new LocationType(loc1).translatePosition(200, 180, 0);
        
        NeptusSolver solver = new NeptusSolver();
        //solver.addVehicle("lauv-xtreme-2", loc1);
       solver.addVehicle("lauv-xplore-1", loc2);
        
        for (PlanType pt : mt.getIndividualPlansList().values()) {
            solver.addTask(pt);            
        }
        
        solver.closeDomain();
        //for (PlanType pt : mt.getIndividualPlansList().values()) {
           PSToken goal = solver.addGoal("lauv-xplore-1", mt.getIndividualPlansList().values().iterator().next().getId(), 1.1);            
        //}
        
       solver.solve(10000);
       System.out.println(solver.europa.planDatabaseToString());
        System.out.println(goal.toLongString());
        
    }
}
