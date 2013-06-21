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
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.utils.CalcUtils;
import vtk.vtkKdTreePointLocator;
import vtk.vtkMath;
import vtk.vtkPolyData;

/**
 * @author hfq
 *
 */
public class RadiusOutlierRemoval {
    private double searchRadius = 10.0;
    private int minPtsNeighboursRadius = 2;
    
    public RadiusOutlierRemoval() {
        
    }
    
    public void applyFilter2(vtkPolyData polyData) {
        NeptusLog.pub().info("Radius outliers removal start: " + System.currentTimeMillis());
        
        vtkKdTreePointLocator kdTree = new vtkKdTreePointLocator();
        kdTree.SetDataSet(polyData);
        // kdTree.
        
        
        NeptusLog.pub().info("Radius outliers removal end: " + System.currentTimeMillis());       
    }
    
    public void applyFilter(vtkPolyData polyData) {
        
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
            //NeptusLog.pub().info("Point indice: " + i + " number neighbours: " + numNeighbourPts);
            
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
        
        for (int i = 0; i < remIndPts.size(); i++) {
          System.out.println("1");
          //polyData.RemoveCellReference(remIndPts.get(i));
          System.out.println("2");
          //polyData.RemoveDeletedCells();
          System.out.println("3");
          //polyData.Update();
          System.out.println("4");
          //polyData.UpdateData();
          System.out.println("5");
          //polyData.UpdateInformation();
          System.out.println("6");
        }
        
        // remove no compliant points from point cloud
//        for (Integer ind : remIndPts) {
//            System.out.println("1");
//            polyData.RemoveCellReference(ind);
//            System.out.println("2");
//            polyData.RemoveDeletedCells();
//            System.out.println("3");
//            polyData.Update();
//            System.out.println("4");
//            polyData.UpdateData();
//            System.out.println("5");
//            polyData.UpdateInformation();
//            System.out.println("6");
//        }
        
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
    private int getMinPtsNeighboursRadius() {
        return minPtsNeighboursRadius;
    }

    /**
     * @param minPtsNeighboursRadius the minPtsNeighboursRadius to set
     */
    private void setMinPtsNeighboursRadius(int minPtsNeighboursRadius) {
        this.minPtsNeighboursRadius = minPtsNeighboursRadius;
    }

    
}
