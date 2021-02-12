/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * 24/01/2018
 */
package pt.lsts.neptus.soi;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.plaf.basic.BasicButtonUI;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.SimpleMapPanel;
import pt.lsts.neptus.endurance.AssetsManager;
import pt.lsts.neptus.endurance.Plan;
import pt.lsts.neptus.gui.editor.StringListEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.FeatureFocuser;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@Popup(pos = POSITION.RIGHT, width = 600, height = 600)
@PluginDescription(author = "Paulo Dias", version = "0.1.0", name = "SOI Planning", category = CATEGORY.PLANNING)
public class SoiPlanning extends SimpleMapPanel implements ILayerPainter, ConfigurationListener {

    @NeptusProperty(name = "Ignored Renderers", editorClass = StringListEditor.class, description = "A list of comma separated renderers to be ignored (requeres restart).")
    private String ignoredPostRenderersList = "";
    
    private ArrayList<String> ignoredPostRenderersListArray = new ArrayList<>();
    private AssetsManager assetsManager = AssetsManager.getInstance();
    
    private Renderer2DPainter planPainter = null;
    protected FeatureFocuser featureFocuser = null;
    
    protected SoiPlanEditor planEditor = null;
    
    // ------- UI Components -------
    protected JPanel sidePanel = null;
    protected JPanel bottomPanel = null;
    protected JToggleButton paintPlansButton = null;
    protected JToggleButton mapSyncButton = null;
    private JButton zoomInButton;
    private JButton zoomOutButton;
    protected JToggleButton plansEditModeButton = null;
    
    // Flags
    private boolean isPaintPlans = true;
    private boolean isMapMoveSync = isSyncronizeAllMapsMovements;

    /**
     * @param console
     */
    public SoiPlanning(ConsoleLayout console) {
        super(console);
        initialize();
    }

    private void initialize() {
        removeAll();
        setLayout(new MigLayout());
        
        JPanel rendererHolderPanel = new JPanel();
        rendererHolderPanel.setLayout(new BorderLayout());
        rendererHolderPanel.setBorder(BorderFactory.createEmptyBorder());
        rendererHolderPanel.add(renderer, BorderLayout.CENTER);
        
        featureFocuser = new FeatureFocuser(getConsole(), true, false);
        renderer.addMenuExtension(featureFocuser);
        
        planEditor = new SoiPlanEditor(getConsole());
        renderer.addInteraction(planEditor);

        renderer.addPostRenderPainter(getPlansPainter(), getName() + " - Plans");

        add(getSidePanel(), "w 100px:200px:");
        add(rendererHolderPanel, "w :100%:, h :100%:, wrap");
        add(getBottomPanel(), "w :100%:, span 2");
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        super.initSubPanel();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        super.cleanSubPanel();
        
        renderer.removeInteraction(planEditor);
    }
    
    /**
     * @return
     */
    private Component getSidePanel() {
        if (sidePanel == null) {
            sidePanel = new JPanel(new MigLayout("", "[]", ""));
        }

        return sidePanel;
    }

