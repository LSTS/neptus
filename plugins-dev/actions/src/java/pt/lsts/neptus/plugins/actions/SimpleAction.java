/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 2009/06/11
 */
package pt.lsts.neptus.plugins.actions;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JToggleButton;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * This class eases the creation of console buttons
 * @author ZP
 */
public abstract class SimpleAction extends ConsolePanel implements ActionListener {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    protected AbstractButton btn = null;
	
	/**
	 * Default is <b>false</b>. Override and return <b>true</b> for a toggle button. 
	 * @return Whether to use a toggle button.
	 */
	protected boolean isSwitch(){
		return false;
	}
	
	/**
	 * Defaults to <b>false</b> which shows a mini-button (icon only). <br>
	 * Override if a normal button is preferred.
	 * @return Whether button text is visible to the user or not
	 */
	protected boolean showText() {
		return false;
	}
	
	/**
	 * Retrieves the current state of the associated button.
	 * @return Current state of the associated button.
	 */
	public boolean isSelected() {
		return btn.isSelected();
	}
	
	public SimpleAction(ConsoleLayout console) {
	    super(console);
		if (isSwitch())
			btn = new JToggleButton(ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(getClass()), 20,20));
		else
			btn = new JButton(ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(getClass()), 20,20));
		
		btn.setMargin(new Insets(0,0,0,0));
		btn.setToolTipText(PluginUtils.getPluginDescription(getClass()));
		btn.setFocusPainted(false);
		btn.addActionListener(this);		
		if (showText())
			btn.setText(getName());
		setLayout(new BorderLayout());
		add(btn, BorderLayout.CENTER);		
		setSize(getPreferredSize().width+8,getPreferredSize().height+8);
	}	
}
