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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.Future;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.reflections.Reflections;

import com.jogamp.newt.event.KeyEvent;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.tablelayout.TableLayout;
import pt.lsts.neptus.i18n.I18n;
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
    private JLabel lblCmd = new JLabel(I18n.text("Command")+":");
    private JLabel lblMean = new JLabel(I18n.text("Comm. Mean")+":");
    private JTextArea txtResult = new JTextArea();

    private JComboBox<String> comboCmd = null;
    private JComboBox<String> comboMean = null;
    private LinkedHashMap<String, ITextCommand> commands = new LinkedHashMap<>();
    private PropertySheetPanel propsTable = new PropertySheetPanel();

    private WiFiSender wifiSender = new WiFiSender();
    private IridiumSender iridiumSender = new IridiumSender();
    private SmsSender smsSender = new SmsSender();
    private AcousticModemSender acousticSender = new AcousticModemSender();

    public TextCommands(ConsoleLayout console) {
        super(console);
        setLayout(new TableLayout(new double[] {100, TableLayout.FILL}, new double[] {24,24,TableLayout.FILL,38,32}));

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
        propsTable.setEditorFactory(PropertiesEditor.getPropertyEditorRegistry());    
        propsTable.setRendererFactory(PropertiesEditor.getPropertyRendererRegistry());    
        propsTable.setToolBarVisible(false);
        add(propsTable, "0,2 1,2");
        txtResult.setEditable(false);
        add(new JScrollPane(txtResult), "0,3 1,3");
        comboCmd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ITextCommand selection = commands.get(comboCmd.getSelectedItem());
                propsTable.setProperties(selection.getProperties());
                parse();
            }
        });

        propsTable.addPropertySheetChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                parse();
            }
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 2));
        JButton btnSend = new JButton(I18n.text("Send"));
        JButton btnPreview = new JButton(I18n.text("Preview"));
        //JButton btnSettings = new JButton(I18n.text("Settings"));

        btnSend.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                send();
            }
        });
        btnPreview.addActionListener(new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                preview();
            }
        });

        parse();      

        bottom.add(btnPreview);
        bottom.add(btnSend);
        //bottom.add(btnSettings);
        add(bottom, "0,4 1,4");
    }

    @Override
    public void cleanSubPanel() {

    }

    public String parse() {
        commands.get(comboCmd.getSelectedItem()).setProperties(propsTable.getProperties());
        String cmd = commands.get(comboCmd.getSelectedItem()).buildCommand();
        txtResult.setText(cmd);
        return cmd;
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

    public static void main(String[] args) {
        GuiUtils.setLookAndFeel();
        TextCommands cmds = new TextCommands(null);
        cmds.initSubPanel();
        GuiUtils.testFrame(cmds);        
    }
}
