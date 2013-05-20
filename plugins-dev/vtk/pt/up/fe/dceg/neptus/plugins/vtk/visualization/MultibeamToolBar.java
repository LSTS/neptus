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
 * May 9, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.visualization;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.NeptusMRA;
import pt.up.fe.dceg.neptus.plugins.vtk.filters.Contours;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.ExaggeratePointCloudZ;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import vtk.vtkActorCollection;
import vtk.vtkCanvas;
import vtk.vtkLODActor;
import vtk.vtkTextActor;

/**
 * @author hfq
 * 
 */
public class MultibeamToolBar {

    private JToggleButton zExaggerationToggle;
    private JToggleButton rawPointsToggle;
    private JToggleButton downsampledPointsToggle;
    private JToggleButton meshToogle;
    private JToggleButton contoursToogle;
    private JButton resetViewportButton;
    private JButton helpButton;
    private JPanel toolBar;

    private vtkCanvas canvas;
    private LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud;
    // private boolean isLogMultibeam;

    private PointCloud<PointXYZ> pointCloud;
    private ExaggeratePointCloudZ exaggeZ;

    private vtkTextActor textProcessingActor;
    private vtkTextActor textZExagInfoActor;
    

    public MultibeamToolBar(vtkCanvas canvas, LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud) {
        this.canvas = canvas;
        this.linkedHashMapCloud = linkedHashMapCloud;

        this.textProcessingActor = new vtkTextActor();
        buildTextProcessingActor();
        
        this.textZExagInfoActor = new vtkTextActor();
        buildTextZExagInfoActor();
        
        setToolBar(new JPanel());
    }

    /**
     * 
     */
    private void buildTextZExagInfoActor() {
        textZExagInfoActor.GetTextProperty().BoldOn();
        textZExagInfoActor.GetTextProperty().ItalicOn();
        textZExagInfoActor.GetTextProperty().SetColor(1.0, 1.0, 1.0);
        textZExagInfoActor.GetTextProperty().SetFontFamilyToArial();
        textZExagInfoActor.GetTextProperty().SetFontSize(12);
        //textZExagInfoActor.GetTextProperty().SetJustificationToRight();
        //textZExagInfoActor.GetTextProperty().SetJustificationToCentered();
        //textZExagInfoActor.UseBoundsOn();
        //textZExagInfoActor.SetDisplayPosition(10,  300);
        textZExagInfoActor.SetPosition2(0.9, 0.8);
        textZExagInfoActor.SetInput("Depth multiplied by:" + NeptusMRA.zExaggeration);   //  
        textZExagInfoActor.VisibilityOn();
    }

    /**
     * 
     */
    private void buildTextProcessingActor() {
       textProcessingActor.GetTextProperty().BoldOn();
       textProcessingActor.GetTextProperty().ItalicOn();
       textProcessingActor.GetTextProperty().SetColor(1.0, 0.0, 0.0);
       textProcessingActor.GetTextProperty().SetFontSize(48);
       textProcessingActor.GetTextProperty().SetFontFamilyToArial();
       //textProcessingActor.GetTextProperty().SetVerticalJustificationToCentered();

       textProcessingActor.SetInput("Processing data...");
       //textProcessingActor.UseBorderAlignOn();
       textProcessingActor.SetPosition(0.4, 0.9);
       
       //textProcessingActor.SetDisplayPosition(100, 150);
       textProcessingActor.VisibilityOn();   
    }

