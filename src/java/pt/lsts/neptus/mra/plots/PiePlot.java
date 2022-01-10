/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.ImageIcon;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.LogStatisticsItem;
import pt.lsts.neptus.mra.MRAChartPanel;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.llf.chart.LLFChart;

/**
 * @author zp
 *
 */
public abstract class PiePlot implements LLFChart, LogMarkerListener {   
    protected MRAPanel mraPanel;
    protected LsfIndex index;
    protected double timestep = 0;
    protected JFreeChart chart;
    protected LinkedHashMap<String, Double> sums = new LinkedHashMap<>();
    //protected double total = 0;
    
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


    public void incValue(String name) {
        addValue(name, 1);
    }

    
    public void cleanupSeries(double otherRatio) {
        Vector<Pair<String, Double>> values = new Vector<>();
        double totalSum = 0;
        for (Entry<String, Double> k : sums.entrySet()) {
            totalSum += k.getValue();
            values.add(new Pair<String, Double>(k.getKey(), k.getValue()));
        }
        
        Collections.sort(values, new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                return o1.second().compareTo(o2.second());
            }
        });
        
        double otherSum = 0;
        
        for (Pair<String, Double> v : values) {
            if ((otherSum + v.second()) / totalSum < otherRatio) {
                otherSum += v.second();
                sums.remove(v.first());
            }
            else
                break;
        }
        
        sums.put("Other", otherSum);
        for (Entry<String, Double> k : sums.entrySet()) {
            sums.put(k.getKey(), (k.getValue()/totalSum) * 100);
        }
        
    }
    
    public void addValue(String name, double amount) {

        if (!sums.containsKey(name))
            sums.put(name, 0d);
        sums.put(name, sums.get(name)+amount);
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        return new MRAChartPanel(this, source, mraPanel);
    }

    @Override
    public final boolean canBeApplied(IMraLogGroup source) {     
        return canBeApplied(source.getLsfIndex());
    }

    public abstract boolean canBeApplied(LsfIndex index);

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("pt/lsts/neptus/mra/plots/chart-pie.png");
    }

    @Override
    public Double getDefaultTimeStep() {
        return 0.1;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }


    public JFreeChart createChart() {

        DefaultPieDataset dataSet = new DefaultPieDataset();

        // contraption for interleaving small and big values
        ArrayList<String> keys = new ArrayList<>();
        keys.addAll(sums.keySet());
        boolean beginning = true;
        while (!keys.isEmpty()) {
            int index = 0;
            if (!beginning)
                index = keys.size()-1;
            String key = keys.get(index);
            keys.remove(index);
            beginning = !beginning;
            dataSet.setValue(key, sums.get(key));
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

    @Override
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

    @Override
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
    public void goToMarker(LogMarker marker) {

    }
}
