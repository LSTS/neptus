/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 2010/06/27
 */
package pt.lsts.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.beans.editor.FixedButton;
import com.l2fprod.common.swing.LookAndFeelTweaks;

import pt.lsts.neptus.mp.actions.PlanActions;
import pt.lsts.neptus.mp.actions.PlanActionsPanel;

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
