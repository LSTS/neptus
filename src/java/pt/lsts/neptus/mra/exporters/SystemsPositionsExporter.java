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
 * Author: pdias
 * 29/04/2016
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.Announce;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.RemoteSensorInfo;
import pt.lsts.imc.ReportedState;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.csv.CsvWriter;

/**
 * @author pdias
 *
 */
@PluginDescription(name="Systems Positions Exporter to CSV", experimental = true)
public class SystemsPositionsExporter implements MRAExporter {

    @NeptusProperty
    private boolean includeAnnounceMessages = false;

    @NeptusProperty
    private boolean includeRemoteInfoMessages = false;
    
    private Map<String, CsvWriter> outputWriters = new HashMap<>();
    private Map<String, BufferedWriter> outputFiles = new HashMap<>();

    private File writingFolder;

    /**
     * @param source
     */
    public SystemsPositionsExporter(IMraLogGroup source) {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mra.exporters.MRAExporter#canBeApplied(pt.lsts.neptus.mra.importers.IMraLogGroup)
     */
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.mra.exporters.MRAExporter#process(pt.lsts.neptus.mra.importers.IMraLogGroup, javax.swing.ProgressMonitor)
     */
    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        if (PluginUtils.editPluginProperties(this, true))
            return I18n.text("Cancelled by the user.");
        
        writingFolder = new File(source.getDir().getAbsolutePath() + "/mra/pos");

        if (!writingFolder.exists())
            writingFolder.mkdirs();
        
        LsfIndex index = source.getLsfIndex();
        
        int numberOfMessages = index.getNumberOfMessages();
        pmonitor.setMinimum(0);
        pmonitor.setMaximum(numberOfMessages);
        index.getAvailableSystems();
        
        for (int i = 0; i < numberOfMessages; i++) {
            if (pmonitor.isCanceled())
                break;
            
            pmonitor.setProgress(i);
            
            IMCMessage msg = index.getMessage(i);
            LocationType loc = null;
            String sysName = index.getSystemName(msg.getSrc());
            long timestamp = msg.getTimestampMillis();
            if (msg.getClass().isAssignableFrom(EstimatedState.class)) {
                loc = IMCUtils.parseLocation((EstimatedState) msg);
                loc = loc.convertToAbsoluteLatLonDepth();
            }
            else if (msg.getClass().isAssignableFrom(Announce.class)) {
                Announce announceMsg = (Announce) msg;
                loc = new LocationType(Math.toDegrees(announceMsg.getLat()),
                            Math.toDegrees(announceMsg.getLon()));
                loc.setHeight(announceMsg.getHeight());
            } 
            else if (msg.getClass().isAssignableFrom(RemoteSensorInfo.class)) {
                RemoteSensorInfo rsim = (RemoteSensorInfo) msg;
                loc = new LocationType(Math.toDegrees(rsim.getLat()),
                        Math.toDegrees(rsim.getLon()));
                loc.setHeight(rsim.getAlt());
            } 
            else if (msg.getClass().isAssignableFrom(ReportedState.class)) {
                ReportedState rsm = (ReportedState) msg;
                loc = new LocationType(Math.toDegrees(rsm.getLat()),
                        Math.toDegrees(rsm.getLon()));
                loc.setHeight(-rsm.getDepth());
                timestamp = (long) (rsm.getRcpTime() * 1E3);
            }
            
            
            if (loc != null) {
                CsvWriter writer = getOrCreateWriter(sysName);
                try {
                    writer.writeData(timestamp, loc.getLatitudeDegs(), loc.getLongitudeDegs(),
                            loc.getHeight());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        for (BufferedWriter bw : outputFiles.values()) {
            try {
                bw.flush();
                bw.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        outputFiles.clear();
        outputWriters.clear();
        
        return null;
    }

    /**
     * @param systemName
     * @return
     */
    private CsvWriter getOrCreateWriter(String systemName) {
        CsvWriter writer = outputWriters.get(systemName);
        if (writer == null) {
            try {
                File fx = new File(writingFolder, systemName.toLowerCase() + ".csv");
                FileWriter out = new FileWriter(fx);
                BufferedWriter bufWriter = new BufferedWriter(out);
                writer = new CsvWriter(bufWriter);
                writer.writeHeader(systemName, "time (UTC), lat, lon, height");
                outputWriters.put(systemName, writer);
                outputFiles.put(systemName, bufWriter);
            }
            catch (Exception e) {
                e.printStackTrace();
                writer = null;
            }
        }
        return writer;
    }
}
