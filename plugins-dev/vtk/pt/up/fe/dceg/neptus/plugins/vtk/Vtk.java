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

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.mra3d.Marker3d;
import pt.up.fe.dceg.neptus.plugins.vtk.filters.DownsamplePointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.MultibeamToPointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.AxesWidget;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.MultibeamToolBar;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.Window;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import vtk.vtkActor;
import vtk.vtkCanvas;
import vtk.vtkHardwareSelector;
import vtk.vtkLinearExtrusionFilter;
import vtk.vtkNativeLibrary;
import vtk.vtkPolyDataMapper;
import vtk.vtkVectorText;

/**
 * @author hfq
 *
 */
@PluginDescription(author = "hfq", name = "Vtk")
public class Vtk extends JPanel implements MRAVisualization {
    private static final long serialVersionUID = 1L;
    
    private static Boolean vtkEnabled = true;
    
    // there are 2 types of rendering objects on VTK - vtkPanel and vtkCanvas. vtkCanvas seems to have a better behaviour and performance.
    //public vtkPanel vtkPanel;
    public vtkCanvas vtkCanvas;
    
    public Window winCanvas;

    private static final String FILE_83P_EXT = ".83P";
    
    public LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud = new LinkedHashMap<>();       
    public PointCloud<PointXYZ> pointCloud;
 
    private Vector<Marker3d> markers = new Vector<>();
    
    public IMraLogGroup mraVtkLogGroup;
    public File file;
    
    private Boolean componentEnabled = false;

    private DownsamplePointCloud performDownsample;
    private Boolean isDownsampleDone = false;
    
    //public boolean isLogMultibeam; // for 
    
    static {  
        try {
            System.loadLibrary("jawt");
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load jawt lib!");
        } 
            // for simple visualizations
        try {
            vtkNativeLibrary.COMMON.LoadLibrary();
            if(!vtkNativeLibrary.COMMON.IsLoaded())
                vtkEnabled = false;
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load vtkCommon, skipping...");
        }
        try {
            vtkNativeLibrary.FILTERING.LoadLibrary();
            if(!vtkNativeLibrary.FILTERING.IsLoaded())
                vtkEnabled = false;
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load vtkFiltering, skipping...");
        }
        try {
            vtkNativeLibrary.IO.LoadLibrary();
            if(!vtkNativeLibrary.IO.IsLoaded())
                vtkEnabled = false;
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load vtkImaging, skipping...");
            System.out.println("cannot load vtkImaging, skipping...");
        }
        try {
            vtkNativeLibrary.GRAPHICS.LoadLibrary();
            if(!vtkNativeLibrary.GRAPHICS.IsLoaded())
                vtkEnabled = false;
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load vtkGrahics, skipping...");
            System.out.println("cannot load vtkGrahics, skipping...");
        }
        try {
            vtkNativeLibrary.RENDERING.LoadLibrary();
            if(!vtkNativeLibrary.RENDERING.IsLoaded())
                vtkEnabled = false;
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load vtkRendering, skipping...");
            System.out.println("cannot load vtkRendering, skipping...");
        }
                
        // Other
        try {
            vtkNativeLibrary.INFOVIS.LoadLibrary();
            if(!vtkNativeLibrary.INFOVIS.IsLoaded())
                vtkEnabled = false;
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load vtkInfoVis, skipping...");
        }
        try {
            vtkNativeLibrary.VIEWS.LoadLibrary();
            if(!vtkNativeLibrary.VIEWS.IsLoaded())
                vtkEnabled = false;
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load vtkViews, skipping...");
        }
        try {
            vtkNativeLibrary.WIDGETS.LoadLibrary();
            if(!vtkNativeLibrary.WIDGETS.IsLoaded())
                vtkEnabled = false;
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load vtkWidgets, skipping...");
        }
        try {
            vtkNativeLibrary.GEOVIS.LoadLibrary();
            if(!vtkNativeLibrary.GEOVIS.IsLoaded())
                vtkEnabled = false;
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load vtkGeoVis, skipping...");
        }
        try {
            vtkNativeLibrary.CHARTS.LoadLibrary();
            if(!vtkNativeLibrary.CHARTS.IsLoaded())
                vtkEnabled = false;
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load vtkCharts, skipping...");
        }
        // FIXME not load vtkHybrid ?!
        try {
            vtkNativeLibrary.HYBRID.LoadLibrary();
            if(!vtkNativeLibrary.HYBRID.IsLoaded())
                vtkEnabled = false;
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load vtkHybrid, skipping...");
        }
        try {
            vtkNativeLibrary.VOLUME_RENDERING.LoadLibrary();
            if(!vtkNativeLibrary.VOLUME_RENDERING.IsLoaded())
                vtkEnabled = false;
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load vtkVolumeRendering, skipping...");
        }
    }
    
    /**
     * @param panel
     */
    public Vtk(MRAPanel panel) {
        //super(new BorderLayout()); // change to MigLayout
        super(new MigLayout());
    }

