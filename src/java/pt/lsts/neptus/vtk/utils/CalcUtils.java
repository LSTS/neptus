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
 * Author: hfq
 * Jun 21, 2013
 */
package pt.lsts.neptus.vtk.utils;

/**
 * @author hfq
 * 
 */
public class CalcUtils {

    /**
     * Calc distance between 2 points in space
     * @param p1
     * @param p2
     * @return
     */
    public static float distanceBetween2Points(float p1[], float p2[]) {
        return (float) (Math.sqrt((p1[0] - p2[0]) * (p1[0] - p2[0]) + (p1[1] - p2[1]) * (p1[1] - p2[1])
                + (p1[2] - p2[2]) * (p1[2] - p2[2])));

    }

    /**
     * Calc distance between 2 points in space
     * @param p1
     * @param p2
     * @return
     */
    public static double distanceBetween2Points(double[] p1, double[] p2) {
        return (Math.sqrt((p1[0] - p2[0]) * (p1[0] - p2[0]) + (p1[1] - p2[1]) * (p1[1] - p2[1]) + (p1[2] - p2[2])
                * (p1[2] - p2[2])));
    }

    /**
     * 
     * @param sum - sum of values
     * @param sqSum - sum of the square values
     * @param numberValues
     * @return
     */
    public static double stddev(double sum, double sqSum, int numberValues) {
        double variance = (sqSum - (sum * sum) / numberValues) / (numberValues - 1);
        return Math.sqrt(variance);
    }
}
