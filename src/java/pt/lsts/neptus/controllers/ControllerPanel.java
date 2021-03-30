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
 * Author: José Correia
 * Nov 9, 2012
 */
package pt.lsts.neptus.controllers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.google.common.eventbus.Subscribe;

import net.java.games.input.Component;
import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.RemoteActions;
import pt.lsts.imc.RemoteActionsRequest;
import pt.lsts.imc.RemoteActionsRequest.OP;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.events.ConsoleEventVehicleStateChanged.STATE;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.util.GuiUtils;

/**
 * Controller Panel This panel is responsible for providing a away to teleoperate the vehicle, as well as edit the
 * pad/vehicle configuration if needed- Relies on a existing conf/controllers/actions.xml to keep track of the
 * controller mapping
 * 
 * @author jqcorreia
 * @author keila (May 2020)
 * 
 */
@Popup(pos = POSITION.TOP_RIGHT, width = 200, height = 400, accelerator = 'J')
@PluginDescription(author = "jquadrado", description = "Controllers Panel", name = "Controllers Panel", icon = "images/control-mode/teleoperation.png")
public class ControllerPanel extends ConsolePanel implements IPeriodicUpdates {

    @NeptusProperty(name = "Axis Range", description = "Varies between the range and its symmetrical value.")
    protected static float RANGE = (float) 1.0;

    enum ActionType {
        Axis,
        Button
    }

    private static final long serialVersionUID = 1L;

    private static final String ACTION_FILE_XML = "conf/controllers/actions.xml";
    // private boolean sending = false;

    // Vehicle action received via RemoteActionRequest (i.e Heading=axis, Accelerate=Button)
    private LinkedHashMap<String, String> actions = new LinkedHashMap<String, String>();
    // Mapped actions based on XML actions.xml for the current vehicle and selected controller
    private ArrayList<MapperComponent> mappedButtons = new ArrayList<MapperComponent>();
    // A list of actions to be added to a RemoteActions message
    private ArrayList<MapperComponent> mappedAxis = new ArrayList<MapperComponent>();
    // Mapped actions based on XML actions.xml for the current vehicle and selected controller
    private LinkedHashMap<String, String> msgActions = new LinkedHashMap<String, String>();
    // The current controller poll
    private LinkedHashMap<String, Component> poll;

    private ArrayList<JComboBox<String>> controllerSelectors = new ArrayList<JComboBox<String>>();

    @SuppressWarnings("serial")
    private JTable axisTable = new JTable() {
        public javax.swing.table.TableCellRenderer getCellRenderer(int row, int column) {
            if(column != 3)
                return axisRenderer;
            else
                return super.getCellRenderer(row, column);
        };
    };

    @SuppressWarnings("serial")
    private JTable buttonsTable = new JTable() {
        public javax.swing.table.TableCellRenderer getCellRenderer(int row, int column) {
            return btnRenderer;
        };
    };

    private AbstractTableModel axisModel;
    private AbstractTableModel buttonsModel;
    private AxisTableRenderer axisRenderer = new AxisTableRenderer(ActionType.Axis);
    private ButtonTableRenderer btnRenderer = new ButtonTableRenderer(ActionType.Button);

    private ControllerManager manager;

