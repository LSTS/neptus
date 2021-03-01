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
 * Sep 21, 2012
 */
package pt.lsts.neptus.util.llf;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.text.Position;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.gui.swing.RangeSlider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.plots.LogMarkerListener;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author jqcorreia, Manuel R.
 *
 */
@SuppressWarnings("serial")
public class LogTableVisualization implements MRAVisualization, LogMarkerListener {

    private static final String SHOW_ICON = "images/buttons/show.png";
    private IMraLog log;
    private MRAPanel mraPanel;
    private LinkedHashMap<Integer, LogMarker> markerList = new LinkedHashMap<Integer, LogMarker>();
    protected SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");
    private IndexedLogTableModel model;
    private JXTable table;
    private TableRowSorter<IndexedLogTableModel> sorter;
    private JPanel panel = new JPanel(new MigLayout("", "[450px,grow]", "[23px][277px,grow]"));
    private FilterList filterDialog;
    private JButton btnFilter;

    private long finalTime;
    private long initTime;

    private JLabel lblInitTime = new JLabel();
    private JLabel lblFinalTime = new JLabel();
    private boolean closingUp;

    public LogTableVisualization(IMraLog source, MRAPanel panel) {
        this.log = source;
        this.mraPanel = panel;
        this.fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder(log.name());
        sb.append(" Messages");
        return sb.toString();
    }

    private void applyFilter(ArrayList<String> msgsToFilter, int initTS, int finalTS) {
        long initT = initTime + initTS - 1;
        long finalT = initTime + finalTS + 1;

        model = new IndexedLogTableModel(mraPanel.getSource(), log.name(), initT, finalT);

        table.setModel(model);
        table.revalidate();
        table.repaint();

        sorter.setModel(model);

        table.setRowSorter(sorter);
        List<RowFilter<Object,Object>> filters = new ArrayList<RowFilter<Object,Object>>(msgsToFilter.size());

        RowFilter<IndexedLogTableModel, Object> rf = null;
        //If current expression doesn't parse, don't update.
        for (String msg : msgsToFilter)
            filters.add(RowFilter.regexFilter("^"+msg+"$", 2));

        rf = RowFilter.orFilter(filters);

        if (filters.isEmpty())
            sorter.setRowFilter(null);
        else
            sorter.setRowFilter(rf);
    }

