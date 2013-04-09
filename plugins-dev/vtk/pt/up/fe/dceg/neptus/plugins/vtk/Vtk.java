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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.mra3d.Marker3d;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.Axes;
import vtk.vtkActor;
import vtk.vtkGeoAssignCoordinates;
import vtk.vtkNativeLibrary;
import vtk.vtkPanel;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;

/**
 * @author hfq
 *
 */
@PluginDescription(author = "hfq", name = "Vtk")
public class Vtk extends JPanel implements MRAVisualization {
    private static final long serialVersionUID = 1L;
    
    private vtkPanel vtkPanel;
    
    private JToggleButton zExaggerationToggle;
    private JToggleButton rawPointsToggle;
    private JToggleButton downsampledPointsToggle;
    private JToggleButton resetViewportToggle;
    
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
    
    public Vtk(MRAPanel panel) {
        super(new BorderLayout());
        
        vtkGeoAssignCoordinates geoAssignCoords = new vtkGeoAssignCoordinates();
        //geoAssignCoords.set
        
        vtkPoints points = new vtkPoints();     
        float x = (float) 5.0;
        float y = (float) 1.0;
        float z = (float) 10.0;
        PointXYZ p = new PointXYZ(x, y, z);
        int id = 1;
        //points.InsertPoint(id, 0.0, 0.0, 0.0);
        points.InsertNextPoint(p.getX(), p.getY(), p.getZ());
        
        vtkPolyData poly = new vtkPolyData();
        poly.SetPoints(points);
        
        vtkPolyDataMapper mapper = new vtkPolyDataMapper();
        mapper.SetInput(poly);
        
        vtkActor pointsActor = new vtkActor();
        pointsActor.SetMapper(mapper);
        
        
/*        vtkConeSource cone = new vtkConeSource();
        cone.SetResolution(8);

        vtkPolyDataMapper coneMapper = new vtkPolyDataMapper();
        coneMapper.SetInputConnection(cone.GetOutputPort());

        vtkActor coneActor = new vtkActor();
        coneActor.SetMapper(coneMapper);*/
        
        vtkPanel = new vtkPanel();
        
        //vtkPanel.GetRenderer().AddActor(coneActor);

        pointsActor.GetProperty().SetPointSize(5.0);
        vtkPanel.GetRenderer().AddActor(pointsActor);
 
        Axes ax = new Axes();
        vtkPanel.GetRenderer().AddActor(ax.getAxesActor());
        
        vtkPanel.GetRenderer().ResetCamera();
        //vtkPanel.GetRenderer().ResetCameraClippingRange();
        vtkPanel.GetRenderer().LightFollowCameraOn();
        vtkPanel.GetRenderer().VisibleActorCount();
        //vtkPanel.GetRenderer().ViewToDisplay();
        
        add(vtkPanel, BorderLayout.CENTER);
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
        JPanel toolbar = new JPanel();
        
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
