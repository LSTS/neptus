/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Feb 14, 2013
 */
package pt.lsts.neptus.plugins.rtplot;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;


import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.GroovyScriptEngine;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;

@PluginDescription(name = "Real-Time plot", icon = "pt/lsts/neptus/plugins/rtplot/rtplot.png", description = "Real-Time plots with Groovy scripts")
@Popup(accelerator = 'U', pos = POSITION.CENTER, height = 300, width = 300)
public class RealTimePlotGroovy extends ConsolePanel implements IPeriodicUpdates, ConfigurationListener {

    private static final long serialVersionUID = 1L;
    private JFreeChart timeSeriesChart = null;
    private TimeSeriesCollection tsc = new TimeSeriesCollection();
    private JButton btnEdit, btnClear, cfgEdit;
    private JComboBox<String> sysSel;
    private ItemListener itemListener;
    private JLabel vLabel;
    private LinkedHashMap<String, groovy.lang.Script> scripts = new LinkedHashMap<>();
    private JPanel bottom, top;
    private String selectedSys = null;
    private GroovyShell shell;
    private CompilerConfiguration cnfg;
    private ImportCustomizer imports;
    private boolean updating = false;

    @NeptusProperty(name = "Periodicity (milliseconds)")
    public int periodicity = 1000;

    @NeptusProperty(name = "Maximum Number of points")
    public int numPoints = 100;

    @NeptusProperty(name = "Traces Script")
    public String traceScripts = "roll: ${EstimatedState.phi} * 180 / Math.PI;\npitch: ${EstimatedState.theta} * 180 / Math.PI;\nyaw: ${EstimatedState.psi} * 180 / Math.PI";
    
    @NeptusProperty(name = "Initial Script")
    public String initScripts = "createPlot(Salinity)";

    private String traceScriptsBefore = "";
    private int numPointsBefore = numPoints;

    public RealTimePlotGroovy(ConsoleLayout c) {
        super(c);
        //init shell
        cnfg = new CompilerConfiguration();
        imports = new ImportCustomizer();
        imports.addStarImports("pt.lsts.imc.net.IMCProtocol","java.lang.Math");
        shell = new GroovyShell(cnfg);
        configLayout();
    }
    
    /**
     * @param tsc the tsc to set
     */
    public void addSerie(TimeSeries ts) {
        this.tsc.addSeries(ts);
    }

    /**
     * ConsolePanel layout configuration
     */
    public void configLayout() {
        setLayout(new BorderLayout());
        bottom = new JPanel(new GridLayout(1, 0));
        btnEdit = new JButton(I18n.text("Settings"));
        bottom.add(btnEdit);
        btnEdit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                RealTimePlotScript.editSettings(RealTimePlotGroovy.this, selectedSys);
            }
        });
        cfgEdit = new JButton(I18n.text("Config"));
        bottom.add(cfgEdit);
        cfgEdit.addActionListener(new ActionListener() {

            @SuppressWarnings("deprecation")
            @Override
            public void actionPerformed(ActionEvent e) {
                PluginUtils.editPluginProperties(RealTimePlotGroovy.this, true);
            }
        });
        btnClear = new JButton(I18n.text("Clear"));
        bottom.add(btnClear);
        btnClear.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tsc.removeAllSeries();
            }
        });

        add(bottom, BorderLayout.SOUTH);
        timeSeriesChart = ChartFactory.createTimeSeriesChart(null, null, null, tsc, true, true, true);
        add(new ChartPanel(timeSeriesChart), BorderLayout.CENTER);

        sysSel = new JComboBox<String>();
        selectedSys = "ALL";
        itemListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(sysSel.getSelectedItem() != null && !updating)
                    if(e.getStateChange() == ItemEvent.SELECTED) {
                        String sel = (String) sysSel.getSelectedItem();
                        if (!sel.equals(selectedSys)){
                            selectedSys = new String(sel);
                        }
                    }
                
            }
        };
        updateComboBox();
        sysSel.addItemListener(itemListener);
        
        vLabel = new JLabel("System:");
        vLabel.setToolTipText("Select System(s)");
        top = new JPanel(new GridLayout(0, 2));
        top.add(vLabel);
        top.add(sysSel);
        add(top, BorderLayout.NORTH);
    }

    /**
     * Update available systems
     */
    public synchronized void updateComboBox() {
        updating = true;
        sysSel.removeAllItems();
        sysSel.addItem("ALL");
        Arrays.stream(ImcSystemsHolder.lookupActiveSystemVehicles()).forEach(i -> sysSel.addItem(i.getName()));
        sysSel.setSelectedItem(selectedSys);
        updating = false;
    }

    @Override
    public long millisBetweenUpdates() {
        return periodicity;
    }

    @Override
    public boolean update() {
        if (!isShowing())
            return true;
        Collection<String> traces = scripts.keySet();
        updateComboBox();
        for (ImcSystem sys : ImcSystemsHolder.lookupActiveSystemVehicles()) {
            // if (!vehicleContexts.containsKey(sys.getName())) {
            //
            // }

            // Global global = vehicleContexts.get(sys.getName());
            for (String s : traces) {
                // Object o = scripts.get(s).exec(context, global);
                // if (o instanceof NativeJavaObject) {
                // o = ((NativeJavaObject)o).unwrap();
                // }
                Object o = null;
                String seriesName = sys.getName() + "." + s;
                TimeSeries ts = tsc.getSeries(seriesName);
                if (ts == null) {
                    ts = new TimeSeries(seriesName);
                    ts.setMaximumItemCount(numPoints);
                    tsc.addSeries(ts);
                }
                ts.addOrUpdate(new Millisecond(new Date(System.currentTimeMillis())), Double.parseDouble(o.toString()));

            }
        }
        return true;
    }

    @Override
    public void propertiesChanged() {
        if (!traceScripts.equals(traceScriptsBefore) || numPoints != numPointsBefore) {
            tsc.removeAllSeries();
            scripts.clear();

            try {
                //parseScript();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        traceScriptsBefore = traceScripts;
        numPointsBefore = numPoints;
    }

//    protected void parseScript() throws Exception {
//        Pattern p = Pattern.compile("([\\w ]+):(.*)");
//
//        for (String line : traceScripts.split("\n")) {
//            Matcher m = p.matcher(line);
//            if (m.matches()) {
//                 String ss = m.group(2);
//                 ss = ss.replaceAll("\\$\\{([^\\}]*)\\}", "state.expr(\"$1\")");
//                 String name = m.group(1);
//                 Context.enter();
//                 Script sc = context.compileString(ss, name, 1, null);
//                 Context.exit();
//                 scripts.put(name, sc);
//            }
//        }
//        tsc.removeAllSeries();
//    }
    
    public void runScript (Binding b,String script) {
        shell = new GroovyShell(b, cnfg);
        shell.parse(script);
        Object result = shell.evaluate(script);
    }

    @Override
    public void initSubPanel() {
        traceScriptsBefore = traceScripts;
        propertiesChanged();
    }

    @Override
    public void cleanSubPanel() {
        // nothing
    }
}
