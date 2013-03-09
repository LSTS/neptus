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
 */
package pt.up.fe.dceg.neptus.env;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Condition {

	private String conditionText = "false";
	private Environment env = new Environment();
	
	public Condition(String conditionText) {
		setConditionText(conditionText);
	}
	
	public Object evaluate() {
		Scriptable scope = env.getScope();
		Context cx = Context.enter();
		Object result = cx.evaluateString(scope, conditionText, "<condition>", 0, null);
		return result;
	}
	
	public Object evaluate(Environment env) {
		setEnv(env);
		return evaluate();
	}
	
	public String getConditionText() {
		return conditionText;
	}

	public void setConditionText(String conditionText) {
		this.conditionText = conditionText;
	}

	public Environment getEnv() {
		return env;
	}

	public void setEnv(Environment env) {
		this.env = env;
	}
	
}
