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
 * Author: José Pinto
 * 200?/??/??
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.undo.UndoManager;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetDialog;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.console.plugins.planning.edit.AllManeuversPayloadSettingsChanged;
import pt.lsts.neptus.console.plugins.planning.edit.ManeuverAdded;
import pt.lsts.neptus.console.plugins.planning.edit.ManeuverChanged;
import pt.lsts.neptus.console.plugins.planning.edit.ManeuverPropertiesPanel;
import pt.lsts.neptus.console.plugins.planning.edit.ManeuverRemoved;
import pt.lsts.neptus.console.plugins.planning.edit.ManeuverTranslated;
import pt.lsts.neptus.console.plugins.planning.edit.PlanElementChanged;
import pt.lsts.neptus.console.plugins.planning.edit.PlanPayloadSettingsChange;
import pt.lsts.neptus.console.plugins.planning.edit.PlanRotated;
import pt.lsts.neptus.console.plugins.planning.edit.PlanSettingsChanged;
import pt.lsts.neptus.console.plugins.planning.edit.PlanTransitionsChanged;
import pt.lsts.neptus.console.plugins.planning.edit.PlanTransitionsReversed;
import pt.lsts.neptus.console.plugins.planning.edit.PlanTranslated;
import pt.lsts.neptus.console.plugins.planning.edit.PlanVehiclesChange;
import pt.lsts.neptus.console.plugins.planning.edit.PlanZChanged;
import pt.lsts.neptus.console.plugins.planning.overview.MissionOverviewPanel;
import pt.lsts.neptus.gui.MenuScroller;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.VehicleChooser;
import pt.lsts.neptus.gui.VehicleSelectionDialog;
import pt.lsts.neptus.gui.ZValueSelector;
import pt.lsts.neptus.gui.editor.SpeedEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverFactory;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.ManeuverLocation.Z_UNITS;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.mp.actions.PlanActions;
import pt.lsts.neptus.mp.element.IPlanElement;
import pt.lsts.neptus.mp.element.IPlanElementEditorInteraction;
import pt.lsts.neptus.mp.element.PlanElementsFactory;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.mp.preview.PlanSimulationOverlay;
import pt.lsts.neptus.mp.preview.SimDepthProfile;
import pt.lsts.neptus.params.ManeuverPayloadConfig;
import pt.lsts.neptus.params.PlanPayloadConfig;
import pt.lsts.neptus.planeditor.PlanTransitionsSimpleEditor;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.enumeration.AlwaysSelectNeverEnum;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.map.PlanUtil;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.TransitionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * 
 * @author ZP
 * @author pdias
 */
@PluginDescription(name = "Plan Edition", icon = "images/planning/plan_editor.png", 
    author = "José Pinto, Paulo Dias", version = "1.7", category = CATEGORY.INTERFACE)
