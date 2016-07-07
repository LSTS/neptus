/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Correia
 * Sep 21, 2012
 */
package pt.lsts.neptus.util.llf;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;

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
 * @author jqcorreia
 * 
 */
@SuppressWarnings("serial")
public class LogTableVisualization implements MRAVisualization, LogMarkerListener {
    private IMraLog log;
    private MRAPanel mraPanel;
    private LinkedHashMap<Integer, LogMarker> markerList = new LinkedHashMap<Integer, LogMarker>();
    protected SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");
    private IndexedLogTableModel model;
    private JXTable table;

    private JPanel panel = new JPanel(new MigLayout());
    private RangeSlider rangeSlider;

    private JButton btnFilter = new JButton(new AbstractAction(I18n.text("Filter")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            long initTime = log.firstLogEntry().getTimestampMillis();
            model = new IndexedLogTableModel(mraPanel.getSource(), log.name(), initTime + rangeSlider.getValue(),
                    initTime + rangeSlider.getUpperValue());
            table.setModel(model);
            table.revalidate();
            table.repaint();
        }
    });

    private long finalTime;
    private long initTime;

    private JLabel lblInitTime = new JLabel();
    private JLabel lblFinalTime = new JLabel();

    public LogTableVisualization(IMraLog source, MRAPanel panel) {
        this.log = source;
        this.mraPanel = panel;
        this.fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public String getName() {
        return log.name();
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
                        if (markerList.containsKey(row)) {
                            setForeground(Color.RED);
                            setToolTipText(I18n.text("Marker") + ": " + markerList.get(row).getLabel());
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
            };
        });
        table.setAutoResizeMode(JXTable.AUTO_RESIZE_OFF);

        LsfIndex idx = source.getLsfIndex();

        finalTime = (long) (idx.getEndTime() * 1000.0);
        initTime = (long) (idx.getStartTime() * 1000.0);

        if (finalTime < initTime) {
            return new JLabel(I18n.text("Cannot show visualization because messages are unordered"));
        }
        
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

        // Build Panel
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0),
                I18n.textf("%msgtype messages", log.name())));
        panel.add(new JScrollPane(table), "w 100%, h 100%, wrap");
        panel.add(lblInitTime, "split");
        panel.add(rangeSlider, "w 100%");
        panel.add(lblFinalTime, "");
        panel.add(btnFilter, "wrap");

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
    }

    @Override
    public void onHide() {
    }

    @Override
    public void onShow() {
    }

    @Override
    public void addLogMarker(LogMarker marker) {
        Long timestamp = new Double(marker.getTimestamp()).longValue();
        for (int i = 0; i < log.getNumberOfEntries() - 1; i++) {
            if (timestamp < ((long) model.getValueAt(i, 0) - 10)) {
                markerList.put(i, marker);
                break;
            }
        }
        model.fireTableDataChanged();
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
}
