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
 * Author: Frédéric Leishman
 * 12 juin 2014
 */
package pt.lsts.neptus.mra.exporters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Frédéric Leishman
 * 
 */
public class Cells {
    public Cells(int i, int j, double pos_x, double pos_y) {
        num_pts = 0;
        MAX_POINT_CELLS = 1000;
        PointList = new ArrayList<Double>();
        index_i = i;
        index_j = j;
        position_x = pos_x;
        position_y = pos_y;
    }

    public int GetIndex_i() {
        return index_i;
    }

    public int GetIndex_j() {
        return index_j;
    }

    public int GetNumPoint() {
        return num_pts;
    }

    public void SetNumPoint(int n) {
        num_pts = n;
    }

    public void SetValid() {
        valid = true;
    }

    public void SetUnvalid() {
        valid = false;
    }

    public boolean IsValidated() {
        return valid;
    }

    public void ValueByMedian() {
        // Quicksort is used
        Arrays.sort(PointList.toArray());

        // Median computing
        double median = 0;

        if (PointList.toArray().length % 2 == 0) {
            median = ((double) PointList.toArray()[PointList.toArray().length / 2] + (double) PointList.toArray()[PointList
                    .toArray().length / 2 - 1]) / 2;
        }
        else {
            median = (double) PointList.toArray()[PointList.toArray().length / 2];
        }

        // Value of cell
        depth = median;
    }

    private int index_i;
    int index_j;
    int num_pts;
    List<Double> PointList;
    boolean valid;

    public int MAX_POINT_CELLS;

    // Value of the cell
    double position_x;
    double position_y;
    double depth;
}
