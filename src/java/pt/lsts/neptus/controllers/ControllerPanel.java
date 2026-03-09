/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.RowSorter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

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
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;
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
@Popup(pos = POSITION.TOP_RIGHT, width = 450, accelerator = 'J')
@PluginDescription(author = "jquadrado", description = "Controllers Panel", name = "Controllers Panel", icon = "images/control-mode/teleoperation.png")
public class ControllerPanel extends ConsolePanel implements IPeriodicUpdates {

    @NeptusProperty(name = "Axis Range", description = "Varies between the range and its symmetrical value.")
    protected static float RANGE = (float) 127.0;

    enum ActionType {
        Axis,
        Button
    }

    private static final long serialVersionUID = 1L;

    private static final String ACTION_FILE_XML = "conf/controllers/actions.xml";
    private static final String CACHED_ACTIONS_FILE = ".cache/db/controller_cached_actions.properties";
    private volatile boolean mousePressed = false;

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
    // Flag to control RemoteActions requests
    private boolean requestedActions = false;

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

    private LinkedHashMap<String, Float> oldPoll = new LinkedHashMap<String, Float>();

    private boolean hasAnyEditFlag() {
        for (MapperComponent comp : mappedAxis) {
            if (comp.editFlag) return true;
        }
        for (MapperComponent comp : mappedButtons) {
            if (comp.editFlag) return true;
        }
        return false;
    }

    private void clearAllEditFlags() {
        for (MapperComponent comp : mappedAxis) {
            comp.editFlag = false;
        }
        for (MapperComponent comp : mappedButtons) {
            comp.editFlag = false;
        }
        
        if (axisTable != null) axisTable.repaint();
        if (buttonsTable != null) buttonsTable.repaint();
    }
    
    private void clearAllEditFlagsForce() {
        for (MapperComponent comp : mappedAxis) {
            comp.editFlag = false;
        }
        for (MapperComponent comp : mappedButtons) {
            comp.editFlag = false;
        }
        
        if (axisTable != null) axisTable.repaint();
        if (buttonsTable != null) buttonsTable.repaint();
    }

    private boolean noOtherRowEditing() {
        return !hasAnyEditFlag();
    }

    private void updateModel() {
        if (axisModel != null && buttonsModel != null) {
            ((AxisTableModel) axisModel).setList(mappedAxis);
            ((ButtonTableModel) buttonsModel).setList(mappedButtons);

            axisModel.fireTableDataChanged();
            buttonsModel.fireTableDataChanged();
        }
    }

    public ControllerPanel(ConsoleLayout console) {
        super(console);
        this.console = console;
        this.removeAll();

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
        if (console.getMainSystem() != null && actions.isEmpty())
            add(new JLabel(I18n.text("Waiting for vehicle action list")));
        else
            add(new JLabel(I18n.text("No main vehicle selected in the console")));

        if (actions != null) {
            buildDialog();
        }
        refreshInterface();
    }

