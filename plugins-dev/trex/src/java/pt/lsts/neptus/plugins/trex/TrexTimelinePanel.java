/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Apr 23, 2013
 */
package pt.lsts.neptus.plugins.trex;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Vector;

import javax.swing.border.EmptyBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriod;

import com.google.common.eventbus.Subscribe;
import com.jogamp.newt.event.KeyEvent;

import pt.lsts.imc.TrexToken;
import pt.lsts.imc.VehicleState;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "TREX Timeline", icon = "pt/lsts/neptus/plugins/trex/trex.png")
@Popup(accelerator = KeyEvent.VK_F6, pos = POSITION.BOTTOM, height = 300, width = 800, icon = "pt/lsts/neptus/plugins/trex/trex.png")
public class TrexTimelinePanel extends ConsolePanel {

    private static final long serialVersionUID = 1L;
    protected JFreeChart chart;
    protected ChartPanel panel;
    protected TaskSeriesCollection tsc = new TaskSeriesCollection();
    protected LinkedHashMap<String, TaskSeries> series = new LinkedHashMap<>();
    protected LinkedHashMap<String, String> statePerTimeline = new LinkedHashMap<>();


    public TrexTimelinePanel(ConsoleLayout c) {
        super(c);
        setBorder(new EmptyBorder(0, 0, 0, 0));
    }

    @Override
    public void initSubPanel() {
        chart = ChartFactory.createGanttChart("", "", "", tsc, true, true, false);
        panel = new ChartPanel(chart);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void cleanSubPanel() {

    }
    
    public JFreeChart getChart(String title, String xlabel, String ylabel) {
        if (chart != null)
            chart = ChartFactory.createGanttChart(title, xlabel, ylabel, tsc, true, true, false);
        return chart;
    }
    
    public final Collection<String> getSeriesNames() {
        LinkedHashSet<String> series = new LinkedHashSet<>();
        series.addAll(this.series.keySet());
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
        
        if(!series.containsKey(trace))
            addTrace(trace);

        if(statePerTimeline.containsKey(trace)) {
            if (statePerTimeline.get(trace).equals(state)) {
                
                Task t = series.get(trace).get(state);
                if (t.getSubtaskCount() > 0) {
                    setEndTime((Task) t.getSubtask(t.getSubtaskCount()-1), time);
                }
                setEndTime(t, time);
                if(panel.isVisible()) {
                    chart.getCategoryPlot().configureRangeAxes();
                    panel.repaint();
                }
                return;
            }                
            else
                endActivity(time, trace);
        }
        
        statePerTimeline.put(trace, state);
        
        Task t = series.get(trace).get(state);
        if (t == null){
            long start = (long)(time * 1000);
            long end = (long)(time * 1000);
            t = new Task(state, new Date(start), new Date(end));
            series.get(trace).add(t);
        }
        t.addSubtask(new Task(state+time, new Date((long)(time*1000)), new Date((long)(time*1000))));
        
        if(panel.isVisible()) {
            chart.getCategoryPlot().configureRangeAxes();
            panel.repaint();
        }
    }
    
    private Task setEndTime(Task t, double time) {
        TimePeriod tp = t.getDuration();
        t.setDuration(new SimpleTimePeriod(tp.getStart(), new Date((long)(time*1000))));
        return t;
    }
    
    public void endActivity(double time, String trace){
        if (!series.containsKey(trace))
            return;
        Task t = series.get(trace).get(statePerTimeline.get(trace));
        
        if (t.getSubtaskCount() > 0) {
            setEndTime((Task) t.getSubtask(t.getSubtaskCount()-1), time);
        }
        setEndTime(t, time);
        statePerTimeline.put(trace, null);
    } 
    
    @Subscribe
    public void on(TrexToken token) {
        startActivity(token.getTimestamp(), token.getTimeline(), token.getTimeline()+"."+token.getPredicate());
    }    
    
    @Subscribe
    public void on(VehicleState state) {
       // startActivity(state.getTimestamp(), "Vehicle State", state.getOpMode().toString());
    }
}
