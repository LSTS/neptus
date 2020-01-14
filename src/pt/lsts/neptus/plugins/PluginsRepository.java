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

import java.util.LinkedHashMap;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.mp.element.IPlanElement;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.replay.LogReplayLayer;
import pt.lsts.neptus.mra.replay.LogReplayPanel;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.renderer2d.tiles.MapPainterProvider;
import pt.lsts.neptus.renderer2d.tiles.Tile;
import pt.lsts.neptus.types.mission.plan.IPlanFileExporter;
import pt.lsts.neptus.util.ReflectionUtil;

public class PluginsRepository {

   
    private static ExtensionsBag extensions = new ExtensionsBag(
            ConsolePanel.class,
            ConsoleLayer.class,
            ConsoleInteraction.class,
            MRAVisualization.class,
            MRAExporter.class,
            LogReplayLayer.class,
            LogReplayPanel.class,
            IPlanFileExporter.class,
            IPlanElement.class
            );
      
    private static LinkedHashMap<String, Class<? extends MapTileProvider>> tileProviders = new LinkedHashMap<String, Class<? extends MapTileProvider>>();

    @SuppressWarnings("unchecked")
    public static void addPlugin(String className) {
        extensions.addPlugin(className);
        
        // Map Provider specific code FIXME
        try {
            Class<?> c = Class.forName(className);
            if (ReflectionUtil.hasAnnotation(c, MapTileProvider.class)) {
                if (ReflectionUtil.hasAnySuperClass(c, Tile.class)
                        || ReflectionUtil.hasInterface(c, MapPainterProvider.class)) {
                    tileProviders.put(PluginUtils.getPluginName(c), (Class<MapTileProvider>) c);
                }
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
     * 
     * @param pluginName
     * @param console
     * @return
     */
    public static ConsolePanel getPanelPlugin(String pluginName, ConsoleLayout console) {
        try {
            ConsolePanel spprov = extensions.getPlugin(pluginName, ConsolePanel.class, console);
            return spprov;
        }
        catch (Exception e) {
            NeptusLog.pub().error("loading panel plugin ", e);
            return null;
        }
    }
    
    public static ConsoleLayer getConsoleLayer(String pluginName) {
        try {
            ConsoleLayer spprov = extensions.getPlugin(pluginName, ConsoleLayer.class);
            return spprov;
        }
        catch (Exception e) {
            NeptusLog.pub().error("loading layer plugin ", e);
            return null;
        }
    }
    
    public static ConsoleInteraction getConsoleInteraction(String pluginName) {
        try {
            ConsoleInteraction spprov = extensions.getPlugin(pluginName, ConsoleInteraction.class);
            return spprov;
        }
        catch (Exception e) {
            NeptusLog.pub().error("loading interaction plugin ", e);
            return null;
        }
    }
    
    public static IPlanElement<?> getPlanElement(String pluginName) {
        try {
            IPlanElement<?> spprov = extensions.getPlugin(pluginName, IPlanElement.class);
            return spprov;
        }
        catch (Exception e) {
            NeptusLog.pub().error("loading layer plugin ", e);
            return null;
        }
    }
    
    public static <T> LinkedHashMap<String, Class<? extends T>> listExtensions(Class<T> type) {
        return extensions.listExtensions(type);
    }

    public static <T> T getPlugin(String name, Class<T> type, Object... initParams) {
        return extensions.getPlugin(name, type, initParams);
    }
    
    public static LinkedHashMap<String, Class<? extends ConsolePanel>> getPanelPlugins() {
        return extensions.listExtensions(ConsolePanel.class);
    }
    
    public static LinkedHashMap<String, Class<? extends ConsoleLayer>> getConsoleLayerPlugins() {
        return extensions.listExtensions(ConsoleLayer.class);
    }
    
    public static LinkedHashMap<String, Class<? extends ConsoleInteraction>> getConsoleInteractions() {
        return extensions.listExtensions(ConsoleInteraction.class);
    }

    public static LinkedHashMap<String, Class<? extends MRAVisualization>> getMraVisualizations() {
        return extensions.listExtensions(MRAVisualization.class);
    }
    
    public static LinkedHashMap<String, Class<? extends LogReplayLayer>> getReplayLayers() {
        return extensions.listExtensions(LogReplayLayer.class);
    }

    @SuppressWarnings("rawtypes")
    public static LinkedHashMap<String, Class<? extends IPlanElement>> getPlanElements() {
        return extensions.listExtensions(IPlanElement.class);
    }

    public static LinkedHashMap<String, Class<? extends MapTileProvider>> getTileProviders() {
        return tileProviders;
    }
}
