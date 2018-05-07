/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * May 5, 2018
 */
package pt.lsts.neptus.endurance;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription
public class SoiAwareness extends ConsoleInteraction {

    @Override
    public void initInteraction() {

    }

    @Override
    public void cleanInteraction() {

    }
    
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
        Container parent = source.getParent();
        while (parent != null && !(parent.getLayout() instanceof BorderLayout)) 
            parent = parent.getParent();
        if (mode) {
            JPanel panel = new JPanel(new BorderLayout());
            //panel.add(slider, BorderLayout.CENTER);
            //panel.add(minTimeLabel, BorderLayout.WEST);
            //panel.add(maxTimeLabel, BorderLayout.EAST);
            parent.add(panel, BorderLayout.SOUTH);
        }
        else {
            //parent = slider.getParent().getParent();
            //parent.remove(slider.getParent());
        }
        parent.invalidate();
        parent.validate();
        parent.repaint();
    }

    
    public static void main(String[] args) {
        GuiUtils.setLookAndFeel();
        JSlider slider = new JSlider(-240, 240, 0);
        Dictionary<Integer, JLabel> labels = new Hashtable<>();
        for (int i = -12; i <= 12; i++)
            labels.put(i*20, new JLabel(""+i));
        slider.setMajorTickSpacing(20);        
        slider.setPaintTicks(true);
        
        slider.setLabelTable(labels);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(false);
        GuiUtils.testFrame(slider);
    }
}
