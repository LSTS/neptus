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
 * Jul 21, 2011
 */
package pt.lsts.neptus.plugins.acoustic;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Arrays;
import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

import pt.lsts.imc.AcousticOperation;
import pt.lsts.imc.AcousticSystems;
import pt.lsts.imc.AcousticSystemsQuery;
import pt.lsts.imc.GpsFix;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.RSSI;
import pt.lsts.imc.StorageUsage;
import pt.lsts.imc.TextMessage;
import pt.lsts.imc.Voltage;
import pt.lsts.imc.state.ImcSysState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.gui.VehicleChooser;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Acoustic Operations", author = "ZP", icon="pt/lsts/neptus/plugins/acoustic/manta.png")
@LayerPriority(priority = 40)
@Popup(name="Acoustic Operations", accelerator=KeyEvent.VK_M, width=350, height=250, pos=POSITION.BOTTOM_RIGHT, icon="pt/lsts/neptus/plugins/acoustic/manta.png")
public class MantaOperations extends ConsolePanel implements ConfigurationListener, Renderer2DPainter {

    private static final long serialVersionUID = 1L;

    protected static JDialog visibleDialog = null;
    protected LinkedHashMap<String, JRadioButton> radioButtons = new LinkedHashMap<String, JRadioButton>();
    protected LinkedHashMap<String, JButton> cmdButtons = new LinkedHashMap<String, JButton>();

    protected ButtonGroup group = new ButtonGroup();
    protected JPanel listPanel = new JPanel();
    protected JTextArea bottomPane = new JTextArea();
    protected JToggleButton toggle;
    protected String selectedSystem = null;
    protected String gateway = "any";
    protected JLabel lblState = new JLabel("<html><h1>Please select a gateway</h1>");
    
    protected LinkedHashMap<Integer, PlanControl> pendingRequests = new LinkedHashMap<>();

    @NeptusProperty(name = "Systems listing", description = "Use commas to separate system identifiers")
    public String sysListing = "benthos-1,benthos-2,benthos-3,benthos-4,lauv-xtreme-2,lauv-noptilus-1,lauv-noptilus-2,lauv-noptilus-3";

    public HashSet<String> knownSystems = new HashSet<>();

    @NeptusProperty(name = "Display ranges in the map")
    public boolean showRanges = true;

    @NeptusProperty(name = "Use system discovery", description = "Instead of a static list, receive supported systems from gateway")
    public boolean sysDiscovery = true;

    /**
     * @param console
     */
    public MantaOperations(ConsoleLayout console) {
        super(console);
    }

