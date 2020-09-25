/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Nov 19, 2015
 */
package pt.lsts.neptus.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.lang.reflect.Field;

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
            File path = new File("/usr/share/java/opencv4/");
            String libOpencv = "";
            if(path.exists())
                try {
                    addLibraryPath("/usr/share/java/opencv4/");
                }
                catch (Exception e1) {
                    NeptusLog.pub().error("Opencv path not found - " + e1.getMessage());
                    //e1.printStackTrace();
                }

            String[] children = !path.exists() ? new String[0] : path.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    boolean ret = name.toLowerCase().startsWith("libopencv_java440");
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
                    NeptusLog.pub().info("Opencv found: "+libOpencv);
                    return true;
                }
                catch (Exception e) {
                    NeptusLog.pub().error("Opencv not found - " + e.getMessage());
                }
                catch (Error e) {
                    NeptusLog.pub().error("Opencv not found - " + e.getMessage());
                }
            }
        }
        else {
         // If we are here is not loaded yet
            try {
                System.loadLibrary("opencv_java440");
                System.loadLibrary("libopencv_core440");
                System.loadLibrary("libopencv_highgui440");
                try {
                    System.loadLibrary("opencv_ffmpeg440"+ (OsInfo.getDataModel() == DataModel.B64 ? "_64" : ""));
                }
                catch (Exception e1) {
                    System.loadLibrary("opencv_ffmpeg440");
                }
                catch (Error e1) {
                    System.loadLibrary("opencv_ffmpeg440");
                }
                resultState = true;
            }
            catch (Exception e) {
                resultState = false;
                NeptusLog.pub().error("Opencv not found - " + e.getMessage());
            }
            catch (Error e) {
                resultState = false;
                NeptusLog.pub().error("Opencv not found - " + e.getMessage());
            }
        }

        if (!resultState)
            NeptusLog.pub().error("Opencv not found - please install OpenCv 4.4 and dependencies. www.adicionar_url.pt");
            
        return resultState;
    }

    public static void addLibraryPath(String pathToAdd) throws Exception{
        final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);

        //get array of paths
        final String[] paths = (String[])usrPathsField.get(null);

        //check if the path to add is already present
        for(String path : paths) {
            if(path.equals(pathToAdd)) {
                return;
            }
        }
        //add the new path
        final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length-1] = pathToAdd;
        usrPathsField.set(null, newPaths);
    }
}
