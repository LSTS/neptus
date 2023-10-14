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

import javax.swing.JOptionPane;

import pt.lsts.neptus.graph.GraphElementFactory;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverFactory;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
/**
 * 
 * @author ZP
 *
 */
public class ManeuverGraphFactory implements GraphElementFactory<ManeuverNode, ManeuverTransition> {

	private MissionType mission = null;
	private PlanType plan = null;
	private ManeuverFactory mf = null;
	
	
	public ManeuverGraphFactory(MissionType mt, PlanType plan) {
		this.mission = mt;
		this.plan = plan;
		this.mf = new ManeuverFactory(VehiclesHolder.getVehicleById(plan.getVehicle()));
	}
	
	public ManeuverTransition createEdge() {
		return new ManeuverTransition();
	}
	
	public ManeuverNode createNode() {	
		Maneuver man;
		
		String[] avTypes = getManeuverNames();
		int index = 0;
		
		if (avTypes.length != 1)
			index = JOptionPane.showOptionDialog(null, "Set maneuver type", "Set type...", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, avTypes, avTypes[0]);
		
		if (index != -1) {
			man = createManeuver(avTypes[index]);
		}
		else
			return null;
		
		int i = 1;
		while(plan.getGraph().getManeuver(man.getType()+i) != null)
			i++;		
		
		man.setId(man.getType()+i);
		
		plan.getGraph().addManeuver(man);
		
		if (man instanceof LocatedManeuver)
			((LocatedManeuver)man).getManeuverLocation().setLocation(new LocationType(mission.getHomeRef()));
		return new ManeuverNode(man);
	}
	
	public String[] getManeuverNames() {
		return mf.getAvailableManeuversIDs();
	}
	
	public Maneuver createManeuver(String maneuverType) {
		
		return mf.getManeuver(maneuverType);
	}
}
