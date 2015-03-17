/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Feb 21, 2005
 */
package pt.lsts.neptus.util;

import java.util.GregorianCalendar;
import java.util.Random;

/**
 * @author zecarlos
 */
public class NameNormalizer {
	
	private static Random rnd = new Random(System.currentTimeMillis()); 
	
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
        StringBuilder builder = new StringBuilder(9);
        builder.append(prefix);
        builder.append("_");        
        int val = Math.abs(rnd.nextInt());        
        builder.append(Integer.toString(val, 36));
        return builder.toString();
    }    
    
    public static void main(String args[]) {
        System.out.println(getRandomID());
    }
}
