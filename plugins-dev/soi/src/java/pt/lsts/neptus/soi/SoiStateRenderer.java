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
 * May 11, 2018
 */
package pt.lsts.neptus.soi;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Map.Entry;

import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.endurance.AssetsManager;
import pt.lsts.neptus.endurance.Plan;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;

/**
 * @author zp
 *
 */
public class SoiStateRenderer implements Renderer2DPainter {

    private SoiPlanRenderer prenderer = new SoiPlanRenderer();
    private static SoiStateRenderer instance = null;
    private GeneralPath vehShape;

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        for (Entry<String, Plan> p : AssetsManager.getInstance().getPlans().entrySet()) {
            try {
                
                
                String vehicle = p.getKey();
                VehicleType v = VehiclesHolder.getVehicleById(vehicle);
                if (v == null)
                    continue;
                
                Color c = VehiclesHolder.getVehicleById(vehicle).getIconColor();
                SystemPositionAndAttitude estimatedState = SoiUtils
                        .estimatedState(ImcSystemsHolder.getSystemWithName(vehicle), p.getValue());
                
                prenderer.setColor(c);
                prenderer.setPlan(p.getValue());
                prenderer.paint(g, renderer);
                if (estimatedState != null && estimatedState.getPosition() != null) {
                    Point2D pt = renderer.getScreenPosition(estimatedState.getPosition());
                    Graphics2D copy = (Graphics2D) g.create();
                    
                    copy.translate(pt.getX(), pt.getY());
                    copy.setColor(new Color(0,0,0,128));
                    copy.setFont(new Font("Arial", Font.BOLD, 12));
                    copy.drawString(v.getNickname().toUpperCase(), 10, 0);                    
                    
                    copy.rotate(Math.toRadians(estimatedState.getYaw())-renderer.getRotation());
                    
                    vehShape = new GeneralPath();
                    vehShape.moveTo(-12, 5);
                    vehShape.lineTo(0, -12);
                    vehShape.lineTo(12, 5);
                    vehShape.lineTo(0, 0);
                    vehShape.closePath();
                    
                    copy.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 180));
                    copy.fill(vehShape);
                    
                    copy.setColor(new Color(0,0,0,128));
                    copy.draw(vehShape);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void paintStatic(Graphics2D g, StateRenderer2D renderer) {
        if (instance == null)
            instance = new SoiStateRenderer();
        instance.paint(g, renderer);
    }
}
