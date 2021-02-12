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
 * Author: jqcorreia
 * Jun 21, 2013
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.Map;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.ConsoleSystem;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author jqcorreia
 *
 */
@PluginDescription(name = "Distances Layer", author = "José Quadrado", category = CATEGORY.PLANNING)
public class DistancesLayer extends ConsolePanel implements Renderer2DPainter  {
    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Visible", category = "Visibility", userLevel = LEVEL.REGULAR)
    public boolean visible = true;
    
    private ConsoleLayout console;
    private Map<String, ConsoleSystem> systems;
    
    /**
     * @param console
     */
    public DistancesLayer(ConsoleLayout console) {
        super(console);
        this.console = console;
    }

    @Override
    public void initSubPanel() {    
        systems = console.getSystems();
    }

    @Override
    public void cleanSubPanel() {
        systems.clear();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!visible)
            return;
        
        Point2D point;
        Point2D me = renderer.getScreenPosition(MyState.getLocation());
        
        for(ConsoleSystem sys : systems.values()) {
            double dist = sys.getState().getPosition().getDistanceInMeters(MyState.getLocation());
            
            point = renderer.getScreenPosition(sys.getState().getPosition());

            g.setColor(Color.RED.darker());
            
            g.drawRect((int)point.getX()-10, (int)point.getY()-10, 20, 20);
            g.drawLine((int)me.getX(), (int)me.getY(), (int)point.getX(), (int)point.getY());
            
            int x = (int)(((me.getX() + point.getX()) / 2) + 4);
            int y = (int)(((me.getY() + point.getY()) / 2) + 4);
            
            g.setColor(Color.RED.brighter());
            g.drawString(MathMiscUtils.round(dist, 2) + "m", x , y);
        }
    }
}
