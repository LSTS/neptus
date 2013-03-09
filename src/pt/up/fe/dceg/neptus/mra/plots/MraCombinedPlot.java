/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Nov 13, 2012
 */
package pt.up.fe.dceg.neptus.mra.plots;

import java.util.LinkedHashMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.time.TimeSeriesCollection;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.MRAPanel;

/**
 * @author zp
 *
 */
public abstract class MraCombinedPlot extends MraTimeSeriesPlot {

    CombinedDomainXYPlot combinedPlot;
    
    public MraCombinedPlot(MRAPanel panel) {
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
            combinedPlot.add(ChartFactory.createTimeSeriesChart(plotName, I18n.text("Time Of Day"), plotName,
                    timeSeriesCollections.get(plotName), true, true, false).getXYPlot());
        }
        
        // Do this here to make sure we have a built chart.. //FIXME FIXME FIXME
        for(LogMarker marker : mraPanel.getMarkers()) {
           addLogMarker(marker);
        }
        
        return new JFreeChart(combinedPlot);
    }
    
    public void addLogMarker(LogMarker e) {
        if(combinedPlot != null) {
            for(Object plot : combinedPlot.getSubplots()) {
                ValueMarker vm = new ValueMarker(e.timestamp);
                vm.setLabel(e.label);
                ((org.jfree.chart.plot.XYPlot)plot).addDomainMarker(vm);
            }
        }
    }
}
