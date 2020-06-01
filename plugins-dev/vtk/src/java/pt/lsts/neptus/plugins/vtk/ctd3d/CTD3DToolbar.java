/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Mar 10, 2014
 */
package pt.lsts.neptus.plugins.vtk.ctd3d;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.vtk.CTD3D;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.vtk.pointcloud.DepthExaggeration;
import pt.lsts.neptus.vtk.pointcloud.PointCloudCTD;
import pt.lsts.neptus.vtk.pointcloud.PointCloudHandlerCTD;
import pt.lsts.neptus.vtk.visualization.Canvas;
import pt.lsts.neptus.vtk.visualization.ScalarBar;

/**
 * @author hfq
 *
 */
public class CTD3DToolbar extends JToolBar {
    private static final long serialVersionUID = 1L;

    private static final short ICON_SIZE = 18;

    private static final ImageIcon ICON_TEMP = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/vtk/assets/temperature.png", ICON_SIZE, ICON_SIZE);
    private static final ImageIcon ICON_SALINITY = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/vtk/assets/salinity.png", ICON_SIZE, ICON_SIZE);
    private static final ImageIcon ICON_PRESSURE = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/vtk/assets/pressure.png", ICON_SIZE, ICON_SIZE);
    private static final ImageIcon ICON_Z = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/vtk/assets/zexaggerate.png", ICON_SIZE, ICON_SIZE);
    private static final ImageIcon ICON_RESETVIEWPORT = ImageUtils.getScaledIcon(
            "images/menus/camera.png", ICON_SIZE, ICON_SIZE);

    private JToggleButton tempToggle;
    private JToggleButton salinityToggle;
    private JToggleButton pressureToggle;

    private JToggleButton zexaggerToggle;

    private JButton resetViewportButton;

    private final PointCloudCTD pointcloud;
    private final ScalarBar scalarBar;

    private final Canvas canvas;

    /**
     * 
     * @param ctd3dInit
     */
    public CTD3DToolbar(CTD3D ctd3dInit) {
        this.canvas = ctd3dInit.canvas;
        this.pointcloud = ctd3dInit.pointcloud;
        this.scalarBar = ctd3dInit.scalarBar;
    }

