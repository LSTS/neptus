/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 20/02/2011
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.RectanglePainter;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent;
import pt.lsts.neptus.console.plugins.SubPanelChangeListener;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.gui.ToolbarSwitch;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.MultiSystemIMCMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.StringUtils;
import pt.lsts.neptus.util.conf.StringProperty;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(name = "UAV Piccolo Control", author = "Paulo Dias", version="1.0.0")
@LayerPriority(priority=40)
public class UavPiccoloControl extends ConsolePanel implements MainVehicleChangeListener,
        ConfigurationListener, IPeriodicUpdates, Renderer2DPainter, SubPanelChangeListener {

    private final Icon ICON_PICCOLO_WP_DOWNLOAD = ImageUtils.getScaledIcon(
            "images/planning/piccolo-wps.png", 24, 24);
    private final Icon ICON_PICCOLO_WP_GOTO = ImageUtils.getScaledIcon(
            "images/planning/piccolo-goto.png", 24, 24);
    private final Icon ICON_PICCOLO_WP_GOTO_HO = ImageUtils.getScaledIcon(
            "images/planning/piccolo-goto-ho.png", 24, 24);
    private final Icon ICON_PICCOLO_WP_SHOW = ImageUtils.getScaledIcon(
            "images/planning/piccolo-wp-show.png", 24, 24);
//    private static final Icon ICON_PICCOLO_WP_HIDE = ImageUtils.getScaledIcon(
//            "images/planning/piccolo-wp-hide.png", 24, 24);
    private final Icon ICON_PICCOLO_WP_SHOW_FILTER = ImageUtils.getScaledIcon(
            "images/planning/piccolo-wp-filter.png", 24, 24);
    private final Icon ICON_PICCOLO_WP_SHOW_CONFIG = ImageUtils.getScaledIcon(
            "images/planning/piccolo-wp-filter-config.png", 24, 24);
    private final Icon ICON_CONFIG_DOWNLOAD = ImageUtils.getScaledIcon(
            "images/planning/config-download.png", 24, 24);
//    private static final Icon ICON_CONFIG_UPLOAD = ImageUtils.getScaledIcon(
//            "images/planning/config-upload.png", 24, 24);
    private final Icon ICON_EDIT = ImageUtils.getScaledIcon(
            "images/planning/edit2.png", 24, 24);

    private static final Color COLOR_IDLE = Color.LIGHT_GRAY;
    private static final Color COLOR_ACCEPT = Color.GREEN;
    private static final Color COLOR_HANDOVER = new Color(255, 127, 0);

    @NeptusProperty(name = "Font Size Multiplier", description = "The font size. Use '1' for default.")
    public int fontMultiplier = 1;

    @NeptusProperty (name="Show Piccolo Waypoints")
    public boolean showWaypoints = true;

    @NeptusProperty (name="Filter to Show Only Main Vehicle's Piccolo Waypoints")
    public boolean filterMainVehicle = false;

    @NeptusProperty(name = "Piccolo Plan Thickness")
    public float piccoloWpsThickness = 3;

//    @NeptusProperty(name = "PCC Waypoint Colors", description = "Colors in which to display PCC waypoints\nuse '<wpt_min>-<wpt_max> (<red>,<green>,<blue>)' like '10-20 (255,128,0)'")
//    public StringProperty pccColors = new StringProperty("0-99 (255,128,0)\n0-10 (255,255,0)\n");
    private StringProperty pccColors = new StringProperty("0-99 (255,128,0)\n0-10 (255,255,0)\n");

    public static enum UavControlStateEnum {
        UNKNOWN, EXTERNAL, HANDOVER_READY, PLAN_READY, EXECUTING;

        public Color getColor() {
            switch (this) {
                case EXTERNAL:
                    return COLOR_IDLE;
                case HANDOVER_READY:
                    return COLOR_HANDOVER;
                case PLAN_READY:
                case EXECUTING:
                    return COLOR_ACCEPT;
                default:
                    return COLOR_IDLE;
            }
        }
        
        public String getPrettyPrintString() {
            String ret = this.toString().toLowerCase().replaceFirst("_r", " R");
            ret = ret.substring(0, 1).toUpperCase() + ret.substring(1);
            return ret;
        }
    }

    private Vector<ILayerPainter> renderers = new Vector<ILayerPainter>();

    private MultiSystemIMCMessageListener multiSysListener = null;
    private String[] piccoloMsgsArray = new String[] { "PlanControlState", "PiccoloWaypoint",
            "PiccoloWaypointDeleted", "PiccoloTrackingState", "PiccoloControlConfiguration" };

    protected PCCWaypointPainter wptPainter = new PCCWaypointPainter();
    protected LinkedHashMap<String, PiccoloControlConfiguration> configs = new LinkedHashMap<String, PiccoloControlConfiguration>();
    protected LinkedHashMap<String, UavControlStateEnum> controlStates = new LinkedHashMap<String, UavControlStateEnum>();
    protected LinkedHashMap<String, String> planStates = new LinkedHashMap<String, String>();

    // GUI
    private JLabel titleLabel = null;
    private UavStateDisplay stateLabel = null;
    private ToolbarButton downloadAllWaypointsButton, downloadAllConfButton, editConfButton,
            /*uploadConfButton,*/ /*gotoWaypointButton,*/ handoverWaypointButton, 
            showWPFilterWaypointButton;
    private ToolbarSwitch showWaypointsButton, showWaypointsFilterButton;
    private AbstractAction downloadAllWaypointsAction, downloadAllConfAction, editConfAction,
            /*uploadConfAction,*/ /*gotoWaypointAction,*/ handoverWaypointAction, showWaypointsAction,
            showWaypointsFilterAction, showWPFilterWaypointAction;

    /**
     * 
     */
    public UavPiccoloControl(ConsoleLayout console) {
        super(console);
        initializeActions();
        initialize();
        
        wptPainter.setConfigsWaypoints(configs);
    }

    /**
     * 
     */
    private void initialize() {
        removeAll();
        setSize(new Dimension(270, 60));
        setLayout(new BorderLayout());
        titleLabel = new JLabel(PluginUtils.getPluginName(this.getClass()));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9 * fontMultiplier));
        add(titleLabel, BorderLayout.NORTH);
        JPanel tmpPanel = new JPanel();
        tmpPanel.setLayout(new BoxLayout(tmpPanel, BoxLayout.LINE_AXIS));
        add(tmpPanel, BorderLayout.CENTER);

        downloadAllWaypointsButton = new ToolbarButton(downloadAllWaypointsAction);
        tmpPanel.add(downloadAllWaypointsButton);

        downloadAllConfButton = new ToolbarButton(downloadAllConfAction);
        tmpPanel.add(downloadAllConfButton);

        showWaypointsButton = new ToolbarSwitch(showWaypointsAction);
        showWaypointsButton.setSelected(showWaypoints);
        tmpPanel.add(showWaypointsButton);

        showWaypointsFilterButton = new ToolbarSwitch(showWaypointsFilterAction);
        showWaypointsFilterButton.setSelected(filterMainVehicle);
        tmpPanel.add(showWaypointsFilterButton);

        showWPFilterWaypointButton = new ToolbarButton(showWPFilterWaypointAction);
        tmpPanel.add(showWPFilterWaypointButton);

        tmpPanel.add(new JSeparator(SwingConstants.VERTICAL));

