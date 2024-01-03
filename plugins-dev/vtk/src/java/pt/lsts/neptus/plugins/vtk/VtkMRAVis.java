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
 * Apr 3, 2013
 */
package pt.lsts.neptus.plugins.vtk;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.vtk.filters.StatisticalOutlierRemoval;
import pt.lsts.neptus.vtk.mravisualizer.EventsHandler;
import pt.lsts.neptus.vtk.mravisualizer.EventsHandler.SensorTypeInteraction;
import pt.lsts.neptus.vtk.mravisualizer.InteractorStyleVis3D;
import pt.lsts.neptus.vtk.mravisualizer.LoadToPointCloud;
import pt.lsts.neptus.vtk.mravisualizer.VtkOptions;
import pt.lsts.neptus.vtk.mravisualizer.Window;
import pt.lsts.neptus.vtk.pointcloud.APointCloud;
import pt.lsts.neptus.vtk.pointcloud.PointCloudHandlerXYZ;
import pt.lsts.neptus.vtk.pointcloud.PointCloudXYZ;
import pt.lsts.neptus.vtk.surface.PointCloudMesh;
import pt.lsts.neptus.vtk.utils.Utils;
import pt.lsts.neptus.vtk.visualization.Canvas;
import pt.lsts.neptus.vtk.visualization.Text3D;
import vtk.vtkLODActor;

/**
 * @author hfq
 */
@PluginDescription(author = "hfq", name = "Bathymetry 3D", icon = "images/menus/3d.png")
public class VtkMRAVis extends JPanel implements MRAVisualization, PropertiesProvider {
    private static final long serialVersionUID = 8057825167454469065L;

    private Canvas canvas;
    private Window winCanvas;
    private InteractorStyleVis3D interactorStyle;
    private EventsHandler events;

    public vtkLODActor noBeamsTxtActor;
    public Text3D noBeamsText;

    private Vis3DMenuBar menuBar;
    private Vis3DToolBar toolbar;

    private LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud = new LinkedHashMap<String, APointCloud<?>>();
    private LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh = new LinkedHashMap<String, PointCloudMesh>();

    public IMraLogGroup source;
    public File file;

    private Boolean componentEnabled = false;
    private Boolean isFirstRender = true;

    private boolean mbFound = false;
    private static final String FILE_83P_EXT = ".83P";

    private MRAPanel mraPanel = null;
    
    /**
     * @param panel
     */
    public VtkMRAVis(MRAPanel panel) {
        this.mraPanel = panel;
        if (!Utils.hasTryedToLoadVtkLib) {
            Utils.loadVTKLibraries();
            // VTKMemoryManager.GC.SetAutoGarbageCollection(true);
        }
    }

    @Override
    public String getName() {
        return I18n.text("3D Bathymetry");
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        if (!componentEnabled) {
            componentEnabled = true;
            this.source = source;

            setCanvas(new Canvas());
            getCanvas().LightFollowCameraOn();
            getCanvas().setEnabled(true);

            winCanvas = new Window(getCanvas(), interactorStyle, linkedHashMapCloud, linkedHashMapMesh, source);
            interactorStyle = winCanvas.getInteracStyle();
            setEvents(interactorStyle.getEventsHandler());

            // set Interface Layout
            setLayout(new BorderLayout());

            menuBar = new Vis3DMenuBar(this);
            menuBar.createMenuBar();
            add(menuBar, BorderLayout.NORTH);
            toolbar = new Vis3DToolBar(this);
            toolbar.createToolBar();
            add(toolbar, BorderLayout.WEST);

            add(getCanvas());

            // checks if data is available
            // first multibeam then dvl
            loadCloud();

            if (!mbFound)
                toolbar.remove(toolbar.multibeamToggle);
            if (!source.getLsfIndex().containsMessagesOfType("Distance"))
                toolbar.remove(toolbar.dvlToggle);
        }
        return this;
    }

