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
 * Dec 12, 2018
 */
package pt.lsts.neptus.plugins.rtplot

import java.awt.BorderLayout
import java.awt.geom.Point2D
import java.util.Map;

import pt.lsts.imc.*;
import pt.lsts.imc.state.ImcSystemState
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

    static public sum = {double1, double2 -> double1+double2}

    static public diff = { double1, double2 -> double1-double2}

    static public prod = { double1, double2 -> double1*double2}

    static public div = { double1, double2 ->
        if(double2 != 0.0)
            double1/double2
        else
            Double.NaN
    }

    static String newName(String dottedName, String serieName) {
        dottedName.split(/(\.)/)[0] + "." +serieName
    }

    static LinkedHashMap apply (LinkedHashMap map, Object function)  {
        def result = [:]
        map.each {
            if(it.value!=null)
                result.put it.key,function.call(it.value)
        }
        result
    }

    static LinkedHashMap apply (LinkedHashMap map1, LinkedHashMap map2, Object function, String name="custom_series")  {
        def result = [:]
        def lookup
        map1.keySet().each { key ->
            def sys = key.split(/(\.)/)[0]
            def id = sys+"."+name
            if((lookup = map2.keySet().find{it.startsWith(sys)}) != null) {
                double val1  = map1.get key 
                double val2  = map2.get lookup
                if(val1!=null && val2!=null) {
                    double value = function.call(val1,val2)
                    result.put id, value
                }
            }
        }
        result
    }

    static def value = { msgDotField ->
        if(realTimePlot!=null && realTimePlot.getSystems() != null)
            realTimePlot.getSystems().collectEntries{
                [(it+"."+msgDotField): ImcMsgManager.getManager().getState(it).expr(msgDotField)]
            }
        else  [:]
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

    static def setDrawLineForXY(boolean toDraw) {
        realTimePlot.drawLineForXY(toDraw);
    }

    static def xyseries(LinkedHashMap map1,LinkedHashMap map2,String name="serie") {
        synchronized(realTimePlot) {
            if(!realTimePlot.getType().equals(PlotType.GENERICXY)) {
                //plot.resetSeries()
                realTimePlot.setType(PlotType.GENERICXY)
            }

            def result = [:]
            def lookup
            realTimePlot.getSystems().eachWithIndex { sys, index ->
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
    }

    static def addTimeSeries(LinkedHashMap map,String serieName=null) {
        synchronized(realTimePlot) {
            if(!realTimePlot.getType().equals(PlotType.TIMESERIES)) {
                //plot.resetSeries()
                realTimePlot.setType(PlotType.TIMESERIES)
            }
            if(map.size()>0) {
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
    }

    static def addSeries(LinkedHashMap map,String serieName=null) {
        if(map.size()>0) {
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

    static def plotAbsoluteLatLong() {
        synchronized(realTimePlot) {
            realTimePlot.getSystems().each {
                EstimatedState state = ImcMsgManager.getManager().getState(it).get("EstimatedState")
                if(state != null) {
                    if(!realTimePlot.getType().equals(PlotType.GENERICXY)) {
                        realTimePlot.setType(PlotType.GENERICXY)
                    }
                    def resultmap = [:]
                    LocationType loc =  new LocationType(Math.toDegrees(state.getDouble("lat")),Math.toDegrees(state.getDouble("lon"))) //lat long
                    loc.translatePosition(state.getDouble("x"), state.getDouble("y"), state.getDouble("z"))
                    loc.convertToAbsoluteLatLonDepth()
                    def id = it+".position"
                    XYDataItem item = new XYDataItem(loc.getLatitudeDegs(),loc.getLongitudeDegs())
                    resultmap.put id, item
                    addSeries(resultmap)
                }
            }
        }
    }

    static def plotNED() {
        synchronized(realTimePlot) {
            realTimePlot.getSystems().each {
                EstimatedState state = ImcMsgManager.getManager().getState(it).get("EstimatedState")
                if(state != null) {
                    if(!realTimePlot.getType().equals(PlotType.GENERICXY)) {
                        realTimePlot.setType(PlotType.GENERICXY)
                    }
                    def resultmap = [:]
                    def id = it+".NED"
                    XYDataItem item = new XYDataItem(state.getDouble("x"),state.getDouble("y"))
                    resultmap.put id, item
                    addSeries(resultmap)
                }
            }
        }
    }


    static def plotNEDFrom(double lat,double lon,double h) {
        synchronized(realTimePlot) {
            realTimePlot.getSystems().each {
                EstimatedState state = ImcMsgManager.getManager().getState(it).get("EstimatedState")
                if(state != null) {
                    if(!realTimePlot.getType().equals(PlotType.GENERICXY)) {
                        realTimePlot.setType(PlotType.GENERICXY)
                    }
                    LocationType ref = new LocationType(lat,lon)
                    ref.setHeight(h)
                    def resultmap = [:]
                    LocationType loc =  new LocationType(Math.toDegrees(state.getDouble("lat")),Math.toDegrees(state.getDouble("lon"))) //lat long
                    loc.translatePosition(state.getDouble("x"), state.getDouble("y"), state.getDouble("z"))
                    def id = it+".relativeNED"
                    double[] offsets = loc.getOffsetFrom(ref)
                    XYDataItem item = new XYDataItem(offsets[0],offsets[1])
                    resultmap.put id, item
                    addSeries(resultmap)
                }
            }
        }
    }

    static def plotNEDFrom(LinkedHashMap lat0, LinkedHashMap long0, LinkedHashMap h0,LinkedHashMap lat,LinkedHashMap lon,LinkedHashMap h,LinkedHashMap x,LinkedHashMap y,LinkedHashMap z) {
        synchronized(realTimePlot) {
            if(!realTimePlot.getType().equals(PlotType.GENERICXY)) {
                //plot.resetSeries()
                realTimePlot.setType(PlotType.GENERICXY)
            }
            def name = "NED"
            def result = [:]
            def lookup1,lookup2,lookup3,lookup4,lookup5,lookup6,lookup7,lookup8
            lat0.keySet().each { key ->
                def sys = key.split(/(\.)/)[0]
                def id = sys+"."+name
                if((lookup1 = long0.keySet().find{it.startsWith(sys)}) != null && (lookup2 = h0.keySet().find{it.startsWith(sys)}) != null
                && (lookup3 = lat.keySet().find{it.startsWith(sys)}) != null && (lookup4 = lon.keySet().find{it.startsWith(sys)}) != null
                && (lookup5 = h.keySet().find{it.startsWith(sys)}) != null && (lookup6 = x.keySet().find{it.startsWith(sys)}) != null
                && (lookup7 = y.keySet().find{it.startsWith(sys)}) != null && (lookup8 = z.keySet().find{it.startsWith(sys)}) != null ) {
                    double fromLat  = lat0.get key
                    double fromLong = long0.get lookup1
                    double fromH    = h0.get lookup2
                    double toLat  = lat.get lookup3
                    double toLong = lon.get lookup4
                    double toH    = h.get lookup5
                    double xx = x.get lookup6
                    double yy = y.get lookup7
                    double zz = z.get lookup8
                    if(fromLat!=null && fromLong!=null && fromH !=null && toLat!=null && toLong!=null && toH!=null && xx!=null && yy!=null && zz!=null) {
                        LocationType loc0 =  new LocationType(fromLat,fromLong)
                        loc0.setHeight(fromH)
                        LocationType loc1 =  new LocationType(toLat,toLong)
                        loc1.setHeight(toH)
                        loc1.translatePosition(xx, yy, zz)
                        double[] offsets = loc0.getOffsetFrom(loc1)
                        XYDataItem item = new XYDataItem(offsets[0],offsets[1])
                        result.put id, item
                    }
                }
                addSeries result
            }
        }
    }

    static def plotNEDFrom(LinkedHashMap lat, LinkedHashMap lon, LinkedHashMap h) {
        //TODO xyz offset from this coordinates?
        def closure = {arg -> Math.toDegrees(arg)}
        def la = apply ( value ("EstimatedState.lat"), closure)
        def ln = apply ( value ("EstimatedState.lon"), closure)
        def he = apply ( value ("EstimatedState.height"), closure)

        return plotNEDFrom(lat,lon,h,la, ln, he,value("EstimatedState.x"),value("EstimatedState.y"),value("EstimatedState.z"))
    }

}
