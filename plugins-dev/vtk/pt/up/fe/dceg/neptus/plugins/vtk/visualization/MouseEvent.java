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
 * Jun 5, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.visualization;

import java.awt.event.InputEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.plugins.vtk.utils.Utils;
import pt.up.fe.dceg.neptus.plugins.vtk.utils.VTKMemoryManager;
import vtk.vtkCamera;
import vtk.vtkRenderWindow;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;

/**
 * @author hfq
 * 
 */
public class MouseEvent implements MouseWheelListener, MouseListener, MouseMotionListener {

    private Canvas canvas;
    private vtkRenderer renderer;
    private vtkRenderWindow renWin;
    private vtkRenderWindowInteractor interactor;
    private NeptusInteractorStyle style;
    private vtkCamera camera;

    public MouseEvent(Canvas canvas, NeptusInteractorStyle style) {
        this.canvas = canvas;
        this.renderer = canvas.GetRenderer();
        this.renWin = canvas.GetRenderWindow();
        this.interactor = canvas.getRenderWindowInteractor();
        this.style = style;
        this.camera = canvas.GetRenderer().GetActiveCamera();

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
//        style.FindPokedRenderer(interactor.GetEventPosition()[0], interactor.GetEventPosition()[1]);
//        // Zoom in
//        canvas.lock();
//        style.StartDolly();
//        camera = renderer.GetActiveCamera();
//        double zoomFactor = 10.0 * 0.2 * .5;
//        camera.Dolly(Math.pow(1.1, zoomFactor));
//        style.EndDolly();
//        canvas.unlock();
        canvas.lock();       
        camera = renderer.GetActiveCamera();
        double zoomFactor = 1.02;
        if(camera.GetParallelProjection() == 1) {
            camera.SetParallelScale(camera.GetParallelScale() / zoomFactor);
        } else {
            camera.Dolly(Math.pow(1.1, zoomFactor));
            canvas.resetCameraClippingRange();
        }
        canvas.Render();
        canvas.unlock();
    }

    /**
     * Zoom out camera
     */
    private void zoomOut() {
//        style.FindPokedRenderer(interactor.GetEventPosition()[0], interactor.GetEventPosition()[1]);
//        // zoomOut
//        canvas.lock();
//        style.StartDolly();
//        camera = renderer.GetActiveCamera();
//        double factor = 10.0 * -0.2 * .5;
//        camera.Dolly(Math.pow(1.1, factor));
//        style.EndDolly();
//        canvas.unlock();     
        canvas.lock();
        camera = renderer.GetActiveCamera();
        double zoomFactor = -1.02;
        if(camera.GetParallelProjection() == 1) {
            camera.SetParallelScale(camera.GetParallelScale() / zoomFactor);
        } else {
            camera.Dolly(Math.pow(1.1, zoomFactor));
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
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseMoved(java.awt.event.MouseEvent e) { // syncronized?!?!
        // NeptusLog.pub().info("mouse moved");

        canvas.setLastX(e.getX());
        canvas.setLastY(e.getY());

        canvas.setCtrlPressed((e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK ? 1 : 0);
        canvas.setShiftPressed((e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK ? 1 : 0);

        interactor.SetEventInformationFlipY(e.getX(), e.getY(), canvas.getCtrlPressed(), canvas.getShiftPressed(), '0',
                0, "0");

        canvas.lock();
        interactor.MouseMoveEvent();
        canvas.unlock();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    @Override
    public void mousePressed(java.awt.event.MouseEvent e) {

        if (renderer.VisibleActorCount() == 0)
            return;

        // NeptusLog.pub().info("mouse pressed");

        canvas.lock();
        renWin.SetDesiredUpdateRate(5.0);
        canvas.setLastX(e.getX());
        canvas.setLastY(e.getY());

        canvas.setCtrlPressed((e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK ? 1 : 0);

        canvas.setShiftPressed((e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK ? 1 : 0);

        interactor.SetEventInformationFlipY(e.getX(), e.getY(), canvas.getCtrlPressed(), canvas.getShiftPressed(), '0',
                0, "0");

        if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK)
            interactor.LeftButtonPressEvent();
        else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK)
            interactor.RightButtonPressEvent();
        else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK)
            interactor.MiddleButtonPressEvent();

        canvas.unlock();
        // VTKMemoryManager.GC.SetAutoGarbageCollection(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseReleased(final java.awt.event.MouseEvent e) {
        // NeptusLog.pub().info("mouse released");

         renWin.SetDesiredUpdateRate(0.01);
        
         canvas.setCtrlPressed((e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK ? 1 : 0);
         canvas.setShiftPressed((e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK ? 1 : 0);
        
         interactor.SetEventInformationFlipY(e.getX(), e.getY(),
         canvas.getCtrlPressed(), canvas.getShiftPressed(), '0', 0, "0");
        
         if ((e.getModifiers() & InputEvent.BUTTON1_MASK) ==
         InputEvent.BUTTON1_MASK) {
         canvas.lock();
         interactor.LeftButtonReleaseEvent();
         canvas.unlock();
         }
        
         if ((e.getModifiers() & InputEvent.BUTTON2_MASK) ==
         InputEvent.BUTTON2_MASK) {
         canvas.lock();
         interactor.RightButtonReleaseEvent();
         canvas.unlock();
         }
        
         if ((e.getModifiers() & InputEvent.BUTTON3_MASK) ==
         InputEvent.BUTTON3_MASK) {
         canvas.lock();
         interactor.MiddleButtonReleaseEvent();
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
        // TODO Auto-generated method stub

    }
}