    @Override
    public Component getComponent(final IMraLogGroup source, double timestep) {

        model = new IndexedLogTableModel(source, log.name());
        table = new JXTable(model) {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return new DefaultCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                            boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        if (column == 0) {
                            if (value != null)
                                setText(fmt.format(new Date((Long) value)));
                        }
                        if (markerList.containsKey(table.getRowSorter().convertRowIndexToModel(row))) {
                            setForeground(Color.RED);
                            setToolTipText(I18n.text("Marker") + ": " + markerList.get(table.getRowSorter().convertRowIndexToModel(row)).getLabel());
                        }
                        return this;
                    }

                };
            }
        };

        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setHighlighters(HighlighterFactory.createAlternateStriping());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (table.getSelectedRow() != -1 && e.getClickCount() == 2) {
                    int msgIndex = table.convertRowIndexToModel(table.getSelectedRow());
                    mraPanel.loadVisualization(new MessageHtmlVisualization(model.getMessage(msgIndex)), true);
                }
                if(e.getButton() == MouseEvent.BUTTON3) {
                    Point point = e.getPoint();
                    int selRow = MraMessageLogTablePopupMenu.setRowSelection(table, point);
                    MraMessageLogTablePopupMenu.setAddMarkMenu(mraPanel, table, model.getMessage(selRow), point);
                }
            };
        });
        table.setAutoResizeMode(JXTable.AUTO_RESIZE_OFF);

        //remove default swingx's find
        table.getActionMap().remove("find");

        finalTime = log.getLastEntry().getTimestampMillis();
        initTime = log.firstLogEntry().getTimestampMillis();

        if ((int) (finalTime - initTime) < 0) {
            LsfIndex idx = source.getLsfIndex();
            finalTime = (long) (idx.getEndTime() * 1000.0);
            initTime = (long) (idx.getStartTime() * 1000.0);
        }

        if (finalTime < initTime) {
            return new JLabel(I18n.text("Cannot show visualization because messages are unordered"));
        }

        // Build Panel
        JPanel content = new JPanel();
        content.setBorder(new EmptyBorder(0, 2, 0, 2));
        panel.add(content, "cell 0 0,growx,aligny top");
        content.setLayout(new BorderLayout(0, 0));

        JLabel titleLabel = new JLabel(I18n.textf("%msgtype messages", log.name()));
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        content.add(titleLabel, BorderLayout.WEST);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        panel.add(new JScrollPane(table), "cell 0 1,grow");

        ArrayList<String> srcEntList = new ArrayList<>();

        for (int row = 0; row < model.getRowCount(); row++) {
            if (closingUp)
                break;

            String srcEnt = (String) model.getValueAt(row, 2); //SourceEntity

            if (srcEnt != null) {
                if (!srcEntList.contains(srcEnt))
                    srcEntList.add(srcEnt);
            }
        }

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        filterDialog = new FilterList(srcEntList, null);

        AbstractAction finder = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (filterDialog.isVisible())
                    filterDialog.setVisible(false);
                else
                    filterDialog.setVisible(true);
            }
        };

        btnFilter = new JButton(I18n.text("Filter"));
        btnFilter.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (filterDialog.isVisible())
                    filterDialog.setVisible(false);
                else
                    filterDialog.setVisible(true);
            }
        });

        content.add(btnFilter, BorderLayout.EAST);

        btnFilter.setIcon(ImageUtils.createScaleImageIcon(SHOW_ICON, 13, 13));

        table.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "finder");
        table.getActionMap().put("finder", finder);

        return panel;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return true;
    }

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("images/menus/table.png");
    }

    @Override
    public Double getDefaultTimeStep() {
        return null;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }

    @Override
    public Type getType() {
        return Type.TABLE;
    }

    @Override
    public void onCleanup() {
        mraPanel = null;
        closingUp = true;
        if (filterDialog != null)
            filterDialog.dispose();

        filterDialog = null;
    }

    @Override
    public void onHide() {
        if (filterDialog != null)
            filterDialog.dispose();

        mraPanel.getActionMap().remove("finder");
    }

    @Override
    public void onShow() {
    }

    @Override
    public void addLogMarker(LogMarker marker) {
        Long timestamp = Double.valueOf(marker.getTimestamp()).longValue();
        
        long smallestTimestampDiff = Long.MAX_VALUE;
        int iTSMarker = -1;
        
        for (int i = 0; i < log.getNumberOfEntries(); i++) {
            long timestampDiff = Math.abs(((long) model.getValueAt(table.getRowSorter().convertRowIndexToModel(i), 0)) - timestamp);
            if(timestampDiff > 500) {
                continue;
            }
            if(timestampDiff == 0) {
                iTSMarker = i;
                break;
            }
            if(smallestTimestampDiff > timestampDiff) {
                iTSMarker = i;
                smallestTimestampDiff = timestampDiff;
            }
        }
        if(iTSMarker > -1) {
            markerList.put(iTSMarker, marker);
            model.fireTableDataChanged();
        }
    }

    @Override
    public void removeLogMarker(LogMarker marker) {
        for (Integer m : markerList.keySet()) {
            if (marker.getTimestamp() == markerList.get(m).getTimestamp()) {
                markerList.remove(m);
                model.fireTableDataChanged();
                break;
            }
        }
    }

    @Override
    public void goToMarker(LogMarker marker) {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private class FilterList extends JDialog {
        private static final long serialVersionUID = 1L;
        protected JList m_list;
        private JTextField findTxtField = null;
        private RangeSlider rangeSlider;

        public FilterList(ArrayList<String> logs, Window parent) {
            super(parent, I18n.text("Filter"), ModalityType.DOCUMENT_MODAL);
            setType(Type.NORMAL);
            getContentPane().setLayout(new MigLayout("", "[240px]", "[300px]"));
            setSize(240, 330);

            ArrayList<LogItem> options = new ArrayList<>();
            Collections.sort(logs, String.CASE_INSENSITIVE_ORDER);

            for (String log : logs )
                options.add(new LogItem(log, false, true));

            m_list = new JList(options.toArray());
            CheckListCellRenderer renderer = new CheckListCellRenderer();
            m_list.setCellRenderer(renderer);
            m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            CheckListener lst = new CheckListener(this);
            m_list.addMouseListener(lst);
            m_list.addKeyListener(lst);

            JScrollPane ps = new JScrollPane();
            JPanel p = new JPanel();
            JPanel btnPanel = new JPanel();
            JPanel timeRestPanel = new JPanel();
            JButton filterBtn = new JButton(I18n.text("Filter"));
            JButton resetBtn = new JButton("Reset");

            ps.setViewportView(m_list);
            ps.setMaximumSize(new Dimension(210, 200));
            ps.setMinimumSize (new Dimension (210,200));

            p.setLayout(new BorderLayout());
            p.add(ps, BorderLayout.CENTER);
            p.setBorder(new TitledBorder(new EtchedBorder(), I18n.text("Filter messages")+":") );

            final ActionListener entAct = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int index = m_list.getNextMatch(findTxtField.getText(), 0, Position.Bias.Forward);
                    m_list.setSelectedIndex(index);
                    m_list.ensureIndexIsVisible(index);
                }
            };

            AbstractAction dlgFinderAction = new AbstractAction() {
                private final int ADDED_HEIGHT = 50;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (findTxtField != null) {
                        getContentPane().remove(findTxtField);
                        findTxtField = null;

                        Dimension szDim = FilterList.this.getSize();
                        szDim.setSize(szDim.getWidth(), szDim.getHeight() - ADDED_HEIGHT);
                        FilterList.this.setSize(szDim);
                    }
                    else {
                        findTxtField = new JTextField();
                        findTxtField.setColumns(10);
                        getContentPane().add(findTxtField, "cell 0 0,alignx center");
                        findTxtField.addActionListener(entAct);

                        Dimension szDim = FilterList.this.getSize();
                        szDim.setSize(szDim.getWidth(), szDim.getHeight() + ADDED_HEIGHT);
                        FilterList.this.setSize(szDim);
                    }
                    getContentPane().revalidate();
                    getContentPane().repaint();
                }
            };

            m_list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK), "finderDialog");
            m_list.getActionMap().put("finderDialog", dlgFinderAction);

            getContentPane().add(p, "cell 0 1,alignx left,aligny top");

            AbstractAction filterAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    filterBtn.setEnabled(false);
                    resetBtn.setEnabled(false);
                    SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            applyFilter(new ArrayList<String>(getSelectedItems()), rangeSlider.getValue(), rangeSlider.getUpperValue());
                            return null;
                        }
                        
                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            filterBtn.setEnabled(true);
                            resetBtn.setEnabled(true);
                        }
                    };
                    sw.execute();
                }
            };
            filterBtn.addActionListener(filterAction);

            AbstractAction resetAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int listSize = m_list.getModel().getSize();

                    // Get all the selected items using the indices
                    for (int i = 0; i < listSize; i++) {
                        LogItem sel = (LogItem)m_list.getModel().getElementAt(i);
                        if (sel.m_selected) {
                            sel.setSelected(false);
                        }
                    }
                    m_list.revalidate();
                    m_list.repaint();

                    rangeSlider.setValue(0);
                    rangeSlider.setUpperValue((int) (finalTime - initTime));

                    model = new IndexedLogTableModel(mraPanel.getSource(), log.name());
                    table.setModel(model);
                    table.revalidate();
                    table.repaint();

                }
            };

            resetBtn.addActionListener(resetAction);

            getContentPane().add(timeRestPanel, "flowx,cell 0 2,growx");
            timeRestPanel.setLayout(new BorderLayout());

            rangeSlider = new RangeSlider(0, (int) (finalTime - initTime));
            rangeSlider.setUpperValue((int) (finalTime - initTime));
            rangeSlider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    lblInitTime.setText(fmt.format(new Date(rangeSlider.getValue() + initTime)));
                    lblFinalTime.setText(fmt.format(new Date(rangeSlider.getUpperValue() + initTime)));
                }
            });

            rangeSlider.setValue(0);

            timeRestPanel.add(lblInitTime, BorderLayout.WEST);
            timeRestPanel.add(rangeSlider, BorderLayout.NORTH);
            timeRestPanel.add(lblFinalTime, BorderLayout.EAST);

            getContentPane().add(btnPanel, "flowx,cell 0 3,growx");
            btnPanel.setLayout(new BorderLayout(0, 0));

            btnPanel.add(filterBtn, BorderLayout.EAST);
            btnPanel.add(resetBtn, BorderLayout.WEST);

            setLocationRelativeTo(null);
            setResizable(false);
            setVisible(false);
        }

        /**
         * @return
         *
         */
        private Collection<String> getSelectedItems() {
            List<String> selected = new ArrayList<>();

            int listSize = m_list.getModel().getSize();

            // Get all the selected items using the indices
            for (int i = 0; i < listSize; i++) {
                LogItem sel = (LogItem)m_list.getModel().getElementAt(i);
                if (sel.m_selected) {
                    selected.add(sel.logName);
                }

            }
            return selected;
        }

        class CheckListCellRenderer extends JCheckBox implements ListCellRenderer {

            protected Border m_noFocusBorder = new EmptyBorder(1, 1, 1, 1);

            public CheckListCellRenderer() {
                super();
                setOpaque(true);
                setBorder(m_noFocusBorder);
            }

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                setText(value.toString());
                setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
                setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
                LogItem data = (LogItem)value;
                setSelected(data.isSelected());
                setEnabled(data.isEnabled());
                setFont(list.getFont());
                setBorder((cellHasFocus) ? UIManager.getBorder("List.focusCellHighlightBorder") : m_noFocusBorder);

                return this;
            }
        }

        private class CheckListener implements MouseListener, KeyListener {
            protected JList m_list;
            public CheckListener(FilterList parent) {
                m_list = parent.m_list;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getX() < 20)
                    doCheck();
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == ' ')
                    doCheck();
            }

            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {}

            protected void doCheck() {

                int index = m_list.getSelectedIndex();
                if (index < 0)
                    return;
                LogItem data = (LogItem)m_list.getModel().getElementAt(index);
                if (data.isEnabled())
                    data.invertSelected();
                m_list.repaint();
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

            public void setSelected(boolean selected) {
                m_selected = selected;
            }

            public void invertSelected() {
                m_selected = !m_selected;
            }

            public boolean isSelected() {
                return m_selected;
            }

            @Override
            public String toString() {
                return logName;
            }

            @Override
            public int compareTo(LogItem anotherLog) {
                return logName.compareTo(anotherLog.logName);
            }

        }
    }
}
