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
 * $Id:: PluginsRepository.java 9616 2012-12-30 23:23:22Z pdias           $:
 */
package pt.up.fe.dceg.neptus.plugins;

import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.console.plugins.SubPanelProvider;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.renderer2d.tiles.MapPainterProvider;
import pt.up.fe.dceg.neptus.renderer2d.tiles.Tile;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;

public class PluginsRepository {

    private static LinkedHashMap<String, Class<? extends NeptusAction>> actionClasses = new LinkedHashMap<String, Class<? extends NeptusAction>>();
    private static LinkedHashMap<String, Class<? extends SubPanelProvider>> panelClasses = new LinkedHashMap<String, Class<? extends SubPanelProvider>>();
    private static LinkedHashMap<String, Class<? extends NeptusMessageListener>> msgListenerClasses = new LinkedHashMap<String, Class<? extends NeptusMessageListener>>();
    private static LinkedHashMap<String, Class<? extends MRAVisualization>> visualizations = new LinkedHashMap<String, Class<? extends MRAVisualization>>();
    private static LinkedHashMap<String, Class<? extends MapTileProvider>> tileProviders = new LinkedHashMap<String, Class<? extends MapTileProvider>>();

    private static LinkedHashMap<Class<?>, LinkedHashMap<String, Class<?>>> otherPlugins = new LinkedHashMap<Class<?>, LinkedHashMap<String, Class<?>>>();

    public static void addOtherPlugin(Class<?> service, Class<?> implementation) {
        if (!otherPlugins.containsKey(service)) {
            otherPlugins.put(service, new LinkedHashMap<String, Class<?>>());
        }
        otherPlugins.get(service).put(PluginUtils.getPluginName(implementation), implementation);
    }

    public static LinkedHashMap<String, Class<?>> getImplementers(Class<?> service) {
        return otherPlugins.get(service);
    }

    @SuppressWarnings("unchecked")
    public static void addPlugin(String className) {
        try {
            Class<?> c = Class.forName(className);
            NeptusLog.pub().info("loading '" + PluginUtils.getPluginName(c) + "'...");

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
    
    /**
     * Factory for SubPanel plugins
     * @param pluginName
     * @param console
     * @return
     */
    public static SubPanel getPanelPlugin(String pluginName, ConsoleLayout console){
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
