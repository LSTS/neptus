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

import groovy.lang.GroovyShell;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.GuiUtils;

@PluginDescription(name = "Real-Time Plot", icon = "pt/lsts/neptus/plugins/rtplot/rtplot.png", description = "Real-Time plots with Groovy scripts")
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
    private static List<String> systems = Collections.synchronizedList(new ArrayList<String>());

    public enum PlotType {
        TIMESERIES, // default
        GENERICXY

    }

    /**
     * @return the systems
     */
    public static synchronized List<String> getSystems() {
        return systems;
    }

    @NeptusProperty(name = "Maximum Number of points")
    public int numPoints = 100;

    @NeptusProperty(name = "Current Script")
    public String traceScript = "s = msgs(\"EstimatedState.depth\")\naddTimeSerie s";

    @NeptusProperty(name = "Initial Script")
    public String initScripts = "s = msgs(\"EstimatedState.depth\")\naddTimeSerie s";

    @NeptusProperty(name = "Plot Type",units="TIMESERIES or GENERICXY",userLevel = LEVEL.ADVANCED,editable=false)
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
        cnfg.addCompilationCustomizers(imports);
        redirectIO();
        previousScript = traceScript;
        shell = new GroovyShell(this.getClass().getClassLoader(), cnfg);
        shell.setProperty("out", scriptOutput);
        shell.setProperty("err", scriptError);
        
        //Layout
        configLayout();
        add(chart, BorderLayout.CENTER);
    }

    /**
     * Redirects the script output and error writer to a Neptus one
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

    public void unbind(String var) {
        shell.getContext().getVariables().remove(var);
    }

    /**
     * Adds a new time series to the existing plot. If the series already exists, it updates it.
     * 
     * @param allTsc the TimeSeries to be added
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
     * Adds a new XY series to the existing plot. If the series already exists, it updates it.
     * 
     * @param allTsc the TimeSeries to be added
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
     * Swing properties setup ConsolePanel layout configuration
     */
    public void configLayout() {
        setLayout(new BorderLayout());
        bottom = new JPanel(new GridLayout(1, 0));
        btnEdit = new JButton(I18n.text("Settings"));
        bottom.add(btnEdit);
        btnEdit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    RealTimePlotScript.editSettings(RealTimePlotGroovy.this, selectedSys);
                }
                catch (Exception e1) {
                    traceScript = previousScript;
                    NeptusLog.pub().error(I18n.text("Error editing script for real-time plot"), e1);
                }
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
                                    updateSelSeries();
                                }
                                else { // XY
                                    updateSelXY();
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
     * @return the type of the current chart
     */
    public PlotType getType() {
        return type;
    }

    /**
     * Changes the type of chart to @PlotType.TIMESERIES or @PlotType.GenericXY according to the script
     * 
     * @param type the type to set
     */
    public void setType(PlotType t) {
        if (!type.equals(t)) {
            resetSeries();
            RealTimePlotGroovy.type = t;
            plotType = type.name();
            changeChart();
        }
    }

    /**
     * Changes the source of the current chart to a @TimeSeriescollection
     * 
     * @param tsc Data set to be used as source for the chart
     */
    public void changeChartSrc(TimeSeriesCollection tsc) {
        JFreeChart timeSeriesChart = ChartFactory.createTimeSeriesChart(null, null, null, tsc, true, true, false);
        chart.setChart(timeSeriesChart);
    }

    /**
     * Changes the source of the current chart to a @TimeSeriescollection
     * 
     * @param xys Data set to be used as source for the chart
     */
    public void changeChartSrc(XYSeriesCollection xys) {
        JFreeChart xyChart = ChartFactory.createScatterPlot(null, null, null, xys, PlotOrientation.VERTICAL, true, true,
                false);
        chart.setChart(xyChart);
    }

    /**
     * Update available systems on the comboBox
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
        if (!selectedSys.equalsIgnoreCase("ALL") && type.equals(PlotType.TIMESERIES))
            updateSelSeries();
        else if (!selectedSys.equalsIgnoreCase("ALL") && !type.equals(PlotType.TIMESERIES))
            updateSelXY();
        try {
            runScript(traceScript);
        }
        catch (Exception e) {
            traceScript = previousScript;
            NeptusLog.pub().error(I18n.text("Error updating script for real-time plot"), e);
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
     * Resets all currently series collection
     */
    public void resetSeries() {
        allTsc.removeAllSeries();
        selTsc.removeAllSeries();
        xySeries.removeAllSeries();
        xySelSeries.removeAllSeries();
    }

    /**
     * Changes the Type of chart according to the class field value
     */
    private void changeChart() {
        if (systems.size() > 1) {
            if (type.equals(PlotType.TIMESERIES))
                changeChartSrc(allTsc);
            else
                changeChartSrc(xySeries);
        }
        else {
            if (type.equals(PlotType.TIMESERIES))
                updateSelSeries();
            else
                updateSelXY();
        }

    }

    /**
     * Runs the Groovy script after verifying its validity by parsing it.
     * 
     * @param script Text script
     */
    public void runScript(String script) {
        if (ImcSystemsHolder.lookupActiveSystemVehicles().length > 0) {
            try {
                shell.setVariable("plot", RealTimePlotGroovy.this);
                String defplot = "configPlot plot";
                shell.evaluate(defplot);
                shell.parse(script);
                shell.evaluate(script);
            }
            catch (Exception e) {
                traceScript = previousScript;
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
        }
    }

    /**
     * Updates the XY series being used on the chart for a selected system on the comboBox
     */
    private void updateSelXY() {
        xySelSeries = new XYSeriesCollection();
        for (Object s : xySeries.getSeries()) {
            XYSeries serie = (XYSeries) s;
            if (serie.getKey().toString().startsWith(selectedSys))
                xySelSeries.addSeries(serie);
            changeChartSrc(xySelSeries);
        }
    }

    /**
     * Updates the time series being used on the chart for a selected system on the comboBox
     */
    private void updateSelSeries() {
        selTsc = new TimeSeriesCollection();
        for (Object s : allTsc.getSeries()) {
            TimeSeries serie = (TimeSeries) s;
            if (serie.getKey().toString().startsWith(selectedSys))
                selTsc.addSeries(serie);
        }
        changeChartSrc(selTsc);
    }
}
