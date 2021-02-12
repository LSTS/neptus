/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 19, 2020
 */
package pt.lsts.neptus.mra.exporters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ProgressMonitor;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;
import org.imgscalr.Scalr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.api.SidescanHistogramNormalizer;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanParserFactory;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.llf.LogUtils;
import pt.lsts.neptus.util.sidescan.SlantRangeImageFilter;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Sidescan Tiles Exporter")
public class SidescanTilesExporter implements MRAExporter {

    @NeptusProperty
    public double timeVariableGain = 250;

    @NeptusProperty
    public double normalization = 0.2;

    @NeptusProperty
    public boolean slantRangeCorrection = true;

    @NeptusProperty
    ColorMap cmap = ColorMapFactory.createJetColorMap();

    @NeptusProperty
    public int frequencyIndex = 1;

    @NeptusProperty
    public int cellSize = 32;
    
    @NeptusProperty
    public boolean separateCells = true;
    
    @NeptusProperty
    public File tidesFile = GeneralPreferences.tidesFile;
    
    @NeptusProperty(description = "GeoJson file with polygons denoting sand and rock for training")
    public File classificationFile = new File("/home/zp/Desktop/ml-training/NP2/NP2.geojson");
    
    @NeptusProperty(description = "Directory where to put all output")
    public File outDir = new File("/home/zp/Desktop/ml-training/NP2");
    
    @NeptusProperty(description = "Enhanced Gain Normalization")
    public boolean useEGN = true;
    
    @NeptusProperty(description = "Use Corrected Position")
    public boolean useCorrectedPosition = true;

    /* variables below are not parameters */
    private boolean doTraining = false;
    private ArrayList<PolygonType> sandPolygons = new ArrayList<PolygonType>();
    private ArrayList<PolygonType> rockPolygons = new ArrayList<PolygonType>();
    private SidescanParameters params = new SidescanParameters(normalization, timeVariableGain);

    public SidescanTilesExporter(IMraLogGroup source) {
        /* nothing to do */
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return LogUtils.hasIMCSidescan(source) || SidescanParserFactory.existsSidescanParser(source);
    }
    
    public void loadGeoJsonPolygons(File f) throws Exception {
        FeatureCollection features = new ObjectMapper().readValue(Files.toByteArray(f), FeatureCollection.class);
        for (Feature feature : features.getFeatures()) {
            GeoJsonObject obj = feature.getGeometry();
            
            if (obj instanceof Polygon) {
                String type = ""+feature.getProperties().get("TYPE");
                Polygon polygon = (Polygon)obj;
                PolygonType ptype = new PolygonType();
                for (List<LngLatAlt> pts : polygon.getCoordinates())
                    for (LngLatAlt pt : pts)
                        ptype.addVertex(0, new LocationType(pt.getLatitude(), pt.getLongitude()));                    
                
                if (type.toLowerCase().contains("sand"))
                    sandPolygons.add(ptype);
                if (type.toLowerCase().contains("rock"))
                    rockPolygons.add(ptype);                            
            }
        }
        
        System.out.println("Loaded "+sandPolygons.size()+" sand polygons and "+rockPolygons.size()+" rock polygons.");
    }
    
    /**
     * This method tests if a location is contained inside any polygon on the given list
     * @param location Location to check
     * @param polygons List of polygons to check for containment
     * @return <code>true</code> if the point is inside any of the given containers
     */
    boolean isContained(LocationType location, ArrayList<PolygonType> polygons) {
        if (polygons.isEmpty())
            return false;
        for (PolygonType poly : polygons) {
            
            if (poly.containsPoint(location))
                return true;
        }        
        return false;
    }
    
    /**
     * Check if a location is inside any of the sand polygons
     * @param location Location to check
     * @return <code>true</code> if the location resides inside any of the sand polygons
     */
    boolean isSand(LocationType location) {
       return isContained(location, sandPolygons);
    }
    
    /**
     * Check if a location is inside any of the rock polygons
     * @param location Location to check
     * @return <code>true</code> if the location resides inside any of the rock polygons
     */
    boolean isRock(LocationType location) {
        return isContained(location, rockPolygons); 
    }

