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
