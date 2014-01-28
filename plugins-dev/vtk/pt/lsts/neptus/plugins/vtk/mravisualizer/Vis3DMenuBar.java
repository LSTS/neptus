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
 * Jan 24, 2014
 */
package pt.lsts.neptus.plugins.vtk.mravisualizer;

import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.vtk.Vtk;
import pt.lsts.neptus.plugins.vtk.io.Writer3D;
import pt.lsts.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.lsts.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.lsts.neptus.plugins.vtk.surface.PointCloudMesh;
import pt.lsts.neptus.plugins.vtk.utils.File3DUtils;
import pt.lsts.neptus.plugins.vtk.utils.File3DUtils.FileType;
import pt.lsts.neptus.plugins.vtk.visualization.Canvas;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import vtk.vtkActorCollection;
import vtk.vtkLODActor;
import vtk.vtkPolyData;
import vtk.vtkRenderer;

/**
 * @author hfq
 * 
 */
public class Vis3DMenuBar extends JMenuBar {

    private static final long serialVersionUID = 1L;

    private Vtk vtkInit;
    private Canvas canvas;
    private vtkRenderer renderer;
    private LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud;
    private LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh;

    private JMenu fileMenu, editMenu, viewMenu, toolsMenu, helpMenu;

    // File Menu
    private AbstractAction saveFile, saveFileAsPointCloud, saveFileAsMesh;
    // Edit Menu
    private AbstractAction configs;

    // View Menu
    private AbstractAction resetViewportCamera;
    // , incrementPointSize, decrementPointSize, colorGradX, colorGradY,
    // colorGradZ, viewPointCloud, viewMesh, pointBasedRep, wireframeRep, surfaceRep, displayLookUpTable,
    // displayScaleGrid, displayInfoPointcloud;

    // Tools Menu
    // private AbstractAction exaggerateZ, performMeshing, performSmoothing;

    // Help Menu
    private AbstractAction help;

    /**
     * 
     * @param vtkInit
     */
    public Vis3DMenuBar(Vtk vtkInit) {
        this.vtkInit = vtkInit;
        this.canvas = vtkInit.getCanvas();
        this.renderer = vtkInit.getCanvas().GetRenderer();
        this.linkedHashMapCloud = vtkInit.getLinkedHashMapCloud();
        this.linkedHashMapMesh = vtkInit.getLinkedHashMapMesh();
    }

    public void createMenuBar() {
        setUpFileMenu();
        setUpEditMenu();
        setUpViewMenu();
        setUpToolsMenu();
        setUpHelpMenu();

        JLabel label = new JLabel(I18n.text("3D Visualization"));
        label.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        label.setFont(new Font("Verdana", Font.ITALIC + Font.BOLD, 10));
        label.setForeground(Color.BLACK);

        add(label);
        add(fileMenu);
        add(editMenu);
        add(viewMenu);
        add(toolsMenu);
        add(helpMenu);
    }

    /**
     * set up File Menu
     */
    @SuppressWarnings("serial")
    private void setUpFileMenu() {
        fileMenu = new JMenu(I18n.text("File"));

        // FIXME - is it necessary? - Save wherever is on rendereder
        saveFile = new VisAction(I18n.text("Save file"), ImageUtils.getIcon("images/menus/save.png"),
                I18n.text("Save file"), KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, true)) {

            @Override
            public void actionPerformed(ActionEvent e) {

            }
        };

        saveFileAsPointCloud = new VisAction(I18n.text("Save pointcloud as") + "...",
                ImageUtils.getIcon("images/menus/saveas.png"), I18n.text("Save a pointcloud to a file") + ".",
                KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK, true)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(vtkInit.getLog().getFile("Data.lsf").getParentFile());

                // FileFilter filefilter = GuiUtils.getCustomFileFilter(I18n.text("3D files ") + "*.vtk" + ", *.stl"
                // + ", *.ply" + ", *.obj" + ", *.wrl" + " *.x3d", new String[] { "X3D", "VTK", "STL", "PLY", "OBJ",
                // "WRL" });

                FileFilter filefilter = GuiUtils.getCustomFileFilter(I18n.text("3D files ") + "*.vtk" + ", *.stl"
                        + ", *.ply" + ", *.obj" + ", *.wrl" + " *.x3d", File3DUtils.TYPES_3D_FILES);

                chooser.setFileFilter((FileFilter) filefilter);

