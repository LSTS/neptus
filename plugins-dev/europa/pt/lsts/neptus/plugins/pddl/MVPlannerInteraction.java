/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: zp
 * Nov 25, 2014
 */
package pt.lsts.neptus.plugins.pddl;

import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Vector;

import javax.swing.JPopupMenu;

import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="Multi-Vehicle Planner Interaction")
public class MVPlannerInteraction extends ConsoleInteraction {
    
    private Vector<MVPlannerTask> tasks = new Vector<MVPlannerTask>();
    private MVPlannerTask selectedTask = null;    
    private Point2D lastPoint = null;
        
    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        
        g.setTransform(new AffineTransform());
        for (MVPlannerTask t : tasks) {
            t.paint((Graphics2D)g.create(), source);
        }
        
        super.paintInteraction(g, source);
    }
    
    @Override
    public void mouseClicked(final MouseEvent event, final StateRenderer2D source) {
        if (event.getButton() != MouseEvent.BUTTON3) {
            super.mouseClicked(event, source);
            return;
        }
        MVPlannerTask clicked = null;
        LocationType lt = source.getRealWorldLocation(event.getPoint());
        for (MVPlannerTask t : tasks) {
            if (t.containsPoint(lt, source)) {
                clicked = t;
                break;
            }
        }

//        if (clicked == null) {
//            super.mouseClicked(event, source);
//            return;
//        }
        
        JPopupMenu popup = new JPopupMenu();
        final MVPlannerTask clickedTask = clicked;
        
        if (clicked != null) {
            popup.add("Remove "+clicked.getName()).addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    tasks.remove(clickedTask);        
                    source.repaint();
                }
            });
            
            popup.add("Set payloads for "+clickedTask.getName()).addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    PropertiesEditor.editProperties(clickedTask, true);                 
                }
            });
            
            popup.addSeparator();
            
        }
        
        popup.add("Add survey task").addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                SurveyAreaTask task = new SurveyAreaTask(source.getRealWorldLocation(event.getPoint()));
                tasks.add(task);
                source.repaint();
            }
        });
        popup.add("Add sample task").addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                SamplePointTask task = new SamplePointTask(source.getRealWorldLocation(event.getPoint()));
                tasks.add(task);
                source.repaint();
            }
        });
        
        popup.add("Generate").addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                Vector<VehicleType> activeVehicles = new Vector<VehicleType>();
                for (ImcSystem s : ImcSystemsHolder.lookupActiveSystemVehicles()) {
                    activeVehicles.addElement(VehiclesHolder.getVehicleById(s.getName()));
                }
                
                MVProblemSpecification p = new MVProblemSpecification(activeVehicles, tasks);
                System.out.println(p.asPDDL());
                
            }
        });
        popup.show(source, event.getX(), event.getY());
    }
    
    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
        MVPlannerTask selected = null;
        LocationType lt = source.getRealWorldLocation(event.getPoint());
        for (MVPlannerTask t : tasks) {
            if (t.containsPoint(lt, source)) {
                selected = t;
                break;
            }
        }
        if (selected == null)
            super.mousePressed(event, source);
        else {
            selectedTask = selected;
            lastPoint = event.getPoint();
        }
    }
        
    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
        selectedTask = null;
        lastPoint = null;
        super.mouseReleased(event, source);
    }
    
    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
        if (selectedTask == null) {
            super.mouseDragged(event, source);
            return;
        }
       
        LocationType prev = source.getRealWorldLocation(lastPoint);
        LocationType now = source.getRealWorldLocation(event.getPoint());
        
        double xamount = event.getX() - lastPoint.getX();
        double yamount = event.getY() - lastPoint.getY();
        
        if (event.isControlDown()) {
            selectedTask.growLength(-yamount*5 / source.getZoom());
            selectedTask.growWidth(xamount*5 / source.getZoom());
        }
        else if (event.isShiftDown()) {
            selectedTask.rotate(Math.toRadians((yamount+xamount)*3));
        }
        else {
            double offsets[] = now.getOffsetFrom(prev);
            selectedTask.translate(offsets[0], offsets[1]);
        }
        
        // change selected task
        lastPoint = event.getPoint();
    }
    
    @Override
    public void initInteraction() { 
        
    }

    @Override
    public void cleanInteraction() {

    }
    
    public static void main(String[] args) {
        StateRenderer2D renderer = new StateRenderer2D();
        MVPlannerInteraction inter = new MVPlannerInteraction();
        inter.init(new ConsoleLayout());
        renderer.setActiveInteraction(inter);
        GuiUtils.testFrame(renderer);
    }
}
