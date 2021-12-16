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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Mar 12, 2014
 */
package pt.lsts.neptus.plugins.txtcmd;

import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.google.common.eventbus.Subscribe;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertySheet;
import org.reflections.Reflections;

import com.jogamp.newt.event.KeyEvent;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

import pt.lsts.imc.TextMessage;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.tablelayout.TableLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.cmdsenders.AcousticModemSender;
import pt.lsts.neptus.plugins.cmdsenders.ITextMsgSender;
import pt.lsts.neptus.plugins.cmdsenders.IridiumSender;
import pt.lsts.neptus.plugins.cmdsenders.SmsSender;
import pt.lsts.neptus.plugins.cmdsenders.WiFiSender;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="Text Commands")
@Popup(name="Text Commands", accelerator=KeyEvent.VK_F2, height=500, width=500, pos=POSITION.CENTER)
public class TextCommands extends ConsolePanel {
    private static final long serialVersionUID = 2849289726554619882L;

    @NeptusProperty(name = "Maximum Number of Chars in Receive Box", userLevel = NeptusProperty.LEVEL.ADVANCED)
    private int maxRecvChars = 1000;

    @NeptusProperty(name = "Filter Receiving With Prefix",
            userLevel = NeptusProperty.LEVEL.REGULAR,
            description = "Filter out received text messages to shoe. Leave empty for no filter.")
    private String filterRecWithPrefix = "";

    @NeptusProperty(name = "Post Notification With Human Intervention",
            userLevel = NeptusProperty.LEVEL.REGULAR,
            description = "If true user must click the notification popup.")
    public boolean postWithHumanIntervention = false;

    private SimpleDateFormat dateFormater = new SimpleDateFormat("HH:mm:ss");

    private JLabel lblCmd = new JLabel(I18n.text("Command")+":");
    private JLabel lblMean = new JLabel(I18n.text("Comm. Mean")+":");
    private JTextArea txtResult = new JTextArea();

    private JTextArea recvBox = new JTextArea();

    private JComboBox<String> comboCmd = null;
    private JComboBox<String> comboMean = null;
    private LinkedHashMap<String, ITextCommand> commands = new LinkedHashMap<>();
    private PropertySheetPanel propsSettingsTable = new PropertySheetPanel();
    private PropertySheetPanel propsTable = new PropertySheetPanel();

    private WiFiSender wifiSender = new WiFiSender();
    private IridiumSender iridiumSender = new IridiumSender();
    private SmsSender smsSender = new SmsSender();
    private AcousticModemSender acousticSender = new AcousticModemSender();

    public TextCommands(ConsoleLayout console) {
        super(console);
        setLayout(new TableLayout(new double[] {100, TableLayout.FILL},
                new double[] {24,24,60,TableLayout.FILL,60,32,90,32}));

        for (String pkg : new String[] {getClass().getPackage().getName()}) {
            Reflections reflections = new Reflections(pkg);
            for (Class<?> c : reflections.getSubTypesOf(ITextCommand.class)) {
                if (!Modifier.isAbstract(c.getModifiers())) {
                    try {
                        ITextCommand cmd = (ITextCommand)c.getDeclaredConstructor().newInstance();
                        StateRenderer2D r2d = new StateRenderer2D(MapGroup.getMapGroupInstance(getConsole().getMission()));
                        cmd.setCenter(r2d.getCenter());
                        commands.put(cmd.getCommand(), cmd);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        ArrayList<String> cmdList = new ArrayList<>();
        cmdList.addAll(commands.keySet());
        Collections.sort(cmdList);
        comboCmd = new JComboBox<>(cmdList.toArray(new String[0]));
        comboMean = new JComboBox<String>(new String[] {"Wi-Fi", "Iridium", "SMS", "Acoustic Modem"});
        add(lblCmd, "0,0");
        add(lblMean, "0,1");
        add(comboCmd, "1,0");
        add(comboMean, "1,1");

        propsSettingsTable.setEditorFactory(PropertiesEditor.getPropertyEditorRegistry());
        propsSettingsTable.setRendererFactory(PropertiesEditor.getPropertyRendererRegistry());
        propsSettingsTable.setToolBarVisible(false);
        propsSettingsTable.setDescriptionVisible(false);
        propsSettingsTable.setProperties(getProperties());
        propsSettingsTable.setMode( PropertySheet.VIEW_AS_FLAT_LIST);
        add(propsSettingsTable, "0,2 1,2");

        propsTable.setEditorFactory(PropertiesEditor.getPropertyEditorRegistry());
        propsTable.setRendererFactory(PropertiesEditor.getPropertyRendererRegistry());
        propsTable.setToolBarVisible(false);
        propsTable.setDescriptionVisible(true);
        propsTable.setMode( PropertySheet.VIEW_AS_CATEGORIES);
        add(propsTable, "0,3 1,3");

        txtResult.setEditable(false);
        txtResult.setLineWrap(true);
        txtResult.setWrapStyleWord(true);
        add(new JScrollPane(txtResult), "0,4 1,4");
        comboCmd.addActionListener(e -> {
            ITextCommand selection = commands.get(comboCmd.getSelectedItem());
            DefaultProperty[] cmdProps = selection.getProperties();
            for (int i = 0; i < cmdProps.length; i++) {
                cmdProps[i].setCategory(selection.getCommand());
            }
            propsTable.setProperties(cmdProps);
            parse();
        });

        propsTable.addPropertySheetChangeListener(evt -> parse());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 2));
        JButton btnSend = new JButton(I18n.text("Send"));
        JButton btnPreview = new JButton(I18n.text("Preview"));
        //JButton btnSettings = new JButton(I18n.text("Settings"));

        btnSend.addActionListener(e -> send());
        btnPreview.addActionListener(e -> preview());

        parse();      

        bottom.add(btnPreview);
        bottom.add(btnSend);
        //bottom.add(btnSettings);
        add(bottom, "0,5 1,5");

        recvBox.setEnabled(false);
        JScrollPane recvBoxScroll = new JScrollPane(recvBox);
        recvBoxScroll.setAutoscrolls(true);
        add(recvBoxScroll, "0,6 1,6");

        JPanel bottom2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 2));
        JButton clearCtlButton = new JButton(I18n.text("Clear"));
        clearCtlButton.addActionListener((e) -> {
            synchronized (recvBox) {
                recvBox.setText("");
            }
        });
        bottom2.add(clearCtlButton);
        add(bottom2, "0,7 1,7");
    }

