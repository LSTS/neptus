/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Jun 28, 2011
 */
package pt.up.fe.dceg.neptus.junit.plugins;

import java.util.LinkedHashMap;

import junit.framework.TestCase;
import pt.up.fe.dceg.neptus.console.plugins.SubPanelProvider;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.plugins.PluginClassLoader;
import pt.up.fe.dceg.neptus.plugins.PluginsRepository;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
public class PluginsTest extends TestCase {


    public void testPanelPlugins() {
        ConfigFetch.initialize();
        PluginClassLoader.install();

        LinkedHashMap<String, Class<? extends SubPanelProvider>> sp = PluginsRepository.getPanelPlugins();

        for (String p : sp.keySet()) {
            Object o = null;
            try {
                System.out.println("instantiating "+p);
                o = sp.get(p).newInstance();
            }
            catch (Error e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            assertNotNull(o);
        }               
    }

    public void testMRAVisualizations() {
        ConfigFetch.initialize();
        PluginClassLoader.install();

        LinkedHashMap<String, Class<? extends MRAVisualization>> sp = PluginsRepository.getMraVisualizations();

        for (String p : sp.keySet()) {
            Object o = null;
            try {
                System.out.println("instantiating "+p);
                o = sp.get(p).newInstance();
            }
            catch (Error e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            assertNotNull(o);
        }               
    }
}
