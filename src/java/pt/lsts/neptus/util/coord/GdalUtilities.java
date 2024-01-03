/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * 26/08/2016
 */
package pt.lsts.neptus.util.coord;

import java.io.File;
import java.io.IOException;

import org.gdal.gdal.gdal;

/**
 * @author zp
 *
 */
public class GdalUtilities {
    
    private static boolean loaded = false;
    
    public static synchronized void loadNativeLibraries() {
        try {
            if (!loaded) {
                String libFolder = getPlatformPath("libJNI/gdal");
                loadNative(new File(libFolder));
                gdal.AllRegister();
            }
            loaded = true;
        }
        catch (Error e) {
            e.printStackTrace();
        }        
    }

    private static void loadNative(File nativeLibsFolder) {
        String[] libs = { "gdal", "gdalconstjni", "gdaljni", "ogrjni", "osrjni" };
        String platform = getPlatformPath("");

        for (String name : libs) {
            StringBuilder filename = new StringBuilder();
            if (platform.contains("win")) {
                filename.append(name);
                if (name.equals("gdal")) {
                    filename.append("19");
                }
                filename.append(".dll");
            }
            else {
                filename.append("lib");
                filename.append(name);
                filename.append(".so");
            }

            File path = null;
            try {
                path = new File(nativeLibsFolder.getCanonicalFile(), filename.toString());
                System.load(path.getAbsolutePath().toString());
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
            catch (UnsatisfiedLinkError e) {
                System.err.println("Native code library failed to load. " + e);
            }
            catch (SecurityException e) {
                e.printStackTrace();
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getPlatformPath(String base) {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();// x86 or x64
        // check os
        if (os.contains("windows")) {
            os = "win";
        }
        else if (os.contains("linux")) {
            os = "linux";
        }
        else {
            throw new IllegalArgumentException("os not supported");
        }
        // check arch 86 or 64
        if (arch.contains("86")) {
            arch = "x86";
        }
        else if (arch.contains("64")) {
            arch = "x64";
        }
        else {
            throw new IllegalArgumentException("arch not supported " + arch);
        }
        return base + "/" + os + "/" + arch;
    }
}
