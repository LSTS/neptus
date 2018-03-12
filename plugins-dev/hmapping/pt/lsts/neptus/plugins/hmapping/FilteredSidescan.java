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
 * Author: andrediegues
 * Jan 18, 2018
 */
package pt.lsts.neptus.plugins.hmapping;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ProgressMonitor;

import org.imgscalr.Scalr;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanParserFactory;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.sidescan.SlantRangeImageFilter;

/**
 * @author andrediegues
 *
 */
@PluginDescription(name="Filtered sidescan")
public class FilteredSidescan implements MRAExporter {
    private SidescanParser parser = null;
    private IMraLogGroup source = null;
    private final int subImageWidth = 100;    
    private final int subImageHeight = 100;
    private final int imageWidth = 2000;
    private final int imageHeight = 1000;
    
    
    @NeptusProperty(name="Time Variable Gain")
    public double timeVariableGain = 300;

    @NeptusProperty(name="Normalization")
    public double normalization = 0.1;

    @NeptusProperty(name="Swath Length")
    public double swathLength = 1.0;
    
    @NeptusProperty(name="Image Overlap")
    public int imageOverlap = 0;
    
    @NeptusProperty(name="Max Roll", description="Maximum value of Roll of the vehicle.")
    public double maxRoll = 5;
    
    @NeptusProperty(name="Max Pitch", description="Maximum value of Pitch of the vehicle.")
    public double maxPitch = 5;
    
    @NeptusProperty(name="Max Altitude", description="Maximum value of Altitude of the vehicle.")
    public double maxAltitude = 10;
    
    @NeptusProperty(name="Color map")
    public ColorMap cmap = ColorMapFactory.createGrayScaleColorMap();

    public FilteredSidescan(IMraLogGroup source) {
        this.source = source;
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        this.source = source;
        parser = SidescanParserFactory.build(source);
        boolean canBeApplied = (parser != null && !parser.getSubsystemList().isEmpty());
        if (canBeApplied) {
            parser.cleanup();
            parser = null;
        }
        return canBeApplied;
    }
  
    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        parser = SidescanParserFactory.build(this.source);
        SidescanParameters params = new SidescanParameters(normalization, timeVariableGain);
        long start = parser.firstPingTimestamp();
        long end = parser.lastPingTimestamp();
        int sys = parser.getSubsystemList().get(0);
        File out;
        File positions;
        BufferedWriter bw = null;
        String header = "timestamp,latitude,longitude,distance";
        int ypos = 0;
        int image_num = 1;
        ArrayList<SidescanLine> lines = new ArrayList<SidescanLine>();
        BufferedImage img = null;
        int width = imageWidth;
        int height = imageHeight;
        long lastSSSLine = -1;
        int numberOfLinesInImg = 0;        
        boolean cancel = PluginUtils.editPluginProperties(this, true);
        if (cancel)
            return I18n.text("Cancelled by user");
        
        if(pmonitor != null) {
            pmonitor.setMinimum(0);
            pmonitor.setMaximum((int)((end-start)/1000));
        }
        try {
            if(pmonitor != null)
                pmonitor.setNote("Creating output dir");
            out = new File(source.getFile("mra"), "filtered_sss_images");
            out.mkdirs(); 
            positions = new File(source.getFile("mra/filtered_sss_images"), "positions.csv");
            bw = new BufferedWriter(new FileWriter(positions));
            bw.write(header + '\n');           
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return e.getMessage();
        }
        