    protected ActionListener systemActionListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            selectedSystem = e.getActionCommand();
        }
    };

    protected boolean initialized = false;

    private boolean sendAcoustically(IMCMessage msg) {
        ImcSystem[] sysLst = gateways();

        if (sysLst.length == 0) {
            post(Notification.error(I18n.text("Send message"),
                    I18n.text("No acoustic device is capable of sending this message")).src(
                            I18n.text("Console")));
            return false;
        }
        
        AcousticOperation op = new AcousticOperation(AcousticOperation.OP.MSG, selectedSystem, 0, msg);

        int successCount = 0;
        for (ImcSystem sys : sysLst)
            if (ImcMsgManager.getManager().sendMessage(op, sys.getId(), null))
                successCount++;

        if (successCount > 0) {
            bottomPane.setText(I18n.textf(
                    "Message sent to %systemName via %systemCount acoustic gateways", selectedSystem,
                    successCount));
            
            return true;
        }
        else {
            post(Notification.error(I18n.text("Send message"),
                    I18n.text("Unable to send message to selected system")).src(I18n.text("Console")));
            return false;
        }
    }
    
    @Periodic(millisBetweenUpdates=1500) 
    public void updateStateLabel() {
        if (!lblState.isVisible())
            return;
        lblState.setText(buildState());
    }
    
    private String buildState() {
        if (gateway == null || gateway.equals("any"))
            return I18n.text("<html><h1>Please select a gateway</h1></html>");
        ImcSysState state = ImcMsgManager.getManager().getState(gateway);
        StringBuilder html = new StringBuilder("<html>");
        html.append(I18n.textf("<h1>%gateway state</h1>", gateway));
        html.append("<blockquote><ul>\n");
        try {
            RSSI iridiumRSSI = state.lastRSSI("Iridium Modem");    
            html.append(I18n.textf("<li>Iridium RSSI: %d  &#37;</li>\n", iridiumRSSI.getValue()));
        }
        catch (Exception e) {}
        
        try {
            GpsFix gpsFix = state.lastGpsFix();    
            html.append(I18n.textf("<li>GPS satellites: %d</li>\n", gpsFix.getSatellites()));
        }
        catch (Exception e) {}
        
        try {
            StorageUsage storageUsage = state.lastStorageUsage();
            html.append(I18n.textf("<li>Storage Usage: %d  &#37;</li>\n", storageUsage.getValue()));
        }
        catch (Exception e) {}
        
        try {
            Voltage voltage = state.lastVoltage("Main Board"); 
            html.append(I18n.textf("<li>Voltage: %d V</li>\n", voltage.getValue()));
        }
        catch (Exception e) {}

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

        addMenuItem(I18n.text("Tools") + ">" + I18n.text("Start Plan By Acoustic Modem"),
                ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String defaultVehicle = getConsole().getMainSystem();
                Set<String> plans = getConsole().getMission().getIndividualPlansList().keySet();
                Vector<String> filtered = new Vector<String>();
                for (String planId : plans)
                    filtered.add(planId);

                if (filtered.isEmpty()) {
                    post(Notification.error(I18n.text("Send Plan acoustically"),
                                            I18n.text("No plans to send"))
                         .src(I18n.text("Console")));
                    return;
                }

                VehicleType choice = VehicleChooser.showVehicleDialog(null, VehiclesHolder
                        .getVehicleById(defaultVehicle), null);
                if (choice == null)
                    return;
                String[] ops = filtered.toArray(new String[0]);
                int option = JOptionPane.showOptionDialog(getConsole(),
                        I18n.text("Please select plan to start"), I18n.text("Start plan"),
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, ops, null);

                if (option == -1)
                    return;
                NeptusLog.pub().warn("Start plan " + ops[option]);

                ImcSystem[] sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation",
                        SystemTypeEnum.ALL, true);

                if (sysLst.length == 0) {
                    post(Notification.error(I18n.text("Start Plan"),
                            I18n.text("No acoustic device is capable of sending this request")).src(
                                    I18n.text("Console")));
                    return;
                }

                PlanControl pc = new PlanControl();
                pc.setType(PlanControl.TYPE.REQUEST);
                pc.setOp(PlanControl.OP.START);
                pc.setPlanId(ops[option]);
                int req = IMCSendMessageUtils.getNextRequestId();
                pc.setRequestId(req);

                pendingRequests.put(req, pc);

                AcousticOperation aop = new AcousticOperation();
                aop.setOp(AcousticOperation.OP.MSG);
                aop.setSystem(choice.getId());
                aop.setMsg(pc);

                int successCount = 0;
                for (ImcSystem sys : sysLst)
                    if (ImcMsgManager.getManager().sendMessage(aop, sys.getId(), null))
                        successCount++;

                if (successCount == 0) {
                    // GuiUtils.errorMessage(getConsole(), I18n.text("Error sending start plan"),
                    // I18n.text("No system was able to send the message"));
                    post(Notification.error(I18n.text("Error sending start plan"),
                            I18n.text("No system was able to send the message")).src(I18n.text("Console")));
                }
            }
        });

        ImcMsgManager.getManager().addListener(this);
        
        JPanel ctrlPanel = new JPanel();
        //BoxLayout layout = new BoxLayout(ctrlPanel, BoxLayout.PAGE_AXIS);
        ctrlPanel.setLayout(new GridLayout(0, 1, 2, 2));

        JButton btn = new JButton(I18n.textf("GW: %s", gateway));
        btn.setActionCommand("gw");
        cmdButtons.put("gw", btn);

        btn.addActionListener(new ActionListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void actionPerformed(ActionEvent arg0) {
                Vector<Object> systems = new Vector<>();
                systems.add("any");
                systems.addAll(Arrays.asList(ImcSystemsHolder.lookupSystemByService("acoustic/operation",
                        SystemTypeEnum.ALL, true)));
                
                Object[] choices = systems.toArray();
                
                if (choices.length == 0) {
                    GuiUtils.errorMessage(getConsole(), "Select acoustic gateway", "No acoustic gateways have been discovered in the network");
                    return;
                }

                Object gw = JOptionPane.showInputDialog(getConsole(), "Select Gateway", "Select acoustic gateway to use",
                        JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);

                if (gw != null)
                    gateway = ""+gw;
                
                ((JButton)arg0.getSource()).setText(I18n.textf("GW: %s", gateway));
                lblState.setText(buildState());
            }
        });
        ctrlPanel.add(btn);


        btn = new JButton(I18n.text("Range System"));
        btn.setActionCommand("range");
        cmdButtons.put("range", btn);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {


                ImcSystem[] sysLst;

                if (gateway.equals("any"))                    
                    sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation",
                            SystemTypeEnum.ALL, true);
                else {
                    ImcSystem sys = ImcSystemsHolder.lookupSystemByName(gateway);
                    if (sys != null)
                        sysLst = new ImcSystem[]{sys};
                    else 
                        sysLst = new ImcSystem[]{};
                }

                if (sysLst.length == 0) {
                    post(Notification.error(I18n.text("Range System"),
                            I18n.text("No acoustic device is capable of sending this request")).src(
                                    I18n.text("Console")));
                }

                if (selectedSystem == null) {
                    bottomPane.setText(I18n.textf("Please select a system.", selectedSystem));
                }
                else {
                    IMCMessage m = IMCDefinition.getInstance().create("AcousticOperation", "op", "RANGE", "system",
                            selectedSystem);

                    int successCount = 0;
                    for (ImcSystem sys : sysLst)
                        if (ImcMsgManager.getManager().sendMessage(m, sys.getId(), null))
                            successCount++;

                    if (successCount > 0) {
                        bottomPane.setText(I18n.textf("Range %systemName commanded to %systemCount systems",
                                selectedSystem, successCount));
                    }
                    else {
                        post(Notification
                                .error(I18n.text("Range System"), I18n.text("Unable to range selected system")).src(
                                        I18n.text("Console")));
                    }
                }
            }
        });
        ctrlPanel.add(btn);

        toggle = new JToggleButton(I18n.text("Show Ranges"));
        toggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                showRanges = ((JToggleButton) arg0.getSource()).isSelected();
                if (!showRanges) {
                    rangeDistances.clear();
                    rangeSources.clear();
                }
            }
        });
        toggle.setSelected(showRanges);
        ctrlPanel.add(toggle);
        
        btn = new JButton(I18n.text("Send command"));
        btn.setActionCommand("text");
        cmdButtons.put("text", btn);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (selectedSystem == null)
                    return;
                String cmd = JOptionPane.showInputDialog(getConsole(), I18n.textf("Enter command to send to %vehicle", selectedSystem));
                if (cmd == null)
                    return;
                if (cmd.length() > 64) {
                    GuiUtils.errorMessage(getConsole(), I18n.text("Send command"), I18n.text("Cannot send command because it has more than 64 characters."));
                    return;
                }
                TextMessage msg = new TextMessage("", cmd);
                sendAcoustically(msg);
            }
        });
        ctrlPanel.add(btn);
        
        btn = new JButton(I18n.text("Abort"));
        //btn.setForeground(Color.red);
        btn.setBackground(Color.red);
        btn.setActionCommand("abort");
        cmdButtons.put("abort", btn);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {


                ImcSystem[] sysLst;

                if (gateway.equals("any"))                    
                    sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation",
                            SystemTypeEnum.ALL, true);
                else {
                    ImcSystem sys = ImcSystemsHolder.lookupSystemByName(gateway);
                    if (sys != null)
                        sysLst = new ImcSystem[]{sys};
                    else 
                        sysLst = new ImcSystem[]{};
                }

                if (sysLst.length == 0) {
                    post(Notification.error(I18n.text("Abort"),
                            I18n.text("No acoustic device is capable of sending this request")).src(
                                    I18n.text("Console")));
                }

                IMCMessage m = IMCDefinition.getInstance().create("AcousticOperation", "op", "ABORT",
                        "system", selectedSystem);

                int successCount = 0;
                for (ImcSystem sys : sysLst)
                    if (ImcMsgManager.getManager().sendMessage(m, sys.getId(), null))
                        successCount++;

                if (successCount > 0) {
                    bottomPane.setText(I18n.textf(
                            "Abort %systemName commanded to %systemCount systems", selectedSystem,
                            successCount));
                }
                else {
                    post(Notification.error(I18n.text("Abort"),
                            I18n.text("Unable to abort selected system")).src(I18n.text("Console")));
                }
            }
        });
        ctrlPanel.add(btn);

        listPanel.setBackground(Color.white);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
        JSplitPane split1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(listPanel),
                ctrlPanel);
        split1.setDividerLocation(180);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Acoustic Operations", split1);
        tabs.addTab("Gateway state", lblState);
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
        bottomPane.setText(bottomPane.getText() +" \n"+text);        
        bottomPane.scrollRectToVisible(new Rectangle(0, bottomPane.getHeight()+22, 1, 1) );
    }

    protected LinkedHashMap<String, LocationType> systemLocations = new LinkedHashMap<>();

    @Subscribe
    public void on(PlanControl msg) {
        if (pendingRequests.containsKey(msg.getRequestId())) {
            PlanControl request = pendingRequests.get(msg.getRequestId());
            String text = I18n.textf("Request %d completed successfully.", msg.getRequestId());
            String src = ImcSystemsHolder.translateImcIdToSystemName(msg.getSrc());

            if (request != null) {
                switch (request.getOp()) {
                    case START:
                        text = I18n.textf("Starting of %plan was acknowledged by %system.",
                                request.getPlanId(), src);
                        break;
                    case STOP:
                        text = I18n.textf("Stopping of %plan was acknowledged by %system.",
                                request.getPlanId(), src);
                        break;
                    default:
                        break;
                }
            }

            post(Notification.success("Manta Operations", text));    
        }
    }

    @Subscribe
    public void on(AcousticOperation msg) {



        switch (msg.getOp()) {
            case RANGE_RECVED:
                if (showRanges) {
                    LocationType loc = new LocationType(MyState.getLocation());
                    if (ImcSystemsHolder.getSystemWithName(msg.getSourceName()) != null)
                        loc = ImcSystemsHolder.getSystemWithName(msg.getSourceName()).getLocation();

                    rangeDistances.add(msg.getRange());
                    rangeSources.add(loc);
                    addText(I18n.textf("Distance to %systemName is %distance", msg.getSystem().toString(),
                            GuiUtils.getNeptusDecimalFormat(1).format(msg.getRange())));
                }
                break;
            case ABORT_ACKED:
                addText(I18n.textf("%systemName has acknowledged abort command", msg.getSystem().toString()));
                break;
            case BUSY:
                addText(I18n.textf("%manta is busy. Try again in a few moments", msg.getSourceName()));
                break;
            case NO_TXD:
                addText(I18n.textf("%manta has no acoustic transducer connected. Connect a transducer.", msg.getSourceName()));
                break;
            case ABORT_IP:
                addText(I18n.textf("Aborting %systemName acoustically (via %manta)...",  msg.getSystem().toString(), msg.getSourceName()));
                break;
            case ABORT_TIMEOUT:
                addText(I18n.textf("%manta timed out while trying to abort %systemName", msg.getSourceName(), msg.getSystem().toString()));
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
                addText(I18n.textf("Message to %systemName has been queued in %manta.", msg.getSystem().toString(), msg.getSourceName()));
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
        String acSystems = systems.getList();
        boolean newSystem = false;
        
        for (String s : acSystems.split(","))
            newSystem |= knownSystems.add(s);
        
        if (newSystem)
            propertiesChanged();
    }
    
    private ImcSystem[] gateways() {
        ImcSystem[] sysLst = null;
        if (gateway.equals("any"))                    
            sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation",
                    SystemTypeEnum.ALL, true);
        else {
            ImcSystem sys = ImcSystemsHolder.lookupSystemByName(gateway);
            if (sys != null)
                sysLst = new ImcSystem[]{sys};
            else 
                sysLst = new ImcSystem[]{};
        }
        return sysLst;

    }
    
    @Periodic(millisBetweenUpdates=120000)
    public void requestSysListing() {
        if (sysDiscovery) {
            AcousticSystemsQuery asq = new AcousticSystemsQuery();
            for (ImcSystem s : gateways())
                send(s.getName(), asq);            
        }
    }

    protected Vector<LocationType> rangeSources = new Vector<LocationType>();
    protected Vector<Double> rangeDistances = new Vector<Double>();

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        for (int i = 0; i < rangeSources.size(); i++) {
            double radius = rangeDistances.get(i) * renderer.getZoom();
            Point2D pt = renderer.getScreenPosition(rangeSources.get(i));

            if (i < rangeSources.size()-1)
                g.setColor(new Color(255,128,0,128));
            else
                g.setColor(new Color(255,128,0,255));

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
        ImcMsgManager.getManager().removeListener(this);
    }

    public static void main(String[] args) {
        ConsoleParse.testSubPanel(MantaOperations.class);
    }
}
