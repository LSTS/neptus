/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.plugins;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Method;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.util.ReflectionUtil;

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
        
        if (getType().equals(ColorMap.class)) {
            String color = value;
            if (ColorMapFactory.colorMapNamesList.contains(color)) {
                ColorMap colorMap = ColorMapFactory.getColorMapByName(color);
                setValue(colorMap);
            }
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
