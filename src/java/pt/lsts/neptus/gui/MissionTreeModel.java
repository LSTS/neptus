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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: meg
 * Oct 31, 2013
 */
package pt.lsts.neptus.gui;

import java.text.Collator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.MissionBrowser.State;
import pt.lsts.neptus.gui.tree.ExtendedTreeNode;
import pt.lsts.neptus.gui.tree.ExtendedTreeNode.ChildIterator;
import pt.lsts.neptus.gui.tree.RootMissionTreeExtendedTreeNode;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.NameId;
import pt.lsts.neptus.types.map.TransponderElement;
import pt.lsts.neptus.types.mission.HomeReference;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * Handles major changes in node structure.
 * 
 * @author Margarida Faria
 * 
 */
public class MissionTreeModel extends DefaultTreeModel {
    private static final long serialVersionUID = 5581485271978065950L;
    private final ExtendedTreeNode trans, maps;
    private final ExtendedTreeNode plans;
    private ExtendedTreeNode homeR;

    private boolean hideTransponder = false;

    public enum ParentNodes {
        MAP(I18n.text("Maps")),
        TRANSPONDERS(I18n.text("Transponders")),
        PLANS(I18n.text("Plans")),
        MARKS(I18n.text("Marks")),
        CHECKLISTS(I18n.text("Checklists"));

        public final String nodeName;

        private ParentNodes(String nodeName) {
            this.nodeName = nodeName;
        }
    }

    public enum NodeInfoKey {
        ID,
        SYNC,
        VEHICLE;
    }
    // !!Important!! Always add with insertNodeInto (instead of add) and remove with removeNodeFromParent (instead
    // of remove). It will remove directly from the Vector that support the model and notify of the structure
    // changes.

    /**
     * @param root
     */
    public MissionTreeModel() {
        super(new RootMissionTreeExtendedTreeNode("Mission Elements"));
        maps = new ExtendedTreeNode(ParentNodes.MAP.nodeName);
        trans = new ExtendedTreeNode(ParentNodes.TRANSPONDERS.nodeName);
        ((DefaultMutableTreeNode) root).add(trans);
        plans = new ExtendedTreeNode(ParentNodes.PLANS.nodeName);
        ((DefaultMutableTreeNode) root).add(plans);
    }

    /**
     * Used only for clonning
     * 
     * @param root
     */
    private MissionTreeModel(ExtendedTreeNode maps, ExtendedTreeNode plans, ExtendedTreeNode trans) {
        super(new RootMissionTreeExtendedTreeNode("Mission Elements"));
        this.maps = maps;
        this.trans = trans;
        this.plans = plans;
    }

    @Override
    public MissionTreeModel clone() {
        ExtendedTreeNode mapsClone = maps.clone();
        mapsClone.cloneExtendedTreeNodeChildren(maps);
        ExtendedTreeNode plansClone = plans.clone();
        plansClone.cloneExtendedTreeNodeChildren(plans);
        ExtendedTreeNode transClone = trans.clone();
        transClone.cloneExtendedTreeNodeChildren(trans);
        MissionTreeModel newModel = new MissionTreeModel(mapsClone, plansClone, transClone);
        mapsClone.setParent((MutableTreeNode) newModel.root);
        plansClone.setParent((MutableTreeNode) newModel.root);
        transClone.setParent((MutableTreeNode) newModel.root);
        
        newModel.setHideTransponder(isHideTransponder());
        return newModel;
    }

    /**
     * @return the hideTransponder
     */
    public boolean isHideTransponder() {
        return hideTransponder;
    }
    
    /**
     * @param hideTransponder the hideTransponder to set
     */
    public void setHideTransponder(boolean hideTransponder) {
        this.hideTransponder = hideTransponder;
        
        if (getRoot() instanceof RootMissionTreeExtendedTreeNode) {
            if (this.hideTransponder)
                ((RootMissionTreeExtendedTreeNode) getRoot()).addToFilterOut(ParentNodes.TRANSPONDERS.nodeName);
            else
                ((RootMissionTreeExtendedTreeNode) getRoot()).removeFromFilterOut(ParentNodes.TRANSPONDERS.nodeName);
            
            reload();
        }
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

        ExtendedTreeNode parent = getParent(parentType);
        if (parent == null)
            return null;
        int nodeChildCount = getChildCount(parent);
        for (int c = 0; c < nodeChildCount; c++) {
            ExtendedTreeNode childAt = (ExtendedTreeNode) parent.getChildAt(c);
            NameId temp = (NameId) childAt.getUserObject();
            if (temp.getIdentification().equals(id)) {
                return childAt;
            }
        }
        return null;
    }

