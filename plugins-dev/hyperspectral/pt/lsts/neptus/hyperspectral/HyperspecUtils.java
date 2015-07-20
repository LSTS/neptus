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
 * Author: coop
 * 6 Jul 2015
 */
package pt.lsts.neptus.hyperspectral;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import javax.imageio.ImageIO;

import org.apache.commons.io.comparator.NameFileComparator;

import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;

/**
 * @author coop
 *
 */
public class HyperspecUtils {
    private static final String TEST_DATA_DIR = "./plugins-dev/hyperspectral/pt/lsts/neptus/hyperspectral/test-data/";
    
    public static BufferedImage getScaledImage(BufferedImage image, double scalex, double scaley) {
        double newW = scalex * image.getWidth();
        double newH = scaley * image.getHeight();
        BufferedImage scaledImage = new BufferedImage((int)newW, (int)newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) scaledImage.createGraphics();
        
        g2d.drawImage(image, 0, 0, (int)newW, (int)newH, null);
        g2d.dispose();
        
        return scaledImage;
    }
    
    
    public static BufferedImage initVerticalDisplay(int width, int height) {
        BufferedImage dataDisplay = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = dataDisplay.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        
        return dataDisplay;
    }
    
    public static BufferedImage updateVerticalDisplay(BufferedImage dataDisplay, byte[] frameBytes, int width, int height) {
        BufferedImage newFrame = HyperspecUtils.rawToBuffImage(frameBytes);
        
        if(newFrame == null)
            return null;
        
        /* remove oldest frame */
        dataDisplay = dataDisplay.getSubimage(1, 0, width - 1, height);
        
        return joinBufferedImage(dataDisplay, newFrame, width, height);
    }
    
    
    public static BufferedImage joinBufferedImage(BufferedImage img1,BufferedImage img2, int newWidth, int newHeight) {

        //create a new buffer and draw two image into the new image
        BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = newImage.createGraphics();

        g2.drawImage(img1, null, 0, 0);
        g2.drawImage(img2, null, img1.getWidth(), 0);
        g2.dispose();
        
        return newImage;
    }
    
    public static BufferedImage rawToBuffImage(byte[] raw) {
        BufferedImage data = new BufferedImage(1, raw.length, BufferedImage.TYPE_3BYTE_BGR);
        
        ColorMap cp = ColorMapFactory.createGrayScaleColorMap();
        for(int i = 0; i < raw.length; i++)
            data.setRGB(0, i, cp.getColor((raw[i] & 0xFF) / 255.0).getRGB());
        
        return data;
    }
    
    public static void saveFrame(BufferedImage frame, String path, String name, String extension) {
        try {
            System.out.println("WRITING TO DISK");
            File f = new File(path + name + "." + extension);
            ImageIO.write(frame, extension, f);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /* for testing */
    /* load the frames columns */
    public static Queue<byte[]> loadFrames(String path) {
        File dir = new File(TEST_DATA_DIR + path);
        File[] tmpFrames = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".bmp");
            }            
        });
        
        if(!dir.exists())
            return new LinkedList<byte[]>();
        
        File frames[] = new File[tmpFrames.length];
       
        
        for(int i = 0; i < frames.length ; i++) {
            int filepos = Integer.parseInt(tmpFrames[i].getName().split(".bmp")[0]);
            frames[filepos] = tmpFrames[i];
        }
        
        Queue<byte[]> framesList = new LinkedList<>();
        
        for(int i = 0; i < frames.length; i++) {
            try {
                framesList.add(Files.readAllBytes(frames[i].toPath()));
            }
            catch (IOException e) { e.printStackTrace(); }
        }

        return framesList;
    }
    
    /* Given a path for the test data,
       remove everything but the column
       correspondent to the selected wavelength 
       and save them in a folder named after the wavelength
    */
    public static void cropFrames(int wave, String path) {
        File dir = new File(path);
        File[] imgFiles = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".bmp");
            }            
        });
        
        Arrays.sort(imgFiles, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);
        try {
            for(int i = 0; i < imgFiles.length; i++) {
                BufferedImage frame = (BufferedImage) ImageIO.read(imgFiles[i]);
                
                BufferedImage cropped = frame.getSubimage(wave - 1, 0, 1, 250);
                ImageIO.write(cropped, "bmp", new File(TEST_DATA_DIR + wave + "/" + i + ".bmp"));   
            }
        }
        catch (IOException e) { e.printStackTrace(); }
    }
}
