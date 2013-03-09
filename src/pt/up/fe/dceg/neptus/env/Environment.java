/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.env;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class Environment {

	private Hashtable<String,NeptusVariable> variables = new Hashtable<String,NeptusVariable>();
	private Vector<EnvironmentListener> envListeners = new Vector<EnvironmentListener>();
	
	public NeptusVariable putEnv(NeptusVariable var) {
		
		if (variables.get(var.getId()) != null) {
			variables.get(var.getId()).setValue(var.getValue());
			return variables.get(var.getId());
		}	
		
		variables.put(var.getId(), var);	
		var.setEnv(this);
		warnEnvironmentListeners(new EnvironmentChangedEvent(EnvironmentChangedEvent.VARIABLE_ADDED,this,var));
		return variables.get(var.getId());
	}
	
	public NeptusVariable getEnv(String varName) {
		return variables.get(varName);
	}
	
	
	public NeptusVariable delEnv(String varName) {
		NeptusVariable toRemove = variables.get(varName);
		if (toRemove != null) {
			variables.remove(varName);
			warnEnvironmentListeners(new EnvironmentChangedEvent(EnvironmentChangedEvent.VARIABLE_DELETED,this,toRemove));
		}
		return toRemove;
	}
	public boolean isDouble(String varName) {
		return isOfType(varName, Double.class);
	}

	public boolean isInteger(String varName) {
		return isOfType(varName, Integer.class);
	}
	
	public boolean isString(String varName) {
		return isOfType(varName, String.class);
	}
	
	public boolean isBoolean(String varName) {
		return isOfType(varName, Boolean.class);
	}
	
	public boolean isFloat(String varName) {
		return isOfType(varName, Float.class);
	}
	
	public boolean isNumeric(String varName) {
		return (isFloat(varName) || isDouble(varName) || isInteger(varName));
	}
	
	public double getAsDouble(String varName) {
		Object val = getEnv(varName);
		
		if (val instanceof Double)
			return ((Double)val).doubleValue();
		
		if (val instanceof Float)
			return ((Float)val).doubleValue();
		
		if (val instanceof Integer)
			return ((Integer)val).doubleValue();
		
		return 0;
	}
	
	public boolean isOfType(String varName, Class<?> type) {
		NeptusVariable value = getEnv(varName);
		
		if (value == null)
			return false;
		
		return value.getVariableClass().equals(type);
	}

	public String[] getVariableNames() {
		Object tmp[] = variables.keySet().toArray();
		String names[] = new String[tmp.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = (String) tmp[i];
		}		
		return names;
	}
	
	public String getType(String varName) {
		NeptusVariable value = getEnv(varName);
		if (value == null)
			return null;
		
		String classname = value.getVariableClass().toString();
		return classname.substring(classname.lastIndexOf(".")+1);
	}
	
	public void printEnv() {
		String[] names = getVariableNames();
		for (int i = 0 ; i < names.length; i++) {
			System.out.println(names[i]+" = "+getEnv(names[i])+" ("+getType(names[i])+")");
		}
	}	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Environment env = new Environment();
		env.putEnv(new NeptusVariable("Teste1", new Integer(31)));
		env.putEnv(new NeptusVariable("Teste3", "Olá"));
		env.putEnv(new NeptusVariable("Teste2", new Double(12)));
		env.printEnv();
	}
	
	public Scriptable getScope() {
		Context context = Context.enter();
		Scriptable scope = context.initStandardObjects();
		for (Enumeration<?> e = variables.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			ScriptableObject.putProperty(scope, key, variables.get(key));
		}
		return scope;
	}
	
	public Object runScript(String script){
		Scriptable scope = getScope();
		Context cx = Context.enter();
		try {
 			return cx.evaluateString(scope, script, "<script>", 0, null);
		}
		catch (Exception e) {
			System.err.println("Error parsing script: "+e.getMessage());
			return e;
		}
	}
	
	public void addEnvironmentListener(EnvironmentListener listener) {
		envListeners.add(listener);
	}
	
	public void removeEnvironmentListener(EnvironmentListener listener) {
		envListeners.remove(listener);
	}
	
	public void warnEnvironmentListeners(EnvironmentChangedEvent event) {
		for (EnvironmentListener listener : envListeners) {
			listener.EnvironmentChanged(event);
		}
	}
	
	public void valueHasChanged(String varName, Comparable<?> oldValue) {
		EnvironmentChangedEvent vce = new EnvironmentChangedEvent(EnvironmentChangedEvent.VARIABLE_CHANGED, this, getEnv(varName));
		vce.setOldValue(oldValue);
		warnEnvironmentListeners(vce);
	}
}
