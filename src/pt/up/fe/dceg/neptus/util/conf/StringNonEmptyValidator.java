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
 * 2006/12/26
 * $Id:: StringNonEmptyValidator.java 9616 2012-12-30 23:23:22Z pdias     $:
 */
package pt.up.fe.dceg.neptus.util.conf;

/**
 * @author Paulo Dias
 *
 */
public class StringNonEmptyValidator implements Validator {

	/* (non-Javadoc)
	 * @see pt.up.fe.dceg.neptus.util.conf.Validator#validate(java.lang.Object)
	 */
	public String validate(Object newValue) {
        try
        {
        	String val = (String) newValue;
            if (val.trim().length() > 0)
                return null;
        }
        catch (Exception e)
        {
        }
        return "The value should not be empty.";
	}

	@Override
    public String validValuesDesc() {
    	String ret = "The value should not be empty.";
    	return ret;
    }
}
