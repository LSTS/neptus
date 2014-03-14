/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 10, 2014
 */
package pt.lsts.neptus.plugins.vtk.cdt3d;

import pt.lsts.neptus.plugins.vtk.events.MouseEvent;
import pt.lsts.neptus.plugins.vtk.events.PointPickingEvent;
import pt.lsts.neptus.plugins.vtk.visualization.Canvas;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;
import vtk.vtkTextActor;

/**
 * @author hfq
 *
 */
public class InteractorStyle extends vtkInteractorStyleTrackballCamera {

    private Canvas canvas;

    public vtkRenderer renderer;
    public vtkRenderWindowInteractor interactor;

    private boolean fpsActorEnable = false;
    private vtkTextActor fpsActor = new vtkTextActor();

    // Mouse Interaction
    MouseEvent mouseEvent;

    // Point Picking
    PointPickingEvent pointPickEvent;

    /**
     * 
     * @param canvas
     * @param renderer
     * @param renWinInteractor
     */
    public InteractorStyle(Canvas canvas, vtkRenderer renderer, vtkRenderWindowInteractor renWinInteractor) {
        super();

        this.canvas = canvas;
        this.renderer = renderer;
        this.interactor = renWinInteractor;

        this.pointPickEvent = new PointPickingEvent(canvas);
        this.mouseEvent = new MouseEvent(canvas, this.pointPickEvent);

        initialize();
    }

    /**
     * 
     */
    private void initialize() {
        UseTimersOn();
        AutoAdjustCameraClippingRangeOn();
        HandleObserversOn();

        interactor.AddObserver("RenderEvent", this, "callbackFunctionFPS");
    }

    void callbackFunctionFPS() {
        double timeInSeconds = renderer.GetLastRenderTimeInSeconds();
        double fps = 1.0 / timeInSeconds;

        fps = Math.round(fps * 100) / 100.0d;
        fpsActor.SetInput(String.valueOf(fps));

        fpsActor.GetTextProperty().SetColor(0.0, 1.0, 0.0);
        fpsActor.UseBorderAlignOn();
        fpsActor.SetDisplayPosition(2, 2);

        if (fpsActorEnable == false) {
            fpsActorEnable = true;
            renderer.AddActor(fpsActor);
        }
    }
}
