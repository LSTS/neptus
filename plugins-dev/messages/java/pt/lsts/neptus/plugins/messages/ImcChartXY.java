/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Correia
 * Feb 10, 2012
 */
package pt.lsts.neptus.plugins.messages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.JLabel;
import javax.swing.JPanel;

import info.monitorenter.gui.chart.ZoomableChart;
import info.monitorenter.gui.chart.traces.Trace2DLtd;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.plugins.MultiSystemIMCMessageListener;

/**
 * @author jqcorreia
 * A version of ChartPanel made by ZP. This one uses a MultiSystemIMCMessageListener instead to be able to receive multiple messages from multiple systems.
 */
public class ImcChartXY extends JPanel {

    private static final long serialVersionUID = 6181832884003345193L;
    protected static final Color[] colors = {Color.red, Color.blue, Color.green, Color.orange, Color.pink, Color.cyan, Color.yellow, Color.gray, Color.magenta};
    protected int colorIndex = 0;
    protected ArrayList<String> messagesToDisplay = new ArrayList<String>();
    protected ArrayList<String> variablesToDisplay = new ArrayList<String>();
    protected LinkedHashMap<Integer, String> aliases = new LinkedHashMap<Integer, String>();
    protected ZoomableChart chart = new ZoomableChart();
    protected LinkedHashMap<String, Trace2DLtd> traces = new LinkedHashMap<String, Trace2DLtd>();
    protected JLabel title = new JLabel();
    MultiSystemIMCMessageListener listener = new MultiSystemIMCMessageListener(this.getClass().getSimpleName()
            + " [" + Integer.toHexString(hashCode()) + "]") {
        
        @Override
        public void messageArrived(ImcId16 id, IMCMessage msg) {
            
                String name = "EstimatedState";
                if (traces.get(name) == null) {
                    traces.put(name, new Trace2DLtd(name));
                    traces.get(name).setColor(colors[(colorIndex++)%colors.length]);
                    traces.get(name).setMaxSize(5000);
                    chart.addTrace(traces.get(name));               
                    chart.getAxisY().setPaintGrid(true);
                }
                
                traces.get(name).addPoint(msg.getDouble("y"), msg.getDouble("x"));
        }
    };
    
    
    public ImcChartXY(String system) {
        NeptusLog.pub().info("<###>init");
        setLayout(new BorderLayout());
        chart.setUseAntialiasing(true);
        add(chart, BorderLayout.CENTER);
        chart.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    chart.zoomAll();
                }
            }
        });
        
        listener.setSystemToListenStrings(system);
        listener.setMessagesToListen("EstimatedState");
    }

    public ZoomableChart getChart() {
        return chart;
    }
    
    public void stop() {
        listener.clean();
    }
}
