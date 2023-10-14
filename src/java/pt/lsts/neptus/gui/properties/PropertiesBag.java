/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.gui.properties;

import java.util.LinkedHashMap;
import java.util.Vector;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.types.coord.LocationType;

public class PropertiesBag implements PropertiesProvider {

	private String propertiesDialogTitle = "Properties";
	private LinkedHashMap<String, DefaultProperty> properties = new LinkedHashMap<String, DefaultProperty>();
	
	@Override
	public DefaultProperty[] getProperties() {
		return properties.values().toArray(new DefaultProperty[0]);
	}

	public void addProperty(DefaultProperty dp) {
		properties.put(dp.getName(), dp);
	}
	
	public void setProperty(String key, Object value) {
		if (properties.containsKey(key))
			properties.get(key).setValue(value);
	}
	
	@Override
	public String getPropertiesDialogTitle() {
		return propertiesDialogTitle;
	}

	@Override
	public String[] getPropertiesErrors(Property[] properties) {		
		
		Vector<String> errors = new Vector<String>();
		
		for (Property p : properties) {
			if (p instanceof VerifyingProperty) {
				VerifyingProperty vp = (VerifyingProperty) p;
				errors.addAll(vp.verifyErrors(p.getValue()));
			}
		}
		
		return null;
	}

	@Override
	public void setProperties(Property[] properties) {
		for (Property p : properties) {
		
			if (p instanceof DefaultProperty)
				this.properties.put(p.getName(), (DefaultProperty)p);
			else {				
				this.properties.put(p.getName(), cloneProperty(p));
			}
		}
	}

	public void setPropertiesDialogTitle(String propertiesDialogTitle) {
		this.propertiesDialogTitle = propertiesDialogTitle;
	}
	
	private static DefaultProperty cloneProperty(Property p) {
		DefaultProperty dp = new DefaultProperty();
		dp.setName(p.getName());
		dp.setDisplayName(p.getDisplayName());
		dp.setCategory(dp.getCategory());
		dp.setShortDescription(p.getShortDescription());
		dp.setParentProperty(p.getParentProperty());
		dp.setEditable(p.isEditable());
		dp.setType(p.getType());
		for (Property sp : p.getSubProperties())
			dp.addSubProperty(sp);
		dp.setValue(p.getValue());
		return dp;
	}
	
	public PropertiesBag clone() {
		PropertiesBag clone = new PropertiesBag();
		clone.setPropertiesDialogTitle(getPropertiesDialogTitle());
		LinkedHashMap<String, DefaultProperty> props = new LinkedHashMap<String, DefaultProperty>();
		
		for (String key : properties.keySet()) {
			props.put(key, cloneProperty(properties.get(key)));
		}
		
		clone.properties = props;		
		return clone;
		
	}
	
	
	public LocationType getLocationProperty(String key) {
		DefaultProperty dp = properties.get(key);
		if (dp != null && dp.getValue() instanceof LocationType)
			return new LocationType(((LocationType) dp.getValue()));
		else
			return null;
	}
	
	public double getDoubleProperty(String key) {
		DefaultProperty dp = properties.get(key);
		if (dp.getValue() instanceof Number)
			return ((Number)dp.getValue()).doubleValue();
		else
		return Double.NaN; 
	}
	
	public float getFloatProperty(String key) {
		DefaultProperty dp = properties.get(key);
		if (dp != null && dp.getValue() instanceof Number)
			return ((Number)dp.getValue()).floatValue();
		else
		return Float.NaN; 
	}
	
	public int getIntProperty(String key) {
		DefaultProperty dp = properties.get(key);
		if (dp != null && dp.getValue() instanceof Number)
			return ((Number)dp.getValue()).intValue();
		else
		return 0; 
	}
 	
	public String getStringProperty(String key) {
		DefaultProperty dp = properties.get(key);
		if (dp != null)
			return dp.getValue().toString();
		else
			return null;
	}
	
	public Object getPropertyValue(String key) {
		DefaultProperty dp = properties.get(key);
		if (dp != null)
			return dp.getValue();
		return null;
	}
	
	public Vector<DefaultProperty> getPropertiesAsVector() {
		Vector<DefaultProperty> vec = new Vector<DefaultProperty>();
		
		for (DefaultProperty dp : properties.values())
			vec.add(dp);
		
		return vec;
	}
	
}
