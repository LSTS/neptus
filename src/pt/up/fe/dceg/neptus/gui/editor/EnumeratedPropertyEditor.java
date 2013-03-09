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

import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.messages.Enumerated;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;



public class EnumeratedPropertyEditor extends AbstractPropertyEditor {

	private JComboBox<Object> combo = new JComboBox<Object>(new Object[] {});
	private boolean initialized = false;
	private Enumerated enumeratedObj = null;
	
	public EnumeratedPropertyEditor() {
		editor = combo;
	}
	
	@Override
	public void setValue(Object arg0) {
		enumeratedObj = (Enumerated) arg0;
		
		if (!initialized) {			
			combo.setModel(new DefaultComboBoxModel<Object>(enumeratedObj.getPossibleValues().values().toArray()));
		}
		
		combo.setSelectedItem(enumeratedObj.getPossibleValues().get(enumeratedObj.getCurrentValue()));
	}
	
	@Override
	public Object getValue() {
	
		if (combo.getSelectedIndex() == -1)
			return null;			
		
        Long curKey = (Long) ((Set<?>)enumeratedObj.getPossibleValues().keySet()).toArray(new Long[] {})[combo.getSelectedIndex()];
		try {
			enumeratedObj.setCurrentValue(curKey);
		}
		catch (Exception e) {
			NeptusLog.pub().error(e);
		}
		
		return enumeratedObj;
	}
	
}
