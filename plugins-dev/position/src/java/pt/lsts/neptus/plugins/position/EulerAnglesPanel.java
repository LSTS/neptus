/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.Color;
import java.awt.GridLayout;
import java.text.DecimalFormat;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;

/**
 * @author zp
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name="Euler Angles", icon="pt/lsts/neptus/plugins/position/position.png", description="Displays the vehicle's orientation")
public class EulerAnglesPanel extends ConsolePanel implements ConfigurationListener, IPeriodicUpdates, NeptusMessageListener {

	private DisplayPanel displayRoll, displayPitch, displayYaw;
	private DecimalFormat formatter = new DecimalFormat("0.00");
	public enum EnOrientation {Horizontal, Vertical};
	
	private static final char CHAR_PHI = '\u03C6'; //'\u0278';
	private static final char CHAR_THETA = '\u03B8'; //'\u03D1';
	private static final char CHAR_PSI = '\u03C8';
	private static final char CHAR_DEGREE = '\u00B0'; //º Unicode
	private long lastUpdate = 0;
	boolean connected = true;
    
	@NeptusProperty(name="Update interval", description="Interval between updates in milliseconds")
	public long millisBetweenUpdates = 100;
	
	@NeptusProperty(name="Orientation", description="How to show the displays")
	public EnOrientation orientation = EnOrientation.Vertical;
	
	@NeptusProperty(name="Font Size", description="The font size. Use '0' for automatic.")
	public int fontSize = DisplayPanel.DEFAULT_FONT_SIZE;

	
	public EulerAnglesPanel(ConsoleLayout console) {
	    super(console);
		initialize();
	}
	
	protected void initialize() {
		displayRoll = new DisplayPanel(CHAR_PHI+" roll");
		displayPitch = new DisplayPanel(CHAR_THETA+" pitch");
		displayYaw = new DisplayPanel(CHAR_PSI+" yaw");
		
		displayRoll.setFontSize(fontSize);
		displayPitch.setFontSize(fontSize);
		displayYaw.setFontSize(fontSize);
		
		removeAll();
		if (orientation == EnOrientation.Horizontal)
			setLayout(new GridLayout(1,0));
		else
			setLayout(new GridLayout(0,1));
		
		add(displayRoll);
		add(displayPitch);
		add(displayYaw);
	}
	
	@Override
	public void propertiesChanged() {
		displayRoll.setFontSize(fontSize);
		displayPitch.setFontSize(fontSize);
		displayYaw.setFontSize(fontSize);

		initialize();
		invalidate();
		revalidate();
	}
	
	@Override
	public long millisBetweenUpdates() {
		return 500;
	}
	
	
	@Override
	public boolean update() {
		
		if (connected && System.currentTimeMillis() - lastUpdate > 3000 ) {
			displayRoll.setFontColor(Color.red.darker());
			displayPitch.setFontColor(Color.red.darker());
			displayYaw.setFontColor(Color.red.darker());
			connected = false;
		}
		
		if (!connected && System.currentTimeMillis() - lastUpdate < 3000) {
			displayRoll.setFontColor(Color.black);
			displayPitch.setFontColor(Color.black);
			displayYaw.setFontColor(Color.black);
			connected = true;
		}
		return true;
	}

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.NeptusMessageListener#getObservedMessages()
     */
    @Override
    public String[] getObservedMessages() {
        return new String[] { "EstimatedState" };
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.NeptusMessageListener#messageArrived(pt.lsts.neptus.imc.IMCMessage)
     */
    @Override
    public void messageArrived(IMCMessage message) {
        displayRoll.setText(formatter.format(Math.toDegrees(message.getDouble("phi")))+(""+CHAR_DEGREE));
        displayPitch.setText(formatter.format(Math.toDegrees(message.getDouble("theta")))+(""+CHAR_DEGREE));
        displayYaw.setText(formatter.format(Math.toDegrees(message.getDouble("psi")))+(""+CHAR_DEGREE));
        lastUpdate = System.currentTimeMillis();
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
