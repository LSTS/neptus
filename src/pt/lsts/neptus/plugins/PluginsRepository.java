/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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

import java.util.LinkedHashMap;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.SubPanel;
import pt.lsts.neptus.console.plugins.SubPanelProvider;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.renderer2d.tiles.MapPainterProvider;
import pt.lsts.neptus.renderer2d.tiles.Tile;
import pt.lsts.neptus.util.ReflectionUtil;

public class PluginsRepository {

    private static LinkedHashMap<String, Class<? extends NeptusAction>> actionClasses = new LinkedHashMap<String, Class<? extends NeptusAction>>();
    private static LinkedHashMap<String, Class<? extends SubPanelProvider>> panelClasses = new LinkedHashMap<String, Class<? extends SubPanelProvider>>();
    private static LinkedHashMap<String, Class<? extends NeptusMessageListener>> msgListenerClasses = new LinkedHashMap<String, Class<? extends NeptusMessageListener>>();
    private static LinkedHashMap<String, Class<? extends MRAVisualization>> visualizations = new LinkedHashMap<String, Class<? extends MRAVisualization>>();
    private static LinkedHashMap<String, Class<? extends MapTileProvider>> tileProviders = new LinkedHashMap<String, Class<? extends MapTileProvider>>();
    private static LinkedHashMap<Class<?>, LinkedHashMap<String, Class<?>>> otherPlugins = new LinkedHashMap<Class<?>, LinkedHashMap<String, Class<?>>>();

    @SuppressWarnings("unchecked")
    public static void addPlugin(String className) {
        try {
            Class<?> c = Class.forName(className);
            NeptusLog.pub().debug("loading '" + PluginUtils.getPluginName(c) + "'...");

            boolean added = false;

            if (ReflectionUtil.hasInterface(c, NeptusAction.class)) {
                actionClasses.put(PluginUtils.getPluginName(c), (Class<NeptusAction>) c);
                added = true;
            }

            if (ReflectionUtil.hasInterface(c, SubPanelProvider.class)) {
                panelClasses.put(PluginUtils.getPluginName(c), (Class<SubPanelProvider>) c);
                added = true;
            }

            if (ReflectionUtil.hasInterface(c, NeptusMessageListener.class)) {
                msgListenerClasses.put(PluginUtils.getPluginName(c), (Class<NeptusMessageListener>) c);
                added = true;
            }

            if (ReflectionUtil.hasInterface(c, MRAVisualization.class)) {
                visualizations.put(PluginUtils.getPluginName(c), (Class<MRAVisualization>) c);
                added = true;
            }

            if (ReflectionUtil.hasAnnotation(c, MapTileProvider.class)) {
                if (ReflectionUtil.hasAnySuperClass(c, Tile.class)
                        || ReflectionUtil.hasInterface(c, MapPainterProvider.class)) {
                    tileProviders.put(PluginUtils.getPluginName(c), (Class<MapTileProvider>) c);
                    added = true;
                }
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

    public static void addOtherPlugin(Class<?> service, Class<?> implementation) {
        if (!otherPlugins.containsKey(service)) {
            otherPlugins.put(service, new LinkedHashMap<String, Class<?>>());
        }
        otherPlugins.get(service).put(PluginUtils.getPluginName(implementation), implementation);
    }

    public static LinkedHashMap<String, Class<?>> getImplementers(Class<?> service) {
        return otherPlugins.get(service);
    }

    /**
     * Factory for SubPanel plugins
     * 
     * @param pluginName
     * @param console
     * @return
     */
    public static SubPanel getPanelPlugin(String pluginName, ConsoleLayout console) {
        SubPanel np = null;
        try {
            np = (SubPanel) panelClasses.get(pluginName).getConstructor(ConsoleLayout.class).newInstance(console);
        }
        catch (Exception e) {
            NeptusLog.pub().error("loading panel plugin ", e);
        }

        return np;
    }

    public static NeptusAction getActionPlugin(String pluginName, String instanceName) {
        try {
            NeptusAction act = actionClasses.get(pluginName).newInstance();
            PluginUtils.loadProperties(act, instanceName);
            return act;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static NeptusMessageListener getMsgListenerPlugin(String pluginName, String instanceName) {
        try {
            NeptusMessageListener list = msgListenerClasses.get(pluginName).newInstance();
            PluginUtils.loadProperties(list, instanceName);
            return list;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static MRAVisualization getMraVisualization(String pluginName, String instanceName) {
        try {
            MRAVisualization list = visualizations.get(pluginName).newInstance();
            PluginUtils.loadProperties(list, instanceName);
            return list;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // public static Tile getTileProvider(String pluginName, String instanceName) {
    // try {
    // Tile list = tileProviders.get(pluginName).newInstance();
    // PluginUtils.loadProperties(list, instanceName);
    // return list;
    // }
    // catch (Exception e) {
    // e.printStackTrace();
    // }
    // return null;
    // }

    public static LinkedHashMap<String, Class<? extends SubPanelProvider>> getPanelPlugins() {
        return panelClasses;
    }

    public static LinkedHashMap<String, Class<? extends MRAVisualization>> getMraVisualizations() {
        return visualizations;
    }

    public static LinkedHashMap<String, Class<? extends MapTileProvider>> getTileProviders() {
        return tileProviders;
    }
}
