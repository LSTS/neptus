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

/**
 * @author coop
 *
 */
public class HyperspecUtils {
    private static final String TEST_DATA_DIR = "./plugins-dev/hyperspectral/pt/lsts/neptus/hyperspectral/test-data/";
    
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
