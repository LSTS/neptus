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
 * Jan 27, 2014
 */
package pt.lsts.neptus.plugins.vtk.mravisualizer;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.vtk.VtkMRAVis;
import pt.lsts.neptus.plugins.vtk.mravisualizer.EventsHandler.SensorTypeInteraction;
import pt.lsts.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.lsts.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.lsts.neptus.plugins.vtk.visualization.Canvas;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import vtk.vtkRenderer;

/**
 * 
 * @author hfq
 */
@SuppressWarnings("serial")
public class Vis3DToolBar extends JToolBar {
    private static final short ICON_SIZE = 18;

    private static final ImageIcon ICON_MULTIBEAM = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/vtk/assets/multibeam.png", ICON_SIZE, ICON_SIZE);

    private static final ImageIcon ICON_DVL = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/vtk/assets/dvl.png", ICON_SIZE, ICON_SIZE);

    private static final ImageIcon ICON_POINTS = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/vtk/assets/points.png", ICON_SIZE, ICON_SIZE);

    private static final ImageIcon ICON_WIREFRAME = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/vtk/assets/wire.png", ICON_SIZE, ICON_SIZE);

    private static final ImageIcon ICON_SOLID = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/vtk/assets/textures.png", ICON_SIZE, ICON_SIZE);

    private static final ImageIcon ICON_Z = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/vtk/assets/zexag.png", ICON_SIZE, ICON_SIZE);

    private static final ImageIcon ICON_CONTOURS = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/vtk/assets/contours.png", ICON_SIZE, ICON_SIZE);

    private static final ImageIcon ICON_MESHING = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/vtk/assets/meshing.png", ICON_SIZE, ICON_SIZE);

    private static final ImageIcon ICON_SMOOTHING = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/vtk/assets/smoothing.png", ICON_SIZE, ICON_SIZE);

    private VtkMRAVis vtkInit;
    private Canvas canvas;
    private vtkRenderer renderer;
    private EventsHandler events;
    private LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud;

    public JToggleButton multibeamToggle;
    public JToggleButton dvlToggle;

    private JToggleButton rawPointsToggle; // works with pointcloud
    private JToggleButton wireframeToggle; // works with mesh
    private JToggleButton solidToggle; // works with mesh

    private JToggleButton zExaggerationToggle;
    private JToggleButton contoursToggle;

    private JToggleButton meshingToggle;
    private JToggleButton smoothingMeshToggle;

    // private JToggleButton downsamplePointToggle;

    private Vis3DToolBar() {
    }

    /**
     * 
     * @param vtkInit
     */
    public Vis3DToolBar(VtkMRAVis vtkInit) {
        this.vtkInit = vtkInit;
        this.canvas = vtkInit.getCanvas();
        this.renderer = vtkInit.getCanvas().GetRenderer();
        this.events = vtkInit.getEvents();
        this.linkedHashMapCloud = vtkInit.getLinkedHashMapCloud();
    }

