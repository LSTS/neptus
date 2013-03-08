/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Jul 7, 2012
 * $Id:: MultibeamData.java 10011 2013-02-21 14:17:25Z zepinto                  $:
 */
package pt.up.fe.dceg.neptus.plugins.multibeam;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author jqcorreia
 * 
 */
public class MultibeamData implements Serializable {
    private static final long serialVersionUID = 1L;

    public double minLat = 180;
    public double maxLat = -180;
    public double minLon = 360;
    public double maxLon = -360;

    public double minHeight = 1000;
    public double maxHeight = -1;
    
    public ArrayList<Double[]> locationList = new ArrayList<Double[]>();

    LocationType topLeftLT;
    LocationType bottomRightLT;

    private MultibeamData(File f, IMraLogGroup source) {
        process(f, source);
    }

    public static MultibeamData build(File f, IMraLogGroup source) {
        File cacheFile = new File(".cache/multibeam/" + source.getFile("Data.lsf").getParentFile().getName() + ".raw");
//        if (cacheFile.exists()) {
//            return deserialize(cacheFile);
//        }
//        else {
            MultibeamData obj = new MultibeamData(f, source);
            serialize(cacheFile, obj);
            return obj;
//        }
    }

    static MultibeamData deserialize(File f) {
        try {
            InputStream fileStream = new FileInputStream(f);
            InputStream buffer = new BufferedInputStream(fileStream);
            ObjectInput input = new ObjectInputStream(buffer);
            MultibeamData res = (MultibeamData) input.readObject();

            buffer.close();
            input.close();
            return res;
        }
        catch (Exception e) {
            return null;
        }
    }

