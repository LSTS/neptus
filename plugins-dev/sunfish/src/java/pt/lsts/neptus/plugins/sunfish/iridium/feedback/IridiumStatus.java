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
 * Author: keila
 * Feb 5, 2018
 */
package pt.lsts.neptus.plugins.sunfish.iridium.feedback;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import com.l2fprod.common.propertysheet.Property;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.util.GuiUtils;

@PluginDescription(name = "Iridium Communications Status",
        icon = "images/iridium/iridium-logo.png",
        description="Iridium Communications Feedback Panel")
@Popup(pos = Popup.POSITION.BOTTOM_RIGHT, width=355, height=215)
public class IridiumStatus extends ConsolePanel {

    private static final long serialVersionUID = 1L;
    private IridiumStatusTableModel iridiumCommsStatus;
    private JTable table; 
    private JScrollPane scroll;
    private JButton clear;
    private DefaultTableCellRenderer highlightRenderer,defaultRenderer;
    private TableRowSorter<TableModel> rowSorter;
    private TableModelListener changes;
    private int highlight_init,highlight_block_size;

    @NeptusProperty(name = "Clear button", description = "Clear button parameter to cleanup old messages. (seconds)", userLevel = NeptusProperty.LEVEL.REGULAR)
    public long secs = 3600;

    /**
     * @param console
     */
    public IridiumStatus(ConsoleLayout console) {
        super(console);

    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        iridiumCommsStatus.clear();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        removeAll();
        setLayout(new BorderLayout());

        clear =  new JButton(cleanup);
        clear.setToolTipText("Cleanup old messages after "+secs+" seconds");
        clear.setText("Clear");

        highlight_init = 0;
        highlight_block_size = 0;
        
        initTableFields();
        configureTable();

        scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(350, 200));
        add(scroll,BorderLayout.CENTER);
        add(clear,BorderLayout.SOUTH);
    }

    public void initTableFields() {
        iridiumCommsStatus =  new IridiumStatusTableModel();
        defaultRenderer = new DefaultTableCellRenderer();
        highlightRenderer = new DefaultTableCellRenderer() {

            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {

                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if(table.convertRowIndexToModel(row) >= highlight_init && 
                        table.convertRowIndexToModel(row) <=  highlight_init+highlight_block_size-1){//== table.getRowCount()-1)
                    c.setBackground(Color.GREEN.darker());
                }
                else 
                    return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                return c;
            }
        };

        changes = new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if(e.getType()==TableModelEvent.UPDATE || e.getType()==TableModelEvent.INSERT){
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if(e.getType() == TableModelEvent.INSERT){
                                highlight_block_size++;
                                synchronized (IridiumStatus.this) {
                                    int index = table.getModel().getRowCount()-1;
                                    try {
                                        if(index < table.getRowCount() && index > 0) {
                                            int row   = table.convertRowIndexToView(index);
                                            Rectangle rect = table.getCellRect(row, 0, false);
                                            if(rect != null)
                                                table.scrollRectToVisible(rect);                                        
                                        }
                                    }
                                    catch (Exception e) {
                                        NeptusLog.pub().error(e.getCause());
                                        e.printStackTrace();
                                    }
                                    table.repaint();
                                }
                            }
                        }
                    });
                }
            }
        };

        rowSorter = new TableRowSorter<TableModel>(iridiumCommsStatus);
        List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>(); 
        sortKeys.add(new RowSorter.SortKey(IridiumStatusTableModel.TIMESTAMP, SortOrder.ASCENDING));
        rowSorter.setComparator(IridiumStatusTableModel.TIMESTAMP, new Comparator <String>() {

            @Override
            public int compare(String sdf1, String sdf2) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS dd-MM-yyyy 'Z'");
                sdf1 = sdf1.replaceAll("V ", "");
                sdf2 = sdf2.replaceAll("V ", "");
                
                sdf1 = sdf1.replaceAll("M ", "");
                sdf2 = sdf2.replaceAll("M ", "");
                try {
                    return sdf.parse(sdf1).compareTo(sdf.parse(sdf2));
                }
                catch (ParseException e) {
                    NeptusLog.pub().error(I18n.text("Error parsing dates sorting table"), e);
                    e.printStackTrace();
                }

                return 0;
            }});

        rowSorter.setSortKeys(sortKeys);
    }

    /**
     * Configure JTable sorters, listeners and others settings
     */
    private void configureTable() {

        table = new JTable(iridiumCommsStatus){

            private static final long serialVersionUID = -6458618477278894325L;

            @Override
            public String getToolTipText(MouseEvent event) {
                java.awt.Point p = event.getPoint();
                int column = columnAtPoint(p);
                    if( rowAtPoint(p)> 0 && rowAtPoint(p) < table.getRowCount()) {
                        int row = table.convertRowIndexToModel(rowAtPoint(p));
                        if(row > 0 && row < table.getRowCount())
                            return iridiumCommsStatus.getToolTipText(row,column);
                   }


                return super.getToolTipText();
            }
        };
        table.getModel().addTableModelListener(changes);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class,highlightRenderer);
        table.setAutoCreateRowSorter(false);
        table.setRowSorter(rowSorter);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    displayMessage();
                }
                //reset count to last inserted row
                highlight_init = table.getRowCount() > 0 ? table.getRowCount() : 0;
                highlight_block_size = 0;
                table.repaint();
            }
        });
    }

    public void displayMessage(){
        try {
            int index = table.convertRowIndexToModel(table.getSelectedRow());
            String msg = iridiumCommsStatus.getMessageData(index); 
            JTextArea data = new JTextArea();
            data.setEditable(false);
            data.setOpaque(true);
            data.setMaximumSize(new Dimension(500,350));
            data.setText(msg);
            JScrollPane jscroll = new JScrollPane(data); 
            jscroll.setPreferredSize(new Dimension(400,400));
            String title = "Iridium Message Data";
            JOptionPane.showMessageDialog(this, jscroll, title, JOptionPane.PLAIN_MESSAGE);
        }
        catch (Exception ex) {
            GuiUtils.errorMessage(getConsole(), ex);
        }
    }

    @SuppressWarnings("serial")
    AbstractAction cleanup = new AbstractAction("Clear") {
        @Override
        public void actionPerformed(ActionEvent e) {
            long millis = 1000*secs;
            iridiumCommsStatus.cleanupAfter(millis);
        }
    };

    /**
     * Update cleanup button tooltip
     */
    @Override
    public void setProperties(Property[] properties) {
        if((long)properties[0].getValue()!= secs)
        {
            secs = (long)properties[0].getValue();
            clear.setToolTipText("Cleanup old messages after "+secs+" seconds");
        }
        super.setProperties(properties);
    }
}
