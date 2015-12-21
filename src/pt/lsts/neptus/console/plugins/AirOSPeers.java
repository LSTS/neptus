/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Dec 21, 2015
 */
package pt.lsts.neptus.console.plugins;

import java.io.File;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.credentials.Credentials;
import pt.lsts.neptus.util.ssh.SSHUtil;

/**
 * @author zp
 *
 */
@PluginDescription(name="AirOS Peers")
public class AirOSPeers extends ConsolePanel {

    private static final long serialVersionUID = 2622193661006634171L;
    //private JFreeChart chart;
    //private TimeSeriesCollection tsc = new TimeSeriesCollection();
    private long lastUpdateMillis = 0;
    
    @NeptusProperty(name="AirOS hostname", userLevel=LEVEL.REGULAR)
    String host = "10.0.30.1";
    
    @NeptusProperty(name="AirOS SSH port", userLevel=LEVEL.REGULAR)
    int port = 22;
    
    @NeptusProperty(name="Seconds between updates", userLevel=LEVEL.REGULAR)
    int seconds = 2;
    
    @NeptusProperty(name="Credentials", userLevel=LEVEL.REGULAR)
    Credentials credentials = new Credentials(new File("conf/AirOS.conf"));
        
    @Periodic(millisBetweenUpdates=500)
    private void update() {       
        if (System.currentTimeMillis() - lastUpdateMillis < seconds * 1000)
            return;
        lastUpdateMillis = System.currentTimeMillis();
        
        Future<String> result = SSHUtil.exec(host, port, credentials.getUsername(), credentials.getPassword(), "wstalist");
        try {
            String json = result.get(5, TimeUnit.SECONDS);
            System.out.println(json);
            //TODO: parse json...
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }
    
    public AirOSPeers(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void cleanSubPanel() {
        
    }

    @Override
    public void initSubPanel() {
        
    }
}
