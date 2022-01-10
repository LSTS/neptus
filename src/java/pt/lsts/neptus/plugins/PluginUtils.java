/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.Window;
import java.beans.PropertyEditor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.table.TableCellRenderer;

import org.apache.commons.io.IOUtils;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorRegistry;
import com.l2fprod.common.propertysheet.PropertyRendererRegistry;
import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.editor.EnumEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author zepinto
 * @author pdias
 *
 */
public class PluginUtils {

    public static String DEFAULT_ICON = "images/plugin.png";

    public static Map<Class<?>, LinkedHashMap<String, PluginProperty>> defaultValuesProperties = Collections
            .synchronizedMap(new LinkedHashMap<Class<?>, LinkedHashMap<String, PluginProperty>>());

    /**
     * Tries to retrieve the plugin name from a {@link PluginDescription} annotation or else uses the class name
     * 
     * @param clazz The class of the plugin
     * @return The name to be used for this plugin.
     */
    public static String getPluginName(Class<?> clazz) {
        PluginDescription pd = clazz.getAnnotation(PluginDescription.class);
        String name;
        if (pd != null && !pd.name().isEmpty())
            name = pd.name();
        else
            name = clazz.getSimpleName();

        return name;
    }

    public static String getPluginI18nName(Class<?> clazz) {
        return i18nTranslate(getPluginName(clazz));
    }

    public static String i18nTranslate(String txt) {
        String txt1 = txt.replaceFirst("(_\\d+)$", "");
        String translation = I18n.text(txt1);
        return translation;
    }

    /**
     * Tries to retrieve the plugin name from a {@link PluginDescription} annotation or else uses the class name
     * 
     * @param clazz The class of the plugin
     * @return The name to be used for this plugin.
     */
    public static String getLocalizedPluginName(Class<?> clazz) {
        return I18n.textAdvanced("PLUGINNAME_" + clazz.getSimpleName(), getPluginName(clazz), clazz);
    }

    /**
     * Retrieves the description for a given plugin using the {@link PluginDescription} annotation
     * 
     * @param clazz The class of the plugin
     * @return The description found in the PluginDescription annotation or the plugin name (if not found)
     */
    public static String getPluginDescription(Class<?> clazz) {
        PluginDescription pd = clazz.getAnnotation(PluginDescription.class);
        String description;
        if (pd != null)
            description = pd.description();
        else
            description = clazz.getSimpleName();

        if (description == null || description.length() == 0)
            description = getLocalizedPluginName(clazz);

        return I18n.textAdvanced("PLUGINDESC_" + clazz.getSimpleName(), description, clazz);
    }
    
    public static boolean isPluginActive(Class<?> clazz) {
        PluginDescription pd = clazz.getAnnotation(PluginDescription.class);
        return pd != null && pd.active();
    }
    
    public static boolean isPluginExperimental(Class<?> clazz) {
        PluginDescription pd = clazz.getAnnotation(PluginDescription.class);
        return pd != null && pd.experimental();
    }

    /**
     * Retrieves the filename of the icon to be used for this plugin as stated in the {@link PluginDescription}
     * annotation
     * 
     * @param clazz The plugin class
     * @return The icon to be used for this plugin
     */
    public static String getPluginIcon(Class<?> clazz) {
        PluginDescription pd = clazz.getAnnotation(PluginDescription.class);
        String icon = "";
        if (pd != null)
            icon = pd.icon();

        if (icon.length() == 0 || ClassLoader.getSystemResource(icon) == null) {
            return DEFAULT_ICON;
        }
        else
            return icon;
    }

