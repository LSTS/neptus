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
 * $Id:: BitmaskPropertyEditor.java 9616 2012-12-30 23:23:22Z pdias       $:
 */
package pt.up.fe.dceg.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import pt.up.fe.dceg.neptus.gui.BitmaskPanel;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.imc.OperationalLimits;
import pt.up.fe.dceg.neptus.messages.Bitmask;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.beans.editor.FixedButton;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.swing.LookAndFeelTweaks;



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
				System.out.println(properties[0]);
			}
			
			public String[] getPropertiesErrors(Property[] properties) {
				return null;
			}
		}, true);
	}

}
