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
 * Apr 3, 2013
 */
package pt.lsts.neptus.plugins.vtk;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.vtk.filters.StatisticalOutlierRemoval;
import pt.lsts.neptus.plugins.vtk.mravisualizer.MultibeamToolbar;
import pt.lsts.neptus.plugins.vtk.pointcloud.LoadToPointCloud;
import pt.lsts.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.lsts.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.lsts.neptus.plugins.vtk.surface.PointCloudMesh;
import pt.lsts.neptus.plugins.vtk.utils.Utils;
import pt.lsts.neptus.plugins.vtk.visualization.AxesWidget;
import pt.lsts.neptus.plugins.vtk.visualization.Canvas;
import pt.lsts.neptus.plugins.vtk.visualization.Text3D;
import pt.lsts.neptus.plugins.vtk.visualization.Window;
import pt.lsts.neptus.util.ImageUtils;
import vtk.vtkLODActor;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author hfq
 */
@PluginDescription(author = "hfq", name = "3D Visualization", icon = "images/menus/3d.png")
public class Vtk extends JPanel implements MRAVisualization, PropertiesProvider, ComponentListener {
    private static final long serialVersionUID = 8057825167454469065L;

    @NeptusProperty(name = "Depth exaggeration multiplier", description = "Multiplier value for depth exaggeration.")
    public int zExaggeration = 10;

    public Canvas canvas;
    public Window winCanvas;

    public vtkLODActor noBeamsTxtActor;
    public Text3D noBeamsText;

    private MultibeamToolbar toolbar;

    public LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud = new LinkedHashMap<>();
    public PointCloud<PointXYZ> pointCloud;
    public LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh = new LinkedHashMap<>();

    // private Vector<Marker3d> markers = new Vector<>();
    public IMraLogGroup mraVtkLogGroup;
    public File file;

    private Boolean componentEnabled = false;

    public LoadToPointCloud loadToPointCloud;

    private Boolean isFirstRender = true;

    private static final String FILE_83P_EXT = ".83P";

    /**
     * @param panel
     */
    public Vtk(MRAPanel panel) {
        super(new MigLayout());
        Utils.loadVTKLibraries();
    }

