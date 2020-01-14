/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 2008/04/13
 */
package pt.lsts.neptus.gui.editor;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

/**
 * @author pdias
 *
 */
@Deprecated
public class NeptusNumericTypeEditor extends AbstractPropertyEditor {
//FIXME TO DELETE
	
//	protected JTextField textField = new JTextField();
//	protected static NumberFormat nf = new DecimalFormat("###.##");
//	protected NativeNumber nativeType = null;
//	
//	public NeptusNumericTypeEditor() {
//		NeptusLog.pub().info("<###>Created a numericeditor!");
//		textField.setLocale(Locale.US);
//		editor = new JPanel(new BorderLayout(0,0));
//		((JPanel)editor).add(textField, BorderLayout.CENTER);
//		
//		textField.setBorder(LookAndFeelTweaks.EMPTY_BORDER);
//	
//		textField.addFocusListener(new FocusAdapter() {
//			private Double oldVal = null;
//			public void focusGained(java.awt.event.FocusEvent arg0) {
//				try {
//					oldVal = Double.parseDouble(textField.getText());
//				}
//				catch (Exception e) {
//					oldVal = new Double(0);
//					textField.setText("0");
//				}
//			}
//			
//			public void focusLost(java.awt.event.FocusEvent arg0) {
//				try {
//					Double newVal = Double.parseDouble(textField.getText());
//					firePropertyChange(oldVal, newVal);
//				}
//				catch (Exception e) {					
//					textField.setText(oldVal.toString());
//				}
//			}
//		});
//	}
//		
//	public Object getValue() {
//		nativeType.setValue(Double.parseDouble(textField.getText()));
//		return nativeType;
//	}
//	
//	public void setValue(Object arg0) {
//		if (arg0 instanceof Double) {
//			nf.setGroupingUsed(false);
//			textField.setText(nf.format(arg0));			
//		}
//		else if ((arg0 instanceof NativeDOUBLE) || (arg0 instanceof NativeFLOAT)) {
//			nf.setGroupingUsed(false);
//			textField.setText(nf.format(arg0));
//		}
//		
//		else {
//			nf.setGroupingUsed(false);
//			nf.setParseIntegerOnly(true);
//		}
//		nativeType = (NativeNumber)arg0;
//	}
//	
}
