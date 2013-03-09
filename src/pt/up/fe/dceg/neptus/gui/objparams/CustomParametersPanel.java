/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Nov 17, 2011
 */
package pt.up.fe.dceg.neptus.gui.objparams;

import java.awt.BorderLayout;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

public class CustomParametersPanel extends ParametersPanel {
	
	private static final long serialVersionUID = 6373633755033930713L;

	protected PropertySheetPanel psp = new PropertySheetPanel();
	
	public CustomParametersPanel(Property[] properties) {		
		psp.setEditorFactory(PropertiesEditor.getPropertyEditorRegistry());        
        psp.setMode(PropertySheet.VIEW_AS_CATEGORIES);
        for (int i = 0; i < properties.length; i++) 
            psp.addProperty(properties[i]);  
        
        setLayout(new BorderLayout());
        
        add(psp, BorderLayout.CENTER);        
	}
	
	public Property[] getProperties() {
	    return psp.getProperties();
	}
	
	
	public String getErrors() {			
		return null;
	}
}