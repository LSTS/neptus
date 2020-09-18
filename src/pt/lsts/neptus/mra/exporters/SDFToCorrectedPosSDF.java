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
 * 16/09/2020
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.ProgressMonitor;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.sdf.SdfData;
import pt.lsts.neptus.mra.importers.sdf.SdfHeader;
import pt.lsts.neptus.mra.importers.sdf.SdfParser;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.bathymetry.TidePredictionFactory;

/**
 * Apply corrected position to 83P and re-exported.  
 * @author pdias
 * 
 */
@PluginDescription
public class SDFToCorrectedPosSDF implements MRAExporter {
    @SuppressWarnings("unused")
    private IMraLogGroup log = null;
    private CorrectedPosition correctedPosition = null;

    private SdfParser sdfParser = null;

//    private FileInputStream fis;
    private RandomAccessFile raFile;
    private FileChannel channel;
    private ByteBuffer buf;
    private long curPos = 0;

    public SDFToCorrectedPosSDF(IMraLogGroup log) {
        this.log = log;
    }
    
    public String getName() {
        return "SDF to SDF with Corrected Position";
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return SdfParser.canBeApplied(source);
    }

    private CorrectedPosition getCorrectedPosition(IMraLogGroup source) {
        if (correctedPosition == null)
            correctedPosition = new CorrectedPosition(source);

        return correctedPosition;
    }

    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        if (!canBeApplied(source))
            return "No data to process!";
        
        log = source;
        
        if (pmonitor != null)
            pmonitor.setNote(I18n.text("Copying file"));
        
        File[] sourceFiles = SdfParser.findDataSource(source, true);
        
        // Removed the corrected files
        List<File> sourceList = Arrays.asList(sourceFiles);
        sourceList = sourceList.parallelStream().filter(f -> !f.getName().toLowerCase().endsWith("corrected.sdf"))
                .collect(Collectors.toList());
        
        for (File fileOrig : sourceList) {
            
            String destName = FileUtil.getFileNameWithoutExtension(fileOrig) + "Corrected" + "." + FileUtil.getFileExtension(fileOrig);
            
            File fileDest =  new File(fileOrig.getParent(), destName);
            boolean ret = FileUtil.copyFile(fileOrig.getPath(), fileDest.getPath());
            if (!ret) {
                return "Unable to copy data!";
            }
            
            try {
                raFile = new RandomAccessFile(fileDest, "rw");
            }
            catch (FileNotFoundException e1) {
                return "Error getting channel for SDF file!";
            }
            
            if (pmonitor != null)
                pmonitor.setNote(I18n.text("Working"));
            
            channel = raFile.getChannel();
            //// !!! The parser is reading the original file and we have to change the new one !!! 
            // sdfParser = new SdfParser(fileOrig);
            correctedPosition = getCorrectedPosition(source);
            
            SdfHeader header = new SdfHeader();
            SdfData ping = new SdfData();
            
            long count = 0;
//            long pos = 0;
            long curPosition = 0;
            
            try {
                while (true) {
                    // Read the header
                    ByteBuffer buf = channel.map(MapMode.READ_WRITE, curPosition, 512); //header size 512bytes
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    header.parse(buf);
                    
                    //if (header.getPageVersion() == SdfParser.SUBSYS_HIGH || header.getPageVersion() == SdfParser.SUBSYS_LOW) {
                    //}
                    
                    long timestampMillis = SdfData.calculateTimeStampMillis(header);
                    SystemPositionAndAttitude vehPos = correctedPosition.getPosition(timestampMillis / 1E3);
                    LocationType posLoc = vehPos.getPosition();
                    posLoc = posLoc.getNewAbsoluteLatLonDepth();
                    
                    // setShipLat(buffer.getDouble(148));
                    buf.putDouble(148, posLoc.getLatitudeRads());
                    // setShipLon(buffer.getDouble(156));
                    buf.putDouble(156, posLoc.getLongitudeRads());
                    
                    // Fix the depth value with the tide
                    // setAuxDepth(buffer.getFloat(208));
                    // depth + alt - TidePredictionFactory.getTideLevel(sample.getTimestampMillis()));
                    float newDepth = header.getAuxDepth();
                    newDepth += - TidePredictionFactory.getTideLevel(timestampMillis);
                    buf.putFloat(208, newDepth);
                    
                    // ping marker is the plus 4 bytes before the SDF Page
                    curPosition += (4 + header.getNumberBytes());
                    count++;

                    if (curPosition >= channel.size())
                        break;
                    
                    if (pmonitor != null && pmonitor.isCanceled())
                        return "Export to Corrected Position SDF interrupted!";
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                return "Export to Corrected Position SDF completed with errors! (" + e.getMessage() + ")";
            }
            finally {
                if (channel != null) {
                    try {
                        channel.force(true);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        channel.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (raFile != null) {
                    try {
                        raFile.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return "Export to Corrected Position SDF completed successfully";
    }

    public static void main(String[] args) {
        BatchMraExporter.apply(SDFToCorrectedPosSDF.class);
    }
}
