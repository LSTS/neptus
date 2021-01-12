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
 * Mar 21, 2018
 */
package pt.lsts.neptus.plugins.aismanager.panel;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;

public class AisManagerTable extends JTable {
    private static final long serialVersionUID = -129151613053419358L;

    private final String[] COLUMN_NAMES = {
            "Label",
            "MMSI",
            "SOG (m/s)",
            "COG (º)",
            "HDG (º)",
            "Lat(º)",
            "Lon (rad)",
            "Time (s)",
    };

    private final AisManagerTableModel TABLE_MODEL = new AisManagerTableModel (COLUMN_NAMES, 0);
    private final HashMap<Integer, Color> cellState = new HashMap<>();

    public AisManagerTable() {
        super();
        setModel(TABLE_MODEL);
        setBackground(Color.GRAY.brighter());
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        JComponent jc = (JComponent)c;
            synchronized (cellState) {
                Color status = cellState.getOrDefault(row, Color.GREEN);
                jc.setBackground(status);
            }

        return c;
    }

    /**
     * Update, or add if non-existent, a table entry
     * */
    public void update(String aisLabel, int mmsi, double[] navInfo, long timestampMs, Color status) {
        int row = TABLE_MODEL.updateTable(aisLabel, mmsi, navInfo, timestampMs);

        synchronized (cellState) {
            cellState.put(row, status);
        }

        this.repaint();
        this.revalidate();
    }
}
