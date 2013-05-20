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
 * Apr 16, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.visualization;

import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import vtk.vtkCamera;
import vtk.vtkCanvas;
import vtk.vtkCellPicker;
import vtk.vtkInteractorStyleTrackballActor;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkLegendScaleActor;
import vtk.vtkPNGWriter;
import vtk.vtkProp3D;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;
import vtk.vtkScalarBarActor;
import vtk.vtkTextActor;
import vtk.vtkTextProperty;
import vtk.vtkWindowToImageFilter;

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
public class NeptusInteractorStyle extends vtkInteractorStyleTrackballCamera implements MouseWheelListener, KeyListener {

    public LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud = new LinkedHashMap<>();

    // A vtkCanvas
    public vtkCanvas canvas;
    // A renderer
    public vtkRenderer renderer;
    // The render Window Interactor
    public vtkRenderWindowInteractor interactor;

    // A Camera
    public vtkCamera camera = new vtkCamera();

    // the render window interactor style;
    private vtkInteractorStyleTrackballCamera style = new vtkInteractorStyleTrackballCamera();

    // frame per seconds text actor - show frame rate refresh on visualizer
    private boolean fpsActorEnable = false;

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

    private vtkTextActor fpsActor = new vtkTextActor();

    // A PNG Writer for screenshot captures
    protected vtkPNGWriter snapshotWriter = new vtkPNGWriter();
    // Internal Window to image Filter. Needed by a snapshotWriter object
    protected vtkWindowToImageFilter wif = new vtkWindowToImageFilter();

    // Set true if the grid actor is enabled
    protected boolean gridEnabled;
    // Actor for 2D grid on screen
    protected vtkLegendScaleActor gridActor = new vtkLegendScaleActor();

    // Set true if the LUT actor is enabled
    protected boolean lutEnabled;
    // Actor for 2D loookup table on screen
    private vtkScalarBarActor lutActor = new vtkScalarBarActor();

    // ########## Mouse Interaction ##########
    vtkProp3D InteractionProp;
    vtkCellPicker InteractionPicker;

    // Current Window position width/height
    int winHeight, winWidth;
    // Current window postion x/y
    int winPosX, winPosY;

    // TrackballActor style interactor for addObserver callback reference
    vtkInteractorStyleTrackballActor astyle = new vtkInteractorStyleTrackballActor();
    // TrackballCamera style interactor for addObserver callback reference
    vtkInteractorStyleTrackballCamera cstyle = new vtkInteractorStyleTrackballCamera();

    /**
     * 
     * @param canvas
     * @param renderer
     * @param interact
     * @param linkedHashMapCloud
     */
    public NeptusInteractorStyle(vtkCanvas canvas, vtkRenderer renderer, vtkRenderWindowInteractor interact,
            LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud) {
        super();
        this.canvas = canvas;
        this.renderer = renderer;
        this.interactor = interact;
        this.camera = renderer.GetActiveCamera();
        this.linkedHashMapCloud = linkedHashMapCloud;
        keyboardEvent = new KeyboardEvent(this, this.linkedHashMapCloud);

        Initalize();
    }

