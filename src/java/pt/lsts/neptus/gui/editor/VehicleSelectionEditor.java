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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.gui.editor;

import java.awt.BorderLayout;
import java.util.Locale;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.swing.LookAndFeelTweaks;

import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

/**
 * 
 * @author RJPG
 * Selecting vehicles of neptus on propreties panel whith combobox
 *
 */
public class VehicleSelectionEditor extends AbstractPropertyEditor {
	
	protected JComboBox<?> combobox;
	
	public VehicleSelectionEditor() {
			
		String[] list=VehiclesHolder.getVehiclesArray();
		//NeptusLog.pub().info("<###> "+list);
		combobox = new JComboBox<Object>(list);
		editor = new JPanel(new BorderLayout(0,0));
		combobox.setLocale(Locale.US);
		((JPanel)editor).add(combobox, BorderLayout.CENTER);
		combobox.setBorder(LookAndFeelTweaks.EMPTY_BORDER);
		combobox.setVisible(true);
		
		/*combobox.addFocusListener(new FocusAdapter() {
			private String oldVal = null;
			public void focusGained(java.awt.event.FocusEvent arg0) {
				try {
					//oldVal = (String) combobox.getSelectedItem();
				}
				catch (Exception e) {
					//oldVal = new Double(0);
					//textField.setText("0");
				}
			}
			
			public void focusLost(java.awt.event.FocusEvent arg0) {
				try {
				//	String newVal = (String) combobox.getSelectedItem();
					//firePropertyChange(VehiclesHolder.getVehicleById(oldVal),VehiclesHolder.getVehicleById(newVal));
				}
				catch (Exception e) {					
					 //combobox.setSelectedItem(oldVal);
				}
			}
		});	*/
	}
	
	public Object getValue() {
		//NeptusLog.pub().info("<###>erro aqui :"+(String)combobox.getSelectedItem());
		if ((String)combobox.getSelectedItem()==null) return null;
		return VehiclesHolder.getVehicleById((String)combobox.getSelectedItem());
	}
	
	public void setValue(Object arg0) {
		if (arg0 instanceof VehicleType) {
			combobox.setSelectedItem(arg0);			
		}
	}
}
