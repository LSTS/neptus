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
import java.awt.ComponentOrientation;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.sun.java.swing.plaf.windows.resources.windows;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.mra3d.Marker3d;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZI;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.Axes;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.AxesActor;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.BoxWidget;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.Compass;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.CubeAxes;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.Window;
import vtk.vtk3DWidget;
import vtk.vtkActor;
import vtk.vtkBoxRepresentation;
import vtk.vtkBoxWidget;
import vtk.vtkBoxWidget2;
import vtk.vtkCellArray;
import vtk.vtkCompassRepresentation;
import vtk.vtkCompassWidget;
import vtk.vtkDelaunay2D;
import vtk.vtkDelaunay3D;
import vtk.vtkGeoAssignCoordinates;
import vtk.vtkIdList;
import vtk.vtkIdTypeArray;
import vtk.vtkNativeLibrary;
import vtk.vtkPanel;
import vtk.vtkPointSource;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkRenderWindow;
import vtk.vtkRenderWindowInteractor;
import vtk.vtkRenderer;
import vtk.vtkSettings;
import vtk.vtkVertexGlyphFilter;

/**
 * @author hfq
 *
 */
@PluginDescription(author = "hfq", name = "Vtk")
public class Vtk extends JPanel implements MRAVisualization {
    private static final long serialVersionUID = 1L;
    
    public vtkPanel vtkPanel;
    
    private JToggleButton zExaggerationToggle;
    private JToggleButton rawPointsToggle;
    private JToggleButton downsampledPointsToggle;
    private JToggleButton resetViewportToggle;  
    private JPanel toolBar;
    
    private static Path path = null;
    private static final String FILE_83P_EXT = ".83P";
    
    protected Vector<Marker3d> markers = new Vector<>();
    protected IMraLogGroup mraVtkLogGroup;
    
    static {
        System.loadLibrary("jawt");
        
        // for simple visualizations
        vtkNativeLibrary.COMMON.LoadLibrary();
        vtkNativeLibrary.FILTERING.LoadLibrary();
        vtkNativeLibrary.IO.LoadLibrary();
        vtkNativeLibrary.IMAGING.LoadLibrary();
        vtkNativeLibrary.GRAPHICS.LoadLibrary();
        vtkNativeLibrary.RENDERING.LoadLibrary();
                
        // Other
        vtkNativeLibrary.INFOVIS.LoadLibrary();
        vtkNativeLibrary.VIEWS.LoadLibrary();
        vtkNativeLibrary.WIDGETS.LoadLibrary();
        vtkNativeLibrary.GEOVIS.LoadLibrary();

    }
    
