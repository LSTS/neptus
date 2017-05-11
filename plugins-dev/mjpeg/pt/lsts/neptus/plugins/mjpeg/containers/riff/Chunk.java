/*
 * Copyright (c) 2004-2016 OceanScan - Marine Systems & Technology Lda.
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
 */

package pt.lsts.neptus.plugins.mjpeg.containers.riff;

import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Class representing an AVI chunk.
 *
 * @author Ricardo Martins
 */
public class Chunk {
    /** Chunk identifier. */
    private String id;
    /** Chunk size in bytes. */
    private int size;
    /** Offset of chunk in memory buffer. */
    protected int offset;
    /** Memory mapped buffer. */
    protected MappedByteBuffer memory;

    private static String readFourCC(MappedByteBuffer memory, int offset) {
        byte[] fourCC = new byte[4];
        memory.position(offset);
        memory.get(fourCC);
        return new String(fourCC, Charset.forName("ISO-8859-1"));
    }

    public static Chunk load(MappedByteBuffer memory, int offset) {
        Chunk chunk;
        String chunkId = readFourCC(memory, offset);
        int chunkSize = memory.getInt(offset + 4);
        if (chunkId.equals("RIFF") || chunkId.equals("LIST")) {
            String chunkName = readFourCC(memory, offset + 8);
            chunk = new List(memory, chunkId, chunkName, offset + 12, chunkSize);
        } else {
            chunk = new Chunk(memory, chunkId, offset, chunkSize);
        }

        return chunk;
    }

    public Chunk(MappedByteBuffer memory, String id, int offset, int size) {
        this.memory = memory;
        this.id = id;
        this.offset = offset;
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String toString() {
        return String.format(Locale.US, "CHUNK [%s] (%d:%d)", getId(), getOffset(), getSize());
    }
}
