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
 * Mar 14, 2014
 */
package pt.lsts.neptus.vtk.visualization;

import pt.lsts.neptus.vtk.events.MouseEvent;
import pt.lsts.neptus.vtk.events.PointPickingEvent;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;
import vtk.vtkTextActor;

/**
 * @author hfq
 *
 */
public abstract class AInteractorStyleTrackballCamera extends vtkInteractorStyleTrackballCamera {

    private Canvas canvas;
    private vtkRenderer renderer;
    private vtkRenderWindowInteractor interactor;

    private vtkTextActor fpsActor;

    // ########## Mouse Interaction ##########
    private MouseEvent mouseEvent;

    // ########### Point Picking ##########
    private PointPickingEvent pointPickingEvent;

    /**
     * 
     */
    public AInteractorStyleTrackballCamera() {
        super();
    }

    /**
     * @param canvas
     * @param renderer
     * @param interactor
     */
    public AInteractorStyleTrackballCamera(Canvas canvas, vtkRenderer renderer, vtkRenderWindowInteractor interactor) {
        super();

        setCanvas(canvas);
        setRenderer(renderer);
        setInteractor(interactor);

        setPointPickEvent(new PointPickingEvent(canvas));
        setMouseEvent(new MouseEvent(canvas, pointPickingEvent));

        setFpsActor(new vtkTextActor());
    }

    protected abstract void onInitialize();

    /**
     * Callback for FPS Render Event
     * To show frame rate refresh in render window
     */
    protected void callbackFunctionFPS() {
        double timeInSeconds = getRenderer().GetLastRenderTimeInSeconds();
        double fps = 1.0 / timeInSeconds;

        fps = Math.round(fps * 100) / 100.0d;
        getFpsActor().SetInput(String.valueOf(fps));

        getFpsActor().GetTextProperty().SetColor(0.0, 1.0, 0.0);
        getFpsActor().UseBorderAlignOn();
        getFpsActor().SetDisplayPosition(2, 2);

        getRenderer().AddActor(getFpsActor());
    }

    /**
     * @return the canvas
     */
    public Canvas getCanvas() {
        return canvas;
    }

    /**
     * @param canvas the canvas to set
     */
    protected void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    /**
     * @return the renderer
     */
    public vtkRenderer getRenderer() {
        return renderer;
    }

    /**
     * @param renderer the renderer to set
     */
    protected void setRenderer(vtkRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * @return the interactor
     */
    public vtkRenderWindowInteractor getInteractor() {
        return interactor;
    }

    /**
     * @param interactor the interactor to set
     */
    protected void setInteractor(vtkRenderWindowInteractor interactor) {
        this.interactor = interactor;
    }

    /**
     * @return the fpsActor
     */
    protected vtkTextActor getFpsActor() {
        return fpsActor;
    }

    /**
     * @param fpsActor the fpsActor to set
     */
    protected void setFpsActor(vtkTextActor fpsActor) {
        this.fpsActor = fpsActor;
    }

    /**
     * @return the mouseEvent
     */
    protected MouseEvent getMouseEvent() {
        return mouseEvent;
    }

    /**
     * @param mouseEvent the mouseEvent to set
     */
    protected void setMouseEvent(MouseEvent mouseEvent) {
        this.mouseEvent = mouseEvent;
    }

    /**
     * @return the pointPickEvent
     */
    protected PointPickingEvent getPointPickingEvent() {
        return pointPickingEvent;
    }

    /**
     * @param pointPickEvent the pointPickEvent to set
     */
    protected void setPointPickEvent(PointPickingEvent pointPickingEvent) {
        this.pointPickingEvent = pointPickingEvent;
    }

}
