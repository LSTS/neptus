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
 * Author: José Pinto
 * May 30, 2009
 */
package pt.lsts.neptus.gui;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.basic.BasicTableUI;
import javax.swing.table.TableModel;

/**
 * With this class it's possible to add drag and drop support to any JTable.
 * <br>Example: <pre>
 * JTable t = new JTable();
 * t.setUI(new DragAndDropTableUI());
 * </pre>
 * @author zp
 * 
 */
public class DragAndDropTableUI extends BasicTableUI {

	private boolean draggingRow = false;
	private int startDragPoint;
	private int dyOffset;

	protected MouseInputListener createMouseInputListener() {
		return new DragDropRowMouseInputHandler();
	}

	public void paint(Graphics g, JComponent c) {
		super.paint(g, c);

		if (draggingRow) {
			g.setColor(table.getParent().getBackground());
			Rectangle cellRect = table.getCellRect(table.getSelectedRow(), 0,
					false);
			g.copyArea(cellRect.x, cellRect.y, table.getWidth(), table
					.getRowHeight(), cellRect.x, dyOffset);

			if (dyOffset < 0) {
				g.fillRect(cellRect.x, cellRect.y
						+ (table.getRowHeight() + dyOffset), table.getWidth(),
						(dyOffset * -1));
			} else {
				g.fillRect(cellRect.x, cellRect.y, table.getWidth(), dyOffset);
			}
		}
	}

	class DragDropRowMouseInputHandler extends MouseInputHandler {

		public void mousePressed(MouseEvent e) {
			super.mousePressed(e);
			startDragPoint = (int) e.getPoint().getY();
		}

		public void mouseDragged(MouseEvent e) {
			int fromRow = table.getSelectedRow();

			if (fromRow >= 0) {
				draggingRow = true;

				int rowHeight = table.getRowHeight();
				int middleOfSelectedRow = (rowHeight * fromRow)
						+ (rowHeight / 2);

				int toRow = -1;
				int yMousePoint = (int) e.getPoint().getY();

				if (yMousePoint < (middleOfSelectedRow - rowHeight)) {
					// Move row up
					toRow = fromRow - 1;
				} else if (yMousePoint > (middleOfSelectedRow + rowHeight)) {
					// Move row down
					toRow = fromRow + 1;
				}

				if (toRow >= 0 && toRow < table.getRowCount()) {
					TableModel model = table.getModel();

					for (int i = 0; i < model.getColumnCount(); i++) {
						Object fromValue = model.getValueAt(fromRow, i);
						Object toValue = model.getValueAt(toRow, i);

						model.setValueAt(toValue, fromRow, i);
						model.setValueAt(fromValue, toRow, i);
					}
					table.setRowSelectionInterval(toRow, toRow);
					startDragPoint = yMousePoint;
				}

				dyOffset = (startDragPoint - yMousePoint) * -1;
				table.repaint();
			}
		}

		public void mouseReleased(MouseEvent e) {
			super.mouseReleased(e);

			draggingRow = false;
			table.repaint();
		}
	}

}
