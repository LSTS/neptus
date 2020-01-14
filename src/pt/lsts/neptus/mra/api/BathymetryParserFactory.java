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
 * May 28, 2013
 */
package pt.lsts.neptus.mra.api;

import java.io.File;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.mra.importers.lsf.DVLBathymetryParser;

/**
 * @author jqcorreia
 * @author hfq
 */
public class BathymetryParserFactory {
    static File dir;
    static File file;
    static IMraLogGroup source;

    public static BathymetryParser build(IMraLogGroup log) {
        file = null;
        dir = log.getDir();
        source = log;

        return getParser();
    }

    public static BathymetryParser build(File fileOrDir) {
        source = null;
        if(fileOrDir.isDirectory())
            dir = file;
        else {
            file = fileOrDir;
        }
        return getParser();
    }

    /**
     * 
     * @param log
     * @param sensorType could be "DVL" or "Multibeam
     * @return
     */
    public static BathymetryParser build(IMraLogGroup log, String sensorType) {
        file = null;
        dir = log.getDir();
        source = log;

        return getParserByType(sensorType);
    }

    private static BathymetryParser getParser() {
        if(file != null) {
            return null; //FIXME for now only directories are supported 
        }
        else if(dir != null) {
            if (new File(dir.getAbsolutePath()+"/data.83P").exists())
                return new DeltaTParser(source);
            else if (new File(dir.getAbsolutePath()+"/Data.83P").exists())
                return new DeltaTParser(source);
            else if (new File(dir.getAbsolutePath()+"/multibeam.83P").exists())
                return new DeltaTParser(source);

            // Next cases should be file = new File(...) and check for existence
            // TODO
        }
        if (source.getLsfIndex().containsMessagesOfType("Distance"))
            return new DVLBathymetryParser(source);
        return null;
    }

    private static BathymetryParser getParserByType(String sensorType) {
        if(dir != null) {
            if (sensorType.equals("dvl")) {
                if (source.getLsfIndex().containsMessagesOfType("Distance"))
                    return new DVLBathymetryParser(source);
                return null;
            }
            else if (sensorType.equals("multibeam")) {
                if(dir != null) {
                    if (new File(dir.getAbsolutePath()+"/data.83P").exists())
                        return new DeltaTParser(source);
                    else if (new File(dir.getAbsolutePath()+"/Data.83P").exists())
                        return new DeltaTParser(source);
                    else if (new File(dir.getAbsolutePath()+"/multibeam.83P").exists())
                        return new DeltaTParser(source);
                }
            }
            else {
                NeptusLog.pub().error("Sensor Type is not allowed or isn't supported by Bathymetry Parser");
                return null;
            }
        }
        return null;
    }
}
