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
 * 2009/09/15
 * $Id:: GaugeSubPanel.java 9616 2012-12-30 23:23:22Z pdias               $:
 */
package pt.up.fe.dceg.neptus.plugins.gauges;

import java.awt.BorderLayout;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.ConsoleScript;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;

/**
 * @author zp
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(author = "ZP", name = "Gauge Panel", description = "This panel displays a variable as a bar gauge", icon = "pt/up/fe/dceg/neptus/plugins/gauges/gauges.png")
public class GaugeSubPanel extends SimpleSubPanel implements ConfigurationListener, IPeriodicUpdates {
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
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
