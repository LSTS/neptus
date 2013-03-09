/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * May 11, 2010
 */
package pt.up.fe.dceg.neptus.renderer2d;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import pt.up.fe.dceg.neptus.gui.ToolbarSwitch;

/**
 * @author zp
 *
 */
public interface StateRendererInteraction {

	public String getName();	
	public Image getIconImage();		
	public Cursor getMouseCursor();
	public boolean isExclusive();
	
	public void mouseClicked(MouseEvent event, StateRenderer2D source);
	public void mousePressed(MouseEvent event, StateRenderer2D source);
	public void mouseDragged(MouseEvent event, StateRenderer2D source);
	public void mouseMoved(MouseEvent event, StateRenderer2D source);
	public void mouseReleased(MouseEvent event, StateRenderer2D source);
	public void wheelMoved(MouseWheelEvent event, StateRenderer2D source);
	public void setAssociatedSwitch(ToolbarSwitch tswitch);
	public void keyPressed(KeyEvent event, StateRenderer2D source);
	public void keyReleased(KeyEvent event, StateRenderer2D source);
	public void keyTyped(KeyEvent event, StateRenderer2D source);
	
	public void setActive(boolean mode, StateRenderer2D source);
}
