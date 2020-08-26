/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
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
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;

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

    @NeptusProperty(name = "Axis Range", description = "Varies between the range and its simetrical value.\nCan be edited in each field inside the plugin.")
    protected static int RANGE = 127;

    enum ActionType {
        Axis,
        Button
    }

    private static final long serialVersionUID = 1L;

    private static final String ACTION_FILE_XML = "conf/controllers/actions.xml";
    private boolean sending = false;

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
            if (column != 3 || column != 6)
                return axisRenderer;
            else
                return super.getCellRenderer(row, column);
        };
    };

    @SuppressWarnings("serial")
    private JTable buttonsTable = new JTable() {
        public javax.swing.table.TableCellRenderer getCellRenderer(int row, int column) {
            if (column != 3 || column != 6)
                return btnRenderer;
            else
                return super.getCellRenderer(row, column);
        };
    };

    private AbstractTableModel axisModel;
    private AbstractTableModel buttonsModel;
    private TableRenderer axisRenderer = new TableRenderer(ActionType.Axis);
    private TableRenderer btnRenderer = new TableRenderer(ActionType.Button);

    private ControllerManager manager;

    private JButton btnRefresh = new JButton(new AbstractAction(I18n.text("Refresh Controllers")) {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            updateControllers();
        }
    });

    private JToggleButton btnInHold = new JToggleButton("Input Hold");

    private int timeIncrement = 0;
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
        console.addMainVehicleListener(this);
        PeriodicUpdatesService.register(this);
        getConsole().getImcMsgManager().addListener(this);
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

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                sending = true;
            }

            @Override
            public void windowClosing(WindowEvent e) {
                sending = false;
            }
        });

        // Start the interface
        refreshInterface();
    }

    @Override
    public void cleanSubPanel() {
        // Unregister listeners
        console.removeMainVehicleListener(this);
        PeriodicUpdatesService.unregister(this);
        getConsole().getImcMsgManager().removeListener(this);
    }

    public void buildDialog() {
        removeAll();
        setSize(300, 200);
        setLayout(new MigLayout("","[center]",""));
        axisModel = new TableModel(mappedAxis, ActionType.Axis);
        axisTable.setModel(axisModel);

        axisTable.addMouseListener(new JTableButtonMouseListener(axisTable));

        JScrollPane axisContainer = new JScrollPane(axisTable);
        //axisContainer.setSize(this.getWidth(), 10 + (axisTable.getRowHeight() * (rowCount + 1)));
        String args = "height ::"+(20+(axisTable.getRowHeight() * (axisModel.getRowCount() + 1)))+",wrap";
        add(axisContainer, args);

        buttonsModel = new TableModel(mappedButtons, ActionType.Button);
        buttonsTable.setModel(buttonsModel);
        buttonsTable.addMouseListener(new JTableButtonMouseListener(buttonsTable));

        JScrollPane btnContainer = new JScrollPane(buttonsTable);
        args = "height ::"+(20+(buttonsTable.getRowHeight() * (buttonsModel.getRowCount() + 1)))+",wrap";
        add(btnContainer, args);



        JPanel footerLeft = new JPanel(new MigLayout());
        JPanel footerRight = new JPanel(new MigLayout());
        JPanel footer = new JPanel(new MigLayout());

        JMenuBar menu = new JMenuBar();
        JMenu help = new JMenu("Help");
        JMenuItem instructions = new JMenuItem("Instructions");
        instructions.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO Auto-generated method stub
                // Console Informative Panel

            }
        });
        help.add(instructions);
        menu.add(help);

        add(menu, "dock north");
        
        for (JComboBox<String> selector : controllerSelectors) {
            footerLeft.add(selector, "w 200::, wrap");
        }

        footerRight.add(btnInHold,  "w 150::, wrap");
        footerRight.add(btnRefresh, "w 150::, wrap");
        

        footer.add(footerLeft);
        footer.add(footerRight);

        add(footer, "dock south");

        dialog.pack();
    }

    public JComboBox<String> generateControllerSelector() {
        JComboBox<String> comboBox;
        comboBox = new JComboBox<String>(manager.getControllerList().keySet().toArray(new String[0]));
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                @SuppressWarnings("unchecked")
                JComboBox<String> cb = (JComboBox<String>) e.getSource();
                currentController = (String) cb.getSelectedItem();
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
                if (comp == null)
                    result.add(new MapperComponent(action, "", 0.0f, false, RANGE));
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
            if (el.attributeValue("action").equalsIgnoreCase(actionName))
                return new MapperComponent(el.attributeValue("action"), el.attributeValue("component"), 0.0f,
                        Boolean.parseBoolean(el.attributeValue("inverted")),
                        Integer.parseInt(el.attributeValue("range")));
        }
        return null;
    }

    public void requestRemoteActions() {
        if (console.getMainSystem() != null) {
            RemoteActionsRequest raq = new RemoteActionsRequest(OP.QUERY, "");
            ImcMsgManager.getManager().sendMessageToSystem(raq, console.getMainSystem());
        }
    }

    public void updateControllers() {
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

        removeAll();

        if (console.getMainSystem() != null)
            add(new JLabel(I18n.text("Waiting for vehicle action list")));
        else
            add(new JLabel(I18n.text("No main vehicle selected in the console")));

        this.repaint();

        requestRemoteActions();
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
            return true;
        }

        if (!this.isVisible())
            return true;

        sending = dialog.isVisible();

        poll = manager.pollController(currentController);

        // Also if polling fails return true
        if (poll == null) {
            return true;
        }

        btnRefresh.setEnabled(!editing);
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
                                axisModel.fireTableDataChanged();

                                // Finish editing and save mappings
                                editing = false;
                                mcomp.editFlag = false;
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
            if (timeIncrement >= 2000 && actions == null) {
                requestRemoteActions();
                timeIncrement = 0;
            }
            timeIncrement += periodicDelay;

            if (currentController == null || actions == null || console.getMainSystem() == null) {
                return true;
            }
            msgActions.clear();

            for (String k : poll.keySet()) {
                // Find the suitable MapperComponent to get data from
                // Don't need to create an extra method for this one
                MapperComponent comp = null;
                for (MapperComponent c : mappedButtons) { // TODO the same with mappedAxis
                    if (c.button.equals(k)) {
                        comp = c;
                        break;
                    }
                }

                if (comp != null) {
                    comp.value = poll.get(k).getPollData()
                            * (actions.get(comp.action).equals("Axis") ? comp.getRange() : 1)
                            * (comp.inverted ? -1 : 1);
                    ((AbstractTableModel) axisTable.getModel()).fireTableDataChanged();
                    // Only if we are already sending that we build the msgActions LinkedHashMap
                    if (sending || btnInHold.isSelected()) {
                        msgActions.put(comp.action, comp.value + "");
                    }
                }
            }
            if (sending || btnInHold.isSelected()) {
                // Finally send the message
                RemoteActions msg = new RemoteActions();
                msg.setActions(msgActions);
                ImcMsgManager.getManager().sendMessageToSystem(msg, console.getMainSystem());
            }
        }
        return true;
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

            for (MapperComponent mcomp : mappedButtons) { // TODO verify
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
        }
        catch (Exception e) {
            NeptusLog.pub().error(I18n.text("Error Saving Controllers Actions in: " + ACTION_FILE_XML), e);
            e.printStackTrace();
        }
    }

    @Subscribe
    public void consume(RemoteActionsRequest message) {
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
        int range;

        boolean editFlag = false;

        MapperComponent(final String action, String component, float value, boolean inverted, int r) {
            this.action = action;
            this.button = component;
            this.value = value;
            this.inverted = inverted;
            this.edit = new JButton(I18n.text("Edit"));
            this.clear = new JButton(I18n.text("Clear"));
            this.range = r;

            InputVerifier intVer = new InputVerifier() { // TODO

                @Override
                public boolean verify(JComponent input) {
                    String txt = ((JTextField) input).getText();
                    try {
                        Integer.parseInt(txt);
                        return true;
                    }
                    catch (NumberFormatException e) {
                        return false;
                    }
                }
            };

            edit.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    if (!editing) {
                        editing = true;
                        editFlag = true;
                    }
                }
            });
            clear.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    MapperComponent.this.button = "";
                    MapperComponent.this.inverted = false;
                }
            });

        }

        public int getRange() {
            return this.range;
        }

        public void setRange(int r) {
            this.range = r;
        }
    }

    @SuppressWarnings("serial")
    public class TableRenderer extends DefaultTableCellRenderer {
        private ActionType actionType;

        public TableRenderer(ActionType type) {
            actionType = type;
        }

        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, final int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            MapperComponent comp = this.actionType.equals(ActionType.Axis)
                    ? (MapperComponent) ((TableModel) axisModel).getList().get(row)
                    : (MapperComponent) ((TableModel) buttonsModel).getList().get(row);
            // MapperComponent comp = (MapperComponent) ((TableModel)axisModel).getList().get(row);
            if (comp.editFlag) {
                setBackground(Color.green);
            }
            else {
                setBackground(Color.white);
            }
            if (column == 4) {
                JButton b;
                if (this.actionType.equals(ActionType.Axis)) {
                    b = (JButton) axisModel.getValueAt(row, column);
                    b.setEnabled(!editing); // Disable if we are editing
                    return (JButton) axisModel.getValueAt(row, column);
                }
                else if (this.actionType.equals(ActionType.Button)) {
                    b = (JButton) buttonsModel.getValueAt(row, column);
                    b.setEnabled(!editing); // Disable if we are editing
                    return (JButton) buttonsModel.getValueAt(row, column);
                }
            }
            if (column == 5) {
                if (this.actionType.equals(ActionType.Axis)) {
                    return (JButton) axisModel.getValueAt(row, column);
                }
                else if (this.actionType.equals(ActionType.Button)) {
                    return (JButton) buttonsModel.getValueAt(row, column);
                }
            }
            return this;
        }
    }
}
