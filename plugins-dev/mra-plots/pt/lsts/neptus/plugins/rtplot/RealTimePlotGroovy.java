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
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.GuiUtils;

@PluginDescription(name = "Real-Time plot", icon = "pt/lsts/neptus/plugins/rtplot/rtplot.png", description = "Real-Time plots with Groovy scripts")
@Popup(accelerator = 'U', pos = POSITION.CENTER, height = 300, width = 300)
public class RealTimePlotGroovy extends ConsolePanel implements ConfigurationListener {

    private static final long serialVersionUID = 1L;
    private static PlotType type = PlotType.TIMESERIES;
    private TimeSeriesCollection allTsc = new TimeSeriesCollection();
    private TimeSeriesCollection selTsc = new TimeSeriesCollection();
    private XYSeriesCollection xySeries = new XYSeriesCollection();
    private XYSeriesCollection xySelSeries = new XYSeriesCollection();
    private JButton btnEdit, btnClear;
    private JComboBox<String> sysSel;
    private ItemListener itemListener;
    private JLabel vLabel;
    private JPanel bottom, top;
    private String selectedSys = null;
    private GroovyShell shell;
    private CompilerConfiguration cnfg;
    private ImportCustomizer imports;
    private Writer scriptOutput, scriptError;
    private StringBuffer bufferO, bufferE;
    private boolean updating = false;
    private final ChartPanel chart;
    private List<String> systems = Collections.synchronizedList(new ArrayList<String>());

    public enum PlotType {
        TIMESERIES, // default
        LATLONG,
        GENERICXY

    }
    // private final String apllyMethod = "LinkedHashMap.metaClass.function = {f -> delegate.each {
    // [(it.key):function.call(it.value)]}}";

    /**
     * @return the systems
     */
    public synchronized List<String> getSystems() {
        return systems;
    }

    @NeptusProperty(name = "Periodicity (milliseconds)")
    public int periodicity = 1000;

    @NeptusProperty(name = "Maximum Number of points")
    public int numPoints = 100;

    @NeptusProperty(name = "Current Script")
    public String traceScript = "s = \"EstimatedState.depth\"\naddTimeSerie(msgs(s))";

    @NeptusProperty(name = "Initial Script")
    public String initScripts = "s = \"EstimatedState.depth\"\naddTimeSerie(msgs(s))";

    @NeptusProperty(name = "Plot Type")
    public String plotType = PlotType.TIMESERIES.name();

    private String previousScript = "";

    private int numPointsBefore = numPoints;

    public RealTimePlotGroovy(ConsoleLayout c) {
        super(c);
        type = PlotType.valueOf(plotType);
        JFreeChart timeSeriesChart;
        if (type.equals(PlotType.TIMESERIES))
            timeSeriesChart = ChartFactory.createTimeSeriesChart(null, null, null, allTsc, true, true, true);
        else
            timeSeriesChart = ChartFactory.createScatterPlot(null, null, null, xySeries, PlotOrientation.HORIZONTAL,
                    true, true, true);
        chart = new ChartPanel(timeSeriesChart);
        // init shell
        cnfg = new CompilerConfiguration();
        imports = new ImportCustomizer();
        imports.addStarImports("pt.lsts.imc", "java.lang.Math", "pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder");
        imports.addStaticStars("pt.lsts.neptus.plugins.rtplot.PlotScript");
        // try {
        // shell.evaluate(apllyMethod);
        // }
        // catch (CompilationFailedException e) {
        // NeptusLog.pub().error(I18n.text("Error setting up script shell."), e);
        // //e.printStackTrace();
        // }
        cnfg.addCompilationCustomizers(imports);
        redirectIO();
        previousScript = traceScript;
        shell = new GroovyShell(this.getClass().getClassLoader(), cnfg);
        PlotScript.setPlot(this);
        shell.setProperty("out", scriptOutput);
        shell.setProperty("err", scriptError);
        configLayout();
        add(chart, BorderLayout.CENTER);
    }

    /**
     * 
     */
    private void redirectIO() {
        bufferO = new StringBuffer();
        scriptOutput = new Writer() {

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                bufferO.append(cbuf, off, len);

            }

            @Override
            public void flush() throws IOException {
                NeptusLog.pub().debug(I18n.text(bufferO.toString()));
                bufferO = new StringBuffer();

            }

            @Override
            public void close() throws IOException {
            }
        };
        bufferE = new StringBuffer();
        scriptError = new Writer() {

            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                bufferE.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                NeptusLog.pub().error(I18n.text(bufferE.toString()));
                bufferE = new StringBuffer();
            }

