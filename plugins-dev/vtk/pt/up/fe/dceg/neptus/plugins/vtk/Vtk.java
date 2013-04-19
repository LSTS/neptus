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
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.mra3d.Marker3d;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.CubeAxes;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.PointCloudHandlers;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.Window;
import vtk.vtkActor;
import vtk.vtkCanvas;
import vtk.vtkLODActor;
import vtk.vtkLocator;
import vtk.vtkNativeLibrary;
import vtk.vtkPanel;

/**
 * @author hfq
 *
 */
@PluginDescription(author = "hfq", name = "Vtk")
public class Vtk extends JPanel implements MRAVisualization {
    private static final long serialVersionUID = 1L;
    
    public vtkPanel vtkPanel;
    public vtkCanvas vtkCanvas;
    
    private JToggleButton zExaggerationToggle;
    private JToggleButton rawPointsToggle;
    private JToggleButton downsampledPointsToggle;
    private JToggleButton resetViewportToggle;  
    private JPanel toolBar;
    
    private static Path path = null;
    private static final String FILE_83P_EXT = ".83P";
    
    public Hashtable<String, vtkLODActor> hashCloud = new Hashtable<>();
    HashMap<String, vtkLODActor> hashMapCloud = new HashMap<>();
    LinkedHashMap<String, vtkLODActor> linkedHashMapCloud = new LinkedHashMap<>();
    
    protected Vector<Marker3d> markers = new Vector<>();
    protected IMraLogGroup mraVtkLogGroup;
    
    static {
        System.loadLibrary("jawt");
        
        // for simple visualizations
        try {
            vtkNativeLibrary.COMMON.LoadLibrary();
        }
        catch (Throwable e) {
            System.out.println("cannot load vtkCommon, skipping...");
        }
        
        vtkNativeLibrary.FILTERING.LoadLibrary();
        vtkNativeLibrary.IO.LoadLibrary();
        vtkNativeLibrary.IMAGING.LoadLibrary();
        vtkNativeLibrary.GRAPHICS.LoadLibrary();
        vtkNativeLibrary.RENDERING.LoadLibrary();
                
        // Other
        try {
            vtkNativeLibrary.INFOVIS.LoadLibrary();
        }
        catch (Throwable e) {
            System.out.println("cannot load vtkInfoVis, skipping...");
        }
        try {
            vtkNativeLibrary.VIEWS.LoadLibrary();
        }
        catch (Throwable e) {
            System.out.println("cannot load vtkViews, skipping...");
        }
        try {
            vtkNativeLibrary.WIDGETS.LoadLibrary();
        }
        catch (Throwable e) {
            System.out.println("cannot load vtkWidgets skipping...");
        }
        try {
            vtkNativeLibrary.GEOVIS.LoadLibrary();
        }
        catch (Throwable e) {
            System.out.println("cannot load vtkGeoVis, skipping...");
        }
        try {
            vtkNativeLibrary.CHARTS.LoadLibrary();
        }
        catch (Throwable e) {
            System.out.println("cannot load vtkCharts, skipping...");
        }
        try {
            vtkNativeLibrary.HYBRID.LoadLibrary();
        }
        catch (Throwable e) {
            System.out.println("cannot load vtkHybrid, skipping...");
        }
        try {
            vtkNativeLibrary.VOLUME_RENDERING.LoadLibrary();
        }
        catch (Throwable e) {
            System.out.println("cannot load vtkVolumeRendering, skipping...");
        }
    }
    
