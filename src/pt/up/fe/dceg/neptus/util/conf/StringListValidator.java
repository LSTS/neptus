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
 * 2007/07/11
 * $Id:: StringListValidator.java 9616 2012-12-30 23:23:22Z pdias         $:
 */
package pt.up.fe.dceg.neptus.util.conf;


/**
 * @author pdias
 *
 */
public class StringListValidator implements Validator {

	protected String[] list = new String[0];
	
	/**
	 * 
	 */
	public StringListValidator(String ... vals) {
		list = vals;
	}

	public String validate(Object newValue) {
		try {
			String comp = (String) newValue;
			
			for (String val : list)
			{
				if (val.equals(comp))
					return null;
			}
		} catch (Exception e) {
			return e.getMessage();
		}
		return "No valid value found.";
	}

	@Override
	public String validValuesDesc() {
		String ret = "";
		for (String val : list) {
			ret += val + ", ";
		}
		if (list.length >= 1)
			ret = ret.substring(0, ret.length()-2);
		return ret;
	}
}
