/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Jul 21, 2011
 */
package pt.lsts.neptus.plugins.acoustic;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.AcousticOperation;
import pt.lsts.imc.AcousticSystems;
import pt.lsts.imc.AcousticSystemsQuery;
import pt.lsts.imc.Elevator;
import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.GpsFix;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControl.OP;
import pt.lsts.imc.PlanControl.TYPE;
import pt.lsts.imc.PlanDB;
import pt.lsts.imc.RSSI;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.imc.StorageUsage;
import pt.lsts.imc.TextMessage;
import pt.lsts.imc.TransmissionRequest;
import pt.lsts.imc.TransmissionStatus;
import pt.lsts.imc.Voltage;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.imc.def.ZUnits;
import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMessageSenderPanel;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMenuItem;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Acoustic Operations", author = "ZP", icon = "pt/lsts/neptus/plugins/acoustic/manta.png")
@LayerPriority(priority = 40)
@Popup(name = "Acoustic Operations", accelerator = KeyEvent.VK_M, width = 350, height = 250, pos = POSITION.BOTTOM_RIGHT, icon = "pt/lsts/neptus/plugins/acoustic/manta.png")
public class MantaOperations extends ConsolePanel implements ConfigurationListener, Renderer2DPainter {

    private static final long serialVersionUID = 1L;

    protected static JDialog visibleDialog = null;
    protected LinkedHashMap<String, JRadioButton> radioButtons = new LinkedHashMap<String, JRadioButton>();
    protected LinkedHashMap<String, JButton> cmdButtons = new LinkedHashMap<String, JButton>();

    protected ButtonGroup group = new ButtonGroup();
    protected JPanel listPanel = new JPanel();
    protected JTextArea bottomPane = new JTextArea();
    protected final JButton clearButton = new JButton(I18n.text("Clear ranges"));
    private final JCheckBox showRangesCheckBox = new JCheckBox(I18n.text("Show ranges"));
    protected String selectedSystem = null;
    protected String gateway = "any";
    protected JLabel lblState = new JLabel("<html><h1>" + I18n.text("Please select a gateway") + "</h1>");
    protected ImcMessageSenderPanel editor;
    protected LinkedHashMap<Integer, PlanControl> pendingRequests = new LinkedHashMap<>();

    public HashSet<String> knownSystems = new HashSet<>();

    @NeptusProperty(name = "Systems listing", description = "Use commas to separate system identifiers")
    public String sysListing = "benthos-1,benthos-2,benthos-3,benthos-4,lauv-xtreme-2,lauv-noptilus-1,lauv-noptilus-2,lauv-noptilus-3";

    @NeptusProperty(name = "Display ranges in the map")
    public boolean showRanges = true;

    @NeptusProperty(name = "Use system discovery", description = "Instead of a static list, receive supported systems from gateway")
    public boolean sysDiscovery = true;

    @NeptusProperty(name = "Separate ranging when using \"any\" gateway", category = "Any Gateway", userLevel = LEVEL.ADVANCED, 
            description = "Introduces a time separation between messages when \"any\" gateway..")
    private boolean separateRangingForAnyGateway = true;

    @NeptusProperty(name = "Separate ranging when using \"any\" gateway time", category = "Any Gateway", userLevel = LEVEL.ADVANCED, 
            description = "Time in seconds")
    private short separateRangingForAnyGatewaySeconds = 2;

    protected LinkedHashMap<String, LocationType> systemLocations = new LinkedHashMap<>();

    protected Vector<LocationType> rangeSources = new Vector<LocationType>();
    protected Vector<Double> rangeDistances = new Vector<Double>();

    protected boolean initialized = false;

    private ConcurrentHashMap<Integer, TransmissionRequest> transmissions = new ConcurrentHashMap<Integer, TransmissionRequest>();
    /**
     * @param console
     */
    public MantaOperations(ConsoleLayout console) {
        super(console);      
    }