            @Override
            public void close() throws IOException {
                scriptOutput.close();

            }
        };
    }

    public void bind(String var, Object value) {
        shell.setVariable(var, value);
    }

    public void unbind(String var) {
        shell.getContext().getVariables().remove(var);
    }

    /**
     * @param allTsc the TimeSeries to set
     */
    public void addTimeSerie(String id, TimeSeries ts) {
        if (allTsc.getSeries(id) == null) {
            ts.setMaximumItemCount(numPoints);
            allTsc.addSeries(ts);
        }
        else
            allTsc.getSeries(id).addOrUpdate(ts.getDataItem(0));
    }

    /**
     * @param allTsc the TimeSeries to set
     */
    public void addSerie(String id, XYSeries xys) {
        if (xySeries.getSeriesIndex(id) == -1) {
            xys.setMaximumItemCount(numPoints);
            xySeries.addSeries(xys);
        }
        else
            xySeries.getSeries(id).addOrUpdate(xys.getDataItem(0));
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
        btnClear = new JButton(I18n.text("Clear"));
        bottom.add(btnClear);
        btnClear.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                resetSeries();
                runScript(traceScript);
            }
        });

        add(bottom, BorderLayout.SOUTH);
        sysSel = new JComboBox<String>();
        selectedSys = "ALL";
        itemListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (sysSel.getSelectedItem() != null && !updating)
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        String sel = (String) sysSel.getSelectedItem();
                        if (!sel.equals(selectedSys)) {
                            selectedSys = new String(sel);
                            if (selectedSys.equalsIgnoreCase("ALL")) {
                                Arrays.stream(ImcSystemsHolder.lookupActiveSystemVehicles())
                                        .forEach(s -> systems.add(s.getName()));
                                if (type.equals(PlotType.TIMESERIES)) {
                                    changeChartSrc(allTsc); // Reestablish initial timeSeries collection
                                }
                                else {
                                    changeChartSrc(xySeries);
                                }
                            }
                            else { // One System selected
                                systems.clear();
                                systems.add(selectedSys);
                                if (type.equals(PlotType.TIMESERIES)) {
                                    resetSelSeries();
                                }
                                else { // XY
                                    resetSelXY();
                                }
                            }
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
     * @return the type
     */
    public PlotType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(PlotType t) {
        if (!type.equals(t)) {
            RealTimePlotGroovy.type = t;
            changeChart();
        }
    }

    /**
     * 
     */
    public void changeChartSrc(TimeSeriesCollection tsc) {
        JFreeChart timeSeriesChart = ChartFactory.createTimeSeriesChart(null, null, null, tsc, true, true, true);
        chart.setChart(timeSeriesChart);
    }

    public void changeChartSrc(XYSeriesCollection xys) {
        JFreeChart xyChart = ChartFactory.createScatterPlot(null, null, null, xys, PlotOrientation.HORIZONTAL, true,
                true, true);
        chart.setChart(xyChart);
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

    @Periodic(millisBetweenUpdates = 1000)
    public boolean update() {
        // System.err.println("\nUpdating "+type.name());
        if (!isShowing())
            return true;
        updateComboBox();
        systems.clear();
        for (ImcSystem system : ImcSystemsHolder.lookupActiveSystemVehicles()) {
            if (selectedSys.equalsIgnoreCase("ALL"))
                systems.add(system.getName());
            else if (selectedSys.equalsIgnoreCase(system.getName()))
                systems.add(selectedSys);
        }
        try {
            runScript(traceScript);
        }
        catch (Exception e) {
            NeptusLog.pub().error(I18n.text("Error updating script for real-time plot"), e);
            //System.err.println("\nError on Update " + e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    @Override
    public void propertiesChanged() {
        if (!traceScript.equals(previousScript) || numPoints != numPointsBefore) {
            resetSeries();
        }
        previousScript = traceScript;
        numPointsBefore = numPoints;
        runScript(traceScript);
    }

    /**
     * 
     */
    public void resetSeries() {
        allTsc.removeAllSeries();
        resetSelSeries();
        xySeries.removeAllSeries();
        resetSelXY();
        chart.getChart().fireChartChanged();
        //runScript(traceScript);
    }

    private void changeChart() {

        if (systems.size() > 1) {
            if (type.equals(PlotType.TIMESERIES))
                changeChartSrc(allTsc);
            else
                changeChartSrc(xySeries);
        }
        else {
            if (type.equals(PlotType.TIMESERIES))
                resetSelSeries();
            else
                resetSelXY();
        }

    }

    public Object invokeMethod(Binding b, String method, String... args) {
        return shell.invokeMethod(method, args);
    }

    public void runScript(String script) {
        Object result;
        PlotScript.setSystems(systems);
        if (ImcSystemsHolder.lookupActiveSystemVehicles().length > 0) {
            try {
                shell.parse(script);
                result = shell.evaluate(script);
            }
            catch (CompilationFailedException e) {
                GuiUtils.errorMessage(this, "Error Parsing Script", e.getLocalizedMessage());
            }
        }
    }

    @Override
    public void initSubPanel() {
        previousScript = traceScript;
        propertiesChanged();
    }

    @Override
    public void cleanSubPanel() {
        try {
            scriptOutput.close();
            scriptError.close();
        }
        catch (IOException e) {
            NeptusLog.pub().error(I18n.text("Error closing IO Writer"), e);
            // e.printStackTrace();
        }
    }

    /**
     * 
     */
    private void resetSelXY() {
        xySelSeries = new XYSeriesCollection();
        for (Object s : xySeries.getSeries()) {
            XYSeries serie = (XYSeries) s;
            if (serie.getKey().toString().startsWith(selectedSys))
                xySelSeries.addSeries(serie);
            changeChartSrc(xySelSeries);
        }
    }

    /**
     * 
     */
    private void resetSelSeries() {
        selTsc = new TimeSeriesCollection();
        for (Object s : allTsc.getSeries()) {
            TimeSeries serie = (TimeSeries) s;
            if (serie.getKey().toString().startsWith(selectedSys))
                selTsc.addSeries(serie);
        }
        changeChartSrc(selTsc);
    }
}
