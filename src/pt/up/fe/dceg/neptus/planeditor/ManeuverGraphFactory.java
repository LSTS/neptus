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
 * $Id:: ManeuverGraphFactory.java 9616 2012-12-30 23:23:22Z pdias        $:
 */
package pt.up.fe.dceg.neptus.planeditor;

import javax.swing.JOptionPane;

import pt.up.fe.dceg.neptus.graph.GraphElementFactory;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.ManeuverFactory;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
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
