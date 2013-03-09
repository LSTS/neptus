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
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.util.output;

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

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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
		
		public FilteredStream(OutputStream aStream, int type) {
			super(aStream);
			this.type = type;
		}
		
		public void write(byte b[]) throws IOException {
			if (mutedThreads.contains(Thread.currentThread()))
			    return;
		    
		    super.write(b);
			String aString = new String(b);		
			
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
		
		public void write(byte b[], int off, int len) throws IOException {
		    if (mutedThreads.contains(Thread.currentThread()))
                return;
		    
			super.write(b, off, len);
			String aString = new String(b , off , len);
			
			if (lastOutputType != type && lastOutputType != -1) {
				htmlOut.append("</pre></font>\n");
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
		ConfigFetch.initialize();
		
		new OutputMonitor();
		System.out.println("dsd");
		
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