    @Override
    public String getName() {
        return "Multibeam 3D";
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {    
        if (!componentEnabled)
        {   
            vtkCanvas = new vtkCanvas();
           
            pointCloud = new PointCloud<>();
            pointCloud.setCloudName("multibeam");
            linkedHashMapCloud.put(pointCloud.getCloudName(), pointCloud);
            
            winCanvas = new Window(vtkCanvas, linkedHashMapCloud);
            
            vtkCanvas.GetRenderer().ResetCamera();
            
                // add vtkCanvas to Layout
            //add(vtkCanvas, BorderLayout.CENTER);
            add(vtkCanvas, "W 100%, H 100%");
            //add(vtkCanvas);
            
            vtkCanvas.LightFollowCameraOn();
            vtkCanvas.BeginBoxInteraction();
            vtkCanvas.setEnabled(true);
            
            componentEnabled = true;

            MultibeamToPointCloud multibeamToPointCloud = new MultibeamToPointCloud(getLog(), pointCloud);
            //BathymetryInfo batInfo = new BathymetryInfo();
            //batInfo = multibeamToPointCloud.batInfo;
            
            if (pointCloud.getNumberOfPoints() != 0) {  // checks wether there are any points to render!            
                MultibeamToolBar toolbar = new MultibeamToolBar(vtkCanvas, linkedHashMapCloud);
                toolbar.createToolBar();
                //add(toolbar.getToolBar(), BorderLayout.SOUTH);
                add(toolbar.getToolBar(), "dock south");
                
                pointCloud.createLODActorFromPoints();
                
                //double[] center = pointCloud.getPoly().GetCenter();
                //Axes ax = new Axes(30.0, center[0], center[1], center[2], 0);
                //Axes ax = new Axes(30.0, 0.0f, 0.0f, 0.0f, 0);
                //vtkCanvas.GetRenderer().AddActor(ax.getAxesActor());
                //ax.getAxesActor().SetVisibility(true);
                
                //AxesActor axesActor = new AxesActor(vtkCanvas.GetRenderer());
                //axesActor.createAxes();
                //axesActor.setAxesVisibility(true);

                AxesWidget axesWidget = new AxesWidget(winCanvas.getInteractorStyle().GetInteractor());            
                axesWidget.createAxesWidget();
                
                vtkCanvas.GetRenderer().AddActor(pointCloud.getCloudLODActor());
                //vtkCanvas.GetRenderer().AddActor(downsampledCloud.getCloudLODActor());
                
                //vtkLODActor tempActor = performDownsample.getOutputDownsampledCloud().getCloudLODActor();
                //NeptusLog.pub().info("Number of Cloud Points: " + pointCloud.getNumberOfPoints());
                //vtkCanvas.GetRenderer().AddActor(tempActor);
            }
            else {
                //isLogMultibeam = false;            
                //JOptionPane errorPane = new JOptionPane();
                String msgErrorMultibeam;
                msgErrorMultibeam = "No beams on Log file!";
                JOptionPane.showMessageDialog(null, msgErrorMultibeam);
                
//                vtkTextActor3D textActor3d = new vtkTextActor3D();
//                textActor3d.SetPosition(0.0, 0.0, 0.0);
//                textActor3d.SetInput(msgErrorMultibeam);
//                textActor3d.GetTextProperty().BoldOn();
//                textActor3d.GetTextProperty().ItalicOn();
//                textActor3d.GetTextProperty().ShadowOn();
//                textActor3d.GetTextProperty().SetFontFamilyToArial();
//                textActor3d.GetTextProperty().SetLineSpacing(1.0);
//                textActor3d.GetTextProperty().SetFontSize(48);
//                
//                textActor3d.SetScale(2.0);
//                
//                textActor3d.GetTextProperty().SetColor(1.0, 0.0, 0.0);
//                textActor3d.VisibilityOn();
//                textActor3d.GetTextProperty().SetShadowOffset(1, 1);
//                //textActor3d.GetTextProperty().ShadowOn();
//                textActor3d.GetTextProperty().SetVerticalJustificationToCentered();
                
                
                vtkVectorText vectText = new vtkVectorText();
                vectText.SetText("No beams on Log file!");
                
                vtkLinearExtrusionFilter extrude = new vtkLinearExtrusionFilter();
                extrude.SetInputConnection(vectText.GetOutputPort());
                extrude.SetExtrusionTypeToNormalExtrusion();
                extrude.SetVector(0, 0, 1);
                extrude.SetScaleFactor(0.5);
           
                vtkPolyDataMapper txtMapper = new vtkPolyDataMapper();
                txtMapper.SetInputConnection(extrude.GetOutputPort());
                vtkActor txtActor = new vtkActor();
                txtActor.SetMapper(txtMapper);
                txtActor.SetPosition(2.0, 2.0, 2.0);
                txtActor.SetScale(10.0);
                
                //vtkCanvas.GetRenderer().AddActor(textActor3d);
                vtkCanvas.GetRenderer().AddActor(txtActor);
                
            }        
            vtkCanvas.GetRenderer().ResetCamera();
        }
        //NeptusLog.pub().info("isLogMultibeam in vtk class: " + isLogMultibeam);
        
        return this;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        boolean beApplied = false;        
        
        if (vtkEnabled == true) {   // if it could load vtk libraries
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
        //return null;
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
//        try {
//            vtkPanel.disable();
//            //vtkPanel.Delete();
//        }
//        catch (Throwable e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }
    
    /**
     * @return the mraVtkLogGroup
     */
    private IMraLogGroup getLog() {
        return mraVtkLogGroup;
    }

    /**
     * @param mraVtkLogGroup the mraVtkLogGroup to set
     */
    private void setLog(IMraLogGroup log) {
        this.mraVtkLogGroup = log;
    }
}
