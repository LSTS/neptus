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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.gui.checklist.exec;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import pt.lsts.neptus.gui.checklist.CheckItemPanel;
import pt.lsts.neptus.types.checklist.CheckAutoUserActionItem;
import pt.lsts.neptus.util.ImageUtils;


public class CheckActionItem extends CheckSubItemExe 
{
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;

    private CheckAutoUserActionItem checkSubItem;
	
	private JTextArea actionTextArea = null;
	
	private JCheckBox check = null;
	
	private Color bg= null;
	
	public CheckActionItem(CheckAutoUserActionItem casi) {
		super();	
		checkSubItem=casi;
		initialize();
		
	
	}

	private void initialize() {
		//this.setBorder(new LineBorder(Color.BLACK));
		//this.setBorder(new LoweredBorder());
	    this.setBorder(new EmptyBorder(10, 10, 10, 10));
		this.setMaximumSize(new Dimension(2000,80));
		this.setMinimumSize(new Dimension(0,80));
		this.setLayout(new BorderLayout());
		this.bg=this.getBackground();
		if(checkSubItem.isChecked())
			this.setBackground(CheckItemPanel.CHECK_COLOR);
		this.add(getActionTextArea(),BorderLayout.CENTER);
		this.add(getCheck(),BorderLayout.EAST);
		
	}
	
	private JTextArea getActionTextArea() {
		if (actionTextArea == null) {
			actionTextArea = new JTextArea();
			actionTextArea.setLineWrap(true);
			actionTextArea.setEditable(false);
			actionTextArea.setOpaque(false);
			actionTextArea.setText(checkSubItem.getAction());
		}
		
		return actionTextArea;
	}
	
	private JCheckBox getCheck() {
		if (check == null) {
			check = new JCheckBox();
			check.setOpaque(false);
			
			
			//check.setText(checkSubItem.getAction());
			check.setSelected(checkSubItem.isChecked());
			//check.setHorizontalTextPosition(SwingConstants.LEFT);
			check.setMargin(new java.awt.Insets(2, 20, 2, 20));
						
			// Set default icon for checkbox
			check.setIcon(ImageUtils.getIcon("images/checklists/boxIcon.png"));
		    // Set selected icon when checkbox state is selected
			check.setSelectedIcon(ImageUtils.getIcon("images/checklists/selectedIcon.png"));
		    // Set disabled icon for checkbox
			check.setDisabledIcon(ImageUtils.getIcon("images/checklists/disabledIcon.png"));
		    // Set disabled-selected icon for checkbox
			check.setDisabledSelectedIcon(ImageUtils.getIcon("images/checklists/disabledSelectedIcon.png"));
		    // Set checkbox icon when checkbox is pressed
			check.setPressedIcon(ImageUtils.getIcon("images/checklists/disabledIcon.png"));
		    // Set icon when a mouse is over the checkbox
			check.setRolloverIcon(ImageUtils.getIcon("images/checklists/boxIcon.png"));
		    // Set icon when a mouse is over a selected checkbox
			check.setRolloverSelectedIcon(ImageUtils.getIcon("images/checklists/selectedIcon.png"));
			
			
			check.addItemListener(new ItemListener() { 
				public void itemStateChanged(ItemEvent e) {    
					
					CheckActionItem.this.checkSubItem.setChecked(check.isSelected());
					
					if(check.isSelected()==true)
						{CheckActionItem.this.setBackground(CheckItemPanel.CHECK_COLOR);
						//checkSubItem.setChecked(true);
						}
					else
						CheckActionItem.this.setBackground(bg);
					
					CheckActionItem.this.warnCheckSubItemProviders();
						
					//@TODO
				}
			});
		}
		return check;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isCheck() {
		return checkSubItem.isChecked();
	}

	
}
