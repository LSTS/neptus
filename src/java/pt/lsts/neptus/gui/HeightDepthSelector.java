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
 * 28/Fev/2005
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.objparams.ParametersPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Ze Carlos
 */
public class HeightDepthSelector extends ParametersPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JComboBox<String> selectorHD = null;
    private JFormattedTextField heightDepthField = null;
    private boolean editable;
    private JLabel jLabel = null;
    private boolean canceled = false;
    
    public HeightDepthSelector() {
        super();
        initialize();
    }

    /**
     * Returns a String identifying any errors existing in the filled parameters or null if they're all ok
     */
    public String getErrors() {
        try {
            Double.parseDouble(getZField().getText());
        }
        catch (Exception e) {
            return I18n.text("The Z parameters are not valid");
        }
        return null;
    }

    public double getDepth() {
        if (selectorHD.getSelectedItem().equals(I18n.text("Height")))
            return -Double.parseDouble(getZField().getText());
        else
            return Double.parseDouble(getZField().getText());
    }

    /**
     * Sets the selection depth value
     * 
     * @param depth the selected depth
     */
    public void setZ(double depth) {
        getZField().setText(String.valueOf(depth));
    }

    /**
     * This method initializes jComboBox
     * 
     * @return javax.swing.JComboBox
     */
    private JComboBox<String> getZUnitsCombo() {
        if (selectorHD == null) {
            selectorHD = new JComboBox<String>(new String[] {I18n.text("Depth"), I18n.text("Height")});
            selectorHD.setPreferredSize(new java.awt.Dimension(90, 20));
        }
        return selectorHD;
    }

    /**
     * This method initializes jTextField
     * 
     * @return javax.swing.JTextField
     */
    private JTextField getZField() {
        if (heightDepthField == null) {
            heightDepthField = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat());
            heightDepthField.setColumns(7);
            heightDepthField.addFocusListener(new SelectAllFocusListener());
        }
        return heightDepthField;
    }

    public static void main(String[] args) throws Exception {
        LocationType loc = new LocationType();
        loc.setDepth(10);
        
        HeightDepthSelector.showHeightDepthDialog(loc, I18n.text("Set plan depth / altitude"));
        
        NeptusLog.pub().info("<###> "+loc);
        
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        jLabel = new JLabel();
        this.setLayout(new FlowLayout());
        this.setBounds(0, 0, 204, 30);
        jLabel.setText("m ");

        this.add(getZField(), null);
        this.add(jLabel, null);
        this.add(getZUnitsCombo(), null);

    }

    public void setEditable(boolean value) {
        this.editable = value;
        getZUnitsCombo().setEnabled(editable);
        getZField().setEditable(editable);
    }
    
    /**
     * Presents the user with a dialog where he can change the depth / altitude of the location
     * @param loc The location to be edited in terms of Z
     * @param title The title of the dialog presented to the user
     * @return <code>true</code> if the user pressed OK or <code>false</code> otherwise.
     */
    public static boolean showHeightDepthDialog(LocationType loc, String title) {
        final HeightDepthSelector selector = new HeightDepthSelector();
        selector.setZ(loc.getAllZ());
        
        selector.setBorder(new TitledBorder(I18n.text("Value for Z")));
        final JDialog dialog = new JDialog(ConfigFetch.getSuperParentAsFrame());
        final JButton btnOk = new JButton(I18n.text("OK"));
        btnOk.setPreferredSize(new Dimension(86, 24));
        
        JButton btnCancel = new JButton(I18n.text("Cancel"));
        btnCancel.setPreferredSize(new Dimension(86, 24));
        btnCancel.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                selector.canceled = true;
                dialog.dispose();
            }
        });
        
        dialog.setLayout(new BorderLayout());
        dialog.add(selector, BorderLayout.CENTER);
        dialog.setModal(true);
        dialog.setTitle(title);
        dialog.setSize(300, 120);
        
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(btnCancel);
        controls.add(btnOk);
        
        btnOk.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {                
                dialog.dispose();
            }
        });

        dialog.add(controls, BorderLayout.SOUTH);        
        dialog.setVisible(true);
        
        if (!selector.canceled) {
            loc.setDepth(selector.getDepth());
        }
        
        return !selector.canceled;
    }

} 
