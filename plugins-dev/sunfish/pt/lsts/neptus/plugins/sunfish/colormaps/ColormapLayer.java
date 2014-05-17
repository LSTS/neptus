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
 * May 17, 2014
 */
package pt.lsts.neptus.plugins.sunfish.colormaps;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.mra.WorldImage;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription
public class ColormapLayer extends ConsoleLayer implements ConfigurationListener {

    @NeptusProperty
    public File dataSource = null;

    @NeptusProperty(editorClass=pt.lsts.neptus.gui.editor.ColorMapPropertyEditor.class)
    public ColorMap colormap = ColorMapFactory.createJetColorMap();
    
    @NeptusProperty
    public int cellWidthMeters = 100;
    
    @NeptusProperty
    public String scalarName = "Temperature";
    
    @NeptusProperty
    public int alpha = 128;
    
    private String status = "No map loaded";
    
    private BufferedImage image = null;
    private LocationType ne = null, sw = null;
    private WorldImage worldImage = new WorldImage(cellWidthMeters, colormap);
    private int latColumn = 0, lonColumn = 1, dataColumn = -1;
    
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    @Override
    public void initLayer() {
        propertiesChanged();       
        addMenuItem("Tools>Colormap settings", null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               PluginUtils.editPluginProperties(ColormapLayer.this, true);     
            }
        });
    }

    @Override
    public void cleanLayer() {
        image = null;
        worldImage = null;
    }
    
    private void loadFile() {
        if (dataSource == null) {
            status = "No map loaded";
            return;
        }
        else {
            getConsole().post(Notification.info("Colormap", "Loading data..."));
            status = "Loading data";
        }
        worldImage = new WorldImage(cellWidthMeters, colormap);
        
        Vector<String> fields = new Vector<>();
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader(dataSource));
            
            String[] header = reader.readLine().split(",");
            int i = 0;
            latColumn = lonColumn = dataColumn = -1;
            for (String s : header) {
                String field = s.trim().toLowerCase();
                fields.add(field);
                if (field.startsWith("lat"))
                    latColumn = i;
                else if (field.startsWith("lon"))
                    lonColumn = i;
                else if (field.equalsIgnoreCase(scalarName)){
                    dataColumn = i;
                }
                i++;
            }
            
            if (latColumn == -1 || lonColumn == -1 || dataColumn == -1) {
                reader.close();
                throw new Exception("CSV file is not valid. Please include an header with latitude, longitude and "+scalarName+".");            
            }
            
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                double lat = Double.parseDouble(parts[latColumn].trim());
                double lon = Double.parseDouble(parts[lonColumn].trim());
                double value = Double.parseDouble(parts[dataColumn].trim());                
                worldImage.addPoint(new LocationType(lat, lon), value);                
            }
            status = "Processing data";
            
            image = worldImage.processData();
            ne = worldImage.getNorthEast();
            sw = worldImage.getSouthWest();
            
            reader.close();
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
        }
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        if (renderer.getRotation() != 0)
            return;
        if (image != null) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (alpha/255.0)));
            Point2D ptNE = renderer.getScreenPosition(ne);
            Point2D ptSW = renderer.getScreenPosition(sw);
            g.drawImage(image, (int) ptSW.getX(), (int) ptNE.getY(), (int) ptNE.getX(), (int) ptSW.getY(), 0, 0, image.getWidth(), image.getHeight(), null);
        }
        else {
            g.setColor(Color.white);
            g.drawString("ColorMap: "+status, 200, 10);
        }
    }
    
    @Override
    public void propertiesChanged() {
        Thread t = new Thread("Colormap processor") {
            public void run() {
                loadFile();        
            };
        };
        t.setDaemon(true);
        t.start();
    }    
}
