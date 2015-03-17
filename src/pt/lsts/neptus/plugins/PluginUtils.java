/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * 20??/??/??
 */
package pt.lsts.neptus.plugins;

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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.imageio.spi.ServiceRegistry;

import org.apache.commons.io.IOUtils;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.editor.EnumEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.conf.GeneralPreferences;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

public class PluginUtils {

    public static String DEFAULT_ICON = "images/plugin.png";

    @SuppressWarnings("rawtypes")
    private static final Class[] parameters = new Class[] { URL.class };
    public static final File PLUGINS_DIR = new File(".", "plugins");
    private static boolean pluginsLoaded = false;

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
    @SuppressWarnings({ "unchecked", "serial" })
    public static PluginProperty createPluginProperty(Object obj, Field f, String defaultValueString, boolean forEdit) {
        NeptusProperty a = f.getAnnotation(NeptusProperty.class);

        if (a != null) {
            //if (Modifier.isPrivate(f.getModifiers())) {
                f.setAccessible(true);
            //}
            String name = a.name();
            String desc = a.description();
            String defaultStr = "";
            if (defaultValueString != null && forEdit) {
                if (f.getType().getEnumConstants() != null)
                    defaultValueString = I18n.text(defaultValueString);
                defaultStr = "<br><i>[[" + I18n.text("Default value:") + " \"<b><code>" + defaultValueString
                        + "</code></b>\"]]</i>";
            }
            Class<? extends PropertyEditor> editClass = null;
            String category = a.category();

            if (a.name().length() == 0) {
                name = f.getName();
            }
            if (a.description().length() == 0) {
                desc = f.getName();
            }

            if (a.editorClass() != PropertyEditor.class) {
                editClass = a.editorClass();
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
            pp.setShortDescription((forEdit ? I18n.text(desc) : desc) + defaultStr);
            pp.setEditable(a.editable());
            pp.setDisplayName(forEdit ? I18n.text(name) : name);
            if (category != null && category.length() > 0) {
                pp.setCategory(category);
            }

            if (editClass != null)
                PropertiesEditor.getPropertyEditorRegistry().registerEditor(pp, editClass);
            else {
                if (ReflectionUtil.hasInterface(f.getType(), PropertyType.class)) {
                    PropertyType pt = (PropertyType) o;
                    PropertiesEditor.getPropertyEditorRegistry().registerEditor(pp, pt.getPropertyEditor());
                }
                if (f.getType().getEnumConstants() != null)
                    if (o != null) {
                        PropertiesEditor.getPropertyEditorRegistry().registerEditor(pp,
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

            return pp;
        }
        return null;
    }
    
    public static void editPluginProperties(final Object obj, boolean editable) {
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
        
        PropertiesEditor.editProperties(provider, editable);
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
                // Find method
                String validateMethodUpper = "validate" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                String validateMethodLower = "validate" + Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
                Method m;
                Object propValue = props.get(name).getValue();
                if (propValue == null) {
                    NeptusLog.pub().debug(
                            "Property " + providerClass.getSimpleName() + "." + name
                                    + " has no method to validate user input!");
                    continue;
                }
                
                Class<? extends Object> propClass = propValue.getClass();
                if (f.getType().isPrimitive()) {
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
                        NeptusLog.pub().debug("Property "+providerClass.getSimpleName()+"."+name+" has no method to validate user input!" );
                        continue;
                    }
                    catch (SecurityException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        continue;
                    }
                }
                catch (SecurityException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                    continue;
                }

                // If method has been found, invoke it
                Object res;
                try {
                    res = m.invoke(obj, propValue);
                }
                catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    continue;
                }
                // In case of error add error message to the error message array
                if (res != null)
                    errors.add(res.toString());
            }
        }

        return errors.toArray(new String[0]);
    }
    
    private static Field[] getFields(Object o) {
        Class<?> c;
        if (o instanceof Class<?>)
            c = (Class<?>)o;
        else
            c = o.getClass();
        
        HashSet<Field> fields = new HashSet<>(); 
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
                    NeptusLog.pub().debug("Property " + name + " will not be saved.");
                    continue;
                }
                try {
                    propertyValue = property.getValue();
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
                    }
                    catch (Exception e2) {
                        e2.printStackTrace();
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
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
            defaults = (obj instanceof Class<?> ? (Class<?>) obj : obj.getClass()).newInstance();
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

    public static String getConfigXml(Object obj) {
        Properties props = saveProperties(obj, true);
        // props.list(System.err);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            props.storeToXML(baos, "Generated by Neptus on " + (new Date()));
            String xml = baos.toString("utf-8");
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
        String propsFilename = "conf/plugins/" + obj.getClass().getSimpleName() + "-" + instanceName + ".properties";

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
        new File("conf/plugins").mkdirs();
        String propsFilename = "conf/plugins/" + obj.getClass().getSimpleName() + "-" + instanceName + ".properties";

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
        extractFieldsWorker(clazz.getSuperclass(), dFA);
    }

    /**
     * Adds all jars contained in the {@value #PLUGINS_DIR}UGINS_DIR to the system's classpath
     */
    public static void loadPlugins() {
        if (!PLUGINS_DIR.isDirectory() || !PLUGINS_DIR.exists()) {
            // PLUGINS_DIR.mkdirs();
            NeptusLog.pub().warn("No plugins found to load at \"" + PLUGINS_DIR.getPath() + "\"");
            return;
        }

        for (File f : PLUGINS_DIR.listFiles()) {
            if (FileUtil.getFileExtension(f).equalsIgnoreCase("jar")) {
                try {
                    NeptusLog.pub().info("<###>Adding " + f + " to classpath...");
                    addToClassPath(f.toURI().toURL());
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
            }
        }
        pluginsLoaded = true;
    }

    /**
     * Given the classname of an interface, gives all the classes that implement that interface
     * 
     * @param interfaceName the looked after interface
     * @return A list of classes (names) that implement the given interface
     */
    public static String[] listPlugins(String interfaceName) {

        if (!pluginsLoaded)
            loadPlugins();

        Vector<String> plugins = new Vector<String>();
        try {
            @SuppressWarnings("rawtypes")
            Class c = Class.forName(interfaceName);
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Iterator iter = ServiceRegistry.lookupProviders(c);

            while (iter.hasNext()) {
                plugins.add(iter.next().getClass().getCanonicalName());
            }

            return plugins.toArray(new String[] {});
        }

        catch (Exception e) {
            NeptusLog.pub().error(e);
            return new String[] {};
        }
    }
    
    public static InputStream getResourceAsStream(String filename) {
        // Merge this with FileUtils
        return Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(filename);
    }
    
    public static String getResourceAsString(String filename) throws IOException{
        return IOUtils.toString(getResourceAsStream(filename));
    }
    
    /**
     * Given an URL to a resource (.jar, .class, .png, ...), adds that resource to the system class path
     * 
     * @param u The URL of the resource to be added
     * @throws IOException In the case that the resourse can not be read
     */
    public static void addToClassPath(URL u) throws IOException {

        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        @SuppressWarnings("rawtypes")
        Class sysclass = URLClassLoader.class;

        try {
            @SuppressWarnings("unchecked")
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(sysloader, new Object[] { u });
        }
        catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }
    }
}