/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Apr 4, 2013
 */
package pt.lsts.neptus.vtk.visualization;

import vtk.vtkAxes;
import vtk.vtkFloatArray;
import vtk.vtkLODActor;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkTubeFilter;

/**
 * @author hfq
 * 
 */
public class Axes {
    private static final double axesScale = 1.0;
    // private static final int viewport = 0;
    private vtkAxes axes;
    private vtkLODActor axesActor;

    /**
     * Coordinate System is placed by default on (0,0,0), and scale factor is 1.0;
     * 
     */
    public Axes() {
        addCoordinateSystem();
    }

    /**
     * Can be given a specific scale factor to the axes
     * 
     * @param scale
     * @param viewport
     */
    public Axes(double scale, int viewport) {
        addCoordinateSystem(scale, viewport);
    }

    /**
     * Coordinate System Axes origin can be placed in a specific position in the 3D world, and given a scale factor
     * 
     * @param scale
     * @param x
     * @param y
     * @param z
     * @param viewport
     */
    public Axes(double scale, float x, float y, float z, int viewport) {
        addCoordinateSystem(scale, x, y, z, viewport);
    }

    /**
     * 
     * @param scale
     * @param x
     * @param y
     * @param z
     * @param viewport
     */
    public Axes(double scale, double x, double y, double z, int viewport) {
        addCoordinateSystem(scale, x, y, z, viewport);
    }

    /**
     * @param scale
     * @param x
     * @param y
     * @param z
     * @param viewport2
     */
    private void addCoordinateSystem(double scale, double x, double y, double z, int viewport2) {
        axes = new vtkAxes();
        axes.SetOrigin(x, y, z);
        axes.SetScaleFactor(scale);

        vtkFloatArray axesColors = new vtkFloatArray();
        axesColors.InsertNextValue(0.0);
        axesColors.InsertNextValue(0.0);
        axesColors.InsertNextValue(0.5);
        axesColors.InsertNextValue(0.5);
        axesColors.InsertNextValue(1.0);
        axesColors.InsertNextValue(1.0);

        vtkPolyData axesPolyData = axes.GetOutput();
        axesPolyData.Update();
        axesPolyData.GetPointData().SetScalars(axesColors);

        vtkTubeFilter axesTubes = new vtkTubeFilter();
        axesTubes.SetInput(axesPolyData);
        axesTubes.SetRadius(axes.GetScaleFactor() / 50.0);
        axesTubes.SetNumberOfSides(8);

        vtkPolyDataMapper axesMapper = new vtkPolyDataMapper();
        axesMapper.SetScalarModeToUsePointData();
        axesMapper.SetInputConnection(axesTubes.GetOutputPort());

        setAxesActor(new vtkLODActor());
        getAxesActor().SetMapper(axesMapper);
    }

    /**
     * 
     * 
     */
    private void addCoordinateSystem() {
        axes = new vtkAxes();
        axes.SetOrigin(0, 0, 0);
        axes.SetScaleFactor(axesScale);

        vtkFloatArray axesColors = new vtkFloatArray();

        axesColors.InsertNextValue(0.0);
        axesColors.InsertNextValue(0.0);
        axesColors.InsertNextValue(0.5);
        axesColors.InsertNextValue(0.5);
        axesColors.InsertNextValue(1.0);
        axesColors.InsertNextValue(1.0);

        vtkPolyData axesPolyData = axes.GetOutput();
        axesPolyData.Update();
        axesPolyData.GetPointData().SetScalars(axesColors);

        vtkTubeFilter axesTubes = new vtkTubeFilter();
        axesTubes.SetInput(axesPolyData);
        axesTubes.SetRadius(axes.GetScaleFactor() / 50.0);
        axesTubes.SetNumberOfSides(8);

        vtkPolyDataMapper axesMapper = new vtkPolyDataMapper();
        axesMapper.SetScalarModeToUsePointData();
        axesMapper.SetInput(axesTubes.GetOutput());

        setAxesActor(new vtkLODActor());
        getAxesActor().SetMapper(axesMapper);
    }

    /**
     * @param scale
     * @param viewport
     */
    private void addCoordinateSystem(double scale, int viewport) {
        axes = new vtkAxes();
        axes.SetOrigin(0, 0, 0);
        axes.SetScaleFactor(scale);

        vtkFloatArray axesColors = new vtkFloatArray();

        axesColors.InsertNextValue(0.0);
        axesColors.InsertNextValue(0.0);
        axesColors.InsertNextValue(0.5);
        axesColors.InsertNextValue(0.5);
        axesColors.InsertNextValue(1.0);
        axesColors.InsertNextValue(1.0);

        vtkPolyData axesPolyData = axes.GetOutput();
        axesPolyData.Update();
        axesPolyData.GetPointData().SetScalars(axesColors);

        vtkTubeFilter axesTubes = new vtkTubeFilter();
        axesTubes.SetInput(axesPolyData);
        axesTubes.SetRadius(axes.GetScaleFactor() / 50.0);
        axesTubes.SetNumberOfSides(6);

        vtkPolyDataMapper axesMapper = new vtkPolyDataMapper();
        axesMapper.SetScalarModeToUsePointData();
        axesMapper.SetInput(axesTubes.GetOutput());

        setAxesActor(new vtkLODActor());
        getAxesActor().SetMapper(axesMapper);
    }

    /**
     * @param scale
     * @param x
     * @param y
     * @param z
     * @param viewport
     */
    private void addCoordinateSystem(double scale, float x, float y, float z, int viewport) {
        axes = new vtkAxes();
        axes.SetOrigin(x, y, z);
        axes.SetScaleFactor(scale);

        vtkFloatArray axesColors = new vtkFloatArray();

        axesColors.InsertNextValue(0.0);
        axesColors.InsertNextValue(0.0);
        axesColors.InsertNextValue(0.5);
        axesColors.InsertNextValue(0.5);
        axesColors.InsertNextValue(1.0);
        axesColors.InsertNextValue(1.0);

        vtkPolyData axesPolyData = axes.GetOutput();
        axesPolyData.Update();
        axesPolyData.GetPointData().SetScalars(axesColors);

        vtkTubeFilter axesTubes = new vtkTubeFilter();
        axesTubes.SetInput(axesPolyData);
        axesTubes.SetRadius(axes.GetScaleFactor() / 50.0);
        axesTubes.SetNumberOfSides(6);

        vtkPolyDataMapper axesMapper = new vtkPolyDataMapper();
        axesMapper.SetScalarModeToUsePointData();
        axesMapper.SetInput(axesTubes.GetOutput());

        setAxesActor(new vtkLODActor());
        getAxesActor().SetMapper(axesMapper);

    }

    /**
     * @return the axesActor
     */
    public vtkLODActor getAxesActor() {
        return axesActor;
    }

    /**
     * @param axesActor the axesActor to set
     */
    private void setAxesActor(vtkLODActor axesActor) {
        this.axesActor = axesActor;
    }

}