    /**
     * Retrieves the properties found in a Plugin instance
     * 
     * @param obj The plugin instance to be inspected for properties
     * @return The properties found in the plugin retrieved as a LinkedHashMap
     */
    public static LinkedHashMap<String, PluginProperty> getProperties(Object obj, boolean forEdit) {
        LinkedHashMap<String, PluginProperty> props = new LinkedHashMap<String, PluginProperty>();

        Map<String, PluginProperty> defaults = getDefaultsValues(obj);

        //Class<?> c = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
        
        for (Field f : getFields(obj)) {
            String defaultStr = null;
            
            if (defaults.containsKey(f.getName()))
                defaultStr = defaults.get(f.getName()).serialize();
            
            PluginProperty pp = createPluginProperty(obj, f, defaultStr, forEdit);
            
            if (pp != null)
                props.put(f.getName(), pp);
        }
        return props;
    }

    /**
     * @param obj use the null for static
     * @param f
     * @return
     */
    public static PluginProperty createPluginProperty(Object obj, Field f, boolean forEdit) {
        return createPluginProperty(obj, f, null, forEdit);
    }

    public static PluginProperty createPluginProperty(Object obj, Field f) {
        return createPluginProperty(obj, f, null, true);
    }

    /**
     * NOTE: the forEdit is important because if we want to load properties for the {@link GeneralPreferences}
     * we pass forEdit=false so we don't call I18n that loads {@link GeneralPreferences#language} while we are 
     * loading {@link GeneralPreferences} properties from file.
     * @param obj
     * @param f
     * @param defaultValueString or null if not known.
     * @return
     */    
    public static PluginProperty createPluginProperty(Object obj, Field f, String defaultValueString, boolean forEdit) {
        return createPluginProperty(obj, f, defaultValueString, forEdit, PropertiesEditor.getPropertyEditorRegistry(),
                PropertiesEditor.getPropertyRendererRegistry());
    }

    /**
     * NOTE: the forEdit is important because if we want to load properties for the {@link GeneralPreferences}
     * we pass forEdit=false so we don't call I18n that loads {@link GeneralPreferences#language} while we are 
     * loading {@link GeneralPreferences} properties from file.
     * @param obj
     * @param f
     * @param defaultValueString or null if not known.
     * @param propertyEditorRegistry
     * @param propertyRendererRegistry
     * @return
     */
    @SuppressWarnings({ "unchecked", "serial" })
    public static PluginProperty createPluginProperty(Object obj, Field f, String defaultValueString, boolean forEdit,
            PropertyEditorRegistry propertyEditorRegistry, PropertyRendererRegistry propertyRendererRegistry) {
        NeptusProperty a = f.getAnnotation(NeptusProperty.class);

        if (a != null) {
            //if (Modifier.isPrivate(f.getModifiers())) {
                f.setAccessible(true);
            //}
            String name = a.name();
            String desc = a.description();
            String units = a.units();
            String defaultAndUnitsStr = "";
            if (defaultValueString != null && forEdit) {
                if (f.getType().getEnumConstants() != null)
                    defaultValueString = I18n.text(defaultValueString);
                defaultAndUnitsStr = "<br>";
                defaultAndUnitsStr += units.length() > 0 ? "(" + units + ") " : "";
                defaultAndUnitsStr += "<i>(" + I18n.text("Default value:") + " \"<b><code>" + defaultValueString
                        + "</code></b>\")</i>";
            }
            else {
                defaultAndUnitsStr += units.length() > 0 ? "<br>(" + units + ") " : "";
            }
            Class<? extends PropertyEditor> editClass = null;
            Class<? extends TableCellRenderer> rendererClass = null;
            String category = a.category();

            if (a.name().length() == 0) {
                name = f.getName();
            }
            // if (a.description().length() == 0) {
            //     desc = f.getName();
            // }

            if (a.editorClass() != PropertyEditor.class) {
                editClass = a.editorClass();
            }

            if (a.rendererClass() != TableCellRenderer.class) {
                rendererClass = a.rendererClass();
            }

            if (category == null || category.length() == 0) {
                category = I18n.text("Base");
            }

            Object o = null;
            try {
                o = f.get(null);
            }
            catch (Exception e) {
            }
            if (o == null) {
                try {
                    o = f.get(obj);
                }
                catch (Exception e) {
                }
            }

            PluginProperty pp = new PluginProperty(name, f.getType(), o);
            pp.setShortDescription((forEdit ? I18n.text(desc) : desc) + defaultAndUnitsStr);
            pp.setEditable(a.editable());
            pp.setDisplayName(forEdit ? (obj != null && obj.getClass().equals(GeneralPreferences.class) ? "* " : "") + I18n.text(name) : name);
            if (category != null && category.length() > 0) {
                pp.setCategory(category);
            }

            if (editClass != null) {
                propertyEditorRegistry.registerEditor(pp, editClass);
            }
            else {
                if (ReflectionUtil.hasInterface(f.getType(), PropertyType.class)) {
                    PropertyType pt = (PropertyType) o;
                    propertyEditorRegistry.registerEditor(pp, pt.getPropertyEditor());
                }
                if (f.getType().getEnumConstants() != null) {
                    if (o != null) {
                        propertyEditorRegistry.registerEditor(pp,
                                new EnumEditor((Class<? extends Enum<?>>) o.getClass()));
                        PropertiesEditor.getPropertyRendererRegistry().registerRenderer(pp, new DefaultCellRenderer() {
                            {
                                setOpaque(false);
                            }

                            @Override
                            protected String convertToString(Object value) {
                                return I18n.text(value.toString());
                            }
                        });
                    }
                }
            }
            
            if (rendererClass != null)
                propertyRendererRegistry.registerRenderer(pp, rendererClass);

            return pp;
        }
        return null;
    }
    /**
     * @return <b>true</b> if cancelled or <b>false</b> otherwise.
     */
    public static <P extends Window>  boolean editPluginProperties(final Object obj, boolean editable) {
        return editPluginProperties(obj, ConfigFetch.getSuperParentAsFrame(), editable);
    }
    
