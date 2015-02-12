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

package pt.lsts.neptus.mra.markermanagement;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.NeptusMRA;
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
        frmMarkerManagement.setIconImage(Toolkit.getDefaultToolkit().getImage(Markermanagement.class.getResource("/images/menus/marker.png")));
        frmMarkerManagement.setTitle("Marker Management");
        frmMarkerManagement.setBounds(100, 100, 687, 426);
        frmMarkerManagement.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frmMarkerManagement.getContentPane().setLayout(new MigLayout("", "[grow]", "[grow]"));
        frmMarkerManagement.setVisible(true);
        frmMarkerManagement.setResizable(false);

        markerEditFrame = new MarkerEdit(this);
        logMarkers.addAll(mraPanel.getMarkers());

        JPanel panel = new JPanel();
        frmMarkerManagement.getContentPane().add(panel, "cell 0 0,grow");
        panel.setLayout(new MigLayout("", "[][][grow]", "[][][grow]"));

        List<LogMarkerItem> listMarker = fillTableWithMarkers();
        TableModel tableModel = new LogMarkerItemModel(listMarker);
        table = new JTable(tableModel);
        
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        
       
        int columnIndexToSort = 1;
        sortKeys.add(new RowSorter.SortKey(columnIndexToSort, SortOrder.DESCENDING));
        
        sorter.setSortKeys(sortKeys);
        sorter.sort();

        JButton button = new JButton("Print Markers");
        panel.add(button, "cell 0 0");

        JScrollPane scrollPane = new JScrollPane(table);

        panel.add(scrollPane, "cell 0 2 3 1,grow");

    }

    private void openMarkerEditor() {

        // LogMarkerItem selected = ...;
        // markerEditFrame.loadMarker();

        markerEditFrame.setSize(470, 540);
        markerEditFrame.setVisible(true);
        markerEditFrame.setLocation(frmMarkerManagement.getLocation().x + frmMarkerManagement.getSize().width, frmMarkerManagement.getLocation().y);
    }

    private class RowListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
            System.out.println(table.getSelectedRow());
        }
    }

    private List<LogMarkerItem> fillTableWithMarkers() {
        List<LogMarkerItem> listMarker = new ArrayList<>();

        int i=0;
        for(LogMarker l : mraPanel.getMarkers()) {
            listMarker.add(new LogMarkerItem(i, l.getLabel(), l.getTimestamp(), l.getLat(), l.getLon(), null, 0, "", 0, 0));
            i++;
            // defTableModel.addRow(new Object[]{l.getLabel(), DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(l.getDate()), l.getLocation().toString(), "0", "empty"});
        }

        return listMarker;

    }


}
