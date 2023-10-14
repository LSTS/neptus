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
 * Author: José Pinto, Margarida Faria
 * 22/03/2005, 19/03/2013
 */
package pt.lsts.neptus.gui;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

import pt.lsts.imc.LblBeacon;
import pt.lsts.imc.LblConfig;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.HTTPUtils;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBInfo;
import pt.lsts.neptus.gui.MissionTreeModel.NodeInfoKey;
import pt.lsts.neptus.gui.MissionTreeModel.ParentNodes;
import pt.lsts.neptus.gui.tree.ExtendedTreeNode;
import pt.lsts.neptus.gui.tree.ExtendedTreeNode.ChildIterator;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.types.NameId;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.HomeReferenceElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.misc.LBLRangesTimer;
import pt.lsts.neptus.types.mission.HomeReference;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * This is a visual class that displays the various items contained in a mission like transponders and plans.
 * 
 * @author Jose Pinto
 * @author Paulo Dias
 * @author Margarida Faria
 */
public class MissionBrowser extends JPanel implements PlanChangeListener {

    private static final long serialVersionUID = 1L;

    public enum State {
        SYNC("Sync"),
        NOT_SYNC("Unsync"),
        REMOTE("Remote"),
        LOCAL("Local");
        private final String fileName;

        private State(String name) {
            this.fileName = name;
        }

        /**
         * @return the name
         */
        public String getFileName() {
            return fileName;
        }
    };
    

    private final MissionTreeCellRenderer cellRenderer;
    private final JTree elementTree;
    final private MissionTreeModel treeModel;

    private final ArrayList<String> transToMerge;
    
    private boolean ignoreConsolePlanUpdate = false;

    /**
     * Creates a new mission browser which will display the items contained in the given mission type
     * 
     * @param mission The MissionType whose elements are to be displayed
     */
    public MissionBrowser() {
        elementTree = new JTree();
        cellRenderer = new MissionTreeCellRenderer();
        elementTree.setCellRenderer(cellRenderer);
        elementTree.setRootVisible(false);
        elementTree.setShowsRootHandles(true);

        this.setLayout(new BorderLayout());
        this.add(new JScrollPane(elementTree), BorderLayout.CENTER);

        treeModel = new MissionTreeModel();
        elementTree.setModel(treeModel);

        transToMerge = new ArrayList<String>();
    }

    /**
     * @return the hideTransponder
     */
    public boolean isHideTransponder() {
        return treeModel.isHideTransponder();
    }
    
    /**
     * @param hideTransponder the hideTransponder to set
     */
    public void setHideTransponder(boolean hideTransponder) {
        treeModel.setHideTransponder(hideTransponder);
    }
    
    /**
     * Returns the currently selected item (may be a directory, map, vehicle, ...)
     * 
     * @return The currently selected object
     */
    public ArrayList<NameId> getSelectedItems() {
        TreePath[] selectionPaths = elementTree.getSelectionPaths();
        ArrayList<NameId> sel = new ArrayList<NameId>();
        if (selectionPaths == null)
            return sel;

        for (TreePath path : selectionPaths) {
            ExtendedTreeNode node = (ExtendedTreeNode) path.getLastPathComponent();
            Object userObject = node.getUserObject();
            if (userObject instanceof NameId)
                sel.add((NameId) userObject);
            else {
                // if a folder is selected it is useless both when fetching selected plans and when creating the right
                // click menu by returning an empty array the right click menu reacts properly and the selected plans
                // saves some processing
                return new ArrayList<NameId>();
            }
        }

        return sel;
    }

    public ExtendedTreeNode getSelectedTreeNode() {
        if (elementTree.getSelectionPath() == null)
            return null;
        ExtendedTreeNode node = (ExtendedTreeNode) elementTree.getSelectionPath().getLastPathComponent();
        return node;
    }

    public void addTransToMerge(ArrayList<NameId> remoteTrans) {
        for (NameId nameId : remoteTrans) {
            transToMerge.add(nameId.getIdentification());
        }
    }

    /**
     * Returns the currently selected item (may be a directory, map, vehicle, ...)
     * 
     * @return The currently selected object
     */
    public Object getSelectedItem() {
        ExtendedTreeNode node = getSelectedTreeNode();
        if (node == null) {
            return null;
        }
        else {
            return node.getUserObject();
        }
    }

    public void addTransponderElement(ConsoleLayout console2) {
        if (console2 == null) {
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Add transponder"),
                    I18n.text("Unable to find a parent console"));
            return;
        }

