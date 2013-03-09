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
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import pt.up.fe.dceg.neptus.env.Environment;
import pt.up.fe.dceg.neptus.env.EnvironmentBrowser;
import pt.up.fe.dceg.neptus.env.EnvironmentChangedEvent;
import pt.up.fe.dceg.neptus.env.EnvironmentListener;
import pt.up.fe.dceg.neptus.env.NeptusVariable;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import aw.gui.chart.Chart2D;
import aw.gui.chart.Trace2DLtd;
import aw.util.Range;

class MyChart2D extends Chart2D {

	private static final long serialVersionUID = 1L;
	
	public MyChart2D() {
		super();
	}
	
	private boolean antialiased = true;
	
	@Override
	public synchronized void paint(Graphics arg0) {
		Graphics2D g2d = (Graphics2D) arg0;	
		if (antialiased)
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		super.paint(g2d);
	}

	public boolean isAntialiased() {
		return antialiased;
	}

	public void setAntialiased(boolean antialiased) {
		this.antialiased = antialiased;
	}
}

class MyEnvironmentListener implements EnvironmentListener {
	Environment env = null;
	ChartPanel chartPanel;
	String[] varsToListen;
	double[] lastValues;
	long[] lastTimestamps;
	
	public MyEnvironmentListener(ChartPanel chartPanel, Environment env, String[] varsToListen) {
		this.env = env;
		this.chartPanel = chartPanel;
		this.varsToListen = varsToListen;
		this.lastValues = new double[varsToListen.length];
		this.lastTimestamps = new long[varsToListen.length];
		env.addEnvironmentListener(this);
	}
	
	public void stopListening() {
		env.removeEnvironmentListener(this);
	}
	
	public void EnvironmentChanged(EnvironmentChangedEvent event) {
		if (event.getType() == EnvironmentChangedEvent.VARIABLE_DELETED)
			return;
		String varName = event.getVariable().getId();
		for (int i = 0; i < varsToListen.length; i++) {
			String name = varsToListen[i];
			if (name.equalsIgnoreCase(varName)) {
				double newValue = event.getVariable().getValueAsDouble();
				//
				long time = System.currentTimeMillis();
					chartPanel.addValue(name, time, newValue);
					lastValues[i] = newValue;
					lastTimestamps[i] = time;
				//}
				
			}
			else {
				if (lastTimestamps[i] == 0)
					lastTimestamps[i] = System.currentTimeMillis();
				chartPanel.addValue(name, lastTimestamps[i], lastValues[i]);			
			}
		}
	}
}

public class ChartPanel extends JPanel {
	
	private MyEnvironmentListener listener = null;
	private static final long serialVersionUID = 1L;
	public MyChart2D chart2d = new MyChart2D();
	private boolean onlyOneTrace = false;
	private boolean tracesSynchronized = true;
	private LinkedHashMap<String, Double> previousValues = new LinkedHashMap<String, Double>();
	
	//private LinkedHashMap<String, ITrace2D> traces = new LinkedHashMap<String, ITrace2D>();
	
	public boolean isTracesSynchronized() {
		return tracesSynchronized;
	}

	public void setTracesSynchronized(boolean tracesSynchronized) {
		this.tracesSynchronized = tracesSynchronized;
	}
	
	private int maxPoints = 100;
	private boolean paused = false;
	private Hashtable<String, Trace2DLtd> traces = new Hashtable<String, Trace2DLtd>();

	public ChartPanel() {
		this(100);
	}
	
