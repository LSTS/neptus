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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Locale.Category;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.JFrame;

import psengine.PSEngine;
import psengine.PSObject;
import psengine.PSPlanDatabaseClient;
import psengine.PSSolver;
import psengine.PSToken;
import psengine.PSTokenList;
import psengine.PSVarValue;
import psengine.PSVariable;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.plugins.europa.gui.PlanView;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;

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

    private String extraNDDL = "";

    public NeptusSolver() throws Exception {
        europa = EuropaUtils.createPlanner();
        System.out.println(europa);
        EuropaUtils.loadModule(europa, "Neptus");
        EuropaUtils.loadModel(europa, "neptus/auv_model.nddl");
        planDb = europa.getPlanDatabaseClient();

    }

    public Collection<String> getVehicles() {
        return vehicleObjects.keySet();
    }
    
    public String resolveVehicleName(String mangledName) {
        for (Entry<String, PSObject> entry : vehicleObjects.entrySet()) {
            if (entry.getValue().getEntityName().equals(mangledName))
                return entry.getKey();
        }
        return null;
    }

    public String resolvePlanName(String mangledName) {
        for (Entry<String, PSObject> entry : planObjects.entrySet()) {
            if (entry.getValue().getEntityName().equals(mangledName))
                return entry.getKey();
        }
        return null;
    }

    public Collection<PSToken> getPlan(String vehicle) throws Exception {
        PSObject vObj = vehicleObjects.get(vehicle);
        if (vObj == null)
            throw new Exception("Unknown vehicle");

        PSTokenList plan1 = vObj.getTokens();
        Vector<PSToken> plan = new Vector<>();

        for (int i = 0; i < plan1.size(); i++) {
            // PSToken tok = plan1.get(i);
            plan.add(plan1.get(i));
        }

        Collections.sort(plan, new Comparator<PSToken>() {
            @Override
            public int compare(PSToken o1, PSToken o2) {
                return new Double(o1.getStart().getLowerBound()).compareTo(o2.getStart().getLowerBound());
            }
        });

        return plan;
    }

    public PSObject addVehicle(String name, LocationType position, double minSpeed, double nomSpeed, double maxSpeed,
            long minSpeedBattMillis, long nomSpeedBattMillis, long maxSpeedBattMillis) throws Exception {
        
        position.convertToAbsoluteLatLonDepth();
        // Auv(float _min_s, int _min_b, float _nom_s, int _nom_b, float _max_s, int _max_b)
        
        String nddl = String.format("Auv v_%s = new Auv(%.2f, %d, %.2f, %d, %.2f, %d);", 
                EuropaUtils.clearVarName(name),
                minSpeed, minSpeedBattMillis/1000, nomSpeed, nomSpeedBattMillis/1000, maxSpeed, maxSpeedBattMillis/1000                
                );
        
        eval(nddl+" /* lat=" + position.getLatitudeDegs() + ",lon="
                + position.getLongitudeDegs() + ",depth=" + position.getDepth() + "*/");
        PSObject varVehicle = europa.getObjectByName("v_" + EuropaUtils.clearVarName(name)), vehiclePos;

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

        System.out.println(vehicleObjects);
        return varVehicle;
    }

    public void eval(String nddl) throws Exception {
        EuropaUtils.eval(europa, nddl);
        extraNDDL += nddl + "\n";
    }

    public PSObject addTask(PlanType plan) throws Exception {
        String planName = plan.getId();

        Vector<LocatedManeuver> mans = PlanUtil.getLocationsAsSequence(plan);
        if (mans.isEmpty())
            throw new Exception("Cannot compute plan locations");

        System.out.println(plan.getId() +" starts at "+PlanUtil.getFirstLocation(plan));
        double dist = PlanUtil.getPlanLength(plan);
        LocationType startLoc = PlanUtil.getFirstLocation(plan);
        LocationType endLoc = PlanUtil.getEndLocation(plan);
        startLoc.convertToAbsoluteLatLonDepth();
        endLoc.convertToAbsoluteLatLonDepth();
        eval(String.format("DuneTask t_%s = new DuneTask(%.8f, %.8f, %.8f, %.8f, %.1f);",
                EuropaUtils.clearVarName(planName), startLoc.getLatitudeDegs(), startLoc.getLongitudeDegs(),
                endLoc.getLatitudeDegs(), endLoc.getLongitudeDegs(),
                dist));

        PSObject varTask = europa.getObjectByName("t_" + EuropaUtils.clearVarName(planName));

        System.out.println(varTask.getMemberVariable(varTask.getEntityName() + ".entry_latitude").toLongString());

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
        // to force execution of same plan twice or more times if necessary
        g_tok.getParameter("id").specifyValue(PSVarValue.getInstance(g_tok.getEntityKey()));

        return g_tok;
    }

    public void closeDomain() {
        planDb.close();
    }

    public void solve(int maxSteps) throws Exception {

        solver = EuropaUtils.createSolver(europa, 26100);

        while (solver.getStepCount() < maxSteps) {
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
        throw new Exception("Solver could not find a plan in " + maxSteps + " steps");
    }

    public void resetSolver() {
        solver.reset();
    }

    /**
     * @return the europa
     */
    public PSEngine getEuropa() {
        return europa;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(Locale.getDefault());
        
        String oldPath = System.getProperty("java.library.path");
        System.setProperty("java.library.path", "."+File.pathSeparator + oldPath + File.pathSeparator+new File(".", "libJNI/europa/x64").getCanonicalPath());
        oldPath = System.getProperty("java.library.path");
        System.setProperty("java.library.path", oldPath + File.pathSeparator+"/opt/europa-2.6/lib");
        
        Locale.setDefault(Category.FORMAT, Locale.US);
        Locale.setDefault(Locale.US);
        MissionType mt = new MissionType("missions/APDL/missao-apdl.nmisz");

        LocationType loc1 = new LocationType(mt.getHomeRef());
        LocationType loc2 = new LocationType(loc1).translatePosition(200, 180, 0);

        NeptusSolver solver = new NeptusSolver();
        String[] vehicles = new String[2];
        vehicles[0] = "lauv-xtreme-2";
        vehicles[1] = "lauv-xplore-1";
        
        solver.addVehicle(vehicles[0], loc1, 0.7, 1.0, 1.3, 8 * 3600 * 1000, 6 * 3600 * 1000, 4 * 3600 * 1000);
        solver.addVehicle(vehicles[1], loc2, 0.7, 1.0, 1.3, 8 * 3600 * 1000, 6 * 3600 * 1000, 4 * 3600 * 1000);

        for (PlanType pt : mt.getIndividualPlansList().values()) {
            solver.addTask(pt);
        }

        solver.closeDomain();
        
        int count = 0;
        //for (PlanType pt : mt.getIndividualPlansList().values()) {
          //  solver.addGoal(vehicles[0], pt.getId(), 1.1);
        //}
        
        solver.addGoal(vehicles[1], mt.getIndividualPlansList().values().iterator().next().getId(), 1.1);
        solver.addGoal(vehicles[0], mt.getIndividualPlansList().values().iterator().next().getId(), 1.3);
        solver.addGoal(vehicles[0], mt.getIndividualPlansList().values().iterator().next().getId(), 1.1);
        
        System.out.println(FileUtil.getFileAsString("conf/nddl/neptus/auv_model.nddl"));
        System.out.println(solver.extraNDDL);
        solver.solve(10000);
        
        PlanView view = new PlanView(solver);
        System.out.println(solver.europa.planDatabaseToString());
        //TimelineView timeline = new TimelineView(solver);
        //timeline.setPlan(solver.getPlan("lauv-xplore-1"));
        JFrame frm = GuiUtils.testFrame(view);
        frm.pack();
    }
}
