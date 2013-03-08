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
 * $Id:: PropertiesProvider.java 9616 2012-12-30 23:23:22Z pdias          $:
 */
package pt.up.fe.dceg.neptus.gui;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author ZP
 */
public interface PropertiesProvider {
	public DefaultProperty[] getProperties();
	public void setProperties(Property[] properties);
	public String getPropertiesDialogTitle();
	public String[] getPropertiesErrors(Property[] properties);
}
