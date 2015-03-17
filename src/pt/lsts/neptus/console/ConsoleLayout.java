/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Rui Gonçalves
 * 2007/09/23
 * 2007/02/15
 */
package pt.lsts.neptus.console;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.CommManagerStatusChangeListener;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.actions.AboutAction;
import pt.lsts.neptus.console.actions.AutoSnapshotConsoleAction;
import pt.lsts.neptus.console.actions.ConsoleAction;
import pt.lsts.neptus.console.actions.CreateMissionConsoleAction;
import pt.lsts.neptus.console.actions.ExitAction;
import pt.lsts.neptus.console.actions.IncomingDataAction;
import pt.lsts.neptus.console.actions.LayoutEditConsoleAction;
import pt.lsts.neptus.console.actions.OpenConsoleAction;
import pt.lsts.neptus.console.actions.OpenImcMonitorAction;
import pt.lsts.neptus.console.actions.OpenMRAAction;
import pt.lsts.neptus.console.actions.OpenMissionConsoleAction;
import pt.lsts.neptus.console.actions.RunChecklistConsoleAction;
import pt.lsts.neptus.console.actions.SaveAsConsoleAction;
import pt.lsts.neptus.console.actions.SaveConsoleAction;
import pt.lsts.neptus.console.actions.SaveMissionAsConsoleAction;
import pt.lsts.neptus.console.actions.SetMainVehicleConsoleAction;
import pt.lsts.neptus.console.actions.TakeSnapshotConsoleAction;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.events.ConsoleEventMissionChanged;
import pt.lsts.neptus.console.events.ConsoleEventNewSystem;
import pt.lsts.neptus.console.events.ConsoleEventPlanChange;
import pt.lsts.neptus.console.notifications.NotificationsCollection;
import pt.lsts.neptus.console.notifications.NotificationsDialog;
import pt.lsts.neptus.console.plugins.ComponentSelector;
import pt.lsts.neptus.console.plugins.ConsoleVehicleChangeListener;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.console.plugins.PluginManager;
import pt.lsts.neptus.console.plugins.SettingsWindow;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent.SubPanelChangeAction;
import pt.lsts.neptus.console.plugins.SubPanelChangeListener;
import pt.lsts.neptus.console.plugins.planning.MapPanel;
import pt.lsts.neptus.controllers.ControllerManager;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.gui.ConsoleFileChooser;
import pt.lsts.neptus.gui.HideMenusListener;
import pt.lsts.neptus.gui.Loader;
import pt.lsts.neptus.gui.MissionFileChooser;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.WaitPanel;
import pt.lsts.neptus.gui.checklist.exec.CheckListExe;
import pt.lsts.neptus.gui.system.selection.MainSystemSelectionCombo;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.loader.NeptusMain;
import pt.lsts.neptus.renderer2d.VehicleStateListener;
import pt.lsts.neptus.types.XmlInOutMethods;
import pt.lsts.neptus.types.XmlOutputMethods;
import pt.lsts.neptus.types.checklist.ChecklistType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.VehicleMission;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * 
 * @author Rui Gonçalves
 * @author Paulo Dias
 * @author José Pinto
 * @author José Correia
 * @author Hugo Dias
 * 
 */
public class ConsoleLayout extends JFrame implements XmlInOutMethods, ComponentListener {
    private static final long serialVersionUID = -7457352031399061316L;

    public static final String DEFAULT_ROOT_ELEMENT = "console";
    private Document xmlDoc = null;
    private boolean changed = false;
    private boolean active = true;
    private boolean isOnModeEdit = false;
    private MissionType mission = null;
    private String mainVehicle = null;
    private PlanType plan = null;
    protected CommManagerStatusChangeListener imcManagerStatus = null;
    private final ImcMsgManager imcMsgManager;
    private final ConcurrentMap<String, ConsoleSystem> consoleSystems = new ConcurrentHashMap<String, ConsoleSystem>();

    // Controller Manager to be used by every plugin that uses an external controller (Gamepad, etc...)
    private final ControllerManager controllerManager;

    // Holders for all console panels, map layers, and map interactions 
    private final List<ConsolePanel> subPanels = new ArrayList<>();
    private final Map<IConsoleLayer, Boolean> layers = new LinkedHashMap<>();
    private final Map<IConsoleInteraction, Boolean> interactions = new LinkedHashMap<>();

    /*
     * UI stuff
     */
    protected Map<Class<? extends ConsoleAction>, ConsoleAction> actions = new HashMap<>();
    protected Map<String, Action> globalKeybindings = new HashMap<>();
    protected KeyEventDispatcher keyDispatcher;
    private final List<Window> onRunningFrames = new ArrayList<Window>(); // frames to close on exit

    // base components
    protected JMenuBar menuBar;
    protected JPanel menus;
    protected MainPanel mainPanel;
    protected StatusBar statusBar;
    protected NotificationsDialog notificationsDialog;
    protected ComponentSelector consolePluginSelector = null;
    protected MainSystemSelectionCombo mainSystemCombo;
    protected int startIndexForDynamicMenus = 0;

    // Max/min helpers
    private Rectangle2D minimizedBounds = null;
    private ConsolePanel maximizedPanel = null;

    protected Vector<SubPanelChangeListener> subPanelListeners = new Vector<SubPanelChangeListener>();
    protected Vector<MissionChangeListener> missionListeners = new Vector<MissionChangeListener>();
    protected Vector<PlanChangeListener> planListeners = new Vector<PlanChangeListener>();

    // Main Vehicle
    protected Vector<MainVehicleChangeListener> mainVehicleListeners = new Vector<MainVehicleChangeListener>();
    protected Vector<ConsoleVehicleChangeListener> consoleVehicleChangeListeners = new Vector<ConsoleVehicleChangeListener>();

    // -------------------------------- XML console
    public File fileName = null;
    public boolean resizableConsole = false;

    /**
     * Static factory method
     * 
     * @param consoleURL console file to load
     * @return {@link ConsoleLayout}
     */
    public static ConsoleLayout forge(String consoleURL, Loader loader) {
        ConsoleLayout instance = new ConsoleLayout();
        instance.imcOn();
        ConsoleParse.parseFile(consoleURL, instance);
        // load core plugins
        PluginManager manager = new PluginManager(instance);
        manager.init();
        SettingsWindow settings = new SettingsWindow(instance);
        settings.init();

        instance.setConsoleChanged(false);

        if (loader != null)
            loader.end();
        instance.setVisible(true);
        return instance;
    }

    /**
     * Static factory method
     * 
     * @param consoleURL
     * @return
     */
    public static ConsoleLayout forge(String consoleURL) {
        return forge(consoleURL, null);
    }