    /**
     * @param obj
     * @param parent
     * @param editable
     * @return
     */
    public static <P extends Window> boolean editPluginProperties(final Object obj, P parent, boolean editable) {
        PropertiesProvider provider = new PropertiesProvider() {
            
            @Override
            public void setProperties(Property[] properties) {
                setPluginProperties(obj, properties);                
            }
            
            @Override
            public String[] getPropertiesErrors(Property[] properties) {
                return null;
            }
            
            @Override
            public String getPropertiesDialogTitle() {
                return getPluginName(obj.getClass())+" properties";
            }
            
            @Override
            public DefaultProperty[] getProperties() {
                return getPluginProperties(obj);
            }
        };
        return PropertiesEditor.editProperties(provider, parent, editable);
    }

    /**
     * Retrieves the plugin properties as an array
     * 
     * @param obj The plugin instance to be inspected for properties
     * @return the plugin properties as an array
     */
    public static PluginProperty[] getPluginProperties(Object obj, boolean forEdit) {
        return getProperties(obj, forEdit).values().toArray(new PluginProperty[0]);
    }

    public static PluginProperty[] getPluginProperties(Object obj) {
        return getPluginProperties(obj, true);
    }

    /**
     * Changes the fields in the given object instance according to the properties passed as a parameter
     * 
     * @param obj The plugin instance to be initialized
     * @param props The properties to be set on the given object
     */
    public static void setPluginProperties(Object obj, Property[] props) {
        LinkedHashMap<String, PluginProperty> ps = new LinkedHashMap<String, PluginProperty>();

        for (Property p : props)
            ps.put(p.getName(), new PluginProperty(p));

        setPluginProperties(obj, ps);
    }

    public static String[] validatePluginProperties(Object obj, Property[] props) {
        LinkedHashMap<String, PluginProperty> ps = new LinkedHashMap<String, PluginProperty>();

        for (Property p : props)
            ps.put(p.getName(), new PluginProperty(p));

        return validatePluginProperties(obj, ps);
    }

