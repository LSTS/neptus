/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Jun 4, 2009
 * $Id:: ConsoleScript.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

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

import pt.up.fe.dceg.neptus.imc.state.ImcSysState;
import pt.up.fe.dceg.neptus.plugins.PropertyType;

import com.l2fprod.common.beans.editor.StringPropertyEditor;

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
		//System.out.println(script);
        //System.out.println(this.variables);
		
		//String s = "function ask(str) { return javax.swing.JOptionPane.showInputDialog(str); }\n";
		compile(script);
	}
	
	private void compile(String jsScript) throws ScriptException {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
		bindings = engine.createBindings();
		
		compiledScript = ((Compilable)engine).compile(jsScript);
	}
	
	public Object evaluate(ImcSysState state) {
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
