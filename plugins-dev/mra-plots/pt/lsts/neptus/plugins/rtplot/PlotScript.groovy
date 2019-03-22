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
 * Dec 12, 2018
 */
package pt.lsts.neptus.plugins.rtplot

import java.awt.BorderLayout
import java.awt.geom.Point2D
import java.util.Map;

import pt.lsts.imc.*;
import pt.lsts.neptus.NeptusLog
import pt.lsts.neptus.comm.manager.imc.ImcSystem
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder
import pt.lsts.neptus.mra.plots.TimedXYDataItem
import pt.lsts.neptus.plugins.rtplot.RealTimePlotGroovy.PlotType
import pt.lsts.neptus.types.coord.LocationType
import pt.lsts.neptus.util.GuiUtils

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeSeriesCollection
import org.jfree.data.time.TimeSeriesDataItem
import org.jfree.data.xy.XYDataItem
import org.jfree.data.xy.XYSeries



class PlotScript {
    
    static RealTimePlotGroovy realTimePlot = null
    
     static void configPlot(RealTimePlotGroovy p) {
        realTimePlot = p
    }
    
    static public final String newName(String dottedName, String serieName) {
        dottedName.split(/(\.)/)[0] + "." +serieName
    }
    static public LinkedHashMap apply (LinkedHashMap map, Object function)  {
        def result = [:]
        map.each {
            result.put it.key,function.call(it.value)
        }
        result
    }
    
    static def value = { msgDotField ->
        RealTimePlotGroovy.getSystems().collectEntries{ [(it+"."+msgDotField): ImcMsgManager.getManager().getState(it).expr(msgDotField)]}
    }
    
    static def state(String s){
        return this.value("EstimatedState."+s)
    }

    static def roll() {
        apply(state("phi"),{i -> i*180/Math.PI})
    }
    static def pitch() {
        apply(state("theta"),{i -> i*180/Math.PI})
    }
    static def yaw() {
        apply(state("psi"),{i -> i*180/Math.PI})
    }

    static def xyseries(LinkedHashMap map1,LinkedHashMap map2,String name="serie",boolean autosort=false) {
        if(!realTimePlot.getType().equals(PlotType.GENERICXY)) {
            //plot.resetSeries()
            realTimePlot.setType(PlotType.GENERICXY)
        }
        def result = [:]
        def lookup
        RealTimePlotGroovy.getSystems().eachWithIndex { sys, index ->
            def id = sys+"."+name
            if((lookup = map1.keySet().find{it.startsWith(sys)}) != null) {
                def v1 = map1.get lookup
                if((lookup = map2.keySet().find{it.startsWith(sys)}) != null) {
                    def v2 = map2.get lookup
                    if(v1 != null && v2 != null) {
                        XYDataItem item = new XYDataItem(v1,v2)
                        result.put id, item
                        addSeries result
                    }
                }
            }
        }
    }

    static def addTimeSeries(LinkedHashMap map,String serieName=null) {
        if(!realTimePlot.getType().equals(PlotType.TIMESERIES)) {
            //plot.resetSeries()
            realTimePlot.setType(PlotType.TIMESERIES)
        }
        if(RealTimePlotGroovy.getSystems().size()>0) {
            map.each {
                if(it.value != null) {
                    TimeSeriesDataItem item = new TimeSeriesDataItem(new Millisecond(new Date(System.currentTimeMillis())),new Double(it.value))
                    def name = serieName==null ? it.key : newName(it.key,serieName)
                    TimeSeries t =  new TimeSeries(name)
                    t.add(item)
                    realTimePlot.addTimeSerie(name,t)
                }
            }
        }
    }
    static def addSeries(LinkedHashMap map,String serieName=null) {
        if(RealTimePlotGroovy.getSystems().size()>0) {
            map.each {
                if(it.value != null) {
                    def name = serieName==null? it.value : newName(it.key,serieName)
                    XYSeries xy = new XYSeries(it.key,false)
                    xy.add(it.value)
                    realTimePlot.addSerie(it.key,xy)
                }
            }
        }
    }

    static def plotLatLong() {
        RealTimePlotGroovy.getSystems().each {
            EstimatedState state = ImcMsgManager.getManager().getState(it).get("EstimatedState")
            if(state != null) {
                LocationType ref = new LocationType(0,0)
                if(!realTimePlot.getType().equals(PlotType.GENERICXY)) {
                    realTimePlot.setType(PlotType.GENERICXY)
                }
                def resultmap = [:]
                LocationType loc =  new LocationType(state.getDouble("lat"),state.getDouble("lon")) //lat long
                loc.translatePosition(state.getDouble("x"), state.getDouble("y"), state.getDouble("z"))
                def id = it+".position"
                double[] offsets = loc.getOffsetFrom(ref)
                Point2D pt = new Point2D.Double(offsets[0],offsets[1])
                XYDataItem item = new XYDataItem(pt.getX(),pt.getY())
                resultmap.put id, item
                addSeries(resultmap)
            }
        }
    }

}