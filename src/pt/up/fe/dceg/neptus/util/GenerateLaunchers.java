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
 * 03-01-2007
 * $Id:: GenerateLaunchers.java 9616 2012-12-30 23:23:22Z pdias           $:
 */
package pt.up.fe.dceg.neptus.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Vector;


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
		
		System.out.println("NEPTUS.BAT CONTENTS:\n"+batContents+"\n\n");		
		writeFile(dstBatFile, batContents);
		
		String linuxLibs = listContents(libsDir, f, ':');
		String shContents = readFile(srcShFile);
		shContents = shContents.replaceFirst(replaceString, linuxLibs);
		System.out.println("NEPTUS.SH CONTENTS:\n"+shContents+"\n");
		
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
