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
package pt.lsts.neptus.plugins.vtk.pointcloud;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.lsts.neptus.plugins.vtk.utils.PointCloudUtils;
import vtk.vtkCellArray;
import vtk.vtkLODActor;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkShortArray;
import vtk.vtkVertexGlyphFilter;

/**
 * @author hfq
 * 
 */
public class PointCloud<T extends PointXYZ> {

    private String cloudName;
    private vtkPoints points;
    private vtkCellArray verts;
    private vtkPolyData poly;
    // private vtkLODActor cloudLODActor;
    private vtkLODActor cloudLODActor;
    private int numberOfPoints;
    private double[] bounds;
    private int memorySize;
    private PointCloudHandlers<PointXYZ> colorHandler;
    private boolean hasIntensities = false;
    // public vtkActor contourActor;
    private vtkShortArray intensities;

    /**
     * Create a pointcloud object
     */
    public PointCloud() {
        setPoints(new vtkPoints());
        // getPoints().SetDataTypeToFloat();
        setVerts(new vtkCellArray());
        setPoly(new vtkPolyData());
        setCloudLODActor(new vtkLODActor());
        setBounds(new double[6]);
        // setIntensities(new vtkShortArray());

        // setColorHandler(new PointCloudHandlers<>());
    }

    /**
     * Create a Pointcloud Actor from loaded points and verts
     * 
     */
    public void createLODActorFromPoints() {
        try {
            getPoints().Squeeze();
            getPoints().Modified();
            getPoly().Allocate(getNumberOfPoints(), getNumberOfPoints());
            getPoly().SetPoints(getPoints());

//            getVerts().Allocate(getNumberOfPoints(), getNumberOfPoints());
//            // for (int i = 0; i < getNumberOfPoints(); ++i) {
//            // getVerts().InsertNextCell(i);
//            // }
//
//            getVerts().SetNumberOfCells(getNumberOfPoints());
//
//            //getVerts().Modified();
//
//            for (int i = 0; i < getNumberOfPoints(); ++i) {
//                //getVerts().InsertNextCell(i);
//                //getVerts().InsertCellPoint(i);
//                getVerts().InsertNextCell(i);
//            }
//            getVerts().Squeeze();
//            getVerts().DebugOn();
//
//
//            getPoly().SetVerts(getVerts());
//            // getPoly().SetLines(getVerts());
//            getPoly().BuildCells();
//            getPoly().DebugOn();
//            getPoly().Squeeze();
//            getPoly().Modified();
//            getPoly().Update();
            
            vtkVertexGlyphFilter vertex = new vtkVertexGlyphFilter();
            vertex.AddInput(poly);
            vertex.Update();
            
            getPoly().ShallowCopy(vertex.GetOutput());
            getPoly().Update();

            // setBounds(getPoly().GetBounds()); <- subs by core dump crash
            setBounds(PointCloudUtils.computeBounds(getPoints()));

//            getColorHandler().generatePointCloudColorHandlers(getPoly(), bounds);
//
//            getPoly().GetPointData().SetScalars(getColorHandler().getColorsZ());

            vtkPolyDataMapper map = new vtkPolyDataMapper();
            map.SetInputConnection(getPoly().GetProducerPort());
            // map.SetInput(getPoly().);

            getCloudLODActor().SetMapper(map);
            getCloudLODActor().GetProperty().SetPointSize(1.0);
            getCloudLODActor().GetProperty().SetRepresentationToPoints();

            // setMemorySize(map.GetInput().GetActualMemorySize());
            setMemorySize(100);
                
            NeptusLog.pub().info("Number of points: " + getPoints().GetNumberOfPoints());
            NeptusLog.pub().info("Number: " + getPoints().GetDataType());
            NeptusLog.pub().info("Number of cells: " + getVerts().GetNumberOfCells());
            NeptusLog.pub().info("Print Points: " + getPoints().Print());
            NeptusLog.pub().info("Print Verts: " + getVerts().Print());
//            NeptusLog.pub().info("Print Map: " + map.Print());
            NeptusLog.pub().info("Print PolyData: " + getPoly().Print());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // /**
    // * Create a Pointcloud Actor from loaded points and verts
    // * @param intensities
    // */
    // public void createLODActorFromPoints(vtkShortArray intensities) {
    // try {
    // getPoly().SetPoints(getPoints());
    // //setIntensities(intensities);
    //
    // for (int i = 0; i < getNumberOfPoints(); ++i) {
    // getVerts().InsertNextCell(i);
    // }
    //
    // getPoly().SetVerts(getVerts());
    // getPoly().Modified();
    //
    // //getPoly().Update();
    // //setBounds(getPoly().GetBounds());
    // //setBounds(PointCloudUtils.computeBounds((PointCloud<PointXYZ>) this));
    // setBounds(PointCloudUtils.computeBounds(getPoints()));
    //
    // getColorHandler().generatePointCloudColorHandlers(getPoly(), bounds, getIntensities());
    //
    // getPoly().GetPointData().SetScalars(getColorHandler().getColorsZ());
    //
    // vtkPolyDataMapper map = new vtkPolyDataMapper();
    // map.SetInput(getPoly());
    //
    // getCloudLODActor().SetMapper(map);
    // getCloudLODActor().GetProperty().SetPointSize(1.0);
    // getCloudLODActor().GetProperty().SetRepresentationToPoints();
    //
    // setMemorySize(map.GetInput().GetActualMemorySize());
    // }
    // catch (Exception e) {
    // e.printStackTrace();
    // }
    // }

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

    /**
     * @return the hasIntensities
     */
    public boolean isHasIntensities() {
        return hasIntensities;
    }

    /**
     * @param hasIntensities the hasIntensities to set
     */
    public void setHasIntensities(boolean hasIntensities) {
        this.hasIntensities = hasIntensities;
    }

    /**
     * @return the intensities
     */
    public vtkShortArray getIntensities() {
        return intensities;
    }

    /**
     * @param intensities the intensities to set
     */
    public void setIntensities(vtkShortArray intensities) {
        this.intensities = intensities;
    }
}
