/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * 13/05/2016
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang.ArrayUtils;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.mp.maneuvers.FollowTrajectory;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.ScheduledGoto;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.wizard.PlanSelectionPage;
import pt.lsts.neptus.wizard.PojoPropertiesPage;
import pt.lsts.neptus.wizard.VehicleSelectionPage;
import pt.lsts.neptus.wizard.WizardPage;

/**
 * @author zp
 */
@PluginDescription
@Popup(width=800, height=600, pos=POSITION.CENTER)
public class VerticalFormationWizard extends ConsolePanel implements MissionChangeListener {

    private static final long serialVersionUID = -8189579968717153114L;

    private JButton btnAdvance, btnBack, btnCancel;
    private JPanel main;
    
    ArrayList<WizardPage<?>> pages = new ArrayList<>(); 
    private JLabel lblTop = new JLabel();
    private PlanSelectionPage planSelection;
    private VehicleSelectionPage vehicleSelection;
    private PojoPropertiesPage<VerticalFormationOptions> options = 
            new PojoPropertiesPage<VerticalFormationOptions>(new VerticalFormationOptions());
    int page = 0;
    
    /**
     * @param console
     */
    public VerticalFormationWizard(ConsoleLayout console) {
        super(console);
        setLayout(new BorderLayout());
        btnAdvance = new JButton(I18n.text("Next"));
        btnBack = new JButton(I18n.text("Previous"));
        btnCancel = new JButton(I18n.text("Cancel"));
        
        btnAdvance.addActionListener(this::advance);
        btnBack.addActionListener(this::back);
        btnCancel.addActionListener(this::cancel);
        
        JPanel btns = new JPanel(new BorderLayout());
        JPanel flow1 = new JPanel(new FlowLayout());
        flow1.add(btnBack);
        JPanel flow2 = new JPanel(new FlowLayout());
        flow2.add(btnCancel);
        flow2.add(btnAdvance);
        btns.add(flow1, BorderLayout.WEST);
        btns.add(flow2, BorderLayout.EAST);
        add(btns, BorderLayout.SOUTH);
        
        lblTop.setPreferredSize(new Dimension(60, 60));
        lblTop.setMinimumSize(lblTop.getPreferredSize());
        lblTop.setOpaque(true);
        lblTop.setBackground(Color.white);
        lblTop.setFont(new Font("Helvetica", Font.BOLD, 18));
        add(lblTop, BorderLayout.NORTH);
        
        planSelection = new PlanSelectionPage(console.getMission(), false);
        vehicleSelection = new VehicleSelectionPage(new ArrayList<VehicleType>(), true);
        
        pages.add(planSelection);
        pages.add(vehicleSelection);
        pages.add(options);
        main = new JPanel(new CardLayout());
        pages.forEach( p -> main.add(p, p.getTitle()));
        add(main, BorderLayout.CENTER);
        lblTop.setText(pages.get(0).getTitle());
    }
    
    
    @Override
    public void missionUpdated(MissionType mission) {
        planSelection.setMission(mission);
    }

    @Override
    public void missionReplaced(MissionType mission) {
        planSelection.setMission(mission);        
    }
    
    public void advance(ActionEvent evt) {
        if (page == pages.size()-1) {            
            for (WizardPage<?> p : pages) {
                try {
                    p.getSelection();
                }
                catch (Exception e) {
                    page = pages.indexOf(p);
                    ((CardLayout)main.getLayout()).show(main, p.getTitle());
                    if (page < pages.size()-1) 
                        btnAdvance.setText(I18n.text("Next"));
                    GuiUtils.errorMessage(dialog, I18n.text("Invalid parameters"), e.getMessage());      
                    return;
                }
            }
            try {
                generatePlan();
            }
            catch (Exception e) {
                NeptusLog.pub().error(e.getMessage(), e);
            }
            
            dialog.setVisible(false);
            dialog.dispose();
            return;
        }
        else if (page == pages.size()-2) {
            btnAdvance.setText(I18n.text("Finish"));
        }
        
        WizardPage<?> currentPage = pages.get(++page);
        
        
        lblTop.setText(currentPage.getTitle());
        ((CardLayout)main.getLayout()).show(main, currentPage.getTitle());
    }
    
    public void back(ActionEvent evt) {
        if (page == 0)
            return;
        btnAdvance.setText(I18n.text("Next"));
        WizardPage<?> currentPage = pages.get(--page);
        lblTop.setText(currentPage.getTitle());
        ((CardLayout)main.getLayout()).show(main, currentPage.getTitle());
                
    }

