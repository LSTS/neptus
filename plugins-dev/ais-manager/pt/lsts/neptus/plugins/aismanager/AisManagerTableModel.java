/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 *
 * Author: tsm
 * Mar 20, 2018
 */
package pt.lsts.neptus.plugins.aismanager;

import pt.lsts.neptus.util.DateTimeUtil;

import javax.swing.table.DefaultTableModel;

public class AisManagerTableModel extends DefaultTableModel {
    private int MMSI_COL = 1;
    private int SOG_COL = 2;
    private int COG_COL = 3;
    private int HDG_COL = 4;
    private int LAT_COL = 5;
    private int LON_COL = 6;
    private int TIMESTAMP_COL = 7;

    public AisManagerTableModel(Object[] columnNames, int rowCount) {
        super(columnNames, rowCount);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public int updateTable(String aisLabel, int mmsi, double[] navInfo, long timestampMs) {
        boolean found = false;
        int i = 0;
        while(!found && i < this.getRowCount()) {
            if(this.getValueAt(i, 0).equals(aisLabel)) {
                this.setValueAt(mmsi, i, MMSI_COL);
                this.setValueAt(navInfo[0], i, COG_COL);
                this.setValueAt(navInfo[1], i, SOG_COL);
                this.setValueAt(navInfo[2], i, HDG_COL);
                this.setValueAt(navInfo[3], i, LAT_COL);
                this.setValueAt(navInfo[4], i, LON_COL);
                this.setValueAt(DateTimeUtil.formatTime(timestampMs), i, TIMESTAMP_COL);
                found = true;
            }
            i++;
        }

        if(found)
            return i;

        // add new row
        this.insertRow(0, new Object[]{
                aisLabel,
                mmsi,
                navInfo[0],
                navInfo[1],
                navInfo[2],
                navInfo[3],
                navInfo[4],
                DateTimeUtil.formatTime(timestampMs)});

        return i;
    }
}