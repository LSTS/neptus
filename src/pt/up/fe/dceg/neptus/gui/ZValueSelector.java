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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import pt.up.fe.dceg.neptus.gui.objparams.ParametersPanel;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author Ze Carlos
 */
public class ZValueSelector extends ParametersPanel {

    private static final long serialVersionUID = 1L;
    private JComboBox<Z_UNITS> selectorHD = null;
    private JFormattedTextField heightDepthField = null;
    private boolean editable;
    private JLabel jLabel = null;
    private boolean canceled = false;
    
    public ZValueSelector() {
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
            return "The Z parameters are not valid";
        }
        return null;
    }

    public double getZ() {
        return Double.parseDouble(getZField().getText());
    }

    /**
     * Sets the selection depth value
     * 
     * @param depth the selected depth
     */
    public void setZ(double z) {
        getZField().setText(String.valueOf(z));
    }
    
    public Z_UNITS getZUnits() {
        return (Z_UNITS)selectorHD.getSelectedItem();
    }
    
    public void setZUnits(Z_UNITS zunits) {
        selectorHD.setSelectedItem(zunits);
    }

    /**
     * This method initializes jComboBox
     * 
     * @return javax.swing.JComboBox
     */
    private JComboBox<Z_UNITS> getZUnitsCombo() {
        if (selectorHD == null) {
            selectorHD = new ZUnitsComboBox();
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
        ManeuverLocation loc = new ManeuverLocation();
        loc.setZ(10);
        
        ZValueSelector.showHeightDepthDialog(null, loc, "Set plan depth / altitude");
        
        System.out.println(loc.asXML());
        
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
    public static boolean showHeightDepthDialog(JComponent parentComponent, ManeuverLocation loc, String title) {
        final ZValueSelector selector = new ZValueSelector();
        selector.setZ(loc.getZ());
        selector.setZUnits(loc.getZUnits());
        
        selector.setBorder(new TitledBorder(I18n.text("Value for Z")));
        Window parent = parentComponent == null ? null : SwingUtilities.getWindowAncestor(parentComponent);
        final JDialog dialog = new JDialog(parent == null ? ConfigFetch.getSuperParentAsFrame() : parent);
        dialog.setLocationRelativeTo(dialog.getParent());
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
        controls.add(btnOk);
        controls.add(btnCancel);
        
        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {                
                dialog.dispose();
            }
        });

        dialog.add(controls, BorderLayout.SOUTH);        
        dialog.setVisible(true);
        
        if (!selector.canceled) {
            loc.setZ(selector.getZ());
            loc.setZUnits(selector.getZUnits());
        }
        
        return !selector.canceled;
    }
    
    
    @SuppressWarnings("serial")
    public class ZUnitsComboBox extends JComboBox<Z_UNITS> {
        public ZUnitsComboBox() {
            super(ManeuverLocation.Z_UNITS.values());
            this.setRenderer(new ListCellRenderer<Z_UNITS>() {
                @Override
                public Component getListCellRendererComponent(JList<? extends Z_UNITS> list, Z_UNITS value, int index,
                        boolean isSelected, boolean cellHasFocus) {
                    JLabel label = new JLabel(I18n.text(value.toString()));
                    label.setOpaque(true);
                    if (isSelected) {
                        label.setBackground(list.getSelectionBackground());
                        label.setForeground(list.getSelectionForeground());
                    }
                    else {
                        label.setBackground(list.getBackground());
                        label.setForeground(list.getForeground());
                    }
        
                    return label;
                }
            });
        }
    };
} 
