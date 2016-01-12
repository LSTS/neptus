/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.util;

import java.io.File;

import pt.lsts.neptus.NeptusLog;

/** 
 * @author pedrog
 * @version 1.0
 * @category OpenCV-Vision
 *
 */
public class SearchOpenCv {
    
    private static Boolean resultState = null;
    
    private SearchOpenCv() {
    }
    
    public synchronized static boolean searchJni() {
        File path = new File("/usr/lib/jni");
        if(resultState == null) {
            resultState = false;
            String libOpencv = new String();
            String[] children = path.list();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    String filename = children[i];
                    
                    if(filename.toLowerCase().startsWith("libopencv_java24") && filename.toLowerCase().endsWith(".so"))
                        libOpencv = filename.toString().replaceAll("lib", "").replaceAll(".so", "");
                }
            } 
            try {
                System.loadLibrary(libOpencv);
                resultState = true;
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
                    resultState = true;
                }
                catch (Exception e1) {
                    NeptusLog.pub().error("Opencv not found - please install libopencv2.4-jni and dependencies");
                    resultState = false;
                }
                return resultState;
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
                    resultState = true;
                }
                catch (Error e1) {
                    NeptusLog.pub().error("Opencv not found - please install libopencv2.4-jni and dependencies");
                    resultState = false;
                }
            }
        }
        
        return resultState;
    }
}