    /**
     * @return
     */
    public void createToolBar() {
        getToolBar().setLayout(new BoxLayout(getToolBar(), BoxLayout.X_AXIS));
        getToolBar().setBackground(Color.DARK_GRAY);

        // toolbar.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        // toolbar.setAutoscrolls(true);
        // Rectangle rect = new Rectangle();
        // rect.height = 50;
        // rect.height = 50;
        // toolbar.setBounds(rect);

        rawPointsToggle = new JToggleButton(I18n.text("Raw"));
        rawPointsToggle.setBounds(getToolBar().getX(), getToolBar().getY(), getToolBar().getWidth(), 10);
        downsampledPointsToggle = new JToggleButton(I18n.text("Downsampled"));
        downsampledPointsToggle.setBounds(rawPointsToggle.getBounds());

        zExaggerationToggle = new JToggleButton(I18n.text("Exaggerate Z"));

        meshToogle = new JToggleButton(I18n.text("Show Mesh"));
        contoursToogle = new JToggleButton(I18n.text("Show terrain contours"));

        resetViewportButton = new JButton(I18n.text("Reset Viewport"));
        helpButton = new JButton(I18n.text("Help"));

        rawPointsToggle.setSelected(true);
        downsampledPointsToggle.setSelected(false);
        meshToogle.setSelected(false);
        zExaggerationToggle.setSelected(false);
        contoursToogle.setSelected(false);

        // resetViewportToggle.setSelected(false);

        helpButton.setSize(10, 10);
        helpButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String msgHelp;
                msgHelp = "\tHelp for the 3D visualization interaction:\n\n";
                msgHelp = msgHelp + "Key    -   description\n";
                msgHelp = msgHelp + "p, P   -   switch to a point-based representation\n";
                msgHelp = msgHelp + "w, W   -   switch to a wireframe-based representation, when available\n";
                msgHelp = msgHelp + "s, S   -   switch to a surface-based representation, when available\n";
                msgHelp = msgHelp + "j, J   -   take a .PNG snapshot of the current window view\n";
                msgHelp = msgHelp + "g, G   -   display scale grid (on/off)\n";
                msgHelp = msgHelp + "u, U   -   display lookup table (on/off)\n";
                msgHelp = msgHelp + "r, R   -   reset camera (to viewpoint = {0, 0, 0} -> center {x, y, z}\n";
                msgHelp = msgHelp + "i, I   -   information about rendered cloud\n";
                msgHelp = msgHelp + "f, F   -   Fly Mode - press right mouse and then f, to fly to point picked\n";
                msgHelp = msgHelp + "+/-    -   Increment / Decrement overall point size\n";
                msgHelp = msgHelp + "3      -   3D visualization (put the 3D glasses on)\n";
                msgHelp = msgHelp + "7      -   color gradient in relation with X coords (north)\n";
                msgHelp = msgHelp + "8      -   color gradient in relation with Y coords (west)\n";
                msgHelp = msgHelp + "9      -   color gradient in relation with Z coords (depth)\n";

                JOptionPane.showMessageDialog(null, msgHelp);
            }
        });

        rawPointsToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (rawPointsToggle.isSelected()) {

                }
                else {

                }
            }
        });

        downsampledPointsToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (downsampledPointsToggle.isSelected()) {
                    // try {
                    // System.out.println("Before collection");
                    // vtkActorCollection actorCollection = new vtkActorCollection();
                    // actorCollection = vtkCanvas.GetRenderer().GetActors();
                    // actorCollection.InitTraversal();
                    // for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                    // vtkCanvas.GetRenderer().RemoveActor(actorCollection.GetNextActor());
                    // }
                    // System.out.println("After collection");
                    //
                    // vtkCanvas.GetRenderer().Render();
                    //
                    // PointCloud<PointXYZ> downsampledCloud = new PointCloud<>();
                    //
                    // if (!isDownsampleDone) {
                    // PointCloud<PointXYZ> multibeamCloud = new PointCloud<>();
                    // multibeamCloud = linkedHashMapCloud.get("multibeam");
                    //
                    // performDownsample = new DownsamplePointCloud(multibeamCloud, 0.5);
                    //
                    // downsampledCloud = performDownsample.getOutputDownsampledCloud();
                    // linkedHashMapCloud.put(downsampledCloud.getCloudName(), downsampledCloud);
                    // }
                    // vtkCanvas.GetRenderer().AddActor(downsampledCloud.getCloudLODActor());
                    // }
                    // catch (Exception e1) {
                    // e1.printStackTrace();
                    // }
                }
                else {

                }
            }
        });

        meshToogle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (meshToogle.isSelected()) {
                    // NeptusLog.pub().info("<###> Number of actors on render: " +
                    // actorCollection.GetNumberOfItems());
                    //
                    // canvas.GetRenderer().RemoveAllViewProps();
                    // canvas.GetRenderWindow().Render();
                    //
                    // GaussianSplat gaussSplat = new GaussianSplat(linkedHashMapCloud.get("multibeam"));
                    // gaussSplat.performGaussianSplat(20, 20, 20, 0.3);
                    // //for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                    // // vtkActor actor = actorCollection.GetNextActor();
                    // //System.out.println("actor num: " + i + "actor.string: " + actor.toString());
                    //
                    //
                    // //vtkCanvas.GetRenderer().RemoveActor(actorCollection.GetNextActor());
                    // //}
                    //
                    // canvas.GetRenderer().AddActor(gaussSplat.getActorGaussianSplat());          
                    try {
                        vtkActorCollection actorCollection = new vtkActorCollection();
                        actorCollection = canvas.GetRenderer().GetActors();
                        actorCollection.InitTraversal();

                        canvas.GetRenderer().AddActor(textProcessingActor);
                        canvas.Render();
                        
                        for(int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                            vtkLODActor tempActor = new vtkLODActor();
                            tempActor = (vtkLODActor) actorCollection.GetNextActor();
                            Set<String> setOfClouds;
                            setOfClouds = linkedHashMapCloud.keySet();
                            for (String sKey : setOfClouds) {
                                pointCloud = linkedHashMapCloud.get(sKey);
                                if (tempActor.equals(pointCloud.getCloudLODActor())) {
                                    
                                }
                            }
                        }
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                else {

                }
            }
        });

        zExaggerationToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (zExaggerationToggle.isSelected()) {
                    try {
                        vtkActorCollection actorCollection = new vtkActorCollection();
                        actorCollection = canvas.GetRenderer().GetActors();
                        actorCollection.InitTraversal();
                        canvas.GetRenderer().AddActor(textZExagInfoActor);
                        
                        for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                            vtkLODActor tempActor = new vtkLODActor();
                            tempActor = (vtkLODActor) actorCollection.GetNextActor();
                            Set<String> setOfClouds;
                            setOfClouds = linkedHashMapCloud.keySet();
                            for (String sKey : setOfClouds) {
                                pointCloud = linkedHashMapCloud.get(sKey);
                                if (tempActor.equals(pointCloud.getCloudLODActor())) {
                                    pointCloud.getCloudLODActor().VisibilityOff();
                                    canvas.GetRenderer().AddActor(textProcessingActor);
                                    canvas.Render();
                                    exaggeZ = new ExaggeratePointCloudZ(pointCloud);
                                    exaggeZ.performZExaggeration();
                                    canvas.GetRenderer().RemoveActor(textProcessingActor);
                                    canvas.Render();
                                    canvas.GetRenderer().ResetCamera();
                                    pointCloud.getCloudLODActor().VisibilityOn();
                                    canvas.GetRenderer().ResetCamera();
                                    canvas.Render();
                                }
                                
                            }
                        }
                        
                        
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                else {
                    try {                
                        vtkActorCollection actorCollection = new vtkActorCollection();
                        actorCollection = canvas.GetRenderer().GetActors();
                        actorCollection.InitTraversal();
                        canvas.GetRenderer().RemoveActor(textZExagInfoActor);
                        
                        for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                            vtkLODActor tempActor = new vtkLODActor();
                            tempActor = (vtkLODActor) actorCollection.GetNextActor();
                            Set<String> setOfClouds;
                            setOfClouds = linkedHashMapCloud.keySet();
                            for (String sKey : setOfClouds) {
                                pointCloud = linkedHashMapCloud.get(sKey);
                                if (tempActor.equals(pointCloud.getCloudLODActor())) {
                                    pointCloud.getCloudLODActor().VisibilityOff();
                                    canvas.GetRenderer().AddActor(textProcessingActor);
                                    canvas.Render();
                                    exaggeZ.reverseZExaggeration();
                                    canvas.GetRenderer().RemoveActor(textProcessingActor);
                                    canvas.Render();
                                    canvas.GetRenderer().ResetCamera();
                                    pointCloud.getCloudLODActor().VisibilityOn();
                                    canvas.GetRenderer().ResetCamera();
                                    canvas.Render();
                                }
                            }
                        }
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        
        contoursToogle.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (contoursToogle.isSelected()) {
                    try {
                        vtkActorCollection actorCollection = new vtkActorCollection();
                        actorCollection = canvas.GetRenderer().GetActors();
                        actorCollection.InitTraversal();
                        
                        for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                            vtkLODActor tempActor = new vtkLODActor();
                            tempActor = (vtkLODActor) actorCollection.GetNextActor();
                            Set<String> setOfClouds;
                            setOfClouds = linkedHashMapCloud.keySet();
                            for (String sKey : setOfClouds) {
                                pointCloud = linkedHashMapCloud.get(sKey);
                                if (tempActor.equals(pointCloud.getCloudLODActor())) {
                                    Contours contours = new Contours(pointCloud);
                                    contours.generateTerrainContours2();
                                    canvas.GetRenderer().AddActor(contours.planeActor);
                                    canvas.Render();
                                    canvas.GetRenderer().ResetCamera();
                                }
                            }
                        }
                    }
                    catch (Exception e1) {
                        
                    }
                }
                else {
                    
                }
            }
        });
        

        resetViewportButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.GetRenderer().ResetCamera();
                canvas.getRenderWindowInteractor().Render();
            }
        });

            // toogles
        getToolBar().add(rawPointsToggle);
        //getToolBar().add(downsampledPointsToggle);
        //getToolBar().add(meshToogle);
        getToolBar().add(zExaggerationToggle);
        //getToolBar().add(contoursToogle);
            // buttons
        getToolBar().add(resetViewportButton);
        getToolBar().add(helpButton);
    }

    /**
     * @return the toolBar
     */
    public JPanel getToolBar() {
        return toolBar;
    }

    /**
     * @param toolBar the toolBar to set
     */
    private void setToolBar(JPanel toolBar) {
        this.toolBar = toolBar;
    }
}
