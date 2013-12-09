/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * 2009/09/22
 */
package pt.lsts.neptus.plugins.planning;

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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
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
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.console.plugins.IPlanSelection;
import pt.lsts.neptus.console.plugins.ITransponderSelection;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.gui.LocationPanel;
import pt.lsts.neptus.gui.MissionBrowser;
import pt.lsts.neptus.gui.MissionBrowser.State;
import pt.lsts.neptus.gui.MissionTreeModel.NodeInfoKey;
import pt.lsts.neptus.gui.VehicleSelectionDialog;
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
import pt.lsts.neptus.plugins.SimpleSubPanel;
import pt.lsts.neptus.plugins.planning.plandb.PlanDBAdapter;
import pt.lsts.neptus.plugins.planning.plandb.PlanDBControl;
import pt.lsts.neptus.plugins.planning.plandb.PlanDBInfo;
import pt.lsts.neptus.plugins.planning.plandb.PlanDBState;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.NameId;
import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.HomeReferenceElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.mission.HomeReference;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ByteUtil;

import com.google.common.eventbus.Subscribe;


/**
 * Panel that holds mission objects namely plans and accustic beacons.
 * 
 * @author ZP
 * @author pdias
 * @author Margarida
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Mission Tree", author = "José Pinto, Paulo Dias, Margarida Faria", icon = "pt/up/fe/dceg/neptus/plugins/planning/mission_tree.png", category = CATEGORY.PLANNING, version = "1.5.0")
public class MissionTreePanel extends SimpleSubPanel implements MissionChangeListener, MainVehicleChangeListener,
        DropTargetListener, NeptusMessageListener, IPlanSelection, IPeriodicUpdates, ConfigurationListener,
        ITransponderSelection {

    @NeptusProperty(name = "Use Plan DB Sync. Features", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    public boolean usePlanDBSyncFeatures = true;
    @NeptusProperty(name = "Use Plan DB Sync. Features Extended", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER, description = "Needs 'Use Plan DB Sync. Features' on")
    public boolean usePlanDBSyncFeaturesExt = false;
    @NeptusProperty(name = "Debug", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    public boolean debugOn = false;
    @NeptusProperty(name = "Acceptable Elapsed Time", description = "Maximum acceptable interval between beacon ranges, in seconds.")
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
    protected PlanDBAdapter planDBListener = new PlanDBAdapter() {
        // Called only if Type == SUCCESS in received PlanDB message
        @Override
        public void dbCleared() {
        }

        @Override
        public void dbInfoUpdated(PlanDBState updatedInfo) {
            // PlanDB Operation = GET_STATE
            // Get state of the entire DB. Successful replies will yield a
            // 'PlanDBState' message in the 'arg' field but without
            // individual plan information (in the 'plans_info' field of
            // 'PlanDBState').
            TreePath[] selectedNodes = browser.getSelectedNodes();

            // browser.transUpdateElapsedTime(); //TODO

            TreeMap<String, PlanType> localPlans;
            try {
                localPlans = getConsole().getMission().getIndividualPlansList();
            }
            catch (NullPointerException e) {
                NeptusLog.pub().warn("I cannot find local plans for " + getMainVehicleId());
                localPlans = new TreeMap<String, PlanType>();
            }
            browser.updatePlansStateEDT(localPlans, getMainVehicleId());
            browser.setSelectedNodes(selectedNodes);
            // System.out.println("dbInfoUpdated");
        }

        @Override
        public void dbPlanReceived(PlanType spec) {
            // PlanDB Operation = GET
            // Get a plan stored in the DB.The 'plan_id' field identifies
            // the plan. Successful replies will yield a
            // 'PlanSpecification' message in the 'arg' field.
            // Update when received remote plan into our system
            // Put plan in mission
            PlanType lp = getConsole().getMission().getIndividualPlansList().get(spec.getId());
            spec.setMissionType(getConsole().getMission());
            getConsole().getMission().addPlan(spec);
            // Save mission
            getConsole().getMission().save(true);
            // Alert listeners
            getConsole().updateMissionListeners();


            if (debugOn && lp != null) {
                try {
                    IMCMessage p1 = lp.asIMCPlan(), p2 = spec.asIMCPlan();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    IMCOutputStream imcOs = new IMCOutputStream(baos);

                    ByteUtil.dumpAsHex(p1.payloadMD5(), System.out);
                    ByteUtil.dumpAsHex(p2.payloadMD5(), System.out);

                    // NeptusLog.pub().info("<###> "+IMCUtil.getAsHtml(p1));
                    // NeptusLog.pub().info("<###> "+IMCUtil.getAsHtml(p2));

                    p1.serialize(imcOs);
                    ByteUtil.dumpAsHex(baos.toByteArray(), System.out);

                    baos = new ByteArrayOutputStream();
                    imcOs = new IMCOutputStream(baos);
                    p2.serialize(imcOs);
                    ByteUtil.dumpAsHex(baos.toByteArray(), System.out);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("dbPlanReceived");
        }

        @Override
        public void dbPlanRemoved(String planId) {
            // PlanDB Operation = DEL
            // Delete a plan from the DB. The 'plan_id' field identifies
            // the plan to delete.
            browser.removeCurrSelectedNodeRemotely();
            System.out.println("dbPlanRemoved");
        }

        @Override
        public void dbPlanSent(String planId) {
            // PlanDB Operation = SET message.
            // Set a plan in the DB. The 'plan_id' field identifies the
            // plan, and a pre-existing plan with the same identifier, if
            // any will be overwritten. For requests, the 'arg' field must
            // contain a 'PlanSpecification' message.
            System.out.println("dbPlanSent");
            // sent plan confirmation, is now in sync
            browser.setPlanAsSync(planId);
        }
    };

    public MissionTreePanel(ConsoleLayout console) {
        super(console);
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

    public void addPlanMenuItem(ActionItem item) {
        mouseAdapter.addPlanMenuItem(item);
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
        browser.refreshBrowser(getConsole().getMission(), getMainVehicleId());
    }

    @Override
    public void missionUpdated(MissionType mission) {
        // it is called (among others) when the specs for a remote plan have just been received
        browser.refreshBrowser(getConsole().getMission(), getMainVehicleId());
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
        browser.refreshBrowser(getConsole().getMission(), getMainVehicleId());
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

    @Override
    public void mainVehicleChangeNotification(String id) {
        running = false;
        updatePlanDBListener(id);
        askForBeaconConfig();
        browser.refreshBrowser(getConsole().getMission(), getMainVehicleId());
    }

    /**
     * Ask vehicle for configurations of vehicles in use.
     */
    private void askForBeaconConfig() {
        SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    LblConfig msgLBLConfiguration = new LblConfig();
                    msgLBLConfiguration.setOp(LblConfig.OP.GET_CFG);
                    IMCSendMessageUtils.sendMessage(msgLBLConfiguration,
                            I18n.text("Could not ask " + getMainVehicleId() + " for it's accoustic beacons."),
                            getMainVehicleId());
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
        final Object[] multiSel = browser.getSelectedItems();
        Vector<PlanType> plans = new Vector<PlanType>();
        if (multiSel != null) {
            for (Object o : multiSel) {
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
        final TreePath[] multiSel = browser.getSelectedNodes();
        ArrayList<TransponderElement> trans = new ArrayList<>();
        if (multiSel != null) {
            for (TreePath path : multiSel) {
                ExtendedTreeNode node = ((ExtendedTreeNode) path.getLastPathComponent());
                Object userObject = node.getUserObject();

                if (userObject instanceof TransponderElement
                        && node.getUserInfo().get(NodeInfoKey.SYNC) != State.REMOTE)
                    trans.add((TransponderElement) userObject);
            }
        }
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
                    browser.updateTransStateEDT(getConsole().getMission(), getMainVehicleId(), (Vector<LblBeacon>) lblConfig.getBeacons().clone());
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
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                browser.setDebugOn(debugOn);
                browser.setMaxAcceptableElapsedTime(maxAcceptableElapsedTime);
            }
        });
    }

    class MissionTreeMouse extends MouseAdapter {
        private Container popupMenu;
        private final Vector<ActionItem> extraPlanActions = new Vector<ActionItem>();

        /**
         * Item to be added to the menu in case selected item is a local plan.
         * 
         * @param item
         */
        public void addPlanMenuItem(ActionItem item) {
            extraPlanActions.add(item);
        }

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
                final Object selection, JPopupMenu popupMenu) {
            popupMenu.add(I18n.textf("Send '%planName' to %system", selection, console2.getMainSystem()))
                    .addActionListener(

                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (selection != null) {
                                PlanType sel = (PlanType) selection;
                                if (pdbControl == null)
                                    System.out.println("Pdb null");
                                String mainSystem = console2.getMainSystem();
                                pdbControl.setRemoteSystemId(mainSystem);
                                pdbControl.sendPlan(sel);
                            }
                        }
                    });
        }

        /**
         * @param console2
         * @param selection
         * @param popupMenu
         */
        private <T extends NameId> void addActionRemovePlanLocally(final ConsoleLayout console2,
                final T selection, JPopupMenu popupMenu) {

            popupMenu.add(I18n.textf("Delete '%planName' locally", selection)).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (selection != null) {
                        int resp = JOptionPane.showConfirmDialog(console2,
                                I18n.textf("Remove the plan '%planName'?", selection.toString()));
                        if (resp == JOptionPane.YES_OPTION) {
                            console2.getMission().getIndividualPlansList().remove(selection.getIdentification());
                            console2.getMission().save(false);

                            if (console2 != null)
                                console2.setPlan(null);
                            browser.deleteCurrSelectedNodeLocally();
                        }
                    }
                }
            });
        }

        private <T> void addActionGetRemotePlan(final ConsoleLayout console2, final PlanDBControl pdbControl,
                final T selection, JPopupMenu popupMenu) {
            popupMenu.add(I18n.textf("Get '%planName' from %system", selection, console2.getMainSystem()))
                    .addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (selection != null) {
                                // pdbControl.setRemoteSystemId(console2.getMainSystem());
                                pdbControl.requestPlan(((NameId) selection).getIdentification());
                            }
                        }
                    });
        }

        private <T extends NameId> void addActionRemovePlanRemotely(final ConsoleLayout console2,
                final PlanDBControl pdbControl, final T selection, JPopupMenu popupMenu) {
            popupMenu.add(I18n.textf("Remove '%planName' from %system", selection, console2.getMainSystem()))
                    .addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (selection != null) {
                                // PlanType sel = (PlanType) selection;
                                pdbControl.setRemoteSystemId(console2.getMainSystem());
                                pdbControl.deletePlan(((NameId) selection).getIdentification());
                            }
                        }

                    });
        }

        private <T extends NameId> void addActionShare(final T selection, JMenu dissemination,
                final String objectTypeName) {
            dissemination.add(I18n.textf("Share '%transponderName'", selection.getIdentification())).addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            ImcMsgManager.disseminate((XmlOutputMethods) selection, objectTypeName);
                        }
                    });
        }

        @Override
        public void mousePressed(MouseEvent e) {
            final Object[] multiSel = browser.getSelectedItems();
            if (e.getButton() != MouseEvent.BUTTON3)
                return;

            int plansCount = 0;
            if (multiSel != null){
                for (Object o : multiSel) {
                    if (o instanceof PlanType) {
                        plansCount++;
                    }
                }
            }
            if (multiSel == null || multiSel.length <= 1) {
                browser.setMultiSelect(e);
            }
            final Object selection = browser.getSelectedItem();
            DefaultMutableTreeNode selectionNode = browser.getSelectedTreeNode();
            JPopupMenu popupMenu = new JPopupMenu();
            JMenu dissemination = new JMenu(I18n.text("Dissemination"));
            if (Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null) != null) {
                addActionPasteUrl(dissemination);
                dissemination.addSeparator();
            }
            if (selection == null) {
                popupMenu.addSeparator();
                addActionAddNewTrans(popupMenu);
            }
            else if (selection instanceof PlanType) {
                popupMenu.addSeparator();
                addActionSendPlan(console, pdbControl, selection, popupMenu);
                addActionRemovePlanLocally(console, (NameId) selection, popupMenu);
                State syncState = (State) ((ExtendedTreeNode) selectionNode).getUserInfo().get(NodeInfoKey.SYNC.name());
                if (syncState == null)
                    syncState = State.LOCAL;
                else if (syncState == State.SYNC || syncState == State.NOT_SYNC) {
                    addActionRemovePlanRemotely(console, pdbControl, (NameId) selection, popupMenu);
                    addActionGetRemotePlan(console, pdbControl, selection, popupMenu);
                }
                addActionShare(selection, dissemination);
                addActionChangePlanVehicles(selection, popupMenu);
                ActionItem actionItem;
                for (int a = 0; a < extraPlanActions.size(); a++) {
                    actionItem = extraPlanActions.get(a);
                    popupMenu.add(I18n.text(actionItem.label)).addActionListener(actionItem.action);
                }
            }
            else if (selection instanceof PlanDBInfo) {
                State syncState = selectionNode instanceof ExtendedTreeNode ? (State) ((ExtendedTreeNode) selectionNode)
                        .getUserInfo().get(NodeInfoKey.SYNC.name()) : null;
                if (syncState == null)
                    syncState = State.LOCAL;
                else if (syncState == State.REMOTE) {
                    addActionGetRemotePlan(console, pdbControl, selection, popupMenu);
                    addActionRemovePlanRemotely(console, pdbControl, (NameId) selection, popupMenu);
                }
            }
            else if (selection instanceof TransponderElement) {
                TransponderElement transSel = (TransponderElement) selection;
                popupMenu.addSeparator();
                addActionEditTrans(transSel, popupMenu);
                Object state = ((ExtendedTreeNode) selectionNode).getUserInfo().get(NodeInfoKey.SYNC.name());
                if (state == State.LOCAL) {
                    addActionRemoveTrans(transSel, popupMenu);
                }
                Vector<TransponderElement> allTransponderElements = MapGroup.getMapGroupInstance(console.getMission())
                        .getAllObjectsOfType(TransponderElement.class);
                for (final TransponderElement tempTrans : allTransponderElements) {
                    if (!transSel.getDisplayName().equals(tempTrans.getDisplayName())) {
                        addActionSwitchTrans(transSel, popupMenu, tempTrans);
                    }
                }
                addActionShare((NameId) selection, dissemination, "Transponder");
                addActionAddNewTrans(popupMenu);
            }
            else if (selection instanceof HomeReference) {
                addActionEditHomeRef(selection, popupMenu);
            }
            if (plansCount > 1) {
                popupMenu.addSeparator();
                addActionRemoveSelectedPlans(multiSel, selection, popupMenu);
            }
            popupMenu.addSeparator();
            addActionReloadPanel(popupMenu);
            popupMenu.add(dissemination);
            popupMenu.show((Component) e.getSource(), e.getX(), e.getY());
        }

        private void addActionReloadPanel(JPopupMenu popupMenu) {
            popupMenu.add(I18n.text("Reload Panel")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    browser.refreshBrowser(getConsole().getMission(), getMainVehicleId());
                }
            });
        }

        private void addActionRemoveSelectedPlans(final Object[] multiSel, final Object selection, JPopupMenu popupMenu) {
            popupMenu.add(I18n.text("Remove selected plans")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (selection != null) {
                        int resp = JOptionPane.showConfirmDialog(console,
                                I18n.textf("Remove all selected plans (%numberOfPlans)?", multiSel.length));

                        if (resp == JOptionPane.YES_OPTION) {
                            TreeMap<String, PlanType> individualPlansList = console.getMission()
                                    .getIndividualPlansList();
                            for (Object o : multiSel) {
                                NameId sel = (NameId) o;
                                PlanType res = individualPlansList.remove(sel.getIdentification());
                            }
                            console.getMission().save(false);

                            if (console != null)
                                console.setPlan(null);
                            browser.refreshBrowser(getConsole().getMission(),
                                    getMainVehicleId());
                        }
                    }
                }
            });
        }

        private void addActionEditHomeRef(final Object selection, JPopupMenu popupMenu) {
            popupMenu.add(I18n.text("View/Edit home reference")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    LocationType loc = new LocationType((HomeReference) selection);
                    LocationType after = LocationPanel.showLocationDialog(console, I18n.text("Set home reference"),
                            loc, console.getMission(), true);
                    if (after == null)
                        return;

                    console.getMission().getHomeRef().setLocation(after);

                    Vector<HomeReferenceElement> hrefElems = MapGroup.getMapGroupInstance(console.getMission())
                            .getAllObjectsOfType(HomeReferenceElement.class);
                    hrefElems.get(0).setCoordinateSystem(console.getMission().getHomeRef());
                    console.getMission().save(false);
                    console.updateMissionListeners();
                }
            });
        }

        private void addActionSwitchTrans(final TransponderElement selection, JPopupMenu popupMenu,
                final TransponderElement tel) {
            popupMenu.add(
                    I18n.textf("Switch '%transponderName1' with '%transponderName2'", selection.getDisplayName(),
                            tel.getDisplayName()))
                    .addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            new Thread() {
                                @Override
                                public void run() {
                                    browser.swithLocationsTransponder(selection,
                                            tel, console);
                                };
                            }.start();
                        }
                    });
        }

        private void addActionRemoveTrans(final TransponderElement selection, JPopupMenu popupMenu) {
            popupMenu.add(I18n.textf("Remove '%transponderName'", selection.getDisplayName())).addActionListener(
                    new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                            browser.removeTransponder(selection, console);
                }
            });
        }

        private void addActionEditTrans(final TransponderElement selection, JPopupMenu popupMenu) {
            popupMenu.add(I18n.textf("View/Edit '%transponderName'", selection.getDisplayName())).addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            browser.editTransponder(selection, console.getMission(),
                                    getMainVehicleId());
                        }
                    });
        }

        private void addActionChangePlanVehicles(final Object selection, JPopupMenu popupMenu) {
            popupMenu.add(I18n.text("Change plan vehicles")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (selection != null) {
                        PlanType sel = (PlanType) selection;

                        String[] vehicles = VehicleSelectionDialog.showSelectionDialog(console, sel.getVehicles()
                                .toArray(new VehicleType[0]));
                        Vector<VehicleType> vts = new Vector<VehicleType>();
                        for (String v : vehicles) {
                            vts.add(VehiclesHolder.getVehicleById(v));
                        }
                        sel.setVehicles(vts);
                    }
                }
            });
        }

        private void addActionShare(final Object selection, JMenu dissemination) {
            dissemination.add(I18n.textf("Share '%planName'", selection)).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // disseminate((XmlOutputMethods) selection, "Plan");
                    console.getImcMsgManager().broadcastToCCUs(((PlanType) selection).asIMCPlan());
                }
            });
        }

        private void addActionAddNewTrans(JPopupMenu popupMenu) {
            popupMenu.add(I18n.text("Add a new transponder")).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    browser.addTransponderElement(console);
                }
            });
        }

        private void addActionPasteUrl(JMenu dissemination) {
            dissemination.add(I18n.text("Paste URL")).addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    if (browser.setContent(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null),
                            console.getMission())) {
                        console.updateMissionListeners();
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
}
