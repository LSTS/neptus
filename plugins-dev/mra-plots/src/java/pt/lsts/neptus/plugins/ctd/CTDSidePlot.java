/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Jan 24, 2014
 */
package pt.lsts.neptus.plugins.ctd;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitor;

import org.imgscalr.Scalr;

import pt.lsts.imc.Depth;
import pt.lsts.imc.Salinity;
import pt.lsts.imc.Temperature;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.ColorMapUtils;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;

/**
 * @author zp
 *
 */
@PluginDescription(name="CTD color map", icon="pt/lsts/neptus/plugins/ctd/thermometer.png", active=false)
public class CTDSidePlot extends SimpleMRAVisualization {

    private static final long serialVersionUID = -2237994701546034699L;
    private JTabbedPane tabs = new JTabbedPane();
    private MRAPanel panel;
    
    public CTDSidePlot(MRAPanel panel) {
        super(panel);
        this.panel = panel;
    }
    
    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().containsMessagesOfType("Salinity");
    }

    /**
     * Build image
     */
    private JImagePanel buildImage(String name, Vector<Double> xCoords, Vector<Double> yCoords, Vector<Double> values) {
        double maxDepth = Collections.max(yCoords);
        double maxTemp = Collections.max(values);
        double minTemp = Collections.min(values);
        double maxTime = xCoords.lastElement();
        double minTime = xCoords.firstElement();
        
        int cmap_width = 1000;
        int cmap_height = 600;
        
        JImagePanel ji = new JImagePanel(cmap_width+100, cmap_height+125);
        BufferedImage tmpImage = new BufferedImage(cmap_width, cmap_height, BufferedImage.TYPE_INT_ARGB);
        ColorMap cmap = ColorMapFactory.createJetColorMap();
        double depthFactor = cmap_height / maxDepth;
        double timeFactor = cmap_width / (maxTime - minTime);
        
        Point2D[] points = new Point2D[values.size()];
        
        for (int i = 0; i < values.size(); i++) {
            points[i] = new Point2D.Double(xCoords.get(i) * timeFactor, yCoords.get(i) * depthFactor);
        }
        
        ColorMapUtils.generateColorMap(points, values.toArray(new Double[0]), (Graphics2D) tmpImage.getGraphics(),
                cmap_width, cmap_height, 128, cmap, false);
        
        tmpImage = Scalr.resize(tmpImage, cmap_width, cmap_height);
        
        Graphics2D g2d = (Graphics2D)ji.getBi().getGraphics();
        g2d.drawImage(tmpImage, 75, 25, null);
        g2d.drawImage(ColorMapUtils.getBar(cmap, ColorMapUtils.HORIZONTAL_ORIENTATION, cmap_width, 20),
                75, cmap_height+50, null);
        g2d.setColor(Color.black);
        g2d.drawRect(75, cmap_height+50, cmap_width, 20);
        g2d.drawRect(75, 25, cmap_width, cmap_height);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(255,255,255,128));

        int inc = 1;
        if (maxDepth > 20.0)
            inc = 2;
        if (maxDepth > 50.0)
            inc = 5;

        for(int d = 0; d < maxDepth; d = d + inc) {
            double ycoord = depthFactor * d + 25;
            g2d.draw(new Line2D.Double(70, ycoord, 1075, ycoord));
        }
        g2d.setColor(Color.black);
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        for(int d = 0; d < maxDepth; d = d + inc) {
            double ycoord = depthFactor * d + 25;
            g2d.draw(new Line2D.Double(70, ycoord, 75, ycoord));
            g2d.drawString(""+d, 40, (int)ycoord+10);
        }
        g2d.translate(20, 375);
        g2d.rotate(-Math.PI/2);
        g2d.drawString(name, 0, 0);
        g2d.rotate(Math.PI/2);
        g2d.translate(-20, -375);
        double pos = 0;
        for (int i = 75; i <= 1075; i += 100, pos+=0.1) {
            double temperature = minTemp + (maxTemp-minTemp)*pos;
            g2d.draw(new Line2D.Double(i, 670, i, 675));
            g2d.drawString(String.format(Locale.US, "%.2f", temperature), i-25, 700);
        }
        
        g2d.translate(75, 25);
        for (int i = 2; i < xCoords.size(); i+=2) {
            double x1 = (xCoords.get(i-2) - minTime) * timeFactor;
            double y1 = yCoords.get(i-2) * depthFactor;
            double x2 = (xCoords.get(i) - minTime) * timeFactor;
            double y2 = yCoords.get(i) * depthFactor;
            
            g2d.draw(new Line2D.Double(x1,y1,x2,y2));
        }
        
        return ji;
    }
    
    
    @Override
    public JComponent getVisualization(IMraLogGroup source, double timestep) {
        LsfIndex index = source.getLsfIndex();
        ProgressMonitor pmonitor = new ProgressMonitor(panel, "Creating visualization", "Parsing data", 0, index.getNumberOfMessages());
        
        Vector<Double> xCoords = new Vector<>();
        Vector<Double> yCoords = new Vector<>();
        Vector<Double> temp = new Vector<>();
        Vector<Double> sal = new Vector<>();

        int ctdEntity = source.getLsfIndex().getFirst(Salinity.class).getSrcEnt();

        double startTime = source.getLsfIndex().getFirst(Salinity.class).getTimestamp();
        double endTime = source.getLsfIndex().getLast(Salinity.class).getTimestamp();
        int lastIndex = 0;
        for (double time = startTime; time < endTime; time++) {
            int tempIndex = source.getLsfIndex().getMessageAtOrAfer(Temperature.ID_STATIC, ctdEntity, lastIndex, time);
            int salIndex = source.getLsfIndex().getMessageAtOrAfer(Salinity.ID_STATIC, ctdEntity, lastIndex, time);
            int depthIndex = source.getLsfIndex().getMessageAtOrAfer(Depth.ID_STATIC, ctdEntity, lastIndex, time);
            
            if (tempIndex == -1 || salIndex == -1 || depthIndex == -1)
                break;
            lastIndex = tempIndex;
            
            pmonitor.setProgress(tempIndex);
            try {
                Depth d = source.getLsfIndex().getMessage(depthIndex, Depth.class);
                Temperature t = source.getLsfIndex().getMessage(tempIndex, Temperature.class);
                Salinity s = source.getLsfIndex().getMessage(salIndex, Salinity.class);
                
                xCoords.add(d.getTimestamp());
                yCoords.add(d.getValue());
                
                temp.add(t.getValue());
                sal.add(s.getValue());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            
            
        }
        
        pmonitor.setNote("Generating temperature colormap");
        tabs.add("Temperature", buildImage("Depth", xCoords, yCoords, temp));
        
        pmonitor.setNote("Generating salinity colormap");
        tabs.add("Salinity", buildImage("Depth", xCoords, yCoords, sal));
        
        pmonitor.close();
        return tabs;
    }
}
