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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import pt.up.fe.dceg.neptus.gui.swing.AnglePanel;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.beans.editor.FixedButton;
import com.l2fprod.common.swing.LookAndFeelTweaks;

public class AngleEditorDegs extends AbstractPropertyEditor {

	protected JTextField textField = new JTextField();
	private JButton button = new FixedButton();
	private double angleRads = 0;
	
	private Class<?> valueClass = null;
	
	public AngleEditorDegs() {
		textField.setEditable(false);
		editor = new JPanel(new BorderLayout(0,0));
		((JPanel)editor).add(textField, BorderLayout.CENTER);
		((JPanel)editor).add(button, BorderLayout.EAST);
		
		textField.setBorder(LookAndFeelTweaks.EMPTY_BORDER);
		
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double oldAngle = angleRads;
				double newAngle = AnglePanel.angleDialogRads(textField, angleRads);
				
				setValue(newAngle);
				firePropertyChange(oldAngle, newAngle);
				textField.setText(""+Math.toDegrees(angleRads));				
			}
		});
	}
	
	public Object getValue() {
		if (valueClass == Float.class)
			return new Float(Math.toDegrees(angleRads));
		else
			return new Double(Math.toDegrees(angleRads));
	}
	
	public void setValue(Object arg0) {
		if (arg0 instanceof Number) {			
			if (valueClass == null)
				valueClass = arg0.getClass();
			angleRads = Math.toRadians(((Number)arg0).doubleValue());
			
			textField.setText(""+Math.toDegrees(angleRads) + "\u00B0 deg");
		}
	}
}
