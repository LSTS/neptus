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
 * Apr 9, 2013
 */
package pt.lsts.neptus.vtk.visualization;

import vtk.vtkCompassRepresentation;
import vtk.vtkCompassWidget;
import vtk.vtkRenderWindowInteractor;

/**
 * @author hfq Sets up a Compass Widget to the renderer FIXME - not conneted to actors on renderer (?)
 */
public class Compass {

    // public vtkCompassRepresentation compassRep = new vtkCompassRepresentation();
    public vtkCompassWidget compassWidget = new vtkCompassWidget();

    public Compass() {

    }

    /**
     * Adds a vtk compassWiget to render window
     * 
     * @param renderWinInteractor
     */
    public void addCompassToVisualization(vtkRenderWindowInteractor interactor) {
        vtkCompassRepresentation compassRep = new vtkCompassRepresentation();
        compassRep.NeedToRenderOn();
        compassRep.VisibilityOn();
        compassRep.SetUseBounds(true);
        compassRep.Modified();
        // compassRep.BuildRepresentation();

        compassWidget.SetInteractor(interactor);
        compassWidget.SetRepresentation(compassRep);
        compassWidget.On();
        compassWidget.EnabledOn();

        compassWidget.KeyPressActivationOn();
        // compassWidget.On();
        compassWidget.EnabledOn();
        // compassWidget.Render();
    }

    public void removeCompassFromVisualization(vtkRenderWindowInteractor interactor) {
        compassWidget.EnabledOff();
        compassWidget.Off();
        // compassWidget.Render();
    }
}
