/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.SimpleMapPanel;
import pt.lsts.neptus.endurance.AssetsManager;
import pt.lsts.neptus.endurance.Plan;
import pt.lsts.neptus.gui.editor.StringListEditor;
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
    
    /**
     * @param console
     */
    public SoiPlanning(ConsoleLayout console) {
        super(console);
        initialize();
    }

    private void initialize() {
        featureFocuser = new FeatureFocuser(getConsole(), true, false);
        renderer.addMenuExtension(featureFocuser);

        renderer.addPostRenderPainter(getPlansPainter(), getName() + " - Plans");
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
    }
    
    /**
     * @return
     */
    private Renderer2DPainter getPlansPainter() {
        if (planPainter == null) {
            planPainter = new Renderer2DPainter() {
                @Override
                public void paint(Graphics2D g, StateRenderer2D renderer) {
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
}
