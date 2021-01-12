/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Ago 8, 2017
 */
package pt.lsts.neptus.gui.tree;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * @author pdias
 *
 */
public class RootMissionTreeExtendedTreeNode extends ExtendedTreeNode {

    private static final long serialVersionUID = 5480871569838721225L;
    
    private ArrayList<Object> hideWithUserObject = new ArrayList<>();

    public RootMissionTreeExtendedTreeNode() {
        super(null);
    }

    /**
     * Creates a tree node with no parent, no children, but which allows 
     * children, and initializes it with the specified user object.
     * 
     * @param userObject an Object provided by the user that constitutes
     *                   the node's data
     */
    public RootMissionTreeExtendedTreeNode(Object userObject) {
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
    public RootMissionTreeExtendedTreeNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }
    
    public void addToFilterOut(Object optOutObject) {
        if (!hideWithUserObject.contains(optOutObject)) {
            hideWithUserObject.add(optOutObject);
        }
    }

    public void removeFromFilterOut(Object optOutObject) {
        hideWithUserObject.remove(optOutObject);
    }

    @Override
    public TreeNode getChildAt(int index) {
        return getChildAt(index, false);
    }

    public TreeNode getChildAt(int index, boolean unFiltered) {
        if (unFiltered || children == null || children.isEmpty()) {
            return super.getChildAt(index);
        }

        int realIndex = -1;
        int visibleIndex = -1;
        @SuppressWarnings("rawtypes")
        Enumeration e = children.elements();
        while (e.hasMoreElements()) {
            try {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
                if (isVisible(node.getUserObject())) {
                    visibleIndex++;
                }
            }
            catch (Exception e1) {
                visibleIndex++;
            }
            realIndex++;
            if (visibleIndex == index) {
                return (TreeNode) children.elementAt(realIndex);
            }
        }

        throw new ArrayIndexOutOfBoundsException("index unmatched");
    }

    @Override
    public int getChildCount() {
        return (getChildCount(false));
    }

    public int getChildCount(boolean unFiltered) {
        if (unFiltered)
            return super.getChildCount();
        
        if (children == null || children.isEmpty()) {
            return 0;
        }

        int count = 0;
        @SuppressWarnings("rawtypes")
        Enumeration e = children.elements();
        while (e.hasMoreElements()) {
            try {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
                if (isVisible(node.getUserObject())) {
                    count++;
                }
            }
            catch (Exception e1) {
                count++;
            }
        }

        return count;
    }

    /**
     * @param userObject
     * @return
     */
    private boolean isVisible(Object userObject) {
        return !hideWithUserObject.contains(userObject);
    }
}
