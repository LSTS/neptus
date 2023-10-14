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
 * 2008/04/15
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.gui.StatusLed;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.Enumerated;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author pdias
 */
@Popup( pos = POSITION.TOP_LEFT, width=400, height=400, accelerator='E')
@PluginDescription(name = "Entities", icon = "images/buttons/events.png", documentation = "entity-state/entity-state.html")
public class EntityStatePanel extends ConsolePanel implements NeptusMessageListener {

    private static final long serialVersionUID = 5150530667334313096L;

    private final Color COLOR_OFF = Color.GRAY;
    private final Color COLOR_GREEN = new Color(0, 200, 125);
    private final Color COLOR_BLUE = Color.BLUE;
    private final Color COLOR_YELLOW = new Color(200, 200, 0);
    private final Color COLOR_ORANGE = new Color(255, 127, 0); // Color(255, 180, 0);
    private final Color COLOR_RED = Color.RED;

    private final Icon ICON_CLEAR = ImageUtils.getScaledIcon("images/buttons/clear.png", 16, 16);

    // Events Data
    private LinkedHashMap<String, EntityStateType> dataMap = new LinkedHashMap<String, EntityStateType>();
    private Vector<EntityStateType> data = null;
    private EntityStateTableModel etmodel = new EntityStateTableModel();
    private HashMap<Long, Color> eColor = new HashMap<Long, Color>();
    private HashMap<Long, Short> eLevel = new HashMap<Long, Short>();
    private Timer timer = null;
    private TimerTask ttask = null;

    // GUI Components
    private JTable table = null;
    private StatusLed status;

    /**
     * @param console
     */
    @SuppressWarnings("serial")
    public EntityStatePanel(ConsoleLayout console) {
        super(console);
        this.removeAll();
        
        data = new Vector<EntityStateType>() {
            @Override
            public synchronized boolean add(EntityStateType e) {
                if (data.isEmpty()) {
                    super.add(e);
                    return true;
                }
                
                int index = -1;
                for (int i = 0; i < data.size(); i++) {
                    int val = data.get(i).getEntity().compareTo(e.getEntity());
                    if (val >= 0) {
                        index = i;
                        break;
                    }
                }
                if (index >= data.size() || index < 0)
                    super.add(e);
                else
                    super.add(index, e);
                return true;
            }
            
            @Override
            public synchronized void addElement(EntityStateType obj) {
                super.add(obj);
            }
        };
        
        this.setup();
        JTable table = getTable();
        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
        
        // Add the scroll pane to this panel.
        this.setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);
        
