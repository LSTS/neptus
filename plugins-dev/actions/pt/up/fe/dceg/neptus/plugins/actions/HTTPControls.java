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
package pt.up.fe.dceg.neptus.plugins.actions;

import java.awt.event.ActionEvent;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.ws.WebServer;

/**
 * @author zepinto
 * 
 */
@PluginDescription(name = "HTTP Server Control", author = "ZP", description = "Start and Stop local web server")
public class HTTPControls extends SimpleAction implements ConfigurationListener {

    private static final long serialVersionUID = 1L;
    @NeptusProperty(name = "HTTP Server port")
    public int port = WebServer.getPort();

    /**
     * @param console
     */
    public HTTPControls(ConsoleLayout console) {
        super(console);
    }

    @Override
    protected boolean isSwitch() {
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isSelected()) {
            WebServer.start(port);
        }
        else {
            WebServer.stop();
        }
    }

    @Override
    public void propertiesChanged() {
        System.out.println(port);
        if (port != WebServer.getPort() && isSelected()) {
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
