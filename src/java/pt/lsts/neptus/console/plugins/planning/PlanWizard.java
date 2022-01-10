/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.maneuvers.FollowPath;
import pt.lsts.neptus.mp.maneuvers.FollowTrajectory;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.PopUp;
import pt.lsts.neptus.mp.maneuvers.ScheduledGoto;
import pt.lsts.neptus.mp.maneuvers.StationKeeping;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.coord.PolygonType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.AbstractElement.ELEMENT_TYPE;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.wizard.InvalidUserInputException;
import pt.lsts.neptus.wizard.MapElementSelectionPage;
import pt.lsts.neptus.wizard.PojoPropertiesPage;
import pt.lsts.neptus.wizard.WizardPage;

/**
 * @author zp
 */
@PluginDescription
@Popup(width=800, height=600, pos=POSITION.CENTER)
public class PlanWizard extends ConsolePanel implements MissionChangeListener {

    private static final long serialVersionUID = -8189579968717153114L;

    private JButton btnAdvance, btnBack, btnCancel;
    private JPanel main;
    
    ArrayList<WizardPage<?>> pages = new ArrayList<>(); 
    private JLabel lblTop = new JLabel();
    private MapElementSelectionPage elemSelection;
    private PojoPropertiesPage<MultiVehicleSurveyOptions> options = 
            new PojoPropertiesPage<MultiVehicleSurveyOptions>(new MultiVehicleSurveyOptions());
    int page = 0;
    
    /**
     * @param console
     */
    public PlanWizard(ConsoleLayout console) {
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
        
        elemSelection = new MapElementSelectionPage(console.getMission(), ELEMENT_TYPE.TYPE_PATH, ELEMENT_TYPE.TYPE_PARALLELEPIPED);
        
        pages.add(elemSelection);
        pages.add(options);
        main = new JPanel(new CardLayout());
        pages.forEach( p -> main.add(p, p.getTitle()));
        add(main, BorderLayout.CENTER);
        lblTop.setText(pages.get(0).getTitle());
    }
    
    
    @Override
    public void missionUpdated(MissionType mission) {
        elemSelection.setMission(mission);
    }

