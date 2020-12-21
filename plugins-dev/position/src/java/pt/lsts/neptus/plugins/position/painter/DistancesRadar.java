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
 * Author: José Braga
 * 15/11/2014
 */
package pt.lsts.neptus.plugins.position.painter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.Distance;
import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.params.ConfigurationManager;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.params.SystemProperty.Scope;
import pt.lsts.neptus.params.SystemProperty.Visibility;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.conf.IntegerMinMaxValidator;

/**
 * @author José Braga
 * @author Manuel Ribeiro
 */
@PluginDescription(name = "Distances Radar", icon = "pt/lsts/neptus/plugins/position/painter/radar-icon.png",
    description = "Distances Radar on map", category = CATEGORY.INTERFACE, author = "José Braga")
@Popup(pos = POSITION.RIGHT, width = 223, height = 335, accelerator = '0')
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

    @NeptusProperty(name = "Enable", userLevel=LEVEL.REGULAR)
    public boolean enablePainter = true;

    @NeptusProperty(name="Radar Size", description="Beam length (meters)", userLevel=LEVEL.REGULAR)
    public int radarSize = 5;

    @NeptusProperty(name="Number of Points", description="Number of points shown", userLevel=LEVEL.REGULAR)
    private int numberOfPoints = 100;

    @NeptusProperty(name = "Entity Name", description = "Distance entity name", userLevel=LEVEL.REGULAR)
    public String entityName = "Pencil Beam";

    private String mainSysName;
    private long lastMessageMillis = 0;

    private ArrayList<Point2D> pointList = new ArrayList<>();

    private Integer[] rangeValues = { 1, 2, 3, 4, 5, 10, 20, 30, 40, 50, 60, 80, 100 };
    private Integer[] sectorWidthValues = { 0, 10, 20, 40, 80, 100, 120, 140, 160, 180, 220, 360 };
    private int range;
    private int width;
    private Scope scopeToUse = Scope.GLOBAL;
    private Visibility visibility = Visibility.USER;

    private JLabel text;
    private JLabel radarDistanceTxt;
    private JLabel sensorRangeTxt;
    private JLabel sectorWidthTxt;
    private JComboBox<Integer> radarDistanceRange = new JComboBox<Integer>(rangeValues);
    private JComboBox<Integer> sensorRange = new JComboBox<Integer>(rangeValues);
    private JComboBox<Integer> sectorWidth = new JComboBox<Integer>(sectorWidthValues);
    
    /**
     * @param console
     */
    @SuppressWarnings("serial")
    public DistancesRadar(ConsoleLayout console) {
        super(console);
        
        mainSysName = getConsole().getMainSystem();
        
        MigLayout layout = new MigLayout();
        setLayout(layout);
        
        radarDistanceTxt = new JLabel(I18n.text("Radar Range")+ ":");
        sensorRangeTxt = new JLabel(I18n.text("Sensor Range") + ":");
        sectorWidthTxt = new JLabel(I18n.text("Sector Width") + ":");

        text = new JLabel() {
            @Override
            public void paint(Graphics g) {
                if (dialog!=null && dialog.isShowing()) 
                    paintRadarWorker((Graphics2D) g, LENGTH + MARGIN, LENGTH + EXTRA + 2 * MARGIN);
            }
        };
        text.setBounds(0, 0, LENGTH - MARGIN, LENGTH - MARGIN);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {

            WindowAdapter l = new WindowAdapter() {
                @Override
                public void windowActivated(WindowEvent e) {
                    if (!enablePainter) {
                        dialog.setVisible(false);
                    }
                    else {
                        dialog.setVisible(true);
                    }
                }
            };
            
            dialog.addWindowListener(l);

        if (dialog!=null) {
            add(text, "w 100%, h " + (LENGTH + EXTRA + 2 * MARGIN) + "px, grow, span, wrap");

            radarDistanceRange.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    @SuppressWarnings("rawtypes")
                    JComboBox cbox = (JComboBox)e.getSource();
                    radarSize = (Integer) cbox.getSelectedItem();
                    text.repaint();
                }
            });

            sensorRange.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    @SuppressWarnings("rawtypes")
                    JComboBox cbox = (JComboBox)e.getSource();
                    int rangeValue = (int) cbox.getSelectedItem();
                    saveEntityParameter("Range", rangeValue);
                }
            });

            sectorWidth.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    @SuppressWarnings("rawtypes")
                    JComboBox cbox = (JComboBox)e.getSource();
                    int sectorWidthValue = (int) cbox.getSelectedItem();
                    saveEntityParameter("Sector Width", sectorWidthValue);
                }
            });

            radarDistanceRange.setSelectedItem(radarSize);
            add(radarDistanceTxt);
            add(radarDistanceRange, "wrap");

            add(sensorRangeTxt);
            add(sensorRange, "wrap");

            add(sectorWidthTxt);
            add(sectorWidth);
            dialog.setResizable(false);
        }
    }

    @Override
    public void cleanSubPanel() {
    }

    public String validateRadarSize(int value) {
        return new IntegerMinMaxValidator(MIN_RADAR_SIZE, MAX_RADAR_SIZE).validate(value);
    }

    public String validateNumberOfPoints(int value) {
        return new IntegerMinMaxValidator(MIN_NUMBER_POINTS, MAX_NUMBER_POINTS).validate(value);
    }

    private void saveEntityParameter(String type, long value) {
        if (mainSysName==null)
            return;

        SystemProperty rangeProp = null;
        SystemProperty sectorWidthProp = null;
        ArrayList<SystemProperty> sysProps = ConfigurationManager.getInstance().getProperties(mainSysName, visibility, scopeToUse);
        for (SystemProperty s : sysProps) {
            if (s.getName().equals("Range"))
                rangeProp = s;
            if (s.getName().equals("Sector Width"))
                sectorWidthProp = s;
        }

        if (type.equals("Range")) {
            if (rangeProp!=null) {
                rangeProp.setValue(value);
                sendParam(rangeProp);
            }
        } else if (type.equals("Sector Width")) {
            if (sectorWidthProp!=null) {
                sectorWidthProp.setValue(value);
                sendParam(sectorWidthProp);
            }
        }
    }

    /**
     * @param prop
     */
    private void sendParam(SystemProperty prop) {
        String category = prop.getCategoryId();

        EntityParameter ep = new EntityParameter();
        ep.setName(prop.getName());
        ep.setValue((String) prop.getValue().toString());

        ArrayList<EntityParameter> propList = new ArrayList<>(1);
        propList.add(ep);
        SetEntityParameters setParams = new SetEntityParameters();
        setParams.setName(category);
        setParams.setParams(propList);

        send(setParams);
    }

    @Periodic(millisBetweenUpdates=5000)
    private void update() {
        getRadarAndSensorValues();
    }

    private void getRadarAndSensorValues() {

        if (mainSysName!=null) {
            radarDistanceRange.setSelectedItem(radarSize);

            ArrayList<SystemProperty> pr = ConfigurationManager.getInstance().getProperties(mainSysName, visibility, scopeToUse);
            boolean hasPencilBeam = false;
            for (SystemProperty s : pr) {
                if (s.getName().equals("Range") && s.getCategoryId().equals(entityName)) {
                    range = ((Long) s.getValue()).intValue();
                    hasPencilBeam = true;
                }
                if (s.getName().equals("Sector Width")  && s.getCategoryId().equals(entityName)){
                    width = ((Long) s.getValue()).intValue();
                }
            }
            if (!hasPencilBeam) {
                sensorRange.setEnabled(false);
                sectorWidth.setEnabled(false);
                
            } else {
                sensorRange.setEnabled(true);
                sectorWidth.setEnabled(true);
                if ((int)sensorRange.getSelectedItem() != range) 
                    sensorRange.setSelectedItem((int) range);
                if ((int)sectorWidth.getSelectedItem() != width)
                    sectorWidth.setSelectedItem((int) width);
            }
        }
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (dialog!=null && !dialog.isShowing()) {
            int width = renderer.getWidth();
            int height = renderer.getHeight();
            paintRadarWorker(g, width, height);
        }
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

            int x = (int) Math.ceil(LENGTH / 2 * (1 + Math.sin(last.getY())));
            int y = (int) Math.ceil(LENGTH / 2 * (1 - Math.cos(last.getY())));

            g.drawLine(LENGTH / 2, LENGTH / 2, x, y);

            // Radar points.
            g.setColor(Color.GREEN);

            for (Point2D p : pointList) {
                // Only draw the ones within size.
                if (p.getX() <= radarSize) {
                    g.drawOval((int)(LENGTH / 2 * (1 + (p.getX() * Math.sin(p.getY()) / radarSize))),
                               (int)(LENGTH / 2 * (1 - (p.getX() * Math.cos(p.getY()) / radarSize))),
                               2, 2);
                }
            }
        }
        Font font = new Font("default", Font.BOLD, g.getFont().getSize());
        g.setFont(font);
        g.setColor(Color.BLACK);
        g.drawString("" + radarSize, LENGTH - MARGIN * 4, LENGTH / 2 + MARGIN);
    }

    @Subscribe
    public void consume(Distance msg) {
        try {
            if (!msg.getSourceName().equals(mainSysName))
                return;

//            if (msg.getValidity() != Distance.VALIDITY.VALID)
//                return;
            
            int id = EntitiesResolver.resolveId(mainSysName, entityName);
            if (msg.getSrcEnt() != id)
                return;
            
            while (pointList.size() >= numberOfPoints)
                pointList.remove(0);

            pointList.add(new Point2D.Double(msg.getValue(), msg.getLocation().get(0).getPsi()));	    
            lastMessageMillis = System.currentTimeMillis();
            updateDialogPanel();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void updateDialogPanel(){
        repaint();
    }

    @Subscribe
    public void consume(ConsoleEventMainSystemChange ev) {
        mainSysName = ev.getCurrent();
    }
}
