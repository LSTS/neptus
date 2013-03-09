/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/06/03
 */
package pt.up.fe.dceg.neptus.plugins.position;

import java.awt.BorderLayout;
import java.awt.Color;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.AlarmProviderOld;
import pt.up.fe.dceg.neptus.console.plugins.ConsoleScript;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

/**
 * @author zp
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name="Generic Display", icon="pt/up/fe/dceg/neptus/plugins/position/position.png", description="A display that updates itself according to a script (Javascript)")
public class ScriptedDisplay extends SimpleSubPanel implements IPeriodicUpdates, ConfigurationListener, AlarmProviderOld {

	private DisplayPanel display;
	
	@NeptusProperty(name="Update interval", description="Interval between updates in milliseconds")
	public long millisBetweenUpdates = 100;
	
	@NeptusProperty(name="Title", description="Title to use")
	public String title="";
	
	@NeptusProperty(name="Font Size", description="Font size (0 for auto-adjust)")
	public int fontSize = 0;
	
	@NeptusProperty(name="Script", description="Script to run. Use $(var) to access message fields like in $(EstimatedState.Navigation.x)")
	public String script = "java.lang.System.currentTimeMillis()%10000";
	
	@NeptusProperty(name="Decimal digits", description="Decimal digits, applicable only to numeric data. Use -1 for default or text data.")
	public int digits = -1;
	
	@NeptusProperty(name="Trailing text", description="Text appended to script (optional). Can be used to set display units.")
	public String units = "";
	
	@NeptusProperty(name="Alarm condition", description="Condition to generate an alarm (optional)")
	public String alarm = "";
	
	protected int count = 0;
	
	public String validateScript(String script) {
		try {
			new ConsoleScript().setScript(script);
		}
		catch (Exception e) {
			return e.getMessage();
		}
		return null;
	}
	
	ConsoleScript conScript = new ConsoleScript();
	
	public ScriptedDisplay(ConsoleLayout console) {
	    super(console);
		initialize();
	}
	
	protected void initialize() {
		display = new DisplayPanel(title);		
		setLayout(new BorderLayout());
		add(display, BorderLayout.CENTER);	
		propertiesChanged();
	}
	
	@Override
	public long millisBetweenUpdates() {
		return millisBetweenUpdates;
	}
	
	public static int parseAlarmState(Object value) {
		if (value instanceof Boolean) {
			if ((Boolean)value) 
				return 4;
			return 0;
		}
		else if (value instanceof Number) {
			int val = ((Number)value).intValue();
			if (val > 5 || val < -2)
				val = -1;
			return val;
		}
		return -1;
	}
	
	@Override
	public void propertiesChanged() {
		count = 0;
		display.setTitle(title);
		display.setFontSize(fontSize);
		try {
			conScript.setScript(script);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected int lastState = -1;
	
	@Override
	public boolean update() {
		count++;
		try {
			String text = conScript.evaluate(ImcMsgManager.getManager().getState(getConsole().getMainSystem())).toString(); 
			if (digits > -1) {
				try {
					text = GuiUtils.getNeptusDecimalFormat(digits).format(Double.parseDouble(text));
				}
				catch (Exception e) {}
			}
			text = text + " " + units;
			display.setText(text);
			if (alarm.length() > 0) {
				if (lastState == 4)
					display.setForeground(Color.red.darker());
				else
					display.setForeground(Color.green.darker());
			}
		}
		catch (Exception e) {
			display.setText(e.getMessage());
		}
		
		if (count > 10 && alarm.length() > 0) {
			count = 0;
			int alarmState = parseAlarmState(getConsole().evaluateScript(alarm));
			
			if (alarmState != lastState) {
				lastState = alarmState;
				getMainpanel().getAlarmlistener().updateAlarmsListeners(this);
			}
		}
		return true;
	}
	
	@Override
	public int sourceState() {
		return lastState;
	}
	
	@Override
	public String getAlarmMessage() {
		if (lastState == 4) {
			if (title != null)
				return title+" = "+display.getText();
			else {
				return display.getText();
			}
		}
		return "ok";
	}
	
	@Override
	public int getAlarmState() {
		return lastState;
	}

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
