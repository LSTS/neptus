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
 * May 11, 2005
 * $Id:: Unconstrained.java 9880 2013-02-07 15:23:52Z jqcorreia           $:
 */
package pt.up.fe.dceg.neptus.mp.maneuvers;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.Teleoperation;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;


/**
 * @author zepinto
 */
public class Unconstrained extends Maneuver implements IMCSerialization {

	public void loadFromXML(String xml) {
		
	}

	public void initializeManeuver(ParametersPanel params) {
		//No initialization needs to be done...
	}

	public String getType() {
		return "Unconstrained";
	}

	public Object clone() {
		
		Unconstrained u = new Unconstrained();
		super.clone(u);
		return u;
	}

	public SystemPositionAndAttitude ManeuverFunction(SystemPositionAndAttitude lastVehicleState) {
		endManeuver();
		JOptionPane.showMessageDialog(new JFrame(), "<html>The current maneuver is unconstrained (tele-operation)<br>"+
				"Click to proceed to the next maneuver", "Unconstrained Maneuver", JOptionPane.INFORMATION_MESSAGE
			);
		return lastVehicleState;		
	}
	

	public Document getManeuverAsDocument(String rootElementName) {
	    Document document = DocumentHelper.createDocument();
	    Element root = document.addElement( rootElementName );
	    root.addAttribute("kind", "manual");    
	    return document;
	}

	
	@Override
	public void parseIMCMessage(IMCMessage message) {
		setCustomSettings(message.getTupleList("custom"));
	}
	
	public IMCMessage serializeToIMC() {
	    Teleoperation teleop = new Teleoperation();
	    teleop.setCustom(getCustomSettings());		
		return teleop;
	}
}