    /**
     * Constructor: begins an empty Console
     */
    public ConsoleLayout() {
        NeptusEvents.create(this);
        this.setupListeners();
        this.setupKeyBindings();
        this.setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        notificationsDialog = new NotificationsDialog(new NotificationsCollection(this), this);
        statusBar = new StatusBar(this, notificationsDialog);

        mainPanel = new MainPanel(this);
        this.add(mainPanel, BorderLayout.CENTER);

        this.createMenuBar();

        menus = new JPanel();
        menus.setLayout(new BorderLayout());
        this.add(menus, BorderLayout.NORTH);

        // Create Status Bar
        this.add(statusBar, BorderLayout.SOUTH);

        this.setIconImages(ConfigFetch.getIconImagesForFrames());
        this.setName(I18n.text("Neptus Console"));

        this.setPreferredSize(new Dimension(1600, 800));
        this.pack();
        this.setLocationRelativeTo(null);

        this.imcMsgManager = ImcMsgManager.getManager();

        controllerManager = new ControllerManager();
    }

    private void setupListeners() {
        this.addComponentListener(this);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JFrame frame = (JFrame) e.getComponent();
                if (isConsoleChanged()) {
                    int answer = JOptionPane.showConfirmDialog(
                            getConsole(),
                            I18n.text("<html>Do you want to save the changes to the current <strong>Console Configuration</strong>?</html>"),
                            I18n.text("Save changes?"), JOptionPane.YES_NO_CANCEL_OPTION);
                    if (answer == JOptionPane.YES_OPTION) {
                        saveFile();
                        frame.setVisible(false);
                        frame.dispose();
                    }
                    else if (answer == JOptionPane.NO_OPTION) {
                        frame.setVisible(false);
                        frame.dispose();
                    }
                    else if (answer == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
                }
                else {
                    frame.setVisible(false);
                    frame.dispose();
                }
                cleanup();
            }

            @Override
            public void windowDeactivated(WindowEvent arg0) {
                active = false;
            }

            @Override
            public void windowActivated(WindowEvent arg0) {
                active = true;
                ConfigFetch.setSuperParentFrameForced(getConsole());
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
                ConfigFetch.setSuperParentFrameForced(getConsole());
            }
        });
    }

    private void setupKeyBindings() {
        KeyEventDispatcher keyDispatcher = new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                int eventType = e.getID(); // KeyEvent.KEY_PRESSED KeyEvent.KEY_RELEASED KEY_TYPED

                Action action = globalKeybindings.get(KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers())
                        .toString());
                if (action != null && eventType == KeyEvent.KEY_PRESSED) {
                    action.actionPerformed(null);
                    return true;
                }

                return false;
            }
        };
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher);
    }

    private void cleanKeyBindings() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyDispatcher);
        globalKeybindings.clear();
    }

    /**
     * Register a global key binding with the console
     * 
     * @param name
     * @param action
     */
    public void registerGlobalKeyBinding(KeyStroke name, Action action) {
        if (this.globalKeybindings.containsKey(name.toString())) {
            NeptusLog.pub().error("Global keybind " + name + " already registered by another component");
        }
        else {
            this.globalKeybindings.put(name.toString(), action);
        }
    }

    /**
     * @return the isOnModeEdit
     */
    public boolean isOnModeEdit() {
        return isOnModeEdit;
    }

    /**
     * Set the operative mode of the console normal/editing
     * 
     * @param mode boolean to set editing off or on
     */
    public void setModeEdit(boolean mode) {
        if (mode) {
            if (consolePluginSelector == null) {
                // Create Component Selector
                consolePluginSelector = new ComponentSelector(mainPanel);
            }
            menus.setVisible(true);
            menus.add(consolePluginSelector, BorderLayout.NORTH);
            setConsoleChanged(true);
            mainPanel.setEditOn();
            isOnModeEdit = true;
        }
        else {
            menus.setVisible(false);
            menus.remove(consolePluginSelector);
            mainPanel.setEditOff();
            revalidate();
            repaint();
            isOnModeEdit = false;
        }
        revalidate();
        repaint();
    }

    /**
     * The Frame or Dialog will be added to the opened. On the {@link #cleanup()} these will get dispose of.
     * 
     * @param win
     */
    public void addWindowToOppenedList(Window win) {
        onRunningFrames.add(win);
    }

    /**
     * The Frame or Dialog will be removed to the opened.
     * 
     * @param win
     */
    public void removeWindowToOppenedList(Window win) {
        onRunningFrames.add(win);
    }

    /**
     * Initialize the main menu (used at constructor) Please call {@link #setStartIndexForDynamicMenus()} if you
     * override this method.
     */
    public void createMenuBar() {
        menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);

        // createMenuActions();

        /*
         * FILE MENU
         */
        JMenu file = new JMenu(I18n.text("File"));
        ConsoleAction open = new OpenConsoleAction(this);
        actions.put(OpenConsoleAction.class, open);
        file.add(open);

        ConsoleAction save = new SaveConsoleAction(this);
        actions.put(SaveConsoleAction.class, save);
        file.add(save);

        ConsoleAction saveAs = new SaveAsConsoleAction(this);
        actions.put(SaveAsConsoleAction.class, saveAs);
        file.add(saveAs);
        file.addSeparator();

        // create mission
        ConsoleAction createMission = new CreateMissionConsoleAction(this);
        actions.put(CreateMissionConsoleAction.class, createMission);
        file.add(createMission);
        // open mission
        ConsoleAction openMission = new OpenMissionConsoleAction(this);
        actions.put(OpenMissionConsoleAction.class, openMission);
        file.add(openMission);
        // save mission as
        ConsoleAction saveMissionAs = new SaveMissionAsConsoleAction(this);
        actions.put(SaveMissionAsConsoleAction.class, saveMissionAs);
        file.add(saveMissionAs);

        file.addSeparator();
        file.add(new ExitAction());
        menuBar.add(file);

        /*
         * VIEW MENU
         */
        JMenu viewMenu = new JMenu(I18n.text("View"));
        menuBar.add(viewMenu);

        /*
         * TOOLS MENU
         */
        JMenu tools = new JMenu(I18n.text("Tools"));
        ConsoleAction openMRA = new OpenMRAAction();
        actions.put(OpenMRAAction.class, openMRA);
        tools.add(openMRA);

        ConsoleAction runChecklist = new RunChecklistConsoleAction(this);
        actions.put(RunChecklistConsoleAction.class, runChecklist);
        tools.add(runChecklist);
        menuBar.add(tools);

        /*
         * Advanced
         */
        JMenu advanced = new JMenu(I18n.text("Advanced"));
        TakeSnapshotConsoleAction takeSnapshot = new TakeSnapshotConsoleAction(this);
        actions.put(TakeSnapshotConsoleAction.class, takeSnapshot);
        advanced.add(takeSnapshot);

        AutoSnapshotConsoleAction autoSnapshot = new AutoSnapshotConsoleAction(this);
        actions.put(AutoSnapshotConsoleAction.class, autoSnapshot);
        advanced.add(autoSnapshot);

        advanced.addSeparator();

        LayoutEditConsoleAction layoutEdit = new LayoutEditConsoleAction(this);
        actions.put(LayoutEditConsoleAction.class, layoutEdit);
        advanced.add(layoutEdit);

        advanced.add(new SetMainVehicleConsoleAction(this));

        IncomingDataAction incomingData = new IncomingDataAction(this);
        actions.put(IncomingDataAction.class, incomingData);
        advanced.add(incomingData);

        OpenImcMonitorAction imcMonitor = new OpenImcMonitorAction(this);
        actions.put(OpenImcMonitorAction.class, imcMonitor);
        advanced.add(imcMonitor);

        menuBar.add(advanced);
        advanced.addMenuListener(HideMenusListener.forge(new Component[0],
                new JMenuItem[] { getJMenuForAction(OpenImcMonitorAction.class) }));

        setStartIndexForDynamicMenus();
        includeHelpMenu();

        includeExtraMainMenus();
    }

    protected boolean removeJMenuAction(Class<? extends ConsoleAction> consoleAction) {
        JMenuItem mn = getJMenuForAction(consoleAction);
        if (mn == null)
            return false;

        mn.getParent().remove(mn);
        return true;
    }

    protected JMenuItem getJMenuForAction(Class<? extends ConsoleAction> consoleAction) {
        ConsoleAction ca = actions.get(consoleAction);
        if (ca == null) {
            NeptusLog.pub()
                    .error("No action to remove from JMenuBar with class " + consoleAction.getSimpleName() + "!");
            return null;
        }

        for (Component comp : menuBar.getComponents()) {
            if (!(comp instanceof JMenu))
                continue;

            JMenu menu = (JMenu) comp;
            for (Component comp1 : menu.getMenuComponents()) {
                if (!(comp1 instanceof JMenuItem))
                    continue;

                JMenuItem menuItem = (JMenuItem) comp1;
                if (menuItem.getAction() == ca) {
                    return menuItem;
                }
            }
        }
        return null;
    }

    // public void addSettingsWindowtiMenuBar() {
    // Vector<SubPanel> pluginSubPanels = getSubPanelsOfClass(SubPanel.class);
    // SettingsWindow settingsWindow = new SettingsWindow(pluginSubPanels, true);
    // if (settingsWindow.existsSettingsToShow()) {
    //
    // }
    //
    // final String settings = "@Settings";
    // MenuElement[] menuElements = menuBar.getSubElements();
    // for (int i = 0; i < menuElements.length; i++) {
    // JMenu menuElement = (JMenu) menuElements[i];
    // String text = menuElement.getText();
    // if (text.equals(settings)) {
    // menuElement.add(new AbstractAction(I18n.text("All Settings"), ICON_SETTINGS) {
    // private static final long serialVersionUID = 9145507092153218703L;
    //
    // @Override
    // public void actionPerformed(ActionEvent e) {
    // Vector<SubPanel> pluginSubPanels = getSubPanelsOfClass(SubPanel.class);
    // SettingsWindow settingsWindow = new SettingsWindow(pluginSubPanels, mainPanel.isEditFlag());
    // settingsWindow.showWindow();
    // }
    // });
    //
    // }
    // }
    // }

    protected JMenu includeHelpMenu() {
        JMenu helpMenu = new JMenu();
        helpMenu.setText(I18n.text("Help"));
        // helpMenu.add(menuActions.get("Manual"));
        // helpMenu.add(menuActions.get("Extended Manual"));
        // helpMenu.add(menuActions.get("About"));

        // ConsoleAction manual = new ManualAction(this);
        // actions.put(ManualAction.class, manual);
        // helpMenu.add(manual);

        // ConsoleAction extManual = new ExtendedManualAction(this);
        // actions.put(ExtendedManualAction.class, extManual);
        // helpMenu.add(extManual);

        ConsoleAction about = new AboutAction(this);
        actions.put(AboutAction.class, about);
        helpMenu.add(about);

        menuBar.add(helpMenu);
        return helpMenu;
    }

    protected void includeExtraMainMenus() {
        menuBar.add(Box.createHorizontalGlue());
        mainSystemCombo = new MainSystemSelectionCombo(this);
        menuBar.add(mainSystemCombo);
    }

    /**
     * The Help will be the last JMenu and before this one the new created menus by calling
     * {@link #getOrCreateJMenu(String[])} will be inserted in alphabetic order.
     */
    protected void setStartIndexForDynamicMenus() {
        startIndexForDynamicMenus = menuBar.getComponentCount() - 1;
    }

    /**
     * Adds a vehicle to a renderer. the vehicle present on the console starts sending data to the added renderer.
     * 
     * @param id vehicle ID present on the console
     * @param externalrender the render to feed the vehiche data
     */
    public void addRender(String id, VehicleStateListener externalrender) {
        ConsoleSystem vehicle = consoleSystems.get(id);
        if (vehicle != null)
            vehicle.addRenderFeed(externalrender);
    }

    /**
     * The vehicle no more supply an MissionRenderer
     * 
     * @param id vehicle ID to remove
     * @param externalrender the vehicle stop in this render
     */
    public void removeRender(String id, VehicleStateListener externalrender) {
        ConsoleSystem vehicle = consoleSystems.get(id);
        if (vehicle != null)
            vehicle.removeRenderFeed(externalrender);
    }

    /**
     * Every vehicle stops sending data to this MissionRender
     * 
     * @param externalrender MissionRenderer to clean vehicles
     */
    public void removeRenderAll(VehicleStateListener externalrender) {
        for (Entry<String, ConsoleSystem> entry : consoleSystems.entrySet()) {
            entry.getValue().removeRenderFeed(externalrender);
        }
    }

    /**
     * 
     * @return the mission associated to this console
     */
    public MissionType getMission() {
        return mission;
    }

    /**
     * Set new mission or if mission param = null remove current mission
     * 
     * @param mission
     */
    public void setMission(MissionType mission) {
        MissionType old = this.mission;
        this.mission = mission;
        this.setPlan(null);

        if (mission != null) {
            NeptusLog.pub().debug("Mission changed to " + mission.getId());
            // initOtherMissionVehicles();
        }
        else {
            NeptusLog.pub().info("Mission set to null");
        }

        for (MissionChangeListener mlistener : missionListeners) {
            try {
                mlistener.missionReplaced(mission);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
        this.updateTitle();
        this.post(new ConsoleEventMissionChanged(old, this.mission));
    }

    public void setPlan(PlanType plan) {
        PlanType old = this.plan;
        if (plan != null)
            if (plan.getMissionType() != getMission())
                setMission(plan.getMissionType());

        this.plan = plan;

        for (PlanChangeListener pobj : planListeners) {
            try {
                pobj.planChange(this.plan);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
                e.printStackTrace();
            }
        }
        this.post(new ConsoleEventPlanChange(old, this.plan));
    }

    public PlanType getPlan() {
        return this.plan;
    }

    /**
     * The main panel of this console editing
     * 
     * @return The main panel of this console editing
     */
    public MainPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Get main vehicle
     * 
     * @return
     */
    public String getMainSystem() {
        return mainVehicle;
    }

    public void setMainSystem(String mainVehicle) {
        if (this.getSystem(mainVehicle) == null) {
            NeptusLog.pub().error("trying to add main system without addin it");
            return;
        }
        String old = this.mainVehicle;
        this.mainVehicle = mainVehicle;

        if (!(mainVehicle.equals(old))) {
            for (MainVehicleChangeListener mlistener : mainVehicleListeners) {
                try {
                    mlistener.mainVehicleChange(mainVehicle);
                }
                catch (Exception ex) {
                    NeptusLog.pub().error(ex, ex);
                }
            }
            this.post(new ConsoleEventMainSystemChange(old, this.mainVehicle));
        }
    }

    /**
     * Add a new vehicle to the vehicle list of console
     * 
     * @param systemName Vehicle ID
     */
    public void addSystem(String systemName) {
        VehicleType vehicleType = VehiclesHolder.getVehicleById(systemName);
        ImcSystem imcSystem = ImcSystemsHolder.lookupSystemByName(systemName);
        if (vehicleType != null && imcSystem == null) {
            imcMsgManager.initVehicleCommInfo(vehicleType.getId(), "");
            return;
        }

        ConsoleSystem system;
        if (imcSystem == null) {
            NeptusLog.pub().warn("tried to add a vehicle from imc with comms disabled: " + systemName);
            return;
        }
        if (imcSystem.getType() != SystemTypeEnum.VEHICLE) {
            return;
        }

        if (consoleSystems.get(systemName) != null) {
            NeptusLog.pub().warn(
                    ReflectionUtil.getCallerStamp() + " tried to add a vehicle that already exist in the console: "
                            + systemName);
            return;
        }
        else {
            system = new ConsoleSystem(systemName, this, imcSystem, imcMsgManager);
            consoleSystems.put(systemName, system);
            if (this.mainVehicle == null) {
                this.setMainSystem(systemName);
            }
        }

        for (ConsoleVehicleChangeListener cvl : consoleVehicleChangeListeners) {
            try {
                cvl.consoleVehicleChange(vehicleType, ConsoleVehicleChangeListener.VEHICLE_ADDED);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
        this.post(new ConsoleEventNewSystem(system));
        system.enableIMC();
    }

    /**
     * Remove an vehicle from vehicle list of this console Every subpanel that refers to this vehicle stops changing
     * 
     * @param id Vehicle ID to be removed
     */
    public void removeSystem(String id) {
        ConsoleSystem vehicle = consoleSystems.remove(id);

        if (vehicle != null) {
            vehicle.clean();
            for (ConsoleVehicleChangeListener cvl : consoleVehicleChangeListeners)
                try {
                    cvl.consoleVehicleChange(VehiclesHolder.getVehicleById(id),
                            ConsoleVehicleChangeListener.VEHICLE_REMOVED);
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
        }
    }

    /**
     * initialize all vehicles of mission
     * 
     */
    public void initOtherMissionVehicles() {
        Collection<VehicleMission> aMap = mission.getVehiclesList().values();

        for (VehicleMission j : aMap) {
            if (getSystem(j.getId()) == null) {
                addSystem(j.getId());
            }
        }
    }

    public void initSubPanels() {
        for (ConsolePanel sp : subPanels) {
            sp.init();
        }
        
        for (IConsoleLayer layer : layers.keySet()) {
            layer.init(this);
        }
        
        for (IConsoleInteraction inter : interactions.keySet()) {
            inter.init(this);
        }
    }

    public boolean saveFile() {
        if (fileName == null)
            return saveasFile();

        if (new File(fileName.getAbsolutePath()).exists())
            FileUtil.backupFile(fileName.getAbsolutePath());

        FileUtil.saveToFile(fileName.getAbsolutePath(), FileUtil.getAsPrettyPrintFormatedXMLString(asDocument()));
        changed = false;
        return true;
    }

    public boolean saveasFile() {

        File file = ConsoleFileChooser.showSaveConsoleDialog(this);
        if (file == null) {
            return false;

        }
        String ext = FileUtil.getFileExtension(file);
        if (FileUtil.FILE_TYPE_CONSOLE.equalsIgnoreCase(ext) || FileUtil.FILE_TYPE_XML.equalsIgnoreCase(ext)) {
            ext = "";
        }
        else
            ext = "." + FileUtil.FILE_TYPE_CONSOLE;
        file = new File(file.getAbsolutePath() + ext);
        fileName = file;

        FileUtil.saveToFile(fileName.getAbsolutePath(), FileUtil.getAsPrettyPrintFormatedXMLString(asDocument()));
        changed = false;
        return true;
    }

    public void setMissionFile(String[] extensions) {
        final File file = MissionFileChooser.showOpenMissionDialog(extensions);
        if (file != null) {
            final WaitPanel a = new WaitPanel();
            Thread t = new Thread("loader set mission file thread") {
                @Override
                public void run() {
                    try {
                        a.start(ConsoleLayout.this, ModalityType.DOCUMENT_MODAL);
                        setMission(new MissionType(file.getAbsolutePath()));
                        a.stop();
                    }
                    catch (Exception e) {
                        a.stop();
                        GuiUtils.errorMessage(getConsole(), I18n.text("Error"),
                                I18n.text("Error Loading Mission File.\n"));
                        NeptusLog.pub().error("Console Base open file error [" + file.getAbsolutePath() + "]");
                        NeptusLog.pub().error(e, e);
                    }
                };
            };
            t.start();
        }
    }

    // XmlInOut Implementation
    @Override
    public String asXML() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asXML(rootElementName);
    }

    @Override
    public String asXML(String rootElementName) {
        String result = "";
        Document document = asDocument(rootElementName);
        result = document.asXML();
        return result;
    }

    @Override
    public Element asElement() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asElement(rootElementName);
    }

    @Override
    public Element asElement(String rootElementName) {
        return (Element) asDocument(rootElementName).getRootElement().detach();
    }

    @Override
    public Document asDocument() {
        String rootElementName = DEFAULT_ROOT_ELEMENT;
        return asDocument(rootElementName);
    }

    @Override
    public Document asDocument(String rootElementName) {

        Document doc = null;

        doc = DocumentHelper.createDocument();

        Element root = doc.addElement(rootElementName);
        root.addComment(ConfigFetch.getSaveAsCommentForXML());

        root.addAttribute("name", this.getName());
        root.addAttribute("width", "" + this.getWidth());
        root.addAttribute("height", "" + this.getHeight());
        root.addAttribute("resizable", "" + this.isResizableConsole());

        if (getMission() != null)
            if (!mission.getOriginalFilePath().equals(""))
                root.addAttribute("mission-file", FileUtil.relativizeFilePathAsURI(fileName.getAbsolutePath(), mission
                        .getMissionFile().getAbsolutePath()));

        if (getMainSystem() != null)
            root.addAttribute("main-vehicle", getMainSystem());

        Element mainpanel = root.addElement("mainpanel");
        mainpanel.addAttribute("name", "console main panel");
        for (Component c : getMainPanel().getComponents()) {
            if (c instanceof XmlOutputMethods) {
                try {
                    mainpanel.add(((XmlOutputMethods) c).asElement());
                }
                catch (Exception e) {
                    GuiUtils.errorMessage(ConsoleLayout.this, "Error saving XML of " + c.getName(), "[" + c.getName()
                            + "]: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        Element alarmpanel = root.addElement("mainpanel");
        alarmpanel.addAttribute("name", "console alarm panel");
        
        if (!layers.isEmpty()) {
            Element layersElem = root.addElement("layers");
            for (IConsoleLayer l : layers.keySet()) {
                if (layers.get(l))
                    layersElem.add(l.asElement("layer"));
            }
        }
        
        if (!interactions.isEmpty()) {
            Element interactionsElem = root.addElement("interactions");
            for (IConsoleInteraction i : interactions.keySet()) {
                    if (interactions.get(i))
                interactionsElem.add(i.asElement("interaction"));
            }
        }

        return doc;
    }

    @Override
    public void inElement(Element elem) {
        ConsoleParse.parseElement(elem, this, this.getFileName().toString());
    }

    @Override
    public void inDocument(Document d) {
        ConsoleParse.parseDocument(d, this, this.getFileName().toString());
    }

    @Override
    public void parseXML(String d) {
        ConsoleParse.parseString(d, this, this.getFileName().toString());
    }

    /**
     * This will not go to the children of {@link ContainerSubPanel}
     * 
     * @return
     */
    public List<ConsolePanel> getSubPanels() {
        return subPanels;
    }

    public List<PropertiesProvider> getAllPropertiesProviders() {
        List<PropertiesProvider> ret = new ArrayList<>();
        for (ConsolePanel sp : subPanels) {
            if (sp instanceof PropertiesProvider)
                ret.add((PropertiesProvider) sp);
        }
        for (IConsoleLayer ly : layers.keySet()) {
            if (layers.get(ly)) {
                if (ly instanceof PropertiesProvider)
                    ret.add((PropertiesProvider) ly);
            }
        }
        for (IConsoleInteraction in : interactions.keySet()) {
            if (interactions.get(in)) {
                if (in instanceof PropertiesProvider)
                    ret.add((PropertiesProvider) in);
            }
        }
        return ret;
    }
    
    /**
     * 
     * @param subPanelType
     * @return
     */
    public <T extends ConsolePanel> Vector<T> getSubPanelsOfClass(Class<T> subPanelType) {
        try {
            Vector<T> ret = new Vector<T>();

            if (subPanels.isEmpty())
                return ret;

            for (ConsolePanel sp : subPanels) {
                Vector<T> rSp = getSubPanelType(sp, subPanelType);
                if (rSp.size() > 0)
                    ret.addAll(rSp);
            }
            return ret;
        }
        catch (Exception e) {
            e.printStackTrace();
            return new Vector<T>();
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends ConsolePanel> Vector<T> getSubPanelType(ConsolePanel sp, Class<T> superClass) {
        Vector<T> ret = new Vector<T>();
        if (sp == null)
            return ret;

        if (sp.getClass().equals(superClass))
            ret.add((T) sp);
        // else if (ReflectionUtil.isSubclass(sp.getClass(), superClass))
        else if (superClass.isAssignableFrom(sp.getClass()))
            ret.add((T) sp);

        if (sp instanceof ContainerSubPanel) {
            for (ConsolePanel s : ((ContainerSubPanel) sp).getSubPanels()) {
                Vector<T> rSp = getSubPanelType(s, superClass);
                if (rSp.size() > 0)
                    ret.addAll(rSp);
            }
        }
        return ret;
    }

    /**
     * This method returns all Subpanels that implement the given Interface class that are present in the console
     * 
     * @param <T> The interface type you're looking for
     * @param interfaceType Just pass an empty array of the desired type as in:
     * 
     *            <pre>
     * Vector&lt;MissionChangeListener&gt; mcls = getSubPanelImplementations(new MissionChangeListener[0]);
     * </pre>
     * @return A vector of subpanels that implement the given interface
     */
    public <T> Vector<T> getSubPanelsOfInterface(Class<T> interfaceType) {
        Vector<T> ret = new Vector<T>();
        HashSet<T> col = new HashSet<T>();

        if (subPanels.isEmpty())
            return ret;

        for (ConsolePanel sp : subPanels) {
            col.addAll(getSubPanelImplementations(sp, interfaceType));
        }
        ret.addAll(col);
        return ret;
    }

    @SuppressWarnings("unchecked")
    private <T> Vector<T> getSubPanelImplementations(ConsolePanel sp, Class<T> interfaceType) {
        Vector<T> ret = new Vector<T>();

        if (ReflectionUtil.hasInterface(sp.getClass(), interfaceType))
            ret.add((T) sp);

        if (ReflectionUtil.isSubclass(sp.getClass(), ContainerSubPanel.class)) {
            for (ConsolePanel s : ((ContainerSubPanel) sp).getSubPanels()) {
                Vector<T> rSp = getSubPanelImplementations(s, interfaceType);
                if (rSp.size() > 0)
                    ret.addAll(rSp);
            }
        }
        return ret;
    }

    public void addConsoleVehicleListener(ConsoleVehicleChangeListener cvl) {
        if (!consoleVehicleChangeListeners.contains(cvl))
            consoleVehicleChangeListeners.add(cvl);
    }

    public void removeConsoleVehicleListener(ConsoleVehicleChangeListener cvl) {
        consoleVehicleChangeListeners.remove(cvl);
    }

    public void addMainVehicleListener(MainVehicleChangeListener vcl) {
        if (!mainVehicleListeners.contains(vcl))
            mainVehicleListeners.add(vcl);
    }

    public void removeMainVehicleListener(MainVehicleChangeListener vcl) {
        mainVehicleListeners.remove(vcl);
    }

    public void addMissionListener(MissionChangeListener mcl) {
        if (!missionListeners.contains(mcl))
            missionListeners.add(mcl);
    }

    public void removeMissionListener(MissionChangeListener mcl) {
        missionListeners.remove(mcl);
    }

    public void warnMissionListeners() {
        for (MissionChangeListener mcl : missionListeners)
            mcl.missionReplaced(getMission());
    }

    public void updateMissionListeners() {
        for (MissionChangeListener mcl : missionListeners)
            mcl.missionUpdated(getMission());
    }

    public void addPlanListener(PlanChangeListener pcl) {
        if (!planListeners.contains(pcl))
            planListeners.add(pcl);
    }

    public void removePlanListener(PlanChangeListener pcl) {
        planListeners.remove(pcl);
    }

    public void addSubPanelListener(SubPanelChangeListener spl) {
        if (!subPanelListeners.contains(spl))
            subPanelListeners.add(spl);
    }

    public void removeSubPanelListener(SubPanelChangeListener spl) {
        subPanelListeners.remove(spl);
    }

    public void informSubPanelListener(ConsolePanel sub, SubPanelChangeAction action) {
        for (SubPanelChangeListener spcl : subPanelListeners)
            spcl.subPanelChanged(new SubPanelChangeEvent(sub, action));
        
        // If there is a new map, add all existing layers to it        
        if (action == SubPanelChangeAction.ADDED && sub instanceof MapPanel) {
            for (IConsoleLayer layer : layers.keySet()) {
                ((MapPanel)sub).addPostRenderPainter(layer, layer.getName());
            }
            for (IConsoleInteraction i : interactions.keySet()) {
                ((MapPanel)sub).addInteraction(i);
            }
        }
    }
    
    /**
     * Add a layer that (optionally) is not preserved in the console's layout file)
     * Use this for interactions added by ConsolePanels so they don't get added a second time.
     * @param interaction
     * @param storeInConsoleXml Whether the layer should be added to the console's configuration. 
     * Use <code>false</code> if it's an internal layer.
     * @return Whether the interaction was correctly added. 
     */
    public boolean addInteraction(IConsoleInteraction interaction, boolean storeInConsoleXml) {
        Vector<MapPanel> maps = getSubPanelsOfClass(MapPanel.class);
        
        if (interactions.containsKey(interaction)) {
            NeptusLog.pub().error("Interation was already present in this console.");
            return false;
        }
        
        if (maps.isEmpty()) {
            NeptusLog.pub().error("Cannot add interaction because there is no MapPanel in the console.");
            return false;
        }
        
        interaction.init(this);
        interactions.put(interaction, storeInConsoleXml);
        
        for (MapPanel map : maps) {
            map.addInteraction(interaction);            
        }
        
        return true;
    }

    public boolean addInteraction(IConsoleInteraction interaction) {
        return addInteraction(interaction, true);
    }

    public boolean removeInteraction(IConsoleInteraction interaction) {
        if (!interactions.containsKey(interaction)) {
            NeptusLog.pub().error("Interaction not found in this console.");
            return false;
        }
        
        for (MapPanel map : getSubPanelsOfClass(MapPanel.class)) {
            map.removeInteraction(interaction);
        }
        
        interaction.clean();
        
        return interactions.remove(interaction) == null ? false : true;
    }
    
    /**
     * Add a layer that (optionally) is not preserved in the console's layout file)
     * Use this for layers added by ConsolePanels so they don't get added a second time.
     * @param layer The layer to be added
     * @param storeInConsoleXml Whether the layer should be added to the console's configuration. 
     * Use <code>false</code> if it's an internal layer.
     * @return Whether the layer was correctly added. 
     */
    public boolean addMapLayer(IConsoleLayer layer, boolean storeInConsoleXml) {
        Vector<MapPanel> maps = getSubPanelsOfClass(MapPanel.class);
        
        if (layers.containsKey(layer)) {
            NeptusLog.pub().error("Layer was already present in this console.");
            return false;
        }
        
        if (maps.isEmpty()) {
            NeptusLog.pub().error("Cannot add layer beacause there is no MapPanel in the console.");
            return false;
        }
        
        layer.init(this);
        
        layers.put(layer, storeInConsoleXml);
        
        for (MapPanel map : maps) {
            map.addLayer(layer);            
        }
        
        return true;
    }
    
    public boolean addMapLayer(IConsoleLayer layer) {
       return addMapLayer(layer, true);
    }
    
    public boolean removeMapLayer(IConsoleLayer layer) {
        for (MapPanel map : getSubPanelsOfClass(MapPanel.class)) {
            try {
                map.removeLayer(layer);
            }
            catch (Exception e) {
                NeptusLog.pub()
                        .error("Error removing layer '" + layer.getName() + "' from '" + MapPanel.class.getSimpleName()
                                + "'!", e);
            }
        }
        
        try {
            layer.clean();
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error calling clean from layer '" + layer.getName() + "'!", e);
        }
        
        try {
            return layers.remove(layer) == null ? false : true;
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error removing layer '" + layer.getName() + "' from console layers list!", e);
            return false;
        }
    }

    /**
     * 
     * reset the console for a new one useb when a new console is open
     * 
     */
    public void reset() {
        // getContentPane().remove(statusBar);
        this.remove(mainPanel);
        mainPanel.clean();
        mainPanel = new MainPanel(this);
        this.add(mainPanel, BorderLayout.CENTER);

        missionListeners.clear(); // retirar todos os listeners (limpeza forçada)
        planListeners.clear(); // retirar todos os listeners
        consoleVehicleChangeListeners.clear();

        for (ConsoleSystem vehicle : consoleSystems.values()) {
            vehicle.clean();
        }
        consoleSystems.clear();

        for (Window j : onRunningFrames) {
            j.dispose();
        }

        setMission(null);
        mainVehicleListeners.clear();

        changed = false;

        this.revalidate();
        this.repaint();
    }

    /**
     * Free all memory used It must be called in the program (it's not automatic)
     */
    public void cleanup() {
        long start = System.currentTimeMillis();
        NeptusLog.pub().info(ConsoleLayout.this.getClass().getSimpleName() + " cleanup start");
        try {
            removeComponentListener(this);

            missionListeners.clear();
            planListeners.clear();
            consoleVehicleChangeListeners.clear();

            for (Window j : onRunningFrames) {
                j.setVisible(false);
                j.dispose();
            }

            mainVehicleListeners.clear();

            AutoSnapshotConsoleAction autosnapshot = (AutoSnapshotConsoleAction) actions
                    .get(AutoSnapshotConsoleAction.class);
            autosnapshot.cleanClose();

            for (IConsoleLayer layer : layers.keySet().toArray(new IConsoleLayer[layers.size()])) {
                try {
                    NeptusLog.pub().info("Cleaning " + layer.getName());
                    layer.clean();
                    NeptusLog.pub().info("Cleaned " + layer.getName());
                }
                catch (Exception e) {
                    NeptusLog.pub().error("Error cleaning " + layer.getName() + " :: " + e.getMessage(), e);
                }
            }
            layers.clear();

            for (IConsoleInteraction interaction : interactions.keySet().toArray(new IConsoleInteraction[interactions.size()])) {
                try {
                    interaction.clean();
                    NeptusLog.pub().info("Cleaned " + interaction.getName());
                }
                catch (Exception e) {
                    NeptusLog.pub().error("Error cleaning " + interaction.getName() + " :: " + e.getMessage(), e);
                }
            }
            interactions.clear();

            for (ConsoleSystem system : consoleSystems.values()) {
                system.clean();
            }
            consoleSystems.clear();
            mainPanel.clean();
            statusBar.clean();
            
            this.cleanKeyBindings();
            this.imcOff();

            NeptusEvents.delete(this);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Error cleaning " + ConsoleLayout.this.getClass().getSimpleName() + " :: " + e.getMessage(), e);
        }

        NeptusLog.pub().info(ConsoleLayout.this.getClass().getSimpleName() + " cleanup end in " + ((System.currentTimeMillis() - start) / 1E3) + "s");
    }

    public void minimizePanel(ConsolePanel p) {

        p.setVisible(false);

        if (maximizedPanel != null)
            p = maximizedPanel;
        if (minimizedBounds == null)
            return;

        if (subPanels.indexOf(p) == -1) {
            for (ConsolePanel tmp : subPanels) {
                for (ConsolePanel c : tmp.getChildren())
                    if (c.equals(p))
                        p = tmp;
            }
        }
        else {
            p.setBounds(minimizedBounds.getBounds());
            for (ConsolePanel tmp : subPanels) {
                if (!(tmp instanceof ConsolePanel) || ((ConsolePanel) tmp).getVisibility())
                    tmp.setVisible(true);
            }

        }

        minimizedBounds = null;
        maximizedPanel = null;

        mainPanel.revalidate();
    }

    public void maximizePanel(ConsolePanel p) {

        if (maximizedPanel != null)
            minimizePanel(maximizedPanel);

        if (subPanels.indexOf(p) == -1) {
            for (ConsolePanel tmp : subPanels) {
                for (ConsolePanel c : tmp.getChildren())
                    if (c.equals(p))
                        p = tmp;
            }
        }
        else {

            for (ConsolePanel tmp : subPanels) {
                if (tmp.getRootPane() != null && tmp.getRootPane().equals(p.getRootPane()))
                    tmp.setVisible(false);
            }

            Component tmp = p;
            while (tmp.getParent() != mainPanel && tmp.getParent() != null) {
                tmp = tmp.getParent();
            }

            if (tmp instanceof ConsolePanel)
                p = (ConsolePanel) tmp;
            minimizedBounds = p.getBounds();
            maximizedPanel = p;

            p.setBounds(new Rectangle(0, 0, p.getMainpanel().getWidth(), p.getMainpanel().getHeight()));
            p.setSize(getMainPanel().getSize());
            p.setVisible(true);

            p.getMainpanel().revalidate();
            p.getMainpanel().repaint();
        }
    }

    public ConsolePanel getMaximizedPanel() {
        return maximizedPanel;
    }

    public void setMaximizedPanel(ConsolePanel maximizedPanel) {
        if (maximizedPanel == this.maximizedPanel)
            return;

        if (this.maximizedPanel != null) {
            minimizePanel(maximizedPanel);
        }
        maximizePanel(maximizedPanel);
    }

    public void setFileName(File fileName) {
        this.fileName = fileName;
    }

    public File getFileName() {
        return fileName;
    }

    public Document getXmlDoc() {
        return xmlDoc;
    }

    public void setXmlDoc(Document xmlDoc) {
        this.xmlDoc = xmlDoc;
    }

    private Scriptable scope = null;

    @SuppressWarnings("deprecation")
    public Scriptable getScope() {
        if (scope == null) {
            scope = new Context().initStandardObjects();

            // Object wrappedTree = Context.javaToJS(getMainVehicleTree(), scope);
            Object wrappedOut = Context.javaToJS(System.out, scope);
            Object wrappedErr = Context.javaToJS(System.err, scope);
            // Object wrappedEnv = Context.javaToJS(env, scope);
            Object wrappedMsg = Context.javaToJS(null, scope);
            Object wrappedConsole = Context.javaToJS(getConsole(), scope);
            Object wrappedManager = Context.javaToJS(ImcMsgManager.getManager(), scope);

            // ScriptableObject.putProperty(scope, "tree", wrappedTree);
            // ScriptableObject.putProperty(scope, "env", wrappedEnv);
            ScriptableObject.putProperty(scope, "out", wrappedOut);
            ScriptableObject.putProperty(scope, "err", wrappedErr);
            ScriptableObject.putProperty(scope, "msg", wrappedMsg);
            ScriptableObject.putProperty(scope, "console", wrappedConsole);
            ScriptableObject.putProperty(scope, "manager", wrappedManager);
        }

        return scope;
    }

    public Object evaluateScript(String js) {
        Context cx = Context.enter();

        try {
            Object result = cx.evaluateString(getScope(), js, "<script>", 1, null);
            return result;
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    public void executeCheckList(ChecklistType ct) {
        // NeptusLog.pub().info("<###>Ok:"+ct);
        if (getMission() != null) {
            if (getMission().getCompressedFilePath() != null) {
                File file = new File(getMission().getCompressedFilePath());
                CheckListExe.showCheckListExeDialog(getMainSystem(), ct, this, file.getParent());
            }
        }
    }
    
    /**
     * Creates and retrieves a console menu item
     * 
     * @param itemPath The path to the menu item separated by ">". Examples: <li>
     *            <b>"Tools > Local Network > Test Network"</b> <li><b>"Tools>Test Network"</b>
     * @param icon The icon to be used in the menu item. <br>
     *            Size is automatically adjusted to 16x16 pixels.
     * @param actionListener The {@link ActionListener} that will be warned on menu activation
     * @return The created {@link JMenuItem} or <b>null</b> if an error as occurred.
     */
    public JMenuItem addMenuItem(String itemPath, ImageIcon icon, ActionListener actionListener) {
        String[] ptmp = itemPath.split(">");
        if (ptmp.length < 2) {
            NeptusLog.pub().error("Menu path has to have at least two components");
            return null;
        }

        String[] path = new String[ptmp.length - 1];
        System.arraycopy(ptmp, 0, path, 0, path.length);

        String menuName = ptmp[ptmp.length - 1];

        JMenu menu = getConsole().getOrCreateJMenu(path);

        final ActionListener l = actionListener;
        AbstractAction action = new AbstractAction(menuName) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                l.actionPerformed(e);
            }
        };
        if (icon != null)
            action.putValue(AbstractAction.SMALL_ICON, ImageUtils.getScaledIcon(icon.getImage(), 16, 16));

        JMenuItem item = menu.add(action);
        return item;
    }

    public JMenu removeMenuItem(String[] menuPath) {
        JMenu parent = null;
        for (int i = 0; i < this.menuBar.getMenuCount(); i++) {
            JMenu menu = getConsole().getJMenuBar().getMenu(i);
            if (menu == null)
                continue;
            if (menu.getText().equalsIgnoreCase(menuPath[0].trim())) {
                parent = menu;
                break;
            }
        }

        if (parent == null)
            return null;

        int i = 1;

        while (parent != null && i < menuPath.length) {
            boolean found = false;
            for (int j = 0; j < parent.getItemCount(); j++) {
                JMenuItem child = parent.getItem(j);
                if (child != null && child.getText() != null && child.getText().equalsIgnoreCase(menuPath[i].trim())) {
                    i++;
                    found = true;
                    if (i == menuPath.length) {
                        parent.remove(child);
                        return parent;
                    }
                    parent = (JMenu) child;
                    break;
                }
            }
            if (!found)
                break;

        }
        return null;
    }

    public JMenu getOrCreateJMenu(String[] menuPath) {
        int count = this.menuBar.getComponentCount();
        JMenu parent = null;
        for (int i = 0; i < count; i++) {
            Component c = menuBar.getComponent(i);
            if (c instanceof JMenu) {
                JMenu menu = (JMenu) c;
                if (menu.getText().equalsIgnoreCase(menuPath[0].trim())) {
                    parent = menu;
                    break;
                }
            }
        }

        if (parent == null) {
            parent = new JMenu(menuPath[0].trim());
            insertJMenuIntoTheMenuBarOrdered(parent);
        }

        int i = 1;

        while (i < menuPath.length) {
            for (int j = 0; j < parent.getItemCount(); j++) {
                JMenuItem child = parent.getItem(j);
                if (child == null || !(child instanceof JMenu))
                    continue;
                if (child.getText() != null && child.getText().equalsIgnoreCase(menuPath[i].trim())) {
                    parent = (JMenu) child;
                    i++;
                    break;
                }
            }

            if (i >= menuPath.length)
                continue;

            JMenu tmp = new JMenu(menuPath[i]);
            parent.add(tmp);
            i++;
            parent = tmp;
            break;
        }
        return parent;
    }

    /**
     * @param parent
     */
    private synchronized void insertJMenuIntoTheMenuBarOrdered(JMenu topMenu) {
        int indexEnd = 0;
        int size = menuBar.getComponentCount() - 1;
        for (int i = size; i > startIndexForDynamicMenus; i--) {
            Component component = menuBar.getComponent(i);
            if (component instanceof JMenu && ((JMenu) component).getText().equals(I18n.text("Help"))) {
                indexEnd = i;
            }
        }

        int insertM = startIndexForDynamicMenus + 1;
        for (int i = insertM; i < indexEnd - 1; i++) {
            JMenu menu = getConsole().getJMenuBar().getMenu(i);
            if (menu == null)
                continue;
            String nameM = menu.getText();
            int compVal = topMenu.getText().compareTo(nameM);
            if (compVal <= 0) {
                break;
            }
            insertM = i + 1;
        }
        getConsole().getJMenuBar().add(topMenu, insertM);

    }

    @Override
    public void componentResized(ComponentEvent e) {
        notificationsDialog.setVisible(false);
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        notificationsDialog.setVisible(false);
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
    }

    public boolean isResizableConsole() {
        return resizableConsole;
    }

    public void setResizableConsole(boolean resizebleConsole) {
        this.resizableConsole = resizebleConsole;
    }

    public ImcSystemState getImcState() {
        return getImcState(getMainSystem());
    }

    public ImcSystemState getImcState(String system) {
        return ImcMsgManager.getManager().getState(ImcSystemsHolder.getSystemWithName(system).getId());
    }

    /*
     * HELPERS
     */

    public void post(Object event) {
        NeptusEvents.post(event, this);
    }

    /**
     * Refress neptus window title
     */
    protected void updateTitle() {
        StringBuilder title = new StringBuilder();

        title.append(this.getName());

        MissionType mission = this.getMission();
        if (mission != null) {
            title.append(" | " + I18n.text("Mission") + ": ");
            title.append(mission.getName());
            title.append(" [" + mission.getCompressedFilePath() + "]");
        }
        this.setTitle(title.toString());
    }

    public void imcOn() {
        if (this.imcMsgManager.start()) {
            ImcSystem[] systems = ImcSystemsHolder.lookupActiveSystemVehicles();
            for (ImcSystem imcSystem : systems) {
                this.addSystem(imcSystem.getName());
            }

            imcMsgManager.addStatusListener(imcManagerStatus == null ? this.setupImcListener() : imcManagerStatus);
            for (Entry<String, ConsoleSystem> vehicle : consoleSystems.entrySet()) {
                vehicle.getValue().enableIMC();
            }
        }
        else {
            NeptusLog.pub().warn("Error starting IMC");
        }
    }

    public void imcOff() {
        imcMsgManager.removeStatusListener(imcManagerStatus == null ? this.setupImcListener() : imcManagerStatus);
    }

    public CommManagerStatusChangeListener setupImcListener() {
        imcManagerStatus = new CommManagerStatusChangeListener() {
            @Override
            public void managerStatusChanged(int status, String msg) {
                if (status == ImcMsgManager.MANAGER_START) {
                }
                else if (status == ImcMsgManager.MANAGER_STOP) {
                }
                else if (status == ImcMsgManager.MANAGER_ERROR) {
                }
            }

            @Override
            public void managerVehicleAdded(VehicleType vehicle) {
                addSystem(vehicle.getId());
            }

            @Override
            public void managerVehicleRemoved(VehicleType vehicle) {
            }

            @Override
            public void managerVehicleStatusChanged(VehicleType vehicle, int status) {
            }

            @Override
            public void managerSystemAdded(String systemId) {
                addSystem(systemId);
            }

            @Override
            public void managerSystemRemoved(String systemId) {
            }

            @Override
            public void managerSystemStatusChanged(String systemId, int status) {
            }
        };
        return imcManagerStatus;
    }

    /*
     * ACCESSORS
     */

    public ConsoleLayout getConsole() {
        return this;
    }

    public StatusBar getStatusBar() {
        return statusBar;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Get Console Systems
     * 
     * @return {@link Map}
     */
    public Map<String, ConsoleSystem> getSystems() {
        return consoleSystems;
    }

    /**
     * Get ConsoleSystem by name
     * 
     * @param name
     * @return {@link ConsoleSystem}
     */
    public ConsoleSystem getSystem(String name) {
        return this.consoleSystems.get(name);
    }

    /**
     * @return the imcMsgManager
     */
    public ImcMsgManager getImcMsgManager() {
        return imcMsgManager;
    }

    /**
     * @param edited the save to set
     */
    public void setConsoleChanged(boolean needsToSave) {
        this.changed = needsToSave;

        // Check first if the console is editable
        ConsoleAction saveConsoleAction = actions.get(SaveConsoleAction.class);
        if (saveConsoleAction != null)
            saveConsoleAction.setEnabled(needsToSave);
    }

    public boolean isConsoleChanged() {
        return changed;
    }

    /**
     * @return the controllerManager
     */
    public ControllerManager getControllerManager() {
        return controllerManager;
    }

    /**
     * @return a copy of the layers
     */
    public List<IConsoleLayer> getLayers() {
        return Arrays.asList(layers.keySet().toArray(new IConsoleLayer[0]));
    }

    /**
     * @return a copy of the interactions
     */
    public List<IConsoleInteraction> getInteractions() {
        return Arrays.asList(interactions.keySet().toArray(new IConsoleInteraction[0]));
    }

    // MAIN FOR TESTING ONLY
    public static void main(String[] args) {
        GuiUtils.setLookAndFeel();
        File[] consoles = new File("conf/consoles").listFiles();
        Vector<String> options = new Vector<>();
        for (File f : consoles) {
            if (FileUtil.getFileExtension(f).equalsIgnoreCase("ncon")) {
                options.add(FileUtil.getFileNameWithoutExtension(f));
            }
        }
        final Collator collator = Collator.getInstance(Locale.US);
        Collections.sort(options, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return collator.compare(o1, o2);
            };
        });

        String op = ""
                + JOptionPane.showInputDialog(null, I18n.text("Select console to open"), I18n.text("Neptus Console"),
                        JOptionPane.QUESTION_MESSAGE, ImageUtils.getIcon("images/neptus-icon1.png"),
                        options.toArray(new String[0]), "lauv");

        if (op.equals("null")) {
            return;
        }
        NeptusMain.main(new String[] { "-f", new File(new File("conf/consoles"), op + ".ncon").getAbsolutePath() });
    }
}
