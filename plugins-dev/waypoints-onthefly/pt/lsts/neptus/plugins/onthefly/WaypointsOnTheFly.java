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

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.HashMap;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;

/**
 * @author tsmarques
 *
 */
@PluginDescription(author = "tsmarques", name = "Waypoints On-The-Fly", version = "0.1", description = "Edit waypoints on the map, and update the plan automatically")
public class WaypointsOnTheFly extends InteractionAdapter implements PlanChangeListener, Renderer2DPainter {
    private ConsoleLayout console;
    
    private PlanElement planElem;
    private PlanType currPlan;
    StateRenderer2D renderer;
    
    private Maneuver currSelectedManeuver;
    //private HashMap<String, Maneuver> selectedManeuvers;
    private Point2D dragPoint;
    private boolean waypointBeingDragged;
    
    private boolean ctrlKeyPressed;
    private boolean shiftKeyPressed;
    private boolean planBeingDragged;
    
    public WaypointsOnTheFly(ConsoleLayout console) {
        super(console);
        this.console = console;
        waypointBeingDragged = false;
        ctrlKeyPressed = false;
        shiftKeyPressed = false;
        planBeingDragged = false;
    }
    
    private void setPlan(PlanType plan) {
        currPlan = plan;
        if(currPlan != null) {
            planElem = new PlanElement(currPlan.getMapGroup(), new MapType());
            planElem.setRenderer(this.renderer);
            planElem.setPlan(currPlan);
            planElem.allowMultipleSelectedManeuvers(true);
            
            planElem.clearSelectedManeuvers();
        }
    }
       
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D renderer) {
        if(planElem != null) {
            planElem.setBeingEdited(true);
            
            if(SwingUtilities.isLeftMouseButton(event))
                selectManeuver(event.getPoint());
            
            if(SwingUtilities.isRightMouseButton(event) && currSelectedManeuver != null) {
                editWaypointZ(currSelectedManeuver, event.getPoint().getX(), event.getPoint().getY());
                savePlan();
            }
            else
                super.mousePressed(event, renderer);
        }
        else
            super.mousePressed(event, renderer);
    }
    
    /* 
       Handle maneuver selection. 
       If on 'multiple selection mode' store the selected maneuver,
       but if it was already selected, unselect it.
     */
    private void selectManeuver(Point2D position) {
        currSelectedManeuver = planElem.iterateManeuverUnder(position);
        
        if(currSelectedManeuver != null) {
            String maneuverId = currSelectedManeuver.getId();
            if(shiftKeyPressed) { /* multiple maneveuvers selection */
                if(planElem.isSelectedManeuver(maneuverId)) /* unselect */
                    planElem.removeSelectedManeuver(maneuverId);
                else
                    planElem.addSelectedManeuver(maneuverId);
            }
            else { /* single maneuver selection */
                planElem.clearSelectedManeuvers();
                planElem.addSelectedManeuver(maneuverId);
            }
        }
        else /* 'forget' all selected maneuvers */
            planElem.clearSelectedManeuvers();
    }
        
    @Override
    public void mouseDragged(MouseEvent e, StateRenderer2D renderer) {
        if(planElem != null) {
            if(currSelectedManeuver != null && SwingUtilities.isLeftMouseButton(e)) {
                if(dragPoint != null) {
                    if(ctrlKeyPressed) /* whole plan is being moved */
                        dragPlan(e.getPoint());
                    
                    else { /* dragging just one waypoint */
                        waypointBeingDragged = true;
                        updateWaypointPosition(e.getPoint().getX(), e.getPoint().getY());
                    }
                }
                else
                    dragPoint = e.getPoint();
                
                renderer.repaint();
            }
            else
                super.mouseDragged(e, renderer);
        }
        else
            super.mouseDragged(e, renderer);
    }
    
    /* Updates the position of a waypoint to the current the position of the mouse */
    private void updateWaypointPosition(double mouseX, double mouseY) {
        double diffX = mouseX - dragPoint.getX();
        double diffY = mouseY - dragPoint.getY();
        Point2D newManPos = planElem.translateManeuverPosition(currSelectedManeuver.getId(), diffX, diffY);

        ManeuverLocation loc = ((LocatedManeuver) currSelectedManeuver).getManeuverLocation();
        loc.setLocation(renderer.getRealWorldLocation(newManPos));
        ((LocatedManeuver) currSelectedManeuver).setManeuverLocation(loc);
        
        planElem.recalculateManeuverPositions(renderer);
        
        dragPoint = newManPos;
    }
    
    private void editWaypointZ(Maneuver waypoint, double screenX, double screenY) {
        String value= JOptionPane.showInputDialog(console, waypoint.getId() + " Z value:");
        if(value != null) {
            double waypointZ = Double.parseDouble(value);
            ManeuverLocation waypointLoc = ((LocatedManeuver) waypoint).getManeuverLocation();
            waypointLoc.setZ(waypointZ);
            ((LocatedManeuver) waypoint).setManeuverLocation(waypointLoc);
            
            planElem.recalculateManeuverPositions(renderer);
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e, StateRenderer2D renderer) {
        if(planElem != null) {
            if(waypointBeingDragged)
                updateWaypointPosition(e.getPoint().getX(), e.getPoint().getY());
            else if(planBeingDragged)
                dragPlan(e.getPoint());
            
            dragPoint = null;
            waypointBeingDragged = false;
            planBeingDragged = false;
            
            savePlan();
            planElem.setBeingEdited(false);
        }
        else
            super.mouseReleased(e, renderer);
    }
    
    private void dragPlan(Point2D newPos) {
        LocationType oldLoc = renderer.getRealWorldLocation(dragPoint);
        LocationType newLoc= renderer.getRealWorldLocation(newPos);

        double[] offsets = newLoc.getOffsetFrom(oldLoc);

        planElem.translatePlan(offsets[0], offsets[1], 0);
        dragPoint = newPos;
        planBeingDragged = true;
    }
    
    private void savePlan() {
        currPlan.setMissionType(getConsole().getMission());
        console.getMission().addPlan(currPlan);
        
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                getConsole().getMission().save(true);
                return null;
            }
        };
        worker.execute();
        console.updateMissionListeners();
    }
    
    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        renderer = source;
        setPlan(console.getPlan());
        super.setActive(mode, source);
    }


    @Override
    public void planChange(PlanType plan) {
        setPlan(plan);
    }
    
    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        if(planElem != null) {
            if(event.isControlDown())
                ctrlKeyPressed = true;
            else if(event.isShiftDown())
                shiftKeyPressed = true;
            else
                super.keyPressed(event, source);
        }
        else
            super.keyPressed(event, source);
    }
    
    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        if(planElem != null) {
            if(event.getKeyCode() == KeyEvent.VK_CONTROL)
                ctrlKeyPressed = false;
            else if(event.getKeyCode() == KeyEvent.VK_SHIFT)
                shiftKeyPressed = false;
            else
                super.keyReleased(event, source);
        }
        else
            super.keyReleased(event, source);        
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        this.renderer = renderer;
        
        if(planElem != null /*&& waypointBeingDragged*/) {
            planElem.setRenderer(renderer);
            planElem.paint((Graphics2D)(g.create()), renderer);
        }
        renderer.repaint();
    }
}