    static void serialize(File f, MultibeamData obj) {
        try {
            OutputStream fileStream = new FileOutputStream(f);
            OutputStream buffer = new BufferedOutputStream(fileStream);
            ObjectOutput output = new ObjectOutputStream(buffer);

            output.writeObject(obj);
            buffer.flush();
            output.close();
        }
        catch (Exception e) {

        }
    }
    @SuppressWarnings("unused")
    public void process(File f, IMraLogGroup source) {
        RandomAccessFile buf;
        
        short numBeams;
        short samplesPerBeam;
        byte intensity;
        short pingTotalSize;
        short rangeResolution;
        float altitude;
        float startAngle;
        float angleIncrement;
        short sectorSize;
        short acousticRange;

        IMraLog esLog;
        IMCMessage esMsg;

        double heading;
        
        esLog = source.getLog("EstimatedState");
        esLog.firstLogEntry();

        try {
            FileInputStream fin = new FileInputStream(f);
            
            FileChannel fis = fin.getChannel();
            
            
            buf = new RandomAccessFile(f, "r");
            
            int filePos = 0;
            ByteBuffer buffer;
            
            long currPingStartOffset;
            byte[] dataBuffer;
            int[] data;
            int c = 0;
            double lat;
            double lon;

            String timestampStr;
            String millisStr;

            byte timestampBuffer[] = new byte[25];
            byte millisBuffer[] = new byte[5];

            int month = 0;

            Pattern pTimeStamp = Pattern.compile("([0-9]{2})-([A-Z]{3})-([0-9]{4})\0([0-9]{2}):([0-9]{2}):([0-9]{2})");
            Matcher m;

            Calendar cal = Calendar.getInstance();
            LocationType tempLt = new LocationType();
            
            buf.seek(0); // Reset
            while (buf.getFilePointer() < buf.length()) {
                currPingStartOffset = buf.getFilePointer();
                // Skip '83P' string and version number
                buf.skipBytes(4);

                pingTotalSize = buf.readShort();

                // Read timestamp information and process it
                buf.seek(currPingStartOffset + 8);
                buf.read(timestampBuffer, 0, 25);
                timestampStr = new String(timestampBuffer);

                buf.seek(currPingStartOffset + 112);
                buf.read(millisBuffer, 0, 5);
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

                esMsg = esLog.getEntryAtOrAfter(cal.getTimeInMillis());
                heading = Math.toDegrees(esMsg.getDouble("psi"));

                // Read LatLon information
                double r[] = CoordinateUtil.latLonAddNE2(Math.toDegrees(esMsg.getDouble("lat")),
                        Math.toDegrees(esMsg.getDouble("lon")), esMsg.getDouble("x"), esMsg.getDouble("y"));
                lat = r[0];
                lon = r[1];

                if (lat < minLat)
                    minLat = lat;
                if (lon < minLon)
                    minLon = lon;
                if (lat > maxLat)
                    maxLat = lat;
                if (lon > maxLon)
                    maxLon = lon;
                
                // Skip to the end of the ping
                buf.seek(currPingStartOffset + pingTotalSize);
            }
            
            topLeftLT = new LocationType(maxLat, minLon);
            bottomRightLT = new LocationType(minLat, maxLon);

            topLeftLT.setOffsetNorth(10);
            topLeftLT.setOffsetEast(-10);
            bottomRightLT.setOffsetNorth(-10);
            bottomRightLT.setOffsetEast(10);
            
            // SECOND CYCLE
            esLog.firstLogEntry();
            buf.seek(0); // Reset
            while (buf.getFilePointer() < buf.length()) {
                currPingStartOffset = buf.getFilePointer();
                // Skip '83P' string and version number
                buf.skipBytes(4);

                pingTotalSize = buf.readShort();

                // Read timestamp information and process it
                buf.seek(currPingStartOffset + 8);
                buf.read(timestampBuffer, 0, 25);
                timestampStr = new String(timestampBuffer);

                buf.seek(currPingStartOffset + 112);
                buf.read(millisBuffer, 0, 5);
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

                esMsg = esLog.getEntryAtOrAfter(cal.getTimeInMillis());
                heading = Math.toDegrees(esMsg.getDouble("psi"));

                // Read LatLon information
                double r[] = CoordinateUtil.latLonAddNE2(Math.toDegrees(esMsg.getDouble("lat")),
                        Math.toDegrees(esMsg.getDouble("lon")), esMsg.getDouble("x"), esMsg.getDouble("y"));
                lat = r[0];
                lon = r[1];

                tempLt.setLatitude(lat);
                tempLt.setLongitude(lon);

                buf.seek(currPingStartOffset + 70);
                numBeams = buf.readShort();

                samplesPerBeam = buf.readShort();

                buf.seek(currPingStartOffset + 79);
                acousticRange = buf.readShort();

                buf.seek(currPingStartOffset + 85);
                rangeResolution = buf.readShort();

                buf.seek(currPingStartOffset + 117);
                intensity = (byte) buf.read();

                buf.seek(currPingStartOffset + 133);
                altitude = buf.readFloat();

//                buf.seek(currPingStartOffset + 68);
//                heading = buf.readShort();

                buf.seek(currPingStartOffset + 74);
                sectorSize = buf.readShort();
                startAngle = buf.readShort() / 100f - 180;
                angleIncrement = buf.read() / 100f;

                // System.out.println(numBeams + " " + intensity + " " + pingTotalSize + " " + samplesPerBeam + " "
                // + altitude + " $ " + heading + " " + startAngle + " " + angleIncrement + " " + sectorSize + " " +
                // acousticRange + " ");

                dataBuffer = new byte[numBeams * 2];
                data = new int[numBeams];

                // Skip to beam range information
                buf.seek(currPingStartOffset + 256);
                float range;
                double height;
                
                if (c == 0) {
                    System.out.println(heading);
                }

                for (int i = 0; i < dataBuffer.length; i += 2) {
                    int t = i / 2;

                    
                    data[t] = buf.readUnsignedShort();
                    
                    if(data[t] == 0) {
                        continue;
                    }
                    
                    range = data[t] * rangeResolution / 1000f;
                    double angle = startAngle + angleIncrement * t;
                    
                    height = range * Math.cos(Math.toRadians(angle)) + esMsg.getDouble("depth");

                    double x = range * Math.sin(Math.toRadians(angle));
                    double theta = -heading;
                    tempLt.setOffsetEast(x * Math.cos(Math.toRadians(theta)));
                    tempLt.setOffsetNorth(x * Math.sin(Math.toRadians(theta)));
                    double offset[] = tempLt.getDistanceInPixelTo(topLeftLT, 22);
                    Double offHeight[] = {offset[0], offset[1], height};
                    locationList.add(offHeight);
                    
                    if(height < minHeight) minHeight = height;
                    if(height > maxHeight) maxHeight = height;
                }

                // Skip to the end of the ping
                buf.seek(currPingStartOffset + pingTotalSize);
                c++;

            }


            System.out.println("Total ping number " + c);

            // Reset buffer
            buf.seek(0);
            fin.close();
            // ColorMapUtils.getInterpolatedData(matrix,cm, (Graphics2D)img.getGraphics(), w,h,1);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
