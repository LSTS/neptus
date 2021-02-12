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
 * Author: José Pinto
 * Jun 28, 2011
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
public class VehicleSelectionDialog extends JPanel {

    private static final long serialVersionUID = 1146328105007254093L;

    protected boolean canceled = true;    
    protected Vector<String> selection = new Vector<String>();
    protected String[] oldSelection = null;
    
    protected void setSelection(String[] selectedIds) {
        oldSelection = selectedIds;
        selection.clear();
        if (selectedIds != null)
            for (String s : selectedIds)
                selection.add(s.toLowerCase());
    }

    public String[] getSelection() {
        if (canceled)
            return oldSelection;
        else
            return selection.toArray(new String[0]);
    }

    protected JPanel getVehiclesPanel() {
        JPanel vehicles = new JPanel();
        vehicles.setLayout(new GridLayout(0, 1));//BoxLayout(vehicles, BoxLayout.PAGE_AXIS));
        vehicles.setBackground(Color.white);
        String[] allVehicles = VehiclesHolder.getVehiclesArray();
        ChangeListener cl = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JToggleButton source =((JToggleButton)e.getSource());
                
                if (selection.contains(source.getText()))
                    selection.remove(source.getText());
                else
                    selection.add(source.getText());                                
            }
        };

        Arrays.sort(allVehicles);
        for (String v : allVehicles) {
            try {
                JToggleButton check = new JToggleButton(v, new ImageIcon(ImageUtils.getImage(
                        VehiclesHolder.getVehicleById(v).getPresentationImageHref()).getScaledInstance(
                                16, 16, Image.SCALE_DEFAULT)), selection.contains(v));
                vehicles.add(check);
                check.addChangeListener(cl);
                check.setBackground(Color.white);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }

        }

        return vehicles;
    }
    
    public static String[] showSelectionDialog(Window owner, VehicleType[] initialSelection) {
        String[] vehicles = new String[initialSelection.length];
        for (int i = 0; i < initialSelection.length; i++)
            vehicles[i] = initialSelection[i].getId();
        
        return showSelectionDialog(owner, vehicles);        
    }
    
    public static String[] showSelectionDialog(Window owner, String[] initialSelection) {
        JDialog dialog = owner == null ? new JDialog((Frame) ConfigFetch.getSuperParentFrame()) : new JDialog(owner);
        VehicleSelectionDialog vs = new VehicleSelectionDialog(initialSelection, true);        
        dialog.getContentPane().add(vs);
        dialog.setSize(500, 300);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        GuiUtils.centerOnScreen(dialog);
        dialog.setVisible(true);
        return vs.getSelection();
    }

    public VehicleSelectionDialog() {
        this(new String[0], true);
    }
    
    public VehicleSelectionDialog(String[] initialSelection, boolean showButtons) {
        setSelection(initialSelection);
        
        JPanel mainPanel = new JPanel(new BorderLayout());

        mainPanel.add(new JScrollPane(getVehiclesPanel()));

        
        JButton btnOk = new JButton(I18n.text("OK"));
        btnOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                canceled = false;
                SwingUtilities.getWindowAncestor((JButton)arg0.getSource()).dispose();
            }
        });

        JButton btnCancel = new JButton(I18n.text("Cancel"));
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                canceled = true;
                SwingUtilities.getWindowAncestor((JButton)arg0.getSource()).dispose();
            }
        });
        GroupLayout groupLayout = new GroupLayout(this);
        groupLayout.setHorizontalGroup(
                groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
                                .addGroup(groupLayout.createSequentialGroup()
                                        .addComponent(btnOk, GroupLayout.PREFERRED_SIZE, 85, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(ComponentPlacement.UNRELATED)
                                        .addComponent(btnCancel, GroupLayout.PREFERRED_SIZE, 85, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(mainPanel, GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE))
                                        .addContainerGap())
        );
        groupLayout.setVerticalGroup(
                groupLayout.createParallelGroup(Alignment.TRAILING)
                .addGroup(groupLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(mainPanel, GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
                        .addPreferredGap(ComponentPlacement.UNRELATED)
                        .addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
                                .addComponent(btnOk, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnCancel, GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE))
                                .addGap(9))
        );
        setLayout(groupLayout);
    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        GuiUtils.setLookAndFeel();
       JFrame f = GuiUtils.testFrame(new VehicleSelectionDialog());
        String[] sel = VehicleSelectionDialog.showSelectionDialog(f, new String[] {"Isurus", "NAUV"});
        for (String s : sel)
            NeptusLog.pub().info("<###> "+s);
    }
}
