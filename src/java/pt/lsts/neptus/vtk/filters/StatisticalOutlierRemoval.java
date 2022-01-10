/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.vtk.filters;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.vtk.utils.CalcUtils;
import vtk.vtkIdList;
import vtk.vtkKdTree;
import vtk.vtkPoints;

/**
 * @author hfq based on R. B. Rusu, Z. C. Marton, N. Blodow, M. Dolha, and M. Beetz Towards 3D Point Cloud Based Object
 *         Maps for Household Environments Robotics and Autonomous Systems Journal (Special Issue on Semantic
 *         Knowledge), 2008.
 * 
 *         The algorithm iterates through the entire point cloud twice. On the first iteration it will compute the
 *         average distance that each point has to its k nearest neighbors Next, the mean and standard deviation of all
 *         the distances are computed in order to determine a distance threshold. The distance threshold will be = mean
 *         + stdMul * stddev. During the next iteration the point will be classified as inlier or outlier if their
 *         average distance is below or above this threshold respectively
 */
public class StatisticalOutlierRemoval {

    // Number of points to use for the mean distance estimation
    private int meanK = 30;
    // Standard deviations threshold multiplier
    private double stdMul = 1.0;
    private vtkPoints outputPoints;

    /**
     * 
     */
    public StatisticalOutlierRemoval() {

    }

    /**
     * FIXME - Check stddev calc with medium (easier)
     * 
     * @param points
     */
    public void applyFilter(vtkPoints points) {
        try {
            NeptusLog.pub().info("Number of input points: " + points.GetNumberOfPoints());
            NeptusLog.pub().info("Statistical outliers removal start: " + System.currentTimeMillis());

            setOutputPoints(new vtkPoints());
            int outputId = 0;

            vtkKdTree kdTree = new vtkKdTree();
            kdTree.BuildLocatorFromPoints(points);

            double[] meanDistances = new double[points.GetNumberOfPoints()];
            double meanDistancesSum = 0.0;
            double sqSumDistances = 0.0;

            meanK++; // because the indice 0 from N closests points is the query point

            for (int i = 0; i < points.GetNumberOfPoints(); ++i) {
                vtkIdList idsFoundPts = new vtkIdList();
                kdTree.FindClosestNPoints(meanK, points.GetPoint(i), idsFoundPts); // indice 0 is the query point

                double distSum = 0.0;
                // double sqSum = 0.0;
                for (int k = 1; k < meanK; ++k) {
                    double dist = 0.0;
                    dist += CalcUtils.distanceBetween2Points(points.GetPoint(i), points.GetPoint(idsFoundPts.GetId(k)));
                    distSum += dist; // should be sqrt(dist) ?
                    sqSumDistances += dist * dist;
                }
                meanDistances[i] = distSum / (meanK - 1);
                meanDistancesSum += meanDistances[i];
            }
            double mean = meanDistancesSum / points.GetNumberOfPoints();
            double stddev = CalcUtils.stddev(meanDistancesSum, sqSumDistances, points.GetNumberOfPoints());

            double distanceThreshold = mean + stdMul * stddev;

            // check wether it is inlier or outlier
            for (int c = 0; c < points.GetNumberOfPoints(); ++c) {
                if (meanDistances[c] <= distanceThreshold) {
                    getOutputPoints().InsertPoint(outputId, points.GetPoint(c));
                    ++outputId;
                }
            }

            NeptusLog.pub().info("Statistical outliers removal end: " + System.currentTimeMillis());
            NeptusLog.pub().info("Number of points on output: " + getOutputPoints().GetNumberOfPoints());

            float perc = (float) getOutputPoints().GetNumberOfPoints() / points.GetNumberOfPoints();
            NeptusLog.pub().info("Percentage of inlier points: " + perc);

            if (perc >= 0.90) {
                setOutputPoints(outputPoints);
            }
            else {
                NeptusLog.pub().info("Relax your parameters for radius outliers removal");
                setOutputPoints(points);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the outputPoints
     */
    public vtkPoints getOutputPoints() {
        return outputPoints;
    }

    /**
     * @param outputPoints the outputPoints to set
     */
    public void setOutputPoints(vtkPoints outputPoints) {
        this.outputPoints = outputPoints;
    }

    /**
     * @return the meanK
     */
    public int getMeanK() {
        return meanK;
    }

    /**
     * @param meanK the meanK to set
     */
    public void setMeanK(int meanK) {
        this.meanK = meanK;
    }

    /**
     * @return the stdMul
     */
    public double getStdMul() {
        return stdMul;
    }

    /**
     * @param stdMul the stdMul to set
     */
    public void setStdMul(double stdMul) {
        this.stdMul = stdMul;
    }
}
