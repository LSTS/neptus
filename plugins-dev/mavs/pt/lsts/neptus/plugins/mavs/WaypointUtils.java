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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Apr 15, 2011
 */
package pt.lsts.neptus.plugins.mavs;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.Loiter;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class WaypointUtils {

    public static String getWPLWaypoint(int index, int curwpt, Goto man) {
        double lld[] = man.getManeuverLocation().getAbsoluteLatLonDepth();
        return index+"\t"+curwpt+"\t16\t0\t0\t0\t0\t"+lld[0]+"\t"+lld[1]+"\t"+(-lld[2])+"1";
    }
    
    public static String getWPLWaypoint(int index, int curwpt, Loiter man) {
        double lld[] = man.getLocation().getAbsoluteLatLonDepth();
        if (man.getLoiterType().equals("Circular")) {
            return index+"\t"+curwpt+"\t19\t"+man.getMaxTime()+"\t0\t"+man.getRadius()+"\t"+man.getBearing()+"\t"+lld[0]+"\t"+lld[1]+"\t"+(-lld[2])+"1";
        }
        else if (man.getLoiterType().equals("Hover")) {
            return index+"\t"+curwpt+"\t18\t"+man.getMaxTime()+"\t0\t0\t0\t"+lld[0]+"\t"+lld[1]+"\t"+(-lld[2])+"1";
        }
        else 
            return null;
    }
    
    public static String getAsQGCFormat(PlanType plan) throws Exception {
        String line, ret = "QGC WPL 110\n";
        int index = 0, first = 1;
        
        for (Maneuver man : plan.getGraph().getManeuversSequence()) {
            line = null;
            if (man instanceof Goto)
                line = getWPLWaypoint(index, first, (Goto)man);
            if (man instanceof Loiter)
                line = getWPLWaypoint(index, first, (Loiter)man);
            
            if (line != null) {
                first = 0;
                index++;
            }
            else {
                throw new Exception("Error converting maneuver '"+man.getId()+"' ("+man.getClass().getSimpleName()+")");
            }
            ret += line+"\n";
        }
        return ret;
    }
}