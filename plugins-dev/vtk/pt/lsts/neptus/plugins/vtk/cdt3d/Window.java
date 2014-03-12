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
 * Mar 10, 2014
 */
package pt.lsts.neptus.plugins.vtk.cdt3d;

import pt.lsts.neptus.plugins.vtk.visualization.Canvas;
import pt.lsts.neptus.plugins.vtk.visualization.IWindow;
import vtk.vtkRenderWindow;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;

/**
 * @author hfq
 *
 */
public class Window implements IWindow {

    private Canvas canvas;
    private String windowName;

    private vtkRenderer renderer;
    private vtkRenderWindow renWin;
    private vtkRenderWindowInteractor renWinInteractor;

    private InteractorStyle interactorStyle;

    /**
     * 
     */
    public Window(Canvas canvas) {
        this(canvas, "CTD3D");
    }

    public Window(Canvas canvas, String windowName) {
        this.canvas = canvas;
        this.windowName = windowName;

        this.renderer = canvas.GetRenderer();
        this.renWin = canvas.GetRenderWindow();
        this.renWinInteractor = canvas.getRenderWindowInteractor();

        setUpRenderer();
        setUpRenWin();
        setUpRenWinInteractor();
        setUpInteractorStyle();
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
        interactorStyle = new InteractorStyle(canvas, renderer, renWinInteractor);
        renWinInteractor.SetInteractorStyle(interactorStyle);
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

    public Canvas getCanvas() {
        return canvas;
    }

    public InteractorStyle getInteractorStyle() {
        return interactorStyle;
    }
}
