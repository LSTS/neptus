/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.vtk.events;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import pt.lsts.neptus.vtk.visualization.AInteractorStyleTrackballCamera;
import pt.lsts.neptus.vtk.visualization.Canvas;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;

/**
 * FIXME add keys to change mode (trackball, joystick..)
 * 
 * @author hfq
 */
public abstract class AKeyboardEvent implements KeyListener {

    private Canvas canvas;
    private vtkRenderer renderer;
    private vtkRenderWindowInteractor interactor;

    private AInteractorStyleTrackballCamera interactorStyle;

    public AKeyboardEvent(Canvas canvas) {
        setCanvas(canvas);
        setRenderer(canvas.GetRenderer());
        setInteractor(canvas.getRenderWindowInteractor());
    }

    /**
     * Handle key events
     * @param keyCode
     */
    public abstract void handleEvents(int keyCode);


    /**
     * @return the canvas
     */
    protected Canvas getCanvas() {
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
    protected vtkRenderer getRenderer() {
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
    protected vtkRenderWindowInteractor getInteractor() {
        return interactor;
    }

    /**
     * @param interactor the interactor to set
     */
    protected void setInteractor(vtkRenderWindowInteractor interactor) {
        this.interactor = interactor;
    }

    /**
     * @return the interactorStyle
     */
    protected AInteractorStyleTrackballCamera getInteractorStyle() {
        return interactorStyle;
    }

    /**
     * @param interactorStyle the interactorStyle to set
     */
    protected abstract void setInteractorStyle(AInteractorStyleTrackballCamera interactorStyle);

    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    @Override
    public void keyTyped(KeyEvent e) {

    }

    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    @Override
    public void keyPressed(KeyEvent e) {

    }

    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    @Override
    public void keyReleased(KeyEvent e) {
        handleEvents(e.getKeyCode());
    }
}
