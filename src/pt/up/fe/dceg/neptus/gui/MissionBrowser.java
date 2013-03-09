/*
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * 22/03/2005
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import pt.up.fe.dceg.neptus.gui.tree.ExtendedTreeNode;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.LblBeacon;
import pt.up.fe.dceg.neptus.imc.LblConfig;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBInfo;
import pt.up.fe.dceg.neptus.plugins.planning.plandb.PlanDBState;
import pt.up.fe.dceg.neptus.types.checklist.ChecklistType;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.MarkElement;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.map.TransponderUtils;
import pt.up.fe.dceg.neptus.types.misc.LBLRangesTimer;
import pt.up.fe.dceg.neptus.types.mission.ChecklistMission;
import pt.up.fe.dceg.neptus.types.mission.HomeReference;
import pt.up.fe.dceg.neptus.types.mission.MapMission;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.VehicleMission;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.ByteUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.JTreeUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * This is a visual class that displays the various items contained in a mission like
 * maps, vehicles and plans...
 * @author Jose Pinto
 * @author Paulo Dias
 */
public class MissionBrowser extends JPanel {

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

	private JTree elementTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode root, maps = null, trans = null, plans = null, vehicles = null, checklists = null,
            remotePlans = null;
    
    private int rootChildren = 0;
    private final Vector<ChangeListener> listeners = new Vector<ChangeListener>();
    
    private final String mapsNodeName = I18n.text("Maps");
    private final String transpondersNodeName = I18n.text("Transponders");
    private final String plansNodeName = I18n.text("Plans");
    private final String vehiclesNodeName = I18n.text("Vehicles");
    private final String checklistsNodeName = I18n.text("Checklists");
    private final String remotePlansNodeName = I18n.text("Remote Plans");
    
    /**
     * Creates a new mission browser which will display the items contained in the given
     * mission type
     * @param mission The MissionType whose elements are to be displayed
     */
    public MissionBrowser(MissionType mission) {
    	createInterface();
    	setMission(mission);
    }
    
    /**
     * Creates a new mission browser which will display the items contained in the given
     * mission type
     * @param mission The MissionType whose elements are to be displayed
     */
    public MissionBrowser() {
        ConfigFetch.mark("MissionBrowser");
    	createInterface();
    	ConfigFetch.benchmark("MissionBrowser");
    }
    
    /**
     * 
     */
    public void setDebugOn(boolean value) {
        TreeCellRenderer cr = elementTree.getCellRenderer();
        if (cr instanceof MissionTreeCellRenderer)
            ((MissionTreeCellRenderer) cr).debugOn = value;
    }
    
    /**
     * @return the mapsNodeName
     */
    public String getMapsNodeName() {
        return mapsNodeName;
    }
    
    /**
     * @return the transpondersNodeName
     */
    public String getTranspondersNodeName() {
        return transpondersNodeName;
    }
    
    public String getPlansNodeName() {
        return plansNodeName;
    }

    /**
     * @return the vehiclesNodeName
     */
    public String getVehiclesNodeName() {
        return vehiclesNodeName;
    }
    
    /**
     * @return the checklistsNodeName
     */
    public String getChecklistsNodeName() {
        return checklistsNodeName;
    }
    
    /**
     * @return the remotePlansNodeName
     */
    public String getRemotePlansNodeName() {
        return remotePlansNodeName;
    }
    
    private void createMapsNode() {
        if (maps == null) {
            maps = new DefaultMutableTreeNode(mapsNodeName);
            treeModel.insertNodeInto(maps, root, rootChildren++);
        }
    }

    private void createTranspondersNode() {
        if (trans == null) {
            trans = new DefaultMutableTreeNode(transpondersNodeName);
            treeModel.insertNodeInto(trans, root, rootChildren++);
        }
    }

    private void createPlansNode() {
        if (plans == null) {
            plans = new DefaultMutableTreeNode(plansNodeName);
            treeModel.insertNodeInto(plans, root, rootChildren++);
        }
    }

    private void createVehiclesNode() {
        if (vehicles == null) {
            vehicles = new DefaultMutableTreeNode(vehiclesNodeName);
            treeModel.insertNodeInto(vehicles, root, rootChildren++);
        }
    }

    private void createChecklistsNode() {
        if (checklists == null) {
            checklists = new DefaultMutableTreeNode(checklistsNodeName);
            treeModel.insertNodeInto(checklists, root, rootChildren++);
        }
    }
    
