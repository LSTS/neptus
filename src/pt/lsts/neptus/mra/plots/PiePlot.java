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
package pt.lsts.neptus.mra.plots;

import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.ImageIcon;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.LogStatisticsItem;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.MraChartPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.llf.chart.LLFChart;
import pt.lsts.imc.lsf.LsfIndex;

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
    public Component getComponent(IMraLogGroup source, double timestep) {
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
