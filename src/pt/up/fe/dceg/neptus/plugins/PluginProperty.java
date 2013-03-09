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
package pt.up.fe.dceg.neptus.plugins;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Method;

import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

@SuppressWarnings("all")
public class PluginProperty extends DefaultProperty {

    private static final long serialVersionUID = 1L;
    private boolean editionModeOnly = true;

    public PluginProperty(Property prop) {
        setName(prop.getName());
        setType(prop.getClass());
        setDisplayName(prop.getDisplayName());
        setCategory(prop.getCategory());
        setValue(prop.getValue());
        setShortDescription(prop.getShortDescription());
    }

    public PluginProperty(String name, Class<?> clazz, Object value) {
        setName(name);
        setDisplayName(name);
        setShortDescription(name);		
        setType(clazz);
        setValue(value);
    }

    public void enableEditByUser() {
        editionModeOnly = false;
    }

    public boolean isEditByUser() {
        return !editionModeOnly;
    }


    /**
     * @return the current value of the property as a String
     */
    public String serialize() {


        if (getType().equals(IMCMessage.class)) {
            IMCMessage m = (IMCMessage)getValue();
            if (m == null || m.getMgid() == 65535)
                return "";	                
            return m.asXml(true);
        }
        else
            return getValue() != null ? getValue().toString() : null;
    }

    public void unserialize(String value) throws Exception {

        // If it's a String we're ok...
        if (getType().equals(String.class) ) {
            setValue(value);
            return;
        }

        if (getType().equals(File.class)) {
            setValue(new File(value));
            return;
        }

        if (getType().equals(Color.class)) {
            String color = value.replace("java.awt.Color[", "");
            color = color.replaceAll("[\\]rgb=]", "");
            String[] rgb = color.split(",");
            setValue(new Color(Integer.parseInt(rgb[0]),Integer.parseInt(rgb[1]),Integer.parseInt(rgb[2])));
            return;
        }

        if (ReflectionUtil.hasInterface(getType(), PropertyType.class)) {
            PropertyType pt = (PropertyType)getValue();
            pt.fromString(value);
            setValue(pt);
            return;
        }

        if (getType().getEnumConstants() != null) {
            setValue(Enum.valueOf((Class<? extends Enum>) getType(), value));
            return;
        }

        if (getType().equals(Boolean.class) || getType().equals(Boolean.TYPE)) {
            setValue(value.equalsIgnoreCase("true") || value.equals("1"));
            return;
        }

        if (getType().equals(Long.TYPE) || getType().equals(Integer.TYPE) || getType().equals(Short.TYPE) || getType().equals(Byte.TYPE)) {
            try {
                Double val = Double.parseDouble(value);
                setValue(val.longValue());
                return;
            }
            catch (Exception e) {
                e.printStackTrace();
            }		    
        }

        if (getType().equals(Double.TYPE) || getType().equals(Float.TYPE)) {
            try {
                Double val = Double.parseDouble(value);
                setValue(val);
                return;
            }
            catch (Exception e) {
                e.printStackTrace();
            }            
        }

        if (IMCMessage.class.isAssignableFrom(getType())) {
            try {
                IMCMessage m = IMCDefinition.getInstance().create(getType());
                m.parseXml(value);
                setValue(m);
            }
            catch (Exception e) {
                e.printStackTrace();
            } 
            return;
        }

        // if it's a Number then it has a method for parsing its string representation
        try {
            Method m = getType().getMethod("valueOf", String.class);
            Object o = m.invoke(value);
            setValue(o);
            return;
        }
        catch (Exception e) {

        }

        try {
            Method m = getType().getMethod("valueOf", String.class);
            Object o = m.invoke(getValue(), value);
            setValue(o);			
            return;
        }
        catch (Exception e) {
            
            throw new Exception ("Object type not supported: "+getName() + "@"+ getType().getSimpleName(), e);
        }


    }

    public void validate(Object value) throws Exception { }
}
