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

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

public class SdfParser {

    private final Set<Integer> subsystemsInUse;
    private final LinkedHashMap<File, SdfIndex> fileIndex = new LinkedHashMap<>();
    // Map subsystem to SdfTimestampList
    private final HashMap<Integer, SdfTimestampList> timestampListMap = new HashMap<>();

    public SdfParser(File[] files) {

        subsystemsInUse = new HashSet<>();
        subsystemsInUse.add(SdfConstant.SUBSYS_HIGH);
        subsystemsInUse.add(SdfConstant.SUBSYS_LOW);

        for (int subsystem : subsystemsInUse) {
            SdfTimestampList sdfTimestampList = new SdfTimestampList();
            timestampListMap.put(subsystem, sdfTimestampList);
        }

        for (File file : files) {
            int retry = 1;
            boolean loadedIndex = loadIndex(file);

            while (!loadedIndex && retry > 0) {
                // Index file did not load or does not exist
                generateIndex(file);
                loadedIndex = loadIndex(file);
                retry--;
            }
        }
    }

    public long getFirstTimeStamp() {
        long firstTimestamp = Long.MAX_VALUE;
        for (Entry<File, SdfIndex> entry : fileIndex.entrySet()) {
            firstTimestamp = Math.min(firstTimestamp, entry.getValue().getFirstTimestamp());
        }
        return firstTimestamp;
    }

    public long getLastTimeStamp() {
        long lastTimestamp = -1;
        for (Entry<File, SdfIndex> entry : fileIndex.entrySet()) {
            lastTimestamp = Math.max(lastTimestamp, entry.getValue().getLastTimestamp());
        }
        return lastTimestamp;
    }

    public void cleanup() {
    }

    public ArrayList<Integer> getSubsystemList() {
        Set<Integer> subsystems = new HashSet<>();
        for (SdfIndex index : fileIndex.values()) {
            for (Integer system : index.getSubSystemList()) {
                subsystems.add(system);
            }
        }
        return new ArrayList(subsystems);
    }

    public ArrayList<SidescanLine> getLinesBetween(long timestamp1, long timestamp2, int subsystem,
                                                   SidescanParameters config) {

        NeptusLog.pub().debug(">>>>>>>>>>>>>> getLinesBetween timestamp1=" + timestamp1 +
                ",  timestamp2=" + timestamp2 + ",  subsystem=" + subsystem);

        ArrayList<SidescanLine> list = new ArrayList<>();
        Long[] timestamps = timestampListMap.get(subsystem).getTimestampsBetween(timestamp1, timestamp2);

        for (Long timestamp : timestamps) {
            SdfData ping = getPingAt(timestamp, subsystem);

            if(ping == null) {
                continue;
            }

            int nSamples = ping.getNumSamples();
            double fData[] = ping.getData();

            SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
            pose.getPosition().setLatitudeDegs(Math.toDegrees(ping.getHeader().getShipLat())); // rads to
            // degrees
            pose.getPosition().setLongitudeDegs(Math.toDegrees(ping.getHeader().getShipLon()));// rads to
            // degrees

            pose.setRoll(Math.toRadians(ping.getHeader().getAuxRoll()));
            pose.setYaw(Math.toRadians(ping.getHeader().getShipHeading()));
            pose.setAltitude(ping.getHeader().getAuxAlt()); // altitude in meters
            pose.setU(ping.getHeader().getSpeedFish() / 100.0); // Convert cm/s to m/s
            pose.getPosition().setDepth(ping.getHeader().getAuxDepth());

            float frequency = ping.getHeader().getSonarFreq();
            float range = ping.getHeader().getRange();

            fData = SidescanUtil.applyNormalizationAndTVG(fData, range, config);

            list.add(new SidescanLine(ping.getTimestamp(), range, pose, frequency, fData));
        }
        return list;
    }

    private void generateIndex(File file) {
        NeptusLog.pub().info("Generating SDF index for " + file.getAbsolutePath());

        SdfHeader header = new SdfHeader();
        SdfData ping = new SdfData();
        SdfIndex index = new SdfIndex();

        String indexFilePath = getIndexFilePath(file);

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
                filePosition += header.getNumberBytes() + 4;
                if (subsystemsInUse.contains(header.getPageVersion())) {
                    //set header of this ping
                    ping.setHeader(header);
                }
                else { //ignore other pageVersions
                    if (!unimplementedPageVersionSet.contains(header.getPageVersion())) {
                        unimplementedPageVersionSet.add(header.getPageVersion());
                        NeptusLog.pub().info("SDF Data file " + file.getName() + " contains unimplemented pageVersion # " + header.getPageVersion());
                    }
                    continue;
                }

                //get timestamp, freq and subsystem used
                long pingTimestamp = ping.getTimestamp(); // Timestamp
                int pageVersion = header.getPageVersion();

                index.addSubsystem(pageVersion);

                index.addPageHeaderPositionToMap(pingTimestamp, dataPageHeaderPosition, pageVersion);

                //end processing data
            }

            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(indexFilePath));
            out.writeObject(index);
            out.close();
        }
        catch (IOException e) {
            NeptusLog.pub().error("Found corrupted SDF file '" + file.getName() + "' while indexing. Error: " +
                    e.getMessage());
        }
    }

    private boolean loadIndex(File file) {
        NeptusLog.pub().info("Loading SDF index for " + file.getAbsolutePath());

        String indexFilePath = getIndexFilePath(file);
        if (!new File(indexFilePath).exists()) {
            // Index file doesn't exist
            return false;
        }

        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(indexFilePath));
            SdfIndex index = (SdfIndex) in.readObject();

            for (int subsystem : subsystemsInUse) {
                Long[] timestamps = index.getTimestampsAsArray(subsystem);
                timestampListMap.get(subsystem).add(timestamps);
            }

            fileIndex.put(file, index);
            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private SdfData getPingAt(Long timestamp, int subsystem) {

        SdfIndex sdfIndex = getIndexFromTimestamp(timestamp, subsystem);
        if (sdfIndex == null) {
            return null;
        }

        Long filePosition = sdfIndex.getPageHeaderPosition(subsystem, timestamp);
        SdfData ping = new SdfData();
        SdfHeader header = new SdfHeader();

        File file = getFileFromIndex(sdfIndex);
        if (file == null) {
            return null;
        }

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            FileChannel channel = fileInputStream.getChannel();
            ByteBuffer buf = channel.map(MapMode.READ_ONLY, filePosition, SdfHeader.HEADER_SIZE);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            header.parse(buf);
            filePosition += header.getHeaderSize();

            if (header.getPageVersion() != subsystem) {
                return null;
            }

            //define header
            ping.setHeader(header);

            //handle data
            buf = channel.map(MapMode.READ_ONLY, filePosition, (header.getNumberBytes() - header.getHeaderSize() - header.getSDFExtensionSize() + 4));
            buf.order(ByteOrder.LITTLE_ENDIAN);

            ping.parseData(buf);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return ping;
    }

    private String getIndexFilePath(File file) {
        if (file.exists()) {
            return file.getParent() + "/mra/sdf" + file.getName() + ".index";
        }
        return null;
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

    private SdfIndex getIndexFromTimestamp(Long timestamp, int subsystem) {
        SdfIndex index;
        for (Entry<File, SdfIndex> entry : fileIndex.entrySet()) {
            index = entry.getValue();
            if (timestamp >= index.getFirstTimestamp(subsystem) && timestamp <= index.getLastTimestamp(subsystem)) {
                return index;
            }
        }
        return null;
    }

}
