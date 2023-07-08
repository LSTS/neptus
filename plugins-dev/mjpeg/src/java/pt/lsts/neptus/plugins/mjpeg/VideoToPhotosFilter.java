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
 * Author: diegues
 * Feb 27, 2018
 */
package pt.lsts.neptus.plugins.mjpeg;

import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.ProgressMonitor;


import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;

/**
 * @author andrediegues
 *
 */
@PluginDescription(name="Video to photos filter", description="Exports each valid frame of the video as a JPEG file. It also exports a CSV file with the positions of the exported frames.")
public class VideoToPhotosFilter implements MRAExporter{
    private CorrectedPosition positions;
    private FrameDecoderMotionJPEG frameDecoder; 

    @NeptusProperty(name="Max Roll", description="Maximum acceptable value of Roll of the vehicle.")
    public double maxRoll = 5;

    @NeptusProperty(name="Max Pitch", description="Maximum acceptable value of Pitch of the vehicle.")
    public double maxPitch = 5;

    @NeptusProperty(name="Max Altitude", description="Maximum acceptable value of Altitude of the vehicle.")
    public double maxAltitude = 10;


    public VideoToPhotosFilter(IMraLogGroup source) {
        this.frameDecoder = new FrameDecoderMotionJPEG();
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return frameDecoder.folderContainsFrames(source.getDir());
    }
    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        this.positions = new CorrectedPosition(source);

        boolean cancel = PluginUtils.editPluginProperties(this, true);
        if (cancel) {
            return I18n.text("Cancelled by user");
        }
        frameDecoder.load(source.getDir());
        int numberOfFrames = frameDecoder.getFrameCount();  
        int numberOfPhotos = 0;  
        File dir;
        File positions;
        String header = "filename,timestamp,latitude,longitude,altitude,roll,pitch,depth\n";

        if(pmonitor != null) {
            pmonitor.setMinimum(0);
            pmonitor.setMaximum(numberOfFrames);
        }              
        if (pmonitor.isCanceled()) {
            return I18n.text("Cancelled by the user.");
        }
        try {
            pmonitor.setNote("Creating output dir.");
            dir = new File(source.getFile("mra"), "FilteredPhotos/Original");
            dir.mkdirs();
            positions = new File(source.getFile("mra/FilteredPhotos/"),"positions.csv");
            positions.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(positions));
            bw.write(header);
            for(int i = 0; i < numberOfFrames; i++) {
                frameDecoder.seekToFrame(i); 
                VideoFrame frame = frameDecoder.getCurrentFrame();
                double frameTimeStamp = (double) frame.getTimeStamp();
                if(pmonitor != null) {
                    pmonitor.setNote(I18n.textf("Analyzing frame %num", i+1));
                    pmonitor.setProgress(i);                    
                }
                if(isValid(frameTimeStamp)) {
                    numberOfPhotos++;
                    File outputfile = new File(dir ,"frame" + numberOfPhotos + ".jpg");
                    ImageIO.write((RenderedImage) frame.getImage(), "JPG", outputfile);
                    bw.write("frame" + numberOfPhotos + ".jpg," + getPosition(frameTimeStamp) + "," + getVehicleData(frameTimeStamp) + "\n");
                }
            }
            bw.close();
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return e.getMessage();
        }
        pmonitor.close();

        return I18n.textf("Filtering Video frames resulted in %num1 images in %num2 possible. Images were exported to %path.", numberOfPhotos, numberOfFrames, dir.getAbsolutePath());
    }

    private boolean isValid(double time) {
        SystemPositionAndAttitude pose = positions.getPosition(time / 1000);
        return (Math.abs(pose.getAltitude()) <= maxAltitude && Math.abs(Math.toDegrees(pose.getPitch())) <= maxPitch && Math.abs(Math.toDegrees(pose.getRoll())) <= maxRoll);
    }

    private String getPosition(double time) {
        SystemPositionAndAttitude pose = positions.getPosition(time / 1000);
        return String.format(Locale.US, "%.4f, %.5f, %.5f", time / 1000, pose.getPosition().getLatitudeDegs(), pose.getPosition().getLongitudeDegs());
    }
    private String getVehicleData(double time) {
        SystemPositionAndAttitude pose = positions.getPosition(time / 1000);
        return String.format(Locale.US, "%.2f, %.2f, %.2f, %.2f", pose.getAltitude(), Math.toDegrees(pose.getRoll()), Math.toDegrees(pose.getPitch()), pose.getDepth());
    }
}
