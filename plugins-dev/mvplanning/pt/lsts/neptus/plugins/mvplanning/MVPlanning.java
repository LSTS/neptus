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
import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JScrollPane;

import org.dom4j.Element;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.IConsoleInteraction;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.mvplanning.jaxb.Profile;
import pt.lsts.neptus.plugins.mvplanning.jaxb.ProfileMarshaler;
import pt.lsts.neptus.plugins.mvplanning.planning.mapdecomposition.GridArea;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.NameNormalizer;
import pt.lsts.neptus.plugins.mvplanning.consoles.NeptusConsoleAdapter;
import pt.lsts.neptus.plugins.mvplanning.events.MvPlanningEventPlanAllocated;
import pt.lsts.neptus.plugins.mvplanning.interfaces.ConsoleAdapter;

/**
 * @author tsmarques
 *
 */
@PluginDescription(name = "Multi-Vehicle Planning")
@Popup(name = "MvPlanning", pos = POSITION.LEFT, width = 285, height = 240)
public class MVPlanning extends ConsolePanel implements PlanChangeListener, IConsoleInteraction {
    private final ProfileMarshaler pMarsh = new ProfileMarshaler();
    public final Map<String, Profile> availableProfiles = pMarsh.getAllProfiles();

    /* modules */
    private ConsoleAdapter console;
    private VehicleAwareness vawareness;
    private PlanAllocator pAlloc;
    private PlanGenerator pGen;

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

    /* Interaction */
    private boolean interactionActive;
    private GridArea opArea;

    public MVPlanning(ConsoleLayout console) {
        super(console);
        selectedPlans = new HashMap<>();
        initUi();

        this.console = new NeptusConsoleAdapter(console);
        vawareness = new VehicleAwareness(this.console);
        pAlloc = new PlanAllocator(vawareness, this.console);
        pGen = new PlanGenerator(pAlloc, this.console);

        interactionActive = false;
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

        plans.setPreferredSize(new Dimension(225, 280));
        plans.setModel(listModel);
        profiles.setPreferredSize(new Dimension(225, 30));
        allocateButton.setPreferredSize(new Dimension(100, 30));
        clean.setPreferredSize(new Dimension(100, 30));
        allocateAllButton.setPreferredSize(new Dimension(50, 30));


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


        this.add(profiles);
        this.add(listScroller);
        this.add(allocateButton);
        this.add(clean);
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


    /************************************************/
    /* Handle interaction with MvPlanning:
     *  By mouse clicks on the console's map or
     *  by IMC messages coming from Ripples or other
     *  plugins  */


    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        if(interactionActive) {
            LocationType lt = source.getRealWorldLocation(event.getPoint());
            opArea = new GridArea(500, 500, lt);
            opArea.decomposeMap();

            String desiredProfile = (String) profiles.getSelectedItem();
            String planId = "coverage_" + NameNormalizer.getRandomID();
            PlanType plan = pGen.generateCoverageArea(availableProfiles.get(desiredProfile), opArea, planId);

            listModel.addElement(planId);
            selectedPlans.put(planId, plan);
        }
    }

    @Override
    public void mouseDragged(MouseEvent event, StateRenderer2D source) {
    }

    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        if(event.isControlDown())
            interactionActive = true;
    }

    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        /* isControlUp */
        if(event.getKeyCode() == KeyEvent.VK_CONTROL)
            interactionActive = false;
    }

    @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
    }


    @Override
    public void paintInteraction(Graphics2D g, StateRenderer2D source) {
        g.setTransform(new AffineTransform());
        if(opArea != null) {
            g.setColor(Color.cyan);
            opArea.paint(g, source, 0.0);
        }
    }


    /*********************************************************/
    /******************** Unused methods ********************/

    @Override
    public Image getIconImage() {
        return null;
    }

    @Override
    public Cursor getMouseCursor() {
        return null;
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {

    }

    @Override
    public void focusLost(FocusEvent event, StateRenderer2D source) {
    }


    @Override
    public void focusGained(FocusEvent event, StateRenderer2D source) {
    }

    @Override
    public void wheelMoved(MouseWheelEvent event, StateRenderer2D source) {
    }

    @Override
    public void setAssociatedSwitch(ToolbarSwitch tswitch) {
    }

    @Override
    public void mouseMoved(MouseEvent event, StateRenderer2D source) {
    }

    @Override
    public void mouseExited(MouseEvent event, StateRenderer2D source) {
    }

    @Override
    public void mouseReleased(MouseEvent event, StateRenderer2D source) {
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D source) {
    }

    @Override
    public void init(ConsoleLayout console) {
    }

    @Override
    public void parseXmlElement(Element elem) {
    }
}