    private void addTemplates() {

        PlanControl surf = new PlanControl();
        Elevator elev = new Elevator();
        elev.setEndZ(0);
        elev.setEndZUnits(ZUnits.DEPTH);
        elev.setStartZ(0);
        elev.setStartZUnits(ZUnits.DEPTH);
        elev.setRadius(15);
        elev.setSpeed(1.2);
        elev.setSpeedUnits(SpeedUnits.METERS_PS);
        surf.setPlanId("surface");
        surf.setArg(elev);
        surf.setType(TYPE.REQUEST);
        surf.setRequestId(1);
        surf.setFlags(PlanControl.FLG_IGNORE_ERRORS);
        surf.setOp(OP.START);

        editor.addTemplate(surf);

        SetEntityParameters setParams = new SetEntityParameters();
        setParams.setName("Report Supervisor");
        EntityParameter p1 = new EntityParameter();
        p1.setName("Acoustic Reports");
        p1.setValue("true");
        EntityParameter p2 = new EntityParameter();
        p2.setName("Acoustic Reports Periodicity");
        p2.setValue("60");
        setParams.setParams(Arrays.asList(p1, p2));

        editor.addTemplate(setParams);

        TextMessage txt = new TextMessage().setText("info");
        txt.setOrigin("neptus");
        
        editor.addTemplate(txt);
    }

    protected ActionListener systemActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            selectedSystem = e.getActionCommand();
        }
    };


    @NeptusMenuItem("Tools>Acoustic Operations>Start plan...")
    public void startPlan() {
        SendPlanDialog dialog = SendPlanDialog.sendPlan(getConsole());

        if (dialog == null)
            return;

        PlanType pt = getConsole().getMission().getIndividualPlansList().get(dialog.planId);
        IMCMessage spec = pt.asIMCPlan();
        boolean customManeuver = !pt.getGraph().getInitialManeuverId().equals(dialog.startingManeuver);
        
        if (dialog.justSendPlan || dialog.sendDefinition) {
            PlanDB pdb = new PlanDB();
            pdb.setPlanId(pt.getId());
            pdb.setOp(PlanDB.OP.SET);
            pdb.setType(PlanDB.TYPE.REQUEST);
            pdb.setArg(spec);
            sendAcoustically(dialog.selectedVehicle, pdb);            
        }
        if (dialog.justSendPlan)
            return;
        
        if (customManeuver) {
            sendAcoustically(dialog.selectedVehicle,
                    new TextMessage("neptus", "resume " + pt.getId() + " " + dialog.startingManeuver));
            return;
        }
        
        PlanControl pc = new PlanControl();
        pc.setType(PlanControl.TYPE.REQUEST);
        pc.setDst(ImcSystemsHolder.getSystemWithName(dialog.selectedVehicle).getId().intValue());
        pc.setSrc(ImcMsgManager.getManager().getLocalId().intValue());
        pc.setOp(PlanControl.OP.START);
        pc.setPlanId(dialog.planId);
        if (dialog.ignoreErrors)
            pc.setFlags(pc.getFlags() | PlanControl.FLG_IGNORE_ERRORS);
        if (dialog.skipCalibration)
            pc.setFlags(pc.getFlags() & ~PlanControl.FLG_CALIBRATE);

        sendAcoustically(dialog.selectedVehicle, pc);               
    }

    @NeptusMenuItem("Tools>Acoustic Operations>Send message...")
    public void sendMessage() {
        JDialog dialog = new JDialog(getConsole(), I18n.text("Send message acoustically"), true);
        dialog.setLayout(new BorderLayout());
        JButton btn = new JButton(I18n.text("Send"));
        editor = new ImcMessageSenderPanel(btn);
        addTemplates();
        dialog.getContentPane().add(editor, BorderLayout.CENTER);
//        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendAcoustically(getConsole().getMainSystem(),editor.getMessage() );
                dialog.dispose();
                dialog.setVisible(false);
            }
        });
