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
