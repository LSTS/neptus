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
 * $Id:: LocationTypePropertyEditor.java 9616 2012-12-30 23:23:22Z pdias  $:
 */
package pt.up.fe.dceg.neptus.gui.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import pt.up.fe.dceg.neptus.gui.LocationPanel;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.beans.editor.FixedButton;
import com.l2fprod.common.swing.LookAndFeelTweaks;

/**
 * @author ZP
 */
public class LocationTypePropertyEditor extends AbstractPropertyEditor {

	protected JTextField textField = new JTextField();
	private JButton button = new FixedButton();
	LocationType locationType = new LocationType();
	public LocationTypePropertyEditor() {
		textField.setEditable(false);
		editor = new JPanel(new BorderLayout(0,0));
		((JPanel)editor).add(textField, BorderLayout.CENTER);
		((JPanel)editor).add(button, BorderLayout.EAST);
		
		textField.setBorder(LookAndFeelTweaks.EMPTY_BORDER);
		
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocationType oldLoc = new LocationType();
				oldLoc.setLocation(locationType);
				//System.err.println("-<-<-<-<-<Antes:"+oldLoc.getLatitude());
				
				LocationType newLoc = LocationPanel.showLocationDialog(editor, "Set Location", locationType, null, true);
				if (newLoc != null) {
					setValue(newLoc);
					//System.err.println("->_>_>_>_>Depois:"+newLoc.getLatitude());
					//locationType.setLocation(newLoc);
					firePropertyChange(oldLoc, newLoc);
					textField.setText(locationType.toString());
				}
			}
		});
	}
	
	public Object getValue() {
		return locationType;
	}
	
	public void setValue(Object arg0) {
		if (arg0 instanceof LocationType) {
			locationType.setLocation((LocationType) arg0);
			textField.setText(locationType.toString());
		}
	}
	
	public static void main(String[] args) {
	}
}
