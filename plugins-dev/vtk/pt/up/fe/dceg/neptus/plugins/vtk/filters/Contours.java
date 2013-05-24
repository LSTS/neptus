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
 * May 17, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.filters;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.surface.PointCloudMesh;
import vtk.vtkActor;
import vtk.vtkAppendFilter;
import vtk.vtkCellArray;
import vtk.vtkContourFilter;
import vtk.vtkCutter;
import vtk.vtkDataArray;
import vtk.vtkMath;
import vtk.vtkPlane;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkStripper;

/**
 * @author hfq
 *
 */
public class Contours {
    
    private PointCloud<PointXYZ> pointCloud;
    private PointCloudMesh mesh;
    
    private double[] scalarRange;
    private vtkContourFilter contours;
    
    public vtkActor planeActor;
    
    
    private static int pointThreshold = 10;
    
    public Contours(PointCloud<PointXYZ> pointCloud) {
        this.pointCloud = pointCloud;
        
        scalarRange = new double[2];
        contours = new vtkContourFilter();
    }
    
    /**
     * @param mesh
     */
    public Contours(PointCloudMesh mesh) {
        this.mesh = mesh;
        
        scalarRange = new double[2];
        contours = new vtkContourFilter();
    }
    
    public void generateTerrainContours4() {
        vtkPolyDataMapper inputMapper = new vtkPolyDataMapper();
        inputMapper.SetInput(mesh.getPolyData());
        
        vtkPlane plane = new vtkPlane();
        plane.SetOrigin(mesh.getPolyData().GetCenter());
        //plane.SetNormal(1,1,1);
        plane.SetNormal(0.0, 0.0, -1.0);
        
        double[] bounds = mesh.getPolyData().GetBounds();
        
        double[] minBound = new double[3];
        minBound[0] = bounds[0];
        minBound[1] = bounds[2];
        minBound[2] = bounds[4];
        
        
        double[] maxBound = new double[3];
        maxBound[0] = bounds[1];
        maxBound[1] = bounds[3];
        maxBound[2] = bounds[5];
        
        double[] center = new double[3];
        center[0] = mesh.getPolyData().GetCenter()[0];
        center[0] = mesh.getPolyData().GetCenter()[1];
        center[0] = mesh.getPolyData().GetCenter()[2];
        
        vtkMath math = new vtkMath();
        double distanceMin = Math.sqrt(math.Distance2BetweenPoints(minBound, center));
        double distanceMax = Math.sqrt(math.Distance2BetweenPoints(maxBound, center));
        
        vtkCutter cutter = new vtkCutter();
        cutter.SetCutFunction(plane);
        cutter.SetInput(mesh.getPolyData());
        
        cutter.GenerateValues(10, bounds[4], bounds[5]);
        //cutter.GenerateValues(20, -distanceMin, distanceMax);
        NeptusLog.pub().info("number of contours: " + cutter.GetNumberOfContours());
        vtkPolyDataMapper cutterMapper = new vtkPolyDataMapper();
        cutterMapper.SetInputConnection(cutter.GetOutputPort());
        cutterMapper.ScalarVisibilityOff();
        //
        //NeptusLog.pub().info()
        
        planeActor = new vtkActor();
        planeActor.GetProperty().SetColor(1.0, 1,0);
        planeActor.GetProperty().SetLineWidth(2);
        planeActor.SetMapper(cutterMapper);
    }
    
    
    public void generateTerrainContours3() {
        //vtkPolyDataMapper inputMapper = new vtkPolyDataMapper();
        //inputMapper.SetInput(mesh.getMeshCloudLODActor().GetM)
        
        scalarRange = mesh.getPolyData().GetScalarRange();
        NeptusLog.pub().info("Range: " + scalarRange[0] + "; " + scalarRange[1]);
        
        //contours.SetInputConnection(mesh.getPolyData());
        NeptusLog.pub().info("number of points: " + mesh.getPolyData().GetNumberOfPoints());
        
        contours.SetInput(mesh.getPolyData());
        //contours.SetInputConnection(mesh.getMeshCloudLODActor().GetMapper().GetOutputPort());
        contours.SetValue(0, (scalarRange[1] + scalarRange[0]) / 2);       
        contours.GenerateValues(10, mesh.getPolyData().GetBounds()[4], mesh.getPolyData().GetBounds()[5]);
        //contours.ComputeGradientsOn();
        //contours.ComputeNormalsOn();
        //contours.ComputeScalarsOn();
        //contours.UseScalarTreeOn();
        contours.Update();
        NeptusLog.pub().info("Number of contours: " + contours.GetNumberOfContours());
        NeptusLog.pub().info(contours.GetInformation().toString());

        //NeptusLog.pub().info(contours.)
        
        
        vtkStripper contourStripper = new vtkStripper();
        contourStripper.SetInputConnection(contours.GetOutputPort());
        contourStripper.Update();
        
        NeptusLog.pub().info("Number of polys: " + contourStripper.GetOutput().GetNumberOfPolys());
        NeptusLog.pub().info("Memory size: " + contourStripper.GetOutput().GetActualMemorySize());
        NeptusLog.pub().info("Number of contour lines: " + contourStripper.GetOutput().GetNumberOfLines());
        NeptusLog.pub().info("Number of points: " + contourStripper.GetOutput().GetNumberOfPoints());
        NeptusLog.pub().info("Maximum Length: " + contourStripper.GetMaximumLength());
        NeptusLog.pub().info(contourStripper.GetInformation().toString());
        
        vtkPoints points = new vtkPoints();
        points = contourStripper.GetOutput().GetPoints();
        vtkCellArray cells = new vtkCellArray();
        cells = contourStripper.GetOutput().GetLines();
        vtkDataArray scalars = new vtkDataArray();
        scalars = contourStripper.GetOutput().GetPointData().GetScalars();
        
        
        NeptusLog.pub().info("Number of cells: " + cells.GetNumberOfCells());
        
        int linecount = 0;  
    }
    