    public void createtoolBar() {
        setOrientation(JToolBar.VERTICAL);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder()));

        setTempToggle(new JToggleButton());
        getTempToggle().setToolTipText(I18n.text("See Temperature data color map") + ".");
        getTempToggle().setIcon(ICON_TEMP);
        getTempToggle().addActionListener(temperatureToggleAction);

        setSalinityToggle(new JToggleButton());
        getSalinityToggle().setToolTipText(I18n.text("See Salinity data color map") + ".");
        getSalinityToggle().setIcon(ICON_SALINITY);
        getSalinityToggle().addActionListener(salinityToggleAction);

        setPressureToggle(new JToggleButton());
        getPressureToggle().setToolTipText(I18n.text("See Pressure data color map") + ".");
        getPressureToggle().setIcon(ICON_PRESSURE);
        getPressureToggle().addActionListener(pressureToggleAction);

        ButtonGroup groupToggles = new ButtonGroup();
        groupToggles.add(getTempToggle());
        groupToggles.add(getSalinityToggle());
        groupToggles.add(getPressureToggle());

        setZexaggerToggle(new JToggleButton());
        getZexaggerToggle().setToolTipText(I18n.text("Enable/Disable Z Exaggeration") + ".");
        getZexaggerToggle().setIcon(ICON_Z);
        getZexaggerToggle().addActionListener(zexaggerToggleAction);

        resetViewportButton = new JButton();
        resetViewportButton.setToolTipText(I18n.text("Reset Viewport") + ".");
        resetViewportButton.setIcon(ICON_RESETVIEWPORT);
        resetViewportButton.addActionListener(resetViewportAction);

        add(getTempToggle());
        add(getSalinityToggle());
        add(getPressureToggle());

        addSeparator();

        add(getZexaggerToggle());

        addSeparator();

        add(resetViewportButton);
    }

    ActionListener temperatureToggleAction = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            if(tempToggle.isSelected()) {
                pointcloud.getPolyData().GetPointData().SetScalars(((PointCloudHandlerCTD) pointcloud.getColorHandler()).getColorsTemperature());

                scalarBar.setScalarBarTitle(I18n.text("Temperature Color Map"));
                scalarBar.setScalarBarHorizontalProperties();
                scalarBar.setUpScalarBarLookupTable(((PointCloudHandlerCTD) pointcloud.getColorHandler()).getLutTemperature());

                canvas.lock();
                canvas.Render();
                canvas.unlock();
            }
        }
    };

    ActionListener salinityToggleAction = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            if(salinityToggle.isSelected()) {
                pointcloud.getPolyData().GetPointData().SetScalars(((PointCloudHandlerCTD) pointcloud.getColorHandler()).getColorsSalinity());

                scalarBar.setScalarBarTitle(I18n.text("Salinity Color Map"));
                scalarBar.setScalarBarHorizontalProperties();
                scalarBar.setUpScalarBarLookupTable(((PointCloudHandlerCTD) pointcloud.getColorHandler()).getLutSalinity());

                canvas.lock();
                canvas.Render();
                canvas.unlock();
            }
        }
    };

    ActionListener pressureToggleAction = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            if(pressureToggle.isSelected()) {
                pointcloud.getPolyData().GetPointData().SetScalars(((PointCloudHandlerCTD) pointcloud.getColorHandler()).getColorsPressure());

                scalarBar.setScalarBarTitle(I18n.text("Pressure Color Map"));
                scalarBar.setScalarBarHorizontalProperties();
                scalarBar.setUpScalarBarLookupTable(((PointCloudHandlerCTD) pointcloud.getColorHandler()).getLutPressure());

                canvas.lock();
                canvas.Render();
                canvas.unlock();
            }
        }
    };

    ActionListener zexaggerToggleAction = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (zexaggerToggle.isSelected()) {
                canvas.lock();
                DepthExaggeration.performDepthExaggeration(pointcloud.getPolyData(), 10);
                canvas.GetRenderer().ResetCamera();
                canvas.Render();
                canvas.unlock();
            }
            else if (!zexaggerToggle.isSelected()) {
                canvas.lock();
                DepthExaggeration.reverseDepthExaggeration(pointcloud.getPolyData(), 10);
                canvas.GetRenderer().ResetCamera();
                canvas.Render();
                canvas.unlock();
            }
        }
    };

    ActionListener resetViewportAction = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                canvas.lock();
                canvas.GetRenderer().ResetCamera();
                canvas.GetRenderer().GetActiveCamera().SetViewUp(0.0, 0.0, -1.0);
                canvas.Render();
                canvas.unlock();
            }
            catch (Exception e1) {
                e1.printStackTrace();
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

    /**
     * @return the tempToggle
     */
    public JToggleButton getTempToggle() {
        return tempToggle;
    }

    /**
     * @param tempToggle the tempToggle to set
     */
    private void setTempToggle(JToggleButton tempToggle) {
        this.tempToggle = tempToggle;
    }

    /**
     * @return the salinityToggle
     */
    public JToggleButton getSalinityToggle() {
        return salinityToggle;
    }

    /**
     * @param salinityToggle the salinityToggle to set
     */
    private void setSalinityToggle(JToggleButton salinityToggle) {
        this.salinityToggle = salinityToggle;
    }

    /**
     * @return the pressureToggle
     */
    public JToggleButton getPressureToggle() {
        return pressureToggle;
    }

    /**
     * @param pressureToggle the pressureToggle to set
     */
    private void setPressureToggle(JToggleButton pressureToggle) {
        this.pressureToggle = pressureToggle;
    }

    /**
     * @return the zexaggerToggle
     */
    public JToggleButton getZexaggerToggle() {
        return zexaggerToggle;
    }

    /**
     * @param zexaggerToggle the zexaggerToggle to set
     */
    public void setZexaggerToggle(JToggleButton zexaggerToggle) {
        this.zexaggerToggle = zexaggerToggle;
    }

    public static void main(String[] args) {
        CTD3DToolbar toolbar = new CTD3DToolbar(null);
        toolbar.createtoolBar();
        GuiUtils.testFrame(toolbar, "Test toolbar: " + toolbar.getClass().getSimpleName(), ICON_SIZE + 25,
                ICON_SIZE * 3 + 500);
    }
}
