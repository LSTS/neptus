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
 * Author: hfq
 * Mar 10, 2014
 */
package pt.lsts.neptus.plugins.vtk;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.vtk.ctd3d.CTD3DToolbar;
import pt.lsts.neptus.plugins.vtk.ctd3d.InteractorStyleCTD3D;
import pt.lsts.neptus.plugins.vtk.ctd3d.LoadData;
import pt.lsts.neptus.plugins.vtk.ctd3d.Window;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.vtk.pointcloud.PointCloudCTD;
import pt.lsts.neptus.vtk.pointcloud.PointCloudHandlerCTD;
import pt.lsts.neptus.vtk.utils.Utils;
import pt.lsts.neptus.vtk.visualization.AxesWidget;
import pt.lsts.neptus.vtk.visualization.Canvas;
import pt.lsts.neptus.vtk.visualization.ScalarBar;

/**
 * @author hfq
 * 
 */
@PluginDescription(author = "hfq", name = "CTD 3D", icon = "images/menus/3d.png")
public class CTD3D extends JPanel implements MRAVisualization, PropertiesProvider {

    private static final long serialVersionUID = 1L;

    private IMraLogGroup source;

    public Canvas canvas;
    private Window winCanvas;
    protected InteractorStyleCTD3D interactorStyle;

    private CTD3DToolbar toolbar;

    public PointCloudCTD pointcloud;
    
    // private vtkScalarBarActor lutActor = new vtkScalarBarActor();
    public ScalarBar scalarBar;

    private boolean isFirstRender = true;

    /**
     * 
     */
    public CTD3D() {
        if (!Utils.hasTryedToLoadVtkLib) {
            Utils.loadVTKLibraries();
            // VTKMemoryManager.GC.SetAutoGarbageCollection(true);
        }
    }

    @Override
    public String getName() {
        return I18n.text("CTD 3D");
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        this.source = source;

        setLayout(new BorderLayout());

        canvas = new Canvas();
        canvas.LightFollowCameraOn();
        canvas.GetRenderer().AutomaticLightCreationOn();
        canvas.setEnabled(true);

        winCanvas = new Window(canvas, source);
        interactorStyle = winCanvas.getInteractorStyle();

        loadData();

        pointcloud.createActorFromPoints();
        pointcloud.generateHandler();
        pointcloud.getPolyData().GetPointData().SetScalars(((PointCloudHandlerCTD) pointcloud.getColorHandler()).getColorsTemperature());
        canvas.GetRenderer().AddActor(pointcloud.getCloudLODActor());

        setScalarBar(new ScalarBar(I18n.text("Temperature Color Map")));
        scalarBar.setScalarBarHorizontalProperties();
        scalarBar.setUpScalarBarLookupTable(((PointCloudHandlerCTD) pointcloud.getColorHandler()).getLutTemperature());
        scalarBar.getScalarBarActor().Modified();
        canvas.GetRenderer().AddActor(scalarBar.getScalarBarActor());

        AxesWidget axesWidget = new AxesWidget(canvas.getRenderWindowInteractor());
        axesWidget.createAxesWidget();

        toolbar = new CTD3DToolbar(this);
        toolbar.createtoolBar();

        add(toolbar, BorderLayout.WEST);
        add(canvas);

        toolbar.getTempToggle().setSelected(true);

        return this;
    }

    /**
     * 
     */
    private void loadData() {
        LoadData loadData = new LoadData(source);
        loadData.loadCTDData();

        pointcloud = loadData.getPointcloud();
    }

    
    
    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return (NeptusMRA.vtkEnabled && source.getLsfIndex()
                .containsMessagesOfType("Conductivity"));
    }

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("images/menus/3d.png");
    }

    @Override
    public Double getDefaultTimeStep() {
        return null;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }

    @Override
    public void onHide() {

    }

    @Override
    public void onShow() {
        if (isFirstRender) {
            canvas.lock();

            canvas.GetRenderer().GetActiveCamera().SetPosition(1.0, -1.0, -100.0);
            canvas.GetRenderer().GetActiveCamera().SetViewUp(0.0, 1.0, -1.0);

            canvas.GetRenderer().ResetCamera();
            canvas.RenderSecured();

            canvas.unlock();

            isFirstRender = false;
        }
    }

    @Override
    public void onCleanup() {
        // VTKMemoryManager.deleteAll();
    }

    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return "CTD 3D Properties";
    }

    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return PluginUtils.validatePluginProperties(this, properties);
    }

    /**
     * @return the scalarBar
     */
    public ScalarBar getScalarBar() {
        return scalarBar;
    }

    /**
     * @param scalarBar the scalarBar to set
     */
    public void setScalarBar(ScalarBar scalarBar) {
        this.scalarBar = scalarBar;
    }
}
