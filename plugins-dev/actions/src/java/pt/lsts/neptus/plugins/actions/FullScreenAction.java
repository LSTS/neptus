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
 * Author: José Pinto
 * 2009/06/11
 */
package pt.lsts.neptus.plugins.actions;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.JFrame;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author ZP
 *
 */
@PluginDescription(icon="pt/lsts/neptus/plugins/actions/full_toggle.png", name="Full Screen Toggle", description="Toggle fullscreen mode")
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