    public void createToolBar() {
        setOrientation(JToolBar.VERTICAL);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder()));

        multibeamToggle = new JToggleButton();
        multibeamToggle.setToolTipText(I18n.text("See Multibeam data") + ".");
        // multibeamToggle.setText(I18n.text("M"));
        multibeamToggle.setIcon(ICON_MULTIBEAM);

        dvlToggle = new JToggleButton();
        dvlToggle.setToolTipText(I18n.text("See DVL data") + ".");
        //dvlToggle.setText(I18n.text("D"));
        dvlToggle.setIcon(ICON_DVL);

        //        ButtonGroup groupSensorType = new ButtonGroup();
        //        groupSensorType.add(multibeamToggle);
        //        groupSensorType.add(dvlToggle);

        rawPointsToggle = new JToggleButton();
        rawPointsToggle.setToolTipText(I18n.text("Points based representation") + ".");
        rawPointsToggle.setIcon(ICON_POINTS);

        wireframeToggle = new JToggleButton();
        wireframeToggle.setToolTipText(I18n.text("Wireframe based representation") + ".");
        wireframeToggle.setIcon(ICON_WIREFRAME);

        solidToggle = new JToggleButton();
        solidToggle.setToolTipText(I18n.text("Solid based representation") + ".");
        solidToggle.setIcon(ICON_SOLID);

        ButtonGroup groupRepresentationType = new ButtonGroup();
        groupRepresentationType.add(rawPointsToggle);
        groupRepresentationType.add(wireframeToggle);
        groupRepresentationType.add(solidToggle);

        zExaggerationToggle = new JToggleButton();
        zExaggerationToggle.setToolTipText(I18n.text("Exaggerate Z") + ".");
        zExaggerationToggle.setIcon(ICON_Z);

        contoursToggle = new JToggleButton();
        contoursToggle.setToolTipText(I18n.text("Enable/Disable contouts") + ".");
        contoursToggle.setIcon(ICON_CONTOURS);

        meshingToggle = new JToggleButton();
        meshingToggle.setToolTipText(I18n.text("Perform meshing on pointcloud"));
        meshingToggle.setIcon(ICON_MESHING);

        smoothingMeshToggle = new JToggleButton();
        smoothingMeshToggle.setToolTipText(I18n.text("Perform mesh smoothing") + ".");
        smoothingMeshToggle.setIcon(ICON_SMOOTHING);

        // downsamplePointToggle = new JToggleButton();

        // Add Actions Listeners
        multibeamToggle.addActionListener(sensorTypeInteracActionMutibeam);
        dvlToggle.addActionListener(sensorTypeInteracActionDVL);


        rawPointsToggle.addActionListener(renderRepresentationTypeAction);
        wireframeToggle.addActionListener(renderRepresentationTypeAction);
        solidToggle.addActionListener(renderRepresentationTypeAction);

        // Add Components to toolbar
        add(multibeamToggle);
        add(dvlToggle);

        addSeparator();

        add(rawPointsToggle);
        add(wireframeToggle);
        add(solidToggle);

        addSeparator();

        add(zExaggerationToggle);
        add(contoursToggle);

        addSeparator();

        add(meshingToggle);
        add(smoothingMeshToggle);
    }

    /**
     * Actions for type od sensor choosen
     * FIXME - since toogles are grouped it is not possible to have both clouds... will fix on next iteration
     */
    ActionListener sensorTypeInteracActionMutibeam = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            SensorTypeInteraction sensorTypeInt = events.getSensorTypeInteraction(); 

            if(multibeamToggle.isSelected()) {
                if (dvlToggle.isSelected())
                    sensorTypeInt = SensorTypeInteraction.ALL;
                else
                    sensorTypeInt = SensorTypeInteraction.MULTIBEAM;
                events.setSensorTypeInteraction(sensorTypeInt);
                if(!linkedHashMapCloud.containsKey("multibeam")) {
                    vtkInit.loadCloudBySensorType("multibeam");
                    renderer.AddActor(linkedHashMapCloud.get("multibeam").getCloudLODActor());
                    canvas.Render();
                    //canvas.resetCamera();
                } else {
                    renderer.AddActor(linkedHashMapCloud.get("multibeam").getCloudLODActor());
                    canvas.Render();
                    // canvas.resetCamera();
                }
            }
            else if(!multibeamToggle.isSelected()) {
                if (dvlToggle.isSelected())
                    sensorTypeInt = SensorTypeInteraction.DVL;
                else
                    sensorTypeInt = SensorTypeInteraction.NONE;
                events.setSensorTypeInteraction(sensorTypeInt);
                renderer.RemoveActor(linkedHashMapCloud.get("multibeam").getCloudLODActor());
                canvas.Render();
                // canvas.resetCamera();
            }
            if(multibeamToggle.isSelected() && dvlToggle.isSelected())
                sensorTypeInt = SensorTypeInteraction.ALL;
            if(!multibeamToggle.isSelected() && !dvlToggle.isSelected())
                sensorTypeInt = SensorTypeInteraction.NONE;
        }
    };

    ActionListener sensorTypeInteracActionDVL = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            SensorTypeInteraction sensorTypeInt = events.getSensorTypeInteraction(); 

            if(dvlToggle.isSelected()) {
                if (multibeamToggle.isSelected())
                    sensorTypeInt = SensorTypeInteraction.ALL;
                else
                    sensorTypeInt = SensorTypeInteraction.DVL;
                events.setSensorTypeInteraction(sensorTypeInt);
                if(!linkedHashMapCloud.containsKey("dvl")) {
                    vtkInit.loadCloudBySensorType("dvl");
                    renderer.AddActor(linkedHashMapCloud.get("dvl").getCloudLODActor());
                    canvas.Render();
                    // canvas.resetCamera();
                } else {
                    renderer.AddActor(linkedHashMapCloud.get("dvl").getCloudLODActor());
                    canvas.Render();
                    // canvas.resetCamera();
                }
            }
            else if(!dvlToggle.isSelected()) {
                if(multibeamToggle.isSelected())
                    sensorTypeInt = SensorTypeInteraction.MULTIBEAM;
                else
                    sensorTypeInt = SensorTypeInteraction.NONE;
                events.setSensorTypeInteraction(sensorTypeInt);
                canvas.GetRenderer().RemoveActor(linkedHashMapCloud.get("dvl").getCloudLODActor());
                canvas.Render();
                // canvas.resetCamera();
            }
            if(multibeamToggle.isSelected() && dvlToggle.isSelected())
                sensorTypeInt = SensorTypeInteraction.ALL;
            if(!multibeamToggle.isSelected() && !dvlToggle.isSelected())
                sensorTypeInt = SensorTypeInteraction.NONE;
        }
    };

    /**
     * Actions for actor representation on renderer
     */
    ActionListener renderRepresentationTypeAction = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            if(rawPointsToggle.isSelected()) {

            }
            else if(wireframeToggle.isSelected()) {

            }
            else if(solidToggle.isSelected()) {

            }
        }
    };

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

    public static void main(String[] args) {
        Vis3DToolBar toolbar = new Vis3DToolBar();
        toolbar.createToolBar();
        GuiUtils.testFrame(toolbar, "Test toolbar: " + toolbar.getClass().getSimpleName(), ICON_SIZE + 25,
                ICON_SIZE * 3 + 500);
    }
}