                int ans = chooser.showDialog(vtkInit, I18n.text("Save as") + "...");
                if (ans == JFileChooser.APPROVE_OPTION) {
                    if (chooser.getSelectedFile().exists()) {
                        ans = JOptionPane.showConfirmDialog(vtkInit,
                                I18n.text("Are you sure you want to overwrite existing file") + "?",
                                I18n.text("Save file as") + "...", JOptionPane.YES_OPTION);
                        if (ans != JOptionPane.YES_OPTION)
                            return;
                    }
                    File dst = chooser.getSelectedFile();
                    String ext = File3DUtils.getExtension(dst);
                    NeptusLog.pub().info("Extension: " + ext);
                    File3DUtils.FileType type = null;
                    type = File3DUtils.getFileType(ext);
                    NeptusLog.pub().info("Filetype: " + type.toString());
                    Writer3D writer3d = new Writer3D();

                    // getting polydata from rendered cloud
                    vtkPolyData poly = new vtkPolyData();
                    vtkActorCollection actorCollection = new vtkActorCollection();
                    actorCollection = renderer.GetActors();
                    actorCollection.InitTraversal();
                    for (int i = 0, numItems = actorCollection.GetNumberOfItems(); i < numItems; ++i) {
                        vtkLODActor tempActor = new vtkLODActor();
                        tempActor = (vtkLODActor) actorCollection.GetNextActor();
                        Set<String> setOfClouds = linkedHashMapCloud.keySet();
                        for (String cloudsKey : setOfClouds) {
                            PointCloud<PointXYZ> pointCloud = linkedHashMapCloud.get(cloudsKey);
                            if (tempActor.equals(pointCloud.getCloudLODActor())) {
                                poly = pointCloud.getPoly();
                            }
                        }
                    }
                    writer3d.save3dFileType(type, chooser.getSelectedFile(), poly, canvas);
                }
            }
        };

        saveFileAsMesh = new VisAction(I18n.text("Save generated pointcloud mesh as") + "...",
                ImageUtils.getIcon("images/menus/saveas.png"), I18n.text("Save mesh to a file") + ".",
                KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK, true)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(vtkInit.getLog().getFile("Data.lsf").getParentFile());
                FileFilter filefilter = GuiUtils.getCustomFileFilter(I18n.text("3D files ") + "*.vtk" + ", *.stl"
                        + ", *.ply" + ", *.obj" + ", *.wrl" + " *.x3d", File3DUtils.TYPES_3D_FILES);

                chooser.setFileFilter((FileFilter) filefilter);
                int ans = chooser.showDialog(vtkInit, I18n.text("Save as") + "...");
                if (ans == JFileChooser.APPROVE_OPTION) {
                    if (chooser.getSelectedFile().exists()) {
                        ans = JOptionPane.showConfirmDialog(vtkInit,
                                I18n.text("Are you sure you want to overwrite existing file") + "?",
                                I18n.text("Save file as") + "...", JOptionPane.YES_OPTION);
                        if (ans != JOptionPane.YES_OPTION)
                            return;
                    }
                    File dst = chooser.getSelectedFile();
                    String ext = File3DUtils.getExtension(dst);
                    NeptusLog.pub().info("Extension: " + ext);
                    FileType type = File3DUtils.getFileType(ext);
                    NeptusLog.pub().info("Filetype: " + type.toString());
                    Writer3D writer3d = new Writer3D();

                    // getting polydata from rendered cloud
                    vtkPolyData poly = new vtkPolyData();
                    vtkActorCollection actorCollection = new vtkActorCollection();
                    actorCollection = renderer.GetActors();
                    actorCollection.InitTraversal();
                    for (int i = 0, numItems = actorCollection.GetNumberOfItems(); i < numItems; ++i) {
                        vtkLODActor tempActor = new vtkLODActor();
                        tempActor = (vtkLODActor) actorCollection.GetNextActor();
                        Set<String> setOfMeshs = linkedHashMapMesh.keySet();
                        for (String meshsKey : setOfMeshs) {
                            PointCloudMesh pointCloud = linkedHashMapMesh.get(meshsKey);
                            if (tempActor.equals(pointCloud.getMeshCloudLODActor())) {
                                poly = pointCloud.getPolyData();
                            }
                        }
                    }
                    writer3d.save3dFileType(type, chooser.getSelectedFile(), poly, canvas);
                }
            }
        };

        fileMenu.add(saveFile);
        fileMenu.addSeparator();
        fileMenu.add(saveFileAsPointCloud);
        fileMenu.add(saveFileAsMesh);
    }

    /**
     * Set Up Edit Menu
     */
    @SuppressWarnings("serial")
    private void setUpEditMenu() {
        editMenu = new JMenu(I18n.text("Edit"));
        configs = new VisAction(I18n.text("Configurations"), ImageUtils.getIcon("images/menus/configure.png"),
                I18n.text("3DVisualizer configurations") + ".", KeyStroke.getKeyStroke(KeyEvent.VK_E,
                        InputEvent.CTRL_DOWN_MASK, true)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(vtkInit, true);

            }
        };
        editMenu.add(configs);
    }

    /**
     * 
     */
    @SuppressWarnings("serial")
    private void setUpViewMenu() {
        viewMenu = new JMenu(I18n.text("View"));
        resetViewportCamera = new VisAction(I18n.text("Reset Viewport"), ImageUtils.getIcon("images/menus/camera.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    canvas.lock();
                    renderer.GetActiveCamera().SetViewUp(0.0, 0.0, -1.0);
                    renderer.ResetCamera();
                    canvas.Render();
                    canvas.unlock();
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        };
        viewMenu.add(resetViewportCamera);
    }

    /**
     * 
     */
    private void setUpToolsMenu() {
        toolsMenu = new JMenu(I18n.text("Tools"));

    }

    /**
     * 
     */
    @SuppressWarnings("serial")
    private void setUpHelpMenu() {
        helpMenu = new JMenu(I18n.text("Help"));

        help = new VisAction(I18n.text("Help"), ImageUtils.getIcon("images/menus/info.png"),
                I18n.text("Help 3D Visualizer") + ".", KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                GuiUtils.htmlMessage(
                        ConfigFetch.getSuperParentFrame() == null ? vtkInit : ConfigFetch.getSuperParentAsFrame(),
                                I18n.text("3D Visualization Interaction Help") + ".",
                                I18n.text("(3D Multibeam keyboard and mouse interation)"), msgHelp(), ModalityType.MODELESS);

            }
        };

        helpMenu.add(help);
    }

    private String msgHelp() {
        String msgHelp;
        // <h1>3D Multibeam Interaction</h1>
        msgHelp = "<html><font size='2'><br><div align='center'><table border='1' align='center'>"
                + "<tr><th>Keys</th><th>Description</th></tr>"
                + "<tr><td>p, P</td><td>Switch to a point-based representation</td>"
                + "<tr><td>w, W </td><td>Switch to a wireframe-based representation, when available</td>"
                + "<tr><td>s, S</td><td>Switch to a surface-based representation, when available</td>"
                + "<tr><td>j, J</td><td>Take a .PNG snapshot of the current window view</td>"
                + "<tr><td>g, G</td><td>Display scale grid (on/off)</td>"
                + "<tr><td>u, U</td><td>Display lookup table (on/off)</td>"
                + "<tr><td>r, R</td><td>Reset camera view along the current view direction</td>"
                + // (to viewpoint = {0, 0, 0} -> center {x, y, z}\n");
                "<tr><td>i, I</td><td>Information about rendered cloud</td>"
                + "<tr><td>f, F</td><td>Fly Mode - point with mouse cursor the direction and press 'f' to fly</td>"
                + "<tr><td>+/-</td><td>Increment / Decrement overall point size</td>"
                + "<tr><td>3</td><td>Toggle into an out of stereo mode</td>"
                + "<tr><td>7</td><td>Color gradient in relation with X coords (north)</td>"
                + "<tr><td>8</td><td>Color gradient in relation with Y coords (west)</td>"
                + "<tr><td>9</td><td>Color gradient in relation with Z coords (depth)</td>"
                + "<tr><th>Mouse</th><th>Description</th></tr>"
                +
                // rotate the camera around its focal point. The rotation is in the direction defined from the center of
                // the renderer's viewport towards the mouse position
                "<tr><td>Left mouse button</td><td>Rotate camera around its focal point</td>"
                + "<tr><td>Middle mouse button</td><td>Pan camera</td>"
                + "<tr><td>Right mouse button</td><td>Zoom (In/Out) the camera</td>"
                + "<tr><td>Mouse wheel</td><td>Zoom (In/Out) the camera - Static focal point</td>";

        return msgHelp;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphic2d = (Graphics2D) g;
        Color color1 = getBackground();
        Color color2 = Color.GRAY;
        GradientPaint gradPaint = new GradientPaint(0, 0, color1, getWidth(), getHeight(), color2);
        graphic2d.setPaint(gradPaint);
        graphic2d.fillRect(0, 0, getWidth(), getHeight());
    }
}
