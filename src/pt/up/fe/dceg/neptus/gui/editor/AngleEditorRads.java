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

public class AngleEditorRads extends AbstractPropertyEditor {

	protected JTextField textField = new JTextField();
	private JButton button = new FixedButton();
	private double angleRads = 0;
	protected boolean showDegrees = false;
	
	private Class<? extends Object> valueClass = null;

	public AngleEditorRads() {
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
                textField.setText("" + (showDegrees ? Math.toDegrees(newAngle)
                        : newAngle) + (showDegrees?"\u00B0":" rad"));
			}
		});
	}
	
	/* (non-Javadoc)
	 * @see com.l2fprod.common.beans.editor.AbstractPropertyEditor#getAsText()
	 */
	@Override
	public String getAsText() {
        return "" + (showDegrees ? Math.toDegrees(angleRads)
                : angleRads) + (showDegrees?"\u00B0":" rad");
	}
	
	public Object getValue() {
		if (valueClass == Float.class)
			return new Float(angleRads);
		else
			return new Double(angleRads);
	}
	
	public void setValue(Object arg0) {
		if (arg0 instanceof Number) {
			if (valueClass == null)
				valueClass = arg0.getClass();
			angleRads = ((Number)arg0).doubleValue();
            textField.setText("" + (showDegrees ? Math.toDegrees(angleRads)
                    : angleRads) + (showDegrees?"\u00B0 deg":" rad"));
		}
	}
}
