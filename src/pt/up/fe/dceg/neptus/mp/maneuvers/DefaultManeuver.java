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
 * $Id:: DefaultManeuver.java 9880 2013-02-07 15:23:52Z jqcorreia         $:
 */
package pt.up.fe.dceg.neptus.mp.maneuvers;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;

/**
 * When a maneuver of an unknown type is encountered. The maneuver factories use this class as default.
 * @author ZP
 *
 */
public class DefaultManeuver extends Maneuver {

	private String manType = "Unknown";
	
	@Override
	public SystemPositionAndAttitude ManeuverFunction(SystemPositionAndAttitude lastVehicleState) {
		return lastVehicleState;
	}

	@Override
	public Object clone() {
		DefaultManeuver clone = new DefaultManeuver();
		return super.clone(clone);		
	}

	@Override
	public Document getManeuverAsDocument(String rootElementName) {		
		return null;
	}

	@Override
	public String getType() {		
		return manType;
	}

	@Override
	public void loadFromXML(String XML) {
	    try {
	        Document doc = DocumentHelper.parseText(XML);
	        this.manType = doc.getRootElement().getName();
	    }
	    catch (Exception e) {
	        
	        NeptusLog.pub().error(this, e);
	        return;
	    }
	}
}
