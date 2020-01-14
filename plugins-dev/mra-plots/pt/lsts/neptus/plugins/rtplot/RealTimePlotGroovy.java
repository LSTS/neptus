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
 * Author: Keila Lima
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
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
import pt.lsts.neptus.util.GuiUtils;

@PluginDescription(name = "Real-Time Plot", icon = "pt/lsts/neptus/plugins/rtplot/rtplot.png", description = "Real-Time plots with Groovy scripts")
@Popup(accelerator = 'U', pos = POSITION.CENTER, height = 300, width = 300)
public class RealTimePlotGroovy extends ConsolePanel implements ConfigurationListener {

    private static final long serialVersionUID = 1L;
    private PlotType type = PlotType.TIMESERIES;
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
    private boolean updating = false;
    private boolean drawLineXY = false;
    private boolean running = false;
    private final ChartPanel chart;
    private List<String> systems = Collections.synchronizedList(new ArrayList<String>());
    private ScheduledThreadPoolExecutor timedExec;
    private ScheduledFuture scheduleUpdate;
    private final Runnable updateTask;
    private final RealTimePlotScript editSettings;
    public final long PERIODICMIN = 100;

    public enum PlotType {
        TIMESERIES, // default
        GENERICXY

    }

    /**
     * @return the selected systems in the comboBox
     */
    public synchronized List<String> getSystems() {
        return systems;
    }

    @NeptusProperty(name = "Maximum Number of points")
    public int numPoints = 100;

    @NeptusProperty(name = "Periodicity (milliseconds)", description = "Update Interval\nRange:from 100 milliseconds", units = "Milliseconds")
    public long periodicity = 1000;

    @NeptusProperty(name = "Current Script")
    public String traceScript = "s = value(\"EstimatedState.depth\")\naddTimeSeries s";

    @NeptusProperty(name = "Initial Script")
    public String initScripts = "s = value(\"EstimatedState.depth\")\naddTimeSeries s";

    @NeptusProperty(name = "Plot Type", units = "TIMESERIES or GENERICXY", userLevel = LEVEL.ADVANCED, editable = false)
    public String plotType = PlotType.TIMESERIES.name();

    private String previousScript = "";

    private int numPointsBefore = numPoints;
    private long previousPeriod = periodicity;

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
        previousScript = traceScript;

        // Layout
        configLayout();
        add(chart, BorderLayout.CENTER);

        // Periodic Updates
        timedExec = createThreadPool();
        updateTask = new Runnable() {

            @Override
            public void run() {
                if(!running)
                    update();

            }

        };
        scheduleUpdate = timedExec.scheduleAtFixedRate(updateTask, 0, periodicity, TimeUnit.MILLISECONDS);

        editSettings = new RealTimePlotScript(this);

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
                editSettings.editSettings(selectedSys);
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
        if (!this.type.equals(t)) {
            resetSeries();
            this.type = t;
            this.plotType = type.name();
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
        drawLineXY = false;
    }

    /**
     * Changes the source of the current chart to a @XYSeriesCollection
     * 
     * @param xys Data set to be used as source for the chart
     */
    public void changeChartSrc(XYSeriesCollection xys) {
        JFreeChart xyChart = ChartFactory.createScatterPlot(null, null, null, xys, PlotOrientation.VERTICAL, true, true,
                false);
        //connect or not plot points 
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) xyChart.getXYPlot().getRenderer();
        renderer.setDefaultLinesVisible(drawLineXY);
        xyChart.getXYPlot().setRenderer(renderer);
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

    public boolean update() {
        if (!isShowing())
            return true;
        updateComboBox();
        if (ImcSystemsHolder.lookupActiveSystemVehicles().length == 0)
            return true;
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
        
        runScript(traceScript);
        previousScript = traceScript;
        return true;
    }

    /**
     * Creates Scheduled Executor for updates. Original implementation from @LogsDownloaderWorkerUtil
     * 
     * @return
     */
    private ScheduledThreadPoolExecutor createThreadPool() {
        ScheduledThreadPoolExecutor ret = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1,
                new ThreadFactory() {
                    private ThreadGroup group;
                    private long count = 0;
                    {
                        SecurityManager s = System.getSecurityManager();
                        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
                    }

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(group, r);
                        t.setName(RealTimePlotGroovy.this.getClass().getSimpleName() + "::"
                                + Integer.toHexString(this.hashCode()) + "::" + count++);
                        t.setDaemon(true);
                        return t;
                    }
                });

        return ret;
    }

    @Override
    public void propertiesChanged() {
        updating = true;
        if (!traceScript.equals(previousScript) || numPoints != numPointsBefore) {
            resetSeries();
            numPointsBefore = numPoints;
        }
        if (periodicity != previousPeriod) {
            scheduleUpdate.cancel(false);
            timedExec.purge();
            scheduleUpdate = timedExec.scheduleAtFixedRate(updateTask, 0, periodicity, TimeUnit.MILLISECONDS);
            previousPeriod = periodicity;
        }
        updating = false;
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
        if (selectedSys.equalsIgnoreCase("ALL")) {
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
     * Runs the Groovy script after checking the script validity.
     * 
     * @param script Text script
     */
    public void runScript(String script) {
        running = true;
        if (ImcSystemsHolder.lookupActiveSystemVehicles().length > 0) {
            shell = new GroovyShell(this.getClass().getClassLoader(), cnfg);
            try {
                Script parsed = checkScript(script);
                parsed.run();
                releaseThis();
            }
            catch (Exception e) {
                traceScript = previousScript;
                if (editSettings.isShowing())
                    throw e;
                    //GuiUtils.errorMessage(editSettings, "Error Parsing Script1", e.getLocalizedMessage());
                else {
                    GuiUtils.errorMessage(this, "Error Parsing Script", e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }
        running = false;
    }

    private void releaseThis() {
        if (shell != null) {
            if (shell.getContext().hasVariable("plot_")) {
                shell.setVariable("plot_", null);
                shell.getContext().getVariables().clear();
            }
            shell.getClassLoader().clearCache();
        }
    }

    /**
     * Verifies the script validity by parsing it.
     * 
     * @param toBtested - script to be tested
     * @return parsedScript -> Throws Error
     */
    public Script checkScript(String toBtested) throws CompilationFailedException {
        shell = new GroovyShell(this.getClass().getClassLoader(), cnfg);
        shell.setVariable("plot_", RealTimePlotGroovy.this);
        String defplot = "configPlot plot_";
        shell.evaluate(defplot);
        Script result = shell.parse(toBtested);
        return result;
    }

    @Override
    public void initSubPanel() {
        previousScript = traceScript;
        propertiesChanged();
    }

    @Override
    public void cleanSubPanel() {
        if (!timedExec.isTerminated() || !timedExec.isShutdown()) {
            timedExec.purge();
            timedExec.shutdown();
        }
        if (!scheduleUpdate.isCancelled())
            scheduleUpdate.cancel(true);
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
    
    public void drawLineForXY(boolean bool) {
        if (bool != drawLineXY && type.equals(PlotType.GENERICXY)) {
            drawLineXY = bool;
            changeChart();
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
