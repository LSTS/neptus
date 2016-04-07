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
import java.io.FilenameFilter;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.platform.OsInfo;
import pt.lsts.neptus.platform.OsInfo.DataModel;
import pt.lsts.neptus.platform.OsInfo.Family;

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
        if (resultState != null)
            return resultState;

        resultState = false;

        if (OsInfo.getFamily() == Family.UNIX) {
            File path = new File("/usr/lib/jni");
            String libOpencv = "";
            String[] children = path.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    boolean ret = name.toLowerCase().startsWith("libopencv_java24");
                    ret = ret && name.toLowerCase().endsWith(".so");
                    return ret;
                }
            });
            if (children.length > 0) {
                String filename = children[0];
                libOpencv = filename.toString().replaceAll("lib", "").replaceAll(".so", "");

                try {
                    System.loadLibrary(libOpencv);
                    resultState = true;
                    return true;
                }
                catch (Exception e) {
                    NeptusLog.pub().error("Opencv not found - retrying - " + e.getMessage());
                }
                catch (Error e) {
                    NeptusLog.pub().error("Opencv not found - retrying - " + e.getMessage());
                }
            }
            else {
                NeptusLog.pub().error("Opencv not found - retrying");
            }
        }
        
        // If we are here is not loaded yet
        try {
            System.loadLibrary("opencv_java2411");
            System.loadLibrary("libopencv_core2411");
            System.loadLibrary("libopencv_highgui2411");
            try {
                System.loadLibrary("opencv_ffmpeg2411"+ (OsInfo.getDataModel() == DataModel.B64 ? "_64" : ""));
            }
            catch (Exception e1) {
                System.loadLibrary("opencv_ffmpeg2411");
            }
            catch (Error e1) {
                System.loadLibrary("opencv_ffmpeg2411");
            }
            resultState = true;
        }
        catch (Exception e) {
            NeptusLog.pub().error("Opencv not found - please install libopencv2.4-jni and dependencies - " + e.getMessage());
            resultState = false;
        }
        catch (Error e) {
            NeptusLog.pub().error("Opencv not found - please install libopencv2.4-jni and dependencies - " + e.getMessage());
            resultState = false;
        }
        
        return resultState;
    }
}
