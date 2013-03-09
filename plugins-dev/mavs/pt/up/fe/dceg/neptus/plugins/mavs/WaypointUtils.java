/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Apr 15, 2011
 */
package pt.up.fe.dceg.neptus.plugins.mavs;

import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.Goto;
import pt.up.fe.dceg.neptus.mp.maneuvers.Loiter;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

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