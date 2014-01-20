/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2009/09/21
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

import pt.lsts.imc.PlanControlState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.ConsoleSystem;
import pt.lsts.neptus.console.MainPanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.ConsoleVehicleChangeListener;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.console.plugins.containers.LayoutProfileProvider;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.gui.VehicleChooser;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mp.templates.AbstractPlanTemplate;
import pt.lsts.neptus.mp.templates.ScriptedPlanTemplate;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.CustomInteractionSupport;
import pt.lsts.neptus.renderer2d.FeatureFocuser;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.Renderer;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.renderer2d.VehicleStateListener;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.ReflectionUtil;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 * @author Paulo Dias
 */
@PluginDescription(name = "Map Panel", icon = "images/planning/planning.png", author = "ZP, Paulo Dias", documentation = "planning/planning_panel.html", category = CATEGORY.INTERFACE)
public class MapPanel extends ConsolePanel implements MainVehicleChangeListener, MissionChangeListener,
PlanChangeListener, IPeriodicUpdates, ILayerPainter, ConfigurationListener, IMapPopup, 
CustomInteractionSupport, VehicleStateListener, ConsoleVehicleChangeListener {

    private static final long serialVersionUID = 1L;

    private final ImageIcon TRANSLATE_ICON = ImageUtils.getScaledIcon(
            "images/planning/translate_btn.png", 16, 16);
    private final ImageIcon MINIMIZE_ICON = ImageUtils.getScaledIcon(
            "images/planning/minimize.png", 16, 16);
    private final ImageIcon MAXIMIZE_ICON = ImageUtils.getScaledIcon(
            "images/planning/maximize.png", 16, 16);
    private final ImageIcon TEMPLATE_ICON = ImageUtils
            .getIcon("images/planning/template.png");
    private final ImageIcon ADD_PLAN_ICON = ImageUtils.getScaledIcon(
            "images/planning/template.png", 16, 16);
    private final ImageIcon TAIL_ICON = ImageUtils.getScaledIcon(
            "images/planning/tailOnOff.png", 16, 16);
    private final ImageIcon FOLLOW_SYSTEM_ICON = ImageUtils.getScaledIcon(
            "images/planning/lock_vehicle.png", 16, 16);
    private final ImageIcon RULER_ICON = ImageUtils.getScaledIcon(
            "images/planning/ruler_btn.png", 16, 16);
    private final ImageIcon ROTATE_ICON = ImageUtils.getScaledIcon(
            "images/planning/rotate_btn.png", 16, 16);
    private final ImageIcon ZOOM_ICON = ImageUtils.getScaledIcon(
            "images/planning/zoom_btn.png", 16, 16);

    private static final String scriptsDir = "conf/planscripts/";

    @NeptusProperty(name = "Show world map")
    public boolean worldMapShown = true;

    @NeptusProperty
    public int updateMillis = 100;

    @NeptusProperty(name = "Save mission states", description = "Save the mission states whenever the mission is changed")
    public boolean saveMissionStates = false;

    @NeptusProperty(name = "Smooth image resizing")
    public boolean smoothResize = false;

    @NeptusProperty(name = "Antialiasing")
    public boolean antialias = true;

    @NeptusProperty(name = "Interpolate States")
    public boolean interpolate = false;

    @NeptusProperty(name = "Fixed Vehicle Size", description = "Vehicle icon size (0 for real size)")
    public int fixedSize = 0;

    public enum PlacementEnum {
        Left,
        Right,
        Top,
        Bottom
    }

    @NeptusProperty(name = "Toolbar placement", description = "Where to place the toolbar")
    public PlacementEnum toolbarPlacement = PlacementEnum.Bottom;

    protected StateRenderer2D renderer = new StateRenderer2D();
    protected String planId = null;
    protected boolean editing = false;
    protected ToolbarSwitch followSwitch, zoomSwitch, translateSwitch, rotateSwitch, rulerSwitch, tailSwitch,
            editSwitch, maximizeSwitch;
    protected AbstractAction followMode, zoomMode, translateMode, rotateMode, rulerMode, tailMode, editMode, addPlan,
            maximizeMode, addTemplate;
    protected JLabel status = new JLabel();
    protected PlanElement mainPlanPainter = null;
    protected ButtonGroup bg = new ButtonGroup();
    protected JToolBar bottom = new JToolBar(JToolBar.VERTICAL);

    protected LinkedHashMap<VehicleType, SystemPositionAndAttitude> vehicles = new LinkedHashMap<VehicleType, SystemPositionAndAttitude>();
//    protected MessageListener<MessageInfo, IMCMessage> imcMessageListener;
    
    // Interaction variables
    protected LinkedHashMap<String, StateRendererInteraction> interactionModes = new LinkedHashMap<String, StateRendererInteraction>();
    protected LinkedHashMap<String, ToolbarSwitch> interactionButtons = new LinkedHashMap<String, ToolbarSwitch>();

//    protected long lastStamp = 0;
//    protected SystemPositionAndAttitude lastState = null;
//    protected EstimatedStateGenerator stateGenerator = new EstimatedStateGenerator();
    
    private MissionType mission = null;
    private PlanElement planElem;
    private MapGroup mapGroup = null;

    public MapPanel(ConsoleLayout console) {
        super(console);
        removeAll();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder()); //        editor.setEditable(false);

        renderer.setMinDelay(0);
        renderer.setShowWorldMapOnScreenControls(true);
        add(renderer, BorderLayout.CENTER);
        bottom.setFloatable(false);
        bottom.setAlignmentX(JToolBar.CENTER_ALIGNMENT);
        renderer.addMenuExtension(new FeatureFocuser());
        translateMode = new AbstractAction(I18n.text("Translate"), TRANSLATE_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    if (getActiveInteraction() != null && getActiveInteraction().isExclusive())
                        getActiveInteraction().setActive(false, renderer);
                    setActiveInteraction(null);
                    renderer.followVehicle(null);
                    renderer.setViewMode(Renderer.TRANSLATION);
                }
            }

        };
        translateSwitch = new ToolbarSwitch(translateMode);
        bg.add(translateSwitch);
        bottom.add(translateSwitch);
        translateSwitch.setSelected(true);

        zoomMode = new AbstractAction(I18n.text("Zoom"), ZOOM_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    renderer.followVehicle(null);
                    renderer.setViewMode(Renderer.ZOOM);
                    if (getActiveInteraction() != null && getActiveInteraction().isExclusive())
                        getActiveInteraction().setActive(false, renderer);
                    setActiveInteraction(null);
                }
                else {
                    translateSwitch.doClick();
                }
            }
        };
        zoomSwitch = new ToolbarSwitch(zoomMode);
        bg.add(zoomSwitch);
        bottom.add(zoomSwitch);

        rotateMode = new AbstractAction(I18n.text("Rotate"), ROTATE_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    renderer.followVehicle(null);
                    renderer.setViewMode(Renderer.ROTATION);
                    if (getActiveInteraction() != null && getActiveInteraction().isExclusive())
                        getActiveInteraction().setActive(false, renderer);
                    setActiveInteraction(null);
                }
                else {
                    translateSwitch.doClick();
                }
            }
        };
        rotateSwitch = new ToolbarSwitch(rotateMode);
        bg.add(rotateSwitch);
        bottom.add(rotateSwitch);

        rulerMode = new AbstractAction(I18n.text("Ruler"), RULER_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    renderer.followVehicle(null);
                    renderer.setViewMode(Renderer.RULER);
                    if (getActiveInteraction() != null && getActiveInteraction().isExclusive())
                        getActiveInteraction().setActive(false, renderer);
                    setActiveInteraction(null);
                }
                else {
                    translateSwitch.doClick();
                }
            }
        };
        rulerSwitch = new ToolbarSwitch(rulerMode);
        bg.add(rulerSwitch);
        bottom.add(rulerSwitch);

        followMode = new AbstractAction(I18n.text("Lock Vehicle"), FOLLOW_SYSTEM_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    if (getActiveInteraction() != null && getActiveInteraction().isExclusive())
                        getActiveInteraction().setActive(false, renderer);
                    setActiveInteraction(null);
                    String mainVehicle = getConsole().getMainSystem();
                    if (mainVehicle != null)
                        renderer.followVehicle(VehiclesHolder.getVehicleById(mainVehicle).getId());
                }
                else {
                    renderer.followVehicle(null);
                }
            }
        };
        followSwitch = new ToolbarSwitch(followMode);
        bg.add(followSwitch);
        bottom.add(followSwitch);

        tailMode = new AbstractAction(I18n.text("Show tail"), TAIL_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    String mainVehicle = getConsole().getMainSystem();
                    if (mainVehicle != null)
                        renderer.setVehicleTailOn(null);
                }
                else {
                    renderer.setVehicleTailOff(null);
                }
            }
        };
        tailSwitch = new ToolbarSwitch(tailMode);
        tailSwitch.setSelected(false);
        bottom.add(tailSwitch);

        bottom.addSeparator(new Dimension(0, 20));

        addTemplate = new AbstractAction(I18n.text("Add plan from template"), ADD_PLAN_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                VehicleType choice = null;
                if (getConsole().getMainSystem() != null)
                    choice = VehicleChooser.showVehicleDialog(
                            VehiclesHolder.getVehicleById(getConsole().getMainSystem()), getConsole());
                else
                    choice = VehicleChooser.showVehicleDialog(getConsole());

                if (choice == null)
                    return;

                Class<?>[] classes = ReflectionUtil.listPlanTemplates();
                File[] scripts = new File(scriptsDir).listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.endsWith("js");
                    }
                });

                String[] names = new String[classes.length + scripts.length];
                for (int i = 0; i < classes.length; i++) {
                    names[i] = PluginUtils.getPluginName(classes[i]);
                }
                for (int i = 0; i < scripts.length; i++) {
                    names[classes.length + i] = scripts[i].getName();
                }

                JOptionPane jop = new JOptionPane(I18n.text("Choose the plan template"), JOptionPane.QUESTION_MESSAGE,
                        JOptionPane.DEFAULT_OPTION, TEMPLATE_ICON);

                jop.setSelectionValues(names);
                jop.setInitialSelectionValue(names[0]);

                JDialog dialog = jop.createDialog(getConsole(), I18n.text("Add plan template"));
                dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                dialog.setVisible(true);

                Object option = jop.getInputValue();
                if (option == null)
                    return;

                PlanType plan = null;

                if (option.toString().endsWith("js")) {
                    ScriptedPlanTemplate planTemplate = new ScriptedPlanTemplate();
                    String source = FileUtil.getFileAsString(scriptsDir + option);
                    // NeptusLog.pub().info("<###> "+scriptsDir+option+":\n"+source);
                    planTemplate.setSource(source);

                    planTemplate.setMission(getConsole().getMission());
                    PropertiesEditor.editProperties(planTemplate, true);
                    try {
                        plan = planTemplate.generatePlan();
                        // NeptusLog.pub().info("<###> "+plan.asXML());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Class<?> c = classes[0];
                    for (int i = 0; i < classes.length; i++) {
                        if (PluginUtils.getPluginName(classes[i]).equals(option)) {
                            c = classes[i];
                            break;
                        }
                        else {
                            NeptusLog.pub().info("<###> "+option + " != " + PluginUtils.getPluginName(classes[i]));
                        }
                    }

                    plan = AbstractPlanTemplate.addTemplateToMission(getConsole(), getConsole().getMission(), c);
                    if (plan == null)
                        return;

                    // This test is being done just to verify if some error is occurring at the plan generation
                    try {
                        plan.validatePlan();
                    }
                    catch (Exception e) {
                        GuiUtils.errorMessage(getConsole(), e);
                    }
                }
                if (plan == null)
                    return;

                plan.setVehicle(choice);
                plan.setMissionType(getConsole().getMission());
                final PlanType p = plan;

                new Thread() {
                    @Override
                    public void run() {
                        getConsole().getMission().addPlan(p);
                        getConsole().getMission().save(true);
                        getConsole().updateMissionListeners();
                        GuiUtils.infoMessage(getConsole(), I18n.text("Plan template"),
                                I18n.textf("The plan %planid was added to this mission", p.getId()));
                    };
                }.run();

            }
        };
        addTemplate.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Add plan from template"));
        bottom.add(new ToolbarButton(addTemplate));

        bottom.addSeparator(new Dimension(0, 20));

        maximizeMode = new AbstractAction(I18n.text("Maximize"), MAXIMIZE_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    if (MapPanel.this.getParent() instanceof LayoutProfileProvider) {
                        LayoutProfileProvider glc = (LayoutProfileProvider) MapPanel.this.getParent();
                        glc.maximizePanelOnContainer(MapPanel.this);
                    }
                    else if (getConsole() != null)
                        getConsole().maximizePanel(MapPanel.this);
                    maximizeMode.putValue(AbstractAction.SMALL_ICON, MINIMIZE_ICON);
                    maximizeMode.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Minimize"));
                }
                else {
                    if (MapPanel.this.getParent() instanceof LayoutProfileProvider) {
                        LayoutProfileProvider glc = (LayoutProfileProvider) MapPanel.this.getParent();
                        glc.maximizePanelOnContainer(null);
                    }
                    else if (getConsole() != null)
                        getConsole().minimizePanel(MapPanel.this);
                    maximizeMode.putValue(AbstractAction.SMALL_ICON, MAXIMIZE_ICON);
                    maximizeMode.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Maximize"));
                }
            }
        };
        
        maximizeSwitch = new ToolbarSwitch(maximizeMode);
        maximizeSwitch.setSelected(false);
        bottom.add(maximizeSwitch);
        bottom.addSeparator();

        status.setForeground(Color.red);
        bottom.add(status);

        setToolbarPlacement();

        renderer.addMenuExtension(new IEditorMenuExtension() {
            @Override
            public Collection<JMenuItem> getApplicableItems(LocationType loc, IMapPopup source) {
                JMenuItem item = new JMenuItem(I18n.text("Choose visible layers"));
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        renderer.painterSelection();
                    }
                });
                return Arrays.asList(item);
            }
        });
    }

    @Override
    public void initSubPanel() {
        setSize(500, 500);

        followMode.setEnabled(getConsole().getMainSystem() != null);
        
        setMission(getConsole().getMission());

        getConsole().addConsoleVehicleListener(this);
        
        for (ConsoleSystem v : getConsole().getSystems().values()) {   
            v.addRenderFeed(this);
        }
        if (getConsole() != null) {
            // NeptusLog.pub().info("<###>--------------------" +this.getParent());
            if (this.getParent() instanceof MainPanel || (this.getParent() instanceof LayoutProfileProvider &&
                    ((LayoutProfileProvider) this.getParent()).supportsMaximizePanelOnContainer())) {
                maximizeMode.setEnabled(true);
            }
            else {
                maximizeMode.setEnabled(false);
                maximizeSwitch.setVisible(false);
            }
        }
        else {
            maximizeMode.setEnabled(false);
            maximizeSwitch.setVisible(false);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#clean()
     */
    @Override
    public void cleanSubPanel() {

        ImcMsgManager.getManager().removeListenerFromAllSystems(this);
        getConsole().removeRenderAll(this);
    }

    /**
     * @return the showOnScreenControls
     */
    public boolean isShowWorldMapOnScreenControls() {
        return renderer.isShowWorldMapOnScreenControls();
    }

    /**
     * @param showOnScreenControls the showOnScreenControls to set
     */
    public void setShowWorldMapOnScreenControls(boolean showOnScreenControls) {
        renderer.setShowWorldMapOnScreenControls(showOnScreenControls);
    }

    private void setToolbarPlacement() {
        // Orient and position the tab based on placement property (toolbarPlacement)
        String position = BorderLayout.SOUTH;
        switch (toolbarPlacement) {
            case Left:
                position = BorderLayout.WEST;
                bottom.setOrientation(JToolBar.VERTICAL);
                break;
            case Right:
                position = BorderLayout.EAST;
                bottom.setOrientation(JToolBar.VERTICAL);
                break;
            case Top:
                position = BorderLayout.NORTH;
                bottom.setOrientation(JToolBar.HORIZONTAL);
                break;
            case Bottom:
                position = BorderLayout.SOUTH;
                bottom.setOrientation(JToolBar.HORIZONTAL);
                break;
        }
        add(bottom, position);
    }

    @Subscribe
    public void consume(PlanControlState message) {

        if(getConsole().getMainSystem() != null)
            if (!message.getSourceName().equals(getConsole().getMainSystem()))
                return;
        
        String planId = message.getPlanId();
        String manId = message.getManId();

        if (getConsole().getPlan() != null && getConsole().getPlan().getId().equals(planId)) {
            if (mainPlanPainter != null)
                mainPlanPainter.setActiveManeuver(manId);
        }
    }


    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.plugins.MissionChangeListener#missionReplaced(pt.lsts.neptus.types.mission.MissionType)
     */
    @Override
    public void missionReplaced(MissionType mission) {
        // editor.setMission(mission);
        setMission(mission);
        if (addPlan != null)
            addPlan.setEnabled(mission != null);
    }

    @Override
    public void missionUpdated(MissionType mission) {
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange evt) {
        followMode.setEnabled(evt.getCurrent() != null);
        if (mainPlanPainter != null)
            mainPlanPainter.setActiveManeuver(null);
    }

    @Override
    public void planChange(PlanType plan) {
        StateRenderer2D r2d = renderer;
        if (mainPlanPainter != null)
            r2d.removePostRenderPainter(mainPlanPainter);

        if (plan != null) {
            PlanElement po = new PlanElement(r2d.getMapGroup(), new MapType());
            po.setMissionType(plan.getMissionType());
            po.setPlan(plan);
            po.setRenderer(r2d);
            po.setColor(new Color(255, 255, 255, 128));
            po.setShowDistances(false);
            po.setShowManNames(false);
            r2d.addPostRenderPainter(po, I18n.text("Plan Painter"));
            mainPlanPainter = po;
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        return updateMillis;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
        try {
            for (VehicleType vt : vehicles.keySet()) {
                renderer.vehicleStateChanged(vt.getId(), vehicles.get(vt), false);
            }
            renderer.repaint();
        }
        catch (Exception e) {
            NeptusLog.pub().debug(e);
        }
        return true;
    }

    public void editPlan(PlanType plan) {
//        editor.setPlan(plan);
//        editor.setEditable(true);
        editSwitch.setSelected(true);
        editSwitch.setEnabled(true);
    }

    public void setMission(MissionType mission) {
        if (mission == null)
            return;

        mapGroup = MapGroup.getMapGroupInstance(mission);
        renderer.setMapGroup(mapGroup);
        this.mission = mission;
    }

    public void setPlan(PlanType plan) {
        if (plan == null) {
            renderer.removePostRenderPainter(planElem);
            return;
        }
        if (plan.getMissionType() != mission)
            setMission(plan.getMissionType());

        renderer.removePostRenderPainter(planElem);

        planElem = new PlanElement(mapGroup, new MapType());
        planElem.setTransp2d(1.0);
        renderer.addPostRenderPainter(planElem, "Plan Layer");
        planElem.setRenderer(renderer);
        planElem.setPlan(plan);

        renderer.repaint();
    }

    @Override
    public void propertiesChanged() {
        renderer.setSmoothResizing(smoothResize);
        renderer.setAntialiasing(antialias);
        renderer.setFixedVehicleWidth(fixedSize);
        renderer.setWorldMapShown(worldMapShown);
        setToolbarPlacement(); // Refresh toolbar position
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.CustomInteractionSupport#addInteraction(pt.lsts.neptus.renderer2d.StateRendererInteraction)
     */
    @Override
    public void addInteraction(StateRendererInteraction interaction) {
        try {
            final String name = interaction.getName();
            if (!interactionModes.containsKey(name))
                interactionModes.put(name, interaction);
            else
                return;

            AbstractAction custom = new AbstractAction(interaction.getName(), ImageUtils.getScaledIcon(
                    interaction.getIconImage(), 16, 16)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (((ToolbarSwitch) e.getSource()).isSelected()) {
                        StateRendererInteraction ri = interactionModes.get(name);

                        if (ri.isExclusive()) {
                            if (renderer.getActiveInteraction() != null) {
                                renderer.getActiveInteraction().setActive(false, renderer);
                                if (renderer.getActiveInteraction() == ri) {
                                    renderer.setViewMode(Renderer.TRANSLATION);
                                    renderer.setActiveInteraction(null);
                                    translateSwitch.setSelected(true);
                                    return;
                                }
                            }
                            renderer.setViewMode(Renderer.NONE);
                        }
                        renderer.setActiveInteraction(ri);
                        ri.setActive(true, renderer);
                    }
                }
            };
            ToolbarSwitch tswitch = new ToolbarSwitch(I18n.text(name), custom);
            if (tswitch.isEnabled())
                bg.add(tswitch);
            bottom.add(tswitch, 4);
            interactionButtons.put(interaction.getName(), tswitch);
            tswitch.setSelected(false);
            interaction.setAssociatedSwitch(tswitch);
            invalidate();
            revalidate();
        }
        catch (AbstractMethodError e) {
            AbstractMethodError e1 = new AbstractMethodError(e.getMessage() + " :: " + interaction.getClass().getName()
                    + " missed the complete implementation of " + StateRendererInteraction.class.getName()
                    + "!!! Recompiling the plugin jar might resolve " + "this issue.");
            e1.setStackTrace(e.getStackTrace());
            e1.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.CustomInteractionSupport#getInteractionModes()
     */
    @Override
    public Collection<StateRendererInteraction> getInteractionModes() {
        return interactionModes.values();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.CustomInteractionSupport#removeInteraction(pt.lsts.neptus.renderer2d.StateRendererInteraction)
     */
    @Override
    public void removeInteraction(StateRendererInteraction interaction) {
        renderer.removeInteraction(interaction);
        ToolbarSwitch sw = interactionButtons.get(interaction.getName());
        if (sw != null)
            bottom.remove(sw);
        doLayout();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.CustomInteractionSupport#setActiveInteraction(pt.lsts.neptus.renderer2d.StateRendererInteraction)
     */
    @Override
    public void setActiveInteraction(StateRendererInteraction interaction) {
        if (interaction == null) {
            renderer.setActiveInteraction(interaction);
            return;
        }
        ToolbarSwitch sw = interactionButtons.get(interaction.getName());
        if (sw != null)
            sw.doClick();
        else
            renderer.setActiveInteraction(interaction);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.CustomInteractionSupport#getActiveInteraction()
     */
    @Override
    public StateRendererInteraction getActiveInteraction() {
        return renderer.getActiveInteraction();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.VehicleStateListener#setVehicleState(pt.lsts.neptus.types.vehicle.VehicleType, pt.lsts.neptus.mp.SystemPositionAndAttitude)
     */
    @Override
    public void setVehicleState(VehicleType vehicle, SystemPositionAndAttitude state) {
        vehicles.put(vehicle, state);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.plugins.ConsoleVehicleChangeListener#consoleVehicleChange(pt.lsts.neptus.types.vehicle.VehicleType, int)
     */
    @Override
    public void consoleVehicleChange(VehicleType v, int status) {
        if (status != ConsoleVehicleChangeListener.VEHICLE_REMOVED) {
            getConsole().addRender(v.getId(), this);
            ImcMsgManager.getManager().addListener(this, v.getId());
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.ILayerPainter#addPreRenderPainter(pt.lsts.neptus.renderer2d.Renderer2DPainter)
     */
    @Override
    public void addPreRenderPainter(Renderer2DPainter painter) {
        renderer.addPreRenderPainter(painter);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.ILayerPainter#removePreRenderPainter(pt.lsts.neptus.renderer2d.Renderer2DPainter)
     */
    @Override
    public void removePreRenderPainter(Renderer2DPainter painter) {
        renderer.removePreRenderPainter(painter);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.ILayerPainter#addPostRenderPainter(pt.lsts.neptus.renderer2d.Renderer2DPainter, java.lang.String)
     */
    @Override
    public boolean addPostRenderPainter(Renderer2DPainter painter, String name) {
        // NeptusLog.pub().info("<###>Adding a post render painter: "+name);
        return renderer.addPostRenderPainter(painter, name);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.ILayerPainter#removePostRenderPainter(pt.lsts.neptus.renderer2d.Renderer2DPainter)
     */
    @Override
    public boolean removePostRenderPainter(Renderer2DPainter painter) {
        return renderer.removePostRenderPainter(painter);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.ILayerPainter#getPostPainters()
     */
    @Override
    public Collection<Renderer2DPainter> getPostPainters() {
        return renderer.getPostPainters();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.planeditor.IMapPopup#getRenderer()
     */
    @Override
    public StateRenderer2D getRenderer() {
        return renderer;
    }
    
    /**
     * @return
     */
    public int getRendererWidth() {
        return renderer.getWidth();
    }

    /**
     * @return
     */
    public int getRendererHeight() {
        return renderer.getHeight();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.planeditor.IMapPopup#addMenuExtension(pt.lsts.neptus.planeditor.IEditorMenuExtension)
     */
    @Override
    public boolean addMenuExtension(IEditorMenuExtension extension) {
        return renderer.addMenuExtension(extension);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.planeditor.IMapPopup#removeMenuExtension(pt.lsts.neptus.planeditor.IEditorMenuExtension)
     */
    @Override
    public boolean removeMenuExtension(IEditorMenuExtension extension) {
        return renderer.removeMenuExtension(extension);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.planeditor.IMapPopup#getMenuExtensions()
     */
    @Override
    public Collection<IEditorMenuExtension> getMenuExtensions() {
        return renderer.getMenuExtensions();
    }

//    public static void main(String[] args) {
//        MapPanel pp = new MapPanel(null);
//        pp.getRenderer().setLegendShown(true);
//        ConsoleLayout cl = ConsoleParse.dummyConsole(pp);
//        cl.setMission(new MissionType("./missions/APDL/missao-apdl.nmisz"));
//
//        Vector<MapPanel> p = cl.getSubPanelsOfClass(MapPanel.class);
//        p.firstElement().addInteraction(new OperationLimitsSubPanel(cl));
//
//        p.firstElement().addPostRenderPainter(new Renderer2DPainter() {
//            /*
//             * (non-Javadoc)
//             * 
//             * @see pt.lsts.neptus.renderer2d.Renderer2DPainter#getLayerPriority()
//             */
//            @Override
//            public void paint(Graphics2D g, StateRenderer2D renderer) {
//                // renderer.setZoom(0.005f);
//                Graphics2D g2d = (Graphics2D) g.create();
//                // g2d.rotate(Math.toRadians(90));
//                // g2d.scale(1, -1);
//                g2d.setColor(Color.GREEN);
//
//                LocationType loc1 = new LocationType();
//                loc1.setLatitude("44N40.7312");
//                loc1.setLongitude("63W32.2072");
//                Point2D pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                g2d.setColor(Color.RED);
//                loc1.setLatitude("44N38.5408");
//                loc1.setLongitude("63W33.2295");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                loc1.setLatitude("44N34.7773");
//                loc1.setLongitude("63W29.6017");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                loc1.setLatitude("44N35.0540");
//                loc1.setLongitude("63W27.2750");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                g2d.setColor(Color.GREEN);
//
//                loc1.setLatitude("44N37'42.520''");
//                loc1.setLongitude("63W31'25.180''");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                loc1.setLatitude("44N37'35.950''");
//                loc1.setLongitude("63W31'11.880''");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                loc1.setLatitude("44N37'30.900''");
//                loc1.setLongitude("63W31'8.320''");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                loc1.setLatitude("44N37'35.170''");
//                loc1.setLongitude("63W32'3.340''");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                loc1.setLatitude("44N38'9.170''");
//                loc1.setLongitude("63W32'5.000''");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                loc1.setLatitude("44N42'45.530''");
//                loc1.setLongitude("63W40'14.550''");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                loc1.setLatitude("44N40'50.850''");
//                loc1.setLongitude("63W36'51.170''");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                loc1.setLatitude("44N42'11.560''");
//                loc1.setLongitude("63W37'23.380''");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                g2d.setColor(Color.CYAN);
//
//                loc1.setLatitude("41N10.3734");
//                loc1.setLongitude("8W42.4817");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                loc1.setLatitude("41N10.6938");
//                loc1.setLongitude("8W42.5051");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                loc1.setLatitude("41N10.3702");
//                loc1.setLongitude("8W42.4821");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                loc1.setLatitude("41N11.0746");
//                loc1.setLongitude("8W42.2519");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                loc1.setLatitude("41N10.6761");
//                loc1.setLongitude("8W42.3400");
//                pt = renderer.getScreenPosition(loc1);
//                if (pt != null)
//                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);
//
//                g2d.dispose();
//            }
//        }, "Test");
//
//        pp.getRenderer().setZoom(10.56f);
//    }
}
