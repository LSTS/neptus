/*
 * Copyright (c) 2004-2018 Universidade do Porto - Faculdade de Engenharia
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
 * Jun 28, 2013
 */
package pt.lsts.neptus.mra.exporters;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCMessageType;
import pt.lsts.neptus.gui.editor.StringListEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;

import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;


/**
 * @author zp
 * @author pdias
 */
@PluginDescription(name="Export to CSV")
public class CSVExporter implements MRAExporter {

    /** Line ending to use */
    private static final String LINE_ENDING = "\r\n";

    private ArrayList<String> logList = new ArrayList<String>();

    private ProgressMonitor pmonitor;

    @NeptusProperty(name = "Message List to Export", editorClass = StringListEditor.class,
            description = "List of messages to export (comma separated values, no spaces). Use '!' at the begining to make it an exclude list.")
    public String msgList = "";

    @NeptusProperty(name = "Textualize enumerations and bitfields",
            description = "If true will transform the enumerations and bitfields into the textual representation.")
    public boolean textualizeEnum = true;

    @NeptusProperty(name = "Messages List", editorClass = StringListEditor.class,
            description = "List of messages to export (comma separated values, no spaces). Use '!' at the begining to make it an exclude list.")
    public String test = "";

    private IMraLogGroup source;
    private LinkedHashMap<Short, String> entityNames = new LinkedHashMap<>();

    private JFrame window;
    private JPanel mainContent, entitiesFilter, filter, msgFilter;

    private ArrayList<JPanel> filters = new ArrayList<>();
    private JLabel sourceEntCLabel, messagesLabel;
;
    private JComboBox<String> messagesBox;
    private CheckBoxGroup entitiesBox;
    private JPanel entities;

    private HashMap<String, String[]> filtersMap;

