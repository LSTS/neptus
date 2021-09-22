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
 * 24/Jun/2005
 */
package pt.lsts.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JPanel;
import javax.swing.JTextField;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.swing.LookAndFeelTweaks;

import pt.lsts.neptus.NeptusLog;

/**
 * @author ZP
 */
public class NeptusNumericEditor extends AbstractPropertyEditor {

	protected JTextField textField = new JTextField();
	protected static NumberFormat nf = new DecimalFormat("###.##");
	
	public NeptusNumericEditor() {
		NeptusLog.pub().info("<###>Created a numericeditor!");
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
					oldVal = Double.valueOf(0);
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
