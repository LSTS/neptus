/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsmarques
 * 4 Sep 2015
 */
package pt.lsts.neptus.plugins.onthefly;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.mp.Maneuver;

/**
 * @author tsmarques
 *
 */
@PluginDescription(author = "tsmarques", name = "Waypoints On-The-Fly", version = "0.1", description = "Edit waypoints on the map, and update the plan automatically")
public class WaypointsOnTheFly extends InteractionAdapter implements PlanChangeListener {
    private ConsoleLayout console;
    
    private PlanElement planElem;
    private PlanType currPlan;
    StateRenderer2D renderer;
    
    private Maneuver selectedManeuver;
    private Point2D dragPoint;
    
    public WaypointsOnTheFly(ConsoleLayout console) {
        super(console);
        this.console = console;
    }
    
    private void setPlan() {
        currPlan = console.getPlan();
        if(currPlan != null) {
            planElem = new PlanElement(currPlan.getMapGroup(), new MapType());
            planElem.setRenderer(this.renderer);
            planElem.setPlan(currPlan);
            planElem.setBeingEdited(true);
        }
    }
    
    
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D renderer) {
        if(planElem != null) {
            selectedManeuver = planElem.iterateManeuverUnder(event.getPoint());
            if(selectedManeuver != null) {
                
            }
            else
                super.mousePressed(event, renderer);
        }
        else
            super.mousePressed(event, renderer);
    }
    
    @Override
    public void mouseDragged(MouseEvent e, StateRenderer2D renderer) {
        if(planElem != null)
            dragPoint = e.getPoint();
        else
            super.mouseDragged(e, renderer);
    }
    
    @Override
    public void mouseReleased(MouseEvent e, StateRenderer2D renderer) {
        if(planElem != null)
            planElem.recalculateManeuverPositions(renderer);
        else
            super.mouseReleased(e, renderer);
    }
    
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        renderer = source;
        setPlan();
        super.setActive(mode, source);
    }


    @Override
    public void planChange(PlanType plan) {
        setPlan();
    }
}
