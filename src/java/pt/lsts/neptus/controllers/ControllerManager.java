/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: José Quadrado Correia ?
 * 
 */
package pt.lsts.neptus.controllers;

import java.util.LinkedHashMap;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.JoyEnvironment;
import pt.lsts.neptus.NeptusLog;

/**
 * ControllerManager class 
 * 
 * Manages the controllers and serves as an abstraction to JInput.
 * Every console instantiates this class during initialization.
 * @author jqcorreia
 * 
 */
public class ControllerManager {
	private LinkedHashMap<String, Controller> controllerList = new LinkedHashMap<String, Controller>();
	
	public ControllerManager() {
		fetchControllers();
	}

	public void fetchControllers() {
		// Copy current controllers to oldMap
		LinkedHashMap<String, Controller> oldMap = new LinkedHashMap<String, Controller>();
		for (String s : controllerList.keySet()) {
			oldMap.put(s, controllerList.get(s));
		}

		controllerList.clear();

		// Fectch controllers list
		Controller controllers[] = new JoyEnvironment().getControllers();

		// Create new controllerMap
		for (Controller c : controllers) {
		    if( null!=c && null!=c.getName() ) // Protect from rare case where a Controller has a null name
			if(!c.getName().toLowerCase().contains("keyboard") && !c.getName().toLowerCase().contains("mouse"))
			    if(c.getType() == Controller.Type.GAMEPAD || c.getType() == Controller.Type.STICK)
				controllerList.put(c.getName(), c);
		}

		// Look for changes
		for (String k : oldMap.keySet()) {
			if (!controllerList.containsKey(k)) {
				NeptusLog.pub().info("Removed " + oldMap.get(k).getName());
			}
		}
		for (String k : controllerList.keySet()) {
			if (!oldMap.containsKey(k)) {
				NeptusLog.pub().info("Added " + controllerList.get(k).getName());
			}
		}
	}

	public LinkedHashMap<String, Component> pollController(Controller c) {
		LinkedHashMap<String, Component> pollResult = new LinkedHashMap<String, Component>();
		
		// In case of failed device poll NULL should be returned for error capture
		if(!c.poll())
		    return null;

		for (Component comp : c.getComponents()) {
			pollResult.put(comp.getName(), comp);
		}
		return pollResult;
	}

	public LinkedHashMap<String, Component> pollController(String device) {
		Controller c = controllerList.get(device);
		return pollController(c);
	}

	public LinkedHashMap<String, Controller> getControllerList() {
		return controllerList;
	}
}