        if (console2.getMission() == null) {
            GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Add transponder"),
                    I18n.text("No mission opened in the console"));
            return;
        }

        MissionType mt = console2.getMission();
        TransponderElement te = transponderDialog(mt, null);
        if (!te.userCancel) {
            te.getParentMap().addObject(te);
            saveMission(mt);
            treeModel.addTransponderNode(te);
            ImcMsgManager.disseminate(te, "Transponder");
        }
    }

    private TransponderElement transponderDialog(MissionType mt, TransponderElement te) {
        String transNames[];
        MapGroup mapGroupInstance = MapGroup.getMapGroupInstance(mt);

        MapType map = getMap(mt);
        try {
            Vector<TransponderElement> vector = mapGroupInstance.getAllObjectsOfType(
                    TransponderElement.class);
            transNames = new String[vector.size()];
            int i = 0;
            for (TransponderElement transponderElement : vector) {
                transNames[i] = transponderElement.getIdentification();
                i++;
            }
        }
        catch (NullPointerException e) {
            // NeptusLog.pub().warn("I cannot find local trans for main vehicle");
            transNames = new String[0];
        }
        // NeptusLog.pub().error("Adding transponder to map " + map.getId());
        if (te == null)
            te = new TransponderElement(mapGroupInstance, map);
        te.showParametersDialog(MissionBrowser.this, transNames, map, true);
        // te.setName(te.getId());
        return te;
    }


    public void editTransponder(TransponderElement elem, MissionType mission, String vehicleId) {
        ExtendedTreeNode selectedTreeNode = getSelectedTreeNode();
        TransponderElement elemBefore = elem.clone();

        State state = (State) selectedTreeNode.getUserInfo().get(NodeInfoKey.SYNC.name());
        transponderDialog(mission, elem);
        if (!elem.userCancel) {
            if (state == State.SYNC && !elemBefore.equals(elem)) {
                setNodeSyncState(selectedTreeNode, State.NOT_SYNC);
            }

            MapType pivot = elem.getParentMap();
            MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            mce.setSourceMap(pivot);
            mce.setMapGroup(MapGroup.getMapGroupInstance(mission));
            mce.setChangedObject(elem);
            pivot.warnChangeListeners(mce);
            saveMission(mission);
            treeModel.nodeChanged(selectedTreeNode);
            repaint();
        }
    }

    private MapType getMap(MissionType mt) {
        MapGroup mg = MapGroup.getMapGroupInstance(mt);
        return mg.getMaps()[0];
        // return mt.getMapsList().values().iterator().next().getMap();
    }

    private void saveMission(MissionType mission) {
        if (mission.getCompressedFilePath() != null) {
            mission.save(false);
        }
    }

    public void removeTransponder(TransponderElement elem, ConsoleLayout console2) {
        int ret = JOptionPane.showConfirmDialog(this, I18n.textf("Delete '%transponderName'?", elem.getDisplayName()),
                I18n.text("Delete"), JOptionPane.YES_NO_OPTION);
        if (ret == JOptionPane.YES_OPTION) {
            treeModel.removeById(elem.getIdentification(), ParentNodes.TRANSPONDERS);
            MapType parentMap = elem.getParentMap();
            parentMap.remove(elem.getIdentification());
            getMap(console2.getMission()).remove(elem.getIdentification());
            saveMission(console2.getMission());
        }
    }

    public void swithLocationsTransponder(TransponderElement tel1, TransponderElement tel2, ConsoleLayout console2) {
        LocationType loc1 = tel1.getCenterLocation();
        LocationType loc2 = tel2.getCenterLocation();
        tel1.setCenterLocation(loc2);
        tel2.setCenterLocation(loc1);

        MapType pivot = tel1.getParentMap();
        MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        mce.setSourceMap(pivot);
        mce.setMapGroup(MapGroup.getMapGroupInstance(console2.getMission()));
        mce.setChangedObject(tel1);
        pivot.warnChangeListeners(mce);

        MapType pivot2 = tel2.getParentMap();
        MapChangeEvent mce2 = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        mce2.setSourceMap(pivot2);
        mce2.setMapGroup(MapGroup.getMapGroupInstance(console2.getMission()));
        mce2.setChangedObject(tel2);
        pivot2.warnChangeListeners(mce2);

        LinkedHashMap<String, MapMission> mapsList = console2.getMission().getMapsList();
        MapMission mm = mapsList.get(pivot.getId());
        mm.setMap(pivot);
        pivot.saveFile(mm.getHref());
        mm = mapsList.get(pivot2.getId());
        mm.setMap(pivot2);
        pivot2.saveFile(mm.getHref());

        if (console2 != null && console2.getMission() != null && console2.getMission().getCompressedFilePath() != null) {
            console2.getMission().save(false);
            console2.updateMissionListeners();
        }
    }

    /**
     * Sets home reference as described in the mission.
     * <p>
     * For transponders and plans, compares the mission to information received from the vehicle (ImcSystems) and
     * derivates their state.
     * <p>
     * Restores selection of nodes.
     * 
     * @param mission
     * @param mainVehicleId
     */
    public void refreshBrowser(final MissionType mission, final String mainVehicleId, ConsoleLayout console) {
        // Selected nodes
        TreePath[] selectedNodes = getSelectionPath();
        // Home ref
        treeModel.setHomeRef(mission.getHomeRef());
        TreeMap<String, PlanType> localPlans = getLocalPlans(mission);
        updatePlansStateEDT(localPlans, mainVehicleId);
        updateTransStateEDT(mission, mainVehicleId, console);
        // Set the right nodes as selected
        setSelectedNodes(selectedNodes);
    }

    private TreeMap<String, PlanType> getLocalPlans(final MissionType mission) {
        TreeMap<String, PlanType> localPlans;
        try {
            localPlans = mission.getIndividualPlansList();
        }
        catch (NullPointerException e) {
            localPlans = new TreeMap<String, PlanType>();
        }
        return localPlans;
    }

    @Override
    public void planChange(PlanType plan) {
        setSelectedPlan(plan);
    }

    /**
     * 
     * @param tr
     * @param mission
     * @return true if mission listeners should be updated
     */
    public boolean setContent(Transferable tr, MissionType mission) {
        DataFlavor[] flavors = tr.getTransferDataFlavors();
        for (int i = 0; i < flavors.length; i++) {
            if (flavors[i].isMimeTypeEqual("text/plain; class=java.lang.String; charset=Unicode")) {
                String url = null;

                try {
                    Object data = tr.getTransferData(flavors[i]);
                    if (data instanceof InputStreamReader) {
                        BufferedReader reader = new BufferedReader((InputStreamReader) data);
                        url = reader.readLine();
                        reader.close();
                    }
                    else if (data instanceof String) {
                        url = data.toString();
                    }

                    return parseURL(url, mission);
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
            }
        }
        return false;
    }

    public void setMultiSelect(MouseEvent e) {
        TreePath path = elementTree.getPathForLocation(e.getX(), e.getY());
        elementTree.setSelectionPath(path);
    }

    public void addMouseAdapter(MouseAdapter mouseAdapter) {
        elementTree.addMouseListener(mouseAdapter);
    }

    /**
     * 
     * @param url
     * @param mission
     * @return true if mission listeners should be updated
     */
    public boolean parseURL(String url, MissionType mission) {
        NeptusLog.pub().error("parsing " + url);
        if (url == null || mission == null)
            return false;

        if (url.startsWith("http")) {
            String xml = HTTPUtils.get(url);
            return parseContents(xml, mission);
        }
        return false;
    }

    /**
     * 
     * @param file
     * @param mission
     * @return true if mission listeners should be updated
     */
    public boolean parseContents(String file, MissionType mission) {
        try {
            Document doc = DocumentHelper.parseText(file);
            String root = doc.getRootElement().getName();
            if (root.equalsIgnoreCase("home-reference")) {
                HomeReference homeRef = new HomeReference();
                boolean loadOk = homeRef.load(file);
                if (loadOk) {
                    mission.getHomeRef().setCoordinateSystem(homeRef);
                    Vector<HomeReferenceElement> hrefElems = MapGroup.getMapGroupInstance(mission).getAllObjectsOfType(
                            HomeReferenceElement.class);
                    hrefElems.get(0).setCoordinateSystem(mission.getHomeRef());
                    mission.save(false);
                    return true;
                }
            }
            else if (root.equalsIgnoreCase("StartLocation")) {
                MarkElement start = new MarkElement(file);
                AbstractElement[] startLocs = MapGroup.getMapGroupInstance(mission).getMapObjectsByID("start");
                MapType pivot = null;

                if (startLocs.length == 1 && startLocs[0] instanceof MarkElement) {
                    ((MarkElement) startLocs[0]).setCenterLocation(start.getCenterLocation());
                    pivot = startLocs[0].getParentMap();
                    MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                    mce.setSourceMap(pivot);
                    mce.setMapGroup(pivot.getMapGroup());
                    mce.setChangedObject(startLocs[0]);
                    pivot.warnChangeListeners(mce);
                }
                else if (startLocs.length == 0) {
                    try {
                        pivot = getMap(mission);
                        start.setId("start");
                        start.setParentMap(pivot);
                        start.setMapGroup(pivot.getMapGroup());
                        pivot.addObject(start);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                    }
                }
                if (pivot != null) {
                    pivot.saveFile(pivot.getHref());
                    if (mission != null && mission.getCompressedFilePath() != null) {
                        mission.save(false);
                    }
                }
                return true;
            }
            else if (root.equalsIgnoreCase("Transponder")) {
                TransponderElement transponder = new TransponderElement(file);
                AbstractElement[] sameId = MapGroup.getMapGroupInstance(mission).getMapObjectsByID(transponder.getId());
                MapType pivot = null;

                if (sameId.length == 1 && sameId[0] instanceof TransponderElement) {
                    ((TransponderElement) sameId[0]).setCenterLocation(transponder.getCenterLocation());
                    ((TransponderElement) sameId[0]).setBuoyAttached(transponder.isBuoyAttached());
                    ((TransponderElement) sameId[0]).setConfiguration(transponder.getConfiguration());

                    pivot = sameId[0].getParentMap();
                    MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
                    mce.setSourceMap(pivot);
                    mce.setMapGroup(pivot.getMapGroup());
                    mce.setChangedObject(sameId[0]);
                    pivot.warnChangeListeners(mce);
                }
                else if (sameId.length == 0) {
                    try {
                        pivot = getMap(mission);
                        transponder.setParentMap(pivot);
                        transponder.setMapGroup(pivot.getMapGroup());
                        pivot.addObject(transponder);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error(e);
                    }
                }
                if (pivot != null) {
                    pivot.saveFile(pivot.getHref());
                    if (mission != null && mission.getCompressedFilePath() != null) {
                        mission.save(false);
                    }
                }
                return true;
            }
            else if (root.equalsIgnoreCase("Plan") && mission != null) {
                PlanType plan = new PlanType(file, mission);

                mission.getIndividualPlansList().put(plan.getId(), plan);

                if (mission != null && mission.getCompressedFilePath() != null) {
                    mission.save(false);
                }
                return true;
            }
        }
        catch (DocumentException e) {
            NeptusLog.pub().error(e);
            return false;
        }
        return false;
    }

    public void addTreeListener(final ConsoleLayout console2) {
        elementTree.addTreeSelectionListener(new TreeSelectionListener() {

            // protected Object lastSelection = null;

            @Override
            public void valueChanged(TreeSelectionEvent e) {

                // if (e.isAddedPath()) {
                    TreePath selPath = elementTree.getSelectionPath();
                    if(selPath == null){
                        return;
                    }
                    ExtendedTreeNode node = (ExtendedTreeNode) selPath.getLastPathComponent();

                // if (node.getUserObject() == lastSelection)
                // return;

                // lastSelection = node.getUserObject();

                    if (node.getUserObject() instanceof PlanType) {
                        PlanType selectedPlan = (PlanType) node.getUserObject();
                        if (console2 != null && !ignoreConsolePlanUpdate)
                            console2.setPlan(selectedPlan);
                    }
                    else if (console2 != null && !ignoreConsolePlanUpdate) {
                        console2.setPlan(null);
                    }
            }

        });
    }

    public void setSelectedPlan(PlanType plan) {
        if (plan == null || getSelectedItem() == plan) {
            return;
        }

        ExtendedTreeNode planNode = null;
        TreePath selPath;
        NameId tmpPlan;
        ChildIterator planIt = treeModel.getIterator(ParentNodes.PLANS);
        try {
            while (planIt.hasNext()) {
                planNode = planIt.next();
                tmpPlan = (NameId) planNode.getUserObject();
                if (tmpPlan.getIdentification().equals(plan.getIdentification())) {
                    // planNode.setUserObject(plan);
                    // setNodeSyncState(planNode, State.SYNC);
                    break;
                }
                planNode = null;
            }
            // for (planNode = planIt.next(); planIt.hasNext(); planNode = planIt.next()) {
            // NameId tmpPlan = (NameId) planNode.getUserObject();
            // if (tmpPlan.getIdentification().equals(plan.getIdentification())) {
            // // planNode.setUserObject(plan);
            // // setNodeSyncState(planNode, State.SYNC);
            // break;
            // }
            // }
            if (planNode == null) {
                // System.out.println("hasNext:" + planIt.hasNext());
                NeptusLog.pub().error(
                        "Cannot find a plan with id " + plan.getIdentification() + ". It was not set as selected.");
                return;
            }

            selPath = new TreePath(treeModel.getPathToRoot(planNode));
            ignoreConsolePlanUpdate = true;
            elementTree.setSelectionPath(selPath);
            ignoreConsolePlanUpdate = false;
            elementTree.scrollPathToVisible(selPath);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public ArrayList<ExtendedTreeNode> getSelectedNodes() {
        ArrayList<ExtendedTreeNode> nodes = new ArrayList<ExtendedTreeNode>();
        TreePath[] selectionPaths = elementTree.getSelectionPaths();
        if (selectionPaths == null)
            return nodes;
        ExtendedTreeNode node;
        for (int i = 0; i < selectionPaths.length; i++) {
            node = (ExtendedTreeNode) selectionPaths[i].getLastPathComponent();
            nodes.add(node);
        }
        return nodes;
    }

    public TreePath[] getSelectionPath() {
        return elementTree.getSelectionPaths();
    }

    public void setSelectedNodes(TreePath[] selectedNodes) {
        elementTree.setSelectionPaths(selectedNodes);
    }

    /**
     * Takes the local plans and gets the remote ones stored in the PlanDBState associated with the system and merges
     * with the current tree. The following rules are applied:
     * <p>
     * - insert plans not previously in the tree
     * <p>
     * - if a plan with the same id and md5 exists in both local and remote set as SYNC
     * <p>
     * - if a plan with the same id exists in both local and remote but different md5 set as NOT_SYNC
     * <p>
     * - if a plan only exists in remote set as REMOTE
     * <p>
     * - if a plan only exists in local set as LOCAL
     * <p>
     * The merging is done in the EDT since the process of the deep copy and updating the tree after merging costs too
     * much time and memory in comparison to the small cost of doing the merge (since there are at most 20 plans
     * typically).
     * 
     * @param localPlans the plans in the mission
     * @param sysName the system to consider
     */
    public void updatePlansStateEDT(final TreeMap<String, PlanType> localPlans, final String sysName) {
        final Map<String, PlanDBInfo> remotePlans = getRemotePlans(sysName);
        Runnable runnble = new Runnable() {
            @Override
            public void run() {
                // NeptusLog.pub().error("--> updatePlansStateEDT ");
                // NeptusLog.pub().error("[1] " + localPlans.size() + " in mission: " + localPlans.values().toString());
                // NeptusLog.pub().error("[2] " + localPlans.size() + " in vehicle: " +
                // remotePlans.values().toString());
                // treeModel.printTree("[3] ", ParentNodes.PLANS);
                HashSet<String> existingPlans = mergeLocalPlans(localPlans, sysName, treeModel);
                // treeModel.printTree("[4] ", ParentNodes.PLANS);
                treeModel.removeSet(existingPlans, ParentNodes.PLANS);
                // treeModel.printTree("[5] ", ParentNodes.PLANS);
                existingPlans = mergeRemotePlans(sysName, remotePlans, treeModel, existingPlans);
                // treeModel.printTree("[6] ", ParentNodes.PLANS);
                elementTree.expandPath(treeModel.getPathToParent(ParentNodes.PLANS));
                // NeptusLog.pub().error("------------------------ ");
                revalidate();
                repaint();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            runnble.run();
        }
        else {
            try {
                SwingUtilities.invokeAndWait(runnble);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Go through remote plans and alphabetically merge into nextModel.
     * 
     * @param sysName the main vehicle
     * @param remotePlans remote plans known to IMCSystem
     * @param treeModel the model where to merge
     */
    private HashSet<String> mergeRemotePlans(String sysName, Map<String, PlanDBInfo> remotePlans,
            MissionTreeModel treeModel, HashSet<String> existingPlans) {
        // System.out.println("Merging " + remotePlans.size() + " remote plans");
        ExtendedTreeNode target;
        Set<String> remotePlansIds = new HashSet<>();
        remotePlansIds.addAll(remotePlans.keySet());
        for (String planId : remotePlansIds) {
            existingPlans.add(planId);
            target = treeModel.findNode(planId, ParentNodes.PLANS);
            PlanDBInfo remotePlan = remotePlans.get(planId);
            // System.out.print(planId + "\t");
            if (target == null) {
                // If no plan exits insert as remote
                target = new ExtendedTreeNode(remotePlan);
                target.getUserInfo().put(NodeInfoKey.ID.name(), planId);
                setNodeSyncState(target, State.REMOTE);
                treeModel.insertAlphabetically(target, ParentNodes.PLANS);
                // System.out.println(" plan from IMCSystem not found in mission tree  >> Remote.");
            }
            else {
                // Check if existing plan is PlanDBInfo
                Object existingPlan = target.getUserObject();
                if (existingPlan instanceof PlanDBInfo) {
                    setNodeSyncState(target, State.REMOTE);
                    target.setUserObject(remotePlan);
                    // System.out.println(" in tree mission is PlanDBInfo (remote type)  >> Remote.");
                }
                else if (existingPlan instanceof PlanType) {
                    PlanType existingLocalPlan = (PlanType) existingPlan;
                    // If there is already a plan use md5 to find out if it is sync
                    byte[] localMD5 = existingLocalPlan.asIMCPlan().payloadMD5();
                    byte[] remoteMD5 = remotePlan.getMd5();
                    if (ByteUtil.equal(localMD5, remoteMD5)) {
                        setNodeSyncState(target, State.SYNC);
                        // System.out.println(" in tree mission is PlanType (local type). Md5 ==,  >> Sync.");
                    }
                    else {
                        setNodeSyncState(target, State.NOT_SYNC);
                        // System.out.println(" in tree mission is PlanType (local type). Md5 !=,  >> Not_sync.");
                    }
                }
            }
            target.getUserInfo().put(NodeInfoKey.VEHICLE.name(), sysName);
        }
        return existingPlans;
    }

    /**
     * Go through remote LblBeacon configurations and alphabetically merge into nextModel.
     * <p>
     * This is where the synchronization state is decided.
     * <p>
     * Unable to use the same method as for plan because of the differences between LblBeacons, PlanDBInfo and PlanType.
     * 
     * @param sysName the main vehicle
     * @param remoteList beacon configurations known to IMCSystem
     * @param treeModel the model where to merge
     */
    private HashSet<String> mergeRemoteTrans(String sysName, Vector<LblBeacon> remoteList,
 MissionTreeModel treeModel,
            HashSet<String> existing, MissionType mission, ConsoleLayout console) {
        ExtendedTreeNode node;
        ChildIterator transIt;
        ExtendedTreeNode tempNode;
        TransponderElement tempTrans;

        // System.out.println("mergeRemoteTrans");
        HashMap<String, TransponderElement> idMap = new HashMap<String, TransponderElement>();
        transIt = treeModel.getIterator(ParentNodes.TRANSPONDERS);
        // Make a list of all the nodes with a link to their id to keep track of which ids to reset in the end
        TransponderElement transponderElement;
        while (transIt.hasNext()) {
            tempNode = transIt.next();
            transponderElement = (TransponderElement) tempNode.getUserObject();
            idMap.put(transponderElement.getIdentification(), transponderElement);
        }

        boolean saveMisison = false;
        boolean found = false;
        short id = 0; // the id inside DUNE is the index in the vector
        for (LblBeacon lblBeacon : remoteList) {
            found = false;
            transIt = treeModel.getIterator(ParentNodes.TRANSPONDERS);
            tempTrans = null;
            while (transIt.hasNext()) {
                tempNode = transIt.next();
                tempTrans = (TransponderElement) tempNode.getUserObject();
                if (tempTrans.getIdentification().equals(lblBeacon.getBeacon())) {
                    // Counts as the same beacon
                    if (tempTrans.equals(lblBeacon)) {
                        // Sync
                        // set state
                        setNodeSyncState(tempNode, State.SYNC);
                        // System.out.println(" >> Sync.");
                    }
                    else {
                        // Not sync
                        // set state
                        if (transToMerge.contains(tempTrans.getIdentification())) {
                            ExtendedTreeNode treeNode = treeModel.findNode(lblBeacon.getBeacon(),
                                    ParentNodes.TRANSPONDERS);
                            TransponderElement newTrans = new TransponderElement(lblBeacon, id,
                                    tempTrans.getMapGroup(), tempTrans.getParentMap());
                            treeNode.setUserObject(newTrans);
                            // System.out.println(lblBeacon.getBeacon() + " updated to "
                            // + newTrans.getCenterLocation().getDepth() + " depth");
                            transToMerge.remove(lblBeacon.getBeacon());
                            setNodeSyncState(treeNode, State.SYNC);
                            // update mission
                            newTrans.getParentMap().addObject(newTrans);
                            saveMission(mission);
                        }
                        else {
                            setNodeSyncState(tempNode, State.NOT_SYNC);
                        }
                        // System.out.println(" >> Not Sync.");
                    }
                    // set id
                    tempTrans.setDuneId(id);
                    // System.out.print("[" + tempTrans.duneId + "] " + tempTrans.getDisplayName()
                    // + " from IMCSystem found in mission tree  ");
                    // remove from reset id list
                    idMap.remove(tempTrans.getIdentification());
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Remote => create one
                MapGroup mapGroup = MapGroup.getMapGroupInstance(mission);
                // MapType[] maps = mapGroup.getMaps();
                MapType map = getMap(mission);
                tempTrans = new TransponderElement(lblBeacon, id, mapGroup, map);
                node = new ExtendedTreeNode(tempTrans);
                setNodeSyncState(node, State.SYNC);
                // maps[0].addObject(tempTrans);
                // maps[0].saveFile(maps[0].getHref());
                treeModel.insertAlphabetically(node, ParentNodes.TRANSPONDERS);
                map.addObject(tempTrans);
                saveMisison = true;
                // System.out.println(" [" + tempTrans.duneId + "] " + tempTrans.getDisplayName()
                // + " from IMCSystem not found in mission tree  >> Sync.");
            }
            // signal as existing
            existing.add(tempTrans.getIdentification());
            id++;
        }
        if (saveMisison)
            saveMission(mission);

        // reset id of transponders not in vehicle
        transIt = treeModel.getIterator(ParentNodes.TRANSPONDERS);
        // System.out.print("Reseting id of:");
        while (transIt.hasNext() && idMap.size() > 0) {
            tempNode = transIt.next();
            tempTrans = (TransponderElement) tempNode.getUserObject();
            String tempId = tempTrans.getIdentification();
            if (idMap.containsKey(tempId)) {
                // System.out.print(tempId + ", ");
                ((TransponderElement) tempNode.getUserObject()).setDuneId((short) -1);
                idMap.remove(tempId);
            }
        }
        // System.out.println();
        return existing;
    }


    /**
     * Go through remote plans and alphabetically merge into model. Make a list of all seen plans so discontinued ones
     * can be deleted later on.
     * 
     * @param localPlans local plans known to IMCSystem
     * @param sysName the main vehicle
     * @param treeModel the model where to merge
     * @return the list of seen plans.
     */
    private HashSet<String> mergeLocalPlans(TreeMap<String, PlanType> localPlans, String sysName,
            MissionTreeModel treeModel) {
        Set<String> localPlansIds = localPlans.keySet();
        HashSet<String> existingPlans = new HashSet<String>();
        ExtendedTreeNode target, newNode;
        PlanType plan;
        for (String planId : localPlansIds) {
            existingPlans.add(planId);
            target = treeModel.findNode(planId, ParentNodes.PLANS);
            plan = localPlans.get(planId);
            // System.out.print(planId + " \t");
            if (target == null) {
                // If no plan exits insert as local
                newNode = new ExtendedTreeNode(plan);
                newNode.getUserInfo().put(NodeInfoKey.ID.name(), planId);
                treeModel.insertAlphabetically(newNode, ParentNodes.PLANS);
                target = newNode;
                // System.out.print(" mission plan not found in mission tree. Creating with mission plan.");
            }
            else {
                target.setUserObject(plan);
                // System.out.print(" updated plan object.");
                // not worth the troubele of checking if it is different
            }
            // Set the node to local regardless.
            // It will be checked when processing remote states.
            setNodeSyncState(target, State.LOCAL);
            target.getUserInfo().put(NodeInfoKey.VEHICLE.name(), sysName);
            // System.out.println(" Setting as local.");
        }
        return existingPlans;
    }

    /**
     * Go through remote items and alphabetically merge into model. Make a list of all seen items so discontinued ones
     * can be deleted later on.
     * 
     * @param local items associated with the mission known to IMCSystem
     * @param sysName the main vehicle
     * @param treeModel the model where to merge
     * @return the list of seen items.
     */
    private HashSet<String> mergeLocalTrans(LinkedHashMap<String, TransponderElement> local, String sysName,
            MissionTreeModel treeModel,
            ParentNodes itemType) {
        Set<String> localIds = local.keySet();
        HashSet<String> existing = new HashSet<String>();
        ExtendedTreeNode target, newNode;
        TransponderElement localTrans;
        // System.out.println("Merge local " + itemType.nodeName);
        // System.out.println(local.size() + " trans in Maps: ");
        // StringBuilder remotes;
        for (String id : localIds) {
            existing.add(id);
            target = treeModel.findNode(id, itemType);
            localTrans = local.get(id);
            // remotes = new StringBuilder();
            // remotes.append("[");
            // remotes.append(localTrans.duneId);
            // remotes.append("] ");
            // remotes.append(localTrans.getDisplayName());
            // remotes.append(" \t");
            // System.out.print(remotes.toString());
            if (target == null) {
                // If no trans exits insert as local
                localTrans.setDuneId((short) -1);
                newNode = new ExtendedTreeNode(localTrans);
                treeModel.insertAlphabetically(newNode, itemType);
                target = newNode;
                // System.out.print(localTrans.getDisplayName() + " not found in mission tree.");
            }
            else {
                target.setUserObject(localTrans);
                // System.out.print(" updated object.");
                // not worth the troubele of checking if it is different
            }
            // Set the node to local regardless.
            // It will be checked when processing remote states.
            setNodeSyncState(target, State.LOCAL);
            target.getUserInfo().put(NodeInfoKey.VEHICLE.name(), sysName);
            // System.out.println(" Setting as local.");
        }
        return existing;
    }

    /**
     * Go to IMCSystem in the IMCSystemHolder and the plans stored in the associated PlanDBControl.
     * 
     * @param sysName the name of the system you want the plans from
     * @return the plans found, an empty map if none are found
     */
    private Map<String, PlanDBInfo> getRemotePlans(String sysName) {
        Map<String, PlanDBInfo> remotePlans;
        try {
            remotePlans = ImcSystemsHolder.lookupSystemByName(sysName).getPlanDBControl().getRemoteState()
                    .getStoredPlans();
        }
        catch (NullPointerException e) {
            // NeptusLog.pub().warn("I cannot find remote plans for " + sysName);
            remotePlans = new LinkedHashMap<String, PlanDBInfo>();
        }
        return remotePlans;
    }


    /**
     * Go to IMCSystem in the IMCSystemHolder and return the lastest LblBeacon configurations.
     * 
     * @param sysName the name of the system you want the plans from
     * @return the LBLBeacons found, an empty Map if anything was uninitialized
     */
    private Vector<LblBeacon> getRemoteTrans(String sysName) {
        try {
            return ((LblConfig) ImcSystemsHolder.lookupSystemByName(sysName).retrieveData(
                    SystemUtils.LBL_CONFIG_KEY)).getBeacons();
        }
        catch (NullPointerException e) {
            // NeptusLog.pub().warn("I cannot find remote beacon configuration for " + sysName);
            return new Vector<LblBeacon>();
        }
    }

    /**
     * If at surface, stop timer. Otherwise reset timer to 0.
     * 
     * @param id
     * @param mainVehicle
     */
    public void transUpdateTimer(short id, String mainVehicle) {
        HashMap<String, Object> userInfo;
        short nodeId;
        String transVehicle, name;
        ImcSystem imcSystems;
        LBLRangesTimer timer;
        TransponderElement trans;
        ChildIterator transIt = treeModel.getIterator(ParentNodes.TRANSPONDERS);
        ExtendedTreeNode transNode;
        boolean reval = false;
        while (transIt.hasNext()) {
            transNode = transIt.next();
            trans = (TransponderElement) transNode.getUserObject();
            // System.out.println(trans.getDisplayName());
            userInfo = transNode.getUserInfo();
            nodeId = trans.duneId;
            transVehicle = (String) userInfo.get(NodeInfoKey.VEHICLE.name());
            if (nodeId == id && transVehicle.equals(mainVehicle)) {
                imcSystems = ImcSystemsHolder.lookupSystemByName(transVehicle);
                if (imcSystems != null) {
                    name = trans.getIdentification();
                    timer = (LBLRangesTimer) imcSystems.retrieveData(name);
                    if (timer == null) {
                        timer = new LBLRangesTimer();
                        imcSystems.storeData(name, timer);
                    }
                    timer.resetTime();
                    reval = true;
                }
                break;
            }
        }
        if (reval)
            revalidate();
    }

    /**
     * Start all synchronized transponders associated with the vehicle.
     * 
     * @param mainVehicle
     */
    public void transStartVehicleTimers(String mainVehicle) {
        ChildIterator transIt = treeModel.getIterator(ParentNodes.TRANSPONDERS);
        ExtendedTreeNode transNode;
        while (transIt.hasNext()) {
            transNode = transIt.next();
            HashMap<String, Object> userInfo = transNode.getUserInfo();
            String transVehicle = (String) userInfo.get(NodeInfoKey.VEHICLE.name());
            // only looks at synchronized transponders that are linked to the designated vehicle
            State nodeSync = (State) userInfo.get(NodeInfoKey.SYNC.name());
            if (nodeSync == State.SYNC && transVehicle.equals(mainVehicle)) {
                ImcSystem imcSystems = ImcSystemsHolder.lookupSystemByName(transVehicle);
                if (imcSystems != null) {
                    String name = ((TransponderElement) transNode.getUserObject()).getIdentification();
                    LBLRangesTimer timer = (LBLRangesTimer) imcSystems.retrieveData(name);
                    if (timer == null) {
                        timer = new LBLRangesTimer();
                        imcSystems.storeData(name, timer);
                    }
                    timer.resetTime();
                    // NeptusLog.pub().error(name + " Started timer");
                }
            }
        }

        revalidate(); // call EDT
    }

    /**
     * Stop all transponder timers for every vehicle.
     * 
     */
    public void transStopTimers() {
        HashMap<String, Object> transInfo;
        ImcSystem imcSystems;
        String name;
        LBLRangesTimer timer;
        ChildIterator transIt = treeModel.getIterator(ParentNodes.TRANSPONDERS);
        ExtendedTreeNode transNode;
        while (transIt.hasNext()) {
            transNode = transIt.next();
            transInfo = transNode.getUserInfo();
            String transVehicle = (String) transInfo.get(NodeInfoKey.VEHICLE.name());
            imcSystems = ImcSystemsHolder.lookupSystemByName(transVehicle);
            if (imcSystems != null) {
                name = ((TransponderElement) transNode.getUserObject()).getIdentification();
                timer = (LBLRangesTimer) imcSystems.retrieveData(name);
                if (timer != null) {
                    timer.stopTimer();
                }
            }
        }
        revalidate();
    }


    /**
     * This method gets the transponders in the mission file and sets them as local in the mission tree.
     * <p>
     * Then he compares the mission tree with the transponder configuration last received from the vehicle (LblConfing
     * stored in associated ImcSystem). Every LblBeacon in the configuration with the same name and same parameters is
     * set as synchronized (because of the known approximation bug, the equality is not recognized sometimes). Every
     * LblBeacon in the configuration with the same name but some differing parameters is set as not synchronized.
     * <p>
     * If a transponder in the mission tree is not referenced in either mission file or ImcSystem it is deleted.
     * <p>
     * All synchronized or not synchronized transponders regenerate their id based on their alphabetical order (this
     * replicates the order they are sent to the vehicle and that Dune creates for them based on that). The other get an
     * id of -1.
     * 
     * @param mission
     * @param sysName
     * @param remoteTrans
     */
    public void updateTransStateEDT(final MissionType mission, final String sysName,
            final Vector<LblBeacon> remoteTrans, final ConsoleLayout console) {
        final LinkedHashMap<String, TransponderElement> localTrans = getLocalTrans(mission);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                // NeptusLog.pub().error("--> updateTransStateEDT ");
                // NeptusLog.pub().error(localTrans.size() + " in mission: " + localTrans.values().toString());
                // treeModel.printTree("1. ");
                HashSet<String> existingTrans = mergeLocalTrans(localTrans, sysName, treeModel,
                        ParentNodes.TRANSPONDERS);
                // treeModel.printTree("2. ");
                // String remotes = printBeacons(remoteTrans);
                // NeptusLog.pub().error(remotes);
                existingTrans = mergeRemoteTrans(sysName, remoteTrans, treeModel, existingTrans, mission, console);
                // treeModel.printTree("3. ");
                treeModel.removeSet(existingTrans, ParentNodes.TRANSPONDERS);
                // treeModel.printTree("4. ");
                elementTree.expandPath(treeModel.getPathToParent(ParentNodes.TRANSPONDERS));
                // NeptusLog.pub().error(" --- ");
                repaint();
            }

//            private String printBeacons(final Vector<LblBeacon> remoteTrans) {
//                short id = 0;
//                StringBuilder remotes = new StringBuilder(remoteTrans.size() + " trans in ImcSystem: ");
//                for (LblBeacon lblBeacon : remoteTrans) {
//                    remotes.append("[");
//                    remotes.append(id);
//                    remotes.append("] ");
//                    remotes.append(lblBeacon.getBeacon());
//                    remotes.append(" ( query: ");
//                    remotes.append(lblBeacon.getQueryChannel());
//                    remotes.append(", reply: ");
//                    remotes.append(lblBeacon.getReplyChannel());
//                    remotes.append(", delay:");
//                    remotes.append(lblBeacon.getTransponderDelay());
//                    remotes.append(")\n         ");
//                    id++;
//                }
//                return remotes.toString();
//            }

        });
    }

    /**
     * Convenience method for when the source of the remote files is the ImcSystem associated with the current main
     * vehicle.
     * 
     * @param mission
     * @param sysName
     */
    public void updateTransStateEDT(MissionType mission, final String sysName, ConsoleLayout console) {
        updateTransStateEDT(mission, sysName, getRemoteTrans(sysName), console);
    }

    /**
     * Get all the transponder elements associated with the mission.
     * 
     * @param mission current mission
     * @return the found elements
     */
    private LinkedHashMap<String, TransponderElement> getLocalTrans(MissionType mission) {
        LinkedHashMap<String, TransponderElement> map = new LinkedHashMap<String, TransponderElement>();
        Vector<TransponderElement> vector;
        try {
            vector = MapGroup.getMapGroupInstance(mission).getAllObjectsOfType(TransponderElement.class);
            for (TransponderElement transponderElement : vector) {
                map.put(transponderElement.getIdentification(), transponderElement);
            }
        }
        catch (NullPointerException e) {
            // NeptusLog.pub().warn("I cannot find local trans for main vehicle");
        }
        // NeptusLog.pub().error("Got " + map.size() + " local transponders.");
        return map;
    }


    public <T extends NameId> void removeCurrSelectedNodeRemotely() {
        ExtendedTreeNode selectionNode = getSelectedTreeNode();

        // This is triggered twice, on the second one check for null
        if (selectionNode == null) {
            return;
        }
        State syncState = (State) selectionNode.getUserInfo().get(NodeInfoKey.SYNC.name());
        switch (syncState) {
            case SYNC:
                // Local
            case NOT_SYNC:
                // Local
                    setNodeSyncState(selectionNode, State.LOCAL);
                break;
            case REMOTE:
                // Disappear
                Object selectedItem = getSelectedItem();
                treeModel.removeById(((NameId) selectedItem).getIdentification(), ParentNodes.PLANS);
                break;
            case LOCAL:
                // Invalid
                NeptusLog.pub()
                        .error("Invalid removal of local plan, this is ok if it is triggered by duplicate sending of plan deletion.");
                break;
            default:
                NeptusLog.pub().error("Invalid local removal of plan with unkown state");
        }
    }

    public <T extends NameId> void deleteCurrSelectedNodeLocally() {
        ArrayList<ExtendedTreeNode> selectedNodes = getSelectedNodes();
        // TreePath[] selectedNodes = selectedNodes2;
        // for (TreePath treePath : selectedNodes) {
        for (ExtendedTreeNode selectionNode : selectedNodes) {
            // ExtendedTreeNode selectionNode = getSelectedTreeNode();
            // ExtendedTreeNode selectionNode = (ExtendedTreeNode) treePath.getLastPathComponent();
            State syncState = (State) selectionNode.getUserInfo().get(NodeInfoKey.SYNC.name());
            NameId userObject = (NameId) selectionNode.getUserObject();
            switch (syncState) {
                case NOT_SYNC:
                    // To Remote
                case SYNC:
                    // Remote
                    PlanDBInfo remoteNode = new PlanDBInfo();
                    remoteNode.setPlanId(userObject.getIdentification());
                    ExtendedTreeNode node = new ExtendedTreeNode(remoteNode);
                    setNodeSyncState(node, State.REMOTE);
                    treeModel.insertAlphabetically(node, ParentNodes.PLANS);
                case LOCAL:
                    // Disappear
                    treeModel.removeById(userObject.getIdentification(), ParentNodes.PLANS);
                    break;
                case REMOTE:
                    // Invalid
                    NeptusLog.pub().error("Invalid local removal of remote plan");
                    break;
                default:
                    NeptusLog.pub().error("Invalid local removal of plan with unkown state");
            }
        }
    }

    public void setPlanAsSync(String planId) {
        ExtendedTreeNode plan = treeModel.findNode(planId, ParentNodes.PLANS);
        if (plan != null) {
            setNodeSyncState(plan, State.SYNC);
        }
    }

    private void setNodeSyncState(ExtendedTreeNode child, State state) {
        child.getUserInfo().put(NodeInfoKey.SYNC.name(), state);
        repaint();
    }

    public void setDebugOn(boolean value) {
        TreeCellRenderer cr = elementTree.getCellRenderer();
        if (cr instanceof MissionTreeCellRenderer)
            ((MissionTreeCellRenderer) cr).debugOn = value;
    }

    public void setMaxAcceptableElapsedTime(int maxAcceptableElapsedTime) {
        cellRenderer.maxAcceptableElapsedTime = maxAcceptableElapsedTime;
    }

    
    public ArrayList<TransponderElement> getTransponders(){
        ChildIterator transIt = treeModel.getIterator(ParentNodes.TRANSPONDERS);
        ArrayList<TransponderElement> trans = new ArrayList<TransponderElement>();
        ExtendedTreeNode transNode = null;
        while (transIt.hasNext()) {
            transNode = transIt.next();
//        }
            // for (; transIt.hasNext(); transNode = transIt.next()) {
            TransponderElement t = (TransponderElement)transNode.getUserObject();
            // System.out.println(t.getName() + " ");
            trans.add(t);
        }
        return trans;
    }
}

