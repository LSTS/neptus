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
 * 2010/05/01
 */
package pt.up.fe.dceg.neptus.test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import pt.up.fe.dceg.neptus.util.ByteUtil;

/**
 * @author pdias
 * 
 */
public class TestBlowfish {

	public static String encryptBlowfish(String to_encrypt, String strkey) {
		try {
			SecretKeySpec key = new SecretKeySpec(strkey.getBytes(), "Blowfish");
			Cipher cipher = Cipher.getInstance("Blowfish");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			return new String(cipher.doFinal(to_encrypt.getBytes()));
		} catch (Exception e) {
			return null;
		}
	}

	public static String decryptBlowfish(String to_decrypt, String strkey) {
		try {
			SecretKeySpec key = new SecretKeySpec(strkey.getBytes(), "Blowfish");
			Cipher cipher = Cipher.getInstance("Blowfish");
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] decrypted = cipher.doFinal(to_decrypt.getBytes());
			return new String(decrypted);
		} catch (Exception e) {
			return null;
		}
	}

	public static void main(String[] args) {
		String resEnc = encryptBlowfish("Test Hello Fish!!", "BlowItFish");
		//System.out.println(resEnc);
		ByteUtil.dumpAsHex(resEnc.getBytes(), System.out);
		String resDec = decryptBlowfish(resEnc, "BlowItFish");
		System.out.println(resDec);
		
	}
}
