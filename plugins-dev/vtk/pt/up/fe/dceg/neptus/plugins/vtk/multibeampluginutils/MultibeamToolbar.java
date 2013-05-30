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
 * May 30, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.multibeampluginutils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.vtk.Vtk;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.DepthExaggeration;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.ExaggeratePointCloudZ;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.surface.PointCloudMesh;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import vtk.vtkActorCollection;
import vtk.vtkCanvas;
import vtk.vtkLODActor;
import vtk.vtkLookupTable;
import vtk.vtkTextActor;

/**
 * @author hfq
 *
 */
public class MultibeamToolbar {
    private JPanel toolbar;
    
    private JToggleButton rawPointsToggle;
    private JToggleButton zExaggerationToogle;
    private JToggleButton downsamplePointsToogle;
    private JToggleButton meshToogle;
    private JToggleButton smoothingMeshToogle;
    private JToggleButton contoursToogle;
    
    private JButton resetViewportButton;
    private JButton helpButton;
    private JButton configButton;
    
    private vtkCanvas canvas;
    private LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud;
    private LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh;
    
    //private ExaggeratePointCloudZ exaggeZ;
    //private DepthExaggeration exaggeDepth;
    
    private vtkTextActor textProcessingActor;
    private vtkTextActor textZExagInfoActor;
    
    private int currentPtsToIgnore = 0;
    private boolean currentApproachToIgnorePts = false;
    private long currentTimestampMultibeamIncrement = 0;
    private boolean currentYawMultibeamIncrement = false;
    private int currentDepthExaggeValue = 0;
    private int lastDepthExaggeValue = 0;
    
    ToolbarAddons addons;

    private Vtk vtkInit;
    
    /**
     * @param vtkInit
     */
    public MultibeamToolbar(Vtk vtkInit) {
        this.vtkInit = vtkInit;
        this.canvas = vtkInit.vtkCanvas;
        this.linkedHashMapCloud = vtkInit.linkedHashMapCloud;
        this.linkedHashMapMesh = vtkInit.linkedHashMapMesh;
        this.currentApproachToIgnorePts = vtkInit.approachToIgnorePts;
        this.currentPtsToIgnore = vtkInit.ptsToIgnore;
        this.currentTimestampMultibeamIncrement = vtkInit.timestampMultibeamIncrement;
        this.currentYawMultibeamIncrement = vtkInit.yawMultibeamIncrement;
        this.currentDepthExaggeValue = vtkInit.zExaggeration;
        
        this.lastDepthExaggeValue = currentDepthExaggeValue;
        
        addons = new ToolbarAddons();
        textZExagInfoActor = new vtkTextActor();
        addons.setCurrentZexagge(currentDepthExaggeValue);
        addons.buildTextZExagInfoActor(textZExagInfoActor);
        textProcessingActor = new vtkTextActor();
        addons.buildTextProcessingActor(textProcessingActor);
        
        setToolbar(new JPanel());
    }
    
    /**
     * Set up toolbar and internal buttons, toogles and actions
     */
    public void createToolbar() {
        getToolbar().setLayout(new BoxLayout(getToolbar(), BoxLayout.X_AXIS));
        getToolbar().setBackground(Color.LIGHT_GRAY);

        // getToolbar().setAutoscrolls(true);
        // toolbar.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        // Rectangle rect = new Rectangle();
        // rect.height = 50;
        // rect.height = 50;
        // toolbar.setBounds(rect);

        setupToolbarButtonsAndToogles();

        //getToolbar().add(getToolbar(), BorderLayout.PAGE_START);

        // add toogle buttons to toolbar
        getToolbar().add(rawPointsToggle);
        // getToolbar().add(downsamplePointsToogle);
        getToolbar().add(zExaggerationToogle);
        getToolbar().add(meshToogle);
        // getToolbar().add(smoothingMeshToogle);
        // getToolbar().add(contoursToogle);

        getToolbar().add(new JSeparator(JSeparator.VERTICAL), BorderLayout.LINE_START);

        // buttons
        getToolbar().add(resetViewportButton);
        // getToolbar().add(configButton);
        getToolbar().add(helpButton);
    }