    @Override
    public void missionReplaced(MissionType mission) {
        elemSelection.setMission(mission);        
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
    
    private ManeuverLocation createLoc(LocationType loc) throws InvalidUserInputException {
        ManeuverLocation manLoc;
        manLoc = new ManeuverLocation(loc);
        if (options.getSelection().depth >= 0) {
            manLoc.setZ(options.getSelection().depth);
            manLoc.setZUnits(Z_UNITS.DEPTH);
        }
        else {
            manLoc.setZ(-options.getSelection().depth);
            manLoc.setZUnits(Z_UNITS.ALTITUDE);
        }
        
        return manLoc;
    }
    
    private void generatePlan() throws Exception {
        
        PolygonType poly = new PolygonType();
        AbstractElement path = elemSelection.getSelection();
        path.getShapePoints().forEach(p -> poly.addVertex(p));
        
        Pair<Double, Double> diamAngle = poly.getDiameterAndAngle();
        ArrayList<LocationType> coverage = poly.getCoveragePath(diamAngle.second(), options.getSelection().swathWidth, options.getSelection().corner);
        
        if (options.getSelection().reversed)
            Collections.reverse(coverage);
        
        PlanType generated = new PlanType(getConsole().getMission());
        
        int manId = 1;
        LocationType lastLoc;
        long curTime = System.currentTimeMillis() + options.getSelection().startInMins * 60_000;
        if (options.getSelection().timedPlan) {
            ScheduledGoto m1 = new ScheduledGoto();
            m1.setId("SG"+manId++);
            m1.setManeuverLocation(createLoc(coverage.remove(0)));            
            m1.setArrivalTime(new Date(curTime));
            generated.getGraph().addManeuverAtEnd(m1);
            lastLoc = new LocationType(m1.getManeuverLocation());        
        }
        else {
            Goto m1 = new Goto();
            m1.setId("Go"+manId++);
            lastLoc = new ManeuverLocation(coverage.remove(0));
            ManeuverLocation mloc = createLoc(lastLoc);
            mloc.setZ(0);
            mloc.setZUnits(Z_UNITS.DEPTH);
            m1.setManeuverLocation(mloc);
            generated.getGraph().addManeuverAtEnd(m1);
            m1.setManeuverLocation(mloc);
            m1.getManeuverLocation().setAbsoluteDepth(0);
            lastLoc = new LocationType(m1.getManeuverLocation());
            
            PopUp man = new PopUp();
            man.setId("P"+manId++);
            man.setManeuverLocation(createLoc(lastLoc));
            man.setDuration(options.getSelection().popupDuration);
            generated.getGraph().addManeuverAtEnd(man);
        }
        long lastPopup = System.currentTimeMillis();
        
        FollowTrajectory traj = null;
        
        while(!coverage.isEmpty()) {
            LocationType loc = coverage.remove(0);
            
            double distanceToTarget = lastLoc.getDistanceInMeters(loc);
            long targetEta = (long) ((distanceToTarget / options.getSelection().speedMps) * 1000 + curTime);
            
            if ((targetEta - lastPopup)/60_000.0 > options.getSelection().popupMins) {
                if (traj != null)
                    generated.getGraph().addManeuverAtEnd(traj);                
                traj = null;
                
                //add popup
                PopUp man = new PopUp();
                man.setId("P"+manId++);
                ManeuverLocation mloc = createLoc(lastLoc);
                man.setManeuverLocation(mloc);
                man.setDuration(options.getSelection().popupDuration);
                man.setWaitAtSurface(options.getSelection().popupWaitAtSurface);
                generated.getGraph().addManeuverAtEnd(man);
                lastPopup = curTime + options.getSelection().popupDuration * 1_000;
                targetEta += options.getSelection().popupDuration * 1_000;
                
            }
            
            if (traj == null) {
                if (options.getSelection().timedPlan) {
                    traj = new FollowTrajectory();
                    traj.setId("FT"+manId++);
                }
                else {
                    traj = new FollowPath();
                    traj.setId("FP"+manId++);
                }
                traj.setManeuverLocation(createLoc(lastLoc));
                Vector<double[]> curPath = new Vector<>();
                curPath.add(new double[] {0, 0, 0, 0});
                traj.setOffsets(curPath);            
                
            }
            
            Vector<double[]> curPath = new Vector<>();
            curPath.addAll(traj.getPathPoints());            
            double[] offsets = loc.getOffsetFrom(traj.getManeuverLocation());
            curPath.add(new double[] {offsets[0], offsets[1], offsets[2], (targetEta - curTime) / 1000.0});
            traj.setOffsets(curPath);            
            lastLoc = loc;
            curTime = targetEta;            
        }
        
        if (traj != null)
            generated.getGraph().addManeuverAtEnd(traj);        
        
        
        StationKeeping man = new StationKeeping();
        man.setId("SK"+manId++);
        ManeuverLocation mloc = createLoc(lastLoc);
        mloc.setZ(0);
        mloc.setZUnits(Z_UNITS.DEPTH);
        man.setManeuverLocation(mloc);
        man.setDuration(0);
        generated.getGraph().addManeuverAtEnd(man);
        man.getManeuverLocation().setAbsoluteDepth(0);
        
        PlanUtil.setPlanSpeed(generated, options.getSelection().speedMps);
        
        generated.setId(options.getSelection().planId);
        getConsole().getMission().addPlan(generated);
        getConsole().getMission().save(true);
        getConsole().warnMissionListeners();
    }

    public static class MultiVehicleSurveyOptions {
        @NeptusProperty(name="Swath Width", description="Cross-track region covered by each vehicle")
        public double swathWidth = 180;
        
        @NeptusProperty(name="Depth", description= "Depth at which to travel (negative for altitude)")
        public double depth = 4;
        
        @NeptusProperty(name="Speed (m/s)", description="Speed to use while travelling")
        public double speedMps = 1.2;
        
        @NeptusProperty(name="Minutes till first point", description="Amount of minutes to travel to the first waypoint")
        public int startInMins = 1;
        
        @NeptusProperty(name="Create timed plan", description="Opt to generate desired ETA for each waypoint")
        public boolean timedPlan = false;
        
        @NeptusProperty(name="Popup periodicity in minutes", description="Do not stay underwater more than this time (minutes)")
        public int popupMins = 30;
        
        @NeptusProperty(name="Popup duration in seconds", description="How long to stay at surface when the vehicle pops up")
        public int popupDuration = 45;
        
        @NeptusProperty(name="Popup Wait at surface", description="If set, the vehicle will wait <duration> seconds before diving, otherwise will dive after GPS fix.")
        public boolean popupWaitAtSurface = true;
                
        @NeptusProperty(name="Generated plan id", description="Name of the generated plan")
        public String planId = "plan_wiz";
        
        @NeptusProperty(name="Reversed", description="Reverse plan")
        public boolean reversed = false;
        
        @NeptusProperty(name="Corner", description="First Corner")
        public int corner = 0;
        
        
    }
}
