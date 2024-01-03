/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * May 17, 2013
 */
package pt.lsts.neptus.vtk.filters;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.vtk.surface.PointCloudMesh;
import vtk.vtkActor;
import vtk.vtkCellArray;
import vtk.vtkContourFilter;
import vtk.vtkCutter;
import vtk.vtkDoubleArray;
import vtk.vtkPlane;
import vtk.vtkPolyDataMapper;
import vtk.vtkStripper;

/**
 * @author hfq
 *
 */
public class Contours {

    private final PointCloudMesh mesh;

    private double[] scalarRange;
    private final vtkContourFilter contoursFilter;

    public vtkActor planeActor;

    private vtkActor isolinesActor;

    // private static int pointThreshold = 10;

    /**
     * FIXME
     * Doesn't work properly
     */
    public Contours(PointCloudMesh mesh) {
        this.mesh = mesh;
        this.scalarRange = new double[2];
        this.contoursFilter = new vtkContourFilter();
    }

    /**
     * 
     * @param mesh
     */
    public void generateTerrainContours() {
        //vtkPolyDataMapper inputMapper = new vtkPolyDataMapper();
        //inputMapper.SetInput(mesh.getMeshCloudLODActor().GetM)

        scalarRange = mesh.getPolyData().GetScalarRange();
        NeptusLog.pub().info("Range: " + scalarRange[0] + "; " + scalarRange[1]);

        //contours.SetInputConnection(mesh.getPolyData());
        NeptusLog.pub().info("number of points: " + mesh.getPolyData().GetNumberOfPoints());
        //NeptusLog.pub().info("Bounds: minZ: " + mesh.getPolyData().GetBounds()[4] + " max Z: " + mesh.getPolyData().GetBounds()[5]);

        contoursFilter.SetInput(mesh.getPolyData());
        //contours.SetInputConnection(mesh.getMeshCloudLODActor().GetMapper().GetOutputPort());
        //contours.SetValue(0, (scalarRange[1] + scalarRange[0]) / 2);
        //contours.SetValue(0, (mesh.getPolyData().GetBounds()[4] + mesh.getPolyData().GetBounds()[5])/2);
        //contours.GenerateValues(10, mesh.getPolyData().GetBounds()[4], mesh.getPolyData().GetBounds()[5]);
        contoursFilter.GenerateValues(10, scalarRange[0], scalarRange[1]);
        //contours.ComputeGradientsOn();
        //contours.ComputeNormalsOn();
        //contours.ComputeScalarsOn();
        //contours.UseScalarTreeOn();
        contoursFilter.Update();
        NeptusLog.pub().info("Number of contours: " + contoursFilter.GetNumberOfContours());
        NeptusLog.pub().info(contoursFilter.GetInformation().toString());

        //NeptusLog.pub().info(contours.)


        vtkStripper contourStripper = new vtkStripper();
        contourStripper.SetInputConnection(contoursFilter.GetOutputPort());
        contourStripper.Update();

        NeptusLog.pub().info("Number of polys: " + contourStripper.GetOutput().GetNumberOfPolys());
        NeptusLog.pub().info("Memory size: " + contourStripper.GetOutput().GetActualMemorySize());
        NeptusLog.pub().info("Number of contour lines: " + contourStripper.GetOutput().GetNumberOfLines());
        NeptusLog.pub().info("Number of points: " + contourStripper.GetOutput().GetNumberOfPoints());
        NeptusLog.pub().info("Maximum Length: " + contourStripper.GetMaximumLength());
        NeptusLog.pub().info(contourStripper.GetInformation().toString());

        //vtkPoints points = new vtkPoints();
        //points = contourStripper.GetOutput().GetPoints();
        vtkCellArray cells = new vtkCellArray();
        cells = contourStripper.GetOutput().GetLines();
        //vtkDataArray scalars = new vtkDataArray();
        //scalars = contourStripper.GetOutput().GetPointData().GetScalars();

        NeptusLog.pub().info("Number of cells: " + cells.GetNumberOfCells());

        //vtkPolyData labelPolyData = new vtkPolyData();
        //vtkPoints labelPoints = new vtkPoints();
        vtkDoubleArray labelScalars = new vtkDoubleArray();

        labelScalars.SetNumberOfComponents(1);
        labelScalars.SetName("Isovalues");

        //        vtkIdList indices = new vtkIdList();
        //        int numberOfPoints;
        //        int lineCount = 0;

        //        for (cells.InitTraversal(); cells.GetCell(numberOfPoints ,indices); ++lineCount) {
        //            //cells.get
        //        }
        //        for (iterable_type iterable_element : iterable) {
        //
        //        }

        vtkPolyDataMapper contourMapper = new vtkPolyDataMapper();
        contourMapper.SetInputConnection(contourStripper.GetOutputPort());
        contourMapper.ScalarVisibilityOff();

        setIsolinesActor(new vtkActor());
        getIsolinesActor().SetMapper(contourMapper);
    }

    public void generateTerrainContoursThroughCutter() {
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

        // double distanceMin = Math.sqrt(math.Distance2BetweenPoints(minBound, center));
        // double distanceMax = Math.sqrt(math.Distance2BetweenPoints(maxBound, center));

        vtkCutter cutter = new vtkCutter();
        cutter.SetCutFunction(plane);
        cutter.SetInput(mesh.getPolyData());

        cutter.GenerateValues(10, bounds[4], bounds[5]);
        //cutter.GenerateValues(20, -distanceMin, distanceMax);
        NeptusLog.pub().info("number of contours: " + cutter.GetNumberOfContours());
        vtkPolyDataMapper cutterMapper = new vtkPolyDataMapper();
        cutterMapper.SetInputConnection(cutter.GetOutputPort());
        cutterMapper.ScalarVisibilityOff();
        NeptusLog.pub().info("number of pieces on mapper: " + cutterMapper.GetNumberOfPieces());
        NeptusLog.pub().info("Number of clipping planes: " + cutterMapper.GetClippingPlanes().GetNumberOfItems());

        planeActor = new vtkActor();
        planeActor.GetProperty().SetColor(1.0, 1,0);
        planeActor.GetProperty().SetLineWidth(2);
        planeActor.SetMapper(cutterMapper);
    }

    /**
     * @return the isolinesActor
     */
    public vtkActor getIsolinesActor() {
        return isolinesActor;
    }

    /**
     * @param isolinesActor the isolinesActor to set
     */
    public void setIsolinesActor(vtkActor isolinesActor) {
        this.isolinesActor = isolinesActor;
    }
}
