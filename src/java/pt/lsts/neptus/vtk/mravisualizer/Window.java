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
 * Jan 28, 2014
 */
package pt.lsts.neptus.vtk.mravisualizer;

import java.util.LinkedHashMap;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.vtk.pointcloud.APointCloud;
import pt.lsts.neptus.vtk.surface.PointCloudMesh;
import pt.lsts.neptus.vtk.visualization.AWindow;
import pt.lsts.neptus.vtk.visualization.Canvas;
import vtk.vtkPanel;

/**
 * @author hfq
 * 
 */
public class Window extends AWindow {

    // the Neptus interactor Style - mouse, and keyboard events
    private InteractorStyleVis3D interacStyle;
    private LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud;
    private LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh;

    public Window(vtkPanel panel, InteractorStyleVis3D interactorStyle,
            LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud,
            LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh,
            IMraLogGroup source, String windowName) {
        super(panel, windowName);

        setRenderer(panel.GetRenderer());
        setRenWin(panel.GetRenderWindow());
        setInteracStyle(interactorStyle);
        // setRenWinInteractor(getNeptusInteracStyle());
        setRenWinInteractor(panel.GetRenderWindow().GetInteractor());
        setLinkedHashMapCloud(linkedHashMapCloud);
        setLinkedHashMapMesh(linkedHashMapMesh);
        this.setSource(source);

        setUpRenderer();
        setUpRenWin();
        setUpRenWinInteractor();
        setUpInteractorStyle();
    }

    public Window(vtkPanel panel, InteractorStyleVis3D interactorStyle,
            LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud,
            LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh, IMraLogGroup source) {
        this(panel, interactorStyle, linkedHashMapCloud, linkedHashMapMesh,
                source, I18n.text("Visualizer3D"));
    }

    public Window(Canvas canvas, InteractorStyleVis3D interactorStyle,
            LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud,
            LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh,
            IMraLogGroup source, String windowName) {
        super(canvas, windowName);

        setRenderer(canvas.GetRenderer());
        setRenWin(canvas.GetRenderWindow());
        setInteracStyle(interactorStyle);
        setRenWinInteractor(canvas.getRenderWindowInteractor());
        setLinkedHashMapCloud(linkedHashMapCloud);
        setLinkedHashMapMesh(linkedHashMapMesh);
        this.setSource(source);

        setUpRenderer();
        setUpRenWin();
        setUpRenWinInteractor();
        setUpInteractorStyle();
    }

    public Window(Canvas canvas, InteractorStyleVis3D interactorStyle,
            LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud,
            LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh, IMraLogGroup source) {
        this(canvas, interactorStyle, linkedHashMapCloud, linkedHashMapMesh, source, I18n.text("Visualizer3D"));
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
        setInteracStyle(new InteractorStyleVis3D(getCanvas(), getRenderer(), getRenWinInteractor(),
                getLinkedHashMapCloud(), getLinkedHashMapMesh(), getSource()));
        getRenWinInteractor().SetInteractorStyle(getInteracStyle());
    }

    /**
     * @return the neptusInteracStyle
     */
    public InteractorStyleVis3D getInteracStyle() {
        return interacStyle;
    }

    /**
     * @param neptusInteracStyle the neptusInteracStyle to set
     */
    private void setInteracStyle(InteractorStyleVis3D interacStyle) {
        this.interacStyle = interacStyle;
    }

    /**
     * @return the linkedHashMapCloud
     */
    public LinkedHashMap<String, APointCloud<?>> getLinkedHashMapCloud() {
        return linkedHashMapCloud;
    }

    /**
     * @param linkedHashMapCloud the linkedHashMapCloud to set
     */
    private void setLinkedHashMapCloud(LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud) {
        this.linkedHashMapCloud = linkedHashMapCloud;
    }

    /**
     * @return the linkedHashMapMesh
     */
    public LinkedHashMap<String, PointCloudMesh> getLinkedHashMapMesh() {
        return linkedHashMapMesh;
    }

    /**
     * @param linkedHashMapMesh the linkedHashMapMesh to set
     */
    private void setLinkedHashMapMesh(LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh) {
        this.linkedHashMapMesh = linkedHashMapMesh;
    }

}
