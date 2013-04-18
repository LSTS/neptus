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

import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import ucar.nc2.dt.PointObsDatatype;
import vtk.vtkActor;
import vtk.vtkAlgorithm;
import vtk.vtkCellArray;
import vtk.vtkGlyph3D;
import vtk.vtkLODActor;
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
    public vtkLODActor cloud;
    private int numberOfPoints;
    
    private String cloudName;
    
    public PointCloud () {
        points = new vtkPoints();
        points.SetDataTypeToFloat();
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
    
    public vtkLODActor getRandomPointCloud(int nPoints) {
        cloud = new vtkLODActor();
        setNumberOfPoints(nPoints);
        points = new vtkPoints();
        points.Allocate(getNumberOfPoints(), 0);
        verts = new vtkCellArray();
        verts.Allocate(verts.EstimateSize(1, getNumberOfPoints()), 0);
        
        Random pos = new Random();
        
        for (int i = 0; i < getNumberOfPoints(); i++) {
            PointXYZ p = new PointXYZ(pos.nextFloat()*10, pos.nextFloat()*10, pos.nextFloat()*10);
            //System.out.println("X: " + p.getX() + " Y: " + p.getY() + " Z: " + p.getZ());
            verts.InsertCellPoint(points.InsertNextPoint(p.getX(), p.getY(), p.getZ()));            
        }
        
        //vtkPolyData pol = new vtkPolyData();
        poly = new vtkPolyData();
        poly.SetPoints(points);
        poly.SetVerts(verts);
        
        //vtkGlyph3D glyph = new vtkGlyph3D();
        //glyph.SetInput(pol);
        vtkVertexGlyphFilter glyph = new vtkVertexGlyphFilter();
        glyph.SetInput(poly);
        
        
        
        vtkPolyDataMapper map = new vtkPolyDataMapper();
        map.SetInputConnection(glyph.GetOutputPort());
        //map.SetInput(pol);
        
        cloud.SetMapper(map);
        cloud.GetProperty().SetPointSize(1.0);
        
        return cloud;
    }
    
    public vtkLODActor getRamdonPointCloudFromVtkPointSource(int nPoints, double radius) {
        cloud = new vtkLODActor();
        setNumberOfPoints(nPoints);
        
        vtkPointSource pointSource = new vtkPointSource();
        pointSource.SetNumberOfPoints(getNumberOfPoints());
        pointSource.SetCenter(0.0, 0.0, 0.0);
        pointSource.SetDistributionToUniform();
        pointSource.SetRadius(radius);

        vtkPolyDataMapper mapper = new vtkPolyDataMapper();
        mapper.AddInputConnection(pointSource.GetOutputPort());
        
        cloud = new vtkLODActor();
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
    
}
