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
package pt.lsts.neptus.gui.checklist;

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

import pt.lsts.neptus.types.checklist.CheckAutoSubItem;
import pt.lsts.neptus.types.checklist.CheckAutoUserLogItem;

public class UserCommentItem extends JPanel implements CheckSubItem {
	
	private static final long serialVersionUID = 4053699765892712805L;

	public static final String TYPE_ID = "userLog";

	private AutoItemsList parent = null;

	private JTextField logRequest = null;
	private JTextField logMessage = null;

	private JButton remove = null;

	private JCheckBox check = null;
	
	
	public UserCommentItem(AutoItemsList p, CheckAutoUserLogItem cauli) 	{
		this(p);
		fillFromCheckAutoUserLogItem(cauli);
	}
	
	public UserCommentItem(AutoItemsList p) {
		super();
		parent = p;
		initialize();
	}
	
	private void initialize() {
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.setOpaque(false);
		logRequest = new JTextField();
		logRequest.setColumns(20);
		logRequest.addKeyListener(new KeyAdapter() {
	        @Override
	        public void keyReleased(KeyEvent e) {
                parent.fireChangeEvent(UserCommentItem.this);
	        }
	    });
		
		this.add(new JLabel("User Comment: "));
		this.add(logRequest);
		
		logMessage = new JTextField();
		logMessage.setColumns(20);
		logMessage.addKeyListener(new KeyAdapter() {
	        @Override
	        public void keyReleased(KeyEvent e) {
                parent.fireChangeEvent(UserCommentItem.this);
	        }
	    });

		
		this.add(new JLabel(" Comment: "));
		this.add(logMessage);
	
		remove = new JButton(ICON_CLOSE);
		remove.setMargin(new Insets(0,0,0,0));
		remove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
                parent.removeUserCommentItem(UserCommentItem.this);
			}
		});
		
		this.add(new JLabel(" Checked:"));
		this.add(getCheck());
		
		this.add(remove);
	}
	
	private JCheckBox getCheck()
	{
		if (check == null) {
			check = new JCheckBox("check");
			check.setOpaque(false);
			check.setText(" ");
			check.addItemListener(new java.awt.event.ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					parent.fireChangeEvent(UserCommentItem.this);
				} 
			});
		}
		return	check; 
	}
	
	private void fillFromCheckAutoUserLogItem(CheckAutoUserLogItem cauli) {
		check.setSelected(cauli.isChecked());
		logRequest.setText(cauli.getLogRequest());
		logMessage.setText(cauli.getLogMessage());
		
	}

	@Override
	public CheckAutoSubItem getCheckAutoSubItem() {
		CheckAutoUserLogItem ret=new CheckAutoUserLogItem();
		ret.setChecked(getCheck().isSelected());
		ret.setLogMessage(logMessage.getText());
		ret.setLogRequest(logRequest.getText());
		return ret;
	}
	
}
