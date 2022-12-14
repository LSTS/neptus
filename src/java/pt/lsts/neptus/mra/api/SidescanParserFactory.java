/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.i872.Imagenex872SidescanParserChecker;
import pt.lsts.neptus.mra.importers.jsf.JsfSidescanParserChecker;
import pt.lsts.neptus.mra.importers.sdf.SdfSidescanParserChecker;
import pt.lsts.neptus.util.llf.LogUtils;

/**
 * @author jqcorreia
 *
 */
public class SidescanParserFactory {

    private static List<Class<SidescanParserChecker>> sidescanParserCheckerList = new ArrayList<>();
    static {
        registerChecker(Imagenex872SidescanParserChecker.class);
        registerChecker(JsfSidescanParserChecker.class);
        registerChecker(SdfSidescanParserChecker.class);
    }

    private static final String JSF_FILE = "Data.jsf";
    private static final String I872_FILE = "Data.872";

    private static String[] validSidescanFiles = {JSF_FILE, I872_FILE};

    static File dir;
    static File file;
    static IMraLogGroup source;

    /**
     * This method allows to register additional {@link SidescanParserChecker} to be used.
     *
     * @param checker
     */
    @SuppressWarnings("unchecked")
    public synchronized static <C extends SidescanParserChecker> void registerChecker(Class<C> checker) {
        if (!sidescanParserCheckerList.contains(checker)) {
            sidescanParserCheckerList.add((Class<SidescanParserChecker>) checker);
        }
    }

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
        return sidescanParserCheckerList.stream().anyMatch(ssc -> {
            try {
                return ssc.getConstructor().newInstance().isCompatibleParser(log);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    private static SidescanParser getParser() {
        if(file != null) {
            return null; //FIXME for now only directories are supported 
        }
        else if(dir != null) {
            List<SidescanParserChecker> compatibleParserCheckersList = sidescanParserCheckerList.stream().map(ssc -> {
                try {
                    return ssc.getConstructor().newInstance();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }).filter(ssc -> {
                if (ssc == null) {
                    return false;
                }

                return ssc.isCompatibleParser(source);
            }).collect(Collectors.toList());
            
            // FIXME using the first one
            if (!compatibleParserCheckersList.isEmpty()) {
                return compatibleParserCheckersList.get(0).getParser(source);
            }
            
            // Fallback. Defaults to using IMC (in case of sidescan data existence)
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
}
