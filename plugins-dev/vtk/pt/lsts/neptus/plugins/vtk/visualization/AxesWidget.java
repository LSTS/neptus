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
 * May 14, 2013
 */
package pt.lsts.neptus.plugins.vtk.visualization;

import vtk.vtkAxesActor;
import vtk.vtkOrientationMarkerWidget;
import vtk.vtkRenderWindowInteractor;

/**
 * @author hfq Adds axes fixed to viewport position
 * 
 *         FIXME - Orientation Marker should be a prop from Axes or AxesActor Classes
 */
public class AxesWidget {

    vtkRenderWindowInteractor interactor;

    public AxesWidget(vtkRenderWindowInteractor interactor) {
        this.interactor = interactor;
    }

    /**
     * 
     */
    public void createAxesWidget() {
        try {
            vtkAxesActor axes = new vtkAxesActor();

            vtkOrientationMarkerWidget widget = new vtkOrientationMarkerWidget();
            widget.SetInteractor(interactor);
            widget.SetOutlineColor(0.9300, 0.5700, 0.1300);
            widget.SetOrientationMarker(axes);
            // widget.SetOrientationMarker(cubeActor);
            // widget.InteractiveOff();

            // widget.InteractiveOn();
            // widget.SetViewport(0.77, 0.77, 1.0, 1.0); // top right
            widget.SetViewport(0.00, 0.05, 0.23, 0.28);
            widget.EnabledOn();
            // widget.SetTolerance(2); // tolerance representing distance to the widget (in pixels)
            // !??! interactive must be off because of multi-threaded events -> java: ../../src/xcb_io.c:273:
            // poll_for_event: Assertion `!xcb_xlib_threads_sequence_lost' failed.
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
