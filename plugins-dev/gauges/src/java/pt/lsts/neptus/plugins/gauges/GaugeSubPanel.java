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
 * Author: José Pinto
 * 2009/09/15
 */
package pt.lsts.neptus.plugins.gauges;

import java.awt.BorderLayout;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.ConsoleScript;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;

/**
 * @author zp
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(author = "ZP", name = "Gauge Panel", description = "This panel displays a variable as a bar gauge", icon = "pt/lsts/neptus/plugins/gauges/gauges.png")
public class GaugeSubPanel extends ConsolePanel implements ConfigurationListener, IPeriodicUpdates {
    private GaugeDisplay display = new GaugeDisplay();
    private ConsoleScript script = new ConsoleScript(), textScript = new ConsoleScript();

    @NeptusProperty
    public String expression = "$(CpuUsage.usage)/100";

    public String validateExpression(String script) {
        try {
            new ConsoleScript().setScript(script);
        }
        catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    @NeptusProperty
    public String textToDisplay = "\"Usage: \"+($(CpuUsage.usage)/100)";

    public String validateTextToDisplay(String script) {
        try {
            new ConsoleScript().setScript(script);
        }
        catch (Exception e) {
            return e.getMessage();
        }
        return null;
    }

    @NeptusProperty
    public int millisBetweenUpdates = 1000;

    public GaugeSubPanel(ConsoleLayout console) {
        super(console);
        removeAll();
        setLayout(new BorderLayout());
        add(display);
    }

    @Override
    public long millisBetweenUpdates() {
        return millisBetweenUpdates;
    }

    @Override
    public void propertiesChanged() {
        try {
            script.setScript(expression);
            textScript.setScript(textToDisplay);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean update() {
        Object o = script.evaluate(getState());
        if (o != null && o instanceof Number)
            display.setValue(((Number) o).doubleValue());

        o = textScript.evaluate(getState());
        if (o != null)
            display.setToolTipText(o.toString());
        else
            display.setToolTipText("null");
        return true;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
