/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zepinto
 * 2010/01/21
 */
package pt.up.fe.dceg.neptus.mp.templates;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author zepinto
 *
 */
public class ScriptedPlanTemplate extends AbstractPlanTemplate {

	protected ScriptEngineManager manager = new ScriptEngineManager();
	protected ScriptEngine engine = manager.getEngineByName("js");
	protected CompiledScript script = null;
	protected String source = "";
	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}



	protected LinkedHashMap<String, Object> properties = new LinkedHashMap<String, Object>();
	
	
	
	private String commonScript =
		
		"importPackage(java.util);\n" +
		"function param(name, value) {\n"+
			"if (mode == 0)\n"+
				"properties.put(name, value);\n"+
			"return properties.get(name);\n"+			
		"}\n"+
		
		"function move(north, east, down) {\n"+
			"plan.move(north, east, down);\n"+
		"}\n"+
		
		"function maneuver(name, props) {\n"+
			"if (props)\n"+
				"return plan.addManeuver(name, props);\n"+
			"else\n"+
				"return plan.addManeuver(name);\n"+
		"}\n";
	
	@Override
	public PlanType generatePlan() throws Exception {
		
		if (script == null)
			script = ((Compilable)engine).compile(commonScript+source);
		
		
		
		PlanCreator planCreator = new PlanCreator(mission);
		Bindings bindings = engine.createBindings();
		bindings.put("properties", properties);
		bindings.put("mode", 1);
		bindings.put("mission", mission);
		bindings.put("plan", planCreator);
		script.eval(bindings);
		return planCreator.getPlan();
	}
	
	@Override
	public DefaultProperty[] getProperties() {
		
		LinkedHashMap<String, Object> properties = new LinkedHashMap<String, Object>();
		try {
			if (script == null)
				script = ((Compilable)engine).compile(source+commonScript);
			
			PlanCreator planCreator = new PlanCreator(mission);
			Bindings bindings = engine.createBindings();
			bindings.put("mode", 0);
			bindings.put("mission", mission);
			bindings.put("plan", planCreator);
			bindings.put("properties", properties);
			
			script.eval(bindings);			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		Vector<DefaultProperty> props = new Vector<DefaultProperty>();
		
		for (String name : properties.keySet()) {
			Object value = properties.get(name);
			if (!name.equals("name"))
				props.add(PropertiesEditor.getPropertyInstance(name, value.getClass(), value, true));
		}
		
		return props.toArray(new DefaultProperty[0]);
	}
	
	@Override
	public void setProperties(Property[] properties) {
		for (Property p : properties) {
			this.properties.put(p.getName(), p.getValue());
		}
	}
	
	
	public static void main(String[] args) {
		ConfigFetch.initialize();
		MissionType mt = new MissionType("missions/APDL/missao-apdl.nmisz");
		
		String script = FileUtil.getFileAsString(new File("conf/planscripts/rows.js")); 
		
		ScriptedPlanTemplate planTemplate = new ScriptedPlanTemplate();
		planTemplate.mission = mt;
		planTemplate.source = script;
		
		PropertiesEditor.editProperties(planTemplate, true);
	}

}
