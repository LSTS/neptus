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
 * Author: Paulo Dias
 * Nov 8, 2015
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.tid.TidWriter;

/**
 * @author Paulo Dias
 *
 */
@PluginDescription(name="Depth to TID Exporter")
public class DepthToTidExporter implements MRAExporter {

    /* Format:
       --------
       2014/08/27 07:53:32.817 0.007601 
       2014/08/27 07:53:32.861 0.007705 
       2014/08/27 07:53:32.957 0.007487 
       2014/08/27 07:53:33.057 0.006612 
     */
    

    private TidWriter tidWriter;
    
    public DepthToTidExporter(IMraLogGroup source) {
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().containsMessagesOfType("EstimatedState");
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        
        File outputDir = new File(source.getDir(), "mra");
        
        LsfIndex index = source.getLsfIndex();
        // LinkedHashMap<String, Double> lastTimestamps = new LinkedHashMap<>();
        // LinkedHashMap<String, BufferedWriter> writers = new LinkedHashMap<>();
        
        Collection<Integer> vehs = source.getVehicleSources();
        int srcId = vehs.iterator().next(); 
        
        try {
            int decimalHouses = 2;
            BufferedWriter outFile = new BufferedWriter(new FileWriter(new File(outputDir, "Depths.tid")));
            tidWriter = new TidWriter(outFile, decimalHouses);
            tidWriter.writeHeader("Vehicle's Depths (m)");

            for (EstimatedState state: index.getIterator(EstimatedState.class)) {
                if (state.getSrc() != srcId)
                    continue;
                
                try {
                    long timeMillis = state.getTimestampMillis();
                    double depth = state.getDepth();
                    tidWriter.writeData(timeMillis, depth);        
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            try {
                outFile.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        return I18n.textf("Generated TID file for %system at %file.", 
                source.getSystemName(srcId), outputDir.getAbsolutePath());
    }
}
