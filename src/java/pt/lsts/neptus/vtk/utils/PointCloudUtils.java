/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Jul 15, 2013
 */
package pt.lsts.neptus.vtk.utils;

import pt.lsts.neptus.vtk.pointcloud.APointCloud;
import vtk.vtkDataArray;
import vtk.vtkPoints;

/**
 * @author hfq
 * 
 */
public class PointCloudUtils {

    protected static double[] center;

    /**
     * @param points
     * @return
     */
    public static double[] computeBounds(vtkPoints points) {
        double[] bounds = new double[6];

        double p[] = points.GetPoint(0);
        bounds[0] = bounds[1] = p[0];
        bounds[2] = bounds[3] = p[1];
        bounds[4] = bounds[5] = p[2];

        for (int i = 1; i < points.GetNumberOfPoints(); ++i) {
            p = points.GetPoint(i);
            for (int j = 0; j < 3; ++j) {
                if (p[j] < bounds[2 * j]) // min values
                    bounds[2 * j] = p[j];
                if (p[j] > bounds[2 * j + 1]) { // max values
                    bounds[2 * j + 1] = p[j];
                }
            }
        }

        return bounds;
    }

    public static double[] computeBounds(APointCloud<?> pointCloud) {

        double[] bounds = new double[6];

        double p[] = pointCloud.getXYZPoints().GetPoint(0);
        bounds[0] = bounds[1] = p[0];
        bounds[2] = bounds[3] = p[1];
        bounds[4] = bounds[5] = p[2];

        for (int i = 1; i < pointCloud.getNumberOfPoints(); ++i) {
            p = pointCloud.getXYZPoints().GetPoint(i);
            for (int j = 0; j < 3; ++j) {
                if (p[j] < bounds[2 * j]) // min values
                    bounds[2 * j] = p[j];
                if (p[j] > bounds[2 * j + 1]) { // max values
                    bounds[2 * j + 1] = p[j];
                }
            }
        }
        return bounds;
    }

    public static double[] computeScalarRange(APointCloud<?> pointCloud) {
        double[] scalarRange = new double[2];

        return scalarRange;
    }

    public static double[] computeScalarRange(vtkDataArray scalars) {
        double[] scalarRange = new double[2];

        return scalarRange;
    }

    public static double[] computeCenter(APointCloud<?> pointCloud) {
        double[] center = new double[3];

        double[] bounds = computeBounds(pointCloud);

        for (int i = 0; i < 3; ++i) {
            center[i] = (bounds[i * 2] + bounds[i * 2 + 1]) / 2;
        }

        return center;
    }

    public static double[] computeCenter(vtkPoints points) {
        double[] center = new double[3];

        double[] bounds = computeBounds(points);

        for (int i = 0; i < 3; ++i) {
            center[i] = (bounds[i * 2] + bounds[i * 2 + 1]) / 2;
        }

        return center;
    }
}
