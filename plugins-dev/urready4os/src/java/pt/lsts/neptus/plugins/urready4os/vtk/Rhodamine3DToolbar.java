/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 12/06/2015
 */
package pt.lsts.neptus.plugins.urready4os.vtk;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.vtk.pointcloud.DepthExaggeration;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class Rhodamine3DToolbar extends JToolBar {

    private static final short ICON_SIZE = 18;

    private static final ImageIcon ICON_RHOD = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/urready4os/vtk/rhodamine.png", ICON_SIZE, ICON_SIZE);
    private static final ImageIcon ICON_PRED = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/plugins/urready4os/vtk/prediction.png", ICON_SIZE, ICON_SIZE);
    private static final ImageIcon ICON_Z = ImageUtils.getScaledIcon(
            "pt/lsts/neptus/vtk/assets/zexaggerate.png", ICON_SIZE, ICON_SIZE);
    private static final ImageIcon ICON_RESETVIEWPORT = ImageUtils.getScaledIcon(
            "images/menus/camera.png", ICON_SIZE, ICON_SIZE);

    private JToggleButton rhodToggle;
    private JToggleButton predToggle;
    private JToggleButton zExaggerToggle;

    private JButton resetViewportButton;

    private final Rhodamine3DPanel rhod3dInit;

    /**
     * 
     * @param rhod3dInit
     */
    public Rhodamine3DToolbar(Rhodamine3DPanel rhod3dInit) {
        this.rhod3dInit = rhod3dInit;
//        this.canvas = rhod3dInit.getCanvas();
//        this.pointcloud = rhod3dInit.pointcloud;
//        this.scalarBar = rhod3dInit.scalarBar;
    }

    public void createtoolBar() {
        setOrientation(JToolBar.VERTICAL);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder()));

        setRhodToggle(new JToggleButton());
        getRhodToggle().setToolTipText(I18n.text("See Rhodamine Dye data color map") + ".");
        getRhodToggle().setIcon(ICON_RHOD);
        getRhodToggle().addActionListener(rhodamineDyeToggleAction);
        
        setPredToggle(new JToggleButton());
        getPredToggle().setToolTipText(I18n.text("See Prediction data color map") + ".");
        getPredToggle().setIcon(ICON_PRED);
        getPredToggle().addActionListener(predictionToggleAction);


