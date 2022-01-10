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

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import pt.lsts.neptus.gui.checklist.CheckItemPanel;
import pt.lsts.neptus.types.checklist.CheckAutoUserLogItem;
import pt.lsts.neptus.util.ImageUtils;


public class CheckLogItem extends CheckSubItemExe{

	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;

    private CheckAutoUserLogItem checkSubItem;
	
	private JTextArea logRequestTextArea = null;
	
	private JTextArea logMessageTextArea = null;
	
	private JScrollPane listSubScroll = null;
	
	private JPanel logPanel = null;
	
	private JCheckBox check = null;
	
	private Color bg= null;
	
	public CheckLogItem(CheckAutoUserLogItem casi) {
		super();
		checkSubItem=casi;
		
		initialize();
	}

	private void initialize() {
		//this.setBorder(new LineBorder(Color.BLACK));
		//this.setBorder(new LoweredBorder());
		this.setBorder(new EmptyBorder(10, 10, 10, 10));
		this.setMaximumSize(new Dimension(2000,100));
		this.setMinimumSize(new Dimension(300,100));
		this.setLayout(new BorderLayout());
		this.bg=this.getBackground();
		if(checkSubItem.isChecked())
			this.setBackground(CheckItemPanel.CHECK_COLOR);
		this.add(getLogPanel(),BorderLayout.CENTER);
		this.add(getCheck(),BorderLayout.EAST);
	}
	
	private JPanel getLogPanel() {
		if (logPanel == null)
		{
			logPanel = new JPanel();
			logPanel.setLayout(new BorderLayout());
			logPanel.setOpaque(false);
			logPanel.add(getLogRequestTextArea(),BorderLayout.NORTH);
			logPanel.add(getListSubScroll(),BorderLayout.CENTER);
		}
		return logPanel;
	}
	
	private JTextArea getLogRequestTextArea () {
		if (logRequestTextArea == null) {
			logRequestTextArea = new JTextArea();
			logRequestTextArea.setLineWrap(true);
			logRequestTextArea.setEditable(false);
			logRequestTextArea.setOpaque(false);
			logRequestTextArea.setText(checkSubItem.getLogRequest());
		}
		
		return logRequestTextArea;
	}
	
	private JScrollPane getListSubScroll() {
		if (listSubScroll == null) {
			listSubScroll = new JScrollPane();
			listSubScroll.setOpaque(false);
			listSubScroll.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			listSubScroll.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			//listSubScroll.setBorder(javax.swing.BorderFactory.createEmptyBorder(3,5,3,5));
			listSubScroll.setPreferredSize(new java.awt.Dimension(300,50));
			//listSubScroll.setBorder(new LoweredBorder());
			listSubScroll.setBorder(new EmptyBorder(10, 10, 10, 10));
			listSubScroll.setViewportView(getLogMessageTextArea());
		}
		return listSubScroll;
	}
	
	private JTextArea getLogMessageTextArea () {
		if (logMessageTextArea == null) {
			logMessageTextArea = new JTextArea();
			logMessageTextArea.setLineWrap(true);
			logMessageTextArea.setEditable(true);
			
			logMessageTextArea.setText(checkSubItem.getLogMessage());
			
			logMessageTextArea.addKeyListener(new java.awt.event.KeyAdapter()
		    {
		        public void keyTyped(java.awt.event.KeyEvent e)
		        {
		        	CheckLogItem.this.checkSubItem.setLogMessage(CheckLogItem.this.logMessageTextArea.getText());
		        }
		        
		        
		    });
		}
		
		return logMessageTextArea;
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
			
			
			check.addItemListener(new java.awt.event.ItemListener() { 
				public void itemStateChanged(java.awt.event.ItemEvent e) {    
					CheckLogItem.this.checkSubItem.setChecked(check.isSelected());
					if(check.isSelected()==true)
					{
						CheckLogItem.this.checkSubItem.setLogMessage(CheckLogItem.this.logMessageTextArea.getText());
						CheckLogItem.this.setBackground(CheckItemPanel.CHECK_COLOR);
					}
					else
						CheckLogItem.this.setBackground(bg);
					
					CheckLogItem.this.warnCheckSubItemProviders();
					
				}
			});
			check.setSize(30,30);
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
