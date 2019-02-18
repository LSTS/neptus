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

import pt.lsts.imc.lsf.LsfIndex
import pt.lsts.neptus.plugins.rtplot.PlotScript
import pt.lsts.neptus.plugins.rtplot.RealTimePlotGroovy

import org.jfree.data.time.Millisecond
import org.jfree.data.time.TimeSeries
import org.jfree.data.time.TimeSeriesDataItem
import org.jfree.data.xy.XYDataItem
import org.jfree.data.xy.XYSeries

/**
 * @author keila
 *
 */
class ScriptedPlotGroovy  {
    
    static ScriptedPlot plot = null
    
    
    static void configPlot(ScriptedPlot p) {
        plot = p
    }
    
    static def value = { msgDotField ->
    	if(plot!= null)
    		return plot.getDataFromExpr(msgDotField);
    }

	static void addTimeSeries(List<TimeSeries> lts) {
	lts.each {
		plot.addTimeSeries(it)
		}
	}
    
    static public List<TimeSeries> apply(List<TimeSeries> lts, Object function) {
    	lts.each {
	    	for(int i = 0;i<it.getItemCount();i++) {
	    		def value = it.getDataItem(i).getValue
	        	it.getDataItem(i).setValue(function.call(value))
	        }
    	}
    	lts
    }
    
    static void title(String t) {
    	plot.defineTitle(t);
    }

}
