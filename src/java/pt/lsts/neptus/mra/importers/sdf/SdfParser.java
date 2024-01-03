/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Manuel R.
 * Oct 21, 2014
 */
package pt.lsts.neptus.mra.importers.sdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;

public class SdfParser {
    // Minimum valid timestamp (2000-01-01 00:00:00).
    private static final long minimumValidTimestamp = 946684800000L;
    private File file;
    private FileInputStream fis;
    private FileChannel channel;
    private long curPosition = 0;
    private SdfIndex index = new SdfIndex();
    private boolean multipleFiles = false;
    private String indexPath;

    private LinkedHashMap<Integer, Long[]> tslist = new LinkedHashMap<Integer, Long[]>();
    private LinkedHashMap<Integer, Long> nextTimestamp = new LinkedHashMap<Integer, Long>();
    private LinkedHashMap<File, SdfIndex> fileIndex = new LinkedHashMap<>();

    private ArrayList<Long[]> tsSHigh = new ArrayList<>();
    private ArrayList<Long[]> tsSLow = new ArrayList<>();

    final static int SUBSYS_LOW = 3501;
    final static int SUBSYS_HIGH = 3502;
    final static int BATHY_PULSE_COMPRESSED = 3503;

    public SdfParser(File file) {
        try {
            this.file = file;
            fis = new FileInputStream(file);
            channel = fis.getChannel();
            indexPath = file.getParent() + "/mra/sdf.index";

            if (!new File(indexPath).exists()) {
                NeptusLog.pub().info("Generating SDF index for " + file.getAbsolutePath());
                generateIndex();
            }
            else {
                NeptusLog.pub().info("Loading SDF index for " + file.getAbsolutePath());
                if(!loadIndex()) {
                    NeptusLog.pub().error("Corrupted SDF index file. Trying to create a new index.");
                    generateIndex();
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public SdfParser(File[] files) {
        multipleFiles = true;
        Arrays.sort(files);

        for (File file : files) {
            try {
                this.file = file;
                fis = new FileInputStream(file);
                channel = fis.getChannel();
                indexPath = file.getParent() + "/mra/sdf"+file.getName()+".index";

                if (!new File(indexPath).exists()) {
                    NeptusLog.pub().info("Generating SDF index for " + file.getAbsolutePath());
                    generateIndex();
                }
                else {
                    NeptusLog.pub().info("Loading SDF index for " + file.getAbsolutePath());
                    if(!loadIndex(file)) {
                        NeptusLog.pub().error("Corrupted SDF index file. Trying to create a new index.");
                        generateIndex();
                    }
                }
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        int sizeLow = 0;
        int sizeHigh = 0;

        for (Long[] set : tsSLow) {
            sizeLow = sizeLow + set.length;
        }

        for (Long[] set : tsSHigh) {
            sizeHigh = sizeHigh + set.length;
        }

        Long[] longHigh = new Long[sizeHigh];
        Long[] longLow = new Long[sizeLow];

        int count = 0;
        for (int i=0; i< tsSLow.size(); i++) {
            for (int j=0; j < tsSLow.get(i).length; j++) {
                longLow[count] = tsSLow.get(i)[j];
                count++;
            }
        }

        count = 0;
        for (int i=0; i< tsSHigh.size(); i++) {
            for (int j=0; j < tsSHigh.get(i).length; j++) {
                longHigh[count] = tsSHigh.get(i)[j];
                count++;
            }
        }

        tslist.put(SUBSYS_LOW, longLow);
        tslist.put(SUBSYS_HIGH, longHigh);

        //        for (Entry<File, SdfIndex> e : fileIndex.entrySet()) {
        //            System.out.println(e.getKey().getName() + /*" " + e.getValue().firstTimestampLow +*/ " "
        //                    + e.getValue().firstTimestampHigh+ " "/*+ e.getValue().lastTimestampLow +" "*/+  e.getValue().lastTimestampHigh);
        //        }
        //
        //        System.out.println(getFirstTimeStamp());
        //        System.out.println(getLastTimeStamp());
    }

    private void generateIndex() {

        SdfHeader header = new SdfHeader();
        SdfData ping = new SdfData();
        SdfIndex index2 = new SdfIndex();

        long maxTimestampHigh = 0;
        long maxTimestampLow = 0;
        long minTimestampHigh = Long.MAX_VALUE;
        long minTimestampLow = Long.MAX_VALUE;

        long count = 0;
        long pos = 0;
        curPosition = 0;
        try {
            Set<Integer> unimplementedPageVersionSet = new HashSet<>();
            while (true) {
                // Read the header
                ByteBuffer buf = channel.map(MapMode.READ_ONLY, curPosition, 512); //header size 512bytes
                buf.order(ByteOrder.LITTLE_ENDIAN);
                header.parse(buf);
                curPosition += header.getHeaderSize();
                //System.out.println("curPos " + curPosition);

                if (header.getPageVersion() == SUBSYS_HIGH || header.getPageVersion() == SUBSYS_LOW) {
                    //set header of this ping
                    ping.setHeader(header);
                    ping.calculateTimeStamp();
                    ping.calculateFixTimeStamp();
                    pos = curPosition-header.getHeaderSize();
                } else { //ignore other pageVersions
                    if (!unimplementedPageVersionSet.contains(header.getPageVersion())) {
                        unimplementedPageVersionSet.add(header.getPageVersion());
                        NeptusLog.pub().info("SDF Data file contains unimplemented pageVersion # " + header.getPageVersion());
                    }
                    curPosition += (header.getNumberBytes()+4) - header.getHeaderSize();
                    pos = curPosition;
                    if (curPosition >= channel.size()) //check if curPosition is at the end of file
                        break;
                    else
                        continue;
                }

                //get timestamp, freq and subsystem used
                long t = ping.getTimestamp(); // Timestamp
                long tfix = ping.getFixTimestamp(); // FixTimestamp
                int f = ping.getHeader().getSonarFreq(); // Frequency
                int subsystem = ping.getHeader().getPageVersion();
                //  System.out.println(pos+": ["+header.getPingNumber()+"] timestamp "+ t + " freq "+f + " subsys "+subsystem);

                if (!index2.frequenciesList.contains(f)) {
                    index2.frequenciesList.add(f);
                }
                if (!index2.subSystemsList.contains(subsystem)) {
                    index2.subSystemsList.add(subsystem);
                }

                if (t < 5000000) { // Fixing timestamp from 1970
                    NeptusLog.pub().warn(I18n.textf("Something is wrong with the timestamp (%d). " +
                                    "Trying to calculate using GPS data for ping %d for subsystem %d. New timestamp is %d.",
                            new Date(t), ping.getHeader().getPingNumber(), subsystem, new Date(tfix)));
                    t = tfix;
                }

                if(subsystem == SUBSYS_LOW) {
                    if(!index2.hasLow) index2.hasLow = true;

                    ArrayList<Long> l = index2.positionMapLow.get(t);
                    if (l == null) {
                        l = new ArrayList<Long>();
                        l.add(pos);
                        index2.positionMapLow.put(t, l);
                    }
                    else {
                        l.add(pos);
                    }

                    if (t > minimumValidTimestamp) {
                        minTimestampLow = Math.min(minTimestampLow, t);
                        maxTimestampLow = Math.max(maxTimestampLow, t);
                    }
                }

                if(subsystem == SUBSYS_HIGH) {
                    if(!index2.hasHigh) index2.hasHigh = true;

                    ArrayList<Long> l = index2.positionMapHigh.get(t);
                    if (l == null) {
                        l = new ArrayList<Long>();
                        l.add(pos);
                        index2.positionMapHigh.put(t, l);
                    }
                    else {
                        l.add(pos);
                    }
                    
                    if (t > minimumValidTimestamp) {
                        minTimestampHigh = Math.min(minTimestampHigh, t);
                        maxTimestampHigh = Math.max(maxTimestampHigh, t);
                    }
                }

                //end processing data

                curPosition += (header.getNumberBytes()+4) - header.getHeaderSize();
                count++;

                if (curPosition >= channel.size())
                    break;
            }

            index2.firstTimestampHigh = minTimestampHigh;
            index2.firstTimestampLow = minTimestampLow;

            index2.lastTimestampHigh = maxTimestampHigh;
            index2.lastTimestampLow = maxTimestampLow;

            // Save timestamp list
            Long[] tslisthigh;
            Long[] tslistlow;

            tslisthigh = index2.positionMapHigh.keySet().toArray(new Long[] {});
            tslistlow = index2.positionMapLow.keySet().toArray(new Long[] {});

            Arrays.sort(tslisthigh);
            Arrays.sort(tslistlow);

            if (tslisthigh.length >= 2968) {
                NeptusLog.pub().debug(">??>>>> >= 2968 >> HLength:" + tslisthigh.length + "|LLength:" + tslistlow.length +
                        ">" + tslisthigh[2967] + ", " + "----" + " | " +
                        (tslistlow.length >= 2968 ? tslistlow[2967] : "----") + ", " +
                        (tslistlow.length >= 2969 ? tslistlow[2968] : "----") +
                        " >> " + indexPath);
            }

            tslist.put(SUBSYS_LOW, tslistlow);
            tslist.put(SUBSYS_HIGH, tslisthigh);

            index2.numberOfPackets = count;

            index2.frequenciesList.sort(null);
            index2.subSystemsList.sort(null);

            ObjectOutputStream out = new ObjectOutputStream(new  FileOutputStream(indexPath));
            out.writeObject(index2);
            out.close();
            
            if (multipleFiles)
                fileIndex.put(file, index2);
        }
        catch (IOException e) {
            NeptusLog.pub().error("Found corrupted SDF file '" + file.getName() + "' while indexing. Error: " +
                    e.getMessage());
            // e.printStackTrace();
        }
    }

    private int hasAnyPageVersion(String... pageVersions) {
        ArrayList<Integer> pageVersionList = new ArrayList<>();

        Arrays.stream(pageVersions).forEachOrdered((pv) -> {
            try {
                pageVersionList.add(Integer.parseInt(pv));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });

        if (pageVersionList.isEmpty())
            return 0;

        SdfHeader header = new SdfHeader();
        curPosition = 0;
        try {
            while (true) {
                // Read the header
                ByteBuffer buf = channel.map(MapMode.READ_ONLY, curPosition, 512); //header size 512bytes
                buf.order(ByteOrder.LITTLE_ENDIAN);
                header.parse(buf);
                curPosition += header.getHeaderSize();
                //System.out.println("curPos " + curPosition);

                if (pageVersionList.stream().anyMatch((p) -> p == header.getPageVersion()))
                    return 1;

                curPosition += (header.getNumberBytes()+4) - header.getHeaderSize();
                if (curPosition >= channel.size()) //check if curPosition is at the end of file
                    break;
                else
                    continue;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public boolean loadIndex() {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(indexPath));
            index = (SdfIndex) in.readObject();

            Long[] tslisthigh;
            Long[] tslistlow;

            tslisthigh = index.positionMapHigh.keySet().toArray(new Long[] {});
            tslistlow = index.positionMapLow.keySet().toArray(new Long[] {});

            Arrays.sort(tslisthigh);
            Arrays.sort(tslistlow);

            tslist.put(SUBSYS_LOW, tslistlow);
            tslist.put(SUBSYS_HIGH, tslisthigh);

            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean loadIndex(File file) {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(indexPath));
            SdfIndex indexN = (SdfIndex) in.readObject();

            Long[] tslisthigh;
            Long[] tslistlow;

            tslisthigh = indexN.positionMapHigh.keySet().toArray(new Long[] {});
            tslistlow = indexN.positionMapLow.keySet().toArray(new Long[] {});

            Arrays.sort(tslisthigh);
            Arrays.sort(tslistlow);

            tsSHigh.add(tslisthigh);
            tsSLow.add(tslistlow);

            if (tslisthigh.length >= 2968) {
                NeptusLog.pub().debug(">?!>>>> >= 2968 >> " + file.getName() +
                        ">HLength:" + tslisthigh.length + "|LLength:" + tslistlow.length +
                        "> " + tslisthigh[2967] + ", " + "---" + " | " +
                        (tslistlow.length >= 2968 ? tslistlow[2967] : "----") + ", " +
                        (tslistlow.length >= 2969 ? tslistlow[2968] : "----"));
            }
            else {
                NeptusLog.pub().debug(">?!>>>>" + file.getName() + ">" + tslisthigh.length + "|" + tslistlow.length + ">");
            }

            fileIndex.put(file, indexN);

            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public long getFirstTimeStamp() {
        if (multipleFiles) {
            for (Entry<File, SdfIndex> entry : fileIndex.entrySet()) {
                return Math.min(entry.getValue().firstTimestampHigh, entry.getValue().firstTimestampLow);
            }
        }

        return Math.min(index.firstTimestampHigh, index.firstTimestampLow);
    }

    public long getLastTimeStamp() {
        if (multipleFiles) {
            Entry<File, SdfIndex> lastEntry = null;
            for (Entry<File, SdfIndex> entry : fileIndex.entrySet()) {
                lastEntry = entry;
            }

            return Math.max(lastEntry.getValue().lastTimestampHigh, lastEntry.getValue().lastTimestampLow);
        }

        return Math.max(index.lastTimestampHigh, index.lastTimestampLow);
    }

    public SdfData nextPing(int subsystem) {
        return getPingAt(nextTimestamp.get(subsystem), subsystem); // This fetches the next ping and updates nextTimestamp
    }

    /**
     * @return the index
     */
    public SdfIndex getIndex() {
        if (multipleFiles) {
            for (Entry<File, SdfIndex> entry : fileIndex.entrySet()) {
                if (entry.getKey() == file)
                    return entry.getValue();
            }
        }
        return index;
    }

    public SdfData getPingAtPosition(long pos, int subsystem) {
        long posHeader = pos;
        SdfHeader header = new SdfHeader();
        SdfData ping = new SdfData();
        try {
            // Map right file 
            if (multipleFiles) {
                fis = new FileInputStream(file);
                channel = fis.getChannel();
            }
            //
            ByteBuffer buf = channel.map(MapMode.READ_ONLY, pos, 512);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            header.parse(buf);
            pos += header.getHeaderSize();

            if(header.getPageVersion() != subsystem)
                return null;

            //define header
            ping.setHeader(header);
            ping.calculateTimeStamp();
            ping.calculateFixTimeStamp();

            // Let us try to see if we corrected the timestamp
            //index = getIndex(); // making sure we have the right index
            LinkedHashMap<Long, ArrayList<Long>> posMapTsToPosList = null;
            if (subsystem == SUBSYS_LOW)
                posMapTsToPosList = index.positionMapLow;
            else if (subsystem == SUBSYS_HIGH)
                posMapTsToPosList = index.positionMapHigh;
            if (posMapTsToPosList != null && !posMapTsToPosList.isEmpty()) {
                for (long tsK : posMapTsToPosList.keySet()) {
                    boolean found = false;
                    for (long posK : posMapTsToPosList.get(tsK)) {
                        if (posK == posHeader) {
                            ping.setTimestamp(tsK);
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
            }

            if (ping.getTimestamp() < 1000 || (tslist.get(subsystem).length > 2969 &&
                    (tslist.get(subsystem)[2968] < 1000 || tslist.get(subsystem)[2969] < 1000))) {
                if (tslist.get(subsystem).length > 2969) {
                    NeptusLog.pub().debug(">!!>>>> ts 2968 vs 2969 >>" + tslist.get(subsystem)[2968] +
                            ", " + tslist.get(subsystem)[2969]);
                } else {
                    NeptusLog.pub().debug(">!!>>>> ts ping ts vs fixts>>" + ping.getTimestamp() +
                            " " + ping.getFixTimestamp());
                }
            }

            //handle data 
            buf = channel.map(MapMode.READ_ONLY, pos, (header.getNumberBytes() - header.getHeaderSize() - header.getSDFExtensionSize()+4));
            buf.order(ByteOrder.LITTLE_ENDIAN);

            ping.parseData(buf);

            pos+= (header.getNumberBytes() - header.getHeaderSize() - header.getSDFExtensionSize());

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return ping;
    }

    private void redirectIndex(Long timestamp, int subsystem) {
        for (Entry<File, SdfIndex> entry : fileIndex.entrySet()) {
            if (subsystem == SUBSYS_LOW) {
                if (timestamp >= entry.getValue().firstTimestampLow && timestamp <= entry.getValue().lastTimestampLow)  {
                    index = entry.getValue();
                    file = entry.getKey();
                    return;
                }
            } else 
                if (subsystem == SUBSYS_HIGH) {
                    if (timestamp >= entry.getValue().firstTimestampHigh && timestamp <= entry.getValue().lastTimestampHigh) {
                        index = entry.getValue();
                        file = entry.getKey();
                        return;
                    }
                }
        }
    }

    private boolean existsTimestamp(long timestamp, SdfIndex searchIndex) {
        if (timestamp >= searchIndex.firstTimestampLow && timestamp <= searchIndex.lastTimestampLow)
            return true;

        return false;
    }

    public SdfData getPingAt(Long timestamp, int subsystem) {

        // point index to right index_ file according to timestamp
        if (index != null && multipleFiles) {
            if (!existsTimestamp(timestamp, index)) {
                redirectIndex(timestamp, subsystem);
            }
        }
        // end

        curPosition = 0;
        SdfData ping = null;
        LinkedHashMap<Long, ArrayList<Long>> positionMap = ( subsystem == SUBSYS_LOW ? index.positionMapLow : index.positionMapHigh);
        long ts = 0;
        int c = 0;
        for (Long time : tslist.get(subsystem)) {
            if (time >= timestamp) {
                ts = time;
                break;
            }
            c++;
        }

        NeptusLog.pub().debug(">>> " + subsystem + " >>>>> Fetch ping " + (c+1) + " of " +
                tslist.get(subsystem).length + " < " + tslist.get(subsystem).length
                + " @" + timestamp + " for ping @" + tslist.get(subsystem)[c+1]
                + " " + (tslist.get(subsystem)[c+1] >= timestamp ? 'T' : 'F')
                + "   >>> " + file.getName());
        nextTimestamp.put(subsystem, tslist.get(subsystem)[c+1]);
        for(Long pos : positionMap.get(ts)) {
            ping = getPingAtPosition(pos, subsystem);
            NeptusLog.pub().debug(">>> " + subsystem + " >>>>> For long " + pos +
                    " @ ts:" + ping.getTimestamp() + " | fixts:" + ping.getFixTimestamp());
        }

        return ping;
    }

    public void cleanup(){
        try { 
            if (fis != null) {
                fis.close();
            }
            if (channel != null) {
                channel.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static int main(String[] args) throws Exception {
        //        SdfParser parser = new SdfParser(new File("C://Users//Manuel//workspace//neptus-dev//SDF-datasamples//test.sdf"));
        //        
        //        System.out.println();
        //        for(Integer i : parser.index.subSystemsList) {
        //            System.out.println("Subsys: "+i);
        //        }
        //  
        //        ArrayList<SdfData> ping = parser.getPingAt(parser.index.firstTimestampHigh, parser.index.subSystemsList.get(0));
        //       
        //        System.out.println();
        //        System.out.println();
        //        System.out.println("first ts HIGH: "+parser.index.firstTimestampHigh);
        //        System.out.println("first ts LOW: "+parser.index.firstTimestampLow);
        //        System.out.println("last ts HIGH: "+parser.index.lastTimestampHigh);
        //        System.out.println("last ts LOW: "+parser.index.lastTimestampLow);
        //        System.out.println();
        //        System.out.println("first ts: "+parser.getFirstTimeStamp());
        //        System.out.println("last ts: "+parser.getLastTimeStamp());
        //        System.out.println();
        //        
        //        System.out.println("First ping "+ ping.get(0).getTimestamp());     
        //
        //  
        //        SdfData singlePing = parser.getPingAtPosition(34406800, 3502);       
        //        System.out.println("Last ping: "+ singlePing.getHeader().getPingNumber() + " " + singlePing.getTimestamp()+"\n");
        //
        //
        //        while(true) {
        //            if(ping == null)
        //                break;
        //            ping = parser.nextPing(parser.index.subSystemsList.get(0));
        //
        //            System.out.println(ping.get(0).getTimestamp());
        //        }

        if (args.length < 2)
            throw new Exception("Usage: <sdf_file> <page_version>  example 3503 for Bathy Pulse Compressed Data");

        SdfParser parser = new SdfParser(new File(args[0]));
        return parser.hasAnyPageVersion(Arrays.copyOfRange(args, 1, args.length - 1));
    }
}
