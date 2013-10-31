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
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.PlanChangeListener;
import pt.up.fe.dceg.neptus.gui.tree.ExtendedTreeNode;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.LblBeacon;
import pt.up.fe.dceg.neptus.imc.LblConfig;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBInfo;
import pt.up.fe.dceg.neptus.types.Identifiable;
import pt.up.fe.dceg.neptus.types.map.MapGroup;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.misc.LBLRangesTimer;
import pt.up.fe.dceg.neptus.types.mission.HomeReference;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.ByteUtil;
import pt.up.fe.dceg.neptus.util.comm.HTTPUtils;
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
    
    private enum ParentNodes {
        MAP(I18n.text("Maps")),
        TRANSPONDERS(I18n.text("Transponders")),
        PLANS(I18n.text("Plans")),
        // REMOTE_PLANS("Remote Plans"),
        MARKS(I18n.text("Marks")),
        CHECKLISTS(I18n.text("Checklists"));

        public final String nodeName;

        private ParentNodes(String nodeName) {
            this.nodeName = nodeName;
        }
    }

    private final MissionTreeCellRenderer cellRenderer;
    private final JTree elementTree;
    final private Model treeModel;

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

        treeModel = new Model();
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
        // TODO uncomment
        NeptusLog.pub().error("getSelectedItems");
