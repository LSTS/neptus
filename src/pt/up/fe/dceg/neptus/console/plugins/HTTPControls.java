/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zepinto
 * 2010/01/08
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.ws.WebServer;

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
