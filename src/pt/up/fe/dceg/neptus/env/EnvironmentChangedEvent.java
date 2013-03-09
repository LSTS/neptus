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
