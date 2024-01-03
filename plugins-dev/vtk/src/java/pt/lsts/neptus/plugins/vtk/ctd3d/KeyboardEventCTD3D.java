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
package pt.lsts.neptus.plugins.vtk.ctd3d;

import java.awt.event.KeyEvent;

import pt.lsts.neptus.vtk.events.AKeyboardEvent;
import pt.lsts.neptus.vtk.visualization.AInteractorStyleTrackballCamera;
import pt.lsts.neptus.vtk.visualization.Canvas;
import vtk.vtkAbstractPropPicker;
import vtk.vtkAssemblyPath;

/**
 * @author hfq
 *
 */
public class KeyboardEventCTD3D extends AKeyboardEvent {

    private InteractorStyleCTD3D interactorStyle;
    private final EventsHandlerCTD3D events;

    /**
     * @param canvas
     * @param interactorStyleCTD3D 
     * @param events 
     */
    public KeyboardEventCTD3D(Canvas canvas, InteractorStyleCTD3D interactorStyleCTD3D, EventsHandlerCTD3D events) {
        super(canvas);
        this.events = events;
        this.interactorStyle = interactorStyleCTD3D;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.visualization.AKeyboardEvent#handleEvents(int)
     */
    @Override
    public void handleEvents(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_J:
                events.takeSnapShot("CTD_");
                break;
            case KeyEvent.VK_PLUS:
                break;
            case KeyEvent.VK_MINUS:
                break;
            case KeyEvent.VK_R:
                try {
                    getCanvas().lock();
                    getRenderer().GetActiveCamera().SetViewUp(0.0, 0.0, -1.0);
                    getRenderer().ResetCamera();
                    getCanvas().Render();
                    getCanvas().unlock();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case KeyEvent.VK_F:
                getCanvas().lock();

                vtkAssemblyPath path = null;
                interactorStyle.FindPokedRenderer(getInteractor().GetEventPosition()[0],
                        getInteractor().GetEventPosition()[1]);
                getInteractor().GetPicker().Pick(getInteractor().GetEventPosition()[0], getInteractor().GetEventPosition()[1], 0.0,
                        getRenderer());

                vtkAbstractPropPicker picker;
                if ((picker = (vtkAbstractPropPicker) getInteractor().GetPicker()) != null) {
                    path = picker.GetPath();
                }
                if (path != null) {
                    getInteractor().FlyTo(getRenderer(), picker.GetPickPosition()[0], picker.GetPickPosition()[1],
                            picker.GetPickPosition()[2]);
                }
                getCanvas().unlock();
                break;
            default:
                break;
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.visualization.AKeyboardEvent#setInteractorStyle(pt.lsts.neptus.plugins.vtk.visualization.AInteractorStyleTrackballCamera)
     */
    @Override
    protected void setInteractorStyle(AInteractorStyleTrackballCamera interactorStyle) {
        this.interactorStyle = (InteractorStyleCTD3D) interactorStyle;
    }
}
