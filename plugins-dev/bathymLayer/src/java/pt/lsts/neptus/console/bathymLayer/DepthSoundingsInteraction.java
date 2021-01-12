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
 * Author: zp
 * Nov 26, 2015
 */
package pt.lsts.neptus.console.bathymLayer;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.plugins.planning.SimulatedBathymetry;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="Depth Soundings Interaction", icon="pt/lsts/neptus/console/bathymLayer/echo.png")
public class DepthSoundingsInteraction extends ConsoleInteraction {

    @NeptusProperty(name="Default depth")
    double defaultDepth = 10;   
    
    @NeptusProperty(name="Soundings file")
    File soundingsFile = new File("soundings.txt");   
    
    
    @Override
    public void initInteraction() {
        SimulatedBathymetry.getInstance().setDefaultDepth(defaultDepth);
    }

    @Override
    public void cleanInteraction() {

    }
    
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        super.paintInteraction(g, source);
        SimulatedBathymetry.getInstance().paint(g, source);
    }
    
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        super.mouseClicked(event, source);
        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();
            popup.add(I18n.text("Add depth sounding")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LocationType loc = source.getRealWorldLocation(event.getPoint());
                    float depth = (float)SimulatedBathymetry.getInstance().getSimulatedDepth(loc);
                    String depthStr = JOptionPane.showInputDialog(getConsole(), I18n.text("Please enter depth for this location"), depth);
                    if (depthStr == null)
                        return;
                    try {
                        depth = Float.parseFloat(depthStr);
                        if (depth < 0 )
                            throw new Exception("Depth must be greater than 0");
                        
                        SimulatedBathymetry.getInstance().addSounding(loc, depth);                        
                    }
                    catch (Exception ex) {
                        GuiUtils.errorMessage(getConsole(), I18n.text("Add depth sounding"), I18n.textf("Wrong depth: %error", ex.getMessage()));
                    }
                }
            });
            
            popup.add(I18n.text("Set default depth")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    float depth = (float)SimulatedBathymetry.getInstance().getDefaultDepth();
                    String depthStr = JOptionPane.showInputDialog(getConsole(), I18n.text("Please enter depth for this location"), depth);
                    if (depthStr == null)
                        return;
                    try {
                        depth = Float.parseFloat(depthStr);
                        if (depth < 0 )
                            throw new Exception("Depth must be greater than 0");
                        
                        SimulatedBathymetry.getInstance().setDefaultDepth(depth);                   
                    }
                    catch (Exception ex) {
                        GuiUtils.errorMessage(getConsole(), I18n.text("Set default depth"), I18n.textf("Wrong depth: %error", ex.getMessage()));
                    }                    
                }
            });
            
            popup.add(I18n.text("Clear depth soundings")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SimulatedBathymetry.getInstance().clearSoundings();
                }
            });
            
            popup.show(source, event.getX(), event.getY());
        }
    }
}
