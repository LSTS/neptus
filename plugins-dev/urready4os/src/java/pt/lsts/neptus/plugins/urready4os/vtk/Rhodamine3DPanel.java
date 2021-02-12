/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 12/06/2015
 */
package pt.lsts.neptus.plugins.urready4os.vtk;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.urready4os.vtk.window.InteractorStyleRhod3D;
import pt.lsts.neptus.plugins.urready4os.vtk.window.WindowRhod3D;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.vtk.utils.Utils;
import pt.lsts.neptus.vtk.visualization.AxesWidget;
import pt.lsts.neptus.vtk.visualization.Canvas;
import pt.lsts.neptus.vtk.visualization.ScalarBar;


/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class Rhodamine3DPanel extends JPanel {

    private Canvas canvas;
    private WindowRhod3D winCanvas;
    protected InteractorStyleRhod3D interactorStyle;

    private Rhodamine3DToolbar toolbar;

    public PointCloudRhodamine pointcloud;
    private PointCloudRhodamine pointcloudPrediction;

    // private vtkScalarBarActor lutActor = new vtkScalarBarActor();
    public ScalarBar scalarBar;

    private boolean isFirstRender = true;

    private double[] useRange = null;
    
    public Rhodamine3DPanel() {
        if (!Utils.hasTryedToLoadVtkLib) {
            Utils.loadVTKLibraries();
            // VTKMemoryManager.GC.SetAutoGarbageCollection(true);
        }
        initialize();
    }
    
    private void initialize() {
        setLayout(new BorderLayout());

        canvas = new Canvas();
        canvas.LightFollowCameraOn();
        canvas.GetRenderer().AutomaticLightCreationOn();
        canvas.setEnabled(true);

        winCanvas = new WindowRhod3D(canvas, null /*source*/);
        interactorStyle = winCanvas.getInteractorStyle();

//        loadData();

//        pointcloud.createActorFromPoints();
//        pointcloud.generateHandler();
//        pointcloud.getPolyData().GetPointData().SetScalars(((PointCloudHandlerRhodamineDye) pointcloud.getColorHandler()).getColorsRhodamineDye());
//        canvas.GetRenderer().AddActor(pointcloud.getCloudLODActor());

//        scalarBar = new ScalarBar(I18n.text("Rhodamine Dye Color Map"));
//        scalarBar.setScalarBarHorizontalProperties();
//        scalarBar.setUpScalarBarLookupTable(((PointCloudHandlerRhodamineDye) pointcloud.getColorHandler()).getLutRhodamineDye());
//        scalarBar.getScalarBarActor().Modified();
////        setScalarBar(scalarBar);
//        canvas.GetRenderer().AddActor(scalarBar.getScalarBarActor());

        AxesWidget axesWidget = new AxesWidget(canvas.getRenderWindowInteractor());
        axesWidget.createAxesWidget();

        toolbar = new Rhodamine3DToolbar(this);
        toolbar.createtoolBar();

        add(toolbar, BorderLayout.WEST);
        add(canvas);

        toolbar.getRhodToggle().setSelected(true);
        toolbar.getPredToggle().setSelected(true);

        reloadCanvas();

    }
    
    /* (non-Javadoc)
     * @see javax.swing.JComponent#setVisible(boolean)
     */
    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (aFlag && isFirstRender) {
//            canvas.lock();
//
//            canvas.GetRenderer().GetActiveCamera().SetPosition(1.0, -1.0, -100.0);
//            canvas.GetRenderer().GetActiveCamera().SetViewUp(0.0, 1.0, -1.0);
//
//            canvas.GetRenderer().ResetCamera();
//            canvas.RenderSecured();
//
//            canvas.unlock();

            isFirstRender = false;
        }
    }
    
    /**
     * @return the canvas
     */
    public Canvas getCanvas() {
        return canvas;
    }

    /**
     * @return the pointcloud
     */
    public PointCloudRhodamine getPointcloud() {
        return pointcloud;
    }
    
    /**
     * @return the pointcloudPrediction
     */
    public PointCloudRhodamine getPointcloudPrediction() {
        return pointcloudPrediction;
    }
    
    /**
     * @return the scalarBar
     */
    public ScalarBar getScalarBar() {
        return scalarBar;
    }
    
    /**
     * @return the useRange
     */
    public double[] getUseRange() {
        return useRange;
    }

    /**
     * @param useRange the useRange to set
     */
    public void setUseRange(double[] useRange) {
        this.useRange = useRange;
    }
    
    public void updatePointCloud(PointCloudRhodamine newPointcloudRhodamine,
            PointCloudRhodamine newPointcloudPrevision) {
        if (newPointcloudRhodamine == null || newPointcloudPrevision == null)
            return;
        
        if (pointcloud != null)
            canvas.GetRenderer().RemoveActor(pointcloud.getCloudLODActor());
        if (pointcloudPrediction != null)
            canvas.GetRenderer().RemoveActor(pointcloudPrediction.getCloudLODActor());
        if (scalarBar != null)
            canvas.GetRenderer().RemoveActor(scalarBar.getScalarBarActor());

        pointcloud = newPointcloudRhodamine;
        pointcloud.createActorFromPoints();
        pointcloud.setUseRange(useRange); // FIXME
        pointcloud.generateHandler();
        pointcloud.getPolyData().GetPointData().SetScalars(((PointCloudHandlerRhodamineDye) pointcloud.getColorHandler()).getColorsRhodamineDye());
        canvas.GetRenderer().AddActor(pointcloud.getCloudLODActor());

        pointcloudPrediction = newPointcloudPrevision;
        pointcloudPrediction.createActorFromPoints();
        pointcloudPrediction.setUseRange(useRange); // FIXME
        pointcloudPrediction.generateHandler();
        pointcloudPrediction.getPolyData().GetPointData().SetScalars(((PointCloudHandlerRhodamineDye) pointcloudPrediction.getColorHandler()).getColorsRhodamineDye());
        canvas.GetRenderer().AddActor(pointcloudPrediction.getCloudLODActor());
        
        scalarBar = new ScalarBar(I18n.text("Rhodamine Dye Color Map"));
        scalarBar.setScalarBarHorizontalProperties();
        scalarBar.setUpScalarBarLookupTable(((PointCloudHandlerRhodamineDye) pointcloud.getColorHandler()).getLutRhodamineDye());
        scalarBar.getScalarBarActor().Modified();
//      setScalarBar(scalarBar);
        canvas.GetRenderer().AddActor(scalarBar.getScalarBarActor());

        toolbar.getRhodToggle().setSelected(true);
        toolbar.getPredToggle().setSelected(true);

        reloadCanvas();
    }

    /**
     * 
     */
    private void reloadCanvas() {
        canvas.lock();

        canvas.GetRenderer().GetActiveCamera().SetPosition(1.0, -1.0, -100.0);
        canvas.GetRenderer().GetActiveCamera().SetViewUp(0.0, 1.0, -1.0);

        canvas.GetRenderer().ResetCamera();
        canvas.RenderSecured();

        canvas.unlock();
    }
    
    public static void main(String[] args) {
        Rhodamine3DPanel panel = new Rhodamine3DPanel();
        GuiUtils.testFrame(panel);
//        panel.setVisible(true);
    }
}
