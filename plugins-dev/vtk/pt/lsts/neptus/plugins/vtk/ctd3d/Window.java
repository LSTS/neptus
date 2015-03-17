/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
package pt.lsts.neptus.plugins.vtk.ctd3d;

import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.vtk.visualization.AWindow;
import pt.lsts.neptus.plugins.vtk.visualization.Canvas;

/**
 * @author hfq
 * 
 */
public class Window extends AWindow {

    private InteractorStyleCTD3D interactorStyle;

    /**
     * @param canvas
     * @param source
     */
    public Window(Canvas canvas, IMraLogGroup source) {
        this(canvas, source, "CTD3D");
    }

    /**
     * @param canvas
     * @param source
     * @param windowName
     */
    public Window(Canvas canvas, IMraLogGroup source, String windowName) {
        super(canvas, windowName, source);

        setRenderer(canvas.GetRenderer());
        setRenWin(canvas.GetRenderWindow());
        setRenWinInteractor(canvas.getRenderWindowInteractor());

        setUpRenderer();
        setUpRenWin();
        setUpRenWinInteractor();
        setUpInteractorStyle();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.vtk.visualization.AWindow#setUpRenderer()
     */
    @Override
    public void setUpRenderer() {
        getRenderer().SetGradientBackground(true);
        getRenderer().SetBackground(0.0, 0.0, 0.0);
        getRenderer().SetBackground2(0.3, 0.7, 1.0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.vtk.visualization.AWindow#setUpRenWin()
     */
    @Override
    public void setUpRenWin() {
        getRenWin().SetWindowName(getWindowName());
        getRenWin().AlphaBitPlanesOff();
        getRenWin().PointSmoothingOff();
        getRenWin().LineSmoothingOff();
        getRenWin().SwapBuffersOn();
        getRenWin().SetStereoTypeToAnaglyph();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.vtk.visualization.AWindow#setUpRenWinInteractor()
     */
    @Override
    public void setUpRenWinInteractor() {
        getRenWinInteractor().SetRenderWindow(getRenWin());
        getRenWinInteractor().SetDesiredUpdateRate(30.0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.vtk.visualization.AWindow#setUpInteractorStyle()
     */
    @Override
    public void setUpInteractorStyle() {
        interactorStyle = new InteractorStyleCTD3D(getCanvas(), getRenderer(), getRenWinInteractor(), getSource());
        getRenWinInteractor().SetInteractorStyle(interactorStyle);
    }

    public InteractorStyleCTD3D getInteractorStyle() {
        return interactorStyle;
    }

}
