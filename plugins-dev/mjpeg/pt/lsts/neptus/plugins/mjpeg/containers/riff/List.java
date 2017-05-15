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
import java.util.Locale;

/**
 * Class representing an AVI chunk list.
 *
 * @author Ricardo Martins
 */
public class List extends Chunk {
    /** List name. */
    private String name;
    /** Current read offset. */
    private int cursor;

    public List(MappedByteBuffer memory, String id, String name, int offset, int size) {
        super(memory, id, offset, size);
        this.name = name;
        this.cursor = offset;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasNext() {
        return (cursor + 8 - getOffset() <= getSize());
    }

    public Chunk next() {
        Chunk chunk = load(memory, cursor);
        cursor += chunk.getSize() + 8;
        return chunk;
    }

    public String toString() {
        return String.format(Locale.US, "LIST [%s:%s] (%d:%d)", getId(), getName(), getOffset(), getSize());
    }
}
