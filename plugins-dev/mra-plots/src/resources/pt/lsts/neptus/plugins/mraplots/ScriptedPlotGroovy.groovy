/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.plugins.mraplots

import org.jfree.data.time.Millisecond
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeSeriesDataItem
import org.jfree.data.time.TimeSeriesCollection
import org.jfree.data.time.RegularTimePeriod
import pt.lsts.neptus.mra.plots.ScriptedPlot

import java.text.SimpleDateFormat

/**
 * @author keila
 *
 */
class ScriptedPlotGroovy  {
    
    static ScriptedPlot scriptedPlot = null
    
    static void configPlot(ScriptedPlot p) {
        scriptedPlot = p
    }

    static boolean processed(){
        scriptedPlot.isProcessed()
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
        tsc.getSeries().each { TimeSeries ts ->
            scriptedPlot.addTimeSeries(ts)
        }
    }
 
    static void addQuery(String... query) {
        query.each {
            scriptedPlot.addQuery(it,it)
        }
    }
    
    static void addQuery(LinkedHashMap<String,String> queries) {
        queries.each {
            scriptedPlot.addQuery(it.key,it.value)
        }
    }
    
    static public TimeSeriesCollection apply(String queryID, Object function) {
        TimeSeriesCollection tsc = scriptedPlot.getTimeSeriesFor(queryID)
        TimeSeriesCollection result = new TimeSeriesCollection()
                tsc.getSeries().each { TimeSeries ts ->
                    String name = ts.getKey().toString()
                    TimeSeries s = new TimeSeries(name)
                    for(TimeSeriesDataItem item: s.getItems()) {
                        def value = function.call(item.getValue())
                        s.addOrUpdate(item.getPeriod(),value)
                    }
                    result.addSeries(s)
                }
        result
    }
    
    static public TimeSeriesCollection apply(String name,String queryID, Closure<Number> function) {
        TimeSeriesCollection result = new TimeSeriesCollection()
        if(!scriptedPlot.isProcessed())
            return result
        TimeSeriesCollection tsc = scriptedPlot.getTimeSeriesFor(queryID)
                tsc.getSeries().each { TimeSeries ts ->
                    def newName = ts.getKey().toString().split("\\.")[0]+ "."+name
                    TimeSeries s = new TimeSeries(newName)
                    for(TimeSeriesDataItem item: ts.getItems()) {
                        def value = item.getValue()
                        def new_value = function.call(value)
                        s.addOrUpdate(item.getPeriod(),new_value)
                    }
                    result.addSeries(s)
                }
        result
    }
    
    static public TimeSeriesCollection apply(String id,String queryID1,String queryID2, Closure<Number> function) {
        TimeSeriesCollection result = new TimeSeriesCollection()
        if(!scriptedPlot.isProcessed())
            return result
        TimeSeriesCollection tsc1 = scriptedPlot.getTimeSeriesFor(queryID1)
        TimeSeriesCollection tsc2 = scriptedPlot.getTimeSeriesFor(queryID2)
        int min_tsc = Math.min(tsc1.getSeriesCount(), tsc2.getSeriesCount())
        TimeSeries ts1, ts2,ts
        for(int j=0;j<min_tsc;j++) {
            String key = tsc1.getSeriesKey(j)
            ts1 = tsc1.getSeries(key)
            key = tsc2.getSeriesKey(j)
            ts2 = tsc2.getSeries(key)
            if(ts1.getKey().toString().split("\\.")[0].equals(ts2.getKey().toString().split("\\.")[0])) { //Same source vehicle lauv-noptilus-1.<Query_ID>
                def newName = ts1.getKey().toString().split("\\.")[0]+ "."+id
                ts = new TimeSeries(newName)
                for (TimeSeriesDataItem val1: ts1.getItems()) {
                    RegularTimePeriod p = val1.getPeriod()
                    int index = ts2.getIndex(p)
                    if(index < 0)
                        continue;
                    TimeSeriesDataItem val2 = ts2.getDataItem(index)
                    double v1 = val1.getValue()
                    double v2 = val2.getValue()
                    double val  = function.call(v1,v2)
                    ts.addOrUpdate(p, val)
                }
                result.addSeries(ts)
            }
        }
        result
    }
    
    static public TimeSeriesDataItem getTimeSeriesMaxItem(String id) {
    	if(!scriptedPlot.isProcessed())
    		return null
        TimeSeriesCollection tsc = scriptedPlot.getTimeSeriesFor(id)
        double max = - Double.MAX_VALUE
        TimeSeriesDataItem result
        tsc.getSeries().each {
            TimeSeries ts ->
            for (int i=0;i<ts.getItemCount();i++) {
                TimeSeriesDataItem t = ts.getDataItem(i)
                if(t.getValue() > max) {
                    max    = t.getValue()
                    result = new TimeSeriesDataItem(t.getPeriod(),t.getValue().doubleValue())
                    
                }
            }
        }
        result
    }
    
