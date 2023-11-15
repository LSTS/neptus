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
 * Nov 19, 2015
 */
package pt.lsts.neptus.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.reflect.Field;
import java.util.stream.Collectors;

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
            if (loadForUnix())
                return true;
        }
        else if(OsInfo.getFamily() == Family.WINDOWS) {
            loadForWindows();
        }
        else {
            NeptusLog.pub().error("Only compatible with x64 architecture");
            resultState = false;
        }

        if (!resultState) {
            if (OsInfo.getFamily() == Family.UNIX) {
                NeptusLog.pub().error("OpenCV not found - please install OpenCV 4.4 and " +
                        "dependencies at https://www.lsts.pt/bin/opencv/v4.4.0-x64_x86/deb/");
            }
            else if(OsInfo.getFamily() == Family.WINDOWS) {
                NeptusLog.pub().error("OpenCV not found - please install OpenCV 4.4 and " +
                        "dependencies at https://www.lsts.pt/bin/opencv/v4.4.0-x64_x86/win-x64_86/");
            }
        }

        return resultState;
    }

    private static boolean loadForUnix() {
        List<String> pathNameList = Arrays.asList(
                "/usr/share/java/opencv4/",
                "/usr/lib/jni/");
        for (String pathStr : pathNameList) {
            File path = new File(pathStr);
            String libOpencv = "";
            if(path.exists()) {
                try {
                    addLibraryPath(pathStr);
                }
                catch (Exception e1) {
                    NeptusLog.pub().error("OpenCV path not found - " + e1.getMessage());
                }
            }
            String[] children = !path.exists() ? new String[0] : path.list((dir, name) -> {
                boolean ret = name.toLowerCase().startsWith("libopencv_java4");
                ret = ret && name.toLowerCase().endsWith(".so");
                return ret;
            });
            assert children != null;
            if (children.length > 0) {
                String filename = children[0];
                libOpencv = filename.replaceAll("lib", "").replaceAll(".so", "");
                try {
                    System.loadLibrary(libOpencv);
                    resultState = true;
                    NeptusLog.pub().info("OpenCV found: "+libOpencv);
                    return true;
                }
                catch (Exception | Error e) {
                    NeptusLog.pub().error("OpenCV not found - " + e.getMessage());
                }
            }
        }
        return false;
    }

    private static void loadForWindows() {
        boolean fail = false;
        if(OsInfo.getDataModel() == DataModel.B64) {
            List<String> pathNameList = Arrays.asList(
                    "C:\\Program Files\\opencv\\build\\x64",
                    "C:\\Program Files\\opencv\\x64",
                    "C:\\opencv\\x64",
                    "C:\\opencv4.40-x64_86\\x64");

            for (String pathStr : pathNameList) {
                File path = new File(pathStr);
                if (!path.exists()) {
                    continue;
                }

                List<String> pathsStrToAdd = new ArrayList<>();
                List<String> libsStrToLoad = new ArrayList<>();
                pathsStrToAdd.add(pathStr);
                if (pathStr.contains("\\build\\")) {
                    if (new File(pathStr + "\\..\\java").exists()) {
                        pathsStrToAdd.add(pathStr + "\\..\\java");
                    }
                    if (new File(pathStr + "\\..\\java\\x64").exists()) {
                        pathsStrToAdd.add(pathStr + "\\..\\java\\x64");
                    }
                    File[] vcPossibleFolders = path.listFiles((dir, s) -> s.startsWith("vc") && s.length() == 4);
                    if (vcPossibleFolders != null) {
                        Arrays.stream(vcPossibleFolders).findFirst()
                                .ifPresent(basePathVC -> pathsStrToAdd.add(
                                        new File(basePathVC, "bin").getAbsolutePath()));
                    }
                }
                for (String pathFld : pathsStrToAdd) {
                    File[] libsToAdd = new File(pathFld).listFiles((file, s) -> s.endsWith(".dll"));
                    if (libsToAdd != null) {
                        List<String> orderedLibs = Arrays.stream(libsToAdd).sorted((o1, o2) -> {
                            if (o1.getName().toLowerCase().contains("msmf")
                                    && o2.getName().toLowerCase().contains("world")) {
                                return 1;
                            } else if (o2.getName().toLowerCase().contains("msmf")
                                    && o1.getName().toLowerCase().contains("world")) {
                                return -1;
                            }

                            return o1.compareTo(o2);
                        }).map(File::getAbsolutePath).collect(Collectors.toList());
                        libsStrToLoad.addAll(orderedLibs);
                    }
                }

                try {
                    NeptusLog.pub().info("java.library.path >> " +  String.join(";", pathsStrToAdd));
                    String libpath = System.getProperty("java.library.path");
                    libpath = libpath + ";" + String.join(";", pathsStrToAdd);
                    System.setProperty("java.library.path", libpath);
                } catch (Exception e) {
                    NeptusLog.pub().error("Error adding OpenCV path to java.library.path: " + e);
                    fail = true;
                }

                // loading libs
                if(!fail) {
                    NeptusLog.pub().info("libs to load >> " +  String.join(";", libsStrToLoad));
                    for (String libPathStr : libsStrToLoad) {
                        File libPath = new File(libPathStr);
                        if (libPath.exists()) {
                            try {
                                System.load(libPathStr);
                                NeptusLog.pub().info("OpenCV - Load DLL: " + libPathStr);
                                resultState = true;
                            } catch (Exception | Error e) {
                                NeptusLog.pub().error("OpenCV not loaded correctly - " + e.getMessage());
                                //resultState = false;
                            }
                        }
                    }
                }

                if (resultState) {
                    break;
                }
            }
        }
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
