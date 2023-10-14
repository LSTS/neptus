/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.planeditor;

import java.util.Vector;

import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;

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
