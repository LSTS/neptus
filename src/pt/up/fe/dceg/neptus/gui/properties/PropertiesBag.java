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
 * $Id:: PropertiesBag.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.gui.properties;

import java.util.LinkedHashMap;
import java.util.Vector;

import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

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
