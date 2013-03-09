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



public class EnvironmentChangedEvent  {
	
	public static final int VARIABLE_CHANGED = 0, VARIABLE_DELETED = 1, VARIABLE_ADDED = 2;
	
	private Environment env = null;
	private NeptusVariable variable = null;
	private Object oldValue = null;
	private int type = VARIABLE_CHANGED;
	
	public EnvironmentChangedEvent(int type, Environment env, NeptusVariable var) {
		this.env = env;
		this.variable = var;	
		this.type = type;
	}

	public Environment getEnvironment() {
		return env;
	}

	public Object getOldValue() {
		return oldValue;
	}

	public NeptusVariable getVariable() {
		return variable;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Environment getEnv() {
		return env;
	}

	public void setOldValue(Object oldValue) {
		this.oldValue = oldValue;
	}
}
