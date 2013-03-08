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
 * 8/Fev/2005
 * $Id:: ToolbarSwitch.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

/**
 * A WindowsXP style toggle button (the margins only appear when the mouse is over)
 * @author Zé Carlos
 */
public class ToolbarSwitch extends JToggleButton {

	public static final long serialVersionUID = 17;
	
	public ToolbarSwitch(String text, String actionCommand) {
		super(text);
		setActionCommand(actionCommand);
		setBorderPainted(false);
		setMargin(new Insets(1,1,1,1));
		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent arg0) {
				if (isEnabled()) {
					setBorderPainted(true);
					repaint();
				}
			}
			
			public void mouseExited(MouseEvent arg0) {
				setBorderPainted(false);
				repaint();
			}
		});
	}
	
	public ToolbarSwitch(ImageIcon icon, String tooltipText, String actionCommand) {
		super(icon);
		setActionCommand(actionCommand);
		setToolTipText(tooltipText);
		setBorderPainted(false);
		setMargin(new Insets(1,1,1,1));
		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent arg0) {
				if (isEnabled()) {
					setBorderPainted(true);
					repaint();
				}
			}
			
			public void mouseExited(MouseEvent arg0) {
				if (!isSelected())
					setBorderPainted(false);
				repaint();
			}
		});
	}
	
	/**
	 * The class contructor
	 * @param imageURL The url of the icon to be displayed on the button
	 * @param toolTipText The text shown to the user when the mouse is over it
	 * @param actionCommand The action to be fired when the user clicks this button
	 * @param cl The ClassLoader that will load the image file
	 */
	public ToolbarSwitch(String imageURL, String toolTipText, String actionCommand, ClassLoader cl) {
		super(new ImageIcon(cl.getResource(imageURL)));
		setToolTipText(toolTipText);
		setActionCommand(actionCommand);
		setBorderPainted(false);
		setMargin(new Insets(1,1,1,1));
		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent arg0) {
				if (isEnabled()) {
					setBorderPainted(true);
					repaint();
				}
			}
			
			public void mouseExited(MouseEvent arg0) {
				setBorderPainted(false);
				repaint();
			}
		});
	}
	
	public ToolbarSwitch(AbstractAction action) {
		super(action);
		setText("");
		setToolTipText(action.getValue(AbstractAction.NAME).toString());
		setBorderPainted(false);
		setMargin(new Insets(1,1,1,1));
		addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent arg0) {
				if (isEnabled()) {
					setBorderPainted(true);
					repaint();
				}
			}
			
			public void mouseExited(MouseEvent arg0) {
				setBorderPainted(false);
				repaint();
			}
		});
		setSelected(true);
	}
	
	public void setState(boolean value) {
	    this.setSelected(value);
	}
	
	public boolean getState() {
	    return this.isSelected();
	}
	
}
