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
 * Nov 27, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.Loiter;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;

/**
 * @author zp
 *
 */
public class MVSolution {

    private ArrayList<Action> actions = new ArrayList<MVSolution.Action>();
    private final Pattern pat = Pattern.compile("(.*)\\:.*\\((.*)\\).* \\[(.*)\\]");
    private LinkedHashMap<String, LocationType> locations = null;
    LinkedHashMap<String, MVPlannerTask> tasks = new LinkedHashMap<String, MVPlannerTask>();
    public MVSolution(LinkedHashMap<String, LocationType> locations, String pddlSolution, List<MVPlannerTask> tasks) {
        for (MVPlannerTask t : tasks)
            this.tasks.put(t.getName(), t);
        
        this.locations = locations;
        System.out.println(locations);
        
        for (String line : pddlSolution.split("\n")) {
            Action act = createAction(line);
            if (act != null)
                actions.add(act);
        }
    }
    
    private Action createAction(String line) {
        Matcher m = pat.matcher(line);
        if (!m.matches())
            System.err.println("Bad output format: "+line);
        double timestamp = Double.parseDouble(m.group(1).trim());
        String a = m.group(2).trim();
        
        Action action = new Action();
        action.startTimestamp = (long) (1000 * timestamp);
        
        String[] parts = a.split(" ");
        System.out.println(ArrayUtils.toString(parts));
        action.vehicle = VehicleParams.getVehicleFromNickname(parts[1]);
        LocationType where = locations.get(parts[2]);
        String taskName = parts[2].split("_")[0];
        
        switch (parts[0]) {
            case "move":
                Goto tmpMove = new Goto();
                tmpMove.setManeuverLocation(new ManeuverLocation(where));
                action.man = tmpMove;
                break;
            case "communicate":
                StationKeeping tmpSk = new StationKeeping();
                tmpSk.setManeuverLocation(new ManeuverLocation(where));
                tmpSk.setDuration(10); //FIXME
                action.man = tmpSk;
                break;
            case "sample":
                Loiter tmpLoiter = new Loiter();
                tmpLoiter.setManeuverLocation(new ManeuverLocation(where));
                tmpLoiter.setLoiterDuration(10); //FIXME
                action.man = tmpLoiter;
                break;
            case "survey-one-payload":
                
                break;
            case "survey-two-payloads":
                break;
            case "survey-three-payloads":
                break;
            default:
                break;
        }
        
        return action;
    }
    
    static class Action {
        public long startTimestamp;
        public long endTimestamp;
        public ArrayList<PayloadRequirement> payloads = new ArrayList<PayloadRequirement>();
        public Maneuver man;
        public VehicleType vehicle;
    }    
}