    private JButton btnReset = new JButton(new AbstractAction(I18n.text("Reset Controllers")) {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            updateControllers();
        }
    });

    private JToggleButton btnInHold = new JToggleButton("Input Hold");

    private int periodicDelay = 100;

    private String currentController;
    private Document doc;
    private ConsoleLayout console;

    private boolean editing = false;

    private LinkedHashMap<String, Float> oldPoll = new LinkedHashMap<String, Float>();

    public ControllerPanel(ConsoleLayout console) {
        super(console);
        this.console = console;
        this.removeAll();

        // Register listeners
        // console.addMainVehicleListener(this);
        // PeriodicUpdatesService.register(this);
        // getConsole().getImcMsgManager().addListener(this);
    }

    @Override
    public void initSubPanel() {
        SAXReader reader = new SAXReader();

        File fx = new File(ACTION_FILE_XML);
        if (fx.exists()) {
            try {
                doc = reader.read(fx);
            }
            catch (DocumentException e) {
                doc = DocumentHelper.createDocument();
                NeptusLog.pub().warn("Error loading controller actions file! Creating a new file.", e);
            }
        }
        else {
            doc = DocumentHelper.createDocument();
        }

        manager = console.getControllerManager();

        controllerSelectors.add(generateControllerSelector());
        controllerSelectors.add(generateControllerSelector());

        // Initialize current controller
        currentController = (String) controllerSelectors.get(0).getSelectedItem();

        setLayout(new MigLayout("", "[center]", ""));
        btnInHold.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    NeptusLog.pub().warn(I18n.text("Entering Input Hold Mode on Teleoperation."));
                }
                else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    msgActions.clear(); // clean on hold remote actions
                }
            }
        });

        // Start the interface
        refreshInterface();
        if (console.getMainSystem() != null)
            add(new JLabel(I18n.text("Waiting for vehicle action list")));
        else
            add(new JLabel(I18n.text("No main vehicle selected in the console")));

        if (actions != null) {
            buildDialog();
        }
    }

    /**
     * 
     */
    private void buildInstructions() {
        JMenuBar menu = new JMenuBar();
        JMenu help = new JMenu("Help");
        JMenuItem instructions = new JMenuItem("Instructions");
        instructions.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog dg = new JDialog(SwingUtilities.getWindowAncestor(ControllerPanel.this),
                        ModalityType.DOCUMENT_MODAL);
                JPanel content = new JPanel(new BorderLayout());
                JTextPane txt = new JTextPane();
                txt.setContentType("text/html");
                // txt.setText(I18n.text("<html>To assign a button from the Joystick to the Main System Available
                // RemoteActions:\n"
                // + " 1. Click on the Edit button of the intended RemoteAction on the Table.\n"
                // + " 2. Once the RemoteAction line gets green you are in edition mode of the Table.\n"
                // + " 3. Select the intended button on the Joystick.\n"
                // + " 4. After the editing mode is disable, verify if the axis is in the correct direction, otherwise
                // inverted on the in the respective column.\n\n"
                // + "After configuring all the RemoteActions of the Main System, youcan enable Teleoperation mode and
                // start controlling with the joystick.</html>"));
                txt.setText(I18n.text("<html>"
                        + "<h1 style=\"text-align: center;\"><strong>Instructions</strong></h1>\n"
                        + "<h2>To assign a button from the Joystick<br /> to the Main System Available RemoteActions:</h2>\n"
                        + "<ol>\n"
                        + "<li>Click on the Edit button of the intended <br />RemoteAction on the Table.</li>\n"
                        + "<li>Once the RemoteAction line gets green <br />you are in edition mode of the Table.</li>\n"
                        + "<li>Select the intended button on the Joystick.</li>\n"
                        + "<li>After the editing mode is disable, <br />&nbsp;verify if the axis is in the correct direction,<br />&nbsp;otherwise you can invert it on the in the respective column.</li>\n"
                        + "</ol>\n"
                        + "<h2>Open the Controllers Panel Plugin to configure the panel before open in the Pilot - ROV 2 profile.</h2>"
                        + "<h2>After configuring all the RemoteActions of the Main System, you can enable Teleoperation mode and start controlling with the Joystick.</h2>"
                        + "<h2>Once in Input Hold Mode, the list of Remote Actions will only increment according to the new buttons selected.</h2>"
                        + "</html>"));
                txt.setEditable(false);
                content.add(txt, BorderLayout.CENTER);
                dg.setContentPane(content);
                dg.setSize(500, 500);
                dg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                dg.getRootPane().registerKeyboardAction(ev -> {
                    dg.dispose();
                }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
                GuiUtils.centerParent(dg, (Window) dg.getParent());
                dg.setVisible(true);

            }
        });
        help.add(instructions);
        menu.add(help);

        add(menu, "dock north");
    }

    @Override
    public void cleanSubPanel() {
        // Unregister listeners
        // console.removeMainVehicleListener(this);
        // PeriodicUpdatesService.unregister(this);
        // getConsole().getImcMsgManager().removeListener(this);
    }

    public void buildDialog() {
        removeAll();
        buildInstructions();

        axisModel = new AxisTableModel(mappedAxis);
        axisTable.setModel(axisModel);

        axisTable.addMouseListener(new JTableButtonMouseListener(axisTable));

        JScrollPane axisContainer = new JScrollPane(axisTable);
        // axisContainer.setSize(this.getWidth(), 10 + (axisTable.getRowHeight() * (rowCount + 1)));
        String args = "height ::" + (20 + (axisTable.getRowHeight() * (axisModel.getRowCount() + 1))) + ",wrap";
        add(axisContainer, args);

        buttonsModel = new ButtonTableModel(mappedButtons);
        buttonsTable.setModel(buttonsModel);
        buttonsTable.addMouseListener(new JTableButtonMouseListener(buttonsTable));

        JScrollPane btnContainer = new JScrollPane(buttonsTable);
        args = "height ::" + (20 + (buttonsTable.getRowHeight() * (buttonsModel.getRowCount() + 1))) + ",wrap";
        add(btnContainer, args);

        JPanel footerLeft = new JPanel(new MigLayout());
        JPanel footerRight = new JPanel(new MigLayout());
        JPanel footer = new JPanel(new MigLayout());

        for (JComboBox<String> selector : controllerSelectors) {
            footerLeft.add(selector, "w 200::, wrap");
        }

        footerRight.add(btnInHold, "w 150::, wrap");
        footerRight.add(btnReset, "w 150::, wrap");

        footer.add(footerLeft);
        footer.add(footerRight);

        add(footer, "dock south");
        setSize(300, 200);


        dialog.pack();
        this.repaint();
    }

    public JComboBox<String> generateControllerSelector() {
        JComboBox<String> comboBox;
        comboBox = new JComboBox<String>(manager.getControllerList().keySet().toArray(new String[0]));
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                @SuppressWarnings("unchecked")
                JComboBox<String> cbox = (JComboBox<String>) e.getSource();
                currentController = (String) cbox.getSelectedItem();
                mappedAxis = getMappedActions(console.getMainSystem(), currentController, ActionType.Axis);
                mappedButtons = getMappedActions(console.getMainSystem(), currentController, ActionType.Button);
                buildDialog();
            }
        });
        return comboBox;
    }

    public ArrayList<MapperComponent> getMappedActions(String systemName, String controllerName,
            ActionType actionType) {
        ArrayList<MapperComponent> result = new ArrayList<MapperComponent>();

        for (Entry<String, String> entry : actions.entrySet()) {
            String action = entry.getKey();
            String aType = entry.getValue();
            MapperComponent comp = getMapperComponentByName(systemName, controllerName, action);
            if (aType.equalsIgnoreCase(actionType.name())) { // verify if action is Axis or Button
                if (comp == null) {
                    if (actionType.equals(ActionType.Axis))
                        result.add(new MapperComponent(action, "", 0.0f, false, RANGE,0.0f));
                    else if (actionType.equals(ActionType.Button))
                        result.add(new MapperComponent(action, "", 0.0f, false,0.0f,0.0f));
                }
                else
                    result.add(comp);
            }
        }
        return result;
    }

    public MapperComponent getMapperComponentByName(String systemName, String controllerName, String actionName) {
        List<?> list = doc.selectNodes(
                "/systems/system[@name='" + systemName + "']/controller[@name='" + controllerName + "']/*");
        for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
            Element el = (Element) iter.next();
            if (el.attributeValue("action").equalsIgnoreCase(actionName)) {
                try {
                    if (el.attribute("range") == null) // Button
                        return new MapperComponent(el.attributeValue("action"), el.attributeValue("component"), 0.0f,
                                Boolean.parseBoolean(el.attributeValue("inverted")), 0.0f,0.0f);
                    else // Axis Component
                        return new MapperComponent(el.attributeValue("action"), el.attributeValue("component"),
                                0.0f,
                                Boolean.parseBoolean(el.attributeValue("inverted")),RANGE, 0.0f);
                }
                catch (Exception e) {
                    NeptusLog.pub().warn(I18n.text("Error parsing controllers configuration file."), e);
                }
            }
        }
        return null;
    }

    public void requestRemoteActions() {
        if (console.getMainSystem() != null) {
            RemoteActionsRequest raq = new RemoteActionsRequest();
            raq.setOp(OP.QUERY);
            // IMCDefinition.getInstance().getResolver().resolve(console.getMainSystem());
            getConsole().getImcMsgManager().sendMessageToSystem(raq, console.getMainSystem());
        }
    }

    public void updateControllers() {
        editing = false;
        manager.fetchControllers();
        String list[] = manager.getControllerList().keySet().toArray(new String[0]);
        for (JComboBox<String> cb : controllerSelectors) {
            cb.removeAllItems();
            for (String s : list) {
                cb.addItem(s);
            }
        }
    }

    /**
     * Clear the layout and ask the system for remote actions
     */
    public void refreshInterface() {
        actions = null;
        editing = false;

        removeAll();
        buildInstructions();

        if (connected())
            requestRemoteActions();
        this.repaint();
    }

    private boolean sending() {
        return console.getSystem(console.getMainSystem()).getVehicleState().equals(STATE.TELEOPERATION);
    }

    private boolean connected() {
        return !console.getSystem(console.getMainSystem()).getVehicleState().equals(STATE.DISCONNECTED);
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange evt) {
        refreshInterface();
    }

    @Override
    public long millisBetweenUpdates() {
        return periodicDelay;
    }

    @Override
    public boolean update() {

        if (manager == null || currentController == null) {
            manager.pollError(currentController);
            return true;
        }

        /*if(!isVisible() || !isShowing() || !isEnabled())
            return true;*/

        if (dialog == null)
            return true;

        try {
            poll = manager.pollController(currentController);
        }
        catch (Exception e) {
            //Remove device not working properly and trigger new search of devices
            manager.pollError(currentController);
            e.printStackTrace();
        }

        // Also if polling fails return true
        if (poll == null) {
            return true;
        }

        btnReset.setEnabled(!editing);
        // comboBox.setEnabled(!editing);

        if (editing) {
            if (oldPoll.size() == poll.size()) {
                for (String k : poll.keySet()) {
                    if (poll.get(k).getPollData() != oldPoll.get(k).floatValue()
                            && Math.abs(poll.get(k).getPollData()) == 1.0) {
                        ArrayList<MapperComponent> remoteActions = new ArrayList<MapperComponent>();
                        remoteActions.addAll(mappedAxis);
                        remoteActions.addAll(mappedButtons);
                        for (MapperComponent mcomp : remoteActions) {
                            if (mcomp.editFlag) {
                                mcomp.button = k;
                                mcomp.inverted = poll.get(k).getPollData() < 0;

                                if (actions.get(mcomp.action).equals("Axis"))
                                    axisModel.fireTableDataChanged();
                                else
                                    buttonsModel.fireTableDataChanged();

                                // Finish editing and save mappings
                                editing = false;
                                mcomp.editFlag = false;
                                mcomp.setDeadZone(poll.get(k).getDeadZone());
                                saveMappings(); // Save every time we edit a single action
                                break;
                            }
                        }
                    }
                }
            }

            // Deep copy poll to oldPoll
            oldPoll.clear();
            for (String k : poll.keySet())
                oldPoll.put(k, poll.get(k).getPollData());

        }
        else {
            // Use the periodic update to keep asking for RemoteActions list
            if (connected() && actions == null) {
                requestRemoteActions();
            }

            if (currentController == null || actions == null || console.getMainSystem() == null) {
                return true;
            }
            
            if(!btnInHold.isSelected())
                msgActions.clear();

            for (String k : poll.keySet()) {
                // Find the suitable MapperComponent to get data from
                // Don't need to create an extra method for this one
                MapperComponent comp = null;
                ArrayList<MapperComponent> remoteActions = new ArrayList<MapperComponent>();
                remoteActions.addAll(mappedAxis);
                remoteActions.addAll(mappedButtons);
                for (MapperComponent c : remoteActions) {
                    if (c.button.equals(k)) {
                        comp = c;
                        break;
                    }
                }

                if (comp != null && poll.get(k) != null) {
                    // update Model list
                    comp.value = poll.get(k).getPollData()
                            * (actions.get(comp.action).equals("Axis") ? comp.getRange() * ((comp.inverted ? -1 : 1))
                                    : 1);

                    if (actions.get(comp.action).equals("Axis")) {
                        int index = mappedAxis.indexOf(comp);
                        if(index != -1) {
                            ((AbstractTableModel) axisTable.getModel()).setValueAt(comp.value, index, 2);
                            ((AbstractTableModel) axisTable.getModel()).fireTableCellUpdated(index, 2);
                        }
                        ((AbstractTableModel) axisTable.getModel()).fireTableDataChanged();
                    }
                    else {
                        int index = mappedButtons.indexOf(comp);
                        if(index != -1) {
                            ((AbstractTableModel) buttonsTable.getModel()).setValueAt(comp.value, index, 2);
                            ((AbstractTableModel) buttonsTable.getModel()).fireTableCellUpdated(index, 2);
                        }
                        ((AbstractTableModel) buttonsTable.getModel()).fireTableDataChanged();
                    }


                    if (sending() && (Float.compare(Math.abs(comp.value), poll.get(k).getDeadZone()) != 0)) {     
                        msgActions.put(comp.action, comp.value + "");
                    }
                }
            }
            // If no new button is selected and we are still in input old mode
            // Sends the last saved buttons in the Tupple list
            if (sending() ) {//&& btnInHold.isSelected()
                sendRemoteActions();
            }
        }
        return true;
    }

    /**
     * 
     */
    private void sendRemoteActions() {
        RemoteActions msg = new RemoteActions();
        msg.setActions(msgActions);
        getConsole().getImcMsgManager().sendMessageToSystem(msg, console.getMainSystem());
    }

    private void saveMappings() {
        try {
            Element systems = (Element) doc.selectSingleNode("/systems");
            if (systems == null) {
                systems = doc.addElement("systems");
            }

            Element system = (Element) systems.selectSingleNode("system[@name='" + console.getMainSystem() + "']");
            if (system == null) {
                NeptusLog.pub().info("Adding new system to controller mapping");
                system = systems.addElement("system").addAttribute("name", console.getMainSystem());
            }

            Element controller = (Element) system.selectSingleNode("controller[@name='" + currentController + "']");
            if (controller == null) {
                NeptusLog.pub().info("Adding new controller to controller mapping");
                controller = system.addElement("controller").addAttribute("name", currentController);
            }

            List<?> l = controller.selectNodes("entry");
            for (int i = 0; i < l.size(); i++) {
                controller.remove((Element) l.get(i));
            }

            for (MapperComponent mcomp : mappedAxis) {
                Element e = controller.addElement("entry");
                e.addAttribute("component", mcomp.button);
                e.addAttribute("action", mcomp.action);
                e.addAttribute("inverted", String.valueOf(mcomp.inverted));
                e.addAttribute("range", String.valueOf(mcomp.getRange()));
            }

            for (MapperComponent mcomp : mappedButtons) {
                Element e = controller.addElement("entry");
                e.addAttribute("component", mcomp.button);
                e.addAttribute("action", mcomp.action);
                e.addAttribute("inverted", String.valueOf(mcomp.inverted));
            }

            File fx = new File(ACTION_FILE_XML);
            fx.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(ACTION_FILE_XML);
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter(fos, format);
            writer.write(doc);
            writer.flush();
            writer.close();
        }
        catch (Exception e) {
            NeptusLog.pub().error(I18n.text("Error Saving Controllers Actions in: " + ACTION_FILE_XML), e);
            e.printStackTrace();
        }
    }

    @Subscribe
    public void on(RemoteActionsRequest message) {
        try {
            if (!message.getOp().equals(OP.REPORT))
                return;
            if (actions == null) {
                actions = new LinkedHashMap<String, String>();
            }
            for (Entry<String, String> entry : message.getActions().entrySet()) {
                String k = entry.getKey();
                actions.put(k, message.getActions().get(k));
            }
            mappedAxis = getMappedActions(console.getMainSystem(), currentController, ActionType.Axis);
            mappedButtons = getMappedActions(console.getMainSystem(), currentController, ActionType.Button);
            buildDialog();
        }
        catch (Exception e) {
            NeptusLog.pub().error(I18n.text("Error parsing incoming RemoteActionRequest"), e);
            e.printStackTrace();
        }
    }

    /**
     * @author jqcorreia
     */
    class MapperComponent {
        String action;
        String button;
        float value;
        boolean inverted;
        JButton edit;
        JButton clear;
        float range;
        float deadZone;

        boolean editFlag = false;

        MapperComponent(final String action, String component, float value, boolean inverted, float r, float zero) {
            this.action = action;
            this.button = component;
            this.value = value;
            this.inverted = inverted;
            this.edit = new JButton(I18n.text("Edit"));
            this.clear = new JButton(I18n.text("Clear"));
            this.range = r;
            this.deadZone = zero;

            initButtons();

        }

        public float getRange() {
            return this.range;
        }

        public void setRange(float  r) {
            this.range = r;
        }
        
        /**
         * @return the deadZone
         */
        public float getDeadZone() {
            return deadZone;
        }

        /**
         * @param deadZone the deadZone to set
         */
        public void setDeadZone(float deadZone) {
            this.deadZone = deadZone;
        }

        public void clear() {
            this.button = "";
            this.inverted = false;
            this.value = (float) 0.0;
            this.inverted = false;
            this.editFlag = false;
        }

        /**
         * 
         */
        private void initButtons() {
            // editing = false; //TODO
            edit.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e); // edit.doClick();
                    if (!editing) { // ItemEvent.SELECTED
                        editing = true;
                        editFlag = true;
                    }
                    // else { // if(e.getStateChange()==ItemEvent.DESELECTED){
                    // editing = false;
                    // editFlag = false;
                    // saveMappings(); // Save every time we edit a single action
                    // }

                }
            });
            clear.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    MapperComponent.this.clear();
                }
            });
        }
    }

    @SuppressWarnings("serial")
    public class ButtonTableRenderer extends DefaultTableCellRenderer {

        public ButtonTableRenderer(ActionType type) {
            super();
        }

        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, final int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            MapperComponent comp = (MapperComponent) ((ButtonTableModel) buttonsModel).getList().get(row);

            if (comp.editFlag) {
                setBackground(Color.green);
            }
            else {
                setBackground(Color.white);
            }

            if(column == 2) {
                if(!sending())
                    return this;
                String action = comp.action;

                float updated_value = comp.value;
                try {
                    updated_value = msgActions.containsKey(action) ? Float.parseFloat(msgActions.get(action)) : comp.value;
                }
                catch (NumberFormatException ignored) {

                };
                if(updated_value != comp.value && (Float.compare(Math.abs(updated_value), comp.deadZone) != 0) && btnInHold.isSelected()) {
                    comp.value = updated_value;
                    ((ButtonTableModel) buttonsModel).setValueAt(updated_value, row, 2);
                    ((AbstractTableModel) buttonsTable.getModel()).fireTableDataChanged();
                }
            }

            if (column == 3) {
                JButton b; // JToggleButton
                b = (JButton) buttonsModel.getValueAt(row, column); // Toggle
                b.setEnabled(!editing); // Disable if we are editing
                return b;
            }
            if (column == 4) {
                return (JButton) buttonsModel.getValueAt(row, column);
            }
            return this;
        }
    }
    @SuppressWarnings("serial")
    public class AxisTableRenderer extends DefaultTableCellRenderer {

        public AxisTableRenderer(ActionType type) {
            super();
        }

        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                boolean hasFocus, final int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            MapperComponent comp =  (MapperComponent) ((AxisTableModel) axisModel).getList().get(row);


            if (comp.editFlag) {
                setBackground(Color.green);
            }
            else {
                setBackground(Color.white);
            }

            if(column == 2) {
                if(!sending())
                    return this;
                String action = comp.action;

                float updated_value = comp.value;
                try {
                    updated_value = msgActions.containsKey(action) ? Float.parseFloat(msgActions.get(action)) : comp.value;
                }
                catch (NumberFormatException ignored) {

                };
                if(updated_value != comp.value && (Float.compare(Math.abs(updated_value), comp.deadZone) != 0) && btnInHold.isSelected()) {
                    comp.value = updated_value;
                    ((AxisTableModel) axisModel).setValueAt(updated_value, row, 2);
                    ((AbstractTableModel) axisTable.getModel()).fireTableDataChanged();
                }
            }

            if (column == 4) {
                JButton b; // JToggleButton
                b = (JButton) axisModel.getValueAt(row, column);
                b.setEnabled(!editing); // Disable if we are editing //TODO
                return b;
            }
            if (column == 5) {
                return (JButton) axisModel.getValueAt(row, column);
            }
            return this;
        }
    }
}
