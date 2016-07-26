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
import pt.lsts.neptus.plugins.mvplanning.consoles.NeptusConsoleAdapter;
import pt.lsts.neptus.plugins.mvplanning.events.MvPlanningEventNewOpArea;
import pt.lsts.neptus.plugins.mvplanning.events.MvPlanningEventPlanAllocated;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;
import pt.lsts.neptus.plugins.mvplanning.interfaces.MapCell;
import pt.lsts.neptus.plugins.mvplanning.interfaces.PlanTask;
import pt.lsts.neptus.plugins.mvplanning.jaxb.ProfileMarshaler;
import pt.lsts.neptus.plugins.mvplanning.jaxb.profiles.Profile;
import pt.lsts.neptus.plugins.mvplanning.monitors.Environment;
import pt.lsts.neptus.plugins.mvplanning.monitors.ExternalSystemsMonitor;
import pt.lsts.neptus.plugins.mvplanning.monitors.StateMonitor;
import pt.lsts.neptus.plugins.mvplanning.monitors.VehicleAwareness;
import pt.lsts.neptus.plugins.mvplanning.planning.algorithm.MST;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;
import pt.lsts.neptus.plugins.mvplanning.planning.tasks.CoverageArea;
import pt.lsts.neptus.plugins.mvplanning.planning.tasks.VisitPoint;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.NameNormalizer;

import javax.swing.*;
import javax.xml.bind.JAXBException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author tsmarques
 *
 */
@PluginDescription(name = "Multi-Vehicle Planning")
@Popup(name = "MvPlanning", pos = POSITION.LEFT, width = 285, height = 275)
public class MVPlanning extends ConsolePanel implements PlanChangeListener, Renderer2DPainter {
    @NeptusProperty(name = "Debug mode", description = "Show or hide debug information such as grid areas, spanning trees, etc", userLevel = LEVEL.REGULAR)
    public boolean inDegubMode = false;

    @NeptusProperty(name = "Show operational area", description = "Show operational area generated", userLevel = LEVEL.REGULAR)
    public boolean showOpArea = false;

    private final ProfileMarshaler pMarsh = new ProfileMarshaler();
    public final Map<String, Profile> availableProfiles = pMarsh.getAllProfiles();

    /* modules */
    private ConsoleAdapter console;
    private VehicleAwareness vawareness;
    private PlanAllocator pAlloc;
    private PlanGenerator pGen;
    private Environment env;
    private StateMonitor stateMonitor;
    private ExternalSystemsMonitor extSysMonitor;
    private GridArea opArea;
    private final Object OP_AREA_LOCK = new Object();

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
    private GridArea covArea;
    private MST mst;


    public MVPlanning(ConsoleLayout console) {
        super(console);
        selectedPlans = new HashMap<>();
        initUi();

        this.console = new NeptusConsoleAdapter(console);
        vawareness = new VehicleAwareness(this.console);
        pGen = new PlanGenerator(this.console);
        pAlloc = new PlanAllocator(vawareness, this.console, pGen);

        env = new Environment(this.console, pAlloc, pGen);
        stateMonitor = new StateMonitor(this.console);
        extSysMonitor = new ExternalSystemsMonitor(this.console, pAlloc, pGen);

        /* FIXME: this values should not be hard-coded */
        pGen.computeOperationalArea(env, 1000, 1000, 10);
        fetchPlans();
    }

    /**
     * If there are unfinished plans, load them
     * */
    private void fetchPlans() {
        try {
            NeptusLog.pub().info("Fetching unfinished plans");
            List<PlanTask> unfPlans = stateMonitor.loadPlans();

            if(unfPlans.size() == 0) {
                NeptusLog.pub().info("No plans to fetch");
                return;
            }

            synchronized (selectedPlans) {
                for(PlanTask ptask : unfPlans) {
                    String planId = ptask.getPlanId();
                    listModel.addElement(planId);
                    selectedPlans.put(planId, ptask.asPlanType());
                }
            }
        }
        catch (JAXBException e) {
            NeptusLog.pub().warn("Couldn't load unfinished plans");
            e.printStackTrace();
        }
    }

    @Subscribe
    public void on(MvPlanningEventNewOpArea event) {
        synchronized(OP_AREA_LOCK) {
            opArea = event.getArea();

            pluginStateButton.setEnabled(true);
            console.notifiySuccess("MvPlanning: Operational area updated", "");
            pausePlugin();
        }
    }

    private void resumePlugin() {
        StateMonitor.resumePlugin();
        pluginStateButton.setText(StateMonitor.STATE.RUNNING.value);
        pluginStateButton.setBackground(Color.GREEN.darker());

        allocateButton.setEnabled(true);
    }

