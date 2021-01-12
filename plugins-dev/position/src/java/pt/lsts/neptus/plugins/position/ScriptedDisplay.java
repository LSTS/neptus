/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2009/06/03
 */
package pt.lsts.neptus.plugins.position;

import java.awt.BorderLayout;
import java.awt.Color;

import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.AlarmProviderOld;
import pt.lsts.neptus.console.plugins.ConsoleScript;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name="Generic Display", icon="pt/lsts/neptus/plugins/position/position.png", description="A display that updates itself according to a script (Javascript)")
public class ScriptedDisplay extends ConsolePanel implements IPeriodicUpdates, ConfigurationListener, AlarmProviderOld {

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
				catch (Exception e) {
				    e.printStackTrace();
				}
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
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
