/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Nov 6, 2012
 */
package pt.up.fe.dceg.neptus.plugins.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import pt.up.fe.dceg.neptus.plugins.state.EntitySubPanel.Icons;

/**
 * @author Margarida Faria
 *
 */
class EntitiesModel extends AbstractTableModel {
    private static final long serialVersionUID = 4880861752680549078L;
    private final List<String> columnNames;
    private final List<Object[]> data;
    
    public enum DataIndex {
        IS_HIDE((short) 0),
        ENTITY_NAME((short) 1),
        ICON((short) 2),
        DESCRIPTION((short) 3),
        TIME_ELAPSED((short) 4),
        ENTITY_ID((short) 5);

        private short index;
        
        private DataIndex(short i) {
            index = i;
        }

        public short getIndex() {
            return index;
        }
    }

    /**
     * @param backingList
     * @param columnNames2
     */
    public EntitiesModel(ArrayList<String> columnNames2) {
        columnNames = Collections.synchronizedList(columnNames2);
        data = Collections.synchronizedList(new ArrayList<Object[]>());
        Object testRow[] = { true, "Entity name", Icons.FAILUREICON.getIcon(), "description", (short) 0, (short) 1 };
        data.add(testRow);
    }


    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public int getRowCount() {
        synchronized (data) {
            return data.size();
        }
    }

    @Override
    public String getColumnName(int col) {
        return columnNames.get(col);
    }

    @Override
    public Object getValueAt(int row, int col) {
        synchronized (data) {
            return data.get(row)[col];
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public String getColumnHeader(int col) {
        return columnNames.get(col);
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        synchronized (data) {
            data.get(row)[col] = value;
            fireTableCellUpdated(row, col);
        }
    }

    public void updateRow(Object[] rowToInsert) {
        int iHide = DataIndex.IS_HIDE.getIndex();
        int i = 0;
        boolean found = false;
        synchronized (data) {
            Object[] currRow;
            Iterator<Object[]> iterator = data.iterator();
            while (iterator.hasNext()) {
                currRow = iterator.next();
                if (((String) currRow[iHide]).equalsIgnoreCase((String) rowToInsert[iHide])) {
                    rowToInsert[DataIndex.IS_HIDE.getIndex()] = currRow[DataIndex.IS_HIDE.getIndex()];
                    data.remove(currRow);
                    data.add(i, rowToInsert);
                    found = true;
                }
                i++;
            }
            if (!found) {
                rowToInsert[DataIndex.IS_HIDE.getIndex()] = false;
                data.add(rowToInsert);
            }
            fireTableRowsInserted(i, i);
        }
    }
}