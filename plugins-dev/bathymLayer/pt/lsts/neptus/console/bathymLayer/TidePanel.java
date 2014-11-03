/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Nov 3, 2014
 */
package pt.lsts.neptus.console.bathymLayer;

import java.awt.BorderLayout;
import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.bathymetry.TidePrediction;

/**
 * @author zp
 *
 */
@PluginDescription(name="Tide panel")
@Popup(accelerator='6',pos=POSITION.CENTER,height=300,width=300)
public class TidePanel extends ConsolePanel {
    private static final long serialVersionUID = 6517658675736342089L;

    private JFreeChart timeSeriesChart = null;
    private TimeSeriesCollection tsc = new TimeSeriesCollection();
    private ValueMarker marker = new ValueMarker(System.currentTimeMillis());
    private ValueMarker levelMarker = new ValueMarker(0); 
    
    @Periodic(millisBetweenUpdates=60000)
    public void updateMarker() {
        marker.setValue(System.currentTimeMillis());
        levelMarker.setValue(TidePrediction.getTideLevel(new Date()));
    }
    
    
    /**
     * @param console
     */
    public TidePanel(ConsoleLayout console) {
        super(console);
        setLayout(new BorderLayout());
        timeSeriesChart = ChartFactory.createTimeSeriesChart(null, null, null, tsc, true, true, true);
        add (new ChartPanel(timeSeriesChart), BorderLayout.CENTER);
    }

    @Override
    public void cleanSubPanel() {
        
    }

    @Override
    public void initSubPanel() {
        TimeSeries ts = new TimeSeries("Tide level");
        tsc.addSeries(ts);
        
        for (double i = -12; i < 12; i+= 0.25) {
            Date d = new Date(System.currentTimeMillis() + (long)(i * 1000 * 3600));
            ts.addOrUpdate(new Millisecond(d), TidePrediction.getTideLevel(d));
        }
        timeSeriesChart.getXYPlot().addDomainMarker(marker);
        levelMarker.setValue(TidePrediction.getTideLevel(new Date()));
        timeSeriesChart.getXYPlot().addRangeMarker(levelMarker);
    }
}