    /**
     * Ideia: se for pretendido colocar vários actores no render fazer
     * um HashMap<String, Actor>
     * @param panel
     */
    public Vtk(MRAPanel panel) {
        super(new BorderLayout());
   
        //vtkPanel = new vtkPanel();       
        //Window win = new Window(vtkPanel);
        
        //HashTable<String, > hash = new Hashtable<>()
        
        vtkCanvas = new vtkCanvas();

//      vtkGeoAssignCoordinates geoAssignCoords = new vtkGeoAssignCoordinates();
//      //geoAssignCoords.set

//        
///*        vtkConeSource cone = new vtkConeSource();
//        cone.SetResolution(8);
//
//        vtkPolyDataMapper coneMapper = new vtkPolyDataMapper();
//        coneMapper.SetInputConnection(cone.GetOutputPort());
//
//        vtkActor coneActor = new vtkActor();
//        coneActor.SetMapper(coneMapper);*/
//        
//        
//        
        //BoxWidget.addBoxWidget2Tovisualizer(vtkPanel.GetRenderer(), win.getRenWinInteractor());
     
        // a Random points, PointCloud
        PointCloud<PointXYZ> poi = new PointCloud<>();
        vtkLODActor cloud = new vtkLODActor();
        cloud = poi.getRandomPointCloud(1000);
        cloud.GetProperty().SetColor(1.0, 0.0, 0.0);
        
        hashCloud.put("cloud", cloud);
        hashMapCloud.put("cloud", cloud);
        linkedHashMapCloud.put("cloud", cloud);
        
        
        

        
        System.out.println("vai hashtable sempre em frente e mais alem");
        int hashCode = hashCloud.hashCode();
        System.out.println("Hash code: " + hashCode);
        System.out.println("elements: " + hashCloud.keySet());

        vtkLODActor testActor = hashCloud.get("cloud");
        vtkLODActor testActor2 = hashMapCloud.get("cloud");
        //hashCloud.
        
        vtkCanvas.GetRenderer().AddActor(testActor);
        
        // a cube Axes actor
        vtkActor cubeAxesActor = new vtkActor();
        cubeAxesActor = CubeAxes.AddCubeAxesToVisualizer(vtkCanvas.GetRenderer(), poi.poly);
        vtkCanvas.GetRenderer().AddActor(cubeAxesActor);
        
        // Setup Window do render
        Window winCanvas = new Window(vtkCanvas, hashCloud);
        
        //PointCloudHandlers<PointXYZ> handler = new PointCloudHandlers<>();
        
        double[] temp = PointCloudHandlers.getRandomColor();
        
        
        // reset the camera from the renderer
        vtkCanvas.GetRenderer().ResetCamera();
        
        // add vtkCanvas to Layout
        add(vtkCanvas, BorderLayout.CENTER);
        
        // axes 1
        //Axes ax = new Axes();
        //vtkCanvas.GetRenderer().AddActor(ax.getAxesActor());
        // axes 2
        //AxesActor axesActor = new AxesActor(vtkCanvas.GetRenderer());
        //axesActor.setAxesVisibility(true);
        

      //vtkTexture texture = new vtkTexture();
      //vtkPanel.GetRenderer().SetBackgroundTexture(null);
        
/*              
        vtkPanel.GetRenderer().ResetCamera();
        //vtkPanel.GetRenderer().ResetCameraClippingRange();
        //vtkPanel.GetRenderer().LightFollowCameraOn();
        //vtkPanel.GetRenderer().VisibleActorCount();
        //vtkPanel.GetRenderer().ViewToDisplay();

        add(vtkPanel, BorderLayout.CENTER);
        //vtkPanel.setBackground(Color.blue);
        //vtkPanel.setForeground(Color.green);
        
        //win.getRenWinInteractor().Initialize();
        //win.getRenWinInteractor().Start();
        
        //vtkPanel.GetRenderWindow().GetInteractor().AddObserver("CharEvent", this, "toogleStyle");
*/        
        toolBar = new JPanel();
        toolBar = createToolbar();
        add(toolBar, BorderLayout.EAST);
    }
    
    void toogleStyle() {
        if (vtkPanel.GetRenderWindow().GetInteractor().GetKeyCode() == 'c' | vtkPanel.GetRenderWindow().GetInteractor().GetKeyCode() == 'C') {
            System.out.println("1- setted interactor style C");
        } else {
            System.out.println("2- setted interactor style A");
        }       
    }
    
