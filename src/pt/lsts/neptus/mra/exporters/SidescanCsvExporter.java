/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Jul 30, 2018
 */
package pt.lsts.neptus.mra.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

import javax.swing.ProgressMonitor;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanParserFactory;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Export Sidescan to CSV")
public class SidescanCsvExporter implements MRAExporter {

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    private void writeCsvValues(BufferedWriter bw, Object... values) throws IOException {
        bw.write(Arrays.asList(values).stream().map(v -> v.toString()).collect(Collectors.joining(", ")) + "\n");
        bw.flush();
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {

        File dir = new File(source.getFile("mra"), "csv");
        dir.mkdirs();
        SidescanParser parser = SidescanParserFactory.build(source);
        long start = parser.firstPingTimestamp();
        long end = parser.lastPingTimestamp();
        SidescanParameters params = new SidescanParameters(0.1, 100);
           
        for (int sys : parser.getSubsystemList()) {
            File out = new File(dir, "sidescan" + sys + ".csv");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(out))) {
                writeCsvValues(bw, "Timestamp", "Latitude", "Longitude", "Altitude", "Depth", "Roll", "Pitch", "Yaw",
                        "Range", "Data");

                for (long time = start; time < end - 1000; time += 1000) {
                    System.out.println(new Date(time));
                    ArrayList<SidescanLine> lines;
                    try {
                        lines = parser.getLinesBetween(time, time + 1000, sys, params);
                        for (SidescanLine sl : lines) {
                            StringBuilder sb = new StringBuilder();//String.format("[ %.3f", sl.getData()[0]));
                            for (int i = 0; i < sl.getData().length; i++)
                                sb.append(String.format("%02X", (byte)(255*sl.getData()[i])));
                            //sb.append(" ]");
                            LocationType loc = sl.getState().getPosition().convertToAbsoluteLatLonDepth();
                            writeCsvValues(bw, sl.getTimestampMillis(), loc.getLatitudeDegs(),
                                    loc.getLongitudeDegs(), (float)sl.getState().getAltitude(),
                                    (float)sl.getState().getDepth(), (float)Math.toDegrees(sl.getState().getRoll()),
                                    (float)Math.toDegrees(sl.getState().getPitch()), (float)Math.toDegrees(sl.getState().getYaw()),
                                    (float)sl.getRange(), sb);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                }
                bw.close();
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                return "Error processing log: " + e.getMessage();
            }
        }

        return null;
    }

}
