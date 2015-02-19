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
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * @author Manuel R.
 *
 */
public class LogMarkerItemModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final int COLUMN_INDEX         = 0;
    private static final int COLUMN_LABEL         = 1;
    private static final int COLUMN_TIMESTAMP    = 2;
    private static final int COLUMN_LOCATION     = 3;
    private static final int COLUMN_DEPTH        = 4;
    private static final int COLUMN_ANNOTATION  = 5;

    private String[] columnNames = {
            "Index #",
            "Label",
            "Timestamp",
            "Location",
            "Depth",
            "Annotation"
    };

    private List<LogMarkerItem> markerList;

    public LogMarkerItemModel(List<LogMarkerItem> markerList) {
        this.markerList = markerList;

        int indexCount = 1;
        for (LogMarkerItem marker : markerList) {
            marker.setIndex(indexCount++);
        }
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
            case COLUMN_DEPTH:
                returnValue = marker.getDepth();
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
}