    @Override
    public String getName() {
        System.out.println("getName: " + mraVtkLogGroup.name());
        return "Vtk Visualization";
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        //String name = source.name();
        //String[] listoflogs = source.listLogs();
        
        
        System.out.println("getComponent: " + mraVtkLogGroup.name());
        return this;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        boolean beApplied = false;        
        //System.out.println("canBeApplied: " + mraVtkLogGroup.name());
        System.out.println("CanBeApplied: " + source.name());

        // Checks wether there is a *.83P file
        File file = source.getFile("Data.lsf").getParentFile();
        File[] files = file.listFiles();
        //int i = 0;
        try {
            if (file.isDirectory()) {
                for (File temp : file.listFiles()) {
                    //System.out.println("count : " + i);
                    //i++;
                    //System.out.println("file name " + i + ":" + temp.getName());
                    if ((temp.toString()).endsWith(FILE_83P_EXT))
                    {
                        setLog(source);
                        //System.out.println("file with 83p ext: " + temp.toString());
                        beApplied = true;
                    }  
                }
            }
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return beApplied;
    }


    @Override
    public ImageIcon getIcon() {
        System.out.println("getIcon: " + mraVtkLogGroup.name());
        return null;
    }

    @Override
    public Double getDefaultTimeStep() {
        System.out.println("get DefaultTimeStep: " + mraVtkLogGroup.name());
        return null;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        System.out.println("supportsVariableTimeSteps: " + mraVtkLogGroup.name());
        return false;
    }

    @Override
    public Type getType() {
        System.out.println("getType: " + mraVtkLogGroup.name());
        return Type.VISUALIZATION;
    }

    @Override
    public void onHide() {
        System.out.println("onHide: " + mraVtkLogGroup.name());
    }

    @Override
    public void onShow() {
        System.out.println("onShow: " + mraVtkLogGroup.name());
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
        System.out.println("onCleanup: " + mraVtkLogGroup.name());
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
    
    private JPanel createToolbar() {
        //JPanel toolbar = new JPanel();
        JPanel toolbar = new JPanel();
        
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.Y_AXIS));
        toolbar.setBackground(Color.WHITE);
        //toolbar.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        //toolbar.setAutoscrolls(true);
        //Rectangle rect = new Rectangle();
        //rect.height = 50;
        //rect.height = 50;
        //toolbar.setBounds(rect);
        
        rawPointsToggle = new JToggleButton(I18n.text("Raw"));
        downsampledPointsToggle = new JToggleButton(I18n.text("Downsampled"));
        zExaggerationToggle = new JToggleButton(I18n.text("Exaggerate Z"));
        resetViewportToggle = new JToggleButton(I18n.text("Reset View"));
        
        rawPointsToggle.setSelected(true);
        downsampledPointsToggle.setSelected(false);
        zExaggerationToggle.setSelected(false);
        resetViewportToggle.setSelected(false);
        
        rawPointsToggle.addActionListener(new ActionListener() {       
            @Override
            public void actionPerformed(ActionEvent e) {
                if (rawPointsToggle.isSelected())
                {
                    
                }
                else
                {
                    
                }
            }
        });
        
        downsampledPointsToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (downsampledPointsToggle.isSelected())
                {
                    
                }
                else
                {
                    
                }
            }
        });
        
        zExaggerationToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (zExaggerationToggle.isSelected())
                {
                    
                }
                else
                {
                    
                }
            }
        });
        
        resetViewportToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (resetViewportToggle.isSelected())
                {
                    
                }
                else
                {
                    
                }
            }
        });
        
        toolbar.add(rawPointsToggle);
        toolbar.add(downsampledPointsToggle);
        toolbar.add(zExaggerationToggle);
        toolbar.add(resetViewportToggle);
        
        return toolbar;
    }
}
