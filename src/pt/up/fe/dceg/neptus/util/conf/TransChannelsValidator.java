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
 * 2007/09/15
 * Extends StringPatternValidator on 2008/12/17
 */
package pt.up.fe.dceg.neptus.util.conf;


/**
 * @author pdias
 *
 */
public class TransChannelsValidator extends StringPatternValidator {

	public TransChannelsValidator() {
		redex = "^T2[2-9]([ ][\\(](\\w)+[\\)])?$";
	}
	
//	@SuppressWarnings("unused")
//	private TransChannelsValidator(String redex) {
//		this.redex = "^T2[2-9]([ ][\\(](\\w)+[\\)])?$";
//	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TransChannelsValidator t = new TransChannelsValidator();
		System.out.println("ok: " + t.validate("T23"));
		System.out.println("nok: " + t.validate("T23(la)"));
		System.out.println("nok: " + t.validate("t23 (la)"));
		System.out.println("ok: " + t.validate("T23 (lsts1)"));
		System.out.println("nok: " + t.validate("T55 (la)"));
		System.out.println("nok: " + t.validate("T23 ()"));
	}

}