    private void triggerGuiParamsSettingsUpdate() {
        ITextCommand selection = commands.get(comboCmd.getSelectedItem());
        propsSettingsTable.setProperties(TextCommands.this.getProperties());
    }

    @Override
    public void cleanSubPanel() {

    }

    public String parse() {
        this.setProperties(propsTable.getProperties());
        commands.get(comboCmd.getSelectedItem()).setProperties(propsTable.getProperties());
        String cmd = commands.get(comboCmd.getSelectedItem()).buildCommand();
        txtResult.setText(cmd);
        return cmd;
    }

    @Override
    public void setProperties(Property[] properties) {
        super.setProperties(properties);
        triggerGuiParamsSettingsUpdate();
    }

    @Override
    public void initSubPanel() {

    }

    private void preview() {
        parse();

        ITextCommand cmd = commands.get(comboCmd.getSelectedItem());
        PlanType pt = cmd.resultingPlan(getConsole().getMission());

        if (pt == null) {
            GuiUtils.errorMessage(I18n.text("Preview command"), I18n.textf("The command %cmd can not be previewed", cmd.getCommand()));
            return;
        }
        StateRenderer2D r2d = new StateRenderer2D(MapGroup.getMapGroupInstance(getConsole().getMission()));
        PlanElement planElem = new PlanElement(MapGroup.getMapGroupInstance(getConsole().getMission()), new MapType());
        planElem.setPlan(pt);
        planElem.setBeingEdited(true);
        planElem.setShowManNames(true);
        planElem.setShowDistances(true);
        planElem.setShowVelocities(true);
        planElem.setRenderer(r2d);
        planElem.setTransp2d(1.0);
        r2d.addPostRenderPainter(planElem, "Plan");
        JDialog dialog = new JDialog(getConsole());
        GuiUtils.centerOnScreen(dialog);
        dialog.getContentPane().add(r2d);
        dialog.setSize(600, 600);
        dialog.setModal(true);
        dialog.setTitle("Previewing "+cmd.getCommand()+" command");
        dialog.setVisible(true);
    }

    private void send() {
        ITextCommand cmd = commands.get(comboCmd.getSelectedItem());
        String command = cmd.buildCommand();
        ITextMsgSender sender = null;

        switch (comboMean.getSelectedItem().toString()) {
            case "SMS":
                sender = smsSender;
                break;
            case "Iridium":
                sender = iridiumSender;
                break;
            case "Acoustic Modem":
                sender = acousticSender;
                break;
            default:
                sender = wifiSender;
                break;
        }

        try {
            Future<String> result = sender.sendToVehicle("neptus", getConsole().getMainSystem(), command);
            GuiUtils.infoMessage(getConsole(), I18n.text("Send command"), result.get());
            PlanType pt = cmd.resultingPlan(getConsole().getMission());

            if (pt != null) {
                pt.setVehicle(getMainVehicleId());
                getConsole().getMission().addPlan(pt);
                getConsole().getMission().save(true);
                getConsole().warnMissionListeners();
                getConsole().setPlan(pt);
            }
        }
        catch (Exception e) {
            GuiUtils.errorMessage(getConsole(), e);
        }
    }

    private void updateRecvBoxTxt(String recMessage) {
        synchronized (recvBox) {
            recvBox.append(dateFormater.format(new Date()) + " | " + recMessage + "\n");
            String t = recvBox.getText();
            if (t.length() > maxRecvChars) {
                t = t.substring(Math.min(t.length(), t.length() - maxRecvChars), t.length());
                recvBox.setText(t);
                recvBox.setCaretPosition(t.length());
            }
            recvBox.scrollRectToVisible(new Rectangle(0, recvBox.getHeight() + 22, 1, 1));
        }
    }

    @Subscribe
    public void onTextMessage(TextMessage msg) {
        String origin = msg.getOrigin();
        String txt = msg.getText().trim();

        if (txt == null || txt.isEmpty())
            return;

        if ((filterRecWithPrefix != null || !filterRecWithPrefix.isEmpty())
                && !txt.trim().startsWith(filterRecWithPrefix.trim())) {
            return;
        }

        String recMessage = "REC>" + origin + ": " + txt;
        updateRecvBoxTxt(recMessage);

        post(Notification.info("Txt Message", recMessage).requireHumanAction(postWithHumanIntervention));
    }

    public static void main(String[] args) {
        GuiUtils.setLookAndFeel();
        TextCommands cmds = new TextCommands(null);
        cmds.initSubPanel();
        GuiUtils.testFrame(cmds);        
    }
}
