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
 * Jan 11, 2014
 */
package pt.lsts.neptus.mra.exporters;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ProgressMonitor;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanParserFactory;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.replay.MraVehiclePosHud;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;

/**
 * @author zp
 *
 */
@PluginDescription
public class SidescanImageExporter implements MRAExporter {

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
    public int hudSize = 250;
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        parser = SidescanParserFactory.build(source);
        return parser != null && !parser.getSubsystemList().isEmpty();
    }
    
    public SidescanImageExporter(IMraLogGroup source) {
        this.source = source;
    }
    
    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        PluginUtils.editPluginProperties(this, true);
        MraVehiclePosHud hud = new MraVehiclePosHud(source.getLsfIndex(), hudSize, hudSize);
        hud.setPathColor(Color.white);
        //double ratio = 9.0 / 16.0;
        SidescanParameters params = new SidescanParameters(normalization, timeVariableGain);
        ColorMap cmap = ColorMapFactory.createBronzeColormap();
        long start = parser.firstPingTimestamp();
        long end = parser.lastPingTimestamp();
        int sys = parser.getSubsystemList().get(0);
        
        pmonitor.setMinimum(0);
        pmonitor.setMaximum((int)((end-start)/1000));
        
        File out;
        try {
            pmonitor.setNote("Creating output dir");
            out = new File(source.getFile("mra"), "sss_images");
            out.mkdirs();            
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return e.getMessage();
        }
        int ypos = 0;
        int image_num = 1;
        ArrayList<SidescanLine> lines;
        BufferedImage img = null;
        int width = imageWidth, height = 1000;
        double startTime = start / 1000.0, endTime;
        for (long time = start; time < end - 1000; time += 1000) {
            if (pmonitor.isCanceled())
                return "Cancelled by the user";
            lines = parser.getLinesBetween(time, time + 1000, sys, params);
            if (img == null) {
                width = Math.min(imageWidth, lines.get(0).xsize);
                height = imageHeight;
                img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            }
            BufferedImage tmp = new BufferedImage(lines.get(0).data.length, 1, BufferedImage.TYPE_INT_RGB);
            for (SidescanLine l : lines) {
                //if (ypos % 200 == 0) {
                    pmonitor.setNote("Generating image "+image_num);
                    pmonitor.setProgress((int)((time - start)/1000));
                //}
                if (ypos >= height || time == end) {
                    endTime = time / 1000.0;
                    BufferedImage hudImg = hud.getImage(startTime, endTime, 1.0);
                    img.getGraphics().drawImage(hudImg, 10, height - hudSize-10, hudSize - 10, height-10, 0, 0, hudSize, hudSize, null);
                    
                    
                    try {
                        ImageIO.write(img, "PNG", new File(out, "sss_"+image_num+".png"));
                        ypos = 0;
                        image_num++;
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                        return e.getMessage();
                    }
                    startTime = endTime;
                }

                // Apply colormap to data
                for (int c = 0; c < l.data.length; c++)
                    tmp.setRGB(c, 0, cmap.getColor(l.data[c]).getRGB());

                img.getGraphics().drawImage(tmp, 0, ypos, width-1, ypos+1, 0, 0, tmp.getWidth(), tmp.getHeight(), null);
                ypos++;
            }
            
            
        }
        pmonitor.close();
        
        return "OK";
    }    

    @Override
    public String getName() {
        return "Sidescan Images";
    }

}
