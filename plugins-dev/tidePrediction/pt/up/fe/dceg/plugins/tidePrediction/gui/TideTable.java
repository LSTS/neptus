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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: meg
 * May 15, 2013
 */
package pt.up.fe.dceg.plugins.tidePrediction.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.DateFormatter;

public class TideTable extends JTable {
    private static final long serialVersionUID = -7113734887099111002L;
    private final DateRenderer dayRenderer = new DateRenderer("dd-MM-yyyy");
    private final DateRenderer tideTimeRenderer = new DateRenderer("HH:mm");
    private final HeightRenderer heightRenderer = new HeightRenderer();
    private final DateCellEditor dayEditor = new DateCellEditor();

    /**
     * @param data
     */
    public TideTable(TableModel data) {
        super(data);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (column == 0) {
            if (row == 0) {
                // System.out.println("dayRenderer");
                return dayRenderer;
            }
            else {
                // System.out.println("tideTimeRenderer");
                return tideTimeRenderer;
            }
        }
        else if(row > 0){
            return heightRenderer;
        }
        // System.out.println("super.getCellRenderer");
        return super.getCellRenderer(row, column);
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        if (column == 0) {
            if (row == 0) {
                return dayEditor;
            }
            // else {
            // return tideTimeRenderer;
            // }
        }
        // else if(row > 0){
        // return heightRenderer;
        // }
        return super.getCellEditor(row, column);
    }
    
    public static void main(String[] args) {
        // Create new Frame
        JFrame frame = new JFrame("Debug Frame");
        frame.setSize(200, 200);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        DefaultTableModel data = new DefaultTableModel();
        // add one colunb to the data model
        data.addColumn("Testing");
        Vector<Date> dates = new Vector<Date>();
        dates.add(new Date(Calendar.getInstance().getTimeInMillis()));


        data.addRow(dates);
        data.addRow(dates);
        data.addRow(dates);

        JTable table = new JTable(data);
        DateRenderer dcr = new DateRenderer("dd-MM-yyyy");
        table.getColumnModel().getColumn(0).setCellRenderer(dcr);

        frame.add(table, BorderLayout.CENTER);
        frame.setVisible(true);
    }

}

class DateRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 5069635891296032588L;
    DateFormat formatter;

    public DateRenderer(String format) {
        super();
        formatter = new SimpleDateFormat(format);
    }

    @Override
    public void setValue(Object value) {
        setText((value == null) ? "" : formatter.format(value));
        System.out.println("DateRenderer Called for " + value);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (value instanceof Date) {
            this.setText(formatter.format((Date) value));
        }
        else {
            // System.out.println("class: " + value.getClass().getCanonicalName());
        }

        return this;
    }
}

class HeightRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 5069635891296032588L;
    NumberFormat formatter = new DecimalFormat("#.#");

    public HeightRenderer() {
        super();
    }

    @Override
    public void setValue(Object value) {
        setText((value == null) ? "" : formatter.format(value));
        // System.out.println("HeightRenderer Called for " + value);
    }
}


class DateCellEditor extends DefaultCellEditor {
    private static final long serialVersionUID = -2995449781104657808L;
    DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    public DateCellEditor() {
        super(new JFormattedTextField());
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        JFormattedTextField editor = (JFormattedTextField) super.getTableCellEditorComponent(table, value, isSelected,
                row, column);

        System.out.println("DateCellEditor getTableCellEditorComponent" + value);
        if (value instanceof Date) {
            editor.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new DateFormatter(formatter)));
            editor.setHorizontalAlignment(SwingConstants.CENTER);
            editor.setValue(value);
            table.getModel().setValueAt(value, row, column);
            // TidePrediction tide = (Date) table.getModel().getValueAt(row, column);
            // tide.setTimeAndDate((Date) value);
        }
        return editor;
    }

    @Override
    public boolean stopCellEditing() {
        try {
            // try to get the value
            this.getCellEditorValue();
            return super.stopCellEditing();
        }
        catch (Exception ex) {
            return false;
        }

    }

    @Override
    public Object getCellEditorValue() {
        // get content of textField
        String str = (String) super.getCellEditorValue();
        if (str == null) {
            return null;
        }

        if (str.length() == 0) {
            return null;
        }

        System.out.print("DateCellEditor getCellEditorValue " + str + " --> ");
        try {
            Date value = formatter.parse(str);
            System.out.println(value.toString());
            return value;

        }
        catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
