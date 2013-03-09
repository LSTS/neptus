/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.gui;

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
