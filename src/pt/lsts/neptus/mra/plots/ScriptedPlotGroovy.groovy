/*
 * Copyright (c) 2004-2019 Universidade do Porto - Faculdade de Engenharia
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
 * Author: keila
 * Feb 8, 2019
 */
package pt.lsts.neptus.mra.plots

import org.jfree.data.time.Millisecond
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeSeriesDataItem
import org.jfree.data.time.TimeSeriesCollection
import org.jfree.data.xy.XYDataItem
import org.jfree.data.xy.XYSeries

/**
 * @author keila
 *
 */
class ScriptedPlotGroovy  {
    
    static ScriptedPlot scriptedPlot = null
    
    static void configPlot(ScriptedPlot p) {
        scriptedPlot = p
    }

	static void plot(LinkedHashMap<String,String> queries) {
        queries.each {
            scriptedPlot.addTimeSeries(it.key, it.value)
        }
	}
    
    static void plot(String... queries) {
       queries.each { 
           scriptedPlot.addTimeSeries(it, it)
       }
    }
       
    static void plot(TimeSeriesCollection tsc) {
        tsc.getSeries().each { ts ->
            scriptedPlot.addTimeSeries(ts)
        }
    }
 
    static void addQuery(String id,String query) {
        scriptedPlot.addQuery(id,query)
    }
    
    static void addQuery(LinkedHashMap<String,String> queries) {
        queries.each {
            scriptedPlot.addQuery(it.key,it.value)
        }
    }
    
    static public TimeSeriesCollection apply(String id=null,String queryID, Object function) {
        TimeSeriesCollection tsc = scriptedPlot.getTimeSeriesFor(queryID)
        TimeSeriesCollection result = new TimeSeriesCollection()
                tsc.getSeries().each { ts ->
                    String name = id ? ts.getKey().toString() : id
                    TimeSeries s = new TimeSeries(name)
                    for(int i = 0;i<ts.getItemCount();i++) {
                        def value = ts.getDataItem(i)
                        def val = function.call(value.getValue())
                        TimeSeriesDataItem item = new TimeSeriesDataItem(value.getPeriod(),val)
                        s.add(item)
                    }
                    result.addSeries(s)
                }
        result
    }
    
    static public TimeSeriesCollection apply(String id,String queryID1,String queryID2, Object function) {
        TimeSeriesCollection tsc1 = scriptedPlot.getTimeSeriesFor(queryID1)
        TimeSeriesCollection tsc2 = scriptedPlot.getTimeSeriesFor(queryID2)
        TimeSeriesCollection result = new TimeSeriesCollection()
        int min_tsc = Math.min(tsc1.getSeriesCount(), tsc2.seriesCount)
        TimeSeries ts1, ts2,ts
        for(int j=0;j<min_tsc;j++) {
            String key = tsc1.getSeriesKey(j)
            ts1 = tsc1.getSeries(key)
            key = tsc2.getSeriesKey(j)
            ts2 = tsc2.getSeries(key)
            if(ts1.getKey().toString().split("\\.")[0].equals(ts2.getKey().toString().split("\\.")[0])) { //Same source vehicle lauv-noptilus-1.<Query_ID>
                def newName = ts1.getKey().toString().split("\\.")[0]+ "."+id
                ts = new TimeSeries(newName)
                int min = Math.min(ts1.getItemCount(), ts2.getItemCount())
                for (int i=0; i<min;i++) {
                    def val1 = ts1.getDataItem(i)
                    def val2 = ts2.getDataItem(i)
                    def val  = function.call(val1.getValue(),val2.getValue())
                    TimeSeriesDataItem item = new TimeSeriesDataItem(val1.getPeriod(), val)
                    ts.add(item)
                }
                result.addSeries(ts)
            }
        }
        result
    }
    
    static public TimeSeriesDataItem getTimeSeriesMaxItem(String id) {
        TimeSeriesCollection tsc = scriptedPlot.getTimeSeriesFor(id)
        double max = Double.MIN_VALUE
        TimeSeriesDataItem result
        tsc.getSeries().each {
            for (int i=0;i<it.getItemCount();i++) {
                TimeSeriesDataItem t = it.getDataItem(i)
                if(t.getValue() > max) {
                    max    = t.getValue()
                    result = new TimeSeriesDataItem(t.getPeriod(),t.getValue().doubleValue())
                }
            }
        }
        result
    }
    
    static public TimeSeriesDataItem getTimeSeriesMinItem(TimeSeriesCollection tsc) {
        double min = Double.MAX_VALUE
        TimeSeriesDataItem result
         tsc.getSeries().each { 
            for (int i=0;i<it.getItemCount();i++) {
                TimeSeriesDataItem t = it.getDataItem(i)
                if(t.getValue() < min) {
                    min    = t.getValue()
                    result = new TimeSeriesDataItem(t.getPeriod(),t.getValue().doubleValue())
                }
            }
        }
        result
    }
        
    static public LinkedHashMap<String,Double> getAGV (String id) {
        TimeSeriesCollection tsc = scriptedPlot.getTimeSeriesFor(id)
        tsc.getSeries().collectEntries {[(it.getKey()): getAGV(it) ]}
    }
    
    static public double getAGV (TimeSeries ts) {
        getAGVItem(ts).getValue().doubleValue()
    }
    
    static public TimeSeriesDataItem getAGVItem (TimeSeries ts) {
        TimeSeriesDataItem item
        double avg = 0.0
        int n = ts.getItemCount()
        for(int i=0;i<n;i++) {
            avg+= ts.getDataItem(i)
        }
        def value = avg/(double)n
        def init_time = ts.getDataItem(0)
        def end_time = ts.getDataItem(0)
        def avg_time = (end_time - init_time)/2;
        item = new TimeSeriesDataItem(avg_time, value)
        item
    }
    
    static public void markFromItem(String id,TimeSeriesDataItem item) {
        if(item != null)
            mark id,item.getPeriod().getFirstMillisecond()/1000
    }
    static public sum = { double1, double2 -> double1+double2}
    
    static public diff = { double1, double2 -> double1-double2}
    
    static public mult = { double1, double2 -> double1*double2}
    
    static public div = { double1, double2 -> 
        if(double2 != 0.0)
            double1/double2
        else
            Double.NaN             
            }
    
    static public List<TimeSeriesDataItem> getAGVItems (TimeSeriesCollection tsc) {
        List<TimeSeriesDataItem> result = new ArrayList<>()
        tsc.getSeries().each { ts->
             TimeSeriesDataItem item = getAVGItem((TimeSeries)ts)
             result.add item
        }
        result
    }
    
    static public void mark(String label,double time) {
        scriptedPlot.mark(time,label)
    }

    static void title(String t) {
    	scriptedPlot.title(t);
    }

}