//        for (TreePath path : selectionPaths) {
//            ExtendedTreeNode node = (ExtendedTreeNode) path.getLastPathComponent();
//            Object userObject = node.getUserObject();
//            // This method is used by the send transponder button so it's important to make sure the button only see
//            // transponder elements that have the full configuration.
//            if (userObject instanceof TransponderElement) {
//                System.out.println("getSelectedItems");
//                if (!node.getUserInfo().get(NodeInfoKey.SYNC).equals(State.REMOTE)) {
//                    sel.add(userObject);
//                }
//            }
//            else {
//                sel.add(userObject);
//            }
//        }

        return sel.toArray();
    }

    public ExtendedTreeNode getSelectedTreeNode() {
        // TODO uncomment and remove this
        NeptusLog.pub().error("getSelectedTreeNode");
        return null;
        // if (elementTree.getSelectionPath() == null)
        // return null;
        // ExtendedTreeNode node = (ExtendedTreeNode) elementTree.getSelectionPath().getLastPathComponent();
        // Object userObject = node.getUserObject();
        // // This method is used by the send transponder button so it's important to make sure the button only see
        // // transponder elements that have the full configuration.
        // if (userObject instanceof TransponderElement) {
        // System.out.println("getSelectedItems");
        // if (!node.getUserInfo().get(NodeInfoKey.SYNC).equals(State.REMOTE)) {
        // return node;
        // }
        // else {
        // return null;
        // }
        // }
        // else {
        // return node;
        // }
    }

    /**
     * Returns the currently selected item (may be a directory, map, vehicle, ...)
     * 
     * @return The currently selected object
     */
    public Object getSelectedItem() {
        return getSelectedTreeNode().getUserObject();
    }




    public void addTransponderElement(ConsoleLayout console2) {
        // TODO uncomment
        NeptusLog.pub().error("addTransponderElement");
        // if (console2 == null) {
        // GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Add transponder"),
        // I18n.text("Unable to find a parent console"));
        // return;
        // }
        //
        // if (console2.getMission() == null) {
        // GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Add transponder"),
        // I18n.text("No mission opened in the console"));
        // return;
        // }
        //
        // MissionType mt = console2.getMission();
        // MapType pivot;
        // Vector<TransponderElement> ts =
        // MapGroup.getMapGroupInstance(mt).getAllObjectsOfType(TransponderElement.class);
        // if (ts.size() > 0) {
        // pivot = ts.firstElement().getParentMap();
        // }
        // else {
        // if (mt.getMapsList().size() > 0)
        // pivot = mt.getMapsList().values().iterator().next().getMap();
        // else {
        // MapType map = new MapType(new LocationType(mt.getHomeRef()));
        // MapMission mm = new MapMission();
        // mm.setMap(map);
        // mt.addMap(mm);
        // MapGroup.getMapGroupInstance(mt).addMap(map);
        // pivot = map;
        // }
        // }
        //
        // TransponderElement te = new TransponderElement(MapGroup.getMapGroupInstance(mt), pivot);
        // te = SimpleTransponderPanel.showTransponderDialog(te, I18n.text("New transponder properties"), true, true,
        // pivot.getObjectNames(), MissionBrowser.this);
        // if (te != null) {
        // te.getParentMap().addObject(te);
        // te.getParentMap().saveFile(te.getParentMap().getHref());
        // if (console2 != null && console2.getMission() != null
        // && console2.getMission().getCompressedFilePath() != null) {
        // console2.getMission().save(false);
        // }
        // // TODO refreshBrowser(console2.getPlan(), console2.getMission());
        // treeModel.addTransponderNode(te);
        // ImcMsgManager.disseminate(te, "Transponder");
        // }
    }

    public void editTransponder(TransponderElement elem, MissionType mission) {
        // TODO uncomment
        NeptusLog.pub().error("editTransponder");
        // ExtendedTreeNode selectedTreeNode = getSelectedTreeNode();
        // TransponderElement res = SimpleTransponderPanel.showTransponderDialog(elem,
        // I18n.text("Transponder properties"), true, true, elem.getParentMap().getObjectNames(),
        // MissionBrowser.this);
        //
        // if (res != null) {
        // MapType pivot = elem.getParentMap();
        // MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        // mce.setSourceMap(pivot);
        // mce.setMapGroup(MapGroup.getMapGroupInstance(mission));
        // mce.setChangedObject(elem);
        // pivot.warnChangeListeners(mce);
        //
        // LinkedHashMap<String, MapMission> mapsList = mission.getMapsList();
        // MapMission mm = mapsList.get(pivot.getId());
        // mm.setMap(pivot);
        // pivot.saveFile(mm.getHref());
        //
        // if (mission != null && mission.getCompressedFilePath() != null) {
        // mission.save(false);
        // }
        // setSyncState(selectedTreeNode, State.LOCAL);
        // treeModel.nodeChanged(selectedTreeNode);
        // }
    }

    public void removeTransponder(TransponderElement elem, ConsoleLayout console2) {
        // TODO uncomment
        NeptusLog.pub().error("removeTransponder");
        // int ret = JOptionPane.showConfirmDialog(this, I18n.textf("Delete '%transponderName'?", elem.getId()),
        // I18n.text("Delete"), JOptionPane.YES_NO_OPTION);
        // if (ret == JOptionPane.YES_OPTION) {
        // elem.getParentMap().remove(elem.getId());
        // elem.getParentMap().warnChangeListeners(new MapChangeEvent(MapChangeEvent.OBJECT_REMOVED));
        // elem.getParentMap().saveFile(elem.getParentMap().getHref());
        //
        // if (console2.getMission() != null) {
        // if (console2.getMission().getCompressedFilePath() != null)
        // console2.getMission().save(false);
        // console2.updateMissionListeners();
        // }
        // removeItem(elem);
        // }
    }

    public void swithLocationsTransponder(TransponderElement tel1, TransponderElement tel2, ConsoleLayout console2) {
        // TODO uncomment
        NeptusLog.pub().error("swithLocationsTransponder");
        // LocationType loc1 = tel1.getCenterLocation();
        // LocationType loc2 = tel2.getCenterLocation();
        // tel1.setCenterLocation(loc2);
        // tel2.setCenterLocation(loc1);
        //
        // MapType pivot = tel1.getParentMap();
        // MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        // mce.setSourceMap(pivot);
        // mce.setMapGroup(MapGroup.getMapGroupInstance(console2.getMission()));
        // mce.setChangedObject(tel1);
        // pivot.warnChangeListeners(mce);
        //
        // MapType pivot2 = tel2.getParentMap();
        // MapChangeEvent mce2 = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        // mce2.setSourceMap(pivot2);
        // mce2.setMapGroup(MapGroup.getMapGroupInstance(console2.getMission()));
        // mce2.setChangedObject(tel2);
        // pivot2.warnChangeListeners(mce2);
        //
        // LinkedHashMap<String, MapMission> mapsList = console2.getMission().getMapsList();
        // MapMission mm = mapsList.get(pivot.getId());
        // mm.setMap(pivot);
        // pivot.saveFile(mm.getHref());
        // mm = mapsList.get(pivot2.getId());
        // mm.setMap(pivot2);
        // pivot2.saveFile(mm.getHref());
        //
        // if (console2 != null && console2.getMission() != null && console2.getMission().getCompressedFilePath() !=
        // null) {
        // console2.getMission().save(false);
        // console2.updateMissionListeners();
        // }

    }


    public void refreshBrowser_(final PlanType selectedPlan, final MissionType mission, final String mainVehicleId) {
        NeptusLog.pub().error("refreshBrowser_");
        // Home ref
        treeModel.setHomeRef(mission.getHomeRef());
        // Plans
        TreeMap<String, PlanType> localPlans;
        try {
            localPlans = mission.getIndividualPlansList();
        }
        catch (NullPointerException e) {
            NeptusLog.pub().warn("I cannot find local plans for " + mainVehicleId);
            localPlans = new TreeMap<String, PlanType>();
        }
        updatePlansStateEDT(localPlans, mainVehicleId);
        // Transponders
        updateTransStateEDT(mission, mainVehicleId);
        // final Vector<TransponderElement> trans = MapGroup.getMapGroupInstance(mission).getAllObjectsOfType(
        // TransponderElement.class);
        // Collections.sort(trans);
        // SwingUtilities.invokeLater(new Runnable() {
        //
        // @Override
        // public void run() {
        // transElemSyncConfig(trans, mainVehicleId);
        // JTreeUtils.expandAll(elementTree);
        // }
        //
        // });
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
        // TODO uncomment
        NeptusLog.pub().error("setContent");
        // DataFlavor[] flavors = tr.getTransferDataFlavors();
        // for (int i = 0; i < flavors.length; i++) {
        // if (flavors[i].isMimeTypeEqual("text/plain; class=java.lang.String; charset=Unicode")) {
        // String url = null;
        //
        // try {
        // Object data = tr.getTransferData(flavors[i]);
        // if (data instanceof InputStreamReader) {
        // BufferedReader reader = new BufferedReader((InputStreamReader) data);
        // url = reader.readLine();
        // reader.close();
        // }
        // else if (data instanceof String) {
        // url = data.toString();
        // }
        //
        // return parseURL(url, mission);
        // }
        // catch (Exception e) {
        // NeptusLog.pub().error(e);
        // }
        // }
        // }
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
        // TODO uncomment
        NeptusLog.pub().error("parseContents");
        // try {
        // Document doc = DocumentHelper.parseText(file);
        // String root = doc.getRootElement().getName();
        // if (root.equalsIgnoreCase("home-reference")) {
        // HomeReference homeRef = new HomeReference();
        // boolean loadOk = homeRef.load(file);
        // if (loadOk) {
        // mission.getHomeRef().setCoordinateSystem(homeRef);
        // Vector<HomeReferenceElement> hrefElems = MapGroup.getMapGroupInstance(mission).getAllObjectsOfType(
        // HomeReferenceElement.class);
        // hrefElems.get(0).setCoordinateSystem(mission.getHomeRef());
        // mission.save(false);
        // return true;
        // }
        // }
        // else if (root.equalsIgnoreCase("StartLocation")) {
        // MarkElement start = new MarkElement(file);
        // AbstractElement[] startLocs = MapGroup.getMapGroupInstance(mission).getMapObjectsByID("start");
        // MapType pivot = null;
        //
        // if (startLocs.length == 1 && startLocs[0] instanceof MarkElement) {
        // ((MarkElement) startLocs[0]).setCenterLocation(start.getCenterLocation());
        // pivot = startLocs[0].getParentMap();
        // MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        // mce.setSourceMap(pivot);
        // mce.setMapGroup(pivot.getMapGroup());
        // mce.setChangedObject(startLocs[0]);
        // pivot.warnChangeListeners(mce);
        // }
        // else if (startLocs.length == 0) {
        // try {
        // pivot = mission.getMapsList().values().iterator().next().getMap();
        // start.setId("start");
        // start.setName("start");
        // start.setParentMap(pivot);
        // start.setMapGroup(pivot.getMapGroup());
        // pivot.addObject(start);
        // }
        // catch (Exception e) {
        // NeptusLog.pub().error(e);
        // }
        // }
        // if (pivot != null) {
        // pivot.saveFile(pivot.getHref());
        // if (mission != null && mission.getCompressedFilePath() != null) {
        // mission.save(false);
        // }
        // }
        // return true;
        // }
        // else if (root.equalsIgnoreCase("Transponder")) {
        // TransponderElement transponder = new TransponderElement(file);
        // AbstractElement[] sameId = MapGroup.getMapGroupInstance(mission).getMapObjectsByID(transponder.getId());
        // MapType pivot = null;
        //
        // if (sameId.length == 1 && sameId[0] instanceof TransponderElement) {
        // ((TransponderElement) sameId[0]).setCenterLocation(transponder.getCenterLocation());
        // ((TransponderElement) sameId[0]).setBuoyAttached(transponder.isBuoyAttached());
        // ((TransponderElement) sameId[0]).setConfiguration(transponder.getConfiguration());
        //
        // pivot = sameId[0].getParentMap();
        // MapChangeEvent mce = new MapChangeEvent(MapChangeEvent.OBJECT_CHANGED);
        // mce.setSourceMap(pivot);
        // mce.setMapGroup(pivot.getMapGroup());
        // mce.setChangedObject(sameId[0]);
        // pivot.warnChangeListeners(mce);
        // }
        // else if (sameId.length == 0) {
        // try {
        // pivot = mission.getMapsList().values().iterator().next().getMap();
        // transponder.setParentMap(pivot);
        // transponder.setMapGroup(pivot.getMapGroup());
        // pivot.addObject(transponder);
        // }
        // catch (Exception e) {
        // NeptusLog.pub().error(e);
        // }
        // }
        // if (pivot != null) {
        // pivot.saveFile(pivot.getHref());
        // if (mission != null && mission.getCompressedFilePath() != null) {
        // mission.save(false);
        // }
        // }
        // return true;
        // }
        // else if (root.equalsIgnoreCase("Plan") && mission != null) {
        // PlanType plan = new PlanType(file, mission);
        //
        // mission.getIndividualPlansList().put(plan.getId(), plan);
        //
        // if (mission != null && mission.getCompressedFilePath() != null) {
        // mission.save(false);
        // }
        // return true;
        // }
        // }
        // catch (DocumentException e) {
        // NeptusLog.pub().error(e);
        // return false;
        // }
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
            /* isChanged = */treeModel.removeById((MapType) item, treeModel.maps);
        }
        else if (item instanceof PlanType) {
            isChanged = treeModel.removeById((PlanType) item, treeModel.plans);
            if(!isChanged){
                NeptusLog.pub().error("Could not find " + ((Identifiable) item).getIdentification());
            }
        }
        else if (item instanceof PlanDBInfo) {
            isChanged = treeModel.removeById((PlanDBInfo) item, treeModel.plans);
            if (!isChanged) {
                NeptusLog.pub().error("Could not find " + ((Identifiable) item).getIdentification());
            }
        }
        else {
            NeptusLog.pub().error(
                    "Missing support for " + item.getClass().getCanonicalName() + " in "
                            + MissionBrowser.class.getCanonicalName() + ".removeItem()");
        }
    }

    public void removeplanById(String planId) {
        treeModel.removeById(planId, ParentNodes.PLANS);
    }


    public void addTreeListener(final ConsoleLayout console2) {
        elementTree.addTreeSelectionListener(new TreeSelectionListener() {

            protected Object lastSelection = null;

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                if (e.isAddedPath()) {
                    ExtendedTreeNode node = (ExtendedTreeNode) elementTree.getSelectionPath()
                            .getLastPathComponent();

                    if (node.getUserObject() == lastSelection)
                        return;

                    lastSelection = node.getUserObject();

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

    public void setSelectedPlan(PlanType plan) {
        if (getSelectedItem() == plan) {
            return;
        }

        if (plan == null) {
            return;
        }

        ExtendedTreeNode plans = treeModel.plans;
        if (plans != null) {
            int numPlans = treeModel.getChildCount(plans);

            for (int i = 0; i < numPlans; i++) {
                ExtendedTreeNode tmp = (ExtendedTreeNode) treeModel.getChild(plans, i);
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

    // /**
    // * Delete all plans not in the set.
    // *
    // * @param existingPlans the set of existing plans.
    // */
    // private void deleteDiscontinuedPlans(HashSet<String> existingPlans){
    // int planNumber = treeModel.plans.getChildCount();
    // int p = 0;
    // ExtendedTreeNode child;
    // while (p < planNumber) {
    // child = (ExtendedTreeNode) treeModel.plans.getChildAt(p);
    // Identifiable plan = (Identifiable) child.getUserObject();
    // String planId = plan.getIdentification();
    // if(!existingPlans.contains(planId)){
    // treeModel.removeById(plan, treeModel.plans);
    // System.out.println("Removing " + planId);
    // planNumber--;
    // p--;
    // }
    // p++;
    // }
    // }

    /**
     * Delete all items not in the set.
     * 
     * @param existing the set of existing items.
     */
    private void deleteDiscontinued(HashSet<String> existing, ParentNodes parentType) {
        ExtendedTreeNode parent;
        switch (parentType) {
            case PLANS:
                parent = treeModel.plans;
                break;
            case TRANSPONDERS:
                parent = treeModel.trans;
                break;
            default:
                NeptusLog.pub().error(
                        "ADD SUPPORT FOR " + parentType.name() + " IN MissionBrowser.deleteDiscontinued()");
                return;
        }
        int count = parent.getChildCount();
        int p = 0;
        ExtendedTreeNode child;
        Identifiable childObj;
        while (p < count) {
            child = (ExtendedTreeNode) parent.getChildAt(p);
            childObj = (Identifiable) child.getUserObject();
            String id = childObj.getIdentification();
            if (!existing.contains(id)) {
                treeModel.removeById(childObj, parent);
                System.out.println("Removing " + id);
                count--;
                p--;
            }
            p++;
        }
    }

    /**
     * Takes the local plans and gets the remote ones stored in the PlanDBState associated with the system and merges
     * with the current tree. The following rules are applied: - insert plans not previouly in the tree - if a plan with
     * the same id and md5 exists in both local and remote set as SYNC - if a plan with the same id exists in both local
     * and remote but different md5 set as NOT_SYNC - if a plan only exists in remote set as REMOTE - if a plan only
     * exists in local set as LOCAL
     * 
     * The merging is done in the EDT since the process of the deep copy and updating the tree after merging costs too
     * much time and memory in comparison to the small cost of doing the merge (since there are at most 20 plans
     * typically).
     * 
     * @param localPlans the plans in the mission
     * @param sysName the system to consider
     */
    public void updatePlansStateEDT(final TreeMap<String, PlanType> localPlans, final String sysName) {
        final LinkedHashMap<String, PlanDBInfo> remotePlans = getRemotePlans(sysName);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                HashSet<String> existingPlans = mergeLocalPlans(localPlans, sysName, treeModel);
                existingPlans = mergeRemotePlans(sysName, remotePlans, treeModel, existingPlans);
                deleteDiscontinued(existingPlans, ParentNodes.PLANS);
                elementTree.expandPath(new TreePath(treeModel.plans.getPath()));
                System.out.println("Nodes in Plans:" + treeModel.plans.getChildCount());
            }
        });
    }

    /**
     * Go through remote plans and alphabetically merge into nextModel.
     * 
     * @param sysName the main vehicle
     * @param remotePlans remote plans known to IMCSystem
     * @param treeModel the model where to merge
     */
    private HashSet<String> mergeRemotePlans(String sysName, LinkedHashMap<String, PlanDBInfo> remotePlans,
            Model treeModel, HashSet<String> existingPlans) {
        ExtendedTreeNode target;
        Set<String> remotePlansIds = remotePlans.keySet();
        for (String planId : remotePlansIds) {
            existingPlans.add(planId);
            target = treeModel.findNode(planId, ParentNodes.PLANS);
            PlanDBInfo remotePlan = remotePlans.get(planId);
            System.out.print(planId + "\t");
            if (target == null) {
                // If no plan exits insert as remote
                target = new ExtendedTreeNode(remotePlan);
                target.getUserInfo().put(NodeInfoKey.ID.name(), planId);
                target.getUserInfo().put(NodeInfoKey.SYNC.name(), State.REMOTE);
                treeModel.insertAlphabetically(target, treeModel, ParentNodes.PLANS);
                System.out.println(" plan from IMCSystem not found in mission tree  >> Remote.");
            }
            else {
                // Check if existing plan is PlanDBInfo
                Object existingPlan = target.getUserObject();
                if (existingPlan instanceof PlanDBInfo) {
                    target.getUserInfo().put(NodeInfoKey.SYNC.name(), State.REMOTE);
                    target.setUserObject(remotePlan);
                    System.out.println(" in tree mission is PlanDBInfo (remote type)  >> Remote.");
                }
                else if (existingPlan instanceof PlanType) {
                    PlanType existingLocalPlan = (PlanType) existingPlan;
                    // If there is already a plan use md5 to find out if it is sync
                    byte[] localMD5 = existingLocalPlan.asIMCPlan().payloadMD5();
                    byte[] remoteMD5 = remotePlan.getMd5();
                    if (ByteUtil.equal(localMD5, remoteMD5)) {
                        target.getUserInfo().put(NodeInfoKey.SYNC.name(), State.SYNC);
                        System.out.println(" in tree mission is PlanType (local type). Md5 ==,  >> Sync.");
                    }
                    else {
                        target.getUserInfo().put(NodeInfoKey.SYNC.name(), State.NOT_SYNC);
                        System.out.println(" in tree mission is PlanType (local type). Md5 !=,  >> Not_sync.");
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
     * Unable to use the same method as for plan because of the differences between LblBeacons, PlanDBInfo and PlanType.
     * 
     * @param sysName the main vehicle
     * @param remote beacon configurations known to IMCSystem
     * @param treeModel the model where to merge
     */
    private HashSet<String> mergeRemoteTrans(String sysName, LinkedHashMap<String, LblBeacon> remote,
            Model treeModel, HashSet<String> existing) {
        ExtendedTreeNode target;
        LblBeacon remoteItem;
        TransponderElement newTrans;
        Set<String> remoteIds = remote.keySet();
        for (String id : remoteIds) {
            existing.add(id);
            target = treeModel.findNode(id, ParentNodes.TRANSPONDERS);
            remoteItem = remote.get(id);
            System.out.print(id + "\t");
            if (target == null) {
                // If no plan exits insert as remote
                newTrans = new TransponderElement();
                newTrans.setId(id);
                target = new ExtendedTreeNode(remoteItem);
                target.getUserInfo().put(NodeInfoKey.ID.name(), id);
                target.getUserInfo().put(NodeInfoKey.SYNC.name(), State.REMOTE);
                treeModel.insertAlphabetically(target, treeModel, ParentNodes.TRANSPONDERS);
                System.out.println(" trans from IMCSystem not found in mission tree  >> Remote.");
            }
            else {
                   // Check if existing trans is remote
                Object existingTrans = target.getUserObject();
                if (target.getUserInfo().get(NodeInfoKey.SYNC.name()) == State.REMOTE) {
                    target.setUserObject(remoteItem);
                    System.out.println(" in tree mission was updated  >> Remote.");
                }
                else {
                    PlanType existingLocalPlan = (PlanType) existingTrans;
                    // If there is already a config use md5 to find out if it is sync
                    byte[] localMD5 = existingLocalPlan.asIMCPlan().payloadMD5();
                    byte[] remoteMD5 = remoteItem.getRawData("md5");
                    if (ByteUtil.equal(localMD5, remoteMD5)) {
                        target.getUserInfo().put(NodeInfoKey.SYNC.name(), State.SYNC);
                        System.out.println(" in tree mission is TransponderElement (local type). Md5 ==,  >> Sync.");
                    }
                    else {
                        target.getUserInfo().put(NodeInfoKey.SYNC.name(), State.NOT_SYNC);
                        System.out.println(" in tree mission is TransponderElement (local type). "
                                + "Md5 !=,  >> Not_sync.");
                    }
                }
            }
            target.getUserInfo().put(NodeInfoKey.VEHICLE.name(), sysName);
        }
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
    private HashSet<String> mergeLocalPlans(TreeMap<String, PlanType> localPlans, String sysName, Model treeModel) {
        Set<String> localPlansIds = localPlans.keySet();
        HashSet<String> existingPlans = new HashSet<String>();
        ExtendedTreeNode target, newNode;
        PlanType plan;
        for (String planId : localPlansIds) {
            existingPlans.add(planId);
            target = treeModel.findNode(planId, ParentNodes.PLANS);
            plan = localPlans.get(planId);
            System.out.print(planId + " \t");
            if (target == null) {
                // If no plan exits insert as local
                newNode = new ExtendedTreeNode(plan);
                newNode.getUserInfo().put(NodeInfoKey.ID.name(), planId);
                treeModel.insertAlphabetically(newNode, treeModel, ParentNodes.PLANS);
                target = newNode;
                System.out.print(" mission plan not found in mission tree. Creating with mission plan.");
            }
            else {
                target.setUserObject(plan);
                System.out.print(" updated plan object.");
                // not worth the troubele of checking if it is different
            }
            // Set the node to local regardless.
            // It will be checked when processing remote states.
            target.getUserInfo().put(NodeInfoKey.SYNC.name(), State.LOCAL);
            target.getUserInfo().put(NodeInfoKey.VEHICLE.name(), sysName);
            System.out.println(" Setting as local.");
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
    private HashSet<String> mergeLocal(LinkedHashMap<String, ? extends Identifiable> local, String sysName,
            Model treeModel,
            ParentNodes itemType) {
        Set<String> localIds = local.keySet();
        HashSet<String> existing = new HashSet<String>();
        ExtendedTreeNode target, newNode;
        Identifiable item;
        for (String id : localIds) {
            existing.add(id);
            target = treeModel.findNode(id, itemType);
            item = local.get(id);
            System.out.print(id + " \t");
            if (target == null) {
                // If no plan exits insert as local
                newNode = new ExtendedTreeNode(item);
                newNode.getUserInfo().put(NodeInfoKey.ID.name(), id);
                treeModel.insertAlphabetically(newNode, treeModel, itemType);
                target = newNode;
                System.out.print(itemType.name() + " not found in mission tree. Creating with mission plan.");
            }
            else {
                target.setUserObject(item);
                System.out.print(" updated " + itemType.name() + " object.");
                // not worth the troubele of checking if it is different
            }
            // Set the node to local regardless.
            // It will be checked when processing remote states.
            target.getUserInfo().put(NodeInfoKey.SYNC.name(), State.LOCAL);
            target.getUserInfo().put(NodeInfoKey.VEHICLE.name(), sysName);
            System.out.println(" Setting as local.");
        }
        return existing;
    }

    public boolean remotePlanUpdated(PlanType plan) {
        ExtendedTreeNode target = treeModel.findNode(plan.getIdentification(), ParentNodes.PLANS);
        if (target != null) {
            target.setUserObject(plan);
            target.getUserInfo().put(NodeInfoKey.SYNC.name(), State.SYNC);
            return true;
        }
        return false;
    }

    /**
     * Go to IMCSystem in the IMCSystemHolder and the plans stored in the associated PlanDBControl.
     * 
     * @param sysName the name of the system you want the plans from
     * @return the plans found, an empty map if none are found
     */
    private LinkedHashMap<String, PlanDBInfo> getRemotePlans(String sysName) {
        LinkedHashMap<String, PlanDBInfo> remotePlans;
        try {
            remotePlans = ImcSystemsHolder.lookupSystemByName(sysName).getPlanDBControl().getRemoteState()
                    .getStoredPlans();
        }
        catch (NullPointerException e) {
            NeptusLog.pub().warn("I cannot find remote plans for " + sysName);
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
    private LinkedHashMap<String, LblBeacon> getRemoteTrans(String sysName) {
        LinkedHashMap<String, LblBeacon> remoteTrans = new LinkedHashMap<String, LblBeacon>();
        try {
            Vector<LblBeacon> beacons = ((LblConfig) ImcSystemsHolder.lookupSystemByName(sysName).retrieveData(
                    ImcSystem.LBL_CONFIG_KEY)).getBeacons();
            for (LblBeacon lblBeacon : beacons) {
                remoteTrans.put(lblBeacon.getBeacon(), lblBeacon);
            }
        }
        catch (NullPointerException e) {
            NeptusLog.pub().warn("I cannot find remote beacon configuration for " + sysName);
        }
        return remoteTrans;
    }

    public synchronized void updateRemotePlansState(ImcSystem[] imcSystems) {
        NeptusLog.pub().error(
                "The method updateRemotePlansState is disabled, please review code in "
                        + MissionBrowser.class.getCanonicalName());
    }

    // public void transUpdateElapsedTime() {
    // try {
    // ExtendedTreeNode trans = treeModel.trans;
    // if (trans != null && trans.getChildCount() != 0) {
    // ExtendedTreeNode childTrans = (ExtendedTreeNode) trans.getFirstChild();
    // while (childTrans != null) {
    // if (childTrans.getUserObject() instanceof TransponderElement) {
    // HashMap<String, Object> userInfo = childTrans.getUserInfo();
    //
    // updateTimeElapsed(childTrans, userInfo);// NEEDS to run!!!
    //
    // }
    // childTrans = (ExtendedTreeNode) childTrans.getNextSibling();
    // }
    // }
    // treeModel.nodeStructureChanged(trans);// --> has twitches
    // }
    // catch (Exception e) {
    // e.printStackTrace();
    // }
    //
    // }

    private void updateTimeElapsed(ExtendedTreeNode childTrans, HashMap<String, Object> userInfo) {
        ImcSystem imcSystems = ImcSystemsHolder.lookupSystemByName((String) userInfo.get(NodeInfoKey.VEHICLE.name()));
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

    // /**
    // * If at surface, stop timer. Otherwise reset timer to 0.
    // *
    // * @param id
    // * @param mainVehicle
    // */
    // public void transUpdateTimer(short id, String mainVehicle) {
    // ExtendedTreeNode trans = treeModel.trans;
    // int childCount = trans.getChildCount();
    // for (int c = 0; c < childCount; c++) {
    // ExtendedTreeNode transNode = (ExtendedTreeNode) trans.getChildAt(c);
    // HashMap<String, Object> userInfo = transNode.getUserInfo();
    // int nodeId = (int) userInfo.get(NodeInfoKey.ID.name());
    // String transVehicle = (String) userInfo.get(NodeInfoKey.VEHICLE.name());
    // if (nodeId == id && transVehicle.equals(mainVehicle)) {
    // ImcSystem imcSystems = ImcSystemsHolder.lookupSystemByName(transVehicle);
    // if (imcSystems != null) {
    // String name = ((TransponderElement) transNode.getUserObject()).getName();
    // LBLRangesTimer timer = (LBLRangesTimer) imcSystems.retrieveData(name);
    // if (timer == null) {
    // timer = new LBLRangesTimer();
    // imcSystems.storeData(name, timer);
    // }
    // timer.resetTime();
    // }
    // revalidate();
    // break;
    // }
    // }
    // }
    //
    // /**
    // * Start all synchronized transponders associated with the vehicle.
    // *
    // * @param mainVehicle
    // */
    // public void transStartVehicleTimers(String mainVehicle) {
    // ExtendedTreeNode trans = treeModel.trans;
    // int childCount = trans.getChildCount();
    // for (int c = 0; c < childCount; c++) {
    // ExtendedTreeNode transNode = (ExtendedTreeNode) trans.getChildAt(c);
    // HashMap<String, Object> userInfo = transNode.getUserInfo();
    // String transVehicle = (String) userInfo.get(NodeInfoKey.VEHICLE.name());
    // // only looks at synchronized transponders that are linked to the designated vehicle
    // State nodeSync = (State) userInfo.get(NodeInfoKey.SYNC.name());
    // if (nodeSync == State.SYNC && transVehicle.equals(mainVehicle)) {
    // ImcSystem imcSystems = ImcSystemsHolder.lookupSystemByName(transVehicle);
    // if (imcSystems != null) {
    // String name = ((TransponderElement) transNode.getUserObject()).getName();
    // LBLRangesTimer timer = (LBLRangesTimer) imcSystems.retrieveData(name);
    // if (timer == null) {
    // timer = new LBLRangesTimer();
    // imcSystems.storeData(name, timer);
    // }
    // timer.resetTime();
    // }
    // }
    // }
    // revalidate(); // call EDT
    // }
    //
    //
    // /**
    // * Stop all transponder timers for every vehicle.
    // *
    // */
    // public void transStopTimers() {
    // ExtendedTreeNode trans = treeModel.trans;
    // int childCount = trans.getChildCount();
    // ExtendedTreeNode transNode;
    // HashMap<String, Object> transInfo;
    // ImcSystem imcSystems;
    // String name;
    // LBLRangesTimer timer;
    // // For every transponder node, get the vehicle associated with it
    // // Then retrieve the timer with the node's name in that vehicle's imcSystem hashmap.
    // for (int c = 0; c < childCount; c++) {
    // transNode = (ExtendedTreeNode) trans.getChildAt(c);
    // transInfo = transNode.getUserInfo();
    // String transVehicle = (String) transInfo.get(NodeInfoKey.VEHICLE.name());
    // imcSystems = ImcSystemsHolder.lookupSystemByName(transVehicle);
    // if (imcSystems != null) {
    // name = ((TransponderElement) transNode.getUserObject()).getName();
    // timer = (LBLRangesTimer) imcSystems.retrieveData(name);
    // if (timer != null) {
    // NeptusLog.pub().info("<###>Stoping timer for " + trans + " of " + transVehicle);
    // timer.stopTimer();
    // }
    // }
    // }
    // revalidate();
    // }

    // /**
    // * Compare current beacons with incoming beacons from vehicle. If there is a transponder configuration in the
    // * vehicle equal to a configuration in the console, it becomes SYNc. If there is none it becomes LOCAL. No
    // * transponder nodes are added.
    // *
    // * @param vehicleBeacons the incoming configuration
    // * @param vehicle
    // */
    // public void transSyncConfig(Vector<LblBeacon> vehicleBeacons, String vehicle) {
    // ExtendedTreeNode trans = treeModel.trans;
    // // Doesn't add nodes, only updates state of existing ones
    // if (trans != null && trans.getChildCount() != 0) {
    // ExtendedTreeNode childTrans = (ExtendedTreeNode) trans.getFirstChild();
    // int id = 0;
    // TransponderElement transE;
    // LblBeacon childBeacon;
    // HashMap<String, Object> userInfo;
    // byte[] localMD5, remoteMD5;
    // boolean sync;
    // while (childTrans != null) {
    // transE = (TransponderElement) childTrans.getUserObject();
    // childBeacon = TransponderUtils.getTransponderAsLblBeaconMessage(transE);
    // localMD5 = childBeacon.payloadMD5();
    // sync = false;
    // // If there is a MD5 match between the child and any incoming transponder configuration that child
    // // becomes SYNC
    // LblBeacon beacon;
    // Iterator<LblBeacon> beaconsIt = vehicleBeacons.iterator();
    // while(beaconsIt.hasNext()){
    // beacon = beaconsIt.next();
    // remoteMD5 = beacon.payloadMD5();
    // if (ByteUtil.equal(localMD5, remoteMD5)) {
    // userInfo = childTrans.getUserInfo();
    // userInfo.put(NodeInfoKey.SYNC.name(), State.SYNC);
    // userInfo.put(NodeInfoKey.ID.name(), id);
    // userInfo.put(NodeInfoKey.VEHICLE.name(), vehicle);
    // treeModel.nodeStructureChanged(childTrans);
    // beaconsIt.remove();
    // sync = true;
    // }
    // }
    // // If no match is found the child becomes LOCAL
    // if (!sync) {
    // userInfo = childTrans.getUserInfo();
    // userInfo.put(NodeInfoKey.SYNC.name(), State.LOCAL);
    // }
    // id++;
    // childTrans = (ExtendedTreeNode) childTrans.getNextSibling();
    // }
    // revalidate();
    // }
    // }
    //
    // /**
    // * This is duplicate with update from IMC Message because of the difference between TransponderElement (which has
    // no
    // * md5) and LblBeacon (which has). This is duplicate with plan update because data types are not uniform
    // * (PlanDBInfo, PlanType, TransElement, LblConfig)
    // *
    // * @param remoteTrans
    // * @param vehicle
    // */
    // public void transElemSyncConfig(Vector<TransponderElement> remoteTrans, String vehicle) {
    // ExtendedTreeNode transParentNode = treeModel.trans;
    // TransponderElement remoteBeacon;
    // byte[] localMD5, remoteMD5;
    // HashMap<String, Object> userInfo;
    // Iterator<TransponderElement> remoteBeaconsIt = remoteTrans.iterator();
    // if (transParentNode != null && transParentNode.getChildCount() != 0) {
    // TransponderElement localTransElem;
    // int id = 0;
    // ExtendedTreeNode childLocalTrans = (ExtendedTreeNode) transParentNode.getFirstChild();
    // while (childLocalTrans != null) {
    // localTransElem = (TransponderElement) childLocalTrans.getUserObject();
    // localMD5 = localTransElem.getMd5();
    // userInfo = childLocalTrans.getUserInfo();
    // userInfo.put(NodeInfoKey.SYNC.name(), State.LOCAL);
    // System.out.print(localTransElem.getIdentification());
    // // If there is a MD5 match between the child and any incoming transponder configuration that child
    // // becomes SYNC
    // while(remoteBeaconsIt.hasNext()){
    // remoteBeacon = remoteBeaconsIt.next();
    // remoteMD5 = remoteBeacon.getMd5();
    // if (ByteUtil.equal(localMD5, remoteMD5)) {
    // userInfo.put(NodeInfoKey.SYNC.name(), State.SYNC);
    // userInfo.put(NodeInfoKey.ID.name(), id);
    // userInfo.put(NodeInfoKey.VEHICLE.name(), vehicle);
    // treeModel.nodeStructureChanged(childLocalTrans);
    // remoteBeaconsIt.remove();
    // System.out.print(" is sync.");
    // break;
    // }
    // }
    // System.out.println();
    // id++;
    // childLocalTrans = (ExtendedTreeNode) childLocalTrans.getNextSibling();
    // }
    // }
    // ExtendedTreeNode newNode;
    // while (remoteBeaconsIt.hasNext()) {
    // remoteBeacon = remoteBeaconsIt.next();
    // newNode = treeModel.addTransponderNode(remoteBeacon);
    // userInfo = newNode.getUserInfo();
    // userInfo.put(NodeInfoKey.SYNC.name(), State.LOCAL);
    // userInfo.put(NodeInfoKey.ID.name(), remoteBeacon.getIdentification());
    // userInfo.put(NodeInfoKey.VEHICLE.name(), vehicle);
    // System.out.println("Adding " + remoteBeacon.getIdentification());
    // }
    // revalidate();
    // }

    public void updateTransStateEDT(MissionType mission, final String sysName) {
        final LinkedHashMap<String, TransponderElement> localTrans = getLocalTrans(mission);
        final LinkedHashMap<String, LblBeacon> remoteTrans = getRemoteTrans(sysName);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                HashSet<String> existingTrans = mergeLocal(localTrans, sysName, treeModel, ParentNodes.TRANSPONDERS);
                // existingTrans = mergeRemoteTrans(sysName, remoteTrans, treeModel, existingTrans);
                // deleteDiscontinued(existingTrans, ParentNodes.TRANSPONDERS);
                elementTree.expandPath(new TreePath(treeModel.plans.getPath()));
                System.out.println("Nodes in Plans:" + treeModel.plans.getChildCount());
            }
        });
    }

    /**
     * Get all the transponder elements associated with the mission.
     * 
     * @param mission current mission
     * @return the found elements
     */
    @SuppressWarnings("unchecked")
    private <T extends Identifiable> LinkedHashMap<String, T> getLocalTrans(MissionType mission) {
        LinkedHashMap<String, T> map = new LinkedHashMap<String, T>();
        Vector<T> vector;
        try {
            vector = (Vector<T>) MapGroup.getMapGroupInstance(mission).getAllObjectsOfType(TransponderElement.class);
            for (T transponderElement : vector) {
                map.put(transponderElement.getIdentification(), transponderElement);
            }
        }
        catch (NullPointerException e) {
            NeptusLog.pub().warn("I cannot find local trans for main vehicle");
        }
        return map;
    }

    private void setSyncState(ExtendedTreeNode child, State state) {
        child.getUserInfo().put(NodeInfoKey.SYNC.name(), state);
        repaint();
    }

    public <T extends Identifiable> void removeCurrSelectedNodeRemotely() {
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
                    setSyncState(selectionNode, State.LOCAL);
                break;
            case REMOTE:
                // Disappear
                Object selectedItem = getSelectedItem();
                    removeItem(selectedItem);
                break;
            case LOCAL:
                // Invalid
                NeptusLog.pub().error("Invalid removal of local plan");
                break;
            default:
                NeptusLog.pub().error("Invalid local removal of plan with unkown state");
        }
    }

    public <T extends Identifiable> void deleteCurrSelectedNodeLocally() {
        ExtendedTreeNode selectionNode = getSelectedTreeNode();
        State syncState = (State) selectionNode.getUserInfo().get(NodeInfoKey.SYNC.name());
        switch (syncState) {
            case NOT_SYNC:
                // To Remote
            case SYNC:
                // Remote
                PlanDBInfo remoteNode = new PlanDBInfo();
                remoteNode.setPlanId(((PlanType) getSelectedItem()).getIdentification());
                ExtendedTreeNode node = new ExtendedTreeNode(remoteNode);
                setSyncState(node, State.REMOTE);
                treeModel.insertAlphabetically(node, treeModel, ParentNodes.PLANS);
            case LOCAL:
                // Disappear
                removeItem(getSelectedItem());
                break;
            case REMOTE:
                // Invalid
                NeptusLog.pub().error("Invalid local removal of remote plan");
                break;
            default:
                NeptusLog.pub().error("Invalid local removal of plan with unkown state");
        }
    }

    public void setPlanAsSync(String planId) {
        ExtendedTreeNode plan = treeModel.findNode(planId, ParentNodes.PLANS);
        if (plan != null) {
            setSyncState(plan, State.SYNC);
        }
    }

    public enum NodeInfoKey {
        ID,
        SYNC,
        VEHICLE;
    }

    /**
     * Handles major changes in node structure.
     * 
     * @author Margarida Faria
     * 
     */
    private class Model extends DefaultTreeModel {
        private static final long serialVersionUID = 5581485271978065950L;
        private final ExtendedTreeNode trans, maps;
        private final ExtendedTreeNode plans;
        private ExtendedTreeNode homeR;


        // !!Important!! Always add with insertNodeInto (instead of add) and remove with removeNodeFromParent (instead
        // of remove). It will remove directly from the Vector that support the model and notify of the structure
        // changes.

        /**
         * @param root
         */
        public Model() {
            super(new ExtendedTreeNode("Mission Elements"));
            maps = new ExtendedTreeNode(ParentNodes.MAP.nodeName);
            plans = new ExtendedTreeNode(ParentNodes.PLANS.nodeName);
            trans = new ExtendedTreeNode(ParentNodes.TRANSPONDERS.nodeName);
        }

        /**
         * Used only for clonning
         * 
         * @param root
         */
        private Model(ExtendedTreeNode maps, ExtendedTreeNode plans, ExtendedTreeNode trans) {
            super(new ExtendedTreeNode("Mission Elements"));
            this.maps = maps;
            this.plans = plans;
            this.trans = trans;
        }

        @Override
        public Model clone() {
            ExtendedTreeNode mapsClone = maps.clone();
            mapsClone.cloneExtendedTreeNodeChildren(maps);
            ExtendedTreeNode plansClone = plans.clone();
            plansClone.cloneExtendedTreeNodeChildren(plans);
            ExtendedTreeNode transClone = trans.clone();
            transClone.cloneExtendedTreeNodeChildren(trans);
            Model newModel = new Model(mapsClone, plansClone, transClone);
            mapsClone.setParent((MutableTreeNode) newModel.root);
            plansClone.setParent((MutableTreeNode) newModel.root);
            transClone.setParent((MutableTreeNode) newModel.root);
            return newModel;
        }

        // /**
        // * Searched for a plan with this id in the nodes of the tree.
        // *
        // * @param id of the plan
        // * @return the node of the plan or null if none is found
        // */
        // public ExtendedTreeNode findPlan(String id) {
        // int nodeChildCount = getChildCount(plans);
        // for (int c = 0; c < nodeChildCount; c++) {
        // ExtendedTreeNode childAt = (ExtendedTreeNode) plans.getChildAt(c);
        // Identifiable tempPlan = (Identifiable) childAt.getUserObject();
        // if (tempPlan.getIdentification().equals(id)) {
        // return childAt;
        // }
        // }
        // return null;
        // }

        /**
         * Searched for a node with this id in the nodes of the parent type.
         * 
         * @param id of the plan
         * @param parentType of the node
         * @return the node with the same id or null if none is found
         */
        public ExtendedTreeNode findNode(String id, ParentNodes parentType) {
            ExtendedTreeNode parent;
            switch (parentType) {
                case PLANS:
                    parent = treeModel.plans;
                    break;
                case TRANSPONDERS:
                    parent = treeModel.trans;
                    break;
                default:
                    NeptusLog.pub().error(
                            "ADD SUPPORT FOR " + parentType.name() + " IN MissionBrowser.insertAlphabetically()");
                    return null;
            }
            int nodeChildCount = getChildCount(parent);
            for (int c = 0; c < nodeChildCount; c++) {
                ExtendedTreeNode childAt = (ExtendedTreeNode) parent.getChildAt(c);
                Identifiable temp = (Identifiable) childAt.getUserObject();
                if (temp.getIdentification().equals(id)) {
                    return childAt;
                }
            }
            return null;
        }

        private ExtendedTreeNode addTransponderNode(TransponderElement elem) {
            ExtendedTreeNode node = new ExtendedTreeNode(elem);
            HashMap<String, Object> transInfo = node.getUserInfo();
            transInfo.put(NodeInfoKey.ID.name(), -1);
            transInfo.put(NodeInfoKey.SYNC.name(), State.LOCAL);
            transInfo.put(NodeInfoKey.VEHICLE.name(), "");
            addToParents(node, ParentNodes.TRANSPONDERS);
            return node;
        }
        
        // /**
        // * TODO remove need for treeModel attribute.
        // *
        // *
        // * @param newNode
        // * @param treeModel
        // */
        // private void insertPlanAlphabetically(ExtendedTreeNode newNode, Model treeModel) {
        // Identifiable plan = (Identifiable) newNode.getUserObject();
        // int nodeChildCount = getChildCount(treeModel.plans);
        // ExtendedTreeNode childAt;
        // Identifiable tempPlan;
        // for (int c = 0; c < nodeChildCount; c++) {
        // childAt = (ExtendedTreeNode) treeModel.plans.getChildAt(c);
        // tempPlan = (Identifiable) childAt.getUserObject();
        // if (tempPlan.getIdentification().compareTo(plan.getIdentification()) > 0) {
        // insertNodeInto(newNode, treeModel.plans, c);
        // return;
        // }
        // }
        // System.out.print(" [addToParents] ");
        // treeModel.addToParents(newNode, ParentNodes.PLANS);
        // }
        
        /**
         * TODO remove need for treeModel attribute. \n
         * TODO check why it is not needed to add if outside the last for
         * 
         * @param newNode
         * @param treeModel
         */
        private boolean insertAlphabetically(ExtendedTreeNode newNode, Model treeModel, ParentNodes parentType) {
            ExtendedTreeNode parent;
            switch (parentType) {
                case PLANS:
                    parent = treeModel.plans;
                    break;
                case TRANSPONDERS:
                    parent = treeModel.trans;
                    Thread.dumpStack();
                    break;
                default:
                    NeptusLog.pub().error(
                            "ADD SUPPORT FOR " + parentType.name() + " IN MissionBrowser.insertAlphabetically()");
                    return false;
            }
            Identifiable plan = (Identifiable) newNode.getUserObject();
            int nodeChildCount = getChildCount(parent);
            ExtendedTreeNode childAt;
            Identifiable temp;
            boolean inserted = false;
            for (int c = 0; c < nodeChildCount && !inserted; c++) {
                childAt = (ExtendedTreeNode) parent.getChildAt(c);
                temp = (Identifiable) childAt.getUserObject();
                if (temp.getIdentification().compareTo(plan.getIdentification()) > 0) {
                    insertNodeInto(newNode, parent, c);
                    inserted = true;
                }
            }
            System.out.println(" [addToParents] "
                    + ((SwingUtilities.isEventDispatchThread() ? " is EDT " : " out of EDT ")));
            // Add to the end (this ensures that if the parent wasn't visible in the tree before it is now
            treeModel.addToParents(newNode, parentType);
            return inserted;
        }

        private void addToParents(ExtendedTreeNode node, ParentNodes parentType) {
            ExtendedTreeNode parent = null;
            int index = -1;
            switch (parentType) {
                case PLANS:
                    insertNodeInto(node, plans, plans.getChildCount());
                    if (plans.getChildCount() == 1) {
                        parent = plans;
                        index = root.getChildCount();
                    }
                    break;
                case TRANSPONDERS:
                    int childCount = trans.getChildCount();

                    try {
                        insertNodeInto(node, trans, childCount);
                    }
                    catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if (trans.getChildCount() == 1) {
                        parent = trans;
                        index = 1;
                    }
                    break;
                default:
                    NeptusLog.pub().error("ADD SUPPORT FOR " + parentType.nodeName + " IN MissionBrowser");
                    return;
            }

            if (index != -1) {
                System.out.println("Adding " + parentType.name() + " at index " + index);
                insertNodeInto(parent, (MutableTreeNode) root, index);
            }
        }


        /**
         * TODO change input to id and parent enumeration
         * 
         * @param item user object of the node
         * @param parent node in tree
         * @return true changes have been made
         */
        private <E extends Identifiable> boolean removeById(E item, ExtendedTreeNode parent) {
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                @SuppressWarnings("unchecked")
                E userObject = (E) ((ExtendedTreeNode) parent.getChildAt(i)).getUserObject();
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

        /**
         * Remove a node with the given id from the given type.
         * 
         * @param id
         * @param parentType
         * @return true if the item was found and removed, false otherwise.
         */
        private <E extends Identifiable> boolean removeById(String id, ParentNodes parentType) {
            ExtendedTreeNode parent;
            switch (parentType) {
                case PLANS:
                    parent = treeModel.plans;
                    break;
                default:
                    NeptusLog.pub().error("ADD SUPPORT FOR " + parentType.name() + " IN MissionBrowser.removeById()");
                    return false;
            }
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                @SuppressWarnings("unchecked")
                E userObject = (E) ((ExtendedTreeNode) parent.getChildAt(i)).getUserObject();
                if (userObject.getIdentification().equals(id)) {
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
            // insert if root has no children or if the first child is not Home Reference
            if (root.getChildCount() == 0
                    || !(((ExtendedTreeNode) root.getChildAt(0)).getUserObject() instanceof HomeReference)) {
                homeR = new ExtendedTreeNode(href);
                insertNodeInto(homeR, (MutableTreeNode) root, 0);
            }
            else {
                homeR.setUserObject(href);
            }
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