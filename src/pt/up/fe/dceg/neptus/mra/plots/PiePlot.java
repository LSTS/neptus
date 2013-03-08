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
 * $Id:: PiePlot.java 9803 2013-01-30 03:32:17Z robot                           $:
 */
package pt.up.fe.dceg.neptus.mra.plots;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.lsf.LsfIndex;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.LogStatisticsItem;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.MraChartPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.llf.chart.LLFChart;

/**
 * @author zp
 *
 */
public abstract class PiePlot implements LLFChart, LogMarkerListener {   
    MRAPanel mraPanel;
    
    
    public PiePlot(MRAPanel panel) {
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
    
    protected LinkedHashMap<String, Double> sums = new LinkedHashMap<>();
    
    public void incValue(String name) {
        addValue(name, 1);
        
    }
    public void addValue(String name, double ammount) {
        
        if (!sums.containsKey(name))
            sums.put(name, 0d);
        
        sums.put(name, sums.get(name)+ammount);
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
    
    
    public JFreeChart createChart() {
        
        DefaultPieDataset dataSet = new DefaultPieDataset();
        
        for (Entry<String, Double> k : sums.entrySet()) {
            dataSet.setValue(k.getKey(), k.getValue());
        }

        JFreeChart chart = ChartFactory.createPieChart( 
                getTitle(),
                dataSet,
                true,
                true,
                false);

        return chart;
    }

    @Override
    public final JFreeChart getChart(IMraLogGroup source, double timestep) {
        this.timestep = timestep;
        this.index = source.getLsfIndex();
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
    
    @Override
    public void addLogMarker(LogMarker marker) {

    }

    @Override
    public void removeLogMarker(LogMarker marker) {

    }

    @Override
    public void GotoMarker(LogMarker marker) {

    }
}
