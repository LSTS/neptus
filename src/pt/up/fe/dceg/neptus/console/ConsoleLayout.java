/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Rui Gonçalves
 * 2007/09/23
 * 2007/02/15
 */
package pt.up.fe.dceg.neptus.console;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.actions.AboutAction;
import pt.up.fe.dceg.neptus.console.actions.AutoSnapshotConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.ConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.CreateMissionConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.ExitAction;
import pt.up.fe.dceg.neptus.console.actions.ExtendedManualAction;
import pt.up.fe.dceg.neptus.console.actions.IncomingDataAction;
import pt.up.fe.dceg.neptus.console.actions.LayoutEditConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.ManualAction;
import pt.up.fe.dceg.neptus.console.actions.OpenConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.OpenImcMonitorAction;
import pt.up.fe.dceg.neptus.console.actions.OpenMRAAction;
import pt.up.fe.dceg.neptus.console.actions.OpenMissionConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.RunChecklistConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.SaveAsConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.SaveConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.SaveMissionAsConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.SetMainVehicleConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.TakeSnapshotConsoleAction;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventMainSystemChange;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventMissionChanged;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventNewSystem;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventPlanChange;
import pt.up.fe.dceg.neptus.console.notifications.NotificationsCollection;
import pt.up.fe.dceg.neptus.console.notifications.NotificationsDialog;
import pt.up.fe.dceg.neptus.console.plugins.ComponentSelector;
import pt.up.fe.dceg.neptus.console.plugins.ConsoleVehicleChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.MissionChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.PlanChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.SubPanelChangeEvent;
import pt.up.fe.dceg.neptus.console.plugins.SubPanelChangeEvent.SubPanelChangeAction;
import pt.up.fe.dceg.neptus.console.plugins.SubPanelChangeListener;
import pt.up.fe.dceg.neptus.controllers.ControllerManager;
import pt.up.fe.dceg.neptus.events.NeptusEventHiddenMenus;
import pt.up.fe.dceg.neptus.events.NeptusEvents;
import pt.up.fe.dceg.neptus.gui.ConsoleFileChooser;
import pt.up.fe.dceg.neptus.gui.HideMenusListener;
import pt.up.fe.dceg.neptus.gui.Loader;
import pt.up.fe.dceg.neptus.gui.MissionFileChooser;
import pt.up.fe.dceg.neptus.gui.WaitPanel;
import pt.up.fe.dceg.neptus.gui.checklist.exec.CheckListExe;
import pt.up.fe.dceg.neptus.gui.system.selection.MainSystemSelectionCombo;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.state.ImcSysState;
import pt.up.fe.dceg.neptus.plugins.PluginClassLoader;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.teleoperation.ControllerPanel;
import pt.up.fe.dceg.neptus.renderer2d.VehicleStateListener;
import pt.up.fe.dceg.neptus.types.XmlInOutMethods;
import pt.up.fe.dceg.neptus.types.XmlOutputMethods;
import pt.up.fe.dceg.neptus.types.checklist.ChecklistType;
import pt.up.fe.dceg.neptus.types.miscsystems.MiscSystemsHolder;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.VehicleMission;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.ConsoleParse;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;
import pt.up.fe.dceg.neptus.util.comm.manager.CommManagerStatusChangeListener;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

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

    // ICONS
   // private static ImageIcon ICON_SETTINGS = ImageUtils.createImageIcon("images/menus/settings.png");
    
    public static final String DEFAULT_ROOT_ELEMENT = "console";
    public static final int CLOSE_ACTION = JFrame.DISPOSE_ON_CLOSE;
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
    // Controller logic and panel
    private final ControllerManager controllerManager;
    private ControllerPanel controllerPanel;

    private final List<SubPanel> subPanels = new ArrayList<>();

    /*
     * UI stuff
     */
    protected Map<Class<? extends ConsoleAction>, ConsoleAction> actions = new HashMap<>();
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

    protected Vector<SubPanelChangeListener> subPanelListeners = new Vector<SubPanelChangeListener>();
    protected Vector<MissionChangeListener> missionListeners = new Vector<MissionChangeListener>();
    protected Vector<PlanChangeListener> planListeners = new Vector<PlanChangeListener>();

    // Main Vehicle
    protected Vector<MainVehicleChangeListener> mainVehicleListeners = new Vector<MainVehicleChangeListener>();
    protected Vector<ConsoleVehicleChangeListener> consoleVehicleChangeListeners = new Vector<ConsoleVehicleChangeListener>();

    // -------------------------------- XML console
    public File fileName = null;
    public boolean resizableConsole = false;

