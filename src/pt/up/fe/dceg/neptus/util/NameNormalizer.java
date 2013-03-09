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
 * Feb 21, 2005
 */
package pt.up.fe.dceg.neptus.util;

import java.util.GregorianCalendar;
import java.util.Random;

/**
 * @author zecarlos
 */
public class NameNormalizer {
	
	static int count = 0;
	static Random rnd = new Random(System.currentTimeMillis()); 
	
    public static String asIdentifier(String text) {

        // Se o texto dado for null ou vazio ("") retorna o tempo corrente em milisegundos
        if (text == null || text.length() == 0) {
            GregorianCalendar cal = new GregorianCalendar();
            return "id"+String.valueOf(cal.getTimeInMillis());
        }
        
        char[] letters = text.toCharArray();
        
        if (!Character.isJavaIdentifierStart(letters[0])) {
            text = "id" + text;
            letters = text.toCharArray();
        }
        	
        for (int i = 1; i < letters.length; i++) {
            if (!Character.isJavaIdentifierPart(letters[i])) {
            	if (Character.isSpaceChar(letters[i]))
            		letters[i] = '_';
            	else {
            		
            		letters[i] = 'X';
            	}
            }
        }
        
        return new String(letters);
    }
    
    
    public static boolean isNeptusIdentifierStart(char c) {
        if (Character.isLetter(c))
            return true;
        if (c == '_')
            return true;
        return false;
        
    }

	public static boolean isNeptusIdentifierPart(char c) {
	    if (Character.isLetterOrDigit(c))
	        return true;
	    switch(c) {
	    case '_':
	        return true;
	    case '-':
	    	return true;
	    case '.':
	    	return true;
	    case ':':
	        return true;
	    default:
	        return false;
	    }
	}
	
    
    public static boolean isNeptusValidIdentifier(String identifier) {
        
        if (identifier.length() < 1)
            return false;
        if (!isNeptusIdentifierStart(identifier.charAt(0)))
            return false;
        for (int i = 1; i < identifier.length(); i++) {
            if (!isNeptusIdentifierPart(identifier.charAt(i)))
                return false;
        }
        return true;
    }
    
    public static String getRandomID() {    	
        return getRandomID("id");        
    }
    
    public static String getRandomID(String prefix) {
        return prefix + "_" + rnd.nextInt(10000) + System.currentTimeMillis()%10000;
    }    
    
    public static void main(String args[]) {
        String[] tests = new String[] {"Olá mundo", "Isto é um teste", " Ora Ora Ora", "12312+2", "!!!!"};
        for (int i = 0; i < tests.length; i++) {
            System.out.println("Test phrase: \""+tests[i]+"\", Result: \""+NameNormalizer.asIdentifier(tests[i])+"\"");
        }
    }
}
