/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 9/Fev/2005
 */
package pt.lsts.neptus.gui.objparams;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import pt.lsts.neptus.i18n.I18n;

/**
 * @author Zé Carlos
 */
public class DrawingParameters extends ParametersPanel {

    static final long serialVersionUID = 23874623;

    private JLabel jLabel8 = null;
    private JButton choose = null;
    private JTextField ColorField = null;
    private JCheckBox shapeCheck = null;

    /**
	 *
	 */
    public DrawingParameters() {
        super();
        initialize();
        setPreferredSize(new Dimension(400, 300));
    }

    /**
     * This method initializes jButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getChoose() {
        if (choose == null) {
            choose = new JButton();
            choose.setBounds(135, 15, 90, 25);
            choose.setText(I18n.text("Choose..."));
        }
        return choose;
    }

    public JCheckBox getShapeCheck() {
        if (shapeCheck == null) {
            shapeCheck = new JCheckBox(I18n.text("Filled shape"));
            shapeCheck.setOpaque(false);
            shapeCheck.setBounds(80, 60, 100, 25);
        }
        return shapeCheck;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    public JTextField getColorField() {
        if (ColorField == null) {
            ColorField = new JTextField();
            ColorField.setBounds(100, 15, 25, 25);
            ColorField.setEditable(true);
            ColorField.setEnabled(false);
            ColorField.setBackground(java.awt.Color.orange);
        }
        return ColorField;
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        jLabel8 = new JLabel();
        this.setLayout(null);
        this.setSize(350, 50);
        this.setBackground(java.awt.SystemColor.control);
        jLabel8.setBounds(15, 15, 50, 25);
        jLabel8.setText("Color:");
        this.add(getChoose(), null);
        this.add(getColorField(), null);
        this.add(getShapeCheck());
        getChoose().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showColorDialog();
            }
        });
        this.add(jLabel8, null);
    }

    public Color getColor() {
        return getColorField().getBackground();
    }

    public void showColorDialog() {
        Color newColor = JColorChooser.showDialog(this, I18n.text("Choose the parallel piped color"), getColorField()
                .getBackground());
        getColorField().setBackground(newColor);
    }

    public static void main(String args[]) {
        JFrame tstFrame = new JFrame("Dialog Unitary Test");
        tstFrame.setLayout(new BorderLayout());
        DrawingParameters params = new DrawingParameters();
        tstFrame.getContentPane().add(params, BorderLayout.CENTER);
        tstFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        tstFrame.setSize(350, 350);
        tstFrame.setVisible(true);

    }

    public String getErrors() {
        return null;
    }

    public void setEditable(boolean value) {
        super.setEditable(value);
        getChoose().setEnabled(isEditable());
        getShapeCheck().setEnabled(isEditable());
    }

} // @jve:decl-index=0:visual-constraint="68,42"
