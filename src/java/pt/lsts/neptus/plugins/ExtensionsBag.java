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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jan 3, 2014
 */
package pt.lsts.neptus.plugins;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.ReflectionUtil;

/**
 * This class is used to store and instantiate implementations of different extension types 
 * @author zp
 *
 */
@SuppressWarnings("unchecked")
public class ExtensionsBag {

    private LinkedHashMap<Class<?>, LinkedHashMap<String, Class<?>>> extensions = new LinkedHashMap<>();
    
    /**
     * Create a new bag that will hold extensions of given types
     * @param extensionTypes The types to be stored in this bag
     */
    public ExtensionsBag(Class<?> ... extensionTypes) {
        for (Class<?> c : extensionTypes) {
            extensions.put(c, new LinkedHashMap<String, Class<?>>());
        }
    }
    
    /**
     * Given a class name, will inspect it and extract all valid extension types
     * @param className The class to be inspected (will not be initialized at this time)
     */
    public void addPlugin(String className) {
        try {
            Class<?> c = Class.forName(className);
            NeptusLog.pub().debug("loading '" + PluginUtils.getPluginName(c) + "'...");

            boolean added = false;

            for (Class<?> intf : extensions.keySet()) {
                if (ReflectionUtil.hasInterface(c, intf) || ReflectionUtil.isSubclass(c, intf)) {
                    String name = PluginUtils.getPluginName(c);
                    if (name.isEmpty())
                        name = c.getSimpleName();
                    
                    extensions.get(intf).put(name,c);
                }
                added = true;
            }
            
            if (!added) {
                System.err.println(c.getCanonicalName() + " not recognized");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        catch (Error e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 
     * Retrieves a list of classes implementing the given extension type
     * @param type The extension type of interest
     * @return A map from plugin names to respective classes
     */
    public <T> LinkedHashMap<String, Class<? extends T>> listExtensions(Class<T> type) {
        LinkedHashMap<String, Class<? extends T>> ret = new LinkedHashMap<>();
        
        if (extensions.containsKey(type)) {
            for (Entry<String, Class<?>> ent : extensions.get(type).entrySet()) {
                ret.put(ent.getKey(), (Class<? extends T>) ent.getValue());
            }            
        }
        
        return ret;
    }
    
    /**
     * Instantiates a plugin gicen extension type, name and initialization arguments
     * @param name The name of the plugin to instantiate
     * @param type The extension type
     * @param initParams The initialization arguments if applicable
     * @return The initialized plugin or <code>null</code> if an error occurs
     */
    public <T> T getPlugin(String name, Class<T> type, Object... initParams) {
        if (!extensions.containsKey(type)) {
            NeptusLog.pub().warn("There is no such plugin type: "+type.getSimpleName());
            return null;
        }
                    
        if (!extensions.get(type).containsKey(name)) {
            NeptusLog.pub().warn("There is no such plugin: '"+name+"' (of type "+type.getSimpleName()+")");
            return null;
        }
        
        Class<?> initTypes[] = new Class<?>[initParams.length];
        for (int i = 0; i < initParams.length; i++)
            initTypes[i] = initParams[i].getClass();
        
        try {
            if (initTypes.length > 0) {
                Class<?> c = extensions.get(type).get(name);
                Constructor<?> cons = c.getConstructor(initTypes);
                return (T) cons.newInstance(initParams);
            }
            else {
                return (T)extensions.get(type).get(name).newInstance();
            }            
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            e.printStackTrace();
            return null;
        }
        catch (Error e) {
            NeptusLog.pub().error(e);
            return null;
        }        
    }    
}
