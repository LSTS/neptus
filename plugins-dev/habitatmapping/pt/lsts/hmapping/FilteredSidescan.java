/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Jan 11, 2014
 */
package pt.lsts.hmapping;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ProgressMonitor;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanParserFactory;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;

/**
 * @author andrediegues
 *
 */
@PluginDescription(name="Filtered sidescan")
public class FilteredSidescan implements MRAExporter {

    SidescanParser parser = null;
    IMraLogGroup source = null;
    
    @NeptusProperty
    public double timeVariableGain = 300;

    @NeptusProperty
    public double normalization = 0.1;

    @NeptusProperty
    public double swathLength = 1.0;
    
    @NeptusProperty
    public int imageWidth = 1920;
    
    @NeptusProperty
    public int imageHeight = 1080;
    
    @NeptusProperty
    public int imageOverlap = 0;
    
    @NeptusProperty
    public double maxRoll = 4;
    
    @NeptusProperty
    public double maxPitch = 4;
    
    @NeptusProperty
    public double maxAltitude = 6;
    
    @NeptusProperty
    public ColorMap cmap = ColorMapFactory.createGrayScaleColorMap();
    
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        parser = SidescanParserFactory.build(source);
        boolean canBeApplied = (parser != null && !parser.getSubsystemList().isEmpty());
        if (canBeApplied) {
            parser.cleanup();
            parser = null;
        }
        return canBeApplied;
    }
    
    public FilteredSidescan(IMraLogGroup source) {
        this.source = source;
    }
    
    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        parser = SidescanParserFactory.build(source);
        boolean cancel = PluginUtils.editPluginProperties(this, true);
        if (cancel)
            return I18n.text("Cancelled by user");
        
        SidescanParameters params = new SidescanParameters(normalization, timeVariableGain);
        long start = parser.firstPingTimestamp();
        long end = parser.lastPingTimestamp();
        int sys = parser.getSubsystemList().get(0);
        
        pmonitor.setMinimum(0);
        pmonitor.setMaximum((int)((end-start)/1000));
        
        File out;
        try {
            pmonitor.setNote("Creating output dir");
            out = new File(source.getFile("mra"), "filtered_sss_images");
            out.mkdirs();            
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return e.getMessage();
        }
        int ypos = 0;
        int image_num = 1;
        ArrayList<SidescanLine> lines = new ArrayList<SidescanLine>();
        BufferedImage img = null;
        int width = imageWidth, height = 1000;
        double startTime = start / 1000.0, endTime;
        
        for (long time = start; time < end - 1000; time += 1000) {
            if (pmonitor.isCanceled())
                return I18n.text("Cancelled by the user");
            lines = parser.getLinesBetween(time, time + 1000, sys, params);

            if (lines.isEmpty())
                continue;

            if (img == null) {
                width = Math.min(imageWidth, lines.get(0).getXSize());
                height = imageHeight;
                img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                img.getGraphics().clearRect(0, 0, img.getWidth(), img.getHeight());
            }
            
            BufferedImage tmp = new BufferedImage(lines.get(0).getData().length, 1, BufferedImage.TYPE_INT_RGB);
            int count = 0;
            for (SidescanLine l : lines) {
                SystemPositionAndAttitude pos = l.getState();

                double altitude = pos.getAltitude();
                double roll = pos.getRoll();
                double pitch = pos.getPitch();
                if(Math.abs(altitude) > maxAltitude || Math.abs(Math.toDegrees(pitch)) > maxPitch || Math.abs(Math.toDegrees(roll)) > maxRoll) {
                    System.out.println(++count + "Lines dropped.");
                    continue;
                }
                pmonitor.setNote(I18n.textf("Generating image %num",image_num));
                pmonitor.setProgress((int)((time - start)/1000));
                if (ypos >= height || time == end) {
                    endTime = time / 1000.0;
                    
                    try {
                        ImageIO.write(img, "PNG", new File(out, "filtered_sss_"+image_num+".png"));
                        
                        
                        if (imageOverlap > 0)
                            img.getGraphics().drawImage(img, 0, 0, img.getWidth(), imageOverlap, 0, img.getHeight()-imageOverlap, img.getWidth(), img.getHeight(), null);                            
                        
                        img.getGraphics().clearRect(0, imageOverlap, img.getWidth(), img.getHeight()-imageOverlap);

                        ypos = imageOverlap;
                        image_num++;
                        
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                        return e.getMessage();
                    }
                    startTime = endTime;
                }

                // Apply colormap to data
                for (int c = 0; c < l.getData().length; c++)
                    tmp.setRGB(c, 0, cmap.getColor(l.getData()[c]).getRGB());

                img.getGraphics().drawImage(tmp, 0, ypos, width-1, ypos+1, 0, 0, tmp.getWidth(), tmp.getHeight(), null);
                ypos++;
            }
        }
        
        try {
            ImageIO.write(img, "PNG", new File(out, "filtered_sss_"+image_num+".png"));
            ypos = 0;
            image_num++;
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return e.getMessage();
        }
        pmonitor.close();
        
        if (parser != null ) {
            parser.cleanup();
            parser = null;
        }
        return I18n.textf("%num images were exported to %path.", image_num, out.getAbsolutePath());
    }    
}
