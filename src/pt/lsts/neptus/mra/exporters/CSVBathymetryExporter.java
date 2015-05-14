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
 * Author: Paulo Dias
 * 25/08/2014
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;

import javax.swing.ProgressMonitor;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.deltat.DeltaTHeader;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.FileUtil;

/**
 * Apply corrected position to 83P and re-exported.  
 * @author pdias
 * 
 */
@PluginDescription
public class CSVBathymetryExporter implements MRAExporter {
    @SuppressWarnings("unused")
    private IMraLogGroup log = null;

    private DeltaTParser deltaParser = null;

    
    private String processResultOutputFileName;
    private Writer processResultOutputWriter;

    public CSVBathymetryExporter(IMraLogGroup log) {
        this.log = log;
    }
    
    public String getName() {
        return "CSV Bathymetry Exporter";
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return DeltaTParser.canBeApplied(source);
    }

    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        if (!canBeApplied(source))
            return "No data to process!";
        
        File f = new File(source.getFile("Data.lsf").getParent() + "/mra/bathy.info");
        File folder = new File(source.getFile("Data.lsf").getParent() + "/mra/");

        if (!folder.exists())
            folder.mkdirs();

        processResultOutputFileName = folder.getAbsolutePath() + "/bathymetry-process.csv";
        boolean fileChecker = initResultOutputFile();
        if (!fileChecker) {
            pmonitor.setNote("Shit Sherlock");
            return "Shit Sherlock";
        }
        
        log = source;
        
        if (pmonitor != null)
            pmonitor.setNote(I18n.text("Copying file"));
        
        deltaParser = new DeltaTParser(source);
        
        BathymetrySwath nextSwath = null; 
        recordMsg("%Time UTC,Latitude Degrees,Longitude Degrees,Roll Radians,Pitch Radians,Yaw Radians, Number of data elem, (X-Offset Y-Offset Heights Meters)* \n");
        long previousTimeStamp = 0;
        try {
            nextSwath = deltaParser.nextSwath();
            while (nextSwath != null) {
            
                long timeInSeconds = (long) (nextSwath.getTimestamp() / 1000);
                // Only one beam array for second
                if (previousTimeStamp == 0 || previousTimeStamp != timeInSeconds) {

                    // Timestamp
                    recordMsg(timeInSeconds * 1000+",");
                    // Position in degrees
                    recordMsg(nextSwath.getPose().getPosition().getLatitudeDegs()+",");
                    recordMsg(nextSwath.getPose().getPosition().getLongitudeDegs()+",");
                    // Attitude in radians
                    recordMsg(nextSwath.getPose().getRoll()+",");
                    recordMsg(nextSwath.getPose().getPitch()+",");
                    recordMsg(nextSwath.getPose().getYaw()+",");
                    // Number of beams
                    recordMsg(nextSwath.getData().length+",");
                    for (int i = 0; i < nextSwath.getData().length; i++) {
                        if (nextSwath.getData()[i] != null) {
                            // Beam x offset
                            recordMsg(nextSwath.getData()[i].north+" ");
                            // Beam y offset
                            recordMsg(nextSwath.getData()[i].east+" ");
                            // Beam Height
                            recordMsg(nextSwath.getData()[i].depth+",");
                        }
                        else
                            recordMsg("NaN NaN NaN,");
                    } 
                    recordMsgln("");
                    
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

    private synchronized boolean initResultOutputFile() {
        if (processResultOutputWriter != null || new File(processResultOutputFileName).exists()) {
            return false;
        }
        try {
            try {
                processResultOutputWriter = new OutputStreamWriter(new FileOutputStream(processResultOutputFileName), "UTF-8");
            }
            catch (UnsupportedEncodingException UEe) {
                System.err.println("\n-- UnsupportedEncodingException\n");
                System.err.flush();
                processResultOutputWriter = new OutputStreamWriter(new FileOutputStream(processResultOutputFileName), "iso8859-1");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private void cleanupResultOutputFile() {
        if (processResultOutputWriter != null) {
            try {
                processResultOutputWriter.flush();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            try {
                processResultOutputWriter.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            processResultOutputWriter = null;
        }
    }
    
    private void recordMsgln(String string) {
        recordMsg(string + "\r\n");
    }

    private void recordMsg(String string) {
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
