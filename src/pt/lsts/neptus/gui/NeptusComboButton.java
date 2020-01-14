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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.swing.JRoundButton;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zepinto
 * @author pdias
 */
@SuppressWarnings("serial")
public class NeptusComboButton extends JButton implements ActionListener{
	
	JButton actionButton = new ToolbarButton("images/buttons/undo.png", "undo", "undo");
	ToolbarButton dropButton = new ToolbarButton("images/buttons/drop.png", "", "drop");
	LinkedHashMap<String, Image> images = new LinkedHashMap<String, Image>();	
	LinkedHashMap<String, String> toolTipTexts = new LinkedHashMap<String, String>();	
	LinkedList<String> disabledActions = new LinkedList<String>();	
	
	
	public NeptusComboButton() {
		this(false);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		paintComponents(g);
	}
	
	public NeptusComboButton(boolean useRoundButton) {
		int ss = 26;
		if (useRoundButton)
		{
			actionButton = new JRoundButton("images/buttons/undo.png", "undo", "undo");
			ss = 30;
		}
		
		setBorderPainted(false);

		setLayout(null);
		actionButton.setSize(ss,ss);
		actionButton.setBounds(0,0,ss,ss);
		add(actionButton);
		dropButton.setSize(14, ss);
		dropButton.setBounds(ss,0,14,ss);
		dropButton.setToolTipText("Choose another option");
		add(dropButton, BorderLayout.EAST);
		setPreferredSize(new Dimension(ss+14, ss));
		setMinimumSize(new Dimension(ss+14, ss));
		setMaximumSize(new Dimension(ss+14, ss));
		
		dropButton.addActionListener(this);
		actionButton.addActionListener(this);
		
		MouseListener ml = new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {			
				super.mouseEntered(e);
				dropButton.setBorderPainted(true);
				actionButton.setBorderPainted(true);
			}
			
			@Override
			public void mouseExited(MouseEvent e) {			
				super.mouseExited(e);
				dropButton.setBorderPainted(false);
				actionButton.setBorderPainted(false);
			}
		};
		
		dropButton.addMouseListener(ml);
		actionButton.addMouseListener(ml);
	}
	
	public void actionPerformed(ActionEvent e) {
		if ("drop".equalsIgnoreCase(e.getActionCommand())) {
			final JPopupMenu popup = new JPopupMenu();
			popup.setLightWeightPopupEnabled(false);
			for (String action : toolTipTexts.keySet().toArray(new String[] {})) {
				if (disabledActions.contains(action))
					continue;
				AbstractAction act = new AbstractAction(action, new ImageIcon(images.get(action))) {
					public void actionPerformed(ActionEvent e) {
						String action = e.getActionCommand();
						actionButton.setActionCommand(action);
						actionButton.setIcon(new ImageIcon(images.get(action)));
						actionButton.setToolTipText(toolTipTexts.get(action));
						fireActionPerformed(new ActionEvent(this, ActionEvent.RESERVED_ID_MAX+1, action));						
					}
				};
				act.putValue(Action.SHORT_DESCRIPTION, toolTipTexts.get(action));				
				popup.add(act);		
			}
			popup.show(this, 26, 26);
		}
		else {
			fireActionPerformed(e);			
		}		
	}
	
	/**
	 * @param action
	 * @param tooltipText
	 * @param image
	 */
	public void addAction(String action, String tooltipText, Image image) {
		toolTipTexts.put(action, tooltipText);
		images.put(action, image);
		if (toolTipTexts.size() == 1) {
			actionButton.setIcon(new ImageIcon(image));
			actionButton.setToolTipText(tooltipText);
			actionButton.setActionCommand(action);
		}
	}
	
	
	@Override
	public void setEnabled(boolean b) {		
		super.setEnabled(b);
		actionButton.setEnabled(b);
		dropButton.setEnabled(b);		
	}

	/**
	 * @param action
	 * @param b
	 */
	public void setEnabledAction(String action, boolean b) {		
		if (toolTipTexts.get(action) == null)
			return;
		if (b)
			disabledActions.remove(action);
		else
			if (!disabledActions.contains(action))
				disabledActions.add(action);
		
		fixGui();
	}
	
	/**
	 * 
	 */
	private void fixGui() {
		Vector<String> vecEnable = getEnableActions();
		//NeptusLog.pub().info("<###>NeptusComboButton:"+ vecEnable.size());
		if (vecEnable.size() <= 1) {
			dropButton.setEnabled(false);
			dropButton.setVisible(false);
			setPreferredSize(actionButton.getSize());
			setMaximumSize(actionButton.getSize());
			setMinimumSize(actionButton.getSize());
		}
		else
			dropButton.setEnabled(true);
		
		if (vecEnable.size() >= 1) {
			String action = vecEnable.firstElement();
			String ttStr = toolTipTexts.get(action);
			Image image = images.get(action);
			actionButton.setIcon(new ImageIcon(image));
			actionButton.setToolTipText(ttStr);
			actionButton.setActionCommand(action);
			actionButton.setEnabled(true);
		}
		else {
			actionButton.setIcon(new ImageIcon(ImageUtils.getImage("images/buttons/drop.png")));
			actionButton.setToolTipText("No enable actions");
			actionButton.setActionCommand("NO_ACTION");
			actionButton.setEnabled(false);
			
		}
	}

	/**
	 * @return
	 */
	private Vector<String> getEnableActions() {
		Vector<String> vecEnable = new Vector<String>();
		for (String ac : toolTipTexts.keySet()) {
			if (!disabledActions.contains(ac))
				vecEnable.add(ac);
		}
		return vecEnable;
	}
	
	/**
	 * @return
	 */
	public boolean isAnyActionEnable() {
		if (getEnableActions().size() > 0)
			return true;
		return false;
	}

	/**
	 * @param action
	 * @return
	 */
	public boolean isEnabled(String action) {
		return !disabledActions.contains(action);
	}

	public static void main(String[] args) {
		//GuiUtils.setLookAndFeel();
		
		NeptusComboButton but = new NeptusComboButton(false);
		but.addAction("Save File", "Save file", ImageUtils.getImage("images/buttons/save.png"));
		but.addAction("grid", "Show Grid", ImageUtils.getImage("images/buttons/grid.png"));	
		but.setEnabled(true);
		but.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				NeptusLog.pub().info("<###>ACTION: "+e.getActionCommand());
			}
		});
		
		NeptusComboButton but2 = new NeptusComboButton(true);
		but2.addAction("Save File", "Save file", ImageUtils.getImage("images/buttons/save.png"));
		but2.addAction("grid", "Show Grid", ImageUtils.getImage("images/buttons/grid.png"));	
		but2.setEnabled(true);
		but2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				NeptusLog.pub().info("<###>ACTION: "+e.getActionCommand());
			}
		});

		JPanel jp = new JPanel();
		jp.add(but2);
		jp.add(but);
		GuiUtils.testFrame(jp, "test");
		but2.setEnabledAction("grid", true);
		but2.setEnabledAction("Save File", false);
	}
}