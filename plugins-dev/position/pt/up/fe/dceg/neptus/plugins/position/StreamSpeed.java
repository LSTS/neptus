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
 * Jun 14, 2010
 * $Id:: StreamSpeed.java 9615 2012-12-30 23:08:28Z pdias                       $:
 */
package pt.up.fe.dceg.neptus.plugins.position;

import java.awt.BorderLayout;
import java.text.NumberFormat;

import javax.swing.JLabel;

import org.jdesktop.swingx.JXLabel;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name="Stream Speed Display", author="zp", icon="pt/up/fe/dceg/neptus/plugins/position/position.png")
public class StreamSpeed extends SimpleSubPanel implements NeptusMessageListener, ConfigurationListener {
	
	
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
