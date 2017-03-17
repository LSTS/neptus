/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: junior
 * Sep 17, 2013
 */
package pt.lsts.neptus.plugins.planqueue;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;

import com.l2fprod.common.propertysheet.PropertySheetPanel;

/**
 * @author Manuel Ribeiro
 *
 */
@SuppressWarnings("serial")
public class PlanQueuePanel extends JPanel {

    protected PropertySheetPanel propsPanel = new PropertySheetPanel();
    protected JPanel controls = new JPanel();
    //protected JButton deleteBtn = new JButton(I18n.text("Delete"), ImageUtils.getScaledIcon("pt/up/fe/dceg/neptus/plugins/planning/images/man_remove.png", 16, 16));
    protected JButton addBtn = new JButton(I18n.text("Add Selected Plan to Queue"), ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/planning/images/edit_new.png", 16, 16));
    public Object[][] data = null;
    private  String[] columnNames = null;
    private JTable table = null;
    private DefaultTableModel model = null;
    private int selectedRow;
    public PlanQueuePanel() {

        setLayout(new BorderLayout());     
        setBorder(new TitledBorder(I18n.text("Plans Queue")));
        // add(propsPanel, BorderLayout.CENTER);

        this.columnNames = new String[] {"#",
                "Plan Name",
        "Plan Type"};

        this.selectedRow = -1;
        controls.setLayout(new GridLayout(1, 0));
        controls.add(addBtn);
        addBtn.setEnabled(true);
        add(controls, BorderLayout.SOUTH);


        data = new Object[][] {{"", "", ""}};

        //model = new DefaultTableModel(data, columnNames);
        
        model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
               //all cells false
               return false;
            }
        };
        
        table = new JTable(model);
        model.removeRow(0);
        
        table.setPreferredScrollableViewportSize(new Dimension(200, 70));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        model = (DefaultTableModel) table.getModel();
        
        ListSelectionModel rowSM = table.getSelectionModel();
        rowSM.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;

                ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                if (lsm.isSelectionEmpty()) {
                   // System.out.println("No rows are selected.");
                    selectedRow = -1;
                } else {
                    selectedRow = lsm.getMinSelectionIndex();
                 //   System.out.println("Row " + selectedRow
                  //          + " is now selected.");
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(table);

        add(scrollPane);
    }

    /**
     * @return the selectedRow
     */
    public int getSelectedRow() {
        return selectedRow;
    }


    @Override
    public Dimension getPreferredSize() {
        return new Dimension(200, 200);
    }

    /**
     * @return the deleteBtn
     */
    public JButton getAddBtn() {
        return addBtn;
    }

    public void main (String args[]) {
        PlanQueuePanel props = new PlanQueuePanel();

    }
    /**
     * @param scrollPane
     */
    public void addRow(String[] args) {
        model.addRow(new Object[]{args[0], args[1], args[2]});

        /* table.addMouseListener(new MouseAdapter() { // DEBUG
                    public void mouseClicked(MouseEvent e) {
                        System.out.println("table click");
                    }
                });

         */

    }

    /*
     * @return true if removed sucessfuly
     * @return  false if error / not removed
     */
    public boolean removeRow() {
        if (!isTableEmpty() && selectedRow != -1) {
            model.removeRow(selectedRow);
            updateRows();  
            return true;
        }
        return false;

    }
    
    public boolean removeRowZero() {
        if (!isTableEmpty()) {
            model.removeRow(0);
            updateRows();  
            return true;
        }
        return false;

    }
    
    

    public void moveUp(){ 
        DefaultTableModel model =  (DefaultTableModel)table.getModel();
        int row = table.getSelectedRow();
        
        
        if (row!=0 && row!=-1) {

            model.moveRow(row,row,row-1);
            table.setRowSelectionInterval(row-1, row-1);
            updateRows();


        }
    }
    public void moveDown(){
        DefaultTableModel model =  (DefaultTableModel)table.getModel();
        int row = table.getSelectedRow();
        
        if (row!=(table.getRowCount()-1) && row!=-1) {

            model.moveRow(row,row,row+1);
            table.setRowSelectionInterval(row+1, row+1);
            updateRows();  

        }
    }

    public boolean isTableEmpty() {
        if (table.getRowCount() == 0) {
            return true;
        }
        return false;
    }

    private void updateRows(){
        for (int i=0;i<table.getRowCount();i++) {
            table.setValueAt(i, i, 0);
        }
    }
    /**
     * @return
     */
    public Component getTable() {
        return this.table;
    }

    public int getTableRows() {
        return this.table.getRowCount();
    }

    public Object getLastRowIndex() {
        return table.getValueAt(getTableRows()-1, 0);
    }

    public Object getSelectedTableValue() {
        
        if (selectedRow!=-1)
            return table.getValueAt(selectedRow,1);
        return null;
    }
    
    public Object getTableValue(int row) {
          return table.getValueAt(row,1);
    }
    
    
}
