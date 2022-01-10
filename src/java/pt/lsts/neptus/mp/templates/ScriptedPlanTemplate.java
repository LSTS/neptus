/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2010/01/21
 */
package pt.lsts.neptus.mp.templates;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;

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
