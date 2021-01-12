/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
import java.util.List;
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
        else if(OsInfo.getFamily() == Family.WINDOWS){
            boolean fail = false;
            if(OsInfo.getDataModel() == DataModel.B64) {
                try {
                    String libpath = System.getProperty("java.library.path");
                    libpath = libpath + ";C:\\opencv4.40-x64_86\\x64";
                    System.setProperty("java.library.path",libpath);
                }
                catch (Exception e) {
                    System.err.println("Add OpenCv path to java.library.path: " + e);
                    fail = true;
                }

                if(!fail) {
                    File path = new File("C:\\opencv4.40-x64_86\\x64");
                    List<String> libOpencvDll = Arrays.asList("opencv_videoio_ffmpeg440_64.dll",
                            "libopencv_core440.dll", "libopencv_imgproc440.dll", "libopencv_dnn440.dll",
                            "libopencv_flann440.dll", "libopencv_imgcodecs440.dll", "libopencv_ml440.dll",
                            "libopencv_photo440.dll", "libopencv_videoio440.dll", "libopencv_highgui440.dll",
                            "libopencv_features2d440.dll", "libopencv_calib3d440.dll", "libopencv_objdetect440.dll",
                            "libopencv_stitching440.dll", "libopencv_video440.dll", "libopencv_gapi440.dll",
                            "libopencv_java440.dll");
                    if (path.exists()) {
                        try {
                            for (String lib : libOpencvDll) {
                                System.load("C:\\opencv4.40-x64_86\\x64\\" + lib);
                                NeptusLog.pub().info("OpenCv - Load DLL: " + lib);
                            }
                            resultState = true;
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error("Opencv not found - " + e.getMessage());
                            resultState = false;
                        }
                        catch (Error e) {
                            NeptusLog.pub().error("Opencv not found - " + e.getMessage());
                            resultState = false;
                        }
                    }
                    else {
                        NeptusLog.pub().error("Opencv path not found");
                        resultState = false;
                    }
                }
            }
        }
        else {
            NeptusLog.pub().error("Only compatible with x64 architecture");
            resultState = false;
        }

        if (!resultState) {
            if (OsInfo.getFamily() == Family.UNIX)
                NeptusLog.pub().error("Opencv not found - please install OpenCv 4.4 and dependencies at https://www.lsts.pt/bin/opencv/v4.4.0-x64_x86/deb/");
            else if(OsInfo.getFamily() == Family.WINDOWS)
                NeptusLog.pub().error("Opencv not found - please install OpenCv 4.4 and dependencies at https://www.lsts.pt/bin/opencv/v4.4.0-x64_x86/win-x64_86/");
        }

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