    private void pausePlugin() {
        StateMonitor.pausePlugin();
        pluginStateButton.setText(StateMonitor.STATE.PAUSED.value);
        pluginStateButton.setBackground(Color.YELLOW.darker());

        allocateButton.setEnabled(false);
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
        allocateButton.setEnabled(false);

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
                if(StateMonitor.isPluginPaused())
                    resumePlugin();
                else
                    pausePlugin();
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
                synchronized(selectedPlans) {
                    if(listModel.contains(planId))
                        listModel.removeElement(planId);
                    listModel.addElement(planId);
                    selectedPlans.put(planId, plan);
                }
            }
        }
    }

    @Subscribe
    public void on(MvPlanningEventPlanAllocated event) {
        synchronized(selectedPlans) {
            if(event.getPlan().getTaskType() == PlanTask.TASK_TYPE.SAFETY)
                console.notifyWarning("MvPlanning: " + event.getVehicle() + " is replanning", "");
            else
                console.notifiySuccess("MvPlanning: Allocated plan " + event.getPlanId() +
                    " " + event.getVehicle(), "");

            String lookupId = event.getPlanId() + " [" + event.getProfile() + "]";
            String newId = lookupId + " [" + event.getVehicle() + "]";

            int index = listModel.indexOf(lookupId);
            listModel.set(index, newId);
        }
    }

    @Override
    public void cleanSubPanel() {
        console.unregisterToEventBus(vawareness);
        console.unsubscribeToIMCMessages(vawareness);
        console.unregisterToEventBus(stateMonitor);
        console.unsubscribeToIMCMessages(stateMonitor);
        console.unregisterToEventBus(env);
        PeriodicUpdatesService.unregister(extSysMonitor);
        extSysMonitor.cleanup();

        NeptusLog.pub().info("Saving unfinished plans/tasks");
        stateMonitor.stopPlugin();
    }

    @Override
    public void initSubPanel() {
        console.registerToEventBus(vawareness);
        console.subscribeToIMCMessages(vawareness);
        console.registerToEventBus(stateMonitor);
        console.subscribeToIMCMessages(stateMonitor);
        console.registerToEventBus(env);

        PeriodicUpdatesService.register(extSysMonitor);
    }

    @Subscribe
    public void mapChanged(MapChangeEvent event) {
        if(StateMonitor.isPluginPaused() || event == null || event.getChangedObject() == null)
            return;

        if(event.getChangedObject().getId().startsWith("mvp_")) {
            if(event.getEventType() == MapChangeEvent.OBJECT_REMOVED)
                return;

            String objType = event.getChangedObject().getType();

            if(objType.equals("Parallelepiped"))
                handleParallelepipedElement((ParallelepipedElement) event.getChangedObject());
            else if(objType.equals("Mark"))
                handleMarkElement((MarkElement) event.getChangedObject());
            else
                NeptusLog.pub().warn(objType + " is not a valid map object to generate an MvPlanning Task");
        }
    }

    private void handleParallelepipedElement(ParallelepipedElement elem) {
        LocationType lt = elem.getCenterLocation();
        covArea = new GridArea(60, elem.getWidth(), elem.getLength(), elem.getYawRad(), lt, env);
        mst = new MST(covArea.getAllCells()[0][0]);

        String id = "c_" + NameNormalizer.getRandomID();
        String desiredProfile = (String) profiles.getSelectedItem();
        PlanTask task = new CoverageArea(id, availableProfiles.get(desiredProfile), covArea);
        List<PlanTask> plans = pAlloc.allocate(task);

        updatePlansList(plans);
    }


    private void handleMarkElement(MarkElement mark) {
        String type = mark.getId().split("mvp_")[1];

        /* generating a visit plan */
        if(type.contains("visit")) {
            String id = "v_" + NameNormalizer.getRandomID();
            String desiredProfile = (String) profiles.getSelectedItem();
            PlanTask task = new VisitPoint(id, availableProfiles.get(desiredProfile), mark.getCenterLocation());
            List<PlanTask> plans = pAlloc.allocate(task);

            updatePlansList(plans);
        }
        else /* marking the position of a vehicle */
            vawareness.setVehicleStartLocation(type, mark.getCenterLocation());
    }

    private void updatePlansList(List<PlanTask> plans) {
        if(!plans.isEmpty()) {
            for(PlanTask plan : plans) {
                String id = plan.getPlanId();
                PlanType planType = plan.asPlanType();

                listModel.addElement(id);
                selectedPlans.put(id, planType);

                 /* add plan to plan's tree */
                console.addPlanToMission(planType);
            }

             /*save mission*/
            console.saveMission();
        }
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if(inDegubMode) {
            g.setTransform(new AffineTransform());
            if(covArea != null && mst != null) {
                g.setColor(Color.cyan);
                covArea.paint(g, renderer, 0.0);

                g.setTransform(new AffineTransform());
                g.setColor(Color.RED);

                for(Pair<MapCell, MapCell> edges : mst.getEdges()) {
                    Point2D p1 = renderer.getScreenPosition(edges.first().getLocation());
                    Point2D p2 = renderer.getScreenPosition(edges.second().getLocation());

                    g.drawLine((int) p1.getX(),(int) p1.getY(),(int) p2.getX(), (int) p2.getY());
                }
            }
        }
        if(showOpArea) {
            synchronized(OP_AREA_LOCK) {
                if(opArea != null)
                    opArea.paint(g, renderer, 0.0);
            }
        }
    }
}
