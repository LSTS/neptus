/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Manuel R.
 * Nov 11, 2016
 */
package pt.lsts.neptus.plugins.uavparameters;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.table.AbstractTableModel;

/**
 * @author Manuel R.
 *
 */
@SuppressWarnings("serial")
public class ParameterTableModel extends AbstractTableModel  {
    private ArrayList<Parameter> params = new ArrayList<>();
    private HashMap<String, Parameter> modifiedParams = new HashMap<>();
    private static final int COLUMN_PARAM_NAME = 0;
    private static final int COLUMN_VALUE = 1;
    private static final int COLUMN_UNITS = 2;
    private static final int COLUMN_OPTIONS = 3;
    private static final int COLUMN_DESCRIPTION = 4;

    public ParameterTableModel(ArrayList<Parameter> params) {
        this.params = params;
    }

    private String[] columnNames = {
            "Name",
            "Value",
            "Units",
            "Options",
            "Description"
    };

    @Override
    public int getRowCount() {
        return params.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    public Color getRowColor(int row, int columnIndex) {
        return ((modifiedParams.containsKey(row) && columnIndex == COLUMN_VALUE) ? (Color.green.darker()) : (row % 2 == 0 ? Color.gray : Color.gray.darker()));
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == COLUMN_VALUE)
            return true;
        else
            return false;
    }
    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex != COLUMN_VALUE)
            return;

        Parameter param = params.get(rowIndex);
        String oldValue = param.getValue();
        param.value = Double.parseDouble((String) value);

        if (!oldValue.equals(value)) {
            System.out.println("Updating: "+ param.name + "(" + oldValue +") with " + value);

            modifiedParams.put(param.name, param);

            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    public void clearModifiedParams() {
        modifiedParams.clear();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (params.isEmpty() || rowIndex >= getRowCount())
            return null;

        Object returnValue = null;
        Parameter param = params.get(rowIndex);

        switch (columnIndex) {
            case COLUMN_PARAM_NAME:
                returnValue = param.name;
                break;
            case COLUMN_VALUE:
                returnValue = param.getValue();
                break;
            case COLUMN_UNITS:
                returnValue = "";
                break;
            case COLUMN_OPTIONS:
                returnValue = "";
                break;
            case COLUMN_DESCRIPTION:
                returnValue = "";
                break;
            default:
                throw new IllegalArgumentException("Invalid column index");
        }

        return returnValue;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (params.isEmpty())
            return Object.class;

        return getValueAt(0, columnIndex).getClass();
    }

    public void updateParamList(ArrayList<Parameter> newParamList) {
        this.params = newParamList;

        fireTableDataChanged();
    }

    /**
     * @return the modifiedParams
     */
    public HashMap<String, Parameter> getModifiedParams() {
        return modifiedParams;
    }

    /**
     * @param name
     * @param value
     * @return
     */
    public boolean checkAndUpdateParameter(String name, String value) {
        Parameter e = modifiedParams.get(name);
        if (e != null && e.getValue().equals(value)) {

            modifiedParams.remove(name);
            return true;
        }
        else {
            System.out.println("FALSE ");
            return false;
        }
    }
}
