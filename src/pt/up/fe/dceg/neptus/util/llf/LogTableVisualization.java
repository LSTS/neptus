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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: jqcorreia
 * Sep 21, 2012
 */
package pt.up.fe.dceg.neptus.util.llf;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import pt.up.fe.dceg.neptus.mra.LogMarker;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.plots.LogMarkerListener;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.util.ImageUtils;

import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

/**
 * @author jqcorreia
 * 
 */
@SuppressWarnings("serial")
public class LogTableVisualization implements MRAVisualization, LogMarkerListener {
    IMraLog log;
    MRAPanel mraPanel;
    LinkedHashMap<Integer, LogMarker> markerList = new LinkedHashMap<Integer, LogMarker>();
    protected SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");
    LogTableModel model;
    JXTable table;
    
    public LogTableVisualization(IMraLog source, MRAPanel panel) {
        this.log = source;
        this.mraPanel = panel;
    }

    @Override
    public String getName() {
        return log.name();
    }

    @Override
    public JComponent getComponent(IMraLogGroup source, double timestep) {
        model = new LogTableModel(source, log); 
        table = new JXTable(model) {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return new DefaultCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                            boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        if(column == 0) {
                            if (value != null)
                                setText(fmt.format(new Date((Long)value)));
                        }
                        if(markerList.containsKey(row)) {
                            setForeground(Color.RED);
                            setToolTipText("Marker: " + markerList.get(row).label);
                        }
                        return this;
                    }
                    
                };
            }
        };
        
        table.setHighlighters(HighlighterFactory.createAlternateStriping());
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {

                if (table.getSelectedRow() != -1 && e.getClickCount() == 2) {
                    log.firstLogEntry();
                    final int msgIndex = table.convertRowIndexToModel(table.getSelectedRow());
                    for (int i = 0; i < msgIndex; i++)
                        log.nextLogEntry();

                    mraPanel.loadVisualization(new MessageHtmlVisualization(log.getCurrentEntry()), true);
                }
            };
        });
        table.setAutoResizeMode(JXTable.AUTO_RESIZE_OFF);
        // Wrap on a JScrollPane and return
        return  new JScrollPane(table);
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
    
    public void onShow() {
        //nothing
    }

    @Override
    public void addLogMarker(LogMarker marker) {
        Long timestamp = new Double(marker.timestamp).longValue();
        for(int i = 0; i < log.getNumberOfEntries()-1; i++) {            
            if(timestamp < ((long)model.getValueAt(i, 0) - 10)) {
                markerList.put(i, marker);
                break;
            }
        }
        model.fireTableDataChanged();
    }

    @Override
    public void removeLogMarker(LogMarker marker) {
        for(Integer m : markerList.keySet()) {
            if(marker.timestamp == markerList.get(m).timestamp) {
                markerList.remove(m);
                model.fireTableDataChanged();
                break;
            }
        }
    }

    @Override
    public void GotoMarker(LogMarker marker) {
        
    }
}
