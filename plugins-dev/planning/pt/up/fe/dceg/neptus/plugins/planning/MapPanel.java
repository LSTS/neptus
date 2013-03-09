/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * 2009/09/21
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.ConsoleSystem;
import pt.up.fe.dceg.neptus.console.MainPanel;
import pt.up.fe.dceg.neptus.console.plugins.ConsoleVehicleChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.MissionChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.PlanChangeListener;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.ToolbarButton;
import pt.up.fe.dceg.neptus.gui.ToolbarSwitch;
import pt.up.fe.dceg.neptus.gui.VehicleChooser;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.CcuEvent;
import pt.up.fe.dceg.neptus.imc.CcuEvent.TYPE;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.PlanControlState;
import pt.up.fe.dceg.neptus.messages.listener.MessageInfo;
import pt.up.fe.dceg.neptus.messages.listener.MessageListener;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.mp.templates.AbstractPlanTemplate;
import pt.up.fe.dceg.neptus.mp.templates.ScriptedPlanTemplate;
import pt.up.fe.dceg.neptus.planeditor.IEditorMenuExtension;
import pt.up.fe.dceg.neptus.planeditor.IMapPopup;
import pt.up.fe.dceg.neptus.planeditor.MapPlanEditor;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginDescription.CATEGORY;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.containers.LayoutProfileProvider;
import pt.up.fe.dceg.neptus.plugins.oplimits.OperationLimitsSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.renderer2d.CustomInteractionSupport;
import pt.up.fe.dceg.neptus.renderer2d.EstimatedStateGenerator;
import pt.up.fe.dceg.neptus.renderer2d.FeatureFocuser;
import pt.up.fe.dceg.neptus.renderer2d.ILayerPainter;
import pt.up.fe.dceg.neptus.renderer2d.Renderer;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer2d.StateRendererInteraction;
import pt.up.fe.dceg.neptus.renderer2d.VehicleStateListener;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.map.MapType;
import pt.up.fe.dceg.neptus.types.map.PlanElement;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.ConsoleParse;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 * @author Paulo Dias
 */
