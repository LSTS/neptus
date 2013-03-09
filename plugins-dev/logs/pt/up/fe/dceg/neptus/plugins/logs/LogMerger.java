/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * May 15, 2012
 */
package pt.up.fe.dceg.neptus.plugins.logs;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;

/**
 * Dialog meant to select various logs and produce a merge of this logs using the LSFMerger class
 * @author jqcorreia
 */
public class LogMerger extends JDialog {
    private static final long serialVersionUID = 1L;
    JFileChooser fileChooser = new JFileChooser();
    JButton btnAdd;
    JButton btnRemove;
    JButton btnMerge;
    JButton btnOutDir;
    
    DefaultListModel<String> listModel = new DefaultListModel<String>();
    JList<String> list = new JList<String>(listModel);

    JTextField txtOutDir = new JTextField();
    
    Vector<String> selectedValues = new Vector<String>();
    
    public LogMerger() {
        initialize();
        setVisible(true);
    }

    public void initialize() {
        // Set layout manager and size
        setLayout(new MigLayout());
        setSize(400,400);
        
        // Initialize components
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
        
        list.setBackground(Color.white);
        list.setBorder(new TitledBorder(I18n.text("Log folders list")));
        list.addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                selectedValues.clear();
                List<String> values = list.getSelectedValuesList();
                for (String s : values)
                    selectedValues.add(s);
            }
        });
        
        btnAdd = new JButton(new AbstractAction(I18n.text("Add")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                int opt = fileChooser.showOpenDialog(LogMerger.this);
                if(opt == JFileChooser.APPROVE_OPTION) {
                    for(File f: fileChooser.getSelectedFiles()) {
                        listModel.addElement(f.getAbsolutePath());
                    }
                }
            }
        });
        
        btnRemove = new JButton(new AbstractAction(I18n.text("Remove")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                for(String s: selectedValues) {
                    listModel.removeElement(s);
                }
            }
        });
        
        btnOutDir = new JButton(new AbstractAction("...") {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                int opt = fileChooser.showOpenDialog(LogMerger.this);
                if(opt == JFileChooser.APPROVE_OPTION) {
                    txtOutDir.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        
        btnMerge = new JButton(new AbstractAction(I18n.text("Merge")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Merging");
            }
        });
        
        
        // Build the layout after component initialization 
        add(btnAdd,"split");
        add(btnRemove,"wrap");
        add(list,"w 100%, h 100%, wrap");
        add(new JLabel(I18n.text("Output directory:")),"wrap");
        add(txtOutDir, "w 100%, split");
        add(btnOutDir, "wrap");
        add(btnMerge);
    }
    
    public static void main(String args[]) {
        LogMerger merger = new LogMerger();
        merger.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
