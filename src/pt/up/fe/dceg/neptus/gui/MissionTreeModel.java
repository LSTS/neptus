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
 * Author: meg
 * Oct 31, 2013
 */
package pt.up.fe.dceg.neptus.gui;

import java.util.HashMap;
import java.util.HashSet;

import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.MissionBrowser.State;
import pt.up.fe.dceg.neptus.gui.tree.ExtendedTreeNode;
import pt.up.fe.dceg.neptus.gui.tree.ExtendedTreeNode.ChildIterator;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.types.Identifiable;
import pt.up.fe.dceg.neptus.types.map.TransponderElement;
import pt.up.fe.dceg.neptus.types.mission.HomeReference;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;

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
    private MissionTreeModel(ExtendedTreeNode maps, ExtendedTreeNode plans, ExtendedTreeNode trans) {
        super(new ExtendedTreeNode("Mission Elements"));
        this.maps = maps;
        this.plans = plans;
        this.trans = trans;
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

        ExtendedTreeNode parent = getParent(parentType);
        if (parent == null)
            return null;
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

    public ExtendedTreeNode addTransponderNode(TransponderElement elem) {
        ExtendedTreeNode node = new ExtendedTreeNode(elem);
        HashMap<String, Object> transInfo = node.getUserInfo();
        transInfo.put(NodeInfoKey.ID.name(), -1);
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
     * TODO remove need for treeModel attribute. \n
     * TODO check why it is not needed to add if outside the last for
     * 
     * @param newNode
     * @param treeModel
     */
    public boolean insertAlphabetically(ExtendedTreeNode newNode, ParentNodes parentType) {
        ExtendedTreeNode parent = getParent(parentType);
        if (parent == null)
            return false;
        Identifiable plan = (Identifiable) newNode.getUserObject();
        int nodeChildCount = getChildCount(parent);
        ExtendedTreeNode childAt;
        Identifiable temp;
        for (int c = 0; c < nodeChildCount; c++) {
            childAt = (ExtendedTreeNode) parent.getChildAt(c);
            temp = (Identifiable) childAt.getUserObject();
            if (temp.getIdentification().compareTo(plan.getIdentification()) > 0) {
                addToParents(newNode, parentType, c);
                return true;
            }
        }
        System.out.print(" [insertAlphabetically] "
                + ((SwingUtilities.isEventDispatchThread() ? " is EDT " : " out of EDT ")));
        // Add to the end (this ensures that if the parent wasn't visible in the tree before it is now
        addToParents(newNode, parentType, nodeChildCount);
        return true;
    }


    private void addToParents(ExtendedTreeNode node, ParentNodes parentType, int index) {
        ExtendedTreeNode parent = getParent(parentType);
        insertNodeInto(node, parent, index);
        if (parent.getChildCount() == 1) {
            switch (parentType) {
                case PLANS:
                    insertNodeInto(parent, (MutableTreeNode) root, 2);
                    break;
                case TRANSPONDERS:
                    insertNodeInto(parent, (MutableTreeNode) root, 1);
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * TODO change input to id and parent enumeration
     * 
     * @param item user object of the node
     * @param parent node in tree
     * @return true changes have been made
     */
    public <E extends Identifiable> boolean removeById(E item, ExtendedTreeNode parent) {
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
        Identifiable childObj;
        while (p < count) {
            child = (ExtendedTreeNode) parent.getChildAt(p);
            childObj = (Identifiable) child.getUserObject();
            String id = childObj.getIdentification();
            if (!toDelete.contains(id)) {
                removeById(childObj, parent);
                System.out.println("Removing " + id);
                count--;
                p--;
            }
            p++;
        }
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
    public <E extends Identifiable> boolean removeById(String id, ParentNodes parentType) {
        ExtendedTreeNode parent;
        switch (parentType) {
            case PLANS:
                parent = plans;
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
        return parent.childIterator();
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
}
