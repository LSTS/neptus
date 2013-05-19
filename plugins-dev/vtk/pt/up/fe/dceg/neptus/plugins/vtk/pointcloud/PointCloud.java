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

import java.util.Random;

import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.PointCloudHandlers;
import vtk.vtkCellArray;
import vtk.vtkDataArray;
import vtk.vtkLODActor;
import vtk.vtkLookupTable;
import vtk.vtkPointSource;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
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
    private vtkLODActor cloudLODActor;
    private int numberOfPoints;
    private double[] bounds;
    private int memorySize;
    private PointCloudHandlers<PointXYZ> colorHandler;
        //public vtkActor contourActor;
    
    /**
     * 
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
     * Create a Pointcloud Actor from vtkPoints & vtkCellArray
     */
    public void createLODActorFromPoints() {
        getPoly().SetPoints(getPoints());
        getPoly().SetVerts(getVerts());
        
        getPoly().Modified();
               
        vtkCellArray cells = new vtkCellArray();
        cells.SetNumberOfCells(getNumberOfPoints());
        getPoly().SetPolys(cells);
        
        setBounds(getPoly().GetBounds());
        setMemorySize(getPoly().GetActualMemorySize());
             
        vtkLookupTable colorLookupTable = new vtkLookupTable();
        //colorLookupTable.SetNumberOfColors(3);
        colorLookupTable.SetRange(getBounds()[4], getBounds()[5]);     
        //colorLookupTable.SetValueRange(getBounds()[4], getBounds()[5]);        
        //colorLookupTable.SetHueRange(0, 1);
        //colorLookupTable.SetSaturationRange(1, 1);
        //colorLookupTable.SetValueRange(1, 1);
        //colorLookupTable.SetTableRange(getBounds()[4], getBounds()[5]);
        colorLookupTable.SetScaleToLinear();
        colorLookupTable.Build();

        getColorHandler().setPointCloudColorHandlers(getNumberOfPoints(), getPoly(), colorLookupTable, bounds);
        
        getPoly().GetPointData().SetScalars(getColorHandler().getColorsZ());

        
        vtkPolyDataMapper map = new vtkPolyDataMapper();
        map.SetInput(getPoly());
            // into coloca os limites na lookuptable
        map.SetScalarRange(getBounds()[4], getBounds()[5]);
        map.SetLookupTable(getColorHandler().getLutZ());
        //map.ScalarVisibilityOn();
        //map.SetScalarModeToUsePointData();
        //map.SetScalarModeToUseCellData();
        //map.SetScalarVisibility_9(true);
            // poor renderering?
        //map.InterpolateScalarsBeforeMappingOn();
        //map.SetColorModeToMapScalars();
     
        getCloudLODActor().SetMapper(map);
        getCloudLODActor().GetProperty().SetPointSize(1.0);
        getCloudLODActor().GetProperty().SetRepresentationToPoints();     
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
    
    
    /**
     * data information transformed to graphical information through cells
     * @param nPoints
     * @return vtkLODActor
     */
    public vtkLODActor getRandomPointCloud(int nPoints) {
        setNumberOfPoints(nPoints);
        setPoints(new vtkPoints());
        getPoints().Allocate(getNumberOfPoints(), 0);
        setVerts(new vtkCellArray());
        getVerts().Allocate(getVerts().EstimateSize(1, getNumberOfPoints()), 0);
        
        Random pos = new Random();
        
        for (int i = 0; i < getNumberOfPoints(); i++) {
            PointXYZ p = new PointXYZ(pos.nextFloat()*10, pos.nextFloat()*10, pos.nextFloat()*10);
            getVerts().InsertNextCell(1);
            getVerts().InsertCellPoint(getPoints().InsertNextPoint(p.getX(), p.getY(), p.getZ()));            
        }
        
        setPoly(new vtkPolyData());
        getPoly().SetPoints(getPoints());
        getPoly().SetVerts(getVerts());
        
        getPoly().Modified();
        
        //vtkDataArray scalars = new vtkDataArray();
        //PointCloudHandlers<PointXYZ> colorHandler = new PointCloudHandlers<>();
        //scalars = colorHandler.getRandomColor2();
        
        //poly.GetPointData().SetScalars(id0)
        
        vtkCellArray cells = new vtkCellArray();
        cells.SetNumberOfCells(nPoints);
        //cells.
        
        //vtkGlyph3D glyph = new vtkGlyph3D();
        //glyph.SetInput(poly);
        //vtkVertexGlyphFilter glyph = new vtkVertexGlyphFilter();
        //glyph.SetInput(poly);
         
        //vtkLookupTable lut = new vtkLookupTable();
        //lut.SetNumberOfColors(256);
        //lut.SetHueRange(0.0, 0.667);
        //lut.SetRampToLinear();
        
        getPoly().SetPolys(cells);
        
        vtkPolyDataMapper map = new vtkPolyDataMapper();
        //map.SetInputConnection(glyph.GetOutputPort());
        map.SetInput(getPoly());
        //map.SetLookupTable(lut);
        //map.SetScalarRange(0.0, 0.7);
        //map.SetInput(pol);

        
        getCloudLODActor().SetMapper(map);
        getCloudLODActor().GetProperty().SetPointSize(2.0);
        getCloudLODActor().GetProperty().SetRepresentationToPoints();
        
        return getCloudLODActor();
    }
    
    /**
     * data information transformed to graphical information through Vertex Glyph
     * @param nPoints
     * @return vtkLODActor
     */ 
    public vtkLODActor getRandomPointCloud2(int nPoints) {
        setNumberOfPoints(nPoints);
        setPoints(new vtkPoints());
        getPoints().Allocate(getNumberOfPoints(), 0);
        
        Random pos = new Random();
        
        for (int i = 0; i < getNumberOfPoints(); i++) {
            PointXYZ p = new PointXYZ(pos.nextFloat()*10, pos.nextFloat()*10, pos.nextFloat()*10);
            //verts.InsertNextCell(1);
            getPoints().InsertPoint(i, p.getX(), p.getY(), p.getZ());
            //points.InsertNextPoint(p.getX(), p.getY(), p.getZ());
            //verts.InsertCellPoint(i);
            //verts.InsertCellPoint(points.InsertNextPoint(p.getX(), p.getY(), p.getZ()));            
        }        
        setPoly(new vtkPolyData());
        getPoly().SetPoints(getPoints());
        
        getPoly().Modified();
        
        vtkDataArray scalars = new vtkDataArray();
        
        //vtkGlyph3D glyph = new vtkGlyph3D();
        //glyph.SetInput(poly);
        vtkVertexGlyphFilter glyph = new vtkVertexGlyphFilter();
        glyph.SetInput(getPoly());
         
        vtkLookupTable lut = new vtkLookupTable();
        lut.SetNumberOfColors(256);
        lut.SetHueRange(0.0, 0.667);
        lut.SetScaleToLinear();
        //lut.SetRampToLinear();
        
        vtkPolyDataMapper map = new vtkPolyDataMapper();
        map.SetInputConnection(glyph.GetOutputPort());
        //map.SetInput(poly);
        map.SetLookupTable(lut);
        //map.SetScalarRange(0.0, 0.7);
        //map.SetInput(pol);
   
        getCloudLODActor().SetMapper(map);
        getCloudLODActor().GetProperty().SetPointSize(2.0);
        getCloudLODActor().GetProperty().SetRepresentationToPoints();
        
        
        return getCloudLODActor();
    }
    
    
    public vtkLODActor getRamdonPointCloudFromVtkPointSource(int nPoints, double radius) {
        setNumberOfPoints(nPoints);
        
        vtkPointSource pointSource = new vtkPointSource();
        pointSource.SetNumberOfPoints(getNumberOfPoints());
        pointSource.SetCenter(0.0, 0.0, 0.0);
        pointSource.SetDistributionToUniform();
        pointSource.SetRadius(radius);

        vtkPolyDataMapper mapper = new vtkPolyDataMapper();
        mapper.AddInputConnection(pointSource.GetOutputPort());
        
        getCloudLODActor().SetMapper(mapper);
        
        return getCloudLODActor();
    }
}