    public void generateTerrainContours2 () {
        vtkPolyDataMapper inputMapper = new vtkPolyDataMapper();
        inputMapper.SetInput(pointCloud.getPoly());
        
        vtkPlane plane = new vtkPlane();
        plane.SetOrigin(pointCloud.getPoly().GetCenter());
        plane.SetNormal(1,1,1);
        
        double[] bounds = pointCloud.getBounds();
        
        double[] minBound = new double[3];
        minBound[0] = bounds[0];
        minBound[1] = bounds[2];
        minBound[2] = bounds[4];
        
        
        double[] maxBound = new double[3];
        maxBound[0] = bounds[1];
        maxBound[1] = bounds[3];
        maxBound[2] = bounds[5];
        
        double[] center = new double[3];
        center[0] = pointCloud.getPoly().GetCenter()[0];
        center[0] = pointCloud.getPoly().GetCenter()[1];
        center[0] = pointCloud.getPoly().GetCenter()[2];
        
        vtkMath math = new vtkMath();
        double distanceMin = Math.sqrt(math.Distance2BetweenPoints(minBound, center));
        double distanceMax = Math.sqrt(math.Distance2BetweenPoints(maxBound, center));
        
        vtkCutter cutter = new vtkCutter();
        cutter.SetCutFunction(plane);
        cutter.SetInput(pointCloud.getPoly());
        cutter.GenerateValues(20, -distanceMin, distanceMax);
        NeptusLog.pub().info("number of cut scalars: " + cutter.GetGenerateCutScalars());
        vtkPolyDataMapper cutterMapper = new vtkPolyDataMapper();
        cutterMapper.SetInputConnection(cutter.GetOutputPort());
        cutterMapper.ScalarVisibilityOff();
        
        planeActor = new vtkActor();
        planeActor.GetProperty().SetColor(1.0, 1,0);
        planeActor.GetProperty().SetLineWidth(3);
        planeActor.SetMapper(cutterMapper);
        
        
    }
    
    public void gerenerateTerrainContours () {
        scalarRange = pointCloud.getPoly().GetScalarRange();
             
        vtkAppendFilter appendFlter = new vtkAppendFilter();
        
        NeptusLog.pub().info("Range: " + scalarRange[0] + "; " + scalarRange[1]);
        double[] bounds = pointCloud.getBounds();
        
        contours.SetValue(0 , (bounds[4] + bounds[5]) / 2);        
        //contours.SetValue(0, (scalarRange[1] + scalarRange[0]) / 2);
        contours.SetNumberOfContours(10);

        
//        vtkPlaneSource plane = new vtkPlaneSource();

//        plane.SetXResolution((int) ((bounds[1] + bounds[0]) / 2));
//        plane.SetYResolution((int) ((bounds[3] + bounds[2]) / 2));
//        plane.Update();
        
        //contours.SetInput(pointCloud.getPoly());

        
        //contours.SetInputConnection(pointCloud.getPoly().
        
        contours.SetInputConnection(pointCloud.getPoly().GetProducerPort());
        
        //contours.SetInput(pointCloud.getPoly().Get);
        //contours.AddInput(pointCloud.getPoly().GetProducerPort());
        
        
        // Connect the segments of the contours into polylines
        vtkStripper contourStripper = new vtkStripper();
        contourStripper.SetInputConnection(contours.GetOutputPort());
        contourStripper.Update();
        
        int numberOfContourLines = contourStripper.GetOutput().GetNumberOfLines();
        NeptusLog.pub().info("Number of Contour lines: " + numberOfContourLines);
        
    }
}
