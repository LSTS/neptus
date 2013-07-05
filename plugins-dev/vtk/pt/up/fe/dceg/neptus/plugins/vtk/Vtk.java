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
 * Apr 3, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk;

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
import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.NeptusMRA;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.vtk.filters.StatisticalOutlierRemoval;
import pt.up.fe.dceg.neptus.plugins.vtk.multibeampluginutils.MultibeamToolbar;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.MultibeamToPointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.surface.PointCloudMesh;
import pt.up.fe.dceg.neptus.plugins.vtk.utils.Utils;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.AxesWidget;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.Canvas;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.Text3D;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.Window;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import vtk.vtkLODActor;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author hfq
 *
 *#define VTK_CURSOR_DEFAULT   0
   76 #define VTK_CURSOR_ARROW     1
   77 #define VTK_CURSOR_SIZENE    2
   78 #define VTK_CURSOR_SIZENW    3
   79 #define VTK_CURSOR_SIZESW    4
   80 #define VTK_CURSOR_SIZESE    5
   81 #define VTK_CURSOR_SIZENS    6
   82 #define VTK_CURSOR_SIZEWE    7
   83 #define VTK_CURSOR_SIZEALL   8
   84 #define VTK_CURSOR_HAND      9
   85 #define VTK_CURSOR_CROSSHAIR 10
 *
 */
@PluginDescription(author = "hfq", name = "Vtk")
public class Vtk extends JPanel implements MRAVisualization, PropertiesProvider, ComponentListener {
    private static final long serialVersionUID = 8057825167454469065L;
    
    //@NeptusProperty(name = "Points to ignore on Multibeam 3D", description="Fixed step of number of points to jump on multibeam Pointcloud stored for render purposes.")
    //public int ptsToIgnore = 100;
    //public int ptsToIgnore = NeptusMRA.ptsToIgnore;
    
    //@NeptusProperty(name = "Approach to ignore points on Multibeam 3D", description="Type of approach to ignore points on multibeam either by a fixed step (false) or by a probability (true).")
    //public boolean approachToIgnorePts = true;
    //public boolean approachToIgnorePts = NeptusMRA.approachToIgnorePts;
        
    //@NeptusProperty(name = "Timestamp increment", description="Timestamp increment for the 83P parser (in miliseconds).")
    //public long timestampMultibeamIncrement = 0;
    //public long timestampMultibeamIncrement = NeptusMRA.timestampMultibeamIncrement;
    
    //@NeptusProperty(name = "Yaw Increment", description="Yaw (psi) increment for the 83P parser, set true to increment + 180º.")
    //public boolean yawMultibeamIncrement = false;
    //public boolean yawMultibeamIncrement = NeptusMRA.yawMultibeamIncrement;
    
    @NeptusProperty(name = "Depth exaggeration multiplier", description="Multiplier value for depth exaggeration.")
    public int zExaggeration = 10;

    // there are 2 types of rendering objects on VTK - vtkPanel and vtkCanvas. vtkCanvas seems to have a better behaviour and performance.
    //public vtkPanel vtkPanel;
    //public vtkCanvas vtkCanvas;
    public Canvas canvas;
    
    
    public Window winCanvas;
    
    public vtkLODActor noBeamsTxtActor;
    
    public Text3D noBeamsText;
    
    private MultibeamToolbar toolbar;

    private static final String FILE_83P_EXT = ".83P";
    
    public LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud = new LinkedHashMap<>();       
    public PointCloud<PointXYZ> pointCloud;
    
    public LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh = new LinkedHashMap<>();
 
    //private Vector<Marker3d> markers = new Vector<>();
    
    public IMraLogGroup mraVtkLogGroup;
    public File file;
    
    private Boolean componentEnabled = false;
    
    public MultibeamToPointCloud multibeamToPointCloud;

    //private DownsamplePointCloud performDownsample;
    //private Boolean isDownsampleDone = false;
    
    static {
        Utils.loadVTKLibraries();
    }
    
    /**
     * @param panel
     */
    public Vtk(MRAPanel panel) {
        super(new MigLayout());
    }

