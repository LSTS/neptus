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
 * Apr 11, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.pointcloud;

import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import vtk.vtkCellArray;
import vtk.vtkLODActor;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;

/**
 * @author hfq
 *
 */
public class PointCloud<T extends PointXYZ> {
    
    private String cloudName;
    private vtkPoints points;
    private vtkCellArray verts;
    private vtkPolyData poly;
    private vtkLODActor cloudLODActor;
    private int numberOfPoints;
    private double[] bounds;
    private int memorySize;
    private PointCloudHandlers<PointXYZ> colorHandler;
        //public vtkActor contourActor;
    
    /**
     * Create a pointcloud object
     */
    public PointCloud () {
        setPoints(new vtkPoints());
        getPoints().SetDataTypeToFloat();
        setVerts(new vtkCellArray());
        setPoly(new vtkPolyData());
        setCloudLODActor(new vtkLODActor());
        setBounds(new double[6]);
        setColorHandler(new PointCloudHandlers<>());
    }
    
    /**
     * Create a Pointcloud Actor from loaded points and verts
     */
    public void createLODActorFromPoints() {
        try {
            getPoly().SetPoints(getPoints());
            getPoly().SetVerts(getVerts());
            
            getPoly().Modified();
                   
            vtkCellArray cells = new vtkCellArray();
            cells.SetNumberOfCells(getNumberOfPoints());
            getPoly().SetPolys(cells);
            
            setBounds(getPoly().GetBounds());
            setMemorySize(getPoly().GetActualMemorySize());
            
            getColorHandler().generatePointCloudColorHandlers(getPoly(), bounds);
            
            getPoly().GetPointData().SetScalars(getColorHandler().getColorsZ());
            
            vtkPolyDataMapper map = new vtkPolyDataMapper();
            map.SetInput(getPoly());
    
            getCloudLODActor().SetMapper(map);
            getCloudLODActor().GetProperty().SetPointSize(1.0);
            getCloudLODActor().GetProperty().SetRepresentationToPoints();
        }
        catch (Exception e) {
            e.printStackTrace();
        }     
    }
    
    /**
     * @return the cloudName
     */
    public String getCloudName() {
        return cloudName;
    }

    /**
     * @param cloudName the cloudName to set
     */
    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }
    
    /**
     * @return the numberOfPoints
     */
    public int getNumberOfPoints() {
        return numberOfPoints;
    }

    /**
     * @param numberOfPoints the numberOfPoints to set
     */
    public void setNumberOfPoints(int numberOfPoints) {
        this.numberOfPoints = numberOfPoints;
    }

    /**
     * @return the cloudLODActor
     */
    public vtkLODActor getCloudLODActor() {
        return cloudLODActor;
    }

    /**
     * @param cloudLODActor the cloudLODActor to set
     */
    public void setCloudLODActor(vtkLODActor cloudLODActor) {
        this.cloudLODActor = cloudLODActor;
    }

    /**
     * @return the points
     */
    public vtkPoints getPoints() {
        return points;
    }

    /**
     * @param points the points to set
     */
    public void setPoints(vtkPoints points) {
        this.points = points;
    }

    /**
     * @return the verts
     */
    public vtkCellArray getVerts() {
        return verts;
    }

    /**
     * @param verts the verts to set
     */
    public void setVerts(vtkCellArray verts) {
        this.verts = verts;
    }

    /**
     * @return the poly
     */
    public vtkPolyData getPoly() {
        return poly;
    }

    /**
     * @param poly the poly to set
     */
    public void setPoly(vtkPolyData poly) {
        this.poly = poly;
    }

    /**
     * @return the bounds
     */
    public double[] getBounds() {
        return bounds;
    }

    /**
     * @param bounds the bounds to set
     */
    public void setBounds(double[] bounds) {
        this.bounds = bounds;
    }

    /**
     * @return the memorySize
     */
    public int getMemorySize() {
        return memorySize;
    }

    /**
     * @param memorySize the memorySize to set
     */
    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    /**
     * @return the colorHandler
     */
    public PointCloudHandlers<PointXYZ> getColorHandler() {
        return colorHandler;
    }

    /**
     * @param colorHandler the colorHandler to set
     */
    public void setColorHandler(PointCloudHandlers<PointXYZ> colorHandler) {
        this.colorHandler = colorHandler;
    }
}
