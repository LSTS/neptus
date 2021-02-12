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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.gui;

import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * @author zp
 * @author pdias
 */
@SuppressWarnings("serial")
public class DesktopIcon extends JLabel {

	private int growSize = 8;
	
	private ActionListener listener;
	private Point lastDragPoint = null;
	Icon iconN = null, iconF = null;
	
	public DesktopIcon(Icon icon, String text, ActionListener listener) {
		super(text, icon, JLabel.CENTER);
		iconN = icon;
		this.listener = listener;
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				super.mouseDragged(e);
				if (lastDragPoint != null) {
					int diffX = e.getXOnScreen() - lastDragPoint.x;
					int diffY = e.getYOnScreen() - lastDragPoint.y;
					Point loc = DesktopIcon.this.getLocation();
					DesktopIcon.this.setLocation(loc.x + diffX, loc.y + diffY);
					DesktopIcon.this.getParent().invalidate();
					//MyIcon.this.getParent().repaint();
				}
				lastDragPoint = new Point(e.getXOnScreen(), e.getYOnScreen());
			}
		});
		addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.getClickCount() >= 2)
					DesktopIcon.this.listener.actionPerformed(new ActionEvent(this, 0, getText()));
				else if (e.getClickCount() == 1) {
					try {
						DesktopIcon.this.getComponentPopupMenu().setVisible(true);
					} catch (Exception e1) {
					}
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				lastDragPoint = new Point(e.getXOnScreen(), e.getYOnScreen());
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				lastDragPoint = null;
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				if (iconF != null) {
					DesktopIcon.this.setIcon(iconF);
					DesktopIcon.this.setSize(
							DesktopIcon.this.getWidth() + growSize,
							DesktopIcon.this.getHeight() + growSize);
				}
				super.mouseEntered(e);
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
				if (iconF != null) {
					DesktopIcon.this.setIcon(iconN);
					DesktopIcon.this.setSize(
							DesktopIcon.this.getWidth() - growSize,
							DesktopIcon.this.getHeight() - growSize);
				}
				super.mouseExited(e);
			}
			
		});
		
		setBounds(0, 0, (int)getPreferredSize().getWidth(),(int) getPreferredSize().getHeight());
		
		if (iconN instanceof ImageIcon) {
			ImageIcon iic = (ImageIcon) iconN;
			Image img = iic.getImage();
			int height = img.getHeight(null);
			int width = img.getWidth(null);
			growSize = (int) (width * 0.25);
			Image imgS = img.getScaledInstance(width + growSize, height
					+ growSize, Image.SCALE_SMOOTH);
			iconF = new ImageIcon(imgS);
		}
	}
}
