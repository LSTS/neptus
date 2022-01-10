/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Correia
 * Jul 27, 2012
 */
package pt.lsts.neptus.util.llf;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.Function;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.plots.GenericPlot;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author jqcorreia
 */
@SuppressWarnings("serial")
public class LogTree extends JTree {
    private final MRAPanel panel;

    // Root node
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");

    // Top level nodes
    private final DefaultMutableTreeNode visualizationsNode = new DefaultMutableTreeNode(I18n.text("Visualizations"));
    private final DefaultMutableTreeNode tablesNode = new DefaultMutableTreeNode(I18n.text("Tables"));
    private final DefaultMutableTreeNode chartsNode = new DefaultMutableTreeNode(I18n.text("Charts"));
    private DefaultMutableTreeNode markersNode;

    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);

    public LogTree(IMraLogGroup source, MRAPanel panel) {
        this.panel = panel;

        setModel(treeModel);
        initializeTreeRenderer();

        setRootVisible(false);
        setShowsRootHandles(true);

        initializeMouseAdapter();

        loadDefaultNodes();
        treeModel.nodeStructureChanged(root);

        // Multiple selections may only contains items of the same compatible type.
        addTreeSelectionListener((e) -> setSelectionPaths(getSelectionsFiltered(null)));
    }

    private void initializeTreeRenderer() {
        DefaultTreeCellRenderer treeRenderer = new DefaultTreeCellRenderer() {
            private final LinkedHashMap<Object, ImageIcon> iconCache = new LinkedHashMap<>();

            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                          boolean selected, boolean expanded,
                                                          boolean leaf, int row, boolean hasFocus) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

                if (node.getUserObject() instanceof MRAVisualization) {
                    MRAVisualization viz = (MRAVisualization) node.getUserObject();
                    setText(viz.getName());
                    if (!iconCache.containsKey(viz))
                        iconCache.put(viz, viz.getIcon());
                    setIcon(iconCache.get(viz));
                }

                if (node.getUserObject() instanceof LogMarker) {
                    LogMarker mark = (LogMarker) node.getUserObject();
                    setText(mark.getLabel());
                    if (!iconCache.containsKey("markers"))
                        iconCache.put("markers", ImageUtils.getIcon("images/menus/marker.png"));
                    setIcon(iconCache.get("markers"));
                }

                return this;
            }
        };

        setCellRenderer(treeRenderer);
    }

    private void initializeMouseAdapter() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final TreePath path = getPathForLocation(e.getX(), e.getY());

                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (e.getClickCount() == 2) {
                        // This takes care of clicking outside item area
                        if (path != null) {
                            DefaultMutableTreeNode n = (DefaultMutableTreeNode) path.getLastPathComponent();

                            // MRAVisualization case
                            if (n.getUserObject() instanceof MRAVisualization) {
                                panel.openVisualization(((MRAVisualization) n.getUserObject()));
                            }

                            // Log marker case.
                            if (n.getUserObject() instanceof LogMarker) {
                                panel.synchVisualizations((LogMarker) n.getUserObject());
                            }
                        }
                    }
                }

                if (e.getButton() == MouseEvent.BUTTON3) {
                    // This takes care of clicking outside item area
                    if (path != null) {
                        final JPopupMenu menu = new JPopupMenu();
                        if (initializeLogMarkerNodeMenu(menu) || initializeDynamicViewNodeMenu(menu))
                            menu.show(LogTree.this, e.getX(), e.getY());
                    }
                }

                // Let the original event go the UI Thread
                super.mouseClicked(e);
            }
        };

        addMouseListener(mouseAdapter);
    }

    /**
     * Tests if a tree node is a log marker.
     *
     * @param node tree node
     * @return true if node is a log marker, false otherwise.
     */
    private Boolean nodeIsLogMarker(final DefaultMutableTreeNode node) {
        return node.getUserObject() instanceof LogMarker;
    }

    /**
     * Tests if a tree node is a dynamic view (i.e., not a builtin/predefined).
     *
     * @param node tree node
     * @return true if node is a user item, false otherwise.
     */
    private Boolean nodeIsDynamicView(final DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        return userObject instanceof LogTableVisualization
                || userObject instanceof GenericPlot
                || userObject instanceof MessageHtmlVisualization;
    }

    /**
     * Retrieves an array of the current selected tree items. This selection shall contain only
     * items of the same compatible type.
     *
     * @param filter optional node filter.
     * @return array of tree paths.
     * @see #nodeTypeIsCompatible
     */
    private TreePath[] getSelectionsFiltered(Function<DefaultMutableTreeNode, Boolean> filter) {
        TreePath[] paths = getSelectionPaths();
        if (paths == null)
            return new TreePath[]{};

        final ArrayList<TreePath> list = new ArrayList<>();
        Object firstUserObject = null;

        for (TreePath path : paths) {
            final Object node = path.getLastPathComponent();
            final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
            final Object userObject = treeNode.getUserObject();

            if (firstUserObject == null) {
                firstUserObject = userObject;
            }

            if (!nodeTypeIsCompatible(firstUserObject, userObject) || (filter != null && !filter.apply(treeNode)))
                break;

            list.add(path);
        }

        return list.toArray(new TreePath[list.size()]);
    }

    /**
     * Tests if two objects have compatible types (i.e., are used in similar ways, have the same menu).
     * @param a first object.
     * @param b second object.
     * @return true if nodes are compatible, false otherwise.
     */
    private boolean nodeTypeIsCompatible(Object a, Object b) {
        return a.getClass().isInstance(b) || b.getClass().isInstance(a);
    }

    /**
     * Initializes a menu containing actions applicable to log markers.
     *
     * @param menu menu object.
     * @return true if menu is applicable to current selection and was initialized, false otherwise.
     */
    private boolean initializeLogMarkerNodeMenu(final JPopupMenu menu) {
        TreePath[] paths = getSelectionsFiltered(this::nodeIsLogMarker);
        if (paths.length == 0)
            return false;

        menu.add(new AbstractAction(I18n.text("Remove")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (LsfReportProperties.generatingReport) {
                    GuiUtils.infoMessage(panel.getRootPane(),
                            I18n.text("Can not remove Marks"),
                            I18n.text("Can not remove Marks - Generating Report."));
                    return;
                }

                for (TreePath path : paths) {
                    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    panel.removeMarker((LogMarker) node.getUserObject());
                }
            }
        });
        
        menu.add(new AbstractAction(I18n.text("Remane")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (LsfReportProperties.generatingReport) {
                    GuiUtils.infoMessage(panel.getRootPane(),
                            I18n.text("Can not rename Marks"),
                            I18n.text("Can not rename Marks - Generating Report."));
                    
                }
                String label = GuiUtils.input(panel.getRootPane(), "Enter Mark New Name", "mark");
                char first = label.charAt(0);
                if(!Character.isLetter(first)){
                    GuiUtils.infoMessage(panel.getRootPane(),
                            I18n.text("Can not rename Mark"),I18n.text("The name should start with a letter."));
                    actionPerformed(e);
                    return;
                }
                else {
                    for (TreePath path : paths) {
                        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        panel.renameMarker((LogMarker) node.getUserObject(), label);
                    }
                }
            }
        });

        menu.add(new AbstractAction(I18n.text("GoTo")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                final DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
                panel.synchVisualizations((LogMarker) node.getUserObject());
            }
        });

        return true;
    }

    /**
     * Initializes a menu containing actions applicable to user items.
     *
     * @param menu menu object.
     * @return true if menu is applicable to current selection and was initialized, false otherwise.
     */
    private boolean initializeDynamicViewNodeMenu(final JPopupMenu menu) {
        TreePath[] paths = getSelectionsFiltered(this::nodeIsDynamicView);
        if (paths.length == 0)
            return false;

        menu.add(new AbstractAction(I18n.text("Remove")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (TreePath path : paths) {
                    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    panel.removeTreeObject(node.getUserObject());
                }
            }
        });

        return true;
    }

    private void loadDefaultNodes() {
        root.add(visualizationsNode);
        root.add(chartsNode);
        root.add(tablesNode);
    }

    public void addVisualization(MRAVisualization vis) {
        DefaultMutableTreeNode parent;
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(vis);

        // Choose parent node based on visualization type
        switch (vis.getType()) {
            case VISUALIZATION:
                parent = visualizationsNode;
                break;
            case CHART:
                parent = chartsNode;
                break;
            case TABLE:
                parent = tablesNode;
                break;
            default:
                parent = visualizationsNode;
        }
        parent.add(node);
        treeModel.nodeStructureChanged(root);
        expandAllTree();
    }

    /**
     * Retrieves the tree node that contains charts.
     *
     * @return chart tree node.
     */
    DefaultMutableTreeNode getChartsNode() {
        return chartsNode;
    }

    private void remove(Object obj, DefaultMutableTreeNode parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (!(parent.getChildAt(i) instanceof DefaultMutableTreeNode))
                continue;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getChildAt(i);

            if (!node.isLeaf())
                remove(obj, node);
            else if (node.isLeaf()) {
                if (node.getUserObject().equals(obj)) {
                    parent.remove(node);
                    if (parent.getChildCount() == 0 && parent.getParent() instanceof DefaultMutableTreeNode) {
                        ((DefaultMutableTreeNode) parent.getParent()).remove(parent);
                    }

                    treeModel.nodeStructureChanged(root);
                    expandAllTree();
                    return;
                }
            }
        }
    }

    public void remove(Object obj) {
        remove(obj, root);
    }

    public void addMarker(LogMarker marker) {
        if (markersNode == null) {
            markersNode = new DefaultMutableTreeNode(I18n.text("Markers"));
            root.add(markersNode);
        }
        markersNode.add(new DefaultMutableTreeNode(marker));
        treeModel.nodeStructureChanged(root);
        expandAllTree();
    }

    public void removeMarker(LogMarker marker) {
        DefaultMutableTreeNode node;
        for (int i = 0; i < markersNode.getChildCount(); i++) {
            node = (DefaultMutableTreeNode) markersNode.getChildAt(i);
            if (node.getUserObject() == marker) {
                markersNode.remove(node);
            }
        }
        if (markersNode.getChildCount() == 0) {
            root.remove(markersNode);
            markersNode = null;
        }

        treeModel.nodeStructureChanged(root);
        expandAllTree();
    }

    // Util methods
    private void expandAllTree() {
        for (int i = 0; i < getRowCount(); i++) {
            expandRow(i);
        }
    }
}