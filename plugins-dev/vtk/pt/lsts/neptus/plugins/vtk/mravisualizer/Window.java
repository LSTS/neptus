/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Jan 28, 2014
 */
package pt.lsts.neptus.plugins.vtk.mravisualizer;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.vtk.visualization.Canvas;
import pt.lsts.neptus.plugins.vtk.visualization.IWindow;
import vtk.vtkPanel;
import vtk.vtkRenderWindow;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;

/**
 * @author hfq
 *
 */
public class Window implements IWindow {

    private vtkPanel panel;
    private Canvas canvas;
    private vtkRenderer renderer;
    private vtkRenderWindow renWin;
    private vtkRenderWindowInteractor renWinInteractor;

    private EventsHandler events;

    private String windowName;


    // private vtkLight light;
    // private vtkLightActor lightActor;

    // the Neptus interactor Style - mouse, and keyboard events
    private NeptusInteractorStyle neptusInteracStyle;

    public Window(vtkPanel panel, NeptusInteractorStyle neptusInteractorStyle, EventsHandler events, String windowName) {
        setPanel(panel);
        this.windowName = windowName;

        setRenderer(panel.GetRenderer());
        setRenWin(panel.GetRenderWindow());
        setNeptusInteracStyle(neptusInteractorStyle);
        //setRenWinInteractor(getNeptusInteracStyle());
        setRenWinInteractor(panel.GetRenderWindow().GetInteractor());

        setUpRenderer();
        setUpRenWin();
        setUpRenWinInteractor();
        setUpInteractorStyle();
    }

    public Window(vtkPanel panel, NeptusInteractorStyle neptusInteractorStyle, EventsHandler events) {
        this(panel, neptusInteractorStyle, events, I18n.text("Visualizer"));
    }

    public Window(Canvas canvas, NeptusInteractorStyle neptusInteractorStyle, EventsHandler events, String windowName) {
        setCanvas(canvas);
        this.windowName = windowName;
        setRenderer(canvas.GetRenderer());
        setRenWin(canvas.GetRenderWindow());
        setNeptusInteracStyle(neptusInteractorStyle);
        setRenWinInteractor(canvas.getRenderWindowInteractor());

        setUpRenderer();
        setUpRenWin();
        setUpRenWinInteractor();
        setUpInteractorStyle();
    }

    public Window(Canvas canvas, NeptusInteractorStyle neptusInteractorStyle, EventsHandler events) {
        this(canvas, neptusInteractorStyle, events, "Visualizer");
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.visualization.IWindow#setUpRenderer()
     */
    @Override
    public void setUpRenderer() {
        renderer.SetGradientBackground(true);
        renderer.SetBackground(0.0, 0.0, 0.0);
        renderer.SetBackground2(0.3, 0.7, 1.0);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.visualization.IWindow#setUpRenWin()
     */
    @Override
    public void setUpRenWin() {
        renWin.SetWindowName(windowName);
        renWin.AlphaBitPlanesOff();
        renWin.PointSmoothingOff();
        renWin.LineSmoothingOff();
        renWin.SwapBuffersOn();
        renWin.SetStereoTypeToAnaglyph();        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.visualization.IWindow#setUpRenWinInteractor()
     */
    @Override
    public void setUpRenWinInteractor() {
        renWinInteractor.SetRenderWindow(renWin);
        renWinInteractor.SetDesiredUpdateRate(30.0);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.visualization.IWindow#setUpInteractorStyle()
     */
    @Override
    public void setUpInteractorStyle() {
        setNeptusInteracStyle(new NeptusInteractorStyle(canvas, renderer, renWinInteractor, events));
        renWinInteractor.SetInteractorStyle(getNeptusInteracStyle());
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.visualization.IWindow#getRenWin()
     */
    @Override
    public vtkRenderWindow getRenWin() {
        return renWin;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.vtk.visualization.IWindow#getRenderer()
     */
    @Override
    public vtkRenderer getRenderer() {
        return renderer;
    }

    /**
     * @return the panel
     */
    public vtkPanel getPanel() {
        return panel;
    }

    /**
     * @param panel the panel to set
     */
    public void setPanel(vtkPanel panel) {
        this.panel = panel;
    }

    /**
     * @return the canvas
     */
    public Canvas getCanvas() {
        return canvas;
    }

    /**
     * @param canvas the canvas to set
     */
    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    /**
     * @param renderer the renderer to set
     */
    public void setRenderer(vtkRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * @param renWin the renWin to set
     */
    public void setRenWin(vtkRenderWindow renWin) {
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
    public void setRenWinInteractor(vtkRenderWindowInteractor renWinInteractor) {
        this.renWinInteractor = renWinInteractor;
    }

    /**
     * @return the neptusInteracStyle
     */
    public NeptusInteractorStyle getNeptusInteracStyle() {
        return neptusInteracStyle;
    }

    /**
     * @param neptusInteracStyle the neptusInteracStyle to set
     */
    private void setNeptusInteracStyle(NeptusInteractorStyle neptusInteracStyle) {
        this.neptusInteracStyle = neptusInteracStyle;
    }

}