    public static String[] validatePluginProperties(Object obj, LinkedHashMap<String, PluginProperty> props) {
        Vector<String> errors = new Vector<String>();
        ArrayList<String> propsMissedProcessed = new ArrayList<>(props.size());
        props.keySet().stream().forEach((s) -> propsMissedProcessed.add(s));

        Class<? extends Object> providerClass = obj.getClass();

        for (Field f : getFields(providerClass)) {
            NeptusProperty a = f.getAnnotation(NeptusProperty.class);
            if (a != null) {
                // Find field name
                String name = a.name();
                String fieldName = f.getName();
                if (name.length() == 0) {
                    name = fieldName;
                }
                if (props.get(name) == null)
                    continue;
                propsMissedProcessed.remove(name);
                
                Object propValue = props.get(name).getValue();
                String res = findValidationMethodAndInvoque(obj, fieldName, name, propValue, f.getType().isPrimitive());
                // In case of error add error message to the error message array
                if (res != null)
                    errors.add(res.toString());
            }
        }
        
        // Let us validate the additional properties that are not annotated
        for (String name : propsMissedProcessed) {
            Object propValue = props.get(name).getValue();
            String fieldName = name.codePoints().filter((p) -> Character.isJavaIdentifierStart(p))
                    .mapToObj((p) -> new String(Character.toChars(p))).collect(Collectors.joining());
            String res = findValidationMethodAndInvoque(obj, fieldName, name, propValue, true);
            // In case of error add error message to the error message array
            if (res != null)
                errors.add(res.toString());
        }

        return errors.stream().toArray(String[]::new);
    }
    
    /**
     * @return
     */
    private static String findValidationMethodAndInvoque(Object obj, String fieldName, String propName,
            Object propValue, boolean fieldIsPrimitive) {
        // Find method
        String validateMethodUpper = "validate" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String validateMethodLower = "validate" + Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method m;
        
        Class<? extends Object> providerClass = obj.getClass();
        
        if (propValue == null) {
            NeptusLog.pub().debug(
                    "Property " + providerClass.getSimpleName() + "." + propName
                            + " has no value to validate user input!");
            return null;
        }
        
        Class<? extends Object> propClass = propValue.getClass();
        if (fieldIsPrimitive) {
            //propClass.isArray() // FIXME
            if (propClass == Double.class)
                propClass = double.class;
            else if (propClass == Float.class)
                propClass = float.class;
            else if (propClass == Byte.class)
                propClass = byte.class;
            else if (propClass == Character.class)
                propClass = char.class;
            else if (propClass == Short.class)
                propClass = short.class;
            else if (propClass == Integer.class)
                propClass = int.class;
            else if (propClass == Long.class)
                propClass = long.class;
            else if (propClass == Boolean.class)
                propClass = boolean.class;
        }
        
        try {
            m = providerClass.getMethod(validateMethodUpper, propClass);
        }
        catch (NoSuchMethodException e1) {
            try {
                m = providerClass.getMethod(validateMethodLower, propClass);
            }
            catch (NoSuchMethodException e) {
                NeptusLog.pub().debug("Property " + providerClass.getSimpleName() + "." + propName
                        + " has no method to validate user input!");
                return null;
            }
            catch (SecurityException e) {
                e.printStackTrace();
                return null;
            }
        }
        catch (SecurityException e1) {
            e1.printStackTrace();
            return null;
        }

        // If method has been found, invoke it
        try {
            Object res = m.invoke(obj, propValue);
            if (res != null)
                return res.toString();
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }

        return null;
    }

    public static Field[] getFields(Object o) {
        Class<?> c;
        if (o instanceof Class<?>)
            c = (Class<?>)o;
        else
            c = o.getClass();
        
        HashSet<Field> fields = new LinkedHashSet<>(); 
        for (Field f : c.getFields())
            fields.add(f);
        for (Field f : c.getDeclaredFields()) {
            f.setAccessible(true);
            fields.add(f);
        }
        return fields.toArray(new Field[0]);
    }

