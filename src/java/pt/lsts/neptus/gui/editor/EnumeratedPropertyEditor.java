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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.gui.editor;

import java.util.Set;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.messages.Enumerated;



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