    /**
     * create toolbar buttons and tooglebuttons
     */
    private void setupToolbarButtonsAndToogles() {
        rawPointsToggle = new JToggleButton(I18n.text("Raw"));
        downsamplePointsToogle = new JToggleButton(I18n.text("Downsampled"));
        zExaggerationToogle = new JToggleButton(I18n.text("Exaggerate Z"));
        meshToogle = new JToggleButton(I18n.text("Show Mesh"));
        smoothingMeshToogle = new JToggleButton(I18n.text("Perform Mesh Smootihng"));
        contoursToogle = new JToggleButton(I18n.text("Show Terrain Contours"));

        rawPointsToggle.setSelected(true);
        downsamplePointsToogle.setSelected(false);
        zExaggerationToogle.setSelected(false);
        meshToogle.setSelected(false);
        smoothingMeshToogle.setSelected(false);
        contoursToogle.setSelected(false);
        
        resetViewportButton = new JButton(I18n.text("Reset Viewport"));
        helpButton = new JButton(I18n.text("Help"));
        
        addActionsToToogles();
        addActionsToButtons();
    }
    
    /**
     * 
     */
    private void addActionsToToogles() {
        
        rawPointsToggle.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                Set<String> setOfClouds = linkedHashMapCloud.keySet();
                if (rawPointsToggle.isSelected()) {
                    try {
                        for (String sKey : setOfClouds) {
                            canvas.lock();
                            canvas.GetRenderer().AddActor(linkedHashMapCloud.get(sKey).getCloudLODActor());
                            canvas.unlock();
                        }
                        canvas.lock();
                        canvas.GetRenderer().ResetCamera();
                        canvas.Render();
                        canvas.unlock();
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                else {
                    try {
                        for (String sKey : setOfClouds) {
                            canvas.lock();
                            canvas.GetRenderer().RemoveActor(linkedHashMapCloud.get(sKey).getCloudLODActor());
                            canvas.Render();
                            canvas.unlock();
                        }
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        
        zExaggerationToogle.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (zExaggerationToogle.isSelected()) {
                    lastDepthExaggeValue = currentDepthExaggeValue;
                    addons.setCurrentZexagge(lastDepthExaggeValue);
                    
                    if (!rawPointsToggle.isSelected() && !meshToogle.isSelected()) {
                        String msgErrorMultibeam = I18n.text("No Pointcloud or Mesh on renderer\n Please load one or press raw or mesh toogle if you hava already loaded a log");
                        JOptionPane.showMessageDialog(null, msgErrorMultibeam);
                    }
                    else {
                        canvas.lock();
                        textZExagInfoActor.SetDisplayPosition(10, canvas.getHeight() - 20);
                        canvas.GetRenderer().AddActor(textZExagInfoActor);
                        textProcessingActor.SetDisplayPosition(canvas.getWidth() / 3, canvas.getHeight() / 2);
                        canvas.Render();
                        canvas.unlock();
                        
                        performActionDepthExaggeration();
                        
                        canvas.lock();
                        canvas.GetRenderer().ResetCamera();
                        canvas.Render();
                        canvas.unlock();
                    }
                }
                else {                  
                    performActionReverseDepthexaggeration();
                    
                    canvas.lock();
                    canvas.GetRenderer().RemoveActor(textProcessingActor);
                    canvas.GetRenderer().RemoveActor(textZExagInfoActor);
                    canvas.GetRenderer().ResetCamera();
                    canvas.Render();
                    canvas.unlock();
                }                
            }
        });
    }
    
    private void performActionReverseDepthexaggeration() {
        try {
            // search for rendered pointclouds or meshes
            vtkActorCollection actorCollection = new vtkActorCollection();
            actorCollection = canvas.GetRenderer().GetActors();
            actorCollection.InitTraversal();

            if (rawPointsToggle.isSelected()) {      
                for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {                   
                    vtkLODActor tempActor = new vtkLODActor();
                    tempActor = (vtkLODActor) actorCollection.GetNextActor();
                    Set<String> setOfClouds = linkedHashMapCloud.keySet();
                    for (String sKey : setOfClouds) {
                        if (tempActor.equals(linkedHashMapCloud.get(sKey).getCloudLODActor())) {
                            canvas.lock();
                            DepthExaggeration.reverseDepthExaggeration(linkedHashMapCloud.get(sKey).getPoly(),
                                    currentDepthExaggeValue);
                            canvas.unlock();
                        }
                    }
                }
            }
            else if (meshToogle.isSelected()) {               
                for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                    vtkLODActor tempActor = new vtkLODActor();
                    tempActor = (vtkLODActor) actorCollection.GetNextActor();
                    Set<String> setOfMeshs = linkedHashMapMesh.keySet();
                    for (String skey : setOfMeshs) {
                        if (tempActor.equals(linkedHashMapMesh.get(skey).getMeshCloudLODActor())) {
                            canvas.lock();
                            DepthExaggeration.reverseDepthExaggeration(linkedHashMapMesh.get(skey).getPolyData(),
                                    lastDepthExaggeValue);
                            canvas.unlock();   
                        }
                    }
                }
            }
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
    }
    
    private void performActionDepthExaggeration() {
        try {           
            // search for rendered pointclouds or meshes
            vtkActorCollection actorCollection = new vtkActorCollection();
            actorCollection = canvas.GetRenderer().GetActors();
            actorCollection.InitTraversal();

            if (rawPointsToggle.isSelected()) {
                for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                    vtkLODActor tempActor = new vtkLODActor();
                    tempActor = (vtkLODActor) actorCollection.GetNextActor();
                    Set<String> setOfClouds = linkedHashMapCloud.keySet();
                    for (String sKey : setOfClouds) {
                        if (tempActor.equals(linkedHashMapCloud.get(sKey).getCloudLODActor())) {
                            canvas.lock();
                            DepthExaggeration.performDepthExaggeration(linkedHashMapCloud.get(sKey).getPoly(),
                                    currentDepthExaggeValue);
                            canvas.unlock();
                        }
                    }
                }
            }
            else if (meshToogle.isSelected()) {
                for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                    vtkLODActor tempActor = new vtkLODActor();
                    tempActor = (vtkLODActor) actorCollection.GetNextActor();
                    Set<String> setOfMeshs = linkedHashMapMesh.keySet();
                    for (String skey : setOfMeshs) {
                        if (tempActor.equals(linkedHashMapMesh.get(skey).getMeshCloudLODActor())) {
                            // DepthExaggeration exaggeDepth = new
                            // DepthExaggeration(linkedHashMapMesh.get(skey).getPolyData(), lastDepthExaggeValue);
                            // exaggeDepth.performDepthExaggeration();
                            canvas.lock();
                            DepthExaggeration.performDepthExaggeration(linkedHashMapMesh.get(skey).getPolyData(),
                                    lastDepthExaggeValue);
                            canvas.unlock();   
                        }
                    }
                }
            }
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Adds actions to toolbar buttons
     */
    private void addActionsToButtons() {
        
        helpButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {              
                GuiUtils.htmlMessage(ConfigFetch.getSuperParentFrame() == null ? vtkInit : ConfigFetch.getSuperParentAsFrame()
                        , "Help for the 3D visualization interaction", "(3D Multibeam keyboard and mouse interaction)", addons.msgHelp(), ModalityType.MODELESS);
            }
        });
        
        resetViewportButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.lock();
                canvas.GetRenderer().GetActiveCamera().SetViewUp(0.0, 0.0, -1.0);
                canvas.GetRenderer().ResetCamera();
                canvas.Render();
                canvas.unlock();
            }
        });
        
//        configButton = new JButton(new AbstractAction(I18n.text("Configure")) {    
//            private static final long serialVersionUID = -1404112253602290953L;
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                PropertiesEditor.editProperties(vtk,
//                        SwingUtilities.getWindowAncestor(vtk), true);
//
//                if (ptsToIgnore != currentPtsToIgnore || approachToIgnorePts != currentApproachToIgnorePts || timestampMultibeamIncrement != currentTimestampMultibeamIncrement || yawMultibeamIncrement != currentYawMultibeamIncrement) {
//                    if (vtk.noBeamsTxtActor != null)
//                        canvas.GetRenderer().RemoveActor(vtk.noBeamsTxtActor);
//                    
//                    pointCloud = linkedHashMapCloud.get("multibeam");
//
//                    canvas.lock();
//                    canvas.GetRenderer().RemoveActor(pointCloud.getCloudLODActor());
//                    if (zExaggerationToggle.isSelected()) {
//                        exaggeZ.reverseZExaggeration();
//                        canvas.GetRenderer().RemoveActor(textProcessingActor);
//                        zExaggerationToggle.setSelected(false);
//                    }
//                    canvas.unlock();
//                            
//                    textProcessingActor.SetDisplayPosition(canvas.getWidth() / 3, canvas.getHeight() / 2);
//                    canvas.GetRenderer().AddActor(textProcessingActor);
//                    canvas.Render();
//                    
//                
//                        // clean up point cloud
//                    pointCloud.setPoints(new vtkPoints());
//                    pointCloud.setVerts(new vtkCellArray());
//                    pointCloud.setPoly(new vtkPolyData());
//                    pointCloud.setCloudLODActor(new vtkLODActor());
//                    pointCloud.setColorHandler(new PointCloudHandlers<>());
//                    
//                    vtk.multibeamToPointCloud = new MultibeamToPointCloud(vtk.getLog(), pointCloud);
//                    vtk.multibeamToPointCloud.parseMultibeamPointCloud(vtk.approachToIgnorePts, vtk.ptsToIgnore, vtk.timestampMultibeamIncrement, vtk.yawMultibeamIncrement);
//                    
//                    if (pointCloud.getNumberOfPoints() != 0) {
//                        if (vtk.noBeamsText != null) {
//                            canvas.lock();
//                            canvas.GetRenderer().RemoveActor(vtk.noBeamsText.getText3dActor());
//                            canvas.unlock();
//                        }
//                        pointCloud.createLODActorFromPoints();    
//                        canvas.lock(); 
//                        canvas.GetRenderer().RemoveActor(textProcessingActor);
//                        
//                        canvas.GetRenderer().AddActor(pointCloud.getCloudLODActor());
//                        
//                        vtk.winCanvas.getInteractorStyle().getScalarBar().setUpScalarBarLookupTable(pointCloud.getColorHandler().getLutZ());
//                        canvas.GetRenderer().AddActor(vtk.winCanvas.getInteractorStyle().getScalarBar().getScalarBarActor());
//                        
//                        canvas.GetRenderer().ResetCamera();
//                        canvas.Render();
//                        canvas.unlock();
//                    }
//                    else {
//                        canvas.lock();
//                        canvas.GetRenderer().RemoveActor(textProcessingActor);
//                        canvas.unlock();
//                        
//                        if (vtk.noBeamsText != null) {
//                            canvas.GetRenderer().AddActor(vtk.noBeamsText.getText3dActor());
//                        }
//                        else {
//                            vtk.noBeamsText = new Text3D();
//                            vtk.noBeamsText.buildText3D("No beams on Log file!", 2.0, 2.0, 2.0, 10.0);
//                            canvas.lock();
//                            canvas.GetRenderer().AddActor(vtk.noBeamsText.getText3dActor());
//                            canvas.unlock();
//                        }
//                    }
//                    currentPtsToIgnore = vtk.ptsToIgnore;
//                    currentApproachToIgnorePts = vtk.approachToIgnorePts;
//                    currentTimestampMultibeamIncrement = vtk.timestampMultibeamIncrement;
//                    currentYawMultibeamIncrement = vtk.yawMultibeamIncrement;
//                }
//            }
        
    }



    /**
     * @return the toolbar
     */
    public JPanel getToolbar() {
        return toolbar;
    }

    /**
     * @param toolbar the toolbar to set
     */
    public void setToolbar(JPanel toolbar) {
        this.toolbar = toolbar;
    }
    
}
