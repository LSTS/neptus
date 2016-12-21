/*
 * Copyright (c) 2004-2016 OceanScan - Marine Systems & Technology, Lda.
 * Polo do Mar do UPTEC, Avenida da Liberdade, 4450-718 Matosinhos, Portugal
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
 */

package pt.lsts.neptus.plugins.mjpeg.containers.avi;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import pt.lsts.neptus.plugins.mjpeg.containers.riff.Chunk;
import pt.lsts.neptus.plugins.mjpeg.containers.riff.List;
import pt.lsts.neptus.plugins.mjpeg.containers.riff.RiffFile;

/**
 * Parser for AVI encoded MJPEG streams.
 *
 * @author Ricardo Martins
 */
public class MjpegFile extends RiffFile {
    /** List of index records. */
    private final ArrayList<IndexRecord> index = new ArrayList<>();
    /** Frame rate. */
    private int frameRate;

    public MjpegFile(File file) throws Exception {
        super(file);

        if (!rootList.getName().equals("AVI "))
            throw new Exception("Invalid AVI file");

        loadIndex();
    }

    public int getFrameRate() {
        return frameRate;
    }
    
    public long getFrameCount() {
        return index.size();
    }

    public long getFrameTime(int frameNumber) {
        return index.get(frameNumber).getTimeStamp();
    }

    public Image getFrameImage(int frameNumber) {
        IndexRecord record = index.get(frameNumber);

        try {
            byte[] rawData = new byte[record.getSize()];
            memory.position(record.getOffset() + 8);
            memory.get(rawData, 0, record.getSize());
            ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(rawData));
            return ImageIO.read(iis);
        } 
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void loadIndex() throws Exception {
        while (rootList.hasNext()) {
            Chunk listChunk = rootList.next();
            if (listChunk.getId().equals("LIST")) {
                pt.lsts.neptus.plugins.mjpeg.containers.riff.List l = (pt.lsts.neptus.plugins.mjpeg.containers.riff.List)listChunk;
                if (l.getName().equals("hdrl"))
                    readHDRL(l);
            }
            else if (listChunk.getId().equals("idx1")) {
                readIDX1(listChunk);
            }
            else if (listChunk.getId().equals("tstp")) {
                readTSTP(listChunk);
            }
        }
    }

    private void readHDRL(List hdrl) {
        while (hdrl.hasNext()) {
            Chunk chunk = hdrl.next();
            if (chunk.getId().equals("avih")) {
                int usecPerFrame = memory.getInt(chunk.getOffset() + 8);
                frameRate = (int) Math.round(1000000.0 / usecPerFrame);
            }
        }
    }

    private void readIDX1(Chunk chunk) {
        int offset = chunk.getOffset() + 8;

        for (int i = 0; i < chunk.getSize(); i += 16)
            readRecordIDX1(i, offset + i);
    }

    private void readRecordIDX1(int number, int offset) {
        IndexRecord record = new IndexRecord();
        record.setFlags(memory.getInt(offset + 4));
        record.setOffset(memory.getInt(offset + 8));
        record.setSize(memory.getInt(offset + 12));
        index.add(record);
    }

    private void readTSTP(Chunk chunk) {
        int offset = chunk.getOffset() + 8;

        for (int i = 0; i < chunk.getSize(); i += 8)
            readRecordTSTP(i / 8, offset + i);
    }

    private void readRecordTSTP(int number, int offset) {
        long timeStamp = (long) (memory.getDouble(offset) * 1000.0);
        index.get(number).setTimeStamp(timeStamp);
    }
}
