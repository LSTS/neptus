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
 * Author: zp
 * Mar 19, 2020
 */
package pt.lsts.neptus.mra.exporters;

import javax.swing.ProgressMonitor;

import pt.lsts.neptus.mra.api.LsfTreeSet;
import pt.lsts.neptus.mra.api.LsfTreeSet.LsfLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * @author zp
 *
 */
public class BatchMraExporter {
    
    @SafeVarargs
    public static void apply(Class<? extends MRAExporter>... exporters) {
        LsfTreeSet lsfFiles = LsfTreeSet.selectFolders();
        apply(lsfFiles, exporters);
    }
    
    @SafeVarargs
    public static void apply(LsfTreeSet lsfFiles, Class<? extends MRAExporter>... exporters) {
        for (LsfLog log : lsfFiles) {
            try {
                LsfLogSource mraSource = new LsfLogSource(log.lsfSource, null);
                for (Class<? extends MRAExporter> e : exporters) {
                    MRAExporter exporter = e.getConstructor(IMraLogGroup.class).newInstance(mraSource);
                    if (exporter.canBeApplied(mraSource)) {
                        System.out.println("Applying  "+PluginUtils.getPluginName(e)+" to "+log.root);    
                        ProgressMonitor pmon = new ProgressMonitor(null, "Processing "+log.root, "Processing "+log.root, 0, 1024);
                        exporter.process(mraSource, pmon);    
                        pmon.close();
                    }    
                }
                mraSource.cleanup();
            }
            catch (Exception e) {
                e.printStackTrace();
            }            
        }
    }
}
