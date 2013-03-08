/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Feb 10, 2012
 * $Id:: ImcChart.java 9615 2012-12-30 23:08:28Z pdias                          $:
 */
package pt.up.fe.dceg.neptus.plugins.messages;

import info.monitorenter.gui.chart.ZoomableChart;
import info.monitorenter.gui.chart.traces.Trace2DLtd;
import info.monitorenter.util.Range;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.MultiSystemIMCMessageListener;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;

/**
 * @author jqcorreia
 * A version of ChartPanel made by ZP. This one uses a MultiSystemIMCMessageListener instead to be able to receive multiple messages from multiple systems.
 */
public class ImcChart extends JPanel {

    private static final long serialVersionUID = 6181832884003345193L;
    private static double startTime = System.currentTimeMillis() / 1000.0;
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
            if (messagesToDisplay.contains(msg.getAbbrev())) {
                int entity = msg.getHeader().getInteger("src_ent");
                String name;
                String varName = variablesToDisplay.get(messagesToDisplay.indexOf(msg.getAbbrev()));
                if(aliases == null || entity == 255)
                    name = msg.getAbbrev() + "." + varName;
                else if (aliases.containsKey(entity))
                {
                    name = msg.getAbbrev() + "." + aliases.get(entity) + "." + varName;
                }
                else
                {
                    name = msg.getAbbrev() + "." + varName;
                }
                if (traces.get(name) == null) {
                    traces.put(name, new Trace2DLtd(name));
                    traces.get(name).setColor(colors[(colorIndex++)%colors.length]);
                    chart.addTrace(traces.get(name));               
                    chart.getAxisY().setPaintGrid(true);
                    chart.getAxisY().setRange(new Range(5, 300));
                }
                traces.get(name).addPoint(System.currentTimeMillis()/1000.0-startTime, msg.getDouble(varName));
            }
        }
    };
    
    
    public ImcChart(String system, ArrayList<String> messageToDisplay, ArrayList<String> variableToDisplay, LinkedHashMap<Integer, String> aliases) {
        messagesToDisplay.addAll(messageToDisplay);
        variablesToDisplay.addAll(variableToDisplay);
        this.aliases = aliases;
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
        listener.setMessagesToListen(messagesToDisplay.toArray(new String[messagesToDisplay.size()]));
    }

    public ZoomableChart getChart() {
        return chart;
    }
    
    public void stop() {
        listener.clean();
    }
}
