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
 * Author: Margarida Faria
 * Nov 6, 2012
 */
package pt.lsts.neptus.plugins.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import pt.lsts.neptus.plugins.state.EntitySubPanel.Icons;

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