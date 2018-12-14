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
import java.util.Map;

import pt.lsts.imc.*;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;


import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeSeriesDataItem



class PlotScript {
    //EstimatedState related closures
    static systems = []     //systems variable must be declared and updated on the script evaluation
    static RealTimePlotGroovy plot = null
    static def state = {field -> systems.collectEntries{ [(it): ImcMsgManager.getManager().getState(it).get("EstimatedState."+field)]}}
    //static final def rollscript  = state("phi").apply()//{state.each {k,v -> [(k):v.phi* 180/Math.PI]}}
    static final def pitchscript = {state.each {k,v -> [(k):v.theta* 180/Math.PI]}}
    static final def yawscript   = {state.each {k,v -> [(k):v.psi* 180/Math.PI]}}
    //ImcMsgManager.getManager().getState(it).get(msg).get(field, Number)
    //static def msgs (String msgDotField) { systems.collectEntries{ [(it): ImcMsgManager.getManager().getState(it).expr(msgDotField)]}} //TODO class Number necessary?
    static def msgs = { msgDotField -> systems.collectEntries{ [(it+"."+msgDotField): ImcMsgManager.getManager().getState(it).expr(msgDotField)]}}
    JFreeChart timeSeriesChart;

    static TimeSeries x() {}

    static TimeSeries y() {}

    //    static def apply (LinkedHashMap map,Closure function) {
    //        map.each {
    //            [(it.key): function.call(it.value)]
    //        }
    //    }
    //LinkedHashMap.metaClass = {}

    static def addSerie(LinkedHashMap map) {
        if(systems.size()>0) {
            map.each {
                TimeSeriesDataItem item = new TimeSeriesDataItem(new Millisecond(new Date(System.currentTimeMillis())),new Double( it.value))
                TimeSeries t = new TimeSeries(it.key)
                t.add(item)
                plot.addSerie(it.key,t)
            }
        }
    }
    //ChartFactory.createTimeSeriesChart(title, timeAxisLabel, valueAxisLabel, dataset)
    //    static PlotScript latLongPlot() {
    //        msgs("lat").call("EstimatedState")
    //        msgs("lon").call("EstimatedState")
    //    }
    //
    //    static JFreeChart xyPlot(String id,TimeSeries ts) {
    //      //assert systems.size() == 1 -> lat lon plot for one selected system only
    //    }
    //
    //    static JFreeChart timePlot(List<TimeSeries> tsc,int numPoints) {}


}
