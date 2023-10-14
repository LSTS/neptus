/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 4, 2014
 */
package pt.lsts.neptus.plugins.s57;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.planning.MapPanel;
import pt.lsts.neptus.console.plugins.planning.SimulatedBathymetry;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.WorldImage;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.tiles.MapPainterProvider;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;

/**
 * @author zp
 * 
 */
@PluginDescription(name="S57 Tools")
public class S57SoundingsExporter extends ConsolePanel {

    private static final long serialVersionUID = 1653621815781506755L;

    public S57SoundingsExporter(ConsoleLayout cl) {
        super(cl);
    }

    @Override
    public void cleanSubPanel() {
        removeMenuItem(I18n.text("Tools") + ">" + I18n.text("S57") + ">"
                + I18n.text("Use S57 Bathymetry for Simulation"));
        removeMenuItem(I18n.text("Tools") + I18n.text("S57") + ">" + I18n.text("Export Depth Soundings"));
        removeMenuItem(I18n.text("Tools") + I18n.text("S57") + ">" + I18n.text("Export Bathymetry Mesh"));
    }

    public static S57Chart getS57Chart(ConsoleLayout console) throws Exception {

        Vector<MapPanel> maps = console.getSubPanelsOfClass(MapPanel.class);
        if (maps.isEmpty())
            throw new Exception(I18n.text("Cannot export soundings because there is no map in the console"));

        StateRenderer2D renderer = maps.firstElement().getRenderer();
        Map<String, MapPainterProvider> painters = renderer.getWorldMapPainter().getMapPainters();

        for (MapPainterProvider p : painters.values())
            if (p instanceof S57Chart)
                return (S57Chart) p;

        throw new Exception(I18n.text("Cannot export soundings because there is no S57 chart loaded"));                
    }

    public static StateRenderer2D getRenderer(ConsoleLayout console) throws Exception {
        Vector<MapPanel> maps = console.getSubPanelsOfClass(MapPanel.class);
        if (maps.isEmpty())
            throw new Exception(I18n.text("There is no map in the console"));
        return maps.firstElement().getRenderer();
    }

