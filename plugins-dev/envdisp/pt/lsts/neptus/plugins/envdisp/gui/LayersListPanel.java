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
 * Author: pdias
 * Apr 25, 2018
 */
package pt.lsts.neptus.plugins.envdisp.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorBarPainter;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.envdisp.loader.NetCDFLoader;
import pt.lsts.neptus.plugins.envdisp.painter.GenericNetCDFDataPainter;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class LayersListPanel extends JPanel {
    
    private ImageIcon logoImage = new ImageIcon(ImageUtils.getScaledImage("pt/lsts/neptus/plugins/envdisp/netcdf-radar.png", 32, 32));
    
    private AtomicLong plotCounter = new AtomicLong();
    private File recentFolder = new File(".");

    private List<GenericNetCDFDataPainter> varLayersList = Collections.synchronizedList(new LinkedList<>());
    
    // GUI
    private Window parentWindow = null;
    private JPanel holder;
    private JPanel buttonBarPanel;
    private JScrollPane scrollHolder;
    private JButton addButton;
    private AbstractAction addAction;
    private ButtonGroup visibleOneButtonGroup = new ButtonGroup();

    public LayersListPanel() {
        this(null);
    }
    
    public <W extends Window> LayersListPanel(W parentWindow) {
        this.parentWindow = parentWindow;
        this.setPreferredSize(new Dimension(600, 400));
        initializeActions();
        initialize();
    }

    private void initialize() {
        setLayout(new MigLayout("ins 0, wrap 1"));

        buttonBarPanel = new JPanel(new MigLayout("ins 10"));
        
        JLabel logoLabel = new JLabel(logoImage);
        buttonBarPanel.add(logoLabel);
        
        Dimension buttonDimension = new Dimension(80, 30);
        addButton = new JButton(addAction);
        addButton.setSize(buttonDimension);
        buttonBarPanel.add(addButton);
        
        add(buttonBarPanel, "w 100%");
        
        holder = new JPanel();
        holder.setLayout(new BoxLayout(holder, BoxLayout.PAGE_AXIS));
        holder.setSize(400, 600);
        
        scrollHolder = new JScrollPane(holder);
        add(scrollHolder, "w 100%, h 100%");
        


    }
    
    private void initializeActions() {
        addAction = new AbstractAction(I18n.text("Add")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                File fx = NetCDFLoader.showChooseANetCDFToOpen(parentWindow, recentFolder);
                if (fx == null)
                    return;
                
                JButton source = (JButton) e.getSource();
                source.setEnabled(false);
                
                try {
                    NetcdfFile dataFile = NetcdfFile.open(fx.getPath());
                    
                    Variable choiceVarOpt = NetCDFLoader.showChooseVar(fx.getName(), dataFile, parentWindow);
                    if (choiceVarOpt != null) {
                        Future<GenericNetCDFDataPainter> fTask = NetCDFLoader.loadNetCDFPainterFor(fx.getPath(), dataFile,
                                choiceVarOpt.getShortName(), plotCounter.getAndIncrement());
                        SwingWorker<GenericNetCDFDataPainter, Void> sw = new SwingWorker<GenericNetCDFDataPainter, Void>() {
                            @Override
                            protected GenericNetCDFDataPainter doInBackground() throws Exception {
                                return fTask.get();
                            }
                            
                            @Override
                            protected void done() {
                                try {
                                    GenericNetCDFDataPainter viz = get();
                                    if (viz != null) {
                                        // PluginUtils.editPluginProperties(viz, parentWindow, true);
                                        addVisualizationLayer(viz);
                                    }
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error(e.getMessage(), e);
                                    GuiUtils.errorMessage(parentWindow,
                                            I18n.textf("Loading netCDF variable %s", choiceVarOpt.getShortName()),
                                            e.getMessage());
                                }
                                NetCDFLoader.deleteNetCDFUnzippedFile(fx);
                                source.setEnabled(true);
                            }
                        };
                        sw.execute();
                    }
                    
                    recentFolder = fx;
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                    source.setEnabled(true);
                    NetCDFLoader.deleteNetCDFUnzippedFile(fx);
                }
            }
        };

    }

    /**
     * @return the parentWindow
     */
    public Window getParentWindow() {
        return parentWindow;
    }
    
    /**
     * @param parentWindow the parentWindow to set
     */
    public <W extends Window> void setParentWindow(W parentWindow) {
        this.parentWindow = parentWindow;
    }
    
    /**
     * @return the varLayersList
     */
    public List<GenericNetCDFDataPainter> getVarLayersList() {
        return varLayersList;
    }
    
    private void addVisualizationLayer(GenericNetCDFDataPainter viz) {
        viz.setShowVarColorbar(false);
        viz.setShowVarLegend(false);
        viz.setShowVarLegendFromZoomLevel(0);
        
        JPanel hdr = new JPanel(new MigLayout("ins 10, fillx"));

        ColorBarPainter cbp = new ColorBarPainter();
        
        JToggleButton vOneButton = new JRadioButton();
        vOneButton.setSelected(viz.isShowVar());
        vOneButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                viz.setShowVar(vOneButton.isSelected());
            }
        });

        JLabel lbl = new JLabel(viz.getPlotName());
        lbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (!SwingUtilities.isRightMouseButton(me))
                    return;
                
                JPopupMenu pum = new JPopupMenu();
                pum.add(new AbstractAction(I18n.text("Edit")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                    }
                });
                pum.show(lbl, me.getX(), me.getY());
            }
        });
        
        // Min/Max section
        JSpinner spinnerMin = new JSpinner(new SpinnerNumberModel(viz.getMinValue(), -1E200, 1E200, 0.5));
        spinnerMin.setValue(viz.getMinValue());
        spinnerMin.setSize(new Dimension(100, 20));
        spinnerMin.setToolTipText(I18n.text("This sets min scale value."));
        ((JSpinner.NumberEditor) spinnerMin.getEditor()).getTextField().setEditable(true);
        ((JSpinner.NumberEditor) spinnerMin.getEditor()).getTextField().setBackground(Color.WHITE);
        spinnerMin.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double val = (double) spinnerMin.getValue();
                viz.setMinValue(val);
                viz.getOffScreenLayer().triggerImageRebuild();
                cbp.setMinVal(val);
            }
        });

        JSpinner spinnerMax = new JSpinner(new SpinnerNumberModel(viz.getMaxValue(), -1E200, 1E200, 0.5));
        spinnerMax.setValue(viz.getMaxValue());
        spinnerMax.setSize(new Dimension(100, 20));
        spinnerMax.setToolTipText(I18n.text("This sets max scale value."));
        ((JSpinner.NumberEditor) spinnerMax.getEditor()).getTextField().setEditable(true);
        ((JSpinner.NumberEditor) spinnerMax.getEditor()).getTextField().setBackground(Color.WHITE);
        spinnerMax.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double val = (double) spinnerMax.getValue();
                viz.setMaxValue(val);
                viz.getOffScreenLayer().triggerImageRebuild();
                cbp.setMaxVal(val);
            }
        });
        JButton fitRangesToDataButton = new JButton(I18n.text("Fit"));
        fitRangesToDataButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                spinnerMin.setValue(viz.getInfo().minVal);
                spinnerMax.setValue(viz.getInfo().maxVal);
            }
        });
        
        // ColorBar
        cbp.setColormap(viz.getColorMapVar());
        cbp.setMinVal(viz.getMinValue());
        cbp.setMaxVal(viz.getMaxValue());
        cbp.setLog10(viz.isLogColorMap());

        JButton colormapButton = new JButton(I18n.text("Change"));
        colormapButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPopupMenu pum = new JPopupMenu();
                pum.add(new AbstractAction(I18n.text("Edit")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                    }
                });
                pum.show(lbl, colormapButton.getX(), colormapButton.getY());
            }
        });
        
        JButton remButton = new JButton(I18n.text("\u00D7"));
        remButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                holder.remove(hdr);
                holder.invalidate();
                holder.revalidate();
                holder.repaint();
                varLayersList.remove(viz);
            }
        });

        JButton upButton = new JButton("\u2206");
        upButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO
                holder.invalidate();
                holder.revalidate();
                holder.repaint();
            }
        });
        JButton downButton = new JButton("\u2207");
        downButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO
                holder.invalidate();
                holder.revalidate();
                holder.repaint();
            }
        });

        JCheckBox interpolateButton = new JCheckBox(I18n.text("Interpolate"));
        interpolateButton.setSelected(viz.isInterpolate());
        interpolateButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                viz.setInterpolate(interpolateButton.isSelected());
                viz.getOffScreenLayer().triggerImageRebuild();
            }
        });

        JCheckBox useLog10Button = new JCheckBox("log10");
        useLog10Button.setSelected(viz.isLogColorMap());
        useLog10Button.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                viz.setLogColorMap(useLog10Button.isSelected());
                cbp.setLog10(viz.isLogColorMap());
                viz.getOffScreenLayer().triggerImageRebuild();
            }
        });

        JSpinner spinnerTrans = new JSpinner(new SpinnerNumberModel(50, 5, 100, 1));
        spinnerTrans.setValue((int) (viz.getTransparency() / 255. * 100));
        spinnerTrans.setSize(new Dimension(50, 20));
        spinnerTrans.setToolTipText(I18n.text("This sets min scale value."));
        ((JSpinner.NumberEditor) spinnerTrans.getEditor()).getTextField().setEditable(false);
        ((JSpinner.NumberEditor) spinnerTrans.getEditor()).getTextField().setBackground(Color.WHITE);
        spinnerTrans.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int val = ((Integer) spinnerTrans.getValue()).intValue();
                viz.setTransparency((int) (255 * (val / 100.)));
                viz.getOffScreenLayer().triggerImageRebuild();
            }
        });

        JCheckBox showNumbersButton = new JCheckBox("Show values");
        showNumbersButton.setSelected(viz.isLogColorMap());
        showNumbersButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                viz.setShowVarLegend(showNumbersButton.isSelected());
                viz.getOffScreenLayer().triggerImageRebuild();
            }
        });
        showNumbersButton.setEnabled(false);

        // Layout
        hdr.add(vOneButton, "sg radio");
        hdr.add(lbl, "sg name, spanx 5, grow");
        hdr.add(remButton, "align right, sg btnSmall, wrap");

        hdr.add(new JLabel(I18n.text("Min") + ":"), "sg minMaxLbl");
        hdr.add(spinnerMin, "sg minmax, w 100:100:");
        hdr.add(new JLabel(I18n.text("Max") + ":"), "sg minMaxLbl");
        hdr.add(spinnerMax, "sg minmax, w 100:100:");
        hdr.add(fitRangesToDataButton, "sg btn");
        hdr.add(showNumbersButton, "align right");
        hdr.add(upButton, "align right, sg btnSmall, wrap");
        
        hdr.add(cbp, "w 100:200:, h :40:, spanx 4, grow");
        hdr.add(colormapButton, "sg btn");
        hdr.add(new JLabel(I18n.text("Transp") + ":"), "split 2, align right");
        hdr.add(spinnerTrans, "align right");
        hdr.add(downButton, "align right, sg btnSmall, wrap");
        
        hdr.add(new JLabel(I18n.text("Unit") + ":"), "spanx, split");
        hdr.add(new JLabel(viz.getInfo().unit), "w 100:100:, grow");
        hdr.add(useLog10Button, "align right");
        hdr.add(interpolateButton, "align right, split");

        holder.add(hdr, "h :30px:");
        
        this.invalidate();
        this.revalidate();
        this.repaint();
        
        varLayersList.add(viz);
    }

    public static void main(String[] args) {
        GuiUtils.testFrame(new LayersListPanel(), "", 400, 300);
    }
}
