package pt.lsts.neptus.hyperspectral;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.apache.commons.collections15.buffer.CircularFifoBuffer;
import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.commons.lang.ArrayUtils;
import org.jzy3d.maths.Array;

import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;

import java.awt.image.DataBufferByte;

/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsmarques
 * 13 May 2015
 */

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "HyperSpectral Data Viewer", author = "tsmarques", version = "0.1")
@LayerPriority(priority = 40)
public class HyperspectralViewer extends ConsoleLayer {
    public static final int MIN_FREQ = 0;
    public static final int MAX_FREQ = 640; /* also frame's width */
    /* frames will be FRAME_WIDTH x MAX_FREQ px */
    public static final int FRAME_HEIGHT = 250;
    private static final float FRAME_OPACITY = 0.9f;
    /* draw frames with opacity */
    private final AlphaComposite alcom = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, FRAME_OPACITY);
    
    private ConsoleLayout console;
    
    
    
    /* testing */
    private BufferedImage dataDisplay; /* image currently being displayed */
    private Queue<BufferedImage> frames;
    private boolean framesLoaded = false;
    private int selectedWavelength = 320; /* column to crop from test data */
    
    BufferedImage i = null;

    public HyperspectralViewer() {
        this.console = getConsole();
        initDisplayedImage();
                
        /* testing */
        frames = loadFrames(selectedWavelength + "/"); /* load bmps */
    }
    
    private void initDisplayedImage() {
        dataDisplay = new BufferedImage(MAX_FREQ, FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics g = dataDisplay.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, MAX_FREQ, FRAME_HEIGHT);
        g.dispose();
    }
    
    private void updateDisplay(BufferedImage newFrame) {       
        /* remove oldest frame */
        dataDisplay = dataDisplay.getSubimage(1, 0, MAX_FREQ - 1, FRAME_HEIGHT);
        dataDisplay = joinBufferedImage(dataDisplay, newFrame);
    }
    
    private static BufferedImage joinBufferedImage(BufferedImage img1,BufferedImage img2) {

        //create a new buffer and draw two image into the new image
        BufferedImage newImage = new BufferedImage(MAX_FREQ, FRAME_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();

        g2.drawImage(img1, null, 0, 0);
        g2.drawImage(img2, null, img1.getWidth(), 0);
        g2.dispose();
        
        return newImage;
    }
    
    private Queue<BufferedImage> loadFrames(String path) {
        File dir = new File("../hyperspec-data/" + path);
        File[] tmpFrames = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".bmp");
            }            
        });
        
        File frames[] = new File[tmpFrames.length];
       
        
        for(int i = 0; i < frames.length ; i++) {
            int filepos = Integer.parseInt(tmpFrames[i].getName().split(".bmp")[0]);
            frames[filepos] = tmpFrames[i];
        }
        
        Queue<BufferedImage> framesList = new LinkedList<>();
        
        for(int i = 0; i < frames.length; i++) {
            try {
                framesList.add((BufferedImage) ImageIO.read(frames[i]));
            }
            catch (IOException e) { e.printStackTrace(); }
        }

        framesLoaded = true;
        return framesList;
    }
    
    /* Given some frames from the test data,
       remove everything but the column
       correspondent to the selected wavelength */
    private void cropFrames(int wave, BufferedImage[] frames) {       
        try {
            for(int i = 0; i < frames.length; i++) {
                BufferedImage cropped = frames[i].getSubimage(wave - 1, 0, 1, 250);
                ImageIO.write(cropped, "bmp", new File("../hyperspec-data/" + wave + "/" + i + ".bmp"));   
            }
        }
        catch (IOException e) { e.printStackTrace(); }
    }
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if(!framesLoaded)
            return;
        
        int posY = (renderer.getHeight() - FRAME_HEIGHT) / 2;
        
        g.setComposite(alcom);
        g.drawImage(dataDisplay, 0, posY, null);
    }

    
    /* Simulate the reception of a frame */
    @Periodic(millisBetweenUpdates = 500)
    public void simReceivedFrame() {
        if(!framesLoaded)
            return;
        
        BufferedImage newFrame = frames.poll();
        updateDisplay(newFrame);
        frames.offer(newFrame); /* keep a circular queue */
    }
    
    @Override
    public void initLayer() {

    }
    
    @Override
    public void cleanLayer() {
        
    }

    @Override
    public boolean userControlsOpacity() { return false; }
}