//        gotoWaypointButton = new ToolbarButton(gotoWaypointAction);
//        tmpPanel.add(gotoWaypointButton);
        
        stateLabel = new UavStateDisplay();
        stateLabel.setControlState(UavControlStateEnum.UNKNOWN);
        tmpPanel.add(stateLabel);

        editConfButton = new ToolbarButton(editConfAction);
        tmpPanel.add(editConfButton);
        
        handoverWaypointButton = new ToolbarButton(handoverWaypointAction);
        tmpPanel.add(handoverWaypointButton);
    }

    /**
     * 
     */
    private void initializeActions() {
        downloadAllWaypointsAction = new AbstractAction("Download Piccolo Waypoints",
                ICON_PICCOLO_WP_DOWNLOAD) {
            public void actionPerformed(ActionEvent arg0) {
                new Thread() {
                    public void run() {
                        if (isUAV(getMainVehicleId())) {
                            send(IMCDefinition.getInstance().create("ListPiccoloWaypoints"));
                            NeptusLog.pub().info("<###>----------Send to " + getMainVehicleId());
                            System.out.flush();
                        }
                        String[] vlst = wptPainter.getVehiclesList();
                        for (String vehicleID : vlst) {
                            if (vehicleID.equalsIgnoreCase(getMainVehicleId()))
                                continue;
                            NeptusLog.pub().info("<###>----------Send to " + vehicleID);
                            System.out.flush();
                            ImcMsgManager.getManager().sendMessageToVehicle(
                                    IMCDefinition.getInstance().create("ListPiccoloWaypoints"),
                                    vehicleID, null);
                        }
                    }
                }.start();
            }
        };
        downloadAllWaypointsAction.putValue(AbstractAction.SHORT_DESCRIPTION,
                "Download Piccolo Waypoints");

        downloadAllConfAction = new AbstractAction("Download Waypoints Configurations",
                ICON_CONFIG_DOWNLOAD) {
            public void actionPerformed(ActionEvent arg0) {
                new Thread() {
                    public void run() {
                        if (isUAV(getMainVehicleId())) {
                            send(IMCDefinition.getInstance().create("GetPiccoloControlConfiguration"));
                            NeptusLog.pub().info("<###>----------Send to " + getMainVehicleId());
                            System.out.flush();
                        }
                        String[] vlst = wptPainter.getVehiclesList();
                        for (String vehicleID : vlst) {
                            if (vehicleID.equalsIgnoreCase(getMainVehicleId()))
                                continue;
                            NeptusLog.pub().info("<###>----------Send to " + vehicleID);
                            System.out.flush();
                            ImcMsgManager.getManager().sendMessageToVehicle(
                                    IMCDefinition.getInstance().create("GetPiccoloControlConfiguration"),
                                    vehicleID, null);
                        }
                    }
                }.start();
            }
        };
        downloadAllConfAction.putValue(AbstractAction.SHORT_DESCRIPTION,
                "Download Waypoints Configurations");

        editConfAction = new AbstractAction("Edit Waypoints Configurations", ICON_EDIT) {
            public void actionPerformed(ActionEvent arg0) {
                new Thread() {
                    public void run() {
                        if (isUAV(getMainVehicleId())) {
                            PiccoloControlConfiguration conf = configs.get(getMainVehicleId());
                            if (conf == null) {
//                                getConsole().warning(
//                                        "No configuration for " + getMainVehicleId() + " yet!");
                            }
                            else {
                                PiccoloControlConfigurationPanel confEdit = new PiccoloControlConfigurationPanel();
                                PiccoloControlConfiguration ret = confEdit.showEditPanel(conf,
                                        SwingUtilities.getWindowAncestor(UavPiccoloControl.this));
                                if (ret != null) {
                                    IMCMessage message = IMCDefinition.getInstance()
                                            .create("PiccoloControlConfiguration");
                                    message.setValue("p_min", ret.planWpMin);
                                    message.setValue("p_max", ret.planWpMax);
                                    message.setValue("h_min", ret.handoverWpMin);
                                    message.setValue("h_max", ret.handoverWpMax);
                                    message.setValue("sw_radius", ret.serviceRadius);
                                    message.setValue("sw_alt", ret.serviceAltitude);
                                    message.setValue("sw_speed", ret.serviceSpeed);
                                    send(message);
                                }
                            }
                        }
                    }
                }.start();
            }
        };
        editConfAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Edit Waypoints Configurations");

//        gotoWaypointAction = new AbstractAction("Go To Waypoint", ICON_PICCOLO_WP_GOTO) {
//            public void actionPerformed(final ActionEvent aevt) {
//                new Thread() {
//                    public void run() {
//                        String vid = getMainVehicleId();
//                        if (isUAV(vid)) {
//                            boolean ctrlOn = false, altOn = false;
//                            if ((aevt.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK)
//                                ctrlOn = true;
//                            if ((aevt.getModifiers() & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK)
//                                altOn = true;
//                            PiccoloControlConfiguration conf = configs.get(vid);
//                            if (conf == null) {
//                                getConsole().warning(
//                                        "No configuration for " + vid + " yet!");
//                                return;
//                            }
//                            
//                            LinkedHashMap<Integer, PCCWaypoint> wps = wptPainter.getWaypoints(vid);
//                            int initialValue = 0;
//                            Vector<String> wpL = new Vector<String>();
//                            for (int wpn : wps.keySet()) {
//                                if (conf.isHandhoverWP((short) wpn)) {
//                                    wpL.add("Handover WP" + wpn);
//                                    if (initialValue == 0)
//                                        initialValue = Math.max(0, wpL.size() - 1);
//                                }
//                                else if (!conf.isPlanWP((short) wpn) && ctrlOn && altOn) {
//                                    if (conf.isServiceWP((short) wpn))
//                                        wpL.add("Service WP" + wpn);
//                                    else
//                                        wpL.add("Plan WP" + wpn);
//                                }
//                                else if (!conf.isHandhoverWP((short) wpn)
//                                        && !conf.isPlanWP((short) wpn) && ctrlOn)
//                                    wpL.add("External WP" + wpn);
//                                else if (ctrlOn && altOn)
//                                    wpL.add("Unknown WP" + wpn);
//                            }
//
//                            int userOpt = JOptionPane.showOptionDialog(
//                                    SwingUtilities.getWindowAncestor(UavPiccoloControl.this),
//                                    "Choose a waypoint", "GoTo waypoint",
//                                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
//                                    ICON_PICCOLO_WP_GOTO, wpL.toArray(new String[wpL.size()]),
//                                    initialValue);
//                            if (userOpt >= 0) {
//                                int idx = Integer.parseInt(wpL.get(userOpt).replaceAll("[\\s\\D]", ""));
//                                send(IMCDefinition.getInstance().create("GotoPiccoloWaypoint", "index",
//                                        idx));
//                            }
//                        }
//                    }
//                }.start();
//            }
//        };
//        gotoWaypointAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Go To Waypoint");

        handoverWaypointAction = new AbstractAction("Handover", ICON_PICCOLO_WP_GOTO_HO) {
            public void actionPerformed(final ActionEvent aevt) {
                new Thread() {
                    public void run() {
                        String vid = getMainVehicleId();
                        if (isUAV(vid)) {
                            PiccoloControlConfiguration conf = configs.get(vid);
                            if (conf == null) {
//                                getConsole().warning(
//                                        "No configuration for " + vid + " yet!");
                                return;
                            }
                            
                            boolean ctrlOn = false, altOn = false, shiftOn = false;
                            if ((aevt.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK)
                                ctrlOn = true;
                            if ((aevt.getModifiers() & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK)
                                altOn = true;
                            if ((aevt.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK)
                                shiftOn = true;

                            if (!ctrlOn && !altOn && !shiftOn) {
                                LinkedHashMap<Integer, PCCWaypoint> wps = wptPainter.getWaypoints(vid);
                                int choosenValue = -1;
                                Vector<String> wpL = new Vector<String>();
                                for (int wpn : wps.keySet()) {
                                    if (conf.isHandhoverWP((short) wpn)) {
                                        wpL.add("Handover WP" + wpn);
                                        if (wpn > choosenValue) {
                                            choosenValue = wpn;
                                            break;
                                        }
                                    }
                                }

                                if (choosenValue > 0) {
                                    send(IMCDefinition.getInstance().create("GotoPiccoloWaypoint", "index",
                                            choosenValue));
                                }
                                else {
//                                    getConsole().warning(
//                                            "No handover waypoint for " + vid + " to go to!");
                                }
                            }
                            else if (ctrlOn) {
                                LinkedHashMap<Integer, PCCWaypoint> wps = wptPainter.getWaypoints(vid);
                                int initialValue = 0;
                                Vector<String> wpL = new Vector<String>();
                                for (int wpn : wps.keySet()) {
                                    if (conf.isHandhoverWP((short) wpn)) {
                                        wpL.add("Handover WP" + wpn);
                                        if (initialValue == 0)
                                            initialValue = Math.max(0, wpL.size() - 1);
                                    }
                                    else if (!conf.isPlanWP((short) wpn) && shiftOn && altOn) {
                                        if (conf.isServiceWP((short) wpn))
                                            wpL.add("Service WP" + wpn);
                                        else
                                            wpL.add("Plan WP" + wpn);
                                    }
                                    else if (!conf.isHandhoverWP((short) wpn)
                                            && !conf.isPlanWP((short) wpn) && shiftOn)
                                        wpL.add("External WP" + wpn);
                                    else if (ctrlOn && altOn)
                                        wpL.add("Unknown WP" + wpn);
                                }

                                int userOpt = JOptionPane.showOptionDialog(
                                        SwingUtilities.getWindowAncestor(UavPiccoloControl.this),
                                        "Choose a waypoint", "GoTo waypoint",
                                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                                        ICON_PICCOLO_WP_GOTO, wpL.toArray(new String[wpL.size()]),
                                        initialValue);
                                if (userOpt >= 0) {
                                    int idx = Integer.parseInt(wpL.get(userOpt).replaceAll("[\\s\\D]", ""));
                                    send(IMCDefinition.getInstance().create("GotoPiccoloWaypoint", "index",
                                            idx));
                                }
                            }
                        }
                    }
                }.start();
            }
        };
        handoverWaypointAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Handover");

        showWaypointsAction = new AbstractAction("Show waypoints", ICON_PICCOLO_WP_SHOW) {
            public void actionPerformed(final ActionEvent e) {
                if ((e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK) {
                    if (!((ToolbarSwitch)e.getSource()).isSelected()) {
                    GuiUtils.errorMessage(SwingUtilities.getWindowAncestor(UavPiccoloControl.this),
                            new Exception("adsdasda"));
                    }
                }
                
                showWaypoints = ((ToolbarSwitch)e.getSource()).isSelected();
            }
        };
        showWaypointsAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Show only main vehicle");
        
        showWaypointsFilterAction = new AbstractAction("Show only main vehicle", ICON_PICCOLO_WP_SHOW_FILTER) {
            public void actionPerformed(final ActionEvent e) {
                filterMainVehicle = ((ToolbarSwitch)e.getSource()).isSelected();
            }
        };
        showWaypointsFilterAction.putValue(AbstractAction.SHORT_DESCRIPTION, "Show only main vehicle");
        
        showWPFilterWaypointAction = new AbstractAction("Edit Visual Filter Configurations",
                ICON_PICCOLO_WP_SHOW_CONFIG) {
            public void actionPerformed(final ActionEvent aevt) {
                new Thread() {
                    public void run() {
                        boolean ctrlOn = false, altOn = false, shiftOn = false;
                        if ((aevt.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK)
                            ctrlOn = true;
                        if ((aevt.getModifiers() & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK)
                            altOn = true;
                        if ((aevt.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK)
                            shiftOn = true;

                        if (!ctrlOn && !altOn && !shiftOn) {
                            PiccoloWPShowPreferencesPanel confEdit = new PiccoloWPShowPreferencesPanel();
                            boolean ret = confEdit.showEditPanel(wptPainter.isPaintHandoverWP(),
                                    wptPainter.isPaintExternalWP(),
                                    SwingUtilities.getWindowAncestor(UavPiccoloControl.this));
                            if (ret) {
                                wptPainter.setPaintHandoverWP(confEdit.isHandoverWPVisible());
                                wptPainter.setPaintExternalWP(confEdit.isExternalWPVisible());
                            }
                        }
                        else {
                            if (shiftOn) {
                                wptPainter.setPaintHandoverWP(false);
                                wptPainter.setPaintExternalWP(false);
                            }
                            else if (ctrlOn && altOn) {
                                wptPainter.setPaintHandoverWP(true);
                                wptPainter.setPaintExternalWP(true);
                            }
                            else if (ctrlOn && !altOn) {
                                wptPainter.setPaintHandoverWP(true);
                                wptPainter.setPaintExternalWP(false);
                            }
                            else if (!ctrlOn && altOn) {
                                wptPainter.setPaintHandoverWP(false);
                                wptPainter.setPaintExternalWP(true);
                            }
                        }
                    }
                }.start();
            }
        };
        showWPFilterWaypointAction.putValue(AbstractAction.SHORT_DESCRIPTION,
                "Edit Visual Filter Configurations");

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.SubPanel#postLoadInit()
     */
    @Override
    public void initSubPanel() {
        
        wptPainter.setThickness(piccoloWpsThickness);
        getMultiSysListener();

        renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
        for (ILayerPainter str2d : renderers) {
            str2d.addPostRenderPainter(this, this.getClass().getSimpleName());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#clean()
     */
    @Override
    public void cleanSubPanel() {
        getMultiSysListener().clean();

        renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
        for (ILayerPainter str2d : renderers) {
            str2d.removePostRenderPainter(this);
        }
    }

    private boolean isUAV(String id) {
        String mId = getMainVehicleId();
        ImcSystem sysM = ImcSystemsHolder.lookupSystemByName(mId);
        if (sysM != null
                && (sysM.getType() == SystemTypeEnum.VEHICLE 
                && sysM.getTypeVehicle() == VehicleTypeEnum.UAV))
            return true;
        else
            return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.consolebase.SubPanelChangeListener#subPanelChanged
     * (pt.lsts.neptus.consolebase.SubPanelChangeEvent)
     */
    @Override
    public void subPanelChanged(SubPanelChangeEvent panelChange) {

        if (panelChange == null)
            return;

        if (ReflectionUtil.hasInterface(panelChange.getPanel().getClass(), ILayerPainter.class)) {

            ILayerPainter sub = (ILayerPainter) panelChange.getPanel();

            if (panelChange.added()) {
                renderers.add(sub);
                ILayerPainter str2d = sub;
                if (str2d != null) {
                    str2d.addPostRenderPainter(this, "LBL Tracker Range");
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
    }

    @Override
    public void propertiesChanged() {
        // pccButton.setVisible(showPCC);
        wptPainter.setThickness(piccoloWpsThickness);
        wptPainter.colors = new WaypointColors(pccColors.toString());
        
        if (showWaypoints != showWaypointsButton.isSelected())
            showWaypointsButton.setSelected(showWaypoints);

        if (filterMainVehicle != showWaypointsFilterButton.isSelected())
            showWaypointsFilterButton.setSelected(filterMainVehicle);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates
     * ()
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

        if (isUAV(getMainVehicleId())) {
            if (configs.containsKey(getMainVehicleId()))
                editConfAction.setEnabled(true);
            else
                editConfAction.setEnabled(false);
//            gotoWaypointAction.setEnabled(true);
            handoverWaypointAction.setEnabled(true);
        }
        else {
            editConfAction.setEnabled(false);
//            gotoWaypointAction.setEnabled(false);
            handoverWaypointAction.setEnabled(false);
        }
        
        for (String id : configs.keySet()) {
            //NeptusLog.pub().info("<###>entering FOR with id:"+id+", pState is:"+planStates.get(id)+", cState is:"+controlStates.get(id));
            try {
                int wpTo = wptPainter.getDestinationWaypoint(id);
                PiccoloControlConfiguration pCConfig = configs.get(id);
                String pState = planStates.get(id);
                UavControlStateEnum cState = controlStates.get(id);
                if (cState == null)
                    cState = UavControlStateEnum.UNKNOWN;

                if (pCConfig == null) {
                    cState = UavControlStateEnum.UNKNOWN;
                }
                else if (wpTo < 0) {
                    cState = UavControlStateEnum.EXTERNAL;
                }
                else if (pCConfig.isHandhoverWP((short) wpTo)) {
                    cState = UavControlStateEnum.HANDOVER_READY;
                }
                else if (pCConfig.isPlanWP((short) wpTo)) {
                    cState = UavControlStateEnum.PLAN_READY;
                    if (pState != null) {
                        if (!"READY".equalsIgnoreCase(pState))
                            cState = UavControlStateEnum.EXECUTING;
                    }
                }
                else {
                    cState = UavControlStateEnum.EXTERNAL;
                }
                controlStates.put(id, cState);
            }
            catch (Exception e) {
                e.printStackTrace();
                controlStates.put(id, UavControlStateEnum.UNKNOWN);
            }
        }

        String mId = getMainVehicleId();
        ImcSystem sysM = ImcSystemsHolder.lookupSystemByName(mId);
        if (sysM == null
                || !(sysM.getType() == SystemTypeEnum.VEHICLE && sysM.getTypeVehicle() == VehicleTypeEnum.UAV)
                || controlStates.get(mId) == null) {
            stateLabel.setControlState(UavControlStateEnum.UNKNOWN);
        }
        else {
            UavControlStateEnum csM = controlStates.get(mId);
            stateLabel.setControlState(csM);
        }
        
        wptPainter.setMainVehicle(mId);

        updateVisibleVehiclesWaypoints();
        
        return true;
    }

    /**
     * 
     */
    private void updateVisibleVehiclesWaypoints() {
        String[] vList = wptPainter.getVehiclesList();
        if (vList.length == 0) {
            return;
        }
        for (String veh : vList) {
            if (!filterMainVehicle)
                wptPainter.setVehicleWaypointsVisible(veh, true);
            else if (veh.equalsIgnoreCase(wptPainter.getMainVehicle()))
                wptPainter.setVehicleWaypointsVisible(veh, true);
            else
                wptPainter.setVehicleWaypointsVisible(veh, false);
        }
    }

    /**
     * 
     */
    private MultiSystemIMCMessageListener getMultiSysListener() {
        if (multiSysListener == null) {
            multiSysListener = new MultiSystemIMCMessageListener(this.getClass().getSimpleName() + " ["
                    + Integer.toHexString(hashCode()) + "]") {
                @Override
                public void messageArrived(ImcId16 id, IMCMessage message) {
                    try {
                        // int id = message.getType().getId();
                        PiccoloWPType wpt = PiccoloWPType.getType(message.getMessageType().getShortName());
                        if (wpt == PiccoloWPType.WAYPOINT_UNKOWN)
                            return;
                        // message.dump(System.out);
                        VehicleType vt = VehiclesHolder.getVehicleWithImc(id);
                        if (vt == null)
                            return;
                        String vid = vt.getId();
                        PCCWaypoint w = parseWaypoint(message);
                        if (StringUtils.isTokenInList("PiccoloWaypoint", message.getAbbrev())) {
                            NeptusLog.pub().info("<###>received waypoint " + w.getId()
                                    + " attached to vehicle " + vid);
                        }

                        switch (wpt) {
                            case WAYPOINT_ADD:
                                PCCWaypoint waypoint = parseWaypoint(message);
                                wptPainter.setWaypoint(vid, waypoint);
                                callRepaint();
                                break;
                            case WAYPOINT_REMOVE:
                                int deleted = parseDeletedId(message);
                                if (deleted != -1)
                                    wptPainter.deleteWaypoint(vid, deleted);
                                callRepaint();
                                break;
                            case WAYPOINTTRACKED_ID:
                                String status = message.getString("status").toUpperCase();
                                int wpt_to = message.getInteger("to");
                                int wpt_from = message.getInteger("from");
                   
                                if ("OFF".equalsIgnoreCase(status)) {
                                    wpt_to = -1;
                                }
                                wptPainter.setDestinationWaypoint(vid, wpt_to);
                                wptPainter.setSourceWaypoint(vid, wpt_from);
                                if (getConsole() != null) {
                                    ImcSystem sys = ImcSystemsHolder.lookupSystemByName(vid);
                                    if (sys != null)
                                        wptPainter.setVehiclePosition(vid, sys.getLocation());
                                }
                                callRepaint();
                                break;
                            case CONFIG_MSG:
                                PiccoloControlConfiguration pCConf = parsePiccoloControlConfiguration(message);
                                configs.put(vid, pCConf);
                                break;
                            case PLAN_STATE:
                                if (configs.containsKey(vid)) {
                                    planStates.put(vid, message.getString("state"));
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            };
            multiSysListener.setSystemToListen();
            multiSysListener.setMessagesToListen(piccoloMsgsArray);
        }
        return multiSysListener;
    }

    /**
     * 
     */
    protected void callRepaint() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D
     * , pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (showWaypoints)
            wptPainter.paint(g, renderer);
    }

    public static enum PiccoloWPType {
        WAYPOINT_UNKOWN(""), WAYPOINT_ADD("PiccoloWaypoint"), WAYPOINT_REMOVE(
                "PiccoloWaypointDeleted"), WAYPOINTTRACKED_ID("PiccoloTrackingState"), CONFIG_MSG(
                "PiccoloControlConfiguration"), PLAN_STATE("PlanControlState");
        private final String pWPType;

        PiccoloWPType(String pwpt) {
            this.pWPType = pwpt;
        }

        public String type() {
            return pWPType;
        }

        public static final PiccoloWPType getType(String type) {
            for (PiccoloWPType val : PiccoloWPType.values()) {
                if (val.type().equalsIgnoreCase(type)) {
                    return val;
                }
            }
            return WAYPOINT_UNKOWN;

            // if (WAYPOINT_ADD.type().equalsIgnoreCase(type))
            // return WAYPOINT_ADD;
            // else if (WAYPOINT_REMOVE.type().equalsIgnoreCase(type))
            // return WAYPOINT_REMOVE;
            // else if (WAYPOINTTRACKED_ID.type().equalsIgnoreCase(type))
            // return WAYPOINTTRACKED_ID;
            // else
            // return WAYPOINT_UNKOWN;
        }
    };

    private static PCCWaypoint parseWaypoint(IMCMessage message) {
        if (message == null)
            return null;

        if (PiccoloWPType.getType(message.getMessageType().getShortName()) == PiccoloWPType.WAYPOINT_ADD) {
            LocationType loc = new LocationType();

            loc.setLatitudeDegs(Math.toDegrees(message.getDouble("lat")));
            loc.setLongitudeDegs(Math.toDegrees(message.getDouble("lon")));
            loc.setDepth(message.getDouble("depth"));

            PCCWaypoint wpt = new PCCWaypoint(
                    message.getInteger("index"),
                    message.getInteger("next"), loc,
                    (float)message.getFloat("lradius"));
            return wpt;
        }
        return null;
    }

    private static int parseDeletedId(IMCMessage message) {
        if (message == null)
            return -1;

        if (PiccoloWPType.getType(message.getMessageType().getShortName()) == PiccoloWPType.WAYPOINT_REMOVE)
            return message.getInteger("index");

        return -1;
    }

    /**
     * @param message
     */
    private PiccoloControlConfiguration parsePiccoloControlConfiguration(IMCMessage message) {
        if (message == null)
            return new PiccoloControlConfiguration();

        PiccoloControlConfiguration ret = new PiccoloControlConfiguration();
        if (PiccoloWPType.getType(message.getMessageType().getShortName()) == PiccoloWPType.CONFIG_MSG) {
            ret.setPlanWpMin((short) message.getInteger("p_min"));
            ret.setPlanWpMax((short) message.getInteger("p_max"));
            ret.setHandoverWpMin((short) message.getInteger("h_min"));
            ret.setHandoverWpMax((short) message.getInteger("h_max"));
            ret.setServiceRadius(message.getDouble("sw_radius"));
            ret.setServiceAltitude(message.getDouble("sw_alt"));
            ret.setServiceSpeed(message.getDouble("sw_speed"));
        }
        return ret;
    }

    // ___________________________________________________

    /**
     * @author zp
     *
     */
    static class Interval {
        Number min = 0;
        Number max = 0;

        public Interval(Number min, Number max) {
            this.min = min;
            this.max = max;
        }

        public boolean contains(Number value) {
            return max.doubleValue() >= value.doubleValue()
                    && value.doubleValue() >= min.doubleValue();
        }

        @Override
        public int hashCode() {
            return (min.toString() + "->" + max.toString()).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Interval) {
                return ((Interval) obj).min.doubleValue() == min.doubleValue()
                        && ((Interval) obj).max.doubleValue() == max.doubleValue();
            }
            return false;
        }

    }

    /**
     * @author zp
     *
     */
    static class WaypointColors {

        LinkedHashMap<Integer, Color> colors = new LinkedHashMap<Integer, Color>();
        LinkedHashMap<Interval, Color> intervals = new LinkedHashMap<Interval, Color>();

        public WaypointColors(String colors) {
            String[] lines = colors.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].startsWith("#"))
                    continue;
                int min, max, red, green, blue;
                try {
                    String[] parts = lines[i].split("[ \\-\\(,\\)]+");
                    if (parts.length < 5) {
                        for (String p : parts)
                            NeptusLog.pub().info("<###> "+p + " --- ");
                        throw new Exception("Syntax Error on line " + (i + 1) + ", " + parts.length);
                    }
                    min = Integer.parseInt(parts[0]);
                    max = Integer.parseInt(parts[1]);
                    red = Integer.parseInt(parts[2]);
                    green = Integer.parseInt(parts[3]);
                    blue = Integer.parseInt(parts[4]);

                    Interval in = new Interval(min, max);
                    Color c = new Color(red, green, blue);

                    intervals.put(in, c);
                }
                catch (Exception e) {
                    NeptusLog.pub().error(
                            "PCC Waypoint Colors: Syntax error on line " + (i + 1), e);
                }
            }
        }

        Color getColor(int wptNumber) {
            if (!colors.containsKey(wptNumber)) {
                Color defColor = Color.orange;
                for (Interval i : intervals.keySet())
                    if (i.contains(wptNumber))
                        defColor = intervals.get(i);
                colors.put(wptNumber, defColor);
            }
            return colors.get(wptNumber);
        }
    }

    /**
     * @author pdias
     *
     */
    public static class PiccoloControlConfiguration implements Cloneable {

        // short serviceWp = -1;
        short planWpMin = -1;
        short planWpMax = -1;
        short handoverWpMin = -1;
        short handoverWpMax = -1;

        double serviceRadius = -1;
        double serviceAltitude = -1;
        double serviceSpeed = -1;

        public static PiccoloControlConfiguration createDefault() {
            PiccoloControlConfiguration ret = new PiccoloControlConfiguration();
            ret.planWpMin = 20;
            ret.planWpMax = 69;
            ret.handoverWpMin = 19;
            ret.handoverWpMax = 19;
            ret.serviceRadius = 100;
            return ret;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#clone()
         */
        @Override
        protected Object clone() throws CloneNotSupportedException {
            PiccoloControlConfiguration ret = new PiccoloControlConfiguration();
            ret.planWpMin = this.planWpMin;
            ret.planWpMax = this.planWpMax;
            ret.handoverWpMin = this.handoverWpMin;
            ret.handoverWpMax = this.handoverWpMax;
            ret.serviceRadius = this.serviceRadius;
            ret.serviceAltitude = this.serviceAltitude;
            ret.serviceSpeed = this.serviceSpeed;
            return ret;
        }

        public boolean isPlanWP(short wp) {
            if (planWpMin < 0 || planWpMax < 0)
                return false;
            return (planWpMin <= wp && wp <= planWpMax) ? true : false;
        }

        public boolean isHandhoverWP(short wp) {
            if (handoverWpMin < 0 || handoverWpMax < 0)
                return false;
            return (handoverWpMin <= wp && wp <= handoverWpMax) ? true : false;
        }

        public boolean isServiceWP(short wp) {
            if (isPlanWP(wp))
                return (wp == getServiceWp()) ? true : false;
            else
                return false;
        }

        public boolean isExternalWP(short wp) {
            if (!isPlanWP(wp) && !isHandhoverWP(wp))
                return true;
            else
                return false;
        }
        
        /**
         * @return the serviceWp
         */
        public short getServiceWp() {
            return planWpMax;
        }

        /**
         * @return the planWpMin
         */
        public short getPlanWpMin() {
            return planWpMin;
        }

        /**
         * @param planWpMin
         *            the planWpMin to set
         */
        public void setPlanWpMin(short planWpMin) {
            this.planWpMin = planWpMin;
        }

        /**
         * @return the planWpMax
         */
        public short getPlanWpMax() {
            return planWpMax;
        }

        /**
         * @param planWpMax
         *            the planWpMax to set
         */
        public void setPlanWpMax(short planWpMax) {
            this.planWpMax = planWpMax;
        }

        /**
         * @return the handoverWpMin
         */
        public short getHandoverWpMin() {
            return handoverWpMin;
        }

        /**
         * @param handoverWpMin
         *            the handoverWpMin to set
         */
        public void setHandoverWpMin(short handoverWpMin) {
            this.handoverWpMin = handoverWpMin;
        }

        /**
         * @return the handoverWpMax
         */
        public short getHandoverWpMax() {
            return handoverWpMax;
        }

        /**
         * @param handoverWpMax
         *            the handoverWpMax to set
         */
        public void setHandoverWpMax(short handoverWpMax) {
            this.handoverWpMax = handoverWpMax;
        }

        /**
         * @return the serviceRadius
         */
        public double getServiceRadius() {
            return serviceRadius;
        }

        /**
         * @param serviceRadius
         *            the serviceRadius to set
         */
        public void setServiceRadius(double serviceRadius) {
            this.serviceRadius = serviceRadius;
        }

        /**
         * @return the serviceAltitude
         */
        public double getServiceAltitude() {
            return serviceAltitude;
        }

        /**
         * @param serviceAltitude
         *            the serviceAltitude to set
         */
        public void setServiceAltitude(double serviceAltitude) {
            this.serviceAltitude = serviceAltitude;
        }

        /**
         * @return the serviceSpeed
         */
        public double getServiceSpeed() {
            return serviceSpeed;
        }

        /**
         * @param serviceSpeed
         *            the serviceSpeed to set
         */
        public void setServiceSpeed(double serviceSpeed) {
            this.serviceSpeed = serviceSpeed;
        }
    }

    /**
     * @author pdias
     *
     */
    private static class UavStateDisplay extends JXPanel {

        private static final Color COLOR_IDLE = new JXPanel().getBackground();

        private JXLabel label = null;
        private UavControlStateEnum controlState = UavControlStateEnum.UNKNOWN;

        /**
         * 
         */
        public UavStateDisplay() {
            initialize();
        }

        private void initialize() {
            setBackgroundPainter(getCompoundBackPainter());
            label = new JXLabel("<html><b>"
                    + controlState.getPrettyPrintString().replaceAll(" ", "<br>"), JLabel.CENTER);
            label.setHorizontalTextPosition(JLabel.CENTER);
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 11));
            label.setForeground(Color.WHITE);

            setLayout(new BorderLayout());
            add(label, BorderLayout.CENTER);

            setPreferredSize(new Dimension(215, 180));
            setSize(new Dimension(215, 180));
        }

        /**
         * @param controlState
         *            the controlState to set
         */
        public void setControlState(UavControlStateEnum controlState) {
            this.controlState = controlState;
            label.setText("<html><b>" + controlState.getPrettyPrintString().replaceAll(" ", "<br>"));
            updateBackColor(this.controlState.getColor());
        }

        // Background Painter Stuff
        private RectanglePainter rectPainter;
        private CompoundPainter<JXPanel> compoundBackPainter;

        /**
         * @return the rectPainter
         */
        private RectanglePainter getRectPainter() {
            if (rectPainter == null) {
                rectPainter = new RectanglePainter(5, 5, 5, 5, 20, 20);
                rectPainter.setFillPaint(COLOR_IDLE);
                rectPainter.setBorderPaint(COLOR_IDLE.darker().darker().darker());
                rectPainter.setStyle(RectanglePainter.Style.BOTH);
                rectPainter.setBorderWidth(2);
                rectPainter.setAntialiasing(true);
            }
            return rectPainter;
        }

        /**
         * @return the compoundBackPainter
         */
        private CompoundPainter<JXPanel> getCompoundBackPainter() {
            compoundBackPainter = new CompoundPainter<JXPanel>(
            // new MattePainter(Color.BLACK),
                    getRectPainter(), new GlossPainter());
            return compoundBackPainter;
        }

        /**
         * @param color
         */
        private void updateBackColor(Color color) {
            getRectPainter().setFillPaint(color);
            getRectPainter().setBorderPaint(color.darker());

            // this.setBackgroundPainter(getCompoundBackPainter());
            repaint();
        }

    }

}
