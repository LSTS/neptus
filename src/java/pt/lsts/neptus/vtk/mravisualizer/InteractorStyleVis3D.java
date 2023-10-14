/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Apr 16, 2013
 */
package pt.lsts.neptus.vtk.mravisualizer;

import java.util.LinkedHashMap;

import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.vtk.pointcloud.APointCloud;
import pt.lsts.neptus.vtk.surface.PointCloudMesh;
import pt.lsts.neptus.vtk.visualization.AInteractorStyleTrackballCamera;
import pt.lsts.neptus.vtk.visualization.AxesWidget;
import pt.lsts.neptus.vtk.visualization.Canvas;
import pt.lsts.neptus.vtk.visualization.Compass;
import pt.lsts.neptus.vtk.visualization.ScalarBar;
import vtk.vtkCamera;
import vtk.vtkLegendScaleActor;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;
import vtk.vtkScalarBarActor;

/**
 * @author hfq
 * 
 *         FIXME - Missing: point picking events: to add markers to cloud (or mesh) to add
 * 
 *         defines a unique, custom VTK based interactory style for Neptus 3D Visualizer apps. Besides defining the
 *         rendering style, it also creates a list of custom actions that are triggered on different keys being pressed:
 * 
 *         - p, P : switch to a point-based representation - w, W : switch to a wireframe-based representation, when
 *         available - s, S : switch to a surface-based representation, when available - j, J : take a .PNG snapshot of
 *         the current window view - c, C : display compass (Compass class) Change!!! * - c, C : display current
 *         camera/window parameters - f, F : fly to point mode - e, E : exit the interactor <- not implemented - q, Q :
 *         stop and call VTK's TerminateApp <- not implemented - + / - : increment/decrement overall point size - g / G
 *         : display scale grid (on/off) - u / U : display lookup table (on/off) - r / R : reset camera [to viewpoint =
 *         {0, 0, 0] -> center {x, y, z}] - 0..9 : switch between different color handlers, when available - SHIFT +
 *         left click : select a point <- point picker not implemented
 */
public class InteractorStyleVis3D extends AInteractorStyleTrackballCamera {

    public LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud;
    public LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh;

    private EventsHandler events;

    // A Camera
    public vtkCamera camera = new vtkCamera();

    // ########## Keyboard interaction ##########
    public KeyboardEvent keyboardEvent;

    // Change default keyboard modified from ALT to a different special key
    private enum InteractorKeyboardModifier {
        INTERACTOR_KB_MOD_ALT,
        INTERACTOR_KB_MOD_CTRL,
        INTERACTOR_KB_MOD_SHIFT
    }

    public InteractorKeyboardModifier interactModifier;

    protected boolean wireframeRepEnabled = false;
    protected boolean solidRepEnabled = false;
    protected boolean pointRepEnabled = true;

    // Set true if the Compass Widget is enabled
    protected boolean compassEnabled;
    // Actor for Compass Widget on screen
    protected Compass compass = new Compass();

    // Set true if the grid actor is enabled
    protected boolean gridEnabled;
    // Actor for 2D grid on screen
    protected vtkLegendScaleActor gridActor = new vtkLegendScaleActor();

    // Set true if the LUT actor is enabled
    protected boolean lutEnabled;
    // Actor for 2D loookup table on screen
    private vtkScalarBarActor lutActor = new vtkScalarBarActor();
    private ScalarBar scalarBar;

    // add axesWidget to vtk canvas fixed to a screen position
    private AxesWidget axesWidget;

    /**
     * @param canvas
     * @param renderer
     * @param interact
     * @param linkedHashMapCloud
     */
    public InteractorStyleVis3D(Canvas canvas, vtkRenderer renderer, vtkRenderWindowInteractor renWinInteractor,
            LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud, LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh,
            IMraLogGroup source) {
        super(canvas, renderer, renWinInteractor);

        this.camera = renderer.GetActiveCamera();
        this.linkedHashMapCloud = linkedHashMapCloud;
        this.linkedHashMapMesh = linkedHashMapMesh;
        this.setScalarBar(new ScalarBar());

        this.setEventsHandler(new EventsHandler(this, linkedHashMapCloud, linkedHashMapMesh, source));
        this.keyboardEvent = new KeyboardEvent(this.getCanvas(), this.linkedHashMapCloud, this, getEventsHandler());

        this.setAxesWidget(new AxesWidget(renWinInteractor));

        onInitialize();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.visualization.AInteractorStyleTrackballCamera#onInitialize()
     */
    @Override
    protected void onInitialize() {
        UseTimersOn();
        HandleObserversOn();
        AutoAdjustCameraClippingRangeOn();

        // add axesWidget to vtk canvas fixed to a screen position
        getAxesWidget().createAxesWidget();

        // Grid is disabled by default
        gridEnabled = false;
        // gridActor = new vtkLegendScaleActor();

        // LUT is enabled by default
        lutEnabled = true;
        getScalarBar().setScalarBarHorizontalProperties();
        // getScalarBar().setScalarBarVerticalProperties();

        compassEnabled = false;

        getInteractor().AddObserver("RenderEvent", this, "callbackFunctionFPS");

        getCanvas().addKeyListener(keyboardEvent);
    }

    /**
     * Render event to show point coords on mouse hover
     */
    void callbackPointCoords() {

    }

    /**
     * @return the lutActor
     */
    public vtkScalarBarActor getLutActor() {
        return lutActor;
    }

    /**
     * @param lutActor the lutActor to set
     */
    public void setLutActor(vtkScalarBarActor lutActor) {
        this.lutActor = lutActor;
    }

    /**
     * @return the scalarBar
     */
    public ScalarBar getScalarBar() {
        return scalarBar;
    }

    /**
     * @param scalarBar the scalarBar to set
     */
    public void setScalarBar(ScalarBar scalarBar) {
        this.scalarBar = scalarBar;
    }

    /**
     * @return the events
     */
    public EventsHandler getEventsHandler() {
        return events;
    }

    /**
     * @param events the events to set
     */
    private void setEventsHandler(EventsHandler events) {
        this.events = events;
    }

    /**
     * @return the axesWidget
     */
    public AxesWidget getAxesWidget() {
        return axesWidget;
    }

    /**
     * @param axesWidget the axesWidget to set
     */
    private void setAxesWidget(AxesWidget axesWidget) {
        this.axesWidget = axesWidget;
    }
}
