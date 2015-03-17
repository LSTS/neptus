/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
public class AxesWidget extends vtkOrientationMarkerWidget {

    private final vtkRenderWindowInteractor interactor;

    /**
     * 
     * @param interactor
     */
    public AxesWidget(vtkRenderWindowInteractor interactor) {
        this.interactor = interactor;
    }

    /**
     * 
     */
    public void createAxesWidget() {
        try {
            SetInteractor(interactor);

            vtkAxesActor axes = new vtkAxesActor();

            SetOutlineColor(0.9300, 0.5700, 0.1300);
            SetOrientationMarker(axes);

            SetViewport(0.00, 0.05, 0.23, 0.28);
            EnabledOn();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