    public CSVExporter(IMraLogGroup source) {
        //super(ConfigFetch.getSuperParentAsFrame(), I18n.text("MRA Exporter"), ModalityType.DOCUMENT_MODAL);
        this.source = source;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    public String getHeader(String messageType) {
       IMCMessageType type = source.getLsfIndex().getDefinitions().getType(messageType);
        String ret = "timestamp (seconds since 01/01/1970), system, entity ";
        for (String field : type.getFieldNames()) {
            if (type.getFieldUnits(field) != null)
                ret += ", " + field + " (" + type.getFieldUnits(field) + ")";
            else
                ret += ", " + field;
        }
        return ret + LINE_ENDING;
    }

    public String getLine(IMCMessage m) {
        NumberFormat doubles = GuiUtils.getNeptusDecimalFormat(8);
        NumberFormat floats = GuiUtils.getNeptusDecimalFormat(3);
        String entity = entityNames.get(m.getSrcEnt());

        if (entity == null)
            entity = "" + m.getSrcEnt();

        String ret = floats.format(m.getTimestamp()) + ", " + m.getSourceName() + ", " + entity;

        for (String field : m.getFieldNames()) {
            Object v = m.getValue(field);
            if (textualizeEnum && v instanceof Number
                    && m.getMessageType().getFieldPossibleValues(field) != null) {
                if (m.getUnitsOf(field).equals("tuplelist")
                        || m.getUnitsOf(field).equals("enumerated")) {
                    String str = m.getMessageType().getFieldPossibleValues(field).get(
                            ((Number) v).longValue());
                    ret += ", " + str;
                }
                else {

                    long val = m.getLong(field);
                    String str = "";
                    for (int i = 0; i < 16; i++) {
                        long bitVal = (long) Math.pow(2, i);
                        if ((val & bitVal) > 0)
                            str += m.getMessageType().getFieldPossibleValues(field).get(bitVal) + "|";
                    }
                    str = str.replaceAll("null\\|", "");
                    str = str.replaceAll("\\|null", "");
                    if (str.length() > 0) // remove last "|"
                        str = str.substring(0, str.length() - 1);
                    ret += ", " + str;
                }
            }
            else {
                switch (m.getTypeOf(field)) {
                    case "fp32_t":
                        ret += ", " + floats.format(m.getDouble(field));
                        break;
                    case "fp64_t":
                        ret += ", " + doubles.format(m.getDouble(field));
                        break;
                    default:
                        ret += ", " + m.getAsString(field);
                        break;
                }
            }
        }
        return ret + LINE_ENDING;
    }

    @Override
    public String process(IMraLogGroup source, ProgressMonitor pMonitor) {
        pmonitor = new ProgressMonitor(ConfigFetch.getSuperParentFrame(), I18n.text("Exporting to CSV"),
                I18n.text("Starting up"), 0, source.listLogs().length);

        if (pmonitor.isCanceled())
            return I18n.text("Cancelled by the user");

        window = new JFrame();
        window.setLocationRelativeTo(ConfigFetch.getSuperParentAsFrame());
        window.setSize(500, 500);
        window.setLayout(new BorderLayout());

        JLabel empty = new JLabel("");
        empty.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        window.add(empty, BorderLayout.WEST);

        JLabel empty2 = new JLabel("");
        empty2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        window.add(empty2, BorderLayout.EAST);

        JLabel empty3 = new JLabel("");
        empty3.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        window.add(empty3, BorderLayout.NORTH);

        mainContent = new JPanel();
        LayoutManager layout = new BoxLayout(mainContent, BoxLayout.PAGE_AXIS);
        mainContent.setLayout(layout);

        filtersMap = new HashMap<>();
        addMsgFilter();
        addEntityFilter();

        filter = new JPanel();
        filter.setBorder(BorderFactory.createTitledBorder("Current filters: "));
        filter.setBounds(30,30,100,200);

        JPanel addFilter = new JPanel();
        layout = new BoxLayout(addFilter, BoxLayout.LINE_AXIS);
        addFilter.setLayout(layout);


        addFilter.add(msgFilter);
        addFilter.add(entitiesFilter);
        mainContent.add(addFilter);
        mainContent.add(filter);

        window.getContentPane().add(mainContent, BorderLayout.CENTER);

        JPanel buttonsPnl = new JPanel();
        JButton exportButton = new JButton("Export");
        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                export();
            }
        });
        buttonsPnl.add(exportButton, BorderLayout.EAST);
        window.getContentPane().add(buttonsPnl, BorderLayout.SOUTH);
        window.setVisible(true);

        while (window.isShowing() && !pmonitor.isCanceled()) {
            try {
                Thread.sleep(100);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (pmonitor.isCanceled()) {
            return I18n.text("Cancelled by the user");
        }

        return I18n.text("Process complete");
    }

    private void addMsgFilter() {
        msgFilter = new JPanel();
        LayoutManager layout = new BoxLayout(msgFilter, BoxLayout.PAGE_AXIS);
        msgFilter.setLayout(layout);

        msgFilter.setBorder(BorderFactory.createTitledBorder("By Messages: "));
        msgFilter.setBounds(30,30,200,200);

        //list of messages in this log source
        String[] logs = source.listLogs();

        ArrayList<String> options = new ArrayList<>();

        for (String log : logs ) {
            if (logList.contains(log))
                options.add(log);
            else
                options.add(log);
        }
        Arrays.sort(options.toArray());

        CheckBoxGroup msgBox = new CheckBoxGroup(options.toArray( new String[options.size()]));
        msgBox.setSize(new Dimension(100,100));
        msgBox.setBounds(30,30,100,100);
        msgFilter.add(msgBox);

        JButton saveButton = new JButton("OK");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                for(int i = 0; i < msgBox.checkBoxes.size(); i++){
                    if(msgBox.checkBoxes.get(i).isSelected()) {
                        filtersMap.put(msgBox.checkBoxes.get(i).getText(), null);
                    }
                }
                setCurrentFilters();
            }
        });
        saveButton.setBounds(30,30,100,100);
        msgFilter.add(saveButton);
    }

    private void addEntityFilter() {
        entitiesFilter = new JPanel();
        LayoutManager layout = new BoxLayout(entitiesFilter, BoxLayout.PAGE_AXIS);
        entitiesFilter.setLayout(layout);

        entitiesFilter.setBorder(BorderFactory.createTitledBorder("By Entities: "));
        entitiesFilter.setBounds(30,30,300,200);
        
        JPanel msgPnl = new JPanel();
        JPanel entiPnl = new JPanel();
        JPanel buttonsPnl2 = new JPanel();

        messagesLabel = new JLabel(I18n.text("Messages"));

        //list of messages in this log source
        String[] logs = source.listLogs();
        messagesBox = generateMessageSelector(logs);
        messagesBox.setBounds(50, 50, 200, 20);
        
        msgPnl.add(messagesLabel);
        msgPnl.add(messagesBox);
        entitiesFilter.add(msgPnl);

        entities = new JPanel();
        sourceEntCLabel = new JLabel(I18n.text("Entity"));
        populateSourceEntity(messagesBox.getSelectedItem().toString());
        messagesBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    populateSourceEntity(messagesBox.getSelectedItem().toString());
                    window.repaint();
                }

            }
        });
        entitiesBox.setSize(new Dimension(100,100));

        entiPnl.add(sourceEntCLabel);
        entiPnl.add(entities);
        entitiesFilter.add(entiPnl);

        JButton saveButton = new JButton("OK");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                List<String> selected = new ArrayList<>();
                for(int i = 0; i < entitiesBox.checkBoxes.size(); i++){
                    if(entitiesBox.checkBoxes.get(i).isSelected()) {
                        selected.add(entitiesBox.checkBoxes.get(i).getText());
                    }
                }
                filtersMap.put(messagesBox.getSelectedItem().toString(), selected.stream().toArray(String[]::new));
                setCurrentFilters();
            }
        });

        buttonsPnl2.add(saveButton);
        entitiesFilter.add(buttonsPnl2);
    }

    private void setCurrentFilters() {
        filter.removeAll();

        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Message");
        model.addColumn("Entities");

        for (String message : filtersMap.keySet()) {
            List<String> entities;
            if(filtersMap.get(message) == null)
                model.addRow(new Object[]{message, "all"});
            else {
                entities = new ArrayList<String>(Arrays.asList(filtersMap.get(message)));
                model.addRow(new Object[]{message, entities.toString()});
            }
        }

        JTable table = new JTable(model);
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(80);
        columnModel.getColumn(1).setPreferredWidth(260);

        filter.add(table);
        filter.revalidate();
    }

    private void export() {
        for (String message : filtersMap.keySet()) {
            List<String> entities = new ArrayList<String>(Arrays.asList(filtersMap.get(message)));

            File dir = new File(source.getFile("mra"), "csv");
            dir.mkdirs();

            //export
            try {

                File out = new File(dir, message + ".csv");
                BufferedWriter bw = new BufferedWriter(new FileWriter(out));
                pmonitor.setNote(I18n.textf("Exporting %message data to %csvfile...", message, out.getAbsolutePath()));
                bw.write(getHeader(message));

                int total = source.getLsfIndex().getNumberOfMessages();
                String[] s = new String[total];
                for (int row = 0; row < source.getLsfIndex().getNumberOfMessages(); row++) {
                    if (source.getLsfIndex().getMessage(row).getMessageType().getShortName().equals(message)) {
                        if (entities.contains(source.getLsfIndex().entityNameOf(row))) {
                            bw.write(getLine(source.getLsfIndex().getMessage(row)));
                        }
                    }
                }
                bw.close();
            } catch (Exception e) {
                e.printStackTrace();
                pmonitor.close();
            }
        }
    }

    private void populateSourceEntity(String messageName) {
        System.out.println("populateSourceEntity:::::" + messageName);

        int total = source.getLsfIndex().getNumberOfMessages();
        String[] s = new String[total];
        for (int row = 0; row < source.getLsfIndex().getNumberOfMessages(); row++) {
            if(source.getLsfIndex().getMessage(row).getMessageType().getShortName().equals(messageName))
               s[row] = source.getLsfIndex().entityNameOf(row);
        }

        Set<String> temp = new LinkedHashSet<String>( Arrays.asList( s ) );
        String[] result = temp.toArray( new String[temp.size()] );

        if(entitiesBox != null)
            entitiesBox.removeAll();

        entitiesBox = new CheckBoxGroup(result);
        entitiesBox.setSizeAll();
        entities.add(entitiesBox);
        entitiesBox.revalidate();
    }

    public JComboBox<String> generateMessageSelector(String[] logs) {
        ArrayList<CSVExporter.LogItem> options = new ArrayList<>();

        for (String log : logs ) {
            if (logList.contains(log))
                options.add(new CSVExporter.LogItem(log, true, false));
            else
                options.add(new CSVExporter.LogItem(log, false, true));
        }
        Arrays.sort(options.toArray());

        JComboBox<String> comboBox = new JComboBox(options.toArray());

        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                @SuppressWarnings("unchecked")
                JComboBox<String> cb = (JComboBox<String>) e.getSource();
            }
        });
        return comboBox;
    }

    public class CheckBoxGroup extends JPanel {

        private JCheckBox all;
        private List<JCheckBox> checkBoxes;
        private JPanel content;

        public CheckBoxGroup(String... options) {
            checkBoxes = new ArrayList<>(25);
            setLayout(new BorderLayout());
            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));
            all = new JCheckBox("Select All...");
            all.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (JCheckBox cb : checkBoxes) {
                        cb.setSelected(all.isSelected());
                    }
                }
            });
            header.add(all);
            add(header, BorderLayout.NORTH);

            content = new ScrollablePane(new GridBagLayout());
            content.setBackground(UIManager.getColor("List.background"));
            if (options.length > 0) {

                GridBagConstraints gbc = new GridBagConstraints();
                gbc.gridwidth = GridBagConstraints.REMAINDER;
                gbc.anchor = GridBagConstraints.NORTHWEST;
                gbc.weightx = 1;
                for (int index = 1; index < options.length - 1; index++) {
                    JCheckBox cb = new JCheckBox(options[index]);
                    cb.setOpaque(false);
                    checkBoxes.add(cb);
                    content.add(cb, gbc);

                    System.out.println("opção "+ index + ": " + options[index]);
                }

                JCheckBox cb = new JCheckBox(options[options.length - 1]);
                cb.setOpaque(false);
                checkBoxes.add(cb);
                gbc.weighty = 1;
                content.add(cb, gbc);

            }

            add(new JScrollPane(content));
        }

        public void setSizeAll() {
            all.setBounds(50, 50, 100, 20);
        }

        public class ScrollablePane extends JPanel implements Scrollable {

            public ScrollablePane(LayoutManager layout) {
                super(layout);
            }

            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return new Dimension(200, 150);
            }

            @Override
            public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
                return 32;
            }

            @Override
            public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
                return 32;
            }

            @Override
            public boolean getScrollableTracksViewportWidth() {
                boolean track = false;
                Container parent = getParent();
                if (parent instanceof JViewport) {
                    JViewport vp = (JViewport) parent;
                    track = vp.getWidth() > getPreferredSize().width;
                }
                return track;
            }

            @Override
            public boolean getScrollableTracksViewportHeight() {
                boolean track = false;
                Container parent = getParent();
                if (parent instanceof JViewport) {
                    JViewport vp = (JViewport) parent;
                    track = vp.getHeight() > getPreferredSize().height;
                }
                return track;
            }

        }

    }


    private class LogItem implements Comparable<LogItem> {
        protected String logName;
        protected boolean m_selected;
        protected boolean m_enabled;

        public LogItem(String name, boolean selected, boolean enabled) {
            this.logName = name;
            this.m_selected = selected;
            this.m_enabled = enabled;
        }

        public boolean isEnabled() {
            return m_enabled;
        }

        @SuppressWarnings("unused")
        public String getName() { return logName; }

        @SuppressWarnings("unused")
        public void setSelected(boolean selected) {
            m_selected = selected;
        }

        public void invertSelected() {
            m_selected = !m_selected;
        }

        public boolean isSelected() {
            return m_selected;
        }

        public String toString() {
            return logName;
        }

        @Override
        public int compareTo(CSVExporter.LogItem anotherLog) {
            return logName.compareTo(anotherLog.logName);
        }
    }
}
