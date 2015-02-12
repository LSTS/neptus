/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Manuel Ribeiro
 * Feb 11, 2015
 */

package pt.lsts.neptus.mra;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import net.miginfocom.swing.MigLayout;

/**
 * @author Manuel R.
 *
 */
public class Markermanagement {

    private JFrame frmMarkerManagement;
    private JTable table;
    private MarkerEdit markerEditFrame;
    protected MRAPanel mraPanel;
    private final ArrayList<LogMarker> logMarkers = new ArrayList<LogMarker>();
    private Object[][] data = null;
    private DefaultTableModel defTableModel;
    private String[] columnNames = {
            "Label",
            "Timestamp",
            "Location",
            "Depth",
            "Annotation"
    };

    public Markermanagement(NeptusMRA mra, MRAPanel mraPanel) {
        this.mraPanel = mraPanel;
        initialize();
    }

    @SuppressWarnings("serial")
    private void initialize() {

        frmMarkerManagement = new JFrame();
        frmMarkerManagement.setTitle("Marker Management");
        frmMarkerManagement.setBounds(100, 100, 687, 426);
        frmMarkerManagement.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frmMarkerManagement.getContentPane().setLayout(new MigLayout("", "[grow]", "[grow]"));
        frmMarkerManagement.setVisible(true);

        markerEditFrame = new MarkerEdit(this);
        logMarkers.addAll(mraPanel.getMarkers());

        JPanel panel = new JPanel();
        frmMarkerManagement.getContentPane().add(panel, "cell 0 0,grow");
        panel.setLayout(new MigLayout("", "[][grow]", "[][grow]"));

        JButton btnPrintMarkers = new JButton("Print Markers");
        panel.add(btnPrintMarkers, "cell 1 0,alignx left");

        data = new Object[][] {{"", "", ""}};

        defTableModel = new DefaultTableModel(data, columnNames) 
        {

            boolean[] columnEditables = new boolean[] {
                    false, false, false, false, false
            };
            public boolean isCellEditable(int row, int column) {
                return columnEditables[column];
            }
        };

        table = new JTable(defTableModel);

        table.setShowVerticalLines(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new RowListener());

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table =(JTable) me.getSource();
                Point p = me.getPoint();
                int row = table.rowAtPoint(p);
                if (me.getClickCount() == 2) {
                    //                    JFrame frame2 = new JFrame();
                    openMarkerEditor();

                }
            }

        });


        table.getColumnModel().getColumn(1).setPreferredWidth(96);
        table.getColumnModel().getColumn(2).setPreferredWidth(146);
        table.getColumnModel().getColumn(4).setPreferredWidth(176);
        table.setShowGrid(false);
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);

        defTableModel.removeRow(0);
        fillTableWithMarkers();

        JScrollPane scrollPane = new JScrollPane(table);

        panel.add(scrollPane, "cell 1 1,grow");

    }

    /**
     * 
     */
    private void openMarkerEditor() {
        Object selected = defTableModel.getDataVector().get(table.getSelectedRow());

        System.out.println(selected.toString());
        LogMarker log = getLogFromTableRow();

        //  markerEditFrame.loadMarker(log);
        //markerEditFrame.setSize(470, 540);
        markerEditFrame.setVisible(true);
        markerEditFrame.setLocation(frmMarkerManagement.getLocation().x + frmMarkerManagement.getSize().width, frmMarkerManagement.getLocation().y);
    }

    /**
     * @param selectedRow
     * @return
     */
    private LogMarker getLogFromTableRow() {
        // TODO Auto-generated method stub
        return null;
    }

    private class RowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
            System.out.println(table.getSelectedRow());
        }
    }

    private void fillTableWithMarkers() {
        for(LogMarker l : logMarkers) {
            defTableModel.addRow(new Object[]{l.getLabel(), DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(l.getDate()), l.getLocation().toString(), "0", "empty"});
        }

    }


}
