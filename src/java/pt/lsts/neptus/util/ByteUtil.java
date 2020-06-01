/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 2008/04/13
 */
package pt.lsts.neptus.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/**
 * Holds some byte utilities.
 * 
 * @author pdias
 *
 */
public class ByteUtil {
	/** To avoid instantiation */
	private ByteUtil() {
	}
	
	/**
	 * Test if the two byte arrays are equal.
	 * 
	 * @param buffer1
	 * @param buffer2
	 * @return
	 */
	public static boolean equal(byte[] buffer1, byte[] buffer2) {
	    if (buffer1.length != buffer2.length)
	        return false;
	    for (int i = 0; i < buffer1.length; i++)
	        if (buffer1[i] != buffer2[i])
	            return false;
	    return true;
	}
	
	/**
	 * Dumps the byte array as hex string to the {@link PrintStream}.
	 * 
	 * @param buffer
	 * @param pStream
	 */
	public static void dumpAsHex (byte[] buffer, PrintStream pStream) {
		dumpAsHex(null, buffer, pStream);
	}
	
	/**
     * Dumps the byte array as hex string to the {@link PrintStream}, with a title.
     * 
	 * @param title
	 * @param buffer
	 * @param pStream
	 */
	public static void dumpAsHex (String title, byte[] buffer, PrintStream pStream) {
		String txt = "";
		if (!"".equalsIgnoreCase(title) && title != null) {
			txt = title + "  ";
		}
		if (buffer == null)
			txt += "NULL";
		else
			txt += "[size: " + buffer.length + " bytes]";
		pStream.println("----------------------------------------------------------  ----------------");
		pStream.println(txt);
		pStream.println("----------------------------------------------------------  ----------------");
		if (buffer == null)
			return;
		char[] chars = Hex.encodeHex(buffer);
		int charCount = 0; StringBuffer lineChars = new StringBuffer(16);
		int byteCount = 0, halfByteCount = 0, lineCount = 0;
		String regex = "[\\s\\e\\a\\x1f\0]";
		String replacement = " ";
		for (char ch : chars) {
			if (byteCount == 0 && halfByteCount == 0) {
				pStream.printf("%8X: ", lineCount*16);
			}
			pStream.print(ch);
			if (halfByteCount == 1) {
				pStream.print(' ');
				
				lineChars.append(new String(new byte[]{buffer[charCount]}).charAt(0));
				//lineChars.append(new String(".").charAt(0));
				charCount++;
			}
			if (halfByteCount == 1)
				byteCount = ++byteCount % 16;
			if (byteCount == 8 && halfByteCount == 1)
				pStream.print(' ');
			if (byteCount == 0 && halfByteCount == 1) {
				if (charCount % 16 != 0) {
					//This here will probably never be called (check)
					for (int i = 0; i < 16 - charCount % 16; i++)
						pStream.print("   ");
					if (charCount % 16 < 8)
						pStream.print(' ');
				}
				pStream.print(" "+lineChars.toString().replaceAll(regex, replacement));
				lineChars = new StringBuffer(16);
				pStream.print('\n');
				lineCount++;
			}
			halfByteCount = ++halfByteCount %2;
		}
		if (byteCount != 0) {
			if (charCount % 16 != 0) {
				for (int i = 0; i < 16 - charCount % 16; i++)
					pStream.print("   ");
				if (charCount % 16 < 8)
					pStream.print(' ');
			}
			pStream.print(" "+lineChars.toString().replaceAll(regex, replacement));
			lineChars = new StringBuffer(16);
			pStream.print('\n');
		}
		pStream.println("----------------------------------------------------------  ----------------");
	}
	
	/**
	 * Encodes the byte array into a string as hexadecimal format.
	 * 
	 * @param buffer
	 * @return
	 */
	public static String encodeToHex(byte[] buffer) {
	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < buffer.length; i++) {
	        sb.append(String.format("%02x", buffer[i]));
	    }
	    
	    return sb.toString();
	}

	/**
	 * The same as {@link #dumpAsHex(byte[], PrintStream)} but the output will be return as a String.
	 * 
	 * @param buffer
	 * @return
	 */
	public static String dumpAsHexToString (byte[] buffer) {
	    return dumpAsHexToString(null, buffer); 
	}

	/**
     * The same as {@link #dumpAsHex(String, byte[], PrintStream)} but the output will be return as a String.
     * 
	 * @param title
	 * @param buffer
	 * @return
	 */
	public static String dumpAsHexToString (String title, byte[] buffer) {
	    String addStr;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream pst = new PrintStream(baos);
            ByteUtil.dumpAsHex(title, buffer, pst);
            addStr = baos.toString();
            try {
                baos.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return addStr;
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
	}
	
	/**
	 * Return the byte array encoded as a hex string.
	 * 
	 * @param source
	 * @return
	 */
	public static String encodeAsString(byte[] source) {
	    return new String(Hex.encodeHex(source));
	}

	/**
     * Decodes the encoded hex string byte array back as byte array.
     * 
	 * @param hexString
	 * @return
	 * @throws DecoderException
	 */
	public static byte[] decodeHexString(String hexString) throws DecoderException {
	    return Hex.decodeHex(hexString.toCharArray());
	}

	/**
     * Decodes the encoded hex string byte array back as byte array.
     * 
	 * @param sourceChars
	 * @return
	 * @throws DecoderException
	 */
	public static byte[] decodeHexString(char[] sourceChars) throws DecoderException {
	    return Hex.decodeHex(sourceChars);
	}

	/**
     * Return the byte array encoded as a base64 string.
     * 
	 * @param source
	 * @param chunked
	 * @return
	 */
	public static byte[] encodeAsBase64(byte[] source, boolean chunked) {
		if (chunked)
			return Base64.encodeBase64Chunked(source);
		else
			return Base64.encodeBase64(source);
	}
	
	/**
     * Return the byte array encoded as a base64 string (chunked).
     * 
	 * @param source
	 * @return
	 */
	public static byte[] encodeAsBase64(byte[] source) {
		return encodeAsBase64(source, true);
	}
		
	/**
     * Decodes the encoded base64 byte array back as byte array.
     * 
	 * @param source
	 * @return
	 */
	public static byte[] decodeBase64(byte[] source) {
		return Base64.decodeBase64(source);
	}
	
	public static void main(String[] args) {
		dumpAsHex("Teste", "Isto é de facto \0 um teste para ver se imprime bem Hex!".getBytes(), System.out);
		
		dumpAsHex(FileUtil.getFileAsString("LICENSE.md").getBytes(), System.out);

		String fxImg = FileUtil.getResourceAsFileKeepName("images/auto-pilot.png");
		dumpAsHex( FileUtil.getFileAsString(fxImg).getBytes(), System.out);

		String tt = "\0\1Isto é de facto um teste para ver se imprime bem Hex!";
		tt = FileUtil.getFileAsString("LICENSE.md");
		System.out.println(new String(Base64.encodeBase64Chunked(tt.getBytes())));
		//dumpAsHex(tt.getBytes(), System.out);

		System.out.println(new String(Base64.decodeBase64(Base64.encodeBase64Chunked(tt.getBytes()))));

		System.err.println(dumpAsHexToString(FileUtil.getFileAsString("LICENSE.md").getBytes()));
	}
}
