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
 * Apr 17, 2015
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Locale;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
@PluginDescription(name="XYZ Path Exporter")
public class PathExporter implements MRAExporter {

    @NeptusProperty(name="Reference Location", description="All coordinates will be relative to this point")
    public LocationType homeref = new LocationType(41.185171, -8.704723);
    
    @NeptusProperty(name="Time Step", description="Timestep between generated timestamps")
    public double timestep = 1.0;
    
    public PathExporter(IMraLogGroup source) {
        
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getLsfIndex().containsMessagesOfType("EstimatedState");
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        PluginUtils.loadProperties(this, "MRAPathExporter");
        PluginUtils.editPluginProperties(this, true);
        PluginUtils.saveProperties(this, true);
        
        File outputDir = new File(source.getDir(), "mra");
        
        LsfIndex index = source.getLsfIndex();
        LinkedHashMap<String, Double> lastTimestamps = new LinkedHashMap<>();
        LinkedHashMap<String, BufferedWriter> writers = new LinkedHashMap<>();
        
        for (EstimatedState state: index.getIterator(EstimatedState.class)) {
            try {
                String v = state.getSourceName();
                if (!lastTimestamps.containsKey(v)) {
                    lastTimestamps.put(v, state.getTimestamp());
                    writers.put(v, new BufferedWriter(new FileWriter(new File(outputDir, v+".csv"))));
                    writeHeader(writers.get(v));
                    SystemPositionAndAttitude pose = IMCUtils.parseState(state);
                    pose.setTime((long)(state.getTimestampMillis()-index.getStartTime()*1000));
                    writePose(writers.get(v), pose);
                }
                else {
                    double lastTimestamp = lastTimestamps.get(v);
                    if (state.getTimestamp() - lastTimestamp < timestep)
                        continue;
                    lastTimestamps.put(v, state.getTimestamp());
                    SystemPositionAndAttitude pose = IMCUtils.parseState(state);
                    pose.setTime((long)(state.getTimestampMillis()-index.getStartTime()*1000));
                    writePose(writers.get(v), pose);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        int count = 0;
        
        for (BufferedWriter w : writers.values()) {
            try {
                count++;
                w.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return I18n.textf("Generated %num files in %path.", count, outputDir.getAbsolutePath());
    }
    
    private void writeHeader(BufferedWriter writer) throws Exception {
        writer.write("Timestamp, Northing, Easting, Altitude\n");
    }
    
    private void writePose(BufferedWriter writer, SystemPositionAndAttitude pose) throws Exception {
        LocationType loc = pose.getPosition();
        double[] offsets = loc.getOffsetFrom(homeref);
        writer.write(String.format(Locale.US, "%.3f, %.2f, %.2f, %.2f\n", pose.getTime()/1000.0, offsets[0], offsets[1], -loc.getDepth()));        
    }
}
