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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ProgressMonitor;
import javax.swing.Scrollable;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicButtonUI;

import org.jdesktop.swingx.JXBusyLabel;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCMessageType;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author zp
 * @author pdias
 * @author anasantos
 */
@PluginDescription(name="Export to CSV")
public class CSVExporter implements MRAExporter {

    /** Line ending to use */
    private static final String LINE_ENDING = "\r\n";
    
    private ProgressMonitor pmonitor;

    @NeptusProperty(name = "Textualize enumerations and bitfields",
            description = "If true will transform the enumerations and bitfields into the textual representation.")
    public boolean textualizeEnum = true;

    @NeptusProperty(name = "Decimal places",
            description = "Maximum number of decimal places desired. Minimum is 3. If this value is < 3 it will default to 8")
    public int maxDecimalPlace = 6;

    private IMraLogGroup source;
    private int progress;
    private int progressMax;

    private Filter filter;
    private LinkedHashMap<Short, String> entityNames = new LinkedHashMap<>();
    private HashMap<String, Set<String>> msgEntitiesMapOriginal;
    private HashMap<String, Set<String>> entityMsgsMapOriginal;
    private HashMap<String, Set<String>> msgEntitiesMap;
    private HashMap<String, Set<String>> entityMsgsMap;
    
    public CSVExporter(IMraLogGroup source) {
        super();
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
        NumberFormat doubles = MathMiscUtils.getNumberFormat(8, maxDecimalPlace);
        NumberFormat floats = MathMiscUtils.getNumberFormat(3, maxDecimalPlace);
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
        if (pMonitor != null) {
            pmonitor = pMonitor;
            pmonitor.setNote(I18n.text("Processing"));
            pmonitor.setMillisToDecideToPopup(Integer.MAX_VALUE);
        }

        if (pmonitor != null && pmonitor.isCanceled())
            return I18n.text("Cancelled by the user");

        // check decimal places property
        if(maxDecimalPlace < 3)
            maxDecimalPlace = 8;

        filter = new Filter(ConfigFetch.getSuperParentAsFrame());
        filter.updateMsgListOnInterface();

        this.progress = 0;
        this.progressMax = msgEntitiesMap.size() + entityMsgsMap.size();

        if (pmonitor != null)
            pmonitor.setMaximum(this.progressMax);

        while (filter.isShowing() && progress < progressMax && pmonitor != null && !pmonitor.isCanceled()) {
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

        filter.close();

        return I18n.text("Process complete");
    }
    
    private void putInMap(HashMap<String, Set<String>> map, String s1, String s2) {
        Set<String> set;
        if (map.containsKey(s1)) {
            set = new HashSet<>(map.get(s1));
            set.add(s2);
            map.replace(s1, set);
        }
        else {
            set = new HashSet<>();
            set.add(s2);
            map.put(s1, set);
        }
    }

    private void setMaps(CheckBoxGroup box, HashMap<String, Set<String>> map1, HashMap<String, Set<String>> map2) {
        for (int i = 0; i < box.checkBoxes.size(); i++) {
            if (!box.checkBoxes.get(i).isSelected()) {
                String key = box.checkBoxes.get(i).getText();
                if (!map1.containsKey(key))
                    continue;
                Set<String> set = map1.get(key);
                for (String s : set) {
                    if (map2.containsKey(s)) {
                        Set<String> aux = map2.get(s);
                        if (aux.contains(key)) {
                            aux.remove(key);
                            if (aux.size() == 0)
                                map2.remove(s);
                        }
                    }
                }
                map1.remove(key);
            }
        }
    }

    private void applyFilter(boolean entFiles) {
        if (entFiles)
            progressMax = msgEntitiesMap.size() + entityMsgsMap.size();
        else
            progressMax = msgEntitiesMap.size();

        File dir = new File(source.getFile("mra"), "csv");
        dir.mkdirs();

        for (String message : msgEntitiesMap.keySet()) {
            Set<String> entities = msgEntitiesMap.get(message);

            if (!filter.isVisible())
                break;

            // export
            try {
                File out = new File(dir, message + ".csv");
                BufferedWriter bw = new BufferedWriter(new FileWriter(out));
                pmonitor.setNote(I18n.textf("Exporting %message data to %csvfile...", message, out.getAbsolutePath()));
                bw.write(getHeader(message));
                for (int row = 0; row < source.getLsfIndex().getNumberOfMessages(); row++) {
                    if (source.getLsfIndex().getMessage(row).getMessageType().getShortName().equals(message)) {
                        if (entities.contains(source.getLsfIndex().entityNameOf(row))) {
                            bw.write(getLine(source.getLsfIndex().getMessage(row)));
                        }
                    }
                    if (!filter.isVisible())
                        break;
                }
                bw.close();
                progress++;
            }
            catch (Exception e) {
                e.printStackTrace();
                pmonitor.close();
            }
        }

        if (!entFiles)
            return;

        for (String entity : entityMsgsMap.keySet()) {
            Set<String> messages = entityMsgsMap.get(entity);

            if (!filter.isVisible())
                break;

            for (String message : messages) {
                // export
                try {
                    File out = new File(dir, message + "_" + entity + ".csv");
                    BufferedWriter bw = new BufferedWriter(new FileWriter(out));
                    bw.write(getHeader(message));

                    for (int row = 0; row < source.getLsfIndex().getNumberOfMessages(); row++) {
                        if (source.getLsfIndex().getMessage(row).getMessageType().getShortName().equals(message)
                                && source.getLsfIndex().entityNameOf(row).equals(entity)) {
                            bw.write(getLine(source.getLsfIndex().getMessage(row)));
                        }
                        if (!filter.isVisible())
                            break;
                    }
                    bw.close();
                    progress++;
                }
                catch (Exception e) {
                    e.printStackTrace();
                    pmonitor.close();
                }
            }
        }
    }

    @SuppressWarnings("serial")
    public class CheckBoxGroup extends JPanel {

        private JCheckBox all;
        private List<JCheckBox> checkBoxes;
        private JPanel content;

        public CheckBoxGroup(String... options) {
            checkBoxes = new ArrayList<>(25);
            setLayout(new BorderLayout());
            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 1));

            all = new JCheckBox(I18n.text("Select All"));
            all.setSelected(true);

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
                List<String> optionsList = Arrays.asList(options);
                Collections.sort(optionsList);
                optionsList.stream().forEachOrdered(op -> {
                    JCheckBox cb = new JCheckBox(op);
                    cb.setOpaque(false);
                    checkBoxes.add(cb);
                    content.add(cb, gbc);
                });

                JCheckBox cb = new JCheckBox(options[options.length - 1]);
                cb.setOpaque(false);
                checkBoxes.add(cb);
                gbc.weighty = 1;
                content.add(cb, gbc);
            }

