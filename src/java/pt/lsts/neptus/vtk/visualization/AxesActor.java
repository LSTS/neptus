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
 * Apr 12, 2013
 */
package pt.lsts.neptus.vtk.visualization;

import vtk.vtkActor;
import vtk.vtkAssembly;
import vtk.vtkAxes;
import vtk.vtkConeSource;
import vtk.vtkPolyDataMapper;
import vtk.vtkRenderer;
import vtk.vtkTextActor;
import vtk.vtkTubeFilter;

/**
 * @author hfq
 * 
 */
public class AxesActor extends vtkAssembly {
    private vtkRenderer ren;
    private double axisLength = 0.8;
    private vtkTextActor xactor, yactor, zactor;

    /**
     * 
     * @param ren_
     */
    public AxesActor(vtkRenderer ren_) {
        super();
        ren = ren_;
    }

    /**
     * 
     */
    public void createAxes() {
        vtkAxes axes = new vtkAxes();
        axes.SetOrigin(0, 0, 0);
        axes.SetScaleFactor(axisLength);

        xactor = new vtkTextActor();
        yactor = new vtkTextActor();
        zactor = new vtkTextActor();

        xactor.SetInput("X");
        yactor.SetInput("Y");
        zactor.SetInput("Z");

        xactor.GetPositionCoordinate().SetCoordinateSystemToWorld();
        yactor.GetPositionCoordinate().SetCoordinateSystemToWorld();
        zactor.GetPositionCoordinate().SetCoordinateSystemToWorld();

        xactor.GetPositionCoordinate().SetValue(axisLength, 0.0, 0.0);
        yactor.GetPositionCoordinate().SetValue(0.0, axisLength, 0.0);
        zactor.GetPositionCoordinate().SetValue(0.0, 0.0, axisLength);

        xactor.GetTextProperty().SetColor(1.0, 1.0, 1.0);
        xactor.GetTextProperty().ShadowOn();
        xactor.GetTextProperty().ItalicOn();
        xactor.GetTextProperty().BoldOff();

        yactor.GetTextProperty().SetColor(1.0, 1.0, 1.0);
        yactor.GetTextProperty().ShadowOn();
        yactor.GetTextProperty().ItalicOn();
        yactor.GetTextProperty().BoldOff();

        zactor.GetTextProperty().SetColor(1.0, 1.0, 1.0);
        zactor.GetTextProperty().ShadowOn();
        zactor.GetTextProperty().ItalicOn();
        zactor.GetTextProperty().BoldOff();

        xactor.SetMaximumLineHeight(0.25);
        yactor.SetMaximumLineHeight(0.25);
        zactor.SetMaximumLineHeight(0.25);

        // xactor.SetMaximumLineHeight(2.0);
        // yactor.SetMaximumLineHeight(2.0);
        // zactor.SetMaximumLineHeight(2.0);

        vtkTubeFilter tube = new vtkTubeFilter();
        tube.SetInputConnection(axes.GetOutputPort());
        tube.SetRadius(0.05);
        // tube.SetRadiusFactor(10.0);
        // tube.SetRadius(axes.GetScaleFactor() / 50.0);
        tube.SetNumberOfSides(6);

        vtkPolyDataMapper tubeMapper = new vtkPolyDataMapper();
        tubeMapper.SetInputConnection(tube.GetOutputPort());

        vtkActor tubeActor = new vtkActor();
        tubeActor.SetMapper(tubeMapper);
        tubeActor.PickableOff();

        int coneRes = 12;
        double coneScale = 0.3;

        // xcone
        vtkConeSource xcone = new vtkConeSource();
        xcone.SetResolution(coneRes);
        vtkPolyDataMapper xconeMapper = new vtkPolyDataMapper();
        xconeMapper.SetInputConnection(xcone.GetOutputPort());
        vtkActor xconeActor = new vtkActor();
        xconeActor.SetMapper(xconeMapper);
        xconeActor.GetProperty().SetColor(1, 0, 0);
        xconeActor.SetScale(coneScale, coneScale, coneScale);
        xconeActor.SetPosition(axisLength, 0.0, 0.0);

        // ycone
        vtkConeSource ycone = new vtkConeSource();
        ycone.SetResolution(coneRes);
        vtkPolyDataMapper yconeMapper = new vtkPolyDataMapper();
        yconeMapper.SetInputConnection(ycone.GetOutputPort());
        vtkActor yconeActor = new vtkActor();
        yconeActor.SetMapper(yconeMapper);
        yconeActor.GetProperty().SetColor(1, 1, 0);
        yconeActor.RotateZ(90);
        yconeActor.SetScale(coneScale, coneScale, coneScale);
        yconeActor.SetPosition(0.0, axisLength, 0.0);

        // zcone
        vtkConeSource zcone = new vtkConeSource();
        zcone.SetResolution(coneRes);
        vtkPolyDataMapper zconeMapper = new vtkPolyDataMapper();
        zconeMapper.SetInputConnection(zcone.GetOutputPort());
        vtkActor zconeActor = new vtkActor();
        zconeActor.SetMapper(zconeMapper);
        zconeActor.GetProperty().SetColor(0, 1, 0);
        zconeActor.RotateY(-90);
        zconeActor.SetScale(coneScale, coneScale, coneScale);
        zconeActor.SetPosition(0.0, 0.0, axisLength);

        ren.AddActor2D(xactor);
        ren.AddActor2D(yactor);
        ren.AddActor2D(zactor);

        this.AddPart(tubeActor);
        this.AddPart(xconeActor);
        this.AddPart(yconeActor);
        this.AddPart(zconeActor);

        ren.AddActor(this);
    }

    /**
     * 
     * @param ison
     */
    public void setAxesVisibility(boolean ison) {
        this.SetVisibility(ison ? 1 : 0);
        xactor.SetVisibility(ison ? 1 : 0);
        yactor.SetVisibility(ison ? 1 : 0);
        zactor.SetVisibility(ison ? 1 : 0);
    }
}
