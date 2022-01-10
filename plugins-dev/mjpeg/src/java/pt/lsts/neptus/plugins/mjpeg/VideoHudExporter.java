/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 10/03/2020
 */
package pt.lsts.neptus.plugins.mjpeg;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.ProgressMonitor;

import org.imgscalr.Scalr;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.api.LsfTreeSet;
import pt.lsts.neptus.mra.exporters.BatchMraExporter;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.replay.MraVehiclePosHud;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.VideoCreator;

/**
 * @author zp
 *
 */
@PluginDescription(name="Video HUD", description="Exports the video with HUD display.")
public class VideoHudExporter implements MRAExporter {

    private CorrectedPosition positions;
    private FrameDecoderMotionJPEG frameDecoder;
    private VideoCreator creator = null;
    private MraVehiclePosHud hud;
    
    @NeptusProperty(name = "Sharpen filter")
    boolean applySharpen = false;
    
    @NeptusProperty(name = "Contrast filter")
    boolean applyContrast = false;
    
    @NeptusProperty(name = "Brighten filter")
    boolean applyBrighten = false;
    
    @NeptusProperty(name = "Grayscale filter")
    boolean applyGraysclae = false;
    
    protected BufferedImageOp contrastOp = ImageUtils.contrastOp();
    protected BufferedImageOp sharpenOp = ImageUtils.sharpenOp();
    protected BufferedImageOp brightenOp = ImageUtils.brightenOp(1.2f, 0);
    protected BufferedImageOp grayscaleOp = ImageUtils.grayscaleOp();
    
    protected SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    
    public VideoHudExporter(IMraLogGroup source) {
        this.positions = new CorrectedPosition(source);
        this.frameDecoder = new FrameDecoderMotionJPEG();    
        hud = new MraVehiclePosHud(source, 180, 180);
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return frameDecoder.folderContainsFrames(source.getDir());
    }
    
    public static BufferedImage toBufferedImage(Image img)
    {
        if (img instanceof BufferedImage)
        {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }
    
    protected void drawLegend(SystemPositionAndAttitude state, Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        if (state == null)
            return;
        long timeUTC = state.getTime();
        String depth = GuiUtils.getNeptusDecimalFormat(1).format(state.getPosition().getDepth());
        String alt = Double.isNaN(state.getW())? "N/A" : GuiUtils.getNeptusDecimalFormat(1).format(state.getAltitude());
        String lat = GuiUtils.getNeptusDecimalFormat(5).format(state.getPosition().getLatitudeDegs());
        String lon = GuiUtils.getNeptusDecimalFormat(5).format(state.getPosition().getLongitudeDegs());
        String yaw = GuiUtils.getNeptusDecimalFormat(1).format(Math.toDegrees(state.getYaw()));
        
        
        Vector<String> details = new Vector<>();
        details.add(I18n.text("Time")+": "+sdf.format(new Date(timeUTC)));
        details.add(I18n.text("Latitude")+": "+lat);
        details.add(I18n.text("Longitude")+": "+lon);
        details.add(I18n.text("Depth")+": "+depth);
        details.add(I18n.text("Altitude")+": "+alt);
        details.add(I18n.text("Heading")+": "+yaw);
        
        
        g.setColor(new Color(32,32,64, 128));
        g.fill(new Rectangle2D.Double(10, 10, 180, 100));
        g.setColor(Color.white);
        g.translate(10, 10);
        for (int y = 15; !details.isEmpty(); y += 15) {
            g.drawString(details.firstElement(), 5, y);
            details.remove(0);
        }
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        
        if (!MRAProperties.batchMode && !GraphicsEnvironment.isHeadless())
            PluginUtils.editPluginProperties(this, true);
        
        frameDecoder.load(source.getDir());
        int nFrames = frameDecoder.getFrameCount();  
        VideoFrame first = frameDecoder.next();
        int width = first.getImage().getWidth(null), height =  first.getImage().getHeight(null);
        long time = 0;
        
        try {
            creator = new VideoCreator(new File(source.getDir(), "ProcessedVideo.mp4"), width, height);           
        }
        catch (Exception e1) {
            e1.printStackTrace();         
        }
        
        ArrayList<BufferedImageOp> ops = new ArrayList<BufferedImageOp>();
        if (applyBrighten)
            ops.add(brightenOp);
        if (applyContrast)
            ops.add(contrastOp);
        if (applyGraysclae)
            ops.add(grayscaleOp);
        if (applySharpen)
            ops.add(sharpenOp);
        if (ops.isEmpty())
            ops.add(ImageUtils.identityOp());
        
        pmonitor.setMaximum(nFrames);
        pmonitor.setProgress(0);
        for (int i = 0; i < nFrames; i++) {
            frameDecoder.seekToFrame(i); 
            VideoFrame frm = frameDecoder.getCurrentFrame();
            BufferedImage img = toBufferedImage(frm.getImage());
            img = Scalr.apply(img, ops.toArray(new BufferedImageOp[0]));                        
            BufferedImage overlay = hud.getImage(frm.getTimeStamp()/1000.0);
            SystemPositionAndAttitude pose = positions.getPosition(frm.getTimeStamp() / 1000.0);
            Graphics2D g2d = (Graphics2D) img.getGraphics();
            g2d.drawImage(overlay, 10, 110, null);
            drawLegend(pose, g2d);
            time += (1000/frameDecoder.getFrameRate());
            creator.addFrame(img, time);
            pmonitor.setProgress(i);
            if (pmonitor.isCanceled()) {
                break;
            }
        }
        
        creator.closeStreams();
        pmonitor.close();
        return "Finished.";
    }
    
    public static void main(String[] args) {
        MRAProperties.batchMode = true;
        GuiUtils.setLookAndFeelNimbus();
        if (args.length == 0)
            BatchMraExporter.apply(VideoHudExporter.class);
        else {
            File[] roots = new File[args.length];
            for (int i = 0; i < roots.length; i++)
                roots[i] = new File(args[i]);

            LsfTreeSet set = new LsfTreeSet(roots);
            BatchMraExporter.apply(set, VideoHudExporter.class);
        }
    }

}
