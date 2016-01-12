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
 * Author: José Pinto
 * Jun 14, 2010
 */
package pt.lsts.neptus.plugins.position;

import java.awt.BorderLayout;
import java.text.NumberFormat;

import javax.swing.JLabel;

import org.jdesktop.swingx.JXLabel;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name="Stream Speed Display", author="zp", icon="pt/lsts/neptus/plugins/position/position.png")
public class StreamSpeed extends ConsolePanel implements NeptusMessageListener, ConfigurationListener {
	
	
	@NeptusProperty(name="Font Size", description="The font size. Use '0' for automatic.")
	public int fontSize = DisplayPanel.DEFAULT_FONT_SIZE;

	protected double angle = 0;
	protected double speed = 0;
	protected OrientationIcon icon = new OrientationIcon(20,2);
	protected DisplayPanel display = new DisplayPanel("");
	protected NumberFormat nf = GuiUtils.getNeptusDecimalFormat(2);
	
	public StreamSpeed(ConsoleLayout console) {
	    super(console);
		setLayout(new BorderLayout());
		display.setTitle("Stream Speed");
		display.setIcon(icon);
		display.setHorizontalAlignment(JLabel.LEFT);
		display.setTextAlignment(JXLabel.TextAlignment.RIGHT);
		display.setFontSize(fontSize);
		add(display, BorderLayout.CENTER);		
	}
	
	@Override
	public void propertiesChanged() {
		display.setFontSize(fontSize);
		invalidate();
		revalidate();
	}
	


    @Override
    public String[] getObservedMessages() {
        return new String[] { "EstimatedStreamVelocity" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        double x = message.getDouble("x");
        double y = message.getDouble("y");
        
        speed = Math.sqrt(x*x+y*y);
        angle = Math.atan2(x, y);
        
        icon.setAngleRadians(angle);
        display.setText("    "+nf.format(speed)+" m/s");
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
