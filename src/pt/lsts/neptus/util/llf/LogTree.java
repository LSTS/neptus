/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: José Correia
 * Jul 27, 2012
 */
package pt.lsts.neptus.util.llf;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import pt.lsts.neptus.NeptusLog;
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
 *
 */
@SuppressWarnings("serial")
public class LogTree extends JTree {

    MRAPanel panel;

    LinkedHashMap<String, Component> visList = new LinkedHashMap<String, Component>();
    IMraLogGroup source;

    private
    // Root node
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");

    // Top level nodes
    DefaultMutableTreeNode visualizationsNode = new DefaultMutableTreeNode(I18n.text("Visualizations"));
    private DefaultMutableTreeNode chartsNode = new DefaultMutableTreeNode(I18n.text("Charts"));
    DefaultMutableTreeNode tablesNode = new DefaultMutableTreeNode(I18n.text("Tables"));
    DefaultMutableTreeNode markersNode;

    // Misc Nodes
    DefaultMutableTreeNode newPlotNode = new DefaultMutableTreeNode(I18n.text("New Plot"));

    DefaultTreeModel treeModel = new DefaultTreeModel(root);
    DefaultTreeCellRenderer treeRenderer = new DefaultTreeCellRenderer() {

        private LinkedHashMap<Object, ImageIcon> iconCache = new LinkedHashMap<>();

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {



            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if(node.getUserObject() instanceof MRAVisualization) {
                MRAVisualization viz = (MRAVisualization) node.getUserObject();
                setText(viz.getName());
                if (!iconCache.containsKey(viz))
                    iconCache.put(viz, viz.getIcon());
                setIcon(iconCache.get(viz));
            }
            if(node.getUserObject() instanceof LogMarker) {
                LogMarker mark = (LogMarker) node.getUserObject();
                setText(mark.getLabel());
                if (!iconCache.containsKey("markers"))
                    iconCache.put("markers", ImageUtils.getIcon("images/menus/marker.png"));
                setIcon(iconCache.get("markers"));
            }

            return this;
        }
    };

    MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getButton()==MouseEvent.BUTTON1) {

                if(e.getClickCount()==1) {
                    // Single Click
                    // TODO
                }
                if(e.getClickCount()==2){
                    // Double Click
                    TreePath path = getPathForLocation(e.getX(), e.getY());

                    // This takes care of clicking outside item area
                    if (path != null) {
                        DefaultMutableTreeNode n = (DefaultMutableTreeNode) path.getLastPathComponent();

                        // MRAVisualization case
                        if (n.getUserObject() instanceof MRAVisualization) {
                            panel.openVisualization(((MRAVisualization) n.getUserObject()));
                        }
                        // 'New plot' case
                        if (n == newPlotNode ) {
                            NeptusLog.pub().info("<###>New Plot");
                        }

                        // MRAVisualization case
                        if (n.getUserObject() instanceof LogMarker) {
                            panel.synchVisualizations((LogMarker)n.getUserObject());
                        }

                        //TODO more case to follow
                    }
                }
            }
            if(e.getButton() == MouseEvent.BUTTON3) {
                JPopupMenu menu = new JPopupMenu();
                TreePath path = getPathForLocation(e.getX(), e.getY());
                setSelectionPath(path);
                boolean showMenu = false;

                // This takes care of clicking outside item area
                if (path != null) {
                    final DefaultMutableTreeNode n = (DefaultMutableTreeNode) path.getLastPathComponent();

                    // MRAVisualization case
                    if (n.getUserObject() instanceof MRAVisualization) {

                    }
                    if(n.getUserObject() instanceof LogMarker) {
                        menu.add(new AbstractAction(I18n.text("Remove")) {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (LsfReportProperties.generatingReport==true){
                                    GuiUtils.infoMessage(panel.getRootPane(), I18n.text("Can not remove Marks"), I18n.text("Can not remove Marks - Generating Report."));
                                    return;
                                }
                                panel.removeMarker((LogMarker)n.getUserObject());
                            }
                        });

                        menu.add(new AbstractAction(I18n.text("GoTo")) {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                panel.synchVisualizations((LogMarker)n.getUserObject());
                            }
                        });
                        showMenu = true;
                    }
                    else if (n.getUserObject() instanceof LogTableVisualization ||
                            n.getUserObject() instanceof GenericPlot ||
                            n.getUserObject() instanceof MessageHtmlVisualization) {

                        menu.add(new AbstractAction(I18n.text("Remove")) {

                            @Override
                            public void actionPerformed(ActionEvent e) {
                                panel.removeTreeObject(n.getUserObject());
                            }
                        });
                        showMenu = true;
                    }

                    if (showMenu)
                        menu.show(LogTree.this, e.getX(), e.getY());
                }
            }
            // Let the original event go the UI Thread
            super.mouseClicked(e);
        }
    };


    public LogTree(IMraLogGroup source,MRAPanel panel) {
        this.source = source;
        this.panel = panel;

        setModel(treeModel);
        setCellRenderer(treeRenderer);
        setRootVisible(false);
        setShowsRootHandles(true);

        addMouseListener(mouseAdapter);

        loadDefaultNodes();
        treeModel.nodeStructureChanged(root);
    }

    public void loadDefaultNodes() {
        root.add(visualizationsNode);
        root.add(chartsNode);
        root.add(tablesNode);
        //        chartsNode.add(newPlotNode);
    }

    public void addVisualization(MRAVisualization vis) {
        DefaultMutableTreeNode parent;
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(vis);

        // Choose parent node based on visualization type
        switch(vis.getType()) {
            case VISUALIZATION : parent = visualizationsNode; break;
            case CHART : parent = chartsNode; break;
            case TABLE : parent = tablesNode; break;
            default : parent = visualizationsNode;
        }
        parent.add(node);
        treeModel.nodeStructureChanged(root);
        expandAllTree();
    }

    public void remove(Object obj, DefaultMutableTreeNode parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (! (parent.getChildAt(i) instanceof DefaultMutableTreeNode))
                continue;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getChildAt(i);

            if (!node.isLeaf())
                remove(obj, (DefaultMutableTreeNode)node);
            else if (node.isLeaf()) {
                if (node.getUserObject().equals(obj)) {
                    parent.remove(node);
                    if (parent.getChildCount() == 0 && parent.getParent() instanceof DefaultMutableTreeNode) {
                        ((DefaultMutableTreeNode)parent.getParent()).remove(parent);
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
        if(markersNode == null) {
            markersNode = new DefaultMutableTreeNode(I18n.text("Markers"));
            root.add(markersNode);
        }
        markersNode.add(new DefaultMutableTreeNode(marker));
        treeModel.nodeStructureChanged(root);
        expandAllTree();
    }

    public void removeMarker(LogMarker marker) {
        DefaultMutableTreeNode node;
        for(int i = 0; i < markersNode.getChildCount(); i++) {
            node = (DefaultMutableTreeNode) markersNode.getChildAt(i);
            if(((LogMarker)node.getUserObject()) == marker) {
                markersNode.remove(node);
            }
        }
        if(markersNode.getChildCount() == 0) {
            root.remove(markersNode);
            markersNode = null;
        }

        treeModel.nodeStructureChanged(root);
        expandAllTree();
    }

    // Util methods
    private void expandAllTree() {
        for(int i = 0; i < getRowCount(); i++) {
            expandRow(i);
        }
    }

    public DefaultMutableTreeNode getChartsNode() {
        return chartsNode;
    }

    public void setChartsNode(DefaultMutableTreeNode chartsNode) {
        this.chartsNode = chartsNode;
    }
}