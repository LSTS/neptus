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
package pt.lsts.neptus.vtk.filters;

import pt.lsts.neptus.NeptusLog;
import vtk.vtkIdList;
import vtk.vtkKdTree;
import vtk.vtkPoints;

/**
 * @author hfq Removes point if it has less than a minimal number of points (defined by user) on its neighbourhood
 *         radius (search radius) Search is done with a kdTree
 * 
 */
public class RadiusOutlierRemoval {
    // if all multibeam points are parsed : searchradius = 1.0 , minPts = 30;

    // log fev 8 de Fev 1.0 - 4 // 3.0 10
    private double searchRadius = 1.0; // search radius on the same dimensional measurement as the coords (meters)
    private int minPtsNeighboursRadius = 4; // minimal number of neighbors on the search neighbourhood

    private vtkPoints outputPoints;

    public RadiusOutlierRemoval() {

    }

    public void applyFilter(vtkPoints points) {
        try {
            NeptusLog.pub().info("Radius outliers removal start: " + System.currentTimeMillis());

            vtkKdTree kdTree = new vtkKdTree();
            kdTree.BuildLocatorFromPoints(points);

            setOutputPoints(new vtkPoints());

            int outputId = 0;

            // outputPoints.Allocate(id0, id1)

            if (points.GetNumberOfPoints() != 0) {
                for (int i = 0; i < points.GetNumberOfPoints(); ++i) {
                    vtkIdList idsFoundPts = new vtkIdList();

                    kdTree.FindPointsWithinRadius(searchRadius, points.GetPoint(i), idsFoundPts);

                    if (idsFoundPts.GetNumberOfIds() > minPtsNeighboursRadius) { // inlier
                        getOutputPoints().InsertPoint(outputId, points.GetPoint(i));
                        ++outputId;
                    }
                    // else // outlier
                    // NeptusLog.pub().info("Number of points on neighbourhood: " + idsFoundPts.GetNumberOfIds() +
                    // " point id to remove: " + i + " point - x: " + points.GetPoint(i)[0] + " y: " +
                    // points.GetPoint(i)[1] + " z: " + points.GetPoint(i)[2]);
                }
            }
            else {
                NeptusLog.pub().error("Pointcloud is empty, no points to process!");

            }

            NeptusLog.pub().info("Radius outliers removal end: " + System.currentTimeMillis());

            NeptusLog.pub().info("Number of input points: " + points.GetNumberOfPoints());
            NeptusLog.pub().info("Number of points on output: " + getOutputPoints().GetNumberOfPoints());

            float perc = (float) getOutputPoints().GetNumberOfPoints() / points.GetNumberOfPoints();
            NeptusLog.pub().info("Percentage of inlier points: " + perc);

            if (perc >= 0.80) {
                setOutputPoints(outputPoints);
                // return getOutputPoints();
            }

            else {
                NeptusLog.pub().info("Relax your parameters for radius outliers removal");
                setOutputPoints(points);
                // return points;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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

}
