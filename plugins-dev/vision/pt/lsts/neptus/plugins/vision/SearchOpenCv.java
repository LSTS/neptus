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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Pedro Gonçalves
 * Nov 19, 2015
 */
package pt.lsts.neptus.plugins.vision;

import java.io.File;

import pt.lsts.neptus.NeptusLog;

/** 
 * @author pedrog
 * @version 1.0
 * @category OpenCV-Vision
 *
 */
public class SearchOpenCv {
    
    public boolean SearchJni(File path) {
        boolean result = false;
        String libOpencv = new String();
        String[] children = path.list();
        if (children == null) {
            //NeptusLog.pub().error("/usr/lib/jni not exist to search Opencv jni");
        }
        else {
           for (int i = 0; i < children.length; i++) {
              String filename = children[i];
              if(filename.equalsIgnoreCase("libopencv_java240.so"))
                  libOpencv = "opencv_java240";
              else if(filename.equalsIgnoreCase("libopencv_java241.so"))
                  libOpencv = "opencv_java241";
              else if(filename.equalsIgnoreCase("libopencv_java242.so"))
                  libOpencv = "opencv_java242";
              else if(filename.equalsIgnoreCase("libopencv_java243.so"))
                  libOpencv = "opencv_java243";
              else if(filename.equalsIgnoreCase("libopencv_java244.so"))
                  libOpencv = "opencv_java244";
              else if(filename.equalsIgnoreCase("libopencv_java245.so"))
                  libOpencv = "opencv_java245";
              else if(filename.equalsIgnoreCase("libopencv_java246.so"))
                  libOpencv = "opencv_java246";
              else if(filename.equalsIgnoreCase("libopencv_java247.so"))
                  libOpencv = "opencv_java247";
              else if(filename.equalsIgnoreCase("libopencv_java248.so"))
                  libOpencv = "opencv_java248";
              else if(filename.equalsIgnoreCase("libopencv_java249.so"))
                  libOpencv = "opencv_java249";
              else if(filename.equalsIgnoreCase("libopencv_java2410.so"))
                  libOpencv = "opencv_java2410";
              else if(filename.equalsIgnoreCase("libopencv_java2411.so"))
                  libOpencv = "opencv_java2411";
              else if(filename.equalsIgnoreCase("libopencv_java2412.so"))
                  libOpencv = "opencv_java2412";
           }
        }
                
        try {
            System.loadLibrary(libOpencv);
            return true;
        }
        catch (Exception e) {
            try {
                System.loadLibrary("opencv_java2411");
                System.loadLibrary("libopencv_core2411");
                System.loadLibrary("libopencv_highgui2411");
                try {
                    System.loadLibrary("opencv_ffmpeg2411_64");
                    }
                catch (Exception e1) {
                    System.loadLibrary("opencv_ffmpeg2411");
                }
                catch (Error e1) {
                    System.loadLibrary("opencv_ffmpeg2411");
                }
                result = true;
            }
            catch (Exception e1) {
                NeptusLog.pub().error("Opencv not found - please install libopencv2.4-jni and dependencies");
                result = false;
            }
            return result;
        }
        catch (Error e) {
            try {
                System.loadLibrary("opencv_java2411");
                System.loadLibrary("libopencv_core2411");
                System.loadLibrary("libopencv_highgui2411");
                try {
                    System.loadLibrary("opencv_ffmpeg2411_64");
                    }
                catch (Exception e1) {
                    System.loadLibrary("opencv_ffmpeg2411");
                }
                catch (Error e1) {
                    System.loadLibrary("opencv_ffmpeg2411");
                }
                result = true;
            }
            catch (Error e1) {
                NeptusLog.pub().error("Opencv not found - please install libopencv2.4-jni and dependencies");
                result = false;
            }
            return result;
        }
    }
}