@PluginDescription(name = "Map Panel", icon = "pt/up/fe/dceg/neptus/plugins/planning/planning.png", author = "ZP, Paulo Dias", documentation = "planning/planning_panel.html", category = CATEGORY.INTERFACE)
public class MapPanel extends SimpleSubPanel implements MainVehicleChangeListener, MissionChangeListener,
PlanChangeListener, IPeriodicUpdates, ILayerPainter, ConfigurationListener, IMapPopup,
CustomInteractionSupport, VehicleStateListener, ConsoleVehicleChangeListener {

    private static final long serialVersionUID = 1L;

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

    private static final String scriptsDir = "conf/planscripts/";

    protected MapPlanEditor editor = new MapPlanEditor(null);
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
    protected MessageListener<MessageInfo, IMCMessage> imcMessageListener;

    int msgid = 0;

    public MapPanel(ConsoleLayout console) {
        super(console);
        removeAll();
        setLayout(new BorderLayout());
        editor.setEditable(false);
        editor.getRenderer().setMinDelay(0);
        editor.getRenderer().setShowWorldMapOnScreenControls(true);
        add(editor, BorderLayout.CENTER);
        bottom.setFloatable(false);
        bottom.setAlignmentX(JToolBar.CENTER_ALIGNMENT);
        addMenuExtension(new FeatureFocuser());
        translateMode = new AbstractAction(I18n.text("Translate"), ImageUtils.getScaledIcon(
                "pt/up/fe/dceg/neptus/plugins/planning/translate_btn.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    if (getActiveInteraction() != null && getActiveInteraction().isExclusive())
                        getActiveInteraction().setActive(false, editor.getRenderer());
                    setActiveInteraction(null);
                    editor.followVehicle(null);
                    editor.setViewMode(Renderer.TRANSLATION);
                }
            }

        };
        translateSwitch = new ToolbarSwitch(translateMode);
        bg.add(translateSwitch);
        bottom.add(translateSwitch);
        translateSwitch.setSelected(true);

        zoomMode = new AbstractAction(I18n.text("Zoom"), ImageUtils.getScaledIcon(
                "pt/up/fe/dceg/neptus/plugins/planning/zoom_btn.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    editor.followVehicle(null);
                    editor.setViewMode(Renderer.ZOOM);
                    if (getActiveInteraction() != null && getActiveInteraction().isExclusive())
                        getActiveInteraction().setActive(false, editor.getRenderer());
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

        rotateMode = new AbstractAction(I18n.text("Rotate"), ImageUtils.getScaledIcon(
                "pt/up/fe/dceg/neptus/plugins/planning/rotate_btn.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    editor.followVehicle(null);
                    editor.setViewMode(Renderer.ROTATION);
                    if (getActiveInteraction() != null && getActiveInteraction().isExclusive())
                        getActiveInteraction().setActive(false, editor.getRenderer());
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

        rulerMode = new AbstractAction(I18n.text("Ruler"), ImageUtils.getScaledIcon(
                "pt/up/fe/dceg/neptus/plugins/planning/ruler_btn.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    editor.followVehicle(null);
                    editor.setViewMode(Renderer.RULER);
                    if (getActiveInteraction() != null && getActiveInteraction().isExclusive())
                        getActiveInteraction().setActive(false, editor.getRenderer());
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

        followMode = new AbstractAction(I18n.text("Lock Vehicle"), ImageUtils.getScaledIcon(
                "pt/up/fe/dceg/neptus/plugins/planning/lock_vehicle.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    if (getActiveInteraction() != null && getActiveInteraction().isExclusive())
                        getActiveInteraction().setActive(false, editor.getRenderer());
                    setActiveInteraction(null);
                    String mainVehicle = getConsole().getMainSystem();
                    if (mainVehicle != null)
                        editor.followVehicle(VehiclesHolder.getVehicleById(mainVehicle).getId());
                }
                else {
                    editor.followVehicle(null);
                }
            }
        };
        followSwitch = new ToolbarSwitch(followMode);
        bg.add(followSwitch);
        bottom.add(followSwitch);

        tailMode = new AbstractAction(I18n.text("Show tail"), ImageUtils.getScaledIcon(
                "pt/up/fe/dceg/neptus/plugins/planning/tailOnOff.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    String mainVehicle = getConsole().getMainSystem();
                    if (mainVehicle != null)
                        editor.setVehicleTailOn(null);
                }
                else {
                    editor.setVehicleTailOff(null);
                }
            }
        };
        tailSwitch = new ToolbarSwitch(tailMode);
        tailSwitch.setSelected(false);
        bottom.add(tailSwitch);

        bottom.addSeparator(new Dimension(0, 20));

        editMode = new AbstractAction(I18n.text("Edit plan"), ImageUtils.getScaledIcon(
                "pt/up/fe/dceg/neptus/plugins/planning/edit.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {

                if (((ToolbarSwitch) e.getSource()).isSelected()) {
                    editor.setPlan(getConsole().getPlan().clonePlan());
                    if (editor.getViewMode() == MapPlanEditor.RULER) {
                        rulerSwitch.setSelected(false);
                        translateSwitch.setSelected(true);
                        editor.setViewMode(MapPlanEditor.TRANSLATION);
                        if (getActiveInteraction() != null && getActiveInteraction().isExclusive())
                            getActiveInteraction().setActive(false, editor.getRenderer());
                    }
                    editor.setEditable(true);
                    status.setText(I18n.textf("Editing %planid",getConsole().getPlan().getId()));
                }
                else {
                    if (!askToSave()) {
                        editSwitch.setSelected(true);
                        setActiveInteraction(null);
                        return;
                    }
                    else {
                        editSwitch.setEnabled(getConsole().getPlan() != null);
                        setActiveInteraction(null);
                        status.setText("");
                    }
                }
            }
        };

        editSwitch = new ToolbarSwitch(editMode);
        editSwitch.setSelected(false);
        // bottom.add(editSwitch);

        addPlan = new AbstractAction(I18n.text("Add plan"), ImageUtils.getScaledIcon(
                "pt/up/fe/dceg/neptus/plugins/planning/add.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (editor.isEditable()) {
                    if (!askToSave()) {

                        return;
                    }
                }
                VehicleType choice = null;
                if (getConsole().getMainSystem() != null)
                    choice = VehicleChooser.showVehicleDialog(
                            VehiclesHolder.getVehicleById(getConsole().getMainSystem()), MapPanel.this);
                else
                    choice = VehicleChooser.showVehicleDialog(MapPanel.this);

                if (choice == null)
                    return;

                PlanType plan = new PlanType(getConsole().getMission());
                plan.setVehicle(choice);
                // plan.setId("unsaved");
                editor.setPlan(plan);
                editor.setEditable(true);
                editSwitch.setSelected(true);
                editSwitch.setEnabled(true);
                if (getActiveInteraction() != null && getActiveInteraction().isExclusive())
                    getActiveInteraction().setActive(false, editor.getRenderer());
                setActiveInteraction(null);
            }
        };
        addPlan.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Create a new plan"));
        // bottom.add(new ToolbarButton(addPlan));

        addTemplate = new AbstractAction(I18n.text("Add plan from template"), ImageUtils.getScaledIcon(
                "pt/up/fe/dceg/neptus/plugins/planning/template.png", 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (editor.isEditable()) {
                    if (!askToSave()) {
                        return;
                    }
                }
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
                        JOptionPane.DEFAULT_OPTION,
                        ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/planning/template.png"));

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
                    // System.out.println(scriptsDir+option+":\n"+source);
                    planTemplate.setSource(source);

                    planTemplate.setMission(getConsole().getMission());
                    PropertiesEditor.editProperties(planTemplate, true);
                    try {
                        plan = planTemplate.generatePlan();
                        // System.out.println(plan.asXML());
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
                            System.out.println(option + " != " + PluginUtils.getPluginName(classes[i]));
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
                        GuiUtils.infoMessage(getConsole(), I18n.text("Plan template"), I18n.textf("The plan %planid was added to this mission",  p.getId()));
                    };
                }.run();

            }
        };
        addTemplate.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Add plan from template"));
        bottom.add(new ToolbarButton(addTemplate));

        bottom.addSeparator(new Dimension(0, 20));

        maximizeMode = new AbstractAction(I18n.text("Maximize"), ImageUtils.getScaledIcon(
                "pt/up/fe/dceg/neptus/plugins/planning/maximize.png", 16, 16)) {
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
                    maximizeMode.putValue(AbstractAction.SMALL_ICON,
                            ImageUtils.getScaledIcon("pt/up/fe/dceg/neptus/plugins/planning/minimize.png", 16, 16));
                    maximizeMode.putValue(AbstractAction.SHORT_DESCRIPTION, I18n.text("Minimize"));
                }
                else {
                    if (MapPanel.this.getParent() instanceof LayoutProfileProvider) {
                        LayoutProfileProvider glc = (LayoutProfileProvider) MapPanel.this.getParent();
                        glc.maximizePanelOnContainer(null);
                    }
                    else if (getConsole() != null)
                        getConsole().minimizePanel(MapPanel.this);
                    maximizeMode.putValue(AbstractAction.SMALL_ICON,
                            ImageUtils.getScaledIcon("pt/up/fe/dceg/neptus/plugins/planning/maximize.png", 16, 16));
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

        addMenuExtension(new IEditorMenuExtension() {
            @Override
            public Collection<JMenuItem> getApplicableItems(LocationType loc, IMapPopup source) {
                JMenuItem item = new JMenuItem(I18n.text("Choose visible layers"));
                item.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        editor.getRenderer().painterSelection();
                    }
                });
                return Arrays.asList(item);
            }
        });
    }

    /**
     * @return the showOnScreenControls
     */
    public boolean isShowWorldMapOnScreenControls() {
        return editor.getRenderer().isShowWorldMapOnScreenControls();
    }

    /**
     * @param showOnScreenControls the showOnScreenControls to set
     */
    public void setShowWorldMapOnScreenControls(boolean showOnScreenControls) {
        editor.getRenderer().setShowWorldMapOnScreenControls(showOnScreenControls);
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

        if (!message.getSourceName().equals(getConsole().getMainSystem()))
            return;
        
        String planId = message.getPlanId();
        String manId = message.getManId();

        if (getConsole().getPlan() != null && getConsole().getPlan().getId().equals(planId)) {
            if (mainPlanPainter != null)
                mainPlanPainter.setActiveManeuver(manId);
        }
    }


    @Override
    public void missionReplaced(MissionType mission) {
        editor.setMission(mission);
        addPlan.setEnabled(mission != null);
    }

    @Override
    public void mainVehicleChangeNotification(String id) {
        followMode.setEnabled(id != null);
        if (mainPlanPainter != null)
            mainPlanPainter.setActiveManeuver(null);
    }

    @Override
    public void PlanChange(PlanType plan) {
        StateRenderer2D r2d = editor.getRenderer();
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
            editMode.setEnabled(true);
        }
        else {
            editMode.setEnabled(false);
        }
    }

    @Override
    public long millisBetweenUpdates() {
        return updateMillis;
    }

    protected long lastStamp = 0;
    protected SystemPositionAndAttitude lastState = null;
    protected EstimatedStateGenerator stateGenerator = new EstimatedStateGenerator();

    @Override
    public boolean update() {

        try {
            for (VehicleType vt : vehicles.keySet()) {
                editor.vehicleStateChanged(vt.getId(), vehicles.get(vt), false);
            }
            editor.repaint();
        }
        catch (Exception e) {
            NeptusLog.pub().debug(e);
        }
        return true;
    }

    @Override
    public void initSubPanel() {
        setSize(500, 500);

        followMode.setEnabled(getConsole().getMainSystem() != null);
        editMode.setEnabled(getConsole().getPlan() != null);
        addPlan.setEnabled(getConsole().getMission() != null);
        editor.setMission(getConsole().getMission());
        editor.setPlan(getConsole().getPlan());

        getConsole().addConsoleVehicleListener(this);
        
        for (ConsoleSystem v : getConsole().getConsoleSystems().values()) {   
            v.addRenderFeed(this);
        }
        if (getConsole() != null) {
            // System.out.println("--------------------" +this.getParent());
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
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#clean()
     */
    @Override
    public void cleanSubPanel() {

        ImcMsgManager.getManager().removeListenerFromAllSystems(this);
        getConsole().removeRenderAll(this);
    }

    public void editPlan(PlanType plan) {
        editor.setPlan(plan);
        editor.setEditable(true);
        editSwitch.setSelected(true);
        editSwitch.setEnabled(true);
    }

    /**
     * @return the editor
     */
    // public MapPlanEditor getEditor() {
    // return editor;
    // }

    protected boolean askToSave() {

        int answer = JOptionPane.showConfirmDialog(getConsole(), I18n.text("Save plan changes to disk?"), I18n.text("Plan editor"),
                JOptionPane.YES_NO_CANCEL_OPTION);

        if (answer == JOptionPane.CANCEL_OPTION) {
            editSwitch.setSelected(true);
            return false;
        }
        else if (answer == JOptionPane.NO_OPTION) {
            editor.setPlan(null);
            editor.setEditable(false);
            return true;
        }
        else {
            String id = null;
            while (true) {
                id = JOptionPane.showInputDialog(getConsole(), I18n.text("Choose a plan identifier"), editor.getPlanElem()
                        .getPlan().getId());
                if (id == null)
                    return false;
                if (getConsole().getMission().getIndividualPlansList().containsKey(id)) {
                    int res = JOptionPane.showConfirmDialog(getConsole(), I18n.text("Overwrite existing plan?"), I18n.text("Plan editor"),
                            JOptionPane.YES_NO_CANCEL_OPTION);
                    if (res == JOptionPane.CANCEL_OPTION) {
                        editSwitch.setSelected(true);
                        return false;
                    }
                    else if (res == JOptionPane.YES_OPTION) {
                        editor.getPlanElem().getPlan().setId(id);
                        break;
                    }
                    continue;
                }
                editor.getPlanElem().getPlan().setId(id);
                break;
            }

            // String curPlan = (getConsole().getPlan()) != null ? getConsole().getPlan().getId() : null;
            boolean planExistedBefore = getConsole().getMission().getIndividualPlansList().get(id) != null;
            PlanType plan = editor.getPlanElem().getPlan();
            getConsole().getMission().getIndividualPlansList().put(id, plan);

            new Thread() {
                @Override
                public void run() {
                    getConsole().getMission().save(saveMissionStates);
                }
            }.start();

            getConsole().updateMissionListeners();

            editor.setPlan(null);
            editor.setEditable(false);

            getConsole().setPlan(plan);
            CcuEvent event = new CcuEvent();
            event.setId(editor.getPlanElem().getPlan().getId());
            event.setArg(plan.asIMCPlan());

            if (planExistedBefore)
                event.setType(TYPE.PLAN_CHANGED);
            else
                event.setType(TYPE.PLAN_ADDED);

            ImcMsgManager.getManager().broadcastToCCUs(event);

            return true;
        }
    }

    @Override
    public void missionUpdated(MissionType mission) {
    }

    @Override
    public boolean addPostRenderPainter(Renderer2DPainter painter, String name) {
        // System.out.println("Adding a post render painter: "+name);
        return editor.getRenderer().addPostRenderPainter(painter, name);
    }

    @Override
    public Collection<Renderer2DPainter> getPostPainters() {
        return editor.getRenderer().getPostPainters();
    }

    @Override
    public boolean removePostRenderPainter(Renderer2DPainter painter) {
        return editor.getRenderer().removePostRenderPainter(painter);
    }

    @Override
    public void propertiesChanged() {
        editor.getRenderer().setSmoothResizing(smoothResize);
        editor.getRenderer().setAntialiasing(antialias);
        editor.getRenderer().setFixedVehicleWidth(fixedSize);
        editor.getRenderer().setWorldMapShown(worldMapShown);
        setToolbarPlacement(); // Refresh toolbar position
    }

    @Override
    public boolean addMenuExtension(IEditorMenuExtension extension) {
        return editor.addMenuExtension(extension);
    }

    @Override
    public Collection<IEditorMenuExtension> getMenuExtensions() {
        return editor.getMenuExtensions();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.planeditor.IMapPopup#getRenderer()
     */
    @Override
    public StateRenderer2D getRenderer() {
        return editor.getRenderer();
    }

    @Override
    public boolean removeMenuExtension(IEditorMenuExtension extension) {
        return editor.removeMenuExtension(extension);
    }

    protected LinkedHashMap<String, StateRendererInteraction> interactionModes = new LinkedHashMap<String, StateRendererInteraction>();
    protected LinkedHashMap<String, ToolbarSwitch> interactionButtons = new LinkedHashMap<String, ToolbarSwitch>();

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
                            if (editor.getActiveInteraction() != null) {
                                editor.getActiveInteraction().setActive(false, editor.getRenderer());
                                if (editor.getActiveInteraction() == ri) {
                                    editor.setViewMode(Renderer.TRANSLATION);
                                    editor.setActiveInteraction(null);
                                    translateSwitch.setSelected(true);
                                    return;
                                }
                            }
                            editor.setViewMode(Renderer.NONE);
                        }
                        editor.setActiveInteraction(ri);
                        ri.setActive(true, editor.getRenderer());
                    }
                }
            };
            ToolbarSwitch tswitch = new ToolbarSwitch(custom);
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
            e = new AbstractMethodError(e.getMessage() + " :: " + interaction.getClass().getName()
                    + " missed the complete implementation of " + StateRendererInteraction.class.getName()
                    + "!!! Recompiling the plugin jar might resolve " + "this issue.");
            e.setStackTrace(e.getStackTrace());
            e.printStackTrace();
        }
    }

    @Override
    public Collection<StateRendererInteraction> getInteractionModes() {
        return interactionModes.values();
    }

    @Override
    public void removeInteraction(StateRendererInteraction interaction) {
        editor.removeInteraction(interaction);
        ToolbarSwitch sw = interactionButtons.get(interaction.getName());
        if (sw != null)
            bottom.remove(sw);
        doLayout();
    }

    @Override
    public void setActiveInteraction(StateRendererInteraction interaction) {
        if (interaction == null) {
            editor.setActiveInteraction(interaction);
            return;
        }
        ToolbarSwitch sw = interactionButtons.get(interaction.getName());
        if (sw != null)
            sw.doClick();
        else
            editor.setActiveInteraction(interaction);
    }

    @Override
    public StateRendererInteraction getActiveInteraction() {
        return editor.getActiveInteraction();
    }

    @Override
    public void setVehicleState(VehicleType vehicle, SystemPositionAndAttitude state) {
        vehicles.put(vehicle, state);
    }

    @Override
    public void consoleVehicleChange(VehicleType v, int status) {
        if (status != ConsoleVehicleChangeListener.VEHICLE_REMOVED) {
            getConsole().addRender(v.getId(), this);
            ImcMsgManager.getManager().addListener(this, v.getId());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.up.fe.dceg.neptus.renderer2d.ILayerPainter#addPreRenderPainter(pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter
     * )
     */
    @Override
    public void addPreRenderPainter(Renderer2DPainter painter) {
        editor.getRenderer().addPreRenderPainter(painter);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.renderer2d.ILayerPainter#removePreRenderPainter(pt.up.fe.dceg.neptus.renderer2d.
     * Renderer2DPainter)
     */
    @Override
    public void removePreRenderPainter(Renderer2DPainter painter) {
        editor.getRenderer().removePreRenderPainter(painter);
    }

    /**
     * @return
     */
    public int getRendererWidth() {
        return editor.getRenderer().getWidth();
    }

    /**
     * @return
     */
    public int getRendererHeight() {
        return editor.getRenderer().getHeight();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        MapPanel pp = new MapPanel(null);
        pp.getRenderer().setLegendShown(true);
        ConsoleLayout cl = ConsoleParse.dummyConsole(pp);
        cl.setMission(new MissionType("./missions/APDL/missao-apdl.nmisz"));

        Vector<MapPanel> p = cl.getSubPanelsOfClass(MapPanel.class);
        p.firstElement().addInteraction(new OperationLimitsSubPanel(cl));

        p.firstElement().addPostRenderPainter(new Renderer2DPainter() {
            /*
             * (non-Javadoc)
             * 
             * @see pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter#getLayerPriority()
             */
            @Override
            public void paint(Graphics2D g, StateRenderer2D renderer) {
                // renderer.setZoom(0.005f);
                Graphics2D g2d = (Graphics2D) g.create();
                // g2d.rotate(Math.toRadians(90));
                // g2d.scale(1, -1);
                g2d.setColor(Color.GREEN);

                LocationType loc1 = new LocationType();
                loc1.setLatitude("44N40.7312");
                loc1.setLongitude("63W32.2072");
                Point2D pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                g2d.setColor(Color.RED);
                loc1.setLatitude("44N38.5408");
                loc1.setLongitude("63W33.2295");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                loc1.setLatitude("44N34.7773");
                loc1.setLongitude("63W29.6017");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                loc1.setLatitude("44N35.0540");
                loc1.setLongitude("63W27.2750");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                g2d.setColor(Color.GREEN);

                loc1.setLatitude("44N37'42.520''");
                loc1.setLongitude("63W31'25.180''");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                loc1.setLatitude("44N37'35.950''");
                loc1.setLongitude("63W31'11.880''");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                loc1.setLatitude("44N37'30.900''");
                loc1.setLongitude("63W31'8.320''");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                loc1.setLatitude("44N37'35.170''");
                loc1.setLongitude("63W32'3.340''");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                loc1.setLatitude("44N38'9.170''");
                loc1.setLongitude("63W32'5.000''");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                loc1.setLatitude("44N42'45.530''");
                loc1.setLongitude("63W40'14.550''");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                loc1.setLatitude("44N40'50.850''");
                loc1.setLongitude("63W36'51.170''");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                loc1.setLatitude("44N42'11.560''");
                loc1.setLongitude("63W37'23.380''");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                g2d.setColor(Color.CYAN);

                loc1.setLatitude("41N10.3734");
                loc1.setLongitude("8W42.4817");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                loc1.setLatitude("41N10.6938");
                loc1.setLongitude("8W42.5051");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                loc1.setLatitude("41N10.3702");
                loc1.setLongitude("8W42.4821");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                loc1.setLatitude("41N11.0746");
                loc1.setLongitude("8W42.2519");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                loc1.setLatitude("41N10.6761");
                loc1.setLongitude("8W42.3400");
                pt = renderer.getScreenPosition(loc1);
                if (pt != null)
                    g2d.fillOval((int) pt.getX() - 5, (int) pt.getY() - 5, 10, 10);

                g2d.dispose();
            }
        }, "Test");

        pp.getRenderer().setZoom(10.56f);
    }
}