    private void createRemotePlansNode() {
        if (remotePlans == null) {
            remotePlans = new DefaultMutableTreeNode(remotePlansNodeName);
            treeModel.insertNodeInto(remotePlans, root, rootChildren++);
        }
    }

    /**
     * Removes the given item from the mission browser
     * @param item The item to be removed from this component
     */
    public void removeItem(Object item) {
    	if (item instanceof MapType) {
    		for (int i = 0; i < maps.getChildCount(); i++) {
    			MapType map = (MapType) ((DefaultMutableTreeNode)maps.getChildAt(i)).getUserObject();
    			if (map.getId().equals(((MapType)item).getId())) {
                    MutableTreeNode child = (MutableTreeNode) maps.getChildAt(i);
                    treeModel.removeNodeFromParent(child);
    			}
    			if (maps.getChildCount() == 0) {
    				treeModel.removeNodeFromParent(maps);
    				maps = null;
    				rootChildren--;
    				warnListeners();
    				return;
    			}
    		}
    	}

    	if (item instanceof PlanType) {
    		for (int i = 0; i < plans.getChildCount(); i++) {
    			PlanType planType = (PlanType) ((DefaultMutableTreeNode)plans.getChildAt(i)).getUserObject();
    			if (planType.getId().equals(((PlanType)item).getId())) {
    				treeModel.removeNodeFromParent((MutableTreeNode)plans.getChildAt(i));
    			}
    			if (plans.getChildCount() == 0) {
    				treeModel.removeNodeFromParent(plans);
    				plans = null;
    				rootChildren--;
    				warnListeners();
    				return;
    			}
    		}
    	}
    	
    	if (item instanceof ChecklistType) {
    		for (int i = 0; i < checklists.getChildCount(); i++) {
    			ChecklistType clist = (ChecklistType) ((DefaultMutableTreeNode)checklists.getChildAt(i)).getUserObject();
    			if (clist.getName().equals(((ChecklistType)item).getName())) {
    				treeModel.removeNodeFromParent((MutableTreeNode)checklists.getChildAt(i));
    			}
    			if (checklists.getChildCount() == 0) {
    				treeModel.removeNodeFromParent(checklists);
    				checklists = null;
    				rootChildren--;
    				warnListeners();
    				return;
    			}
    		}
    	}
    	warnListeners();
    }
    
    /**
     * Adds a new mouse listener to this componnent
     */
    @Override
    public synchronized void addMouseListener(MouseListener l) {
        super.addMouseListener(l);
        elementTree.addMouseListener(l);
    }
    
    /**
     * Adds a new map item to this component
     * @param map The new map to add
     */
    public void addMap(MapType map) {
        createMapsNode();
        treeModel.insertNodeInto(new DefaultMutableTreeNode(map), maps, treeModel.getChildCount(maps));
	    JTreeUtils.expandAll(elementTree);
    }
    
    /**
     * Adds a new plan to the component
     * @param plan A plan which will now be displayed in this component
     */
    public void addPlan(PlanType plan) {
        createPlansNode();
            
        treeModel.insertNodeInto(new ExtendedTreeNode(plan), plans, treeModel.getChildCount(plans));
	    JTreeUtils.expandAll(elementTree);	    	  	    
    }

    
    public void setHomeRef(HomeReference href) {
    	treeModel.insertNodeInto(new DefaultMutableTreeNode(href), root, rootChildren++);
    	JTreeUtils.expandAll(elementTree);
    }
    
    public void setStartPos(MarkElement start) {
    	if (start == null) {
    	    for (int i = 0; i < treeModel.getChildCount(root); i++) {
    	        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeModel.getChild(root, i);
    	        System.out.println(node.getUserObject());
    	        if (node.getUserObject() instanceof MarkElement) {
    	            root.remove(node);
    	            break;
    	        }
    	    }
    	    JTreeUtils.expandAll(elementTree);
    	    return;
    	}
    	
    	treeModel.insertNodeInto(new DefaultMutableTreeNode(start), root, rootChildren++);
    	JTreeUtils.expandAll(elementTree);
    }
    
    
    /**
     * Adds a new plan to the component
     * @param plan A plan wich will now be displayed in this component
     */
    public void addTransponder(TransponderElement transponder) {
        createTranspondersNode();
            
        ExtendedTreeNode node = new ExtendedTreeNode(transponder);
        node.getUserInfo().put("id", -1);
        node.getUserInfo().put("vehicle", "");

        treeModel.insertNodeInto(node, trans, treeModel.getChildCount(trans));
        JTreeUtils.expandAll(elementTree);
    }
    
