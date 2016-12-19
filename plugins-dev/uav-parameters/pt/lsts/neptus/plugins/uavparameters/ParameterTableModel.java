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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.table.AbstractTableModel;

/**
 * @author Manuel R.
 *
 */
@SuppressWarnings("serial")
public class ParameterTableModel extends AbstractTableModel  {

    private HashMap<String, ParameterMetadata> metadata = null;
    private InputStream paramMetadataXML = null;
    private ArrayList<Parameter> params = new ArrayList<>();
    private HashMap<String, ParameterExtended> modifiedParams = new HashMap<>();
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
            "Range",
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

    public Color getRowColor(int row, int columnIndex, String value) {
        return ((modifiedParams.containsKey(value) && columnIndex == COLUMN_VALUE) ? 
                (modifiedParams.get(value).getColor()) : (row % 2 == 0 ? Color.gray : Color.gray.darker()));
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
            ParameterExtended p = new ParameterExtended(param, Color.GREEN.darker());
            modifiedParams.put(param.name, p);

            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    public void clearModifiedParams() {
        modifiedParams.clear();
    }

    private String getDisplayName(String param) {
        if (metadata == null)
            return "";

        return metadata.get(param) == null ? "" : metadata.get(param).getDisplayName();
    }

    private String getDescription(String param) {
        if (metadata == null)
            return "";

        return metadata.get(param) == null ? "" : metadata.get(param).getDescription();
    }

    private String getUnits(String param) {
        if (metadata == null)
            return "";

        return metadata.get(param) == null ? "" : metadata.get(param).getUnits();
    }

    private String getRange(String param) {
        if (metadata == null)
            return "";

        return metadata.get(param) == null ? "" : metadata.get(param).getRange();
    }

    private String getValues(String param) {
        if (metadata == null)
            return "";

        return metadata.get(param) == null ? "" : metadata.get(param).getValues();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (params.isEmpty() || rowIndex >= getRowCount())
            return null;

        Object returnValue = null;
        Parameter param = params.get(rowIndex);
        if (param == null)
            return null;

        switch (columnIndex) {
            case COLUMN_PARAM_NAME:
                returnValue = param.name;
                break;
            case COLUMN_VALUE:
                if (modifiedParams.containsKey(param.name)) {
                    returnValue = modifiedParams.get(param.name).getParameter().getValue();
                    break;
                }
                returnValue = param.getValue();
                break;
            case COLUMN_UNITS:
                returnValue = getUnits(param.name);
                break;
            case COLUMN_OPTIONS:
                returnValue = getRange(param.name);
                break;
            case COLUMN_DESCRIPTION:
                returnValue = getDescription(param.name);
                break;
            default:
                return Object.class;
        }

        return returnValue;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return String.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
            case 3:
                return String.class;
            case 4:
                return String.class;
            default:
                return Object.class;
        }
    }

    public void updateParamList(ArrayList<Parameter> newParamList, String type) {
        this.params = newParamList;

        if (type != null) {
            try {
                paramMetadataXML = getClass().getResourceAsStream("ParameterMetaData.xml");
                metadata = ParameterMetadataMapReader.open(paramMetadataXML, type);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        fireTableDataChanged();
    }

    /**
     * @return the modifiedParams
     */
    public HashMap<String, Parameter> getModifiedParams() {
        HashMap<String, Parameter> list = new HashMap<>();

        for (Entry<String, ParameterExtended> p : modifiedParams.entrySet())
            list.put(p.getKey(), p.getValue().getParameter());

        return list;
    }

    /**
     * Compare incoming parameter's name and value to one on the modified parameters list
     * @param name Incoming parameter name
     * @param value Incoming parameter most recent value
     * @return
     */
    public boolean checkAndUpdateParameter(String name, String value) {
        ParameterExtended p = modifiedParams.get(name);
        Parameter e = p.getParameter();

        if (e == null)
            return false;

        if (!e.getValue().equals(value))
            return false;

        if (modifiedParams.remove(name) != null)
            return true;
        else
            return false;
    }

    private class ParameterExtended {
        private Color color;
        private Parameter parameter;

        public ParameterExtended(Parameter param, Color color) {
            this.setParameter(param);
            this.color = color;
        }

        /**
         * @return the color
         */
        public Color getColor() {
            return color;
        }

        /**
         * @param color the color to set
         */
        public void setColor(Color color) {
            this.color = color;
        }

        /**
         * @return the parameter
         */
        public Parameter getParameter() {
            return parameter;
        }

        /**
         * @param parameter the parameter to set
         */
        public void setParameter(Parameter parameter) {
            this.parameter = parameter;
        }

    }

    public void updateModified(String paramName, Color color) {
        modifiedParams.get(paramName).setColor(color);
        fireTableDataChanged();
    }
}
