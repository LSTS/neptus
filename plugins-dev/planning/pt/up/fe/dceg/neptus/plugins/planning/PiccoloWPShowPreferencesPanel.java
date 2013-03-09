/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 15/04/2011
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class PiccoloWPShowPreferencesPanel extends JPanel {

    private boolean userCanceled = false;

    //GUI
    private JCheckBox showHandoverWpBox, showExternalWpBox;
    
    /**
     * 
     */
    public PiccoloWPShowPreferencesPanel() {
        initialize();
    }
    
    /**
     * 
     */
    private void initialize() {
        showHandoverWpBox = new JCheckBox("Show Handover WP", true);
        showExternalWpBox = new JCheckBox("Show External WP", false);
        
        GroupLayout layout = new GroupLayout(this);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        this.setLayout(layout);
        
        layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createParallelGroup()
                    .addGroup(layout.createSequentialGroup()
                            .addComponent(showHandoverWpBox)
                            .addComponent(showExternalWpBox)
                            .addGap(2)))    
            );

        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup()
                                .addComponent(showHandoverWpBox)
                                .addComponent(showExternalWpBox)))    
                );

        layout.linkSize(SwingConstants.VERTICAL, showHandoverWpBox, showExternalWpBox);
        
        layout.linkSize(SwingConstants.HORIZONTAL, showHandoverWpBox, showExternalWpBox);
    }


    /**
     * 
     */
    public boolean isHandoverWPVisible() {
        return showHandoverWpBox.isSelected();
    }

    /**
     * @param value
     */
    public void setHandoverWPVisible(boolean value) {
        showHandoverWpBox.setSelected(value);
    }

    /**
     * 
     */
    public boolean isExternalWPVisible() {
        return showExternalWpBox.isSelected();
    }

    /**
     * @param value
     */
    public void setExternalWPVisible(boolean value) {
        showExternalWpBox.setSelected(value);
    }

    public boolean showEditPanel(boolean handover, boolean external,
            Window parent) {
        
        setHandoverWPVisible(handover);
        setExternalWPVisible(external);
        
        userCanceled = false;
        final JDialog dialog = new JDialog(parent, "Set Configurations");
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.add(this);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                userCanceled = true;
            }
        });
        final JButton okButton = new JButton(new AbstractAction("Ok") {
            @Override
            public void actionPerformed(ActionEvent e) {
                userCanceled = false;
                dialog.dispose();
            }
        });
        GuiUtils.reactEnterKeyPress(okButton);
        dialog.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                okButton.requestFocusInWindow();
            }
        });
        JButton cancelButton = new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                userCanceled = true;
                dialog.dispose();
            }
        });
        GuiUtils.reactEscapeKeyPress(cancelButton);
        JPanel jp = new JPanel();
        jp.setLayout(new GridLayout(0, 2));
        jp.add(okButton);
        jp.add(cancelButton);
        dialog.add(jp, BorderLayout.SOUTH);
        dialog.pack();
        GuiUtils.centerParent(dialog, parent);
        dialog.setVisible(true);

        return !userCanceled;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        JFrame parent = GuiUtils.testFrame(new JLabel("Teste"));
        new PiccoloWPShowPreferencesPanel().showEditPanel(true, true,
                parent);
    }
}
