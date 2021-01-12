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
 * 09/05/2017
 */
package pt.lsts.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.swing.LookAndFeelTweaks;

import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author zp
 *
 */
public class SpeedEditor extends AbstractPropertyEditor {
    protected JTextField textField = new JTextField();
    protected JComboBox<SpeedType.Units> units = new JComboBox<>();
    SpeedType speed = new SpeedType();

    public SpeedEditor() {
        textField.setEditable(true);
        
        editor = new JPanel(new BorderLayout(0, 0));
        ((JPanel) editor).add(textField, BorderLayout.CENTER);
        
        if (!GeneralPreferences.forceSpeedUnits) {
            for (SpeedType.Units u : SpeedType.Units.values())
                units.addItem(u);
            ((JPanel) editor).add(units, BorderLayout.EAST);
            units.setSelectedItem(speed.getUnits());
        }
        else {
            units.addItem(GeneralPreferences.speedUnits);
            units.setSelectedItem(GeneralPreferences.speedUnits);
        }

        

        textField.setBorder(LookAndFeelTweaks.EMPTY_BORDER);

        units.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SpeedType oldSpeed = new SpeedType(speed);
                Units u = (Units) units.getSelectedItem();
                speed.setUnits(u);
                firePropertyChange(oldSpeed, speed);
                textField.setText(GuiUtils.getNeptusDecimalFormat(2).format(speed.getValue()));
            }
        });

        textField.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SpeedType oldSpeed = new SpeedType(speed);
                try {
                    double val = Double.parseDouble(textField.getText());
                    SpeedType newSpeed = new SpeedType(val, speed.getUnits());
                    speed = newSpeed;
                    firePropertyChange(oldSpeed, speed);
                    textField.getFocusCycleRootAncestor().transferFocus();
                }
                catch (Exception ex) {

                }
                
            }
        });

        textField.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {

            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                try {
                    double val = Double.parseDouble(textField.getText());
                    SpeedType newSpeed = new SpeedType(val, speed.getUnits());
                    speed = newSpeed;
                }
                catch (Exception ex) {

                }
            }

            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                // TODO Auto-generated method stub

            }
        });
    }

    public Object getValue() {
        return speed;
    }

    public void setValue(Object arg0) {
        if (arg0 instanceof SpeedType) {
            this.speed = new SpeedType((SpeedType) arg0);
            if (GeneralPreferences.forceSpeedUnits)
                this.speed.convertTo(GeneralPreferences.speedUnits);
            
            textField.setText(GuiUtils.getNeptusDecimalFormat(2).format(speed.getValue()));
            units.setSelectedItem(speed.getUnits());
        }
    }

    public static void main(String[] args) {
    }
}
