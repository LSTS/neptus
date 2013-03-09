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
 */
package pt.up.fe.dceg.neptus.gui.editor;

import java.awt.BorderLayout;
import java.util.Locale;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.swing.LookAndFeelTweaks;

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
		//System.out.println(list);
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
		//System.out.println("erro aqui :"+(String)combobox.getSelectedItem());
		if ((String)combobox.getSelectedItem()==null) return null;
		return VehiclesHolder.getVehicleById((String)combobox.getSelectedItem());
	}
	
	public void setValue(Object arg0) {
		if (arg0 instanceof VehicleType) {
			combobox.setSelectedItem(arg0);			
		}
	}
}
