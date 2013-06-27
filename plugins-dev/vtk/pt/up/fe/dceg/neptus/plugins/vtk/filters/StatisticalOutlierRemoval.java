/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: hfq
 * Jun 21, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.filters;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.plugins.vtk.utils.CalcUtils;
import vtk.vtkIdList;
import vtk.vtkKdTree;
import vtk.vtkPoints;

/**
 * @author hfq
 * based on R. B. Rusu, Z. C. Marton, N. Blodow, M. Dolha, and M. Beetz
 * Towards 3D Point Cloud Based Object Maps for Household Environments
 * Robotics and Autonomous Systems Journal (Special Issue on Semantic Knowledge), 2008.
 * 
 * The algorithm iterates through the entire point cloud twice.
 * On the first iteration it will compute the average distance that each point has to its k nearest neighbors
 * Next, the mean and standard deviation of all the distances are computed in order to determine a distance threshold.
 * The distance threshold will be = mean + stdMul * stddev.
 * During the next iteration the point will be classified as inlier or outlier if their average distance is below or
 * above this threshold respectively
 *
 */
public class StatisticalOutlierRemoval {
    
        // Number of points to use for the mean distance estimation
    private int meanK = 10;
        // Standard deviations threshold multiplier
    private double stdMul = 0.5;   
    private vtkPoints outputPoints;
    
    /**
     * 
     */
    public StatisticalOutlierRemoval() {
        
    }
    
    public void applyFilter (vtkPoints points) {
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
            
            //double mean = 0.0;
            
            meanK++; // because the indice 0 from N closests points is the query point
            
            for (int i = 0; i < points.GetNumberOfPoints(); ++i) {
                vtkIdList idsFoundPts = new vtkIdList();
                kdTree.FindClosestNPoints(meanK , points.GetPoint(i), idsFoundPts);  // indice 0 is the query point
                
                double distSum = 0.0;
                //double sqSum = 0.0;
                for (int k = 1; k < meanK; ++k) {
                    double dist = 0.0;
                    dist += CalcUtils.distanceBetween2Points(points.GetPoint(i),
                            points.GetPoint(idsFoundPts.GetId(k)));
                    distSum += dist;        // should be sqrt(dist) ?
                    sqSumDistances += dist * dist;
                }
                meanDistances[i] = distSum/(meanK-1);
                meanDistancesSum += meanDistances[i];
            }          
            double mean = meanDistancesSum/points.GetNumberOfPoints();
            //double stddev = 0.0;
            double stddev = CalcUtils.stddev(meanDistancesSum, sqSumDistances, points.GetNumberOfPoints());
            
            double distanceThreshold = mean + stdMul * stddev;
            
            NeptusLog.pub().info("Mean: " + mean);
            NeptusLog.pub().info("StdDev: " + stddev);
            NeptusLog.pub().info("Distance Threshold: " + distanceThreshold);
            
                // check wether it is inlier or outlier
            for (int c = 0; c < points.GetNumberOfPoints(); ++c) {
                if (meanDistances[c] <= distanceThreshold) {
                    getOutputPoints().InsertPoint(outputId, points.GetPoint(c));
                    ++outputId;
                }
            }
            
            NeptusLog.pub().info("Statistical outliers removal end: " + System.currentTimeMillis());
            NeptusLog.pub().info("Number of points on output: " + getOutputPoints().GetNumberOfPoints());
            
            float perc = (float) getOutputPoints().GetNumberOfPoints()/points.GetNumberOfPoints();
            NeptusLog.pub().info("Percentage of inlier points: " + perc);
            
            if (perc >= 0.80) {
                setOutputPoints(outputPoints);
                //return getOutputPoints();
            }

            else {
                NeptusLog.pub().info("Relax your parameters for radius outliers removal");
                setOutputPoints(points);
                //return points;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
//    public void applyFilter (vtkPoints points) {
//        try {
//            NeptusLog.pub().info("Statistical outliers removal start: " + System.currentTimeMillis());
//            
//            vtkKdTree kdTree = new vtkKdTree();
//            kdTree.BuildLocatorFromPoints(points);
//            //kdTree.PrintVerboseTree();     
//            
//            setOutputPoints(new vtkPoints());
//            
//            int outputId = 0;
//            
//            double[] meanDistances = new double[points.GetNumberOfPoints()];
//            double SumMeanDistances = 0.0;
//                        
//            meanK++; // because the indice 0 from N closests points is the query point
//            
//            if (points.GetNumberOfPoints() != 0) {
//                for (int i = 0; i < points.GetNumberOfPoints(); ++i) {
//                    vtkIdList idsFoundPts = new vtkIdList();
//
//                    kdTree.FindClosestNPoints(meanK , points.GetPoint(i), idsFoundPts);  // indice 0 is the query point
//
//                    if (i == 0)
//                    {
//                        double sum = 0.0;
//                        double sqSum = 0.0;
//                        
//                        for (int k = 1; k < idsFoundPts.GetNumberOfIds(); ++k) {
//                            double[] distances = new double[meanK];
//                            
//                            distances[k] = CalcUtils.distanceBetween2Points(points.GetPoint(i),
//                                    points.GetPoint(idsFoundPts.GetId(k)));
//                            // NeptusLog.pub().info("Neighbour " + k + ":" + points.GetPoint(idsFoundPts.GetId(k)));
//
////                            sumDistances += (float) CalcUtils.distanceBetween2Points(points.GetPoint(i),
////                                    points.GetPoint(idsFoundPts.GetId(k)));
//                            sum += distances[k];
//                            sqSum += distances[k] * distances[k];
//                            NeptusLog.pub().info("distance " + k + " :" + distances[k]);
//                        }
//                        //NeptusLog.pub().info("medium dist: " + sumDistances/meanK);
//                        //float meanQueryPointDist = sumDistances/(meanK-1);
//                        meanDistances[i] = sum/(meanK - 1);
//                        //allSumMeanDistances += meanQueryPointDist;
//                        SumMeanDistances += meanDistances[i];
//                        //NeptusLog.pub().info("Mean Query point distance: " + meanQueryPointDist);
//                        NeptusLog.pub().info("Mean Query point distance to neighbours: " + meanDistances[i]);
//                        
//                        double stddev = 0.0;
//                        stddev = CalcUtils.stddev(sum, sqSum, (meanK - 1));
//                        
//                        NeptusLog.pub().info("Standard Dev: " + stddev);
//                    }
//                }
//                double meanCloudDist = (SumMeanDistances/(points.GetNumberOfPoints()));
//                NeptusLog.pub().info("meanCloudDist: " + meanCloudDist);
//                
//            }
//            else {
//                NeptusLog.pub().error("Pointcloud is empty, no points to process");
//            }
//            
//            NeptusLog.pub().info("Statistical outliers removal end: " + System.currentTimeMillis());
//                       
//            NeptusLog.pub().info("Number of input points: " + points.GetNumberOfPoints());
//            NeptusLog.pub().info("Number of points on output: " + getOutputPoints().GetNumberOfPoints());
//            
//            float perc = (float) getOutputPoints().GetNumberOfPoints()/points.GetNumberOfPoints();
//            NeptusLog.pub().info("Percentage of inlier points: " + perc);
//            
//            if (perc >= 0.80) {
//                setOutputPoints(outputPoints);
//                //return getOutputPoints();
//            }
//
//            else {
//                NeptusLog.pub().info("Relax your parameters for Statistical outliers removal");
//                setOutputPoints(points);
//                //return points;
//            }
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

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
