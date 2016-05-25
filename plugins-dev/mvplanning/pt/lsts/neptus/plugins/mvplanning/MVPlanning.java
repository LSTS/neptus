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
 * 4 Nov 2015
 */
package pt.lsts.neptus.plugins.mvplanning;


import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JScrollPane;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.data.Pair;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.mvplanning.jaxb.Profile;
import pt.lsts.neptus.plugins.mvplanning.jaxb.ProfileMarshaler;
import pt.lsts.neptus.plugins.mvplanning.planning.algorithm.MST;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.plugins.mvplanning.consoles.NeptusConsoleAdapter;
import pt.lsts.neptus.plugins.mvplanning.events.MvPlanningEventPlanAllocated;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapCell;

/**
 * @author tsmarques
 *
 */
@PluginDescription(name = "Multi-Vehicle Planning")
@Popup(name = "MvPlanning", pos = POSITION.LEFT, width = 285, height = 275)
public class MVPlanning extends ConsolePanel implements PlanChangeListener, Renderer2DPainter {
    @NeptusProperty(name = "Debug mode", description = "Show or hide debug information such as grid areas, spanning trees, etc", userLevel = LEVEL.REGULAR)
    public boolean inDegubMode = false;

    private final ProfileMarshaler pMarsh = new ProfileMarshaler();
    public final Map<String, Profile> availableProfiles = pMarsh.getAllProfiles();

    /* modules */
    private ConsoleAdapter console;
    private VehicleAwareness vawareness;
    private PlanAllocator pAlloc;
    private PlanGenerator pGen;
    private Environment env;

    private Map<String, PlanType> selectedPlans;

    /* User interface */
    private FlowLayout layout;
    private JScrollPane listScroller;
    private JList<String> plans;
    private DefaultListModel<String> listModel;
    private JComboBox<String> profiles;
    private JButton allocateButton;
    private JButton allocateAllButton;
    private JButton clean;
    private JButton pluginStateButton;

    /* Interaction */
    private GridArea opArea;
    private MST mst;

    public MVPlanning(ConsoleLayout console) {
        super(console);
        selectedPlans = new HashMap<>();
        initUi();

        this.console = new NeptusConsoleAdapter(console);
        vawareness = new VehicleAwareness(this.console);
        pAlloc = new PlanAllocator(vawareness, this.console);
        pGen = new PlanGenerator(pAlloc, this.console);
        env = new Environment(this.console);
    }

    private void initUi() {
        layout = new FlowLayout();
        this.setLayout(layout);
        this.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);

        plans = new JList<>();
        listModel = new DefaultListModel<>();
        listScroller = new JScrollPane(plans);
        profiles = new JComboBox<>();
        allocateButton = new JButton("Allocate plan");
        allocateAllButton = new JButton("Allocate all plans");
        clean = new JButton("clean");

        pluginStateButton = new JButton(StateMonitor.STATE.WAITING.value);
        pluginStateButton.setEnabled(false);
        pluginStateButton.setBackground(Color.RED.darker());

        plans.setPreferredSize(new Dimension(225, 280));
        plans.setModel(listModel);
        profiles.setPreferredSize(new Dimension(225, 30));
        allocateButton.setPreferredSize(new Dimension(100, 30));
        clean.setPreferredSize(new Dimension(100, 30));
        allocateAllButton.setPreferredSize(new Dimension(50, 30));
        pluginStateButton.setPreferredSize(new Dimension(100, 30));


        /* fetch available profiles */
        for(String profile : availableProfiles.keySet())
            profiles.addItem(profile);

        allocateButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String desiredProfile = (String) profiles.getSelectedItem();
                String desiredPlan = plans.getSelectedValue();

                if(desiredPlan != null) {
                    pGen.generatePlan(availableProfiles.get(desiredProfile), selectedPlans.get(desiredPlan));

                    int index = listModel.indexOf(desiredPlan);
                    listModel.set(index, desiredPlan + " [" + desiredProfile + "]");
                }
            }
        });

        clean.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                plans.removeAll();
                listModel.removeAllElements();
            }
        });
        
        pluginStateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(StateMonitor.isPluginPaused()) {
                    StateMonitor.resumePlugin();
                    pluginStateButton.setText(StateMonitor.STATE.RUNNING.value);
                    pluginStateButton.setBackground(Color.GREEN.darker());
                }
                else {
                    StateMonitor.pausePlugin();
                    pluginStateButton.setText(StateMonitor.STATE.PAUSED.value);
                    pluginStateButton.setBackground(Color.YELLOW.darker());
                }
            }
        });


        this.add(profiles);
        this.add(listScroller);
        this.add(allocateButton);
        this.add(clean);
        this.add(pluginStateButton);
    }


    @Override
    public void planChange(PlanType plan) {
        if(plan != null) {
            String planId = plan.getId();
            if(!planId.startsWith("go_") && !planId.startsWith("sk_") && !planId.startsWith("lt_")) {
                if(listModel.contains(planId))
                    listModel.removeElement(planId);
                listModel.addElement(planId);
                selectedPlans.put(planId, plan);
            }
        }
    }

    @Subscribe
    public synchronized void on(MvPlanningEventPlanAllocated event) {
        String lookupId = event.getPlanId() + " [" + event.getProfile() + "]";
        String newId = lookupId + " [" + event.getVehicle() + "]";
        int index;

        while((index = listModel.indexOf(lookupId)) == -1);

        listModel.set(index, newId);
    }

    @Override
    public void cleanSubPanel() {
    }

    @Override
    public void initSubPanel() {
        console.registerToEventBus(vawareness);
    }

    @Subscribe
    public void mapChanged(MapChangeEvent event) {
        if(event.getChangedObject().getId().startsWith("mvp_")) {
            String objType = event.getChangedObject().getType();

            if(objType.equals("Parallelepiped")) {
                ParallelepipedElement elem = (ParallelepipedElement) event.getChangedObject();
                LocationType lt = elem.getCenterLocation();

                opArea = new GridArea(60, elem.getWidth(), elem.getLength(), elem.getYawRad(), lt, env);

                mst = new MST(opArea.getAllCells()[0][0]);

                String desiredProfile = (String) profiles.getSelectedItem();
                List<PlanType> plans = pGen.generateCoverageArea(availableProfiles.get(desiredProfile), opArea);

                if(!plans.isEmpty()) {
                    for(PlanType plan : plans) {
                        listModel.addElement(plan.getId());
                        selectedPlans.put(plan.getId(), plan);
                    }
                }
            }
        }
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if(inDegubMode) {
            g.setTransform(new AffineTransform());
            if(opArea != null && mst != null) {
                g.setColor(Color.cyan);
                opArea.paint(g, renderer, 0.0);

                g.setTransform(new AffineTransform());
                g.setColor(Color.RED);

                for(Pair<MapCell, MapCell> edges : mst.getEdges()) {
                    Point2D p1 = renderer.getScreenPosition(edges.first().getLocation());
                    Point2D p2 = renderer.getScreenPosition(edges.second().getLocation());

                    g.drawLine((int) p1.getX(),(int) p1.getY(),(int) p2.getX(), (int) p2.getY());
                }
            }
        }
    }
}
