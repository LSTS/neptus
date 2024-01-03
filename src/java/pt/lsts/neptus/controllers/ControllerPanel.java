/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

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
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.RemoteActions;
import pt.lsts.imc.RemoteActionsRequest;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.i18n.I18n;
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
 * 
 */
@Popup(pos = POSITION.TOP_RIGHT, width = 200, height = 400, accelerator = 'J')
@PluginDescription(author = "jquadrado", description = "Controllers Panel", name = "Controllers Panel", icon = "images/control-mode/teleoperation.png")
public class ControllerPanel extends ConsolePanel implements IPeriodicUpdates {
    private static final long serialVersionUID = 1L;

    private static final String ACTION_FILE_XML = "conf/controllers/actions.xml";
    private boolean sending = false;

    // Vehicle action received via RemoteActionRequest (i.e Heading=axis, Accelerate=Button)
    private LinkedHashMap<String, String> actions = new LinkedHashMap<String, String>();
    // Mapped actions based on XML actions.xml for the current vehicle and selected controller
    private ArrayList<MapperComponent> mappedActions = new ArrayList<MapperComponent>();
    // A list of actions to be added to a RemoteActions message
    private LinkedHashMap<String, String> msgActions  = new LinkedHashMap<String, String>();
    // The current controller poll
    private LinkedHashMap<String, Component> poll;

    private ArrayList<JComboBox<String>> controllerSelectors = new ArrayList<JComboBox<String>>();
    
    @SuppressWarnings("serial")
    private JTable table = new JTable() {
        public javax.swing.table.TableCellRenderer getCellRenderer(int row, int column) {
            if(column != 3)
                return renderer;
            else
                return super.getCellRenderer(row, column);
        };
    };
    
    private AbstractTableModel model;
    private TableRenderer renderer = new TableRenderer();
    
    private ControllerManager manager;
    
    private JButton btnRefresh = new JButton(new AbstractAction(I18n.text("Refresh Controllers")) {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            updateControllers();
        }
    });
    
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

