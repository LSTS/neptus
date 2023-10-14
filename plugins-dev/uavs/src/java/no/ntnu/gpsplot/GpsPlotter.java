/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: uav
 * Jul 14, 2015
 */
package no.ntnu.gpsplot;

import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author uav
 *
 */
@PluginDescription
public class GpsPlotter extends ConsoleLayer {

    LocationType received = null;
    
    @NeptusProperty
    public String login = "";
    
    @NeptusProperty
    public String password = "";
    
    @NeptusProperty
    public String hostname = "10.0.60.45";
    
    @NeptusProperty
    public String script = "sshpass -p @pass ssh -l @login @host cat /proc/sys/dev/ubnt_poll/gps_info";
    
    @Periodic(millisBetweenUpdates=15000)
    public void getFix() {
        String scr = script.replaceAll("@pass", password);
        scr = scr.replaceAll("@host", hostname);
        scr = scr.replaceAll("@login", login);
        
        System.out.println(scr);
        try {
            Process p = Runtime.getRuntime().exec(scr);
            StringBuilder result = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            while(line != null) {
                result.append(line + "\n");
                line = reader.readLine();
            }
            System.out.println(result);
            String parts[] = result.toString().split(","); 
            received = new LocationType(Double.parseDouble(parts[6]), Double.parseDouble(parts[7]));
            
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (received == null)
            return;
        
        Point2D screenPos = renderer.getScreenPosition(received);
        
        g.setColor(java.awt.Color.MAGENTA);
        g.fill(new Ellipse2D.Double(screenPos.getX()-5, screenPos.getY()-5, 10, 10));
    }
    
    
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    @Override
    public void initLayer() {
      
    }
    
    @Override
    public void cleanLayer() {
       
    }

}
