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
 * $Id:: SensorPropertiesProvider.java 9616 2012-12-30 23:23:22Z pdias    $:
 */
package pt.up.fe.dceg.neptus.renderer3d;

import pt.up.fe.dceg.neptus.gui.PropertiesProvider;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

public class SensorPropertiesProvider  implements PropertiesProvider{

	public SensorObj sensorObj;

	public SensorPropertiesProvider(SensorObj sObj)
	{
		sensorObj=sObj;
	}
	
	@Override
	public DefaultProperty[] getProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPropertiesDialogTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getPropertiesErrors(Property[] properties) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setProperties(Property[] properties) {
		// TODO Auto-generated method stub
		
	}
	

}
