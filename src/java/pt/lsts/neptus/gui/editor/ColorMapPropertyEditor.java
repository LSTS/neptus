/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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

import javax.swing.JComboBox;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.gui.ColorMapListRenderer;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;

public class ColorMapPropertyEditor extends AbstractPropertyEditor {

	private JComboBox<?> combo = new JComboBox<Object>(ColorMap.cmaps.toArray(new ColorMap[ColorMap.cmaps.size()]));
	
	public ColorMapPropertyEditor() {
		combo.setRenderer(new ColorMapListRenderer());
		editor = combo;
	}
	
	@Override
	public String getAsText() {
		return ((ColorMap)combo.getSelectedItem()).toString();
	}
	
	@Override
	public Object getValue() {	    
		return combo.getSelectedItem();
	}
	
	public static void main(String[] args) throws Exception {
		
		PropertiesEditor.editProperties(new PropertiesProvider() {
			public DefaultProperty[] getProperties() {
					DefaultProperty p = PropertiesEditor.getPropertyInstance("Colormap", ColorMap.class, ColorMapFactory.createAllColorsColorMap(), true);
					return new DefaultProperty[] {p};
						
			}
			
			public String getPropertiesDialogTitle() {
				return "testing";
			}
			
			public void setProperties(Property[] properties) {
				NeptusLog.pub().info("<###> "+properties[0]);
			}
			
			public String[] getPropertiesErrors(Property[] properties) {
				return null;
			}
		}, true);
	}
}