    /**
     * @see {@link #setPluginProperties(Object, Property[])}
     */
    public static void setPluginProperties(Object obj, LinkedHashMap<String, PluginProperty> props) {
        Class<? extends Object> providerClass = obj instanceof Class<?> ? (Class<?>) obj : obj.getClass();
        String name;
        PluginProperty property;
        Object propertyValue;
        for (Field f : getFields(providerClass)) {
            NeptusProperty a = f.getAnnotation(NeptusProperty.class);

            if (a != null) {
                name = a.name();

                if (a.name().length() == 0) {
                    name = f.getName();
                }

                property = props.get(name);
                if (property == null) {
                    // Try alternatives
                    String[] alternatives = computeParamNameAlternatives(name);
                    if (alternatives.length > 0) {
                        for (String altName : alternatives) {
                            property = props.get(altName);
                            if (property != null) {
                                NeptusLog.pub().info(String.format("Found alternative \"%s\" for property \"%s\"!",altName, name));
                                break;
                            }
                        }
                    }
                    
                    if (property == null) {
                        NeptusLog.pub().debug("Property " + name + " will not be saved.");
                        continue;
                    }
                }
                try {
                    propertyValue = property.getValue();
                    
                    String[] res = PluginUtils.validatePluginProperties(obj, new Property[] {property});
                    if (res != null && res.length > 0)
                        continue; // not valid value so don't set
                }
                catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    continue;
                }
                try {
                    if (a.editable())
                        f.set(obj, propertyValue);
                }
                catch (IllegalArgumentException e) {
                    try {
                        if ("int".equalsIgnoreCase(f.getGenericType().toString())
                                || "Integer".equalsIgnoreCase(f.getGenericType().toString())) {
                            String className = propertyValue.getClass().getName();
                            if (className.equals("java.lang.String")) {
                                f.set(obj, Integer.parseInt((String) propertyValue));
                            }
                            else { // if (className.equals("java.lang.Long")) {
                                f.set(obj, Integer.valueOf(((Long) propertyValue).intValue()));
                            }
                        }
                        else if ("short".equalsIgnoreCase(f.getGenericType().toString())) {
                            f.set(obj, Short.valueOf(((Long) propertyValue).shortValue()));
                        }
                        else if ("byte".equalsIgnoreCase(f.getGenericType().toString())) {
                            f.set(obj, Byte.valueOf(((Long) propertyValue).byteValue()));
                        }
                        else if ("float".equalsIgnoreCase(f.getGenericType().toString())) {
                            f.set(obj, Float.valueOf(((Double) propertyValue).floatValue()));
                        }
                        else {
                            // Re-throw original exception
                            throw e;
                        }
                    }
                    catch (Exception e2) {
                        NeptusLog.pub().error(e2, e2);;
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);;
                }
            }
        }

