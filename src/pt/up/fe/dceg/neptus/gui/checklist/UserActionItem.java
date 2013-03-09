/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * Author: Rui Gonçalves
 * 200?/??/??
 */
package pt.up.fe.dceg.neptus.gui.checklist;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import pt.up.fe.dceg.neptus.types.checklist.CheckAutoSubItem;
import pt.up.fe.dceg.neptus.types.checklist.CheckAutoUserActionItem;

@SuppressWarnings("serial")
public class UserActionItem extends JPanel implements CheckSubItem{
	
	public static final String TYPE_ID = "userAction";

	private AutoItemsList parent = null;

	private JTextField userMsgActionText = null;
	private JButton remove = null;
	private JCheckBox check = null;
	
	public UserActionItem(AutoItemsList p, CheckAutoUserActionItem cauai) {
		this(p);
		fillFromCheckAutoUserActionItem(cauai);
	}
	
	public UserActionItem(AutoItemsList p) {
		super();
		parent = p;
		initialize();
	}
	
	private void initialize() {
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.setOpaque(false);
		userMsgActionText = new JTextField();
		userMsgActionText.setColumns(20);
		userMsgActionText.addKeyListener(new KeyAdapter() {
	        @Override
	        public void keyReleased(KeyEvent e) {
                parent.fireChangeEvent(UserActionItem.this);
	        }
	    });
		
		this.add(new JLabel("User Action:"));
		this.add(userMsgActionText);
	
		remove = new JButton(ICON_CLOSE);
		remove.setMargin(new Insets(0,0,0,0));
		remove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
                parent.removeUserActionItem(UserActionItem.this);
			}
		});
		
		this.add(new JLabel(" Checked:"));
		this.add(getCheck());
		
		this.add(remove);
	}
	
	private JCheckBox getCheck() {
		if (check == null) {
			check = new JCheckBox("check");
			check.setOpaque(false);
			check.setText(" ");
			check.addItemListener(new java.awt.event.ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					parent.fireChangeEvent(UserActionItem.this);
				} 
			});
		}
		return	check; 
	}
	
	private void fillFromCheckAutoUserActionItem(CheckAutoUserActionItem cauai) {
		check.setSelected(cauai.isChecked());
		userMsgActionText.setText(cauai.getAction());
	}
	
	public CheckAutoSubItem getCheckAutoSubItem() {
		CheckAutoUserActionItem ret = new CheckAutoUserActionItem();
		ret.setAction(userMsgActionText.getText());
		ret.setChecked(getCheck().isSelected());
		return ret;
	}
}
