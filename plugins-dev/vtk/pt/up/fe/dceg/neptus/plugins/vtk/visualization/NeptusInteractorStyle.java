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

import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkLegendScaleActor;
import vtk.vtkPNGWriter;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkScalarBarActor;
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
    
    // the XY plt actor holding the actual data.
    vtkXYPlotActor xyActor;
    
    // The render Window Interactor
    vtkRenderWindowInteractor interactor;
    // the render window interactor style;
    vtkInteractorStyleTrackballCamera style;
    
    // Set true if the LUT actor is enabled
    boolean lutEnabled;
    // Actor for 2D loookup table on screen
    protected vtkScalarBarActor lutActor;

    // Set true if the grid actor is enabled
    boolean gridEnabled;
    // Actor for 2D grid on screen
    protected vtkLegendScaleActor gridActor;
    
    // A PNG Writer for screenshot captures
    vtkPNGWriter snapshotWriter;
    // Internal Window to image Filter. Needed by a snapshotWriter object
    vtkWindowToImageFilter wif;
    
    // Current Window position x/y
    int winHeight, winWidth;
    
    public enum InteractorKeyboardModifier
    {
        INTERACTOR_KB_MOD_ALT,
        INTERACTOR_KB_MOD_CTRL,
        INTERACTOR_KB_MOD_SHIFT
    }
    
    public NeptusInteractorStyle() {
        
    }
    
    /**
     * Initialization routine. Must be called before anything else.
     */
    private void Initalize() {
        
    }
}
