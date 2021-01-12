/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;

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
        else if (getType().isArray()) {
            Class compType = getType().getComponentType();
            if (getType().getComponentType().isPrimitive()) {
                if (compType == Long.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((long[]) getValue()));
                else if (compType == Integer.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((int[]) getValue()));
                else if (compType == Short.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((short[]) getValue()));
                else if (compType == Double.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((double[]) getValue()));
                else if (compType == Float.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((float[]) getValue()));
                else if (compType == Boolean.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((boolean[]) getValue()));
                else if (compType == Byte.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((byte[]) getValue()));
                else if (compType == Character.TYPE)
                    return Arrays.toString(ArrayUtils.toObject((char[]) getValue()));
                else
                    return Arrays.toString((Object[]) getValue());
            }
            else {
                return Arrays.toString((Object[]) getValue());
            }
        }
        else {
            return getValue() != null ? getValue().toString() : null;
        }
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
            setValue(parseColor(value));
            return;
        }
        
        if (getType().equals(ColorMap.class)) {
            ColorMap colorMap = parseColorMap(value);
            if (colorMap != null)
                setValue(colorMap);
            return;
        }

        if (ReflectionUtil.hasInterface(getType(), PropertyType.class)) {
            PropertyType pt = (PropertyType)getValue();
            pt.fromString(value);
            setValue(pt);
            return;
        }

        if (getType().getEnumConstants() != null) {
            Enum eVal = parseEnum(getType(), value.trim());
            setValue(eVal);
            return;
        }

        if (getType().equals(Boolean.class) || getType().equals(Boolean.TYPE)) {
            setValue(BooleanUtils.toBoolean(value));
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
        
        if (getType().isArray()) {
            try {
                Class clczz = getType();
                String[] tk = value.trim().replaceFirst("^\\[", "").replaceFirst("\\]$", "").split(" *, *");
                Class<?> arCompType = getType().getComponentType();
                Object arObj = java.lang.reflect.Array.newInstance(arCompType, tk.length);
                for (int i = 0; i < tk.length; i++) {
                    if (!arCompType.isEnum()) {
                        if (arCompType.equals(Boolean.TYPE))
                            java.lang.reflect.Array.setBoolean(arObj, i, BooleanUtils.toBoolean(tk[i]));
                        else if (arCompType.equals(Long.TYPE))
                            java.lang.reflect.Array.setLong(arObj, i, Long.parseLong(tk[i]));
                        else if (arCompType.equals(Integer.TYPE))
                            java.lang.reflect.Array.setInt(arObj, i, Integer.parseInt(tk[i]));
                        else if (arCompType.equals(Short.TYPE))
                            java.lang.reflect.Array.setShort(arObj, i, Short.parseShort(tk[i]));
                        else if (arCompType.equals(Byte.TYPE))
                            java.lang.reflect.Array.setByte(arObj, i, Byte.parseByte(tk[i]));
                        else if (arCompType.equals(Double.TYPE))
                            java.lang.reflect.Array.setDouble(arObj, i, Double.parseDouble(tk[i]));
                        else if (arCompType.equals(Float.TYPE))
                            java.lang.reflect.Array.setFloat(arObj, i, Float.parseFloat(tk[i]));
                        else if (arCompType.equals(Character.TYPE))
                            java.lang.reflect.Array.setChar(arObj, i, tk[i].charAt(0));
                        else if (arCompType.equals(String.class))
                            java.lang.reflect.Array.set(arObj, i, tk[i]);
                        else if (arCompType.equals(File.class))
                            java.lang.reflect.Array.set(arObj, i, new File(tk[i]));
                        // else if (arCompType.equals(Color.class))
                        //    java.lang.reflect.Array.set(arObj, i, parseColor(tk[i]));
                        else if (arCompType.equals(ColorMap.class))
                            java.lang.reflect.Array.set(arObj, i, parseColorMap(tk[i]));
                        else {
                            try {
                                Method m = getType().getMethod("valueOf", String.class);
                                Object o = m.invoke(value);
                                java.lang.reflect.Array.set(arObj, i, o);
                            }
                            catch (Exception e) {
                                Method m = getType().getMethod("valueOf", String.class);
                                Object o = m.invoke(getValue(), value);
                                java.lang.reflect.Array.set(arObj, i, o);
                            }
                        }
                    }
                    else {
                        Enum eVal = parseEnum(getType().getComponentType(), tk[i]);
                        if (eVal != null)
                            java.lang.reflect.Array.set(arObj, i, eVal);
                    }
                }
                setValue(arObj);
            }
            catch (Exception e) {
                throw new Exception ("Object type not supported: " + getName() + "@" + getType().getSimpleName()
                        + "[" + getType().getComponentType() + "]", e);
            }
            return;
        }

        if (Map.class.isAssignableFrom(getType())) {
            // Only Map<String, String>
            try {
                Map<String, String> curMap = (Map<String, String>) getValue();
                String[] tk = value.trim().replaceFirst("^\\{", "").replaceFirst("\\}$", "").split(" *, *");
                Map<String, String> newMap = curMap != null ? curMap.getClass().newInstance() : (Map<String, String>) getType().newInstance();
                for (int i = 0; i < tk.length; i++) {
                    String[] tk1 = tk[i].trim().split("=");
                    if (tk1.length != 2)
                        continue;
                    newMap.put(tk1[0].trim(), tk1[1].trim());
                }
                setValue(newMap);
                return;
            }
            catch (Exception e) {
                throw new Exception ("Map object type not supported: " + getName() + "@" + getType().getSimpleName()
                        + "[" + getType().getComponentType() + "]", e);
            }
        }

        if (Date.class.isAssignableFrom(getType())) {
            try {
                SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE MMM dd hh:mm:ss zzz yyyy") {{setTimeZone(TimeZone.getTimeZone("UTC"));}}; // This one should be UTC (Zulu)
                Date date = dateFormatter.parse(value.trim());
                setValue(date);
                return;
            }
            catch (Exception e) {
                throw new Exception ("Date object type format not supported (should be \"dow mon dd hh:mm:ss zzz yyyy\"): "
                        + getName() + "@" + getType().getSimpleName()
                        + "[" + getType().getComponentType() + "]", e);
            }
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

    /**
     * @param color
     * @return
     */
    private static ColorMap parseColorMap(String color) {
        if (ColorMapFactory.colorMapNamesList.contains(color))
            return ColorMapFactory.getColorMapByName(color);
        else
            return null;
    }

    /**
     * @param value
     * @return
     */
    private static Color parseColor(String value) throws Exception {
        String color = value.replace("java.awt.Color[", "");
        color = color.replaceAll("[\\]rgb=]", "");
        String[] rgb = color.split(",");
        return new Color(Integer.parseInt(rgb[0]),Integer.parseInt(rgb[1]),Integer.parseInt(rgb[2]));
    }

    /**
     * @param type
     * @param value
     * @return
     */
    private static Enum parseEnum(Class<? extends Enum> type, String value) throws Exception {
        if (value == null)
            return null;
        
        value = value.trim();
        
        if(value.isEmpty())
            return null;
        
        Enum eVal = null;
        try {
            eVal = Enum.valueOf(type, value);
            return eVal;
        }
        catch (Exception e) {
            try {
                Method m = null;
                try {
                    m = type.getMethod("valueOf", String.class);
                    Object o = m.invoke(null, value.trim());
                    return (Enum) o;
                }
                catch (Exception e1) {
                    m = type.getMethod("parse", String.class);
                    Object o = m.invoke(null, value.trim());
                    return (Enum) o;
                }
            }
            catch (Exception e1) {
                throw e;
            }
        }
    }

    public void validate(Object value) throws Exception { }
}