    public ExtendedTreeNode addTransponderNode(TransponderElement elem) {
        ExtendedTreeNode node = new ExtendedTreeNode(elem);
        HashMap<String, Object> transInfo = node.getUserInfo();
        transInfo.put(NodeInfoKey.ID.name(), (short) -1);
        transInfo.put(NodeInfoKey.SYNC.name(), State.LOCAL);
        transInfo.put(NodeInfoKey.VEHICLE.name(), "");
//        addToParents(node, ParentNodes.TRANSPONDERS);
        insertAlphabetically(node, ParentNodes.TRANSPONDERS);
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
     * 
     * @param newNode
     * @param treeModel
     */
    public boolean insertAlphabetically(ExtendedTreeNode newNode, ParentNodes parentType) {
        ExtendedTreeNode parent = getParent(parentType);
        if (parent == null)
            return false;
        NameId missionElem = (NameId) newNode.getUserObject();
        int nodeChildCount = getChildCount(parent);
        ExtendedTreeNode childAt;
        NameId temp;
        Collator collator = Collator.getInstance(Locale.US);
        for (int c = 0; c < nodeChildCount; c++) {
            childAt = (ExtendedTreeNode) parent.getChildAt(c);
            temp = (NameId) childAt.getUserObject();
            if (collator.compare(temp.getDisplayName(), missionElem.getDisplayName()) > 0) {
                addToParents(newNode, parentType, c);
                return true;
            }
        }
        // Add to the end (this ensures that if the parent wasn't visible in the tree before it is now
        addToParents(newNode, parentType, nodeChildCount);
        return true;
    }


    private void addToParents(ExtendedTreeNode node, ParentNodes parentType, int index) {
        ExtendedTreeNode parent = getParent(parentType);
        insertNodeInto(node, parent, index);
        // check if the parent is in the tree at this time
        boolean inTree = parent.getParent() != null;
        // System.out.print("parent " + ((inTree) ? ("is") : ("not")) + " in tree ");
        if (!inTree) {
            int parentIndex = 0;
            if (homeR.getParent() != null)
                parentIndex++;
            switch (parentType) {
                case PLANS:
                    // insert after transponder's node
                    if (trans.getParent() != null)
                        parentIndex++;
                    break;
                case TRANSPONDERS:
                    // if there is a home ref insert at 1
                    break;
                default:
                    NeptusLog.pub().error(
                            "There is no support for " + parentType.name() + " in MissionTreeModel.addToParents()");
                    break;
            }
            // System.out.print("  Gonna insert " + parentType + " at " + parentIndex);
            insertNodeInto(parent, (MutableTreeNode) root, parentIndex);
        }
    }


    /**
     * TODO change input to id and parent enumeration
     * 
     * @param item user object of the node
     * @param parent node in tree
     * @return true changes have been made
     */
    public <E extends NameId> boolean removeById(E item, ExtendedTreeNode parent) {
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
     * Delete all items.
     * 
     * @param toDelete the set of existing items.
     * @param parentType
     */
    public void removeSet(HashSet<String> toDelete, ParentNodes parentType) {
        ExtendedTreeNode parent = getParent(parentType);
        if (parent == null)
            return;
        int count = parent.getChildCount();
        int p = 0;
        ExtendedTreeNode child;
        NameId childObj;
        // System.out.println("Removing ");
        while (p < count) {
            child = (ExtendedTreeNode) parent.getChildAt(p);
            childObj = (NameId) child.getUserObject();
            String id = childObj.getIdentification();
            if (!toDelete.contains(id)) {
                removeById(childObj, parent);
                // System.out.print(childObj.getDisplayName());
                count--;
                p--;
            }
            p++;
        }
        // System.out.println();
    }

    private ExtendedTreeNode getParent(ParentNodes parentType) {
        ExtendedTreeNode parent;
        switch (parentType) {
            case PLANS:
                parent = plans;
                break;
            case TRANSPONDERS:
                parent = trans;
                break;
            default:
                NeptusLog.pub().error("ADD SUPPORT FOR " + parentType.name() + " IN Model.removeSet()");
                return null;
        }
        return parent;
    }

    /**
     * Remove a node with the given id from the given type.
     * 
     * @param id
     * @param parentType
     * @return true if the item was found and removed, false otherwise.
     */
    public <E extends NameId> boolean removeById(String id, ParentNodes parentType) {
        ExtendedTreeNode parent;
        switch (parentType) {
            case PLANS:
                parent = plans;
                break;
            case TRANSPONDERS:
                parent = trans;
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
        NeptusLog.pub().error("Could not find " + id);
        return false;
    }

    // TODO On hold until removing all beacons is stable
    // public <E extends NameId> ArrayList<E> removeAllChildren(ParentNodes parentType) {
    // ExtendedTreeNode parent;
    // switch (parentType) {
    // case PLANS:
    // parent = plans;
    // break;
    // case TRANSPONDERS:
    // parent = trans;
    // break;
    // default:
    // NeptusLog.pub().error("ADD SUPPORT FOR " + parentType.name() + " IN MissionBrowser.removeById()");
    // return null;
    // }
    // int childCount = parent.getChildCount();
    // ArrayList<E> children = new ArrayList<E>();
    // for (int i = 0; i < childCount; i++) {
    // E userObject = (E) ((ExtendedTreeNode) parent.getChildAt(i)).getUserObject();
    // children.add(userObject);
    // }
    // parent.removeAllChildren();
    // reload(parent);
    // return children;
    // }

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

    public TreePath getPlanPath(PlanType plan) {
        if (plan == null) {
            return null;
        }

        if (plans != null) {
            int numPlans = getChildCount(plans);

            for (int i = 0; i < numPlans; i++) {
                ExtendedTreeNode tmp = (ExtendedTreeNode) getChild(plans, i);
                if (tmp.getUserObject() == plan) {
                    TreePath selPath = new TreePath(getPathToRoot(tmp));
                    return selPath;
                }
            }
        }
        return null;
    }
    
    public TreePath getPathToParent(ParentNodes parentType) {
        ExtendedTreeNode parent = getParent(parentType);
        return new TreePath(parent.getPath());
    }

    public ChildIterator getIterator(ParentNodes parentType) {
        ExtendedTreeNode parent = getParent(parentType);
        if (parent == null)
            return null;
        return parent.iterator();
    }

    public void printTree(String msg, ParentNodes parentType) {
        ChildIterator parentIt;
        ExtendedTreeNode tempNode;
        NameId tempElem;
        parentIt = getIterator(parentType);
        StringBuilder treeString = new StringBuilder();
        int size = 0;
        while (parentIt.hasNext()) {
            tempNode = parentIt.next();
            tempElem = ((NameId) tempNode.getUserObject());
            treeString.append(tempElem.toString());
            treeString.append(", ");
            size++;
        }
        NeptusLog.pub().error(msg + size + " in tree:    [" + treeString + "]");
    }

    // public TransNodeIterator iterator(){
    // return new TransNodeIterator();
    // }
    //
    // private class TransNodeIterator implements Iterator<ExtendedTreeNode> {
    // int index;
    // ExtendedTreeNode childLocalTrans, parent;
    // int elemNum;
    //
    // public TransNodeIterator() {
    // index = 0;
    // elemNum = trans.getChildCount();
    // childLocalTrans = (ExtendedTreeNode) trans.getFirstChild();
    // }
    //
    // @Override
    // public boolean hasNext() {
    // return index < elemNum;
    // }
    //
    // @Override
    // public ExtendedTreeNode next() {
    // index++;
    // return (ExtendedTreeNode) childLocalTrans.getNextSibling();
    // }
    //
    // @Override
    // public void remove() {
    // trans.remove(index);
    // elemNum--;
    // }
    //
    // }
    
    /* (non-Javadoc)
     * @see javax.swing.tree.DefaultTreeModel#getChildCount(java.lang.Object)
     */
    @Override
    public int getChildCount(Object parent) {
        // TODO Auto-generated method stub
        return super.getChildCount(parent);
    }
}
