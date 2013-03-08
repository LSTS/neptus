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
 * $Id:: TabCloseUI.java 9616 2012-12-30 23:23:22Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;


public class TabCloseUI implements MouseListener, MouseMotionListener {
	private CloseTabbedPane  tabbedPane;
	private int closeX = 0 ,closeY = 0, meX = 0, meY = 0;
	private int selectedTab;

	public void mouseEntered(MouseEvent me) {}
	public void mouseExited(MouseEvent me) {}
	public void mousePressed(MouseEvent me) {}
	public void mouseReleased(MouseEvent me) {}
	public void mouseDragged(MouseEvent me) {}

	public void mouseClicked(MouseEvent me) {
		if(closeUnderMouse(me.getX(), me.getY())){
			
			tabbedPane.removeTabAt(selectedTab);
		}
	}

	public void mouseMoved(MouseEvent me) {	
		meX = me.getX();
		meY = me.getY();			
		if(mouseOverTab(meX, meY)){			
			tabbedPane.repaint();
		}
	}
	
	private boolean closeUnderMouse(int x, int y) {		
		return new Rectangle(closeX -2 ,closeY -9, 10, 10).contains(x,y);
	}

	public void paint(Graphics g) {
		if(mouseOverTab(meX, meY)){
			drawClose(g,closeX,closeY);
		}
		int tabCount = tabbedPane.getTabCount();
		for(int j = 0; j < tabCount; j++)
			if(tabbedPane.getComponent(j).isShowing()){			
				int x = tabbedPane.getBoundsAt(j).x + tabbedPane.getBoundsAt(j).width -12;
				int y = tabbedPane.getBoundsAt(j).y + 14;			
				drawClose(g,x,y);
				break;
			}
	}

	private void drawClose(Graphics g, int x, int y) {
		if(tabbedPane != null && tabbedPane.getTabCount() > 0){
			Graphics2D g2 = (Graphics2D)g;
			g2.setStroke(new BasicStroke(3,BasicStroke.JOIN_ROUND,BasicStroke.CAP_ROUND));
			g2.setColor(Color.BLACK);
			if(isUnderMouse(x,y)){
				drawColored(g2, Color.RED.darker(), x, y);
			}
			else{
				drawColored(g2, Color.WHITE, x, y);			
			}
		}

	}

	private void drawColored(Graphics2D g2, Color color, int x, int y) {
		g2.drawLine(x, y, x + 8, y - 8);
		g2.drawLine(x + 8, y, x, y - 8);
		g2.setColor(color);
		g2.setStroke(new BasicStroke(3, BasicStroke.JOIN_ROUND, BasicStroke.CAP_ROUND));
		g2.drawLine(x, y, x + 8, y - 8);
		g2.drawLine(x + 8, y, x, y - 8);

	}

	private boolean isUnderMouse(int x, int y) {
		if(Math.abs(x-meX+3)<6 && Math.abs(y-meY-4)<6 )
			return  true;		
		return  false;
	}

	private boolean mouseOverTab(int x, int y) {
	    // Preventing an exception thrown due to synch issues
	    if(tabbedPane.getTabCount() == 0)
		    return false;
		
	    for(int j = 0; j < tabbedPane.getTabCount(); j++) {
	        if(tabbedPane.getBoundsAt(j).contains(meX, meY)){
				selectedTab = j;
				closeX = tabbedPane.getBoundsAt(j).x + tabbedPane.getBoundsAt(j).width -12;
				closeY = tabbedPane.getBoundsAt(j).y + 14;
				return true;
			}
	    }
		return false;
	}

	public void setTabbedPane(CloseTabbedPane ctp){	tabbedPane = ctp;	}
}

