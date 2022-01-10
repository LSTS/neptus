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
 * 25/08/2014
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.util.Arrays;

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
import pt.lsts.neptus.util.FileUtil;

/**
 * Apply corrected position to 83P and re-exported.  
 * @author pdias
 * 
 */
@PluginDescription
public class DeltaT83PToCorrectedPos83P implements MRAExporter {
    @SuppressWarnings("unused")
    private IMraLogGroup log = null;
    private CorrectedPosition correctedPosition = null;

    private DeltaTParser deltaParser = null;

//    private FileInputStream fis;
    private RandomAccessFile raFile;
    private FileChannel channel;
    private ByteBuffer buf;
    private long curPos = 0;

    public DeltaT83PToCorrectedPos83P(IMraLogGroup log) {
        this.log = log;
    }
    
    public String getName() {
        return "83P to 83P with Corrected Position";
    }
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return DeltaTParser.canBeApplied(source);
    }

    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        if (!canBeApplied(source))
            return "No data to process!";
        
        log = source;
        
        if (pmonitor != null)
            pmonitor.setNote(I18n.text("Copying file"));
        
        File fileOrig = DeltaTParser.findDataSource(source);
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
            return "Error getting channel for 83P file!";
        }

        if (pmonitor != null)
            pmonitor.setNote(I18n.text("Working"));

        channel = raFile.getChannel();
        deltaParser = new DeltaTParser(source);
        correctedPosition = deltaParser.getCorrectedPosition();
        
        BathymetrySwath nextSwath = null; 
        
        try {
            nextSwath = deltaParser.nextSwath();
            while (nextSwath != null) {
                // 33-46    -   GNSS Ships Positon Latitude (14 bytes) "_dd.mm.xxxxx_N" dd = degrees, mm = minutes, xxxxx = decimal Minutes, _ = Space, N = North or S = South
                // 47-60    -   GNSS Ships Postion Longitude (14 byes) "ddd.mm.xxxxx_E" ddd= degrees, mm = minutes, xxxxx = decimal minutes, E = East or W = West
                
                buf = channel.map(MapMode.READ_WRITE, curPos, 256);
                DeltaTHeader header = deltaParser.getCurrentHeader();
                curPos = deltaParser.getCurrentPosition() - header.numBytes;
                
                long nextSwathTimeStamp = nextSwath.getTimestamp();
                SystemPositionAndAttitude pos = correctedPosition.getPosition(nextSwathTimeStamp / 1E3);
                LocationType posLoc = pos.getPosition();
                posLoc = posLoc.getNewAbsoluteLatLonDepth();
                
                String lat83P = CoordinateUtil.latTo83PFormatWorker(posLoc.getLatitudeDegs());
                String lon83P = CoordinateUtil.lonTo83PFormatWorker(posLoc.getLongitudeDegs());
                
                byte[] latBytes = new byte[14];
                latBytes = lat83P.getBytes(Charset.forName("ASCII"));
                buf.position(33);
                buf.put(latBytes);

                byte[] lonBytes = new byte[14];
                lonBytes = lon83P.getBytes(Charset.forName("ASCII"));
                buf.position(47);
                buf.put(lonBytes);

                nextSwath = deltaParser.nextSwath();
                
                if (pmonitor != null && pmonitor.isCanceled())
                    return "Export to 83P interrupted!";
            }
            
            return "Export to 83P completed successfully";
        }
        catch (Exception e) {
            e.printStackTrace();
            return "Export to 83P completed with errors! (" + e.getMessage() + ")";
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

    public static void main(String[] args) {
        
        double[] valsLat = { 38.276276276766, -9.276276276766, 41 };
        for (double val : valsLat) {
            System.out.print(val);
            System.out.print("\t::\t");
            System.out.print(CoordinateUtil.latitudeAsPrettyString(val));
            System.out.print("\t::\t");
            String fmt = CoordinateUtil.latTo83PFormatWorker(val);
            System.out.print(fmt);
            System.out.print("\t::\t");
            System.out.println(CoordinateUtil.latFrom83PFormatWorker(fmt));
        }

        double[] valsLon = { 13.453, 122.45334343434, 12.45334343434, -2.45334343434, 2 };
        for (double val : valsLon) {
            System.out.print(val);
            System.out.print("\t::\t");
            System.out.print(CoordinateUtil.longitudeAsPrettyString(val));
            System.out.print("\t::\t");
            String fmt = CoordinateUtil.lonTo83PFormatWorker(val);
            System.out.print(fmt);
            System.out.print("\t::\t");
            System.out.println(CoordinateUtil.lonFrom83PFormatWorker(fmt));
        }
        
        byte[] by = " 38.22.35324 N".getBytes(Charset.forName("ASCII"));
        System.out.println(by.length);
        System.out.println(by);
        
        System.out.println(Arrays.toString(by));
        try {
            System.out.println(new String(by, "ASCII"));
        }
        catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
