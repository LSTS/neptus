/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Jun 28, 2011
 * $Id:: VehicleSelectionDialog.java 9722 2013-01-17 19:50:19Z pdias            $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.Dialog.ModalityType;
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

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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
        VehicleSelectionDialog vs = new VehicleSelectionDialog(initialSelection);        
        dialog.getContentPane().add(vs);
        dialog.setSize(500, 300);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        GuiUtils.centerOnScreen(dialog);
        dialog.setVisible(true);
        return vs.getSelection();
    }

    public VehicleSelectionDialog() {
        this(new String[0]);
    }
    
    public VehicleSelectionDialog(String[] initialSelection) {
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
            System.out.println(s);
    }
}
