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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * 13/12/2011
 */
package pt.lsts.neptus.plugins.sim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.SimulatedState;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author zp
 * 
 */
@PluginDescription(author = "zp", name = "Simulated State Layer", icon = "pt/lsts/neptus/plugins/position/painter/simulator.png")
@LayerPriority(priority = 60)
public class SimulatedStatePainter extends ConsolePanel implements Renderer2DPainter {

    /**
     * @param console
     */
    public SimulatedStatePainter(ConsoleLayout console) {
        super(console);
    }

    protected SystemPositionAndAttitude simulatedState = null;
    protected long lastStateMillis = 0;
    protected GeneralPath path = new GeneralPath();
    {
        path.moveTo(0, -10);
        path.lineTo(0, -2);
        path.lineTo(-7, -4);
        path.lineTo(0, 10);
        path.lineTo(7, -4);
        path.lineTo(0, -2);
        path.closePath();
    }

    @Override
    public void initSubPanel() {
        setVisibility(false);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

        if ((System.currentTimeMillis() - lastStateMillis) > 2000)
            return;

        Point2D pt = renderer.getScreenPosition(simulatedState.getPosition());
        g.translate(pt.getX(), pt.getY());
        g.rotate(-renderer.getRotation() + simulatedState.getYaw() + Math.PI);

        g.setStroke(new BasicStroke(2f));
        g.setColor(Color.cyan.darker().darker());
        g.draw(path);
        g.setColor(Color.cyan.brighter().brighter());
        g.fill(path);
        g.drawString("S", -4, -4);

    }

    @Subscribe
    public void consume(SimulatedState simState) {
        LocationType loc = new LocationType(Math.toDegrees(simState.getLat()), Math.toDegrees(simState.getLon()));
        loc.setHeight(simState.getHeight());
        loc.translatePosition(simState.getX(), simState.getY(), simState.getZ());
        simulatedState = new SystemPositionAndAttitude(loc, simState.getPhi(), simState.getTheta(),
                simState.getPsi());
        lastStateMillis = System.currentTimeMillis();
    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
