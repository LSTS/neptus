package pt.up.fe.dceg.neptus.controllers;

import java.util.LinkedHashMap;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.JoyEnvironment;
import pt.up.fe.dceg.neptus.NeptusLog;

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
