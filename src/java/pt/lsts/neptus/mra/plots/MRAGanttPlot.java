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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriod;

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
public abstract class MRAGanttPlot implements LLFChart, LogMarkerListener {

    protected Vector<String> forbiddenSeries = new Vector<>();
    protected LsfIndex index;
    protected double timestep = 0;
    protected TaskSeriesCollection tsc = new TaskSeriesCollection();
    protected LinkedHashMap<String, TaskSeries> series = new LinkedHashMap<>();
    protected LinkedHashMap<String, String> statePerTimeline = new LinkedHashMap<>();

    protected long localTimeOffset = 0;
    
    protected JFreeChart chart = null;
    protected MRAPanel mraPanel;

    /**
     * 
     */
    public MRAGanttPlot(MRAPanel panel) {
        this.mraPanel = panel;
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
        //return I18n.textf("%plotname plot", getName());
        return (getName() + " plot");
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
        series.put(trace, new TaskSeries(trace));
        tsc.add(series.get(trace));
    }

    public void startActivity(double time, String trace, String state){
        
        if (forbiddenSeries.contains(trace))
            return;

        if(!series.containsKey(trace))
            addTrace(trace);

        if(statePerTimeline.containsKey(trace)) {
            if (statePerTimeline.get(trace).equals(state))
                return;
            else
                endActivity(time, trace);
        }

        statePerTimeline.put(trace, state);

        Task t = series.get(trace).get(state);
        if (t == null){
            long start = (long)(mraPanel.getSource().getLsfIndex().getStartTime() * 1000);
            long end = (long)(mraPanel.getSource().getLsfIndex().getEndTime() * 1000);
            t = new Task(state, new Date(start - localTimeOffset), new Date(end - localTimeOffset));
            series.get(trace).add(t);
        }
        t.addSubtask(new Task(state+time, new Date((long)(time*1000 - localTimeOffset)), new Date((long)(time*1000 - localTimeOffset))));

    }

    private Task setEndTime(Task t, double time) {
        TimePeriod tp = t.getDuration();
        
        t.setDuration(new SimpleTimePeriod(tp.getStart(), new Date((long)(time*1000 - localTimeOffset))));
        return t;
    }

    public void endActivity(double time, String trace){
        if (forbiddenSeries.contains(trace) || !series.containsKey(trace))
            return;
        Task t = series.get(trace).get(statePerTimeline.get(trace));

        if (t.getSubtaskCount() > 0)
            t = (Task) t.getSubtask(t.getSubtaskCount()-1);
        setEndTime(t, time);
        statePerTimeline.put(trace, null);
    }        

    @Override
    public JComponent getComponent(IMraLogGroup source, double timestep) {
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
        return ImageUtils.getIcon("pt/lsts/neptus/mra/plots/clock-frame.png");
    }

    @Override
    public Double getDefaultTimeStep() {
        return 0.1;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }

    public JFreeChart createChart() {
        return ChartFactory.createGanttChart(I18n.text(getTitle()), I18n.text(getVerticalAxisName()), I18n.text(getHorizontalAxisName()),
                tsc, true, true, false);
    }

    public String getVerticalAxisName() {
        return "Time of the day";
    }

    public String getHorizontalAxisName() {
        return "";
    }

    @Override
    public JFreeChart getChart(IMraLogGroup source, double timestep) {
        if (chart != null)
            return chart;
        this.timestep = timestep;
        this.index = source.getLsfIndex();
        tsc = new TaskSeriesCollection();
        series.clear();
        process(index);
        chart = createChart();

        // Do this here to make sure we have a built chart.. //FIXME FIXME FIXME
        for(LogMarker marker : mraPanel.getMarkers()) {
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
        //nothing
    }

    @Override
    public void onShow() {
        //nothing
    }


    @Override 
    public void addLogMarker(LogMarker e) {
        
        ValueMarker marker = new ValueMarker(e.getTimestamp() - localTimeOffset);
        marker.setLabel(e.getLabel());
        if (chart != null)
            chart.getCategoryPlot().addRangeMarker(marker);
    }

    @Override
    public void removeLogMarker(LogMarker e) {
        if(chart != null) {
            chart.getCategoryPlot().clearRangeMarkers();

            for (LogMarker m : mraPanel.getMarkers())
                addLogMarker(m);
        }
    }

    @Override
    public void goToMarker(LogMarker marker) {

    }

}