//        ButtonGroup groupToggles = new ButtonGroup();
//        groupToggles.add(getRhodToggle());
//        groupToggles.add(getPredToggle());

        setZexaggerToggle(new JToggleButton());
        getZexaggerToggle().setToolTipText(I18n.text("Enable/Disable Z Exaggeration") + ".");
        getZexaggerToggle().setIcon(ICON_Z);
        getZexaggerToggle().addActionListener(zexaggerToggleAction);

        resetViewportButton = new JButton();
        resetViewportButton.setToolTipText(I18n.text("Reset Viewport") + ".");
        resetViewportButton.setIcon(ICON_RESETVIEWPORT);
        resetViewportButton.addActionListener(resetViewportAction);

        add(getRhodToggle());
        add(getPredToggle());

        addSeparator();

        add(getZexaggerToggle());

        addSeparator();

        add(resetViewportButton);
    }

    ActionListener rhodamineDyeToggleAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (rhodToggle.isSelected() && rhod3dInit.getPointcloud() != null) {
//                rhod3dInit.getPointcloud().getPolyData().GetPointData().SetScalars(((PointCloudHandlerRhodamineDye) rhod3dInit.getPointcloud().getColorHandler()).getColorsRhodamineDye());
//
//                rhod3dInit.getScalarBar().setScalarBarTitle(I18n.text("Rhodamine Dye Color Map"));
//                rhod3dInit.getScalarBar().setScalarBarHorizontalProperties();
//                rhod3dInit.getScalarBar().setUpScalarBarLookupTable(((PointCloudHandlerRhodamineDye) rhod3dInit.getPointcloud().getColorHandler()).getLutRhodamineDye());
//
//                rhod3dInit.getCanvas().lock();
//                rhod3dInit.getCanvas().Render();
//                rhod3dInit.getCanvas().unlock();

                rhod3dInit.getCanvas().GetRenderer().RemoveActor(rhod3dInit.getPointcloud().getCloudLODActor());
                rhod3dInit.getCanvas().GetRenderer().RemoveActor(rhod3dInit.getScalarBar().getScalarBarActor());
                rhod3dInit.getCanvas().GetRenderer().AddActor(rhod3dInit.getPointcloud().getCloudLODActor());
                rhod3dInit.getCanvas().GetRenderer().AddActor(rhod3dInit.getScalarBar().getScalarBarActor());

                rhod3dInit.getCanvas().lock();
                rhod3dInit.getCanvas().Render();
                rhod3dInit.getCanvas().unlock();
            }
            else if (!rhodToggle.isSelected() && rhod3dInit.getPointcloud() != null) {
                if (rhod3dInit.getPointcloud() != null) {
                    rhod3dInit.getCanvas().GetRenderer().RemoveActor(rhod3dInit.getPointcloud().getCloudLODActor());
                    
                    if (!predToggle.isSelected())
                        rhod3dInit.getCanvas().GetRenderer().RemoveActor(rhod3dInit.getScalarBar().getScalarBarActor());

                    rhod3dInit.getCanvas().lock();
                    rhod3dInit.getCanvas().Render();
                    rhod3dInit.getCanvas().unlock();
                }
            }
        }
    };

    ActionListener predictionToggleAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (predToggle.isSelected() && rhod3dInit.getPointcloudPrediction() != null) {
                rhod3dInit.getCanvas().GetRenderer().RemoveActor(rhod3dInit.getPointcloudPrediction().getCloudLODActor());
                rhod3dInit.getCanvas().GetRenderer().RemoveActor(rhod3dInit.getScalarBar().getScalarBarActor());
                rhod3dInit.getCanvas().GetRenderer().AddActor(rhod3dInit.getPointcloudPrediction().getCloudLODActor());
                rhod3dInit.getCanvas().GetRenderer().AddActor(rhod3dInit.getScalarBar().getScalarBarActor());

                rhod3dInit.getCanvas().lock();
                rhod3dInit.getCanvas().Render();
                rhod3dInit.getCanvas().unlock();
            }
            else if (!predToggle.isSelected() && rhod3dInit.getPointcloudPrediction() != null) {
                rhod3dInit.getCanvas().GetRenderer().RemoveActor(rhod3dInit.getPointcloudPrediction().getCloudLODActor());
                
                if (!rhodToggle.isSelected())
                    rhod3dInit.getCanvas().GetRenderer().RemoveActor(rhod3dInit.getScalarBar().getScalarBarActor());

                rhod3dInit.getCanvas().lock();
                rhod3dInit.getCanvas().Render();
                rhod3dInit.getCanvas().unlock();
            }
        }
    };

    ActionListener zexaggerToggleAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (zExaggerToggle.isSelected() && rhod3dInit.getPointcloud() != null) {
                rhod3dInit.getCanvas().lock();
                DepthExaggeration.performDepthExaggeration(rhod3dInit.getPointcloud().getPolyData(), 10);
                rhod3dInit.getCanvas().GetRenderer().ResetCamera();
                rhod3dInit.getCanvas().Render();
                rhod3dInit.getCanvas().unlock();
            }
            else if (!zExaggerToggle.isSelected() && rhod3dInit.getPointcloud() != null) {
                rhod3dInit.getCanvas().lock();
                DepthExaggeration.reverseDepthExaggeration(rhod3dInit.getPointcloud().getPolyData(), 10);
                rhod3dInit.getCanvas().GetRenderer().ResetCamera();
                rhod3dInit.getCanvas().Render();
                rhod3dInit.getCanvas().unlock();
            }
            
            if (zExaggerToggle.isSelected() && rhod3dInit.getPointcloudPrediction() != null) {
                rhod3dInit.getCanvas().lock();
                DepthExaggeration.performDepthExaggeration(rhod3dInit.getPointcloudPrediction().getPolyData(), 10);
                rhod3dInit.getCanvas().GetRenderer().ResetCamera();
                rhod3dInit.getCanvas().Render();
                rhod3dInit.getCanvas().unlock();
            }
            else if (!zExaggerToggle.isSelected() && rhod3dInit.getPointcloudPrediction() != null) {
                rhod3dInit.getCanvas().lock();
                DepthExaggeration.reverseDepthExaggeration(rhod3dInit.getPointcloudPrediction().getPolyData(), 10);
                rhod3dInit.getCanvas().GetRenderer().ResetCamera();
                rhod3dInit.getCanvas().Render();
                rhod3dInit.getCanvas().unlock();
            }

        }
    };

    ActionListener resetViewportAction = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                rhod3dInit.getCanvas().lock();
                rhod3dInit.getCanvas().GetRenderer().ResetCamera();
                rhod3dInit.getCanvas().GetRenderer().GetActiveCamera().SetViewUp(0.0, 1.0, -1.0);
//                rhod3dInit.getCanvas().GetRenderer().GetActiveCamera().Azimuth(45);
                rhod3dInit.getCanvas().Render();
                rhod3dInit.getCanvas().unlock();
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
    public JToggleButton getRhodToggle() {
        return rhodToggle;
    }

    /**
     * @param tempToggle the tempToggle to set
     */
    private void setRhodToggle(JToggleButton tempToggle) {
        this.rhodToggle = tempToggle;
    }

    /**
     * @return the predToggle
     */
    public JToggleButton getPredToggle() {
        return predToggle;
    }
    
    /**
     * @param predToggle the predToggle to set
     */
    public void setPredToggle(JToggleButton predToggle) {
        this.predToggle = predToggle;
    }
    
    /**
     * @return the zexaggerToggle
     */
    public JToggleButton getZexaggerToggle() {
        return zExaggerToggle;
    }

    /**
     * @param zexaggerToggle the zexaggerToggle to set
     */
    public void setZexaggerToggle(JToggleButton zexaggerToggle) {
        this.zExaggerToggle = zexaggerToggle;
    }

    public static void main(String[] args) {
        Rhodamine3DToolbar toolbar = new Rhodamine3DToolbar(null);
        toolbar.createtoolBar();
        GuiUtils.testFrame(toolbar, "Test toolbar: " + toolbar.getClass().getSimpleName(), ICON_SIZE + 25,
                ICON_SIZE * 3 + 500);
    }}
