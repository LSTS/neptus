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
package pt.up.fe.dceg.neptus.plugins.planning;

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
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventPlanChange;
import pt.up.fe.dceg.neptus.console.plugins.IPlanSelection;
import pt.up.fe.dceg.neptus.console.plugins.ISystemsSelection;
import pt.up.fe.dceg.neptus.console.plugins.ITransponderSelection;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.MissionChangeListener;
import pt.up.fe.dceg.neptus.gui.LocationPanel;
import pt.up.fe.dceg.neptus.gui.MissionBrowser;
import pt.up.fe.dceg.neptus.gui.MissionBrowser.State;
import pt.up.fe.dceg.neptus.gui.MissionTreeModel.NodeInfoKey;
import pt.up.fe.dceg.neptus.gui.VehicleSelectionDialog;
import pt.up.fe.dceg.neptus.gui.tree.ExtendedTreeNode;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.IMCOutputStream;
import pt.up.fe.dceg.neptus.imc.LblBeacon;
import pt.up.fe.dceg.neptus.imc.LblConfig;
import pt.up.fe.dceg.neptus.imc.LblConfig.OP;
import pt.up.fe.dceg.neptus.imc.LblRangeAcceptance;
import pt.up.fe.dceg.neptus.imc.PlanControlState;
import pt.up.fe.dceg.neptus.imc.PlanControlState.STATE;
import pt.up.fe.dceg.neptus.imc.PlanSpecification;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.LEVEL;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginDescription.CATEGORY;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBAdapter;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBControl;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBInfo;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBState;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.types.Identifiable;
import pt.up.fe.dceg.neptus.types.XmlOutputMethods;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.HomeReferenceElement;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.mission.HomeReference;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.ByteUtil;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

import com.google.common.eventbus.Subscribe;


