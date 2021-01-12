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
 * Author: José Correia
 * Jan 8, 2013
 */
package pt.lsts.neptus.mra.exporters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ProgressMonitor;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * 83P to Point Cloud Data exporter
 * @author jqcorreia
 */
@PluginDescription(name="83P to PCD")
public class PCDExporter implements MRAExporter{
    public double minLat = 180;
    public double maxLat = -180;
    public double minLon = 360;
    public double maxLon = -360;

    public double minHeight = 1000;
    public double maxHeight = -1;

    LocationType topLeftLT;
    LocationType bottomRightLT;
    
    File f, output;
    IMraLogGroup source;
    
    public PCDExporter(IMraLogGroup source) {
        this.source = source;
        File baseFolder = source.getFile(".").getParentFile();
        f = source.getFile("multibeam.83P");
        // @JQCorreia Why should we assume the file 83P exists??????? Result NullPointer and no MRA opening!!
        // output = new File(f.getParent()+"multibeam.pcd");
        output = new File(baseFolder, "multibeam.pcd");
    }
    
    @SuppressWarnings("unused")
    public String process(IMraLogGroup source, ProgressMonitor pmonitor) {
        RandomAccessFile buf;
        FileInputStream fis;
        FileChannel channel;
        ByteBuffer b;
        int curPosition = 0;
        
        RandomAccessFile out;
        short numBeams = 0;
        short samplesPerBeam;
        byte intensity;
        short pingTotalSize;
        short rangeResolution;
        float altitude;
        float startAngle;
        float angleIncrement;
        short sectorSize;
        short acousticRange;
        int pingCount = 0;
        IMraLog esLog;
        IMCMessage esMsg;
        List<Double[]> pointList = new LinkedList<Double[]>();
        double heading;
        
        esLog = source.getLog("EstimatedState");
        esLog.firstLogEntry();

        
        try {
            buf = new RandomAccessFile(f, "r");
            out = new RandomAccessFile(output, "rw");
            fis = new FileInputStream(f);
            channel = fis.getChannel();
            
            long currPingStartOffset;
            byte[] dataBuffer = null;
            int[] data = null;
            int c = 0;
            
            String timestampStr;
            String millisStr;

            byte timestampBuffer[] = new byte[25];
            byte millisBuffer[] = new byte[5];

            int month = 0;

            Pattern pTimeStamp = Pattern.compile("([0-9]{2})-([A-Z]{3})-([0-9]{4})\0([0-9]{2}):([0-9]{2}):([0-9]{2})");
            Matcher m;

            Calendar cal = Calendar.getInstance();
            LocationType tempLt = new LocationType();
            double lat = 0, lon = 0;
            int numOfEntries = 0;
            
            NeptusLog.pub().info("<###>Reading 83P Profile Information");
            while (curPosition < channel.size()) {
                // Process Header ---------
                b = channel.map(MapMode.READ_ONLY, curPosition, 256); // Read header from file to buffer
                pingTotalSize = b.getShort(4);

                numBeams = b.getShort(70);
                numOfEntries += numBeams;
                
                curPosition += pingTotalSize;
            }            
            NeptusLog.pub().info("<###>Final number of entries: " + numOfEntries);
            out.writeBytes("VERSION .7\n");
            out.writeBytes("FIELDS x y z\n");
            out.writeBytes("SIZE 4 4 4\n");
            out.writeBytes("TYPE F F F\n");
            out.writeBytes("COUNT 1 1 1\n");
            out.writeBytes("WIDTH " + numOfEntries + "\n");
            out.writeBytes("HEIGHT 1\n");
            out.writeBytes("VIEWPOINT 0 0 0 1 0 0 0\n");
            out.writeBytes("POINTS " + numOfEntries + "\n");
            out.writeBytes("DATA ascii\n");
            
            curPosition = 0;
            NeptusLog.pub().info("<###>Processing Information");
            while (curPosition < channel.size()) {
                // Process Header ---------
                b = channel.map(MapMode.READ_ONLY, curPosition, 256); // Read header from file to buffer
                pingTotalSize = b.getShort(4);

                // Read timestamp information and process it
                b.position(8);
                b.get(timestampBuffer);
                timestampStr = new String(timestampBuffer);

                numBeams = b.getShort(70);
                
                b.position(112);
                b.get(millisBuffer);
                millisStr = new String(millisBuffer);
               
                m = pTimeStamp.matcher(timestampStr);
                m.find();

                if (m.group(2).equalsIgnoreCase("JAN"))
                    month = 0;
                if (m.group(2).equalsIgnoreCase("FEB"))
                    month = 1;
                if (m.group(2).equalsIgnoreCase("MAR"))
                    month = 2;
                if (m.group(2).equalsIgnoreCase("APR"))
                    month = 3;
                if (m.group(2).equalsIgnoreCase("MAY"))
                    month = 4;
                if (m.group(2).equalsIgnoreCase("JUN"))
                    month = 5;
                if (m.group(2).equalsIgnoreCase("JUL"))
                    month = 6;
                if (m.group(2).equalsIgnoreCase("AUG"))
                    month = 7;
                if (m.group(2).equalsIgnoreCase("SEP"))
                    month = 8;
                if (m.group(2).equalsIgnoreCase("OCT"))
                    month = 9;
                if (m.group(2).equalsIgnoreCase("NOV"))
                    month = 10;
                if (m.group(2).equalsIgnoreCase("DEC"))
                    month = 11;

                cal.set(Integer.valueOf(m.group(3)), month, Integer.valueOf(m.group(1)), Integer.valueOf(m.group(4)),
                        Integer.valueOf(m.group(5)), Integer.valueOf(m.group(6)));
                cal.set(Calendar.MILLISECOND, Integer.valueOf(millisStr.substring(1, 4)));

                // Get EstimatedState based on calculated timestamp
                esMsg = esLog.getEntryAtOrAfter(cal.getTimeInMillis());
                heading = Math.toDegrees(esMsg.getDouble("psi"));
                
                if(lat != esMsg.getDouble("lat") && lon != esMsg.getDouble("lon")) {
                    lat = esMsg.getDouble("lat");
                    lon = esMsg.getDouble("lon");
                }
                
                numBeams = b.getShort(70);
                samplesPerBeam = b.getShort(72);
                acousticRange = b.getShort(79);
                rangeResolution = b.getShort(85);
                intensity = b.get(117);
                altitude = b.getFloat(133);
                sectorSize = b.getShort(74);
                startAngle = b.getShort(76) / 100f - 180;
                angleIncrement = b.get(78) / 100f;

                if(data == null)
                    data = new int[numBeams];
                
                b = channel.map(MapMode.READ_ONLY, curPosition + 256, numBeams * 2);
                float range;
                double height;
                
                for(int i = 0; i < numBeams; i++) {
                    data[i] = ((b.get(2*i) << 8) & 0xFF) + (b.get(2 * i + 1) & 0xFF);
                    
                    range = data[i] * rangeResolution / 1000f;
                    double angle = startAngle + angleIncrement * i;

                    double foo = range * Math.cos(Math.toRadians(angle));
                    height = range * Math.cos(Math.toRadians(angle)) + esMsg.getDouble("depth");

                    double x = range * Math.sin(Math.toRadians(angle));
                    double theta = -heading;
                    
                    double offHeight[] = { esMsg.getDouble("x") + (x * Math.cos(Math.toRadians(theta))),
                                           esMsg.getDouble("y") + (x * Math.sin(Math.toRadians(theta))),
                                           height };
                    if (height < minHeight)
                        minHeight = height;
                    if (height > maxHeight)
                        maxHeight = height;

                    out.writeBytes(offHeight[0] + " " + offHeight[1] + " " + (offHeight[2] /* * 10*/) + "\n");
                }
                
                pingCount++;
                curPosition += pingTotalSize;
            }
            NeptusLog.pub().info("<###>Wrinting PCD file");

            buf.close();
            fis.close();
            out.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            return I18n.textf("%ExceptionClass while exporting to PCD: %error", e.getClass().getSimpleName(), e.getMessage());
        }
        
        return I18n.text("Log exported sucessfully");
    }
    
    public static void main(String[] args) {
        try {
            IMraLogGroup source = new LsfLogSource(new File(args[0]+"/Data.lsf"), null);
            PCDExporter pcde = new PCDExporter(source);
            pcde.process(source, null);
        }
        
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return source.getFile("multibeam.83P") != null;
    }
}
