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
 * 2010/01/08
 */
package pt.lsts.neptus.plugins.actions;

import java.awt.event.ActionEvent;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.ws.WebServer;

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
        NeptusLog.pub().info("<###> "+port);
        if (port != WebServer.getPort() && isSelected()) {
            WebServer.stop();
            WebServer.start(port);
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
