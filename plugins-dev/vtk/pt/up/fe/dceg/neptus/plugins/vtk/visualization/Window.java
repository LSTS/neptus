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
 * Apr 11, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.visualization;

import java.util.LinkedHashMap;

import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import vtk.vtkCanvas;
import vtk.vtkInteractorStyle;
import vtk.vtkPanel;
import vtk.vtkRenderWindow;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;

/**
 * @author hfq
 * config vtk window
 */
public class Window {
    private vtkInteractorStyle style;

    public LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud;
      
    private vtkPanel panel;
    private vtkCanvas canvas;
    private vtkRenderer renderer;
    private vtkRenderWindow renWin;
    private vtkRenderWindowInteractor renWinInteractor;
    private String windowName;
    //private vtkLight light;
    //private vtkLightActor lightActor;
    
        // the Neptus interactor Style - mouse, and keyboard events
    private NeptusInteractorStyle interactorStyle;

    /**
     * 
     * @param panel
     * @param linkedHashMapCloud
     * set all vtk render components of a vtkPanel
     */
    public Window(vtkPanel panel, LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud) {
        this.panel = new vtkPanel();
        this.panel = panel;
        this.linkedHashMapCloud = linkedHashMapCloud;
        
            // a Renderer
        try {
            setRenderer(this.panel.GetRenderer());
        }
        catch (Exception e) {
            System.out.println("exception set renderer");
            e.printStackTrace();
        }

            // a Render Window
        try {
            setRenWin(this.panel.GetRenderWindow());
        }
        catch (Exception e) {
            System.out.println("exception set render window");
            e.printStackTrace();
        }

            // a Render Window Interactor
        try {
            setRenWinInteractor(this.panel.GetRenderWindow().GetInteractor());
        }
        catch (Exception e) {
            System.out.println("exception set render window interactor");
            e.printStackTrace();
        }

        setUpRenderer();
        setUpRenWin();
        setUpRenWinInteractor();
        setUpInteractorStyle();
            
            // set up camera to +z viewpoint looking down
        getRenderer().GetActiveCamera().SetViewUp(0.0, 0.0, -1.0);
        getRenderer().Render();
    }

    /**
     * @param canvas
     * @param hashCloud
     * set all vtk render components of a vtkCanvas 
     */
    public Window(vtkCanvas canvas, LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud) {
        this.canvas = new vtkCanvas();
        this.canvas = canvas;
        this.linkedHashMapCloud = linkedHashMapCloud;

            // a Renderer
        try {
            setRenderer(this.canvas.GetRenderer());
        }
        catch (Exception e) {
            System.out.println("exception set renderer");
            e.printStackTrace();
        }

            // a Render Window
        try {
            setRenWin(this.canvas.GetRenderWindow());
        }
        catch (Exception e) {
            System.out.println("exception set render window");
            e.printStackTrace();
        }

            // a Render Window Interactor
        try {
            setRenWinInteractor(this.canvas.getRenderWindowInteractor());
        }
        catch (Exception e) {
            System.out.println("exception set render window interactor");
            e.printStackTrace();
        }

        setUpRenderer();
        setUpRenWin();
        setUpRenWinInteractor();
        setUpInteractorStyle();
    }

    /**
     * Sets up the Renderer
     */
    private void setUpRenderer() {
        try {
            renderer.SetGradientBackground(true);
            renderer.SetBackground(0.0, 0.0, 0.0);
            renderer.SetBackground2(0.3, 0.7, 1.0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sets up the Render Window
     */
    private void setUpRenWin() {
        try {
            windowName = "viewportNeptus";
            getRenWin().SetWindowName(windowName);
            getRenWin().AlphaBitPlanesOff();
            getRenWin().PointSmoothingOff();
            getRenWin().LineSmoothingOff();
            getRenWin().PointSmoothingOff();
            getRenWin().SwapBuffersOn();
            getRenWin().SetStereoTypeToAnaglyph();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the Render Window Interactor
     */
    private void setUpRenWinInteractor() {
        try {
            getRenWinInteractor().SetRenderWindow(getRenWin());
        }
        catch (Exception e) {
            System.out.println("set render window interactor");
            e.printStackTrace();
        }
        
        getRenWinInteractor().SetDesiredUpdateRate(30.0);
        
        double updateRate = getRenWinInteractor().GetDesiredUpdateRate();
        System.out.println("Desired update rate: " + updateRate);
    }

    /**
     * Sets up the Interactor Style
     */
    private void setUpInteractorStyle() {
        try {
            interactorStyle = new NeptusInteractorStyle(canvas, renderer, renWinInteractor, linkedHashMapCloud);
            getRenWinInteractor().SetInteractorStyle(interactorStyle);
        }
        catch (Exception e) {
            System.out.println("set interact Style - Neptus");
            e.printStackTrace();
        }
    }

    /**
     * @return the interactor style
     */
    public vtkInteractorStyle getStyle() {
        return style;
    }

    /**
     * @param style the style to set
     */
    private void setStyle(vtkInteractorStyle style) {
        this.style = style;
    }

    /**
     * @return the renWin
     */
    public vtkRenderWindow getRenWin() {
        return renWin;
    }

    /**
     * @param renWin the renWin to set
     */
    private void setRenWin(vtkRenderWindow renWin) {
        this.renWin = renWin;
    }

    /**
     * @return the renWinInteractor
     */
    public vtkRenderWindowInteractor getRenWinInteractor() {
        return renWinInteractor;
    }

    /**
     * @param renWinInteractor the renWinInteractor to set
     */
    private void setRenWinInteractor(vtkRenderWindowInteractor renWinInteractor) {
        this.renWinInteractor = renWinInteractor;
    }

    /**
     * @return the renderer
     */
    public vtkRenderer getRenderer() {
        return renderer;
    }

    /**
     * @param renderer the renderer to set
     */
    private void setRenderer(vtkRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * @return the interactorStyle
     */
    public NeptusInteractorStyle getInteractorStyle() {
        return interactorStyle;
    }

    /**
     * @param interactorStyle the interactorStyle to set
     */
    private void setInteractorStyle(NeptusInteractorStyle interactorStyle) {
        this.interactorStyle = interactorStyle;
    }
}