    @Override
    public void initSubPanel() {
        
        addMenuItem(I18n.text("Tools") + ">" + I18n.text("S57") + ">"
                + I18n.text("Use S57 Bathymetry for Simulation"), 
                ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())),
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                float tide = (float) TidePredictionFactory.getTideLevel(System.currentTimeMillis());
                String depthStr = JOptionPane.showInputDialog(getConsole(), I18n.text("Please enter tide level (meters)"), tide);
                if (depthStr == null)
                    return;
                try {
                    tide = Float.parseFloat(depthStr);
                    SimulatedBathymetry.getInstance().setDefaultDepth(tide);                   
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), I18n.text("S57 bathymetry"), I18n.textf("Wrong tide value: %error", ex.getMessage()));
                }   
                
                try {
                    S57Chart chart = getS57Chart(getConsole());
                    StateRenderer2D renderer = getRenderer(getConsole());
                    
                    LocationType topLeft = renderer.getTopLeftLocationType().convertToAbsoluteLatLonDepth();
                    LocationType bottomRight = renderer.getBottomRightLocationType()
                            .convertToAbsoluteLatLonDepth();
                    ArrayList<LocationType> soundings = new ArrayList<>();
                    soundings.addAll(chart.getDepthSoundings(bottomRight.getLatitudeDegs(),
                            topLeft.getLatitudeDegs(), topLeft.getLongitudeDegs(),
                            bottomRight.getLongitudeDegs()));
                    SimulatedBathymetry.getInstance().clearSoundings();
                    
                    
                    LinkedHashMap<LocationType, Double> data = new LinkedHashMap<>();
                    
                    for (LocationType loc : soundings)
                       data.put(loc, loc.getDepth()+tide);
                    
                    SimulatedBathymetry.getInstance().addSoundings(data);
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                    ex.printStackTrace();
                }
            }

        });
        
        addMenuItem(I18n.text("Tools") + ">" + I18n.text("S57") + ">" + I18n.text("Export Depth Soundings"), 
                ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())),
                new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<LocationType> soundings = new ArrayList<>();
                File f = new File("soundings.csv");
                try {
                    S57Chart chart = getS57Chart(getConsole());
                    StateRenderer2D renderer = getRenderer(getConsole());
                    
                    JFileChooser chooser = new JFileChooser();
                    chooser.setAcceptAllFileFilterUsed(true);
                    chooser.setFileFilter(GuiUtils.getCustomFileFilter("CSV Files", "csv"));
                    int op = chooser.showSaveDialog(getConsole());
                    if (op != JFileChooser.APPROVE_OPTION)
                        return;
                    f = chooser.getSelectedFile();
                    
                    LocationType topLeft = renderer.getTopLeftLocationType().convertToAbsoluteLatLonDepth();
                    LocationType bottomRight = renderer.getBottomRightLocationType()
                            .convertToAbsoluteLatLonDepth();
                    soundings.addAll(chart.getDepthSoundings(bottomRight.getLatitudeDegs(),
                            topLeft.getLatitudeDegs(), topLeft.getLongitudeDegs(),
                            bottomRight.getLongitudeDegs()));    
                    BufferedWriter w = new BufferedWriter(new FileWriter(f));
                    w.write("Latitude,Longitude,Depth\n");
                    
                    for (LocationType loc : soundings) {
                        w.write(String.format(Locale.US, "%.8f,%.8f,%.2f\n",loc.getLatitudeDegs(),loc.getLongitudeDegs(),loc.getDepth()));
                    }
                    w.close();
                    GuiUtils.infoMessage(getConsole(), I18n.text("Export soundings"), 
                            I18n.textf("Exported %points soundings to %file", soundings.size(), f.getAbsolutePath()));
                }
                catch (Exception err) {
                    GuiUtils.errorMessage(getConsole(), err);
                    err.printStackTrace();                    
                }                
            }
        });
        
        addMenuItem(I18n.text("Tools") + ">" + I18n.text("S57") + ">" + I18n.text("Export Bathymetry Mesh"), 
                ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())),
                new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ArrayList<LocationType> soundings = new ArrayList<>();
                try {
                    S57Chart chart = getS57Chart(getConsole());
                    StateRenderer2D renderer = getRenderer(getConsole());
                    BathymetryMeshOptions options = new BathymetryMeshOptions();
                    options.zero.setLocation(getConsole().getMission().getHomeRef());
                    
                    LocationType topLeft = renderer.getTopLeftLocationType().convertToAbsoluteLatLonDepth();
                    LocationType bottomRight = renderer.getBottomRightLocationType()
                            .convertToAbsoluteLatLonDepth();
                    
                    double dim[] = topLeft.getOffsetFrom(bottomRight);
                    
                    options.cellWidth = (int) Math.ceil( dim[0] / 500); 
                    
                    if (PluginUtils.editPluginProperties(options, true))
                        return;
                    
                    soundings.addAll(chart.getDepthSoundings(bottomRight.getLatitudeDegs(),
                            topLeft.getLatitudeDegs(), topLeft.getLongitudeDegs(),
                            bottomRight.getLongitudeDegs()));
                    
                    if (soundings.isEmpty()) {
                        GuiUtils.infoMessage(getConsole(), I18n.text("Export Bathymetry"), 
                                I18n.text("No soundings to export!"));
                        return;
                    }
                    
                    WorldImage img = new WorldImage(options.cellWidth, ColorMapFactory.createGrayScaleColorMap());
                    
                    for (LocationType value : soundings) {
                        img.addPoint(value, value.getDepth());
                    }
                    
                    BufferedImage image = img.processData();
                    //ImageIO.write(image, "PNG", new File(options.dest.getParentFile(), options.dest.getName()+".png"));
                    
                    double[] size = img.getNorthEast().getOffsetFrom(img.getSouthWest());
                    
                    BufferedImage scaled = new BufferedImage((int)(size[1]/options.cellWidth), (int)(-size[0]/options.cellWidth), BufferedImage.TYPE_INT_ARGB);
                    scaled.getGraphics().drawImage(image, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
                    
                    ImageIO.write(scaled, "PNG", new File(options.dest.getParentFile(), options.dest.getName()+".2.png"));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(options.dest));
                    
                    int x = 0, y;
                    for (double east = 0; x < scaled.getWidth(); east += options.cellWidth, x++) {
                        y = 0;
                        for (double north = 0; y < scaled.getHeight(); north += options.cellWidth, y++) {
                            LocationType loc = new LocationType(img.getSouthWest());
                            loc.translatePosition(north, east, 0);
                            double[] pos = loc.getOffsetFrom(options.zero);
                            long rgb = scaled.getRGB(x, y);
                            double depth = ((rgb & 0xFF) / 255.0) * img.getMaxValue();
                            System.out.printf("%.2f %.2f %.2f\n", pos[1], -pos[0], -depth);
                            bw.write(String.format(Locale.US, "%.2f %.2f %.2f\n", pos[1], -pos[0], -depth));
                        }
                    }
                    
                    bw.close();
                    
                    GuiUtils.infoMessage(getConsole(), I18n.text("Export Bathymetry"), 
                            I18n.textf("Exported mesh to %file.", options.dest.getAbsolutePath()));
                }
                catch (Exception err) {
                    GuiUtils.errorMessage(getConsole(), err);
                    err.printStackTrace();                    
                }                
            }
        });
    }
    
    static class BathymetryMeshOptions {
        @NeptusProperty(name="Mesh cell width, in meters")
        int cellWidth = 10;
        
        @NeptusProperty(name="(0,0,0) Location")
        LocationType zero = new LocationType();
        
        @NeptusProperty(name="Destination file")
        File dest = new File("bathymetry.xyz");
    }
}
