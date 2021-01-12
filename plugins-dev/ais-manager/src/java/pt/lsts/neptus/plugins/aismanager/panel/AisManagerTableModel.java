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
 *
 * Author: tsm
 * Mar 20, 2018
 */
package pt.lsts.neptus.plugins.aismanager.panel;

import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.MathMiscUtils;

import javax.swing.table.DefaultTableModel;

public class AisManagerTableModel extends DefaultTableModel {
    private static final long serialVersionUID = -621914316989929156L;
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

        double sogMps = MathMiscUtils.round(knotsToMps(navInfo[0]), 2);
        double cogDegs = MathMiscUtils.round(Math.toDegrees(navInfo[1]), 2);
        double hdgDegs = MathMiscUtils.round(Math.toDegrees(navInfo[2]), 2);
        double latDegs = Math.toDegrees(navInfo[3]);
        double lonDegs = Math.toDegrees(navInfo[4]);
        String timestamp = DateTimeUtil.formatTime(timestampMs);

        while(!found && i < this.getRowCount()) {
            if(this.getValueAt(i, 0).equals(aisLabel)) {
                this.setValueAt(mmsi, i, MMSI_COL);
                this.setValueAt(sogMps, i, SOG_COL);
                this.setValueAt(cogDegs, i, COG_COL);
                this.setValueAt(hdgDegs, i, HDG_COL);
                this.setValueAt(latDegs, i, LAT_COL);
                this.setValueAt(lonDegs, i, LON_COL);
                this.setValueAt(timestamp, i, TIMESTAMP_COL);
                found = true;
            }
            else
                i++;
        }

        if(found)
            return i;

        // add new row
        this.insertRow(0, new Object[]{
                aisLabel,
                mmsi,
                sogMps,
                cogDegs,
                hdgDegs,
                latDegs,
                lonDegs,
                timestamp});

        return i;
    }

    private double knotsToMps(double knots) {
        return knots * 0.514444444;
    }
}