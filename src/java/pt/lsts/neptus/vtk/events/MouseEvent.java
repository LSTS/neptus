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
 * Jun 5, 2013
 */
package pt.lsts.neptus.vtk.events;

import java.awt.event.InputEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import pt.lsts.neptus.vtk.visualization.Canvas;
import vtk.vtkCamera;

/**
 * @author hfq
 * 
 */
public class MouseEvent implements MouseWheelListener, MouseListener, MouseMotionListener {

    private final Canvas canvas;
    private final vtkCamera camera;
    private final PointPickingEvent pointPickingEvent;

    /**
     * 
     * @param canvas
     * @param pointPickingEvent
     */
    public MouseEvent(Canvas canvas, PointPickingEvent pointPickingEvent) {
        this.canvas = canvas;
        this.camera = canvas.GetRenderer().GetActiveCamera();
        this.pointPickingEvent = pointPickingEvent;

        canvas.addMouseWheelListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int notches = e.getWheelRotation();
        if (notches < 0) {
            zoomIn();
        }
        else {
            zoomOut();
        }
    }

    /**
     * Zoom In Camera
     */
    private void zoomIn() {
        canvas.lock();
        double zoomFactor = 1.02;
        if (camera.GetParallelProjection() == 1) {
            camera.SetParallelScale(camera.GetParallelScale() / zoomFactor);
        }
        else {
            canvas.GetRenderer().GetActiveCamera().Dolly(Math.pow(1.1, zoomFactor));
            canvas.resetCameraClippingRange();
        }
        canvas.Render();
        canvas.unlock();
    }

    /**
     * Zoom out camera
     */
    private void zoomOut() {
        canvas.lock();
        double zoomFactor = -1.02;
        if (camera.GetParallelProjection() == 1) {
            camera.SetParallelScale(camera.GetParallelScale() / zoomFactor);
        }
        else {
            canvas.GetRenderer().GetActiveCamera().Dolly(Math.pow(1.1, zoomFactor));
            canvas.resetCameraClippingRange();
        }
        canvas.Render();
        canvas.unlock();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseDragged(java.awt.event.MouseEvent e) {
        if (canvas.GetRenderer().VisibleActorCount() == 0)
            return;

        canvas.setCtrlPressed((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK ? 1 : 0);
        canvas.setShiftPressed((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK ? 1 : 0);

        canvas.getRenderWindowInteractor().SetEventInformationFlipY(e.getX(), e.getY(), canvas.getCtrlPressed(),
                canvas.getShiftPressed(), '0', 0, "0");

        canvas.lock();
        canvas.getRenderWindowInteractor().MouseMoveEvent();
        canvas.unlock();

        canvas.UpdateLight();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseMoved(java.awt.event.MouseEvent e) { // synchronized?!?!
        canvas.setLastX(e.getX());
        canvas.setLastY(e.getY());

        canvas.setCtrlPressed((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK ? 1 : 0);
        canvas.setShiftPressed((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK ? 1 : 0);

        canvas.getRenderWindowInteractor().SetEventInformationFlipY(e.getX(), e.getY(), canvas.getCtrlPressed(),
                canvas.getShiftPressed(), '0', 0, "0");

        canvas.lock();
        canvas.getRenderWindowInteractor().MouseMoveEvent();
        canvas.unlock();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    @Override
    public void mousePressed(java.awt.event.MouseEvent e) {

        if (canvas.GetRenderer().VisibleActorCount() == 0)
            return;

        canvas.lock();
        canvas.GetRenderWindow().SetDesiredUpdateRate(5.0);
        canvas.setLastX(e.getX());
        canvas.setLastY(e.getY());

        canvas.setCtrlPressed((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK ? 1 : 0);

        canvas.setShiftPressed((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK ? 1 : 0);

        canvas.getRenderWindowInteractor().SetEventInformationFlipY(e.getX(), e.getY(), canvas.getCtrlPressed(),
                canvas.getShiftPressed(), '0', 0, "0");

        if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK)
            if (canvas.getCtrlPressed() == 0)
                canvas.getRenderWindowInteractor().LeftButtonPressEvent();
            else {
                pointPickingEvent.execute(e, e.getID());
            }
        else if ((e.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) == InputEvent.BUTTON2_DOWN_MASK)
            canvas.getRenderWindowInteractor().RightButtonPressEvent();
        else if ((e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) == InputEvent.BUTTON3_DOWN_MASK)
            canvas.getRenderWindowInteractor().MiddleButtonPressEvent();

        canvas.unlock();
        // VTKMemoryManager.GC.SetAutoGarbageCollection(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseReleased(java.awt.event.MouseEvent e) {
        canvas.GetRenderWindow().SetDesiredUpdateRate(0.01);

        canvas.setCtrlPressed((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK ? 1 : 0);
        canvas.setShiftPressed((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK ? 1 : 0);

        canvas.getRenderWindowInteractor().SetEventInformationFlipY(e.getX(), e.getY(), canvas.getCtrlPressed(),
                canvas.getShiftPressed(), '0', 0, "0");

        if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK) {
            canvas.lock();
            canvas.getRenderWindowInteractor().LeftButtonReleaseEvent();
            canvas.unlock();
        }

        if ((e.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) == InputEvent.BUTTON2_DOWN_MASK) {
            canvas.lock();
            canvas.getRenderWindowInteractor().RightButtonReleaseEvent();
            canvas.unlock();
        }

        if ((e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) == InputEvent.BUTTON3_DOWN_MASK) {
            canvas.lock();
            canvas.getRenderWindowInteractor().MiddleButtonReleaseEvent();
            canvas.unlock();
        }
        // VTKMemoryManager.GC.SetAutoGarbageCollection(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseEntered(java.awt.event.MouseEvent e) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseExited(java.awt.event.MouseEvent e) {

    }
}
