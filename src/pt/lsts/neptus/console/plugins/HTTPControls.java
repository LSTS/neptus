/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * 2010/01/08
 */
package pt.lsts.neptus.console.plugins;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.SimpleSubPanel;
import pt.lsts.neptus.ws.WebServer;

/**
 * @author zepinto
 *
 */
public class HTTPControls extends SimpleSubPanel implements ConfigurationListener {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name="HTTP Server port")
	public int port = WebServer.getPort();
	
	public ToolbarButton startStopServer;
	public HTTPControls(ConsoleLayout console) {
	    super(console);
		startStopServer = new ToolbarButton(new AbstractAction("Start Web Server") {
			

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent arg0) {
				
			}
		});
	}
	
	@Override
	public void propertiesChanged() {
		if (port != WebServer.getPort()) {
			WebServer.stop();
			WebServer.start(port);						
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
