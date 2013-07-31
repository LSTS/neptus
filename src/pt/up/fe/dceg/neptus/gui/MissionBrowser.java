/*
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Author: José Pinto, Margarida Faria
 * 22/03/2005, 19/03/2013
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.PlanChangeListener;
import pt.up.fe.dceg.neptus.gui.tree.ExtendedTreeNode;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.LblBeacon;
import pt.up.fe.dceg.neptus.imc.LblConfig;
import pt.up.fe.dceg.neptus.mp.MapChangeEvent;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBInfo;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBState;
import pt.up.fe.dceg.neptus.types.Identifiable;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.AbstractElement;
import pt.up.fe.dceg.neptus.types.map.HomeReferenceElement;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.map.TransponderUtils;
import pt.up.fe.dceg.neptus.types.misc.LBLRangesTimer;
import pt.up.fe.dceg.neptus.types.mission.HomeReference;
import pt.up.fe.dceg.neptus.types.mission.MapMission;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.ByteUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.HTTPUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * This is a visual class that displays the various items contained in a mission like maps, vehicles and plans...
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
    final private Model treeModel;
    private final Vector<ChangeListener> listeners = new Vector<ChangeListener>();

    /**
     * Creates a new mission browser which will display the items contained in the given mission type
     * 
     * @param mission The MissionType whose elements are to be displayed
     */
    public MissionBrowser() {
        ConfigFetch.mark("MissionBrowser");

        elementTree = new JTree();
        ConfigFetch.mark("MissionTreeCellRenderer");
        cellRenderer = new MissionTreeCellRenderer();
        elementTree.setCellRenderer(cellRenderer);
        elementTree.setRootVisible(false);
        elementTree.setShowsRootHandles(true);
        ConfigFetch.benchmark("MissionTreeCellRenderer");

        this.setLayout(new BorderLayout());
        this.add(new JScrollPane(elementTree), BorderLayout.CENTER);
        ConfigFetch.benchmark("MissionBrowser");

        treeModel = new Model(new DefaultMutableTreeNode("Mission Elements"));
        elementTree.setModel(treeModel);
    }

    /**
     * Returns the currently selected item (may be a directory, map, vehicle, ...)
     * 
     * @return The currently selected object
     */
    public Object[] getSelectedItems() {
        TreePath[] selectionPaths = elementTree.getSelectionPaths();
        if (selectionPaths == null)
            return null;

        Vector<Object> sel = new Vector<Object>();
        for (TreePath path : selectionPaths) {
            sel.add(((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject());
        }

        return sel.toArray();
    }

    public DefaultMutableTreeNode getSelectedTreeNode() {
        if (elementTree.getSelectionPath() == null)
            return null;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) elementTree.getSelectionPath().getLastPathComponent();

        return node;
    }

    /**
     * Returns the currently selected item (may be a directory, map, vehicle, ...)
     * 
     * @return The currently selected object
     */
    public Object getSelectedItem() {
        if (elementTree.getSelectionPath() == null)
            return null;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) elementTree.getSelectionPath().getLastPathComponent();

        return node.getUserObject();
    }




    public void addTransponder(ConsoleLayout console2) {
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
        MapType pivot;
        Vector<TransponderElement> ts = MapGroup.getMapGroupInstance(mt).getAllObjectsOfType(TransponderElement.class);
        if (ts.size() > 0) {
            pivot = ts.firstElement().getParentMap();
        }
        else {
            if (mt.getMapsList().size() > 0)
                pivot = mt.getMapsList().values().iterator().next().getMap();
            else {
                MapType map = new MapType(new LocationType(mt.getHomeRef()));
                MapMission mm = new MapMission();
                mm.setMap(map);
                mt.addMap(mm);
                MapGroup.getMapGroupInstance(mt).addMap(map);
                pivot = map;
            }
        }

        TransponderElement te = new TransponderElement(MapGroup.getMapGroupInstance(mt), pivot);
        te = SimpleTransponderPanel.showTransponderDialog(te, I18n.text("New transponder properties"), true, true,
                pivot.getObjectNames(), MissionBrowser.this);
        if (te != null) {
            te.getParentMap().addObject(te);
            te.getParentMap().saveFile(te.getParentMap().getHref());
            if (console2 != null && console2.getMission() != null
                    && console2.getMission().getCompressedFilePath() != null) {
                console2.getMission().save(false);
            }
            refreshBrowser(console2.getPlan(), console2.getMission());
            ImcMsgManager.disseminate(te, "Transponder");
        }
    }

    public void editTransponder(TransponderElement elem, MissionType mission) {
        TransponderElement res = SimpleTransponderPanel.showTransponderDialog(elem,
                I18n.text("Transponder properties"), true, true, elem.getParentMap().getObjectNames(),
                MissionBrowser.this);

        if (res != null) {
            MapType pivot = elem.getParentMap();
            MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
            mce.setSourceMap(pivot);
            mce.setMapGroup(MapGroup.getMapGroupInstance(mission));
            mce.setChangedObject(elem);
            pivot.warnChangeListeners(mce);

            LinkedHashMap<String, MapMission> mapsList = mission.getMapsList();
            MapMission mm = mapsList.get(pivot.getId());
            mm.setMap(pivot);
            pivot.saveFile(mm.getHref());

            if (mission != null && mission.getCompressedFilePath() != null) {
                mission.save(false);
            }
        }
    }

    public void removeTransponder(TransponderElement elem, ConsoleLayout console2) {
        int ret = JOptionPane.showConfirmDialog(this, I18n.textf("Delete '%transponderName'?", elem.getId()),
                I18n.text("Delete"), JOptionPane.YES_NO_OPTION);
        if (ret == JOptionPane.YES_OPTION) {
            elem.getParentMap().remove(elem.getId());
            elem.getParentMap().warnChangeListeners(new MapChangeEvent(MapChangeEvent.OBJECT_REMOVED));
            elem.getParentMap().saveFile(elem.getParentMap().getHref());

            if (console2.getMission() != null) {
                if (console2.getMission().getCompressedFilePath() != null)
                    console2.getMission().save(false);
                console2.updateMissionListeners();
            }

            refreshBrowser(console2.getPlan(), console2.getMission());
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

    public void refreshBrowser(final PlanType selectedPlan, final MissionType mission) {
        if (mission == null) {
            treeModel.clearTree();
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                HomeReference homeRef = mission.getHomeRef();
                Vector<TransponderElement> trans = MapGroup.getMapGroupInstance(mission).getAllObjectsOfType(
                        TransponderElement.class);
                Collection<PlanType> plans = mission.getIndividualPlansList().values();
                PlanType plan = selectedPlan;
                treeModel.redoModel(trans, homeRef, plans, plan);

                // elementTree.expandPath(new TreePath(treeModel.trans));
                expandTree();
                // JTreeUtils.expandAll(elementTree);
                revalidate();
                repaint();
            }

        });
    }

    public void expandTree() {
        // elementTree.expandRow(0);
        expandParent((DefaultMutableTreeNode) treeModel.getRoot());
        expandParent(treeModel.trans);
        expandParent(treeModel.plans);
    }

    private void expandParent(DefaultMutableTreeNode parent) {
        if (parent.getChildCount() > 0) {
            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) parent.getFirstChild();
            elementTree.makeVisible(new TreePath(firstChild.getPath()));
        }
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
        NeptusLog.pub().debug("parsing " + url);
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
                        pivot = mission.getMapsList().values().iterator().next().getMap();
                        start.setId("start");
                        start.setName("start");
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
                        pivot = mission.getMapsList().values().iterator().next().getMap();
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


    /**
     * Removes the given item from the mission browser
     * 
     * @param item The item to be removed from this component
     */
    public void removeItem(Object item) {
        boolean isChanged = false;
        if (item instanceof MapType) {
            isChanged = treeModel.removeById((MapType) item, treeModel.maps);
        }
        else if (item instanceof PlanType) {
            isChanged = treeModel.removeById((PlanType) item, treeModel.plans);
        }
        else {
            NeptusLog.pub().error(
                    "Missing support for " + item.getClass().getCanonicalName() + " in "
                            + MissionBrowser.class.getCanonicalName() + ".removeItem()");
        }
        if (isChanged) {
            warnListeners();
        }
    }


    /**
     * Adds a new map item to this component
     * 
     * @param map The new map to add
     */
    public void addMap(MapType map) {
        // createMapsNode();
        // treeModel.insertNodeInto(new DefaultMutableTreeNode(map), maps, treeModel.getChildCount(maps));
        // JTreeUtils.expandAll(elementTree);

        treeModel.addToParents(new DefaultMutableTreeNode(map), ParentNodes.MAP);
    }

    /**
     * Adds a new plan to the component
     * 
     * @param plan A plan which will now be displayed in this component
     */
    public void addPlan(PlanType plan) {
        treeModel.addToParents(new ExtendedTreeNode(plan), ParentNodes.PLANS);

    }

    public void addTreeListener(final ConsoleLayout console2) {
        elementTree.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if (e.isAddedPath()) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) elementTree.getSelectionPath()
                            .getLastPathComponent();
                    if (node.getUserObject() instanceof PlanType) {
                        PlanType selectedPlan = (PlanType) node.getUserObject();
                        if (console2 != null)
                            console2.setPlan(selectedPlan);
                    }
                    else if (console2 != null) {
                        console2.setPlan(null);
                    }
                }
            }
        });
    }

    // /**
    // * Returns the JTree where all the items are displayed
    // *
    // * @return The JTree component that displays all the items of the mission
    // */
    // public JTree getElementTree() {
    // return elementTree;
    // }

    public Object getElementAt(int x, int y) {
        TreePath path = elementTree.getPathForLocation(x, y);
        if (path != null) {
            elementTree.setSelectionPath(path);
            return getSelectedItem();
        }
        return null;
    }

    public TreePath getPathForLocation(int x, int y) {
        return elementTree.getPathForLocation(x, y);
    }

    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    public void warnListeners() {
        ChangeEvent ce = new ChangeEvent(this);
        for (int i = 0; i < listeners.size(); i++) {
            ChangeListener listener = listeners.get(i);
            listener.stateChanged(ce);
        }
    }

    public void setSelectedPlan(PlanType plan) {
        if (getSelectedItem() == plan) {
            return;
        }

        if (plan == null) {
            return;
        }

        DefaultMutableTreeNode plans = treeModel.trans;
        if (plans != null) {
            int numPlans = treeModel.getChildCount(plans);

            for (int i = 0; i < numPlans; i++) {
                DefaultMutableTreeNode tmp = (DefaultMutableTreeNode) treeModel.getChild(plans, i);
                if (tmp.getUserObject() == plan) {
                    TreePath selPath = new TreePath(treeModel.getPathToRoot(tmp));
                    elementTree.setSelectionPath(selPath);
                    elementTree.scrollPathToVisible(selPath);
                    return;
                }
            }
        }
    }

    public TreePath[] getSelectedNodes() {
        return elementTree.getSelectionPaths();
    }

    public void setSelectedNodes(TreePath[] selectedNodes) {
        elementTree.setSelectionPaths(selectedNodes);
    }

    /**
     * @param imcSystem
     */
    public void updatePlansState(ImcSystem imcSystem) {
        try {
            if (imcSystem == null)
                return;

            PlanDBState prs = imcSystem.getPlanDBControl().getRemoteState();

            Vector<PlanType> plansLocal = new Vector<PlanType>();
            Vector<String> plansThatMatchLocal = new Vector<String>();
            Vector<ExtendedTreeNode> pathsToRemove = new Vector<ExtendedTreeNode>();

            DefaultMutableTreeNode plans = treeModel.plans;
            if (plans != null && plans.getChildCount() != 0) {
                ExtendedTreeNode childPlan = (ExtendedTreeNode) plans.getFirstChild();
                // For all plans on tree
                while (childPlan != null) {
                    if (childPlan.getUserObject() instanceof PlanType) {
                        try {
                            // Store their plan user info in plansLocal
                            PlanType plan = (PlanType) childPlan.getUserObject();
                            plansLocal.add(plan);
                            // Cross check with PlanDBControl's state
                            boolean containsPlan = prs.getStoredPlans().containsKey(plan.getId());
                            if (!containsPlan) {
                                childPlan.getUserInfo().remove("sync");
                            }
                            else {
                                // if the plan is there both Neptus and the system share the plan
                                // if md5 matches it's synced, it's not otherwise -- set the state
                                childPlan.getUserInfo().put("sync",
                                        prs.matchesRemotePlan(plan) ? State.SYNC : State.NOT_SYNC);
                                // if they share info on the plan, it's also added to plansThatMatchLocal
                                plansThatMatchLocal.add(plan.getId());
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    else if (childPlan.getUserObject() instanceof PlanDBInfo) {
                        // it's automatically remote
                        PlanDBInfo planDBInfo = (PlanDBInfo) childPlan.getUserObject();
                        // added to pathsToRemove if it's not contained in PlanDBControl's state
                        if (!prs.getStoredPlans().values().contains(planDBInfo))
                            pathsToRemove.add(childPlan);

                    }
                    childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
                }

                // remove all the plans in pathsToRemove
                for (ExtendedTreeNode extendedTreeNode : pathsToRemove) {
                    try {
                        treeModel.removeNodeFromParent(extendedTreeNode);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                pathsToRemove.clear();

                // if there are still plans
                if (!plans.isLeaf()) {
                    childPlan = (ExtendedTreeNode) plans.getFirstChild();
                    // got through remaining plans in tree - when does this happen?
                    while (childPlan != null) {
                        if (childPlan.getUserObject() instanceof PlanDBInfo) {
                            PlanDBInfo planDBInfo = (PlanDBInfo) childPlan.getUserObject();
                            boolean ct = false;
                            // compare to all localPlans
                            for (PlanType pl : plansLocal) {
                                // if one of the plans in plansLocal id matches a plan of the tree it will be removed
                                if (pl.getId().equals(planDBInfo.getPlanId())) {
                                    pathsToRemove.add(childPlan);
                                    ct = true;
                                    break;
                                }
                            }
                            // if child matched none of the plans on plansLocal it is added to plansThatMatchLocal
                            if (!ct)
                                plansThatMatchLocal.add(planDBInfo.getPlanId());
                        }

                        childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
                    }

                    // remove all the plans in pathsToRemove
                    for (ExtendedTreeNode extendedTreeNode : pathsToRemove) {
                        treeModel.removeNodeFromParent(extendedTreeNode);
                    }
                }
            }

            ArrayList<Identifiable> objectsToAdd = new ArrayList<Identifiable>();

            // Adding all the planDBInfo in store to make sure only does in store are in tree?
            // run through all the plans in PlanDBControl's storedPlans
            for (PlanDBInfo pdbi : prs.getStoredPlans().values()) {
                // if one of them matches a plan in plansThatMatchLocal it is added to objectsToAdd
                if (!plansThatMatchLocal.contains(pdbi.getPlanId())) {
                    objectsToAdd.add(pdbi);
                }
            }
            // all plans in objectsToAdd are added to the tree with state remote
            treeModel.addToParents(objectsToAdd, ParentNodes.PLANS, State.REMOTE);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void updateRemotePlansState(ImcSystem[] imcSystems) {
        NeptusLog.pub().error(
                "The method updateRemotePlansState is disabled, please review code in "
                        + MissionBrowser.class.getCanonicalName());
    }

    public void rebuildTransponderNodes(ImcSystem imcSystem) {
        try {
            if (imcSystem == null)
                return;

            DefaultMutableTreeNode trans = treeModel.trans;
            if (trans != null && trans.getChildCount() != 0) {
                LblConfig lblCfg = (LblConfig) imcSystem.retrieveData(ImcSystem.LBL_CONFIG_KEY);
                Vector<LblBeacon> beacons = lblCfg != null ? lblCfg.getBeacons() : new Vector<LblBeacon>();
                ExtendedTreeNode childTrans = (ExtendedTreeNode) trans.getFirstChild();
                int i = 0;
                while (childTrans != null) {
                    if (childTrans.getUserObject() instanceof TransponderElement) {
                        HashMap<String, Object> userInfo = childTrans.getUserInfo();
                        updateTimeElapsed(childTrans, userInfo);
                        userInfo.put("sync", State.LOCAL);

                        if (lblCfg != null && !beacons.isEmpty()) {
                            try {
                                TransponderElement transE = (TransponderElement) childTrans.getUserObject();
                                LblBeacon bc = TransponderUtils.getTransponderAsLblBeaconMessage(transE);
                                byte[] localMD5 = bc.payloadMD5();
                                for (LblBeacon b : beacons) {
                                    byte[] remoteMD5 = b.payloadMD5();
                                    if (ByteUtil.equal(localMD5, remoteMD5)) {
                                        userInfo.put("sync", State.SYNC);
                                        userInfo.put("id", i);
                                        userInfo.put("vehicle", imcSystem.getName());
                                        beacons.remove(b);
                                        break;
                                    }
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    i++;
                    childTrans = (ExtendedTreeNode) childTrans.getNextSibling();
                }
            }
            treeModel.nodeStructureChanged(trans);// --> has twitches
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void updateTimeElapsed(ExtendedTreeNode childTrans, HashMap<String, Object> userInfo) {
        ImcSystem imcSystems = ImcSystemsHolder.lookupSystemByName((String) userInfo.get("vehicle"));
        if (imcSystems != null) {
            LBLRangesTimer timer = (LBLRangesTimer) imcSystems.retrieveData(((TransponderElement) childTrans
                    .getUserObject()).getName());
            if (timer != null) {
                if (timer.isRunning()) {
                    treeModel.nodeChanged(childTrans);
                }
            }
        }
    }

    /**
     * @param id
     * @param range
     * @param timestampMillis
     * @param reason
     */
    public void updateTransponderRange(short id, double range, String mainVehicle) {
        DefaultMutableTreeNode trans = treeModel.trans;
        int childCount = trans.getChildCount();
        for (int c = 0; c < childCount; c++) {
            ExtendedTreeNode transNode = (ExtendedTreeNode) trans.getChildAt(c);
            HashMap<String, Object> userInfo = transNode.getUserInfo();
            if (((int) userInfo.get("id")) == id && ((String) userInfo.get("vehicle")).equals(mainVehicle)) {

                ImcSystem imcSystems = ImcSystemsHolder.lookupSystemByName((String) userInfo.get("vehicle"));
                if (imcSystems != null) {
                    String name = ((TransponderElement) transNode.getUserObject()).getName();
                    LBLRangesTimer timer = (LBLRangesTimer) imcSystems.retrieveData(name);
                    if (timer == null) {
                        timer = new LBLRangesTimer();
                        imcSystems.storeData(name, timer);
                    }
                    if (range == -1) {
                        timer.stopTimer();
                    }
                    else {
                        timer.resetTime();
                    }
                }
                revalidate();
                break;
            }
        }
    }

    public void stopTimers(String mainVehicle) {
        DefaultMutableTreeNode trans = treeModel.trans;
        int childCount = trans.getChildCount();
        ExtendedTreeNode transNode;
        HashMap<String, Object> userInfo;
        ImcSystem imcSystems;
        String name;
        LBLRangesTimer timer;
        for (int c = 0; c < childCount; c++) {
            transNode = (ExtendedTreeNode) trans.getChildAt(c);
            userInfo = transNode.getUserInfo();
            if (!((String) userInfo.get("vehicle")).equals(mainVehicle)) {
                imcSystems = ImcSystemsHolder.lookupSystemByName((String) userInfo.get("vehicle"));
                if (imcSystems != null) {
                    name = ((TransponderElement) transNode.getUserObject()).getName();
                    timer = (LBLRangesTimer) imcSystems.retrieveData(name);
                    if (timer != null) {
                        NeptusLog.pub().info("<###>Stoping timer for " + trans + " vehicle associated "
                                + userInfo.get("vehicle"));
                        timer.stopTimer();
                    }
                }
            }
        }
        revalidate();
    }

    /**
     * @param id
     * @param range
     * @param timestampMillis
     * @param reason
     */
    public void stopTransponderRange(String mainVehicle) {
        DefaultMutableTreeNode trans = treeModel.trans;
        int childCount = trans.getChildCount();
        for (int c = 0; c < childCount; c++) {
            ExtendedTreeNode transNode = (ExtendedTreeNode) trans.getChildAt(c);
            HashMap<String, Object> userInfo = transNode.getUserInfo();
            if (((String) userInfo.get("vehicle")).equals(mainVehicle)) {

                ImcSystem imcSystems = ImcSystemsHolder.lookupSystemByName((String) userInfo.get("vehicle"));
                if (imcSystems != null) {
                    String name = ((TransponderElement) transNode.getUserObject()).getName();
                    LBLRangesTimer timer = (LBLRangesTimer) imcSystems.retrieveData(name);
                    if (timer == null) {
                        timer = new LBLRangesTimer();
                        imcSystems.storeData(name, timer);
                    }
                    timer.stopTimer();
                }
                revalidate();
                break;
            }
        }
    }

    public enum ParentNodes {
        MAP("Maps"),
        TRANSPONDERS("Transponders"),
        PLANS("Plans"),
        // REMOTE_PLANS("Remote Plans"),
        MARKS("Marks"),
        CHECKLISTS("Checklists");

        public final String nodeName;

        private ParentNodes(String nodeName) {
            this.nodeName = nodeName;
        }
    }

    /**
     * Handles major changes in node structure.
     * 
     * @author Margarida Faria
     * 
     */
    private class Model extends DefaultTreeModel {
        private static final long serialVersionUID = 5581485271978065950L;
        private final DefaultMutableTreeNode trans, plans, maps;
        private DefaultMutableTreeNode homeR;

        // !!Important!! Always add with insertNodeInto (instead of add) and remove with removeNodeFromParent (instead
        // of remove). It will remove directly from the Vector that support the model and notify of the structure
        // changes.

        /**
         * @param root
         */
        public Model(TreeNode root) {
            super(root);
            maps = new DefaultMutableTreeNode(ParentNodes.MAP.nodeName);
            plans = new DefaultMutableTreeNode(ParentNodes.PLANS.nodeName);
            trans = new DefaultMutableTreeNode(ParentNodes.TRANSPONDERS.nodeName);
        }

        public void clearTree() {

            if (((DefaultMutableTreeNode) root).getChildCount() > 0) {
                DefaultMutableTreeNode firstLeaf = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) root)
                        .getFirstChild();
                removeNodeFromParent(firstLeaf);
            }
            if (trans.getParent() != null) {
                removeNodeFromParent(trans);
                cleanParent(ParentNodes.TRANSPONDERS);
            }
            if (plans.getParent() != null) {
                removeNodeFromParent(plans);
                cleanParent(ParentNodes.PLANS);
            }
            if (maps.getParent() != null) {
                removeNodeFromParent(maps);
                cleanParent(ParentNodes.MAP);
            }
        }

        public void redoModel(final Vector<TransponderElement> transElements, final HomeReference homeRef,
                final Collection<PlanType> plansElements, final PlanType selectedPlan) {
            clearTree();
            setHomeRef(homeRef);
            int index = 0; // homeRef is at index 0

            for (TransponderElement elem : transElements) {
                ExtendedTreeNode node = new ExtendedTreeNode(elem);
                node.getUserInfo().put("id", -1);
                node.getUserInfo().put("vehicle", "");
                addToParents(node, ParentNodes.TRANSPONDERS);
            }
            if (trans.getChildCount() >= 0 && !((DefaultMutableTreeNode) root).isNodeChild(trans)) {
                index++;
                insertNodeInto(trans, (MutableTreeNode) root, index);
            }

            for (PlanType planT : plansElements) {
                addToParents(new ExtendedTreeNode(planT), ParentNodes.PLANS);
            }
            if (plans.getChildCount() >= 0 && !plans.isNodeChild(root)) {
                index++;
                insertNodeInto(plans, (MutableTreeNode) root, index);
            }

            if (selectedPlan != null)
                setSelectedPlan(selectedPlan);

        }

        private void cleanParent(ParentNodes parent) {
            DefaultMutableTreeNode parentNode;
            switch (parent) {
                case PLANS:
                    parentNode = plans;
                    break;
                case TRANSPONDERS:
                    parentNode = trans;
                    break;
                case MAP:
                    parentNode = maps;
                    break;
                default:
                    NeptusLog.pub().error("ADD SUPPORT FOR " + parent.nodeName + " IN MissionBrowser");
                    return;
            }
            while (parentNode.getChildCount() > 0) {
                removeNodeFromParent((MutableTreeNode) parentNode.getFirstChild());
            }
        }


        private void addToParents(DefaultMutableTreeNode node, ParentNodes parent) {
            switch (parent) {
                case PLANS:
                    // plans.add(node);
                    insertNodeInto(node, plans, 0);
                    break;
                case TRANSPONDERS:
                    trans.add(node);
                    insertNodeInto(node, trans, 0);
                    break;
                case MAP:
                    maps.add(node);
                    insertNodeInto(node, maps, 0);
                    break;
                default:
                    NeptusLog.pub().error("ADD SUPPORT FOR " + parent.nodeName + " IN MissionBrowser");
                    break;
            }
        }

        private void addToParents(ArrayList<Identifiable> objectToAdd, ParentNodes parent, State state) {
            DefaultMutableTreeNode parentNode;
            switch (parent) {
                case PLANS:
                    parentNode = plans;
                    break;
                case TRANSPONDERS:
                    parentNode = trans;
                    break;
                case MAP:
                    parentNode = maps;
                    break;
                default:
                    NeptusLog.pub().error("ADD SUPPORT FOR " + parent.nodeName + " IN MissionBrowser");
                    return;
            }

            Identifiable object;
            ExtendedTreeNode newChild;
            for (Iterator<Identifiable> iterator = objectToAdd.iterator(); iterator.hasNext();) {
                object = iterator.next();
                newChild = new ExtendedTreeNode(object);
                newChild.getUserInfo().put("sync", state);
                parentNode.add(newChild);
            }
            nodeStructureChanged(parentNode);
        }

        /**
         * 
         * @param item
         * @param parent
         * @return true changes have been made
         */
        private <E extends Identifiable> boolean removeById(E item, DefaultMutableTreeNode parent) {
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                @SuppressWarnings("unchecked")
                E userObject = (E) ((DefaultMutableTreeNode) parent.getChildAt(i)).getUserObject();
                if (userObject.getIdentification().equals(item.getIdentification())) {
                    MutableTreeNode child = (MutableTreeNode) parent.getChildAt(i);
                    removeNodeFromParent(child);
                    if (childCount == 0) {
                        removeNodeFromParent(parent);
                        parent = null;
                    }
                    return true;
                }
            }
            return false;
        }

        public void setHomeRef(HomeReference href) {
            homeR = new DefaultMutableTreeNode(href);
            insertNodeInto(homeR, (MutableTreeNode) root, 0);
        }
    }

    public void setDebugOn(boolean value) {
        TreeCellRenderer cr = elementTree.getCellRenderer();
        if (cr instanceof MissionTreeCellRenderer)
            ((MissionTreeCellRenderer) cr).debugOn = value;
    }

    public void setMaxAcceptableElapsedTime(int maxAcceptableElapsedTime) {
        cellRenderer.maxAcceptableElapsedTime = maxAcceptableElapsedTime;
    }
}

