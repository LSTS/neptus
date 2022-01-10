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
 * Jun 4, 2009
 */
package pt.lsts.neptus.console.plugins;

import java.beans.PropertyEditor;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.l2fprod.common.beans.editor.StringPropertyEditor;

import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.plugins.PropertyType;

/**
 * @author zp
 *
 */
public class ConsoleScript implements PropertyType {

	private LinkedHashMap<String, String> variables = new LinkedHashMap<String, String>();
	private String rawScript;
	private CompiledScript compiledScript;
	private Bindings bindings;
	
	@Override
	public void fromString(String value) {
		try {
			setScript(value);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String toString() {
		return rawScript;
	}

	@Override
	public Class<? extends PropertyEditor> getPropertyEditor() {
		return StringPropertyEditor.class;
	}
	
	
	public void setScript(String script) throws ScriptException {
		this.rawScript = script;
		LinkedHashMap<String, String> vars = new LinkedHashMap<String, String>();
		Pattern p = Pattern.compile("\\$\\(([\\<\\>\\.a-zA-Z0-9_ ]*)\\)");				
		Matcher m = p.matcher(script);
		
		vars.clear();
		while(m.find()) {
			String var = m.group();
			String v = var.substring(2, var.length()-1);
			
			String v2 = v.replace(".", "_");
			v2 = v2.replace("<", "_");
			v2 = v2.replace(">", "_");
			v2 = v2.replace(" ", "_");
			vars.put(v,v2);
			script = script.substring(0, m.start()) + v2 + script.substring(m.end());
			m = p.matcher(script);
		}
		
		// pattern alternative (literal)
		Pattern p2 = Pattern.compile("\\$\\(\"([^\"]*)\"\\)");                
        Matcher m2 = p2.matcher(script);
        
        //vars.clear();
        while(m2.find()) {
            String var = m2.group();
            String v = var.substring(2, var.length()-1);
            
            char[] str = v.toCharArray();
            
            for (int i = 0; i < str.length;i++) {
                if (!Character.isLetter(str[i]))
                    str[i] = '_';                                    
            }
            String v2 = String.copyValueOf(str);
            vars.put(v.replace("\"", ""),v2);
            script = script.substring(0, m2.start()) + v2 + script.substring(m2.end());
            m2 = p2.matcher(script);
        }
        
		
		this.variables = vars;
		//NeptusLog.pub().info("<###> "+script);
        //NeptusLog.pub().info("<###> "+this.variables);
		
		//String s = "function ask(str) { return javax.swing.JOptionPane.showInputDialog(str); }\n";
		compile(script);
	}
	
	private void compile(String jsScript) throws ScriptException {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
		bindings = engine.createBindings();
		
		compiledScript = ((Compilable)engine).compile(jsScript);
	}
	
	public Object evaluate(ImcSystemState state) {
		if (compiledScript == null)
			return null;
		if (state != null) {
			bindings.put("state", state);
			for (String var : variables.keySet()) {
				bindings.put(variables.get(var), state.expr(var));
			}
		}
		return evaluate();
	}
	
	public Object evaluate() {
		try {
			return compiledScript.eval(bindings);
		}
		catch (Exception e) {
			//e.printStackTrace();
			return e.getCause();
		}	
	}
	
	public Bindings getBindings() {
		return bindings;
	}

	public void setBindings(Bindings bindings) {
		this.bindings = bindings;
	}
	
	public static void main(String[] args) throws Exception {
		ConsoleScript script = new ConsoleScript();
		script.setScript("x = $(EstimatedState (ola).x)*2+34;\n"+
			"y = x * 3;"
		);
	}
}