        for (long time = start; time < end - 1000; time += 1000) {
            if (pmonitor != null && pmonitor.isCanceled()) {
                return I18n.text("Cancelled by the user");
            }
            lines = parser.getLinesBetween(time, time + 1000, sys, params);
            if (lines.isEmpty()) {
                continue;
            }            
            if (img == null) {
                width = Math.min(imageWidth, lines.get(0).getXSize());
                height = imageHeight;
                img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                img.getGraphics().clearRect(0, 0, img.getWidth(), img.getHeight());
            }
            BufferedImage tmp;
            for (SidescanLine l : lines) {
                lastSSSLine = l.getTimestampMillis();
                SystemPositionAndAttitude pos = l.getState();
                double altitude = pos.getAltitude();
                double roll = pos.getRoll();
                double pitch = pos.getPitch();
                if (Math.abs(altitude) > maxAltitude || Math.abs(Math.toDegrees(pitch)) > maxPitch
                        || Math.abs(Math.toDegrees(roll)) > maxRoll) {
                    continue;
                }
                if(pmonitor != null) {
                    pmonitor.setNote(I18n.textf("Generating image %num",image_num));
                    pmonitor.setProgress((int)((time - start)/1000));                    
                }

                numberOfLinesInImg++;
                applySlantCorrection(l);
                if(numberOfLinesInImg % (subImageHeight/2) == 0 && numberOfLinesInImg % subImageHeight != 0) {
                    writeToPositionsFile(bw, l);
                }
                if (ypos >= height || time == end) {
                    try {
                        ImageIO.write(img, "PNG", new File(out, l.getTimestampMillis()+".png"));
                        if (imageOverlap > 0) {
                            img.getGraphics().drawImage(img, 0, 0, img.getWidth(), imageOverlap, 0,
                                    img.getHeight() - imageOverlap, img.getWidth(), img.getHeight(), null);
                        }
                        img.getGraphics().clearRect(0, imageOverlap, img.getWidth(), img.getHeight()-imageOverlap);
                        ypos = imageOverlap;
                        image_num++;
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                        return e.getMessage();
                    }
                }
                tmp = l.getImage();
                img.getGraphics().drawImage(tmp, 0, ypos, width-1, ypos+1, 0, 0, tmp.getWidth(), tmp.getHeight(), null);
                ypos++;
            }
        }
        
        try {
            ImageIO.write(img, "PNG", new File(out, lastSSSLine+".png"));
            ypos = 0;
            bw.close();
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return e.getMessage();
        }
        
        if (pmonitor != null)
            pmonitor.close();
        
        if (parser != null ) {
            parser.cleanup();
            parser = null;
        }
        
        return I18n.textf("%num images were exported to %path.", image_num, out.getAbsolutePath());
    }    
    
    /**
     * @param x 
     * @param bw: BufferedWriter to write to file data
     * @param l: SidescanLine wit the data to write
     */
    private void writeToPositionsFile(BufferedWriter bw, SidescanLine l) {
        double timestamp = l.getTimestampMillis() / 1000;
        double d = getStartingDistance(-l.getDistanceFromIndex(0, l.isImageWithSlantCorrection()));
        while(d < Math.abs(l.getDistanceFromIndex(0, l.isImageWithSlantCorrection()))) {
            if(d == 0) {
                continue;
            }
            int i = l.getIndexFromDistance(d, l.isImageWithSlantCorrection());
            double lat = l.calcPointFromIndex(i, l.isImageWithSlantCorrection()).location.getLatitudeDegs();
            double lon = l.calcPointFromIndex(i, l.isImageWithSlantCorrection()).location.getLongitudeDegs();
            double distance = l.getDistanceFromIndex(i, l.isImageWithSlantCorrection());
            try {
                bw.write(timestamp + "," + lat + "," + lon + "," + distance + '\n');
            }
            catch (IOException e) {
                NeptusLog.pub().error(e);
                return;
            }
            d += subImageWidth/10;
        }
    }

    /**
     * @param distanceFromIndex
     * @return
     */
    private int getStartingDistance(double distanceFromIndex) {
        int dist = 0;
        while(dist < distanceFromIndex) {
            dist += subImageWidth/20;
        }
        return -dist + subImageWidth/20;
    }

    private void applySlantCorrection(SidescanLine sidescanLine) {
        sidescanLine.setImage(new BufferedImage(sidescanLine.getData().length, 1, BufferedImage.TYPE_INT_RGB),false);
        for (int c = 0; c < sidescanLine.getData().length; c++) {
            sidescanLine.getImage().setRGB(c, 0, cmap.getColor(sidescanLine.getData()[c]).getRGB());
        }
        sidescanLine.setImage(Scalr.apply(sidescanLine.getImage(),
                new SlantRangeImageFilter(sidescanLine.getState().getAltitude(), sidescanLine.getRange(),
                        sidescanLine.getImage().getWidth())), true);
    }
}
