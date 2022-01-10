/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2009/04/04
 */
package pt.lsts.neptus.util;

/**
 * @author pdias
 *
 */
public class StringUtils {

	/** To avoid instantiation */
	private StringUtils() {
	}

	/**
	 * Searches for a token in a string with spaces or comma as separator.
	 * 
	 * @param searchString
	 * @param token
	 * @return
	 */
	public static final boolean isTokenInList (String searchString, String token) {
		return isTokenInList(searchString, token, "[ ,]+");
	}

	/**
     * Searches for a token in a string with the provided splitRedex.
     * 
	 * @param searchString
	 * @param token
	 * @param splitRedex
	 * @return
	 */
	public static final boolean isTokenInList (String searchString, String token, String splitRedex) {
		String[] lt = searchString.split(splitRedex);
		for (String val : lt) {
			if (val.equalsIgnoreCase(token)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Replaces all spaces and '_' with '-'.
	 * 
	 * @param name
	 * @return
	 */
	public static final String toImcName(String name) {
		return name.toLowerCase().trim().replaceAll("[ _]", "-");
	}

	/**
	 * @see #wrapEveryNChars(String, short, int, boolean)
	 * 
	 * @param txt
	 * @param lineLength
	 * @return
	 */
	public static final String wrapEveryNChars(String txt, short lineLength) {
	    return wrapEveryNChars(txt, lineLength, -1, false);
	}

    /**
     * @see #wrapEveryNChars(String, short, int, boolean)
     * 
     * @param txt
     * @param lineLength
     * @param maxCharacters
     * @return
     */
    public static final String wrapEveryNChars(String txt, short lineLength, int maxCharacters) {
        return wrapEveryNChars(txt, lineLength, maxCharacters, false);
    }
    
    /**
     * Returns a string wrapped (inserts new lines).
     * 
     * @param txt
     * @param lineLength The maximum line length, after inserts a new line
     * @param maxCharacters Place -1 for no limit, if positive is the maximum number of characters, after that gets
     *            truncated
     * @param placeEllipsisIfCuted If is truncated, adds an ellipsis at the end
     * @return
     */
    public static final String wrapEveryNChars(String txt, short lineLength, int maxCharacters,
            boolean placeEllipsisIfCuted) {
        boolean hasLimit = true;
        if (maxCharacters <= 0)
            hasLimit = false;
	    if (lineLength <= 1) {
            if (hasLimit && txt.length() >= maxCharacters)
                return txt.substring(0, maxCharacters) + (placeEllipsisIfCuted?"...":"");
            return txt;
	    }
	    String ret = "";
	    if (lineLength >= txt.length()) {
	        if (hasLimit && txt.length() >= maxCharacters)
	            return txt.substring(0, maxCharacters) + (placeEllipsisIfCuted?"...":"");
	        return txt;
	    }
	    for (int i = 0; i < txt.length(); i+=lineLength) {
	        int end = i+ lineLength;
	        if (end > txt.length())
	            end = txt.length();
	        ret += txt.substring(i, end) + "\n";
	        if (hasLimit && ret.length() >= maxCharacters) {
	            return ret.substring(0, maxCharacters) + (placeEllipsisIfCuted?"...":"");
	        }
	    }
	    return ret;
	}
	
	public static void main(String[] args) {
		System.out.println(isTokenInList("UDP, RTPS", "UDP"));
		System.out.println(isTokenInList("UDP,RTPS", "UDP"));
		System.out.println(isTokenInList(" UDP ,  RTPS", "UDP"));
		
		String txt = "- Retrying connect"+
"4097816 [Foxtrot Multi Worker Thread Runner #70] INFO org.apache.http.impl.client.DefaultHttpClient  - Retrying connect"+
"- I/O exception (java.net.NoRouteToHostException) caught when connecting to the target host: No route to host: connect"+
"4097818 [Foxtrot Multi Worker Thread Runner #70] INFO org.apache.http.impl.client.DefaultHttpClient  - I/O exception (java.net.NoRouteToHostException) caught when connecting to the target host: No route to host: connect"+
"- Retrying connect"+
"4097818 [Foxtrot Multi Worker Thread Runner #70] INFO org.apache.http.impl.client.DefaultHttpClient  - Retrying connect xxxx END";
		System.out.println(wrapEveryNChars(txt, (short) 100));
		System.out.println("\n\n------------------------------------------------\n\n");
		System.out.println(wrapEveryNChars(txt, (short) 100, 100, true));
        System.out.println("\n\n------------------------------------------------\n\n");
        System.out.println(wrapEveryNChars(txt, (short) 100, 1000, true));
        System.out.println("\n\n------------------------------------------------\n\n");
        System.out.println(wrapEveryNChars(txt, (short) 100, 100, true));
        System.out.println(wrapEveryNChars(txt, (short) 120, 100, true));
	}
}