            for (JCheckBox cb : checkBoxes) {
                cb.setSelected(true);
            }

            add(new JScrollPane(content));
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

    private class Filter extends JDialog {
        private static final long serialVersionUID = 1L;

        private JPanel mainContent, entitiesFilter, msgFilter;
        private CheckBoxGroup msgBox, entitiesBox;
        private JButton okEntityFilterButton, okMsgFilterButton, resetButton, exportButton;
        private JCheckBox entFilescheckBox, textualizeEnumChecbox;
        private JXBusyLabel busyPanel;

        public Filter(Window parent) {
            super(parent);
            setType(Type.NORMAL);
            setTitle(PluginUtils.getPluginName(CSVExporter.this.getClass()));

            msgEntitiesMapOriginal = new HashMap<>();
            entityMsgsMapOriginal = new HashMap<>();
            msgEntitiesMap = new HashMap<>();
            entityMsgsMap = new HashMap<>();
            //getMaps();

            setSize(500, 500);
            setLayout(new BorderLayout());

            JLabel empty = new JLabel("");
            empty.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            add(empty, BorderLayout.WEST);
            add(empty, BorderLayout.EAST);
            add(empty, BorderLayout.NORTH);

            mainContent = new JPanel();
            LayoutManager layout = new BoxLayout(mainContent, BoxLayout.PAGE_AXIS);
            mainContent.setLayout(layout);

            addMsgFilter();
            addEntityFilter();

            JPanel addFilter = new JPanel();
            layout = new BoxLayout(addFilter, BoxLayout.LINE_AXIS);
            addFilter.setLayout(layout);

            addFilter.add(msgFilter);
            addFilter.add(entitiesFilter);
            mainContent.add(addFilter);

            getContentPane().add(mainContent, BorderLayout.CENTER);

            JPanel footerPnl = new JPanel();

            busyPanel = InfiniteProgressPanel.createBusyAnimationInfiniteBeans(20);
            footerPnl.add(busyPanel);

            footerPnl.add(Box.createHorizontalStrut(7));

            JButton configButton = new JButton(I18n.text("Config"));
            configButton.setUI(new BasicButtonUI());
            configButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    PluginUtils.editPluginProperties(CSVExporter.this, Filter.this, true);
                }
            });
            footerPnl.add(configButton);

            textualizeEnumChecbox = new JCheckBox(I18n.text("Textualize Enum"), textualizeEnum);
            textualizeEnumChecbox.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    textualizeEnum = textualizeEnumChecbox.isSelected();
                }
            });

            entFilescheckBox = new JCheckBox(I18n.text("Export entities by file"));
            entFilescheckBox.setBounds(100, 100, 50, 50);
            footerPnl.add(entFilescheckBox);

            footerPnl.add(Box.createHorizontalStrut(7));

            resetButton = new JButton(I18n.text("Reset"));
            resetButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setButtonsEnabled(false);
                    busyPanel.setBusy(true);
                    SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            getMaps();
                            return null;
                        }

                        @Override
                        protected void done() {
                            updateInterface();
                            setButtonsEnabled(true);
                            busyPanel.setBusy(false);
                        }
                    };
                    sw.execute();
                }
            });
            footerPnl.add(resetButton, BorderLayout.EAST);

            exportButton = new JButton(I18n.text("Export"));
            exportButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setButtonsEnabled(false);
                    busyPanel.setBusy(true);
                    SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            applyMsgsFilterToMaps();
                            applyEntFilterToMaps();
                            applyFilter(entFilescheckBox.isSelected());
                            return null;
                        }

                        @Override
                        protected void done() {
                            setButtonsEnabled(true);
                            busyPanel.setBusy(false);
                        }
                    };
                    sw.execute();
                }
            });
            footerPnl.add(exportButton, BorderLayout.WEST);

            footerPnl.add(Box.createHorizontalStrut(7));

            JButton cancelButton = new JButton(I18n.text("Cancel"));
            cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Filter.this.close();
                }
            });
            GuiUtils.reactEscapeKeyPress(cancelButton);
            footerPnl.add(cancelButton);

            getContentPane().add(footerPnl, BorderLayout.SOUTH);

            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            this.setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
            setResizable(false);
            setAlwaysOnTop(true);
            setModalityType(ModalityType.MODELESS);
            setVisible(true);
        }

        void updateMsgListOnInterface() {
            resetButton.doClick();
            while(!resetButton.isEnabled()) {
                Thread.yield();
            }
        }

        void setButtonsEnabled(boolean state) {
            okEntityFilterButton.setEnabled(state);
            okMsgFilterButton.setEnabled(state);
            resetButton.setEnabled(state);
            exportButton.setEnabled(state);
        }

        private void addMsgFilter() {
            msgFilter = new JPanel();
            LayoutManager layout = new BoxLayout(msgFilter, BoxLayout.PAGE_AXIS);
            msgFilter.setLayout(layout);

            msgFilter.setBorder(BorderFactory.createTitledBorder(I18n.text("Messages")));
            msgFilter.setBounds(30, 30, 200, 200);

            Set<String> msg = msgEntitiesMap.keySet();
            msgBox = new CheckBoxGroup(msg.toArray(new String[msg.size()]));
            msgBox.setSize(new Dimension(100, 100));
            msgBox.setBounds(30, 30, 100, 100);
            msgFilter.add(msgBox);

            okMsgFilterButton = new JButton(I18n.text("Apply"));
            okMsgFilterButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    applyMsgsFilterToMaps();
                }
            });

            okMsgFilterButton.setBounds(30, 30, 100, 100);
            msgFilter.add(okMsgFilterButton);
        }

        private void getMaps() {
            if (msgEntitiesMapOriginal.isEmpty()) {
                for (int row = 0; row < source.getLsfIndex().getNumberOfMessages(); row++) {
                    if (!Filter.this.isVisible())
                        break;

                    try {
                        String message = source.getLsfIndex().getMessage(row).getMessageType().getShortName();
                        String entity = source.getLsfIndex().entityNameOf(row);
                        if (entity == null) {
                            entity = "" + source.getLsfIndex().entityOf(row);
                        }
                        putInMap(msgEntitiesMapOriginal, message, entity);
                        putInMap(entityMsgsMapOriginal, entity, message);
                    } catch (Exception e) {
                        NeptusLog.pub().warn(e.getMessage());
                    }
                }
            }
            msgEntitiesMap.clear();
            msgEntitiesMap.putAll(msgEntitiesMapOriginal);
            entityMsgsMap.clear();
            entityMsgsMap.putAll(entityMsgsMapOriginal);
        }

        private void addEntityFilter() {
            entitiesFilter = new JPanel();
            LayoutManager layout = new BoxLayout(entitiesFilter, BoxLayout.PAGE_AXIS);
            entitiesFilter.setLayout(layout);

            entitiesFilter.setBorder(BorderFactory.createTitledBorder(I18n.text("Entities")));
            entitiesFilter.setBounds(30, 30, 300, 200);

            Set<String> entities = entityMsgsMap.keySet();
            entitiesBox = new CheckBoxGroup(entities.toArray(new String[entities.size()]));
            entitiesBox.setSize(new Dimension(100, 100));
            entitiesBox.setBounds(30, 30, 100, 100);
            entitiesFilter.add(entitiesBox);

            okEntityFilterButton = new JButton(I18n.text("Apply"));
            okEntityFilterButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    applyEntFilterToMaps();
                }
            });

            okEntityFilterButton.setBounds(30, 30, 100, 100);
            entitiesFilter.add(okEntityFilterButton);
        }

        private void updateInterface() {
            msgFilter.remove(msgBox);
            Set<String> msg = msgEntitiesMap.keySet();
            msgBox = new CheckBoxGroup(msg.toArray(new String[msg.size()]));
            msgFilter.add(msgBox);
            msgFilter.add(okMsgFilterButton);
            msgFilter.revalidate();

            entitiesFilter.remove(entitiesBox);
            Set<String> entities = entityMsgsMap.keySet();
            entitiesBox = new CheckBoxGroup(entities.toArray(new String[entities.size()]));
            entitiesFilter.add(entitiesBox);
            entitiesFilter.add(okEntityFilterButton);
            entitiesBox.revalidate();
        }

        public void close() {
            setVisible(false);
            dispose();
        }

        private void applyMsgsFilterToMaps() {
            setMaps(msgBox, msgEntitiesMap, entityMsgsMap);
            updateInterface();
            progress = 0;
        }

        private void applyEntFilterToMaps() {
            setMaps(entitiesBox, entityMsgsMap, msgEntitiesMap);
            updateInterface();
            progress = 0;
        }
    }
}
