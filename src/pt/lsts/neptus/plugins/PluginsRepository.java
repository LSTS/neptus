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
import pt.lsts.neptus.mra.replay.LogReplayLayer;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.renderer2d.tiles.MapPainterProvider;
import pt.lsts.neptus.renderer2d.tiles.Tile;
import pt.lsts.neptus.util.ReflectionUtil;

public class PluginsRepository {

   
    private static ExtensionsBag extensions = new ExtensionsBag(
            NeptusAction.class,
            SubPanelProvider.class,
            NeptusMessageListener.class,
            MRAVisualization.class,
            LogReplayLayer.class
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
    public static SubPanel getPanelPlugin(String pluginName, ConsoleLayout console) {
        try {
            SubPanelProvider spprov = extensions.getPlugin(pluginName, SubPanelProvider.class, console);
            return spprov.getSubPanel();
        }
        catch (Exception e) {
            NeptusLog.pub().error("loading panel plugin ", e);
            return null;
        }
    }

    public static <T> T getPlugin(String name, Class<T> type, Object... initParams) {
        return extensions.getPlugin(name, type, initParams);
    }
    
    public static LinkedHashMap<String, Class<? extends SubPanelProvider>> getPanelPlugins() {
        return extensions.listExtensions(SubPanelProvider.class);
    }

    public static LinkedHashMap<String, Class<? extends MRAVisualization>> getMraVisualizations() {
        return extensions.listExtensions(MRAVisualization.class);
    }
    
    public static LinkedHashMap<String, Class<? extends LogReplayLayer>> getReplayLayers() {
        return extensions.listExtensions(LogReplayLayer.class);
    }

    public static LinkedHashMap<String, Class<? extends MapTileProvider>> getTileProviders() {
        return tileProviders;
    }
}
