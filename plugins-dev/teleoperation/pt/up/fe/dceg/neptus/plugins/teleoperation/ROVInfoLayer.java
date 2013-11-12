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
 * Author: jqcorreia
 * Nov 7, 2013
 */
package pt.up.fe.dceg.neptus.plugins.teleoperation;

import java.awt.Color;
import java.awt.Graphics2D;

import pt.lsts.imc.DesiredHeading;
import pt.lsts.imc.DesiredZ;
import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.SimpleSubPanel;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.MathMiscUtils;

import com.google.common.eventbus.Subscribe;

/**
 * @author jqcorreia
 *
 */
@PluginDescription(name = "ROV Information Layer", icon = "pt/up/fe/dceg/neptus/plugins/position/position.png", description = "ROV Information Layer", category = CATEGORY.INTERFACE)
@LayerPriority(priority = 70)
public class ROVInfoLayer extends SimpleSubPanel implements Renderer2DPainter
{
    /**
     * @param console
     */
    
    private double desiredDepth = 0;
    private double desiredHeading = 0;
    private double depth = 0;
    private double heading = 0;
    
    public ROVInfoLayer(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        System.out.println();
        g.setColor(Color.BLACK);
        g.translate(0, renderer.getHeight() - 100);
        g.drawString("Desired Heading: " + desiredHeading, 10, 0);
        g.drawString("Heading: " + heading, 10, 10);
        g.drawString("Desired Depth: " + desiredDepth, 10, 20);
        g.drawString("Depth: " + depth, 10, 30);
        
    }

    @Override
    public void initSubPanel() {
        
    }

    @Override
    public void cleanSubPanel() {
        
    }
    
    @Subscribe
    public void onMessage(EstimatedState state) {
        if(state.getSourceName().equals(getMainVehicleId())) {
            depth = MathMiscUtils.round(state.getDepth(), 3);
            heading =MathMiscUtils.round(state.getPsi(), 3);
        }
    }
    
    @Subscribe
    public void onMessage(DesiredZ dz) {
        if(dz.getSourceName().equals(getMainVehicleId())) {
            desiredDepth = MathMiscUtils.round(dz.getValue(), 3);
        }
    }
    
    @Subscribe
    public void onMessage(DesiredHeading dh) {
        if(dh.getSourceName().equals(getMainVehicleId())) {
            desiredHeading = MathMiscUtils.round(dh.getValue(), 3);
        }
    }
}