/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2009/06/11
 * $Id:: FullScreenAction.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.plugins.actions;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.JFrame;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;

/**
 * @author ZP
 *
 */
@PluginDescription(icon="pt/up/fe/dceg/neptus/plugins/actions/full_toggle.png", name="Full Screen Toggle", description="Toggle fullscreen mode")
public class FullScreenAction extends SimpleAction {

	/**
     * @param console
     */
    public FullScreenAction(ConsoleLayout console) {
        super(console);
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private Rectangle bounds = null;
	
	@Override
	protected boolean isSwitch() {
		return true;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gs = ge.getDefaultScreenDevice();
        
		if (!isSelected()) {
			gs.setFullScreenWindow(null);
			if (bounds != null)
				//getConsole().setVisible(false);	
				//getConsole().setUndecorated(false);
				//getConsole().setVisible(true);				
	    		getConsole().setBounds(bounds);
		}
		else {
		    try {
		        bounds = getConsole().getBounds();
		    	getConsole().setExtendedState(JFrame.MAXIMIZED_BOTH);
		    	//getConsole().setVisible(false);
		    	//getConsole().setUndecorated(true);
		    	getConsole().setBounds(gs.getDefaultConfiguration().getBounds());
		    	//getConsole().setVisible(true);
		    	getConsole().validate();       
		        gs.setFullScreenWindow(getConsole());
		        
		    }
		    catch (Exception ex) {
		    	ex.printStackTrace();			
		    } finally {
		        gs.setFullScreenWindow(null);
		    }
		}
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
