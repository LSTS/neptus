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
 * Author: Paulo Dias
 * 13/Dec/2022
 */
package pt.lsts.neptus.plugins.deepvision;

import pt.lsts.neptus.mra.api.SidescanParserChecker;
import pt.lsts.neptus.mra.api.SidescanParserFactory;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

/**
 * @author Paulo Dias
 * @author Pedro Costa
 */
@PluginDescription(name = "Deepvision Sidescan Parser", author = "Pedro Costa", version = "0.1", description = "Parse DeepVision Sidescan data")
public class DvsSidescanParserChecker implements SidescanParserChecker<DvsSidescanParser> {
    static {
        SidescanParserFactory.registerChecker(DvsSidescanParserChecker.class);
    }

    @Override
    public boolean isCompatibleParser(IMraLogGroup log) {
        return !listDataFiles(log).isEmpty();
    }

    @Override
    public List<File> listDataFiles(IMraLogGroup log) {
        FilenameFilter sdfFilter = DVSFilter();
        File[] files = log.getDir().listFiles(sdfFilter);
        return Arrays.asList(files);
    }

    @Override
    public DvsSidescanParser getParser(IMraLogGroup log) {
        List<File> fileList = listDataFiles(log);

        if (fileList.isEmpty())
            return null;

        File file = fileList.get(0);
        if(file.exists()) {
            return new DvsSidescanParser(file);
        }

        return null;
    }

    private static FilenameFilter DVSFilter() {
        FilenameFilter dvsFilter = (dir, name) -> {
            return name.toLowerCase().endsWith(".dvs"); // Possibly test if it starts with "Data"
        };
        return dvsFilter;
    }
}