    @Override
    public String getName() {
        return I18n.text("Multibeam 3D");
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {    
        if (!componentEnabled)
        {
            componentEnabled = true;

            canvas = new Canvas();
       
            pointCloud = new PointCloud<>();
            pointCloud.setCloudName("multibeam");
            linkedHashMapCloud.put(pointCloud.getCloudName(), pointCloud);
            
            winCanvas = new Window(canvas, linkedHashMapCloud);
            
            canvas.GetRenderer().ResetCamera();
            canvas.LightFollowCameraOn();
            
                // add vtkCanvas to Layout
            add(canvas,  "W 100%, H 100%");

                // parse 83P data storing it on a pointcloud
            multibeamToPointCloud = new MultibeamToPointCloud(getLog(), pointCloud);
            multibeamToPointCloud.parseMultibeamPointCloud();
            
                // add toolbar to Layout
            toolbar = new MultibeamToolbar(this);
            toolbar.createToolbar();
            add(toolbar.getToolbar(), "dock south");
            
                // for resizing porpuses
            canvas.getParent().addComponentListener(this);

            //vtkCanvas.setEnabled(true);
            canvas.setEnabled(true);

                // add axesWidget to vtk canvas fixed to a screen position
            AxesWidget axesWidget = new AxesWidget(winCanvas.getInteractorStyle().GetInteractor());            
            axesWidget.createAxesWidget();
            
            if (pointCloud.getNumberOfPoints() != 0) {  // checks wether there are any points to render!                         
                
                //canvas.lock();
                
                if (NeptusMRA.outliersRemoval) {
                    // remove outliers
//                RadiusOutlierRemoval radOutRem = new RadiusOutlierRemoval();
//                radOutRem.applyFilter(multibeamToPointCloud.getPoints());
//                pointCloud.setPoints(radOutRem.getOutputPoints());
                //NeptusLog.pub().info("Get number of points: " + pointCloud.getPoints().GetNumberOfPoints());
                
                    StatisticalOutlierRemoval statOutRem = new StatisticalOutlierRemoval();
                    // statOutRem.applyFilter(multibeamToPointCloud.getPoints());
                    statOutRem.setMeanK(20);
                    statOutRem.setStdMul(0.2);
                    statOutRem.applyFilter(multibeamToPointCloud.getPoints());
                    pointCloud.setPoints(statOutRem.getOutputPoints());

                }
                else 
                    pointCloud.setPoints(multibeamToPointCloud.getPoints());
      
                pointCloud.setNumberOfPoints(pointCloud.getPoints().GetNumberOfPoints());               
                // create an actor from parsed beams
                //pointCloud.createLODActorFromPoints(multibeamToPointCloud.getIntensities());
                pointCloud.createLODActorFromPoints();
                
                //NeptusLog.pub().info("antes delete");
                Utils.delete(multibeamToPointCloud.getPoints());
                //NeptusLog.pub().info("depois delete");
                //canvas.unlock();
                
                    // add parsed beams stored on pointcloud to canvas
                canvas.GetRenderer().AddActor(pointCloud.getCloudLODActor());
                    // set Up scalar Bar look up table
                winCanvas.getInteractorStyle().getScalarBar().setUpScalarBarLookupTable(pointCloud.getColorHandler().getLutZ());
                canvas.GetRenderer().AddActor(winCanvas.getInteractorStyle().getScalarBar().getScalarBarActor());
                
                    // set up camera to +z viewpoint looking down
                canvas.GetRenderer().GetActiveCamera().SetPosition(pointCloud.getPoly().GetCenter()[0] ,pointCloud.getPoly().GetCenter()[1] , pointCloud.getPoly().GetCenter()[2] - 200);
                canvas.GetRenderer().GetActiveCamera().SetViewUp(0.0, 0.0, -1.0);
                //canvas.Report();
            }
            else {  // if no beams were parsed
                String msgErrorMultibeam;
                msgErrorMultibeam = I18n.text("No beams on Log file!");
                JOptionPane.showMessageDialog(null, msgErrorMultibeam);

                noBeamsText = new Text3D();
                noBeamsText.buildText3D("No beams on Log file!", 2.0, 2.0, 2.0, 10.0);
                canvas.GetRenderer().AddActor(noBeamsText.getText3dActor());
            }
            canvas.RenderSecured();
            canvas.GetRenderWindow().SetCurrentCursor(9);
            
            canvas.GetRenderer().ResetCamera();
        }      
        return this;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        boolean beApplied = false;        
        
        if (NeptusMRA.vtkEnabled) {   // if it could load vtk libraries
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
        return beApplied;
    }


    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("images/buttons/model3d.png");
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
    }

    @Override
    public void onCleanup() {        
            /*
             * [xcb] Unknown sequence number while processing queue
             * [xcb] Most likely this is a multi-threaded client and XInitThreads has not been called
             * [xcb] Aborting, sorry about that.
             * java: ../../src/xcb_io.c:274: poll_for_event: Assertion `!xcb_xlib_threads_sequence_lost' failed.
             */
        //VTKMemoryManager.deleteAll();
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

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#getProperties()
     */
    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#setProperties(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return "Multibeam 3D properties";
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#getPropertiesErrors(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return PluginUtils.validatePluginProperties(this, properties);
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentResized(ComponentEvent e) {

        Rectangle toolbarBounds = toolbar.getToolbar().getBounds();
        
        Rectangle parentBounds = new Rectangle();
        parentBounds.setBounds(canvas.getParent().getX(), canvas.getParent().getY(), canvas.getParent().getParent().getWidth() - 6, canvas.getParent().getParent().getHeight() - 12); //- toolBarBounds.getHeight()
        canvas.getParent().setBounds(parentBounds);

        Rectangle canvasBounds = new Rectangle();
        canvasBounds.setBounds(canvas.getX(), canvas.getY(), canvas.getParent().getWidth() - 6, (int) (canvas.getParent().getHeight() - toolbarBounds.getHeight()));
        canvas.setBounds(canvasBounds); 
               
        Rectangle newToolbarBounds = new Rectangle();
        newToolbarBounds.setBounds(toolbarBounds.x, (canvas.getY() + canvas.getHeight()), toolbarBounds.width, toolbarBounds.height);
        toolbar.getToolbar().setBounds(newToolbarBounds);
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentMoved(ComponentEvent e) {
        
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentShown(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentShown(ComponentEvent e) {
        
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentHidden(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentHidden(ComponentEvent e) {
        
    }
    

}
