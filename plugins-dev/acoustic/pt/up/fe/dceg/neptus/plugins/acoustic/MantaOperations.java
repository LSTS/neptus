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
 * Author: José Pinto
 * Jul 21, 2011
 */
package pt.up.fe.dceg.neptus.plugins.acoustic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.notifications.Notification;
import pt.up.fe.dceg.neptus.gui.VehicleChooser;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.AcousticOperation;
import pt.up.fe.dceg.neptus.imc.AcousticSystems;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.PlanControl;
import pt.up.fe.dceg.neptus.mystate.MyState;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.ILayerPainter;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.ConsoleParse;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 * 
 */
@PluginDescription(name = "Manta Operations", author = "ZP")
@LayerPriority(priority = 40)
public class MantaOperations extends SimpleSubPanel implements ConfigurationListener, Renderer2DPainter {

    private static final long serialVersionUID = 1L;

    protected static JDialog visibleDialog = null;
    protected LinkedHashMap<String, JRadioButton> radioButtons = new LinkedHashMap<String, JRadioButton>();
    protected LinkedHashMap<String, JButton> cmdButtons = new LinkedHashMap<String, JButton>();

    protected ButtonGroup group = new ButtonGroup();
    protected JPanel listPanel = new JPanel();
    protected JTextArea bottomPane = new JTextArea();
    protected JToggleButton toggle;
    protected String selectedSystem = null;

    @NeptusProperty(name = "Systems listing", description = "Use commas to separate system identifiers")
    public String sysListing = "benthos-1,benthos-2,benthos-3,benthos-4,lauv-seacon-1,lauv-seacon-2,lauv-seacon-3,lauv-seacon-4,lauv-xtreme-2,lauv-noptilus-1";

    public HashSet<String> knownSystems = new HashSet<>();

    @NeptusProperty(name = "Display ranges in the map")
    public boolean showRanges = true;

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
                    if (planId.length() == 1)
                        filtered.add(planId);

                if (filtered.isEmpty()) {
                    // GuiUtils.errorMessage(I18n.text("Send Plan acoustically"),
                    // I18n.text("Plans started acoustically cannot have an ID bigger than 1 character"));
                    post(Notification.error(I18n.text("Send Plan acoustically"),
                            I18n.text("Plans started acoustically cannot have an ID bigger than 1 character"))
                            .src(I18n.text("Console")));
                    return;
                }

                VehicleType choice = VehicleChooser.showVehicleDialog(VehiclesHolder
                        .getVehicleById(defaultVehicle));
                if (choice == null)
                    return;
                String[] ops = filtered.toArray(new String[0]);
                int option = JOptionPane.showOptionDialog(getConsole(),
                        I18n.text("Please select plan to start"), I18n.text("Start plan"),
                        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, ops, null);

                if (option == -1)
                    return;
                System.err.println("start plan " + ops[option]);

