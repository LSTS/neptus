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

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author rjpg
 *
 */
public class AutoItemsList extends JPanel{
	
	private static final long serialVersionUID = -7753351429507075143L;

    private static final ImageIcon ICON_ADD = new ImageIcon(ImageUtils.getScaledImage(
            "images/checklists/add.png", 16, 16));

	private JComboBox<?> optionsList = null;
	private String [] optionsListString;
	
	private JButton addAutoCheckItem = null;

	private CheckItemPanel parentCheckItemPanel;
		
	public AutoItemsList(CheckItemPanel p) {
		super();
		parentCheckItemPanel = p;
		initialize();
	}

	private void initialize() {
		
		this.setOpaque(false);
		
		addAutoCheckItem = new JButton("Add", ICON_ADD);
		addAutoCheckItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int Selection;
			    Selection = getOptionsList().getSelectedIndex();
			    if (Selection == 0) {
			    	AutoItemsList.this.add(new VariableIntervalItem(AutoItemsList.this));
			    }
			    else if(Selection == 1) {
			    	AutoItemsList.this.add(new UserActionItem(AutoItemsList.this));
			    }
			    else if(Selection == 2) {
			    	AutoItemsList.this.add(new UserCommentItem(AutoItemsList.this));
			    }
				repaintCheck();
				fireChangeEvent(AutoItemsList.this);
	        }
		});
		
		JPanel title=new JPanel();
		title.setLayout(new FlowLayout(FlowLayout.LEFT));
		title.setOpaque(false);
		/*title.add(new JLabel("Variable name"),null);
		title.add(new JLabel("[Start  "),null);
		title.add(new JLabel("  End]"),null);
		title.add(new JLabel(" Out of"),null);*/
		title.add(addAutoCheckItem);
		title.add(getOptionsList());
		
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(title);
	}
	
	private JComboBox<?> getOptionsList() {
		if(optionsList==null) {
			optionsListString = new String [3];
			optionsListString[0] = "Variable Test";
			optionsListString[1] = "User Action";
			optionsListString[2] = "User Comment";
		    
			optionsList = new JComboBox<Object> (optionsListString);
		}
		return optionsList;
	}
	
	public void removeVarIntervalItem(VariableIntervalItem vti) {
		this.remove(vti);
		repaintCheck();
		fireChangeEvent(vti);
	}
	
	public void removeUserActionItem(UserActionItem uai) {
		this.remove(uai);
		repaintCheck();
		fireChangeEvent(uai);
	}
	
	public void removeUserCommentItem(UserCommentItem uci) {
		this.remove(uci);
		repaintCheck();
		fireChangeEvent(uci);
	}
	
	public void repaintCheck() {
		Component cmp = parentCheckItemPanel.getParent();
		while(cmp != null) {
			cmp.doLayout();
			cmp.invalidate();
			cmp.validate();				
			cmp = cmp.getParent();
		}
	}
	
	public int numberOfSubItems() {
        Component[] list = getComponents();
        int count = 0;
        for(Component c : list) {
        	try {			
        		@SuppressWarnings("unused")
				CheckSubItem si = (CheckSubItem) c;
        		count++;
        	}
        	catch (Exception e2) {
        		//e2.printStackTrace();
        	}
        }
        return count;
	}
	
	void fireChangeEvent(Component source) {
	    parentCheckItemPanel.fireChangeEvent(source);    
	}

}