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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import vtk.vtkCommand;
import vtk.vtkImageAlgorithm;
import vtk.vtkInteractorStyleTrackballActor;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkLegendScaleActor;
import vtk.vtkPNGWriter;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;
import vtk.vtkScalarBarActor;
import vtk.vtkTextActor;
import vtk.vtkTextProperty;
import vtk.vtkWindowToImageFilter;
import vtk.vtkXYPlotActor;

/**
 * @author hfq
 * defines a unique, custom VTK based interactory style for Neptus 3D Visualizer apps.
 * Besides defining the rendering style, it also creates a list of custom actions that
 * are triggered on different keys being pressed:
 * 
 * -    p, P        : switch to a point-based representation
 * -    w, W        : switch to a wireframe-based representation, when available
 * -    s, S        : switch to a surface-based representation, when available
 * -    j, J        : take a .PNG snapshot of the current window view
 * -    c, C        : display current camera/window parameters
 * -    f, F        : fly to point mode
 * -    e, E        : exit the interactor
 * -    q, Q        : stop and call VTK's TerminateApp
 * -    + / -       : increment/decrement overall point size
 * -    g / G       : display scale grid (on/off)
 * -    u / U       : display lookup table (on/off)
 * -    r / R       : reset camera [to viewpoint = {0, 0, 0] -> center {x, y, z}]
 * -    0..9        : switch between different color handlers, when available
 * - SHIFT + left click     : select a point
 */
public class NeptusInteractorStyle extends vtkInteractorStyleTrackballCamera{
    
    // A renderer
    private vtkRenderer renderer = new vtkRenderer();
    
    // The render Window Interactor
    private vtkRenderWindowInteractor interactor = new vtkRenderWindowInteractor();
    
    // the XY plt actor holding the actual data.
    vtkXYPlotActor xyActor = new vtkXYPlotActor();
    
    // the render window interactor style;
    private vtkInteractorStyleTrackballCamera style = new vtkInteractorStyleTrackballCamera();
    
    // Set true if the LUT actor is enabled
    boolean lutEnabled;
    // Actor for 2D loookup table on screen
    protected vtkScalarBarActor lutActor = new vtkScalarBarActor();

    // Set true if the grid actor is enabled
    boolean gridEnabled;
    // Actor for 2D grid on screen
    protected vtkLegendScaleActor gridActor = new vtkLegendScaleActor();
    
    // A PNG Writer for screenshot captures
    vtkPNGWriter snapshotWriter = new vtkPNGWriter();
    // Internal Window to image Filter. Needed by a snapshotWriter object
    vtkWindowToImageFilter wif = new vtkWindowToImageFilter();
    
    // Current Window position width/height
    int winHeight, winWidth;
    // Current window postion x/y
    int winPosX, winPosY;
    
    /*
     * TrackballActor style interactor for addObserver callback reference
     */
    vtkInteractorStyleTrackballActor astyle = new vtkInteractorStyleTrackballActor();
    /*
     * TrackballCamera style interactor for addObserver callback reference
     */
    vtkInteractorStyleTrackballCamera cstyle = new vtkInteractorStyleTrackballCamera();
    char curIStyle = 'A'; // interaction style A = Actor C = camera,
    // toggled by 'C' key handler.
    
    
    vtkTextActor fpsActor = new vtkTextActor();
    
    // Change default keyboard modified from ALT to a different special key
    public enum InteractorKeyboardModifier
    {
        INTERACTOR_KB_MOD_ALT,
        INTERACTOR_KB_MOD_CTRL,
        INTERACTOR_KB_MOD_SHIFT
    }
    
    private InteractorKeyboardModifier interactModifier;
    
    public NeptusInteractorStyle(vtkRenderer renderer, vtkRenderWindowInteractor interact) {
        super();
        this.renderer = renderer;
        this.interactor = interact;
        Initalize();
    }
    
    /**
     * Initialization routine. Must be called before anything else.
     */
    private void Initalize() {
        System.out.println("veio ao initialize do Neptus Style");
        //interactModifier = InteractorKeyboardModifier.INTERACTOR_KB_MOD_ALT;
        // Set window size (width, height) to unknow (-1)
        winHeight = winWidth = -1;
        winPosX = winPosY = 0;
        
        // Grid is disabled by default
        gridEnabled = false;
        //gridActor = new vtkLegendScaleActor();
        
        // LUT is disabled by default
        lutEnabled = false;
        //lutActor = new vtkScalarBarActor();
        //lutActor.SetTitle("");
        lutActor.SetOrientationToHorizontal();
        //lutActor.SetOrientationToVertical();
        lutActor.SetPosition(0.05, 0.01);
        lutActor.SetWidth(0.9);
        lutActor.SetHeight(0.1);
        lutActor.SetNumberOfLabels(lutActor.GetNumberOfLabels() * 2);
        vtkTextProperty prop = new vtkTextProperty();
        prop = lutActor.GetLabelTextProperty();
        prop.SetFontSize(10);
        lutActor.SetLabelTextProperty(prop);
        lutActor.SetTitleTextProperty(prop);
        
        // Create the image filter and PNG wirter objects
        wif = new vtkWindowToImageFilter();
        snapshotWriter = new vtkPNGWriter();
        snapshotWriter.SetInputConnection(wif.GetOutputPort());

        
        // add a observer (callback) for point picking
        //vtkCommand leftMouse = new vtkCommand();
        AddObserver("MouseEvent", this, "leftMouse");
        ////interactor.AddObserver(null, leftMouse, id2)
        getInteractor().AddObserver("CharEvent", this, "callbackFunctionFPS");
        getInteractor().AddObserver("CharEvent", this, "saveScreenshot");

    }
    
    void leftMouse() {
        System.out.println("carregou no botão esquerdo do rato");
    }
    
    /**
     * for now saves on neptus directory
     */
    void saveScreenshot() {
        if (getInteractor().GetKeyCode() == 'j') {
            //System.out.println("save screenshot");
            //int pos1 = getInteractor().GetEventPosition()[0];
            //int pos2 = getInteractor().GetEventPosition()[1];
            //System.out.println("Event Position - pos1: " + pos1 + " pos2: " + pos2);
            
            FindPokedRenderer(getInteractor().GetEventPosition()[0], getInteractor().GetEventPosition()[1]);
            wif.SetInput(interactor.GetRenderWindow());
            wif.Modified();           
            snapshotWriter.Modified();
            
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmssmm").format(Calendar.getInstance().getTimeInMillis());
            timeStamp = "snapshot_" + timeStamp;
            //System.out.println("timeStamp: " + timeStamp);
            
            snapshotWriter.SetFileName(timeStamp);
            snapshotWriter.Write();
        }
    }
    
    void callbackFunctionFPS() {
        
        //System.out.println("veio ao callbackfunction");
        ////vtkRenderer renderer = (vtkRenderer)caller;
        
        double timeInSeconds = this.renderer.GetLastRenderTimeInSeconds();
        double fps = 1.0/timeInSeconds;   

        fpsActor.SetInput(Double.toString(fps));
        //System.out.println("FPS: " + fps);
        
        fpsActor.GetPositionCoordinate().SetCoordinateSystemToNormalizedDisplay();
        fpsActor.GetTextProperty().SetColor(0.0, 0.0, 0.0);
        
        if (getInteractor().GetKeyCode() == 'k') {
            System.out.println("FPS2: " + fps);
            
        }       
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
}