	protected Vector<AbstractAction> getClickActions(MouseEvent evt) {
		
		Vector<AbstractAction> actions = new Vector<AbstractAction>();
		
		if (evt.getButton() != MouseEvent.BUTTON3)
			return actions;
		
		if (!paused) {
			AbstractAction pause = new AbstractAction("Pause") {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
					paused = true;	
				}
			};			
			actions.add(pause);
		}
		else {
			AbstractAction pause = new AbstractAction("Resume") {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent e) {
					paused = false;	
				}
			};	
			actions.add(pause);
		}

		return actions;
	}
	
	public ChartPanel(int maxPoints) {
		this.maxPoints = maxPoints;
		this.setLayout(new BorderLayout());
		chart2d.setGridY(true);
		chart2d.setScaleY(true);
		chart2d.setGridX(true);
		chart2d.setScaleX(true);
		
		//chart2d.
		
		
		//chart2d.setDecimalsY(10);
		chart2d.addMouseListener(new MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent arg0) {
				JPopupMenu menu = new JPopupMenu("ChartPanel");
				
				Vector<AbstractAction> actions = getClickActions(arg0);
				
				if (actions.size() == 0)
					return;
				
				for (AbstractAction act : actions) {
					menu.add(act);
				}
				
				
				menu.show(chart2d, arg0.getX(), arg0.getY());
			}
		});
		chart2d.setOpaque(false);
		this.add(chart2d, BorderLayout.CENTER);
	}
	
	public void setOnlyOneTrace(boolean value) {
		this.onlyOneTrace = value;
	}
	
	public boolean isOnlyOneTrace() {
		return onlyOneTrace;
	}
	
	public void addValue(String traceName, double x, double y) {
		if (paused)
			return;
		
		
		if (onlyOneTrace) {
			for (Enumeration<String> e = traces.keys(); e.hasMoreElements();) {
				String key = e.nextElement();
				if (!key.equals(traceName)) {
					chart2d.removeTrace(traces.get(key));
					traces.remove(key);
				}
			}
		}
		
		Trace2DLtd tmp = traces.get(traceName);
		if (tmp == null) {
			tmp = new Trace2DLtd(maxPoints);
			tmp.setName(traceName);
			tmp.setColor(getRandomColor());
			chart2d.addTrace(tmp);
			traces.put(traceName, tmp);			
		}
		
		//traces.put(traceName, tmp); -?
		previousValues.put(traceName, y);
		
		if (onlyOneTrace) {
			tmp.addPoint(x,y);
		}
		else if (isTracesSynchronized()){
			for (String traceNm : traces.keySet()) {
				Double val = previousValues.get(traceNm);
				traces.get(traceNm).addPoint(x, (val != null)? val : 0);
				System.out.println(traceName+"="+val);
			}
		}
		else {
			traces.get(traceName).addPoint(x, y);
		}
		
	}
	
	public void addValue(String traceName, double x, double y,Color c) {
		if (paused)
			return;
		
		if (onlyOneTrace) {
			for (Enumeration<String> e = traces.keys(); e.hasMoreElements();) {
				String key = e.nextElement();
				if (!key.equals(traceName)) {
					chart2d.removeTrace(traces.get(key));
					traces.remove(key);
				}
			}
		}
		
		Trace2DLtd tmp = traces.get(traceName);
		if (tmp == null) {
			tmp = new Trace2DLtd(maxPoints);
			tmp.setName(traceName);
			//tmp.setColor(c);
			chart2d.addTrace(tmp);
			traces.put(traceName, tmp);			
		}
		tmp.setColor(c);
		//traces.put(traceName, tmp);
		tmp.addPoint(x,y);
	}
	
	public void removeTrace(String traceName) {
		if (traces.containsKey(traceName)) {
			chart2d.removeTrace(traces.get(traceName));
			traces.remove(traceName);
		}
	}
	
	private int curColor = 0;
	
	public Color getRandomColor() {
		
		Color[] colors = new Color[] {
				Color.blue,
				Color.red,
				new Color(0,170,0),								
				Color.orange,
				Color.black,
				new Color(255,0,255)
		};
		
		return colors[ curColor++ % colors.length ];
	}
	
	public void listenToEnvironment(Environment env, String[] varsToMonitor) {
		if (listener != null) {
			listener.stopListening();
		}
		listener = new MyEnvironmentListener(this, env, varsToMonitor);
	}
	
	public static void main(String[] args) {
		GuiUtils.setLookAndFeel();
		EnvironmentBrowser eb = new EnvironmentBrowser();		
		GuiUtils.testFrame(eb, "EnvironmentBrowser");
		final Environment env = new Environment();
		
		env.putEnv(new NeptusVariable("Isurus.CTD.Conductivity", new Double(12)));
		env.putEnv(new NeptusVariable("Isurus.CTD.Temperature", new Double(23.3)));
		env.putEnv(new NeptusVariable("Isurus.CTD.Depth", new Double(0.0)));
		
		eb.setEnvironment(env);
		
		Thread test = new Thread(new Runnable() {
			public void run() {
				Random rnd = new Random(System.currentTimeMillis());
				while(true) {
					try {
						Thread.sleep(50);
					} catch (Exception e) {}
					env.putEnv(new NeptusVariable("Isurus.CTD.Conductivity", new Double(rnd.nextDouble() * 20)));
					if (Math.random() > 0.3)
						env.putEnv(new NeptusVariable("Isurus.CTD.Temperature", new Double(rnd.nextDouble() * 5 + 10)));
					
				}
			}
		});
		
		test.start();
		
		final ChartPanel cpanel = new ChartPanel(100);
		cpanel.setBackground(Color.white);
		GuiUtils.testFrame(cpanel, "Chart2D");
		
		cpanel.listenToEnvironment(env, new String[] {"Isurus.CTD.Conductivity", "Isurus.CTD.Temperature"});
	}

	public void setSimple()
	{
		chart2d.setGridX(false);
		chart2d.setGridY(false);
		chart2d.setGridX(false);
		//chart2d.
	}
	
	public void setXRange(double a,double b)
	{
		
		Range range=new Range(a,b);
		chart2d.setForceXRange(range);
	}
	
	public void setYRange(double a,double b)
	{
		Range range=new Range(a,b);
		chart2d.setForceYRange(range);
	}

	public Range YRange()
	{
		return chart2d.getForceYRange();	
	}
	
	public Range XRange()
	{
		return chart2d.getForceXRange();	
	}


}
