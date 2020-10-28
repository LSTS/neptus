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
 * Author: Margarida Faria
 * Oct 24, 2012
 */
package pt.lsts.neptus.plugins.state;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.EntityState;
import pt.lsts.imc.EntityState.STATE;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.state.EntitiesModel.DataIndex;
import pt.lsts.neptus.util.ImageUtils;


/**
 * @author Margarida Faria
 *
 */
@PluginDescription(name = "Entity Sub Panel")
public class EntitySubPanel extends ConsolePanel {

    /**
     * @param console
     */
    public EntitySubPanel(ConsoleLayout console) {
        super(console);
    }

    private static final long serialVersionUID = -2631714987613438043L;
    // Layout
    private final int PANEL_WIDTH = 300;
    private final int GAP = 5;
    private final int COMPONENT_WIDTH = PANEL_WIDTH - GAP * 2;
    private final int TABLE_HEIGHT = 200;

    // Interface
    JLabel globalState;
    JButton setEntitiesVisibility = new JButton(I18n.text("Choose entities"));
    private ImageIcon bootstrappingIcon, errorIcon, failureIcon, faultIcon;
    public enum Icons {
        BOOTSTRAPPINGICON("images/information/bootsInfo.png", I18n.text("Bootstraping")),
        ERRORICON("images/information/errorInfo.png", I18n.text("Error")),
        FAILUREICON("images/information/failureInfo.png", I18n.text("Failure")),
        FAULTICON("images/information/faultInfo.png", I18n.text("Fault"));
        private final ImageIcon icon;

        private Icons(String path, String description) {
            icon = ImageUtils.createImageIcon(path);
            icon.setDescription(description);
        }

        public ImageIcon getIcon() {
            return icon;
        }
    }

    // Data
    private final int numberOfMsgValuesStored = 7;
    EntitiesModel model;


    @Override
    public void initSubPanel() {
        removeAll();

        this.setSize(PANEL_WIDTH, 500);
        this.setBackground(Color.BLACK);

        // Build Status
        ArrayList<String> columnNames = new ArrayList<String>();
        columnNames.add(I18n.text("Hide"));
        columnNames.add(I18n.text("Entity"));
        columnNames.add(I18n.text("State"));
        columnNames.add(I18n.text("Description"));
        columnNames.add(I18n.text("\u0394t"));
        columnNames.add(I18n.text("Entity identifier"));
        model = new EntitiesModel(columnNames);
        JTable table = new EntityTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(COMPONENT_WIDTH, TABLE_HEIGHT));
        scrollPane.setMaximumSize(new Dimension(COMPONENT_WIDTH, TABLE_HEIGHT));
        scrollPane.setMinimumSize(new Dimension(COMPONENT_WIDTH, TABLE_HEIGHT));

        globalState = new JLabel(I18n.text("Error in selected entities!"));
        globalState.setForeground(Color.red);

