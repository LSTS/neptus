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

public class DvsReturn {
    private final double DECOMPRESSION_FACTOR_CENTER = 1.045;
    private final double DECOMPRESSION_FACTOR_EDGE = 1.025;
    private final double diff = DECOMPRESSION_FACTOR_EDGE - DECOMPRESSION_FACTOR_CENTER;
    private final double[] data;

    public DvsReturn(byte[] data) {
        this.data = new double[data.length];

        // The data points are logarithmically compressed.
        // So we need to decompress them.
        double half_length = (data.length / 2) - 1;
        for (int i = 0; i < data.length / 2; i++) {
            double decompression_factor = DECOMPRESSION_FACTOR_CENTER + ((double) i / half_length) * diff;
            this.data[i] = Math.pow(decompression_factor, Byte.toUnsignedInt(data[i]));
        }
        for (int i = data.length / 2; i < data.length; i++) {
            double decompression_factor = DECOMPRESSION_FACTOR_EDGE - (((double) i - (double) (data.length / 2)) / half_length) * diff;
            this.data[i] = Math.pow(decompression_factor, Byte.toUnsignedInt(data[i]));
        }
    }

    public double[] getData() {
        return data;
    }
}
