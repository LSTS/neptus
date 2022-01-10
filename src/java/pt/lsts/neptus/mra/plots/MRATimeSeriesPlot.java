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
 * Nov 13, 2012
 */
package pt.lsts.neptus.mra.plots;

import java.awt.Color;
import java.awt.Component;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.ImageIcon;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.LogStatisticsItem;
import pt.lsts.neptus.mra.MRAChartPanel;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.llf.chart.LLFChart;

/**
 * @author zp
 *
 */
public abstract class MRATimeSeriesPlot implements LLFChart, LogMarkerListener {

    protected Vector<String> forbiddenSeries = new Vector<>();
    protected LsfIndex index;
    protected double timestep = 0;
    protected TimeSeriesCollection tsc = new TimeSeriesCollection();
    protected LinkedHashMap<String, TimeSeries> series = new LinkedHashMap<>();
    protected JFreeChart chart;

    protected long localTimeOffset = 0;
    
    protected MRAPanel mraPanel;

    protected static Color[] seriesColors = new Color[] { Color.red.darker(), Color.blue.darker(),
            Color.green.darker(), Color.orange, Color.cyan.darker(), Color.gray.darker(), Color.magenta.darker(),
            Color.blue.brighter().brighter(), Color.red.brighter().brighter(), Color.green.brighter().brighter(),
            Color.black, Color.pink, Color.yellow.darker(), Color.cyan, Color.magenta };

    public MRATimeSeriesPlot(MRAPanel panel) {
        this.mraPanel = panel;
        if(panel != null)
            this.localTimeOffset = LogLocalTimeOffset.getLocalTimeOffset((long)
                mraPanel.getSource().getLsfIndex().getStartTime()*1000);
    }

    public Vector<String> getForbiddenSeries() {
        return forbiddenSeries;
    }

    @Override
    public String getName() {
        return PluginUtils.getPluginName(getClass());
    }

    public String getTitle() {
        return I18n.textf("%plotname plot", getName());
    }

    public final Collection<String> getSeriesNames() {
        LinkedHashSet<String> series = new LinkedHashSet<>();
        series.addAll(this.series.keySet());
        series.addAll(forbiddenSeries);
        Vector<String> col = new Vector<>();
        col.addAll(series);
        Collections.sort(col);
        return col;
    }

    public void addTrace(String trace) {
        series.put(trace, new TimeSeries(trace));
        tsc.addSeries(series.get(trace));
    }

    public void addValue(long timeMillis, String trace, double value) {

        if (forbiddenSeries.contains(trace))
            return;

        if (!series.containsKey(trace)) {
            addTrace(trace);
        }
        series.get(trace).addOrUpdate(new Millisecond(new Date(timeMillis), TimeZone.getTimeZone("UTC"), Locale.getDefault()), value);
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        MRAChartPanel fcp = new MRAChartPanel(this, source, mraPanel);
        return fcp;
    }

    @Override
    public final boolean canBeApplied(IMraLogGroup source) {
        return canBeApplied(source.getLsfIndex());
    }

    public abstract boolean canBeApplied(LsfIndex index);

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("images/menus/graph2.png");
    }

    @Override
    public Double getDefaultTimeStep() {
        return 0.1;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return true;
    }

    public JFreeChart createChart() {
        return ChartFactory.createTimeSeriesChart(I18n.text(getTitle()), I18n.text(getVerticalAxisName()),
                I18n.text(getHorizontalAxisName()), tsc, true, true, false);
    }

    public String getHorizontalAxisName() {
        return "";
    }

    public String getVerticalAxisName() {
        return I18n.text("Time of day");
    }

    @Override
    public JFreeChart getChart(IMraLogGroup source, double timestep) {
        this.timestep = timestep;
        this.index = source.getLsfIndex();
        tsc = new TimeSeriesCollection();
        series.clear();
        process(index);
        chart = createChart();
        XYItemRenderer r = chart.getXYPlot().getRenderer();
        if (r != null) {
            for (int i = 0; i < tsc.getSeriesCount(); i++) {
                r.setSeriesPaint(i, seriesColors[i % seriesColors.length]);
            }
        }
        for (LogMarker marker : mraPanel.getMarkers()) {
            addLogMarker(marker);
        }
        return chart;
    }

    public abstract void process(LsfIndex source);

    @Override
    public Vector<LogStatisticsItem> getStatistics() {
        return null;
    }

    @Override
    public Type getType() {
        return Type.CHART;
    }

    @Override
    public void onCleanup() {
        mraPanel = null;
    }

    @Override
    public void onHide() {
        // nothing
    }

    @Override
    public void onShow() {
        // nothing
    }

    @Override
    public void addLogMarker(LogMarker e) {
        ValueMarker marker = new ValueMarker(e.getTimestamp() - localTimeOffset);
        marker.setLabel(e.getLabel());
        if (chart != null)
            chart.getXYPlot().addDomainMarker(marker);
    }

    @Override
    public void removeLogMarker(LogMarker e) {
        if (chart != null) {
            chart.getXYPlot().clearDomainMarkers();

            for (LogMarker m : mraPanel.getMarkers())
                addLogMarker(m);
        }
    }

    @Override
    public void goToMarker(LogMarker marker) {
    }
}