    static public TimeSeriesDataItem getTimeSeriesMinItem(String id) {
    	if(!scriptedPlot.isProcessed())
    		return null
        TimeSeriesCollection tsc = scriptedPlot.getTimeSeriesFor(id)
        double min = Double.MAX_VALUE
        TimeSeriesDataItem result
         tsc.getSeries().each { 
             TimeSeries ts ->
            for (int i=0;i<ts.getItemCount();i++) {
                TimeSeriesDataItem t = ts.getDataItem(i)
                if(min > t.getValue()) {
                    min    = t.getValue()
                    result = new TimeSeriesDataItem(t.getPeriod(),t.getValue().doubleValue())
                    
                }
            }
        }
        result
    }
    
    static public double getTimeSeriesMinValue(String id) {
        def item = getTimeSeriesMinItem(id)
        if(item!=null)
            item.getValue().doubleValue()
        else
            Double.NaN
    }
    
    static public double getTimeSeriesMaxValue(String id) {
        def item = getTimeSeriesMaxItem(id)
        if(item!=null)
            item.getValue().doubleValue()
        else
            Double.NaN
    }
    
    static public long getTimeSeriesMinTime(String id) {
        def item = getTimeSeriesMinItem(id)
        if(item!=null) {
            item.getPeriod().getFirstMillisecond()
        }
        else
            Double.NaN
    }
    
    static public long getTimeSeriesMaxTime(String id) {
        def item = getTimeSeriesMaxItem(id)
        if(item!=null) {
            
            item.getPeriod().getFirstMillisecond()
        }
        else
            Double.NaN
    }
    
     static public double getAGVFor(String id) {
        double result
        String system,key=""
        String [] split = id.split("\\.")
        if( split.length > 1) {
            system = split[0]
            key = id.substring(system.size()+1)//skip the dot "."
            TimeSeriesCollection tsc = scriptedPlot.getTimeSeriesFor(key)
            if(tsc!=null && tsc.seriesCount > 0) {
                Iterator iter = tsc.getSeries().iterator()
                while(iter.hasNext()) {
                    double total = 0.0
                    TimeSeries ts = iter.next()
                    if(!ts.getKey().toString().equalsIgnoreCase(id))
                        continue
                    int n = ts.getItemCount()
                    for(int i=0;i<n;i++) {
                        double aux = new Double(ts.getDataItem(i).getValue()).doubleValue()
                        if(Double.isNaN(aux))
                            total = total + aux
                    }
                    result = total/(double)n
                }
            }
        }
        if (result==null)
            return Double.NaN
        return result
    }
    
    static public void plotRangeMarker(String id,double value) {
        if(scriptedPlot!= null || !Double.isNaN(value)) {
            scriptedPlot.addRangeMarker(id, value)
        }
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
  
    static public void plotDomainMarker(String label,long time) {
        if(scriptedPlot != null || time != Double.NaN)
            scriptedPlot.mark(time,label)
    }

    static void title(String t) {
    	scriptedPlot.title(t);
    }

    static TimeSeriesCollection crop(String id, long from, long to=0){
        if(!scriptedPlot.isProcessed() || scriptedPlot == null || from == 0)
            return null
        TimeSeries ts = scriptedPlot.getTimeSeriesFor(id).getSeries()[0];
        def newName = ts.getKey().toString()+".cropped"
        Millisecond start = new Millisecond(new Date(from))
        RegularTimePeriod end = to==0? ts.getTimePeriods().last() : new Millisecond(new Date(to))
        TimeSeriesCollection result = new TimeSeriesCollection()
        TimeSeries newTs = new TimeSeries(newName)
        newTs.addAndOrUpdate(ts.createCopy(start,end))
        result.addSeries(newTs)
        result

    }
    static TimeSeriesCollection crop(String id, String from, String to=null){
        SimpleDateFormat sdf=new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS z")
        try {
            crop(id, sdf.parse(from).getTime(), to ? null : sdf.parse(to).getTime())
        }
        catch (Exception e) {
            System.err.println("Please enter datetime in the following format: dd-MMM-yyyy HH:mm:ss z")
            throw e;
        }
    }

    static String show (Object o) {
        scriptedPlot.addTextToShow(o.toString());
    }

    static String logName(){
        scriptedPlot.getLogName()
    }

    static TimeSeries hideTimeSeries (String id){
        if(!scriptedPlot.isProcessed() || scriptedPlot == null)
            return null
        TimeSeriesCollection result = scriptedPlot.getTimeSeriesFor(id)
        TimeSeries ts = result.getSeries()[0];
        scriptedPlot.hideTimeSeries(id,ts)
        ts
    }


}
