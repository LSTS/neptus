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
 * $Id:: RenderSelectionEditor.java 9616 2012-12-30 23:23:22Z pdias       $:
 */
package pt.up.fe.dceg.neptus.gui.editor;

import java.awt.BorderLayout;
import java.util.Locale;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.swing.LookAndFeelTweaks;




public class RenderSelectionEditor extends AbstractPropertyEditor {
	
	protected JComboBox<?> combobox;
	
	public RenderSelectionEditor() {
		
		
		
		String[] list=RenderType.list;
		//System.err.println(list[3]);
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
					//String newVal = (String) combobox.getSelectedItem();
					//firePropertyChange(RenderType.getRenderType(oldVal),RenderType.getRenderType(newVal));
				}
				catch (Exception e) {					
					 //combobox.setSelectedItem(oldVal);
				}
			}
		});	*/
	}
	
	public Object getValue() {
		System.out.println("erro aqui :"+(String)combobox.getSelectedItem());
		if ((String)combobox.getSelectedItem()==null) return null;
		return RenderType.getRenderType((String)combobox.getSelectedItem());
	}
	
	public void setValue(Object arg0) {
		if (arg0 instanceof RenderType) {
			combobox.setSelectedItem(arg0);			
		}
	}
}
