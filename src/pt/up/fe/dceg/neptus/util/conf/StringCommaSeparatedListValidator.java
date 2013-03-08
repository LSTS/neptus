/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2009/04/02
 * $Id:: StringCommaSeparatedListValidator.java 9615 2012-12-30 23:08:28Z#$:
 */
package pt.up.fe.dceg.neptus.util.conf;

import java.util.HashSet;

/**
 * @author pdias
 *
 */
public class StringCommaSeparatedListValidator extends StringListValidator {

	/**
	 * @param vals
	 */
	public StringCommaSeparatedListValidator(String... vals) {
		super(vals);
	}

	@Override
	public String validate(Object newValue) {
		try {
			String comp = (String) newValue;
			String[] lt = comp.split("[ ,]+");
			if (lt.length == 0)
				return "No valid value found.";
			for (String val : lt)
			{
				if (super.validate(val) != null)
					return "No valid value found.";
			}
			HashSet<String> ve = new HashSet<String>();
			for (String val : lt) {
				if (ve.contains(val))
					return "No valid value found.";
				else
					ve.add(val);
			}
			return null;
		} catch (Exception e) {
			return e.getMessage();
		}
	}
	
	public static void main(String[] args) {
		System.out.println(new StringCommaSeparatedListValidator("UDP", "RTPS").validate(""));
		System.out.println(new StringCommaSeparatedListValidator("UDP", "RTPS").validate("UDP"));
		System.out.println(new StringCommaSeparatedListValidator("UDP", "RTPS").validate("UDP,RTPS"));
		System.out.println(new StringCommaSeparatedListValidator("UDP", "RTPS").validate("RTPS , UDP"));
		System.out.println(new StringCommaSeparatedListValidator("UDP", "RTPS").validValuesDesc());
	}
}
