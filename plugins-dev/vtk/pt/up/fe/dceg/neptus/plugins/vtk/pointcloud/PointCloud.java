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

import java.awt.Point;
import java.util.Random;

import com.jogamp.graph.curve.OutlineShape.VerticesState;
import com.kitfox.svg.pathcmd.Vertical;

import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.PointCloudHandlers;
import ucar.nc2.dt.PointObsDatatype;
import vtk.vtkActor;
import vtk.vtkAlgorithm;
import vtk.vtkCellArray;
import vtk.vtkDataArray;
import vtk.vtkFloatArray;
import vtk.vtkGlyph3D;
import vtk.vtkIdTypeArray;
import vtk.vtkLODActor;
import vtk.vtkLookupTable;
import vtk.vtkPointSource;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkVTKJavaCommonDriver;
import vtk.vtkVertexGlyphFilter;



/**
 * @author hfq
 *
 */
public class PointCloud<T extends PointXYZ> {
    
    public vtkPoints points;
    public vtkCellArray verts;
    public vtkPolyData poly;
    public vtkLODActor cloud = new vtkLODActor();
    private int numberOfPoints;
    
    public vtkActor contourActor;
    
    private String cloudName;
    
    public PointCloud () {
        points = new vtkPoints();
        points.SetDataTypeToFloat();
    }
    
    
    /**
     * data information transformed to graphical information through cells
     * @param nPoints
     * @return
     */
    public vtkLODActor getRandomPointCloud(int nPoints) {
        setNumberOfPoints(nPoints);
        points = new vtkPoints();
        points.Allocate(getNumberOfPoints(), 0);
        verts = new vtkCellArray();
        verts.Allocate(verts.EstimateSize(1, getNumberOfPoints()), 0);
        
        Random pos = new Random();
        
        for (int i = 0; i < getNumberOfPoints(); i++) {
            PointXYZ p = new PointXYZ(pos.nextFloat()*10, pos.nextFloat()*10, pos.nextFloat()*10);
            //System.out.println("X: " + p.getX() + " Y: " + p.getY() + " Z: " + p.getZ());
            verts.InsertNextCell(1);
            verts.InsertCellPoint(points.InsertNextPoint(p.getX(), p.getY(), p.getZ()));            
        }
        
        //vtkPolyData pol = new vtkPolyData();
        poly = new vtkPolyData();
        poly.SetPoints(points);
        poly.SetVerts(verts);
        
        poly.Modified();
        
        vtkDataArray scalars = new vtkDataArray();
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
        
        poly.SetPolys(cells);
        
        vtkPolyDataMapper map = new vtkPolyDataMapper();
        //map.SetInputConnection(glyph.GetOutputPort());
        map.SetInput(poly);
        //map.SetLookupTable(lut);
        //map.SetScalarRange(0.0, 0.7);
        //map.SetInput(pol);
   
        cloud.SetMapper(map);
        cloud.GetProperty().SetPointSize(2.0);
        cloud.GetProperty().SetRepresentationToPoints();
        
        return cloud;
    }
    
    /**
     * data information transformed to graphical information through Vertex Glyph
     * @param nPoints
     * @return
     */ 
    public vtkLODActor getRandomPointCloud2(int nPoints) {
        setNumberOfPoints(nPoints);
        points = new vtkPoints();
        points.Allocate(getNumberOfPoints(), 0);
        
        Random pos = new Random();
        
        for (int i = 0; i < getNumberOfPoints(); i++) {
            PointXYZ p = new PointXYZ(pos.nextFloat()*10, pos.nextFloat()*10, pos.nextFloat()*10);
            //verts.InsertNextCell(1);
            points.InsertPoint(i, p.getX(), p.getY(), p.getZ());
            //points.InsertNextPoint(p.getX(), p.getY(), p.getZ());
            //verts.InsertCellPoint(i);
            //verts.InsertCellPoint(points.InsertNextPoint(p.getX(), p.getY(), p.getZ()));            
        }        
        poly = new vtkPolyData();
        poly.SetPoints(points);
        
        poly.Modified();
        
        vtkDataArray scalars = new vtkDataArray();
        
        //vtkGlyph3D glyph = new vtkGlyph3D();
        //glyph.SetInput(poly);
        vtkVertexGlyphFilter glyph = new vtkVertexGlyphFilter();
        glyph.SetInput(poly);
         
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
   
        cloud.SetMapper(map);
        cloud.GetProperty().SetPointSize(2.0);
        cloud.GetProperty().SetRepresentationToPoints();
        
        
        return cloud;
    }
    /**
     * usuless
     */
    public void convertPointCloudToVTKPolyData () {
        //vtkCellArray vertices = new vtkCellArray(); 
        //if (!poly) {
        //    poly.SetVerts(vertices);
        //}
        
        verts = poly.GetVerts();
        
        points = poly.GetPoints();
        points.SetNumberOfPoints(getNumberOfPoints());
        
        vtkFloatArray data = (vtkFloatArray) points.GetData();
        
        for (int i = 0; i < getNumberOfPoints(); i++)
        {
            
        }
        
        vtkIdTypeArray cells = verts.GetData();
        
        vtkIdTypeArray initcells = new vtkIdTypeArray();
        //updateCells (cells, )
        
        verts.SetCells(numberOfPoints, cells);
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
        
        cloud.SetMapper(mapper);
        
        return cloud;
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
}
