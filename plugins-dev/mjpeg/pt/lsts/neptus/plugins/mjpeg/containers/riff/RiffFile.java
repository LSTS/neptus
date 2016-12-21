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

package pt.lsts.neptus.plugins.mjpeg.containers.riff;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Simple RIFF file parser.
 *
 * @author Ricardo Martins
 */
public class RiffFile {
    private RandomAccessFile memoryMappedFile;
    private long fileSize = 0;
    protected List rootList;
    protected MappedByteBuffer memory;

    public RiffFile(File file) throws Exception {
        memoryMappedFile = new RandomAccessFile(file, "r");
        fileSize = file.length();
        memory = memoryMappedFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
        memory.order(ByteOrder.LITTLE_ENDIAN);
        rootList = loadRootList();
    }

    private List loadRootList() throws Exception {
        Chunk chunk = getChunkAt(0);
        if (!(chunk instanceof List)) {
            throw new Exception("root list is not present");
        }

        List list = (List) chunk;
        if (!(list.getId().equals("RIFF") && list.getName().equals("AVI ")))
            throw new Exception("invalid file");

        return list;
    }

    public Chunk getChunkAt(int offset) {
        return Chunk.load(memory, offset);
    }
}
