/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.mra;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import net.miginfocom.swing.MigLayout;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.gui.SelectAllFocusListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.plots.LogLocalTimeOffset;
import pt.lsts.neptus.mra.plots.MRA2DPlot;
import pt.lsts.neptus.mra.plots.MRATimeSeriesPlot;
import pt.lsts.neptus.mra.plots.TimedXYDataItem;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.llf.LsfReport;
import pt.lsts.neptus.util.llf.LsfReportProperties;
import pt.lsts.neptus.util.llf.chart.LLFChart;

/**
 * 
 * @author ZP
 */
public class MRAChartPanel extends JPanel implements ChartMouseListener {

    private static final long serialVersionUID = 1L;
    private LLFChart chart;
    private double timestep = MRAProperties.defaultTimestep;
    private ChartPanel cpanel = null;
    private IMraLogGroup source;
    private JTextField timeStepField;
    private JButton selectEntities;
    private JButton redraw;

    private InfiniteProgressPanel busyPanel;

    private JLabel lblX = new JLabel();
    private JLabel lblY = new JLabel();

    JPanel controlPanel = new JPanel();
    MRAPanel mraPanel;
    double mouseValue;

    public MRAChartPanel(LLFChart chart, IMraLogGroup source, MRAPanel panel) {
        this.mraPanel = panel;
        this.chart = chart;
        this.source = source;

        MigLayout layout = new MigLayout(
                "fill, insets 10 0 10 0",
                "[right]"
        );
        controlPanel.setLayout(layout);

        if (chart.supportsVariableTimeSteps())
            this.timestep = Math.max(chart.getDefaultTimeStep(), timestep);
        else
            this.timestep = 0;

        setLayout(new BorderLayout(2, 2));

        final Vector<LogStatisticsItem> items = chart.getStatistics();
        final String chartName = chart.getName();

        controlPanel.add(lblX);
        controlPanel.add(lblY);

        if (items != null) {
            JButton info = new JButton(I18n.text("Statistics..."));
            info.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String[][] model = new String[items.size()][2];
                    for (int i = 0; i < items.size(); i++) {
                        model[i][0] = items.get(i).getName();
                        model[i][1] = items.get(i).getValue();
                    }

                    JTable table = new JTable(model, new String[] { I18n.text("Name"), I18n.text("Value") });
                    JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(MRAChartPanel.this));
                    dialog.setTitle(I18n.textf("Statistics for %chartname", chartName));
                    JScrollPane scroll = new JScrollPane(table);
                    dialog.getContentPane().add(scroll);
                    dialog.setSize(300, 500);
                    GuiUtils.centerOnScreen(dialog);
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    dialog.setVisible(true);
                }
            });

            controlPanel.add(info, "split");
        }

        if (chart.supportsVariableTimeSteps()) {
            busyPanel = InfiniteProgressPanel.createInfinitePanelBeans(I18n.text(""), 25);
            controlPanel.add(busyPanel, "split");
            busyPanel.setVisible(false);

            redraw = new JButton(I18n.text("Redraw"));
            timeStepField = new JTextField("" + timestep, 4);
            if (MRATimeSeriesPlot.class.isAssignableFrom(chart.getClass())) {
                selectEntities = new JButton(I18n.text("Series..."));
                controlPanel.add(selectEntities, "split");
                selectEntities.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        selectEntities();
                    }
                });
            }
            controlPanel.add(new JLabel(I18n.text("Time Step:")), "split");
            controlPanel.add(timeStepField);
            controlPanel.add(redraw);

            ActionListener l = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    double val = Double.NaN;
                    try {
                        val = Double.parseDouble(timeStepField.getText());
                    }
                    catch (Exception ex) {
                        val = Double.NaN;
                    }
                    if (!(val < 0) && !Double.isNaN(val) && val != timestep) {
                        timestep = val;
                        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                            @Override
                            protected Void doInBackground() throws Exception {
                                regeneratePanel();
                                return null;
                            }

                            @Override
                            protected void done() {
                                blockButtonsUI(false);
                            }
                        };
                        blockButtonsUI(true);
                        worker.execute();
                    }
                    timeStepField.setText(timestep + "");
                }
            };

            redraw.addActionListener(l);
            timeStepField.addActionListener(l);
            timeStepField.addFocusListener(new SelectAllFocusListener());

        }
        if (chart.supportsVariableTimeSteps() || chart.getStatistics() != null)
            add(controlPanel, BorderLayout.SOUTH);

        regeneratePanel();
    }

    public ChartPanel getCpanel() {
        return cpanel;
    }

    protected void selectEntities() {
        boolean allSelected = true;
        MRATimeSeriesPlot chart = (MRATimeSeriesPlot) this.chart;
        final List<JCheckBox> listModel = new ArrayList<>();
        for (String name : chart.getSeriesNames()) {
            JCheckBox check = new JCheckBox(name);
            check.setOpaque(false);
            check.setForeground(Color.black);
            if (chart.getForbiddenSeries().contains(name)) {
                check.setSelected(false);
                allSelected = false;
            } else {
                check.setSelected(true);
            }
            listModel.add(check);
        }
        List<String> seriesPrevState = new ArrayList<>();
        for (JCheckBox chk : listModel) {
            if (chk.isSelected())
                seriesPrevState.add(chk.getText());
        }
        JCheckBox checkAll = new JCheckBox(I18n.text("ALL"));
        checkAll.setOpaque(false);
        checkAll.setForeground(Color.black);
        checkAll.setSelected(allSelected);
        listModel.add(checkAll);

        final JList<JCheckBox> listUI = new JList<>(listModel.toArray(new JCheckBox[0]));
        listUI.setCellRenderer((ListCellRenderer<Object>) (list1, value, index, isSelected, cellHasFocus) -> (JCheckBox) value);
        listUI.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedIndex = listUI.locationToIndex(e.getPoint());
                if (selectedIndex < 0)
                    return;

                JCheckBox item = (JCheckBox) listUI.getModel().getElementAt(selectedIndex);
                item.setSelected(!item.isSelected());
                listUI.setSelectedIndex(selectedIndex);
                if (!item.isSelected()) {
                    ((JCheckBox) listUI.getModel().getElementAt(listUI.getModel().getSize() - 1)).setSelected(false);
                }
                if (selectedIndex == listUI.getModel().getSize() - 1) {
                    // select all check..
                    for (int i = 0; i < listUI.getModel().getSize(); i++)
                        ((JCheckBox) listUI.getModel().getElementAt(i)).setSelected(item.isSelected());
                }

                listUI.repaint();
            }
        });
        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this));
        dialog.getContentPane().setLayout(new BorderLayout());
        JPanel inner = new JPanel();
        inner.setBackground(listUI.getBackground());
        inner.setLayout(new BoxLayout(inner, BoxLayout.PAGE_AXIS));
        dialog.getContentPane().add(inner, BorderLayout.CENTER);
        inner.add(new JScrollPane(listUI, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        inner.add(new JLabel());
        JButton okButton = new JButton(new AbstractAction(I18n.text("OK")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        GuiUtils.reactEnterKeyPress(okButton);
        dialog.getContentPane().add(okButton, BorderLayout.SOUTH);
        dialog.setSize(380, 400);
        dialog.setTitle(I18n.text("Select Entities"));
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        GuiUtils.centerParent(dialog, SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        chart.getForbiddenSeries().clear();
        for (int i = 0; i < listUI.getModel().getSize() - 1; i++) {
            JCheckBox chk = listUI.getModel().getElementAt(i);
            if (!chk.isSelected())
                chart.getForbiddenSeries().add(chk.getText());
        }

        boolean unchanged = listModel.stream().allMatch(chk -> {
            if ("ALL".equalsIgnoreCase(chk.getText())) {
                return true;
            }
            return seriesPrevState.contains(chk.getText()) == chk.isSelected();
        });

        if (unchanged)
            return;

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                regeneratePanel();
                return null;
            }

            @Override
            protected void done() {
                blockButtonsUI(false);
            }
        };
        blockButtonsUI(true);
        worker.execute();
    }

    private void blockButtonsUI(boolean block) {
        setBusy(block);
        if (timeStepField != null)
            timeStepField.setEnabled(!block);
        if (selectEntities != null)
            selectEntities.setEnabled(!block);
        if (redraw != null)
            redraw.setEnabled(!block);
    }

    private void setBusy(boolean busy) {
        if (busyPanel != null) {
            busyPanel.setVisible(busy);
            busyPanel.setBusy(busy);
        }
    }

    public void regeneratePanel() {
        if (cpanel != null)
            remove(cpanel);
        JFreeChart c = chart.getChart(source, timestep);

        cpanel = new ChartPanel(c);

        cpanel.setMouseWheelEnabled(true);
        try {
            JFreeChart cht = cpanel.getChart();
            if (cht.getPlot().getClass().isAssignableFrom(CategoryPlot.class)) {
                cht.getCategoryPlot().setRangePannable(true);
            } else if (cht.getPlot().getClass().isAssignableFrom(XYPlot.class)) {
                cht.getXYPlot().setDomainPannable(true);
                cht.getXYPlot().setRangePannable(true);
            }
        } catch (Exception e) {
            NeptusLog.pub().error("Error setting parameters to chart {}", e.getMessage());
        }

        cpanel.getPopupMenu().add(I18n.text("Add Mark")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (LsfReportProperties.generatingReport==true){
                    GuiUtils.infoMessage(mraPanel.getRootPane(), I18n.text("Can not add Marks"), I18n.text("Can not add Marks - Generating Report."));
                    return;
                }

                String res = JOptionPane.showInputDialog(I18n.text("Marker name"));
                if (res != null && !res.isEmpty())
                    mraPanel.addMarker(new LogMarker(res, mouseValue, 0, 0));
            }
        });

        cpanel.getPopupMenu().add(I18n.text("Save as PDF")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = GuiUtils.getFileChooser(source.getDir(), I18n.text("PDF files"), "pdf");
                int op = chooser.showSaveDialog(MRAChartPanel.this);
                if (op == JFileChooser.APPROVE_OPTION)
                    if(LsfReport.savePdf(source, chart, chooser.getSelectedFile()))
                        GuiUtils.infoMessage(MRAChartPanel.this, I18n.text("Save as PDF"), I18n
                                .textf("File saved successfully to %fileLocation", chooser.getSelectedFile()
                                        .getAbsolutePath()));
                    else
                        GuiUtils.errorMessage(MRAChartPanel.this, I18n.text("Save as PDF"), I18n.text("Error exporting to PDF"));
            }
        });

        cpanel.addChartMouseListener(new ChartMouseListener() {

            @Override
            public void chartMouseMoved(ChartMouseEvent e) {
                MouseEvent me = e.getTrigger();
                int x = me.getX();
                if(!cpanel.getPopupMenu().isVisible()) {
                    if(chart instanceof MRA2DPlot) {
                        ChartEntity entity = e.getEntity();

                        if (entity != null && entity instanceof XYItemEntity) {
                            XYItemEntity ent = (XYItemEntity) entity;

                            int sindex = ent.getSeriesIndex();
                            int iindex = ent.getItem();

                            mouseValue = ((TimedXYDataItem) ((XYSeriesCollection) e.getChart().getXYPlot().getDataset())
                                    .getSeries(sindex).getDataItem(iindex)).timestamp;
                        }
                    }
                    else if (e.getChart().getPlot() instanceof XYPlot) {
                        long timestamp = (long) e.getChart().getXYPlot().getDomainAxis()
                                .java2DToValue(x, cpanel.getScreenDataArea(),
                                        e.getChart().getXYPlot().getDomainAxisEdge());
                        
                        mouseValue = timestamp + LogLocalTimeOffset.getLocalTimeOffset(timestamp);
                    }
                    else if (e.getChart().getPlot() instanceof CategoryPlot){
                        long timestamp = (long) e.getChart().getCategoryPlot().getRangeAxis()
                                .java2DToValue(x, cpanel.getScreenDataArea(),
                                        e.getChart().getCategoryPlot().getRangeAxisEdge());
                        
                        mouseValue = timestamp + LogLocalTimeOffset.getLocalTimeOffset(timestamp);
                    }
                }                
            }

            @Override
            public void chartMouseClicked(ChartMouseEvent e) {
            }
        });
        add(cpanel);
        revalidate();
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent arg0) {
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent e) {
        Insets insets = cpanel.getInsets();
        int x = (int) ((e.getTrigger().getX() - insets.left) / cpanel.getScaleX());
        // int y = (int) ((e.getTrigger().getY() - insets.top) / cpanel.getScaleY());

        double dx = ((Number) e.getChart().getXYPlot().getDataset().getX(0, x)).doubleValue();
        double dy = ((Number) e.getChart().getXYPlot().getDataset().getYValue(0, x)).intValue();

        lblX.setText(dx + "");
        lblY.setText(dy + "");
    }
}
