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
 * $Id:: MiniButton.java 9616 2012-12-30 23:23:22Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import pt.up.fe.dceg.neptus.util.ImageUtils;

public class MiniButton extends JPanel {

	private static final long serialVersionUID = 1L;
	//private String iconFileName = "images/buttons/web.png";
	
	private ImageIcon icon = ImageUtils.getIcon("images/buttons/web.png");
	
	private String toolTipText = "";
	private String actionCommand = "";
	private JButton myButton = null;
	private JToggleButton myToggleButton = null;
	private boolean toggle = false;
	private Vector<ActionListener> actionListeners = new Vector<ActionListener>();
	
	public MiniButton() {
		setLayout(new BorderLayout());
		//myButton = new JButton(new ImageIcon(GuiUtils.getImage("images/buttons/web.png")));
		myButton = new JButton(icon);
		myButton.setMargin(new Insets(0,0,0,0));
		add(myButton, BorderLayout.CENTER);
	}

    public void doClick() {
    	if (isToggle())
    		myToggleButton.doClick();
    	else
    		myButton.doClick();
    }

    public void doClick(int pressTime) {
    	if (isToggle())
    		myToggleButton.doClick(pressTime);
    	else
    		myButton.doClick(pressTime);
    }

    @Override
    public void setEnabled(boolean enabled) {
    	if (isToggle())
    		myToggleButton.setEnabled(enabled);
    	else
    		myButton.setEnabled(enabled);
    }
    
	public boolean getState() {
		if (myToggleButton != null)
			return myToggleButton.isSelected();
		
		return false;
	}

	public void setState(boolean state) {
	    if (state != myToggleButton.isSelected())
	        myToggleButton.setSelected(state);
	}

	
	public ImageIcon getIcon() {
		return icon;
	}
	
	public void setIcon(ImageIcon icon) {
		if (myButton != null)
			myButton.setIcon(icon);
		
		if (myToggleButton != null)
			myToggleButton.setIcon(icon);
		
		this.icon = icon;
	}
	
	public boolean isToggle() {
		return toggle;
	}

	public void setToggle(boolean toggle) {
		this.toggle = toggle;
		if (myButton != null && toggle) {
			remove(myButton);
			myButton = null;
			//myToggleButton = new JToggleButton(new ImageIcon(GuiUtils.getImage(iconFileName)));
			myToggleButton = new JToggleButton(icon);
			myToggleButton.setMargin(new Insets(0,0,0,0));
			myToggleButton.setActionCommand(actionCommand);
			myToggleButton.setToolTipText(toolTipText);
			add(myToggleButton, BorderLayout.CENTER);
			for (ActionListener al : actionListeners) {
				myToggleButton.addActionListener(al);
			}
			
		}
		if (myToggleButton != null && !toggle) {
			remove(myToggleButton);
			myToggleButton = null;
			//myButton = new JButton(new ImageIcon(GuiUtils.getImage(iconFileName)));
			myButton = new JButton(icon);
			myButton.setMargin(new Insets(0,0,0,0));
			myButton.setActionCommand(actionCommand);
			myButton.setToolTipText(toolTipText);
			add(myButton, BorderLayout.CENTER);
			for (ActionListener al : actionListeners) {
				myButton.addActionListener(al);
			}
		}
	}

	public String getToolTipText() {
		return toolTipText;
	}

	public void setToolTipText(String toolTipText) {
		this.toolTipText = toolTipText;
		if (myButton != null)
			myButton.setToolTipText(toolTipText);
		if (myToggleButton != null)
			myToggleButton.setToolTipText(toolTipText);
		
	}

	public String getActionCommand() {
		return actionCommand;
	}

	public void setActionCommand(String actionCommand) {
		this.actionCommand = actionCommand;
		if (myButton != null)
			myButton.setActionCommand(actionCommand);
		if (myToggleButton != null)
			myToggleButton.setActionCommand(actionCommand);
	}
	
	public void addActionListener(ActionListener newListener) {
		actionListeners.add(newListener);
		if (myButton != null)
			myButton.addActionListener(newListener);
		if (myToggleButton != null)
			myToggleButton.addActionListener(newListener);
	}
}