    /**
     * Initialization routine. Must be called before anything else. Possible Vtk Oberver Events (some don't work in
     * Java) :
     * 
     * LeftButtonPressEvent RightButtonPressEvent <- works StartInteractionEvent ModifiedEvent EndInteractionEvent
     * RenderEvent MouseMoveEvent <- works InteractorEvent UserEvent LeaveEvent <- works (triggers event everytime mouse
     * gets off render window)
     */
    private void Initalize() {
        UseTimersOn();
        HandleObserversOn();

        // interactModifier = InteractorKeyboardModifier.INTERACTOR_KB_MOD_ALT;

            // Set window size (width, height) to unknow (-1)
        winHeight = winWidth = -1;
        winPosX = winPosY = 0;

            // Grid is disabled by default
        gridEnabled = false;
        // gridActor = new vtkLegendScaleActor();

            // LUT is disabled by default
        //lutEnabled = false;
        lutEnabled = true;
        // lutActor = new vtkScalarBarActor();
        // lutActor.SetTitle("");
        getLutActor().SetOrientationToHorizontal();
        // lutActor.SetOrientationToVertical();
        getLutActor().SetPosition(0.15, 0.01);
        getLutActor().SetWidth(0.7);
        getLutActor().SetHeight(0.1);
        getLutActor().SetNumberOfLabels(getLutActor().GetNumberOfLabels() * 2);
        vtkTextProperty textProp = new vtkTextProperty();
        textProp = getLutActor().GetLabelTextProperty();
        textProp.SetFontFamilyToArial();
        textProp.BoldOn();
        textProp.ItalicOn();
        textProp.SetOpacity(0.9);
        textProp.SetFontSize(8);
        getLutActor().SetLabelTextProperty(textProp);
        getLutActor().SetTitleTextProperty(textProp);

            // Create the image filter and PNG writer objects
        wif = new vtkWindowToImageFilter();
        snapshotWriter = new vtkPNGWriter();
        snapshotWriter.SetInputConnection(wif.GetOutputPort());

        compassEnabled = false;

        getInteractor().AddObserver("RenderEvent", this, "callbackFunctionFPS");

        // //getInteractor().AddObserver("LeftButtonPressEven", this, "leftMousePress");
        // //getInteractor().AddObserver("LeftButtonReleaseEvent", this, "leftMouseRelease");
        // //getInteractor().AddObserver("RightButtonPressEvent", this, "rightMousePress");
        // //getInteractor().AddObserver("RightButtonReleaseEvent", this, "rightMouseRelease");
        // //getInteractor().AddObserver("MiddleButtonPressEvent", this, "middleButtonPress");
        // //getInteractor().AddObserver("MiddleButtonReleaseEvent", this, "middleButtonRelease");
        // //getInteractor().AddObserver("MouseWheelForwardEvent", this, "mouseWheelForwardEvent");
        // //getInteractor().AddObserver("MouseWheelBackwardEvent", this, "mouseWheelBackwardEvent");
        // //getInteractor().AddObserver("EndInteractionEvent", this, "endInteractionEvent");
        // //getInteractor().AddObserver("LeaveEvent", this, "leaveEvent");
        // //getInteractor().AddObserver("InteractorEvent", this, "callbackFunctionFPS");
        // //getInteractor().AddObserver("CharEvent", this, "saveScreenshot");
        // //interactor.AddObserver("LeftButtonReleaseEvent", interactor.LeftButtonPressEvent(), "leftMousePress");
        // //interactor.LeftButtonPressEvent()
        // //getInteractor().AddObserver("EndEvent", this, "callbackFunctionFPS");
        // getInteractor().AddObserver("CharEvent", this, "emitKeyboardEvents");

        canvas.addMouseWheelListener(this);
        canvas.addKeyListener(this);

            // não colocar o render logo, senão os eventos do java (mouseWheel)
        // canvas.Render();
    }