    @SuppressWarnings("resource")
    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {

    	CorrectedPosition correctedPosition = new CorrectedPosition(source);
        // Create sidescan parser and skip this log if no sss data is available
        SidescanParser ss = SidescanParserFactory.build(source);
        if (ss.getSubsystemList().isEmpty())
            return "no sidescan data to be processed.";
        
        // Create output directory
        if (outDir != null)
            outDir.mkdirs();
        
        
        // if the classification file is set, build a training set
        if (classificationFile.canRead()) {
            try {
                loadGeoJsonPolygons(classificationFile);    
                doTraining = true;
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                return e.getMessage();
            }            
        }
        int yCount = 1;
        
        long start = ss.firstPingTimestamp();
        long end = ss.lastPingTimestamp();
        int sys = ss.getSubsystemList().get(ss.getSubsystemList().size()-1);

        pmonitor.setMinimum(0);
        pmonitor.setMaximum((int) ((end - start) / 1000));

        BufferedWriter writer = null;
        try {
            pmonitor.setNote("Creating output file");
            
            File csvFile = new File(outDir, "tiles.csv");
            
            if (doTraining)
                csvFile = new File(outDir, "training.csv");
            
            boolean appending = true;
            if (!csvFile.exists())
                appending = false;
            writer = new BufferedWriter(new FileWriter(csvFile, true));
            // if the file didn't exist previously, write the header
            if (!appending)
                writer.write("filename,time,frequency,range,latitude,longitude,depth,distance,speed,classification\n");            
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return e.getMessage();
        }
        ArrayList<SidescanLine> lines = new ArrayList<SidescanLine>();
        
        for (long time = start; time < end; time += 1000) {
        
            if (pmonitor.isCanceled())
                return I18n.text("Cancelled by the user");

            // add one second of lines 
            lines.addAll(ss.getLinesBetween(time, time+1000, sys, params));
            
            // if there are less lines than the target, keep adding lines
            if (lines.size() <= cellSize)
                continue;
            
            // assumes all lines will be similar to middle one
            SidescanLine pivot = lines.get(cellSize/2);
            if (useCorrectedPosition)
            	pivot.getState().setPosition(correctedPosition.getPosition(pivot.getTimestampMillis()/1000.0).getPosition());
            
            int originalLength = pivot.getData().length;
            
            // calculate tide level using tide station configured in GeneralPreferences
            double tideLevel = TidePredictionFactory.getTideLevel(pivot.getTimestampMillis());
            
            int dataLength = originalLength;
            // create an image that will to store the sidescan data from these lines  
            BufferedImage img = new BufferedImage(dataLength, cellSize, BufferedImage.TYPE_INT_RGB);
            
            double avgDepth = 0;
            double avgSpeed = 0;
            // draw the lines' data in the image
            for (int l = 0; l < cellSize; l++) {
                SidescanLine line = lines.remove(0);
                for (int c = 0; c < line.getData().length; c++) {
                    img.setRGB(c, l, cmap.getColor(line.getData()[c]).getRGB());                    
                }            
                avgDepth += (line.getState().getAltitude() + line.getState().getDepth()) * (1.0/cellSize);
                avgSpeed += (line.getState().getU()) * (1.0/cellSize);
            };

            if (avgSpeed < 0.8)
                continue;
            
            
            if (dataLength > 2000) {
                BufferedImage copy = new BufferedImage(2000, cellSize, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = (Graphics2D)copy.getGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(img, 0, 0, 2000-1, cellSize-1, 0, 0, img.getWidth()-1, img.getHeight()-1, null);
                img = copy;
            }
                
            dataLength = img.getWidth();
            
            // optionally apply slant range correction to the image
            if (slantRangeCorrection) {
                img = Scalr.apply(img,
                        new SlantRangeImageFilter(pivot.getState().getAltitude(), pivot.getRange(), img.getWidth()));
            }
            
            Graphics2D g = (Graphics2D) img.getGraphics();
            g.setColor(Color.white);
            int xCount = 1;
            String logName = source.getDir().getName();
            double ratio = originalLength / (double) dataLength;
            
            new File(outDir+"/tiles").mkdirs();
            
            
            
            // write all cells to separate image files
            for (int px = (dataLength%cellSize)/2; px < dataLength; px += cellSize, xCount++) {
                try {
                    int originalPx = (int) ((px+cellSize/2.0) * ratio);
                    LocationType loc = pivot.calcPointFromIndex(originalPx, true).location;
                    float distance = (float) loc.getDistanceInMeters(pivot.getState().getPosition());
                    String text = logName+"_"+String.format("t%03d_%02d", yCount, xCount)+","+pivot.getTimestampMillis()+","+pivot.getFrequency()+","+pivot.getRange()+","+loc.getLatitudeDegs()+","+loc.getLongitudeDegs()+","+(tideLevel+avgDepth)+","+distance+","+avgSpeed; 
                    if (doTraining) {
                        if (isRock(loc))
                            text += ",rock";
                        else if (isSand(loc))
                            text += ",sand";
                        else
                            continue;
                    }
                    
                    writer.write(text+"\n");
                    System.out.println(text);
                    
                    File file = new File(outDir, logName+"_"+String.format("t%03d_%02d", yCount, xCount) + ".png");
                    if (separateCells) {
                        BufferedImage cell = new BufferedImage(cellSize, cellSize, BufferedImage.TYPE_INT_RGB);
                        if (px > dataLength/2)
                            cell.getGraphics().drawImage(img, 0, 0, cellSize-1, cellSize-1, px, 0, px+cellSize, cellSize-1, null);
                        else
                            cell.getGraphics().drawImage(img, 0, 0, cellSize-1, cellSize-1, px+cellSize, 0, px, cellSize-1, null);
                        ImageIO.write(cell, "PNG", file);                            
                    }           
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                
                //g.drawLine(px, 0, px, cellSize-1);
                //g.drawString(String.format("t%03d_%02d", yCount, xCount), px+5, 15);
            }
            /*
            try {
                File file = new File(outDir, logName+"_"+String.format("l%03d", yCount++) + ".png");
                ImageIO.write(img, "PNG", file);
                System.out.println("wrote " + file);
            }
            catch (Exception e) {
                e.printStackTrace();
            }*/
            yCount++;
        }
        try {
            writer.close();    
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return e.getMessage();
        }
        return "done.";
    }

    public static void main(String[] args) {
        GeneralPreferences.initialize();
        GuiUtils.setLookAndFeelNimbus();
        BatchMraExporter.apply(SidescanTilesExporter.class);
    }

}
