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
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ProgressMonitor;

import org.imgscalr.Scalr;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanParserFactory;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
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
    public boolean slantRangeCorrection = false;

    @NeptusProperty
    ColorMap cmap = ColorMapFactory.createBronzeColormap();

    @NeptusProperty
    public int frequencyIndex = 0;

    @NeptusProperty
    public int cellSize = 64;
    
    @NeptusProperty
    public boolean separateCells = true;
    
    @NeptusProperty
    public File tidesFile = GeneralPreferences.tidesFile;
    
    
    private SidescanParameters params = new SidescanParameters(normalization, timeVariableGain);

    public SidescanTilesExporter(IMraLogGroup source) {
        /* nothing to do */
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return LogUtils.hasIMCSidescan(source) || SidescanParserFactory.existsSidescanParser(source);
    }

    @SuppressWarnings("resource")
    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        SidescanParser ss = SidescanParserFactory.build(source);
        
        
        
        int yCount = 1;
        if (ss.getSubsystemList().isEmpty())
            return "no sidescan data to be processed.";

        long start = ss.firstPingTimestamp();
        long end = ss.lastPingTimestamp();
        int sys = ss.getSubsystemList().get(frequencyIndex);

        pmonitor.setMinimum(0);
        pmonitor.setMaximum((int) ((end - start) / 1000));

        File out;
        BufferedWriter writer = null;
        try {
            pmonitor.setNote("Creating output dir");
            out = new File(source.getFile("mra"), "sss_tiles");
            out.mkdirs();            
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return e.getMessage();
        }
        
        try {
            writer = new BufferedWriter(new FileWriter(new File(out, "cells.csv")));  
            writer.write("filename,time,frequency,range,latitude,longitude,tide,depth,speed");
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
            
            // assumes all lines will be similar to first one
            SidescanLine pivot = lines.get(cellSize/2);
            int imgWidth = pivot.getData().length;
            
            double tideLevel = TidePredictionFactory.getTideLevel(pivot.getTimestampMillis());
            
            // create an image that will to store the sidescan data from these lines  
            BufferedImage img = new BufferedImage(imgWidth, cellSize, BufferedImage.TYPE_INT_RGB);
            
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
            if (imgWidth > 2000)
                img = Scalr.resize(img, 2000, cellSize);
            imgWidth = img.getWidth();
            
            // optionally apply slant range correction to the image
            if (slantRangeCorrection) {
                img = Scalr.apply(img,
                        new SlantRangeImageFilter(pivot.getState().getAltitude(), pivot.getRange(), img.getWidth()));
            }
            
            Graphics2D g = (Graphics2D) img.getGraphics();
            g.setColor(Color.white);
            int xCount = 1;
            // write all cells to separate image files
            for (int px = (imgWidth%cellSize)/2; px < imgWidth; px += cellSize, xCount++) {
                try {
                    File file = new File(out, String.format("t%03d_%02d", yCount, xCount) + ".png");
                    if (separateCells) {
                        BufferedImage cell = new BufferedImage(cellSize, cellSize, BufferedImage.TYPE_INT_RGB);
                        if (px > imgWidth/2)
                            cell.getGraphics().drawImage(img, 0, 0, cellSize-1, cellSize-1, px, 0, px+cellSize, cellSize-1, null);
                        else
                            cell.getGraphics().drawImage(img, 0, 0, cellSize-1, cellSize-1, px+cellSize, 0, px, cellSize-1, null);
                        ImageIO.write(cell, "PNG", file);                            
                    }
                    LocationType loc = pivot.calcPointFromIndex(px+imgWidth/2, true).location;
                    
                    String text = String.format("t%03d_%02d", yCount, xCount)+","+pivot.getTimestampMillis()+","+pivot.getFrequency()+","+pivot.getRange()+","+loc.getLatitudeDegs()+","+loc.getLongitudeDegs()+","+tideLevel+","+avgDepth+","+avgSpeed; 
                    writer.write(text+"\n");
                    System.out.println(text);                    
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                
                g.drawLine(px, 0, px, cellSize-1);
                g.drawString(String.format("t%03d_%02d", yCount, xCount), px+5, 15);
            }
            
            try {
                File file = new File(out, String.format("l%03d", yCount++) + ".png");
                ImageIO.write(img, "PNG", file);
                System.out.println("wrote " + file);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
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
