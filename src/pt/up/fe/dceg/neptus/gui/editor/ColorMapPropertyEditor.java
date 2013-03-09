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

import javax.swing.JComboBox;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.gui.ColorMapListRenderer;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;



public class ColorMapPropertyEditor extends AbstractPropertyEditor {

	private JComboBox<?> combo = new JComboBox<Object>(ColorMap.cmaps);
	
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
				System.out.println(properties[0]);
			}
			
			public String[] getPropertiesErrors(Property[] properties) {
				return null;
			}
		}, true);
	}
}