    /**
     * @return
     */
    private Component getBottomPanel() {
        if (bottomPanel == null) {
            bottomPanel = new JPanel();
            bottomPanel.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
            bottomPanel.setLayout(new MigLayout("", "", "[]"));

            plansEditModeButton = new JToggleButton(I18n.text("Edit plans"));
            plansEditModeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JToggleButton btn = (JToggleButton) e.getSource();
                    planEditor.setActive(btn.isSelected(), renderer);
                }
            });
            plansEditModeButton.setUI(new BasicButtonUI());
            bottomPanel.add(plansEditModeButton, "push, al right, sg g1, gapright 10");

            paintPlansButton = new JToggleButton(I18n.text("Show cur. plans"));
            paintPlansButton.setSelected(isMapMoveSync);
            paintPlansButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JToggleButton btn = (JToggleButton) e.getSource();
                    isPaintPlans = btn.isSelected();
                }
            });
            paintPlansButton.setUI(new BasicButtonUI());
            bottomPanel.add(paintPlansButton, "sg g1, gapright 10");

            mapSyncButton = new JToggleButton(I18n.text("Map move sync"));
            mapSyncButton.setSelected(isMapMoveSync);
            mapSyncButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JToggleButton btn = (JToggleButton) e.getSource();
                    isMapMoveSync = btn.isSelected();
                    renderer.setRespondToRendererChangeEvents(isMapMoveSync);
                }
            });
            mapSyncButton.setUI(new BasicButtonUI());
            bottomPanel.add(mapSyncButton, "sg g1");
            
            zoomInButton = new JButton(new AbstractAction("+") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    renderer.zoomIn();
                }
            });
            zoomInButton.setUI(new BasicButtonUI());
            bottomPanel.add(zoomInButton, "sg g2");

            zoomOutButton = new JButton(new AbstractAction("-") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    renderer.zoomOut();
                }
            });
            zoomOutButton.setUI(new BasicButtonUI());
            bottomPanel.add(zoomOutButton, "sg g2");
        }

        return bottomPanel;
    }

    /**
     * @return
     */
    private Renderer2DPainter getPlansPainter() {
        if (planPainter == null) {
            planPainter = new Renderer2DPainter() {
                @Override
                public void paint(Graphics2D g, StateRenderer2D renderer) {
                    if (!isPaintPlans)
                        return;
                    
                    Graphics2D g2 = (Graphics2D) g.create();
                    paintPlans(g2, renderer);
                    g2.dispose();
                }
            };
        }
        
        return planPainter;
    }

    @Override
    public void propertiesChanged() {
        super.propertiesChanged();

        ignoredPostRenderersListArray.clear();
        String tmpList = ignoredPostRenderersList.trim();
        String[] lst = tmpList.split(",");
        Arrays.asList(lst).stream().forEach(s -> ignoredPostRenderersListArray.add(s.trim()));
        
        isMapMoveSync = isSyncronizeAllMapsMovements;
        mapSyncButton.setSelected(isMapMoveSync);
    }

    private void paintPlans(Graphics2D g, StateRenderer2D renderer) {
        Map<String, Plan> plans = Collections.unmodifiableMap(assetsManager.getPlans());
        for (Entry<String, Plan> p : plans.entrySet()) {
            SoiPlanRenderer prenderer = new SoiPlanRenderer();
            try {
                prenderer.setColor(VehiclesHolder.getVehicleById(p.getKey()).getIconColor());
                prenderer.setPlan(p.getValue());
                prenderer.paint(g, renderer);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#paint(java.awt.Graphics)
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.ILayerPainter#addPostRenderPainter(pt.lsts.neptus.renderer2d.Renderer2DPainter, java.lang.String)
     */
    @Override
    public boolean addPostRenderPainter(Renderer2DPainter painter, String name) {
        if (ignoredPostRenderersListArray.contains(name))
            return false;
        
        renderer.addPostRenderPainter(painter, name);
        return true;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.ILayerPainter#removePostRenderPainter(pt.lsts.neptus.renderer2d.Renderer2DPainter)
     */
    @Override
    public boolean removePostRenderPainter(Renderer2DPainter painter) {
        renderer.removePostRenderPainter(painter);
        return true;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.ILayerPainter#addPreRenderPainter(pt.lsts.neptus.renderer2d.Renderer2DPainter)
     */
    @Override
    public void addPreRenderPainter(Renderer2DPainter painter) {
        renderer.addPreRenderPainter(painter);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.ILayerPainter#removePreRenderPainter(pt.lsts.neptus.renderer2d.Renderer2DPainter)
     */
    @Override
    public void removePreRenderPainter(Renderer2DPainter painter) {
        renderer.removePreRenderPainter(painter);
    }
    
    public static void main(String[] args) {
        GuiUtils.testFrame(new SoiPlanning(ConsoleLayout.forge()), "", 400, 400);
    }
}