    @Override
    public String getName() {
        return I18n.text("3D Visualization");
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        if (!componentEnabled) {
            componentEnabled = true;

            canvas = new Canvas();

            pointCloud = new PointCloud<>();
            pointCloud.setCloudName("multibeam");
            linkedHashMapCloud.put(pointCloud.getCloudName(), pointCloud);

            winCanvas = new Window(canvas, linkedHashMapCloud);

            canvas.LightFollowCameraOn();
            // add vtkCanvas to Layout
            add(canvas, "W 100%, H 100%");

            // parse 83P data storing it on a pointcloud
            loadToPointCloud = new LoadToPointCloud(source, pointCloud);
            loadToPointCloud.parseMultibeamPointCloud();

            // add toolbar to Layout
            toolbar = new MultibeamToolbar(this);
            toolbar.createToolbar();
            add(toolbar.getToolbar(), "dock south");

            // for resizing porpuses
            canvas.getParent().addComponentListener(this);
            canvas.setEnabled(true);

            // add axesWidget to vtk canvas fixed to a screen position
            AxesWidget axesWidget = new AxesWidget(winCanvas.getInteractorStyle().GetInteractor());
            axesWidget.createAxesWidget();

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
                    statOutRem.applyFilter(loadToPointCloud.getPoints());
                    pointCloud.setPoints(statOutRem.getOutputPoints());
                }
                else
                    pointCloud.setPoints(loadToPointCloud.getPoints());

                // pointCloud.setNumberOfPoints(pointCloud.getPoints().GetNumberOfPoints());
                // create an actor from parsed beams
                // if (pointCloud.isHasIntensities()) {
                // multibeamToPointCloud.showIntensities();
                // pointCloud.setIntensities(multibeamToPointCloud.getIntensities());
                //
                // pointCloud.createLODActorFromPoints(multibeamToPointCloud.getIntensities());
                // NeptusLog.pub().info("create LOD actor with intensities");
                // }
                //
                // else {
                pointCloud.createLODActorFromPoints();
                NeptusLog.pub().info("create LOD actor without intensities");
                // }

                Utils.delete(loadToPointCloud.getPoints());

                // add parsed beams stored on pointcloud to canvas
                canvas.GetRenderer().AddActor(pointCloud.getCloudLODActor());
                // set Up scalar Bar look up table
                winCanvas.getInteractorStyle().getScalarBar()
                .setUpScalarBarLookupTable(pointCloud.getColorHandler().getLutZ());
                canvas.GetRenderer().AddActor(winCanvas.getInteractorStyle().getScalarBar().getScalarBarActor());

                // set up camera to +z viewpoint looking down
                //double[] center = new double[3];
                //center = PointCloudUtils.computeCenter(pointCloud);
                //canvas.GetRenderer().GetActiveCamera().SetPosition(center[0], center[1], center[2] - 200);
                //canvas.GetRenderer().GetActiveCamera().SetPosition(0, 0, 0 - 200);
                //canvas.GetRenderer().GetActiveCamera().SetViewUp(0.0, 0.0, -1.0);
            }
            else { // if no beams were parsed
                String msgErrorMultibeam;
                msgErrorMultibeam = I18n.text("No beams on Log file!");
                JOptionPane.showMessageDialog(null, msgErrorMultibeam);

                noBeamsText = new Text3D();
                noBeamsText.buildText3D("No beams on Log file!", 2.0, 2.0, 2.0, 10.0);
                canvas.GetRenderer().AddActor(noBeamsText.getText3dActor());
            }
        }
        return this;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        boolean beApplied = false;

        if (NeptusMRA.vtkEnabled) { // if it could load vtk libraries
            // Checks existance of a *.83P file
            file = source.getFile("Data.lsf").getParentFile();
            try {
                if (file.isDirectory()) {
                    for (File temp : file.listFiles()) {
                        if ((temp.toString()).endsWith(FILE_83P_EXT)) {
                            setLog(source);
                            beApplied = true;
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return beApplied || source.getLsfIndex().containsMessagesOfType("Distance");
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
            canvas.RenderSecured();
            canvas.GetRenderWindow().SetCurrentCursor(9);
            canvas.GetRenderer().ResetCamera();

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
        return mraVtkLogGroup;
    }

    /**
     * @param mraVtkLogGroup the mraVtkLogGroup to set
     */
    private void setLog(IMraLogGroup log) {
        this.mraVtkLogGroup = log;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.gui.PropertiesProvider#getProperties()
     */
    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.gui.PropertiesProvider#setProperties(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return "Multibeam 3D properties";
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

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentResized(ComponentEvent e) {

        Rectangle toolbarBounds = toolbar.getToolbar().getBounds();

        Rectangle parentBounds = new Rectangle();
        parentBounds.setBounds(canvas.getParent().getX(), canvas.getParent().getY(), canvas.getParent().getParent()
                .getWidth() - 6, canvas.getParent().getParent().getHeight() - 12); // - toolBarBounds.getHeight()
        canvas.getParent().setBounds(parentBounds);

        Rectangle canvasBounds = new Rectangle();
        canvasBounds.setBounds(canvas.getX(), canvas.getY(), canvas.getParent().getWidth() - 6, (int) (canvas
                .getParent().getHeight() - toolbarBounds.getHeight()));
        canvas.setBounds(canvasBounds);

        Rectangle newToolbarBounds = new Rectangle();
        newToolbarBounds.setBounds(toolbarBounds.x, (canvas.getY() + canvas.getHeight()), toolbarBounds.width,
                toolbarBounds.height);
        toolbar.getToolbar().setBounds(newToolbarBounds);

        canvas.RenderSecured();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentMoved(ComponentEvent e) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ComponentListener#componentShown(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentShown(ComponentEvent e) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ComponentListener#componentHidden(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentHidden(ComponentEvent e) {

    }
}
