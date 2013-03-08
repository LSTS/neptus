/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Jul 27, 2012
 * $Id:: LogTree.java 9730 2013-01-18 14:49:49Z jqcorreia                       $:
 */
package pt.up.fe.dceg.neptus.util.llf;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.plots.GenericPlot;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;


/**
 * @author jqcorreia
 *
 */
@SuppressWarnings("serial")
public class LogTree extends JTree {

    MRAPanel panel;

    LinkedHashMap<String, Component> visList = new LinkedHashMap<String, Component>();
    IMraLogGroup source;

    // Root node
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");

    // Top level nodes
    DefaultMutableTreeNode visualizationsNode = new DefaultMutableTreeNode(I18n.text("Visualizations"));
    DefaultMutableTreeNode chartsNode = new DefaultMutableTreeNode(I18n.text("Charts"));
    DefaultMutableTreeNode tablesNode = new DefaultMutableTreeNode(I18n.text("Tables"));
    DefaultMutableTreeNode markersNode;

    // Misc Nodes
    DefaultMutableTreeNode newPlotNode = new DefaultMutableTreeNode(I18n.text("New Plot"));

    DefaultTreeModel treeModel = new DefaultTreeModel(root);
    DefaultTreeCellRenderer treeRenderer = new DefaultTreeCellRenderer() {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if(node.getUserObject() instanceof MRAVisualization) {
                MRAVisualization viz = (MRAVisualization) node.getUserObject();
                setText(viz.getName());
                setIcon(viz.getIcon());
            }
            if(node.getUserObject() instanceof LogMarker) {
                LogMarker mark = (LogMarker) node.getUserObject();
                setText(mark.label);
                // setIcon(viz.getIcon());
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
                            System.out.println("New Plot");
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

                        menu.add(new AbstractAction("Remove") {

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

}