/**
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
            System.out.println("dbInfoUpdated");
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

    @Override
    public void initSubPanel() {
        if (inited)
            return;
        inited = true;
        updatePlanDBListener(getMainVehicleId());

        browser.refreshBrowser(getConsole().getMission(), getMainVehicleId());

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
        // else {
        // messages[3] = "MissionSpecification";
        // }
        // messages[4] = "PlanDB";
        return messages;
    }

    @Override
    public void mainVehicleChangeNotification(String id) {
        // browser.transStopTimers();
        running = false;
        updatePlanDBListener(id);
        browser.refreshBrowser(getConsole().getMission(), getMainVehicleId());
    }

    /**
     * Remove current PlanDB listener. Fetch the running PlanDB listener from IMCSystemsHolder or create a new one if
     * none is found. Set the designeted PlanDB listener.
     * 
     * @param id of the main vehicle.
     */
    private void updatePlanDBListener(String id) {
        removePlanDBListener();
        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(id);

        // pdbControl = sys.getPlanDBControl();
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
            System.out.println("Pdb set");
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

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.console.plugins.ITransponderSelection#getSelectedTransponders()
     */
    // @Override
    // public Collection<TransponderElement> getSelectedTransponders() {
    // final Object[] multiSel = browser.getSelectedItems();
    // ArrayList<TransponderElement> trans = new ArrayList<>();
    // if (multiSel != null) {
    // for (Object o : multiSel) {
    // if (o instanceof TransponderElement)
    // trans.add((TransponderElement) o);
    // }
    // }
    // return trans;
    // }
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

    @Override
    public void messageArrived(IMCMessage message) {
        int mgid = message.getMgid();
        // pdbControl.onMessage(null, message);
        switch (mgid) {
            // Plan state and list management
            case PlanSpecification.ID_STATIC:
                PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(), message);
                if (getConsole().getMission().getIndividualPlansList().containsKey(plan.getId())) {
                }
                else {
                    getConsole().getMission().getIndividualPlansList().put(plan.getId(), plan);
                    getConsole().updateMissionListeners();
                    getConsole().getMission().save(true);
                }
                break;
            // Timer management
            case PlanControlState.ID_STATIC:
                PlanControlState planState = (PlanControlState) message;
                if (planState.getState() == STATE.READY || planState.getState() == STATE.BLOCKED) {
                    if (running) {
                        browser.transStopTimers();
                        this.running = false;
                    }
                }
                else if (!running) {
                    browser.transStartVehicleTimers(getMainVehicleId());
                    this.running = true;
                }

                break;
            // Timer management
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
            // Beacons list and state management
            case LblConfig.ID_STATIC:
                LblConfig lblConfig = (LblConfig) message;
                if (((LblConfig) message).getOp() == OP.CUR_CFG) {
                    @SuppressWarnings("unchecked")
                    final Vector<LblBeacon> beacons = (Vector<LblBeacon>) lblConfig.getBeacons().clone();
                    // browser.transSyncConfig(beacons, getMainVehicleId());
                    LinkedHashMap<String, LblBeacon> remoteTrans = new LinkedHashMap<String, LblBeacon>();
                    for (LblBeacon lblBeacon : beacons) {
                        remoteTrans.put(lblBeacon.getBeacon(), lblBeacon);
                    }
                    browser.updateTransStateEDT(getConsole().getMission(), getMainVehicleId(), remoteTrans);
                }
                break;

            default:
                NeptusLog.pub().error("Unknown message " + mgid);
                break;
        }
    }

    @Override
    public long millisBetweenUpdates() {
        return 1500;
    }

    @Override
    public boolean update() {

        return true;
    }

    @Subscribe
    public void on(ConsoleEventPlanChange event) {
        // removed runnable
        browser.setSelectedPlan(event.getCurrent());
    }

    private ImcSystem[] convertToImcSystemsArray(Vector<ISystemsSelection> sys) {
        Collection<String> asys = sys.firstElement().getAvailableSelectedSystems();
        Vector<ImcSystem> imcSystems = new Vector<ImcSystem>();
        for (String id : asys) {
            ImcSystem tsys = ImcSystemsHolder.lookupSystemByName(id);
            if (tsys != null) {
                if (!imcSystems.contains(tsys))
                    imcSystems.add(tsys);
            }
        }
        ImcSystem[] imcSystemsArray = imcSystems.toArray(new ImcSystem[imcSystems.size()]);
        return imcSystemsArray;
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
        private <T extends Identifiable> void addActionRemovePlanLocally(final ConsoleLayout console2,
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
                                pdbControl.requestPlan(((Identifiable) selection).getIdentification());
                            }
                        }
                    });
        }

        private <T extends Identifiable> void addActionRemovePlanRemotely(final ConsoleLayout console2,
                final PlanDBControl pdbControl, final T selection, JPopupMenu popupMenu) {
            popupMenu.add(I18n.textf("Remove '%planName' from %system", selection, console2.getMainSystem()))
                    .addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (selection != null) {
                                // PlanType sel = (PlanType) selection;
                                pdbControl.setRemoteSystemId(console2.getMainSystem());
                                pdbControl.deletePlan(((Identifiable) selection).getIdentification());
                            }
                        }

                    });
        }

        private <T extends Identifiable> void addActionShare(final T selection, JMenu dissemination,
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
                // if (plansCount == 1) {
                popupMenu.addSeparator();
                addActionSendPlan(console, pdbControl, selection, popupMenu);
                addActionRemovePlanLocally(console, (Identifiable) selection, popupMenu);
                State syncState = (State) ((ExtendedTreeNode) selectionNode).getUserInfo().get(NodeInfoKey.SYNC.name());
                if (syncState == null)
                    syncState = State.LOCAL;
                else if (syncState == State.SYNC || syncState == State.NOT_SYNC) {
                    addActionRemovePlanRemotely(console, pdbControl, (Identifiable) selection, popupMenu);
                    addActionGetRemotePlan(console, pdbControl, selection, popupMenu);
                }
                addActionShare(selection, dissemination);
                addActionChangePlanVehicles(selection, popupMenu);
                // }
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
                    addActionRemovePlanRemotely(console, pdbControl, (Identifiable) selection, popupMenu);
                    // popupMenu.add(
                    // I18n.textf("bug Remove '%planName' from %system", selection, console2.getMainSystem()))
                    // .addActionListener(new ActionListener() {
                    // @Override
                    // public void actionPerformed(ActionEvent e) {
                    // if (selection != null) {
                    // pdbControl.setRemoteSystemId(console2.getMainSystem());
                    // PlanDBInfo sel = (PlanDBInfo) selection;
                    // pdbControl.deletePlan(sel.getPlanId());
                    // }
                    // }
                    // });
                }
            }
            else if (selection instanceof TransponderElement) {
                popupMenu.addSeparator();
                addActionEditTrans(selection, popupMenu);
                addActionRemoveTrans(selection, popupMenu);
                Vector<TransponderElement> allTransponderElements = MapGroup.getMapGroupInstance(console.getMission())
                        .getAllObjectsOfType(TransponderElement.class);
                for (final AbstractElement tel : allTransponderElements) {
                    if ((TransponderElement) selection != (TransponderElement) tel) {
                        addActionSwitchTrans(selection, popupMenu, tel);
                    }
                }
                addActionShare((Identifiable) selection, dissemination, "Transponder");
                addActionAddNewTrans(popupMenu);
            }
            //
            // else if (selection instanceof MarkElement) {
            //
            // popupMenu.addSeparator();
            //
            // dissemination.add(I18n.text("Share startup position")).addActionListener(new ActionListener() {
            // @Override
            // public void actionPerformed(ActionEvent e) {
            // ImcMsgManager.disseminate((XmlOutputMethods) selection, "StartLocation");
            // }
            // });
            // }
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
                            for (Object o : multiSel) {
                                PlanType sel = (PlanType) o;
                                console.getMission().getIndividualPlansList().remove(sel.getId());
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

        private void addActionSwitchTrans(final Object selection, JPopupMenu popupMenu, final AbstractElement tel) {
            popupMenu.add(I18n.textf("Switch '%transponderName1' with '%transponderName2'", selection, tel))
                    .addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            new Thread() {
                                @Override
                                public void run() {
                                    browser.swithLocationsTransponder((TransponderElement) selection,
                                            (TransponderElement) tel, console);
                                };
                            }.start();
                        }
                    });
        }

        private void addActionRemoveTrans(final Object selection, JPopupMenu popupMenu) {
            popupMenu.add(I18n.textf("Remove '%transponderName'", selection)).addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    browser.removeTransponder((TransponderElement) selection, console);
                }
            });
        }

        private void addActionEditTrans(final Object selection, JPopupMenu popupMenu) {
            popupMenu.add(I18n.textf("View/Edit '%transponderName'", selection)).addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            browser.editTransponder((TransponderElement) selection, console.getMission(),
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
