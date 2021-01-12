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
 * Author: Paulo Dias
 * 15/04/2011
 */
package pt.lsts.neptus.console.plugins.planning;

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

import pt.lsts.neptus.util.GuiUtils;

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
