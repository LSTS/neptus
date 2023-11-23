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
 * 17/Oct/2023
 */
package pt.lsts.neptus.plugins.deepvision;

import java.io.Serializable;

public class DvsHeader implements Serializable {
    /* HEADER STRUCTURE
    Version, UINT32, Must be 0x00000001
    File Header, V1_FileHeader

    Where V1_FileHeader is

    struct V1_FileHeader {
        float sampleRes;
        float lineRate;
        int nSamples;
        bool left;
        bool right;
    };

    Plus 2 padding bytes to align at the word boundary
    */
    public static final int HEADER_SIZE = 20;      // Bytes
    public static final int VERSION = 1;           // VERSION

    private final float DEFAULT_LINE_RATE = (float) 9.900990099;  // Default line rate
    private float sampleResolution;         // sampleRes [m]
    private float lineRate;                 // lineRate [ ping/s ]
    private int nSamples;                   // nSamples: Number of samples per side
    private boolean leftChannelActive;      // left: true if left/port side active
    private boolean rightChannelActive;     // right: true if right/starboard side active

    public DvsHeader() {
    }

    public float getSampleResolution() {
        return sampleResolution;
    }

    public void setSampleResolution(float sampleResolution) {
        this.sampleResolution = sampleResolution;
    }

    public float getLineRate() {
        return lineRate;
    }

    public void setLineRate(float lineRate) {
        this.lineRate = lineRate > 0 ? lineRate : DEFAULT_LINE_RATE;
    }

    public int getnSamples() {
        return nSamples;
    }

    public void setnSamples(int nSamples) {
        this.nSamples = nSamples;
    }

    public boolean isLeftChannelActive() {
        return leftChannelActive;
    }

    public void setLeftChannelActive(boolean leftChannelActive) {
        this.leftChannelActive = leftChannelActive;
    }

    public boolean isRightChannelActive() {
        return rightChannelActive;
    }

    public void setRightChannelActive(boolean rightChannelActive) {
        this.rightChannelActive = rightChannelActive;
    }

    public boolean versionMatches(int version) {
        return VERSION == version;
    }

    public int getNumberOfActiveChannels() {
        return (leftChannelActive ? 1 : 0) + (rightChannelActive ? 1 : 0);
    }

    public float getRange() {
        return sampleResolution * nSamples;
    }
}
