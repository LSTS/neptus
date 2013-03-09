/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 28/Fev/2005
 */
package pt.up.fe.dceg.neptus.gui;

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

import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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
        
        System.out.println(loc);
        
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