//        // Create a JComboBox with a list of controllers from the manager
//        comboBox = new JComboBox<String>(manager.getControllerList().keySet().toArray(new String[0]));
//        comboBox.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                @SuppressWarnings("unchecked")
//                JComboBox<String> cb = (JComboBox<String>) e.getSource();
//                currentController = (String) cb.getSelectedItem();
//                mappedActions = getMappedActions(console.getMainSystem(), currentController);
//                buildDialog();
//            }
//        });

        controllerSelectors .add(generateControllerSelector());
        controllerSelectors .add(generateControllerSelector());
        
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
        setLayout(new MigLayout());
        model = new TableModel(mappedActions);
        table.setModel(model);
        table.addMouseListener(new JTableButtonMouseListener(table));
        
        add(new JScrollPane(table), "wrap");
        
        for(JComboBox<String> selector : controllerSelectors) { 
            add(selector, "w 200::, wrap");
        }
        
        add(btnRefresh);
        
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
                mappedActions = getMappedActions(console.getMainSystem(), currentController);
                buildDialog();
            }
        });
        return comboBox;
    }
    
    public ArrayList<MapperComponent> getMappedActions(String systemName, String controllerName) {
        ArrayList<MapperComponent> result = new ArrayList<MapperComponent>();

        for (String action : actions.keySet()) {
            MapperComponent comp = getMapperComponentByName(systemName, controllerName, action);
            if (comp == null)
                result.add(new MapperComponent(action, "", 0.0f, false));               
            else
                result.add(comp);
        }
        return result;
    }

    public MapperComponent getMapperComponentByName(String systemName, String controllerName, String actionName) {
        List<?> list = doc.selectNodes("/systems/system[@name='" + systemName + "']/controller[@name='"
                + controllerName + "']/*");
        for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
            Element el = (Element) iter.next();
            if (el.attributeValue("action").equalsIgnoreCase(actionName))
                return new MapperComponent(el.attributeValue("action"), el.attributeValue("component"), 0.0f, Boolean.parseBoolean(el.attributeValue("inverted")));
        }
        return null;
    }

    public void requestRemoteActions() {
        if (console.getMainSystem() != null) {
            IMCMessage msg = RemoteActionsRequest.create("op", 1);
            ImcMsgManager.getManager().sendMessageToSystem(msg, console.getMainSystem());
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
        
        if(console.getMainSystem() != null)
            add(new JLabel(I18n.text("Waiting for vehicle action list")));
        else
            add(new JLabel(I18n.text("No main vehicle selected in the console")));
        
        invalidate();
        revalidate();

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
        
        if(manager == null || currentController == null) {
            return true;
        }
        
        sending = dialog.isVisible();
        
        poll = manager.pollController(currentController);
        
        // Also if polling fails return true
        if(poll == null) {
            return true;
        }
        
        btnRefresh.setEnabled(!editing);
//        comboBox.setEnabled(!editing);
        
        if (editing) {
            if (oldPoll.size() == poll.size()) {
                for (String k : poll.keySet()) {
                    if (poll.get(k).getPollData() != oldPoll.get(k).floatValue()
                            && Math.abs(poll.get(k).getPollData()) == 1.0) {
                        for (MapperComponent mcomp : mappedActions) {
                            if (mcomp.editFlag) {
                                mcomp.component = k;
                                mcomp.inverted = poll.get(k).getPollData() < 0;
                                model.fireTableDataChanged();

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
                for (MapperComponent c : mappedActions) {
                    if (c.component.equals(k)) {
                        comp = c;
                        break;
                    }
                }

                if (comp != null) {
                    comp.value = poll.get(k).getPollData() * (actions.get(comp.action).equals("Axis") ? 127 : 1) * (comp.inverted ? -1 : 1);
                    ((AbstractTableModel)table.getModel()).fireTableDataChanged();
                    // Only if we are already sending that we build the msgActions LinkedHashMap
                    if (sending) {
                        msgActions.put(comp.action, comp.value + "");
                    }
                }
            }
            if (sending) {
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
            if(systems == null) {
                systems = doc.addElement("systems");
            }

            Element system = (Element) systems.selectSingleNode("system[@name='" + console.getMainSystem() + "']");
            if(system == null) {
                NeptusLog.pub().info("Adding new system to controller mapping");
                system = systems.addElement("system").addAttribute("name", console.getMainSystem());
            }
            
            Element controller = (Element) system.selectSingleNode("controller[@name='" + currentController + "']");
            if(controller == null) {
                NeptusLog.pub().info("Adding new controller to controller mapping");
                controller = system.addElement("controller").addAttribute("name", currentController);
            }

            List<?> l = controller.selectNodes("entry");
            for(int i = 0; i < l.size(); i++) {
                controller.remove((Element)l.get(i));
            }
            
            for(MapperComponent mcomp : mappedActions) {
                Element e = controller.addElement("entry");
                e.addAttribute("component", mcomp.component);
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
            e.printStackTrace();
        }
    }
    
    @Subscribe
    public void consume(RemoteActionsRequest message) {
        if(actions == null) {
            actions = new LinkedHashMap<String, String>();
        }
        for(String k: message.getActions().keySet()) {
            actions.put(k, message.getActions().get(k));
        }
        
        mappedActions = getMappedActions(console.getMainSystem(), currentController);
        buildDialog();
    }

    /**
     * @author jqcorreia
     */
    class MapperComponent {
        String action;
        String component;
        float value;
        boolean inverted;
        JButton edit;
        JButton clear;
        
        boolean editFlag = false;
        
        MapperComponent(final String action, String component, float value, boolean inverted) {
            this.action = action;
            this.component = component;
            this.value = value;
            this.inverted = inverted;
            this.edit = new JButton(I18n.text("Edit"));
            this.clear = new JButton(I18n.text("Clear"));
            
            edit.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    if(!editing) {
                        editing = true;
                        editFlag = true;
                    }
                } 
            });
            clear.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    MapperComponent.this.component = "";
                    MapperComponent.this.inverted = false;
                } 
            });

        }
    }
    
    @SuppressWarnings("serial")
    private class TableModel extends AbstractTableModel {
        public ArrayList<MapperComponent> list;
        
        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return I18n.text("Action");
                case 1:
                    return I18n.text("Component");
                case 2:
                    return I18n.text("Value");
                case 3:
                    return I18n.text("Inverted");
                default:
                    return "";
            }
        }
        
        public TableModel(ArrayList<MapperComponent> list) {
            this.list = list;
        }
        
        public ArrayList<MapperComponent> getList() {
            return list;
        }
        @Override
        public int getRowCount() {
            return list.size();
        }

        @Override
        public int getColumnCount() {
            return 6;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            MapperComponent comp = list.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return comp.action;
                case 1:
                    return comp.component;
                case 2:
                    return comp.value;
                case 3:
                    return comp.inverted;
                case 4:
                    return comp.edit;
                case 5:
                    return comp.clear;
            }
            return null;
        }
        
        public boolean isCellEditable(int row, int col) {
            return col == 3 || col == 1 || col == 4 || col == 5; // Hard-coded for now
        }

        public void setValueAt(Object value, int row, int col) {
            if(col == 3) {
                list.get(row).inverted = (Boolean)value;
                fireTableCellUpdated(row, col);
            }
        }
        
        public Class<?> getColumnClass(int c) {
            Object cl = getValueAt(0, c);
            if (cl == null)
                return Object.class;
            else
                return cl.getClass();
        }
    }
    
    @SuppressWarnings("serial")
    private class TableRenderer extends DefaultTableCellRenderer {
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, final int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            MapperComponent comp = (MapperComponent) ((TableModel)model).getList().get(row);
            if(comp.editFlag) {
                setBackground(Color.green);
            }
            else {
                setBackground(Color.white);
            }
            if(column == 4) {
                JButton b = (JButton)model.getValueAt(row, column);
                b.setEnabled(!editing); // Disable if we are editing
                return (JButton)model.getValueAt(row, column);
            }
            if(column == 5) {
                return (JButton)model.getValueAt(row, column);
            }
            return this;
        }
    }
    
    class JTableButtonMouseListener implements MouseListener {
        private JTable __table;

        private void __forwardEventToButton(MouseEvent e) {
           
          TableColumnModel columnModel = __table.getColumnModel();
          int column = columnModel.getColumnIndexAtX(e.getX());
          int row    = e.getY() / __table.getRowHeight();
          Object value;
          JButton button;
          MouseEvent buttonEvent;

          if(row >= __table.getRowCount() || row < 0 ||
             column >= __table.getColumnCount() || column < 0)
            return;

          value = __table.getValueAt(row, column);

          if(!(value instanceof JButton))
            return;

          button = (JButton)value;

          buttonEvent =
            (MouseEvent)SwingUtilities.convertMouseEvent(__table, e, button);
          button.dispatchEvent(buttonEvent);
          // This is necessary so that when a button is pressed and released
          // it gets rendered properly.  Otherwise, the button may still appear
          // pressed down when it has been released.
          __table.repaint();
        }

        public JTableButtonMouseListener(JTable table) {
          __table = table;
        }

        public void mouseClicked(MouseEvent e) {
          __forwardEventToButton(e);
        }

        public void mouseEntered(MouseEvent e) {
          __forwardEventToButton(e);
        }

        public void mouseExited(MouseEvent e) {
          __forwardEventToButton(e);
        }

        public void mousePressed(MouseEvent e) {
          __forwardEventToButton(e);
        }

        public void mouseReleased(MouseEvent e) {
          __forwardEventToButton(e);
        }
      }
}
