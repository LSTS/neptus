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
 * 24/Jun/2005
 * $Id:: NeptusNumericEditor.java 9616 2012-12-30 23:23:22Z pdias         $:
 */
package pt.up.fe.dceg.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JPanel;
import javax.swing.JTextField;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.swing.LookAndFeelTweaks;

/**
 * @author ZP
 */
public class NeptusNumericEditor extends AbstractPropertyEditor {

	protected JTextField textField = new JTextField();
	protected static NumberFormat nf = new DecimalFormat("###.##");
	
	public NeptusNumericEditor() {
		System.out.println("Created a numericeditor!");
		textField.setLocale(Locale.US);
		editor = new JPanel(new BorderLayout(0,0));
		((JPanel)editor).add(textField, BorderLayout.CENTER);
		
		textField.setBorder(LookAndFeelTweaks.EMPTY_BORDER);
	
		textField.addFocusListener(new FocusAdapter() {
			private Double oldVal = null;
			public void focusGained(java.awt.event.FocusEvent arg0) {
				try {
					oldVal = Double.parseDouble(textField.getText());
				}
				catch (Exception e) {
					oldVal = new Double(0);
					textField.setText("0");
				}
			}
			
			public void focusLost(java.awt.event.FocusEvent arg0) {
				try {
					Double newVal = Double.parseDouble(textField.getText());
					firePropertyChange(oldVal, newVal);
				}
				catch (Exception e) {					
					textField.setText(oldVal.toString());
				}
			}
		});
	}
		
	public Object getValue() {
		return Double.parseDouble(textField.getText());
	}
	
	public void setValue(Object arg0) {
		if (arg0 instanceof Double) {
			nf.setGroupingUsed(false);
			textField.setText(nf.format(arg0));			
		}
	}
	
	public static void main(String[] args) {
	}
}
