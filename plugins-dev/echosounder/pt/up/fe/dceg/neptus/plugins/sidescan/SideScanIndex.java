/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Sep 12, 2011
 */
package pt.up.fe.dceg.neptus.plugins.sidescan;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.LinkedHashMap;
import java.util.Vector;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.llf.LogUtils;

/**
 * This class is used to hold sidescan data which can be loaded from LLF and converted to a faster binary format
 * @author zp
 */
public class SideScanIndex {

    protected File indexFile;
    protected File dataFile;
    protected File marksFile;

    /**
     * Used for accessing sidescan data (binary format)
     */
    protected MappedByteBuffer data;

    /**
     * Holds all the timestamps for each sidescan ping
     */
    protected Vector<Long> timestamps = new Vector<Long>();

    /**
     * Holds all the sidescan pings along with their position data. The keys are the timestamps of each ping
     */
    protected LinkedHashMap<Long, SidescanIndexEntry> entries = new LinkedHashMap<Long, SidescanIndexEntry>();

    /**
     * Holds marks added by human operators
     */
    protected Vector<SideScanMark> marks = new Vector<SideScanMark>();

    /**
     * Try to load an alrady existing binary plot from given source and, if it doesn't exist creates it and loads it
     * @param source The log source where to look for / generate binary sidescan index
     * @return All the sidescan data as a SideScanIndex
     */
    public static SideScanIndex loadIndex(IMraLogGroup source) {
        SideScanIndex index = new SideScanIndex();

        index.indexFile = source.getFile("sidescan-index.mra");
        index.dataFile = source.getFile("sidescan-data.mra");
        index.marksFile = source.getFile("sidescan-marks.mra");

        if (!SideScanIndex.isIndexGenerated(source)) {
            NeptusLog.pub().warn("Generating sidescan index files...");
            try {
                SideScanIndex.generateIndex(source);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                e.printStackTrace();
            }
            File folder = null;
            if (source.getFile("IMC.xml") != null)
                folder = source.getFile("IMC.xml").getParentFile();     
            else if (source.getLog("EstimatedState") != null)
                folder = source.getFile("EstimatedState").getParentFile();

            if (folder == null)
                return null;

            index.indexFile = new File(folder, "sidescan-index.mra");
            index.dataFile = new File(folder, "sidescan-data.mra");
            index.marksFile = new File(folder, "sidescan-marks.mra");
        }

        try {
            FileInputStream is = new FileInputStream(index.dataFile);
            FileChannel channel = is.getChannel();
            index.data = channel.map(MapMode.READ_ONLY, 0, index.dataFile.length());
            int numEntries = (int)index.indexFile.length() / SidescanIndexEntry.FRAME_SIZE;
            DataInputStream dis = new DataInputStream(new FileInputStream(index.indexFile));

            for (int i = 0; i < numEntries; i++) {
                SidescanIndexEntry entry = new SidescanIndexEntry();
                entry.read(dis);
                index.timestamps.add(entry.timestamp);
                index.entries.put(entry.timestamp, entry);
            }

            if (index.marksFile != null && index.marksFile.canRead()) {
                try {
                    index.marks = SideScanMark.read(index.marksFile);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Read "+index.marks.size()+" marks");
            }
            is.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return index;
    }

    /**
     * @param time The time
     * @return the SidescanIndexEntry at time time
     */
    public SidescanIndexEntry getEntryAt(long time) {
        return entries.get(time);
    }

    /**
     * @param index Integer index in the vector of entries
     * @return the SidescanIndexEntry at index index
     */
    public SidescanIndexEntry getEntryAt(int index) {
        if (index >= timestamps.size())
            return null;
        return entries.get(timestamps.get(index));
    }

    /**
     * Retrieve the number of pings in this sidescan index
     * @return the number of pings in this sidescan index
     */
    public int getNumEntries() {
        return timestamps.size();
    }

    /**
     * @return the marks
     */
    public Vector<SideScanMark> getMarks() {
        return marks;
    }

    /**
     * @param marks the marks to set
     */
    public void setMarks(Vector<SideScanMark> marks) {
        this.marks = marks;
    }

    /**
     * Retrieve the raw sidescan data for given entry (to be shown using a colormap)
     * @param entry A sidescan index entry
     * @return The sidescan data belonging to the given entry
     */
    public byte[] getData(SidescanIndexEntry entry) {
        data.position((int)entry.offset);
        byte ret[] = new byte[entry.numBytes];
        data.get(ret);
        return ret;
    }

    /**
     * Tests if the index was already generated in the given log source
     * @param logSource The log source where to look for sidescan index
     * @return <b>true</b> if the index was already generated at the given log source
     */
    public static boolean isIndexGenerated(IMraLogGroup logSource) {
        return logSource.getFile("sidescan-index.mra") != null;     
    }

    /**
     * Generates new index files at the given log source
     * @param source Where to generate the index files
     * @return <b>false</b> if it wasn't possible to generate the index
     * @throws IOException If it's not possible to read/write some file, etc
     */
    public static boolean generateIndex(IMraLogGroup source) throws Exception {
        File folder = null;

               
        if (source.getFile("IMC.xml") != null)
            folder = source.getFile("IMC.xml").getParentFile();     

        if (folder == null)
            return false;

        File indexFile = new File(folder, "sidescan-index.mra");
        File dataFile = new File(folder, "sidescan-data.mra");
        File marksFile = new File(folder, "sidescan-marks.mra");
        marksFile.createNewFile();

        DataOutputStream dataOs = new DataOutputStream(new FileOutputStream(dataFile));
        DataOutputStream indexOs = new DataOutputStream(new FileOutputStream(indexFile));

        IMraLog llfPing = source.getLog("SidescanPing");
        IMraLog llfBDistance = source.getLog("BottomDistance");
        IMraLog llfState = source.getLog("EstimatedState");     

        if (llfPing == null || llfState == null) {
            dataOs.close();
            indexOs.close();
            return false;
        }
        
        IMCMessage pingEntry = llfPing.firstLogEntry();
        while (pingEntry != null) {
            if(!source.getEntityName(pingEntry.getInteger("src"), pingEntry.getInteger("src_ent")).equalsIgnoreCase("Sidescan")) {
                pingEntry = llfPing.nextLogEntry();
                continue;
            }
            IMCMessage stateEntry = llfState.getEntryAtOrAfter(pingEntry.getTimestampMillis());
            
            if (stateEntry == null)
                break;
            
            SidescanIndexEntry indexEntry = new SidescanIndexEntry();

            LocationType loc = new LocationType(LogUtils.getLocation(stateEntry)
                    .convertToAbsoluteLatLonDepth());
            byte[] data = pingEntry.getRawData("data");

            int min = 255, max = 0;
            
            for (int i = 0; i < data.length; i++) {
                if (data[i] < min)
                    min = data[i];
                if (data[i] > max)
                    max = data[i];
            }

            indexEntry.timestamp = pingEntry.getTimestampMillis();        
            indexEntry.latitude = loc.getLatitudeAsDoubleValue();
            indexEntry.longitude = loc.getLongitudeAsDoubleValue();
            indexEntry.yawDegs = (float)Math.toDegrees(stateEntry.getDouble("psi"));
            indexEntry.numBytes = (short)data.length;
//            indexEntry.speed = (float) stateEntry.getFloat("u"); //TODO Check conversion
            indexEntry.speed = (float) Math.sqrt(Math.pow(stateEntry.getDouble("vx"),2) + Math.pow(stateEntry.getDouble("vy"),2)) * 2f;
            System.out.println(indexEntry.speed);
            indexEntry.offset = dataOs.size();
            indexEntry.opFreq = pingEntry.getInteger("frequency");
            indexEntry.range = pingEntry.getInteger("range");
            try {
                if (llfBDistance != null) {
                    IMCMessage bdMsg = llfBDistance.getEntryAtOrAfter(indexEntry.timestamp);
                    System.out.println(source.getEntityName(bdMsg.getInteger("src"), bdMsg.getInteger("src_ent")));
                    
                    if(source.getEntityName(bdMsg.getInteger("src"), bdMsg.getInteger("src_ent")).equals("Depth & Heading Control")) {
                        System.out.println("a");
                        indexEntry.altitude = (float) llfBDistance.getEntryAtOrAfter(indexEntry.timestamp).getFloat("value");
                    }
                    else {
                        while((bdMsg = llfBDistance.nextLogEntry()) != null) {
                            System.out.println(source.getEntityName(bdMsg.getInteger("src"), bdMsg.getInteger("src_ent")));
                            if(source.getEntityName(bdMsg.getInteger("src"), bdMsg.getInteger("src_ent")).equals("Depth & Heading Control")) {
                                System.out.println("b");
                                indexEntry.altitude = (float) llfBDistance.getEntryAtOrAfter(indexEntry.timestamp).getFloat("value");
                                break;
                            }
                        }
                    }
                }
            }
            catch (Exception e) { }
            
            
            indexEntry.write(indexOs);            
            dataOs.write(data);

            
            pingEntry = llfPing.nextLogEntry();
            
        }

        indexOs.close();
        dataOs.close();

        return true;
    }
    
    public void addMark(SideScanMark mark) throws IOException {
        marks.add(mark);
        SideScanMark.write(marksFile, marks);
    }
    
}
