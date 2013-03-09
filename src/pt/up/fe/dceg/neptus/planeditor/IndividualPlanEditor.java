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
 * Author: 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.planeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import org.jdesktop.swingx.JXStatusBar;

import pt.up.fe.dceg.neptus.graph.GraphSelectionListener;
import pt.up.fe.dceg.neptus.graph.NeptusEdgeElement;
import pt.up.fe.dceg.neptus.graph.NeptusGraphElement;
import pt.up.fe.dceg.neptus.graph.NeptusNodeElement;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.gui.tablelayout.TableLayout;
import pt.up.fe.dceg.neptus.mp.Maneuver;
import pt.up.fe.dceg.neptus.mp.MissionPlanner;
import pt.up.fe.dceg.neptus.renderer2d.MissionRenderer;
import pt.up.fe.dceg.neptus.types.mission.GraphType;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.TransitionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author zp
 */
@SuppressWarnings("rawtypes")
public class IndividualPlanEditor extends JPanel implements PropertyChangeListener, UndoableEditListener,
        GraphSelectionListener, PropertiesProvider {

    private static final long serialVersionUID = 1L;
    protected IndividualPlanGraph graph;
    private AbstractAction undoAction, redoAction, layoutAction, previewAction, sendAction, settingsAction;

    private MissionType mission = new MissionType();
    private PlanType plan = new PlanType(mission);

    private MissionPlanner parentMP = null;

    private MissionRenderer previewRenderer = null;

    private boolean editable = true;

    private JXStatusBar statusBar = new JXStatusBar();

    private MapPlanEditor mapEditor = null;

    private void initialize() {

        graph.setBackground(Color.white);
        graph.getUndoSupport().removeUndoableEditListener(this);
        graph.getUndoSupport().addUndoableEditListener(this);

        graph.removeGraphSelectionListener(this);
        graph.addGraphSelectionListener(this);

        setLayout(new TableLayout(new double[][] { { TableLayout.FILL }, { 32, TableLayout.FILL, 20 } }));

        if (MapPlanEditor.isVehicleSupported(plan.getVehicle()) && isEditable()) {
            final JTabbedPane tabs = new JTabbedPane();
            // tabs.add("Graph", new JScrollPane(graph));
            mapEditor = new MapPlanEditor(plan);
            mapEditor.setPlanEditor(this);
            tabs.add("Sequence", mapEditor);

            // this.add(tabs, "0,1");
            this.add(mapEditor, "0,1");
            /*
             * tabs.setSelectedIndex(1);
             * 
             * tabs.addChangeListener(new ChangeListener() { public void stateChanged(ChangeEvent e) { if
             * (tabs.getSelectedIndex() == 0) { mapEdition = false; parsePlan(); graph.autoLayout(); repaint(); } if
             * (tabs.getSelectedIndex() == 1) { mapEdition = true; parseGraph(); mapEditor.reset(); } } });
             */
        }
        else {
            this.add(new JScrollPane(graph), "0,1");
        }

        this.add(createToolbar(), "0,0");
        statusBar.removeAll();
        JXStatusBar.Constraint c2 = new JXStatusBar.Constraint(JXStatusBar.Constraint.ResizeBehavior.FILL);

        statusBar.add(new JLabel("Vehicle: " + plan.getVehicle()));
        statusBar.add(new JLabel("Editable: " + isEditable()), c2);

        this.add(statusBar, "0,2");
    }

    public IndividualPlanEditor(PlanType plan) {
        this.graph = new IndividualPlanGraph(plan);
        setPlan(plan);
        initialize();
    }

    public IndividualPlanEditor(MissionPlanner parentMP, PlanType plan) {
        this.graph = new IndividualPlanGraph(plan);
        this.parentMP = parentMP;
        setPlan(plan);
        initialize();
    }

    public void parsePlan() {

        // Clear the current graph..
        graph.clear();

        // Add all new nodes
        for (Maneuver man : plan.getGraph().getAllManeuvers()) {
            ManeuverNode node = new ManeuverNode(man);
            graph.addNode(node);
        }

        // Add all edges
        for (TransitionType trans : plan.getGraph().getAllEdges()) {
            ManeuverTransition edge = new ManeuverTransition();
            // edge.setUserObject(trans.getCondition());
            edge.setID(trans.getId());
            edge.setTargetNodeID(trans.getTargetManeuver());
            edge.setSourceNodeID(trans.getSourceManeuver());
            // edge.setUserObject(trans.getCondition().getStringRepresentation());
            TransitionConditionAction tca = new TransitionConditionAction();
            tca.setCondition(trans.getCondition());
            tca.setAction(trans.getAction());
            edge.setTransitionCondAction(tca);
            graph.addEdge(edge);
        }
    }

    public void setPlan(PlanType plan) {
        this.plan = plan;
        this.mission = plan.getMissionType();

        parsePlan();

        removeAll();
        initialize();
        revalidate();
        repaint();
    }

    public ManeuverNode getInitialManeuver() {

        Vector<ManeuverNode> initialNodes = new Vector<ManeuverNode>();

        for (NeptusNodeElement<?> node : graph.getAllNodes()) {
            if (((ManeuverNode) node).isInitialNode()) {
                initialNodes.add((ManeuverNode) node);
            }
        }

        if (initialNodes.size() == 0) {
            return null;
        }

        if (initialNodes.size() > 1) {
            return null;
        }

        return initialNodes.firstElement();
    }

    public void parseGraph() {
        GraphType gt = plan.getGraph();
        gt.clear();

        ManeuverNode firstNode = getInitialManeuver();

        if (firstNode == null) {
            System.err.println("No initial maneuver");
        }
        else {
            gt.setInitialManeuver(firstNode.getID());
        }

        for (NeptusNodeElement<?> node : graph.getAllNodes()) {
            Maneuver man = (Maneuver) node.getUserObject();
            for (String s : man.getReacheableManeuvers()) {
                man.removeTransition(s);
            }
            gt.addManeuver(man);
        }

        for (NeptusEdgeElement<?> edge : graph.getAllEdges()) {
            gt.addTransition(edge.getID(), edge.getSourceNodeID(), edge.getTargetNodeID(),
                    ((TransitionConditionAction) edge.getUserObject()).getCondition(),
                    ((TransitionConditionAction) edge.getUserObject()).getAction());
        }
    }

    public PlanType getPlan() {

        /*
         * if (mapEdition) return plan; else parseGraph();
         */
        // if (graph.getAllNodes().length == 0 || mapEdition)
        // return plan;

        return plan;
    }

    private boolean validPlan(PlanType plan) {
        return true;
    }

    private void sendPlan() {

        if (getParentMP().isMissionChanged()) {
            int option = JOptionPane.showConfirmDialog(this, "The mission has to be saved first", "Mission changed",
                    JOptionPane.OK_CANCEL_OPTION);
            if (option == JOptionPane.CANCEL_OPTION)
                return;

            getParentMP().saveMission(false);
        }

        PlanType plan = getPlan();

        if (!validPlan(plan))
            return;
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        undoAction = new AbstractAction("Undo") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                if (graph.getUndoManager().canUndo())
                    graph.getUndoManager().undo();
                updateUndoRedo();
            }
        };
        undoAction.putValue(AbstractAction.SMALL_ICON, new ImageIcon(ImageUtils.getImage("images/buttons/undo.png")));
        toolbar.add(new ToolbarButton(undoAction));

        redoAction = new AbstractAction("Redo") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                if (graph.getUndoManager().canRedo())
                    graph.getUndoManager().redo();
                updateUndoRedo();
            }
        };
        redoAction.putValue(AbstractAction.SMALL_ICON, new ImageIcon(ImageUtils.getImage("images/buttons/redo.png")));
        toolbar.add(new ToolbarButton(redoAction));

        updateUndoRedo();

        toolbar.addSeparator();

        layoutAction = new AbstractAction("Layout") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                graph.autoLayout();
            }
        };
        layoutAction.putValue(AbstractAction.SMALL_ICON,
                new ImageIcon(ImageUtils.getImage("images/buttons/wizard.png")));
        layoutAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Automatically layout the graph");
        toolbar.add(new ToolbarButton(layoutAction));

        previewAction = new AbstractAction("Preview") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                preview();
            }
        };
        previewAction.putValue(AbstractAction.SMALL_ICON,
                new ImageIcon(ImageUtils.getImage("images/buttons/preview.png")));
        previewAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Preview the plan execution");

        toolbar.add(new ToolbarButton(previewAction));

        sendAction = new AbstractAction("Process") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                sendPlan();
            }
        };
        sendAction.putValue(AbstractAction.SMALL_ICON, new ImageIcon(ImageUtils.getImage("images/buttons/launch.png")));
        sendAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Process this plan");
        sendAction.setEnabled(getParentMP() != null);
        toolbar.add(new ToolbarButton(sendAction));

        toolbar.addSeparator();

        settingsAction = new AbstractAction("Settings") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(IndividualPlanEditor.this, isEditable());
            }
        };
        settingsAction.putValue(AbstractAction.SMALL_ICON,
                new ImageIcon(ImageUtils.getImage("images/buttons/settings.png")));
        settingsAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Settings");
        toolbar.add(new ToolbarButton(settingsAction));

        toolbar.setFloatable(false);
        return toolbar;
    }

    private void preview() {
        PlanType tmp = getPlan();

        if (tmp == null || tmp.getGraph().getAllManeuvers().length == 0) {
            System.out.println("Invalid plan");
            return;
        }

        if (previewRenderer == null) {
            previewRenderer = new MissionRenderer(plan, plan.getMapGroup(), MissionRenderer.R2D_AND_R3D1CAM);

            JFrame newFrame = new JFrame(plan.getId() + " preview");
            newFrame.setLayout(new BorderLayout());
            newFrame.add(previewRenderer, BorderLayout.CENTER);
            newFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            newFrame.addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent e) {
                    super.windowClosed(e);
                    previewRenderer.dispose();
                    previewRenderer = null;
                }
            });
            newFrame.setSize(500, 400);
            newFrame.setVisible(true);
        }
        else {
            previewRenderer.dispose();
            Container parent = previewRenderer.getParent();
            parent.remove(previewRenderer);
            previewRenderer = new MissionRenderer(plan, plan.getMapGroup(), MissionRenderer.R2D_AND_R3D1CAM);
            parent.add(previewRenderer, BorderLayout.CENTER);
            parent.invalidate();
            parent.validate();
        }

    }

    private void updateUndoRedo() {
        undoAction.setEnabled(graph.getUndoManager().canUndo());
        undoAction.putValue(AbstractAction.SHORT_DESCRIPTION, graph.getUndoManager().getUndoPresentationName());

        redoAction.setEnabled(graph.getUndoManager().canRedo());
        redoAction.putValue(AbstractAction.SHORT_DESCRIPTION, graph.getUndoManager().getRedoPresentationName());
    }

    public void undoableEditHappened(UndoableEditEvent e) {
        updateUndoRedo();
    }

    public void selectionChanged(NeptusGraphElement[] selection) {

    }

    public void propertyChange(PropertyChangeEvent evt) {
        graph.repaint();
    }

    public MissionPlanner getParentMP() {
        return parentMP;
    }

    public void setParentMP(MissionPlanner parentMP) {
        this.parentMP = parentMP;
    }

    public DefaultProperty[] getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPropertiesDialogTitle() {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getPropertiesErrors(Property[] properties) {
        // TODO Auto-generated method stub
        return null;
    }

    public void setProperties(Property[] properties) {
        // TODO Auto-generated method stub

    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        graph.setEditable(editable);

        removeAll();
        initialize();
        revalidate();
        repaint();
    }

    public static IndividualPlanEditor showPlanEditorNoEdit(PlanType plan) {
        JFrame frm = new JFrame("Plan Editor - " + plan.getId());

        frm.setIconImage(ImageUtils.getImage("images/menus/plan.png"));

        IndividualPlanEditor editor = new IndividualPlanEditor(plan);
        editor.setEditable(false);
        frm.setContentPane(editor);

        frm.setSize(500, 350);
        frm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frm.setVisible(true);

        return editor;
    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        GuiUtils.setLookAndFeel();
        MissionType mt = new MissionType("missions/Monterey/monterey.nmis");
        IndividualPlanEditor editor = IndividualPlanEditor.showPlanEditorNoEdit(mt.getIndividualPlansList()
                .get("plan1"));
        editor.setEditable(true);

    }
}
