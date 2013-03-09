/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 22/Jun/2005
 */
package pt.up.fe.dceg.neptus.mme;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.PropertySheetDialog;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

/**
 * @author ZP
 */
public class MapEditorPreferences {

	public static void editProperties(MissionMapEditor mme) {
		PropertySheetPanel psp = new PropertySheetPanel();

		DefaultProperty property = new DefaultProperty();
		property.setCategory("Visualization");
		property.setDisplayName("Fast Rendering");
		property.setName("fastRender");
		
		property.setEditable(true);
		property.setShortDescription("Disables anti-aliasing, and the full rendering of some objects.");
		property.setType(Boolean.class);
		property.setValue(new Boolean(false));
		psp.addProperty(property);
		
		property = new DefaultProperty();
		property.setCategory("Web Mapping Service");
		property.setDisplayName("Server URL");
		property.setName("wmsURL");
		property.setEditable(true);
		property.setShortDescription("The URL of the WMS server from where to fetch the real world images.");
		property.setType(String.class);
		property.setValue("http://www2.demis.nl/wms/wms.asp?Service=WMS&WMS=BlueMarble&");
		
		property = new DefaultProperty();
		property.setCategory("Web Mapping Service");
		property.setDisplayName("Visible layers");
		property.setName("wmsLayers");
		property.setEditable(true);
		property.setShortDescription("The layers that should be fetched from the WMS server (separated by commas).");
		property.setType(String.class);
		property.setValue("Earth Image, Borders");
		
		
		psp.addProperty(property);
		
		PropertySheetDialog psd= new PropertySheetDialog();
		psd.getContentPane().add(psp);
		psd.setDialogMode(PropertySheetDialog.OK_CANCEL_DIALOG);
		psd.setModal(true);
		psd.getBanner().setTitle("Mission Map Editor Properties");
		psd.getBanner().setSubtitle("Some properties for the Mission Map Editor");
		psd.setTitle("Mission Map Editor Properties");
		
		psd.pack();
		psd.centerOnScreen();
		psd.setVisible(true);
	
	//	Property[] properties = psp.getProperties();
	//	for (int i = 0; i < properties.length; i++) {
	//		System.out.println(properties[i].getName()+"->"+properties[i].getValue());
	//	}

	}
	
	
	public static void main(String[] args) {
	}
}