    /**
     * Render Event to show frame rate in render window
     */
    void callbackFunctionFPS() {
        double timeInSeconds = this.renderer.GetLastRenderTimeInSeconds();
        double fps = 1.0 / timeInSeconds;

        fps = Math.round(fps * 100) / 100.0d;
        fpsActor.SetInput(String.valueOf(fps));

        fpsActor.GetTextProperty().SetColor(0.0, 1.0, 0.0);
        fpsActor.UseBorderAlignOn();
        fpsActor.SetDisplayPosition(2, 2);

        if (fpsActorEnable == false) {
            fpsActorEnable = true;
            this.renderer.AddActor(fpsActor);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        String message;
        int notches = e.getWheelRotation();
        if (notches < 0) {
            zoomIn();
        }
        else {
            zoomOut();
        }
    }

    /**
     * Operates like a magnifying lens
     */
    private void zoomIn() {
        FindPokedRenderer(interactor.GetEventPosition()[0], interactor.GetEventPosition()[1]);
            // Zoom in
        StartDolly();
        camera = renderer.GetActiveCamera();
        double factor = 10.0 * 0.2 * .5;
        camera.Dolly(Math.pow(1.1, factor));
        EndDolly();
    }

    /**
     * Operates like a magnifying lens
     */
    private void zoomOut() {
        FindPokedRenderer(interactor.GetEventPosition()[0], interactor.GetEventPosition()[1]);
            // zoomOut
        // double[] posCam;
        // double[] posCam2;
        // double[] focalPointCam;
        // double[] focalPointCam2;
        // double[] newFocalPoint;
        // double posX, posY, posZ;
        // posCam = camera.GetPosition();
        // focalPointCam = camera.GetFocalPoint();
        // newFocalPoint = focalPointCam;
        // System.out.println("posX: " + posCam[0] + ", posY: " + posCam[1] + ", posZ: " + posCam[2]);
        // System.out.println("focalPointCam:" + focalPointCam[0] + ", fy: " + focalPointCam[1] + ", fz: " +
        // focalPointCam[2]);

        StartDolly();
        camera = renderer.GetActiveCamera();
        double factor = 10.0 * -0.2 * .5;
        camera.Dolly(Math.pow(1.1, factor));

        // posCam2 = camera.GetPosition();
        // focalPointCam2 = camera.GetFocalPoint();

        // System.out.println("posX2: " + posCam2[0] + ", posY2: " + posCam2[1] + ", posZ2: " + posCam2[2]);
        // System.out.println("focalPointCamx2:" + focalPointCam2[0] + ", fy2: " + focalPointCam2[1] + ", fz2: " +
        // focalPointCam2[2]);
        // newFocalPoint[0] = newFocalPoint[0] + (posCam[0] - posCam2[0]);
        // newFocalPoint[1] = newFocalPoint[1] + (posCam[1] - posCam2[1]);
        // newFocalPoint[2] = newFocalPoint[2] + (posCam2[2] - posCam[2]);

        // camera.SetFocalPoint(newFocalPoint);
        EndDolly();
        // camera.SetPosition(posCam);
    }

    @Override
    public void keyTyped(java.awt.event.KeyEvent e) {

    }

    /**
     * keyboard events launched from vtk interactor
     */
    void emitKeyboardEvents() {
        char keyCode = Character.toLowerCase(interactor.GetKeyCode());
        //System.out.println("keycode is: " + keyCode);
        //String keySym = interactor.GetKeySym();
        //System.out.println("Sym is: " + keySym);
        int keyInt = Character.getNumericValue(keyCode);
        //System.out.println("keyInt is: " + keyInt);

        this.keyboardEvent.handleEvents(keyCode);
    }

    @Override
    public void keyPressed(java.awt.event.KeyEvent ke) {
        this.keyboardEvent.handleEvents(ke.getKeyCode());
    }

    @Override
    public void keyReleased(java.awt.event.KeyEvent e) {

    }

    /**
     * works only when mouse leaves render window
     */
    void leaveEvent() {
        NeptusLog.pub().info("leaveEvent");
    }

    /**
     * works on release of every mouse button
     */
    void endInteractionEvent() {
        System.out.println("end Interaction event");
    }

    /*
     * works
     */
    void middleButtonPress() {
        System.out.println("middle button pressed");
    }

    void middleButtonRelease() {
        System.out.println("middle button released");
    }

    void leftMousePress() {
        System.out.println("carregou no botão esquerdo do rato");
    }

    void leftMouseRelease() {
        System.out.println("left mouse release");
    }

    /*
     * works
     */
    void rightMousePress() {
        System.out.println("right mouse press");
    }

    void rightMouseRelease() {
        System.out.println("right mouse release");
    }

    void mouseWheelForwardEvent() {
        System.out.println("mouse Wheel forward Event");
    }

    void mouseWheelBackwardEvent() {
        double dolly = interactor.GetDolly();
        System.out.println("mouse wheel backward event, dolly: " + dolly);
    }

    /**
     * @return the interactor
     */
    vtkRenderWindowInteractor getInteractor() {
        return interactor;
    }

    /**
     * @param interactor the interactor to set
     */
    void setInteractor(vtkRenderWindowInteractor interactor) {
        this.interactor = interactor;
    }

    /**
     * @return the style
     */
    vtkInteractorStyleTrackballCamera getStyle() {
        return style;
    }

    /**
     * @param style the style to set
     */
    void setStyle(vtkInteractorStyleTrackballCamera style) {
        this.style = style;
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
}