//        bottom.add(btn);
//        dialog.getContentPane().add(bottom, BorderLayout.SOUTH);
        dialog.setSize(600, 500);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        GuiUtils.centerParent(dialog, getConsole());
        dialog.setVisible(true);
    }

    @NeptusMenuItem("Tools>Acoustic Operations>Reverse Range")
    public void reverseRange() {
        ImcSystem[] sysLst = gateways();
        
        if (sysLst.length == 0) {
            post(Notification
                    .error(I18n.text("Reverse range"),
                            I18n.text("No acoustic device  could be found."))
                    .src(I18n.text("Console")));
            return;
        }
        
        AcousticOperation op = new AcousticOperation();
        op.setOp(AcousticOperation.OP.REVERSE_RANGE);
        op.setSystem(getConsole().getMainSystem());

        int successCount = 0;
        for (ImcSystem sys : sysLst)
            if (ImcMsgManager.getManager().sendMessage(op.cloneMessage(), sys.getId(), null))
                successCount++;

        if (successCount > 0) {
            bottomPane.setText(
                    I18n.textf("Request for reverse range to %systemName triggered via %systemCount acoustic gateways",
                            getConsole().getMainSystem(), successCount));
        }
        else {
            post(Notification
                    .error(I18n.text("Reverse Range"),
                            I18n.textf("Unable to trigger reverse range of %systemName", getConsole().getMainSystem()))
                    .src(I18n.text("Console")));
        }
    }


    private boolean sendAcoustically(String destination, IMCMessage msg) {
        try {
            IMCSendMessageUtils.sendMessageAcoustically(msg, destination)
                    .forEach(t -> transmissions.put(t.getReqId(), t));
            return true;
        }
        catch (Exception ex) {
            post(Notification.error(I18n.text("Acoustic Operations"), "Error starting plan: "+ex.getMessage()));
            return false;
        }               
    }

    @Periodic(millisBetweenUpdates = 1500)
    public void updateStateLabel() {
        if (!lblState.isVisible())
            return;
        lblState.setText(buildState());
    }

    private String buildState() {
        if (gateway == null || gateway.equals(I18n.text("any")))
            return "<html><h1>" + I18n.text("Please select a gateway") + "</h1></html>";
        ImcSystemState state = ImcMsgManager.getManager().getState(gateway);
        StringBuilder html = new StringBuilder("<html>");
        html.append("<h1>" + I18n.textf("%gateway state", gateway) + "</h1>");
        html.append("<blockquote><ul>\n");
        try {
            RSSI iridiumRSSI = state.last(RSSI.class, "Iridium Modem"); // Check if works in I18n
            html.append("<li>" + I18n.textf("Iridium RSSI: %d  &#37;", iridiumRSSI.getValue()) + "</li>\n");
        }
        catch (Exception e) {
        }

        try {
            GpsFix gpsFix = state.last(GpsFix.class);
            html.append("<li>" + I18n.textf("GPS satellites: %d", gpsFix.getSatellites()) + "</li>\n");
        }
        catch (Exception e) {
        }

        try {
            StorageUsage storageUsage = state.last(StorageUsage.class);
            html.append("<li>" + I18n.textf("Storage Usage: %d  &#37;", storageUsage.getValue()) + "</li>\n");
        }
        catch (Exception e) {
        }

        try {
            Voltage voltage = state.last(Voltage.class, "Main Board"); // Check if works in I18n
            html.append("<li>" + I18n.textf("Voltage: %d V", voltage.getValue()) + "</li>\n");
        }
        catch (Exception e) {
        }

        html.append("</html>");
        return html.toString();
    }

    @Override
    public void initSubPanel() {
        if (initialized)
            return;
        initialized = true;

        Vector<ILayerPainter> renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
        for (ILayerPainter str2d : renderers) {
            str2d.addPostRenderPainter(this, this.getClass().getSimpleName());
        }

        getConsole().getImcMsgManager().addListener(this);

        JPanel ctrlPanel = new JPanel();
        ctrlPanel.setLayout(new GridLayout(0, 1, 2, 2));

        JButton btn = new JButton(I18n.textf("GW: %gateway", gateway));
        btn.setActionCommand("gw");
        cmdButtons.put("gw", btn);

        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Vector<Object> systems = new Vector<>();
                systems.add(I18n.text("any"));
                systems.addAll(Arrays.asList(
                        ImcSystemsHolder.lookupSystemByService("acoustic/operation", SystemTypeEnum.ALL, true)));

                Object[] choices = systems.toArray();

                if (choices.length == 0) {
                    GuiUtils.errorMessage(getConsole(), I18n.text("Select acoustic gateway"),
                            I18n.text("No acoustic gateways have been discovered in the network"));
                    return;
                }

                Object gw = JOptionPane.showInputDialog(getConsole(), I18n.text("Select gateway"),
                        I18n.text("Select acoustic gateway to use"), JOptionPane.QUESTION_MESSAGE, null, choices,
                        choices[0]);

                if (gw != null)
                    gateway = "" + gw;

                ((JButton) event.getSource()).setText(I18n.textf("GW: %gateway", gateway));
                lblState.setText(buildState());
            }
        });
        ctrlPanel.add(btn);

        final JButton btnR = new JButton(I18n.text("Range system"));
        btnR.setActionCommand("range");
        cmdButtons.put("range", btnR);
        btnR.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                ImcSystem[] sysLst;

                if (gateway.equals(I18n.text("any")))
                    sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation", SystemTypeEnum.ALL, true);
                else {
                    ImcSystem sys = ImcSystemsHolder.lookupSystemByName(gateway);
                    if (sys != null)
                        sysLst = new ImcSystem[] { sys };
                    else
                        sysLst = new ImcSystem[] {};
                }

                if (sysLst.length == 0) {
                    post(Notification
                            .error(I18n.text("Range System"),
                                    I18n.text("No acoustic device is capable of sending this request"))
                            .src(I18n.text("Console")));
                }

                if (selectedSystem == null) {
                    bottomPane.setText(I18n.textf("Please select a system.", selectedSystem));
                }
                else {
                    IMCMessage m = IMCDefinition.getInstance().create("AcousticOperation", "op", "RANGE", "system",
                            selectedSystem);

                    btnR.setEnabled(false);
                    SwingWorker<Integer, Void> sWorker = new SwingWorker<Integer, Void>() {
                        @Override
                        protected Integer doInBackground() throws Exception {
                            int successCount = 0;
                            for (ImcSystem sys : sysLst) {
                                if (ImcMsgManager.getManager().sendMessage(m.cloneMessage(), sys.getId(), null))
                                    successCount++;
                                if (separateRangingForAnyGateway && sysLst.length > 1) {
                                    try {
                                        Thread.sleep(separateRangingForAnyGatewaySeconds * 1000);
                                    }
                                    catch (Exception e) {
                                        NeptusLog.pub().warn(e);
                                    }
                                }
                            }
                            return successCount;
                        }
                        @Override
                        protected void done() {
                            int successCount = 0;
                            try {
                                successCount = get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }

                            if (successCount > 0) {
                                bottomPane.setText(I18n.textf("Range %systemName commanded to %systemCount systems",
                                        selectedSystem, successCount));
                            }
                            else {
                                post(Notification.error(I18n.text("Range System"), I18n.text("Unable to range selected system"))
                                        .src(I18n.text("Console")));
                            }

                            btnR.setEnabled(true);
                        }
                    };
                    sWorker.execute();
                }
            }
        });
        ctrlPanel.add(btnR);

        btn = new JButton(I18n.text("Send Message"));
        btn.setActionCommand("text");
        cmdButtons.put("text", btn);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JDialog dialog = new JDialog(getConsole(), I18n.text("Send message acoustically"));
                dialog.setLayout(new BorderLayout());
                JButton btn = new JButton(I18n.text("Send"));
                editor = new ImcMessageSenderPanel(btn);
                addTemplates();
                dialog.getContentPane().add(editor, BorderLayout.CENTER);
                //JPanel bottom = new JPanel(new FlowLayout(FlowLayout.TRAILING));
                btn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        sendAcoustically(selectedSystem, editor.getMessage());
                    }
                });