        ToolbarButton clearButton = new ToolbarButton(new AbstractAction("clear", ICON_CLEAR) {
            private static final long serialVersionUID = 1L;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                clearData();
            }
        });
        clearButton.setToolTipText(I18n.text("Clear table"));
        status = new StatusLed();
        status.made5LevelIndicator();
        status.setLevel(StatusLed.LEVEL_OFF);
        JPanel wPanel = new JPanel();
        wPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        wPanel.add(status);
        wPanel.add(clearButton);
        this.add(wPanel, BorderLayout.NORTH);

    }

    @Override
    public void initSubPanel() {
        getTimer().scheduleAtFixedRate(getTtask(), 100, 1000);
    }

    private void setup() {
        eColor.put(0L, COLOR_BLUE);
        eColor.put(1L, COLOR_GREEN);
        eColor.put(2L, COLOR_YELLOW);
        eColor.put(3L, COLOR_ORANGE);
        eColor.put(4L, COLOR_RED);
        eColor.put(5L, COLOR_RED);
        eColor.put(6L, COLOR_OFF);
        eColor.put(7L, COLOR_OFF);

        eLevel.put(0L, StatusLed.LEVEL_1);
        eLevel.put(1L, StatusLed.LEVEL_0);
        eLevel.put(2L, StatusLed.LEVEL_2);
        eLevel.put(3L, StatusLed.LEVEL_3);
        eLevel.put(4L, StatusLed.LEVEL_4);
        eLevel.put(5L, StatusLed.LEVEL_4);
        eLevel.put(6L, StatusLed.LEVEL_NONE);
        eLevel.put(7L, StatusLed.LEVEL_OFF);
    }

    /**
     * @param value
     * @return
     */
    private short mapValueToWarningLevel(short value) {
        if (eLevel.containsKey((long) value))
            return eLevel.get((long) value);

        return value;
    }
    
    @Override
    public void cleanSubPanel() {
        if (ttask != null) {
            ttask.cancel();
            ttask = null;
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        clearData(); // calling this to remove alarms
    }

    /**
     * @return the timer
     */
    private Timer getTimer() {
        if (timer == null)
            timer = new Timer("EventsTableSubPanel [" + this.hashCode() + "]");
        return timer;
    }

    /**
     * @return the ttask
     */
    private TimerTask getTtask() {
        if (ttask == null) {
            ttask = new TimerTask() {

                public void run() {
                    boolean needCalc = true;
                    try {
                        for (int i = 0; i < data.toArray(new EntityStateType[0]).length; i++) {
                            etmodel.fireTableCellUpdated(i, EntityStateType.TIME_COL);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (needCalc)
                        calcTotalState();
                }
            };
        }
        return ttask;
    }

    /**
     * @return the table
     */
    private JTable getTable() {
        if (table == null) {
            table = new JTable(etmodel);
            table.setPreferredScrollableViewportSize(new Dimension(500, 70));
            table.setFillsViewportHeight(true);
            table.setDefaultRenderer(Enumerated.class, new EnumeratedTableCellRenderer());
            table.setDefaultRenderer(Long.class, new HourTableCellRenderer());
            table.setDefaultRenderer(JLabel.class, new JLabelTableCellRenderer());

            // table.setRowSorter(new TableRowSorter<EntityStateTableModel>(etmodel)); //FIXME Problem with clear
            table.setAutoCreateRowSorter(true);

            TableColumn col = ((DefaultTableColumnModel) table.getColumnModel()).getColumn(EntityStateType.STATE_COL);
            col.setPreferredWidth(30);
            col = ((DefaultTableColumnModel) table.getColumnModel()).getColumn(EntityStateType.DONT_CARE_FLAG_COL);
            col.setPreferredWidth(10);
            col = ((DefaultTableColumnModel) table.getColumnModel()).getColumn(EntityStateType.TIME_COL);
            col.setPreferredWidth(20);
        }
        return table;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.AlarmProvider#getAlarmMessage()
     */
    public String getAlarmMessage() {
        return status.getMessage();
    }

    public String getAlarmName() {
        return I18n.text("Entity State Alarm");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.AlarmProvider#sourceState()
     */
    public int sourceState() {
        return StatusLed.LEVEL_NONE;
    }

    private synchronized void calcTotalState() {
        short max = -1;
        String evtLabel = "";
        for (EntityStateType en : data.toArray(new EntityStateType[0])) {
            if (en.isDontCare())
                continue;
            Enumerated evt = en.getState();
            Short value;
            try {
                value = (short) evt.intValue();
                value = mapValueToWarningLevel(value);
            }
            catch (Exception e) {
                NeptusLog.pub().error(EntityStatePanel.class.getSimpleName() + "calcTotalState: " + e.getMessage());
                continue;
            }
            if (value != null) {
                if (value > max) {
                    max = value;
                    evtLabel = evt.toString();
                }
            }
        }
        // short oldState = status.getLevel();
        status.setLevel(max);
        status.setMessage(I18n.textf("State '%state'", evtLabel));

    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange evt) {
        clearData();
    }

    private void clearData() {
        if (data.size() != 0)
            etmodel.fireTableRowsDeleted(0, data.size() - 1);
        data.clear();
        dataMap.clear();
//        etmodel.fireTableDataChanged();
        etmodel.fireTableStructureChanged();
        calcTotalState();
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "EntityState" };
    }

    public void messageArrived(IMCMessage message) {
        String entityName = EntitiesResolver.resolveName(getMainVehicleId(), message.getHeader().getInteger("src_ent"));
        if (entityName != null) {
            // Add/Update entity state
            EntityStateType eType = dataMap.get(entityName);
            Integer index = eType == null ? null : data.indexOf(eType);

            // Updating (not the first time receiving for this entity)
            if (index != null) {
                eType = data.get(index);
                if (message.getLong("state") != eType.getState().longValue()) { // Means it has changed, time to post a
//                    msg_type type = msg_type.info;
                }
                eType.update(entityName, new Enumerated(message.getMessageType().getFieldPossibleValues("state"),
                        message.getLong("state")), message.getString("description"), System.currentTimeMillis());
                data.set(index, eType);
                etmodel.fireTableRowsUpdated(index, index);
            }
            else {
                eType = new EntityStateType(entityName, new Enumerated(message.getMessageType().getFieldPossibleValues(
                        "state"), message.getLong("state")), getDescription(), System.currentTimeMillis());

                if (data.add(eType)) {
                    index = data.indexOf(eType);
                    dataMap.put(entityName, eType);
                    etmodel.fireTableRowsInserted(index, index);
                }
            }
            calcTotalState();
        }
    }

    /**
     * This is for representing an {@link EntityStateType} in a JTable.
     * 
     * @author pdias
     * 
     */
    class EntityStateTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 4368923349985497368L;

        private String[] columnNames = { I18n.text("Entity"), I18n.text("State"), "x", I18n.text("Description"),
                "\u2206t" };
        boolean DEBUG = false;

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (data.size() < rowIndex + 1)
                return null;
            Object obj = data.get(rowIndex).getElement(columnIndex);
            return obj;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        /*
         * JTable uses this method to determine the default renderer/ editor for each cell. If we didn't implement this
         * method, then the last column would contain text ("true"/"false"), rather than a check box.
         */
        public Class<?> getColumnClass(int c) {
            Object cl = getValueAt(0, c);
            if (cl == null)
                return Object.class;
            else
                return cl.getClass();
        }

        /*
         * Don't need to implement this method unless your table's editable.
         */
        public boolean isCellEditable(int row, int col) {
            // Note that the data/cell address is constant,
            // no matter where the cell appears onscreen.
            if (col == EntityStateType.DONT_CARE_FLAG_COL) {
                return true;
            }
            else {
                return false;
            }
        }

        /*
         * Don't need to implement this method unless your table's data can change.
         */
        public void setValueAt(Object value, int row, int col) {
            if (DEBUG) {
                NeptusLog.pub().info("<###>Setting value at " + row + "," + col + " to " + value + " (an instance of "
                        + value.getClass() + ")");
            }

            data.get(row).setElement(col, value);
            fireTableCellUpdated(row, col);
        }
    }

    /**
     * This represents a entity state
     * 
     * @author pdias
     * 
     */
    class EntityStateType {
        public static final int ENTITY_COL = 0, STATE_COL = 1, DONT_CARE_FLAG_COL = 2, DESCRIPTION_COL = 3,
                TIME_COL = 4;

        JLabel entity = new JLabel("");
        Enumerated state = null;
        boolean dontCare = false;
        JLabel description = new JLabel("");
        long timeDelta = 0;

        public EntityStateType(String entity, Enumerated state, String description, long timeDelta) {
            setEntity(entity);
            setState(state);
            setDescription(description);
            setTimeDelta(timeDelta);
        }

        public void update(String entity, Enumerated state, String description, long timeDelta) {
            setEntity(entity);
            setState(state);
            setDescription(description);
            setTimeDelta(timeDelta);
        }

        /**
         * @return the device
         */
        public String getEntity() {
            return entity.getText();
        }

        /**
         * @param entity the device to set
         */
        public void setEntity(String entity) {
            this.entity.setText(entity);
            this.entity.setToolTipText(entity);
        }

        /**
         * @return the event
         */
        public Enumerated getState() {
            return state;
        }

        /**
         * @param state the event to set
         */
        public void setState(Enumerated state) {
            this.state = state;
        }

        /**
         * @return the dontCare
         */
        public boolean isDontCare() {
            return dontCare;
        }

        /**
         * @param dontCare the dontCare to set
         */
        public void setDontCare(boolean dontCare) {
            this.dontCare = dontCare;
        }

        /**
         * @return the description
         */
        public String getDescription() {
            return description.getText();
        }

        /**
         * @param description the description to set
         */
        public void setDescription(String description) {
            this.description.setText(description);
            this.description.setToolTipText(description);
        }

        /**
         * @return the timeDelta
         */
        public long getTimeDelta() {
            return timeDelta;
        }

        /**
         * @param timeDelta the timeDelta to set
         */
        public void setTimeDelta(long timeDelta) {
            this.timeDelta = timeDelta;
        }

        public void setElement(int columnIndex, Object value) {
            switch (columnIndex) {
                case ENTITY_COL:
                    entity = (JLabel) value;
                    break;
                case STATE_COL:
                    state = (Enumerated) value;
                    break;
                case DONT_CARE_FLAG_COL:
                    dontCare = (Boolean) value;
                    break;
                case DESCRIPTION_COL:
                    description = (JLabel) value;
                    break;
                case TIME_COL:
                    timeDelta = (Long) value;
                    break;
                default:
                    break;
            }
        }

        public Object getElement(int columnIndex) {
            switch (columnIndex) {
                case ENTITY_COL:
                    return entity;
                case STATE_COL:
                    return state;
                case DONT_CARE_FLAG_COL:
                    return dontCare;
                case DESCRIPTION_COL:
                    return description;
                case TIME_COL:
                    return timeDelta;
                default:
                    return null;
            }
        }
    }

    /**
     * This class if for render the EntityState.state
     * 
     * @author pdias
     * 
     */
    class EnumeratedTableCellRenderer extends JLabel implements TableCellRenderer {

        private static final long serialVersionUID = -619157378506879550L;

        public EnumeratedTableCellRenderer() {
            // this.isBordered = isBordered;
            setOpaque(true); // MUST do this for background to show up.
        }

        public Component getTableCellRendererComponent(JTable table, Object eventType, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Enumerated enu = (Enumerated) eventType;
            setText(enu.toString());

            Color newColor = eColor.get(enu.getCurrentValue());
            if (newColor == null)
                newColor = COLOR_OFF;
            setBackground(newColor);
            setForeground(Color.WHITE);
            setToolTipText("");
            return this;
        }
    }

    /**
     * This is for render JLabels.
     * 
     * @author pdias
     * 
     */
    @SuppressWarnings("serial")
    class JLabelTableCellRenderer extends JLabel implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            return (JLabel) value;
        }
    }

    /**
     * This class is for render delta time.
     * 
     * @author pdias
     * 
     */
    class HourTableCellRenderer extends JLabel implements TableCellRenderer {

        private static final long serialVersionUID = -619157378506879550L;

        public HourTableCellRenderer() {
            setOpaque(false); // MUST do this for background to show up.
        }

        public Component getTableCellRendererComponent(JTable table, Object timems, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Long enu = System.currentTimeMillis() - (Long) timems;
            setText(DateTimeUtil.milliSecondsToFormatedString(enu / 1000 * 1000));
            return this;
        }
    }
}
