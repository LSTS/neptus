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
 * Author: Pedro Costa
 * 04/Oct/2023
 */
package pt.lsts.neptus.plugins.deepvision;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanUtil;
import pt.lsts.neptus.types.coord.LocationType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Pedro Costa
 */
public class DvsParser {
    private File dvsFile;
    private DvsHeader dvsHeader;
    private DvsIndex dvsIndex;

    public DvsParser(File dvsFile) {
        this.dvsFile = dvsFile;
        dvsHeader = new DvsHeader();

        int retry = 1;
        dvsIndex = loadIndex(dvsFile);

        while (dvsIndex == null && retry > 0) {
            // Index file did not load or does not exist
            generateIndex(dvsFile);
            dvsIndex = loadIndex(dvsFile);
            retry--;
        }
    }

    private void generateIndex(File file) {
        int filePosition = 0;

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            ByteBuffer buffer;
            FileChannel fileChannel = fileInputStream.getChannel();
            buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, filePosition, dvsHeader.HEADER_SIZE);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // Read Header data
            int VERSION = buffer.getInt() & 0xFFFFFFFF; // Turn int to unsigned int value
            float sampleRes = buffer.getFloat();
            float lineRate = buffer.getFloat();
            int nSamples = buffer.getInt();
            boolean left = buffer.get() > 0;
            boolean right = buffer.get() > 0;

            if (!dvsHeader.versionMatches(VERSION)) {
                NeptusLog.pub().error("Dvs file is not version 1. Abort.");
                return;
            }
            dvsHeader.setSampleResolution(sampleRes);
            dvsHeader.setLineRate(lineRate);
            dvsHeader.setnSamples(nSamples);
            dvsHeader.setLeftChannelActive(left);
            dvsHeader.setRightChannelActive(right);
            filePosition += dvsHeader.HEADER_SIZE;

            ArrayList<Long> timestampList = new ArrayList<>();
            ArrayList<Integer> positionList = new ArrayList<>();
            int bufferSize = dvsHeader.getNumberOfActiveChannels() * dvsHeader.getnSamples() + DvsPos.SIZE;
            while (filePosition < dvsFile.length()) {
                buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, filePosition, bufferSize);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                positionList.add(filePosition);

                timestampList.add((long) (timestampList.size() / (dvsHeader.getLineRate() / 1000)));

                filePosition += bufferSize;
            }

            fileChannel.close();

            DvsIndex dvsIndex = new DvsIndex(dvsHeader, timestampList, positionList);
            dvsIndex.save(getIndexFilePath(file));
        }
        catch (FileNotFoundException e) {
            NeptusLog.pub().error("File " + file.getAbsolutePath() + " not found while creating the DvsParser object.");
            e.printStackTrace();
        }
        catch (IOException e) {
            NeptusLog.pub().error("While trying to read " + file.getAbsolutePath() + " an IOException occurred");
            e.printStackTrace();
        }

    }

    private DvsIndex loadIndex(File file) {
        String indexFilePath = getIndexFilePath(file);
        if(!new File(indexFilePath).exists()) {
            return null;
        }

        return DvsIndex.restore(indexFilePath);
    }

    public long getLastPingTimestamp() {
        return dvsIndex.getLastTimestamp();
    }

    public long getFirstPingTimestamp() {
        return dvsIndex.getFirstTimestamp();
    }

    public ArrayList<Integer> getSubsystemList() {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(0);
        return list;
    }

    /**
     * Find and return the lines between the start and stop timestamps in chronological order
     *
     * @param startTimestamp the starting point timestamp
     * @param stopTimestamp the stopping point timestamp
     * @param subsystem the subsystem number
     * @param params the Sidescan parameters to apply to the data
     * @return returns an ArrayList of SidescanLine objects containing the lines between the given timestamps
     */
    public ArrayList<SidescanLine> getLinesBetween(long startTimestamp, long stopTimestamp, int subsystem, SidescanParameters params) {
        DvsPos dvsPos;
        DvsReturn dvsReturn;
        DvsHeader dvsHeader = dvsIndex.getDvsHeader();
        float range;
        SystemPositionAndAttitude state;
        float frequency;
        double[] data;

        ArrayList<SidescanLine> lines = new ArrayList<>();
        List<Integer> positions = dvsIndex.getPositionsBetween(startTimestamp, stopTimestamp);

        for(long position: positions) {
            DvsPing ping = getPingAt(position);
            if(ping == null) {
                NeptusLog.pub().error("Debug - ping is null");
            }
            dvsPos = ping.getDvsPos();
            dvsReturn = ping.getDvsReturn();

            range = dvsHeader.getSampleResolution();
            state = new SystemPositionAndAttitude();
            state.setPosition(new LocationType(dvsPos.getLatitudeDegrees(), dvsPos.getLongitudeDegrees()));
            state.setYaw(dvsPos.getHeading());
            frequency = dvsHeader.getLineRate();
            data = dvsReturn.getData();
            data = SidescanUtil.applyNormalizationAndTVG(data, range, params);

            SidescanLine line = new SidescanLine(dvsPos.getTimestamp(), range, state, frequency, data);
            lines.add(line);
        }
        return lines;
    }

    private DvsPing getPingAt(long position) {
        DvsHeader dvsHeader = dvsIndex.getDvsHeader();
        try(FileInputStream fileInputStream = new FileInputStream(dvsFile)) {
            FileChannel fileChannel = fileInputStream.getChannel();
            ByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, position, dvsIndex.getPingBlockSize());
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // Read "Ping Pos" data
            DvsPos dvsPos = new DvsPos();
            dvsPos.setLatitude(buffer.getDouble());
            dvsPos.setLongitude(buffer.getDouble());
            dvsPos.setSpeed(buffer.getFloat());
            dvsPos.setHeading(buffer.getFloat());
            long timestamp = (long)((position / dvsIndex.getPingBlockSize() ) / (dvsHeader.getLineRate() / 1000));
            dvsPos.setTimestamp(timestamp);

            byte[] returnData = new byte[dvsHeader.getnSamples() * dvsHeader.getNumberOfActiveChannels()];
            // Read "Ping Return" data
            buffer.get(returnData);

            // Bytes from the left channel array need to be reversed
            if (dvsHeader.isLeftChannelActive()) {
                int length = dvsHeader.getnSamples();
                reverseArray(returnData, length);
            }
            DvsReturn dvsReturn = new DvsReturn(returnData);
            return new DvsPing(dvsPos, dvsReturn);
        } catch (FileNotFoundException e) {
            NeptusLog.pub().error("getPingAt: Could not find file " + dvsFile.getAbsolutePath());
        } catch (IOException e) {
            NeptusLog.pub().error("Could not load file " + dvsFile.getAbsolutePath());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Reverse an array in place
     *
     * @param arr the byte array to reverse
     * @param index reverse from start up to (not including) index
     */
    private void reverseArray(byte[] arr, int index) {
        byte temp;

        for (int i = 0; i < index / 2; i++) {
            temp = arr[i];
            arr[i] = arr[index - 1 - i];
            arr[index - 1 - i] = temp;
        }
    }

    private String getIndexFilePath(File file) {
        if (file.exists()) {
            return file.getParent() + "/mra/" + file.getName() + ".index";
        }
        return null;
    }
}


