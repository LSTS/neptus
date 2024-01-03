/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.comm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.ProgressMonitor;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

public class HTTPUtils {

	
	private static LinkedHashMap<String, String> cache = new LinkedHashMap<String, String>();
	private static ReentrantLock lock = new ReentrantLock();
	
	public static String post(String url, String content) {
		try {
			URLConnection conn = new URL(url).openConnection();
			conn.setDoOutput(true);
			
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			bw.write(content);
			bw.close();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String result = "";
			String line = reader.readLine();
			while (line != null) {
				result = result.concat(line+"\n");
				line = reader.readLine();
			}
			return result;
		}
		catch (Exception e) {
			e.printStackTrace();
			return "ERROR";
			
		}
	}
	
	
	public static String getUsingCache(String url) {
		if (!cache.containsKey(url))
			cache.put(url, get(url));

		return cache.get(url); 
	}
	
	
	public static String get(String url) {
		
			
		lock.lock();
		String result = "";
		
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			result = "";
			String line = reader.readLine();
			while (line != null) {
				result = result.concat(line+"\n");
				line = reader.readLine();
			}
			lock.unlock();
			return result;
		}
		catch (Exception e) {			
			NeptusLog.pub().error("Error in HTTPUtils.get("+url+")", e);
		}
		lock.unlock();
		return null;
	}
	
	public static String put(String url, String content) {
		HttpURLConnection conn = null;
		
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setDoOutput(true);			
			conn.setRequestMethod("PUT");
			
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
			bw.write(content);
			bw.flush();
			bw.close();
			
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String result = "["+conn.getResponseCode()+"] ";
			String line = reader.readLine();
			while (line != null) {
				result = result.concat(line+"\n");
				line = reader.readLine();
			}
			conn.disconnect();
			return result;
		}
		catch (Exception e) {
			e.printStackTrace();
			if (conn != null)
				conn.disconnect();
			
			return null;			
		}
	}
	
	
	
	public static boolean isValidURL(String url) {
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(3000);
			conn.connect();
			conn.disconnect();
			return conn.getResponseCode() >= 200 && conn.getResponseCode() < 300;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
			
		}
	}
	
	public static void main(String[] args) {
		//NeptusLog.pub().info("<###> "+isValidURL("http://www.iol.pt/nota.txt"));
		//NeptusLog.pub().info("<###> "+get("http://localhost:8080/dune/config/list"));
		//NeptusLog.pub().info("<###> "+getRemoteFileLength("http://whale.fe.up.pt/"));
		
		NeptusLog.pub().info("<###> "+downloadFile("http://www.mirrorservice.org/sites/download.eclipse.org/eclipseMirror/technology/epp/downloads/release/20070702/eclipse-java-europa-win32.zip", 
				"files/downloads/file.zip"));
	}
	
	public static long getRemoteFileLength(String url) {
		try {		    
			HttpURLConnection connection;
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setConnectTimeout(3000);			
			connection.connect();
			long length = connection.getContentLength();
			connection.disconnect();
			return length;
		}		
		catch (Exception e) {
		    e.printStackTrace();
		}
		return -1;
	}
	
	public static String getPrettySizeString(long bytes) {
		// See MathMiscUtils.parseToEngineeringRadix2Notation and parseEngineeringRadix2ModeToDouble and getEngRadix2Multiplier
		String[] sizes = {"B", "KiB", "MiB", "GiB", "TiB", "PiB", "You wish bytes"};
		double size = (double) bytes;
		int sizeIndex = 0;
		while (size > 1024) {
			sizeIndex++;
			size /= 1024.;
		}
		if (Math.floor(size) != size)
			return GuiUtils.getNeptusDecimalFormat(1).format(size)+" "+sizes[sizeIndex];
		else
			return GuiUtils.getNeptusDecimalFormat(0).format(size)+" "+sizes[sizeIndex];
	}
	
	
	public static String downloadFile(String url, String destination) {
		
		NumberFormat df = DecimalFormat.getInstance(Locale.US);
		df.setGroupingUsed(false);
		df.setMaximumFractionDigits(2);
		if (!isValidURL(url)) {			
			return "The URL '"+url+"' is not available.";
		}
		long contentLength = getRemoteFileLength(url);
		if (contentLength == -1) {
			NeptusLog.pub().warn("HTTPUtils.download('"+url+"'): Unknown file length. Resuming is disabled.");
		}
		else {
			NeptusLog.pub().info("Downloading '"+url+"' ("+getPrettySizeString(contentLength)+" bytes) to "+destination);
		}
		ProgressMonitor mon = null;
		if (contentLength > 4096) {
			mon = new ProgressMonitor(ConfigFetch.getSuperParentFrame(), "Downloading '"+url+"'", "Downloaded 0 bytes (0%)", 0, (int) contentLength);
		}
		
		File destFile = new File(destination);		
		if (destFile.exists() && destFile.canRead()) {			
			NeptusLog.pub().info("HTTPUtils.download('"+url+"'): Destination file will be overwritten.");			
		}
		else {			
			destFile.getParentFile().mkdirs();			
		}
		
		
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(3000);
			conn.connect();
			int bytesRead = 0;
			boolean eof = false;
			byte[] buffer = new byte[4096];
			
			BufferedInputStream stream = new BufferedInputStream(conn.getInputStream());
			BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(destFile));
			while (bytesRead < contentLength && !eof) {
				if (mon != null && mon.isCanceled())
					break;
								
				int bytes = stream.read(buffer);
				if (bytes == -1) {
					eof = true;
					if (bytesRead < contentLength) {
						NeptusLog.pub().info("<###>Connection closed before getting entire file ("+bytesRead+" bytes read, expecting "+contentLength+")");
					}
				}
				else {
					bytesRead += bytes;
					outStream.write(buffer, 0, bytes);
					if (mon != null) {
						mon.setProgress(bytesRead);
						double percent = (double) bytesRead / (double) contentLength;						
						mon.setNote("Downloaded "+getPrettySizeString(bytesRead)+" ("+df.format(percent*100)+"%)");
					}
				}						
			}
			outStream.close();
			conn.disconnect();
			NeptusLog.pub().info("Downloaded '"+url+"' to '"+destination+"'");
			return null;
		}
		catch (Exception e) {			
			e.printStackTrace();
			return e.getClass().getSimpleName()+" thrown while downloading ("+e.getMessage()+")";
			
		}
	}
}
