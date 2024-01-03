/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: richard
 * 19/10/2017
 */
package pt.lsts.neptus.util.llf;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;

/**
 * Util class with static fuctions to create a JTable menu with an "Add mark" option
 * 
 * @author richard
 *
 */
public class MraMessageLogTablePopupMenu {
    
    /**
     * Defines the row selected at a certain point in the table
     * 
     * @param table Table from which a row is selected
     * @param point Point on the table that was clicked
     * @return Returns the selected row of the table
     */
    public static int setRowSelection(JTable table, Point point){
        int currentRow = table.rowAtPoint(point);
        table.setRowSelectionInterval(currentRow, currentRow);
        return table.getSelectedRow();
    }

    /**
     * Creates a new table menu with an "Add mark" option
     * 
     * @param mraPanel Mission Review & Analysis panel
     * @param table Table where the menu is generated
     * @param msg Message selected message
     * @param point Point where the menu is generated
     */
    public static void setAddMarkMenu(MRAPanel mraPanel, JTable table, IMCMessage msg, Point point) {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem menuItemAddMark = new JMenuItem(I18n.text("Add Mark"));

        popupMenu.add(menuItemAddMark);
        popupMenu.show(table, point.x, point.y);
        
        menuItemAddMark.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent  e) {
                if (e.getSource() == menuItemAddMark){
                    String res = JOptionPane.showInputDialog(I18n.text("Marker name"));
                    if (res != null && !res.isEmpty()) {
                        long ts = (long) msg.getTimestampMillis();
                        mraPanel.addMarker(new LogMarker(res, ts, 0, 0));
                    }
                }
            }
        });
    }
}
