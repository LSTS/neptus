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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.util.output;

import java.awt.BorderLayout;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.JPanel;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

public class OutputMonitor {

	private static PrintStream oldErr = System.err;
	private static PrintStream oldOut = System.out;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final int ERR = 1, OUT = 0, START = -1;
	private Vector<OutputListener> listeners = new Vector<OutputListener>();
	private Vector<Thread> mutedThreads = new Vector<Thread>();
	
	private BufferedWriter bw = null, htmlOut;
	private FilteredStream filteredOut = new FilteredStream(oldOut, OUT);
	private FilteredStream filteredErr = new FilteredStream(oldErr, ERR);
		
	private static OutputMonitor instance = null;
	
	int lastOutputType = START;
	
	private static boolean disable = false;
	
	public void muteThread(Thread t) {
	    if (!mutedThreads.contains(t))
	        mutedThreads.add(t);
	}
	
	public static boolean isDisable() {
		return disable;
	}
	
	public static void setDisable(boolean disable) {
		OutputMonitor.disable = disable;
	}
	
	public static OutputMonitor getInstance() {
	    if (instance == null)
            instance = new OutputMonitor(); 
	    return instance;
	}
	
	public static void grab() {
		if (isDisable())
			return;
		getInstance();
	}
	
	private OutputMonitor() {
		try {
			htmlOut = new BufferedWriter(new FileWriter(GuiUtils.getLogFileName("output", "html")));
			htmlOut.write("<html><head><title>Neptus Output</title></head><body><font color='green'><i>Neptus started at "+sdf.format(new Date())+"</i></font><hr>\n\n");
		}
		catch (IOException e) {
			NeptusLog.pub().error(e);
		}
		System.setOut(new PrintStream(filteredOut));
		System.setErr(new PrintStream(filteredErr));		
	}
	
	public static void end() {
		if (instance != null)
			instance.endGrabbing();
		instance = null;
	}
	
	public void endGrabbing() {
		System.setErr(oldErr);
		System.setOut(oldOut);
		try {
			if (bw != null)
				bw.close();
		}
		catch (IOException e) {
			NeptusLog.pub().error(e);
		}
		try {
			if (htmlOut != null)
			{
				htmlOut.write("</pre></font>\n<hr>\n<font color='green'><i>Neptus closed at "+sdf.format(new Date())+"</i></font>\n</body></html>");
				htmlOut.close();
			}
		}
		catch (IOException e) {
			NeptusLog.pub().error(e);
		}
	}
	
	class FilteredStream extends FilterOutputStream {
		private int type = -1;
		private String[] color = new String[] {"black", "red"};
		
//		private String infoColor = "#0000CC";
//        private String warnColor = "#FFFF00";
//        private String errorColor = "#FF9900";
//        private String fatalColor = "#FF0033";
//        private String debugColor = "#CC00CC";
//        private String traceColor = "#FF33FF";
//
//		private String brownishColor = "#FF9900";
//
//		private String infoStr  = "INFO";
//        private String warmStr  = "WARN";
//        private String errorStr = "ERROR";
//        private String fatalStr = "FATAL";
//        private String debugStr = "DEBUG";
//        private String traceStr = "TRACE";
		
		public FilteredStream(OutputStream aStream, int type) {
			super(aStream);
			this.type = type;
		}
		
		public void write(byte b[]) throws IOException {
			if (mutedThreads.contains(Thread.currentThread()))
			    return;
		    
		    super.write(b);
			String aString = new String(b);		
			
			writeToHtmlOutput(aString);
		}

		public void write(byte b[], int off, int len) throws IOException {
		    if (mutedThreads.contains(Thread.currentThread()))
                return;
		    
			super.write(b, off, len);
			String aString = new String(b , off , len);
			
//			if (lastOutputType != type && lastOutputType != -1) {
//				htmlOut.append("</pre></font>\n");
//			}
//			if (lastOutputType != type) {
//				htmlOut.append("<font color='"+color[type]+"'><pre>");
//			}
//			htmlOut.append(aString);
//			htmlOut.flush();
//			lastOutputType = type;
//			warnListeners(aString, type);
//			
//			if (bw != null) {
//				bw.write(aString);
//				bw.flush();
//			}
            writeToHtmlOutput(aString);
		}

        /**
         * @param aString
         * @throws IOException
         */
        private void writeToHtmlOutput(String aString) throws IOException {
            if (lastOutputType != type && lastOutputType != -1) {
                htmlOut.append("</pre></font>");
            }
            if (lastOutputType != type) {
                htmlOut.append("<font color='"+color[type]+"'><pre>");
            }
            htmlOut.append(aString);
            htmlOut.flush();
            lastOutputType = type;
            warnListeners(aString, type);
            
            if (bw != null) {
                bw.write(aString);
                bw.flush();
            }
        }
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//	    OutputMonitor.grab();
		ConfigFetch.initialize();
		
//		new OutputMonitor();
		NeptusLog.pub().info("<###>dsd");
		
		System.err.println("dsd asdf sdf sadf asdfa sdkfg aksjdgf ksdagfk jasdgfkjasgd fkajsgd fkjasgdkjf hgaskjdgfkasjd f\nsdasdf gasdf hgaskdjfg ksajdgfk asgdfk gaskdjfgkjashdgf " +
				"sdf sdf sadfa sdf");
	}
	
	public void addOutputListener(OutputListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}
	
	public void removeOutputListener(OutputListener listener) {
		if (listeners.contains(listener))
			listeners.remove(listener);
	}
	
	public void warnListeners(String text, int type) {
		
		if (type == ERR) {
			for (OutputListener listener : listeners)
				listener.addErr(text);
		}
		
		if (type == OUT) {
			for (OutputListener listener : listeners)
				listener.addOut(text);
		}		
	}
	
	public static void addListener(OutputListener listener) {
		if (instance != null)
			instance.addOutputListener(listener);
	}
	
	public JPanel getOutputPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		
		return panel;
	}
}
