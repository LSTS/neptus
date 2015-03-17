/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * May 11, 2010
 */
package pt.lsts.neptus.renderer2d;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AngleCalc;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
public class InteractionAdapter extends ConsolePanel implements StateRendererInteraction {

    private static final long serialVersionUID = 1L;
	private Point2D lastDragPoint = null;	
	
	private double deltaX = 0, deltaY = 0;
	private static Cursor cursor;
	private static Image image;
	private boolean active = false;
	protected ToolbarSwitch associatedSwitch = null;
	boolean rotating = false, measuring = false, zooming = false;
	private LocationType firstDragPoint = null;
	
	private static final Image rotateIcon = ImageUtils.getImage("images/menus/rotate.png");
	private static final Image zoomIcon = ImageUtils.getImage("images/menus/zoom.png");
	private static final Image rulerIcon = ImageUtils.getImage("images/menus/ruler.png");
	{
		cursor = Toolkit.getDefaultToolkit().createCustomCursor(ImageUtils.getImage("images/cursors/crosshair_cursor.png"), new Point(6,6), "Zoom");
		image = ImageUtils.getImage("images/buttons/alarm.png");
	}
	
	
	public void paintInteraction(Graphics2D g, StateRenderer2D source) {
	    g.setTransform(source.identity);
	    if (rotating) {
	        g.drawImage(rotateIcon, 20, 50, null);
	    }
	    else if (measuring) {
	        if (firstDragPoint != null) {
	            LocationType end = source.getRealWorldLocation(lastDragPoint);
	            double distance = end.getDistanceInMeters(firstDragPoint);
	            String txt = String.format("%.2f m", distance);
	            g.setStroke(new BasicStroke(5.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
	            g.setColor(new Color(0,0,0,100));
	            Point2D start = source.getScreenPosition(firstDragPoint);
	            
	            
	            int angle = (int) Math.toDegrees(Math.PI/2+Math.atan2(lastDragPoint.getY()-start.getY(),lastDragPoint.getX() - start.getX()));
	            if (angle < 0)
	                angle += 360;
	            String angleTxt = String.format("%dº", angle);
                g.fillArc((int)start.getX()-15, (int)start.getY()-15, 30, 30, 90, -angle);
	            g.draw(new Line2D.Double(start, lastDragPoint));
	            g.setColor(Color.black);
	            g.setStroke(new BasicStroke(2.5f));
                g.setColor(Color.green.brighter().brighter());
                g.draw(new Line2D.Double(start, lastDragPoint));
                g.setFont(new Font("Arial", Font.BOLD, 16));
                g.setColor(new Color(0,0,0,100));
	            g.drawString(txt, (int)(lastDragPoint.getX()+12), (int)(lastDragPoint.getY()+11));
	            g.drawString(txt, (int)(lastDragPoint.getX()+12), (int)(lastDragPoint.getY()+12));
	            
	            g.setFont(new Font("Arial", Font.BOLD, 12));
	            g.drawString(angleTxt, (int)(start.getX()+7), (int)(start.getY()+6));
                g.drawString(angleTxt, (int)(start.getX()+6), (int)(start.getY()+6));
                
                
	            g.setFont(new Font("Arial", Font.BOLD, 12));
	            g.setColor(Color.gray.brighter());
	            g.drawString(angleTxt, (int)(start.getX()+5), (int)(start.getY()+5));
	            
	            g.setColor(Color.white);
                g.setFont(new Font("Arial", Font.BOLD, 16));
                g.drawString(txt, (int)(lastDragPoint.getX()+10), (int)(lastDragPoint.getY()+10));
                
	        }	        
            g.drawImage(rulerIcon, 20, 50, null);
	    }
	    else if (zooming) {
            g.drawImage(zoomIcon, 20, 50, null);
	    }	    
	}
	
    /**
     * @param console
     */
    public InteractionAdapter(ConsoleLayout console) {
        super(console);
    }

	@Override
	public Cursor getMouseCursor() {
		return cursor;
	}
	
	@Override
	public Image getIconImage() {
		return image;
	}
	
	@Override
	public boolean isExclusive() {
		return true;
	}
	
	public void resetView() {
	    
	}
	
	public void keyPressed(KeyEvent event, StateRenderer2D source) {
	    
        switch (event.getKeyCode()) {
            case (KeyEvent.VK_PLUS):
            case (KeyEvent.VK_PAGE_UP):
                source.setLevelOfDetail(source.getLevelOfDetail() + 1);
                source.repaint();
                break;

            case (KeyEvent.VK_MINUS):
            case (KeyEvent.VK_PAGE_DOWN):
                source.setLevelOfDetail(source.getLevelOfDetail() - 1);
                source.repaint();
                break;

            case (KeyEvent.VK_LEFT):
                if (!event.isControlDown()) {
                    double deltaX = -source.getWidth() / 16.0, deltaY = 0;
                    if (source.getRotation() != 0) {
                        double[] offsets = AngleCalc.rotate(source.getRotation(), deltaX, deltaY, false);
                        deltaX = offsets[0];
                        deltaY = offsets[1];
                    }
                    source.worldPixelXY.setLocation(source.worldPixelXY.getX() + deltaX, source.worldPixelXY.getY() + deltaY);
                }
                else
                    source.setRotation(source.getRotation() - 0.05);
                source.repaint();
                break;

            case (KeyEvent.VK_RIGHT):
                if (!event.isControlDown()) {
                    double deltaX = source.getWidth() / 16.0, deltaY = 0;
                    if (source.getRotation() != 0) {
                        double[] offsets = AngleCalc.rotate(source.getRotation(), deltaX, deltaY, false);
                        deltaX = offsets[0];
                        deltaY = offsets[1];
                    }
                    source.worldPixelXY.setLocation(source.worldPixelXY.getX() + deltaX, source.worldPixelXY.getY() + deltaY);
                }
                else
                    source.setRotation(source.getRotation() + 0.05);

                source.repaint();
                break;

            case (KeyEvent.VK_UP):
                double deltaXU = 0, deltaYU = -source.getHeight() / 16.0;
                if (source.getRotation() != 0) {
                    double[] offsets = AngleCalc.rotate(source.getRotation(), deltaXU, deltaYU, false);
                    deltaXU = offsets[0];
                    deltaYU = offsets[1];
                }
                source.worldPixelXY.setLocation(source.worldPixelXY.getX() + deltaXU, source.worldPixelXY.getY() + deltaYU);
                source.repaint();
                break;

            case (KeyEvent.VK_DOWN):
                double deltaXD = 0, deltaYD = source.getHeight() / 16.0;
                if (source.getRotation() != 0) {
                    double[] offsets = AngleCalc.rotate(source.getRotation(), deltaXD, deltaYD, false);
                    deltaXD = offsets[0];
                    deltaYD = offsets[1];
                }
                source.worldPixelXY.setLocation(source.worldPixelXY.getX() + deltaXD, source.worldPixelXY.getY() + deltaYD);
                source.repaint();
                break;

            case (KeyEvent.VK_N):
                source.setRotation(0);
                source.repaint();
                break;

            case (KeyEvent.VK_L):
                source.setLegendShown(!source.isLegendShown());
                source.repaint();
                break;
            case (KeyEvent.VK_F1):
                source.resetView();
                repaint();
                break;
          case (KeyEvent.VK_G):
              source.setGridShown(!source.isGridShown());
              repaint();
              break;
          case (KeyEvent.VK_SHIFT):
              rotating = true;
          break;
          case (KeyEvent.VK_CONTROL):
              measuring = true;
          break;
        }
	}
	
	public void keyReleased(java.awt.event.KeyEvent event, StateRenderer2D source) {
	    switch (event.getKeyCode()) {
	    case (KeyEvent.VK_SHIFT):
            rotating = false;
        break;
        case (KeyEvent.VK_CONTROL):
            measuring = false;
        break;
	    }
	}
	
	public void keyTyped(java.awt.event.KeyEvent event, StateRenderer2D source) {
	    
	}
	
	public void mouseDragged(MouseEvent event, StateRenderer2D source) {
		if (lastDragPoint == null || measuring) {
			lastDragPoint = event.getPoint();
			deltaX = deltaY = 0;
			return;
		}
		else {
			deltaX = event.getPoint().getX() - lastDragPoint.getX();
			deltaY = event.getPoint().getY() - lastDragPoint.getY();	
		}
		
		

		double rotationRads = source.getRotation();

		if (rotating) {
		    source.setRotation(rotationRads + deltaY * 0.05);
		}
		else {
		    if (rotationRads != 0) {

		        double dist = event.getPoint().distance(lastDragPoint);
		        double angle =  Math.atan2(event.getPoint().getY() - lastDragPoint.getY(), event.getPoint()
		                .getX() - lastDragPoint.getX());

		        deltaX = dist * Math.cos(angle + rotationRads);
		        deltaY = dist * Math.sin(angle + rotationRads);
		    }

		    source.worldPixelXY.setLocation(source.worldPixelXY.getX()-deltaX, source.worldPixelXY.getY()-deltaY);
		}
		lastDragPoint = event.getPoint();
		source.repaint();
	}
	

	public void mousePressed(MouseEvent event, StateRenderer2D source) {
		lastDragPoint = event.getPoint();
		if(!event.isControlDown())
		    measuring = false;
		if(!event.isShiftDown())
            rotating = false;        
		if (measuring && firstDragPoint == null)
		    firstDragPoint = source.getRealWorldLocation(event.getPoint());
		deltaX = deltaY = 0;
	}
	
	public void mouseReleased(MouseEvent event, StateRenderer2D source) {
		lastDragPoint = null;
		firstDragPoint = null;
	}	
	
	long lastMouseWheelMillis = 0;
	public void wheelMoved(MouseWheelEvent arg0, StateRenderer2D source) {
	    if (arg0.getWhen() - lastMouseWheelMillis < 50)
	        return;
	    lastMouseWheelMillis = arg0.getWhen();
	    
	    source.zoomInOut(arg0.getWheelRotation() < 0, arg0.getPoint().getX(), arg0.getPoint().getY());
		source.repaint();
	}
	
	@Override
	public void mouseExited(MouseEvent event, StateRenderer2D source) {
	    
	}

	public void mouseClicked(MouseEvent e, StateRenderer2D source) {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
            if (e.isShiftDown()) { // zoom out
                source.zoomInOut(false, e.getX(), e.getY());
            }
            else { // zoom in
                source.zoomInOut(true, e.getX(), e.getY());
            }
        }
	}

	public void mouseMoved(MouseEvent event, StateRenderer2D source) {
	    
	}
	
	@Override
	public void focusGained(FocusEvent event, StateRenderer2D source) {
	    
	}
	
	@Override
	public void focusLost(FocusEvent event, StateRenderer2D source) {
	    lastDragPoint = null;
        firstDragPoint = null;
        measuring = rotating = false;
	}
	
	@Override
	public void setActive(boolean mode, StateRenderer2D source) {
	    this.active = mode;	
	}
	
	public boolean isActive() {
	    return active;
	}

    /**
     * @return the associatedSwitch
     */
    public ToolbarSwitch getAssociatedSwitch() {
        return associatedSwitch;
    }

    /**
     * @param associatedSwitch the associatedSwitch to set
     */
    public void setAssociatedSwitch(ToolbarSwitch associatedSwitch) {
        this.associatedSwitch = associatedSwitch;
    }

    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
