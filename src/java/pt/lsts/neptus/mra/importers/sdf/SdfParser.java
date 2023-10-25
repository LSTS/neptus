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

    private LinkedHashMap<Integer, Long[]> tslist = new LinkedHashMap<Integer, Long[]>();
    private LinkedHashMap<Integer, Long> nextTimestamp = new LinkedHashMap<Integer, Long>();
    private LinkedHashMap<File, SdfIndex> fileIndex = new LinkedHashMap<>();

    private ArrayList<Long[]> tsSHigh = new ArrayList<>();
    private ArrayList<Long[]> tsSLow = new ArrayList<>();

    final static int SUBSYS_LOW = 3501;
    final static int SUBSYS_HIGH = 3502;
    final static int BATHY_PULSE_COMPRESSED = 3503;     // Will be used in the future

    public SdfParser(File[] files) {

        for (File file : files) {
            openIndexFile(file);
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
        for (int i = 0; i < tsSLow.size(); i++) {
            for (int j = 0; j < tsSLow.get(i).length; j++) {
                longLow[count] = tsSLow.get(i)[j];
                count++;
            }
        }

        count = 0;
        for (int i = 0; i < tsSHigh.size(); i++) {
            for (int j = 0; j < tsSHigh.get(i).length; j++) {
                longHigh[count] = tsSHigh.get(i)[j];
                count++;
            }
        }

        tslist.put(SUBSYS_LOW, longLow);
        tslist.put(SUBSYS_HIGH, longHigh);

    }

    private void openIndexFile(File file) {
        String indexFilePath = getIndexFilePath(file);

        if (new File(indexFilePath).exists()) {
            if(loadIndex(file)) {
                // File loaded
                return;
            }
        }

        // Index file did not load or does not exist
        generateIndex(file);
    }

    private String getIndexFilePath(File file) {
        if (file.exists()) {
            return file.getParent() + "/mra/sdf" + file.getName() + ".index";
        }
        return null;
    }

    private void generateIndex(File file) {
        NeptusLog.pub().info("Generating SDF index for " + file.getAbsolutePath());

        SdfHeader header = new SdfHeader();
        SdfData ping = new SdfData();
        SdfIndex index = new SdfIndex();

        String indexFilePath = getIndexFilePath(file);

        long maxTimestampHigh = 0;
        long maxTimestampLow = 0;
        long minTimestampHigh = Long.MAX_VALUE;
        long minTimestampLow = Long.MAX_VALUE;

        long count = 0;
        long dataPageHeaderPosition;
        long filePosition = 0;

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            FileChannel channel = fileInputStream.getChannel();

            Set<Integer> unimplementedPageVersionSet = new HashSet<>();
            while (filePosition < file.length()) {
                // Read the header
                ByteBuffer buf = channel.map(MapMode.READ_ONLY, filePosition, SdfHeader.HEADER_SIZE);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                header.parse(buf);

                dataPageHeaderPosition = filePosition;
                if (header.getPageVersion() == SUBSYS_HIGH || header.getPageVersion() == SUBSYS_LOW) {
                    //set header of this ping
                    ping.setHeader(header);
                    ping.calculateTimeStamp();
                    ping.calculateFixTimeStamp();
                    filePosition += header.getHeaderSize();
                }
                else { //ignore other pageVersions
                    if (!unimplementedPageVersionSet.contains(header.getPageVersion())) {
                        unimplementedPageVersionSet.add(header.getPageVersion());
                        NeptusLog.pub().info("SDF Data file contains unimplemented pageVersion # " + header.getPageVersion());
                    }
                    filePosition += header.getNumberBytes() + 4;
                    continue;
                }

                //get timestamp, freq and subsystem used
                long t = ping.getTimestamp(); // Timestamp
                long tfix = ping.getFixTimestamp(); // FixTimestamp
                int f = ping.getHeader().getSonarFreq(); // Frequency
                int subsystem = ping.getHeader().getPageVersion();

                if (!index.frequenciesList.contains(f)) {
                    index.frequenciesList.add(f);
                }
                if (!index.subSystemsList.contains(subsystem)) {
                    index.subSystemsList.add(subsystem);
                }

                if (t < 5000000) { // Fixing timestamp from 1970
                    NeptusLog.pub().warn(I18n.textf("Something is wrong with the timestamp (%d). " +
                                    "Trying to calculate using GPS data for ping %d for subsystem %d. New timestamp is %d.",
                            new Date(t), ping.getHeader().getPingNumber(), subsystem, new Date(tfix)));
                    t = tfix;
                }

                if (subsystem == SUBSYS_LOW) {
                    if (!index.hasLow) {
                        index.hasLow = true;
                    }

                    ArrayList<Long> l = index.positionMapLow.get(t);
                    if (l == null) {
                        l = new ArrayList<Long>();
                        l.add(dataPageHeaderPosition);
                        index.positionMapLow.put(t, l);
                    }
                    else {
                        l.add(dataPageHeaderPosition);
                    }

                    if (t > minimumValidTimestamp) {
                        minTimestampLow = Math.min(minTimestampLow, t);
                        maxTimestampLow = Math.max(maxTimestampLow, t);
                    }
                }

                if (subsystem == SUBSYS_HIGH) {
                    if (!index.hasHigh) {
                        index.hasHigh = true;
                    }

                    ArrayList<Long> l = index.positionMapHigh.get(t);
                    if (l == null) {
                        l = new ArrayList<Long>();
                        l.add(dataPageHeaderPosition);
                        index.positionMapHigh.put(t, l);
                    }
                    else {
                        l.add(dataPageHeaderPosition);
                    }

                    if (t > minimumValidTimestamp) {
                        minTimestampHigh = Math.min(minTimestampHigh, t);
                        maxTimestampHigh = Math.max(maxTimestampHigh, t);
                    }
                }

                //end processing data

                filePosition += (header.getNumberBytes() + 4) - header.getHeaderSize();
                count++;
            }

            index.firstTimestampHigh = minTimestampHigh;
            index.firstTimestampLow = minTimestampLow;

            index.lastTimestampHigh = maxTimestampHigh;
            index.lastTimestampLow = maxTimestampLow;

            // Save timestamp list
            Long[] tslisthigh;
            Long[] tslistlow;

            tslisthigh = index.positionMapHigh.keySet().toArray(new Long[]{});
            tslistlow = index.positionMapLow.keySet().toArray(new Long[]{});

            Arrays.sort(tslisthigh);
            Arrays.sort(tslistlow);

            if (tslisthigh.length >= 2968) {
                NeptusLog.pub().debug(">??>>>> >= 2968 >> HLength:" + tslisthigh.length + "|LLength:" + tslistlow.length +
                        ">" + tslisthigh[2967] + ", " + "----" + " | " +
                        (tslistlow.length >= 2968 ? tslistlow[2967] : "----") + ", " +
                        (tslistlow.length >= 2969 ? tslistlow[2968] : "----") +
                        " >> " + indexFilePath);
            }

            tslist.put(SUBSYS_LOW, tslistlow);
            tslist.put(SUBSYS_HIGH, tslisthigh);

            index.numberOfPackets = count;

            index.frequenciesList.sort(null);
            index.subSystemsList.sort(null);

            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(indexFilePath));
            out.writeObject(index);
            out.close();

            fileIndex.put(file, index);
        }
        catch (IOException e) {
            NeptusLog.pub().error("Found corrupted SDF file '" + file.getName() + "' while indexing. Error: " +
                    e.getMessage());
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

        if (pageVersionList.isEmpty()) {
            return 0;
        }

        SdfHeader header = new SdfHeader();
        long curPosition = 0;

        for(File file: fileIndex.keySet()) {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                while (curPosition < file.length()) {
                    // Read the header
                    FileChannel fileChannel = fileInputStream.getChannel();
                    ByteBuffer buf = fileChannel.map(MapMode.READ_ONLY, curPosition, SdfHeader.HEADER_SIZE);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    header.parse(buf);
                    curPosition += header.getHeaderSize();

                    if (pageVersionList.stream().anyMatch((p) -> p == header.getPageVersion())) {
                        return 1;
                    }

                    curPosition += (header.getNumberBytes() + 4) - header.getHeaderSize();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    public boolean loadIndex(File file) {
        NeptusLog.pub().info("Loading SDF index for " + file.getAbsolutePath());
        String indexFilePath = getIndexFilePath(file);

        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(indexFilePath));
            SdfIndex index = (SdfIndex) in.readObject();

            Long[] tsListHigh;
            Long[] tsListLow;

            tsListHigh = index.positionMapHigh.keySet().toArray(new Long[]{});
            tsListLow = index.positionMapLow.keySet().toArray(new Long[]{});

            Arrays.sort(tsListHigh);
            Arrays.sort(tsListLow);

            tsSHigh.add(tsListHigh);
            tsSLow.add(tsListLow);

            fileIndex.put(file, index);

            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public long getFirstTimeStamp() {
        for (Entry<File, SdfIndex> entry : fileIndex.entrySet()) {
            return Math.min(entry.getValue().firstTimestampHigh, entry.getValue().firstTimestampLow);
        }
        return -1L;
    }

    public long getLastTimeStamp() {
        long entryTimestamp;
        long maxTimestamp = 0L;

        for (Entry<File, SdfIndex> entry : fileIndex.entrySet()) {
            entryTimestamp = entry.getValue().lastTimestampHigh;
            if (entryTimestamp > maxTimestamp) {
                maxTimestamp = entryTimestamp;
            }
            entryTimestamp = entry.getValue().lastTimestampLow;
            if (entryTimestamp > maxTimestamp) {
                maxTimestamp = entryTimestamp;
            }
        }

        return maxTimestamp;
    }

    public SdfData nextPing(int subsystem) {
        return getPingAt(nextTimestamp.get(subsystem), subsystem); // This fetches the next ping and updates nextTimestamp
    }

    // Get corresponding file for the given index
    private File getFileFromIndex(SdfIndex index) {
        for (Entry<File, SdfIndex> entry : fileIndex.entrySet()) {
            if (entry.getValue() == index) {
                return entry.getKey();
            }
        }
        return null;
    }

    public SdfData getPingAtPosition(long pos, int subsystem, SdfIndex index) {
        long posHeader = pos;
        SdfHeader header = new SdfHeader();
        SdfData ping = new SdfData();

        File file = getFileFromIndex(index);
        if(file == null) {
            return null;
        }

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            FileChannel channel = fileInputStream.getChannel();
            ByteBuffer buf = channel.map(MapMode.READ_ONLY, pos, SdfHeader.HEADER_SIZE);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            header.parse(buf);
            pos += header.getHeaderSize();

            if (header.getPageVersion() != subsystem) {
                return null;
            }

            //define header
            ping.setHeader(header);
            ping.calculateTimeStamp();
            ping.calculateFixTimeStamp();

            // Let us try to see if we corrected the timestamp
            LinkedHashMap<Long, ArrayList<Long>> posMapTsToPosList = null;
            if (subsystem == SUBSYS_LOW) {
                posMapTsToPosList = index.positionMapLow;
            }
            else if (subsystem == SUBSYS_HIGH) {
                posMapTsToPosList = index.positionMapHigh;
            }
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
                    if (found) {
                        break;
                    }
                }
            }

            if (ping.getTimestamp() < 1000 || (tslist.get(subsystem).length > 2969 &&
                    (tslist.get(subsystem)[2968] < 1000 || tslist.get(subsystem)[2969] < 1000))) {
                if (tslist.get(subsystem).length > 2969) {
                    NeptusLog.pub().debug(">!!>>>> ts 2968 vs 2969 >>" + tslist.get(subsystem)[2968] +
                            ", " + tslist.get(subsystem)[2969]);
                }
                else {
                    NeptusLog.pub().debug(">!!>>>> ts ping ts vs fixts>>" + ping.getTimestamp() +
                            " " + ping.getFixTimestamp());
                }
            }

            //handle data 
            buf = channel.map(MapMode.READ_ONLY, pos, (header.getNumberBytes() - header.getHeaderSize() - header.getSDFExtensionSize() + 4));
            buf.order(ByteOrder.LITTLE_ENDIAN);

            ping.parseData(buf);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return ping;
    }

    private SdfIndex getIndexFromTimestamp(Long timestamp, int subsystem) {
        for (Entry<File, SdfIndex> entry : fileIndex.entrySet()) {
            if (subsystem == SUBSYS_LOW) {
                if (timestamp >= entry.getValue().firstTimestampLow && timestamp <= entry.getValue().lastTimestampLow) {
                    return entry.getValue();
                }
            }
            else if (subsystem == SUBSYS_HIGH) {
                if (timestamp >= entry.getValue().firstTimestampHigh && timestamp <= entry.getValue().lastTimestampHigh) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public SdfData getPingAt(Long timestamp, int subsystem) {

        SdfIndex index = getIndexFromTimestamp(timestamp, subsystem);
        if(index == null) return null;

        LinkedHashMap<Long, ArrayList<Long>> positionMap = index.getPositionMap(subsystem);
        long ts = 0;
        int c = 0;
        for (Long time : tslist.get(subsystem)) {
            if (time >= timestamp) {
                ts = time;
                break;
            }
            c++;
        }

        nextTimestamp.put(subsystem, tslist.get(subsystem)[c + 1]);

        if (positionMap.get(ts) == null) {
            return null;
        }
        Long position = positionMap.get(ts).get(0);
        SdfData ping = getPingAtPosition(position, subsystem, index);
        NeptusLog.pub().debug(">>> " + subsystem + " >>>>> For long " + position +
                " @ ts:" + ping.getTimestamp() + " | fixts:" + ping.getFixTimestamp());

        return ping;
    }

    public void cleanup() {
    }

    public ArrayList<Integer> getSubsystemList() {
        Set<Integer> subsystems = new HashSet<>();
        for (SdfIndex index : fileIndex.values()) {
            for (Integer system : index.subSystemsList) {
                subsystems.add(system);
            }
        }
        return new ArrayList(subsystems);
    }

    public static int main(String[] args) throws Exception {

        if (args.length < 2) {
            throw new Exception("Usage: <sdf_file> <page_version>  example 3503 for Bathy Pulse Compressed Data");
        }

        SdfParser parser = new SdfParser(new File[]{new File(args[0])});
        return parser.hasAnyPageVersion(Arrays.copyOfRange(args, 1, args.length - 1));
    }

}
