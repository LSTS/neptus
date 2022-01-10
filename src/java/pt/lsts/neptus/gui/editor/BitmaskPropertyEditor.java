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
package pt.lsts.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.beans.editor.FixedButton;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.swing.LookAndFeelTweaks;

import pt.lsts.imc.OperationalLimits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.BitmaskPanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.messages.Bitmask;



public class BitmaskPropertyEditor extends AbstractPropertyEditor {

	private Bitmask bitmask, auxBitmask;
	public JPanel bitmaskEditor = new JPanel(new BorderLayout(0,0));
	
	protected JTextField textField = new JTextField();
	private JButton button = new FixedButton();
	
	public BitmaskPropertyEditor() {
		
		textField.setEditable(false);
		textField.setBorder(LookAndFeelTweaks.EMPTY_BORDER);
		bitmaskEditor.add(textField, BorderLayout.CENTER);		
		bitmaskEditor.add(button, BorderLayout.EAST);
		
		editor = bitmaskEditor;
		
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {				
				auxBitmask = BitmaskPanel.showBitmaskDialog(auxBitmask);
				textField.setText(auxBitmask.toString());				
			}
		});
	}
	
	@Override
	public void setValue(Object arg0) {
		this.bitmask = (Bitmask) arg0;
		this.auxBitmask = new Bitmask(bitmask.getPossibleValues(), bitmask.getCurrentValue());		
		textField.setText(auxBitmask.toString());
	}
	
	@Override
	public Object getValue() {
		bitmask.setCurrentValue(auxBitmask.getCurrentValue());
		return bitmask;
	}

	public static void main(String[] args) throws Exception {
		
		PropertiesEditor.editProperties(new PropertiesProvider() {
			public DefaultProperty[] getProperties() {
				OperationalLimits msg = new OperationalLimits();
				try {
					DefaultProperty p = PropertiesEditor.getPropertyInstance("test", Bitmask.class, msg.getValue("flags"), true);
					return new DefaultProperty[] {p};
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				return new DefaultProperty[] {};
						
			}
			
			public String getPropertiesDialogTitle() {
				return "testing";
			}
			
			public void setProperties(Property[] properties) {
				NeptusLog.pub().info("<###> "+properties[0]);
			}
			
			public String[] getPropertiesErrors(Property[] properties) {
				return null;
			}
		}, true);
	}

}