                ImcSystem[] sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation",
                        SystemTypeEnum.ALL, true);

                if (sysLst.length == 0) {
                    // GuiUtils.errorMessage(getConsole(), I18n.text("Range system"),
                    // I18n.text("No acoustic device is capable of sending this request"));
                    post(Notification.error(I18n.text("Range System"),
                            I18n.text("No acoustic device is capable of sending this request")).src(
                                    I18n.text("Console")));
                    return;
                }

                PlanControl pc = new PlanControl();
                pc.setType(PlanControl.TYPE.REQUEST);
                pc.setOp(PlanControl.OP.START);
                pc.setPlanId(ops[option]);

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
        addMenuItem(I18n.text("Advanced") + ">" + I18n.text("Manta Operations"),
                ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())), new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (visibleDialog != null) {
                    GuiUtils.centerOnScreen(visibleDialog);
                    visibleDialog.toFront();
                }
                JPanel ctrlPanel = new JPanel();
                ctrlPanel.setLayout(new BoxLayout(ctrlPanel, BoxLayout.PAGE_AXIS));

                JButton btn = new JButton(I18n.text("range"));
                btn.setActionCommand("range");
                cmdButtons.put("range", btn);
                btn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        ImcSystem[] sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation",
                                SystemTypeEnum.ALL, true);

                        if (sysLst.length == 0) {
                            // GuiUtils.errorMessage(getConsole(), I18n.text("Range system"),
                            // I18n.text("No acoustic device is capable of sending this request"));
                            post(Notification.error(I18n.text("Range System"),
                                    I18n.text("No acoustic device is capable of sending this request")).src(
                                            I18n.text("Console")));
                        }

                        IMCMessage m = IMCDefinition.getInstance().create("AcousticOperation", "op", "RANGE",
                                "system", selectedSystem);

                        int successCount = 0;
                        for (ImcSystem sys : sysLst)
                            if (ImcMsgManager.getManager().sendMessage(m, sys.getId(), null))
                                successCount++;

                        if (successCount > 0) {
                            bottomPane.setText(I18n.textf(
                                    "Range %systemName commanded to %systemCount systems", selectedSystem,
                                    successCount));
                            // bottomPane.setCaretPosition(bottomPane.getText().length());
                            // ((JScrollPane)bottomPane.getParent()).scrollRectToVisible(bottomPane.getBounds());
                        }
                        else {
                            // GuiUtils.errorMessage(getConsole(), I18n.text("Range system"),
                            // I18n.text("Unable to range selected system"));
                            post(Notification.error(I18n.text("Range System"),
                                    I18n.text("Unable to range selected system")).src(I18n.text("Console")));

                        }
                    }
                });
                ctrlPanel.add(btn);

                toggle = new JToggleButton(I18n.text("show ranges"));
                toggle.addActionListener(new ActionListener() {
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

                btn = new JButton(I18n.text("abort"));
                btn.setActionCommand("abort");
                cmdButtons.put("abort", btn);

                btn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        ImcSystem[] sysLst = ImcSystemsHolder.lookupSystemByService("acoustic/operation",
                                SystemTypeEnum.ALL, true);

                        if (sysLst.length == 0) {
                            // GuiUtils.errorMessage(getConsole(), I18n.text("Abort execution"),
                            // I18n.text("No acoustic device is capable of sending this request"));
                            post(Notification.error(I18n.text("Abort execution"),
                                    I18n.text("No acoustic device is capable of sending this request")).src(
                                            I18n.text("Console")));
                        }
                        IMCMessage m = IMCDefinition.getInstance().create("AcousticOperation", "op", "ABORT",
                                "system", selectedSystem);

                        int successCount = 0;

                        for (ImcSystem sys : sysLst) {
                            if (ImcMsgManager.getManager().sendMessage(m, sys.getId(), null)) {
                                successCount++;
                                NeptusLog.pub().error("Acoustic Abort sent through " + sys.getName());
                            }
                        }
                        if (successCount > 0) {
                            bottomPane.setText(I18n.textf("Abort %system sent to %sucCount systems", selectedSystem, successCount));
                        }
                        else {
                            // GuiUtils.errorMessage(getConsole(), I18n.text("Abort System"),
                            // I18n.text("Unable to abort selected system"));
                            post(Notification.error(I18n.text("Abort System"),
                                    I18n.text("Unable to abort selected system")).src(I18n.text("Console")));
                        }
                    }
                });
                ctrlPanel.add(btn);

                btn = new JButton(I18n.text("clear text"));
                btn.setActionCommand("clear");
                cmdButtons.put("clear", btn);

                btn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        bottomPane.setText("");
                    }
                });
                ctrlPanel.add(btn);

                listPanel.setBackground(Color.white);
                listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
                JSplitPane split1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(listPanel),
                        ctrlPanel);
                split1.setDividerLocation(180);
                bottomPane.setEditable(false);
                bottomPane.setBackground(Color.white);
                JSplitPane split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, split1, new JScrollPane(bottomPane));
                split2.setDividerLocation(150);

                JDialog dialog = new JDialog(getConsole(), I18n.text("Manta Operations"));
                dialog.setContentPane(split2);
                dialog.setSize(320, 240);
                dialog.setAlwaysOnTop(true);
                GuiUtils.centerOnScreen(dialog);
                dialog.setVisible(true);
                visibleDialog = dialog;
                dialog.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        if (visibleDialog != null)
                            visibleDialog.dispose();
                        visibleDialog = null;
                    }
                });
            }
        });

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
                addText(I18n.textf("%systemName has acknowledged the abort command", msg.getSystem().toString()));
                break;
            case BUSY:
                addText(I18n.textf("%manta is busy. Try again in a few moments", msg.getSourceName()));
                break;
            case NO_TXD:
                addText(I18n.textf("%manta has no acoustic transducer connected. Connect a transducer.", msg.getSourceName()));
                break;
            case ABORT_IP:
                addText(I18n.textf("Aborting of %sysname is in progress...",  msg.getSystem().toString()));
                break;
            case ABORT_TIMEOUT:
                addText(I18n.textf("Aborting of %sysname timed out.", msg.getSystem().toString()));
                break;
            case MSG_DONE:
                addText(I18n.textf("Message to %sysname has been sent successfully.", msg.getSystem().toString()));
                break;
            case MSG_FAILURE:
                addText(I18n.textf("Failed to send message to %sysname.", msg.getSystem().toString()));
                break;
            case MSG_IP:
                addText(I18n.textf("Sending message to %sysname...", msg.getSystem().toString()));
                break;
            case MSG_QUEUED:
                addText(I18n.textf("Message to %sysname has been queued in %manta.", msg.getSystem().toString(), msg.getSourceName()));
                break;
            case RANGE_IP:
                addText(I18n.textf("Ranging of %sysname is in progress...", msg.getSystem().toString()));
                break;
            case RANGE_TIMEOUT:
                addText(I18n.textf("Ranging of %sysname timed out.", msg.getSystem().toString()));
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
        for (String s : acSystems.split(","))
            knownSystems.add(s);
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
                g.setColor(new Color(255,128,0,0));
            
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Double(pt.getX() - radius, pt.getY() - radius, radius * 2, radius * 2));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        ImcMsgManager.getManager().removeListener(this);
    }

    public static void main(String[] args) {
        ConsoleParse.testSubPanel(MantaOperations.class);
    }
}
