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
 * Author: zp
 * 26/02/2017
 */
package pt.lsts.neptus.mp.preview;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.Vector;

/**
 * @author zp
 *
 */
public class SimDepthProfile {

    private PlanSimulationOverlay simulation;
    private Vector<SimulationState> simStates;
    private BufferedImage img = null;
    
    public SimDepthProfile(PlanSimulationOverlay simulation) {
        this.simulation = simulation;
    }
    
    public synchronized BufferedImage getProfile() {
        if (img != null || !simulation.simulationFinished)
            return img;
        else {
            simStates = simulation.simStates;
            Color sky = Color.cyan.darker(), water = Color.blue.darker(), sand = Color.yellow.darker(),
                    vehicle = Color.green, crash = Color.red;
            
            
            SimulationState deepest = simStates.parallelStream().max(new Comparator<SimulationState>() {
                @Override
                public int compare(SimulationState o1, SimulationState o2) {
                    return new Double(o1.getSysState().getDepth()).compareTo(o2.getSysState().getDepth());
                }
            }).get();
            
            double maxDepth = deepest.getSysState().getDepth() + 5;
            double resolution = 10;
            double margin = 3;
            
            BufferedImage tmp = new BufferedImage(simStates.size(), (int) (resolution * (margin * 2 + maxDepth)),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) tmp.getGraphics();
            
            for (int i = 0; i < simStates.size(); i++) {
                SimulationState s = simStates.get(i);
                double depth = s.getSysState().getDepth();
                double altitude = s.getSysState().getAltitude();
                if (altitude <= 0) {
                    altitude = SimulationEngine.simBathym.getSimulatedDepth(s.getSysState().getPosition());
                }
                
                double waterPos = margin * resolution;
                double vehiclePos = (margin + depth) * resolution;
                vehiclePos = Math.max(waterPos, vehiclePos);
                
                double sandPos = (margin + SimulationEngine.simBathym.getSimulatedDepth(s.getSysState().getPosition())) * resolution;
                g.setColor(sky);
                g.draw(new Line2D.Double(i, 0, i, waterPos));
                g.setColor(water);
                if (sandPos < tmp.getHeight()) {
                    g.draw(new Line2D.Double(i, waterPos, i, sandPos));
                    g.setColor(sand);
                    g.draw(new Line2D.Double(i, sandPos, i, tmp.getHeight()-1));
                }
                else {
                    g.draw(new Line2D.Double(i, waterPos, i, tmp.getHeight()-1));
                }
                g.setColor(vehicle);
                if (vehiclePos >= sandPos-resolution)
                    g.setColor(crash);
                double lineWidth = maxDepth * 0.2;
                g.draw(new Line2D.Double(i, vehiclePos-lineWidth, i, vehiclePos+lineWidth));                
            }
            img = tmp;
        }
        
        return img;
    }
    
    
}
