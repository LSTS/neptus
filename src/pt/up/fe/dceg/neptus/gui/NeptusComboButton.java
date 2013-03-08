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
 * $Id:: NeptusComboButton.java 9616 2012-12-30 23:23:22Z pdias           $:
 */
package pt.up.fe.dceg.neptus.gui;

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

import pt.up.fe.dceg.neptus.gui.swing.JRoundButton;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zepinto
 * @author pdias
 * $Id:: NeptusComboButton.java 9616 2012-12-30 23:23:22Z pdias           $:
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
		//System.out.println("NeptusComboButton:"+ vecEnable.size());
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
				System.out.println("ACTION: "+e.getActionCommand());
			}
		});
		
		NeptusComboButton but2 = new NeptusComboButton(true);
		but2.addAction("Save File", "Save file", ImageUtils.getImage("images/buttons/save.png"));
		but2.addAction("grid", "Show Grid", ImageUtils.getImage("images/buttons/grid.png"));	
		but2.setEnabled(true);
		but2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println("ACTION: "+e.getActionCommand());
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