    /**
     * Adds a new vehicle to the component
     * @param vehicle A vehicle that is to be added to the component
     */
    public void addVehicle(VehicleMission vehicle) {
        createVehiclesNode();
        treeModel.insertNodeInto(new DefaultMutableTreeNode(vehicle), vehicles, treeModel.getChildCount(vehicles));
        JTreeUtils.expandAll(elementTree);
    }

    /**
     * Adds a new check list instance to this component
     * @param clist The new check list to be shown here
     */
    public void addCheckList(ChecklistType clist) {
    	createChecklistsNode();
    	treeModel.insertNodeInto(new DefaultMutableTreeNode(clist), checklists, treeModel.getChildCount(checklists));
    	JTreeUtils.expandAll(elementTree);
    }

    /**
     * Returns the currently selected item (may be a directory, map, vehicle, ...)
     * @return The currently selected object 
     */
    public Object getSelectedItem() {
    	if (elementTree.getSelectionPath() == null)
    		return null;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) elementTree.getSelectionPath().getLastPathComponent();
        
        return node.getUserObject();        
    }

    public DefaultMutableTreeNode getSelectedTreeNode() {
        if (elementTree.getSelectionPath() == null)
            return null;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) elementTree.getSelectionPath().getLastPathComponent();
        
        return node;        
    }

    /**
     * Returns the currently selected item (may be a directory, map, vehicle, ...)
     * @return The currently selected object 
     */
    public Object[] getSelectedItems() {
    	if (elementTree.getSelectionPaths() == null)
    		return null;
    	
    	Vector<Object> sel = new Vector<Object>();
    	for (TreePath path : elementTree.getSelectionPaths()) {
    		  sel.add( ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject());
    	}
        
        return sel.toArray(); 
    }
    
    
    // Creates the component interface
    private void createInterface() {
        
	    root = new DefaultMutableTreeNode("Mission Elements");
	    
	    treeModel = new DefaultTreeModel(root);
	    elementTree = new JTree(treeModel);
	    elementTree.setRootVisible(false);
	    ConfigFetch.mark("MissionTreeCellRenderer");
	    elementTree.setCellRenderer(new MissionTreeCellRenderer());
	    ConfigFetch.benchmark("MissionTreeCellRenderer");
	    this.setLayout(new BorderLayout());
	    
	    this.add(new JScrollPane(elementTree), BorderLayout.CENTER);
    }
    
    
    public void clearTree() {
    	for (int i = 0; i < treeModel.getChildCount(root); i++) {
            treeModel.removeNodeFromParent((MutableTreeNode)treeModel.getChild(root, i));    
        }
        maps =  trans = plans = vehicles = checklists = remotePlans = null;
    	root = new DefaultMutableTreeNode("Mission Elements"); 	   
    	rootChildren = 0;
    	treeModel.setRoot(root);
    }
    
    /**
     * Cleans the current browser and displays only the item presented in the given mission
     * @param mission The mission whose components are to be displayed
     */
    public void setMission(MissionType mission) {
        
        clearTree();
        root.setUserObject("No mission loaded");
        
        rootChildren = 0;
        
        if (mission != null) {
            root.setUserObject("Mission Elements");

		    treeModel.insertNodeInto(new DefaultMutableTreeNode("Home Reference"), root, rootChildren++);
		    treeModel.insertNodeInto(new DefaultMutableTreeNode("Mission Information"), root, rootChildren++);
		    
		    // Adiciona os mapas existentes à árvore
		    Object[] mapList = mission.getMapsList().values().toArray();
		    if (mapList.length > 0) {
		        createMapsNode();
		    }
		    for (int i = 0; i < mapList.length; i++) {
		        treeModel.insertNodeInto(new DefaultMutableTreeNode(((MapMission) mapList[i]).getMap()), maps, i);
		    }
		    
		    // Adiciona os planos (individuais) existentes
		    Object[] planList = mission.getIndividualPlansList().values().toArray();
		    createPlansNode();
		    for (int i = 0; i < planList.length; i++) {
		        treeModel.insertNodeInto(new ExtendedTreeNode(planList[i]), plans, i);
		    }
		    
		    Object[] vehicleList = mission.getVehiclesList().values().toArray();
		    
		    if (vehicleList.length > 0) {
		        createVehiclesNode();
		    }
		    for (int i = 0; i < vehicleList.length; i++) {
		        treeModel.insertNodeInto(new DefaultMutableTreeNode(vehicleList[i]), vehicles, i);
		    }
		    
		    Object[] checks = mission.getChecklistsList().values().toArray();
		    
		    if (checks.length > 0) {
		        createChecklistsNode();
		    }
		    for (int i = 0; i < checks.length; i++) {
		        treeModel.insertNodeInto(new DefaultMutableTreeNode(((ChecklistMission)checks[i]).getChecklist()), checklists, i);
		    }
		    
		    JTreeUtils.expandAll(elementTree);
        }
        else {
            root.setUserObject("No mission Loaded");
        }
    }
    
    /**
     * Returns the JTree where all the items are displayed
     * @return The JTree component that displays all the items of the mission
     */
	public JTree getElementTree() {
		return elementTree;
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
	
	public void reload() {
		treeModel.reload();
		JTreeUtils.expandAll(elementTree);
	}
	
	
	public void setSelectedPlan(PlanType plan) {
		if (getSelectedItem() == plan) {
			return;
		}
		
		if (plan == null) {
	   		return;
		}
		
		if (plans != null) {
			int numPlans = 	treeModel.getChildCount(plans);
			
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
	
	public void addTreeSelectionListener(TreeSelectionListener listener) {
		elementTree.addTreeSelectionListener(listener);
	}
	
	public void removeTreeSelectionListener(TreeSelectionListener listener) {
		elementTree.removeTreeSelectionListener(listener);
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

	        if (plans != null && plans.getChildCount() != 0) {
	            ExtendedTreeNode childPlan = (ExtendedTreeNode) plans.getFirstChild();
	            while (childPlan != null) {
	                if (childPlan.getUserObject() instanceof PlanType) {
	                    try {
	                        PlanType plan = (PlanType) childPlan.getUserObject();
	                        plansLocal.add(plan);
	                        boolean containsPlan = prs.getStoredPlans().containsKey(plan.getId());
	                        if (!containsPlan) {
	                            childPlan.getUserInfo().remove("sync");
	                        }
	                        else {
	                            childPlan.getUserInfo().put("sync", prs.matchesRemotePlan(plan) ? State.SYNC : State.NOT_SYNC);
	                            plansThatMatchLocal.add(plan.getId());
	                        }
	                    }
	                    catch (Exception e) {
	                        e.printStackTrace();
	                    }
	                }
	                else if (childPlan.getUserObject() instanceof PlanDBInfo) {
	                    PlanDBInfo planDBInfo = (PlanDBInfo) childPlan.getUserObject();
	                    if (!prs.getStoredPlans().values().contains(planDBInfo))
	                        pathsToRemove.add(childPlan);
	                }
	                childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
	            }
	            
	            for (ExtendedTreeNode extendedTreeNode : pathsToRemove) {
	                try {
	                    treeModel.removeNodeFromParent(extendedTreeNode);
	                }
	                catch (Exception e) {
	                    e.printStackTrace();
	                }
	            }
	            pathsToRemove.clear();
	            
                if (!plans.isLeaf()) {
                    childPlan = (ExtendedTreeNode) plans.getFirstChild();
                    while (childPlan != null) {
                        if (childPlan.getUserObject() instanceof PlanDBInfo) {
                            PlanDBInfo planDBInfo = (PlanDBInfo) childPlan.getUserObject();
                            boolean ct = false;
                            for (PlanType pl : plansLocal) {
                                if (pl.getId().equals(planDBInfo.getPlanId())) {
                                    pathsToRemove.add(childPlan);
                                    ct = true;
                                    break;
                                }
	                        }
                            if (!ct)
                                plansThatMatchLocal.add(planDBInfo.getPlanId());
	                    }

                        childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
                    }

                    for (ExtendedTreeNode extendedTreeNode : pathsToRemove) {
                        treeModel.removeNodeFromParent(extendedTreeNode);
	                }
	            }
	        }
	        
	        if (plans == null)
	            createPlansNode();

	        for (PlanDBInfo pdbi : prs.getStoredPlans().values()) {
	            if (!plansThatMatchLocal.contains(pdbi.getPlanId())) {
	                ExtendedTreeNode ex = new ExtendedTreeNode(pdbi);
	                ex.getUserInfo().put("sync", State.REMOTE);
	                treeModel.insertNodeInto(ex, plans, treeModel.getChildCount(plans));
	            }
	        }
	    }
	    catch (Exception e) {
	        e.printStackTrace();
	    }

        repaint();
	}
	
	public synchronized void updateRemotePlansState(ImcSystem[] imcSystems) {
	    try {
	        if (remotePlans != null && (imcSystems == null || imcSystems.length == 0)) {
	            treeModel.removeNodeFromParent(remotePlans);
	            remotePlans = null;
	            return;
	        }
	        
	        createRemotePlansNode();
	        
	        // Adding or update systems planDBInfos
	        for (ImcSystem imcSystem : imcSystems) {
	            PlanDBState prs = imcSystem.getPlanDBControl().getRemoteState();
	            
	            ExtendedTreeNode systemRemotePlansRoot = null;
	            
	            // Find if already in the tree
	            if (remotePlans.getChildCount() != 0) {
	                ExtendedTreeNode childPlan = (ExtendedTreeNode) remotePlans.getFirstChild();
	                while (childPlan != null) {
	                    try {
	                        String id = (String) childPlan.getUserObject();
	                        if (imcSystem.getName().equalsIgnoreCase(id)) {
	                            systemRemotePlansRoot = childPlan;
	                            break;
	                        }
	                    }
	                    catch (Exception e) {
	                        e.printStackTrace();
	                    }
	                    
	                    childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
	                }
	            }
	            
	            // If not in the tree create and add system remote plan holder
	            if (systemRemotePlansRoot == null) {
	                systemRemotePlansRoot = new ExtendedTreeNode(imcSystem.getName());
	                treeModel.insertNodeInto(systemRemotePlansRoot, remotePlans, treeModel.getChildCount(remotePlans));
	            }
	            
	            // So now let's update or create planInfos for system
	            String[] planNames = prs.getStoredPlans().keySet().toArray(new String[0]);
	            PlanDBInfo[] planInfos = prs.getStoredPlans().values().toArray(new PlanDBInfo[0]);
	            for (int i = 0; i < planNames.length; i++) {
	                // look for planInfo on tree
	                ExtendedTreeNode systemPlanInfoRoot = null;
	                if (systemRemotePlansRoot.getChildCount() != 0) {
	                    ExtendedTreeNode childPlan = (ExtendedTreeNode) systemRemotePlansRoot.getFirstChild();
	                    while (childPlan != null) {
	                        try {
	                            String id = ((PlanDBInfo) childPlan.getUserObject()).getPlanId();
	                            if (planNames[i].equalsIgnoreCase(id)) {
	                                systemPlanInfoRoot = childPlan;
	                                break;
	                            }
	                        }
	                        catch (Exception e) {
	                            e.printStackTrace();
	                        }
	                        
	                        childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
	                    }
	                }
	                
	                // If not in the tree create and add
	                if (systemPlanInfoRoot != null) {
	                    systemPlanInfoRoot.setUserObject(planInfos[i]);
	                }
	                else {
	                    systemPlanInfoRoot = new ExtendedTreeNode(planInfos[i]);
	                    treeModel.insertNodeInto(systemPlanInfoRoot, systemRemotePlansRoot, treeModel.getChildCount(systemRemotePlansRoot));
	                }
	                
	                // test if this remote plan is in this console plan list
	                systemPlanInfoRoot.getUserInfo().put("sync", testPlanDBInfoForEqualityInMission(planInfos[i]));
	            }
	            
	            // see if planDBInfo is for removal
	            if (systemRemotePlansRoot.getChildCount() != 0) {
	                ExtendedTreeNode childPlan = (ExtendedTreeNode) systemRemotePlansRoot.getFirstChild();
	                while (childPlan != null) {
	                    try {
	                        String id = ((PlanDBInfo) childPlan.getUserObject()).getPlanId();
	                        for (String planId : planNames) {
	                            if (planId.equalsIgnoreCase(id)) {
	                                id = null;
	                                break;
	                            }
	                        }
	                        if (id != null) {
	                            treeModel.removeNodeFromParent(childPlan);
//                            System.out.println(id);
	                        }
	                    }
	                    catch (Exception e) {
	                        e.printStackTrace();
	                    }
	                    
	                    childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
	                }
	            }
	            
//	        JTreeUtils.expandAll(elementTree);
	        }
	        
	        // If is to remove and remove it
	        if (remotePlans != null && remotePlans.getChildCount() != 0) {
//	        System.out.println(remotePlans.getChildCount());
	            ExtendedTreeNode childPlan = (ExtendedTreeNode) remotePlans.getFirstChild();
	            while (childPlan != null) {
	                try {
	                    String id = (String) childPlan.getUserObject();
	                    for (ImcSystem imcSystem : imcSystems) {
	                        if (imcSystem.getName().equalsIgnoreCase(id)) {
	                            id = null;
	                            break;
	                        }
	                    }
	                    if (id != null) {
	                        treeModel.removeNodeFromParent(childPlan);
//                        System.out.println(id);
	                    }
	                }
	                catch (Exception e) {
	                    e.printStackTrace();
	                }
	                
	                childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
	            }
	        }
	        
	        repaint();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
	}

	/**
     * @param planDBInfo
     */
    private State testPlanDBInfoForEqualityInMission(PlanDBInfo planDBInfo) {
        if (plans == null)
            return State.REMOTE;
        
        if (plans.getChildCount() == 0)
            return State.REMOTE;
        
        ExtendedTreeNode childPlan = (ExtendedTreeNode) plans.getFirstChild();
        while (childPlan != null) {
            try {
                if (childPlan.getUserObject() instanceof PlanType) {
                    PlanType plan = (PlanType) childPlan.getUserObject();
                    boolean equals = planDBInfo.getPlanId().equalsIgnoreCase(plan.getId());
                    if (equals) {
                        byte[] localMD5 = plan.asIMCPlan().payloadMD5();
                        byte[] remoteMD5 = planDBInfo.getMd5();
                        return ByteUtil.equal(localMD5, remoteMD5) ? State.SYNC : State.NOT_SYNC;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            childPlan = (ExtendedTreeNode) childPlan.getNextSibling();
        }

        return State.REMOTE;
    }

    public void rebuildTransponderNodes(ImcSystem imcSystem) {
        try {
            if (imcSystem == null)
                return;
            
            if (trans != null && trans.getChildCount() != 0) {
                LblConfig lblCfg = (LblConfig) imcSystem.retrieveData(ImcSystem.LBL_CONFIG_KEY);
                Vector<LblBeacon> beacons = lblCfg != null ? lblCfg.getBeacons() : new Vector<LblBeacon>();
                ExtendedTreeNode childTrans = (ExtendedTreeNode) trans.getFirstChild();
                int i = 0;
                while (childTrans != null) {
                    if (childTrans.getUserObject() instanceof TransponderElement) {
                        HashMap<String, Object> userInfo = childTrans.getUserInfo();
                        updateTimeElapsed(childTrans, userInfo);
                        // userInfo.put("sync", State.LOCAL);

                        if (lblCfg != null && !beacons.isEmpty()) {
                            try {
                                TransponderElement trans = (TransponderElement) childTrans.getUserObject();
                                LblBeacon bc = TransponderUtils.getTransponderAsLblBeaconMessage(trans);
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
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        repaint();
        // treeModel.nodeStructureChanged(trans); --> has twitches
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
        
//     // LBLRanges timer = (LBLRanges) userInfo.get("timer");
//        ImcSystem imcSystems = ImcSystemsHolder.lookupSystemByName((String) userInfo.get("vehicle"));
//        if (imcSystems != null) {
//            LBLRanges timer = (LBLRanges) imcSystems.retrieveData(((TransponderElement) childTrans
//                    .getUserObject()).getName());
//            if (timer != null) {
//                if (timer.isRunning()) {
//                    treeModel.nodeChanged(childTrans);
//                }
//            }
//        }
//
//        userInfo.put("sync", State.LOCAL);
    }


    /**
     * @param id
     * @param range
     * @param timestampMillis
     * @param reason
     */
    public void updateTransponderRange(short id, double range, long timeStampMillis, String mainVehicle) {
        int childCount = trans.getChildCount();
        for (int c = 0; c < childCount; c++) {
            ExtendedTreeNode transNode = (ExtendedTreeNode) trans.getChildAt(c);
            HashMap<String, Object> userInfo = transNode.getUserInfo();
            if (((int) userInfo.get("id")) == id && ((String)userInfo.get("vehicle")).equals(mainVehicle)) {

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

    public static void main(String[] args) {
		ConfigFetch.initialize();
		MissionBrowser browser = new MissionBrowser();
		
		browser.addPlan(new PlanType(new MissionType()));
		
		GuiUtils.testFrame(browser, "title");
	}

}
