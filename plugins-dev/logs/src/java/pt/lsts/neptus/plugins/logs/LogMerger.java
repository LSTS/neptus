/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Correia
 * May 15, 2012
 */
package pt.lsts.neptus.plugins.logs;

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
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * Dialog meant to select various logs and produce a merge of this logs using the LSFMerger class
 * @author jqcorreia
 */
public class LogMerger extends JDialog {
    private static final long serialVersionUID = 1L;
    JFileChooser fileChooser;
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
        fileChooser = GuiUtils.getFileChooser(ConfigFetch.getLogsDownloadedFolder());
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
                NeptusLog.pub().info("<###>Merging");
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
