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
import scala.sys.process.ProcessBuilderImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * @author: Pedro Costa
 */
public class DvsParser {
    File file;
    DvsHeader dvsHeader;
    // List of the Ping Pos data
    ArrayList<DvsPos> posDataList;
    // List of the Ping Return data
    ArrayList<DvsReturn> returnDataList;

    public DvsParser(File file) {
        this.file = file;
        dvsHeader = new DvsHeader();
        posDataList = new ArrayList<>();
        returnDataList = new ArrayList<>();

        readInData();
    }

    // Called by constructor
    private void readInData() {
        int bufferPosition = 0;

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            ByteBuffer buffer;
            FileChannel fileChannel = fileInputStream.getChannel();
            buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, bufferPosition, dvsHeader.HEADER_SIZE);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // Header
            int VERSION = buffer.getInt() & 0xFFFFFFFF;
            float sampleRes = buffer.getFloat();
            float lineRate = buffer.getFloat();
            int nSamples = buffer.getInt();
            boolean left = buffer.get() > 0;
            boolean right = buffer.get() > 0;

            if(!dvsHeader.versionMatches(VERSION)) {
                NeptusLog.pub().error("Dvs file is not version 1. Abort.");
                return;
            }
            dvsHeader.setSampleResolution(sampleRes);
            dvsHeader.setLineRate(lineRate);
            dvsHeader.setnSamples(nSamples);
            dvsHeader.setLeftChannelActive(left);
            dvsHeader.setRightChannelActive(right);

            bufferPosition += dvsHeader.HEADER_SIZE;

            // Pos + Return
            int bufferSize = dvsHeader.getNumberOfActiveChannels() * dvsHeader.getnSamples() + 24 ;
            buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, bufferPosition, bufferSize);

            // Ping Pos
            double lat = buffer.getDouble();
            double lon = buffer.getDouble();
            float speed = buffer.getFloat();
            float heading = buffer.getFloat();

            System.out.println("VERSION: " + VERSION);
            System.out.println("sampleRes: " + sampleRes);
            System.out.println("lineRate: " + lineRate);
            System.out.println("nSamples: " + nSamples);
            System.out.println("left: " + left);
            System.out.println("right: " + right);

            // V1_Position
            System.out.println("lat: " + lat);
            System.out.println("lon: " + lon);
            System.out.println("speed: " + speed);
            System.out.println("heading: " + heading);

            if (left) {
                for (int i = 0; i < nSamples; i++) {
                    System.out.println("V[" + i + "] = " + (buffer.get() & 0xFF));
                }
            }

            if (right) {
                for (int i = 0; i < nSamples; i++) {
                    System.out.println("V[" + i + "] = " + (buffer.get() & 0xFF));
                }
            }
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


    public long getLastPingTimestamp() {
        return 1000L;
    }

    public long getFirstPingTimestamp() {
        return 0L;
    }

    public ArrayList<Integer> getSubsystemList() {
        return new ArrayList<>();
    }

    public void cleanup() {
    }
}
