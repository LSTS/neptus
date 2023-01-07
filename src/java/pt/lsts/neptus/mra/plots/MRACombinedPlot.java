/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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

import java.util.LinkedHashMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.time.TimeSeriesCollection;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;

/**
 * @author zp
 *
 */
public abstract class MRACombinedPlot extends MRATimeSeriesPlot {

    CombinedDomainXYPlot combinedPlot;
    LinkedHashMap<String, JFreeChart> charts = new LinkedHashMap<>(); 
    
    public MRACombinedPlot(MRAPanel panel) {
        super(panel);
    }

    @Override
    public JFreeChart createChart() {

        LinkedHashMap<String, TimeSeriesCollection> timeSeriesCollections = new LinkedHashMap<>();
        
        combinedPlot = new CombinedDomainXYPlot(new DateAxis(I18n.text("Time Of Day")));

        for (String seriesName : series.keySet()) {
            if (forbiddenSeries.contains(seriesName))
                continue;
            String plot = seriesName.split("\\.")[0];
            if (!timeSeriesCollections.containsKey(plot))
                timeSeriesCollections.put(plot, new TimeSeriesCollection());
            timeSeriesCollections.get(plot).addSeries(series.get(seriesName));
        }

        for (String plotName : timeSeriesCollections.keySet()) {
            charts.put(plotName, ChartFactory.createTimeSeriesChart(plotName, I18n.text("Time Of Day"), plotName,
                    timeSeriesCollections.get(plotName), true, true, false));
            
            combinedPlot.add(charts.get(plotName).getXYPlot());
        }

        // Do this here to make sure we have a built chart.. //FIXME FIXME FIXME
        for(LogMarker marker : mraPanel.getMarkers()) {
            addLogMarker(marker);
        }
        
        return new JFreeChart(combinedPlot);
    }
    
    public JFreeChart getChart(String plotName) {
        return charts.get(plotName);
    }

    @Override
    public void addLogMarker(LogMarker e) {
        if(combinedPlot != null) {
            for(Object plot : combinedPlot.getSubplots()) {
                ValueMarker vm = new ValueMarker(e.getTimestamp()- localTimeOffset);
                vm.setLabel(e.getLabel());
                ((org.jfree.chart.plot.XYPlot)plot).addDomainMarker(vm);
            }
        }
    }
}
