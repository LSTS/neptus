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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: José Correia
 * Nov 9, 2012
 * 
 * Author: keila
 * Aug 25, 2020
 */
package pt.lsts.neptus.controllers;

import java.awt.*;
import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.controllers.ControllerPanel.ActionType;
import pt.lsts.neptus.controllers.ControllerPanel.MapperComponent;
import pt.lsts.neptus.i18n.I18n;

@SuppressWarnings("serial")
class AxisTableModel extends AbstractTableModel {
    public ArrayList<MapperComponent> list;

    @Override
    public String getColumnName(int column) {
        switch (column) {
            case 0:
                return I18n.text("Axis");
            case 1:
                return I18n.text("Component");
            case 2:
                return I18n.text("Value");
            case 3:
                return I18n.text("Inverted");
            case 4:
                return I18n.text("Assign");
            case 5:
                return I18n.text("Unassign");
            case 6:
                return I18n.text("Range");
            default:
                return "";
        }
    }

    public AxisTableModel(ArrayList<MapperComponent> list) {
        this.list = list;
    }

    public ArrayList<MapperComponent> getList() {
        return this.list;
    }

    @Override
    public int getRowCount() {
        return list.size();
    }

    @Override
    public int getColumnCount() {
        return 7; // extra Range column

    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        MapperComponent comp = list.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return comp.action;
            case 1:
                return comp.button;
            case 2:
                return comp.value;
            case 3:
                return comp.inverted;
            case 4:
                return comp.edit;
            case 5:
                return comp.clear;
            case 6:
                return comp.range;
        }
        return null;
    }

    public boolean isCellEditable(int row, int col) {
        return col == 3 || col == 1 || col == 4 || col == 5; // Hard-coded for now
    }

    public void setValueAt(Object value, int row, int col) {
        if(col == 3) {
            list.get(row).inverted = (Boolean) value;
        }
        if (col == 6) {
            try {
                float v = (float) value;
                this.list.get(row).setRange(v);
            }
            catch (NumberFormatException e) {
                NeptusLog.pub().error(I18n.text("Invalid Number Format for Range."), e);
            }
        }
        fireTableCellUpdated(row, col);
    }

    public Class<?> getColumnClass(int c) {
        Object cl = getValueAt(0, c);
        if (cl == null)
            return Object.class;
        else
            return cl.getClass();
    }
}