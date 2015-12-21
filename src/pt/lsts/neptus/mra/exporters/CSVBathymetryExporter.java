/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Tiago Rodrigues
 * 06/05/2015
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.swing.ProgressMonitor;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;

/**
 * 
 * @author Tiago Rodrigues
 */
@PluginDescription(name = "DeltaT CSV Bathymetry Exporter")
public class CSVBathymetryExporter implements MRAExporter {
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
            return "No data to process!";
        
        File folder = new File(source.getFile("Data.lsf").getParent() + "/mra/");

        if (!folder.exists())
            folder.mkdirs();

        // To include plan name and timestamp in the csv file name
        String parentFolderAbsolutePath = source.getFile("Data.lsf").getParent();
        String parentFolder = parentFolderAbsolutePath.substring(parentFolderAbsolutePath.lastIndexOf('/') + 1);
        
        // Create all beams csv file and file descriptor
        processResultOutputFileNameAllBeams = folder.getAbsolutePath() + "/"+parentFolder+"-bathymetry-process-all-beams.csv";
        boolean fileChecker = initResultOutputFile(processResultOutputFileNameAllBeams, processResultOutputWriterAllBeams);
        if (!fileChecker) {
            pmonitor.setNote("File already exists!");
            return "File already exists!";
        }
        if (fileChecker) {
            processResultOutputWriterAllBeams = initializeWriter(processResultOutputFileNameAllBeams);
        }
        
        // Create center beam csv file and file descriptor
        processResultOutputFileNameCenterBeam = folder.getAbsolutePath() + "/"+parentFolder+"-bathymetry-process-center-beam.csv";
        fileChecker = initResultOutputFile(processResultOutputFileNameCenterBeam, processResultOutputWriterCenterBeam);
        if (!fileChecker) {
            pmonitor.setNote("File already exists!");
            return "File already exists!";
        }
        if (fileChecker) {
            processResultOutputWriterCenterBeam = initializeWriter(processResultOutputFileNameCenterBeam);
        }
        
        log = source;
        
        if (pmonitor != null)
            pmonitor.setNote(I18n.text("Copying file"));
        
        deltaParser = new DeltaTParser(source);
        
        BathymetrySwath nextSwath = null; 
        
        /* Wirte all beams to csv */
        // Center beam csv first line
        recordMsg("%Time UTC,Latitude Degrees,Longitude Degrees,Roll Radians,Pitch Radians,Yaw Radians, Number of data elem, (X-Offset Y-Offset Height - meters) \n", processResultOutputWriterCenterBeam);
        // All beams csv first line        
        recordMsg("%Time UTC,Latitude Degrees,Longitude Degrees,Roll Radians,Pitch Radians,Yaw Radians, Number of data elem, (X-Offset Y-Offset Heights Meters)* \n", processResultOutputWriterAllBeams);
        long previousTimeStamp = 0;
        try {
            nextSwath = deltaParser.nextSwath();
            while (nextSwath != null) {
            
                long timeInSeconds = (long) (nextSwath.getTimestamp() / 1000);
                // Only one beam array for second
                if (previousTimeStamp == 0 || previousTimeStamp != timeInSeconds) {

                    // Timestamp
                    recordMsg(timeInSeconds * 1000 + ",", processResultOutputWriterAllBeams);
                    // Position in degrees
                    recordMsg(nextSwath.getPose().getPosition().getLatitudeDegs()+",", processResultOutputWriterAllBeams);
                    recordMsg(nextSwath.getPose().getPosition().getLongitudeDegs()+",", processResultOutputWriterAllBeams);
                    // Attitude in radians
                    recordMsg(nextSwath.getPose().getRoll()+",", processResultOutputWriterAllBeams);
                    recordMsg(nextSwath.getPose().getPitch()+",", processResultOutputWriterAllBeams);
                    recordMsg(nextSwath.getPose().getYaw()+",", processResultOutputWriterAllBeams);
                    
   
                    // Timestamp
                    recordMsg(timeInSeconds * 1000 + ",", processResultOutputWriterCenterBeam);
                    // Position in degrees
                    recordMsg(nextSwath.getPose().getPosition().getLatitudeDegs()+",", processResultOutputWriterCenterBeam);
                    recordMsg(nextSwath.getPose().getPosition().getLongitudeDegs()+",", processResultOutputWriterCenterBeam);
                    // Attitude in radians
                    recordMsg(nextSwath.getPose().getRoll()+",", processResultOutputWriterCenterBeam);
                    recordMsg(nextSwath.getPose().getPitch()+",", processResultOutputWriterCenterBeam);
                    recordMsg(nextSwath.getPose().getYaw()+",", processResultOutputWriterCenterBeam);
                    
                    // Number of beams
                    recordMsg("1,", processResultOutputWriterCenterBeam);
                    recordMsg(nextSwath.getData().length+",", processResultOutputWriterAllBeams);

                    for (int i = 0; i < nextSwath.getData().length; i++) {
                        if (nextSwath.getData()[i] != null) {
                            // Beam x offset
                            recordMsg(nextSwath.getData()[i].north+" ", processResultOutputWriterAllBeams);
                            // Beam y offset
                            recordMsg(nextSwath.getData()[i].east+" ", processResultOutputWriterAllBeams);
                            // Beam Height
                            recordMsg(nextSwath.getData()[i].depth+",", processResultOutputWriterAllBeams);
                        }
                        else
                            recordMsg("NaN NaN NaN,", processResultOutputWriterAllBeams);
                        
                        if (nextSwath.getData()[i] != null && i == (nextSwath.getData().length / 2)) {
                            // Beam x offset
                            recordMsg(nextSwath.getData()[i].north+" ", processResultOutputWriterCenterBeam);
                            // Beam y offset
                            recordMsg(nextSwath.getData()[i].east+" ", processResultOutputWriterCenterBeam);
                            // Beam Height
                            recordMsg(nextSwath.getData()[i].depth+",", processResultOutputWriterCenterBeam);
                        }
                        else if (nextSwath.getData()[i] == null && i == (nextSwath.getData().length / 2))
                            recordMsg("NaN NaN NaN,", processResultOutputWriterCenterBeam);
                    } 
                    recordMsgln("", processResultOutputWriterCenterBeam);
                    
                    recordMsgln("", processResultOutputWriterAllBeams);
                    
                    previousTimeStamp = timeInSeconds;
                }                          

                nextSwath = deltaParser.nextSwath();
      
                
                if (pmonitor != null && pmonitor.isCanceled())
                    return "Export interrupted!";
            }
            
            return "Export completed successfully";
        }
        catch (Exception e) {
            e.printStackTrace();
            return "Export completed with errors! (" + e.getMessage() + ")";
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
        catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
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