    public void cancel(ActionEvent evt) {
        dialog.setVisible(false);
        dialog.dispose();
    }


    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }
    
    private void generatePlan() throws Exception {
        System.out.println("GENERATE PLAN");
        PlanType plan = planSelection.getSelection().iterator().next();
        ArrayList<VehicleType> vehicles = new ArrayList<>();
        vehicles.addAll(vehicleSelection.getSelection());
        VerticalFormationOptions params = options.getSelection();
        ArrayList<ManeuverLocation> locations = new ArrayList<>();
        locations.addAll(PlanUtil.getPlanWaypoints(plan));
        double bearing = 0;
        
        if (locations.size() > 1)
            bearing = locations.get(1).getXYAngle(locations.get(0));
        
        long arrivalTime = System.currentTimeMillis() + params.startInMins * 60 * 1000;
        double depth = params.firstDepthMeters;
        
        int v = 0;
        for (VehicleType vehicle : vehicles) {
            PlanType generated = new PlanType(getConsole().getMission());
            generated.setVehicle(vehicle);
            generated.setId(params.planId+"_"+vehicle.getId());
            ManeuverLocation first = new ManeuverLocation(locations.get(0));
            first.setZ(depth);
            first.setZUnits(Z_UNITS.DEPTH);
            FollowTrajectory formation = new FollowTrajectory();
            formation.setManeuverLocation(new ManeuverLocation(first));
            formation.setSpeed(new SpeedType(params.speedMps, Units.MPS));
            formation.setId("3");
            Vector<double[]> waypoints = new Vector<>();
            LocationType previous = first;
            for (LocationType l : locations) {
                double[] point = new double[4];
                double[] offsets = l.getOffsetFrom(first);
                double time = l.getHorizontalDistanceInMeters(previous) / params.speedMps;
                point[0] = offsets[0];
                point[1] = offsets[1];
                point[2] = 0;
                point[3] = time;
                System.out.println("Using time "+time+" to travel "+ l.getHorizontalDistanceInMeters(previous));
                previous = l;
                System.out.println(ArrayUtils.toString(point));
                waypoints.addElement(point);
            }
            formation.setOffsets(waypoints);
            
            double offsetX = 50 * Math.cos(bearing);
            double offsetY = 50 * Math.sin(bearing);
            ManeuverLocation s1 = new ManeuverLocation(first);
            s1.translatePosition(offsetX, offsetY, 0);
            ScheduledGoto man1 = new ScheduledGoto();
            man1.setId("1");
            man1.setManeuverLocation(s1);
            man1.setArrivalTime(new Date(arrivalTime));
            ScheduledGoto man2 = new ScheduledGoto();
            man2.setId("2");
            man2.setManeuverLocation(new ManeuverLocation(first));
            man2.setArrivalTime(new Date(arrivalTime + 50 * 1000));
            ManeuverLocation end1 = new ManeuverLocation(formation.getEndLocation());
            double off = (v % 2 == 1)? v * 30 : v * -30;
            end1.translatePosition(30, off, 0);
            Goto man3 = new Goto();
            man3.setId("4");
            man3.setManeuverLocation(end1);
            ManeuverLocation end2 = new ManeuverLocation(end1);
            end2.setZ(0);            
            StationKeeping sk = new StationKeeping();
            sk.setDuration(0);
            sk.setId("5");
            sk.setManeuverLocation(end2);
            
            generated.getGraph().addManeuver(man1);
            generated.getGraph().addManeuver(man2);
            generated.getGraph().addManeuver(formation);    
            generated.getGraph().addManeuver(man3);
            generated.getGraph().addManeuver(sk);
            generated.getGraph().addTransition(man1.getId(), man2.getId(), "true");
            generated.getGraph().addTransition(man2.getId(), formation.getId(), "true");
            generated.getGraph().addTransition(formation.getId(), man3.getId(), "true");
            generated.getGraph().addTransition(man3.getId(), sk.getId(), "true");
            
            PlanUtil.setPlanSpeed(generated, params.speedMps);
            getConsole().getMission().addPlan(generated);
            v++;
            depth += params.depthSeparationMeters;
        }
        getConsole().getMission().save(true);
        getConsole().warnMissionListeners();
    }

    public static class VerticalFormationOptions {
        @NeptusProperty(name="Depth separation (meters)", description="Vertical separation between the vehicles")
        public double depthSeparationMeters = 1;
        
        @NeptusProperty(name="Depth for the first vehicle", description= "The depth of other vehicles will have \"depth separation\" meters)")
        public double firstDepthMeters = 0.5;
        
        @NeptusProperty(name="Formation speed (m/s)", description="Speed to use while travelling in vertical formation")
        public double speedMps = 1.2;
        
        @NeptusProperty(name="Minutes till first point", description="Amount of minutes to travel to the first waypoint")
        public int startInMins = 1;
        
        @NeptusProperty(name="Generated plans prefix", description="Name of the generated plan will <prefix>_<vehicle>")
        public String planId = "formation";
    }
}