        // Build Layout
        StringBuilder colConstraints = new StringBuilder();
        colConstraints.append("[]");
        colConstraints.append(GAP);
        colConstraints.append("[");
        colConstraints.append(" grow]");
        colConstraints.append(GAP);
        colConstraints.append("[]");
        setLayout(new MigLayout("", // Layout Constraints
                colConstraints.toString(), // Column constraints
                "[]" + GAP + "[]")); // Row constraints
        add(globalState, "span 2 ");
        add(setEntitiesVisibility, "wrap");
        add(scrollPane, "span, grow, wrap");

    }

    @Subscribe
    public void handleMessage(EntityState stateMsg) {
        STATE state = stateMsg.getState();
        String entityName = EntitiesResolver.resolveName(getMainVehicleId(), (int) stateMsg.getSrcEnt());
        if (!state.equals(STATE.NORMAL) && entityName != null) {
            UpdateWorker worker = new UpdateWorker(stateMsg);
            worker.execute();
        }

    }

    public class UpdateWorker extends SwingWorker<Void, Void> {
        private final EntityState msg;

        public UpdateWorker(EntityState msg) {
            super();
            this.msg = msg;
        }

        @Override
        public Void doInBackground() {
            int srcEntityId = msg.getSrcEnt();
            String entityName = EntitiesResolver.resolveName(getMainVehicleId(), srcEntityId);
            Object row[] = new Object[numberOfMsgValuesStored];
            // row[1] - 
            STATE state = msg.getState();
            switch (state) {
                case BOOT:
                    row[DataIndex.ICON.getIndex()] = bootstrappingIcon;
                    break;
                case ERROR:
                    row[DataIndex.ICON.getIndex()] = errorIcon;
                    break;
                case FAILURE:
                    row[DataIndex.ICON.getIndex()] = failureIcon;
                    break;
                case FAULT:
                    row[DataIndex.ICON.getIndex()] = faultIcon;
                    break;
                default:
                    break;
            }
            row[DataIndex.ENTITY_NAME.getIndex()] = entityName;
            row[DataIndex.DESCRIPTION.getIndex()] = msg.getDescription();
            row[DataIndex.ENTITY_ID.getIndex()] = srcEntityId;
            row[DataIndex.TIME_ELAPSED.getIndex()] = (short) 0;
            model.updateRow(row);
            return null;
        }

    }

    private class EntityTable extends JTable {
        private static final long serialVersionUID = 1L;
        private final Timer timer;
        private final int INITIAL_DELAY = 30000;
        private final int UPDATE_FREQUENCY = 100;

        public EntityTable(EntitiesModel model) {
            super(model);
            setPreferredScrollableViewportSize(new Dimension(1600, 70));
            setAutoCreateRowSorter(true);
            setFillsViewportHeight(true);

            // Checkbox for visibility
            TableColumn column = columnModel.getColumn(0);
            column.setPreferredWidth((int) Math.floor(PANEL_WIDTH * 0.1f));
            // Entity
            column = columnModel.getColumn(DataIndex.ENTITY_NAME.getIndex());
            column.setPreferredWidth((int) Math.floor(PANEL_WIDTH * 0.35f));
            // State
            column = columnModel.getColumn(DataIndex.ICON.getIndex());
            column.setPreferredWidth((int) Math.floor(PANEL_WIDTH * 0.1f));
            column.setResizable(false);
            // Description
            column = columnModel.getColumn(DataIndex.DESCRIPTION.getIndex());
            column.setPreferredWidth((int) Math.floor(PANEL_WIDTH * 0.3f));
            // delta t
            column = columnModel.getColumn(DataIndex.TIME_ELAPSED.getIndex());
            column.setPreferredWidth((int) Math.floor(PANEL_WIDTH * 0.15f));
            column.setResizable(false);
            // ElapsedTimeCellRenderer elapsedTime = new ElapsedTimeCellRenderer();
            // column.setCellRenderer(elapsedTime);

            // In the columnModel is what will be shown in the table
            // The model has all the data
            removeColumn(getColumnModel().getColumn(DataIndex.ENTITY_ID.getIndex()));

            timer = new Timer(UPDATE_FREQUENCY, new ElapsedTimeTimer());
            timer.setInitialDelay(INITIAL_DELAY);
            timer.start();
        }

        @Override
        // Render ImageIcons inside table
        public Class<? extends Object> getColumnClass(int column) {
            Object value = this.getValueAt(0, column);
            return (value == null ? Object.class : value.getClass());
            // Class<? extends Object> columnClass = getValueAt(0, column).getClass();
            // return columnClass;
        }

        @Override
        // Tooltip for rows
        public String getToolTipText(MouseEvent e) {
            // String tip = null;
            java.awt.Point p = e.getPoint();
            int rowIndex = rowAtPoint(p);
            if (rowIndex == -1) {
                return "";
            }

            int colIndex = columnAtPoint(p);


            Object cell = getValueAt(rowIndex, colIndex);
            if (cell instanceof String) {
                return (String) cell;
            }
            else if (cell instanceof ImageIcon) {
                return ((ImageIcon) cell).getDescription();
            }
            else if (cell instanceof Boolean) {
                Boolean cellB = (Boolean) cell;
                if (cellB) {
                    return I18n.text("Hide");
                }
                else {
                    return I18n.text("Show");
                }
            }
            else {
                return I18n.text("A new adaptor is needed!");
            }

        }

        @Override
        // Tooltip for headers
        protected JTableHeader createDefaultTableHeader() {
            return new EntityTableHeader(columnModel);
        }

        private class EntityTableHeader extends JTableHeader {
            private static final long serialVersionUID = 7972061124799780985L;

            public EntityTableHeader(TableColumnModel columnModel) {
                super(columnModel);
                setBackground(Color.WHITE);
            }

            @Override
            public String getToolTipText(MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = columnModel.getColumn(index).getModelIndex();
                return model.getColumnName(realIndex);
            }
        }

        //
        // /**
        // * This class uses render call to update the delta t (elapsed time) on the table.
        // *
        // * @author pdias
        // */
        // class ElapsedTimeCellRenderer extends JLabel implements TableCellRenderer {
        // private static final long serialVersionUID = -619157378506879550L;
        //
        // public ElapsedTimeCellRenderer() {
        // setHorizontalAlignment(JLabel.CENTER);
        // setOpaque(false); // MUST do this for background to show up.
        // }
        //
        // @Override
        // public Component getTableCellRendererComponent(JTable table, Object timems, boolean isSelected,
        // boolean hasFocus, int row, int column) {
        // Long enu = System.currentTimeMillis() - (Long) timems;
        //
        // setText(DateTimeUtil.milliSecondsToFormatedString(enu / 1000 * 1000));
        // return this;
        // }
        // }

        class ElapsedTimeTimer implements ActionListener {
            // This happens in the Swing's event dispatch thread so components can be modified freely
            @Override
            public void actionPerformed(ActionEvent e) {
                short row;
                for (int i = 0; i < dataModel.getRowCount(); i++) {
                    row = (short) getValueAt(i, DataIndex.TIME_ELAPSED.getIndex());
                    row += UPDATE_FREQUENCY / 1000;
                    setValueAt(row, i, DataIndex.TIME_ELAPSED.getIndex());
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
}
