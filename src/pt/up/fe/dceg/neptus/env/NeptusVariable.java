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
 * $Id:: NeptusVariable.java 9616 2012-12-30 23:23:22Z pdias              $:
 */
package pt.up.fe.dceg.neptus.env;

import java.util.StringTokenizer;

@SuppressWarnings("rawtypes")
public class NeptusVariable implements Comparable<NeptusVariable>{

	private Comparable value = new String("N/A");
	private String id;
	private Environment env;
	
	public NeptusVariable(String id, Comparable value) {
		this.id = id;
		this.value = value;
	}
	
	public Class getVariableClass() {
		return value.getClass();
	}
	
	public Comparable getValue() {
		return value;
	}
	
	public void setValue(Comparable value) {
		Comparable oldValue = this.value;
		this.value = value;
		if (env != null)
			env.valueHasChanged(getId(), oldValue);
	}
	
	public String getId() {
		return this.id;
	}
	
	public String toString() {
		return getLastIdPart()+" = "+value.toString();
	}
	
	public String getLastIdPart() {
		String s[] = getIdParts();
		return s[s.length-1];
	}
	
	public String[] getIdParts() {
		StringTokenizer st = new StringTokenizer(getId(), ".");
		String[] ret = new String[st.countTokens()];
		for (int i = 0; st.hasMoreTokens(); i++){
			ret[i] = st.nextToken();
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
    public int compareTo(NeptusVariable arg0) {
		if (arg0 instanceof NeptusVariable) {
			NeptusVariable other = (NeptusVariable) arg0;
			if (other.getVariableClass() == this.getVariableClass())
				return this.getValue().compareTo(other.getValue());
		}
		return 0;
	}
	
	public double getValueAsDouble() {

		if (value instanceof Double)
			return ((Double)value).doubleValue();
		
		if (value instanceof Float)
			return ((Float)value).doubleValue();

		if (value instanceof Integer)
			return ((Integer)value).doubleValue();

		if (value instanceof Long)
			return ((Long)value).doubleValue();

		if (value instanceof String)
			return Double.parseDouble((String)value);
		
		return 0;
	}

	public Environment getEnv() {
		return env;
	}

	public void setEnv(Environment env) {
		this.env = env;
	}
}
