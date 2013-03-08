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
 * $Id:: EulerAnglesPanel.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.plugins.position;

import java.awt.Color;
import java.awt.GridLayout;
import java.text.DecimalFormat;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;

/**
 * @author zp
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name="Euler Angles", icon="pt/up/fe/dceg/neptus/plugins/position/position.png", description="Displays the vehicle's orientation")
public class EulerAnglesPanel extends SimpleSubPanel implements ConfigurationListener, IPeriodicUpdates, NeptusMessageListener {

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
     * @see pt.up.fe.dceg.neptus.plugins.NeptusMessageListener#getObservedMessages()
     */
    @Override
    public String[] getObservedMessages() {
        return new String[] { "EstimatedState" };
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.NeptusMessageListener#messageArrived(pt.up.fe.dceg.neptus.imc.IMCMessage)
     */
    @Override
    public void messageArrived(IMCMessage message) {
        displayRoll.setText(formatter.format(Math.toDegrees(message.getDouble("phi")))+(""+CHAR_DEGREE));
        displayPitch.setText(formatter.format(Math.toDegrees(message.getDouble("theta")))+(""+CHAR_DEGREE));
        displayYaw.setText(formatter.format(Math.toDegrees(message.getDouble("psi")))+(""+CHAR_DEGREE));
        lastUpdate = System.currentTimeMillis();
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