        if (obj instanceof ConfigurationListener) {
            ((ConfigurationListener) obj).propertiesChanged();
        }
    }


    /**
     * Inspects the plugin instance passed as a parameter and retrieves its properties as a java.util.Properties
     * 
     * @param obj The plugin instance to be inspected for properties
     * @param clean If <strong>true</strong>, the properties with default values will be removed from the result
     * @return A java.util.Properties object with the properties of the given plugin instance
     */
    public static Properties saveProperties(Object obj, boolean clean) {
        try {
            LinkedHashMap<String, PluginProperty> ps = getProperties(obj, false);

            LinkedHashMap<String, PluginProperty> defPs = new LinkedHashMap<>();
            if (clean) {
                LinkedHashMap<String, PluginProperty> vl = getDefaultsPropertyValues(obj);
                if (vl != null)
                    defPs.putAll(vl);

                // clear properties that are already default
                Object[] keys = ps.keySet().toArray();
                for (Object key : keys) {
                    PluginProperty curP = ps.get(key);
                    PluginProperty defaultP = defPs.get(key);

                    if (curP == null || curP.serialize() == null
                            || (defaultP != null && curP.serialize().equals(defaultP.serialize()))) {
                        ps.remove(key);
                    }
                }
            }

            Properties props = new Properties();
            for (String key : ps.keySet()) {
                if (ps.get(key).getValue() == null)
                    NeptusLog.pub().warn("Not saving plugin null property named " + key + "=" + ps.get(key));
                else {
                    PluginProperty p = ps.get(key);
                    props.setProperty(p.getName(), p.serialize());
                }
            }
            return props;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param clazz
     */
    protected static synchronized void extractDefaultFieldsValues(Object obj) {
        // Let us save the static defaults before they are first changed
        Class<? extends Object> clazz = (obj instanceof Class<?> ? (Class<?>) obj : obj.getClass());
        if (!defaultValuesProperties.containsKey(clazz)) {
            LinkedHashMap<String, PluginProperty> defPs = new LinkedHashMap<>();
            LinkedHashMap<String, PluginProperty> vl = new LinkedHashMap<>();
            for (Field f : getFields(obj)) {
                PluginProperty pp = createPluginProperty(obj, f, null, false);
                if (pp != null)
                    vl.put(f.getName(), pp);
            }

            if (vl.size() > 0) {
                defPs.putAll(vl);
            }
            defaultValuesProperties.put(clazz, defPs);
        }
    }

    public static Map<String, PluginProperty> getDefaultsValues(Object obj) {
        Map<String, PluginProperty> defs = new LinkedHashMap<>();
        Class<? extends Object> clazz = (obj instanceof Class<?> ? (Class<?>) obj : obj.getClass());
        if (defaultValuesProperties.containsKey(clazz)) {
            defs= Collections.unmodifiableMap(defaultValuesProperties.get(clazz));
        }
        return defs;
    }

    /**
     * @param obj
     */
    private static LinkedHashMap<String, PluginProperty> getDefaultsPropertyValues(Object obj) {
        LinkedHashMap<String, PluginProperty> defPs = new LinkedHashMap<>();

        Object defaults = null;
        try {
            defaults = (obj instanceof Class<?> ? (Class<?>) obj : obj.getClass()).getDeclaredConstructor().newInstance();
        }
        catch (Exception e) {
            // e.printStackTrace();
        }

        if (defaults != null && !(obj instanceof Class<?>)) {
            LinkedHashMap<String, PluginProperty> vl = getProperties(defaults, false);
            if (vl != null)
                defPs.putAll(vl);
        }
        LinkedHashMap<String, PluginProperty> df = defaultValuesProperties
                .get((obj instanceof Class<?> ? (Class<?>) obj : obj.getClass()));
        if (df != null)
            defPs.putAll(df);

        return defPs;
    }

    /**
     * @param filename
     * @param ignorePropertiesWithDefaultValues
     * @param obj
     * @throws IOException
     */
    public static void saveProperties(String filename, boolean ignorePropertiesWithDefaultValues, Object obj)
            throws IOException {
        Properties props = saveProperties(obj, ignorePropertiesWithDefaultValues);
        props.store(new FileOutputStream(filename), "Properties generated by Neptus");
    }

    /**
     * 
     * @param filename
     * @param obj
     * @throws IOException
     */
    public static void saveProperties(String filename, Object obj) throws IOException {
        saveProperties(filename, false, obj);
    }

    /**
     * @param filename
     * @param ignorePropertiesWithDefaultValues
     * @param obj
     * @throws IOException
     */
    public static void savePropertiesToXML(String filename, boolean ignorePropertiesWithDefaultValues, Object obj)
            throws IOException {
        Properties props = saveProperties(obj, ignorePropertiesWithDefaultValues);
        props.storeToXML(new FileOutputStream(filename), "Properties generated by Neptus");
    }

    /**
     * @param filename
     * @param obj
     * @throws IOException
     */
    public static void savePropertiesToXML(String filename, Object obj) throws IOException {
        savePropertiesToXML(filename, false, obj);
    }

    /**
     * @param filename
     * @param obj
     * @throws IOException
     */
    public static void loadProperties(String filename, Object obj) throws IOException {
        Properties props = new Properties();
        loadProperties(filename, props, obj);
    }

    /**
     * @param filename
     * @param props
     * @param obj
     * @throws IOException
     */
    public static void loadProperties(String filename, Properties props, Object obj) throws IOException {
        if (new File(filename).exists()) {
            try {
                props.loadFromXML(new FileInputStream(filename));
            }
            catch (InvalidPropertiesFormatException e) {
                props.load(new FileReader(filename));
            }
            catch (Exception e) {
                // TODO: handle exception
            }
        }
        loadProperties(props, obj);
    }

    /**
     * 
     * @param props
     * @param obj
     */
    public static void loadProperties(Properties props, Object obj) {
//        Class<? extends Object> clazz = (obj instanceof Class<?> ? (Class<?>) obj : obj.getClass());
        extractDefaultFieldsValues(obj);

        PluginProperty[] ps = getPluginProperties(obj, false);

        for (PluginProperty p : ps) {
            if (props.containsKey(p.getName())) {
                try {
                    p.unserialize(props.getProperty(p.getName().toString()));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        setPluginProperties(obj, ps);
    }

    /**
     * Get configuration/properties from a plugin (or combined) and return it as XML
     * 
     * @param obj
     * @return
     */
    public static String getConfigXml(Object... obj) {
        return getConfigXml(false, obj);
    }

    /**
     * Get configuration/properties from a plugin (or combined) and return it as XML
     * with option to add a date comment to it.
     * 
     * @param addComment
     * @param obj
     * @return
     */
    public static String getConfigXml(boolean addComment, Object... obj) {
        Properties props = null;
        try {
            for (Object o : obj) {
                if (props == null) {
                    props = saveProperties(o, true);
                }
                else {
                    Properties propTmp = saveProperties(o, true);
                    final Properties p = props;
                    if (propTmp != null)
                        propTmp.forEach((k, v) -> p.putIfAbsent(k, v));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return "<properties/>";
        }
        
        return getConfigXmlWorker(addComment, props);
    }

    public static String getConfigXmlWithDefaults(Object... obj) {
        return getConfigXmlWithDefaults(false, obj);
    }

    public static String getConfigXmlWithDefaults(boolean addComment, Object... obj) {
        Properties props = null;
        try {
            for (Object o : obj) {
                if (props == null) {
                    props = saveProperties(o, false);
                }
                else {
                    Properties propTmp = saveProperties(o, false);
                    final Properties p = props;
                    if (propTmp != null)
                        propTmp.forEach((k, v) -> p.putIfAbsent(k, v));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return "<properties/>";
        }
        
        return getConfigXmlWorker(addComment, props);
    }

    private static String getConfigXmlWorker(boolean addComment, Properties props) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            props.storeToXML(baos, addComment ? "Generated by Neptus on " + (new Date()) : null, "UTF-8");
            String xml = baos.toString("UTF-8");
            int start = xml.indexOf("<properties");
            return xml.substring(start);
        }
        catch (Exception e) {
            e.printStackTrace();
            return "<properties/>";
        }
    }
    
    public static void setConfigXml(Object obj, String xml) {
        Properties props = new Properties();

        xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n" + xml;

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            props.loadFromXML(bais);
            loadProperties(props, obj);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadProperties(Object obj, String instanceName) {
        String propsFilename = ConfigFetch.getConfFolder() + "/plugins/" + obj.getClass().getSimpleName() + "-" + instanceName + ".properties";

        File propsFile = new File(propsFilename);
        if (propsFile.canRead()) {
            try {
                PluginUtils.loadProperties(propsFilename, obj);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveProperties(Object obj, String instanceName) {
        new File(ConfigFetch.getConfFolder() + "/plugins").mkdirs();
        String propsFilename = ConfigFetch.getConfFolder() + "/plugins/" + obj.getClass().getSimpleName() + "-" + instanceName + ".properties";

        try {
            PluginUtils.saveProperties(propsFilename, obj);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@link NeptusProperty}
     * 
     * @param objWithNeptusPropertyAnotation
     */
    public static final PropertiesProvider wrapIntoAPlugInPropertiesProvider(final Object objWithNeptusPropertyAnotation) {
        PropertiesProvider pp = new PropertiesProvider() {
            @Override
            public DefaultProperty[] getProperties() {
                return PluginUtils.getPluginProperties(objWithNeptusPropertyAnotation, true);
            }

            @Override
            public void setProperties(Property[] properties) {
                PluginUtils.setPluginProperties(objWithNeptusPropertyAnotation, properties);
            }

            @Override
            public String getPropertiesDialogTitle() {
                return PluginUtils.getPluginName(objWithNeptusPropertyAnotation.getClass()) + " parameters";
            }

            @Override
            public String[] getPropertiesErrors(Property[] properties) {
                return PluginUtils.validatePluginProperties(objWithNeptusPropertyAnotation, properties);
            }
        };

        return pp;
    }

    /**
     * @param clazz
     * @param dFA
     */
    public static void extractFieldsWorker(Class<?> clazz, Vector<Field> dFA) {
        if (clazz == null || clazz.equals(Object.class))
            return;

        Field[] dFt = clazz.getDeclaredFields();
        for (Field fd : dFt) {
            if (fd.getAnnotation(NeptusProperty.class) != null) {
                boolean hasField = false;
                for (Field fdC : dFA) {
                    if (fdC.getName().equals(fd.getName())) {
                        hasField = true;
                        break;
                    }
                }
                if (!hasField)
                    dFA.add(fd);
            }
        }
        
        extractDefaultFieldsValues(clazz);
        
        extractFieldsWorker(clazz.getSuperclass(), dFA);
    }

    public static InputStream getResourceAsStream(String filename) {
        // Merge this with FileUtils
        return Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(filename);
    }
    
    public static String getResourceAsString(String filename) throws IOException{
        return IOUtils.toString(getResourceAsStream(filename), (Charset) null);
    }
    
    private static String[] computeParamNameAlternatives(String name) {
        ArrayList<String> alternatives = new ArrayList<>();
        
        // To lower case
        String pattern = "( [A-Z])";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(name);
        StringBuffer sb = new StringBuffer();
        int c = 0;
        while (m.find()) {
            c++;
            m.appendReplacement(sb, m.group().toLowerCase());
        }
        m.appendTail(sb);
        if (c > 0)
            alternatives.add(sb.toString());

        // To camel case
        pattern = "( [a-z])";
        r = Pattern.compile(pattern);
        m = r.matcher(name);
        sb = new StringBuffer();
        c = 0;
        while (m.find()) {
            c++;
            m.appendReplacement(sb, m.group().toUpperCase());
        }
        m.appendTail(sb);
        if (c > 0)
            alternatives.add(sb.toString());
        
        // As field name
        pattern = "( [a-zA-Z0-9])";
        r = Pattern.compile(pattern);
        m = r.matcher(name);
        sb = new StringBuffer();
        c = 0;
        while (m.find()) {
            c++;
            m.appendReplacement(sb, m.group().toUpperCase().trim());
        }
        m.appendTail(sb);
        if (c > 0) {
            pattern = "(^.)";
            r = Pattern.compile(pattern);
            m = r.matcher(sb.toString());
            sb = new StringBuffer();
            if (m.find()) {
                m.appendReplacement(sb, m.group().toLowerCase());
            }
            m.appendTail(sb);

            alternatives.add(sb.toString());
        }
        else {
            alternatives.add(name.toLowerCase());
        }
        
        return alternatives.toArray(new String[alternatives.size()]);
    }
    
    public static void main(String[] args) {
        String test = "Speed Units dff";
        String[] alt = computeParamNameAlternatives(test);
        System.out.println(Arrays.toString(alt));
        
        String[] values = {
                "Test of the speed (m/s)",
                "Test of the water",
                "Teste de água (m/s)"
        };
        for (String name : values) {
            String fieldName = name.codePoints().filter((p) -> Character.isJavaIdentifierStart(p))
                    .mapToObj((p) -> new String(Character.toChars(p))).collect(Collectors.joining());
            System.out.println(String.format("name: '%s' >> '%s'", name, fieldName));
        }
    }
}