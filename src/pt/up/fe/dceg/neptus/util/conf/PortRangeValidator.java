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
 * 2010/01/20
 * $Id:: PortRangeValidator.java 9616 2012-12-30 23:23:22Z pdias          $:
 */
package pt.up.fe.dceg.neptus.util.conf;

/**
 * @author pdias
 *
 */
public class PortRangeValidator extends StringPatternValidator {

	/**
	 * 
	 */
	public PortRangeValidator() {
		redex = "\\d{1,5}(-\\d{1,5})?(,\\ ?\\d{1,5}(-\\d{1,5})?)*";
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//StringPatternValidator t1 = new StringPatternValidator("\\d{1,5}(,\\s?\\d{1,5})*");
		//StringPatternValidator t1 = new StringPatternValidator("\\d{1,5}(-\\d{1,5})?(,\\ ?\\d{1,5}(-\\d{1,5})?)*");
		PortRangeValidator t1 = new PortRangeValidator();
		System.out.println("ok: " + t1.validate("52000"));
		System.out.println("ok: " + t1.validate("52000, 22"));
		System.out.println("ok: " + t1.validate("52000,22"));
		System.out.println("nok: " + t1.validate("52000,22,223333"));
		System.out.println("ok: " + t1.validate("52000,22,23333"));
		System.out.println("ok: " + t1.validate("52000-52003"));
	}

}
