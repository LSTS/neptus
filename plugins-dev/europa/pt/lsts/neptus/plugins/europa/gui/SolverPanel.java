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
 * Jun 27, 2014
 */
package pt.lsts.neptus.plugins.europa.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;
import psengine.PSObject;
import psengine.PSToken;
import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.europa.NeptusSolver;
import pt.lsts.neptus.plugins.europa.PlanTask;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
public class SolverPanel extends JPanel {
    
    private static final long serialVersionUID = -7352001540896377542L;
    DefaultListModel<PlanTask> listModel = new DefaultListModel<>();
    JList<PlanTask> tasks = new JList<>(listModel);
    JComboBox<VehicleType> vehicles = new JComboBox<>();
    JComboBox<PlanType> plans = new JComboBox<>();
    JFormattedTextField speed = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(2));
    ConsoleLayout console;
    
    public SolverPanel(ConsoleLayout console, Collection<VehicleType> vehicles, Collection<PlanType> plans) {
        this.console = console;
        
        for (VehicleType vt : vehicles)
            this.vehicles.addItem(vt);
        
        for (PlanType pt : plans)
            this.plans.addItem(pt);
        
        initialize();
    }
    
    private void solve() {
        
        Vector<PSToken> goals = new Vector<>();
        
        try {
            NeptusSolver solver = new NeptusSolver();

            for (int i = 0;i < vehicles.getItemCount(); i++) {
                String vehicle = vehicles.getItemAt(i).getId();
                LocationType loc = console.getMission().getHomeRef();
                
                if (ImcMsgManager.getManager().getState(vehicle).isActive()) {
                    EstimatedState pos = ImcMsgManager.getManager().getState(vehicle).lastEstimatedState();
                    loc = IMCUtils.getLocation(pos);
                }
                
                // FIXME
                PSObject obj = solver.addVehicle(vehicles.getItemAt(i).getId(), loc, 0.7, 1.0, 1.3, 8 * 3600 * 1000, 6 * 3600 * 1000, 4 * 3600 * 1000);   
                
                System.out.println("Added this vehicle: "+vehicles.getItemAt(i).getId()+": "+obj);
            }
            
            for (int i = 0;i < plans.getItemCount(); i++)
                solver.addTask(plans.getItemAt(i));
            
            for (int i = 0;i < listModel.getSize(); i++) {
                PlanTask pt = listModel.get(i);
                goals.add(solver.addGoal(pt.vehicle.getId(), pt.plan.getId(), pt.speed));
            }
            
            solver.solve(1000);
             
            //TimelineView timeline = new TimelineView(solver);
            PlanView plan = new PlanView(solver);
            //timeline.setPlan(solver.getPlan("lauv-xtreme-2"));
            JFrame frm = GuiUtils.testFrame(plan);
            frm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            //PlanVisualization pv = new PlanVisualization(solver.getPlan("lauv-xtreme-2"));
            //frm = GuiUtils.testFrame(pv);
            //frm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            
            
            
            for (PSToken goal : goals) {
                System.out.println(goal.toLongString());
            }
        }
        catch (Exception e) {
            GuiUtils.errorMessage(console, e);
        }
        
    }
    
    private void initialize() {
        setLayout(new MigLayout());
        speed.setColumns(4);
        add(new JLabel("Add "));
        add(plans);
        add(new JLabel(" to "));
        add(vehicles);
        add(new JLabel(" travelling at "));
        add(speed);
        add(new JLabel(" m/s "));
        JButton addBtn = new JButton("OK");
        add(addBtn, "span");
        addBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PlanTask pt = new PlanTask((PlanType)plans.getSelectedItem(), (VehicleType)vehicles.getSelectedItem(), Double.parseDouble(speed.getText()));
                listModel.addElement(pt);
            }
        });
        tasks.setBorder(new TitledBorder("Tasks"));
        add(tasks, new CC().grow().span());
        
        JButton solveBtn = new JButton("Solve");
        solveBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                solve();
            }
        });
        
        add(solveBtn, "span");
    }
}
