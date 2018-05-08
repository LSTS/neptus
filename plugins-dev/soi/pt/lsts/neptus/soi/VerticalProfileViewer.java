/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Apr 4, 2018
 */
package pt.lsts.neptus.soi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler.ChartTheme;

import pt.lsts.imc.VerticalProfile;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class VerticalProfileViewer implements Renderer2DPainter {

    private ArrayList<VerticalProfile> profiles = new ArrayList<>();
    private VerticalProfile selected = null;

    public void addProfile(VerticalProfile prof) {
        synchronized (profiles) {
            profiles.add(prof);
        }
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        synchronized (profiles) {
            for (VerticalProfile vp : profiles)
                paintProfileIcon(vp, g, renderer);
        }

        VerticalProfile sel = selected;
        if (sel != null) {
            paintProfileDetails(sel, g, renderer);
        }

    }

    public void paintProfileDetails(VerticalProfile p, Graphics2D g, StateRenderer2D renderer) {
        
        Point2D pt = renderer.getScreenPosition(new LocationType(p.getLat(), p.getLon()));
        g.drawString(p.getSourceName(), (int)pt.getX()+10, (int)pt.getY()+20);
        g.drawString(p.getDate().toString(), (int)pt.getX()+10, (int)pt.getY()+35);
    }
    
    XYChart getChart(VerticalProfile p) {

        ArrayList<Double> depths = new ArrayList<>();
        ArrayList<Double> values = new ArrayList<>();

        p.getSamples().forEach(s -> {
            depths.add(-s.getDepth() / 10.0);
            values.add(s.getAvg());
        });

        XYChart chart = new XYChartBuilder().title(p.getDate().toString()).theme(ChartTheme.GGPlot2).width(500)
                .height(500).yAxisTitle("depth").xAxisTitle(p.getParameterStr().toLowerCase()).build();
        chart.addSeries(p.getParameterStr().substring(0, 1), values, depths);
        
        return chart;
    }

    public void paintProfileIcon(VerticalProfile p, Graphics2D g, StateRenderer2D renderer) {

        Point2D pt = renderer.getScreenPosition(new LocationType(p.getLat(), p.getLon()));
        g.setColor(new Color(128, 128, 128, 128));
        if (selected == p)
            g.setColor(new Color(128, 255, 128));

        g.fill(new Ellipse2D.Double(pt.getX() - 8, pt.getY() - 8, 16, 16));
        g.setColor(Color.BLACK);
        switch (p.getParameter()) {
            case TEMPERATURE:
                g.drawString("T", (int) pt.getX() - 3, (int) pt.getY() + 5);
                break;
            case SALINITY:
                g.drawString("S", (int) pt.getX() - 5, (int) pt.getY() - 5);
                break;
            case CHLOROPHYLL:
                g.drawString("C", (int) pt.getX() - 5, (int) pt.getY() - 5);
                break;
            case PH:
                g.drawString("pH", (int) pt.getX() - 5, (int) pt.getY() - 5);
                break;
            default:
                break;
        }
    }

    /**
     * @param event
     * @param source
     */
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
        synchronized (profiles) {
            for (VerticalProfile p : profiles) {
                Point2D pt = source.getScreenPosition(new LocationType(p.getLat(), p.getLon()));
                if (pt.distance(event.getPoint()) < 5) {
                    selected = p;
                    return;
                }
            }
            selected = null;
        }
    }
    
    private ConcurrentHashMap<VerticalProfile, JDialog> openedWindows = new ConcurrentHashMap<>();
    
    /**
     * @param event
     * @param source
     */
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        final VerticalProfile sel = selected;
        
        if (sel != null && event.getClickCount() == 2) {
            JDialog opened = openedWindows.get(selected);
            if (opened != null) {
                opened.toFront();
            }
            else {
                JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(source), sel.getDate().toString());
                openedWindows.put(sel, dialog);
                dialog.getContentPane().setLayout(new BorderLayout());
                XChartPanel<XYChart> panel = new XChartPanel<XYChart>(getChart(sel)); 
                dialog.getContentPane().add(panel, BorderLayout.CENTER);
                dialog.setSize(700, 500);
                dialog.setLocationRelativeTo(null);
                panel.addMouseMotionListener(new MouseMotionListener() {
                    
                    @Override
                    public void mouseMoved(MouseEvent e) {
                        selected = sel;                        
                    }
                    
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        selected = sel;
                    }
                });
                
                dialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        super.windowClosing(e);
                        openedWindows.remove(sel);
                    }
                    
                    @Override
                    public void windowActivated(WindowEvent e) {
                        selected = sel;
                        super.windowActivated(e);
                    }
                });
                
                
                
                dialog.setVisible(true);
                
                
            }
        }
    }
    
    public static void main(String[] args) {
        
        XYSeries series = new XYSeries("temp");
        for (int i = 0; i < 20; i++) {
            series.add(Math.random()*10, i / 10.0);                   
        }
        XYDataset dataset = new XYSeriesCollection(series);
        
        JFreeChart chart = ChartFactory.createXYLineChart("test",
               "test2", "Depth", dataset, PlotOrientation.VERTICAL, false, false, false);        
        chart.getXYPlot().setRenderer(new XYSplineRenderer());
        ChartPanel cp = new ChartPanel(chart);
        GuiUtils.testFrame(cp);
    }

}
