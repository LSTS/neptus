/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Jan 23, 2014
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

import javax.swing.JLabel;

import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Keyboard Shortcuts", icon="pt/lsts/neptus/console/plugins/planning/keyboard.png")
@LayerPriority(priority = 200)
public class MapShortcutsLayer extends ConsoleLayer {

    private String filename = "pt/lsts/neptus/console/plugins/planning/shortcuts.html";
    private String html;
    JLabel lbl;

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        Dimension d = lbl.getPreferredSize();

        double x = (renderer.getWidth() - d.getWidth()) / 2;
        double y = (renderer.getHeight() - d.getHeight()) / 2;
        g.translate(x, y);
        lbl.setBounds((int) x, (int) y, (int) d.getWidth(), (int) d.getHeight());
        lbl.paint(g);
    }

    @Override
    public boolean userControlsOpacity() {
        return true;
    }

    @Override
    public void initLayer() {
        
        String f_loc = filename+"."+GeneralPreferences.language;
        try {
            html = StreamUtil.copyStreamToString(getClass().getClassLoader().getResourceAsStream(
               f_loc));
        }
        catch (Exception e) {
            html = StreamUtil.copyStreamToString(getClass().getClassLoader().getResourceAsStream(
                    filename));
        }
        lbl = new JLabel(html);
        lbl.setOpaque(true);
        lbl.setBackground(Color.black);
        lbl.setBackground(new Color(255,255,255,200));
    }

    @Override
    public void cleanLayer() {

    }

}
