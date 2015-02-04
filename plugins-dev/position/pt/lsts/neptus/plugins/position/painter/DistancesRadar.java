/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Braga
 * 15/11/2014
 */
package pt.lsts.neptus.plugins.position.painter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.Distance;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.conf.IntegerMinMaxValidator;

import com.google.common.eventbus.Subscribe;

/**
 * @author José Braga
 * 
 */
@Popup(pos = POSITION.RIGHT, width = 450, height = 335)
@PluginDescription(name = "Distances Radar", icon = "pt/lsts/neptus/plugins/position/painter/radar-icon.png", 
description = "Distances Radar on map", category = CATEGORY.INTERFACE)
@LayerPriority(priority = 70)
public class DistancesRadar extends ConsolePanel implements Renderer2DPainter {

    private static final long serialVersionUID = -7637556359747388703L;
    private static final int LENGTH = 200;
    private static final int EXTRA = 70;
    private static final int MARGIN = 5;
    private static final int MIN_RADAR_SIZE = 1;
    private static final int MAX_RADAR_SIZE = 100;
    private static final int MIN_NUMBER_POINTS = 1;
    private static final int MAX_NUMBER_POINTS = 250;

    @NeptusProperty(name = "Enable")
    public boolean enablePainter = true;

    @NeptusProperty(name="Radar Size", description="Beam length (meters)")
    public int radarSize = 5;

    @NeptusProperty(name="Number of Points", description="Number of points shown")
    private int numberOfPoints = 100;

    @NeptusProperty(name = "Entity Name", description = "Distance entity name")
    public String entityName = "Pencil Beam";

    private String mainSysName;
    private long lastMessageMillis = 0;

    private ArrayList<Point2D> pointList = new ArrayList<>();

    Integer[] rangeValues = { 1, 2, 3, 4, 5, 10, 20, 30, 40, 50, 60, 80, 100 };
    Integer[] sectorWithValues = { 0, 10, 20, 40, 80, 100, 120, 140, 160, 180, 220, 360 };
    private JLabel text;
    private JLabel radarDistanceTxt;
    private JLabel sensorDistanceTxt;
    private JLabel sectorWithTxt;
    private JComboBox<Integer> radarDistanceRange = new JComboBox<Integer>(rangeValues);
    private JComboBox<Integer> sensorRange = new JComboBox<Integer>(rangeValues);
    private JComboBox<Integer> sectorWith = new JComboBox<Integer>(sectorWithValues);

    /**
     * @param console
     */
    public DistancesRadar(ConsoleLayout console) {
        super(console);
        mainSysName = getConsole().getMainSystem();
        MigLayout layout=new MigLayout("");
        setLayout(layout);
        radarDistanceTxt = new JLabel("Radar Range:");
        sensorDistanceTxt = new JLabel("Sens. Range:");
        sectorWithTxt = new JLabel("Sens. sector with:");
        
        text = new JLabel() {
            @Override
            public void paint(Graphics g) {
                // TODO Auto-generated method stub
                super.paint(g);
                paintRadarWorker((Graphics2D) g, LENGTH + MARGIN, LENGTH + EXTRA + 2 * MARGIN);
            }
        };
        //text.setHorizontalAlignment(JLabel.RIGHT);
        text.setBounds(0, 0, LENGTH - MARGIN, LENGTH - MARGIN);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        if (dialog!=null) {
            add(text, "w 100%, h " + (LENGTH + EXTRA + 2 * MARGIN) + "px, grow, span, wrap");
            
            radarDistanceRange.addActionListener(new ActionListener() {
              
                public void actionPerformed(ActionEvent e) {
                    JComboBox cbox = (JComboBox)e.getSource();
                    int rangeValue = (Integer) cbox.getSelectedItem();
                    System.out.println("choosen value: "+ rangeValue);
                    // TODO
                }
            });
            
            sensorRange.addActionListener(new ActionListener() {
                
                public void actionPerformed(ActionEvent e) {
                    JComboBox cbox = (JComboBox)e.getSource();
                    int rangeValue = (Integer) cbox.getSelectedItem();
                    System.out.println("choosen value: "+ rangeValue);
                    // TODO
                }
            });
            
            sectorWith.addActionListener(new ActionListener() {
                
                public void actionPerformed(ActionEvent e) {
                    JComboBox cbox = (JComboBox)e.getSource();
                    int sectorWithValue = (Integer) cbox.getSelectedItem();
                    System.out.println("choosen value: "+ sectorWithValue);
                    // TODO
                }
            });
            add(radarDistanceTxt);
            add(radarDistanceRange);
            
            add(sensorDistanceTxt);
            add(sensorRange);

            add(sectorWithTxt);
            add(sectorWith);
            dialog.setResizable(false);

        }
    }

