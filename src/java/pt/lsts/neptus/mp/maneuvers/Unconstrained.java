/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * May 11, 2005
 */
package pt.lsts.neptus.mp.maneuvers;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Teleoperation;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.mp.Maneuver;

/**
 * @author zepinto
 */
public class Unconstrained extends Maneuver implements IMCSerialization {

	public void loadManeuverFromXML(String xml) {
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
