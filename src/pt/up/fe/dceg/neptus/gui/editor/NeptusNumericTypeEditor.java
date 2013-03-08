/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 2008/04/13
 * $Id:: NeptusNumericTypeEditor.java 9616 2012-12-30 23:23:22Z pdias     $:
 */
package pt.up.fe.dceg.neptus.gui.editor;

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
//		System.out.println("Created a numericeditor!");
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
