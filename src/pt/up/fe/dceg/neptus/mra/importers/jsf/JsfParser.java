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
 * Author: jqcorreia
 * Feb 5, 2013
 */
package pt.up.fe.dceg.neptus.mra.importers.jsf;

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

/**
 * @author jqcorreia
 * 
 */
public class JsfParser {
    File file;
    FileInputStream fis;
    FileChannel channel;
    int curPosition = 0;
    int curPingNumber = 0;
    JsfIndex index = new JsfIndex();    
    
    public JsfParser(File file) {
        try {
            this.file = file;
            fis = new FileInputStream(file);
            channel = fis.getChannel();
            if (!new File(file.getParent() + "/jsf.index").exists()) {
                System.out.println("Generating JSF index for " + file.getAbsolutePath());
                generateIndex();
            }
            else {
                System.out.println("Loading JSF index for " + file.getAbsolutePath());
                loadIndex();
            }

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void generateIndex() {
        JsfHeader header = new JsfHeader();
        JsfSonarData ping = new JsfSonarData();

        int count = 0;
        int pos = 0;
        try {
            while (true) {
                // Read ONLY the header
                ByteBuffer buf = channel.map(MapMode.READ_ONLY, curPosition, 16);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                header.parse(buf);
                curPosition += 16;
                if (header.getType() == 80) {
                    ping.setHeader(header);

                    buf = channel.map(MapMode.READ_ONLY, curPosition, 240);
                    buf.order(ByteOrder.LITTLE_ENDIAN);
                    ping.parseHeader(buf);
                    curPosition += header.getMessageSize();
                     System.out.println(ping.getPingNumber() + " " + ping.getTimestamp() + " " +
                     ping.getNumberOfSamples() + " " +
                     ping.getFrequency() + " "
                     + ping.getHeader().getSubsystem() + " " + ping.getHeader().getChannel());
                }
                else {
                    curPosition += header.getMessageSize();
                    pos = curPosition;
                    continue;
                }

                // Process first message to save timestamp
                if (count == 0) {
                    index.firstTimestamp = ping.getTimestamp();
                }

                // Process the header to build the index
                long t = ping.getTimestamp(); // Timestamp
                int pn = ping.getPingNumber(); // Ping number
                float f = ping.getFrequency(); // Frequency
                int subsystem = ping.getHeader().getSubsystem();
                
                if (!index.frequenciesList.contains(f)) {
                    index.frequenciesList.add(f);
                }
                
                if (!index.subSystemsList.contains(subsystem)) {
                    index.subSystemsList.add(subsystem);
                }
                
                if (index.pingMap.get(t) == null) {
                    index.pingMap.put(t, pn);
                }

                ArrayList<Integer> l = index.positionMap.get(pn);
                if (l == null) {
                    l = new ArrayList<Integer>();
                    l.add(pos);
                    index.positionMap.put(pn, l);
                }
                else {
                    l.add(pos);
                }

                count++;
                pos = curPosition;

                if (curPosition >= channel.size())
                    break;
            }
            index.lastTimestamp = ping.getTimestamp();
            index.numberOfPackets = count;

            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file.getParent() + "/jsf.index"));
            out.writeObject(index);
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadIndex() {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file.getParent() + "/jsf.index"));
            index = (JsfIndex) in.readObject();

            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getFirstTimeStamp() {
        return index.firstTimestamp;
    }

    public long getLastTimeStamp() {
        return index.lastTimestamp;
    }

    public JsfSonarData getPingAtPosition(int pos, int subsystem) {
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
        ArrayList<JsfSonarData> ping = new ArrayList<JsfSonarData>();
        
        if(index.positionMap.get(curPingNumber) == null) {
            return ping;
        }
        for (int i = 0; i < index.positionMap.get(curPingNumber).size(); i++) {
            JsfSonarData p = getPingAtPosition(index.positionMap.get(curPingNumber).get(i), subsystem);
            if(p != null)
                ping.add(p);
        }
        
        curPingNumber++; 
        return ping;
    }

    public ArrayList<JsfSonarData> getPingAt(long timestamp, int subsystem) {
        curPosition = 0;
        ArrayList<JsfSonarData> ping;
        long ts = 0;

        for (long l : index.pingMap.keySet()) {
            if (l >= timestamp) {
                ts = l;
                break;
            }
        }
        curPingNumber = index.pingMap.get(ts);
        ping = nextPing(subsystem);

        return ping;
    }

    public static void main(String[] args) throws IOException {
        JsfParser parser = new JsfParser(new File("/home/jqcorreia/lsts/logs/182142_edgetch_sweep/Data.jsf"));
    
        for(Integer i : parser.index.subSystemsList) {
            System.out.println(i);
        }
    }
}