@LayerPriority(priority = 100)
public class PlanEditor extends InteractionAdapter implements Renderer2DPainter,
        MissionChangeListener, ConfigurationListener {

    private static final long serialVersionUID = 1L;
    private final String defaultCondition = "ManeuverIsDone";
    private MissionType mission = null;
    protected PlanType plan = null;
    private PlanElement planElem;
    private ManeuverFactory mf = null;
    private final MapGroup mapGroup = null;
    private Maneuver selectedManeuver = null;
    // private Vector<Maneuver> selection = new Vector<Maneuver>();
    private Point2D lastDragPoint = null;
    private final Vector<String> takenNames = new Vector<String>();
    private StateRenderer2D renderer;
    private StateRendererInteraction delegate = null;
    protected JPanel controls;
    protected JPanel sidePanel = null;
    protected JLabel statsLabel = null;
    protected static final String maneuverPreamble = "[Neptus:Maneuver]\n";
    protected PlanSimulationOverlay overlay = null;
    protected SimDepthProfile sdp = null;

    protected IPlanElement<?> activePlanElement = null;
    protected PlanElementsFactory pef = null;
    
    protected MissionOverviewPanel overviewPanel = null;
    protected JPanel bottomPanel = new JPanel(new BorderLayout());
    private boolean overviewIsVisible = false;
    private HashMap<Component, Object> componentList = new HashMap<>();
    private JSplitPane verticalSplit = null;

    public enum ToolbarLocation {
        Right,
        Left
    };

    private ManeuverPropertiesPanel propertiesPanel = null;
    private ManeuverLocation maneuverLocationBeforeMoving = null;
    private boolean maneuverWasMoved = false;
    private boolean planChanged = false;
    private boolean planTranslated = false;
    // private boolean planRotated = false;
    private double planRotatedRads = 0;

    private boolean showSimulation = false;
    private boolean showDepth = false;

    private String planStatistics = "";

    private String maneuverUndoRedoXml = null;
    private String planElementUndoRedoXml = null;

    private SwingWorker<Void, Void> editExitDoubleClickSwingWorker = null;

    private Object lock = new Object();

    @NeptusProperty(name = "Toolbar Location", userLevel = LEVEL.REGULAR)
    public ToolbarLocation toolbarLocation = ToolbarLocation.Right;

    @NeptusProperty(name = "Show Plan Simulation", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    public AlwaysSelectNeverEnum allowShowSimulation = AlwaysSelectNeverEnum.SELECTABLE_OFF;
    
    @NeptusProperty(name = "Show Depth Profile", userLevel = LEVEL.ADVANCED, distribution = DistributionEnum.DEVELOPER)
    public AlwaysSelectNeverEnum allowShowDepth = AlwaysSelectNeverEnum.SELECTABLE_OFF;

    @NeptusProperty(name = "Select Saved Plan on Console", userLevel = LEVEL.ADVANCED)
    public boolean selectSavedPlanOnConsole = false;
    
    @NeptusProperty(name = "Close Editor on Save", userLevel = LEVEL.ADVANCED)
    public boolean closeEditorOnSave = true;
    
    /**
     * @param console
     */
    public PlanEditor(ConsoleLayout console) {
        super(console);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        showSimulation = AlwaysSelectNeverEnum.isActive(allowShowSimulation);
        showDepth = AlwaysSelectNeverEnum.isActive(allowShowDepth);
    }

    /**
     * @return the planChanged
     */
    protected boolean isPlanChanged() {
        return planChanged;
    }
    
    /**
     * @param planChanged the planChanged to set
     */
    protected void setPlanChanged(boolean planChanged) {
        this.planChanged = planChanged;
    }
    
    protected ManeuverPropertiesPanel getPropertiesPanel() {
        if (propertiesPanel == null)
            propertiesPanel = new ManeuverPropertiesPanel();
        return propertiesPanel;
    }

    @Periodic(millisBetweenUpdates=20000)
    public void updateSim() {
        if (!isActive() || (!showSimulation && !showDepth) || plan == null || !plan.hasInitialManeuver()) {
            overlay = null;
            sdp = null;
        }
        else {
            VehicleType vt = plan.getVehicleType();
            overlay = new PlanSimulationOverlay(plan, 0, vt == null ? VehicleType.MAX_DURATION_H : vt.getMaxDurationHours(), null);
            sdp = new SimDepthProfile(overlay);
        }
    }
    
    @Periodic(millisBetweenUpdates=1000)
    public void update() {
        
        if (!isActive())
            return;
        
        try {
            if (plan != null && overviewPanel != null)
                overviewPanel.updatePlan(plan);
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getMessage(), e);
        }
        try {
            Maneuver curManeuver = getPropertiesPanel().getManeuver();

            if (curManeuver != null && renderer.isFocusOwner()) {
                synchronized (lock) {
                    getPropertiesPanel().setPlan(plan);
                    getPropertiesPanel().setManeuver(curManeuver);
                    getPropertiesPanel().setManager(manager);
                    if (delegate != null)
                        getPropertiesPanel().getEditBtn().setSelected(true);
                }
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getMessage(), e);
        }

        try {
            if (planElem != null && planElem.getPlan() != null) {
                planStatistics = PlanUtil.getPlanStatisticsAsText(planElem.getPlan(), null, true, true);
                statsLabel.setText(planStatistics);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final UndoManager manager = new UndoManager() {
        private static final long serialVersionUID = 1L;

        @Override
        public synchronized boolean addEdit(javax.swing.undo.UndoableEdit anEdit) {
            boolean ret = super.addEdit(anEdit);
            updateUndoRedo();
            return ret;
        };

        @Override
        public synchronized void undo() throws javax.swing.undo.CannotUndoException {
            super.undo();
            updateUndoRedo();
        };

        @Override
        public synchronized void redo() throws javax.swing.undo.CannotRedoException {
            super.redo();
            updateUndoRedo();
        };
    };

    @Override
    public Image getIconImage() {
        return ImageUtils.getImage(PluginUtils.getPluginIcon(getClass()));
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
        synchronized (lock) {
            getPropertiesPanel().setManeuver(null);
        }
        this.renderer = source;

        JSplitPane horizontalSplit = null;
        if (mode) {
            Container c = source;
            while (c.getParent() != null && !(c.getLayout() instanceof BorderLayout))
                c = c.getParent();
            if (c.getLayout() instanceof BorderLayout) {
                componentList.clear();

                BorderLayout bl = (BorderLayout) c.getLayout();
                for (Component component : c.getComponents()) {
                    Object constraint = bl.getConstraints(component);
                    componentList.put(component, constraint);
                }

                Component comp = bl.getLayoutComponent(BorderLayout.CENTER);

                switch (toolbarLocation) {
                    case Left:
                        horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, getSidePanel(), comp);
                        break;
                    default:
                        horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, comp, getSidePanel());
                        break;
                }

                horizontalSplit.setResizeWeight(1.0);

                verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, horizontalSplit, null);
                verticalSplit.setResizeWeight(1.0);
                c.add(verticalSplit);
                
                c.invalidate();
                c.validate();
                if (c instanceof JComponent)
                    ((JComponent) c).setBorder(new LineBorder(Color.orange.darker(), 3));
            }

            if (plan == null && getConsole().getPlan() != null) {
                setPlan(getConsole().getPlan().clonePlan());
            }
            else if (plan == null) {

                VehicleType choice = null;
                if (getConsole().getMainSystem() != null)
                    choice = VehicleChooser.showVehicleDialog(null,
                            VehiclesHolder.getVehicleById(getConsole().getMainSystem()), getConsole());
                else
                    choice = VehicleChooser.showVehicleDialog(null, null, getConsole());

                if (choice == null) {
                    if (getAssociatedSwitch() != null)
                        getAssociatedSwitch().doClick();

                    return;
                }
                PlanType plan = new PlanType(getConsole().getMission());
                plan.setVehicle(choice.getId());
                setPlan(plan);
            }
            else {
                setPlan(plan);
            }

            updateSim();

            if (overviewPanel == null) {
                overviewPanel = new MissionOverviewPanel(this, plan);
                overviewPanel.setVisible(overviewIsVisible);
            }
            else {
                overviewPanel.updatePlan(plan);
                overviewPanel.setVisible(overviewIsVisible);
            }

            verticalSplit.setBottomComponent(overviewPanel);
            verticalSplit.getRightComponent().setMinimumSize(overviewPanel.getPreferredSize());
        }
        else {
            if (delegate != null) {
                delegate.setActive(false, source);
                getPropertiesPanel().getEditBtn().setSelected(false);
                resetDelegate();
            }
            Container c = source;
            while (c.getParent() != null && !(c.getLayout() instanceof BorderLayout))
                c = c.getParent();
            if (c.getLayout() instanceof BorderLayout) {
                // c.remove(getSidePanel());
                c.removeAll();
                for (Entry<Component, Object> e : componentList.entrySet()) {
                    c.add(e.getKey(), e.getValue());
                }
                componentList.clear();

                if (overviewPanel != null) {
                    bottomPanel.removeAll();
                    overviewPanel = null;
                }

                c.invalidate();
                c.validate();
                if (c instanceof JComponent)
                    ((JComponent) c).setBorder(new EmptyBorder(0, 0, 0, 0));
            }
            if (plan != null && !manager.canUndo()
                    && getConsole().getMission().getIndividualPlansList().containsKey(plan.getId()))
                plan = null;
            planElem = null;
            renderer.setToolTipText("");
            overlay = null;
        }
    }

    @SuppressWarnings("serial")
    protected JPanel getSidePanel() {

        if (sidePanel == null) {
            sidePanel = new JPanel(new BorderLayout(2, 2));

            statsLabel = new JLabel() {
                @Override
                public void setText(String text) {
                    String[] txts = text.split("\n");
                    text = "<html>";
                    for (String str : txts) {
                        if (!str.contains(I18n.text("Plan Statistics")) && !str.contains(I18n.text("ID") + ":")
                                && !str.contains(I18n.text("Vehicle") + ":") && !str.equals("")) {
                            text += str + "<br>";
                        }
                    }
                    super.setText(text);
                }
            };

            controls = new JPanel(new GridLayout(0, 3));

            controls.add(new JButton(getNewAction()));
            controls.add(new JButton(getSaveAction()));
            controls.add(new JButton(getCloseAction()));
            controls.add(new JButton(getOverviewAction()));
            controls.add(new JButton(getUndoAction()));
            controls.add(new JButton(getRedoAction()));
            updateUndoRedo();
            controls.setBorder(new TitledBorder(I18n.text("Plan")));

            // controls.add(statsLabel);

            sidePanel.add(controls, BorderLayout.SOUTH);

            JPanel holder = new JPanel(new BorderLayout());
            holder.add(getPropertiesPanel());
            holder.add(statsLabel, BorderLayout.SOUTH);

            sidePanel.add(holder, BorderLayout.CENTER);
            getPropertiesPanel().getEditBtn().addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    Maneuver man = getPropertiesPanel().getManeuver();
                    if (man instanceof StateRendererInteraction) {
                        if (getPropertiesPanel().getEditBtn().isSelected()) {
                            updateDelegate((StateRendererInteraction) man, renderer);
                            
                            saveManeuverXmlState();
                            savePlanElementXmlState();
                        }
                        else {
                            delegate.setActive(false, renderer);
                            resetDelegate();
                            planElem.recalculateManeuverPositions(renderer);
                            
                            saveManeuverXmlToUndoManager();
                            savePlanElementXmlToUndoManager();
                        }
                    }
                }
            });

            getPropertiesPanel().getDeleteBtn().addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (getPropertiesPanel().getManeuver() != null) {
                        removeManeuver(getPropertiesPanel().getManeuver().getId());
                    }
                }
            });
            getPropertiesPanel().setOpaque(false);
            controls.setOpaque(false);
        }
        return sidePanel;
    }

    protected AbstractAction getOverviewAction() {
        return new AbstractAction(I18n.text("Overview"), ImageUtils.getScaledIcon(
                "images/buttons/log.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (overviewPanel != null) {
                    if (overviewPanel.isVisible()) {
                        overviewPanel.setVisible(false);
                        overviewIsVisible = false;
                    }
                    else {
                        overviewPanel.reset();
                        overviewPanel.setVisible(true);
                        overviewIsVisible = true;

                        int t = (int) renderer.getHeight() - 100;
                        verticalSplit.setDividerLocation(t);
                    }
                }
            }
        };
    }

    public void editDifferentPlan(PlanType newPlan) {

        if (plan != null && manager.canUndo()) {
            int option = JOptionPane.showConfirmDialog(getConsole(),
                    I18n.textf("Continuing will discard all changes made to %planName. Continue?", plan.getId()),
                    I18n.text("Edit plan"), JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.NO_OPTION)
                return;
        }
        manager.discardAllEdits();
        updateUndoRedo();
        setPlan(newPlan.clonePlan());

        if (getAssociatedSwitch() != null && !getAssociatedSwitch().isSelected())
            getAssociatedSwitch().doClick();
    }

    public void newPlan() {
        synchronized (lock) {
            getPropertiesPanel().setManeuver(null);
        }
        if (plan != null) {
            int option = JOptionPane.showConfirmDialog(getConsole(),
                    I18n.textf("Continuing will discard all changes made to %planName. Continue?", plan.getId()),
                    I18n.text("Create new plan"), JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.NO_OPTION)
                return;
        }

        manager.discardAllEdits();
        updateUndoRedo();
        VehicleType choice = null;
        if (getConsole().getMainSystem() != null)
            choice = VehicleChooser.showVehicleDialog(null,
                    VehiclesHolder.getVehicleById(getConsole().getMainSystem()), getConsole());
        else
            choice = VehicleChooser.showVehicleDialog(null, null, getConsole());

        if (choice == null)
            return;

        PlanType plan = new PlanType(getConsole().getMission());
        plan.setVehicle(choice.getId());
        setPlan(plan);

        if (getAssociatedSwitch() != null && !getAssociatedSwitch().isSelected())
            getAssociatedSwitch().doClick();

        if (plan != null && overviewPanel != null) {
            overviewPanel.reset();
        }
    }

    protected AbstractAction getNewAction() {
        return new AbstractAction(I18n.textc("New", "Plan"), ImageUtils.getScaledIcon("images/planning/edit_new.png",
                16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                newPlan();
            }
        };
    }

    protected AbstractAction undoAction, redoAction;

    protected void updateUndoRedo() {
        getUndoAction().putValue(AbstractAction.SHORT_DESCRIPTION, manager.getUndoPresentationName());
        getRedoAction().putValue(AbstractAction.SHORT_DESCRIPTION, manager.getRedoPresentationName());
        getUndoAction().setEnabled(manager.canUndo());
        getRedoAction().setEnabled(manager.canRedo());
        setPlanChanged(manager.canUndo());

        if (planElem != null)
            planElem.recalculateManeuverPositions(renderer);

        try {
            if (getPropertiesPanel().getManeuver() != null) {
                if (plan.getGraph().getManeuver(getPropertiesPanel().getManeuver().getId()) == null) {
                    synchronized (lock) {
                        getPropertiesPanel().setManeuver(null);
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        updateSim();
    }

    protected AbstractAction getUndoAction() {
        if (undoAction == null)
            undoAction = new AbstractAction(I18n.text("Undo"), ImageUtils.getScaledIcon("images/planning/undo.png", 16,
                    16)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    manager.undo();
                    planElem.recalculateManeuverPositions(renderer);
                    refreshPropertiesManeuver();
                }
            };
        return undoAction;
    }

    protected AbstractAction getRedoAction() {

        if (redoAction == null)
            redoAction = new AbstractAction(I18n.text("Redo"), ImageUtils.getScaledIcon("images/planning/redo.png", 16,
                    16)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    manager.redo();
                    planElem.recalculateManeuverPositions(renderer);
                    refreshPropertiesManeuver();
                }
            };
        return redoAction;
    }

    protected AbstractAction getSaveAction() {
        return new AbstractAction(I18n.text("Save"), ImageUtils.getScaledIcon("images/planning/edit_save.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {

                synchronized (lock) {
                    getPropertiesPanel().setManeuver(null);
                }
                String lastPlanId = plan.getId();
                String planId = lastPlanId;
                while (true) {
                    try {
                        plan.validatePlan();
                    }
                    catch (Exception ex) {
                        int option = GuiUtils.confirmDialog(getConsole(), I18n.text("Plan Validation"), 
                                I18n.text("Are you sure you want to save the plan?\nThe following errors where found:")
                                        +"\n - " + ex.getMessage().replaceAll("\n", "\n - "));
                        if (option == JOptionPane.YES_OPTION)
                            break;
                        else
                            return;
                    }
                    
                    planId = JOptionPane.showInputDialog(getConsole(), I18n.text("Enter the plan ID"), lastPlanId);
                    
                    if (planId == null)
                        return;
                    if (getConsole().getMission().getIndividualPlansList().get(planId) != null) {
                        int option = JOptionPane.showConfirmDialog(getConsole(),
                                I18n.text("Do you wish to replace the existing plan with same name?"));
                        if (option == JOptionPane.CANCEL_OPTION)
                            return;
                        else if (option == JOptionPane.YES_OPTION)
                            break;
                        lastPlanId = planId;
                    }
                    else
                        break;
                }
                
                plan.setId(planId);
                plan.setMissionType(getConsole().getMission());
                
                plan.load(plan.asXML());
                
                getConsole().getMission().addPlan(plan);
                
 //               // getConsole().getMission().save(true);
 //               SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
 //                   @Override
 //                   protected Void doInBackground() throws Exception {
                getConsole().getMission().save(true);
 //                       return null;
 //                   }
 //               };
 //               worker.execute();

                boolean consolePlanSet = false;
                PlanType tmpPlan = plan;
                setPlan(null);
                manager.discardAllEdits();
                updateUndoRedo();
                if (closeEditorOnSave){
                    if (getAssociatedSwitch() != null)
                        getAssociatedSwitch().doClick();
                }
                else {
                    setPlan(tmpPlan.clonePlan());
                }
                
                getConsole().updateMissionListeners();

                if (getConsole().getPlan() == null || getConsole().getPlan().getId().equalsIgnoreCase(tmpPlan.getId())) {
                    getConsole().setPlan(tmpPlan);
                    consolePlanSet = true;
                }
                
                if (selectSavedPlanOnConsole && !consolePlanSet) {
                    getConsole().setPlan(tmpPlan);
                }
                
                setPlanChanged(false);
            }
        };
    }

    protected AbstractAction getCloseAction() {
        return new AbstractAction(I18n.text("Close"),
                ImageUtils.getScaledIcon("images/planning/edit_close.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {

                if (plan != null && planChanged) {
                    int option = JOptionPane.showConfirmDialog(getConsole(), I18n.textf(
                            "Continuing will discard all changes made to %planName. Continue?", plan.getId()), I18n
                            .text("Close discarding changes"), JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.NO_OPTION)
                        return;
                }
                synchronized (lock) {
                    getPropertiesPanel().setManeuver(null);
                    setPlan(null);
                    manager.discardAllEdits();
                    updateUndoRedo();
                }
                if (getAssociatedSwitch() != null)
                    getAssociatedSwitch().doClick();
            }
        };
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        this.renderer = renderer;

        if (showSimulation && overlay != null && isActive())
            overlay.paint(g, renderer);
        
        g.setTransform(renderer.getIdentity());
        
        if (plan != null) {
            for (IPlanElement<?> pe : plan.getPlanElements().getPlanElements()) {
                Renderer2DPainter painter = pe.getPainter();
                if (painter != null) {
                    Graphics2D gt = (Graphics2D) g.create();
                    pe.getPainter().paint(gt, renderer);
                    gt.dispose();
                }
            }
        }
        
        if (planElem != null) {
            planElem.setRenderer(renderer);
            planElem.paint((Graphics2D) g.create(), renderer);
        }

        g.setFont(new Font("Helvetica", Font.BOLD, 14));
        if (delegate != null) {
            delegate.paintInteraction(g, renderer);
            
            String txtDelegate;
            if (delegate instanceof Maneuver)
                txtDelegate = ((Maneuver) delegate).getId();
            else
                txtDelegate = delegate.getName();
            String txt = I18n.textf("Editing %manName - Double click or press ESC to end", txtDelegate);
            g.setColor(Color.black);
            g.drawString(txt, 55, 15);
            g.setColor(Color.white);
            g.drawString(txt, 54, 14);
        }
        
        BufferedImage depthProfile = null;
        if (showDepth && sdp != null && isActive()) {
            depthProfile = sdp.getProfile();
            if (depthProfile != null) {
                g.setTransform(renderer.getIdentity());
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
                g.drawImage(depthProfile, 10, renderer.getHeight() - 130, renderer.getWidth()-10, renderer.getHeight()-30, 0, 0,
                        depthProfile.getWidth(), depthProfile.getHeight(), this);
            }
        }
    }

    /**
     * Verifies if a given vehicle supports this kind of editor
     * 
     * @param vehicleID The vehicle's ID
     * @return <b>true</b> if the vehicle is supported or <b>false</b> otherwise
     */
    public static boolean isVehicleSupported(String vehicleID) {

        VehicleType vehicle = VehiclesHolder.getVehicleById(vehicleID);

        if (vehicle == null)
            return false;

        LinkedHashMap<String, String> maneuvers = vehicle.getFeasibleManeuvers();

        for (String maneuver : maneuvers.keySet()) {
            Maneuver man = ManeuverFactory.createManeuver(maneuver, maneuvers.get(maneuver));
            if (man != null) {
                return true;
            }
        }

        return false;
    }

    public void setPlan(PlanType plan) {

        this.plan = plan;
        if (plan == null) {
            planElem = null;
            return;
        }
        
        overlay = null;
        sdp = null;

        synchronized (lock) {
            getPropertiesPanel().setManeuver(null);
            getPropertiesPanel().setManager(null);
            parsePlan();
            planElem = new PlanElement(mapGroup, new MapType());
            planElem.setBeingEdited(true);
            planElem.setRenderer(renderer);
            planElem.setTransp2d(1.0);
            planElem.setPlan(plan);
            controls.setBorder(new TitledBorder(I18n.textf("Editing %planName", plan.getId())));
            getPropertiesPanel().setPlan(plan);
        }
    }

    private void parsePlan() {
        VehicleType vt = plan.getVehicleType();
        if (vt == null) {
            NeptusLog.pub().warn("No vehicle type for vehicle " + plan.getVehicle() + " for plan " + plan.getId());
            String mvid = getMainVehicleId();
            vt = VehiclesHolder.getVehicleById(mvid);
            if (vt == null)
                NeptusLog.pub().warn(
                        "No vehicle type for main vehicle " + getMainVehicleId() + " for plan " + plan.getId());
            else
                plan.setVehicle(getMainVehicleId());
        }

        if (vt != null) {
            this.mf = vt.getManeuverFactory();
            this.pef = vt.getPlanElementsFactory();
        }
        else {
            NeptusLog.pub().warn("No vehicle type creating empty maneuver factory for plan " + plan.getId());
            this.mf = new ManeuverFactory(null);
            this.pef = new PlanElementsFactory(getMainVehicleId());
        }

        for (Maneuver man : plan.getGraph().getAllManeuvers()) {
            takenNames.add(man.getId());
            man.setVehicle(vt);
        }
    }

    protected Collection<AbstractAction> getActionsForManeuver(final Maneuver man, final Point mousePoint) {

        Vector<AbstractAction> actions = new Vector<AbstractAction>();

        AbstractAction props = new AbstractAction(I18n.textf("%manName properties", man.getId())) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                boolean wasInitialManeuver = man.isInitialManeuver();
                String oldXml = man.asXML();
                PropertiesEditor.editProperties(man, PlanEditor.this.getConsole(), true);
                manager.addEdit(new ManeuverChanged(man, plan, oldXml));
                if (man.isInitialManeuver())
                    plan.getGraph().setInitialManeuver(man.getId());
                else {
                    if (wasInitialManeuver) {
                        man.setInitialManeuver(true);
                        GuiUtils.infoMessage(ConfigFetch.getSuperParentFrame(), I18n.text("Set Properties"),
                                I18n.text("To change the initial maneuver, please set another maneuver as the initial"));
                    }
                }

                planElem.recalculateManeuverPositions(renderer);
                renderer.repaint();
                refreshPropertiesManeuver();
            }
        };
        props.putValue(AbstractAction.SMALL_ICON, new ImageIcon(ImageUtils.getImage("images/menus/edit.png")));
        actions.add(props);

        if (man instanceof StateRendererInteraction) {
            AbstractAction editMan = new AbstractAction(I18n.textf("Edit %maneuverName in the map", man.getId())) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    updateDelegate((StateRendererInteraction) man, renderer);
                }
            };
            actions.add(editMan);
        }

        AbstractAction remove = new AbstractAction(I18n.textf("Remove %maneuverName", man.getId())) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                removeManeuver(man.getId());

            }
        };
        remove.putValue(AbstractAction.SMALL_ICON, new ImageIcon(ImageUtils.getImage("images/menus/editdelete.png")));
        actions.add(remove);

        AbstractAction copy = new AbstractAction(I18n.textf("Copy %maneuverName to clipboard", man.getId())) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {

                ClipboardOwner owner = new ClipboardOwner() {
                    @Override
                    public void lostOwnership(java.awt.datatransfer.Clipboard clipboard,
                            java.awt.datatransfer.Transferable contents) {
                    }
                };
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(maneuverPreamble + man.getManeuverXml()), owner);
            }
        };
        copy.putValue(AbstractAction.SMALL_ICON, new ImageIcon(ImageUtils.getImage("images/menus/editcopy.png")));
        actions.add(copy);

        actions.add(getPasteAction((Point) mousePoint));
        actions.add(getPasteBeforeAction(mousePoint, man));

        List<String> names = Arrays.asList(mf.getAvailableManeuversIDs());
        Collections.sort(names, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                Collator collator = Collator.getInstance(Locale.US);
                return collator.compare(o1, o2);
            }
        });
        ImageIcon icon = ImageUtils.getIcon("images/led_none.png");
        for (String manName : names) {
            final String manType = manName;
            AbstractAction act = new AbstractAction(I18n.textf("Add %maneuverName1 before %maneuverName2", manName,
                    man.getId()), icon) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent evt) {
                    Vector<TransitionType> trans = plan.getGraph().getIncomingTransitions(man);

                    if (trans.isEmpty())
                        addManeuverAtBeginning(manType, mousePoint);
                    else
                        addManeuverAfter(manType, trans.firstElement().getSourceManeuver());
                    planElem.recalculateManeuverPositions(renderer);
                    renderer.repaint();
                }
            };
            actions.add(act);
        }

        return actions;
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        final StateRenderer2D renderer = source;
        final Point2D mousePoint = event.getPoint();

        if (delegate != null) {
            if (event.getClickCount() == 2) {
                if (editExitDoubleClickSwingWorker != null)
                    editExitDoubleClickSwingWorker.cancel(true);
                
                endManeuverEdition(source);
                return;
            }
            else {
                Integer timerinterval = (Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
                if (editExitDoubleClickSwingWorker != null) {
                    editExitDoubleClickSwingWorker.cancel(true);
                    editExitDoubleClickSwingWorker = null;
                }
                editExitDoubleClickSwingWorker = new SwingWorker<Void, Void>() {
                    protected Void doInBackground() {
                        try { Thread.sleep(timerinterval); } catch (Exception e) { }
                        return null;
                    };
                    @Override
                    protected void done() {
                        try {
                            get();
                            delegate.mouseClicked(event, source);
                        }
                        catch (Exception e) {
                        }
                    }
                };
                editExitDoubleClickSwingWorker.execute();
                return;
            }
        }

        if (event.getClickCount() == 2) {
            planElem.iterateManeuverBack(event.getPoint());
            final Maneuver man = planElem.iterateManeuverBack(event.getPoint());
            if (man != null) {
                if (man instanceof StateRendererInteraction) {
                    updateDelegate((StateRendererInteraction) man, source);
                    getPropertiesPanel().getEditBtn().setSelected(true);
                    saveManeuverXmlState();
                }
                return;
            }
        }

        if (event.isControlDown() && event.getButton() == MouseEvent.BUTTON1) {
            Maneuver m = plan.getGraph().getLastManeuver();
            addManeuverAtEnd(event.getPoint(), m.getType());
            return;
        }

        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu popup = new JPopupMenu();

            AbstractAction copy = new AbstractAction(I18n.text("Copy location")) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    CoordinateUtil.copyToClipboard(renderer.getRealWorldLocation(mousePoint));
                }
            };
            copy.putValue(AbstractAction.SMALL_ICON, new ImageIcon(ImageUtils.getImage("images/menus/editcopy.png")));
            popup.add(copy);
            
            JCheckBoxMenuItem showSim = new JCheckBoxMenuItem(I18n.text("View Simulation"));
            showSim.setSelected(showSimulation);
            
            showSim.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showSimulation = ((JCheckBoxMenuItem)e.getSource()).isSelected();      
                    updateSim();
                }
            });
            if (AlwaysSelectNeverEnum.isSelectable(allowShowSimulation))
                popup.add(showSim);
            
            JCheckBoxMenuItem showDepthItem = new JCheckBoxMenuItem(I18n.text("View Depth Profile"));
            showDepthItem.setSelected(showDepth);
            showDepthItem.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showDepth = ((JCheckBoxMenuItem)e.getSource()).isSelected();      
                    updateSim();
                }
            });
            if (AlwaysSelectNeverEnum.isSelectable(allowShowDepth))
                popup.add(showDepthItem);

            final Maneuver[] mans = planElem.getAllInterceptedManeuvers(event.getPoint());

            popup.addSeparator();
            if (mans.length == 1) {
                for (AbstractAction act : getActionsForManeuver(mans[0], (Point) mousePoint))
                    popup.add(act);
            }
            else if (mans.length > 1) {
                for (Maneuver m : mans) {
                    JMenu subMenu = new JMenu(m.getId());
                    if (m instanceof LocatedManeuver) {
                        subMenu.setText(m.getId()
                                + " ("
                                + (GuiUtils.getNeptusDecimalFormat(1).format(((LocatedManeuver) m)
                                        .getManeuverLocation().getAllZ())) + " m)");
                    }
                    subMenu.setIcon(new ImageIcon(ImageUtils.getImage("images/menus/plan.png")));

                    for (AbstractAction act : getActionsForManeuver(m, (Point) mousePoint))
                        subMenu.add(act);

                    popup.add(subMenu);
                }
            }
            else {
                if (plan.hasInitialManeuver()) {
                    JMenu planSettings = new JMenu(I18n.text("Change Existing Maneuvers"));
                    AbstractAction pDepth = new AbstractAction(I18n.text("Plan depth / altitude...")) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent e) {

                            ManeuverLocation loc = new ManeuverLocation();

                            for (Maneuver m : plan.getGraph().getAllManeuvers()) {
                                if (m instanceof LocatedManeuver) {
                                    loc = ((LocatedManeuver) m).getManeuverLocation().clone();
                                    break;
                                }
                            }

                            boolean newZ = ZValueSelector.showHeightDepthDialog(getConsole().getMainPanel(),
                                    plan.getVehicle(), loc, I18n.text("Plan Z "));
                            if (newZ) {
                                LinkedHashMap<String, Z_UNITS> prevUnits = new LinkedHashMap<String, ManeuverLocation.Z_UNITS>();
                                LinkedHashMap<String, Double> prevValues = new LinkedHashMap<String, Double>();
                                for (Maneuver m : plan.getGraph().getAllManeuvers()) {
                                    if (m instanceof LocatedManeuver) {
                                        ManeuverLocation l = ((LocatedManeuver)m).getManeuverLocation();
                                        prevUnits.put(m.getId(), l.getZUnits());
                                        prevValues.put(m.getId(), l.getZ());
                                    }
                                }
                                planElem.setPlanZ(loc.getZ(), loc.getZUnits());
                                manager.addEdit(new PlanZChanged(plan, loc.getZ(), loc.getZUnits(), prevUnits, prevValues));
                                refreshPropertiesManeuver();
                            }
                        }
                    };

                    pDepth.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));

                    planSettings.add(pDepth);
                    AbstractAction pVel = new AbstractAction(I18n.text("Plan speed...")) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            SpeedEditor editor = new SpeedEditor();
                            SpeedType lastSpeed = null;
                            try {
                                lastSpeed = (SpeedType) planElem.getLastSetProperties().get("Speed").getValue();
                            }
                            catch (Exception ex) { }
                            
                            if (lastSpeed == null)
                                lastSpeed = new SpeedType(1, Units.MPS);
                            
                            editor.setValue(lastSpeed);
                            int resp = JOptionPane.showConfirmDialog(getConsole(),
                                    editor.getCustomEditor(),
                                    I18n.text("Speed for all maneuvers"),
                                    JOptionPane.OK_CANCEL_OPTION,
                                    JOptionPane.QUESTION_MESSAGE);
                            
                            if (resp != JOptionPane.OK_OPTION)
                                return;
                            
                            SpeedType newSpeed = (SpeedType)editor.getValue();
                            
                            LinkedHashMap<String, Vector<DefaultProperty>> previousSettings = new LinkedHashMap<String, Vector<DefaultProperty>>();
                            
                            for (Maneuver m : plan.getGraph().getAllManeuvers()) {
                                for (DefaultProperty p : m.getProperties()) {
                                    if (p.getName().equalsIgnoreCase("Speed")) {
                                        if (!previousSettings.containsKey(m.getId()))
                                            previousSettings.put(m.getId(), new Vector<DefaultProperty>());
                                        previousSettings.get(m.getId()).add(p);
                                    }
                                }
                            }
                            
                            DefaultProperty propVel = new DefaultProperty();
                            propVel.setName("Speed");
                            propVel.setValue(newSpeed);
                            propVel.setType(SpeedType.class);
                            propVel.setDisplayName(I18n.text("Speed"));
                            planElem.setPlanProperty(propVel);
                            
                            manager.addEdit(new PlanSettingsChanged(plan, Arrays.asList(propVel), previousSettings));
                            refreshPropertiesManeuver();
                        }
                    };
                    pVel.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    planSettings.add(pVel);

                    AbstractAction pPayload = new AbstractAction(I18n.text("Maneuvers payload settings...")) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Goto pivot = new Goto();
                            PropertySheetPanel psp = new PropertySheetPanel();
                            psp.setEditorFactory(PropertiesEditor.getPropertyEditorRegistry());
                            psp.setRendererFactory(PropertiesEditor.getPropertyRendererRegistry());
                            psp.setMode(PropertySheet.VIEW_AS_CATEGORIES);
                            psp.setToolBarVisible(false);
                            psp.setSortingCategories(true);

                            psp.setDescriptionVisible(true);

                            ManeuverPayloadConfig payloadConfig = new ManeuverPayloadConfig(plan.getVehicle(), pivot,
                                    psp);
                            DefaultProperty[] properties = payloadConfig.getProperties();

                            ManeuverPayloadConfig payloadDefaultsConfig = new ManeuverPayloadConfig(plan.getVehicle(),
                                    pivot, psp);
                            DefaultProperty[] propertiesDefaults = payloadDefaultsConfig.getProperties();

                            int ret = fillPropertiesWithAllChangesFromDefaults(plan, properties, propertiesDefaults,
                                    psp);
                            String extraTxt = "";
                            switch (ret) {
                                case 0:
                                case 1:
                                    extraTxt = "<br><small>(" + I18n.text("All maneuvers have the same values.") + ")";
                                    break;
                                default:
                                    extraTxt = "<br><small>(" + I18n.text("Not all maneuvers have the same values.")
                                            + ")";
                                    break;
                            }

                            psp.setProperties(properties);

                            final PropertySheetDialog propertySheetDialog = PropertiesEditor.createWindow(getConsole(),
                                    true, psp, I18n.text("Maneuvers Payload Settings"),
                                    "<html>" + I18n.text("Maneuvers payload settings to apply to entire plan maneuvers")
                                            + extraTxt);
                            if (propertySheetDialog.ask()) {
                                payloadConfig.setProperties(properties);
                                PlanActions newPlanActions = pivot.getStartActions();
                                Map<String, PlanActions> originalPlanActionsPerManeuver = new HashMap<>();
                                Arrays.asList(plan.getGraph().getAllManeuvers()).stream().forEach(m -> {
                                    PlanActions sa = m.getStartActions();
                                    if (sa != null)
                                        sa = (PlanActions) sa.clone();
                                    originalPlanActionsPerManeuver.put(m.getId(), sa);
                                });
                                AllManeuversPayloadSettingsChanged undoRedo = new AllManeuversPayloadSettingsChanged(
                                        plan, newPlanActions, originalPlanActionsPerManeuver);
                                undoRedo.redo();
                                manager.addEdit(undoRedo);
                                
                                refreshPropertiesManeuver();
                            }
                        }
                    };
                    pPayload.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    planSettings.add(pPayload);

                    LinkedHashMap<String,VehicleType> vehiclesAvailableMap = VehiclesHolder.getVehiclesList();
                    if (vehiclesAvailableMap.size() > 1 || vehiclesAvailableMap.size() == 1
                            && !plan.getVehicles().containsAll(vehiclesAvailableMap.values())) {
                        AbstractAction pVehicle = new AbstractAction(I18n.text("Set plan vehicles...")) {
                            private static final long serialVersionUID = 1L;
                            
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                Window parentW = SwingUtilities.getWindowAncestor(getConsole());
                                String[] vehicles = VehicleSelectionDialog.showSelectionDialog(parentW, plan.getVehicles()
                                        .toArray(new VehicleType[0]));
                                Vector<VehicleType> vts = new Vector<VehicleType>();
                                for (String v : vehicles) {
                                    vts.add(VehiclesHolder.getVehicleById(v));
                                }
                                
                                Vector<VehicleType> oVts = plan.getVehicles();
                                boolean changed = false;
                                if (vts.size() != oVts.size() 
                                        || (vts.isEmpty() && oVts.size() > 0)
                                        || (vts.size() > 0 && oVts.isEmpty())) {
                                    changed = true;
                                }
                                else {
                                    for (VehicleType v : vts) {
                                        if (!oVts.contains(v)) {
                                            changed = true;
                                            break;
                                        }
                                    }
                                }
                                
                                if (!changed)
                                    return;
                                
                                PlanType newPlan = plan.clonePlan();
                                PlanUtil.changePlanVehiclesAndAdjustSettings(newPlan, vts);
                                PlanVehiclesChange pce = new PlanVehiclesChange(PlanEditor.this, plan, newPlan);
                                pce.redo(); // To clear gui and references
                                manager.addEdit(pce);
                            }
                        };
                        pVehicle.putValue(AbstractAction.SMALL_ICON,
                                new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                        planSettings.add(pVehicle);
                    }

                    AbstractAction pTrans = new AbstractAction(I18n.text("Reverse plan transitions")) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            Maneuver[] manSeq = plan.getGraph().getManeuversSequence();
                            if (manSeq.length > 1) {
                                Maneuver startManeuver = manSeq[0];
                                Maneuver endManeuver = manSeq[manSeq.length - 1];
                                PlanTransitionsReversed ptr = new PlanTransitionsReversed(plan, startManeuver, endManeuver);
                                ptr.redo();
                                manager.addEdit(ptr);

                                if (plan != null && overviewPanel != null)
                                    overviewPanel.updatePlan(plan);
                            }
                        }
                    };
                    pTrans.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    planSettings.add(pTrans);
                    
                    // Remove all maneuvers with no incoming transitions
                    AbstractAction pUnreach = new AbstractAction(I18n.text("Remove unreachable maneuvers")) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent e) {

                            Maneuver toRemove = null;
                            do {
                                toRemove = null;
                                // keep removing one maneuver at a time until no more maneuvers are to be removed
                                for (Maneuver man : plan.getGraph().getAllManeuvers()) {
                                    if (plan.getGraph().getIncomingTransitions(man).isEmpty()
                                            && !man.getId().equals(plan.getGraph().getInitialManeuverId())) {
                                        toRemove = man;
                                        break;
                                    }
                                }
                                if (toRemove != null)
                                    plan.getGraph().removeManeuver(toRemove);
                            } while (toRemove != null);
                        }
                    };
                    pUnreach.putValue(AbstractAction.SMALL_ICON,
                            new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    planSettings.add(pUnreach);


                    planSettings.setIcon(new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                    popup.add(planSettings);

                    // popup.addSeparator();
                }
                
                AbstractAction planPayload = new AbstractAction(I18n.text("Plan General Settings")) {
                    private static final long serialVersionUID = 1L;
                    @SuppressWarnings("serial")
                    private PropertySheetPanel psp = new PropertySheetPanel() {{
                        setEditorFactory(PropertiesEditor.getPropertyEditorRegistry());
                        setRendererFactory(PropertiesEditor.getPropertyRendererRegistry());
                    }};
                    
                    private String beforeStartActions = plan.getStartActions().asDocument("startActions").asXML();
                    private String beforeEndActions = plan.getEndActions().asDocument("endActions").asXML();
                    
                    private PlanPayloadConfig payloadConfig = new PlanPayloadConfig(plan.getVehicle(), plan,
                            psp);
                    private DefaultProperty[] properties = payloadConfig.getProperties();
                    
                    {
                        if (properties.length == 0)
                            this.setEnabled(false);
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        psp.setMode(PropertySheet.VIEW_AS_CATEGORIES);
                        psp.setToolBarVisible(false);
                        psp.setSortingCategories(true);

                        psp.setDescriptionVisible(true);

                        if (properties.length > 0)
                            ((JMenuItem) e.getSource()).setEnabled(false);
                        
                        psp.setProperties(properties);

                        final PropertySheetDialog propertySheetDialog = PropertiesEditor.createWindow(getConsole(),
                                true, psp, I18n.text("Plan General Settings"),
                                "<html>" + I18n.text("Plan General Settings"));
                        if (propertySheetDialog.ask()) {
                            payloadConfig.setProperties(properties);
                            
                            PlanPayloadSettingsChange ppsc = new PlanPayloadSettingsChange(plan, beforeStartActions,
                                    beforeEndActions);
                            manager.addEdit(ppsc);
                        }
                    }
                };
                planPayload.putValue(AbstractAction.SMALL_ICON,
                        new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                popup.add(planPayload).setEnabled(planPayload.isEnabled());

                AbstractAction pTransitions = new AbstractAction(I18n.text("Plan Transitions")) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Window parent = SwingUtilities.getWindowAncestor(getConsole());
                        if (parent == null)
                            parent = SwingUtilities.getWindowAncestor(ConfigFetch.getSuperParentAsFrame());
                        JDialog transitions = new JDialog(parent, I18n.textf("Edit '%planName' plan transitions",
                                plan.getId()));
                        
                        ArrayList<TransitionType> originalTransitions = new ArrayList<>();
                        Arrays.asList(plan.getGraph().getAllEdges()).stream().forEach(t -> {
                            try {
                                originalTransitions.add((TransitionType) t.clone());
                            }
                            catch (CloneNotSupportedException e1) {
                                e1.printStackTrace();
                            }
                        });
                        
                        transitions.setModalityType(ModalityType.DOCUMENT_MODAL);
                        transitions.getContentPane().add(new PlanTransitionsSimpleEditor(plan));
                        transitions.setSize(800, 500);
                        GuiUtils.centerParent(transitions, getConsole());
                        transitions.setVisible(true);
                        parsePlan();
                        renderer.repaint();

                        refreshPropertiesManeuver();
                        
                        ArrayList<TransitionType> newTransitions = new ArrayList<>();
                        Arrays.asList(plan.getGraph().getAllEdges()).stream().forEach(t -> {
                            try {
                                newTransitions.add((TransitionType) t.clone());
                            }
                            catch (CloneNotSupportedException e1) {
                                e1.printStackTrace();
                            }
                        });

                        if (!originalTransitions.containsAll(newTransitions)
                                || !newTransitions.containsAll(originalTransitions)) {
                            PlanTransitionsChanged ptc = new PlanTransitionsChanged(plan, originalTransitions, newTransitions);
                            manager.addEdit(ptc);
                        }
                    }
                };
                pTransitions.putValue(AbstractAction.SMALL_ICON,
                        new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                popup.add(pTransitions);

                // popup.addSeparator();

                JMenu pStatistics = PlanUtil.getPlanStatisticsAsJMenu(plan, I18n.text("Edited Plan Statistics"));
                pStatistics.setIcon(new ImageIcon(ImageUtils.getScaledImage("images/buttons/wizard.png", 16, 16)));
                popup.add(pStatistics);

                PlanEditorMenus.addPlanElementsMenuItems(this, plan, event, source, popup, manager);

                popup.addSeparator();

                popup.add(getPasteAction((Point) mousePoint));

                List<String> names = Arrays.asList(mf.getAvailableManeuversIDs());
                Collections.sort(names);

                ImageIcon icon = ImageUtils.getIcon("images/led_none.png");
                for (final String manName : names) {
                    String manNameStr = I18n.text(manName);
                    AbstractAction act = new AbstractAction(I18n.textf("Add %maneuverName", manNameStr), icon) {
                        private static final long serialVersionUID = 1L;

                        private final Point2D mousePos = mousePoint;

                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            String maneuverName = manName;
                            addManeuverAtEnd((Point) mousePos, maneuverName);
                            planElem.recalculateManeuverPositions(renderer);
                            renderer.repaint();
                        }
                    };
                    popup.add(act);
                }

                // AbstractAction act = new AbstractAction(I18n.text("Simulate plan"), null) {
                // private static final long serialVersionUID = 1L;
                //
                // @Override
                // public void actionPerformed(ActionEvent evt) {
                //
                // String vehicle = getConsole().getMainSystem();
                // LocationType startLoc = plan.getMissionType().getStartLocation();
                // SystemPositionAndAttitude start = new SystemPositionAndAttitude(startLoc, 0, 0, 0);
                // EstimatedState lastState = null;
                // FuelLevel lastFuel = null;
                // double motionRemaingHours = 4;
                //
                // try {
                // lastState = ImcMsgManager.getManager().getState(vehicle).lastEstimatedState();
                // lastFuel = ImcMsgManager.getManager().getState(vehicle).lastFuelLevel();
                //
                // if (lastState != null)
                // start = new SystemPositionAndAttitude(lastState);
                //
                // if (lastFuel != null) {
                // LinkedHashMap<String, String> opmodes = lastFuel.getOpmodes();
                // motionRemaingHours = Double.parseDouble(opmodes.get("Full"));
                // }
                // }
                // catch (Exception e) {
                // NeptusLog.pub().error("Error getting info from main vehicle", e);
                // }
                //
                // overlay = new PlanSimulationOverlay(plan, 0, motionRemaingHours, start);
                // PlanSimulation3D.showSimulation(getConsole(), overlay, plan);
                //
                // }
                // };
                // popup.add(act);
            }

            MenuScroller.setScrollerFor(popup, getConsole(), 150, 0, 0);
            popup.show(source, (int) mousePoint.getX(), (int) mousePoint.getY());
        }
    }

    protected void updateDelegate(IPlanElement<?> pel, StateRenderer2D renderer) {
        activePlanElement = pel;
        savePlanElementXmlState();
        delegate = pel.getEditor();
        delegate.setActive(true, renderer);
    }

    protected void updateDelegate(StateRendererInteraction sri, StateRenderer2D renderer) {
        activePlanElement = null;
        delegate = sri;
        delegate.setActive(true, renderer);
    }

    protected void resetDelegate() {
        if (delegate == null)
            return;
        if (delegate instanceof IPlanElementEditorInteraction<?> && activePlanElement != null) {
            IPlanElementEditorInteraction<?> pai = (IPlanElementEditorInteraction<?>) delegate;
            if (pai.hasChanges()) {
                activePlanElement.setElement(pai.getUpdatedElement());
                savePlanElementXmlToUndoManager();
            }
        }
        activePlanElement = null;
        delegate = null;
    }

    /**
     * @param source
     */
    private void endManeuverEdition(StateRenderer2D source) {
        delegate.setActive(false, source);
        synchronized (lock) {
            getPropertiesPanel().setManeuver(getPropertiesPanel().getManeuver());
            getPropertiesPanel().getEditBtn().setSelected(false);
            planElem.recalculateManeuverPositions(source);
            resetDelegate();
            saveManeuverXmlToUndoManager();
        }
    }

    private void saveManeuverXmlState() {
        Maneuver man = getPropertiesPanel().getManeuver();
        if (man != null)
            maneuverUndoRedoXml = getPropertiesPanel().getManeuver().getManeuverXml();
    }

    private void saveManeuverXmlToUndoManager() {
        Maneuver man = getPropertiesPanel().getManeuver();
        if (maneuverUndoRedoXml != null && man != null) {
            ManeuverChanged edit = new ManeuverChanged(man, plan, maneuverUndoRedoXml);
            maneuverUndoRedoXml = null;
            manager.addEdit(edit);
        }
    }

    private void savePlanElementXmlState() {
        if (activePlanElement != null)
            planElementUndoRedoXml = activePlanElement.getElementAsXml();
    }

    private void savePlanElementXmlToUndoManager() {
        if (planElementUndoRedoXml != null && activePlanElement != null) {
            PlanElementChanged pecEvt = new PlanElementChanged(activePlanElement, plan, planElementUndoRedoXml);
            planElementUndoRedoXml = null;
            manager.addEdit(pecEvt);
        }
    }

    /**
     * @param plan
     * @param properties
     * @param propertiesDefaults
     * @param psp
     * @return changes counter, 0 for all default values, 1 for changes but all equals, >1 not all with same values
     */
    protected int fillPropertiesWithAllChangesFromDefaults(PlanType plan, DefaultProperty[] properties,
            DefaultProperty[] propertiesDefaults, PropertySheetPanel psp) {
        Maneuver[] allManeuvers = plan.getGraph().getAllManeuvers();
        int[] countChangesArray = new int[properties.length];
        Arrays.fill(countChangesArray, 0);
        for (Maneuver man : allManeuvers) {
            ManeuverPayloadConfig payloadConfig = new ManeuverPayloadConfig(plan.getVehicle(), man, psp);
            DefaultProperty[] manProperties = payloadConfig.getProperties();
            // Assuming the order is the same
            for (int i = 0; i < manProperties.length; i++) {
                // if (properties[i].getValue().equals(propertiesDefaults[i].getValue()) &&
                // !properties[i].getValue().equals(manProperties[i].getValue())) {
                // properties[i].setValue(manProperties[i].getValue());
                // }
                if (!manProperties[i].getValue().equals(propertiesDefaults[i].getValue())) {
                    if (!manProperties[i].getValue().equals(properties[i].getValue())) {
                        properties[i].setValue(manProperties[i].getValue());
                    }
                    countChangesArray[i]++;
                }
            }
        }
        int sumAll = 0;
        int sumAll2 = 0;
        for (int i : countChangesArray) {
            sumAll += i;
            sumAll2 += (i == 0 ? allManeuvers.length : i);
        }
        if (sumAll == 0)
            return 0;
        else if (sumAll2 == countChangesArray.length * allManeuvers.length)
            return 1;
        else
            return 2;
    }

    private void refreshPropertiesManeuver() {
        if (getPropertiesPanel().getManeuver() != null) {
            synchronized (lock) {
                getPropertiesPanel().setManeuver(getPropertiesPanel().getManeuver());
            }
        }
    }

    protected AbstractAction getPasteAction(final Point mousePoint) {
        boolean enabled = isClipboardTextAManeuver();
        
        AbstractAction paste = new AbstractAction(I18n.text("Paste maneuver from clipboard")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                try {
                    String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
                    String xml = text.substring(maneuverPreamble.length());
                    Document doc = DocumentHelper.parseText(xml);

                    String maneuverType = doc.getRootElement().getName();
                    Maneuver m = mf.getManeuver(maneuverType);
                    if (m != null) {
                        m.loadManeuverFromXML(xml);
                        m.setId(getNewManeuverName(maneuverType));
                        if (m instanceof LocatedManeuver) {
                            ManeuverLocation originalPos = ((LocatedManeuver) m).getManeuverLocation().clone();
                            originalPos.convertToAbsoluteLatLonDepth();
                            LocationType pos = renderer.getRealWorldLocation(mousePoint);
                            originalPos.setLatitudeRads(pos.getLatitudeRads());
                            originalPos.setLongitudeRads(pos.getLongitudeRads());
                            ((LocatedManeuver) m).setManeuverLocation(originalPos);
                        }

                        Vector<TransitionType> addedTransitions = new Vector<TransitionType>();
                        Vector<TransitionType> removedTransitions = new Vector<TransitionType>();

                        plan.getGraph().addManeuver(m);
                        parsePlan();
                        if (plan.getGraph().getAllManeuvers().length > 1)
                            addedTransitions.add(plan.getGraph().addTransition(plan.getGraph().getLastManeuver().getId(),
                                    m.getId(), defaultCondition));
                        
                        planElem.recalculateManeuverPositions(renderer);

                        manager.addEdit(new ManeuverAdded(m, plan, addedTransitions, removedTransitions));

                        synchronized (lock) {
                            getPropertiesPanel().setManeuver(m);
                        }
                        
                        // PlanEditor.this.updateSelected(m);
                        planElem.setSelectedManeuver(m.id);

                        repaint();
                    }
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                }
            }
        };
        
        paste.putValue(AbstractAction.SMALL_ICON, new ImageIcon(ImageUtils.getImage("images/menus/editpaste.png")));
        paste.setEnabled(enabled);
        return paste;
    }

    private AbstractAction getPasteBeforeAction(Point mousePoint, Maneuver man) {
        boolean enabled = isClipboardTextAManeuver();
        
        AbstractAction paste = new AbstractAction(I18n.textf("Paste maneuver from clipboard before %man", man.getId())) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                try {
                    String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
                    String xml = text.substring(maneuverPreamble.length());
                    Document doc = DocumentHelper.parseText(xml);

                    String maneuverType = doc.getRootElement().getName();
                    Maneuver m = mf.getManeuver(maneuverType);
                    if (m != null) {
                        m.loadManeuverFromXML(xml);
                        m.setId(getNewManeuverName(maneuverType));

                        Vector<TransitionType> trans = plan.getGraph().getIncomingTransitions(man);

                        if (trans.isEmpty()) {
                            if (m instanceof LocatedManeuver) {
                                ManeuverLocation originalPos = ((LocatedManeuver) m).getManeuverLocation().clone();
                                LocationType pos = renderer.getRealWorldLocation(mousePoint);
                                originalPos.setLatitudeRads(pos.getLatitudeRads());
                                originalPos.setLongitudeRads(pos.getLongitudeRads());
                                ((LocatedManeuver) m).setManeuverLocation(originalPos);
                            }

                            String initial = plan.getGraph().getInitialManeuverId();
                            Vector<TransitionType> addedTransitions = new Vector<TransitionType>();
                            plan.getGraph().addManeuver(m);

                            if (initial != null)
                                addedTransitions.add(plan.getGraph().addTransition(m.getId(), initial, defaultCondition));

                            plan.getGraph().setInitialManeuver(m.getId());
                            parsePlan();
                            manager.addEdit(new ManeuverAdded(m, plan, addedTransitions, new Vector<TransitionType>()));

                            synchronized (lock) {
                                selectedManeuver = m;
                                getPropertiesPanel().setManeuver(m);
                            }
                            PlanEditor.this.updateSelected(m);
                        }
                        else {
                            String currManID = trans.firstElement().getSourceManeuver();
                            Maneuver nextMan = plan.getGraph().getFollowingManeuver(currManID);
                            Maneuver previousMan = plan.getGraph().getManeuver(currManID);
                            HashSet<TransitionType> addedTransitions = new HashSet<TransitionType>();
                            HashSet<TransitionType> removedTransitions = new HashSet<TransitionType>();

                            ManeuverLocation worldLoc = new ManeuverLocation(renderer.getCenter());
                            if (m instanceof LocatedManeuver)
                                worldLoc = ((LocatedManeuver) m).getManeuverLocation().clone();
                            else
                                worldLoc = new ManeuverLocation(renderer.getCenter());
                                
                            if (previousMan instanceof LocatedManeuver
                                    && nextMan instanceof LocatedManeuver) {
                                ManeuverLocation loc1 = ((LocatedManeuver) previousMan).getManeuverLocation().clone();
                                ManeuverLocation loc2 = ((LocatedManeuver) nextMan).getManeuverLocation().clone();

                                double offsets[] = loc2.getOffsetFrom(loc1);

                                loc1.translatePosition(offsets[0] / 2, offsets[1] / 2, 0);
                                loc1.setDepth(loc1.getDepth());
                                loc1.convertToAbsoluteLatLonDepth();
                                worldLoc.setLocation(loc1);
                            }
                            else {
                                if (previousMan instanceof LocatedManeuver) {
                                    LocationType loc1 = new LocationType(((LocatedManeuver) previousMan).getManeuverLocation());
                                    loc1.translatePosition(0, 30, 0);
                                    worldLoc.setLocation(loc1);
                                }
                                if (nextMan instanceof LocatedManeuver) {
                                    LocationType loc1 = new LocationType(((LocatedManeuver) nextMan).getManeuverLocation());
                                    loc1.translatePosition(0, -30, 0);
                                    worldLoc.setLocation(loc1);
                                }
                            }

                            if (m instanceof LocatedManeuver)
                                ((LocatedManeuver) m).setManeuverLocation(worldLoc);

                            plan.getGraph().addManeuver(m);

                            if (plan.getGraph().getExitingTransitions(previousMan).size() != 0) {
                                for (TransitionType exitingTrans : plan.getGraph().getExitingTransitions(previousMan)) {
                                    removedTransitions.add(plan.getGraph().removeTransition(exitingTrans.getSourceManeuver(), exitingTrans.getTargetManeuver()));
                                }
                            }

                            addedTransitions.add(plan.getGraph().addTransition(previousMan.getId(), m.getId(), defaultCondition));

                            if (nextMan != null) {
                                removedTransitions.add(plan.getGraph().removeTransition(previousMan.getId(), nextMan.getId()));
                                addedTransitions.add(plan.getGraph().addTransition(m.getId(), nextMan.getId(), defaultCondition));
                            }

                            parsePlan();

                            manager.addEdit(new ManeuverAdded(m, plan, addedTransitions, removedTransitions));
                            synchronized (lock) {
                                getPropertiesPanel().setManeuver(m);
                            }
                            PlanEditor.this.updateSelected(m);
                        }
                    }
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                }
            }
        };
        paste.putValue(AbstractAction.SMALL_ICON, new ImageIcon(ImageUtils.getImage("images/menus/editpaste.png")));
        paste.setEnabled(enabled);
        return paste;
    }

    /**
     * @return
     */
    private boolean isClipboardTextAManeuver() {
        Transferable contents = null;
        try {
            contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        }
        catch (Exception e1) {
            NeptusLog.pub().warn(e1);
        }
        
        boolean enabled = false;

        boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);

        if (hasTransferableText) {
            try {
                String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
                if (text.startsWith(maneuverPreamble))
                    enabled = true;
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
        return enabled;
    }

    @Override
    public void mouseDragged(MouseEvent e, StateRenderer2D renderer) {

        if (delegate != null) {
            delegate.mouseDragged(e, renderer);
//            getPropertiesPanel().setManeuver((Maneuver) delegate);
            return;
        }

        if (lastDragPoint == null && selectedManeuver != null) {
            selectedManeuver = planElem.iterateManeuverBack(e.getPoint());
            synchronized (lock) {
                getPropertiesPanel().setManeuver(selectedManeuver);
                getPropertiesPanel().getEditBtn().setEnabled(selectedManeuver instanceof StateRendererInteraction);
                getPropertiesPanel().getEditBtn().setSelected(false);
                getPropertiesPanel().setManager(manager);
                getPropertiesPanel().setPlan(plan);
            }
        }

        if (selectedManeuver != null && selectedManeuver instanceof LocatedManeuver) {

            if (lastDragPoint != null) {

                maneuverWasMoved = true;
                double diffX = e.getPoint().getX() - lastDragPoint.getX();
                double diffY = e.getPoint().getY() - lastDragPoint.getY();

                if (e.isControlDown()) {
                    LocationType oldPt = renderer.getRealWorldLocation(lastDragPoint);
                    LocationType newPt = renderer.getRealWorldLocation(e.getPoint());

                    double[] offsets = newPt.getOffsetFrom(oldPt);

                    planElem.translatePlan(offsets[0], offsets[1], 0);
                    lastDragPoint = e.getPoint();
                    planTranslated = true;
                }
                else {
                    if (e.isShiftDown()) {
                        double ammount = Math.toRadians(lastDragPoint.getY() - e.getPoint().getY());
                        planElem.rotatePlan((LocatedManeuver) selectedManeuver, ammount);
                        lastDragPoint = e.getPoint();
                        planRotatedRads += ammount;
                    }
                    else {
                        Point2D newManPos = planElem.translateManeuverPosition(selectedManeuver.getId(), diffX, diffY);
                        ManeuverLocation loc = ((LocatedManeuver) selectedManeuver).getManeuverLocation();
                        loc.setLocation(renderer.getRealWorldLocation(newManPos));
                        ((LocatedManeuver) selectedManeuver).setManeuverLocation(loc);
                        lastDragPoint = newManPos;
                    }
                }
                renderer.repaint();
            }
            else {
                lastDragPoint = e.getPoint();
                renderer.setCursor(Cursor.getDefaultCursor());
            }
        }
        else {
            super.mouseDragged(e, renderer);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e, StateRenderer2D renderer) {

        if (plan == null || selectedManeuver != null) {
            super.mouseMoved(e, renderer);
            return;
        }

        String[] mans = planElem.getManeuversUnder(e.getPoint());
        if (mans.length > 0) {
            renderer.setCursor(Cursor.getDefaultCursor());

            String text = plan.getGraph().getManeuver(mans[0]).getTooltipText();

            for (int i = 1; i < mans.length && i < 3; i++) {
                String tooltip = plan.getGraph().getManeuver(mans[i]).getTooltipText();
                if (tooltip.startsWith("<html>"))
                    tooltip = tooltip.substring(6);
                text = text + "<br><br>" + tooltip;
            }

            if (mans.length >= 4)
                text = text + "<br><b>(...)</b>";
            renderer.setToolTipText(text);
        }

        super.mouseMoved(e, renderer);
    }

    @Override
    public void mousePressed(MouseEvent event, StateRenderer2D renderer) {

        planTranslated = false;
        planRotatedRads = 0;

        if (delegate != null) {
            delegate.mousePressed(event, renderer);
            if (delegate instanceof Maneuver) {
                synchronized (lock) {
                    getPropertiesPanel().setManeuver((Maneuver) delegate);
                }
            }
            return;
        }

        if (event.getButton() == MouseEvent.BUTTON1) {
            if (planElem != null && event.getPoint() != null) {
                selectedManeuver = planElem.iterateManeuverUnder(event.getPoint());
                if (overviewPanel != null)
                    overviewPanel.setSelectedManeuver(selectedManeuver);
                lastDragPoint = event.getPoint();
                if (selectedManeuver != null && selectedManeuver instanceof LocatedManeuver) {
                    maneuverLocationBeforeMoving = ((LocatedManeuver) selectedManeuver).getManeuverLocation();
                }
                if (selectedManeuver != getPropertiesPanel().getManeuver()) {
                    if (getPropertiesPanel().getManeuver() != null && getPropertiesPanel().isChanged()) {
                        ManeuverChanged edit = new ManeuverChanged(getPropertiesPanel().getManeuver(), plan,
                                getPropertiesPanel().getBeforeXml());
                        manager.addEdit(edit);
                    }

                    synchronized (lock) {
                        getPropertiesPanel().setPlan(plan); // This call has to be before setManeuver (pdias 20130822)
                        getPropertiesPanel().setManeuver(selectedManeuver);
                        getPropertiesPanel().setManager(manager);

                        getPropertiesPanel().getEditBtn().setEnabled(selectedManeuver instanceof StateRendererInteraction);
                        getPropertiesPanel().getEditBtn().setSelected(false);
                    }
                }
                if (selectedManeuver == null)
                    super.mousePressed(event, renderer);
            }
        }
        else {
            super.mousePressed(event, renderer);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e, StateRenderer2D renderer) {

        if (delegate != null) {
            delegate.mouseReleased(e, renderer);
            if (delegate instanceof Maneuver) {
                synchronized (lock) {
                    getPropertiesPanel().setManeuver((Maneuver) delegate);
                }
            }
            return;
        }

        if (selectedManeuver != null) {
            if (planTranslated) {
                LocatedManeuver locProvider = (LocatedManeuver) selectedManeuver;
                LocationType curLocation = locProvider.getManeuverLocation();

                double[] offsets = curLocation.getOffsetFrom(maneuverLocationBeforeMoving);
                PlanTranslated edit = new PlanTranslated(plan, offsets[1], offsets[0]);
                manager.addEdit(edit);

            }
            else if (planRotatedRads != 0) {
                PlanRotated edit = new PlanRotated(plan, (LocatedManeuver) selectedManeuver, planRotatedRads);
                manager.addEdit(edit);
            }
            else if (maneuverWasMoved) {
                LocatedManeuver locProvider = (LocatedManeuver) selectedManeuver;
                ManeuverLocation after = locProvider.getManeuverLocation().clone();
                ManeuverLocation before = maneuverLocationBeforeMoving.clone();
                manager.addEdit(new ManeuverTranslated(locProvider, plan, before, after));
            }
           
            maneuverWasMoved = false;
            maneuverLocationBeforeMoving = null;

            planElem.recalculateManeuverPositions(renderer);
            synchronized (lock) {
                getPropertiesPanel().setPlan(plan);
                getPropertiesPanel().setManeuver(selectedManeuver);
                getPropertiesPanel().getEditBtn().setEnabled(selectedManeuver instanceof StateRendererInteraction);
                getPropertiesPanel().getEditBtn().setSelected(false);
                getPropertiesPanel().setManager(manager);
            }
        }
        lastDragPoint = null;
        selectedManeuver = null;
        mouseMoved(e, renderer);

        super.mouseReleased(e, renderer);
    }

    @Override
    public void keyPressed(KeyEvent event, StateRenderer2D source) {
        if (delegate != null) {
            if (event.getKeyCode() == KeyEvent.VK_ESCAPE)
                endManeuverEdition(source);
            else
                delegate.keyPressed(event, source);
        }
        else {
            if (event.getKeyCode() == KeyEvent.VK_Z && event.isControlDown()) {
                getUndoAction().actionPerformed(new ActionEvent(this, 0, ""));
                event.consume();
            }
            else if (event.getKeyCode() == KeyEvent.VK_Y && event.isControlDown()) {
                getRedoAction().actionPerformed(new ActionEvent(this, 0, ""));
                event.consume();
            }

            super.keyPressed(event, source);
        }
    }

    @Override
    public void keyReleased(KeyEvent event, StateRenderer2D source) {
        if (delegate != null)
            delegate.keyReleased(event, source);
        else
            super.keyReleased(event, source);
    }

    @Override
    public void keyTyped(KeyEvent event, StateRenderer2D source) {
        if (delegate != null)
            delegate.keyTyped(event, source);
        else
            super.keyTyped(event, source);
    }

    private void removeManeuver(String manID) {
        Maneuver man = plan.getGraph().getManeuver(manID);
        Maneuver next = plan.getGraph().getFollowingManeuver(manID);
        boolean wasInitial = man.isInitialManeuver();
        
        Vector<TransitionType> addedTransitions = new Vector<TransitionType>();
        Vector<TransitionType> removedTransitions = new Vector<TransitionType>();

        if (next != null)
            removedTransitions.add(plan.getGraph().removeTransition(manID, next.getId()));

        for (Maneuver previous : plan.getGraph().getPreviousManeuvers(manID)) {
            removedTransitions.add(plan.getGraph().removeTransition(previous.getId(), manID));

            if (next != null && previous != null)
                addedTransitions.add(plan.getGraph().addTransition(previous.getId(), next.getId(), defaultCondition));
        }
        plan.getGraph().removeManeuver(man);

        planElem.recalculateManeuverPositions(renderer);
        renderer.repaint();

        if (man.isInitialManeuver() && next != null) {
            plan.getGraph().setInitialManeuver(next.getId());
        }

        ManeuverRemoved edit = new ManeuverRemoved(man, plan, addedTransitions, removedTransitions,
                wasInitial);
        manager.addEdit(edit);

    }

    private Maneuver addManeuverAfter(String manType, String manID) {
        Maneuver nextMan = plan.getGraph().getFollowingManeuver(manID);
        Maneuver previousMan = plan.getGraph().getManeuver(manID);
        ManeuverLocation worldLoc = new ManeuverLocation(renderer.getCenter());

        HashSet<TransitionType> addedTransitions = new HashSet<TransitionType>();
        HashSet<TransitionType> removedTransitions = new HashSet<TransitionType>();

        
        if (previousMan instanceof LocatedManeuver
                && nextMan instanceof LocatedManeuver) {
            ManeuverLocation loc1 = ((LocatedManeuver) previousMan).getManeuverLocation().clone();
            ManeuverLocation loc2 = ((LocatedManeuver) nextMan).getManeuverLocation().clone();

            double offsets[] = loc2.getOffsetFrom(loc1);

            loc1.translatePosition(offsets[0] / 2, offsets[1] / 2, 0);
            loc1.setDepth(loc1.getDepth());
            worldLoc.setLocation(loc1);
        }
        else {
            if (previousMan instanceof LocatedManeuver) {
                LocationType loc1 = new LocationType(((LocatedManeuver) previousMan).getManeuverLocation());
                loc1.translatePosition(0, 30, 0);
                worldLoc.setLocation(loc1);
            }
            if (nextMan instanceof LocatedManeuver) {
                LocationType loc1 = new LocationType(((LocatedManeuver) nextMan).getManeuverLocation());
                loc1.translatePosition(0, -30, 0);
                worldLoc.setLocation(loc1);
            }
        }
        
        Maneuver newMan = create(manType, worldLoc, previousMan);
        
        
        if (newMan == null) {
            GuiUtils.errorMessage(this, I18n.text("Error adding maneuver"),
                    I18n.textf("The maneuver %maneuverType can't be added", manType));
            return null;
        }
        
        plan.getGraph().addManeuver(newMan);

        if (plan.getGraph().getExitingTransitions(previousMan).size() != 0) {
            for (TransitionType exitingTrans : plan.getGraph().getExitingTransitions(previousMan)) {
                removedTransitions.add(plan.getGraph().removeTransition(exitingTrans.getSourceManeuver(), exitingTrans.getTargetManeuver()));
            }
        }

        addedTransitions.add(plan.getGraph().addTransition(previousMan.getId(), newMan.getId(), defaultCondition));
        
        if (nextMan != null) {
            removedTransitions.add(plan.getGraph().removeTransition(previousMan.getId(), nextMan.getId()));
            addedTransitions.add(plan.getGraph().addTransition(newMan.getId(), nextMan.getId(), defaultCondition));
        }
        

        parsePlan();
        
        manager.addEdit(new ManeuverAdded(newMan, plan, addedTransitions, removedTransitions));
        synchronized (lock) {
            getPropertiesPanel().setManeuver(newMan);
        }
        
        return newMan;
    }

    private Maneuver create(String manType, LocationType worldLoc, Maneuver copyFrom) {
        Maneuver man = mf.getManeuver(manType);
        if (man == null)
            return null;
        
        man.setVehicle(plan.getVehicleType());
        
        if (copyFrom != null) {
            try {
                man.setProperties(copyFrom.getProperties());
            }
            catch (Exception e) {
                NeptusLog.pub().error(e, e);
            }
            man.cloneActions(copyFrom);
        }

        if (man instanceof LocatedManeuver) {
            ManeuverLocation lt = ((LocatedManeuver) man).getManeuverLocation();
            lt.setLocation(worldLoc);
            if (copyFrom != null && copyFrom instanceof LocatedManeuver) {
                ManeuverLocation l = ((LocatedManeuver) copyFrom).getManeuverLocation();
                lt.setZ(l.getZ());
                lt.setZUnits(l.getZUnits());
            }
            ((LocatedManeuver) man).setManeuverLocation(lt);
        }

        man.setId(getNewManeuverName(manType));

        return man;
    }

    private Maneuver addManeuverAtBeginning(String manType, Point loc) {
        String initial = plan.getGraph().getInitialManeuverId();
        Maneuver man, copyFrom = null;
        if (initial != null)
            copyFrom = plan.getGraph().getManeuver(plan.getGraph().getInitialManeuverId());

        man = create(manType, renderer.getRealWorldLocation(loc), copyFrom);

        Vector<TransitionType> addedTransitions = new Vector<TransitionType>();
        plan.getGraph().addManeuver(man);
        
        if (initial != null)
            addedTransitions.add(plan.getGraph().addTransition(man.getId(), initial, defaultCondition));

        
        plan.getGraph().setInitialManeuver(man.getId());
        parsePlan();
        manager.addEdit(new ManeuverAdded(man, plan, addedTransitions, new Vector<TransitionType>()));

        synchronized (lock) {
            selectedManeuver = man;
            getPropertiesPanel().setManeuver(man);
        }
        
        return man;

    }

    private Maneuver addManeuverAtEnd(Point loc, String manType) {

        Maneuver lastMan = plan.getGraph().getLastManeuver();
        
        LocationType worldLoc = renderer.getRealWorldLocation(loc);
        Maneuver man = create(manType, worldLoc, lastMan);
        plan.getGraph().addManeuver(man);
        
        Vector<TransitionType> addedTransitions = new Vector<TransitionType>();
        Vector<TransitionType> removedTransitions = new Vector<TransitionType>();

        if (man == null) {
            GuiUtils.errorMessage(this, I18n.text("Error adding maneuver"),
                    I18n.textf("The maneuver %maneuverType can't be added", manType));
            return null;
        }

        if (lastMan != null && lastMan != man) {

            if (plan.getGraph().getExitingTransitions(lastMan).size() != 0) {
                for (TransitionType exitingTrans : plan.getGraph().getExitingTransitions(lastMan)) {
                    removedTransitions.add(plan.getGraph().removeTransition(exitingTrans.getSourceManeuver(),
                            exitingTrans.getTargetManeuver()));
                }
            }

            addedTransitions.add(plan.getGraph().addTransition(lastMan.getId(), man.getId(), defaultCondition));

        }

        if (man instanceof LocatedManeuver) {
            ManeuverLocation l = new ManeuverLocation();
            l.setLocation(renderer.getRealWorldLocation(loc));
            l.setZ(((LocatedManeuver) man).getManeuverLocation().getZ());
            l.setZUnits(((LocatedManeuver) man).getManeuverLocation().getZUnits());
            ((LocatedManeuver) man).setManeuverLocation(l);
        }

        plan.getGraph().addManeuver(man);
        parsePlan();
        manager.addEdit(new ManeuverAdded(man, plan, addedTransitions, removedTransitions));

        synchronized (lock) {
            getPropertiesPanel().setManeuver(man);
        }

        if (lastMan == null) {
            synchronized (lock) {
                selectedManeuver = null;
                getPropertiesPanel().setManeuver(null);
            }
        }
        else {
            planElem.setSelectedManeuver(man.id);
        }
        return man;
    }

    private String getNewManeuverName(String manType) {

        int i = 1;
        while (plan.getGraph().getManeuver(manType + i) != null)
            i++;

        return manType + i;
    }

    public void reset() {
        planElem.recalculateManeuverPositions(renderer);
        repaint();
    }

    public PlanElement getPlanElem() {
        return planElem;
    }

    public StateRenderer2D getRenderer() {
        return renderer;
    }

    public MissionType getMission() {
        return mission;
    }

    @Override
    public void missionReplaced(MissionType mission) {
        this.mission = mission;
    }

    @Override
    public void missionUpdated(MissionType mission) {
        this.mission = mission;
    }

    @Override
    public void initSubPanel() {
        this.mission = getConsole().getMission();

        addMenuItem(I18n.text("Tools") + ">" + I18n.text("Generate plan..."), ImageUtils.getIcon("images/planning/template.png"), new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                new PlanTemplatesDialog(getConsole()).showDialog();
            }
        });
    }

    public void updateSelected(Maneuver m) {
        if (m != null) {
            synchronized (lock) {
                getPropertiesPanel().setManeuver(m);
            }
            planElem.setSelectedManeuver(m.id);
            if (overviewPanel != null)
                overviewPanel.setSelectedManeuver(m);
        }
    }

    public static String getManeuverpreamble() {
        return maneuverPreamble;
    }
}
