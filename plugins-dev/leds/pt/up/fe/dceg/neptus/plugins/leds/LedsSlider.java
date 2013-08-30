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
 * Author: hfq
 * Aug 30, 2013
 */
package pt.up.fe.dceg.neptus.plugins.leds;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;


/**
 * @author hfq
 *
 */
public class LedsSlider extends JPanel implements ChangeListener {

    private static final long serialVersionUID = 1L;

    private JSlider slider;
    
    private JLabel sliderLabel;
    
    public LedsSlider(String name) {
        //super();
        this.setLayout(new MigLayout());
        //this.setSize(600, 1000);
        createSlider(name);
    }
    
    public void createSlider(String name) {
        sliderLabel = new JLabel(name, JLabel.CENTER);
        sliderLabel.setBackground(Color.DARK_GRAY);
        sliderLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 0));
        //sliderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        slider = new JSlider(JSlider.HORIZONTAL, LedsControlPanel.LED_MIN_BRIGHTNESS, LedsControlPanel.LED_MAX_BRIGHTNESS, LedsControlPanel.LED_INIT_BRIGHTNESS);
        slider.setAlignmentX(Component.RIGHT_ALIGNMENT);
        slider.setValue(LedsControlPanel.LED_INIT_BRIGHTNESS);
        slider.setToolTipText(name + " Brightness Controller");
        
        slider.addChangeListener(this);
        
        slider.setMajorTickSpacing(20);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        Font fontSlider = new Font("Serif", Font.ITALIC, 8);
        slider.setFont(fontSlider);
        
        this.add(sliderLabel);
        this.add(slider);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        
    }
}
