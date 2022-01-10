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
 * Jan 28, 2014
 */
package pt.lsts.neptus.vtk.visualization;

import pt.lsts.neptus.mra.importers.IMraLogGroup;
import vtk.vtkPanel;
import vtk.vtkRenderWindow;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;

/**
 * @author hfq
 *
 */
public abstract class AWindow {
    private vtkPanel panel;
    private Canvas canvas;

    private vtkRenderer renderer;
    private vtkRenderWindow renWin;
    private vtkRenderWindowInteractor renWinInteractor;

    private String windowName;
    
    private IMraLogGroup source;
    
    /**
     * @param canvas
     * @param windowName
     * @param source
     */
    public AWindow(Canvas canvas, String windowName, IMraLogGroup source) {
        this.setCanvas(canvas);
        this.setWindowName(windowName);
        this.setSource(source);
    }

    /**
     * @param canvas
     * @param windowName
     */
    public AWindow(Canvas canvas, String windowName) {
        this(canvas, windowName, null);
    }
    
    /**
     * @param canvas
     */
    public AWindow(Canvas canvas) {
        this(canvas, "", null);
    }
    
    /**
     * @param panel
     * @param windowName
     * @param source
     */
    public AWindow(vtkPanel panel, String windowName, IMraLogGroup source) {
        this.setvtkPanel(panel);
        this.setWindowName(windowName);
        this.setSource(source);
    }
    
    /**
     * @param panel
     * @param windowName
     */
    public AWindow(vtkPanel panel, String windowName) {
        this(panel, windowName, null);
    }

    /**
     * @param panel
     */
    public AWindow(vtkPanel panel) {
        this(panel, "", null);
    }
    
    /**
     * Sets up the renderer
     */
    public abstract void setUpRenderer();

    /**
     * Sets up the render Window
     */
    public abstract void setUpRenWin();

    /**
     * Sets up the renderer Window Interactors
     */
    public abstract void setUpRenWinInteractor();

    /**
     * Sets up the interactor style
     */
    public abstract void setUpInteractorStyle();


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
     * @return the panel
     */
    protected vtkPanel getvtkPanel() {
        return panel;
    }

    /**
     * @param panel the panel to set
     */
    protected void setvtkPanel(vtkPanel panel) {
        this.panel = panel;
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
    public void setRenderer(vtkRenderer renderer) {
        this.renderer = renderer;
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
     * @return the windowName
     */
    protected String getWindowName() {
        return windowName;
    }

    /**
     * @param windowName the windowName to set
     */
    protected void setWindowName(String windowName) {
        this.windowName = windowName;
    }

    /**
     * @return the source
     */
    protected IMraLogGroup getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    protected void setSource(IMraLogGroup source) {
        this.source = source;
    }
}