//    protected LinkedHashMap<String, AbstractAction> menuActions = new LinkedHashMap<String, AbstractAction>();

    /**
     * Static factory method
     * 
     * @param consoleURL console file to load
     * @return {@link ConsoleLayout}
     */
    public static ConsoleLayout forge(String consoleURL, Loader loader) {
        ConsoleLayout instance = new ConsoleLayout();
        try {
            instance.imcOn();
            ConsoleParse.parseFile(consoleURL, instance);
            instance.setConsoleChanged(false);
            // Rectangle screen = MouseInfo.getPointerInfo().getDevice().getDefaultConfiguration().getBounds();
            // instance.setLocation(screen.x, screen.y);
            if (loader != null)
                loader.end();
            instance.setVisible(true);
            return instance;
        }
        catch (DocumentException e) {
            e.printStackTrace();
        }

        return null;
    }
    
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
                frame.setDefaultCloseOperation(CLOSE_ACTION);
                if (isConsoleChanged()) {
                    int answer = JOptionPane.showConfirmDialog(
                            getConsole(),
                            I18n.text("<html>Do you want to save the changes to the current <strong>Console Configuration</strong>?</html>"),
                            I18n.text("Save changes?"), JOptionPane.YES_NO_CANCEL_OPTION);
                    if (answer == JOptionPane.YES_OPTION) {
                        saveFile();
                    }
                    else if (answer == JOptionPane.CANCEL_OPTION) {
                        return;
                    }
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
        JRootPane rootPane = this.getRootPane();
        InputMap globalInputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        // Hidden menus shift + S key binding
        globalInputMap.put(KeyStroke.getKeyStroke("shift S"), "pressed");
        rootPane.getActionMap().put("pressed", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                post(new NeptusEventHiddenMenus());
            }
        });

        // KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
        // @Override
        // public boolean dispatchKeyEvent(KeyEvent e) {
        // int eventType = e.getID(); //KeyEvent.KEY_PRESSED KeyEvent.KEY_RELEASED KEY_TYPED
        //
        // if(e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_S){
        // NeptusEvents.manager().post(new NeptusEventHiddenMenus());
        // if(eventType == KeyEvent.KEY_PRESSED)
        // System.out.println("pressed");
        // else if(eventType == KeyEvent.KEY_RELEASED)
        // System.out.println("released");
        // }
        // return false;
        // }
        // });
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

//    @SuppressWarnings("serial")
//    public void createMenuActions() {
//
//        menuActions.put("About", new AbstractAction(I18n.text("About"), new ImageIcon(this.getClass().getClassLoader()
//                .getResource("images/menus/info.png"))) {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                final AboutPanel ap = new AboutPanel();
//                ap.setVisible(true);
//                addWindowToOppenedList(ap);
//                ap.addWindowListener(new WindowAdapter() {
//                    @Override
//                    public void windowClosed(WindowEvent e) {
//                        removeWindowToOppenedList(ap);
//                    }
//                });
//            }
//        });
//        menuActions.get("About").putValue(
//                AbstractAction.ACCELERATOR_KEY,
//                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H, java.awt.Event.CTRL_MASK
//                        + java.awt.Event.ALT_MASK, true));
//
//        menuActions.put("Manual", new AbstractAction(I18n.text("Manual"), new ImageIcon(this.getClass()
//                .getClassLoader().getResource("images/menus/info.png"))) {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                DocumentationPanel.showDocumentation("start.html");
//            }
//        });
//
//        menuActions.put("Extended Manual", new AbstractAction(I18n.text("Extended Manual"), new ImageIcon(this
//                .getClass().getClassLoader().getResource("images/menus/info.png"))) {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                try {
//                    Desktop.getDesktop().browse(new File("doc/seacon/manual-seacon.html").toURI());
//                }
//                catch (IOException e1) {
//                    e1.printStackTrace();
//                    GuiUtils.errorMessage(I18n.text("Error opening Extended Manual"), e1.getMessage());
//                }
//            }
//        });
//    }

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

