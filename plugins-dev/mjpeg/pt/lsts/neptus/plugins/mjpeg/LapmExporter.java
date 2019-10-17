/*
 * Copyright (c) 2004-2019 Universidade do Porto - Faculdade de Engenharia
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
 * Oct 17, 2019
 */
package pt.lsts.neptus.plugins.mjpeg;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import javax.imageio.ImageIO;
import javax.swing.ProgressMonitor;

import org.imgscalr.Scalr;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name = "LAPM Exporter", description = "Creates an LAPMV2 project folder with the photos and auxiliary CSV files.")
public class LapmExporter implements MRAExporter {
    private CorrectedPosition positions;
    private FrameDecoderMotionJPEG frameDecoder;

    @NeptusProperty(name = "Seconds between photos")
    public double secsBetweenPhotos = 1.0;

    @NeptusProperty(name = "Maximum Altitude")
    public double maxAltitude = 5;
    
    @NeptusProperty(name = "Enhance Constrast")
    public boolean contrastEnhancement = true;
    

    public LapmExporter(IMraLogGroup source) {
        this.positions = new CorrectedPosition(source);
        this.frameDecoder = new FrameDecoderMotionJPEG();
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return frameDecoder.folderContainsFrames(source.getDir());
    }

    private File createOutputFolder(IMraLogGroup source) throws Exception {
        File out = new File(source.getDir(), "mra/lapm");
        out.mkdirs();
        return out;
    }

    private void createSensorList(File outputFolder, int xRes, int yRes) throws Exception {
        File out = new File(outputFolder, "sensor_list.csv");
        BufferedWriter writer = new BufferedWriter(new FileWriter(out));

        writer.write(
                "sensor.id,platform,camera.name,sensor.width,sensor.height,sensor.principalpointX,sensor.principalpointY,sensor.width,sensor.height,sensor.focal,offset.longitudinal,offset.transversal,offset.vertical,offset.roll,offset.pitch,offset.heading,comments\n");
        writer.write(
                "integer,string,string,[pixels],[pixels],[pixels],[pixels],[mm],[mm],[mm],[m],[m],[m],[rad],[rad],[deg],string\n");
        writer.write("1,LAUV,Lumenera HD," + xRes + "," + yRes + ",,,4.8,2.7,3.55,0.576,0.4495,0.0406,0,0,0,\n");
        writer.close();
    }

    private BufferedWriter createImageSequence(File outputFolder) throws Exception {
        File out = new File(outputFolder, "image_sequence.csv");
        BufferedWriter writer = new BufferedWriter(new FileWriter(out));
        return writer;
    }

    private BufferedWriter createNavigation(File outputFolder) throws Exception {
        File out = new File(outputFolder, "navigation.csv");
        BufferedWriter writer = new BufferedWriter(new FileWriter(out));
        writer.write("filename,Latitude,Longitude,depth,altitude_approx,heading,roll,pitch,sensor_id,focal\n");
        writer.write("string,[dd],[dd],[m],[m],[deg],[deg],[deg],integer,[mm]\n");
        return writer;
    }

    private File saveFrame(File outputFolder, VideoFrame frame) throws Exception {
        File out = new File(outputFolder, "frm_" + frame.getNumber() + ".jpg");
        
        if (!contrastEnhancement)
            ImageIO.write((RenderedImage) frame.getImage(), "JPG", out);
        else {
            Image img = frame.getImage();
            BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D bGr = bimage.createGraphics();
            bGr.drawImage(img, 0, 0, null);
            bGr.dispose();
            System.out.println(bimage.getWidth()+" / "+bimage.getHeight());
            bimage = Scalr.apply(bimage, ImageUtils.sharpenOp());
            
            ImageIO.write((RenderedImage)bimage, "JPG", out);
        }
        return out;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        boolean cancel = PluginUtils.editPluginProperties(this, true);
        if (cancel)
            return I18n.text("Cancelled by the user");

        frameDecoder.load(source.getDir());
        int numberOfFrames = frameDecoder.getFrameCount();
        
        if (pmonitor != null) {
            pmonitor.setMinimum(0);
            pmonitor.setMaximum(numberOfFrames);
        }
        pmonitor.setNote("Creating output directories...");

        VideoFrame frame = frameDecoder.getCurrentFrame();
        Image img = frame.getImage();
        int xRes = img.getWidth(null);
        int yRes = img.getHeight(null);
        double lastTimeStamp = frame.getTimeStamp() / 1000.0;

        try {

            File outFolder = createOutputFolder(source);
            createSensorList(outFolder, xRes, yRes);
            BufferedWriter navWriter = createNavigation(outFolder);
            BufferedWriter imgWriter = createImageSequence(outFolder);

            for (int i = 0; i < numberOfFrames; i++) {

                frameDecoder.seekToFrame(i);
                frame = frameDecoder.getCurrentFrame();
                
                if (frame.getTimeStamp() < (secsBetweenPhotos + lastTimeStamp) * 1000)
                    continue;

                double frameTimeStamp = (double) frame.getTimeStamp() / 1000.0;

                SystemPositionAndAttitude pos = positions.getPosition(frameTimeStamp);
                if (pos.getAltitude() > maxAltitude)
                    continue;

                lastTimeStamp = frameTimeStamp;

                if (pmonitor != null) {
                    pmonitor.setNote(I18n.textf("Exporting frame %d", i + 1));
                    pmonitor.setProgress(i);
                }

                File frmFile = saveFrame(outFolder, frame);

                imgWriter.write(frmFile.getName() + "\n");

                navWriter.write(frmFile.getName() + "," + pos.getPosition().getLatitudeDegs() + ","
                        + pos.getPosition().getLongitudeDegs() + "," + pos.getDepth() + "," + pos.getAltitude() + ","
                        + Math.toDegrees(pos.getYaw()) + "," + Math.toDegrees(pos.getRoll()) + ","
                        + Math.toDegrees(pos.getPitch()) + ",1,0\n");
            }

            navWriter.close();
            imgWriter.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
        return "Done.";
    }
}
