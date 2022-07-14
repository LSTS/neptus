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
 * Author: Paulo Dias
 * 2010/05/19
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.EntityParameters;
import pt.lsts.imc.FuelLevel;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystem.IMCAuthorityState;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MonitorIMCComms;
import pt.lsts.neptus.comm.manager.imc.SystemImcMsgCommInfo;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.ConsoleSystem;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.gui.system.AttentionSymbol;
import pt.lsts.neptus.gui.system.AuthoritySymbol;
import pt.lsts.neptus.gui.system.ConnectionSymbol;
import pt.lsts.neptus.gui.system.ConnectionSymbol.ConnectionStrengthEnum;
import pt.lsts.neptus.gui.system.EmergencyTaskSymbol;
import pt.lsts.neptus.gui.system.EmergencyTaskSymbol.EmergencyStatus;
import pt.lsts.neptus.gui.system.FuelLevelSymbol;
import pt.lsts.neptus.gui.system.LocationSymbol;
import pt.lsts.neptus.gui.system.MilStd2525LikeSymbolsDefinitions.SymbolIconEnum;
import pt.lsts.neptus.gui.system.MilStd2525LikeSymbolsDefinitions.SymbolOperationalConditionEnum;
import pt.lsts.neptus.gui.system.MilStd2525LikeSymbolsDefinitions.SymbolShapeEnum;
import pt.lsts.neptus.gui.system.MilStd2525LikeSymbolsDefinitions.SymbolTypeEnum;
import pt.lsts.neptus.gui.system.SystemDisplay;
import pt.lsts.neptus.gui.system.SystemDisplayComparator;
import pt.lsts.neptus.gui.system.SystemPainterHelper;
import pt.lsts.neptus.gui.system.SystemPainterHelper.CircleTypeBySystemType;
import pt.lsts.neptus.gui.system.SystemTypeSymbol;
import pt.lsts.neptus.gui.system.TaskSymbol;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.planeditor.IEditorMenuExtension;
import pt.lsts.neptus.planeditor.IMapPopup;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.CustomInteractionSupport;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.InteractionAdapter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.renderer2d.SystemPainterProvider;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystem.ExternalTypeEnum;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.StringUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@Popup(pos = POSITION.RIGHT, width = 300, height = 600, accelerator = 'S')
@PluginDescription(author = "Paulo Dias", version = "1.7.1", name = "Systems List", icon = "images/imc.png", documentation = "systems-list/systems-list.html")
@LayerPriority(priority = 180)
public class SystemsList extends ConsolePanel implements MainVehicleChangeListener, IPeriodicUpdates,
        Renderer2DPainter, SystemPainterProvider, IEditorMenuExtension, ConfigurationListener, SubPanelChangeListener,
        ISystemsSelection {

    private final int ICON_SIZE = 20;
    public static final int LOD_MIN_TO_SHOW_LABEL = 11;
    public static final int LOD_MIN_TO_SHOW_SPEED_VECTOR = 9;
    public static final int LOD_MIN_OFFSET_FOR_EXTERNAL = 4;

    private static boolean printPaintDebug = false;
    private final Icon ICON_EDIT = ImageUtils.getScaledIcon("images/buttons/edit2.png", ICON_SIZE, ICON_SIZE);
    private final Icon ICON_CLEAR = ImageUtils.getScaledIcon("images/systems/selection-clear.png", ICON_SIZE,
            ICON_SIZE);
    private final Icon ICON_REDO = ImageUtils.getScaledIcon("images/systems/selection-redo.png", ICON_SIZE,
            ICON_SIZE);
    private final Icon ICON_FILTER = ImageUtils.getScaledIcon("images/buttons/filter.png", ICON_SIZE, ICON_SIZE);
    private final Icon ICON_VIEW_INFO = ImageUtils.getScaledIcon("images/systems/view-info.png", ICON_SIZE,
            ICON_SIZE);
    private final Icon ICON_VIEW_EXTRA = ImageUtils.getScaledIcon("images/systems/view-extra.png", ICON_SIZE,
            ICON_SIZE);
    private final Icon ICON_VIEW_SYMBOL = ImageUtils.getScaledIcon("images/systems/view-symbol.png", ICON_SIZE,
            ICON_SIZE);
    private final Icon ICON_VIEW_EXT_SYMBOL = ImageUtils.getScaledIcon("images/systems/view-ext-symbol.png", ICON_SIZE,
            ICON_SIZE);
    private final Icon ICON_VIEW_EXPAND = ImageUtils.getScaledIcon("images/systems/expand.png", ICON_SIZE,
            ICON_SIZE);
    private final Icon ICON_VIEW_RETREAT = ImageUtils.getScaledIcon("images/systems/retreat.png", ICON_SIZE,
            ICON_SIZE);

    private final Pattern RPM_PATTERN_SEARCH = Pattern.compile("^\\{[^=]+=([\\d\\.,]+)\\}$");

    public enum SortOrderEnum {
        UNSORTED,
        SORTED
    };

    public enum MilStd2525SymbolsFilledEnum {
        FILLED,
        NOT_FILLED
    };
    
    @NeptusProperty(name = "Systems Icons Size", description = "Configures the state symbols size for the system box", 
            category = "List", userLevel = LEVEL.ADVANCED)
    public int iconsSize = 15;

    @NeptusProperty(name = "System Indicator Icons Size", description = "Configures the state symbols size for the OSD (on screen display) balloon", 
            category = "Renderer", userLevel = LEVEL.ADVANCED)
    public int indicatorsSize = 15;

    @NeptusProperty(name = "Main System Icon Size Increment", description = "Configures the increment in size for the system box for the operational console main system", 
            category = "List", userLevel = LEVEL.ADVANCED)
    public int mainSizeIncrement = 5;

    @NeptusProperty(name = "Show System Symbol Or Text", description = "Configures if for the system type symbol will use the picture symbol or the text version", 
            category = "Layout")
    public boolean showSystemSymbolOrText = false;

    @NeptusProperty(name = "System Class Filter", description = "This is the filter. This filter will show in the list only the system that match the filter", 
            category = "Filter", userLevel = LEVEL.REGULAR)
    public SystemTypeEnum systemsFilter = SystemTypeEnum.VEHICLE;

    @NeptusProperty(name = "System Vehicle Class Subtype Filter", description = "In case the previous filter is VEHICLE, can filter by subtype", 
            category = "Filter", userLevel = LEVEL.REGULAR)
    public VehicleTypeEnum vehicleTypeFilter = VehicleTypeEnum.ALL;

    @NeptusProperty(name = "Systems Ordering", description = "This configures if the systems should be shown UNSORTED or SORTED", 
            category = "Filter", userLevel = LEVEL.REGULAR)
    public SortOrderEnum systemsOrdering = SortOrderEnum.SORTED;

    @NeptusProperty(name = "Show System With Authority Equal Or Above", description = "This configures if the systems should be shown dependent on the Authority State", 
            category = "Filter", userLevel = LEVEL.REGULAR)
    public IMCAuthorityState showSystemWithAuthorityEqualOrAbove = IMCAuthorityState.OFF;

    @NeptusProperty(name = "Systems Ordering Option", description = "This configures the ordering type", 
            category = "Filter", userLevel = LEVEL.REGULAR)
    public SystemDisplayComparator.OrderOptionEnum orderingOption = SystemDisplayComparator.OrderOptionEnum.ID_AUTHORITY_MAIN;

    @NeptusProperty(name = "Enable Selection", description = "Configures if system selection is active or not", 
            userLevel = LEVEL.ADVANCED)
    public boolean enableSelection = true;

    @NeptusProperty(name = "Show Systems Icons On Renderer", description = "If true, if this component is showing systems icons, it will disable them on the renderer panel.", 
            editable = false)
    public boolean showSystemsIconsOnRenderer = true;

    @NeptusProperty(name = "Renderer Icons Size", description = "Configures the system symbols size for the renderer", 
            category = "Layout", userLevel = LEVEL.REGULAR)
    public int rendererIconsSize = 25;

    @NeptusProperty(name = "Adapt Icons Size with Zoom", description = "Configures the system symbols size reducing with zoom for declustering",
            category = "Layout", userLevel = LEVEL.REGULAR)
    public boolean adaptIconsSizeWithZoom = true;

    @NeptusProperty(name = "Show Systems Icons That Are Filtered Out", description = "If true, if this component will show systems icons on renderer that were filtered out. Option to show system icons has to be enabled", 
            category = "Systems in Renderer", userLevel = LEVEL.REGULAR)
    public boolean showSystemsIconsThatAreFilteredOut = true;

    @NeptusProperty(name = "Show External Systems Icons", description = "If true, if this component will show external systems icons on renderer. Option to show system icons has to be enabled", 
            category = "Systems in Renderer", userLevel = LEVEL.REGULAR)
    public boolean showExternalSystemsIcons = true;

    @NeptusProperty(name = "Minimum Speed To Be Stopped", description = "Configures the maximum speed (m/s) for the system to be considered stoped (affects the drawing of the course/speed vector on the renderer)",
            category = "Renderer", userLevel = LEVEL.REGULAR)
    public double minimumSpeedToBeStopped = 0.1;

    @NeptusProperty(name = "Draw System Label", description = "Configures if this component will draw the system label on the renderer",
            category = "Renderer", userLevel = LEVEL.REGULAR)
    public boolean drawSystemLabel = true;

    @NeptusProperty(name = "Draw System Location Age", description = "Configures if this component will draw the system location age on the renderer",
            category = "Renderer", userLevel = LEVEL.REGULAR)
    public boolean drawSystemLocAge = true;

    @NeptusProperty(name = "Use Mil Std 2525 Like Symbols", description = "This configures if the location symbols to draw on the renderer will use the MIL-STD-2525 standard", 
            category = "MilStd-2525", userLevel = LEVEL.REGULAR)
    public boolean useMilStd2525LikeSymbols = false;

    @NeptusProperty(name = "Mil Std 2525 Symbols Filled Or Not", description = "This configures if the symbol is to be filled or not", 
            category = "MilStd-2525", userLevel = LEVEL.REGULAR)
    public MilStd2525SymbolsFilledEnum milStd2525FilledOrNot = MilStd2525SymbolsFilledEnum.FILLED;

    @NeptusProperty(name = "Minutes To Hide Systems Without Known Location", description = "Minutes after which systems disapear from render if inactive (0 to disable)", 
            category = "Systems in Renderer", userLevel = LEVEL.REGULAR)
    public int minutesToHideSystemsWithoutKnownLocation = 5;

    @NeptusProperty(name = "Draw Circle Arround System Icon In Render Dependent Of System Type", description = "This configures if the circle arround the symbol in render is to be drawn dependent of system type",
            category = "Renderer", userLevel = LEVEL.REGULAR)
    public boolean drawCircleInRenderDependentOfSystemType = true;
    
    @NeptusProperty(name = "Minutes to Show Distress Signal", category = "Test", userLevel = LEVEL.ADVANCED)
    private int minutesToShowDistress = 5; 

    private final SystemDisplayComparator comparator = new SystemDisplayComparator();

    private final LinkedHashMap<ImcSystem, SystemDisplay> systems = new LinkedHashMap<ImcSystem, SystemDisplay>();

    private Vector<IMapPopup> renderersPopups;
    private Vector<ILayerPainter> renderers;
    private Vector<CustomInteractionSupport> renderersInteractions;

    private JPanel holder;
    private JScrollPane scrollPane;
    private JPanel toolbar;
    private ToolbarButton editConf, clearSelection, redoSelection, expandAll, retractAll;
    private ToolbarSwitch viewInfoOSDSwitch, filterSwitch, viewExtendedOSDSwitch, viewIconsSwitch, viewExternalSystemsSwitch;
    private AbstractAction editConfAction, clearSelectionAction, redoSelectionAction, viewInfoOSDAction,
            filterSwitchAction, viewExtendedOSDAction, viewIconsAction, viewExternalSystemsAction;

    private boolean updateMainVehicle = true;
    private boolean updateOrdering = true;

    private final Vector<SystemDisplay> prevSelection = new Vector<SystemDisplay>();

    private InteractionAdapter stateRendererInteraction = null;

    // Symbols to be used on OSD balloons on renderer
    private SystemTypeSymbol sts = new SystemTypeSymbol();
    private ConnectionSymbol cns = new ConnectionSymbol();
    private LocationSymbol locs = new LocationSymbol();
    private AuthoritySymbol ats = new AuthoritySymbol() {
        { fullOrNoneOnly = true; }
    };
    private TaskSymbol tks = new TaskSymbol();
    private AttentionSymbol as = new AttentionSymbol();
    private FuelLevelSymbol fl = new FuelLevelSymbol();
    private EmergencyTaskSymbol es = new EmergencyTaskSymbol();
    {
        sts.setBlinkOnChange(false);
        cns.setBlinkOnChange(false);
        locs.setBlinkOnChange(false);
        ats.setBlinkOnChange(false);
        tks.setBlinkOnChange(false);
        as.setBlinkOnChange(false);
        fl.setBlinkOnChange(false);
        es.setBlinkOnChange(false);
    }

    /**
	 * 
	 */
    public SystemsList(ConsoleLayout console) {
        super(console);
        initialize();
    }

    private void initialize() {
        removeAll();

        setSize(220, 250);

        initializeActions();

        this.setLayout(new BorderLayout());

        holder = new JPanel(true);
        holder.setLayout(new BoxLayout(holder, BoxLayout.Y_AXIS));

        editConf = new ToolbarButton(editConfAction);
        if (getConsole() != null)
            editConf.setVisible(false);
        clearSelection = new ToolbarButton(clearSelectionAction);
        redoSelection = new ToolbarButton(redoSelectionAction);
        viewInfoOSDSwitch = new ToolbarSwitch(viewInfoOSDAction);
        viewInfoOSDSwitch.setSelected(false);
        viewExtendedOSDSwitch = new ToolbarSwitch(viewExtendedOSDAction);
        viewExtendedOSDSwitch.setSelected(false); // showExtendedOSD
        viewIconsSwitch = new ToolbarSwitch(viewIconsAction);
        viewIconsSwitch.setSelected(showSystemsIconsOnRenderer);
        filterSwitch = new ToolbarSwitch(filterSwitchAction);
        filterSwitch.setSelected(true);
        viewExternalSystemsSwitch = new ToolbarSwitch(viewExternalSystemsAction);
        viewExternalSystemsSwitch.setSelected(showExternalSystemsIcons);
        expandAll = new ToolbarButton(new AbstractAction(I18n.text("Expand extra info."), ICON_VIEW_EXPAND) {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (SystemDisplay sd : systems.values().toArray(new SystemDisplay[0])) {
                    sd.setShowExtraInfoVisible(true);
                }
            }
        });
        // expandAll.setText("\u02C5"); // "\u25BC"
        retractAll = new ToolbarButton(new AbstractAction(I18n.text("Retract extra info."), ICON_VIEW_RETREAT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (SystemDisplay sd : systems.values().toArray(new SystemDisplay[0])) {
                    sd.setShowExtraInfoVisible(false);
                }
            }
        });
        // retractAll.setText("\u02C4"); // "\u25B2"

        toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.LINE_AXIS));
        toolbar.add(editConf);
        // toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(clearSelection);
        toolbar.add(redoSelection);
        // toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(viewInfoOSDSwitch);
        toolbar.add(viewExtendedOSDSwitch);
        toolbar.add(viewIconsSwitch);
        toolbar.add(viewExternalSystemsSwitch);
        // toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(filterSwitch);
        toolbar.add(expandAll);
        toolbar.add(retractAll);

        scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        // scrollPane.setPreferredSize(new Dimension(800,600));
        scrollPane.setViewportView(holder);

        add(toolbar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        holder.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SystemDisplay sd = null;
                try {
                    try {
                        sd = (SystemDisplay) e.getSource();
                    }
                    catch (ClassCastException ex) {
                        return;
                    }
                    if (MouseEvent.BUTTON1 == e.getButton() && (e.isControlDown() || e.getClickCount() == 2)) {
                        String veh = sd.getId();
                        if (!(e.isAltDown() || e.isAltGraphDown())) {
                            if (getConsole() != null) {
                                getConsole().setMainSystem(veh);
                                if (veh.equalsIgnoreCase(getConsole().getMainSystem())) {
                                    ImcSystem sys = ImcSystemsHolder.lookupSystemByName(veh);
                                    if (sys == null) {
                                        sd.setWithAuthority(ImcSystem.IMCAuthorityState.SYSTEM_FULL);
                                    }
                                    else {
                                        sys.setAuthorityState(ImcSystem.IMCAuthorityState.SYSTEM_FULL);
                                    }
                                }
                            }
                        }
                        else {
                            if (!sd.isMainVehicle()) {
                                try {
                                    ImcSystem sys = ImcSystemsHolder.lookupSystemByName(veh);
                                    if (sys == null || sys.getVehicle() == null) {
                                        // sd.setWithAuthority(!sd.isWithAuthority());
                                        sd.setWithAuthority(sd.isWithAuthority() ? ImcSystem.IMCAuthorityState.NONE
                                                : ImcSystem.IMCAuthorityState.SYSTEM_FULL
                                                /* ImcSystem.IMCAuthorityState.SYSTEM_MONITOR */);
                                    }
                                    else {
                                        IMCAuthorityState authToChange = ImcSystem.IMCAuthorityState.NONE;
                                        if (!sys.isWithAuthority())
                                            authToChange = ImcSystem.IMCAuthorityState.SYSTEM_FULL
                                                    /* ImcSystem.IMCAuthorityState.SYSTEM_MONITOR */;

                                        sys.setAuthorityState(authToChange);
                                        ConsoleSystem vtl = getConsole().getSystem(veh);
                                        if (vtl == null) {
                                            getConsole().addSystem(veh);
                                            sys.setAuthorityState(authToChange);
                                        }
                                        else {
                                            vtl.toggleIMC();
                                        }
                                    }
                                }
                                catch (Exception e2) {
                                    e2.printStackTrace();
                                    // sd.setWithAuthority(!sd.isWithAuthority());
                                    // sd.setWithAuthority(sd.isWithAuthority() ? ImcSystem.IMCAuthorityState.NONE :
                                    // ImcSystem.IMCAuthorityState.SYSTEM_MONITOR);
                                }
                            }
                            // else {
                            // sd.setWithAuthority(false);
                            // ImcSystem[] syss = ImcSystemsHolder.lookupSystemByName(sd.getId());
                            // if (syss.length > 0) {
                            // syss[0].set
                            // }
                            // }

                            // if (sys[0].isWithAuthority()) {
                            // sys[0].setAuthorityState(ImcSystem.IMCAuthorityState.NONE);
                            // }
                            // else {
                            // sys[0].setAuthorityState(ImcSystem.IMCAuthorityState.SYSTEM_MONITOR);
                            // }

                        }
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                // if (MouseEvent.BUTTON1 == e.getButton()) {
                // e.setSource(SystemDisplay.this);
                // super.mouseClicked(e);
                // }
            }
        });

        repaint();
    }

    /**
     * 
     */
    private void initializeActions() {
        editConfAction = new AbstractAction(I18n.text("Edit Systems List Configurations"), ICON_EDIT) {
            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(SystemsList.this, SwingUtilities.getWindowAncestor(SystemsList.this),
                        true);
            }
        };

        clearSelectionAction = new AbstractAction(I18n.text("Clear Selection"), ICON_CLEAR) {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearSelectedSystems();
            }
        };

        redoSelectionAction = new AbstractAction(I18n.text("Redo Selection"), ICON_REDO) {
            @Override
            public void actionPerformed(ActionEvent e) {
                redoSelectedSystems();
            }
        };

        viewInfoOSDAction = new AbstractAction(I18n.text("Show systems info OSD in render"), ICON_VIEW_INFO) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected())
                    viewExtendedOSDSwitch.setSelected(false);
            }
        };

        viewExtendedOSDAction = new AbstractAction(I18n.text("Show systems extended OSD in render"), ICON_VIEW_EXTRA) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ToolbarSwitch) e.getSource()).isSelected())
                    viewInfoOSDSwitch.setSelected(false);
            }
        };

        viewIconsAction = new AbstractAction(I18n.text("View systems icons in render"), ICON_VIEW_SYMBOL) {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSystemsIconsOnRenderer = viewIconsSwitch.isSelected();
            }
        };

        filterSwitchAction = new AbstractAction(I18n.text("Enable filter"), ICON_FILTER) {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateMainVehicle = true;
                updateOrdering = true;
            }
        };

        viewExternalSystemsAction = new AbstractAction(I18n.text("View external systems icons in render"), ICON_VIEW_EXT_SYMBOL) {
            @Override
            public void actionPerformed(ActionEvent e) {
                showExternalSystemsIcons = viewExternalSystemsSwitch.isSelected();
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.SubPanel#postLoadInit()
     */
    @Override
    public void initSubPanel() {

        renderersPopups = getConsole().getSubPanelsOfInterface(IMapPopup.class);
        for (IMapPopup str2d : renderersPopups) {
            str2d.addMenuExtension(this);
        }

        renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
        for (ILayerPainter str2d : renderers) {
            str2d.addPostRenderPainter(this, "Systems List");
        }

        renderersInteractions = getConsole().getSubPanelsOfInterface(CustomInteractionSupport.class);
        // for (CustomInteractionSupport str2d : renderersInteractions) {
        // str2d.addInteraction(getInteractionInterface());
        // }

        updateMainVehicle = true;

        updateSystemsList();

        if (getConsole() != null) {
            String mainVeh = getConsole().getMainSystem();
            changeMainVehicle(mainVeh);
        }
    }

    @Override
    public void cleanSubPanel() {
        if (renderersPopups != null) {
            for (IMapPopup str2d : renderersPopups) {
                str2d.removeMenuExtension(this);
            }
        }
        if (renderers != null) {
            for (ILayerPainter str2d : renderers) {
                str2d.removePostRenderPainter(this);
            }
        }
        if (renderersInteractions != null) {
            for (CustomInteractionSupport str2d : renderersInteractions) {
                str2d.removeInteraction(getInteractionInterface());
            }
        }
        prevSelection.clear();

        if (sts != null)
            sts.dispose();
        sts = null;
        
        if (cns != null)
            cns.dispose();
        cns = null;
        
        if (locs != null)
            locs.dispose();
        locs = null;
        
        if (ats != null)
            ats.dispose();
        ats = null;
        
        if (tks != null)
            tks.dispose();
        tks = null;
        
        if (as != null)
            as.dispose();
        as = null;
        
        if (fl != null)
            fl.dispose();
        fl = null;
        
        if (es != null)
            es.dispose();
        es = null;

        for (SystemDisplay sd : systems.values()) {
            sd.dispose();
        }
        holder.removeAll();
        systems.clear();

        holder = null;
        scrollPane = null;
        toolbar = null;
        editConf = null;
        clearSelection = null;
        redoSelection = null;
        expandAll = null;
        retractAll = null;
        viewInfoOSDSwitch = null;
        filterSwitch = null;
        viewExtendedOSDSwitch = null;
        viewIconsSwitch = null;
    }

    /**
     * @return the iconsSize
     */
    public int getIconsSize() {
        return iconsSize;
    }

    /**
     * @param iconsSize the iconsSize to set
     */
    public void setIconsSize(int iconsSize) {
        this.iconsSize = iconsSize;
        propertiesChanged();
    }

    /**
     * @return the indicatorsSize
     */
    public int getIndicatorsSize() {
        return indicatorsSize;
    }

    /**
     * @param indicatorsSize the indicatorsSize to set
     */
    public void setIndicatorsSize(int indicatorsSize) {
        this.indicatorsSize = indicatorsSize;
        propertiesChanged();
    }

    /**
     * @return the enableSelection
     */
    public boolean isEnableSelection() {
        return enableSelection;
    }

    /**
     * @param enableSelection the enableSelection to set
     */
    public void setEnableSelection(boolean enableSelection) {
        this.enableSelection = enableSelection;
        propertiesChanged();
    }

    public boolean isViewEnable() {
        return viewInfoOSDSwitch.isEnabled();
    }

    /**
     * @param enableSelection the enableSelection to set
     */
    public void setViewEnable(boolean enable) {
        viewInfoOSDSwitch.setEnabled(enable);
        viewExtendedOSDSwitch.setEnabled(enable);
        viewIconsSwitch.setEnabled(enable);
        if (!enable) {
            viewInfoOSDSwitch.setSelected(enable);
            viewExtendedOSDSwitch.setSelected(enable);
            viewIconsSwitch.setSelected(enable);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.renderer2d.SystemPainterProvider#isSystemPainterEnabled()
     */
    @Override
    public boolean isSystemPainterEnabled() {
        return showSystemsIconsOnRenderer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.SubPanelChangeListener#subPanelChanged(pt.lsts.neptus.consolebase.
     * SubPanelChangeEvent)
     */
    @Override
    public void subPanelChanged(SubPanelChangeEvent panelChange) {

        if (panelChange == null)
            return;

        renderersPopups = getConsole().getSubPanelsOfInterface(IMapPopup.class);

        if (ReflectionUtil.hasInterface(panelChange.getPanel().getClass(), IMapPopup.class)) {

            IMapPopup sub = (IMapPopup) panelChange.getPanel();

            if (panelChange.added()) {
                renderersPopups.add(sub);
                IMapPopup str2d = sub;
                if (str2d != null) {
                    str2d.addMenuExtension(this);
                }
            }

            if (panelChange.removed()) {
                renderersPopups.remove(sub);
                IMapPopup str2d = sub;
                if (str2d != null) {
                    str2d.removeMenuExtension(this);
                }
            }
        }

        if (ReflectionUtil.hasInterface(panelChange.getPanel().getClass(), ILayerPainter.class)) {

            ILayerPainter sub = (ILayerPainter) panelChange.getPanel();

            if (panelChange.added()) {
                renderers.add(sub);
                ILayerPainter str2d = sub;
                if (str2d != null) {
                    str2d.addPostRenderPainter(this, "System List");
                }
            }

            if (panelChange.removed()) {
                renderers.remove(sub);
                ILayerPainter str2d = sub;
                if (str2d != null) {
                    str2d.removePostRenderPainter(this);
                }
            }
        }

        if (ReflectionUtil.hasInterface(panelChange.getPanel().getClass(), CustomInteractionSupport.class)) {

            CustomInteractionSupport sub = (CustomInteractionSupport) panelChange.getPanel();

            if (panelChange.added()) {
                renderersInteractions.add(sub);
                CustomInteractionSupport str2d = sub;
                if (str2d != null) {
                    // str2d.addInteraction(getInteractionInterface());
                }
            }

            if (panelChange.removed()) {
                renderersInteractions.remove(sub);
                CustomInteractionSupport str2d = sub;
                if (str2d != null) {
                    str2d.removeInteraction(getInteractionInterface());
                }
            }
        }
    }

    // ------------- MainVehicleChange -------------------------------------------------------------

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange ev) {
        update();
        changeMainVehicle(ev.getCurrent());
    }

    /**
     * @param id
     */
    private void changeMainVehicle(String id) {
        ImcSystem system = ImcSystemsHolder.lookupSystemByName(id);
        if (system == null)
            return;
        if (systems.containsKey(system)) {
            for (ImcSystem sys : systems.keySet().toArray(new ImcSystem[0])) {
                SystemDisplay display = systems.get(sys);
                if (display == null)
                    continue;
                if (sys.compareTo(system) == 0) {
                    if (!display.isMainVehicle()) {
                        display.setIconSize(iconsSize + mainSizeIncrement);
                        display.setIndicatorsSize(indicatorsSize + mainSizeIncrement);
                        display.setIncrementFontSize(mainSizeIncrement);
                    }
                    display.setMainVehicle(true);
                }
                else {
                    if (display.isMainVehicle()) {
                        display.setIconSize(iconsSize);
                        display.setIndicatorsSize(indicatorsSize);
                        display.setIncrementFontSize(0);
                    }
                    display.setMainVehicle(false);
                }
            }
        }
    }

    // ------------- Update -----------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        return 500;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
        if (filterSwitch == null) {
            NeptusLog.pub().info("<###> "+SystemsList.class.getSimpleName()
                    + " : update of SystemsList called after cleanup call!!!!!");
            return true;
        }

        updateSystemsList();

        boolean enable = true;
        if (renderers == null || renderers.size() == 0) {
            enable = false;
        }
        viewInfoOSDSwitch.setEnabled(enable);
        viewExtendedOSDSwitch.setEnabled(enable);
        viewIconsSwitch.setEnabled(enable);

        repaint();
        return true;
    }

    private synchronized void updateSystemsList() {
        if (filterSwitch == null) {
            NeptusLog.pub().info("<###> "+SystemsList.class.getSimpleName()
                    + " : updateSystemsList called after cleanup call!!!!!");
            return;
        }
        ImcSystem[] filteredSystems = ImcSystemsHolder.lookupSystemByType(filterSwitch.isSelected() ? systemsFilter
                : SystemTypeEnum.ALL);
        for (ImcSystem sys : filteredSystems) {
            boolean isToAdd = false;
            SystemDisplay sd = systems.get(sys);
            if (sd == null) {
                sd = createSystemElement(sys);
                isToAdd = true;
            }
            else {
                sd.updateId(sys.getName());
            }

            sd.setActive(sys.isActive());
            try {
                SystemImcMsgCommInfo commS = ImcMsgManager.getManager().getCommInfoById(sys.getId());
                long deltaMillis = System.currentTimeMillis() - (long) commS.getArrivalTimeMillisLastMsg();
                if (deltaMillis >= DateTimeUtil.MINUTE * 20)
                    sd.setActiveToolTip(null);
                else if (deltaMillis > 10000)
                    sd.setActiveToolTip(I18n.textf("%deltaTime with no messages",
                            DateTimeUtil.milliSecondsToFormatedString(deltaMillis)));
                else
                    sd.setActiveToolTip(null);

                if (deltaMillis > 10000)
                    sd.setConnectionStrength(ConnectionStrengthEnum.LOW);
                else if (deltaMillis > 7000)
                    sd.setConnectionStrength(ConnectionStrengthEnum.MEDIAN);
                else if (deltaMillis > 4000)
                    sd.setConnectionStrength(ConnectionStrengthEnum.HIGH);
                else
                    sd.setConnectionStrength(ConnectionStrengthEnum.FULL);
            }
            catch (Exception e) {
                e.printStackTrace();
                sd.setActiveToolTip(null);
            }

            if (sys.isOnAnnounceState() && System.currentTimeMillis() - sys.getLastAnnounceStateReceived() < 12000)
                sd.setAnnounceReceived(true);
            else
                sd.setAnnounceReceived(false);

            sd.setTaskAlocated(sys.getActivePlan() != null);
            sd.setTaskAlocatedToolTip((sys.getActivePlan() != null) ? sys.getActivePlan().getId() : null);
            if (sys.getType() == SystemTypeEnum.VEHICLE)
                sd.setSystemType(sys.getTypeVehicle().name());
            else
                sd.setSystemType(sys.getType().name());

            sd.setEmergencyTaskAlocated((sys.getEmergencyPlanId() != null && !"".equalsIgnoreCase(sys
                    .getEmergencyPlanId())) ? true : false);
            sd.setEmergencyTaskAlocatedToolTip((sys.getEmergencyPlanId() != null && !"".equalsIgnoreCase(sys
                    .getEmergencyPlanId())) ? sys.getEmergencyPlanId() : null);
            if (sys.getEmergencyPlanId() != null && !"".equalsIgnoreCase(sys.getEmergencyPlanId())) {
                EmergencyStatus es;
                try {
                    es = EmergencyStatus.valueOf(sys.getEmergencyStatusStr());
                    sd.setEmergencyStatus(es);
                }
                catch (Exception e) {
                    e.printStackTrace();
                    sd.setEmergencyStatus(EmergencyStatus.NOT_CONFIGURED);
                }
            }
            else {
                sd.setEmergencyStatus(EmergencyStatus.NOT_CONFIGURED);
            }

            sd.setAttentionAlert(sys.isOnErrorState());
            sd.setAttentionToolTip(I18n.textf(
                    "last update age: %deltaTime",
                    DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis()
                            - sys.getLastErrorStateReceived())));
            if (sys.isOnErrorState() && sys.getOnErrorStateStr() != null
                    && !"".equalsIgnoreCase(sys.getOnErrorStateStr())) {
                sd.setAttentionToolTip("<html>"
                        + I18n.textf("Entities in error: %errorMessage", sys.getOnErrorStateStr()) + "<br>"
                        + sd.getAttentionToolTip());
            }

            if (sys.containsData(SystemUtils.FUEL_LEVEL_KEY)) {
                sd.setFuelLevel(true);
                FuelLevel fuelLevelMsg = (FuelLevel) sys.retrieveData(SystemUtils.FUEL_LEVEL_KEY);
                long time = sys.retrieveDataTimeMillis(SystemUtils.FUEL_LEVEL_KEY);
                sd.setFuelLevelPercentage(fuelLevelMsg.getValue());
                sd.setFuelLevelToolTip(I18n.textf("Fuel Level: %fuelValue% with %confidanceValue% confidance",
                        ((long) MathMiscUtils.round(fuelLevelMsg.getValue(), 0)),
                        ((long) MathMiscUtils.round(fuelLevelMsg.getConfidence(), 0))));
                if (System.currentTimeMillis() - time > 2 * DateTimeUtil.MINUTE) {
                    sd.setFuelLevel(false);
                }
            }
            else {
                sd.setFuelLevel(false);
                if (!sys.containsData(SystemUtils.FUEL_LEVEL_KEY))
                    sd.setFuelLevelPercentage(-1);
            }
            
            sd.setIdAlert(sys.isOnIdErrorState());
            sd.setIdAttentionToolTip(I18n.textf("last update age: %deltaTime",
                    DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis()
                            - sys.getLastIdErrorStateReceived())));

            VehicleType vehT = sys.getVehicle();
            if (vehT != null) {
                sd.setDisplayColor(vehT.getIconColor());
            }

            try {
                if (sd.getSystemImage() == null) {
                    VehicleType veh = sys.getVehicle();
                    if (veh != null) {
                        String fxPath = veh.getSideImageHref();
                        if (veh.getPresentationImageHref() != null && !"".equals(veh.getPresentationImageHref()))
                            fxPath = veh.getPresentationImageHref();
                        sd.setSystemImage(ImageUtils.getImage(fxPath));
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                NeptusLog.pub().debug("System " + sys.getName() + " image was not loaded!");
            }

            if (sd.isMainVehicle()) {
                sys.setAuthorityState(IMCAuthorityState.SYSTEM_FULL);
            }

            try {
                // VehicleType veh = sys.getVehicle();
                // VehicleTreeListener vtl = (veh == null || getConsole() == null) ? null : getConsole()
                // .getVehicleTreeListener(veh.getId());
                // boolean oldValue = sd.isWithAuthority();
                // if (vtl != null) {
                // //sd.setWithAuthority(vtl.isNeptusCommunications());
                // sd.setWithAuthority(sys.getAuthorityState());
                //
                // }
                // // else
                // // sd.setWithAuthority(false);
                // if (sd.isWithAuthority() != oldValue)
                // updateOrdering = true;

                IMCAuthorityState oldValue = sd.getWithAuthority();
                sd.setWithAuthority(sys.getAuthorityState());
                sd.setWithAuthorityToolTip(I18n.text("authority") +": "
                        + sys.getAuthorityState().toString().toLowerCase().replace("_", " "));
                if (sd.getWithAuthority() != oldValue)
                    updateOrdering = true;
            }
            catch (Exception e) {
                e.printStackTrace(); // Ignore exception logging for now
            }

            // Update Loc info age
            if (SystemPainterHelper.isLocationKnown(sys)) {
                sd.setLocationKnown(true);
                sd.setLocationKnownToolTip(I18n.text("Location known"));
            }
            else {
                sd.setLocationKnown(false);
                sd.setLocationKnownToolTip(I18n.text("Unknown location for more than 10s"));
            }

            
            Object epObj = sys.retrieveData(SystemUtils.ENTITY_PARAMETERS);
            if (epObj != null && (epObj instanceof EntityParameters)) {
                sd.updateSystemParameters((EntityParameters) epObj); 
            }
            
            String txtInfo = getSection4Data(sys, true, true);
            if (txtInfo.length() != 0)
                sd.setInfoLabel("<html>" + txtInfo);
            else
                sd.setInfoLabel(I18n.text("No extra info"));

            if (isToAdd) {
                if (!isForRemoval(sd)) {
                    systems.put(sys, sd);
                    if (systemsOrdering != SortOrderEnum.UNSORTED) {
                        int index = -1;
                        for (int i = 0; i < holder.getComponentCount(); i++) {
                            int val = comparator.compare(sd, (SystemDisplay) holder.getComponent(i));
                            if (val < 0) {
                                index = i;
                                break;
                            }
                        }
                        holder.add(sd, index);
                    }
                    else {
                        holder.add(sd);
                    }
                }
            }
        }

        SystemDisplay[] vals = systems.values().toArray(new SystemDisplay[systems.size()]);
        for (SystemDisplay sd : vals) {
            if (isForRemoval(sd) && !sd.isMainVehicle()) {
                holder.remove(sd);
                // systems.remove(sd);
                for (ImcSystem sys : systems.keySet().toArray(new ImcSystem[0])) {
                    if (sd.equals(systems.get(sys))) {
                        systems.remove(sys);
                        break;
                    }
                }
                sd.dispose();
            }
        }

        if (updateMainVehicle) {
            if (getConsole() != null && getConsole().getMainSystem() != null) {
                updateMainVehicle = false;
                updateOrdering = true;
            }
        }

        if (updateOrdering) {
            try {
                if (systemsOrdering != SortOrderEnum.UNSORTED) {
                    for (int i = 0; i < holder.getComponentCount(); i++) {
                        for (int j = i; j > 0
                                && comparator.compare((SystemDisplay) holder.getComponent(j - 1),
                                        (SystemDisplay) holder.getComponent(j)) > 0; j--) {
                            Component cp = holder.getComponent(j);
                            holder.remove(cp);
                            holder.add(cp, j - 1);
                        }
                    }
                    return;
                }
                updateOrdering = false;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param sd
     * @return
     */
    private boolean isForRemoval(SystemDisplay sd) {
        if (filterSwitch != null && !filterSwitch.isSelected())
            return false;

        boolean ret = false;
        if (systemsFilter != SystemTypeEnum.ALL
                && systemsFilter != ImcSystem.translateSystemTypeFromMessage(sd.getSystemType()))
            ret = true;

        if (!ret) {
            if (SystemTypeEnum.VEHICLE == ImcSystem.translateSystemTypeFromMessage(sd.getSystemType())
                    && vehicleTypeFilter != VehicleTypeEnum.ALL
                    && vehicleTypeFilter != ImcSystem.translateVehicleTypeFromMessage(sd.getSystemType()))
                ret = true;
        }

        if (!ret) {
            // filter by Authority
            if (sd.getWithAuthority().ordinal() < showSystemWithAuthorityEqualOrAbove.ordinal())
                ret = true;
        }

        return ret;
    }

    /**
     * @param sys
     * @return
     */
    private final SystemDisplay createSystemElement(ImcSystem sys) {
        SystemDisplay sd = new SystemDisplay(sys.getName());
        sd.setIconSize(iconsSize);
        sd.setIndicatorsSize(indicatorsSize);
        sd.setEnableSelection(enableSelection);
        sd.setActive(sys.isActive());
        sd.setShowSystemSymbolOrText(showSystemSymbolOrText);
        if (sys.getActivePlan() != null)
            sd.setTaskAlocated(true);

        return sd;
    }

    // ------------- Getting textual data ----------------------------------------------------------

    /**
     * @param sys
     * @return
     */
    private String getSection3Data(ImcSystem sys) {
        LocationType loc = sys.getLocation();
        String txtInfo = "", lineSep = "\n";
        if (loc != null && !loc.isLocationEqual(LocationType.ABSOLUTE_ZERO)) {
            loc = loc.convertToAbsoluteLatLonDepth();
            txtInfo += (txtInfo.length() != 0 ? lineSep + " " : "");
            txtInfo += I18n.text("Height") + ": " + MathMiscUtils.round(loc.getHeight(), 1) + " m";
        }
        return txtInfo;
    }

    private String getSection4Data(ImcSystem sys, boolean htmlFragmentOrSimpleText, boolean extendedOrReduced) {
        return getSection4Data(sys, htmlFragmentOrSimpleText, extendedOrReduced, true);
    }

    /**
     * @param sys
     * @return
     */
    private String getSection4Data(ImcSystem sys, boolean htmlFragmentOrSimpleText, boolean extendedOrReduced,
            boolean showHeight) {
        // long timeStampMillis = System.currentTimeMillis();

        long maxAgeTimeMillis = DateTimeUtil.MINUTE;

        String lineSep = htmlFragmentOrSimpleText ? "<br>" : "\n";
        String txtInfo = "";

        if (sys.isSimulated()) {
            txtInfo += (txtInfo.length() != 0 ? lineSep : "");
            txtInfo += I18n.text("SIMULATION");
        }

        if (sys.containsData(SystemUtils.WEB_UPDATED_KEY)) {
            if (System.currentTimeMillis() - sys.retrieveDataTimeMillis(SystemUtils.WEB_UPDATED_KEY) < DateTimeUtil.MINUTE) {
                txtInfo += (txtInfo.length() != 0 ? lineSep : "");
                txtInfo += I18n.text("WEB POS RECEIVED");
            }
        }

        PlanType plan = sys.getActivePlan();
        if (plan != null) {
            txtInfo += (txtInfo.length() != 0 ? lineSep : "");
            txtInfo += I18n.text("Plan") + ": " + StringUtils.wrapEveryNChars(plan.getId(), (short) 30, 30, true);
        }

        if (extendedOrReduced && sys.isOnErrorState()) {
            txtInfo += (txtInfo.length() != 0 ? lineSep : "");
            txtInfo += I18n.text("Errors") +": "
                    + StringUtils.wrapEveryNChars(sys.getOnErrorStateStr(), (short) 30, 30, true);
        }

        SystemImcMsgCommInfo commS = ImcMsgManager.getManager().getCommInfoById(sys.getId());
        long deltaMillis = System.currentTimeMillis() - (long) commS.getArrivalTimeMillisLastMsg();
        if (deltaMillis > 10000) {
            txtInfo += (txtInfo.length() != 0 ? lineSep + "" : "");
            txtInfo += I18n.textf("%deltaTime with no messages",
                    DateTimeUtil.milliSecondsToFormatedString(deltaMillis, true));
        }
        
        // Update Loc info
        LocationType loc = sys.getLocation();
        if (loc != null && !loc.isLocationEqual(LocationType.ABSOLUTE_ZERO)) {
            loc = loc.convertToAbsoluteLatLonDepth();
            txtInfo += (txtInfo.length() != 0 ? lineSep + "" : "");
            txtInfo += I18n.text("Lat") +": "
                    + CoordinateUtil.latitudeAsPrettyString(MathMiscUtils.round(loc.getLatitudeDegs(), 6))
                    + " "
                    + I18n.text("Lon") +": "
                    + CoordinateUtil.longitudeAsPrettyString(MathMiscUtils.round(loc.getLongitudeDegs(), 6))
                    + (showHeight ? lineSep + I18n.text("Height") +": " + MathMiscUtils.round(loc.getHeight(), 1) + " m"
                            : "");
        }

        if (sys.containsData(SystemUtils.COURSE_DEGS_KEY) || sys.containsData(SystemUtils.HEADING_DEGS_KEY)) {
            boolean hasCourse = sys.containsData(SystemUtils.COURSE_DEGS_KEY, maxAgeTimeMillis);
            boolean hasHeading = sys.containsData(SystemUtils.HEADING_DEGS_KEY, maxAgeTimeMillis);

            txtInfo += (txtInfo.length() != 0 ? lineSep : "");
            if (hasCourse)
                txtInfo += I18n.text("Course") +": " // ImcSystem.COURSE_KEY + ": "
                        + CoordinateUtil.heading3DigitsFormat.format(sys.retrieveData(SystemUtils.COURSE_DEGS_KEY))
                        + CoordinateUtil.CHAR_DEGREE;
            if (hasHeading) {
                txtInfo += (hasCourse ? " | " : "");
                txtInfo += I18n.text("Heading") +": " // ImcSystem.HEADING_KEY + ": "
                        + CoordinateUtil.heading3DigitsFormat.format(sys.retrieveData(SystemUtils.HEADING_DEGS_KEY))
                        + CoordinateUtil.CHAR_DEGREE;
            }
        }

        if (sys.containsData(SystemUtils.GROUND_SPEED_KEY, maxAgeTimeMillis)) {
            double speedMs = (Double) sys.retrieveData(SystemUtils.GROUND_SPEED_KEY);
            SpeedType speedType = new SpeedType(speedMs, Units.MPS);
            speedType.convertToDefaultUnits();
            txtInfo += (txtInfo.length() != 0 ? lineSep : "") + I18n.text("Ground Speed") + ": " + speedType;
        }

        if (sys.containsData(SystemUtils.VERTICAL_SPEED_KEY, maxAgeTimeMillis)) {
            double speedMs = (Double) sys.retrieveData(SystemUtils.VERTICAL_SPEED_KEY);
            SpeedType speedType = new SpeedType(speedMs, Units.MPS);
            speedType.convertToDefaultUnits();
            txtInfo += (txtInfo.length() != 0 ? lineSep : "") + I18n.text("Vertical Speed") + ": " + speedType;
        }

        if (sys.containsData(SystemUtils.RPM_MAP_ENTITY_KEY, maxAgeTimeMillis)) {
            String rpms = "" + sys.retrieveData(SystemUtils.RPM_MAP_ENTITY_KEY);
            Matcher m = RPM_PATTERN_SEARCH.matcher(rpms);
            if (m.find())
                rpms = m.group(1);
            rpms = rpms.replace(",", lineSep + (htmlFragmentOrSimpleText ? "&nbsp;&nbsp;&nbsp;&nbsp;" : "    "));
            txtInfo += (txtInfo.length() != 0 ? lineSep : "") + I18n.text("RPM") +": " // ImcSystem.RPM_MAP_ENTITY_KEY + ": "
                    + rpms;
        }

        if (extendedOrReduced) {
            if (sys.getId() != ImcId16.NULL_ID && sys.getId() != ImcId16.ANNOUNCE
                    && sys.getId() != ImcId16.BROADCAST_ID) {
                txtInfo += (txtInfo.length() != 0 ? lineSep : "") + I18n.text("IMC ID") +": "
                        + sys.getId().toPrettyString();
            }
            if (sys.getHostAddress() != null && !"".equals(sys.getHostAddress())) {
                txtInfo += (txtInfo.length() != 0 ? lineSep : "") + I18n.text("IP") +": " + sys.getHostAddress();
                if (sys.isUDPOn() && sys.getRemoteUDPPort() != 0)
                    txtInfo += "@udp:" + sys.getRemoteUDPPort();
                if (sys.isTCPOn() && sys.getRemoteTCPPort() != 0)
                    txtInfo += "@tcp:" + sys.getRemoteTCPPort();
            }
        }
        return txtInfo;
    }

    // ------------- StateRendererInteraction ------------------------------------------------------

    /**
     * @return
     */
    private StateRendererInteraction getInteractionInterface() {
        if (stateRendererInteraction == null) {
            stateRendererInteraction = new InteractionAdapter(getConsole()) {
                
                @Override
                public Image getIconImage() {
                    try {
                        return ImageUtils.getIcon(PluginUtils.getPluginIcon(SystemsList.this.getClass())).getImage();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        return super.getIconImage();
                    }
                }

                @Override
                public boolean isExclusive() {
                    return false;
                }

                @Override
                public void mouseMoved(MouseEvent event, StateRenderer2D source) {
                    super.mouseMoved(event, source);
                    NeptusLog.pub().info("<###>mouse: " + event.getX() + ":" + event.getY() + " -> " + source);
                }
            };
        }
        return stateRendererInteraction;
    }

    // ------------- Paint -------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D,
     * pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

        if (!(viewInfoOSDSwitch.isSelected() || viewExtendedOSDSwitch.isSelected()) && !viewIconsSwitch.isSelected()) {
            return;
        }

        Component[] comps = holder.getComponents();
        List<SystemDisplay> systemsList = new Vector<SystemDisplay>();
        for (Component c : comps) {
            try {
                systemsList.add((SystemDisplay) c);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (viewInfoOSDSwitch.isSelected() || viewExtendedOSDSwitch.isSelected()) {
            boolean showExtendedOSD = false;
            if (viewExtendedOSDSwitch.isSelected())
                showExtendedOSD = true;

            Graphics2D g2;

            int fontSize = 12;
            int boxBorderSize = 2;
            int colorTransparency = 100;
            int iconColorTransparency = 220;
            int iconSize = 20;
            int contentLineSpacer = 1;
            Font fontTxt = new Font("Arial", 0, fontSize);

            if (printPaintDebug)
                NeptusLog.pub().info("<###> ><<><><><><>< ");

            // Now let's collect data for the balloon
            LinkedHashMap<SystemDisplay, RenderPainterSystemData> painterData = new LinkedHashMap<SystemDisplay, SystemsList.RenderPainterSystemData>();
            for (SystemDisplay sd : systemsList) {
                g2 = (Graphics2D) g.create(); // mainly use to getFontMetrics, we don't paint in this cycle
                ImcSystem sys = ImcSystemsHolder.lookupSystemByName(sd.getId());
                if (sys == null)
                    continue;

                // Collector for the information to show on the OSD balloon on the render
                collectSystemDataAndPositionToDrawInfoBalloon(renderer, g2, sys, sd, showExtendedOSD, painterData,
                        fontTxt, iconSize, contentLineSpacer, boxBorderSize);

                g2.dispose();
            }
            // To put the order of paint so the "top" system to paint last on top of all others
            Collections.reverse(systemsList);
            // Now let's paint the balloon
            for (SystemDisplay sd : systemsList) {
                g2 = (Graphics2D) g.create();
                ImcSystem sys = ImcSystemsHolder.lookupSystemByName(sd.getId());
                if (sys == null)
                    continue;

                RenderPainterSystemData rpd = painterData.get(sd);
                if (rpd == null)
                    continue;

                drawInfoBalloonForSystem(g2, sys, sd, rpd, fontTxt, colorTransparency, iconColorTransparency,
                        contentLineSpacer, iconSize);

                g2.dispose();
            }
        }

        // To paint the systems icons on the render
        if (viewIconsSwitch.isSelected()) {
            if (showExternalSystemsIcons) {
                // Draw in render the systems NOT on the list
                ExternalSystem[] externalSystems = ExternalSystemsHolder.lookupAllSystems();
                drawInRenderExternalSystemList(renderer, (Graphics2D) g.create(), Arrays.asList(externalSystems));
            }

            if (showSystemsIconsThatAreFilteredOut) {
                // Draw in render the systems NOT on the list
                ImcSystem[] allSystems = ImcSystemsHolder.lookupAllSystems();
                List<ImcSystem> imcSystemsNotOnList = new Vector<ImcSystem>();
                for (ImcSystem sys : allSystems) {
                    if (!systems.containsKey(sys))
                        imcSystemsNotOnList.add(sys);
                }
                drawInRenderImcSystemList(renderer, (Graphics2D) g.create(), imcSystemsNotOnList);
            }

            // Draw in render the systems on the list
            drawInRenderSystemDisplayList(renderer, (Graphics2D) g.create(), systemsList);
        }
    }

    // ------------- RenderPainterSystemData -------------------------------------------------------

    private class RenderPainterSystemData {
        Point2D pt;
        String sysName;
        Rectangle2D boundsSysName;

        boolean showSection3;
        String[] section3Data;
        Integer[][] boundsSection3DataLines;
        int section3ContentLines;

        boolean showSection4;
        String[] section4Data;
        Integer[][] boundsSection4DataLines;
        int section4ContentLines;

        int offsetX = 20;
        int offsetY = -20;

        int numberOfIcons = 8;

        int section1Width;
        int section1Height;
        int section2Width;
        int section2Height;
        int section3Width;
        int section3Height;
        int section4Width;
        int section4Height;

        double boxContentW;
        double boxContentH;
        double boxW;
        double boxH;

        double boxContentXOffset;
        double boxContentYOffset;
        double boxXOffset;
        double boxYOffset;

        RoundRectangle2D boxShape;
        Polygon pointerShape;
    }

    private void collectSystemDataAndPositionToDrawInfoBalloon(StateRenderer2D renderer, Graphics2D g, ImcSystem sys,
            SystemDisplay sd, boolean showExtendedOSD,
            LinkedHashMap<SystemDisplay, RenderPainterSystemData> painterData, Font fontTxt, int iconSize,
            int contentLineSpacer, int boxBorderSize) {

        if (printPaintDebug)
            NeptusLog.pub().info("<###> > " + sys.getName());

        Graphics2D g2 = (Graphics2D) g.create(); // mainly use to getFontMetrics, we don't paint in this cycle

        // Collector for the information to show on the OSD balloon on the render
        RenderPainterSystemData rpd = new RenderPainterSystemData();

        rpd.pt = renderer.getScreenPosition(sys.getLocation());

        g2.setFont(fontTxt);

        rpd.sysName = sys.getName();
        rpd.boundsSysName = g2.getFontMetrics().getStringBounds(rpd.sysName, g2);

        rpd.section3Data = getSection3Data(sys).split("\n");
        rpd.boundsSection3DataLines = new Integer[rpd.section3Data.length][2];
        for (int i = 0; i < rpd.section3Data.length; i++) {
            Rectangle2D boundsT = g2.getFontMetrics().getStringBounds(rpd.section3Data[i], g2);
            rpd.boundsSection3DataLines[i][0] = (int) boundsT.getWidth();
            rpd.boundsSection3DataLines[i][1] = (int) boundsT.getHeight();
        }
        rpd.section3ContentLines = 2 + (rpd.section3Data.length > 0 ? rpd.section3Data.length : 0);
        rpd.showSection3 = (rpd.section3Data.length > 0 ? true : false);

        rpd.section4Data = getSection4Data(sys, false, false, false).split("\n");
        rpd.boundsSection4DataLines = new Integer[rpd.section4Data.length][2];
        for (int i = 0; i < rpd.section4Data.length; i++) {
            Rectangle2D boundsT = g2.getFontMetrics().getStringBounds(rpd.section4Data[i], g2);
            rpd.boundsSection4DataLines[i][0] = (int) boundsT.getWidth();
            rpd.boundsSection4DataLines[i][1] = (int) boundsT.getHeight();
        }
        rpd.section4ContentLines = 2 + (rpd.section4Data.length > 0 ? rpd.section4Data.length : 0);
        rpd.showSection4 = showExtendedOSD && (rpd.section4Data.length > 0 ? true : false);

        rpd.numberOfIcons = 7;

        rpd.section1Width = (int) rpd.boundsSysName.getWidth();
        rpd.section1Height = (int) rpd.boundsSysName.getHeight();
        rpd.section2Width = iconSize * rpd.numberOfIcons;
        rpd.section2Height = iconSize;
        rpd.section3Width = 0;
        rpd.section3Height = 0;
        if (rpd.showSection3) {
            for (int i = 0; i < rpd.section3Data.length; i++) {
                rpd.section3Width = Math.max(rpd.section3Width, rpd.boundsSection3DataLines[i][0]);
                rpd.section3Height += rpd.boundsSection3DataLines[i][1];
            }
            if (rpd.section3Data.length > 1)
                rpd.section3Height += (rpd.section3Data.length - 1) * contentLineSpacer;
        }
        rpd.section4Width = 0;
        rpd.section4Height = 0;
        if (rpd.showSection4) {
            for (int i = 0; i < rpd.section4Data.length; i++) {
                rpd.section4Width = Math.max(rpd.section4Width, rpd.boundsSection4DataLines[i][0]);
                rpd.section4Height += rpd.boundsSection4DataLines[i][1];
            }
            if (rpd.section4Data.length > 1)
                rpd.section4Height += (rpd.section4Data.length - 1) * contentLineSpacer;
        }

        rpd.boxContentW = Math.max(rpd.section1Width,
                Math.max(rpd.section2Width, Math.max(rpd.section3Width, rpd.section4Width)));
        rpd.boxContentH = rpd.section1Height
                + rpd.section2Height
                + (rpd.showSection3 ? rpd.section3Height + contentLineSpacer
                        * (Math.max(rpd.section3ContentLines - 1, 0)) : 0)
                + (rpd.showSection4 ? rpd.section4Height + contentLineSpacer
                        * (Math.max(rpd.section4ContentLines - 1, 0)) : 0);
        rpd.boxW = boxBorderSize * 2 + rpd.boxContentW;
        rpd.boxH = boxBorderSize * 2 + rpd.boxContentH;

        rpd.offsetX = 20;
        rpd.offsetY = -20;

        // Test if other systems in the area and find empty space to put the balloon
        Vector<RenderPainterSystemData> closeBad = new Vector<RenderPainterSystemData>();
        double maxDist = Math.sqrt((rpd.offsetX * rpd.offsetX + rpd.boxW * rpd.boxW)
                + (rpd.offsetX * rpd.offsetX + rpd.boxH * rpd.boxH));
        Rectangle2D boundingBox = new Rectangle2D.Double(rpd.pt.getX() - maxDist, rpd.pt.getY() - maxDist, maxDist * 2,
                maxDist * 2);
        for (RenderPainterSystemData pd : painterData.values()) {
            Rectangle2D bBox = new Rectangle2D.Double(pd.pt.getX() + pd.boxXOffset, pd.pt.getY() + pd.boxYOffset,
                    pd.boxW, pd.boxH);
            if (boundingBox.intersects(bBox))
                closeBad.add(pd);
        }
        if (printPaintDebug)
            NeptusLog.pub().info("<###>>> " + closeBad.size());
        for (;;) { // To allow multiple tries to find empty space to put the balloon
            // Temporary calculation with temporary offsets (recalculated bellow again)
            rpd.boxContentXOffset = rpd.offsetX + (Math.signum(rpd.offsetX) < 0 ? -rpd.boxContentW : 0);
            rpd.boxContentYOffset = rpd.offsetY + (Math.signum(rpd.offsetY) < 0 ? -rpd.boxContentH : 0);
            rpd.boxXOffset = rpd.boxContentXOffset - boxBorderSize;
            rpd.boxYOffset = rpd.boxContentYOffset - boxBorderSize;

            Rectangle2D placeBBox = new Rectangle2D.Double(rpd.pt.getX() + rpd.boxXOffset, rpd.pt.getY()
                    + rpd.boxYOffset, rpd.boxW, rpd.boxH);

            if (printPaintDebug)
                NeptusLog.pub().info("<###>>>>   " + placeBBox);
            boolean freeSpaceFound = true;
            for (RenderPainterSystemData pd : closeBad) {
                Rectangle2D bBox = new Rectangle2D.Double(pd.pt.getX() + pd.boxXOffset, pd.pt.getY() + pd.boxYOffset,
                        pd.boxW, pd.boxH);
                if (placeBBox.intersects(bBox)) {
                    freeSpaceFound = false;
                    if (printPaintDebug)
                        NeptusLog.pub().info("<###>>>>    " + pd.sysName + " > NOT FREE  " + bBox);
                    break;
                }
                if (printPaintDebug)
                    NeptusLog.pub().info("<###>>>>    " + pd.sysName + " > FREE  " + bBox);
            }
            if (freeSpaceFound) {
                break;
            }
            else {
                freeSpaceFound = true;
                if (rpd.offsetX == 20 && rpd.offsetY == -20) {
                    rpd.offsetX = 20;
                    rpd.offsetY = 20;
                }
                else if (rpd.offsetX == 20 && rpd.offsetY == 20) {
                    rpd.offsetX = -20;
                    rpd.offsetY = 20;
                }
                else if (rpd.offsetX == -20 && rpd.offsetY == 20) {
                    rpd.offsetX = -20;
                    rpd.offsetY = -20;
                }
                else {// if (rpd.offsetX == -20 && rpd.offsetY == -20) {
                    // rpd.offsetX = 20;
                    // rpd.offsetY = -20;
                    break;
                }
            }
        }

        rpd.boxContentXOffset = rpd.offsetX + (Math.signum(rpd.offsetX) < 0 ? -rpd.boxContentW : 0);
        rpd.boxContentYOffset = rpd.offsetY + (Math.signum(rpd.offsetY) < 0 ? -rpd.boxContentH : 0);
        rpd.boxXOffset = rpd.boxContentXOffset - boxBorderSize;
        rpd.boxYOffset = rpd.boxContentYOffset - boxBorderSize;

        rpd.boxShape = new RoundRectangle2D.Double(rpd.boxXOffset, rpd.boxYOffset, rpd.boxW, rpd.boxH, 5, 5);
        rpd.pointerShape = new Polygon(new int[] { 0, rpd.offsetX, rpd.offsetX + (int) Math.signum(rpd.offsetX) * 20 },
                new int[] { 0, rpd.offsetY + -(int) Math.signum(rpd.offsetY) * boxBorderSize,
                        rpd.offsetY + -(int) Math.signum(rpd.offsetY) * boxBorderSize }, 3);

        painterData.put(sd, rpd);

        g2.dispose();
    }

    private void drawInfoBalloonForSystem(Graphics2D g, ImcSystem sys, SystemDisplay sd, RenderPainterSystemData rpd,
            Font fontTxt, int colorTransparency, int iconColorTransparency, int contentLineSpacer, int iconSize) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.translate(rpd.pt.getX(), rpd.pt.getY());

        g2.setFont(fontTxt);

        g2.setColor(ColorUtils.setTransparencyToColor(Color.BLACK, colorTransparency));
        g2.fill(rpd.pointerShape);
        g2.fill(rpd.boxShape);
        if (sd.isMainVehicle()) {
            g2.setStroke(new BasicStroke(4));
            g2.setColor(ColorUtils.setTransparencyToColor(Color.GREEN, colorTransparency));
            g2.draw(rpd.boxShape);
            g2.setStroke(new BasicStroke(1));
        }

        g2.setColor(ColorUtils.setTransparencyToColor(Color.BLACK, colorTransparency));
        g2.fillOval(-5, -5, 10, 10);

        Color activeColor = Color.GREEN;
        Color errorColor = Color.ORANGE;

        if (!sd.isWithAuthority() || sys.getType() != SystemTypeEnum.VEHICLE) {
            activeColor = errorColor = Color.WHITE;
        }

        g2.translate(rpd.boxContentXOffset, rpd.boxContentYOffset);

        g2.setColor(ColorUtils.setTransparencyToColor(activeColor, iconColorTransparency));

        g2.drawString(rpd.sysName, 0, (int) rpd.boundsSysName.getHeight());

        g2.translate(0, contentLineSpacer + rpd.section1Height);
        Graphics2D gtemp = (Graphics2D) g2.create(); // To restore the graphics bellow the paint of the symbols

        sts.setSystemType(sd.getSystemType());
        sts.setShowSymbolOrText(showSystemSymbolOrText);
        sts.setColor(ColorUtils.setTransparencyToColor(activeColor, iconColorTransparency));
        sts.paint(g2, sts, iconSize, iconSize);

        g2.translate(iconSize, 0);
        cns.setActive(sd.isActive());
        cns.setStrength(sd.getConnectionStrength());
        cns.setActiveAnnounce(sd.isAnnounceReceived());
        cns.setColor(ColorUtils.setTransparencyToColor(sd.isActive() ? activeColor : errorColor, iconColorTransparency));
        cns.paint(g2, cns, iconSize, iconSize);

        g2.translate(iconSize, 0);
        locs.setActive(sd.isLocationKnown());
        locs.setColor(ColorUtils.setTransparencyToColor(sd.isLocationKnown() ? activeColor : errorColor,
                iconColorTransparency));
        locs.paint(g2, locs, iconSize, iconSize);

        // g2.translate(iconSize, 0);
        // mvs.setActive(sd.isMainVehicle());
        // mvs.setColor(ColorUtils.setTransparencyToColor(sd.isMainVehicle() ? activeColor : errorColor,
        // iconColorTransparency));
        // mvs.paint(g2, mvs, iconSize, iconSize);

        g2.translate(iconSize, 0);
        ats.setActive(sd.isWithAuthority());
        ats.setAuthorityType(sd.getWithAuthority());
        ats.setColor(ColorUtils.setTransparencyToColor(sd.isWithAuthority() ? activeColor : errorColor,
                iconColorTransparency));
        ats.paint(g2, ats, iconSize, iconSize);

        g2.translate(iconSize, 0);
        tks.setActive(sd.isTaskAlocated());
        tks.setColor(ColorUtils.setTransparencyToColor(sd.isTaskAlocated() ? activeColor : errorColor,
                iconColorTransparency));
        tks.paint(g2, tks, iconSize, iconSize);

        g2.translate(iconSize, 0);
        as.setActive(sd.isAttentionAlert());
        as.setColor(ColorUtils.setTransparencyToColor(!sd.isAttentionAlert() ? activeColor : errorColor,
                iconColorTransparency));
        as.paint(g2, as, iconSize, iconSize);

        g2.translate(iconSize, 0);
        fl.setActive(sd.isFuelLevel());
        fl.setPercentage(sd.getFuelLevelPercentage());
        fl.setColor(ColorUtils.setTransparencyToColor(sd.getFuelLevelPercentage() >= 0.1 ? activeColor : errorColor,
                iconColorTransparency));
        fl.paint(g2, fl, iconSize, iconSize);
        
        g2.translate(iconSize, 0);
        es.setActive(sd.isEmergencyTaskAlocated());
        es.setColor(ColorUtils.setTransparencyToColor(sd.isEmergencyTaskAlocated() ? activeColor : errorColor,
                iconColorTransparency));
        es.paint(g2, es, iconSize, iconSize);

        g2.dispose();
        g2 = gtemp;

        if (rpd.showSection3) {
            g2.translate(0, contentLineSpacer + rpd.section2Height);
            for (int i = 0; i < rpd.section3Data.length; i++) {
                if (i != 0)
                    g2.translate(0, contentLineSpacer + rpd.boundsSection3DataLines[i - 1][1]);
                g2.drawString(rpd.section3Data[i], 0, rpd.boundsSection3DataLines[i][1]);
            }
        }

        if (rpd.showSection4) {
            g2.translate(0, contentLineSpacer + rpd.section3Height);
            for (int i = 0; i < rpd.section4Data.length; i++) {
                if (i != 0)
                    g2.translate(0, contentLineSpacer + rpd.boundsSection4DataLines[i - 1][1]);
                g2.drawString(rpd.section4Data[i], 0, rpd.boundsSection4DataLines[i][1]);
            }
        }

        g2.dispose();
    }

    // ------------- Draw System in Renderer -------------------------------------------------------

    private void drawInRenderSystemDisplayList(StateRenderer2D renderer, Graphics2D g, List<SystemDisplay> systemsList) {
        Graphics2D g2;
        for (SystemDisplay sd : systemsList) {
            g2 = (Graphics2D) g.create();

            // Search for the system
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(sd.getId());
            if (sys == null)
                continue;

            // Go to the center point
            Point2D pt = renderer.getScreenPosition(sys.getLocation());
            g2.translate(pt.getX(), pt.getY());

            // Choose main color
            Color color = Color.WHITE;
            if (sys.getVehicle() != null && sys.getVehicle().getIconColor() != null)
                color = sys.getVehicle().getIconColor();
            if (sd.isMainVehicle())
                color = new Color(0, 255, 64); // Color.GREEN.darker();

            drawImcSystem(renderer, g2, sys, color,
                    !viewInfoOSDSwitch.isSelected() && !viewExtendedOSDSwitch.isSelected());

            g2.dispose();
        }
    }

    private void drawInRenderImcSystemList(StateRenderer2D renderer, Graphics2D g, List<ImcSystem> systemsList) {
        Graphics2D g2;
        for (ImcSystem sys : systemsList) {
            g2 = (Graphics2D) g.create();

            // Go to the center point
            Point2D pt = renderer.getScreenPosition(sys.getLocation());
            g2.translate(pt.getX(), pt.getY());

            // Choose main color
            Color color = new Color(210, 176, 106); // KHAKI

            if (minutesToHideSystemsWithoutKnownLocation <= 0 || SystemPainterHelper.getLocationAge(sys.getLocation(),
                    sys.getLocationTimeMillis()) < DateTimeUtil.MINUTE * minutesToHideSystemsWithoutKnownLocation
                    || sys.isActive())
                drawImcSystem(renderer, g2, sys, color, true);

            g2.dispose();
        }
    }

    private void drawInRenderExternalSystemList(StateRenderer2D renderer, Graphics2D g, List<ExternalSystem> systemsList) {
        Graphics2D g2;
        for (ExternalSystem sys : systemsList) {
            g2 = (Graphics2D) g.create();

            // Go to the center point
            Point2D pt = renderer.getScreenPosition(sys.getLocation());
            g2.translate(pt.getX(), pt.getY());

            // Choose main color
            Color color = SystemPainterHelper.EXTERNAL_SYSTEM_COLOR;

            if (minutesToHideSystemsWithoutKnownLocation <= 0 || System.currentTimeMillis()
                    - sys.getLocationTimeMillis() < DateTimeUtil.MINUTE * minutesToHideSystemsWithoutKnownLocation) {
                try {
                    drawExternalSystem(renderer, g2, sys, color);
                }
                catch (Exception e) {
                    NeptusLog.pub().error(String.format("Error while drawing external system '%s'", sys), e);
                }
            }
            else {
                if (sys.getName().equalsIgnoreCase("Ship")) {
                    LocationType myLoc = sys.getLocation();
                    NeptusLog.pub().debug((String.format(">>>>>>>>> NOT Paint Ship >>>>>>> %s  :: %s :: %s",
                            CoordinateUtil.latitudeAsPrettyString(myLoc.getLatitudeDegs()), CoordinateUtil.longitudeAsPrettyString(myLoc.getLongitudeDegs()),
                            DateTimeUtil.dateTimeFormatterISO8601.format(new Date(sys.getLocationTimeMillis())))));
                }
            }

            g2.dispose();
        }
    }

    private void drawImcSystem(StateRenderer2D renderer, Graphics2D g, ImcSystem sys, Color color, boolean drawLabel) {
        Graphics2D g2 = (Graphics2D) g.create();
        int lod = renderer.getLevelOfDetail();
        double iconWidth = calculateSystemIconSize(rendererIconsSize, renderer.getLevelOfDetail()); // 20

        boolean isLocationKnownUpToDate = SystemPainterHelper.isLocationKnown(sys);

        // To draw a box with error state of the system
        if (!useMilStd2525LikeSymbols) {
            SystemPainterHelper.drawErrorStateForSystem(renderer, g2, sys, iconWidth, isLocationKnownUpToDate);
        }

        // To draw the system icon
        if (useMilStd2525LikeSymbols) {
            SystemPainterHelper.drawMilStd2525LikeSymbolForSystem(g2, sys, isLocationKnownUpToDate, false,
                    milStd2525FilledOrNot == MilStd2525SymbolsFilledEnum.FILLED ? true : false);
        }
        else {
            SystemPainterHelper.drawSystemIcon(renderer, g2, sys, color, iconWidth, isLocationKnownUpToDate);
        }

        // To draw the circle around the system position (if not using useMilStd2525LikeSymbols)
        if (!useMilStd2525LikeSymbols) {
            if (drawCircleInRenderDependentOfSystemType) { // Now the circle is not painted unless for type indicator
                CircleTypeBySystemType circleType = getCircleTypeForSystem(sys);
                SystemPainterHelper.drawCircleForSystem(g2, color, iconWidth, circleType, isLocationKnownUpToDate);
            }
        }

        // To paint the system name on the render
        if (lod >= LOD_MIN_TO_SHOW_LABEL && drawLabel && drawSystemLabel) {
            /* !viewInfoOSDSwitch.isSelected() && !viewExtendedOSDSwitch.isSelected() */
            SystemPainterHelper.drawSystemNameLabel(g2, sys.getName(), color, iconWidth, isLocationKnownUpToDate);
        }
        
        if (lod >= LOD_MIN_TO_SHOW_LABEL && drawSystemLocAge) {
            SystemPainterHelper.drawSystemLocationAge(g2, sys.getLocationTimeMillis(), color, iconWidth, isLocationKnownUpToDate);
        }

        // To draw the course/speed vector
        if (lod >= LOD_MIN_TO_SHOW_SPEED_VECTOR) {
            SystemPainterHelper.drawCourseSpeedVectorForSystem(renderer, g2, sys, iconWidth, isLocationKnownUpToDate,
                    minimumSpeedToBeStopped);
        }

        g2.dispose();
    }

    private double calculateSystemIconSize(int rendererIconsSize, int levelOfDetail) {
        if (!adaptIconsSizeWithZoom) {
            return rendererIconsSize;
        }

        if (levelOfDetail <= 9) {
            return rendererIconsSize * 0.1;
        }
        if (levelOfDetail <= 11) {
            return rendererIconsSize * 0.2;
        }
        if (levelOfDetail <= 13) {
            return rendererIconsSize * 0.4;
        }
        if (levelOfDetail <= 15) {
            return rendererIconsSize * 0.6;
        }
        return rendererIconsSize;
    }



    /**
     * @param sys
     * @return
     */
    private CircleTypeBySystemType getCircleTypeForSystem(ImcSystem sys) {
        CircleTypeBySystemType circleType = CircleTypeBySystemType.DEFAULT;
        SystemTypeEnum sysType = sys.getType();
        VehicleTypeEnum sysVehicleType = sys.getTypeVehicle();
        circleType = getCircleTypeWorker(sysType, sysVehicleType, null);
        return circleType;
    }

    private CircleTypeBySystemType getCircleTypeForSystem(ExternalSystem sys) {
        CircleTypeBySystemType circleType = CircleTypeBySystemType.DEFAULT;
        SystemTypeEnum sysType = sys.getType();
        VehicleTypeEnum sysVehicleType = sys.getTypeVehicle();
        ExternalTypeEnum sysExternalType = sys.getTypeExternal();
        circleType = getCircleTypeWorker(sysType, sysVehicleType, sysExternalType);
        return circleType;
    }

    /**
     * @param sysType
     * @param sysVehicleType
     * @param sysExternalType
     * @return
     */
    private CircleTypeBySystemType getCircleTypeWorker(SystemTypeEnum sysType, VehicleTypeEnum sysVehicleType,
            ExternalTypeEnum sysExternalType) {
        CircleTypeBySystemType circleType;
        switch (sysType) {
            case CCU:
                circleType = CircleTypeBySystemType.SURFACE_UNIT;
                break;
            case MOBILESENSOR:
            case STATICSENSOR:
                circleType = CircleTypeBySystemType.DEFAULT;
                break;
            case VEHICLE:
                switch (sysVehicleType) {
                    case UAV:
                        circleType = CircleTypeBySystemType.AIR;
                        break;
                    case UUV:
                        circleType = CircleTypeBySystemType.SUBSURFACE;
                        break;
                    case USV:
                    case UGV:
                        circleType = CircleTypeBySystemType.SURFACE;
                        break;
                    default:
                        circleType = CircleTypeBySystemType.DEFAULT;
                        break;
                }
                break;
            default:
                circleType = CircleTypeBySystemType.DEFAULT;
                break;
        }
        
        if (sysExternalType != null) {
            switch (sysExternalType) {
                case MANNED_AIRPLANE:
                    circleType = CircleTypeBySystemType.AIR;
                    break;
                case MANNED_CAR:
                case MANNED_SHIP:
                case PERSON:
                    circleType = CircleTypeBySystemType.SURFACE_UNIT;
                    break;
                case UNKNOWN:
                case ALL:
                    circleType = null;
                    break;
                case VEHICLE:
                case CCU:
                case MOBILESENSOR:
                case STATICSENSOR:
                default:
                    break;
            }
        }
        
        return circleType;
    }

    private void drawExternalSystem(StateRenderer2D renderer, Graphics2D g, ExternalSystem sys, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        int lod = renderer.getLevelOfDetail();
        double iconWidth = calculateSystemIconSize(rendererIconsSize, renderer.getLevelOfDetail()); // 20

        boolean isLocationKnownUpToDate = SystemPainterHelper.isLocationKnown(sys.getLocation(),
                sys.getLocationTimeMillis());
        
        if (sys.getName().equalsIgnoreCase("Ship")) {
            LocationType myLoc = sys.getLocation();
            NeptusLog.pub().debug((String.format(">>>>>>>>> Paint Ship >>>>>>> %s  :: %s :: %s",
                    CoordinateUtil.latitudeAsPrettyString(myLoc.getLatitudeDegs()), CoordinateUtil.longitudeAsPrettyString(myLoc.getLongitudeDegs()),
                    DateTimeUtil.dateTimeFormatterISO8601.format(new Date(sys.getLocationTimeMillis())))));
        }
        
        {
            Object obj = sys.retrieveData(SystemUtils.WIDTH_KEY);
            if (obj != null) {
                double width = ((Number) obj).doubleValue();
                obj = sys.retrieveData(SystemUtils.LENGHT_KEY);
                if (obj != null) {
                    double length = ((Number) obj).doubleValue();

                    double headingDegrees = sys.getYawDegrees();

                    double widthOffsetFromCenter = 0;
                    obj = sys.retrieveData(SystemUtils.WIDTH_CENTER_OFFSET_KEY);
                    if (obj != null)
                        widthOffsetFromCenter = ((Number) obj).doubleValue();
                    double lenghtOffsetFromCenter = 0;
                    obj = sys.retrieveData(SystemUtils.LENGHT_CENTER_OFFSET_KEY);
                    if (obj != null)
                        lenghtOffsetFromCenter = ((Number) obj).doubleValue();

                    SystemPainterHelper.drawVesselDimentionsIconForSystem(renderer, g2, width, length,
                            widthOffsetFromCenter, lenghtOffsetFromCenter, headingDegrees, color, false);
                }
            }
        }

        // To draw the system icon
        if (useMilStd2525LikeSymbols) {
            SymbolTypeEnum type = SymbolTypeEnum.SURFACE;
            SymbolShapeEnum shapeType = SymbolShapeEnum.UNFRAMED;
            SymbolOperationalConditionEnum operationalCondition = SymbolOperationalConditionEnum.NONE;
            SymbolIconEnum drawIcon = SymbolIconEnum.UNKNOWN;
            SystemPainterHelper.drawMilStd2525LikeSymbolForSystem(g2, type, shapeType, operationalCondition, drawIcon,
                    isLocationKnownUpToDate, false, milStd2525FilledOrNot == MilStd2525SymbolsFilledEnum.FILLED ? true : false);
        }
        else {
            SystemPainterHelper.drawSystemIcon(renderer, g2, sys.getYawDegrees(), color, iconWidth,
                    isLocationKnownUpToDate);
        }

         // To draw the circle around the system position (if not using useMilStd2525LikeSymbols)
         if (!useMilStd2525LikeSymbols) {
             if (drawCircleInRenderDependentOfSystemType) { // Now the circle is not painted unless for type indicator
                 CircleTypeBySystemType circleType = getCircleTypeForSystem(sys);
                 SystemPainterHelper.drawCircleForSystem(g2, color, iconWidth, circleType, isLocationKnownUpToDate);
             }
         }

        // // To draw a box with error state of the system
        // if (!useMilStd2525LikeSymbols) {
        // drawErrorStateForSystem(renderer, g2, sys, iconWidth, isLocationKnownUpToDate);
        // }

        // To paint the system name on the render
        if (lod >= (LOD_MIN_TO_SHOW_LABEL + LOD_MIN_OFFSET_FOR_EXTERNAL) && drawSystemLabel) {
            /* !viewInfoOSDSwitch.isSelected() && !viewExtendedOSDSwitch.isSelected() && */
            SystemPainterHelper.drawSystemNameLabel(g2, sys.getName(), color, iconWidth, isLocationKnownUpToDate);
        }
        
        if (lod >= (LOD_MIN_TO_SHOW_LABEL + LOD_MIN_OFFSET_FOR_EXTERNAL) && drawSystemLocAge) {
            SystemPainterHelper.drawSystemLocationAge(g2, sys.getLocationTimeMillis(), color, iconWidth, isLocationKnownUpToDate);
        }

        // To draw the course/speed vector
        Object obj = sys.retrieveData(SystemUtils.COURSE_DEGS_KEY);
        if (lod >= (LOD_MIN_TO_SHOW_SPEED_VECTOR + LOD_MIN_OFFSET_FOR_EXTERNAL) && obj != null) {
            double courseDegrees = ((Number) obj).doubleValue();
            obj = sys.retrieveData(SystemUtils.GROUND_SPEED_KEY);
            if (obj != null) {
                double gSpeed = ((Number) obj).doubleValue();
                SystemPainterHelper.drawCourseSpeedVectorForSystem(renderer, g2, courseDegrees, gSpeed, color, iconWidth,
                        isLocationKnownUpToDate, minimumSpeedToBeStopped);
            }
        }

        obj = sys.retrieveData(SystemUtils.DISTRESS_MSG_KEY, minutesToShowDistress * DateTimeUtil.MINUTE);
        if (obj != null) {
            Graphics2D g3 = (Graphics2D) g.create();
            g3.setStroke(new BasicStroke(3));
            g3.setColor(new Color(255, 155, 155, 255));
            int s = 40;
            g3.drawOval(-s / 2, -s / 2, s, s);
            s = 60;
            g3.drawOval(-s / 2, -s / 2, s, s);
            s = 80;
            g3.drawOval(-s / 2, -s / 2, s, s);
            g3.dispose();
        }

        g2.dispose();
    }

    // ------------- IEditorMenuExtension ----------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.planeditor.IEditorMenuExtension#getApplicableItems(pt.lsts.neptus.types.coord.LocationType
     * , pt.lsts.neptus.planeditor.IMapPopup)
     */
    @Override
    public Collection<JMenuItem> getApplicableItems(LocationType loc, IMapPopup source) {
        Vector<JMenuItem> menus = new Vector<JMenuItem>();

        JMenu mainMenu = new JMenu(I18n.text("Systems in renderer"));
        
        JCheckBoxMenuItem viewInfoBallonCheckMenu = new JCheckBoxMenuItem(new AbstractAction(
                (String) viewInfoOSDAction.getValue(Action.NAME), ICON_VIEW_INFO) {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewInfoOSDSwitch.doClick(50);
            }
        });
        viewInfoBallonCheckMenu.setSelected(viewInfoOSDSwitch.isSelected());

        JCheckBoxMenuItem viewExtendedBallonCheckMenu = new JCheckBoxMenuItem(new AbstractAction(
                (String) viewExtendedOSDAction.getValue(Action.NAME), ICON_VIEW_EXTRA) {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewExtendedOSDSwitch.doClick(50);
            }
        });
        viewExtendedBallonCheckMenu.setSelected(viewExtendedOSDSwitch.isSelected());

        mainMenu.add(viewInfoBallonCheckMenu);
        mainMenu.add(viewExtendedBallonCheckMenu);
        
        menus.add(mainMenu);
        return menus;
    }

    // ------------- ConfigurationListener ----------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        if (mainSizeIncrement < 0 || mainSizeIncrement > Math.min(iconsSize, indicatorsSize) / 2)
            mainSizeIncrement = 0;
        for (SystemDisplay sys : systems.values().toArray(new SystemDisplay[0])) {
            sys.setIconSize(iconsSize);
            sys.setIndicatorsSize(indicatorsSize);
            sys.setShowSystemSymbolOrText(showSystemSymbolOrText);
            sys.setEnableSelection(enableSelection);
            if (sys.isMainVehicle())
                changeMainVehicle(sys.getId());
        }

        if (rendererIconsSize < 20)
            rendererIconsSize = 20;
        else if (rendererIconsSize > 50)
            rendererIconsSize = 50;

        viewIconsSwitch.setSelected(showSystemsIconsOnRenderer);
        viewExternalSystemsSwitch.setSelected(showExternalSystemsIcons);

        clearSelection.setEnabled(enableSelection);
        redoSelection.setEnabled(enableSelection);
        // extendedOSDSwitch.setSelected(showExtendedOSD);

        comparator.setOrderOption(orderingOption);

        updateMainVehicle = true;
        updateOrdering = true;
    }

    // ------------- SystemsSelection --------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.ISystemsSelection#getSelectedSystems(boolean)
     */
    @Override
    public Collection<String> getSelectedSystems(boolean clearSelection) {
        Vector<String> sel = new Vector<String>();
        for (SystemDisplay disp : systems.values().toArray(new SystemDisplay[0]))
            if (disp.isSelected())
                sel.add(disp.getId());
        // new
        if (clearSelection)
            clearSelectedSystems();
        return sel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.ISystemsSelection#getAvailableSelectedSystems()
     */
    @Override
    public Collection<String> getAvailableSelectedSystems() {
        Vector<String> sel = new Vector<String>();
        for (SystemDisplay disp : systems.values().toArray(new SystemDisplay[0]))
            sel.add(disp.getId());
        return sel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.ISystemsSelection#getSelectedVehicles(boolean)
     */
    @Override
    public Collection<String> getSelectedVehicles(boolean clearSelection) {
        Vector<String> sel = new Vector<String>();
        for (SystemDisplay disp : systems.values().toArray(new SystemDisplay[0]))
            if (disp.isSelected()) {
                VehicleType v = VehiclesHolder.getVehicleById(disp.getId());
                if (v != null)
                    sel.add(disp.getId());
            }
        // new
        if (clearSelection)
            clearSelectedSystems();

        return sel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.ISystemsSelection#getAvailableVehicles()
     */
    @Override
    public Collection<String> getAvailableVehicles() {
        Vector<String> sel = new Vector<String>();
        for (SystemDisplay disp : systems.values().toArray(new SystemDisplay[0])) {
            VehicleType v = VehiclesHolder.getVehicleById(disp.getId());
            if (v != null)
                sel.add(disp.getId());
        }

        return sel;
    }

    /**
     * 
     */
    public void clearSelectedSystems() {
        synchronized (prevSelection) {
            Vector<SystemDisplay> tmp = new Vector<SystemDisplay>();
            for (SystemDisplay disp : systems.values().toArray(new SystemDisplay[0])) {
                if (disp.isSelected()) {
                    disp.setSelected(false);
                    tmp.add(disp);
                }
            }
            if (tmp.size() > 0) {
                prevSelection.clear();
                prevSelection.addAll(tmp);
                tmp = null;
            }
        }
    }

    /**
     * 
     */
    private void redoSelectedSystems() {
        synchronized (prevSelection) {
            if (prevSelection.size() > 0) {
                for (SystemDisplay disp : prevSelection) {
                    if (!disp.isSelected())
                        disp.setSelected(true);
                }
            }
        }
    }

    // ------------- Main --------------------------------------------------------------------------

    /**
     * @param args
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        ConfigFetch.initialize();

        boolean test1Enable = false;
        boolean test2Enable = true;
        boolean test3Enable = false;
        
        
        if (test1Enable) {
            ConsoleParse.consoleLayoutLoader("conf/consoles/seacon-light.ncon");
        }

        if (test2Enable) {
            JFrame frame = GuiUtils.testFrame(new MonitorIMCComms(ImcMsgManager.getManager()), "Monitor IMC Comms");
            frame.setIconImage(MonitorIMCComms.ICON_ON.getImage());
            frame.setSize(396, 411 + 55);

            SystemDisplay sys1 = new SystemDisplay("lauv-xtreme-2");
            SystemDisplay sys2 = new SystemDisplay("lauv-seacon-1");
            SystemDisplay sys3 = new SystemDisplay("lauv-seacon-2");

            final SystemsList sList = new SystemsList(null);
            new Thread() {
                @Override
                public void run() {
                    for (;;) {
                        sList.update();
                        try {
                            Thread.sleep(500);
                        }
                        catch (Exception e) {
                        }
                    }
                }
            }.start();

            // sList.holder.add(sys1);
            // sList.holder.add(sys2);
            // sList.holder.add(sys3);

            sys2.setActive(true);
            Image image = new ImageIcon("vehicles-files\\lauv\\conf\\images\\lauv-seacon0-presentation.png").getImage();
            sys1.setSystemImage(image);

            image = new ImageIcon("vehicles-files\\lauv\\conf\\images\\lauv-seacon0-presentation.png").getImage();
            sys2.setSystemImage(image);

            // sys2.setIconSize(100);
            // sys2.setIndicatorsSize(70);

            // GuiUtils.testFrame(sList);
            ConsoleParse.dummyConsole(new ConsolePanel[] { sList });

            try {
                Thread.sleep(5000);
            }
            catch (Exception e) {
            }
            // sys1.setIconSize(100);
            // sys1.setIndicatorsSize(70);
            // sys2.setIconSize(100);
            // sys2.setIndicatorsSize(70);
            // sys3.setIconSize(100);
            // sys3.setIndicatorsSize(70);

            // sys1.setActive(true);
            //
            // sys2.setAttentionAlert(true);
            // try {Thread.sleep(5000);} catch (Exception e) {}
            // sys1.setWithAuthority(true);
            // sys2.setMainVehicle(true);
            // try {Thread.sleep(5000);} catch (Exception e) {}
            // sys1.setTaskAlocated(true);
            // sys2.setTaskAlocated(true);
            // try {Thread.sleep(5000);} catch (Exception e) {}
            // sys3.setActive(true);
            // sys1.setTaskAlocated(false);
            // try {Thread.sleep(5000);} catch (Exception e) {}
            // sys2.setWithAuthority(true);
            // try {Thread.sleep(5000);} catch (Exception e) {}
            // sys2.setActive(false);
            // try {Thread.sleep(5000);} catch (Exception e) {}
            // sys1.setActive(false);

            VehiclesHolder.loadVehicles();
            ImcMsgManager.getManager().start();
            ImcMsgManager.getManager().initVehicleCommInfo("lauv-xtreme-2", "");
            ImcMsgManager.getManager().initVehicleCommInfo("lauv-seacon-3", "");
            ImcMsgManager.getManager().initVehicleCommInfo("lauv-seacon-1", "");
            ImcMsgManager.getManager().initVehicleCommInfo("lauv-seacon-2", "");
            sList.systemsFilter = SystemTypeEnum.ALL;
            // new Thread() {
            // @Override
            // public void run() {
            // for (;;) {
            // sList.update();
            // try {Thread.sleep(500);} catch (Exception e) {}
            // }
            // }
            // }.start();
        }

        if (test3Enable) {
            final StateRenderer2D sr = new StateRenderer2D();
            final ImcSystem sys = new ImcSystem(new ImcId16(0x18));
            JLabel lb = new JLabel() {
                @Override
                public void paint(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int w = this.getWidth();
                    int h = this.getHeight();
                    g2.translate(w / 2, h / 2);

                    int iconWidth = 130;
                    sys.setOnErrorState(true);
                    SystemPainterHelper.drawErrorStateForSystem(sr, g2, sys, iconWidth, true);

                    sys.setAttitudeDegrees(60);
                    SystemPainterHelper.drawSystemIcon(sr, g2, sys, Color.GREEN, iconWidth, true);
                    SystemPainterHelper.drawCircleForSystem(g2, Color.GREEN, iconWidth, CircleTypeBySystemType.AIR,
                            true);
                }
            };
            GuiUtils.testFrame(lb, "test", 300, 300);
        }

    }
}
