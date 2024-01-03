/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.gui.properties;

import java.util.Vector;

public class BoundedNumericalProperty extends VerifyingProperty {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    private double minVal, maxVal;
	
	public BoundedNumericalProperty(double minVal, double maxVal) {
		this.maxVal = maxVal;
		this.minVal = minVal;
	}
	
	@Override
	public Vector<String> verifyErrors(Object value) {
		Vector<String> errors = new Vector<String>();
		
		if (value instanceof Number) {
			double val = ((Number)value).doubleValue();
			
			if (val < minVal) { 
				errors.add("The minimum allowed value for the property "+getName()+" is "+minVal);				
				return errors;
			}
			if (val > maxVal) {
				errors.add("The maximum allowed value for the property "+getName()+" is "+maxVal);				
				return errors;
			}
				
			
			return errors;
		}
		errors.add("The property "+getName()+" must have a numerical value");		
		return errors;
	}

	public double getMinVal() {
		return minVal;
	}

	public void setMinVal(double minVal) {
		this.minVal = minVal;
	}

	public double getMaxVal() {
		return maxVal;
	}

	public void setMaxVal(double maxVal) {
		this.maxVal = maxVal;
	}

}
