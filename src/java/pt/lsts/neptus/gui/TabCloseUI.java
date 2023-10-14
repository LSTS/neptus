/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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

