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

import java.awt.Color;
import java.util.Hashtable;

import visad.SetIface;
import vtk.vtkCanvas;
import vtk.vtkCommand;
import vtk.vtkInteractorStyle;
import vtk.vtkInteractorStyleTrackballActor;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkLODActor;
import vtk.vtkLight;
import vtk.vtkLightActor;
import vtk.vtkPNGWriter;
import vtk.vtkPanel;
import vtk.vtkRenderWindow;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;
import vtk.vtkTextActor;
import vtk.vtkWindowToImageFilter;

/**
 * @author hfq
 * 
 */
public class Window {

    private vtkCommand mouseCommmand;
    private vtkCommand keyboardCommand;
    private vtkInteractorStyle style;

    private vtkWindowToImageFilter wif;
    private vtkPNGWriter pngWriter;
    private String wifName;
    
    private Hashtable<String, vtkLODActor> hashCloud;

    private vtkPanel panel;
    private vtkCanvas canvas;
    private vtkRenderer renderer;
    private vtkRenderWindow renWin;
    private vtkRenderWindowInteractor renWinInteractor;
    private String windowName;
    private vtkLight light;
    private vtkLightActor lightActor;

    vtkTextActor fpsActor = new vtkTextActor();

    /*
     * TrackballCamera style interactor for addObserver callback reference
     */
    vtkInteractorStyleTrackballCamera cstyle = new vtkInteractorStyleTrackballCamera();

    /**
     * Ideia: include snapshots with the interactor
     * @param hashCloud 
     * 
     * @param renWin
     * @param interactor
     * @param windowName
     */
    public Window(vtkPanel panel, Hashtable<String, vtkLODActor> hashCloud) {
        // a Renderer
        this.panel = panel;
        this.hashCloud = hashCloud;

        renderer = new vtkRenderer();
        setRenderer(this.panel.GetRenderer());

        // a Render Window
        setRenWin(new vtkRenderWindow());
        // an Interactor
        setRenWinInteractor(new vtkRenderWindowInteractor());
        // a style interactor
        setStyle(new vtkInteractorStyle());
        getRenWin().AddRenderer(this.panel.GetRenderer());

        setUpRenWin();
        setUpInteractorStyle();
        setUpRenWinInteractor();
    }

    /**
     * ideia include snapshots with the interactor
     * 
     * @param canvas
     * @param hashCloud 
     */
    public Window(vtkCanvas canvas, Hashtable<String, vtkLODActor> hashCloud) {
        this.canvas = new vtkCanvas();
        this.canvas = canvas;

        // a Renderer
        try {
            setRenderer(this.canvas.GetRenderer());
        }
        catch (Exception e) {
            System.out.println("exception set renderer");
            e.printStackTrace();
        }
        System.out.println("set renderer");

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
        // a style interactor
        // setStyle(new vtkInteractorStyle());
        //style = new vtkInteractorStyle();

        setUpRenderer();
        setUpRenWin();
        setUpRenWinInteractor();
        setUpInteractorStyle();
    }

    /**
     * Configures the Renderer
     */
    private void setUpRenderer() {
        renderer.SetBackground(0.1, 0.1, 0.1);
    }

    /**
     * Configures the Render Window
     */
    private void setUpRenWinInteractor() {

        try {
            getRenWinInteractor().SetRenderWindow(getRenWin());
        }
        catch (Exception e) {
            System.out.println("set render window interactor");
            e.printStackTrace();
        }
 
        double updateRate = getRenWinInteractor().GetDesiredUpdateRate();
        System.out.println("Desired update rate: " + updateRate);
    }

    /**
     * Configure the Interactor Style
     */
    private void setUpInteractorStyle() {
        try {
            NeptusInteractorStyle interactStyle = new NeptusInteractorStyle(renderer, renWinInteractor, hashCloud);
            getRenWinInteractor().SetInteractorStyle(interactStyle);
        }
        catch (Exception e) {
            System.out.println("set interact Style - Neptus");
            e.printStackTrace();
        }
    }

    /**
     * Configures the Render Window
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
     * still have to create a callback for this (keyboard event)
     */
    public void takeSnapShot() {

        wifName = "snapshot";

        wif = new vtkWindowToImageFilter();
        pngWriter = new vtkPNGWriter();

        getRenWinInteractor().FindPokedRenderer(getRenWinInteractor().GetEventPosition()[0],
                getRenWinInteractor().GetEventPosition()[1]);
        wif.SetInput(getRenWinInteractor().GetRenderWindow());
        wif.Modified(); // Update the WindowToImageFilter

        pngWriter.Modified();
        pngWriter.SetFileName(wifName);
        pngWriter.Write();
        // wifName = new String();
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
}
