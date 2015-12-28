/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * 03-01-2007
 */
package pt.lsts.neptus.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Vector;

import pt.lsts.neptus.NeptusLog;


/**
 * @author zp
 */
public class GenerateLaunchers {
	
	private static String srcBatFile = "./srcShell/neptus.bat";
	private static String srcShFile = "./srcShell/neptus.sh";
	
	private static String dstBatFile = "./neptus.bat";
	private static String dstShFile = "./neptus.sh";
	
	private static String replaceString = "@NEPTUS_LIBS@";
	private static String libsDir = "lib";
	private static Vector<String> ignoreDirs = new Vector<String>();
	
	static {
		ignoreDirs.add(".svn");
	}
	
	public static void main(String[] args) throws Exception {

		File f = new File(libsDir);
		
		String windowsLibs = listContents(libsDir, f, ';');		
		String batContents = readFile(srcBatFile);		
		batContents = batContents.replaceFirst(replaceString, windowsLibs);		
		
		NeptusLog.pub().info("<###>NEPTUS.BAT CONTENTS:\n"+batContents+"\n\n");		
		writeFile(dstBatFile, batContents);
		
		String linuxLibs = listContents(libsDir, f, ':');
		String shContents = readFile(srcShFile);
		shContents = shContents.replaceFirst(replaceString, linuxLibs);
		NeptusLog.pub().info("<###>NEPTUS.SH CONTENTS:\n"+shContents+"\n");
		
		writeFile(dstShFile, shContents);
		
	}
	
	private static String readFile(String filename) throws Exception {
		String contents = "";
		
		/*
        BufferedReader br = new BufferedReader(new FileReader(filename));		
		String line = br.readLine();
		while (line != null) {
			contents += line+"\n";
			line = br.readLine();
		}
		br.close();
        */
        
        FileInputStream fis = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int len;
        byte[] ba = null;

        fis = new FileInputStream (filename);
        ba = new byte[1024];
        while ((len = fis.read(ba)) > 0) 
        {
            bos.write(ba, 0, len);
        }
        ba = bos.toByteArray();
        
        contents = new String (ba);
        fis.close();
        
		return contents;
	}
	
	private static void writeFile(String filename, String contents) throws Exception {
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename));		
		bw.write(contents);
		bw.close();
	}
	
	private static String listContents(String dirPrefix, File dir, char separator) {
		String sp = System.getProperty("file.separator");
		String contents = "";
		if (dir.canRead() && dir.isDirectory()) {
			for (String subDir : dir.list()) {
				
				if (ignoreDirs.contains(subDir))
					continue;
				
				File entry = new File(dir.getAbsoluteFile()+sp+subDir);
				
				if (entry.canRead() && entry.isDirectory()) {
					
					contents += listContents(dirPrefix+"/"+subDir, entry, separator);
				}
				else {
					contents += separator+dirPrefix+"/"+subDir;
				}
			}
		}
		
		return contents;
	}
}
