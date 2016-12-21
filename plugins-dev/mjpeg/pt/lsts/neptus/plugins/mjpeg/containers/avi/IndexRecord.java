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

/**
 * Representation of an AVI index record.
 *
 * @author Ricardo Martins
 */
public class IndexRecord {
    /** Offset in the AVI file. */
    private int offset;
    /** Size of the record. */
    private int size;
    /** Flags. */
    private int flags;
    /** Record timestamp. */
    private long timeStamp;

    /**
     * Gets the absolute record offset in the AVI file.
     *
     * @return record offset.
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Sets the absolute record offset in the AVI file.
     *
     * @param offset record offset.
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Gets the record size.
     *
     * @return record size in bytes.
     */
    public int getSize() {
        return size;
    }

    /**
     * Sets the record size.
     *
     * @param size record size in bytes.
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Gets the record flags.
     *
     * @return record flags.
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Sets the record flags.
     *
     * @param flags record flags.
     */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /**
     * Gets the record timestamp.
     *
     * @return record timestamp in milliseconds since the Unix Epoch.
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * Sets the record timestamp.
     *
     * @param timeStamp timestamp in milliseconds since the Unix Epoch.
     */
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }
}
