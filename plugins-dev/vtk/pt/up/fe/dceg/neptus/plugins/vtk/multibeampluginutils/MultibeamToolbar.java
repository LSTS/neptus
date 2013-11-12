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
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.plugins.vtk.Vtk;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.DepthExaggeration;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.surface.Delauny2D;
import pt.up.fe.dceg.neptus.plugins.vtk.surface.MeshSmoothingLaplacian;
import pt.up.fe.dceg.neptus.plugins.vtk.surface.PointCloudMesh;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.Canvas;
import vtk.vtkActorCollection;
import vtk.vtkLODActor;
import vtk.vtkPolyData;
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

    // private vtkCanvas canvas;
    private Canvas canvas;
    private LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud;
    private LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh;

    // private ExaggeratePointCloudZ exaggeZ;
    // private DepthExaggeration exaggeDepth;

    private vtkTextActor textProcessingActor;
    private vtkTextActor textZExagInfoActor;

    protected int currentPtsToIgnore = 0;
    protected boolean currentApproachToIgnorePts = false;
    protected long currentTimestampMultibeamIncrement = 0;
    protected boolean currentYawMultibeamIncrement = false;
    private int currentDepthExaggeValue = 0;
    private int lastDepthExaggeValue = 0;

    ToolbarAddons addons;

    private Vtk vtkInit;

    /**
     * @param vtkInit
     */
    public MultibeamToolbar(Vtk vtkInit) {
        this.vtkInit = vtkInit;
        // this.canvas = vtkInit.vtkCanvas;
        this.canvas = vtkInit.canvas;
        this.linkedHashMapCloud = vtkInit.linkedHashMapCloud;
        this.linkedHashMapMesh = vtkInit.linkedHashMapMesh;
        // this.currentApproachToIgnorePts = vtkInit.approachToIgnorePts;
        this.currentApproachToIgnorePts = NeptusMRA.approachToIgnorePts;
        // this.currentPtsToIgnore = vtkInit.ptsToIgnore;
        this.currentPtsToIgnore = NeptusMRA.ptsToIgnore;
        // this.currentTimestampMultibeamIncrement = vtkInit.timestampMultibeamIncrement;
        this.currentTimestampMultibeamIncrement = NeptusMRA.timestampMultibeamIncrement;
        // this.currentYawMultibeamIncrement = vtkInit.yawMultibeamIncrement;
        this.currentYawMultibeamIncrement = NeptusMRA.yawMultibeamIncrement;
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

        // getToolbar().add(getToolbar(), BorderLayout.PAGE_START);

        // add toogle buttons to toolbar
        getToolbar().add(rawPointsToggle);
        // getToolbar().add(downsamplePointsToogle);
        getToolbar().add(zExaggerationToogle);
        getToolbar().add(meshToogle);
        getToolbar().add(smoothingMeshToogle);
        // getToolbar().add(contoursToogle);

        getToolbar().add(new JSeparator(JSeparator.VERTICAL), BorderLayout.LINE_START);

        // buttons
        getToolbar().add(resetViewportButton);
        getToolbar().add(configButton);
        getToolbar().add(helpButton);
    }

    /**
     * create toolbar buttons and tooglebuttons
     */
    private void setupToolbarButtonsAndToogles() {
        try {
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
        }
        catch (Exception e) {
            e.printStackTrace();
        }

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
                    // NeptusLog.pub().info("current depth exagge: " + currentDepthExaggeValue);
                    // NeptusLog.pub().info("lastDepthExaggeVue: " + lastDepthExaggeValue);

                    lastDepthExaggeValue = currentDepthExaggeValue;
                    // addons.setCurrentZexagge(lastDepthExaggeValue);
                    addons.setCurrentZexagge(vtkInit.zExaggeration);

                    // NeptusLog.pub().info("lastDepthExaggeVue 2: " + lastDepthExaggeValue);

                    if (!rawPointsToggle.isSelected() && !meshToogle.isSelected()) {
                        String msgErrorMultibeam = I18n
                                .text("No Pointcloud or Mesh on renderer\n Please load one or press raw or mesh toogle if you hava already loaded a log");
                        JOptionPane.showMessageDialog(null, msgErrorMultibeam);
                        zExaggerationToogle.setSelected(false);
                    }
                    else {
                        try {
                            canvas.lock();
                            textZExagInfoActor.SetDisplayPosition(10, canvas.getHeight() - 20);
                            canvas.GetRenderer().AddActor(textZExagInfoActor);
                            textProcessingActor.SetDisplayPosition(canvas.getWidth() / 3, canvas.getHeight() / 2);
                            canvas.GetRenderer().AddActor(textProcessingActor);
                            canvas.Render();
                            canvas.unlock();
                        }
                        catch (Exception e1) {
                            e1.printStackTrace();
                        }

                        performActionDepthExaggeration();

                        try {
                            canvas.lock();
                            canvas.GetRenderer().RemoveActor(textProcessingActor);
                            canvas.GetRenderer().ResetCamera();
                            canvas.Render();
                            canvas.unlock();
                        }
                        catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                else {
                    try {
                        canvas.lock();
                        textProcessingActor.SetDisplayPosition(canvas.getWidth() / 3, canvas.getHeight() / 2);
                        canvas.GetRenderer().AddActor(textProcessingActor);
                        canvas.Render();
                        canvas.unlock();
                    }
                    catch (Exception e2) {
                        e2.printStackTrace();
                    }

                    performActionReverseDepthexaggeration();

                    try {
                        canvas.lock();
                        canvas.GetRenderer().RemoveActor(textProcessingActor);
                        canvas.GetRenderer().RemoveActor(textZExagInfoActor);
                        canvas.GetRenderer().ResetCamera();
                        canvas.Render();
                        canvas.unlock();
                    }
                    catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        meshToogle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (meshToogle.isSelected()) {
                    canvas.lock();
                    canvas.GetRenderer().AddActor(textProcessingActor);
                    textProcessingActor.SetDisplayPosition(canvas.getWidth() / 3, canvas.getHeight() / 2);
                    canvas.Render();
                    canvas.unlock();

                    performActionMeshing();

                    canvas.lock();
                    canvas.GetRenderer().ResetCamera();
                    canvas.GetRenderer().RemoveActor(textProcessingActor);
                    canvas.Render();
                    canvas.unlock();
                }
                else {
                    Set<String> setOfMeshs = linkedHashMapMesh.keySet();
                    for (String sKey : setOfMeshs) {
                        canvas.lock();
                        canvas.GetRenderer().RemoveActor(linkedHashMapMesh.get(sKey).getMeshCloudLODActor());
                        canvas.Render();
                        canvas.unlock();
                    }
                }
            }
        });

        smoothingMeshToogle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (smoothingMeshToogle.isSelected()) {
                    canvas.lock();
                    textProcessingActor.SetDisplayPosition(canvas.getWidth() / 3, canvas.getHeight() / 2);
                    canvas.GetRenderer().AddActor(textProcessingActor);
                    canvas.Render();
                    canvas.unlock();

                    performActionMeshSmoothing();

                    canvas.lock();
                    canvas.GetRenderer().RemoveActor(textProcessingActor);
                    canvas.GetRenderer().ResetCamera();
                    canvas.Render();
                    canvas.unlock();
                }
                else
                    smoothingMeshToogle.setSelected(true);
            }
        });

        contoursToogle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (contoursToogle.isSelected()) {
                    if (meshToogle.isSelected()) {
                        canvas.lock();
                        textProcessingActor.SetDisplayPosition(canvas.getWidth() / 3, canvas.getHeight() / 2);
                        canvas.GetRenderer().AddActor(textProcessingActor);
                        canvas.Render();
                        canvas.unlock();

                        performActionAddContours();

                        canvas.lock();
                        canvas.GetRenderer().RemoveActor(textProcessingActor);
                        canvas.GetRenderer().ResetCamera();
                        canvas.Render();
                        canvas.unlock();

                    }
                    else {
                        contoursToogle.setSelected(false);
                        String msgErrorMultibeam = I18n
                                .text("No Mesh on renderer\n Please load one or press raw or mesh toogle if you hava already loaded a log");
                        JOptionPane.showMessageDialog(null, msgErrorMultibeam);
                    }
                }
                else {

                }
            }
        });
    }

    private void performActionAddContours() {
        vtkActorCollection actorCollection = new vtkActorCollection();
        actorCollection = canvas.GetRenderer().GetActors();
        actorCollection.InitTraversal();

        Set<String> setOfMeshs = linkedHashMapMesh.keySet();

        for (String sKey : setOfMeshs) {

            for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                vtkLODActor tempActor = new vtkLODActor();
                tempActor = (vtkLODActor) actorCollection.GetNextActor();
                if (linkedHashMapMesh.get(sKey).getMeshCloudLODActor().equals(tempActor)) {
                    linkedHashMapMesh.get(sKey).getContours().generateTerrainContours(linkedHashMapMesh.get(sKey));
                    canvas.lock();
                    canvas.GetRenderer().AddActor(linkedHashMapMesh.get(sKey).getContours().getIsolinesActor());
                    canvas.Render();
                    canvas.unlock();
                }
            }

        }
    }

    private void performActionMeshSmoothing() {
        if (meshToogle.isSelected()) {

            vtkActorCollection actorCollection = new vtkActorCollection();
            actorCollection = canvas.GetRenderer().GetActors();
            actorCollection.InitTraversal();

            for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                vtkLODActor tempActor = new vtkLODActor();
                tempActor = (vtkLODActor) actorCollection.GetNextActor();
                Set<String> setOfMeshes = linkedHashMapMesh.keySet();
                for (String sKey : setOfMeshes) {
                    PointCloudMesh mesh = linkedHashMapMesh.get(sKey);
                    if (tempActor.equals(mesh.getMeshCloudLODActor())) {
                        MeshSmoothingLaplacian smoothing = new MeshSmoothingLaplacian();
                        smoothing.performProcessing(mesh);

                        canvas.lock();
                        canvas.GetRenderer().RemoveActor(mesh.getMeshCloudLODActor());
                        canvas.unlock();

                        mesh.setPolyData(new vtkPolyData());
                        mesh.generateLODActorFromPolyData(smoothing.getPolyData());
                        canvas.lock();
                        canvas.GetRenderer().AddActor(mesh.getMeshCloudLODActor());
                        canvas.unlock();
                    }
                }
            }
        }
        else {
            String msgErrorMultibeam = I18n.text("Add a mesh to renderer to perform smoothing");
            JOptionPane.showMessageDialog(null, msgErrorMultibeam);
            smoothingMeshToogle.setSelected(false);
        }
    }

    private void performActionMeshing() {
        try {
            boolean hasFound = false;

            if (smoothingMeshToogle.isSelected()) {
                Set<String> setOfMeshs = linkedHashMapMesh.keySet();
                for (String sKey : setOfMeshs) {
                    canvas.lock();
                    canvas.GetRenderer().AddActor(linkedHashMapMesh.get(sKey).getMeshCloudLODActor());
                    canvas.Render();
                    canvas.unlock();
                    hasFound = true;
                }
            }

            if (!linkedHashMapCloud.isEmpty()) {
                vtkActorCollection actorCollection = new vtkActorCollection();
                actorCollection = canvas.GetRenderer().GetActors();
                actorCollection.InitTraversal();

                for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                    vtkLODActor tempActor = new vtkLODActor();
                    tempActor = (vtkLODActor) actorCollection.GetNextActor();
                    Set<String> setOfClouds = linkedHashMapCloud.keySet();
                    for (String sKey : setOfClouds) {
                        if (linkedHashMapMesh.get(sKey) != null) {
                            NeptusLog.pub().info("Meshing on this cloud already perfomed");
                            canvas.lock();
                            canvas.GetRenderer().RemoveActor(linkedHashMapCloud.get(sKey).getCloudLODActor());
                            canvas.GetRenderer().AddActor(linkedHashMapMesh.get(sKey).getMeshCloudLODActor());
                            canvas.unlock();
                            hasFound = true;
                        }
                        else {
                            if (tempActor.equals(linkedHashMapCloud.get(sKey).getCloudLODActor())) {
                                NeptusLog.pub().info("Create Mesh from pointcloud");

                                Delauny2D delauny = new Delauny2D();
                                delauny.performDelauny(linkedHashMapCloud.get(sKey));

                                PointCloudMesh mesh = new PointCloudMesh();
                                mesh.generateLODActorFromPolyData(delauny.getPolyData());

                                linkedHashMapMesh.put(linkedHashMapCloud.get(sKey).getCloudName(), mesh);

                                canvas.lock();
                                canvas.GetRenderer().RemoveActor(linkedHashMapCloud.get(sKey).getCloudLODActor());
                                canvas.GetRenderer().AddActor(mesh.getMeshCloudLODActor());
                                canvas.unlock();
                                hasFound = true;
                            }
                        }
                    }
                }
            }
            else {
                String msgErrorMultibeam = I18n.text("No Pointclouds to perform meshing");
                JOptionPane.showMessageDialog(null, msgErrorMultibeam);
                meshToogle.setSelected(false);
            }
            if (!hasFound) {
                String msgErrorMultibeam = I18n.text("No Pointclouds to perform meshing");
                JOptionPane.showMessageDialog(null, msgErrorMultibeam);
                meshToogle.setSelected(false);
            }
            else {
                rawPointsToggle.setSelected(false);
            }
        }
        catch (HeadlessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Performs DepthExaggeration on rendered pointcloud or mesh (when available on renderer)
     */
    private void performActionDepthExaggeration() {
        try {
            // search for rendered pointclouds or meshes
            vtkActorCollection actorCollection = new vtkActorCollection();
            actorCollection = canvas.GetRenderer().GetActors();
            actorCollection.InitTraversal();

            boolean hasFound = false;

            if (rawPointsToggle.isSelected()) {
                for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                    vtkLODActor tempActor = new vtkLODActor();
                    tempActor = (vtkLODActor) actorCollection.GetNextActor();
                    Set<String> setOfClouds = linkedHashMapCloud.keySet();
                    for (String sKey : setOfClouds) {
                        if (tempActor.equals(linkedHashMapCloud.get(sKey).getCloudLODActor())) {
                            canvas.lock();
                            NeptusLog.pub().info("current Depth on perform action: " + currentDepthExaggeValue);
                            NeptusLog.pub().info("last Depth on perform action: " + lastDepthExaggeValue);

                            DepthExaggeration.performDepthExaggeration(linkedHashMapCloud.get(sKey).getPoly(),
                                    lastDepthExaggeValue);
                            canvas.unlock();
                            hasFound = true;
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
                            // DepthExaggeration.performDepthExaggeration(linkedHashMapMesh.get(skey).getPolyData(),
                            // lastDepthExaggeValue);
                            DepthExaggeration.performDepthExaggeration(linkedHashMapCloud.get(skey).getPoly(),
                                    lastDepthExaggeValue);
                            canvas.unlock();
                            hasFound = true;
                        }
                    }
                }
            }
            if (!hasFound)
                zExaggerationToogle.setSelected(false);
        }
        catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Reverts DepthExaggeration on rendered pointcloud or mesh (when available on renderer)
     */
    private void performActionReverseDepthexaggeration() {
        try {
            // search for rendered pointclouds or meshes
            vtkActorCollection actorCollection = new vtkActorCollection();
            actorCollection = canvas.GetRenderer().GetActors();
            actorCollection.InitTraversal();

            boolean hasFound = false;

            if (rawPointsToggle.isSelected()) {
                for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                    vtkLODActor tempActor = new vtkLODActor();
                    tempActor = (vtkLODActor) actorCollection.GetNextActor();
                    Set<String> setOfClouds = linkedHashMapCloud.keySet();
                    for (String sKey : setOfClouds) {
                        if (tempActor.equals(linkedHashMapCloud.get(sKey).getCloudLODActor())) {
                            canvas.lock();
                            NeptusLog.pub().info("current Depth on perform reverse action: " + currentDepthExaggeValue);
                            NeptusLog.pub().info("last Depth on perform reverse action: " + lastDepthExaggeValue);

                            DepthExaggeration.reverseDepthExaggeration(linkedHashMapCloud.get(sKey).getPoly(),
                                    lastDepthExaggeValue);
                            canvas.unlock();
                            hasFound = true;
                        }
                    }
                }
            }
            else if (meshToogle.isSelected()) {
                for (int i = 0; i < actorCollection.GetNumberOfItems(); ++i) {
                    vtkLODActor tempActor = new vtkLODActor();
                    tempActor = (vtkLODActor) actorCollection.GetNextActor();
                    Set<String> setOfMeshs = linkedHashMapMesh.keySet();
                    for (String sKey : setOfMeshs) {
                        if (tempActor.equals(linkedHashMapMesh.get(sKey).getMeshCloudLODActor())) {
                            canvas.lock();
                            // DepthExaggeration.reverseDepthExaggeration(linkedHashMapMesh.get(sKey).getPolyData(),
                            // lastDepthExaggeValue);
                            DepthExaggeration.reverseDepthExaggeration(linkedHashMapCloud.get(sKey).getPoly(),
                                    lastDepthExaggeValue);
                            canvas.unlock();
                            hasFound = true;
                        }
                    }
                }
            }

            if (!hasFound)
                zExaggerationToogle.setSelected(true);
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
                GuiUtils.htmlMessage(
                        ConfigFetch.getSuperParentFrame() == null ? vtkInit : ConfigFetch.getSuperParentAsFrame(),
                        "Help for the 3D visualization interaction", "(3D Multibeam keyboard and mouse interaction)",
                        addons.msgHelp(), ModalityType.MODELESS);
            }
        });

        resetViewportButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    canvas.lock();
                    canvas.GetRenderer().GetActiveCamera().SetViewUp(0.0, 0.0, -1.0);
                    canvas.GetRenderer().ResetCamera();
                    canvas.Render();
                    canvas.unlock();
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        configButton = new JButton(new AbstractAction(I18n.text("Configure")) {
            private static final long serialVersionUID = -1404112253602290953L;

            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(vtkInit, SwingUtilities.getWindowAncestor(vtkInit), true);
                if (vtkInit.zExaggeration != currentDepthExaggeValue) {
                    currentDepthExaggeValue = vtkInit.zExaggeration;
                }

            }
        });

        // configButton = new JButton(new AbstractAction(I18n.text("Configure")) {
        // private static final long serialVersionUID = -1404112253602290953L;
        //
        // @Override
        // public void actionPerformed(ActionEvent e) {
        // PropertiesEditor.editProperties(vtk,
        // SwingUtilities.getWindowAncestor(vtk), true);
        //
        // if (ptsToIgnore != currentPtsToIgnore || approachToIgnorePts != currentApproachToIgnorePts ||
        // timestampMultibeamIncrement != currentTimestampMultibeamIncrement || yawMultibeamIncrement !=
        // currentYawMultibeamIncrement) {
        // if (vtk.noBeamsTxtActor != null)
        // canvas.GetRenderer().RemoveActor(vtk.noBeamsTxtActor);
        //
        // pointCloud = linkedHashMapCloud.get("multibeam");
        //
        // canvas.lock();
        // canvas.GetRenderer().RemoveActor(pointCloud.getCloudLODActor());
        // if (zExaggerationToggle.isSelected()) {
        // exaggeZ.reverseZExaggeration();
        // canvas.GetRenderer().RemoveActor(textProcessingActor);
        // zExaggerationToggle.setSelected(false);
        // }
        // canvas.unlock();
        //
        // textProcessingActor.SetDisplayPosition(canvas.getWidth() / 3, canvas.getHeight() / 2);
        // canvas.GetRenderer().AddActor(textProcessingActor);
        // canvas.Render();
        //
        //
        // // clean up point cloud
        // pointCloud.setPoints(new vtkPoints());
        // pointCloud.setVerts(new vtkCellArray());
        // pointCloud.setPoly(new vtkPolyData());
        // pointCloud.setCloudLODActor(new vtkLODActor());
        // pointCloud.setColorHandler(new PointCloudHandlers<>());
        //
        // vtk.multibeamToPointCloud = new MultibeamToPointCloud(vtk.getLog(), pointCloud);
        // vtk.multibeamToPointCloud.parseMultibeamPointCloud(vtk.approachToIgnorePts, vtk.ptsToIgnore,
        // vtk.timestampMultibeamIncrement, vtk.yawMultibeamIncrement);
        //
        // if (pointCloud.getNumberOfPoints() != 0) {
        // if (vtk.noBeamsText != null) {
        // canvas.lock();
        // canvas.GetRenderer().RemoveActor(vtk.noBeamsText.getText3dActor());
        // canvas.unlock();
        // }
        // pointCloud.createLODActorFromPoints();
        // canvas.lock();
        // canvas.GetRenderer().RemoveActor(textProcessingActor);
        //
        // canvas.GetRenderer().AddActor(pointCloud.getCloudLODActor());
        //
        // vtk.winCanvas.getInteractorStyle().getScalarBar().setUpScalarBarLookupTable(pointCloud.getColorHandler().getLutZ());
        // canvas.GetRenderer().AddActor(vtk.winCanvas.getInteractorStyle().getScalarBar().getScalarBarActor());
        //
        // canvas.GetRenderer().ResetCamera();
        // canvas.Render();
        // canvas.unlock();
        // }
        // else {
        // canvas.lock();
        // canvas.GetRenderer().RemoveActor(textProcessingActor);
        // canvas.unlock();
        //
        // if (vtk.noBeamsText != null) {
        // canvas.GetRenderer().AddActor(vtk.noBeamsText.getText3dActor());
        // }
        // else {
        // vtk.noBeamsText = new Text3D();
        // vtk.noBeamsText.buildText3D("No beams on Log file!", 2.0, 2.0, 2.0, 10.0);
        // canvas.lock();
        // canvas.GetRenderer().AddActor(vtk.noBeamsText.getText3dActor());
        // canvas.unlock();
        // }
        // }
        // currentPtsToIgnore = vtk.ptsToIgnore;
        // currentApproachToIgnorePts = vtk.approachToIgnorePts;
        // currentTimestampMultibeamIncrement = vtk.timestampMultibeamIncrement;
        // currentYawMultibeamIncrement = vtk.yawMultibeamIncrement;
        // }
        // }
        // });

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
