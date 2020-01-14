/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Renato Campos
 * 14 Nov 2018
 */
package pt.lsts.neptus.mra.importers.i872;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.HashMap;
import java.util.TreeSet;

import pt.lsts.neptus.NeptusLog;

public class Imagenex872Parser {

    private final int WINDOW_SIZE = 100;
    private HashMap<Long, Imagenex872Ping> pings; // Maps timestamps to pings
    private TreeSet<Long> timestampsSet;
    private FileInputStream fis;
    private FileChannel channel;
    private long minTimestamp, maxTimestamp; // Current minTimestamp and maxTimestamp loaded.
    private String indexPath;
    private Imagenex872Index index;

    public Imagenex872Parser(File file) {
        index = new Imagenex872Index();
        indexPath = file.getParent() + "/mra/i872.index";
        minTimestamp = -1;
        maxTimestamp = -1;
        try {
            fis = new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        channel = fis.getChannel();
        if (!new File(indexPath).exists()) {
            NeptusLog.pub().info("Generating 872 index for " + file.getAbsolutePath());
            generateIndex();
        }
        else {
            NeptusLog.pub().info("Loading 872 index for " + file.getAbsolutePath());
            if(!loadIndex()) {
                NeptusLog.pub().error("Corrupted 872 index file. Trying to create a new index.");
                generateIndex();
            }
        }
    }
    
    /**
     * Create an index for the file being parsed in order to be possible to load big data files.
     */
    private void generateIndex() {
        try {
            for (long pos = 0; pos < channel.size(); pos += Imagenex872Ping.PING_SIZE) {
                ByteBuffer pingBuffer = channel.map(MapMode.READ_ONLY, pos, Imagenex872Ping.PING_SIZE);
                Imagenex872Header pingHeader = new Imagenex872Header(pingBuffer, true);
                index.addPing(pingHeader.getTimestamp(), pos);
            }
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(indexPath));
            out.writeObject(index);
            out.close();
            NeptusLog.pub().info("872 index written");
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
    
    private boolean loadIndex() {
        ObjectInputStream in;
        try {
            in = new ObjectInputStream(new FileInputStream(indexPath));
            index = (Imagenex872Index) in.readObject();
            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Parses a part of the file.
     * The number of pings being read depends on the WINDOW_SIZE variable.
     * @param initialPos Initial position to start parsing the file.
     */
    private void parseFile(Long initialPos) {
        pings = new HashMap<Long, Imagenex872Ping>();
        timestampsSet = new TreeSet<Long>();
        try {
            long pingTimestamp;
            long lastIndex = channel.size() - Imagenex872Ping.PING_SIZE;
            if (channel.size() - Imagenex872Ping.PING_SIZE < initialPos + WINDOW_SIZE * Imagenex872Ping.PING_SIZE) {
                lastIndex = channel.size() - Imagenex872Ping.PING_SIZE;
            }
            else {
                lastIndex = initialPos + WINDOW_SIZE * Imagenex872Ping.PING_SIZE;
            }
            for (long i = initialPos; i <= lastIndex; i += Imagenex872Ping.PING_SIZE) {
                ByteBuffer pingBuffer = channel.map(MapMode.READ_ONLY, i, Imagenex872Ping.PING_SIZE);
                Imagenex872Ping currentPing = new Imagenex872Ping(pingBuffer);
                pingTimestamp = currentPing.getTimestamp();
                timestampsSet.add(pingTimestamp);
                pings.put(pingTimestamp, currentPing);
                if (i == initialPos) {
                    minTimestamp = pingTimestamp;
                }
                else if (i == lastIndex) {
                    maxTimestamp = pingTimestamp;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    public long getFirstTimestamp() {
        return index.getFirstTimestamp();
    }

    public long getLastTimestamp() {
        return index.getLastTimestamp();
    }

    public void cleanup() {
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

    /**
     * Get the lower ping which timestamp is greater than or equal to the given timestamp input.
     * @param timestamp The timestamp of the ping to search.
     * @return the lower ping which timestamp is greater than or equal to the given timestamp input.
     */
    public Imagenex872Ping getPingAt(long timestamp) {
        if (timestamp < minTimestamp || timestamp > maxTimestamp) {
            parseFile(index.getPositionOfPing(timestamp));
        } 
        long nextTimestamp = timestampsSet.ceiling(timestamp);
        return pings.get(nextTimestamp);
    }
    
    public static void main(String[] args) throws IOException {
        Imagenex872Parser parser = new Imagenex872Parser(new File("/home/ineeve/Downloads/111958_AFPF/Data.872"));
        long firstTimestamp = parser.getFirstTimestamp();
        parser.getPingAt(firstTimestamp);
    }
}
