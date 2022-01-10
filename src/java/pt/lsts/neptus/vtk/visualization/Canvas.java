/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 4, 2013
 */
package pt.lsts.neptus.vtk.visualization;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.SwingUtilities;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.vtk.utils.Utils;
import vtk.vtkActorCollection;
import vtk.vtkCanvas;
import vtk.vtkMapper;
import vtk.vtkUnsignedCharArray;

/**
 * @author hfq
 * Overrides some functionalities of vtkCanvas (vtkCanvas extends vtkPanel)
 */
public class Canvas extends vtkCanvas {

    private static final long serialVersionUID = 5165188310777500794L;

    // for 2D Graphics
    private vtkUnsignedCharArray buffer = new vtkUnsignedCharArray();
    private int bufferWidth, bufferHeight = 0;

    /**
     * Constructor
     */
    public Canvas() {
        super();
        setMinimumSize(new Dimension(0, 0));
        setPreferredSize(new Dimension(0, 0));
    }

    /**
     * Override to correct the bug of the UpdateLight (the light position is not updated if the
     * camera is moved by programming
     */
    public void RenderSecured() {
        if (!isWindowSet())
            return;
        Utils.goToAWTThread(new Runnable() {

            @Override
            public void run() {
                Render();     
            }
        });
    }

    //public void Render() {
    @Override
    public synchronized void Render() {
        if (!rendering)
        {
            rendering = true;
            // if there's no visible actor to render
            if (ren.VisibleActorCount() == 0)
            {
                rendering = false;
                return;
            }
            if (rw != null)
            {
                if (windowset == 0)
                {
                    // set the window id and the active camera
                    if (lightingset == 0)
                    {
                        ren.AddLight(lgt);
                        lightingset = 1;
                    }
                    RenderCreate(rw);
                    Lock();
                    rw.SetSize(getWidth(), getHeight());
                    UnLock();
                    windowset = 1;
                }
                UpdateLight();
                Lock();
                rw.Render();
                UnLock();
                rendering = false;
            }
        }
    }

    @Override
    public void lock() {
        if (isWindowSet())
            super.lock();
    }

    @Override
    public void unlock() {
        if (isWindowSet())
            super.unlock();
    }

    /**
     * Set the immediateRenderingMode on the current view
     * @param mode
     */
    public void setImmediateRenderingMode(boolean mode) {
        vtkMapper mapper;
        vtkActorCollection listOfActors = GetRenderer().GetActors();
        int nbActors = listOfActors.GetNumberOfItems();

        listOfActors.InitTraversal();
        for (int i = 0; i < nbActors; ++i) {
            // browing the list of actores and getting their associated mappers
            mapper = listOfActors.GetNextActor().GetMapper();
            mapper.SetImmediateModeRendering(Utils.booleanToInt(mode));
        }
    }

    /**
     * Corrects a bug : update the reference of camera
     * Changing the original camera the light stops following the camera
     */
    @Override
    public void UpdateLight() {
        if(LightFollowCamera == 0)
            return;

        cam = GetRenderer().GetActiveCamera();
        super.UpdateLight();
    }

    /**
     * must be performed on awt event thread
     */
    @Override
    public void Report() {
        Runnable updateAComponent = new Runnable() {

            @Override
            public void run() {
                lock();
                NeptusLog.pub().info("direct rendering = " + (GetRenderWindow().IsDirect() == 1));
                NeptusLog.pub().info("opengl supported = " + (GetRenderWindow().SupportsOpenGL() == 1));
                NeptusLog.pub().info("report = " + GetRenderWindow().ReportCapabilities());
                unlock();
            }
        };

        SwingUtilities.invokeLater(updateAComponent);
    }

    @Override
    public void paint(Graphics g) {
        if (windowset == 0 || bufferWidth != getWidth() || bufferHeight != getHeight()) {
            Render();
        }
        else {
            lock();
            rw.SetPixelData(0, 0, getWidth()-1, getHeight()-1, buffer, 1);
            unlock();
        }
    }

    public int getCtrlPressed() {
        return ctrlPressed;
    }

    public void setCtrlPressed(int ctrlPressed) {
        this.ctrlPressed = ctrlPressed;
    }

    public int getShiftPressed() {
        return shiftPressed;
    }

    public void setShiftPressed(int shiftPressed) {
        this.shiftPressed = shiftPressed;
    }

    public int getLastX() {
        return lastX;
    }

    public void setLastX(int lastX) {
        this.lastX = lastX;
    }

    public int getLastY() {
        return lastY;
    }

    public void setLastY(int lastY) {
        this.lastY = lastX;
    }
}
