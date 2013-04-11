/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * Nov 13, 2012
 */
package pt.up.fe.dceg.neptus.mra.plots;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.LogStatisticsItem;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.MraChartPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.llf.chart.LLFChart;

/**
 * @author zp
 *
 */
public abstract class Mra2DPlot implements LLFChart, LogMarkerListener {   
    MRAPanel mraPanel;
    private XYSeries markerSeries;
    
    public Mra2DPlot(MRAPanel panel) {
        this.mraPanel = panel;
    }
    @Override
    public String getName() {
        return PluginUtils.getPluginName(getClass());
    }
    
    public String getTitle() {
        return I18n.textf("%plotname plot", getName());
    }
    
    private LsfIndex index;
    protected double timestep = 0;
    JFreeChart chart;
    
    protected LinkedHashMap<String, LinkedHashMap<Long, Point2D.Double>> series = new LinkedHashMap<>();
    protected LinkedHashMap<String, Long> lastAddedPoints = new LinkedHashMap<>();
    
    public void addValue(long timeMillis, double x, double y, String src, String variable) {
        String seriesName = src+"."+variable;
        
        if (!series.containsKey(seriesName)) {
           series.put(seriesName, new LinkedHashMap<Long, Point2D.Double>());
        }
//        else {
//            if (supportsVariableTimeSteps()) {
//                long lastTime = lastAddedPoints.get(seriesName);                        
//                if (timeMillis - lastTime < timestep*1000) {
//                   return;
//                }
//            }
//        }
        series.get(seriesName).put(timeMillis, new Point2D.Double(x,y));
        lastAddedPoints.put(seriesName, timeMillis);        
    }
    
    @Override
    public JComponent getComponent(IMraLogGroup source, double timestep) {
        return new MraChartPanel(this, source, mraPanel);
    }

    @Override
    public final boolean canBeApplied(IMraLogGroup source) {     
        return canBeApplied(source.getLsfIndex());
    }
    
    public abstract boolean canBeApplied(LsfIndex index);
   
    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("images/menus/graph.png");
    }

    @Override
    public Double getDefaultTimeStep() {
        return 0.1;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return true;
    }
    
    public String getXAxisName() {
        return "X";
    }
    
    public String getYAxisName() {
        return "Y";
    }
    
    public JFreeChart createChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();

        for (String k : series.keySet()) {
            XYSeries ser = new XYSeries(k);
            for (long time : series.get(k).keySet()) {
                TimedXYDataItem item = new TimedXYDataItem(series.get(k).get(time).getX(), series.get(k).get(time).getY(), time, "");
                ser.add(item);
            }
            dataset.addSeries(ser);
        }

        JFreeChart chart = ChartFactory.createScatterPlot(I18n.text(getTitle()), I18n.text(getXAxisName()),
                I18n.text(getYAxisName()), dataset, PlotOrientation.HORIZONTAL, true, true, false);

        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            ((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setSeriesShape(i, new Ellipse2D.Double(0, 0, 3,
                    3));
        }

        ((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setSeriesToolTipGenerator(0,
                new XYToolTipGenerator() {

                    @Override
                    public String generateToolTip(XYDataset arg0, int arg1, int arg2) {
                        LocationType loc = new LocationType(arg0.getX(arg1, arg2).doubleValue(), arg0.getY(arg1, arg2)
                                .doubleValue());
                        return loc.getLatitudeAsPrettyString() + " / " + loc.getLongitudeAsPrettyString();
                    }
                });
        
        // Clean markers series
        markerSeries = null;
        return chart;
    }

    @Override
    public final JFreeChart getChart(IMraLogGroup source, double timestep) {
        this.timestep = timestep;
        this.index = source.getLsfIndex();
        series.clear();
        process(index);
        chart = createChart();

        // Do this here to make sure we have a built chart.. //FIXME FIXME FIXME
        for (LogMarker marker : mraPanel.getMarkers()) {
            addLogMarker(marker);
        }
        return chart;
    }
    
    public abstract void process(LsfIndex source);

    @Override
    public Vector<LogStatisticsItem> getStatistics() {
        return null;
    }
    
    public Type getType() {
        return Type.CHART;
    }
    
    @Override
    public void onCleanup() {
        mraPanel = null;
    }
    
    @Override
    public void onHide() {
        // TODO Auto-generated method stub
        
    }
    
    public void onShow() {
        //nothing
    }

    public XYSeries getMarkerSeries() {
        if(markerSeries == null && chart != null) {
            markerSeries = new XYSeries("Marks");
            XYSeriesCollection dataset = (XYSeriesCollection) chart.getXYPlot().getDataset();
            dataset.addSeries(markerSeries);
            // Special case for marks    
            ((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setSeriesShape(dataset.indexOf(markerSeries.getKey()), new Ellipse2D.Double(0, 0, 5, 5));
            ((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setSeriesItemLabelsVisible(dataset.indexOf(markerSeries.getKey()), true);

            ((XYLineAndShapeRenderer) chart.getXYPlot().getRenderer()).setSeriesItemLabelGenerator(dataset.indexOf(markerSeries.getKey()), new XYItemLabelGenerator() {
                        @Override
                        public String generateLabel(XYDataset arg0, int arg1, int arg2) {
                            return ((TimedXYDataItem)markerSeries.getDataItem(arg2)).label;
                        }
            });
        }
        return markerSeries;
    }
}
