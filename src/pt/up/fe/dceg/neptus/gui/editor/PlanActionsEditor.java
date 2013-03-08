/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2010/06/27
 * $Id:: PlanActionsEditor.java 9615 2012-12-30 23:08:28Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import pt.up.fe.dceg.neptus.mp.actions.PlanActions;
import pt.up.fe.dceg.neptus.mp.actions.PlanActionsPanel;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.beans.editor.FixedButton;
import com.l2fprod.common.swing.LookAndFeelTweaks;

/**
 * @author pdias
 *
 */
public class PlanActionsEditor extends AbstractPropertyEditor {

	protected JTextField textField = new JTextField();
	private JButton button = new FixedButton();

	protected PlanActions actions = new PlanActions();
	
	/**
	 * 
	 */
	public PlanActionsEditor() {
		textField.setEditable(false);
		editor = new JPanel(new BorderLayout(0,0));
		((JPanel)editor).add(textField, BorderLayout.CENTER);
		((JPanel)editor).add(button, BorderLayout.EAST);
		
		textField.setBorder(LookAndFeelTweaks.EMPTY_BORDER);
		
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				LocationType oldLoc = new LocationType();
//				oldLoc.setLocation(locationType);
//				//System.err.println("-<-<-<-<-<Antes:"+oldLoc.getLatitude());
//				
//				LocationType newLoc = LocationPanel.showLocationDialog("Set Location", locationType, null);
//				if (newLoc != null) {
//					setValue(newLoc);
//					//System.err.println("->_>_>_>_>Depois:"+newLoc.getLatitude());
//					//locationType.setLocation(newLoc);
//					firePropertyChange(oldLoc, newLoc);
//				}
				
				PlanActionsPanel.showDialog(actions, editor, "Plan Actions");
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see com.l2fprod.common.beans.editor.AbstractPropertyEditor#getValue()
	 */
	@Override
	public Object getValue() {
		return actions;
	}
	
	/* (non-Javadoc)
	 * @see com.l2fprod.common.beans.editor.AbstractPropertyEditor#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(Object value) {
		if (value instanceof PlanActions) {
			actions = (PlanActions) value;
			actions = (PlanActions) actions.clone();
			textField.setText(actions.toString());
		}
	}
}
