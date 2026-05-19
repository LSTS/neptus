/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Correia
 * Nov 9, 2012
 * 
 * Author: keila
 * Aug 25, 2020
 */
package pt.lsts.neptus.controllers;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

class JTableButtonMouseListener implements MouseListener {
    private JTable __table;

    private void __forwardEventToButton(MouseEvent e) {
       
      TableColumnModel columnModel = __table.getColumnModel();
      int column = columnModel.getColumnIndexAtX(e.getX());
      int row    = e.getY() / __table.getRowHeight();
      Object value;
      JButton button;
      MouseEvent buttonEvent;

      if(row >= __table.getRowCount() || row < 0 ||
         column >= __table.getColumnCount() || column < 0)
        return;

      value = __table.getValueAt(row, column);

      if(!(value instanceof JButton))
        return;

      button = (JButton)value;

      buttonEvent =
        (MouseEvent)SwingUtilities.convertMouseEvent(__table, e, button);
      button.dispatchEvent(buttonEvent);
      // This is necessary so that when a button is pressed and released
      // it gets rendered properly.  Otherwise, the button may still appear
      // pressed down when it has been released.
      __table.repaint();
    }

    public JTableButtonMouseListener(JTable table) {
      __table = table;
    }

    public void mouseClicked(MouseEvent e) {
      __forwardEventToButton(e);
    }

    public void mouseEntered(MouseEvent e) {
      __forwardEventToButton(e);
    }

    public void mouseExited(MouseEvent e) {
      __forwardEventToButton(e);
    }

    public void mousePressed(MouseEvent e) {
      __forwardEventToButton(e);
    }

    public void mouseReleased(MouseEvent e) {
      __forwardEventToButton(e);
    }
  }