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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Pedro Gonçalves
 */

package pt.lsts.neptus.plugins.videostream;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.opencv.core.Size;
import pt.lsts.neptus.util.conf.ConfigFetch;

/** 
 * @author pedrog
 * @author Pedro Costa
 * @version 1.0
 * @category OpenCV-Vision
 *
 */
public class UtilVideoStream {
    
    private UtilVideoStream() {
    }
    
    public static String[][] readIpUrl(File nameFile) {
        BufferedReader br = null;
        String lineFile;
        String[] splits;
        String[] emptyData = {"Select Device", "", ""};
        ArrayList<String[]> dataIpCam = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(nameFile));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        dataIpCam.add(emptyData);
        try {
            while ((lineFile = br.readLine()) != null) {
                if(!lineFile.isEmpty()) {
                    splits = lineFile.split("#");                  
                    if(splits.length == 3)
                        dataIpCam.add(splits);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        try {
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return dataIpCam.toArray(new String[dataIpCam.size()][0]);
    }

    public static void removeLineFromFile(int lineToRemove, String fileName) {
        File confIni = new File(fileName);
        File tempFile = new File("/tmp/urlIp.ini-temp");

        String currentLine;

        // Can't remove the Select Device line
        if (lineToRemove == 0) {
            return;
        }

        // The file doesn't include the Select Device line so we need to
        // decrease the line number to match the lines in the file
        lineToRemove--;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(confIni));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
            int lineNumber = 0;
            while((currentLine = reader.readLine()) != null) {
                if (lineToRemove != lineNumber) {
                    writer.write(currentLine.trim() + System.getProperty("line.separator"));
                }
                lineNumber++;
            }
            writer.close();
            reader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        tempFile.renameTo(confIni);
    }
    
    public static boolean pingIp(String host) {
        boolean ping = false;
        boolean ping2 = false;
        try {
            String[] cmd;
            if (System.getProperty("os.name").startsWith("Windows")) {
                // For Windows
                cmd = new String[]{
                        "ping",
                        "-n",
                        "1",
                        host};
            }
            else {
                // For Linux and OSX
                cmd = new String[]{
                        "ping",
                        "-c",
                        "1",
                        host};
            }
            Process myProcess = Runtime.getRuntime().exec(cmd);
            try {
                myProcess.waitFor();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            InetAddress hostip = InetAddress.getByName(host);
            ping2 = hostip.isReachable(1000);
            
            if (myProcess.exitValue() == 0 && ping2)
                ping = true;
            else
                ping = false;
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        } // Ping doesnt work
        
        return ping;
    }
    
    public static BufferedImage resizeBufferedImage(BufferedImage img, Size size) {
        if(size != null && size.width != 0 && size.height != 0){
            BufferedImage dimg = new BufferedImage((int)size.width, (int)size.height, img.getType());
            Graphics2D g2d = dimg.createGraphics();
            g2d.drawImage(img.getScaledInstance((int)size.width, (int)size.height, Image.SCALE_SMOOTH), 0, 0, null);
            g2d.dispose();
            return dimg;
        }
        else {
            //NeptusLog.pub().warn(I18n.text("Size in resizeBufferedImage must be != NULL and not 0"));
            return null;
        }
    }
}