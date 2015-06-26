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
 * Author: Manuel
 * Feb 12, 2015
 */
package pt.lsts.neptus.mra.markermanagement;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import pt.lsts.neptus.mra.markermanagement.LogMarkerItem.Classification;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author Manuel R.
 *
 */
public class LogMarkerItemModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final int COLUMN_INDEX           = 0;
    private static final int COLUMN_LABEL           = 1;
    private static final int COLUMN_TIMESTAMP      = 2;
    private static final int COLUMN_LOCATION       = 3;
    private static final int COLUMN_ALTITUDE           = 4;
    private static final int COLUMN_CLASSIFICATION = 5;
    private static final int COLUMN_ANNOTATION     = 6;

    private String[] columnNames = {
            "#",
            "Label",
            "Timestamp",
            "Location",
            "Altitude (m)",
            "Classification",
            "Annotation"
    };

    private List<LogMarkerItem> markerList;

    public LogMarkerItemModel(List<LogMarkerItem> markerList) {
        this.markerList = markerList;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return markerList.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        
        LogMarkerItem marker = markerList.get(rowIndex);
        Object returnValue = null;

        switch (columnIndex) {
            case COLUMN_INDEX:
                returnValue = marker.getIndex();
                break;
            case COLUMN_LABEL:
                returnValue = marker.getLabel();
                break;
            case COLUMN_TIMESTAMP:
                returnValue = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(marker.getDate());
                break;
            case COLUMN_LOCATION:
                returnValue = marker.getLocation();
                break;
            case COLUMN_ALTITUDE:
                returnValue = marker.getAltitude() < 0 ? "-" : marker.getAltitude();
                break;
            case COLUMN_CLASSIFICATION:
                returnValue = marker.getClassification();
                break;
            case COLUMN_ANNOTATION:
                returnValue = marker.getAnnotation();
                break;

            default:
                throw new IllegalArgumentException("Invalid column index");
        }

        return returnValue;
    }


    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        LogMarkerItem marker = markerList.get(rowIndex);
        if (columnIndex == COLUMN_INDEX) {
            marker.setIndex((int) value);
        }      
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (markerList.isEmpty()) {
            return Object.class;
        }
        return getValueAt(0, columnIndex).getClass();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    public void removeRow(int row) {
        fireTableRowsDeleted(row, row);
    }

    public void updateRow(int row) {

        fireTableRowsUpdated(row, row);
    }
    
    public void insertRow(int row) {
        fireTableRowsInserted(row, row);
    }
    
    public void setTableSorter(int columnIndexToSort, JTable table) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();

        sortKeys.add(new RowSorter.SortKey(columnIndexToSort, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        sorter.sort();
    }

    public void setCenteredColumns(JTable table) {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer centerRenderer2 = new DefaultTableCellRenderer();
        centerRenderer2.setHorizontalAlignment(SwingConstants.LEFT);

        table.setDefaultRenderer(String.class, centerRenderer2);
        table.setDefaultRenderer(Classification.class, centerRenderer);
        table.setDefaultRenderer(Integer.class, centerRenderer);
        table.setDefaultRenderer(Double.class, centerRenderer);
        table.setDefaultRenderer(LocationType.class, centerRenderer);
        
        //set altitude column to be centered
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
    }

    public void setColumnsWidth(JTable table) {
        // column 0 - width
        table.getColumnModel().getColumn(0).setMinWidth(25);
        table.getColumnModel().getColumn(0).setMaxWidth(25);
        table.getColumnModel().getColumn(0).setPreferredWidth(25);

        // column 2 - width
        table.getColumnModel().getColumn(2).setMaxWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(115);

        // column 3 - width
        table.getColumnModel().getColumn(3).setMaxWidth(190);
        table.getColumnModel().getColumn(3).setPreferredWidth(175);

        // column 4 - width
        table.getColumnModel().getColumn(4).setMaxWidth(75);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);

        // column 5 - width
        table.getColumnModel().getColumn(5).setMaxWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(105);
    }
}
