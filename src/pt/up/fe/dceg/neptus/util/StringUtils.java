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
 * 2009/04/04
 */
package pt.up.fe.dceg.neptus.util;


/**
 * @author pdias
 *
 */
public class StringUtils {

	/**
	 * 
	 */
	private StringUtils() {
	}


	/**
	 * @param searchString
	 * @param token
	 * @return
	 */
	public static final boolean isTokenInList (String searchString, String token) {
		return isTokenInList(searchString, token, "[ ,]+");
	}

	/**
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
	 * @param name
	 * @return
	 */
	public static final String toImcName(String name) {
		return name.toLowerCase().trim().replaceAll("[ _]", "-");
	}

	/**
	 * @param txt
	 * @param lineLenght
	 * @return
	 */
	public static final String wrapEveryNChars(String txt, short lineLenght) {
	    return wrapEveryNChars(txt, lineLenght, -1, false);
	}

    /**
     * @param txt
     * @param lineLenght
     * @param maxCharacters
     * @return
     */
    public static final String wrapEveryNChars(String txt, short lineLenght, int maxCharacters) {
        return wrapEveryNChars(txt, lineLenght, maxCharacters, false);
    }
    
    /**
     * @param txt
     * @param lineLenght
     * @param maxCharacters Place -1 for no limit
     * @param placeElipsisIfCuted
     * @return
     */
    public static final String wrapEveryNChars(String txt, short lineLenght, int maxCharacters,
            boolean placeElipsisIfCuted) {
        boolean hasLimit = true;
        if (maxCharacters <= 0)
            hasLimit = false;
	    if (lineLenght <= 1) {
            if (hasLimit && txt.length() >= maxCharacters)
                return txt.substring(0, maxCharacters) + (placeElipsisIfCuted?"...":"");
            return txt;
	    }
	    String ret = "";
	    if (lineLenght >= txt.length()) {
	        if (hasLimit && txt.length() >= maxCharacters)
	            return txt.substring(0, maxCharacters) + (placeElipsisIfCuted?"...":"");
	        return txt;
	    }
	    for (int i = 0; i < txt.length(); i+=lineLenght) {
	        int end = i+ lineLenght;
	        if (end > txt.length())
	            end = txt.length();
	        ret += txt.substring(i, end) + "\n";
	        if (hasLimit && ret.length() >= maxCharacters) {
	            return ret.substring(0, maxCharacters) + (placeElipsisIfCuted?"...":"");
	        }
	    }
	    return ret;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(""+isTokenInList("UDP, RTPS", "UDP"));
		System.out.println(""+isTokenInList("UDP,RTPS", "UDP"));
		System.out.println(""+isTokenInList(" UDP ,  RTPS", "UDP"));
		
		String txt = "- Retrying connect"+
"4097816 [Foxtrot Multi Worker Thread Runner #70] INFO org.apache.http.impl.client.DefaultHttpClient  - Retrying connect"+
"- I/O exception (java.net.NoRouteToHostException) caught when connecting to the target host: No route to host: connect"+
"4097818 [Foxtrot Multi Worker Thread Runner #70] INFO org.apache.http.impl.client.DefaultHttpClient  - I/O exception (java.net.NoRouteToHostException) caught when connecting to the target host: No route to host: connect"+
"- Retrying connect"+
"4097818 [Foxtrot Multi Worker Thread Runner #70] INFO org.apache.http.impl.client.DefaultHttpClient  - Retrying connect xxxx";
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
