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
 * Author: jqcorreia
 * Mar 28, 2013
 */
package pt.lsts.neptus.mra.api;

import java.io.File;
import java.io.FilenameFilter;

import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.i872.Imagenex872SidescanParser;
import pt.lsts.neptus.mra.importers.jsf.JsfSidescanParser;
import pt.lsts.neptus.mra.importers.sdf.SdfSidescanParser;
import pt.lsts.neptus.util.llf.LogUtils;

/**
 * @author jqcorreia
 *
 */
public class SidescanParserFactory {

    private static final String JSF_FILE = "Data.jsf";
    private static final String I872_FILE = "Data.872";

    private static String[] validSidescanFiles = {JSF_FILE, I872_FILE};

    static File dir;
    static File file;
    static IMraLogGroup source;

    public static SidescanParser build(IMraLogGroup log) {
        file = null;
        dir = log.getDir();
        source = log;

        return getParser();
    }

    public static SidescanParser build(File fileOrDir) {
        source = null;
        if(fileOrDir.isDirectory())
            dir = file;
        else {
            file = fileOrDir;
        }
        return getParser();
    }

    public static boolean existsSidescanParser(IMraLogGroup log) {
        for (String file : validSidescanFiles) {
            if (log.getFile(file) != null) {
                return true;
            }
        }

        if (countSDFFiles(log) > 0) {
            return true;
        }

        return false;
    }

    private static int countSDFFiles(IMraLogGroup log) {
        FilenameFilter sdfFilter = SDFFilter();
        File[] files = log.getDir().listFiles(sdfFilter);
        return files.length;
    }

    private static SidescanParser getParser() {
        if(file != null) {
            return null; //FIXME for now only directories are supported 
        }
        else if(dir != null) {

            file = new File(dir.getAbsolutePath()+"/"+JSF_FILE);
            if(file.exists()) {
                return new JsfSidescanParser(file);
            }
            file = new File(dir.getAbsolutePath()+"/"+I872_FILE);
            if(file.exists()) {
                return new Imagenex872SidescanParser(file);
            }
            FilenameFilter sdfFilter = SDFFilter();
            File[] files = source.getDir().listFiles(sdfFilter);
            if (files.length == 1)
                return new SdfSidescanParser(files[0]);
            else if (files.length > 1)
                return new SdfSidescanParser(files);
            

            // Next cases should be file = new File(...) and check for existence
            // TODO

            // Defaults to using IMC (in case of sidescan data existence)
            if(source != null) {
                if(LogUtils.hasIMCSidescan(source))
                    return new ImcSidescanParser(source);
                else if(source.getLog("SidescanPing") != null) { // Legacy IMC message. We still have a lot of data requests for this format, so be it...
                    return new LegacyImcSidescanParser(source);
                }
            }
        }
        return null;
    }
    
    private static FilenameFilter SDFFilter() {
        FilenameFilter sdfFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".sdf"); // Possibly test if it starts with "Data"
            }
        };
        return sdfFilter;
    }
}