    private void loadCloud() {
        if (mbFound) {
            try {
                PointCloudXYZ pointCloudMultibeam = new PointCloudXYZ();
                LoadToPointCloud load = new LoadToPointCloud(source, pointCloudMultibeam);
                NeptusLog.pub().info("Parsing Multibeam data.");
                pointCloudMultibeam.setCloudName("multibeam");
                load.parseMultibeamPointCloud();
                getLinkedHashMapCloud().put(pointCloudMultibeam.getCloudName(), pointCloudMultibeam);
                processPointCloud(pointCloudMultibeam, load);
                pointCloudMultibeam.getPolyData().GetPointData().SetScalars(((PointCloudHandlerXYZ) (pointCloudMultibeam.getColorHandler())).getColorsZ());
                events.setSensorTypeInteraction(SensorTypeInteraction.MULTIBEAM);
                toolbar.multibeamToggle.setSelected(true);
                setUpRenderer(pointCloudMultibeam);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e.getMessage(), e);
            }
        }
        if (source.getLsfIndex().containsMessagesOfType("Distance")) {
            PointCloudXYZ pointCloudDVL = new PointCloudXYZ();
            LoadToPointCloud load2 = new LoadToPointCloud(source, pointCloudDVL);
            NeptusLog.pub().info("Parsing DVL data.");
            pointCloudDVL.setCloudName("dvl");
            load2.parseDVLPointCloud();
            getLinkedHashMapCloud().put(pointCloudDVL.getCloudName(), pointCloudDVL);
            processPointCloud(pointCloudDVL, load2);
            pointCloudDVL.getPolyData().GetPointData().SetScalars(((PointCloudHandlerXYZ) (pointCloudDVL.getColorHandler())).getColorsZ());
            if (!mbFound) {
                events.setSensorTypeInteraction(SensorTypeInteraction.DVL);
                toolbar.dvlToggle.setSelected(true);
                setUpRenderer(pointCloudDVL);
            }
        }
        if (!source.getLsfIndex().containsMessagesOfType("Distance") && !mbFound) {
            String msgErrorNoData = I18n.text("No data Available!");
            GuiUtils.errorMessage(mraPanel, I18n.text("Info"), msgErrorNoData);

            Text3D noDataText = new Text3D();
            noDataText.buildText3D(msgErrorNoData, 2.0, 2.0, 2.0, 10.0);
            noDataText.getText3dActor().RotateY(180);
            getCanvas().GetRenderer().AddActor(noDataText.getText3dActor());
        }
    }

    public void loadCloudBySensorType(String sensorType) {
        PointCloudXYZ pointCloud =  new PointCloudXYZ();
        LoadToPointCloud load = new LoadToPointCloud(source, pointCloud);
        if (sensorType.equals("dvl") && source.getLsfIndex().containsMessagesOfType("Distance")) {
            pointCloud.setCloudName(sensorType);
            load.parseDVLPointCloud();
        }
        else if (sensorType.equals("multibeam")) {
            pointCloud.setCloudName("multibeam");
            load.parseMultibeamPointCloud();
        }

        getLinkedHashMapCloud().put(pointCloud.getCloudName(), pointCloud);
        processPointCloud(pointCloud, load);
        setUpRenderer(pointCloud);
    }

    private void processPointCloud(APointCloud<?> pointCloud, LoadToPointCloud load) {
        if (pointCloud.getCloudName().equals("multibeam")) {
            if (pointCloud.getNumberOfPoints() != 0) { // checks wether there are any points to render!
                if (MRAProperties.outliersRemoval) {
                    // remove outliers
                    // RadiusOutlierRemoval radOutRem = new RadiusOutlierRemoval();
                    // radOutRem.applyFilter(multibeamToPointCloud.getPoints());
                    // pointCloud.setPoints(radOutRem.getOutputPoints());
                    // NeptusLog.pub().info("Get number of points: " + pointCloud.getPoints().GetNumberOfPoints());

                    StatisticalOutlierRemoval statOutRem = new StatisticalOutlierRemoval();
                    statOutRem.setMeanK(20);
                    statOutRem.setStdMul(0.2);
                    statOutRem.applyFilter(load.getPoints());
                    pointCloud.setXYZPoints(statOutRem.getOutputPoints());
                }
                else
                    pointCloud.setXYZPoints(load.getPoints());
                // create an actor from parsed beams
                pointCloud.createActorFromPoints();
                pointCloud.generateHandler();
                NeptusLog.pub().info("Created Actor for multibeam data");
            }
        }
        else {
            pointCloud.setXYZPoints(load.getPoints());
            pointCloud.createActorFromPoints();
            pointCloud.generateHandler();
            NeptusLog.pub().info("Created Actor for dvl data");
        }
    }

    /**
     * @param pointCloud
     */
    private void setUpRenderer(APointCloud<?> pointCloud) {
        if (pointCloud.getNumberOfPoints() != 0) {
            // add parsed beams stored on pointcloud to canvas
            getCanvas().GetRenderer().AddActor(pointCloud.getCloudLODActor());
            // set Up scalar Bar look up table
            interactorStyle.getScalarBar().setUpScalarBarLookupTable(((PointCloudHandlerXYZ) pointCloud.getColorHandler()).getLutZ());
            getCanvas().GetRenderer().AddActor(winCanvas.getInteracStyle().getScalarBar().getScalarBarActor());
        }
        else { // if no beams were parsed
            String msgErrorMultibeam;
            msgErrorMultibeam = I18n.text("No beams on Log file!");
            GuiUtils.errorMessage(mraPanel, I18n.text("Info"), msgErrorMultibeam);

            noBeamsText = new Text3D();
            noBeamsText.buildText3D(msgErrorMultibeam, 2.0, 2.0, 2.0, 10.0);
            noBeamsText.getText3dActor().RotateY(180);
            getCanvas().GetRenderer().AddActor(noBeamsText.getText3dActor());
        }
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {

        if (NeptusMRA.vtkEnabled) { // if it could load vtk libraries
            // Checks existance of a *.83P file
            file = source.getFile("Data.lsf").getParentFile();
            try {
                if (file.isDirectory()) {
                    for (File temp : file.listFiles()) {
                        if ((temp.toString()).endsWith(FILE_83P_EXT)) {
                            setLog(source);
                            mbFound = true;
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return mbFound || source.getLsfIndex().containsMessagesOfType("Distance");
        }
        else
            return false;

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
            canvas.GetRenderer().GetActiveCamera().SetPosition(1.0, -1.0, -100.0);
            canvas.GetRenderer().GetActiveCamera().SetViewUp(0.0, 1.0, -1.0);

            getCanvas().GetRenderWindow().SetCurrentCursor(9);
            getCanvas().RenderSecured();
            getCanvas().GetRenderer().ResetCamera();

            isFirstRender = false;
            // canvas.Report();
        }
    }

    @Override
    public void onCleanup() {
        // VTKMemoryManager.deleteAll();
    }

    /**
     * @return the mraVtkLogGroup
     */
    public IMraLogGroup getLog() {
        return source;
    }

    /**
     * @param mraVtkLogGroup the mraVtkLogGroup to set
     */
    private void setLog(IMraLogGroup log) {
        this.source = log;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.gui.PropertiesProvider#getProperties()
     */
    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(new VtkOptions());
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.gui.PropertiesProvider#setProperties(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(new VtkOptions(), properties);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return I18n.text("3D Bathymetry Properties");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.gui.PropertiesProvider#getPropertiesErrors(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return PluginUtils.validatePluginProperties(this, properties);
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
     * @return the linkedHashMapCloud
     */
    public LinkedHashMap<String, APointCloud<?>> getLinkedHashMapCloud() {
        return linkedHashMapCloud;
    }

    /**
     * @param linkedHashMapCloud the linkedHashMapCloud to set
     */
    public void setLinkedHashMapCloud(LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud) {
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
    public void setLinkedHashMapMesh(LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh) {
        this.linkedHashMapMesh = linkedHashMapMesh;
    }

    /**
     * @return the events
     */
    public EventsHandler getEvents() {
        return events;
    }

    /**
     * @param events the events to set
     */
    private void setEvents(EventsHandler events) {
        this.events = events;
    }
}
