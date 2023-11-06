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
 * 06/Nov/2023
 */
package pt.lsts.neptus.plugins.deepvision;

import pt.lsts.neptus.NeptusLog;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.io.FileOutputStream;
import java.util.List;

public class DvsIndex implements Serializable {
    private static final long serialVersionUID = 1L;
    private DvsHeader dvsHeader;
    private int pingBlockSize;
    private ArrayList<Long> timestampList;
    private ArrayList<Integer> positionList;

    public DvsIndex(DvsHeader dvsHeader, ArrayList<Long> timestampList, ArrayList<Integer> positionList) {
        this.dvsHeader = dvsHeader;
        this.pingBlockSize = DvsPos.SIZE + dvsHeader.getnSamples() * dvsHeader.getNumberOfActiveChannels();
        this.timestampList = timestampList;
        this.positionList = positionList;
    }

    public void save(String filePath) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))){
            out.writeObject(this);
        }
        catch (IOException e) {
            NeptusLog.pub().error("Could not save index to " + filePath);
            e.printStackTrace();
        }
    }

    public static DvsIndex restore(String filePath) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))){
            return (DvsIndex) in.readObject();
        }
        catch (Exception e) {
            NeptusLog.pub().error("Could not restore index file at " + filePath);
            e.printStackTrace();
        }
        return null;
    }

    public long getFirstTimestamp() {
        return 0;
    }

    public long getLastTimestamp() {
        return timestampList.get(timestampList.size() - 1);
    }

    public DvsHeader getDvsHeader() {
        return dvsHeader;
    }

    public List<Long> getTimestampsBetween(long startTimestamp, long stopTimestamp) {
        int startIndex = findTimestamp(startTimestamp);
        int stopIndex = findTimestamp(stopTimestamp);
        ArrayList<Long> timestamps = new ArrayList<>();
        for(int i = startIndex; i < stopIndex; i++) {
            timestamps.add((long)(i / (dvsHeader.getLineRate() / 1000)));
        }
        return timestamps;
    }

    public List<Integer> getPositionsBetween(long startTimestamp, long stopTimestamp) {
        int startIndex = findTimestamp(startTimestamp);
        int stopIndex = startIndex;
        while(timestampList.get(stopIndex) < stopTimestamp) {
            stopIndex++;
        }
        return positionList.subList(startIndex, stopIndex);
    }

    public int getPingBlockSize() {
        return pingBlockSize;
    }

    private int getPosition(int index) {
        return dvsHeader.HEADER_SIZE + index * pingBlockSize;
    }

    private int findTimestamp(long timestamp) {
        if(timestamp < timestampList.get(0) || timestamp > timestampList.get(timestampList.size() - 1)) {
            return -1;
        }

        int index = 0;
        while(timestampList.get(index) < timestamp) {
            index++;
        }
        return index;
    }
}