//                bottom.add(btn);
//                dialog.getContentPane().add(bottom, BorderLayout.SOUTH);
                dialog.setSize(600, 500);
                dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                GuiUtils.centerParent(dialog, getConsole());
                dialog.setVisible(true);
            }
        });
        ctrlPanel.add(btn);

        btn = new JButton(I18n.text("Abort"));
        btn.setBackground(Color.red);
        btn.setActionCommand("abort");
        cmdButtons.put("abort", btn);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                
                ImcSystem[] sysLst;

                if (gateway.equals(I18n.text("any"))) {
                    sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation", SystemTypeEnum.ALL, true);
                }
                else {
                    ImcSystem sys = ImcSystemsHolder.lookupSystemByName(gateway);
                    if (sys != null)
                        sysLst = new ImcSystem[] { sys };
                    else
                        sysLst = new ImcSystem[] {};
                }
                
                AcousticOperation m = new AcousticOperation(AcousticOperation.OP.ABORT, selectedSystem, 0, null);
                
                int successCount = 0;
                for (ImcSystem sys : sysLst)
                    if (ImcMsgManager.getManager().sendMessage(m, sys.getId(), null))
                        successCount++;

                if (successCount > 0) {
                    bottomPane.setText(I18n.textf("Abort %systemName commanded to %systemCount systems", selectedSystem,
                            successCount));
                }
                else {
                    post(Notification.error(I18n.text("Abort"), I18n.text("Unable to abort selected system"))
                            .src(I18n.text("Console")));
                }
            }
        });
        ctrlPanel.add(btn);

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                rangeDistances.clear();
                rangeSources.clear();
            }
        });
        ctrlPanel.add(clearButton);

        showRangesCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showRanges = ((JCheckBox) event.getSource()).isSelected();
            }
        });
        showRangesCheckBox.setSelected(showRanges);
        ctrlPanel.add(showRangesCheckBox);

        listPanel.setBackground(Color.white);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
        JSplitPane split1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(listPanel), ctrlPanel);
        split1.setDividerLocation(180);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(I18n.text("Acoustic Operations"), split1);
        tabs.addTab(I18n.text("Gateway state"), lblState);
        bottomPane.setEditable(false);
        bottomPane.setBackground(Color.white);
        JSplitPane split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, new JScrollPane(bottomPane));
        split2.setDividerLocation(150);
        setLayout(new BorderLayout());
        add(split2, BorderLayout.CENTER);
        propertiesChanged();
    }

    @Override
    public void propertiesChanged() {
        for (JRadioButton r : radioButtons.values()) {
            r.setVisible(false);
            group.remove(r);
        }
        radioButtons.clear();

        for (String s : sysListing.split(",")) {
            if (!s.isEmpty() && !s.endsWith(" list"))
                knownSystems.add(s.trim());
        }

        ArrayList<String> systems = new ArrayList<String>(knownSystems);
        Collections.sort(systems);

        for (String s : systems) {
            JRadioButton btn = new JRadioButton(s);
            btn.setActionCommand(s);
            group.add(btn);
            radioButtons.put(s, btn);
            btn.setBackground(Color.white);
            btn.addActionListener(systemActionListener);
            listPanel.add(btn);
        }

        if (radioButtons.containsKey(selectedSystem)) {
            radioButtons.get(selectedSystem).setSelected(true);
        }
        else {
            selectedSystem = null;
        }
    }

    protected void updateButtons() {
        if (selectedSystem == null)
            for (String key : cmdButtons.keySet())
                cmdButtons.get(key).setEnabled(false);

        else {
            for (String key : cmdButtons.keySet())
                cmdButtons.get(key).setEnabled(true);

            if (selectedSystem.startsWith("lsts"))
                cmdButtons.get("abort").setEnabled(false);
        }
    }    

    public void addText(String text) {
        bottomPane.setText(bottomPane.getText() + " \n" + text);
        bottomPane.scrollRectToVisible(new Rectangle(0, bottomPane.getHeight() + 22, 1, 1));
    }
    
    @Subscribe
    public void on(TransmissionStatus msg) {
        if (transmissions.containsKey(msg.getReqId())) {
            addText("["+msg.getReqId()+"] "+msg.getStatusStr()+" "+msg.getInfo());
            
            switch (msg.getStatus()) {
                case RANGE_RECEIVED:
                    LocationType loc = new LocationType(MyState.getLocation());
                    if (ImcSystemsHolder.getSystemWithName(msg.getSourceName()) != null)
                        loc = ImcSystemsHolder.getSystemWithName(msg.getSourceName()).getLocation();

                    rangeDistances.add(msg.getRange());
                    rangeSources.add(loc);

                    addText(I18n.textf("Distance to %systemName is %distance", "unknown",
                            GuiUtils.getNeptusDecimalFormat(1).format(msg.getRange())));
                    break;
                case DELIVERED:
                case MAYBE_DELIVERED:
                case SENT:
                    post(Notification.success("Acoustic Operations", "Request "+msg.getReqId()+" was "+msg.getStatusStr().replaceAll("_", " ").toLowerCase()));
                    break;
                case INPUT_FAILURE:
                case PERMANENT_FAILURE:
                case TEMPORARY_FAILURE:
                    post(Notification.error("Acoustic Operations", msg.getInfo()));
                default:
                    break;
            }
        }        
    }

    @Subscribe
    public void on(PlanControl msg) {
        if (pendingRequests.containsKey(msg.getRequestId())) {
            PlanControl request = pendingRequests.get(msg.getRequestId());
            String text = I18n.textf("Request %d completed successfully.", msg.getRequestId());
            String src = ImcSystemsHolder.translateImcIdToSystemName(msg.getSrc());

            if (request != null) {
                switch (request.getOp()) {
                    case START:
                        text = I18n.textf("Starting of %plan was acknowledged by %system.", request.getPlanId(), src);
                        break;
                    case STOP:
                        text = I18n.textf("Stopping of %plan was acknowledged by %system.", request.getPlanId(), src);
                        break;
                    default:
                        break;
                }
            }

            post(Notification.success(I18n.text("Manta Operations"), text));
        }
    }

    @Subscribe
    public void on(AcousticOperation msg) {
        switch (msg.getOp()) {
            case RANGE_RECVED:
                LocationType loc = new LocationType(MyState.getLocation());
                if (ImcSystemsHolder.getSystemWithName(msg.getSourceName()) != null)
                    loc = ImcSystemsHolder.getSystemWithName(msg.getSourceName()).getLocation();

                rangeDistances.add(msg.getRange());
                rangeSources.add(loc);

                addText(I18n.textf("Distance to %systemName is %distance", msg.getSystem().toString(),
                        GuiUtils.getNeptusDecimalFormat(1).format(msg.getRange())));
                break;
            case ABORT_ACKED:
                addText(I18n.textf("%systemName has acknowledged abort command", msg.getSystem().toString()));
                break;
            case BUSY:
                addText(I18n.textf("%manta is busy. Try again in a few moments", msg.getSourceName()));
                break;
            case NO_TXD:
                addText(I18n.textf("%manta has no acoustic transducer connected. Connect a transducer.",
                        msg.getSourceName()));
                break;
            case ABORT_IP:
                addText(I18n.textf("Aborting %systemName acoustically (via %manta)...", msg.getSystem().toString(),
                        msg.getSourceName()));
                break;
            case ABORT_TIMEOUT:
                addText(I18n.textf("%manta timed out while trying to abort %systemName", msg.getSourceName(),
                        msg.getSystem().toString()));
                break;
            case MSG_DONE:
                addText(I18n.textf("Message to %systemName has been sent successfully.", msg.getSystem().toString()));
                break;
            case MSG_FAILURE:
                addText(I18n.textf("Failed to send message to %systemName.", msg.getSystem().toString()));
                break;
            case MSG_IP:
                addText(I18n.textf("Sending message to %systemName...", msg.getSystem().toString()));
                break;
            case MSG_QUEUED:
                addText(I18n.textf("Message to %systemName has been queued in %manta.", msg.getSystem().toString(),
                        msg.getSourceName()));
                break;
            case RANGE_IP:
                addText(I18n.textf("Ranging of %systemName is in progress...", msg.getSystem().toString()));
                break;
            case RANGE_TIMEOUT:
                addText(I18n.textf("Ranging of %systemName timed out.", msg.getSystem().toString()));
                break;
            case UNSUPPORTED:
                addText(I18n.textf("The command is not supported by %manta.", msg.getSourceName()));
                break;
            default:
                addText(I18n.textf("[%manta]: %status", msg.getSourceName(), msg.getOp().toString()));
                break;
        }
    }

    @Subscribe
    public void on(AcousticSystems systems) {
        String acSystems = systems.getString("list", false);
        boolean newSystem = false;

        for (String s : acSystems.split(","))
            newSystem |= knownSystems.add(s);

        if (newSystem)
            propertiesChanged();
    }

    private ImcSystem[] gateways() {
        ImcSystem[] sysLst = null;
        if (gateway.equals(I18n.text("any")))
            sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation", SystemTypeEnum.ALL, true);
        else {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(gateway);
            if (sys != null)
                sysLst = new ImcSystem[] { sys };
            else
                sysLst = new ImcSystem[] {};
        }
        return sysLst;
    }

    @Periodic(millisBetweenUpdates = 120000)
    public void requestSysListing() {
        if (sysDiscovery) {
            AcousticSystemsQuery asq = new AcousticSystemsQuery();
            for (ImcSystem s : gateways())
                send(s.getName(), asq);
        }
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!showRanges)
            return;

        for (int i = 0; i < rangeSources.size(); i++) {
            double radius = rangeDistances.get(i) * renderer.getZoom();
            Point2D pt = renderer.getScreenPosition(rangeSources.get(i));

            if (i < rangeSources.size() - 1)
                g.setColor(new Color(255, 128, 0, 128));
            else
                g.setColor(new Color(255, 128, 0, 255));

            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(pt.getX() - radius, pt.getY() - radius, radius * 2, radius * 2));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        getConsole().getImcMsgManager().removeListener(this);       
    }

    public static void main(String[] args) {
        ConsoleParse.testSubPanel(MantaOperations.class);
    }
}
