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
 * Jun 27, 2014
 */
package pt.lsts.neptus.plugins.europa.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.TitledBorder;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import psengine.PSObject;
import psengine.PSToken;
import psengine.PSTokenState;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FuelLevel;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.europa.NeptusSolver;
import pt.lsts.neptus.plugins.europa.gui.TimelineView.PlanToken;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 * 
 */
public class SolverPanel extends JPanel {

    private static final long serialVersionUID = -7352001540896377542L;
    private DefaultListModel<PlanTask> listModel = new DefaultListModel<>();
    private JList<PlanTask> tasks = new JList<>(listModel);
    private JButton btnSolve, btnReset;
    private JComboBox<VehicleType> vehicles = new JComboBox<>();
    private JComboBox<PlanType> plans = new JComboBox<>();
    private JFormattedTextField speed = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(2));
    private JFormattedTextField idleTime = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(0));
    private ConsoleLayout console;
    private PlanView plan;
    private JPanel planViewHolder = new JPanel(new BorderLayout());
    private NeptusSolver solver;
    private Thread backgroundTask;
    public SolverPanel(ConsoleLayout console) {
        this.console = console;
        initialize();
        reset();
    }

    private Collection<VehicleType> getVehicles() {
        HashSet<VehicleType> ret = new HashSet<>();
        for (ImcSystem sys : ImcSystemsHolder.lookupActiveSystemVehicles()) {
            ret.add(sys.getVehicle());
        }
        ret.add(VehiclesHolder.getVehicleById(console.getMainSystem()));
        return ret;
    }

    private Collection<PlanType> getPlans() {
        return console.getMission().getIndividualPlansList().values();
    }

    public void reset() {
        this.vehicles.removeAllItems();
        this.plans.removeAllItems();
        planViewHolder.removeAll();
        listModel.removeAllElements();
        for (VehicleType vt : getVehicles())
            this.vehicles.addItem(vt);

        for (PlanType pt : getPlans())
            this.plans.addItem(pt);

        if (solver != null) {
            try {
                solver.shutdown();
            }
            catch (Exception e) {
                e.printStackTrace();
            }            
        }

        planViewHolder.removeAll();
        plan = null;

        planViewHolder.doLayout();
        btnSolve.setEnabled(true);
    }

    private void solve() {

        if (solver != null) {
            synchronized (solver.getEuropa()) {
                solver.getEuropa().shutdown();
            }
        }

        if (plan != null) {
            planViewHolder.removeAll();
            planViewHolder.doLayout();
            planViewHolder.revalidate();
        }

        backgroundTask = new Thread("Europa solver") {
            @Override
            public void run() {
                Vector<PSToken> goals = new Vector<>();
                Vector<PSObject> planVehicles = new Vector<>();
                try {
                    solver = new NeptusSolver();
                    for (int i = 0; i < vehicles.getItemCount(); i++) {
                        String vehicle = vehicles.getItemAt(i).getId();
                        LocationType loc = console.getMission().getHomeRef();
                        double speed1 = 0.7;
                        double speed2 = 1.1;
                        double speed3 = 1.3;

                        long speed2Batt = 5 * 3600 * 1000;
                        long speed1Batt = (long) (speed2Batt * (speed2 / speed1));
                        long speed3Batt = (long) (speed2Batt * (speed2 / speed3));

                        if (ImcMsgManager.getManager().getState(vehicle).isActive()) {
                            EstimatedState pos = ImcMsgManager.getManager().getState(vehicle).last(EstimatedState.class);
                            loc = IMCUtils.getLocation(pos);
                            FuelLevel fl = ImcMsgManager.getManager().getState(vehicle).last(FuelLevel.class);
                            if (fl != null) {
                                LinkedHashMap<String, String> opModes = fl.getOpmodes();
                                // speed conversion is not linear but this is just for demonstration sake...
                                if (opModes != null && opModes.containsKey("Motion")) {
                                    speed2Batt = (long) (Double.parseDouble(opModes.get("Motion")) * 3600 * 1000);
                                    speed1Batt = (long) (speed2Batt * (speed2 / speed1));
                                    speed3Batt = (long) (speed2Batt * (speed2 / speed3));
                                }
                            }

                        }

                        planVehicles.add(solver.addVehicle(vehicles.getItemAt(i).getId(), loc, speed1, speed2, speed3, speed1Batt,
                                speed2Batt, speed3Batt));
                    }

                    for (int i = 0; i < plans.getItemCount(); i++)
                        solver.addTask(plans.getItemAt(i));

                    for (int i = 0; i < listModel.getSize(); i++) {
                        PlanTask pt = listModel.get(i);
                        goals.add(solver.addGoal(pt.vehicle.getId(), pt.plan.getId(), pt.speed));
                    }

                    try {
                        solver.solve(1000);
                        System.out.println(solver.getEuropa().planDatabaseToString());
                    }
                    catch (Exception e) {
                        GuiUtils.errorMessage(SolverPanel.this, I18n.text("Mission Planner"),
                                I18n.textf("The solver could not find a solution: %explanation", e.getMessage()));
                        
                        btnSolve.setEnabled(true);
                        return;
                    }

                    Vector<PSToken> rejectedTokens = new Vector<>();
                    String rejectionList = "";
                    for (PSToken goal : goals) {
                        if (goal.getTokenState() == PSTokenState.REJECTED) {
                            rejectedTokens.add(goal);
                            String vehicle = solver.resolveVehicleName(goal.getParameter("object").getSingletonValue().asObject().getEntityName());
                            String task = solver.resolvePlanName(goal.getParameter("task").getSingletonValue().asObject().getEntityName());
                            float speed = (float) goal.getParameter("speed").getLowerBound(); 
                            
                            rejectionList += "\n\t" + " - " + I18n.textf("Execute %plan by %vehicle at %speed m/s", task, vehicle, speed); 
                        }                
                    }

                    if (!rejectedTokens.isEmpty()) {
                            GuiUtils.errorMessage(SolverPanel.this, I18n.text("Mission Planner"),
                                    "<html>" + I18n.text("The following objectives were rejected by the solver: ")
                                            + rejectionList);
                    }

                    plan = new PlanView(solver);
                    plan.setMinimumSize(new Dimension(10, 200));
                    plan.endTimeChanged(null, 0);
                    planViewHolder.add(plan);
                    planViewHolder.doLayout();
                    planViewHolder.revalidate();
                    plan.addListener(new PlanViewListener() {

                        @Override
                        public void tokenSelected(PlanToken token) {
                            try {
                                console.setPlan(console.getMission().getIndividualPlansList().get(token.id));
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                        }
                    });                   
                }
                catch (Exception e) {
                    GuiUtils.errorMessage(SolverPanel.this, e);
                }
                btnSolve.setEnabled(true);
            }
        };
        backgroundTask.setDaemon(true);
        backgroundTask.start();
    }

    private void initialize() {
        setLayout(new MigLayout(new LC().minWidth("600px"), new AC().fill()));

        speed.setColumns(4);
        idleTime.setColumns(4);

        String textLocalized[] = I18n.textf(
                "Execute %plan with %vehicle travelling at %speed m/s idling up to %idletime seconds. ", "<SPLIT>",
                "<SPLIT>", "<SPLIT>", "<SPLIT>").split("<SPLIT>");

        int i = 0;
        add(new JLabel(textLocalized[i++]));
        add(plans);
        add(new JLabel(textLocalized[i++]));
        add(vehicles);
        add(new JLabel(textLocalized[i++]));
        add(speed);
        speed.setValue(1.1);
        add(new JLabel(textLocalized[i++]));
        add(idleTime);
        idleTime.setValue(0);
        add(new JLabel(textLocalized[i++]));
        JButton addBtn = new JButton("Add");
        add(addBtn, "span");
        addBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PlanTask pt = new PlanTask((PlanType) plans.getSelectedItem(),
                        (VehicleType) vehicles.getSelectedItem(), Double.parseDouble(speed.getText()));
                listModel.addElement(pt);
                planViewHolder.removeAll();
                plan = null;
                planViewHolder.doLayout();
            }
        });
        tasks.setBorder(new TitledBorder("Tasks"));
        add(tasks, new CC().grow(0.5f).span());
        tasks.setMinimumSize(new Dimension(10, 200));

        tasks.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                PlanTask pt = tasks.getSelectedValue();
                if (pt != null && e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popup = new JPopupMenu();
                    popup.add("Remove task").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            listModel.removeElement(tasks.getSelectedValue());
                            planViewHolder.removeAll();
                            plan = null;
                            planViewHolder.doLayout();
                        }
                    });
                    popup.show((Component) e.getSource(), e.getX(), e.getY());
                }
            }
        });

        btnSolve = new JButton(I18n.text("Solve"));
        btnSolve.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                btnSolve.setEnabled(false);
                solve();
            }
        });

        add(btnSolve);

        btnReset = new JButton(I18n.text("Reset"));
        btnReset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reset();
            }
        });

        add(btnReset, "wrap");

        planViewHolder.setBorder(BorderFactory.createTitledBorder(I18n.text("Result")));
        planViewHolder.setMinimumSize(new Dimension(10, 200));
        add(planViewHolder, new CC().grow(0.4f).span());

    }
}
