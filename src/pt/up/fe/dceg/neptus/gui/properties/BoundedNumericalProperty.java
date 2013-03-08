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
 * $Id:: BoundedNumericalProperty.java 9616 2012-12-30 23:23:22Z pdias    $:
 */
package pt.up.fe.dceg.neptus.gui.properties;

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