    public String validateRadarSize(int value) {
        return new IntegerMinMaxValidator(MIN_RADAR_SIZE, MAX_RADAR_SIZE).validate(value);
    }

    public String validateNumberOfPoints(int value) {
        return new IntegerMinMaxValidator(MIN_NUMBER_POINTS, MAX_NUMBER_POINTS).validate(value);
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
       

        int width = renderer.getWidth();
        int height = renderer.getHeight();

        paintRadarWorker(g, width, height);
        
    }

    /**
     * @param g
     * @param width
     * @param height
     */
    private void paintRadarWorker(Graphics2D g, int width, int height) {
        if (!enablePainter || mainSysName == null )
            return;
        if (width == 0 || height==0)
            return;
   //     System.out.println("with "+ width + " " + height);
        g.setColor(new Color(0, 0, 0, 200));
        g.drawOval(width - LENGTH - MARGIN, height - LENGTH - EXTRA - 2 * MARGIN, LENGTH, LENGTH);

        g.setColor(new Color(0, 0, 0, 100));

        g.fillOval(width - LENGTH - MARGIN, height - LENGTH - EXTRA - 2 * MARGIN, LENGTH, LENGTH);

        g.translate(width - LENGTH - MARGIN, height - LENGTH - EXTRA - 2 * MARGIN);

        // Radar lines.
        g.setColor(new Color(20, 130, 0));
        g.drawLine(LENGTH / 2, 0, LENGTH / 2, LENGTH);
        g.drawLine(0, LENGTH / 2, LENGTH, LENGTH / 2);

        // Radar circles.
        g.drawOval(LENGTH / 4, LENGTH / 4, LENGTH / 2, LENGTH / 2);
        g.drawOval(MARGIN, MARGIN, LENGTH - MARGIN * 2, LENGTH - MARGIN * 2);

        if (System.currentTimeMillis() - lastMessageMillis > 10000)
            pointList.clear();

        // Draw last scanned angle.
        g.setColor(Color.BLACK);

        if (pointList.size() > 0) {
            Point2D last = pointList.get(pointList.size() - 1);

            int x = (int)Math.ceil(LENGTH / 2 * (1 + Math.sin(last.getY())));
            int y = (int)Math.ceil(LENGTH / 2 * (1 - Math.cos(last.getY())));

            g.drawLine(LENGTH / 2, LENGTH / 2, x, y);

            // Radar points.
            g.setColor(Color.GREEN);
            double scale = (LENGTH / 2) / radarSize;

            for (Point2D p : pointList) {
                // Only draw the ones within size.
                if (p.getX() <= radarSize) {
                    g.drawOval((int)(LENGTH / 2 + p.getX() * Math.sin(p.getY()) * scale), 
                            (int)(LENGTH / 2 - p.getX() * Math.cos(p.getY()) * scale), 
                            2, 2);
                }
            }
        }

       text.setText("<html><b><font color=#148200>" + radarSize + "</font></b></html>");

    }

    @Subscribe
    public void consume(Distance msg) {
        try {
            if (!msg.getSourceName().equals(mainSysName))
                return;

            int id = EntitiesResolver.resolveId(mainSysName, entityName);
            if (msg.getSrcEnt() != id)
                return;

            while (pointList.size() >= numberOfPoints)
                pointList.remove(0);

            pointList.add(new Point2D.Double(msg.getValue(), msg.getLocation().get(0).getPsi()));	    
            lastMessageMillis = System.currentTimeMillis();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void consume(ConsoleEventMainSystemChange ev) {
        mainSysName = ev.getCurrent();
    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
    }

}
