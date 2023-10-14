/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: hfq
 * Jan 24, 2014
 */
package pt.lsts.neptus.plugins.vtk;

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
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.vtk.io.Writer3D;
import pt.lsts.neptus.vtk.mravisualizer.EventsHandler;
import pt.lsts.neptus.vtk.mravisualizer.VisAction;
import pt.lsts.neptus.vtk.pointcloud.APointCloud;
import pt.lsts.neptus.vtk.surface.PointCloudMesh;
import pt.lsts.neptus.vtk.utils.File3DUtils;
import pt.lsts.neptus.vtk.utils.File3DUtils.FileType;
import pt.lsts.neptus.vtk.visualization.Canvas;
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

    private final VtkMRAVis vtkInit;
    private final Canvas canvas;
    private final vtkRenderer renderer;
    private final EventsHandler events;
    private final LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud;
    private final LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh;

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
    private AbstractAction takeSnapShot;
    // private AbstractAction exaggerateZ, performMeshing, performSmoothing;

    // Help Menu
    private AbstractAction help;

    /**
     * 
     * @param vtkInit
     */
    public Vis3DMenuBar(VtkMRAVis vtkInit) {
        this.vtkInit = vtkInit;
        this.events = vtkInit.getEvents();
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

        JLabel label = new JLabel(I18n.text("3D Bathymetry Visualization"));
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
                JFileChooser chooser = GuiUtils.getFileChooser(vtkInit.getLog().getDir(), I18n.text("3D files"), File3DUtils.TYPES_3D_FILES); 

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
                            APointCloud<?> pointCloud = linkedHashMapCloud.get(cloudsKey);
                            if (tempActor.equals(pointCloud.getCloudLODActor())) {
                                poly = pointCloud.getPolyData();
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

                chooser.setFileFilter(filefilter);
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
        resetViewportCamera = new VisAction(I18n.text("Reset Viewport"), ImageUtils.getIcon("images/menus/camera.png"),
                I18n.text("Reset Viewport") + ".", KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK,
                        true)) {

            @Override
            public void actionPerformed(ActionEvent e) {
                events.resetViewport();
            }
        };
        viewMenu.add(resetViewportCamera);
    }

    /**
     * 
     */
    @SuppressWarnings("serial")
    private void setUpToolsMenu() {
        toolsMenu = new JMenu(I18n.text("Tools"));
        // takeSnapShot = new VisAction(I18n.text("Take SnapShot"), ImageUtils.getIcon("images/menus/camera.png"),
        // I18n.text("Take SnapShot from current Viewport") + ".", KeyStroke.getKeyStroke(KeyEvent.VK_J,
        // InputEvent.CTRL_DOWN_MASK, true)) {
        takeSnapShot = new VisAction(I18n.text("Take SnapShot"), ImageUtils.getIcon("images/menus/camera.png"),
                I18n.text("Take SnapShot from current Viewport") + ".") {

            @Override
            public void actionPerformed(ActionEvent e) {
                events.takeSnapShot("Bathymetry_");
            }
        };
        toolsMenu.add(takeSnapShot);
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
                                "(" + I18n.text("3D Multibeam keyboard and mouse interation") + ")", events.getMsgHelp(),
                                ModalityType.MODELESS);

            }
        };

        helpMenu.add(help);
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
