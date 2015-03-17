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
 * Author: Paulo Dias
 * 2010/05/01
 */
package pt.lsts.neptus.test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.ByteUtil;

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
		//NeptusLog.pub().info("<###> "+resEnc);
		ByteUtil.dumpAsHex(resEnc.getBytes(), System.out);
		String resDec = decryptBlowfish(resEnc, "BlowItFish");
		NeptusLog.pub().info("<###> "+resDec);
		
	}
}
