/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: lsts
 * 9 de Out de 2013
 */
package pt.lsts.neptus.plugins.position;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.JPanel;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.DeviceState;
import pt.lsts.imc.Distance;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;

/**
 * @author jqcorreia
 *
 */
@PluginDescription(name="Distances Panel", description="Distances Panel")
@Popup(name = "Distances Panel", pos=POSITION.CENTER, width = 800, height = 600, accelerator=KeyEvent.VK_D)
public class DistancesPanel extends ConsolePanel implements IPeriodicUpdates {
    private static final long serialVersionUID = 1L;

    int w = 600;
    int h = 600;
    double max = 50.0;
    
    int centerX = w / 2;
    int centerY = h / 2;
    
    double scale = w / max;
    
    ArrayList<Point2D> pointList = new ArrayList<>();
   
    JPanel panel = new JPanel() {
        private static final long serialVersionUID = -7057170093044940039L;

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            
            g2d.setBackground(Color.BLACK);
            g2d.clearRect(0, 0, w, h);
            
            g2d.setColor(Color.YELLOW);
            g2d.drawOval(centerX-5, centerY-5, 10, 10);
            synchronized (pointList) {
                for(Point2D p : pointList) {
                    g2d.setColor(Color.RED);
                    g2d.drawOval((int)p.getX(), (int)p.getY(), 2, 2);
                }
                pointList.clear();
            }
        };
    };
    
    public DistancesPanel(ConsoleLayout console) {
        super(console);
    }


    @Override
    public void initSubPanel() {
        setLayout(new MigLayout());
        add(panel, " w 600, h 600");
    }

    @Override
    public void cleanSubPanel() {
        
    }

    @Subscribe
    public void onDistance(Distance distance) {
        System.out.println(distance);
        System.out.println(scale);
        
        
        DeviceState devState = distance.getLocation().get(0);

        double x = centerX + distance.getValue() * Math.cos(devState.getPsi()) * scale;
        double y = centerY + distance.getValue() * Math.sin(devState.getPsi()) * scale;
        System.out.println(x + " " + y);
        pointList.add(new Point2D.Double(x, y));
    }


    @Override
    public long millisBetweenUpdates() {
        return 1000;
    }


    @Override
    public boolean update() {
        revalidate();
        repaint();
        return true;
    }
}
