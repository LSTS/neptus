/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * Jun 3, 2012
 */
package pt.up.fe.dceg.neptus.gui.tree;

import java.util.HashMap;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author pdias
 *
 */
public class ExtendedTreeNode extends DefaultMutableTreeNode {

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
}
