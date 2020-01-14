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

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import pt.lsts.neptus.util.ImageUtils;

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