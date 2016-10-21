/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
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
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
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

import pt.lsts.imc.IMCDefinition;
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
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
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
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.NameId;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.HomeReferenceElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.mission.HomeReference;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;


/**
 * Panel that holds mission objects namely plans and acoustic transponders.
 * 
 * @author ZP
 * @author pdias
 * @author Margarida
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Mission Tree", author = "José Pinto, Paulo Dias, Margarida Faria", icon = "pt/up/fe/dceg/neptus/plugins/planning/mission_tree.png", category = CATEGORY.PLANNING, version = "1.5.0")
public class MissionTreePanel extends ConsolePanel implements MissionChangeListener, MainVehicleChangeListener,
        DropTargetListener, NeptusMessageListener, IPlanSelection, IPeriodicUpdates, ConfigurationListener,
        ITransponderSelection {

    @NeptusProperty(name = "Use Plan DB Sync. Features", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    public boolean usePlanDBSyncFeatures = true;
    @NeptusProperty(name = "Use Plan DB Sync. Features Extended", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER, description = "Needs 'Use Plan DB Sync. Features' on")
    public boolean usePlanDBSyncFeaturesExt = false;
    @NeptusProperty(name = "Debug", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    public boolean debugOn = false;
    @NeptusProperty(name = "Acceptable Elapsed Time", description = "Maximum acceptable interval between transponder ranges, in seconds.")
    public int maxAcceptableElapsedTime = 600;

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
        setMaximumSize(new Dimension(1000, 1000));

        setLayout(new BorderLayout());
        add(browser, BorderLayout.CENTER);

        new DropTarget(browser, this).setActive(true);

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

    /**
     * 
     */
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

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragExit(DropTargetEvent dte) {

    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {

    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        // try {
        // Transferable tr = dtde.getTransferable();
        // DataFlavor[] flavors = tr.getTransferDataFlavors();
        // for (int i = 0; i < flavors.length; i++) {
        // // NeptusLog.pub().info("<###>Possible flavor: " + flavors[i].getMimeType());
        // if (flavors[i].isMimeTypeEqual("text/plain; class=java.lang.String; charset=Unicode")) {
        // dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        // String url = null;
        //
        // Object data = tr.getTransferData(flavors[i]);
        // if (data instanceof InputStreamReader) {
        // BufferedReader reader = new BufferedReader((InputStreamReader) data);
        // url = reader.readLine();
        // reader.close();
        // dtde.dropComplete(true);
        //
        // }
        // else if (data instanceof String) {
        // url = data.toString();
        // }
        // else
        // dtde.rejectDrop();
        //
        // if (browser.parseURL(url, getConsole().getMission()))
        // dtde.dropComplete(true);
        // else
        // dtde.rejectDrop();
        //
        // return;
        // }
        // }
        // }
        // catch (Exception e) {
        // e.printStackTrace();
        // }
        // dtde.rejectDrop();
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {

    }

    @Override
    public String[] getObservedMessages() {
        String[] messages = new String[4];
        messages[0] = "LblRangeAcceptance";
        // For one specific plan
        messages[1] = "PlanControlState";
        messages[2] = "LblConfig";
        // For one specific plan
        if (IMCDefinition.getInstance().getMessageId("PlanSpecification") != -1) {
            messages[3] = "PlanSpecification";
        }
        return messages;
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange evt) {
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
        // final TreePath[] multiSel = selectedNodes;
        ArrayList<TransponderElement> trans = new ArrayList<>();
        // if (multiSel != null) {
        // for (TreePath path : multiSel) {
        for (ExtendedTreeNode node : selectedNodes) {
            // ExtendedTreeNode node = ((ExtendedTreeNode) path.getLastPathComponent());
                Object userObject = node.getUserObject();

                if (userObject instanceof TransponderElement
                        && node.getUserInfo().get(NodeInfoKey.SYNC) != State.REMOTE)
                    trans.add((TransponderElement) userObject);
            }
        // }
        return trans;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void messageArrived(IMCMessage message) {
        int mgid = message.getMgid();
        // pdbControl.onMessage(null, message);
        switch (mgid) {
        // If new plan arrives add it to mission
            case PlanSpecification.ID_STATIC:
                PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(), message);
                if (!getConsole().getMission().getIndividualPlansList().containsKey(plan.getId())) {
                    getConsole().getMission().getIndividualPlansList().put(plan.getId(), plan);
                    getConsole().updateMissionListeners();
                    getConsole().getMission().save(true);
                }
                break;
            // Timer management
            // Update the state of timers for last received LblRangeAcceptance
            case PlanControlState.ID_STATIC:
                PlanControlState planState = (PlanControlState) message;
                // If vehicle stops, the timers stop as well
                if (planState.getState() == STATE.READY || planState.getState() == STATE.BLOCKED) {
                        browser.transStopTimers();
                        this.running = false;
                }
                // If vehicle starts, the timers start
                else if (!running) {
                    browser.transStartVehicleTimers(getMainVehicleId());
                    this.running = true;
                }

                break;
            // Timer management
            // Reset the timer corresponding to the message
            case LblRangeAcceptance.ID_STATIC:
                LblRangeAcceptance acceptance;
                try {
                    acceptance = LblRangeAcceptance.clone(message);
                    short id = acceptance.getId();
                    browser.transUpdateTimer(id, getMainVehicleId());
                }
                catch (Exception e) {
                    NeptusLog.pub().error("Problem cloning a message.", e);
                }
             break;
            // Update beacons list and state
            case LblConfig.ID_STATIC:
                LblConfig lblConfig = (LblConfig) message;
                if (((LblConfig) message).getOp() == OP.CUR_CFG) {
                    // NeptusLog.pub().error("LblConfig message arrived");
                    browser.updateTransStateEDT(getConsole().getMission(), getMainVehicleId(),
                            (Vector<LblBeacon>) lblConfig.getBeacons().clone(), getConsole());
                }
                break;

            default:
                NeptusLog.pub().error("Unknown message " + mgid);
                break;
        }
    }

    @Override
    public long millisBetweenUpdates() {
        return 900;
    }

    @Override
    public boolean update() {
        // only for timers so only repaint if they are running
        if (running) {
            repaint();
        }
        return true;
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
    }

    class MissionTreeMouse extends MouseAdapter {
        private Container popupMenu;

        // private final Vector<ActionItem> extraPlanActions = new Vector<ActionItem>();

        // /**
        // * Item to be added to the menu in case selected item is a local plan.
        // *
        // * @param item
        // */
        // public void addPlanMenuItem(ActionItem item) {
        // extraPlanActions.add(item);
        // }

        /**
         * 
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

            // StringBuilder objectNames = new StringBuilder();
            // for (NameId nameId : selectedItems) {
            // objectNames.append(nameId.getDisplayName());
            // objectNames.append(" ");
            // }
            // return objectNames;
        }

        // @Override
        // public String toString() {
        // Iterator<E> it = iterator();
        // if (!it.hasNext())
        // return "[]";
        //
        // StringBuilder sb = new StringBuilder();
        // sb.append('[');
        // for (;;) {
        // E e = it.next();
        // sb.append(e == this ? "(this Collection)" : e);
        // if (!it.hasNext())
        // return sb.append(']').toString();
        // sb.append(',').append(' ');
        // }
        // }

        /**
         * @param console2
         * @param selection
         * @param popupMenu
         */
        private void addActionRemovePlanLocally(final ConsoleLayout console2, final ArrayList<NameId> selectedItems,
                JPopupMenu popupMenu) {
            final StringBuilder itemsInString = getPlanNamesString(selectedItems, true);
            popupMenu.add(I18n.textf("Delete %planName locally", itemsInString)).addActionListener(
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
                                // if (console2 != null)
                                console2.setPlan(null);
                                console2.getMission().save(false);
                            }
                            // }
                        }
                    });
        }

        private void addActionGetRemotePlan(final ConsoleLayout console2, final PlanDBControl pdbControl,
                final ArrayList<NameId> remotePlans, JPopupMenu popupMenu) {
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
            StringBuilder itemsInString = getPlanNamesString(remoteTrans, false);
            popupMenu.add(I18n.textf("Get %planName from %system", itemsInString, console2.getMainSystem()))
                    .addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            // if (selection != null) {
                            // pdbControl.setRemoteSystemId(console2.getMainSystem());
                           // for (NameId nameId : remoteTrans) {
                                // Signal ids to merge
                                browser.addTransToMerge(remoteTrans);
                                // Request LBLConfig
                                LblConfig msgLBLConfiguration = new LblConfig();
                                msgLBLConfiguration.setOp(LblConfig.OP.GET_CFG);
                                sendMsg(msgLBLConfiguration);
                            //}
                            // }
                        }
                    });
        }

        private void addActionRemovePlanRemotely(final ConsoleLayout console2, final PlanDBControl pdbControl,
                final ArrayList<NameId> synAndUnsyncPlans, JPopupMenu popupMenu) {
            popupMenu.add(
		    I18n.textf("Remove '%planName' from %system", getPlanNamesString(synAndUnsyncPlans, true),
                            console2.getMainSystem())).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // if (selection != null) {
                    // PlanType sel = (PlanType) selection;
                    pdbControl.setRemoteSystemId(console2.getMainSystem());
                    for (NameId nameId : synAndUnsyncPlans) {
                        pdbControl.deletePlan(nameId.getIdentification());
                        }
                    // }
                }
            });
        }

        // private void addActionShare(final ArrayList<NameId> selectedItems, JMenu dissemination,
        // final String objectTypeName) {
        // StringBuilder itemsInString = getItemsInString(selectedItems);
        // dissemination.add(I18n.textf("Share '%transponderName'", itemsInString)).addActionListener(
        // new ActionListener() {
        // @Override
        // public void actionPerformed(ActionEvent e) {
        // for (NameId nameId : selectedItems) {
        // ImcMsgManager.disseminate((XmlOutputMethods) nameId, objectTypeName);
        // }
        // }
        // });
        // }

        private void addActionRenamePlan(final ConsoleLayout console2, final ArrayList<NameId> selectedItems,
                JPopupMenu popupMenu) {
            popupMenu.add(
            I18n.textf("Rename %planName", getPlanNamesString(selectedItems, true), console2.getMainSystem()))
            .addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    String newName = null;
                    while (true) {
                        String oldPlanName = getPlanNamesString(selectedItems, true).toString();
                        newName = JOptionPane.showInputDialog("New plan name:", oldPlanName);
                        if (newName == null)
                            return;

                        if (!getConsole().getMission().getIndividualPlansList().containsKey(newName)) {
                            if (!newName.isEmpty()) {
                                PlanType plan = getConsole().getMission().getIndividualPlansList()
                                        .get(oldPlanName);
                                if (plan != null) {
                                    plan.setMissionType(getConsole().getMission());

                                    getConsole().getMission().renamePlan(plan, newName, true);
                                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                                        @Override
                                        protected Void doInBackground() throws Exception {
                                            getConsole().getMission().save(true);
                                            return null;
                                        }
                                    };
                                    worker.execute();
                                    browser.refreshBrowser(getConsole().getMission(),
                                            getMainVehicleId(), getConsole());
                                    return;
                                }
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
            // JMenu dissemination = new JMenu(I18n.text("Dissemination"));
            // addActionAddNewTrans(popupMenu);
            ArrayList<NameId> toShare = new ArrayList<NameId>();
            ItemTypes selecType = findSelecMissionElem(selectedItems);
            switch (selecType) {
                case Plans:

                    if (selectedItems.size() == 1)
                        addActionRenamePlan(getConsole(), selectedItems, popupMenu);

                    popupMenu.addSeparator();
                    // ArrayList<NameId> synAndUnsyncPlans = new ArrayList<NameId>();
                    // ArrayList<NameId> remotePlans = new ArrayList<NameId>();
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
                                    break;
                            }
                            // if (syncState == State.REMOTE)
                            // remotePlans.add((NameId) extendedTreeNode.getUserObject());
                            // else
                            // synAndUnsyncPlans.add((NameId) extendedTreeNode.getUserObject());
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

                    // if (synAndUnsyncPlans.size()>0) {
                    // addActionRemovePlanRemotely(getConsole(), pdbControl, synAndUnsyncPlans, popupMenu);
                    // addActionGetRemotePlan(getConsole(), pdbControl, synAndUnsyncPlans, popupMenu);
                    // addActionShare(selectedItems, dissemination);
                    // // addActionChangePlanVehicles(selection, popupMenu); // Uncomment when multiple vehicles needs
                    // // this
                    // // ActionItem actionItem;
                    // // for (int a = 0; a < extraPlanActions.size(); a++) {
                    // // actionItem = extraPlanActions.get(a);
                    // // popupMenu.add(I18n.text(actionItem.label)).addActionListener(actionItem.action);
                    // // }
                    // }
                    // if (remotePlans.size() > 0) {
                    // addActionGetRemotePlan(getConsole(), pdbControl, remotePlans, popupMenu);
                    // addActionRemovePlanRemotely(getConsole(), pdbControl, remotePlans, popupMenu);
                    // }
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
            if (toShare.size() > 0)
                addActionShare(toShare, popupMenu);
            // popupMenu.add(dissemination);
            if (Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null) != null) {
                addActionPasteUrl(popupMenu);
            }
            addActionReloadPanel(popupMenu);
            popupMenu.show((Component) e.getSource(), e.getX(), e.getY());
        }

        private void addActionRemoveAllTrans(JPopupMenu popupMenu) {
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
                // else if(nameId instanceof PlanDBInfo){
                // if (type == ItemTypes.RemotePlan || type == ItemTypes.None)
                // type = ItemTypes.RemotePlan;
                // else
                // type = ItemTypes.Mix;
                // }
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

        // private void addActionRemoveSelectedPlans(final ArrayList<NameId> selectedItems, final Object selection,
        // JPopupMenu popupMenu) {
        // popupMenu.add(I18n.text("Delete selected plans locally")).addActionListener(new ActionListener() {
        // @Override
        // public void actionPerformed(ActionEvent e) {
        // if (selection != null) {
        // int resp = JOptionPane.showConfirmDialog(console,
        // I18n.textf("Delete all selected plans (%numberOfPlans) locally?", selectedItems.size()));
        //
        // if (resp == JOptionPane.YES_OPTION) {
        // TreeMap<String, PlanType> individualPlansList = console.getMission()
        // .getIndividualPlansList();
        // for (Object o : selectedItems) {
        // NameId sel = (NameId) o;
        // PlanType res = individualPlansList.remove(sel.getIdentification());
        // }
        // console.getMission().save(false);
        //
        // if (console != null)
        // console.setPlan(null);
        // browser.refreshBrowser(getConsole().getMission(),
        // getMainVehicleId());
        // }
        // }
        // }
        // });
        // }

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
                                    browser.swithLocationsTransponder(selection,
 tel, getConsole());
                                };
                            }.start();
                        }
                    });
        }

        private void addActionRemoveTrans(final ArrayList<TransponderElement> selectedTrans, JPopupMenu popupMenu) {
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
            popupMenu.add(I18n.textf("View/Edit %transponderName", selection.getDisplayName())).addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            browser.editTransponder(selection, getConsole().getMission(),
                                    getMainVehicleId());
                        }
                    });
        }

        // Redo when multiple vehicles needs this
        // private void addActionChangePlanVehicles(final Object selection, JPopupMenu popupMenu) {
        // popupMenu.add(I18n.text("Change plan vehicles")).addActionListener(new ActionListener() {
        // @Override
        // public void actionPerformed(ActionEvent e) {
        // if (selection != null) {
        // PlanType sel = (PlanType) selection;
        //
        // String[] vehicles = VehicleSelectionDialog.showSelectionDialog(getConsole(), sel.getVehicles()
        // .toArray(new VehicleType[0]));
        // Vector<VehicleType> vts = new Vector<VehicleType>();
        // for (String v : vehicles) {
        // vts.add(VehiclesHolder.getVehicleById(v));
        // }
        // sel.setVehicles(vts);
        // }
        // }
        // });
        // }

        private void addActionShare(final ArrayList<NameId> selectedItems, JPopupMenu popupMenu2) {
            StringBuilder itemsInString = getPlanNamesString(selectedItems, true);
            popupMenu2.add(I18n.textf("Share %planName", itemsInString)).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // disseminate((XmlOutputMethods) selection, "Plan");
                    for (NameId nameId : selectedItems) {
                        getConsole().getImcMsgManager().broadcastToCCUs(((PlanType) nameId).asIMCPlan());
                    }
                }
            });
        }

        private void addActionAddNewTrans(JPopupMenu popupMenu) {
            popupMenu.add(I18n.text("Add a new transponder")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    browser.addTransponderElement(getConsole());
                }
            });
        }

        private void addActionPasteUrl(JPopupMenu popupMenu2) {
            popupMenu2.add(I18n.text("Paste URL")).addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    if (browser.setContent(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null),
                            getConsole().getMission())) {
                        getConsole().updateMissionListeners();
                    }
                }
            });
        }
    }

    /**
     * Class with items to add to a JMenu.
     * 
     * @author Margarida
     * 
     */
    class ActionItem {
        /**
         * The label appering in the menu. This is the string without translation.
         */
        public String label;
        /**
         * The listner associated with the menu item.
         */
        public ActionListener action;
    }

    private enum ItemTypes {
        Plans,
        HomeRef,
        Transponder,
        Mix,
        None;
    }
}
