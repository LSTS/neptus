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
 * Author: José Pinto
 * Jun 28, 2011
 */
package pt.lsts.neptus.junit.plugins;

import java.util.LinkedHashMap;

import junit.framework.TestCase;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.plugins.PluginsLoader;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 *
 */
public class PluginsTest extends TestCase {


    public void testPanelPlugins() {
        ConfigFetch.initialize();
        PluginsLoader.load();

        LinkedHashMap<String, Class<? extends ConsolePanel>> sp = PluginsRepository.getPanelPlugins();

        for (String p : sp.keySet()) {
            Object o = null;
            try {
                NeptusLog.pub().info("<###>instantiating "+p);
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
        PluginsLoader.load();

        LinkedHashMap<String, Class<? extends MRAVisualization>> sp = PluginsRepository.getMraVisualizations();

        for (String p : sp.keySet()) {
            Object o = null;
            try {
                NeptusLog.pub().info("<###>instantiating "+p);
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
