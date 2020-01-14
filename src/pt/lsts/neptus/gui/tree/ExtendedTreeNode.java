/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * Jun 3, 2012
 */
package pt.lsts.neptus.gui.tree;

import java.util.HashMap;
import java.util.Iterator;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author pdias
 *
 */
public class ExtendedTreeNode extends DefaultMutableTreeNode implements Iterable<ExtendedTreeNode> {

    private static final long serialVersionUID = -8215761874801749660L;

    private HashMap<String, Object> userInfo = new HashMap<String, Object>();
    
    public ExtendedTreeNode() {
        super(null);
    }

    /**
     * Creates a tree node with no parent, no children, but which allows 
     * children, and initializes it with the specified user object.
     * 
     * @param userObject an Object provided by the user that constitutes
     *                   the node's data
     */
    public ExtendedTreeNode(Object userObject) {
        super(userObject, true);
    }

    /**
     * Creates a tree node with no parent, no children, initialized with
     * the specified user object, and that allows children only if
     * specified.
     * 
     * @param userObject an Object provided by the user that constitutes
     *        the node's data
     * @param allowsChildren if true, the node is allowed to have child
     *        nodes -- otherwise, it is always a leaf node
     */
    public ExtendedTreeNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    /**
     * @return the userInfo
     */
    public HashMap<String, Object> getUserInfo() {
        return userInfo;
    }

    protected void setUserInfo(HashMap<String, Object> userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    /**
     * Only clones userObject and userInfo. Parent and children are lost.
     */
    public ExtendedTreeNode clone() {
        ExtendedTreeNode clone = new ExtendedTreeNode(getUserObject());
        clone.setUserInfo(userInfo);
        return clone;
    }

    /**
     * Clones the children of a node that are of type ExtendedTreeNode
     * 
     * @param familyModel node serving as source
     */
    public void cloneExtendedTreeNodeChildren(ExtendedTreeNode familyModel) {
        ExtendedTreeNode clone;
        if (familyModel.children != null) {
            for (Object child : familyModel.children) {
                if (child instanceof ExtendedTreeNode) {
                    clone = ((ExtendedTreeNode) child).clone();
                    add(clone);
                }
            }
        }
    }

    @Override
    public ChildIterator iterator() {
        return new ChildIterator();
    }

    /**
     * 
     * @author Margarida
     * 
     */
    public class ChildIterator implements Iterator<ExtendedTreeNode> {
        int index;
        ExtendedTreeNode childLocalTrans = null;
        int elemNum;

        public ChildIterator() {
            index = 0;
            elemNum = getChildCount();
            if (elemNum != 0)
                childLocalTrans = (ExtendedTreeNode) getFirstChild();
        }

        @Override
        public boolean hasNext() {
            return index < elemNum;
        }

        @Override
        public ExtendedTreeNode next() {
            ExtendedTreeNode temp = childLocalTrans;
            index++;
            childLocalTrans = (ExtendedTreeNode) childLocalTrans.getNextSibling();
            return temp;
        }

        @Override
        public void remove() {
            ExtendedTreeNode.this.remove(index);
            elemNum--;
        }

    }
}
