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
 * Feb 5, 2013
 */
package pt.lsts.neptus.mra.importers.jsf;

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
import java.util.LinkedHashMap;

import pt.lsts.neptus.NeptusLog;

/**
 * @author jqcorreia
 * 
 */
public class JsfParser {
    @SuppressWarnings("unused")
    private File file;

    private FileInputStream fis;
    private FileChannel channel;
    private long curPosition = 0;
    private JsfIndex index = new JsfIndex();    

    private String indexPath;

    private LinkedHashMap<Integer, Long[]> tslist = new LinkedHashMap<Integer, Long[]>();
    private LinkedHashMap<Integer, Long> nextTimestamp = new LinkedHashMap<Integer, Long>();

    final static int SUBSYS_LOW = 20;
    final static int SUBSYS_HIGH = 21;

    public JsfParser(File file) {
        try {
            this.file = file;
            fis = new FileInputStream(file);
            channel = fis.getChannel();

            indexPath = file.getParent() + "/mra/jsf.index";

            if (!new File(indexPath).exists()) {
                NeptusLog.pub().info("Generating JSF index for " + file.getAbsolutePath());
                generateIndex();
            }
            else {
                NeptusLog.pub().info("Loading JSF index for " + file.getAbsolutePath());
                if(!loadIndex()) {
                    NeptusLog.pub().error("Corrupted JSF index file. Trying to create a new index.");
                    generateIndex();
                }
            }

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the index
     */
    public JsfIndex getIndex() {
        return index;
    }

    public void generateIndex() {
        JsfHeader header = new JsfHeader();
        JsfSonarData ping = new JsfSonarData();

        long count = 0;
        long pos = 0;

        long maxTimestampHigh = 0;
        long maxTimestampLow = 0;
        long minTimestampHigh = Long.MAX_VALUE;
        long minTimestampLow = Long.MAX_VALUE;

        try {
            while (true) {
                int headerSize = 16;
                if (curPosition + headerSize >= channel.size())
                    break;
                // Read ONLY the header
                ByteBuffer buf = channel.map(MapMode.READ_ONLY, curPosition, headerSize);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                header.parse(buf);
                curPosition += headerSize;
                if (header.getType() == 80) {
                    int mapSize = 240;
                    if (curPosition + mapSize >= channel.size())
                        break;
                    
                    ping.setHeader(header);

                    buf = channel.map(MapMode.READ_ONLY, curPosition, mapSize);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    ping.parseHeader(buf);
                    curPosition += header.getMessageSize();
                    //                     NeptusLog.pub().info("<###> "+ping.getPingNumber() + " " + ping.getTimestamp() + " " +
                    //                     ping.getNumberOfSamples() + " " +
                    //                     ping.getFrequency() + " "
                    //                     + ping.getHeader().getSubsystem() + " " + ping.getHeader().getChannel());
                }
                else { // Ignore other messages;
                    curPosition += header.getMessageSize();
                    pos = curPosition;
                    if (curPosition >= channel.size())
                        break;
                    else
                        continue;
                }

                // Common processing to both subsystems
                long t = ping.getTimestamp(); // Timestamp
                float f = ping.getFrequency(); // Frequency
                int subsystem = ping.getHeader().getSubsystem();

                if (!index.frequenciesList.contains(f)) {
                    index.frequenciesList.add(f);
                }

                if (!index.subSystemsList.contains(subsystem)) {
                    index.subSystemsList.add(subsystem);
                }

                if(subsystem == SUBSYS_LOW) {
                    if(!index.hasLow) index.hasLow = true;

                    ArrayList<Long> l = index.positionMapLow.get(t);
                    if (l == null) {
                        l = new ArrayList<Long>();
                        l.add(pos);
                        index.positionMapLow.put(t, l);
                    }
                    else {
                        l.add(pos);
                    }
                    minTimestampLow = Math.min(minTimestampLow, t);
                    maxTimestampLow = Math.max(maxTimestampLow, t);
                }

                if(subsystem == SUBSYS_HIGH) {
                    if(!index.hasHigh) index.hasHigh = true;

                    ArrayList<Long> l = index.positionMapHigh.get(t);
                    if (l == null) {
                        l = new ArrayList<Long>();
                        l.add(pos);
                        index.positionMapHigh.put(t, l);
                        // System.out.println(t);
                    }
                    else {
                        l.add(pos);
                    }
                    minTimestampHigh = Math.min(minTimestampHigh, t);
                    maxTimestampHigh = Math.max(maxTimestampHigh, t);
                }

                count++;
                pos = curPosition;

                if (curPosition >= channel.size())
                    break;
            }
            //            System.out.println(JsfParser.class.getSimpleName() + " :: " + minTimestampHigh);
            //            System.out.println(JsfParser.class.getSimpleName() + " :: " + maxTimestampHigh);
            //            System.out.println(JsfParser.class.getSimpleName() + " :: " + minTimestampLow);
            //            System.out.println(JsfParser.class.getSimpleName() + " :: " + maxTimestampLow);

            index.firstTimestampHigh = minTimestampHigh;
            index.firstTimestampLow = minTimestampLow;

            index.lastTimestampHigh = maxTimestampHigh;
            index.lastTimestampLow = maxTimestampLow;

            // Save timestamp list
            Long[] tslisthigh;
            Long[] tslistlow;

            tslisthigh = index.positionMapHigh.keySet().toArray(new Long[] {});
            tslistlow = index.positionMapLow.keySet().toArray(new Long[] {});

            Arrays.sort(tslisthigh);
            Arrays.sort(tslistlow);

            tslist.put(SUBSYS_LOW, tslistlow);
            tslist.put(SUBSYS_HIGH, tslisthigh);

            index.numberOfPackets = count;

            index.frequenciesList.sort(null);
            index.subSystemsList.sort(null);

            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(indexPath));
            out.writeObject(index);
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean loadIndex() {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(indexPath));
            index = (JsfIndex) in.readObject();

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

    public long getFirstTimeStamp() {
        return Math.min(index.firstTimestampHigh, index.firstTimestampLow);
    }

    public long getLastTimeStamp() {
        return Math.max(index.lastTimestampHigh, index.lastTimestampLow);
    }

    public JsfSonarData getPingAtPosition(long pos, int subsystem) {
        JsfHeader header = new JsfHeader();
        JsfSonarData ping = new JsfSonarData();
        try {
            ByteBuffer buf = channel.map(MapMode.READ_ONLY, pos, 16);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            header.parse(buf);
            pos += 16;

            if(header.getSubsystem() != subsystem) 
                return null;

            ping.setHeader(header);

            buf = channel.map(MapMode.READ_ONLY, pos, 240);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            ping.parseHeader(buf);
            pos += 240;
            buf = channel.map(MapMode.READ_ONLY, pos, header.getMessageSize() - 240);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            ping.parseData(buf);
            pos += header.getMessageSize() - 240;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return ping;
    }

    public ArrayList<JsfSonarData> nextPing(int subsystem) {
        return getPingAt(nextTimestamp.get(subsystem), subsystem); // This fetches the next ping and updates nextTimestamp
    }

    public ArrayList<JsfSonarData> getPingAt(Long timestamp, int subsystem) {
        curPosition = 0;
        ArrayList<JsfSonarData> ping = new ArrayList<JsfSonarData>();

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

        nextTimestamp.put(subsystem, tslist.get(subsystem)[c+1]);

        for(Long pos : positionMap.get(ts)) {
            ping.add(getPingAtPosition(pos, subsystem));
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

    public static void main(String[] args) throws IOException {
        JsfParser parser = new JsfParser(new File("/home/jqcorreia/lsts/logs/lauv-dolphin-1/20130626/133827_rows_a1.5m/Data.jsf"));
        //            ArrayList<JsfSonarData> ping = parser.getPingAt(parser.index.firstTimestampHigh, parser.index.subSystemsList.get(0));

        System.out.println();
        System.out.println();
        System.out.println(parser.index.firstTimestampHigh);
        System.out.println(parser.index.firstTimestampLow);
        System.out.println(parser.index.lastTimestampHigh);
        System.out.println(parser.index.lastTimestampLow);
        System.out.println();
        System.out.println(parser.getFirstTimeStamp());
        System.out.println(parser.getLastTimeStamp());

        //            while(true) {
        //                if(ping == null)
        //                    break;
        //                ping = parser.nextPing(parser.index.subSystemsList.get(0));
        //                System.out.println(ping.get(0).getTimestamp());
        //            }
        //            
        //            for(Integer i : parser.index.subSystemsList) {
        //                NeptusLog.pub().info("<###> "+i);
        //            }
    }
}
