/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.planeditor;

import java.util.Vector;

import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

public class PlanValidator {

	public static String[] validatePlan(PlanType plan) {
		Vector<String> errors = new Vector<String>();
		
		String type = plan.getVehicleType().getType();
		if (type.equals("AUV")) {
			errors.addAll(validateAUVPlan(plan));
		}
		
		if (type.equals("UAV")) {
			errors.addAll(validateUAVPlan(plan));
		}
		
		return errors.toArray(new String[0]);
	}
	
	private static Vector<String> validateAUVPlan(PlanType plan) {
		
		Vector<String> errors = new Vector<String>();
		
		for (Maneuver m : plan.getGraph().getAllManeuvers()) {
			if (m instanceof LocatedManeuver) {
				LocationType loc = new LocationType(((LocatedManeuver)m).getManeuverLocation());
				if (loc.getAllZ() < 0) {
					errors.add("The maneuver <font color='#000066'>"+m.getId()+"</font> has negative depth. ("+loc.getAllZ()+" m)");
				}
			}
		}
		
		return errors;
	}
	
	private static Vector<String> validateUAVPlan(PlanType plan) {
		
		Vector<String> errors = new Vector<String>();
		
		for (Maneuver m : plan.getGraph().getAllManeuvers()) {
			if (m instanceof LocatedManeuver) {
				LocationType loc = new LocationType(((LocatedManeuver)m).getManeuverLocation());
				if (loc.getAllZ() > 0) {
					errors.add("The maneuver <font color='#000066'>"+m.getId()+"</font> has negative altitude. ("+loc.getAllZ()+" m)");
				}
			}
		}
		
		return errors;
	}
}
