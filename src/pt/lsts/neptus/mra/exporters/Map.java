/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
import java.util.List;

/**
 * @author Frédéric Leishman
 * 
 */
public class Map {
    public Map(double res, double x_min, double x_max, double y_min, double y_max) {

        cells = new ArrayList<Cells>();

        // Map resolution
        resolution = res;

        // Num Pts min by cells
        pts_by_cells = 30;

        // Num Pts for gaussian kernel validation
        num_pts_validation = 20;

        // Map limits
        min_x = x_min;
        max_x = x_max;
        min_y = y_min;
        max_y = y_max;

        // Map indices size
        num_i = (int) ((Math.abs(x_max - x_min) / resolution));
        num_j = (int) ((Math.abs(y_max - y_min) / resolution));
        num_cells = 0;

        // Map initialization
        double pos_x;
        double pos_y;
        for (int j = 0; j < num_j; j++) {
            for (int i = 0; i < num_i; i++) {
                pos_x = min_x + (double) (i) * resolution;
                pos_y = min_y + (double) (j) * resolution;
                cells.add(new Cells(i, j, pos_x, pos_y));
                num_cells++;
            }
        }

        num_cells_valid = 0;
    }

    public void SetParameters(int pts_min_cells, int pts_min_gaussian) {
        // Num Pts min by cells
        pts_by_cells = pts_min_cells;

        // Num Pts for gaussian kernel validation
        num_pts_validation = pts_min_gaussian;
    }

    public void CreateMapWithPointCloud(List<double[]> PointList, int num_point) {
        // Attribution points to cells
        int i, j, id;
        for (int n = 0; n < num_point; n++) {
            i = (int) ((double) (num_i) * (PointList.get(n)[0] - min_x) / (Math.abs(max_x - min_x)));
            j = (int) ((double) (num_j) * (PointList.get(n)[1] - min_y) / (Math.abs(max_y - min_y)));
            id = i + j * num_i;

            // Add the point to the correct cell
            if (id < num_cells) {
                int nb_pts = cells.get(id).GetNumPoint();
                if (nb_pts < cells.get(id).MAX_POINT_CELLS) {
                    cells.get(id).PointList.add(PointList.get(n)[2]);
                    cells.get(id).SetNumPoint(nb_pts + 1);
                }
            }
            else {
                // Just for the x y max points
                id = -1;
            }
        }

        // Value Computing of cells
        for (int n = 0; n < num_cells; n++) {
            if (cells.get(n).GetNumPoint() > pts_by_cells) {
                // Cell is valid and filled
                cells.get(n).ValueByMedian();
                num_cells_valid++;
                cells.get(n).SetValid();
            }
            else {
                // Cell is unknown
                cells.get(n).SetUnvalid();
            }
        }

        // Outliers deletion
        for (i = 3; i < num_i - 3; i++) {
            for (j = 3; j < num_j - 3; j++) {
                int n = i + j * num_i;

                // If the cell is filled, his value is checked to eliminated the outliers
                if (cells.get(n).IsValidated()) {
                    double average = 0;
                    double standard_deviance = 0;

                    double num = 0;
                    double[][] val = new double[7][7];

                    for (int a = 0; a < 7; a++) {
                        for (int b = 0; b < 7; b++) {
                            id = (i + a - 3) + (j + b - 3) * num_i;
                            if (cells.get(id).IsValidated()) {
                                num += 1;
                                average += cells.get(id).depth;
                                val[a][b] = cells.get(id).depth;
                            }
                        }
                    }
                    average /= num;

                    num = 0;
                    for (int a = 0; a < 7; a++) {
                        for (int b = 0; b < 7; b++) {
                            id = (i + a - 3) + (j + b - 3) * num_i;
                            if (cells.get(id).IsValidated()) {
                                standard_deviance += Math.abs(val[a][b] - average);
                                num++;
                            }
                        }
                    }
                    standard_deviance /= num;

                    if (Math.abs(cells.get(n).depth - average) > 3 * standard_deviance) {
                        cells.get(n).SetUnvalid();
                        num_cells_valid--;
                    }
                }
            }
        }

        // Filled empty cells inside the map
        for (i = 3; i < num_i - 3; i++) {
            for (j = 3; j < num_j - 3; j++) {
                int n = i + j * num_i;

                // If the cell is not filled
                if (!cells.get(n).IsValidated()) {

                    double ponseration_sum = 0;
                    double num_valid = 0;
                    double val_temp = 0;

                    for (int a = 0; a < 7; a++) {
                        for (int b = 0; b < 7; b++) {
                            id = (i + a - 3) + (j + b - 3) * num_i;
                            if (cells.get(id).IsValidated()) {
                                // Gaussian ponderation (close value is more important than far value)
                                val_temp += cells.get(id).depth * gaussian_ponderation[a][b];
                                ponseration_sum += gaussian_ponderation[a][b];
                                num_valid++;
                            }
                        }
                    }
                    if (num_valid > num_pts_validation) // Number of point required for a good average
                    {// If num_valid is too low, all cells empty will fill with a wrong value
                        val_temp /= ponseration_sum;
                        cells.get(n).depth = val_temp;
                        cells.get(n).SetValid();
                        num_cells_valid++;
                    }
                }
            }
        }

    }

    private double resolution;

    double min_x;
    double max_x;
    double min_y;
    double max_y;

    int num_cells;
    int num_cells_valid;
    int num_i;
    int num_j;

    int pts_by_cells;
    int num_pts_validation;

    List<Cells> cells;

    double gaussian_ponderation[][] = { { 0.1054, 0.1970, 0.2865, 0.3246, 0.2865, 0.1970, 0.1054 },
            { 0.1970, 0.3679, 0.5353, 0.6065, 0.5353, 0.3679, 0.1970 },
            { 0.2865, 0.5353, 0.7788, 0.8825, 0.7788, 0.5353, 0.2865 },
            { 0.3246, 0.6065, 0.8825, 1.0000, 0.8825, 0.6065, 0.3246 },
            { 0.2865, 0.5353, 0.7788, 0.8825, 0.7788, 0.5353, 0.2865 },
            { 0.1970, 0.3679, 0.5353, 0.6065, 0.5353, 0.3679, 0.1970 },
            { 0.1054, 0.1970, 0.2865, 0.3246, 0.2865, 0.1970, 0.1054 } };

}
