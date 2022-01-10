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
package pt.lsts.neptus.plugins.mraplots;

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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.ProgressMonitor;

import org.imgscalr.Scalr;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
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
import pt.lsts.neptus.plugins.mraplots.MraPhotosVisualization;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.VideoCreator;

/**
 * @author zp
 *
 */
@PluginDescription(name="Photos HUD", description="Exports the video with HUD display.")
public class PhotosHudExporter implements MRAExporter {

    private CorrectedPosition positions;
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
    protected LinkedHashMap<File, SystemPositionAndAttitude> states = null;
    
    
    protected SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    
    public double timestampOf(File f) {
        return Double.parseDouble(f.getName().substring(0, f.getName().lastIndexOf('.')));
    }
    
    protected void loadStates(IMraLogGroup source) {
        LsfIndex index = source.getLsfIndex();
        File[] files = MraPhotosVisualization.listPhotos(source.getFile("Photos"));

        int lastIndex = 0;
        int stateId = index.getDefinitions().getMessageId("EstimatedState");
        int lastBDistanceIndex = 0;
        int bdistId = index.getDefinitions().getMessageId("BottomDistance");
        int dvlId = index.getEntityId("DVL");

        states = new LinkedHashMap<>();
        for (int i = 0; i < files.length; i++) {
            int msgIndex = index.getMessageAtOrAfer(stateId, 0xFF, lastIndex, timestampOf(files[i]));

            if (msgIndex == -1) {
                states.put(files[i], null);
            }
            else {
                lastIndex = msgIndex;
                IMCMessage m = index.getMessage(msgIndex);
                LocationType loc = new LocationType(Math.toDegrees(m.getDouble("lat")), Math.toDegrees(m
                        .getDouble("lon")));
                loc.setDepth(m.getDouble("depth"));
                loc.translatePosition(m.getDouble("x"), m.getDouble("y"), 0);
                loc.convertToAbsoluteLatLonDepth();
                SystemPositionAndAttitude state = new SystemPositionAndAttitude(loc, m.getDouble("phi"),
                        m.getDouble("theta"), m.getDouble("psi"));
                if (m.getTypeOf("alt") == null) {
                    state.setW(Double.NaN);
                    int bdIndex = index.getMessageAtOrAfer(bdistId, dvlId, lastBDistanceIndex, timestampOf(files[i]));

                    if (bdIndex != -1) {
                        state.setW(index.getMessage(bdIndex).getDouble("value"));
                        lastBDistanceIndex = bdIndex;
                    }
                }
                else {
                    state.setW(m.getDouble("alt"));
                }

                state.setU(m.getDouble("u"));
                states.put(files[i], state);
            }
        }
    }
    
    
    
    public PhotosHudExporter(IMraLogGroup source) {
        this.positions = new CorrectedPosition(source);
        hud = new MraVehiclePosHud(source, 180, 180);        
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getFile("Photos") != null && source.getFile("Photos").isDirectory();
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
        
        loadStates(source);
        
        if (!MRAProperties.batchMode && !GraphicsEnvironment.isHeadless())
            PluginUtils.editPluginProperties(this, true);
        
        int nFrames = states.keySet().size();
        
        File firstFile = states.keySet().iterator().next();
        Image firstImage = null;
        try {
            firstImage = ImageIO.read(firstFile);
        }
        catch (Exception e) {
            return "unable to load first image: "+firstFile.getPath();
        }
        
        double firstTimestamp = timestampOf(firstFile);
        
        int width = firstImage.getWidth(null), height =  firstImage.getHeight(null);
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
        int count = 0;
        for(Entry<File, SystemPositionAndAttitude> state : states.entrySet()) {
            double tstamp = timestampOf(state.getKey());
            BufferedImage img;
            try {
                img = ImageIO.read(state.getKey());
                img = Scalr.apply(img, ops.toArray(new BufferedImageOp[0]));                        
                BufferedImage overlay = hud.getImage(tstamp);
                SystemPositionAndAttitude pose = positions.getPosition(tstamp);
                Graphics2D g2d = (Graphics2D) img.getGraphics();
                g2d.drawImage(overlay, 10, 110, null);
                drawLegend(pose, g2d);
                time = (long)((tstamp - firstTimestamp) * 1000.0);
                creator.addFrame(img, time);
            }
            catch (IOException e) {
                e.printStackTrace();
            }            
            
            pmonitor.setProgress(++count);
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
            BatchMraExporter.apply(PhotosHudExporter.class);
        else {
            File[] roots = new File[args.length];
            for (int i = 0; i < roots.length; i++)
                roots[i] = new File(args[i]);

            LsfTreeSet set = new LsfTreeSet(roots);
            BatchMraExporter.apply(set, PhotosHudExporter.class);
        }
    }

}
