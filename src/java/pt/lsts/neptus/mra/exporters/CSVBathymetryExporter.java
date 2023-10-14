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
 * Author: Tiago Rodrigues
 * 06/05/2015
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.swing.ProgressMonitor;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * 
 * @author Tiago Rodrigues
 * @author pdias (Cleanup)
 */
@PluginDescription(name = "DeltaT CSV Bathymetry Exporter", author = "Tiago Rodrigues", version = "1.0")
public class CSVBathymetryExporter implements MRAExporter {
    public enum Periodicity {
        ALL,
        EVERY_SECOND,
        EVERY_MINUTE
    };
    
    @NeptusProperty
    private Periodicity periodicity = Periodicity.EVERY_SECOND;
    
    @SuppressWarnings("unused")
    private IMraLogGroup log = null;

    private DeltaTParser deltaParser = null;

    // All beams csv
    private String processResultOutputFileNameAllBeams;
    private Writer processResultOutputWriterAllBeams;
    
    // Center beam csv
    private String processResultOutputFileNameCenterBeam;
    private Writer processResultOutputWriterCenterBeam;

    public CSVBathymetryExporter(IMraLogGroup log) {
        this.log = log;
    }
    
    public String getName() {
        return PluginUtils.getPluginName(getClass());
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return DeltaTParser.canBeApplied(source);
    }

    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        if (!canBeApplied(source))
            return I18n.text("No data to process!");
        
        PluginUtils.editPluginProperties(this, true);
        
        Periodicity periodicity = this.periodicity;
        
        if (pmonitor != null)
            pmonitor.setProgress(0);
        
        File folder = new File(source.getDir().getAbsolutePath() + "/mra/");

        if (!folder.exists())
            folder.mkdirs();

        // To include plan name and timestamp in the csv file name
        String parentFolder = source.getDir().getName();
        
        // Create all beams csv file and file descriptor
        processResultOutputFileNameAllBeams = new File(folder, parentFolder + "-bathymetry-process-all-beams.csv").getAbsolutePath();
        boolean fileChecker = initResultOutputFile(processResultOutputFileNameAllBeams, processResultOutputWriterAllBeams);
        if (!fileChecker)
            return I18n.text("File already exists!");
        
        if (fileChecker)
            processResultOutputWriterAllBeams = initializeWriter(processResultOutputFileNameAllBeams);
        
        // Create center beam csv file and file descriptor
        processResultOutputFileNameCenterBeam = new File(folder, parentFolder + "-bathymetry-process-center-beam.csv").getAbsolutePath();
        fileChecker = initResultOutputFile(processResultOutputFileNameCenterBeam, processResultOutputWriterCenterBeam);
        if (!fileChecker)
            return I18n.text("File already exists!");
        
        if (fileChecker)
            processResultOutputWriterCenterBeam = initializeWriter(processResultOutputFileNameCenterBeam);
        
        log = source;
        
        if (pmonitor != null)
            pmonitor.setNote(I18n.text("Copying file"));
        
        deltaParser = new DeltaTParser(source);
        
        BathymetrySwath nextSwath = null; 
        
        /* Write header to CSVs */
        String headerStr0 = "% Imagenex Delta-T Multibeam data export";
        String headerStr1 = "% Tide corrected: no";
        String headerStr2 = "%";
        String headerStr3 = "% Time UTC Miliseconds, Latitude Degrees, Longitude Degrees, "
                + "Roll Radians, Pitch Radians, Yaw Radians, "
                + "Number of data elem, (X-Offset Y-Offset Height - meters)";
        // Center beam CSV header
        recordMsgln(headerStr0, processResultOutputWriterCenterBeam);
        recordMsgln(headerStr1, processResultOutputWriterCenterBeam);
        recordMsgln(headerStr2, processResultOutputWriterCenterBeam);
        recordMsgln(headerStr3, processResultOutputWriterCenterBeam);
        // All beams CSV header        
        recordMsgln(headerStr0, processResultOutputWriterAllBeams);
        recordMsgln(headerStr1, processResultOutputWriterAllBeams);
        recordMsgln(headerStr2, processResultOutputWriterAllBeams);
        recordMsgln(headerStr3, processResultOutputWriterAllBeams);
        
        long fTime = deltaParser.getFirstTimestamp();
        long lTime = deltaParser.getLastTimestamp();
        double span = lTime - fTime;
        if (span <= 0)
            span = 1;
        if (pmonitor != null) {
            pmonitor.setMinimum(0);
            pmonitor.setMaximum(100);
        }
        
