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

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.plugins.vtk.utils.CalcUtils;
import vtk.vtkIdList;
import vtk.vtkKdTree;
import vtk.vtkPoints;
import vtk.vtkPolyData;

/**
 * @author hfq
 * Removes point if it has less than a minimal number of points (defined by user) on its neighbourhood radius (search radius)
 * Search is done with a kdTree
 *
 */
public class RadiusOutlierRemoval {
        // if all multibeam points are parsed : searchradius = 1.0 , minPts = 30;
    
    private double searchRadius = 2.0;     // search radius on the same dimensional measurement as the coords (meters)
    private int minPtsNeighboursRadius = 4; // minimal number of neighbors on the search neighbourhood
    
    public RadiusOutlierRemoval() {
        
    }
    
    public vtkPoints applyFilter (vtkPoints points) {        
        try {
            NeptusLog.pub().info("Radius outliers removal start: " + System.currentTimeMillis());
            
            vtkKdTree kdTree = new vtkKdTree();
            kdTree.BuildLocatorFromPoints(points);
            
            vtkPoints outputPoints = new vtkPoints();
            
            int outputId = 0;
            
            if (points.GetNumberOfPoints() != 0) {
                for (int i = 0; i < points.GetNumberOfPoints(); ++i) {
                    vtkIdList idsFoundPts = new vtkIdList();
                    
                    kdTree.FindPointsWithinRadius(searchRadius, points.GetPoint(i), idsFoundPts);
                    
                    if (idsFoundPts.GetNumberOfIds() < minPtsNeighboursRadius) {    // defined has an outlier
                        // NeptusLog.pub().info("Number of points on neighbourhood: " + idsFoundPts.GetNumberOfIds() + " point id to remove: " + i + " point - x: " + points.GetPoint(i)[0] + " y: " + points.GetPoint(i)[1] + " z: " + points.GetPoint(i)[2]);
                    }
                    else {
                        outputPoints.InsertPoint(outputId, points.GetPoint(i));
                        ++outputId;
                    }
                }
            }
            else {
                NeptusLog.pub().error("Pointcloud is empty, no points to process!");
                return points;
            }
            
            NeptusLog.pub().info("Radius outliers removal end: " + System.currentTimeMillis());
            
            NeptusLog.pub().info("Number of input points: " + points.GetNumberOfPoints());
            NeptusLog.pub().info("Number of points on output: " + outputPoints.GetNumberOfPoints());
            
            float perc = (float) outputPoints.GetNumberOfPoints()/points.GetNumberOfPoints();
            NeptusLog.pub().info("Percentage of points removed: " + perc);
            
            if (perc >= 0.80)
                return outputPoints;
            else {
                NeptusLog.pub().info("Relax your parameters for radius outliers removal");
                return points;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return points;
        }
    }
    
    public void applyFilter3(vtkPolyData polyData) {
        
        NeptusLog.pub().info("Radius outliers removal start: " + System.currentTimeMillis());
        
        //vtkMath math = new vtkMath();        
        List<Integer>  remIndPts = new ArrayList<Integer>();
        
        // NeptusLog.pub().info("Number of points: " + polyData.GetNumberOfPoints());
        
        for (int i = 0; i < polyData.GetNumberOfPoints(); ++i) {    // iterate the qu
            double[] p = new double[3];
            p = polyData.GetPoint(i);
            
            int numNeighbourPts = 0;
            
            for (int k = 0; k < polyData.GetNumberOfPoints(); ++k) {
                if (numNeighbourPts == minPtsNeighboursRadius)
                    break;
                
                if (i != k) {
                    double[] pK = new double[3];
                    pK = polyData.GetPoint(k);
                    double dist = CalcUtils.distanceBetween2Points(p, pK);
                    
                    // NeptusLog.pub().info("Distance between points: " + dist);
                    
                    if (dist <= searchRadius) {
                        ++numNeighbourPts;
                    }
                }
            }         
            if (numNeighbourPts < minPtsNeighboursRadius)
            {
                NeptusLog.pub().info("Point indice: " + i + " x: " + p[0] + " y: " + p[1] + " z: " + p[2] + " is to be removed");
                remIndPts.add(i);
            }
            if (i == 200)
                break;
        }
        
        NeptusLog.pub().info("Number of going to be removed points: " + remIndPts.size());
        
        NeptusLog.pub().info("Number of points: " + polyData.GetNumberOfPoints());
        NeptusLog.pub().info("Number of cells: " + polyData.GetNumberOfCells());
        

        NeptusLog.pub().info("size of rem array: " + remIndPts.size());
        
        // remove no compliant points from point cloud
        for (int i = 0; i < remIndPts.size(); i++) {
          //polyData.RemoveCellReference(remIndPts.get(i));
          //polyData.RemoveDeletedCells();
          //polyData.Update();
          //polyData.UpdateInformation();
        }
        
        NeptusLog.pub().info("Number of points 2: " + polyData.GetNumberOfPoints());
        NeptusLog.pub().info("Number of cells 2: " + polyData.GetNumberOfCells());
        
        NeptusLog.pub().info("Radius outliers removal end: " + System.currentTimeMillis());
    }
    
    
    /**
     * @return the searchRadius
     */
    public double getSearchRadius() {
        return searchRadius;
    }
    /**
     * @param searchRadius the searchRadius to set
     */
    public void setSearchRadius(double searchRadius) {
        this.searchRadius = searchRadius;
    }

    /**
     * @return the minPtsNeighboursRadius
     */
    public int getMinPtsNeighboursRadius() {
        return minPtsNeighboursRadius;
    }

    /**
     * @param minPtsNeighboursRadius the minPtsNeighboursRadius to set
     */
    public void setMinPtsNeighboursRadius(int minPtsNeighboursRadius) {
        this.minPtsNeighboursRadius = minPtsNeighboursRadius;
    }

    
}
