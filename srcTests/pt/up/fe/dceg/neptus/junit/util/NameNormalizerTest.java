/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Zé Carlos
 * 23/Fev/2005
 */
package pt.up.fe.dceg.neptus.junit.util;

import junit.framework.TestCase;
import pt.up.fe.dceg.neptus.util.NameNormalizer;

/**
 * @author Zé Carlos
 */
public class NameNormalizerTest extends TestCase {

	
	public void testGenerationOfValidIdentifiers() {
        String[] tests = new String[] { " Espaço no início.", "Olá mundo!", "Isto é um teste",
                "Um grandessíssimo identificador", "Um outro identificador que consegue ser ainda maior...", "123",
                null };
		for (int j = 0; j < tests.length; j++) {
			String id = NameNormalizer.asIdentifier(tests[j]);
			assertTrue(Character.isJavaIdentifierStart(id.charAt(0)));
			for (int i = 1; i < id.length(); i++) {
				assertTrue(Character.isJavaIdentifierPart(id.charAt(i)));
			}
			System.out.println("generated "+id);
		}
	}
	
	public static void main(String[] args) {
		junit.swingui.TestRunner.run(NameNormalizerTest.class);
	}

}
