/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
import java.util.LinkedHashMap;

import pt.lsts.neptus.NeptusLog;

public class SdfParser {

    @SuppressWarnings("unused")
    private File file;
    private FileInputStream fis;
    private FileChannel channel;
    private long curPosition = 0;
    private SdfIndex index = new SdfIndex();    

    private String indexPath;

    private LinkedHashMap<Integer, Long[]> tslist = new LinkedHashMap<Integer, Long[]>();
    private LinkedHashMap<Integer, Long> nextTimestamp = new LinkedHashMap<Integer, Long>();

    final static int SUBSYS_LOW = 3501;
    final static int SUBSYS_HIGH = 3502;

    public SdfParser(File file) {
        try {
            this.file = file;
            fis = new FileInputStream(file);
            channel = fis.getChannel();
            indexPath = file.getParent().endsWith("mra") ? file.getParent() + "/sdf.index" : file.getParent() + "/mra/sdf.index";

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


    private void generateIndex() {
        SdfHeader header = new SdfHeader();
        SdfData ping = new SdfData();

        long maxTimestampHigh = 0;
        long maxTimestampLow = 0;
        long minTimestampHigh = Long.MAX_VALUE;
        long minTimestampLow = Long.MAX_VALUE;

        long count = 0;
        long pos = 0;

        try {

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
                    pos = curPosition-header.getHeaderSize();
     
                } else { //ignore other pageVersions
                    NeptusLog.pub().info("SDF Data file contains unimplemented pageVersion # "+header.getPageVersion());
                    curPosition += (header.getNumberBytes()+4) - header.getHeaderSize();
                    pos = curPosition;
                    if (curPosition >= channel.size()) //check if curPosition is at the end of file
                        break;
                    else
                        continue;
                }


                //get timestamp, freq and subsystem used
                long t = ping.getTimestamp(); // Timestamp
                int f = ping.getHeader().getSonarFreq(); // Frequency
                int subsystem = ping.getHeader().getPageVersion();
              //  System.out.println(pos+": ["+header.getPingNumber()+"] timestamp "+ t + " freq "+f + " subsys "+subsystem);

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
                    }
                    else {

                        l.add(pos);
                    }
                    minTimestampHigh = Math.min(minTimestampHigh, t);
                    maxTimestampHigh = Math.max(maxTimestampHigh, t);
                }

                //end processing data

                curPosition += (header.getNumberBytes()+4) - header.getHeaderSize();
                count++;

                if (curPosition >= channel.size())
                    break;
            }

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

            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(indexPath));
            out.writeObject(index);
            out.close();


        }
        catch (IOException e) {
            e.printStackTrace();
        }
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

    public long getFirstTimeStamp() {
        return Math.min(index.firstTimestampHigh, index.firstTimestampLow);
    }

    public long getLastTimeStamp() {
        return Math.max(index.lastTimestampHigh, index.lastTimestampLow);
    }

    public SdfData nextPing(int subsystem) {
        return getPingAt(nextTimestamp.get(subsystem), subsystem); // This fetches the next ping and updates nextTimestamp
    }

    /**
     * @return the index
     */
    public SdfIndex getIndex() {
        return index;
    }

    public SdfData getPingAtPosition(long pos, int subsystem) {
        SdfHeader header = new SdfHeader();
        SdfData ping = new SdfData();
        try {
            ByteBuffer buf = channel.map(MapMode.READ_ONLY, pos, 512);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            header.parse(buf);
            pos += header.getHeaderSize();

            if(header.getPageVersion() != subsystem) 
                return null;

            //define header
            ping.setHeader(header);
            ping.calculateTimeStamp(); 

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

    public SdfData getPingAt(Long timestamp, int subsystem) {
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

        nextTimestamp.put(subsystem, tslist.get(subsystem)[c+1]);

        for(Long pos : positionMap.get(ts)) {
            ping = getPingAtPosition(pos, subsystem);
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
    
    public static void main(String[] args) {
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

    }
}
