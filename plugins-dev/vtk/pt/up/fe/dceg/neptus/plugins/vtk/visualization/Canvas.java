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
 * Jun 4, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.visualization;

import java.awt.Dimension;

import pt.up.fe.dceg.neptus.plugins.vtk.utils.Utils;

import vtk.vtkCanvas;
import vtk.vtkUnsignedCharArray;

/**
 * @author hfq
 * Overrides some functionalities of vtkCanvas (vtkCanvas extends vtkPanel)
 */
public class Canvas extends vtkCanvas {
    
    /**
     * 
     */
    private static final long serialVersionUID = 5165188310777500794L;
    
    private vtkUnsignedCharArray buffer = new vtkUnsignedCharArray();
    private int bufferWidth, bufferHeight = 0;

    public Canvas() {
        setMinimumSize(new Dimension(0, 0));
        setPreferredSize(new Dimension(0, 0));
    }
    
    /**
     * Correct a bug : update the reference of camera
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
    
    @Override
    public void Render() {
        if (!rendering)
        {
            rendering = true;
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

}
