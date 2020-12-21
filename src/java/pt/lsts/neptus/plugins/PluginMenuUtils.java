/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Feb 1, 2016
 */
package pt.lsts.neptus.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ReflectionUtil;

/**
 * This class provides utilitary methods to automatically add menus to a console.
 * Any method in the plugin class annotated with {@linkplain NeptusMenuItem} is 
 * invoked when the menu is activated by the user.
 * @author zp
 */
public class PluginMenuUtils {

    
    public static String translatePath(String path) {
        String translatedPath = "";
        for (String part : path.split(">")) {
            if (translatedPath.isEmpty())
                translatedPath = I18n.text(part.trim());
            else
                translatedPath += ">"+I18n.text(part.trim());                    
        }
        
        return translatedPath;
    }
    
    /**
     * This method will look for {@linkplain NeptusMenuItem} annotatated methods and add them as menus to the console
     * @param console The console where to add the menus
     * @param plugin A class with zero or more {@linkplain NeptusMenuItem} annotated methods.
     * @return The menus that were added to the console
     */
    public static List<JMenuItem> addPluginMenus(ConsoleLayout console, final Object plugin) {
        ArrayList<JMenuItem> items = new ArrayList<>();
        for (final Method m : ReflectionUtil.getMethodsAnnotatedWith(NeptusMenuItem.class, plugin)) {
            NeptusMenuItem ann = m.getAnnotation(NeptusMenuItem.class);
            
            JMenuItem item = console.addMenuItem(translatePath(ann.value()), null, new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        if (m.getParameterCount() == 0)
                            m.invoke(plugin);
                        else if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == ActionEvent.class)
                            m.invoke(plugin, e);
                    }
                    catch (Exception ex) {
                        NeptusLog.pub().error(ex);
                    }
                }
            });
            items.add(item); 
        }       
        return items;
    }
    
    /**
     * This method will remove the automatically added menus from given plugin
     * @param console The console from which to remove the menus
     * @param plugin The plugin from where the menus were added
     * @see PluginMenuUtils#addPluginMenus(ConsoleLayout, Object)
     */
    public static void removePluginMenus(ConsoleLayout console, final Object plugin) {
        for (final Method m : ReflectionUtil.getMethodsAnnotatedWith(NeptusMenuItem.class, plugin)) {
            NeptusMenuItem ann = m.getAnnotation(NeptusMenuItem.class);
            String[] path = ann.value().split(">");
            for (int i = 0; i < path.length; i++)
                path[i] = I18n.text(path[i].trim());
            console.removeMenuItem(path);
        }
    }
}