    /**
     * Ideia: se for pretendido colocar vários actores no render fazer
     * um HashMap<String, Actor>
     * @param panel
     */
    public Vtk(MRAPanel panel) {
        super(new BorderLayout());
   
        vtkPanel = new vtkPanel();
        vtkPanel.setBackground(Color.blue);
        Window win = new Window(vtkPanel);
        //win.setColorBackGround();

        
        // a Render Window
//        vtkRenderWindow renWin = new vtkRenderWindow();
//        renWin.AddRenderer(vtkPanel.GetRenderer());
//        
        // an interactor
//        vtkRenderWindowInteractor renderWinInteractor = new vtkRenderWindowInteractor();
//        renderWinInteractor.SetRenderWindow(renWin);
//        
//        Compass.addCompassToVisualization(renderWinInteractor);
        
//        vtkGeoAssignCoordinates geoAssignCoords = new vtkGeoAssignCoordinates();
//        //geoAssignCoords.set
//        
//        vtkPoints points = new vtkPoints();     
//        float x = (float) 5.0;
//        float y = (float) 1.0;
//        float z = (float) 10.0;
//        PointXYZ p = new PointXYZ(x, y, z);
//        int id = 1;
//        //points.InsertPoint(id, 0.0, 0.0, 0.0);
//        points.InsertNextPoint(p.getX(), p.getY(), p.getZ());
//        
//        //vtkCellArray verts = new vtkCellArray();
//        //vtkIdTypeArray idTypes = new vtkIdTypeArray();
//        
//        
//        vtkPolyData poly = new vtkPolyData();
//        poly.SetPoints(points);
//        
//        // apply vtkVertexGlyphFilter to make cells around points, vtk only render cells
//        vtkVertexGlyphFilter vertexGlyphFilter = new vtkVertexGlyphFilter();
//        vertexGlyphFilter.SetInput(poly);
//        
//        vtkPolyDataMapper mapper = new vtkPolyDataMapper();
//        //mapper.SetInput(poly);
//        mapper.SetInputConnection(vertexGlyphFilter.GetOutputPort());
//        
//        vtkActor pointsActor = new vtkActor();
//        pointsActor.SetMapper(mapper);
//
//        pointsActor.GetProperty().SetPointSize(5.0);
//        vtkPanel.GetRenderer().AddActor(pointsActor);
//        
//        vtkPointSource pointSource = new vtkPointSource();
//        pointSource.SetNumberOfPoints(150000);
//        //pointSource.SetCenter(0.0, 0.0, 0.0);
//        pointSource.SetDistributionToUniform();
//        //pointSource.SetDistributionToShell();
//        pointSource.SetRadius(10.0);
//
//        //polyData = pointSource.GetPolyDataInput(ALLBITS);
//        
//        vtkPolyDataMapper inputMapper = new vtkPolyDataMapper();
//        inputMapper.SetInput(pointSource.GetOutput());
//          
//        vtkActor actor2 = new vtkActor();
//        actor2.SetMapper(inputMapper);
//        
//        
//        vtkPoints points2 = getPoints();
//        vtkPolyData polyData = new vtkPolyData();
//        
//        polyData.SetPoints(points2);
//        
//        vtkCellArray verts = new vtkCellArray();
//        vtkIdTypeArray idTypeArray = new vtkIdTypeArray();
//        
//        
//        
//        
//        //vtkDelaunay2D delauny = new vtkDelaunay2D();
//        //vtkDelaunay3D delauny = new vtkDelaunay3D(); -> dá erro
//        //delauny.SetInputConnection(polyData.GetProducerPort());
//        
//        vtkPolyDataMapper mapper2 = new vtkPolyDataMapper();
//        mapper2.SetInput(polyData);
//        //mapper2.SetInputConnection(delauny.GetOutputPort());
//        
//        vtkActor actor_2 = new vtkActor();
//        actor_2.SetMapper(mapper2);
//        
//        actor_2.GetProperty().SetPointSize(20);
//        actor_2.GetProperty().SetColor(1.0, 0.0, 0.0);
//        
//        vtkPanel.GetRenderer().AddActor(CubeAxes.AddCubeAxesToVisualizer(vtkPanel.GetRenderer(), polyData));
//        vtkPanel.GetRenderer().AddActor(actor_2);
//        
//        
//        //vtkPanel.GetRenderer().AddActor(actor2);
// 
        BoxWidget.addBoxWidget2Tovisualizer(vtkPanel.GetRenderer(), win.getRenWinInteractor());
        
        Axes ax = new Axes();
        AxesActor axesActor = new AxesActor(vtkPanel.GetRenderer());
        axesActor.setAxesVisibility(true);
        
//        vtkBoxWidget widget1 = new vtkBoxWidget();
//        vtkBoxWidget2 widget2 = new vtkBoxWidget2();
//        
//        vtkBoxRepresentation boxrep = new vtkBoxRepresentation();
//        widget1.SetCurrentRenderer(vtkPanel.GetRenderer());
//        widget1.SetPlaceFactor(1.25);
//        widget1.PlaceWidget();
//        //widget1.SetInput();
//        widget1.EnabledOn();
        
        //widget2.SetCurrentRenderer(vtkPanel.GetRenderer());
        //widget2.SetRepresentation(boxrep);
            // estoira completamente
        //widget2.AddObserver("EndInteractionEvent", this, "selectPolygons");
        

        //boxWidget.SetPlaceFactor(1.25);
        //boxWidget.SetInput(glyph.GetOutput());
        //boxWidget.PlaceWidget();
        //boxWidget.AddObserver("EndInteractionEvent", this, "selectPolygons");
        
        //widget2.SetInteractor(win.getRenWinInteractor());
        //widget2.EnabledOn();
        //widget1.ComputeDisplayToWorld(id0, id1, id2, id3, id4)
        
        //vtk3DWidget widget = new vtk3DWidget();
        //widget.SetCurrentRenderer(vtkPanel.GetRenderer());
        //widget.EnabledOn();
        //widget.SetInteractor(win.getRenWinInteractor());
        //widget.ComputeDisplayToWorld(vtkPanel.GetRenderer(), 0.0, 0.0, 0.0);
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
        //vtkPanel.GetRenderer().AddActor(ax.getAxesActor());
        
        PointCloud<PointXYZI> poi = new PointCloud<>();
        
        vtkPanel.GetRenderer().ResetCamera();
        //vtkPanel.GetRenderer().ResetCameraClippingRange();
        //vtkPanel.GetRenderer().LightFollowCameraOn();
        //vtkPanel.GetRenderer().VisibleActorCount();
        //vtkPanel.GetRenderer().ViewToDisplay();

        add(vtkPanel, BorderLayout.CENTER);
        //vtkPanel.setBackground(Color.blue);
        //vtkPanel.setForeground(Color.green);
        
        toolBar = new JPanel();
        toolBar = createToolbar();
        add(toolBar, BorderLayout.EAST);
        vtkPanel.setBackground(Color.blue);
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
