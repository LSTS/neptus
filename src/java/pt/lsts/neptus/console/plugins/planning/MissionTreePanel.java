/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Pinto
 * 2009/09/22
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.LblBeacon;
import pt.lsts.imc.LblConfig;
import pt.lsts.imc.LblConfig.OP;
import pt.lsts.imc.LblRangeAcceptance;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanControlState.STATE;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.console.plugins.IPlanSelection;
import pt.lsts.neptus.console.plugins.ITransponderSelection;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBControl;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBInfo;
import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.gui.MissionBrowser;
import pt.lsts.neptus.gui.MissionBrowser.State;
import pt.lsts.neptus.gui.MissionTreeModel.NodeInfoKey;
import pt.lsts.neptus.gui.MissionTreeModel.ParentNodes;
import pt.lsts.neptus.gui.tree.ExtendedTreeNode;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.NameId;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.HomeReferenceElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.mission.HomeReference;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ByteUtil;


/**
 * Panel that holds mission objects namely plans and acoustic transponders.
 * 
 * @author ZP
 * @author pdias
 * @author Margarida
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Mission Tree", author = "José Pinto, Paulo Dias, Margarida Faria", icon = "pt/up/fe/dceg/neptus/plugins/planning/mission_tree.png", category = CATEGORY.PLANNING, version = "1.5.0")
public class MissionTreePanel extends ConsolePanel
        implements MissionChangeListener, IPlanSelection, ConfigurationListener, ITransponderSelection {

    @NeptusProperty(name = "Use Plan DB Sync. Features", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    private boolean usePlanDBSyncFeatures = true;
    @NeptusProperty(name = "Use Share and URL Features", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    private boolean useShareAndURLFeatures = true;
    @NeptusProperty(name = "Use Transponder Features", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    private boolean useTransponderFeatures = true;
    @NeptusProperty(name = "Debug", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    private boolean debugOn = false;
    @NeptusProperty(name = "Acceptable Elapsed Time", description = "Maximum acceptable interval between transponder ranges, in seconds.")
    private int maxAcceptableElapsedTime = 600;

    private MissionTreeMouse mouseAdapter;
    private boolean running = false;
    boolean inited = false;
    protected MissionBrowser browser = new MissionBrowser();
    protected PlanDBControl pdbControl;

    /**
     * This adapter is called by a class monitoring PlanDB messages. It is only called if a PlanDB message with field
     * type set to success is received.
     */
    protected MissionTreePlanDbAdapter planDBListener;

    public MissionTreePanel(ConsoleLayout console) {
        super(console);
        planDBListener = new MissionTreePlanDbAdapter(getConsole(), this);
        browser.setMaxAcceptableElapsedTime(maxAcceptableElapsedTime);
        removeAll();
        setPreferredSize(new Dimension(150, 400));
        setMinimumSize(new Dimension(0, 0));
        
        setLayout(new BorderLayout());
        add(browser, BorderLayout.CENTER);

        setupListeners();        
    }

    public void setupListeners() {
        browser.addTreeListener(getConsole());
        mouseAdapter = new MissionTreeMouse();
        browser.addMouseAdapter(mouseAdapter);
    }

    public boolean removePlanMenuItem(String label) {
        return mouseAdapter.removePlanMenuItem(label);
    }

    @Override
    public void cleanSubPanel() {
        removePlanDBListener();
    }

    @Override
    public void missionReplaced(MissionType mission) {
        browser.refreshBrowser(getConsole().getMission(), getMainVehicleId(), getConsole());
    }

    @Override
    public void missionUpdated(MissionType mission) {
        // it is called (among others) when the specs for a remote plan have just been received
        Object selItemOld = browser.getSelectedItem();
        browser.refreshBrowser(getConsole().getMission(), getMainVehicleId(), getConsole());
        Object selItem = browser.getSelectedItem();
        // Update selected plan for console if it has changed from remote to sync (you can now edit it)
        if (selItemOld instanceof PlanDBInfo && selItem instanceof PlanType) {
            getConsole().setPlan((PlanType) selItem);
        }
    }

    @Override
    public void initSubPanel() {
        if (inited)
            return;
        inited = true;
        updatePlanDBListener(getMainVehicleId());
        browser.refreshBrowser(getConsole().getMission(), getMainVehicleId(), getConsole());
        planDBListener.setDebugOn(debugOn);
        addClearPlanDbMenuItem();
    }

    private void addClearPlanDbMenuItem() {
        addMenuItem(I18n.text("Advanced") + ">" + I18n.text("Clear remote PlanDB for main system"), new ImageIcon(
                PluginUtils.getPluginIcon(getClass())), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (pdbControl != null)
                    pdbControl.clearDatabase();
            }
        });
    }

    @Subscribe
    public void on(ConsoleEventMainSystemChange evt) {
        running = false;
        updatePlanDBListener(evt.getCurrent());
        askForBeaconConfig();
        browser.refreshBrowser(getConsole().getMission(), getMainVehicleId(), getConsole());
    }

    /**
     * Ask vehicle for configurations of vehicles in use just by WiFi.
     */
    private void askForBeaconConfig() {
        SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    LblConfig msgLBLConfiguration = new LblConfig();
                    msgLBLConfiguration.setOp(LblConfig.OP.GET_CFG);
                    IMCSendMessageUtils.sendMessage(msgLBLConfiguration, 
                            I18n.textf("Unable to get %vehicle list of transponders.", getMainVehicleId()), 
                            true, true, getMainVehicleId());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        sw.run();
    }

    /**
     * Initializes the vehicle whose plan changes we are listening to.
     * <p>
     * Remove current PlanDB listener. Fetch the running PlanDB listener from IMCSystemsHolder or create a new one if
     * none is found. Set the designeted PlanDB listener.
     * 
     * @param id of the main vehicle.
     */
    private void updatePlanDBListener(String id) {
        removePlanDBListener();
        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(id);
        if (sys == null) {
            pdbControl = null;
            NeptusLog.pub().error(
                    "The main vehicle selected " + id
                            + " is not in the vehicles-defs folder. Please add definitions file for this vehicle.");
        }
        else {
            pdbControl = sys.getPlanDBControl();
            if (pdbControl == null) {
                pdbControl = new PlanDBControl();
                pdbControl.setRemoteSystemId(id);
            }
            pdbControl.addListener(planDBListener);
        }
    }

    private void removePlanDBListener() {
        if (pdbControl != null)
        pdbControl.removeListener(planDBListener);
    }

    @Override
    public Vector<PlanType> getSelectedPlans() {
        ArrayList<NameId> selectedItems = browser.getSelectedItems();
        Vector<PlanType> plans = new Vector<PlanType>();
        if (selectedItems.size() > 0) {
            for (NameId o : selectedItems) {
                if (o instanceof PlanType)
                    plans.add((PlanType) o);
            }
        }
        if (plans.isEmpty() && getConsole().getPlan() != null)
            plans.add(getConsole().getPlan());
        return plans;
    }

    @Override
    public Collection<TransponderElement> getSelectedTransponders() {
        ArrayList<ExtendedTreeNode> selectedNodes = browser.getSelectedNodes();
        ArrayList<TransponderElement> trans = new ArrayList<>();
        for (ExtendedTreeNode node : selectedNodes) {
            Object userObject = node.getUserObject();
            if (userObject instanceof TransponderElement
                    && node.getUserInfo().get(NodeInfoKey.SYNC.name()) != State.REMOTE)
                trans.add((TransponderElement) userObject);
        }
        return trans;
    }

    public boolean matchingPlans(PlanSpecification plan1, PlanSpecification plan2) {
        byte[] localMD5 = plan1.payloadMD5();
        byte[] remoteMD5 = plan2.payloadMD5();
        return ByteUtil.equal(localMD5, remoteMD5);        
    }
    
    @Subscribe
    public void on(PlanSpecification msg) {
        PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(), msg);
        
        boolean alreadyLocal = getConsole().getMission().getIndividualPlansList().containsKey(plan.getId());
        if (alreadyLocal) {
            PlanSpecification local = (PlanSpecification) getConsole().getMission().getIndividualPlansList().get(plan.getId()).asIMCPlan();
            if (!matchingPlans(local, msg)) {
                int option = JOptionPane.showConfirmDialog(getConsole(),
                        I18n.text("Replace plan '"+plan.getId()+"' with version disseminated by "+msg.getSourceName()+"?"));
                if (option != JOptionPane.YES_OPTION)
                    return;
            }
        }
        
        getConsole().getMission().getIndividualPlansList().put(plan.getId(), plan);
        getConsole().getMission().save(true);
        getConsole().post(Notification.success(I18n.text("Plan Dissemination"),
                I18n.textf("Received plan '%plan' from %ccu.", plan.getId(), msg.getSourceName())));
        
        if (alreadyLocal && getConsole().getPlan() != null) {
            if(getConsole().getPlan().getId().equals(plan.getId()))
                getConsole().setPlan(plan);
        }
        
    }
    
    @Subscribe
    public void on(PlanControlState msg) {
     // If vehicle stops, the timers stop as well
        if (msg.getState() == STATE.READY || msg.getState() == STATE.BLOCKED) {
                browser.transStopTimers();
                this.running = false;
        }
        // If vehicle starts, the timers start
        else if (!running) {
            browser.transStartVehicleTimers(getMainVehicleId());
            this.running = true;
        }
    }
    
    @Subscribe
    public void on(LblRangeAcceptance msg) {
        browser.transUpdateTimer(msg.getId(), getMainVehicleId());
    }
    
    @Subscribe
    @SuppressWarnings("unchecked")
    public void on(LblConfig msg) {
        if (msg.getOp() == OP.CUR_CFG) {
            browser.updateTransStateEDT(getConsole().getMission(), getMainVehicleId(),
                    (Vector<LblBeacon>) msg.getBeacons().clone(), getConsole());
        }
    }
    
    @Periodic(millisBetweenUpdates=900)
    public void update() {        
        repaint();       
    }

    @Subscribe
    public void on(ConsoleEventPlanChange event) {
        browser.setSelectedPlan(event.getCurrent());
    }

    /**
     * Called every time a property is changed
     */
    @Override
    public void propertiesChanged() {
        browser.setDebugOn(debugOn);
        planDBListener.setDebugOn(debugOn);
        browser.setMaxAcceptableElapsedTime(maxAcceptableElapsedTime);
        browser.setHideTransponder(!useTransponderFeatures);
    }

    class MissionTreeMouse extends MouseAdapter {
        private Container popupMenu;

        /**
         * @param label in the menu item to remove (before translation)
         * @return true if menu item is found, false otherwise
         */
        public boolean removePlanMenuItem(String label) {
            int componentCount = popupMenu.getComponentCount();
            for (int c = 0; c < componentCount; c++) {
                Component component = popupMenu.getComponent(c);
                if (component instanceof JMenuItem) {
                    String labelM = ((JMenuItem) component).getText();
                    if (labelM.equals(I18n.text(label))) {
                        popupMenu.remove(component);
                        return true;
                    }
                }
            }
            return false;
        }

        private void addActionSendPlan(final ConsoleLayout console2, final PlanDBControl pdbControl,
                final ArrayList<NameId> selectedItems, JPopupMenu popupMenu) {
            if (!usePlanDBSyncFeatures)
                return;
            
            popupMenu.add(
		    I18n.textf("Send %planName to %system", getPlanNamesString(selectedItems, true), console2.getMainSystem()))
                    .addActionListener(

                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            for (NameId nameId : selectedItems) {
                                PlanType sel = (PlanType) nameId;
                                String mainSystem = console2.getMainSystem();
                                pdbControl.setRemoteSystemId(mainSystem);
                                pdbControl.sendPlan(sel);
                            }
                        }
                    });
        }

        private <T extends NameId> StringBuilder getPlanNamesString(final ArrayList<T> selectedItems, boolean plans) {
            StringBuilder objectNames = new StringBuilder();
            
            if (selectedItems.size() > 3) {
                if (plans)
                    objectNames.append(selectedItems.size()+" plans");
                else
                    objectNames.append(selectedItems.size()+" transponders");
                return objectNames;
            }
            
            Iterator<T> it = selectedItems.iterator();
            if (!it.hasNext())
                return objectNames;
            for (;;) {
                T e = it.next();
                objectNames.append(e.getDisplayName());
                if (!it.hasNext())
                    return objectNames;
                objectNames.append(',').append(' ');
            }
        }

        /**
         * @param console2
         * @param selection
         * @param popupMenu
         */
        private void addActionRemovePlanLocally(final ConsoleLayout console2, final ArrayList<NameId> selectedItems,
                JPopupMenu popupMenu) {
            final StringBuilder itemsInString = getPlanNamesString(selectedItems, true);
            String actionTxt = I18n.textf("Delete %planName locally", itemsInString);
            if (!usePlanDBSyncFeatures)
                actionTxt = I18n.textf("Delete %planName", itemsInString);
            popupMenu.add(actionTxt).addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            // if (selection != null) {
                            int resp = JOptionPane.showConfirmDialog(console2,
                                    I18n.textf("Remove %planName from mission?", itemsInString));
                            if (resp == JOptionPane.YES_OPTION) {
                                for (NameId nameId : selectedItems) {
                                    console2.getMission().getIndividualPlansList().remove(nameId.getIdentification());
                                    browser.deleteCurrSelectedNodeLocally();
                                }
                                console2.setPlan(null);
                                console2.getMission().save(false);
                            }
                        }
                    });
        }

        private void addActionGetRemotePlan(final ConsoleLayout console2, final PlanDBControl pdbControl,
                final ArrayList<NameId> remotePlans, JPopupMenu popupMenu) {
            if (!usePlanDBSyncFeatures)
                return;

            StringBuilder itemsInString = getPlanNamesString(remotePlans, true);
            popupMenu.add(I18n.textf("Get %planName from %system", itemsInString, console2.getMainSystem()))
                    .addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            for (NameId nameId : remotePlans) {
                                pdbControl.requestPlan(nameId.getIdentification());
                            }
                        }
                    });
        }

        private void addActionGetRemoteTrans(final ConsoleLayout console2, JPopupMenu popupMenu,
                final ArrayList<NameId> remoteTrans) {
            if (!useTransponderFeatures)
                return;

            StringBuilder itemsInString = getPlanNamesString(remoteTrans, false);
            popupMenu.add(I18n.textf("Get %planName from %system", itemsInString, console2.getMainSystem()))
                    .addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            // Signal ids to merge
                            browser.addTransToMerge(remoteTrans);
                            // Request LBLConfig
                            LblConfig msgLBLConfiguration = new LblConfig();
                            msgLBLConfiguration.setOp(LblConfig.OP.GET_CFG);
                            sendMsg(msgLBLConfiguration);
                        }
                    });
        }

        private void addActionRemovePlanRemotely(final ConsoleLayout console2, final PlanDBControl pdbControl,
                final ArrayList<NameId> synAndUnsyncPlans, JPopupMenu popupMenu) {
            if (!usePlanDBSyncFeatures)
                return;

            popupMenu.add(
		    I18n.textf("Remove '%planName' from %system", getPlanNamesString(synAndUnsyncPlans, true),
                            console2.getMainSystem())).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    pdbControl.setRemoteSystemId(console2.getMainSystem());
                    for (NameId nameId : synAndUnsyncPlans) {
                        pdbControl.deletePlan(nameId.getIdentification());
                    }
                }
            });
        }

        private void addActionRenamePlan(final ConsoleLayout console2, final ArrayList<NameId> selectedItems,
                JPopupMenu popupMenu) {
            
            JMenuItem mItem = popupMenu.add(
                    I18n.textf("Rename %planName", getPlanNamesString(selectedItems, true), console2.getMainSystem()));
            mItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String newName = null;
                    while (true) {
                        String oldPlanName = getPlanNamesString(selectedItems, true).toString();
                        newName = JOptionPane.showInputDialog(I18n.text("New plan name"), oldPlanName);
                        if (newName == null)
                            return;

                        if (!newName.isEmpty()
                                && !getConsole().getMission().getIndividualPlansList().containsKey(newName)) {
                            PlanType plan = getConsole().getMission().getIndividualPlansList().get(oldPlanName);
                            if (plan != null) {
                                if (!getConsole().getMission().renamePlan(plan, newName, true))
                                    continue;

                                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                                    @Override
                                    protected Void doInBackground() throws Exception {
                                        getConsole().getMission().save(true);
                                        return null;
                                    }
                                };
                                worker.execute();
                                browser.refreshBrowser(getConsole().getMission(), getMainVehicleId(), getConsole());
                                return;
                            }
                        }
                    }
                }
            });
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON3)
                return;
            ArrayList<NameId> selectedItems = browser.getSelectedItems();
            ArrayList<ExtendedTreeNode> selectedNodes = browser.getSelectedNodes();
            JPopupMenu popupMenu = new JPopupMenu();
            ItemTypes selecType = findSelecMissionElem(selectedItems);
            
            ArrayList<NameId> toShare = new ArrayList<NameId>();

            switch (selecType) {
                case Plans:

                    if (selectedItems.size() == 1)
                        addActionRenamePlan(getConsole(), selectedItems, popupMenu);

                    popupMenu.addSeparator();
                    // New
                    ArrayList<NameId> toRemoveLocally = new ArrayList<NameId>();
                    ArrayList<NameId> toRemoveRemotely = new ArrayList<NameId>();
                    ArrayList<NameId> toGetPlan = new ArrayList<NameId>();
                    ArrayList<NameId> toSend = new ArrayList<NameId>();
                    
                    State syncState;
                    // Separate plans by state to give appropriated options to each
                    // addActionChangePlanVehicles(selection, popupMenu); // add appropriatly when multivehicles are
                    // needed
                    for (ExtendedTreeNode extendedTreeNode : selectedNodes) {
                        syncState = (State) extendedTreeNode.getUserInfo().get(NodeInfoKey.SYNC.name());
                        
                        if (syncState != null) {
                            switch (syncState) {
                                case REMOTE:
                                    toRemoveRemotely.add((NameId) extendedTreeNode.getUserObject());
                                    toGetPlan.add((NameId) extendedTreeNode.getUserObject());
                                    break;
                                case SYNC:
                                    toRemoveRemotely.add((NameId) extendedTreeNode.getUserObject());
                                    toRemoveLocally.add((NameId) extendedTreeNode.getUserObject());
                                    toShare.add((NameId) extendedTreeNode.getUserObject());
                                    break;
                                case NOT_SYNC:
                                    toRemoveRemotely.add((NameId) extendedTreeNode.getUserObject());
                                    toRemoveLocally.add((NameId) extendedTreeNode.getUserObject());
                                    toSend.add((NameId) extendedTreeNode.getUserObject());
                                    toGetPlan.add((NameId) extendedTreeNode.getUserObject());
                                    toShare.add((NameId) extendedTreeNode.getUserObject());
                                    break;
                                case LOCAL:
                                    toRemoveLocally.add((NameId) extendedTreeNode.getUserObject());
                                    toSend.add((NameId) extendedTreeNode.getUserObject());
                                    toShare.add((NameId) extendedTreeNode.getUserObject());
                                    break;
                            }
                        }
                        else {
                            NeptusLog.pub().error("The plan " + extendedTreeNode + " has no state.");
                        }
                    }
                    if (toRemoveRemotely.size() > 0)
                        addActionRemovePlanRemotely(getConsole(), pdbControl, toRemoveRemotely, popupMenu);
                    if (toRemoveLocally.size() > 0)
                        addActionRemovePlanLocally(getConsole(), toRemoveLocally, popupMenu);
                    if (toSend.size() > 0)
                        addActionSendPlan(getConsole(), pdbControl, toSend, popupMenu);
                    if (toGetPlan.size() > 0)
                        addActionGetRemotePlan(getConsole(), pdbControl, toGetPlan, popupMenu);
                    break;
                case Transponder:
                    addActionAddNewTrans(popupMenu);
                    addActionRemoveAllTrans(popupMenu);
                    if (selectedItems.size() == 1)
                        addActionEditTrans((TransponderElement) selectedItems.get(0), popupMenu);
                    ArrayList<TransponderElement> localTrans = new ArrayList<TransponderElement>();
                    ArrayList<NameId> notSyncTrans = new ArrayList<NameId>();
                    State state;
                    for (ExtendedTreeNode extendedTreeNode : selectedNodes) {
                        state = (State) extendedTreeNode.getUserInfo().get(NodeInfoKey.SYNC.name());
                        // addActionShare(selectedItems, dissemination, "Transponder");
                        toShare.add((NameId) extendedTreeNode.getUserObject());
                        if (state == State.LOCAL)
                            localTrans.add((TransponderElement) extendedTreeNode.getUserObject());
                        if (state == State.NOT_SYNC)
                            notSyncTrans.add((TransponderElement) extendedTreeNode.getUserObject());
                    }
                    if (localTrans.size() > 0) {
                        addActionRemoveTrans(localTrans, popupMenu);
                    }
                    if (notSyncTrans.size() > 0) {
                        addActionGetRemoteTrans(getConsole(), popupMenu, notSyncTrans);
                    }
                    
                    if (useTransponderFeatures) {
                        // Switch
                        JMenu switchM = new JMenu(I18n.text("Switch"));
                        ArrayList<TransponderElement> transponders = browser.getTransponders();
                        if (transponders.size() > 0) {
                            TransponderElement transA, transB;
                            for (int iA = 0; iA < transponders.size(); iA++) {
                                transA = transponders.get(iA);
                                for (int iB = iA + 1; iB < transponders.size(); iB++) {
                                    transB = transponders.get(iB);
                                    if (!transA.getDisplayName().equals(transB.getDisplayName())) {
                                        addActionSwitchTrans(transA, switchM, transB);
                                    }
                                }
                            }
                            popupMenu.add(switchM);
                        }
                    }
                    break;
                case HomeRef:
                    addActionEditHomeRef(selectedItems.get(0), popupMenu);
                    break;
                case Mix:
                case None:
                    // Check if what is selected is a parent folders
                    if (selectedNodes.size() == 1) {
                        String parentName = (String) selectedNodes.get(0).getUserObject();
                        if(parentName.equals(ParentNodes.TRANSPONDERS.nodeName)){
                            addActionAddNewTrans(popupMenu);
                            addActionRemoveAllTrans(popupMenu);
                        }
                    }
                    break;
                default:
                    break;
            }
            popupMenu.addSeparator();
            
            if (!toShare.isEmpty())
                addActionShare(toShare, popupMenu);

            addActionReloadPanel(popupMenu);
            
            Component pef = popupMenu.getComponent(0);
            if (pef != null && pef instanceof JPopupMenu.Separator)
                popupMenu.remove(0);
            
            popupMenu.show((Component) e.getSource(), e.getX(), e.getY());
        }

        private void addActionRemoveAllTrans(JPopupMenu popupMenu) {
            if (!useTransponderFeatures)
                return;

            popupMenu.add(I18n.text("Remove all transponders from vehicle")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LblConfig msgLBLConfiguration = new LblConfig();
                    msgLBLConfiguration.setOp(LblConfig.OP.SET_CFG);
                    msgLBLConfiguration.setBeacons(new Vector<LblBeacon>());
                    sendMsg(msgLBLConfiguration);
                    msgLBLConfiguration = new LblConfig();
                    msgLBLConfiguration.setOp(LblConfig.OP.GET_CFG);
                    sendMsg(msgLBLConfiguration);
                    // TODO On hold until removing all beacons is stable
                    // browser.removeAllTransponders(console.getMission());
                }
            });
        }

        private void sendMsg(IMCMessage msgLBLConfiguration) {
            String errorTextForDialog = I18n.text("Error sending acoustic transponders");
            boolean ignoreAcousticSending = true;
            String acousticOpServiceName = "acoustic/operation";
            boolean acousticOpUseOnlyActive = false;
            boolean acousticOpUserAprovedQuestion = true;
            IMCSendMessageUtils.sendMessage(msgLBLConfiguration, ImcMsgManager.TRANSPORT_TCP, listener,
                    MissionTreePanel.this, errorTextForDialog, ignoreAcousticSending, acousticOpServiceName,
                    acousticOpUseOnlyActive, acousticOpUserAprovedQuestion, true, getMainVehicleId());
        }

        MessageDeliveryListener listener = new MessageDeliveryListener() {
            int tries = 0;
            private final int maxAttemps = 3;

            private String getDest(IMCMessage message) {
                ImcSystem sys = message != null ? ImcSystemsHolder.lookupSystem(message.getDst()) : null;
                String dest = sys != null ? sys.getName() : I18n.text("unknown destination");
                return dest;
            }

            private void processDeliveryFailure(IMCMessage message, String errorText) {
                if (maxAttemps < tries) {
                    tries = 0;
                    post(Notification.error(I18n.text("Delivering Message"), errorText));
                }
                else {
                    tries++;
                    sendMsg(message);
                }
            }

            @Override
            public void deliveryUnreacheable(IMCMessage message) {
                processDeliveryFailure(
                        message,
                        I18n.textf("Message %messageType to %destination delivery destination unreacheable",
                                message.getAbbrev(), getDest(message)));
            }

            @Override
            public void deliveryTimeOut(IMCMessage message) {
                processDeliveryFailure(message, I18n.textf("Message %messageType to %destination delivery timeout",
                        message.getAbbrev(), getDest(message)));
            }

            @Override
            public void deliveryError(IMCMessage message, Object error) {
                processDeliveryFailure(
                        message,
                        I18n.text(I18n.textf("Message %messageType to %destination delivery error. (%error)",
                                message.getAbbrev(), getDest(message), error)));
            }

            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
            }

            @Override
            public void deliverySuccess(IMCMessage message) {
                tries = 0;
            }
        };

        private ItemTypes findSelecMissionElem(ArrayList<NameId> selectedItems) {
            ItemTypes type = ItemTypes.None;
            for (NameId item : selectedItems) {
                if (item instanceof PlanType || item instanceof PlanDBInfo) {
                    if (type == ItemTypes.Plans || type == ItemTypes.None)
                        type = ItemTypes.Plans;
                    else
                        type = ItemTypes.Mix;

                }
                else if (item instanceof HomeReference) {
                    if (type == ItemTypes.HomeRef || type == ItemTypes.None)
                        type = ItemTypes.HomeRef;
                    else
                        type = ItemTypes.Mix;
                }
                else if (item instanceof TransponderElement) {
                    if (type == ItemTypes.Transponder || type == ItemTypes.None)
                        type = ItemTypes.Transponder;
                    else
                        type = ItemTypes.Mix;
                }
            }
            return type;
        }

        private void addActionReloadPanel(JPopupMenu popupMenu) {
            popupMenu.add(I18n.text("Reload Panel")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    browser.refreshBrowser(getConsole().getMission(), getMainVehicleId(), getConsole());
                }
            });
        }

        private void addActionEditHomeRef(final Object selection, JPopupMenu popupMenu) {
            popupMenu.add(I18n.text("View/Edit home reference")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    LocationType loc = new LocationType((HomeReference) selection);
                    LocationType after = LocationPanel.showLocationDialog(getConsole(),
                            I18n.text("Set home reference"), loc, getConsole().getMission(), true);
                    if (after == null)
                        return;

                    getConsole().getMission().getHomeRef().setLocation(after);

                    Vector<HomeReferenceElement> hrefElems = MapGroup.getMapGroupInstance(getConsole().getMission())
                            .getAllObjectsOfType(HomeReferenceElement.class);
                    hrefElems.get(0).setCoordinateSystem(getConsole().getMission().getHomeRef());
                    getConsole().getMission().save(false);
                    getConsole().updateMissionListeners();
                }
            });
        }

        private void addActionSwitchTrans(final TransponderElement selection, JMenu popupMenu,
                final TransponderElement tel) {
            popupMenu.add(
                    I18n.textf("Switch %transponderName1 with %transponderName2", selection.getDisplayName(),
                            tel.getDisplayName()))
                    .addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            new Thread() {
                                @Override
                                public void run() {
                                    browser.swithLocationsTransponder(selection, tel, getConsole());
                                };
                            }.start();
                        }
                    });
        }

        private void addActionRemoveTrans(final ArrayList<TransponderElement> selectedTrans, JPopupMenu popupMenu) {
            if (!useTransponderFeatures)
                return;

            StringBuilder itemsInString = getPlanNamesString(selectedTrans, false);
            popupMenu.add(I18n.textf("Remove %transponderName", itemsInString)).addActionListener(
                    new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (TransponderElement transponderElement : selectedTrans) {
                        browser.removeTransponder(transponderElement, getConsole());
                    }
                }
            });
        }

        private void addActionEditTrans(final TransponderElement selection, JPopupMenu popupMenu) {
            if (!useTransponderFeatures)
                return;

            popupMenu.add(I18n.textf("View/Edit %transponderName", selection.getDisplayName())).addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            browser.editTransponder(selection, getConsole().getMission(),
                                    getMainVehicleId());
                        }
                    });
        }
        
        private void addActionShare(final ArrayList<NameId> selectedItems, JPopupMenu popupMenu2) {
            if (!useShareAndURLFeatures)
                return;

            StringBuilder itemsInString = getPlanNamesString(selectedItems, true);
            popupMenu2.add(I18n.textf("Share %planName", itemsInString)).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (NameId nameId : selectedItems) {
                        IMCMessage spec = ((PlanType) nameId).asIMCPlan();
                        getConsole().getImcMsgManager().broadcastToCCUs(spec);
                    }
                }
            });
        }

        private void addActionAddNewTrans(JPopupMenu popupMenu) {
            if (!useTransponderFeatures)
                return;

            popupMenu.add(I18n.text("Add a new transponder")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    browser.addTransponderElement(getConsole());
                }
            });
        }
    }

    private enum ItemTypes {
        Plans,
        HomeRef,
        Transponder,
        Mix,
        None;
    }
}