    @Override
    public void popupShown() {
        super.popupShown();
        if (actions != null) {
            int numActions = actions.size();
            int height = 330;
            if (numActions > 8) {
                height += 20 * (numActions - 8);
            }
            if (dialog != null) {
                dialog.setSize(450, height);
            }
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
                        + "<li>After the editing mode is disable, <br />&nbsp;verify if the axis is in the correct direction,<br />&nbsp;otherwise you can invert it in the respective column.</li>\n"
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
        PeriodicUpdatesService.unregister(this);
        getConsole().getImcMsgManager().removeListener(this);
    }

    public void buildDialog() {
        removeAll();
        buildInstructions();

        axisModel = new AxisTableModel(mappedAxis, this);
        buttonsModel = new ButtonTableModel(mappedButtons);

        RowSorter<?> axisSorter = axisTable.getRowSorter();
        RowSorter<?> buttonSorter = buttonsTable.getRowSorter();

        axisTable.setRowSorter(null);
        buttonsTable.setRowSorter(null);

        axisTable.setModel(axisModel);
        axisTable.getTableHeader().setReorderingAllowed(false);
        axisTable.setRowSelectionAllowed(false);
        axisTable.getColumnModel().getColumn(3).setCellEditor(new BooleanCellEditor());
        axisTable.getColumnModel().getColumn(4).setCellEditor(new ActionButtonEditor());
        axisTable.getColumnModel().getColumn(5).setCellEditor(new ActionButtonEditor());
        axisTable.setDefaultRenderer(Boolean.class, new BooleanRenderer());
        axisTable.revalidate();

        axisTable.getColumnModel().getColumn(1).setMinWidth(90);

        buttonsTable.setModel(buttonsModel);
        buttonsTable.getTableHeader().setReorderingAllowed(false);
        buttonsTable.setRowSelectionAllowed(false);
        buttonsTable.setDefaultRenderer(Object.class, new ButtonTableRenderer(ActionType.Button));
        buttonsTable.getColumnModel().getColumn(3).setCellEditor(new ActionButtonEditor());
        buttonsTable.getColumnModel().getColumn(4).setCellEditor(new ActionButtonEditor());
        buttonsTable.revalidate();

        axisModel.fireTableDataChanged();
        buttonsModel.fireTableDataChanged();

        JScrollPane axisContainer = new JScrollPane(axisTable);
        String args = "height ::" + (20 + (axisTable.getRowHeight() * (axisModel.getRowCount() + 1))) + ",wrap";
        add(axisContainer, args);
        axisContainer.revalidate();

        JScrollPane btnContainer = new JScrollPane(buttonsTable);
        args = "height ::" + (20 + (buttonsTable.getRowHeight() * (buttonsModel.getRowCount() + 1))) + ",wrap";
        add(btnContainer, args);
        btnContainer.revalidate();

        JPanel footerLeft = new JPanel(new MigLayout());
        JPanel footerRight = new JPanel(new MigLayout());
        JPanel footer = new JPanel(new MigLayout("", "[center]", ""));

        for (JComboBox<String> selector : controllerSelectors) {
            footerLeft.add(selector, "w 200::, wrap");
        }

        footerRight.add(btnInHold, "w 150::, wrap");
        footerRight.add(btnReset, "w 150::, wrap");

        footer.add(footerLeft, "push");
        footer.add(footerRight, "push");

        add(footer, "dock south");

        int numActions = actions.size();
        int height = 330;
        if (numActions > 8) {
            height += 20 * (numActions - 8);
        }
        if (dialog != null && dialog.isVisible()) {
            dialog.setSize(450, height);
        }

        revalidate();
        repaint();

        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }

        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
            if (getParent() != null) {
                getParent().revalidate();
                getParent().repaint();
            }
        });

        SwingUtilities.invokeLater(() -> {
            try {
                if (axisSorter != null) {
                    axisTable.setRowSorter((RowSorter<? extends TableModel>) axisSorter);
                } else {
                    axisTable.setAutoCreateRowSorter(true);
                }

                if (buttonSorter != null) {
                    buttonsTable.setRowSorter((RowSorter<? extends TableModel>) buttonSorter);
                } else {
                    buttonsTable.setAutoCreateRowSorter(true);
                }
            } catch (Exception e) {
                axisTable.setAutoCreateRowSorter(true);
                buttonsTable.setAutoCreateRowSorter(true);
            }
        });
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
                updateModel();
                buildDialog();
            }
        });
        return comboBox;
    }

    public ArrayList<MapperComponent> getMappedActions(String systemName, String controllerName,
            ActionType actionType) {
        ArrayList<MapperComponent> result = new ArrayList<MapperComponent>();

        if (actions == null) {
            return result;
        }

        boolean isDecimal = !actions.get("Ranges").equals("Range127");
        float range = (float) (isDecimal ? 1.0 : 127.0);
        for (Entry<String, String> entry : actions.entrySet()) {
            String action = entry.getKey();
            String aType = entry.getValue();
            MapperComponent comp = getMapperComponentByName(systemName, controllerName, action);
            if (aType.equalsIgnoreCase(actionType.name())) { // verify if action is Axis or Button
                if (comp == null) {
                    comp = findExistingComponent(action, actionType);
                    if (comp == null) {
                        if (actionType.equals(ActionType.Axis)) {
                            comp = new MapperComponent(action, "", 0.0f, false, range, 0.0f);
                        }
                        else if (actionType.equals(ActionType.Button)) {
                            comp = new MapperComponent(action, "", 0.0f, false, 0.0f, 0.0f);
                        }
                    }
                }
                if (comp != null) {
                    result.add(comp);
                }
            }
        }
        return result;
    }

    private MapperComponent findExistingComponent(String action, ActionType actionType) {
        if (actionType.equals(ActionType.Axis)) {
            for (MapperComponent comp : mappedAxis) {
                if (comp.action.equals(action)) {
                    return comp;
                }
            }
        } else if (actionType.equals(ActionType.Button)) {
            for (MapperComponent comp : mappedButtons) {
                if (comp.action.equals(action)) {
                    return comp;
                }
            }
        }
        return null;
    }

    public MapperComponent getMapperComponentByName(String systemName, String controllerName, String actionName) {
        List<?> list = doc.selectNodes(
                "/systems/system[@name='" + systemName + "']/controller[@name='" + controllerName + "']/*");

        boolean isDecimal = true; // Default to decimal
        if (actions != null && actions.containsKey("Ranges")) {
            isDecimal = !actions.get("Ranges").equals("Range127");
        }
        float range = (float) (isDecimal ? 1.0 : 127.0);
        for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
            Element el = (Element) iter.next();
            if (el.attributeValue("action").equalsIgnoreCase(actionName)) {
                try {
                    if (el.attribute("range") == null) {
                        return new MapperComponent(el.attributeValue("action"), el.attributeValue("component"), 0.0f,
                                Boolean.parseBoolean(el.attributeValue("inverted")), 0.0f, 0.0f);
                    }
                    else {
                        return new MapperComponent(el.attributeValue("action"), el.attributeValue("component"),
                                0.0f,
                                Boolean.parseBoolean(el.attributeValue("inverted")),range, 0.0f);
                    }
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
        }
    }

    private void updateControllers() {
        if (!connected()) {
            return;
        }

        manager.fetchControllers();
        String list[] = manager.getControllerList().keySet().toArray(new String[0]);
        for (JComboBox<String> cb : controllerSelectors) {
            cb.removeAllItems();
            for (String s : list) {
                cb.addItem(s);
            }
        }
        for (JComboBox<String> cb : controllerSelectors) {
            cb.setSelectedItem(currentController);
        }
    }

    /**
     * Clear the layout and ask the system for remote actions
     */
    public void refreshInterface() {
        clearAllEditFlagsForce();

        if (!connected()) {
            removeAll();
            buildInstructions();
            add(new JLabel(I18n.text("Vehicle disconnected")));
            actions = null;
            requestedActions = false;
            revalidate();
            repaint();
            return;
        }

        if (actions != null && !actions.isEmpty()) {
            return;
        }

        removeAll();
        buildInstructions();
        add(new JLabel(I18n.text("Waiting for vehicle action list")));

        if (!requestedActions) {
            requestRemoteActions();
            requestedActions = true;
        }

        revalidate();
        repaint();
    }

    private boolean sending() {
        return console.getSystem(console.getMainSystem()).getVehicleState().equals(STATE.TELEOPERATION);
    }

    private boolean connected() {
        if(console.getMainSystem() != null)
            return !console.getSystem(console.getMainSystem()).getVehicleState().equals(STATE.DISCONNECTED);
        return false;
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange evt) {

        SwingUtilities.invokeLater(() -> {

            if (console.getMainSystem() == null) {
                return;
            }

            actions = null;
            requestedActions = false;

            LinkedHashMap<String, String> cached = loadCachedActions(console.getMainSystem());
            if (!cached.isEmpty()) {
                actions = new LinkedHashMap<>(cached);
            }

            clearAllEditFlagsForce();

            msgActions.clear();

            mappedAxis.clear();
            mappedButtons.clear();

            axisModel = null;
            buttonsModel = null;

            updateControllers();

            if (!controllerSelectors.isEmpty()) {
                currentController = (String) controllerSelectors.get(0).getSelectedItem();
            }

            if (actions != null && !actions.isEmpty()) {
                buildDialog();
            } else {
                refreshInterface();
            }
        });
    }

    @Override
    public long millisBetweenUpdates() {
        return periodicDelay;
    }

    private int controllerFetchCounter = 0;

    @Override
    public boolean update() {
        if (manager == null || currentController == null) {
            return true;
        }

        if (!connected()) {
            return true;
        }

        /*if(!isVisible() || !isShowing() || !isEnabled())
            return true;*/
        // Use the periodic update to keep asking for RemoteActions list
        if (connected() && actions == null) {
            requestRemoteActions();
            requestedActions = true;
        }

        if (!isShowing()) {
            return true;
        }

        try {
            poll = manager.pollController(currentController);
        } catch (Exception e) {
            manager.pollError(currentController);
            e.printStackTrace();
            return true;
        }

        if (poll == null) {
            return true;
        }

        btnReset.setEnabled(!hasAnyEditFlag());

        if (hasAnyEditFlag()) {
            for (String k : poll.keySet()) {
                float currentData = poll.get(k).getPollData();
                float previousData = oldPoll.getOrDefault(k, 0f);
                if (currentData != previousData &&
                        Math.abs(currentData) > 0.9f &&
                        Math.abs(previousData) < 0.5f) {

                    ArrayList<MapperComponent> remoteActions = new ArrayList<>();
                    remoteActions.addAll(mappedAxis);
                    remoteActions.addAll(mappedButtons);

                    for (MapperComponent mcomp : remoteActions) {
                        if (mcomp.editFlag) {
                            mcomp.button = k;
                            mcomp.value = 0f;
                            mcomp.editFlag = false;
                            mcomp.setDeadZone(poll.get(k).getDeadZone());
                            saveMappings();
                            break;
                        }
                    }
                }
            }

            oldPoll.clear();
            for (String k : poll.keySet()) {
                oldPoll.put(k, poll.get(k).getPollData());
            }
        } else {
            if (currentController == null || actions == null || console.getMainSystem() == null) {
                return true;
            }

            if (!btnInHold.isSelected()) {
                msgActions.clear();
            }

            boolean valuesChanged = false;

            for (String k : poll.keySet()) {
                MapperComponent comp = null;
                for (MapperComponent c : mappedAxis) {
                    if (c.button.equals(k)) {
                        comp = c;
                        break;
                    }
                }
                if (comp == null) {
                    for (MapperComponent c : mappedButtons) {
                        if (c.button.equals(k)) {
                            comp = c;
                            break;
                        }
                    }
                }

                if (comp != null && poll.get(k) != null) {
                    float raw = poll.get(k).getPollData();
                    float updated_value = 0f;

                    String type = actions.get(comp.action);

                    if ("Axis".equalsIgnoreCase(type)) {

                        if (comp.action.toLowerCase().contains("thrust") ||
                                comp.action.toLowerCase().contains("surge") ||
                                comp.action.toLowerCase().contains("forward") ||
                                comp.action.toLowerCase().contains("throtle"))
                        {
                            float normalized = (raw + 1f) / 2f;

                            if (comp.inverted) {
                                normalized = 1f - normalized;
                            }

                            updated_value = normalized * comp.getRange();
                        }
                        else {

                            if (comp.inverted) {
                                raw *= -1f;
                            }

                            updated_value = raw * comp.getRange();
                        }
                    }
                    else if ("Button".equalsIgnoreCase(type)) {

                        // Buttons normalmente 0 ou 1
                        if (comp.inverted) {
                            raw = 1f - raw;
                        }

                        updated_value = raw;
                    }

                    if (Math.abs(updated_value) < 0.0001f) {
                        updated_value = 0f;
                    }

                    if (btnInHold.isSelected()) {
                        if (Float.compare(Math.abs(updated_value), Math.abs(comp.value)) >= 0) {
                            if (comp.value != updated_value) {
                                comp.value = updated_value;
                                valuesChanged = true;
                            }
                        }
                    } else {
                        if (comp.value != updated_value) {
                            comp.value = updated_value;
                            valuesChanged = true;
                        }
                    }

                    if (sending() && (Float.compare(Math.abs(comp.value), poll.get(k).getDeadZone()) != 0)) {
                        msgActions.put(comp.action, comp.value + "");
                    }
                }
            }

            if (valuesChanged && !mousePressed) {
                SwingUtilities.invokeLater(() -> {
                    if (axisTable != null) axisTable.repaint();
                    if (buttonsTable != null) buttonsTable.repaint();
                });
            }
        }

        if (sending()) {
            sendRemoteActions();
        }

        if (!connected() && (actions != null || axisModel != null || buttonsModel != null)) {
            if (axisModel != null) {
                ((AxisTableModel) axisModel).setList(new ArrayList<>());
            }
            if (buttonsModel != null) {
                ((ButtonTableModel) buttonsModel).setList(new ArrayList<>());
            }
            actions = null;
            axisModel = null;
            buttonsModel = null;
            refreshInterface();
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
    
    public void prepareForEdit() {
        if (poll != null) {
            oldPoll.clear();
            for (String k : poll.keySet()) {
                oldPoll.put(k, poll.get(k).getPollData());
            }
        }
    }

    protected void saveMappings() {
        try {
            Element systems = (Element) doc.selectSingleNode("/systems");
            if (systems == null) {
                systems = doc.addElement("systems");
            }

            Element system = (Element) systems.selectSingleNode("system[@name='" + console.getMainSystem() + "']");
            if (system == null) {
                system = systems.addElement("system").addAttribute("name", console.getMainSystem());
            }

            Element controller = (Element) system.selectSingleNode("controller[@name='" + currentController + "']");
            if (controller == null) {
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

    private LinkedHashMap<String, String> loadCachedActions(String vehicle) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        Properties props = new Properties();
        File file = new File(CACHED_ACTIONS_FILE);
        if (!file.exists()) {
            return result;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        } catch (Exception e) {
            NeptusLog.pub().warn("Error loading cached actions", e);
            return result;
        }
        String vehicleData = props.getProperty(vehicle);
        if (vehicleData != null) {
            String[] pairs = vehicleData.split(";");
            for (String pair : pairs) {
                String[] parts = pair.split("=", 2);
                if (parts.length == 2) {
                    result.put(parts[0], parts[1]);
                }
            }
        }
        return result;
    }

    private void saveCachedActions(String vehicle, LinkedHashMap<String, String> actions) {
        Properties props = new Properties();
        File file = new File(CACHED_ACTIONS_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (Exception e) {
                // ignore
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : actions.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        props.setProperty(vehicle, sb.toString());
        // Save
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "Cached actions for vehicles");
        } catch (Exception e) {
            NeptusLog.pub().error("Error saving cached actions", e);
        }
    }

    @Subscribe
    public void on(RemoteActionsRequest message) {
        try {
            if (hasAnyEditFlag()) {
                return;
            }

            if (!message.getOp().equals(OP.REPORT)) {
                return;
            }

            System.out.println("REPORT MESSAGE RECEIVED FROM" + message.getSourceName());
            System.out.println("ACTIONS: " + message.getActions());

            if (actions == null) {
                actions = new LinkedHashMap<String, String>();
            }

            String messageSource = message.getSourceName();
            String currentMainSystem = console.getMainSystem();

            if (messageSource == null || !messageSource.equals(currentMainSystem)) {
                return;
            }

            for (Entry<String, String> entry : message.getActions().entrySet()) {
                String k = entry.getKey();
                actions.put(k, message.getActions().get(k));
            }

            saveCachedActions(messageSource, actions);

            if (actions.size() > 0) {
                if (isShowing()) {
                    SwingUtilities.invokeLater(() -> {

                        if (hasAnyEditFlag()) {
                            return;
                        }

                        mappedAxis = getMappedActions(console.getMainSystem(), currentController, ActionType.Axis);
                        mappedButtons = getMappedActions(console.getMainSystem(), currentController, ActionType.Button);

                        if (!isShowing()) {
                            return;
                        }

                        removeAll();
                        buildDialog();

                        revalidate();
                        repaint();
                        if (getParent() != null) {
                            getParent().revalidate();
                            getParent().repaint();
                        }
                        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                            @Override
                            protected Void doInBackground() throws Exception {
                                manager.fetchControllers();
                                return null;
                            }
                            @Override
                            protected void done() {
                                try {
                                    String list[] = manager.getControllerList().keySet().toArray(new String[0]);
                                    for (JComboBox<String> cb : controllerSelectors) {
                                        cb.removeAllItems();
                                        for (String s : list) {
                                            cb.addItem(s);
                                        }
                                    }
                                    for (JComboBox<String> cb : controllerSelectors) {
                                        cb.setSelectedItem(currentController);
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        };
                        worker.execute();
                    });
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
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
        float range;
        float deadZone;

        boolean editFlag = false;

        MapperComponent(final String action, String component, float value, boolean inverted, float r, float zero) {
            this.action = action;
            this.button = component;
            this.value = value;
            this.inverted = inverted;
            this.range = r;
            this.deadZone = zero;
        }

        public String getEditText() {
            return editFlag ? "Cancel" : "Edit";
        }

        public void toggleEdit() {
            if (editFlag) {
                editFlag = false;
            } else {
                clearAllEditFlags();
                editFlag = true;
            }
        }

        public void doClear() {
            this.button = "";
            this.inverted = false;
            this.value = (float) 0.0;
            this.editFlag = false;
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
    }

    @SuppressWarnings("serial")
    class BooleanRenderer extends JCheckBox implements TableCellRenderer {

        public BooleanRenderer() {
            super();
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setSelected((Boolean) value);
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    @SuppressWarnings("serial")
    class BooleanCellEditor extends AbstractCellEditor implements TableCellEditor {

        private JCheckBox check = new JCheckBox();

        public BooleanCellEditor() {
            check.setHorizontalAlignment(SwingConstants.CENTER);
            check.setOpaque(true);
            check.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            check.setSelected((Boolean) value);
            return check;
        }

        @Override
        public Object getCellEditorValue() {
            return check.isSelected();
        }
    }

    class ActionButtonEditor extends AbstractCellEditor implements TableCellEditor {

        private JButton button = new JButton();
        private MapperComponent current;
        private int column;
        private JTable editingTable;

        public ActionButtonEditor() {
            button.addActionListener(e -> {

                if (editingTable == axisTable) {

                    if (column == 4) {
                        current.toggleEdit();
                        if (current.editFlag) {
                            ControllerPanel.this.prepareForEdit();
                        }
                    }
                    else if (column == 5) {
                        current.doClear();
                    }

                } else if (editingTable == buttonsTable) {

                    if (column == 3) {
                        current.toggleEdit();
                        if (current.editFlag) {
                            ControllerPanel.this.prepareForEdit();
                        }
                    }
                    else if (column == 4) {
                        current.doClear();
                    }
                }

                fireEditingStopped();
                if (axisTable != null) {
                    axisTable.revalidate();
                    axisTable.repaint();
                }
                if (buttonsTable != null) buttonsTable.repaint();
            });
        }

        @Override
        public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {

            editingTable = table;
            column = col;
            int modelRow = table.convertRowIndexToModel(row);

            if (table == axisTable) {
                current = ((AxisTableModel) axisModel).getList().get(modelRow);
            } else {
                current = ((ButtonTableModel) buttonsModel).getList().get(modelRow);
            }

            button.setText(value.toString());
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return button.getText();
        }
    }

    @SuppressWarnings("serial")
    public class ButtonTableRenderer extends DefaultTableCellRenderer {

        public ButtonTableRenderer(ActionType type) {
            super();
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                                                                boolean isSelected, boolean hasFocus, final int row, int column) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            try {
                int modelRow = table.convertRowIndexToModel(row);
                if (modelRow >= 0 && modelRow < ((ButtonTableModel)buttonsModel).getList().size()) {
                    MapperComponent comp = ((ButtonTableModel) buttonsModel).getList().get(modelRow);

                    label.setOpaque(true);
                    if (comp.editFlag && column != 3) {
                        label.setBackground(Color.green);
                    } else if (column == 3 || column == 4) {
                        label.setBackground(UIManager.getColor("Button.background"));
                        label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                    } else {
                        label.setBackground(Color.white);
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                    }
                }
            } catch (Exception e) {
                label.setBackground(Color.WHITE);
                label.setText("");
            }

            return label;
        }
    }
    @SuppressWarnings("serial")
    public class AxisTableRenderer extends DefaultTableCellRenderer {

        public AxisTableRenderer(ActionType type) {
            super();
        }

        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                                                                boolean isSelected, boolean hasFocus, final int row, int column) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            try {
                int modelRow = table.convertRowIndexToModel(row);
                if (modelRow >= 0 && modelRow < ((AxisTableModel)axisModel).getList().size()) {
                    MapperComponent comp = ((AxisTableModel) axisModel).getList().get(modelRow);

                    label.setOpaque(true);
                    if (comp.editFlag && column != 4) {
                        label.setBackground(Color.green);
                    } else if (column == 4 || column == 5) {
                        label.setBackground(UIManager.getColor("Button.background"));
                        label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                    } else {
                        label.setBackground(Color.white);
                    }
                }
            } catch (Exception e) {
                label.setBackground(Color.WHITE);
                label.setText("");
            }

            return label;
        }
    }
}