        long previousTimeStamp = 0;
        try {
            nextSwath = deltaParser.nextSwath();
            while (nextSwath != null) {
                if (pmonitor != null) {
                    double prog = (nextSwath.getTimestamp() - fTime) / span * 100;
                    pmonitor.setProgress((int) prog);
                }
                
                long multi;
                switch (periodicity) {
                    case ALL:
                        multi = 1;
                        break;
                    case EVERY_MINUTE:
                        multi = DateTimeUtil.MINUTE;
                        break;
                    case EVERY_SECOND:
                    default:
                        multi = 1000;
                        break;
                }
                long timeInSeconds = (long) (nextSwath.getTimestamp() / multi);
                // Only one beam array for second
                if (previousTimeStamp == 0 || previousTimeStamp != timeInSeconds) {
                    // Timestamp
                    recordMsg(timeInSeconds * multi + "", processResultOutputWriterAllBeams);
                    // Position in degrees
                    recordMsg("," + nextSwath.getPose().getPosition().getLatitudeDegs(), processResultOutputWriterAllBeams);
                    recordMsg("," + nextSwath.getPose().getPosition().getLongitudeDegs(), processResultOutputWriterAllBeams);
                    // Attitude in radians
                    recordMsg("," + nextSwath.getPose().getRoll(), processResultOutputWriterAllBeams);
                    recordMsg("," + nextSwath.getPose().getPitch(), processResultOutputWriterAllBeams);
                    recordMsg("," + nextSwath.getPose().getYaw(), processResultOutputWriterAllBeams);
   
                    // Timestamp
                    recordMsg(timeInSeconds * multi + "", processResultOutputWriterCenterBeam);
                    // Position in degrees
                    recordMsg("," + nextSwath.getPose().getPosition().getLatitudeDegs(), processResultOutputWriterCenterBeam);
                    recordMsg("," + nextSwath.getPose().getPosition().getLongitudeDegs(), processResultOutputWriterCenterBeam);
                    // Attitude in radians
                    recordMsg("," + nextSwath.getPose().getRoll(), processResultOutputWriterCenterBeam);
                    recordMsg("," + nextSwath.getPose().getPitch(), processResultOutputWriterCenterBeam);
                    recordMsg("," + nextSwath.getPose().getYaw(), processResultOutputWriterCenterBeam);
                    
                    // Number of beams
                    recordMsg(",1", processResultOutputWriterCenterBeam);
                    recordMsg("," + nextSwath.getData().length, processResultOutputWriterAllBeams);

                    for (int i = 0; i < nextSwath.getData().length; i++) {
                        LocationType loc = nextSwath.getPose().getPosition();
                        if (nextSwath.getData()[i] != null) {
                            double n = nextSwath.getData()[i].north;
                            double e = nextSwath.getData()[i].east;
                            double d = (nextSwath.getData()[i].depth  + (loc.getDepth() >= 0 ? loc.getDepth() : 0));
                            
                            recordMsg(",", processResultOutputWriterAllBeams);
                            // Beam x offset
                            recordMsg(n + " ", processResultOutputWriterAllBeams);
                            // Beam y offset
                            recordMsg(e + " ", processResultOutputWriterAllBeams);
                            // Beam Height
                            recordMsg(d + "", processResultOutputWriterAllBeams);
                            
                            if (i == (nextSwath.getData().length / 2)) {
                                recordMsg(",", processResultOutputWriterCenterBeam);
                                // Beam x offset
                                recordMsg(n + " ", processResultOutputWriterCenterBeam);
                                // Beam y offset
                                recordMsg(e + " ", processResultOutputWriterCenterBeam);
                                // Beam Height
                                recordMsg(d + "", processResultOutputWriterCenterBeam);
                            }
                        }
                        else {
                            recordMsg(",NaN NaN NaN", processResultOutputWriterAllBeams);
                            if (i == (nextSwath.getData().length / 2))
                                recordMsg(",NaN NaN NaN", processResultOutputWriterCenterBeam);
                        }
                    } 
                    recordMsgln("", processResultOutputWriterCenterBeam);
                    
                    recordMsgln("", processResultOutputWriterAllBeams);
                    
                    previousTimeStamp = timeInSeconds;
                }                          

                nextSwath = deltaParser.nextSwath();
      
                if (pmonitor != null && pmonitor.isCanceled())
                    return "Export interrupted!";
            }
            
            return I18n.text("Export completed successfully");
        }
        catch (Exception e) {
            e.printStackTrace();
            return I18n.textf("Export completed with errors! (%error)", e.getMessage());
        }
        finally {
            cleanupResultOutputFile();
        }

    }

    private synchronized boolean initResultOutputFile(String processResultOutputFileName, Writer processResultOutputWriter) {
        if (processResultOutputWriter != null || new File(processResultOutputFileName).exists()) {
            return false;
        }

        return true;
    }
    
    private Writer initializeWriter(String processResultOutputFileName) {
        try {
            return new OutputStreamWriter(new FileOutputStream(processResultOutputFileName), "UTF-8");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void cleanupResultOutputFile() {
        if (processResultOutputWriterAllBeams != null) {
            try {
                processResultOutputWriterAllBeams.flush();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            try {
                processResultOutputWriterAllBeams.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            processResultOutputWriterAllBeams = null;
        }

        if (processResultOutputWriterCenterBeam != null) {
            try {
                processResultOutputWriterCenterBeam.flush();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            try {
                processResultOutputWriterCenterBeam.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            processResultOutputWriterCenterBeam = null;
        }
    }
    
    private void recordMsgln(String string, Writer processResultOutputWriter) {
        recordMsg(string + "\r\n", processResultOutputWriter);
    }

    private void recordMsg(String string, Writer processResultOutputWriter) {
        if (processResultOutputWriter != null) {
            try {
                processResultOutputWriter.write(string);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    
    public static void main(String[] args) {
        
    }
}