//        createMenuActions();

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
        
        IncomingDataAction incomingData =  new IncomingDataAction(this);
        actions.put(IncomingDataAction.class, incomingData);
        advanced.add(incomingData);

        OpenImcMonitorAction imcMonitor =  new OpenImcMonitorAction(this);
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
            NeptusLog.pub().error("No action to remove from JMenuBar with class " + consoleAction.getSimpleName() + "!");
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

        ConsoleAction manual = new ManualAction(this);
        actions.put(ManualAction.class, manual);
        helpMenu.add(manual);

        ConsoleAction extManual = new ExtendedManualAction(this);
        actions.put(ExtendedManualAction.class, extManual);
        helpMenu.add(extManual);

        ConsoleAction about = new AboutAction(this);
        actions.put(AboutAction.class, about);
        helpMenu.add(about);

        menuBar.add(helpMenu);
        return helpMenu;
    }

    protected void includeExtraMainMenus() {
        menuBar.add(Box.createHorizontalGlue());
//        JButton teleoperationButton = new JButton(new AbstractAction("", ICON_TELEOP) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if (controllerPanel == null) {
//                    controllerPanel = new ControllerPanel(getConsole());
//                }
//                controllerPanel.setVisible(true);
//            }
//        });
//        teleoperationButton.setSize(25, 25);
//        menuBar.add(teleoperationButton);
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
            NeptusLog.pub().info("Mission changed to " + mission.getId());
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
                pobj.PlanChange(this.plan);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
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
        String old = this.mainVehicle;
        if (mainVehicle != null) {
            VehicleType vehicleType = VehiclesHolder.getVehicleById(mainVehicle);
            if (vehicleType == null) {
                ImcSystem sys = ImcSystemsHolder.lookupSystemByName(mainVehicle);
                if (sys == null) {
                    NeptusLog.pub().error(
                            "tried to add a main vehicle but no info in vehicles holder or imc systems holder. "
                                    + mainVehicle);
                    return;
                }
            }
            this.mainVehicle = mainVehicle;
            // if that vehicle still doesn't exist in the console add it
            if (getSystem(this.mainVehicle) == null) {
                addSystem(this.mainVehicle);
            }

            if (this.mainVehicle != null)
                consoleSystems.get(mainVehicle).enableIMC();
            if (old != null && !(old.equals(mainVehicle))) {
                for (MainVehicleChangeListener mlistener : mainVehicleListeners) {
                    try {
                        mlistener.mainVehicleChange(mainVehicle);
                    }
                    catch (Exception ex) {
                        NeptusLog.pub().error(ex, ex);
                    }
                }
            }
        }
        else {
            this.mainVehicle = null;
        }

        if (old != null && !(old.equals(mainVehicle))) {
            this.post(new ConsoleEventMainSystemChange(old, this.mainVehicle));
        }
    }

    /**
     * Add a new vehicle to the vehicle list of console
     * 
     * @param id Vehicle ID
     */
    public void addSystem(String id) {
        VehicleType vehicle = VehiclesHolder.getVehicleById(id);
        ConsoleSystem vtl;
        if (vehicle == null) {
            NeptusLog.pub().warn("tried to add a vehicle from imc that doesnt exist in the vehicle holder (XML stuff)");
            return;
        }
        if (consoleSystems.get(id) != null) {
            NeptusLog.pub().warn("WTH are you trying to add a vehicle that already exist in the console!!");
            return;
        }
        else {
            vtl = new ConsoleSystem(id, this, vehicle, imcMsgManager);
            consoleSystems.put(id, vtl);

        }
        for (ConsoleVehicleChangeListener cvl : consoleVehicleChangeListeners) {
            try {
                cvl.consoleVehicleChange(VehiclesHolder.getVehicleById(id), ConsoleVehicleChangeListener.VEHICLE_ADDED);
            }
            catch (Exception e) {
                NeptusLog.pub().error(e);
            }
        }
        this.post(new ConsoleEventNewSystem(vtl));
        vtl.enableIMC();
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
        for (SubPanel sp : subPanels) {
            sp.init();
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
    public void inXML(String d) {
        ConsoleParse.parseString(d, this, this.getFileName().toString());
    }

    /**
     * This will not go to the children of {@link ContainerSubPanel}
     * 
     * @return
     */
    public List<SubPanel> getSubPanels() {
        return subPanels;
    }

    /**
     * 
     * @param subPanelType
     * @return
     */
    public <T extends SubPanel> Vector<T> getSubPanelsOfClass(Class<T> subPanelType) {
        try {
            Vector<T> ret = new Vector<T>();

            if (subPanels.isEmpty())
                return ret;

            for (SubPanel sp : subPanels) {
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
    private <T extends SubPanel> Vector<T> getSubPanelType(SubPanel sp, Class<T> superClass) {
        Vector<T> ret = new Vector<T>();
        if (sp == null)
            return ret;

        if (sp.getClass().equals(superClass))
            ret.add((T) sp);
//        else if (ReflectionUtil.isSubclass(sp.getClass(), superClass))
        else if (superClass.isAssignableFrom(sp.getClass()))
            ret.add((T) sp);

        if (sp instanceof ContainerSubPanel) {
            for (SubPanel s : ((ContainerSubPanel) sp).getSubPanels()) {
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
    @SuppressWarnings("unchecked")
    public <T> Vector<T> getSubPanelsOfInterface(Class<T> interfaceType) {
        Vector<T> ret = new Vector<T>();
        HashSet<T> col = new HashSet<T>();
        
        if (subPanels.isEmpty())
            return ret;

        for (SubPanel sp : subPanels) {

            if (ReflectionUtil.hasInterface(sp.getClass(), interfaceType))
                col.add((T) sp);

            else if (ReflectionUtil.isSubclass(sp.getClass(), ContainerSubPanel.class)) {
                for (SubPanel s : ((ContainerSubPanel) sp).getSubPanels()) {
                    Vector<T> rSp = getSubPanelImplementations(s, interfaceType);
                    if (rSp.size() > 0)
                        col.addAll(rSp);
                }
            }
        }
        ret.addAll(col);
        return ret;
    }

    @SuppressWarnings("unchecked")
    private <T> Vector<T> getSubPanelImplementations(SubPanel sp, Class<T> interfaceType) {
        Vector<T> ret = new Vector<T>();

        if (ReflectionUtil.hasInterface(sp.getClass(), interfaceType))
            ret.add((T) sp);

        if (ReflectionUtil.isSubclass(sp.getClass(), ContainerSubPanel.class)) {
            for (SubPanel s : ((ContainerSubPanel) sp).getSubPanels()) {
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

    public void informSubPanelListener(SubPanel sub, SubPanelChangeAction action) {
        for (SubPanelChangeListener spcl : subPanelListeners)
            spcl.subPanelChanged(new SubPanelChangeEvent(sub, action));
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
        setPlan(null);
        setMainSystem(null);
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
        System.out.println("console layout cleanup start");
        try {
            removeComponentListener(this);

            this.imcOff();
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

            for (ConsoleSystem system : consoleSystems.values()) {
                system.clean();
            }
            consoleSystems.clear();
            mainPanel.clean();
            statusBar.clean();

            setMission(null);
            setPlan(null);
            setMainSystem(null);

            NeptusEvents.clean();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (controllerPanel != null)
            controllerPanel.cleanup();

        System.out.println("console layout cleanup end in " + ((System.currentTimeMillis() - start) / 1E3) + "s ");
    }

    private Rectangle2D minimizedBounds = null;
    private SubPanel maximizedPanel = null;

    public void minimizePanel(SubPanel p) {

        p.setVisible(false);

        if (maximizedPanel != null)
            p = maximizedPanel;
        if (minimizedBounds == null)
            return;

        if (subPanels.indexOf(p) == -1) {
            for (SubPanel tmp : subPanels) {
                for (SubPanel c : tmp.getChildren())
                    if (c.equals(p))
                        p = tmp;
            }
        }
        else {
            p.setBounds(minimizedBounds.getBounds());
            for (SubPanel tmp : subPanels) {
                if (!(tmp instanceof SimpleSubPanel) || ((SimpleSubPanel) tmp).getVisibility())
                    tmp.setVisible(true);
            }

        }

        minimizedBounds = null;
        maximizedPanel = null;

        mainPanel.revalidate();
    }

    public void maximizePanel(SubPanel p) {

        if (maximizedPanel != null)
            minimizePanel(maximizedPanel);

        if (subPanels.indexOf(p) == -1) {
            for (SubPanel tmp : subPanels) {
                for (SubPanel c : tmp.getChildren())
                    if (c.equals(p))
                        p = tmp;
            }
        }
        else {

            for (SubPanel tmp : subPanels) {
                if (tmp.getRootPane() != null && tmp.getRootPane().equals(p.getRootPane()))
                    tmp.setVisible(false);
            }

            Component tmp = p;
            while (tmp.getParent() != mainPanel && tmp.getParent() != null) {
                tmp = tmp.getParent();
            }

            if (tmp instanceof SubPanel)
                p = (SubPanel) tmp;
            minimizedBounds = p.getBounds();
            maximizedPanel = p;

            p.setBounds(new Rectangle(0, 0, p.getMainpanel().getWidth(), p.getMainpanel().getHeight()));
            p.setSize(getMainPanel().getSize());
            p.setVisible(true);

            p.getMainpanel().revalidate();
            p.getMainpanel().repaint();
        }
    }

    public SubPanel getMaximizedPanel() {
        return maximizedPanel;
    }

    public void setMaximizedPanel(SubPanel maximizedPanel) {
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
        // System.out.println("Ok:"+ct);
        if (getMission() != null) {
            if (getMission().getCompressedFilePath() != null) {
                File file = new File(getMission().getCompressedFilePath());
                CheckListExe.showCheckListExeDialog(getMainSystem(), ct, this, file.getParent());
            }
        }
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

    public ImcSysState getImcState() {
        return getImcState(getMainSystem());
    }

    public ImcSysState getImcState(String system) {
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
            title.append(" | Mission: ");
            title.append(mission.getName());
            title.append(" [" + mission.getCompressedFilePath() + "]");
        }
        this.setTitle(title.toString());
    }

    public void imcOn() {
        if (this.imcMsgManager.start()) {
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
     * @return the vehicleTreeListeners
     */
    public Map<String, ConsoleSystem> getConsoleSystems() {
        return consoleSystems;
    }

    /**
     * Get ConsoleSystem by ID
     * 
     * @param id
     * @return
     */
    public ConsoleSystem getSystem(String id) {
        return this.consoleSystems.get(id);
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

    // MAIN FOR TESTING ONLY
    public static void main(String[] args) {
        Loader loader = new Loader();
        loader.start();
        ConfigFetch.initialize();
        ConfigFetch.setSuperParentFrameForced(loader);

        loader.setText(I18n.text("Loading Plug-ins..."));
        PluginClassLoader.install();
        loader.setText(I18n.text("Loading Systems..."));

        if (!VehiclesHolder.loadVehicles()) {
            GuiUtils.errorMessage(loader, I18n.text("Loading Systems"), I18n.text("Error loading systems!"));
        }

        if (!MiscSystemsHolder.loadMiscSystems()) {
            GuiUtils.errorMessage(loader, I18n.text("Loading Misc Systems"), I18n.text("Error loading misc systems!"));
        }
        
        loader.setText(I18n.text("Loading Console..."));
        
        // GuiUtils.setSystemLookAndFeel();
        GuiUtils.setLookAndFeel();
        
        // ConsoleLayout.forge("conf/consoles/seacon-light.ncon");
        ConsoleLayout.forge("conf/consoles/lauv.ncon", loader);
        System.out.println("BENCHMARK "+ ((System.currentTimeMillis() - ConfigFetch.STARTTIME) / 1E3) + "s");
        // ConsoleLayout console = new ConsoleLayout();
        // console.setVisible(true);
    }
}
