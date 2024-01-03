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
 * Author: pdias
 * 16/05/2018
 */
package pt.lsts.neptus.plugins.envdisp.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.DoubleAccumulator;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.colormap.ColorBarPainter;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.gui.ColorMapListRenderer;
import pt.lsts.neptus.gui.swing.RangeSlider;
import pt.lsts.neptus.gui.swing.SpinnerIsAdjustingUI;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.envdisp.gui.LayersListPanel.UpOrDown;
import pt.lsts.neptus.plugins.envdisp.painter.GenericNetCDFDataPainter;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.StringUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class VizConfigPanel extends JPanel {
    
    private JPanel holder;
    private GenericNetCDFDataPainter viz;
    
    private ColorBarPainter cbp;;
    
    private JCheckBox vOneButton;
    private JLabel lbl;
    private JSpinner spinnerMin;
    private JSpinner spinnerMax;
    private JButton fitRangesToDataButton;

    private JButton colormapButton;
    
    private JButton remButton;
    private JButton upButton;
    private JButton downButton;

    private JCheckBox interpolateButton;
    private JCheckBox useLog10Button;
    private JCheckBox clampToFitButton;
    private JCheckBox gradientButton;
    private JSpinner spinnerTrans;
    private JCheckBox showNumbersButton;
    
    private RangeSlider timeSlider;
    private RangeSlider depthSlider;
    
    public VizConfigPanel(JPanel holder, GenericNetCDFDataPainter viz) {
        super(new MigLayout("ins 10, fillx"));
        setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        this.holder = holder;
        this.viz = viz;
        
        initialize();
    }
    
    /**
     * @return the viz
     */
    public GenericNetCDFDataPainter getViz() {
        return viz;
    }
    
    public boolean isVizVisible() {
        return vOneButton.isSelected();
    }

    public void setVizVisible(boolean visible) {
        vOneButton.setSelected(visible);
    }
    
    private void initialize() {
        viz.setShowVarColorbar(false);
        viz.setShowVarLegend(false);
        viz.setShowVarLegendFromZoomLevel(0);

        cbp = new ColorBarPainter();
        
        vOneButton = new JCheckBox();
        vOneButton.setSelected(viz.isShowVar());
        vOneButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                viz.setShowVar(vOneButton.isSelected());
            }
        });

        lbl = new JLabel(viz.getPlotName());
        StringBuilder commentSb = new StringBuilder();
        if (!viz.getInfo().comment.isEmpty()) {
            commentSb.append("<b>");
            commentSb.append(I18n.text("Comment"));
            commentSb.append(":</b><br/>");
            commentSb.append(StringUtils.wrapEveryNChars(viz.getInfo().comment, (short) 60).replaceAll("\n", "<br/>"));
        }
        if (viz.getInfo().fileName != null && !viz.getInfo().fileName.isEmpty()) {
            commentSb.append(commentSb.length() > 0 ? "<br/>" : "");
            commentSb.append("<b>");
            commentSb.append(I18n.text("Source"));
            commentSb.append(":</b><br/>");
            commentSb.append(StringUtils.wrapEveryNChars(viz.getInfo().fileName, (short) 60).replaceAll("\n", "<br/>"));
        }
        if (commentSb.length() > 0)
            lbl.setToolTipText("<html>" + commentSb.toString());
        
        // Min/Max section
        spinnerMin = new JSpinner(new SpinnerNumberModel(viz.getMinValue(), -1E200, 1E200, 0.5));
        SpinnerIsAdjustingUI spinnerMinUIIsAdjustingUI = new SpinnerIsAdjustingUI();
        spinnerMin.setUI(spinnerMinUIIsAdjustingUI);
        spinnerMin.setValue(viz.getMinValue());
        spinnerMin.setSize(new Dimension(100, 20));
        spinnerMin.setToolTipText(I18n.text("This sets min scale value."));
        ((JSpinner.NumberEditor) spinnerMin.getEditor()).getTextField().setEditable(true);
        ((JSpinner.NumberEditor) spinnerMin.getEditor()).getTextField().setBackground(Color.WHITE);
        spinnerMin.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double val = (double) spinnerMin.getValue();
                if (!spinnerMinUIIsAdjustingUI.getValueIsAdjusting()) {
                    viz.setMinValue(val);
                    viz.getOffScreenLayer().triggerImageRebuild();
                }
                cbp.setMinVal(val);
            }
        });

        spinnerMax = new JSpinner(new SpinnerNumberModel(viz.getMaxValue(), -1E200, 1E200, 0.5));
        SpinnerIsAdjustingUI spinnerMaxUIIsAdjustingUI = new SpinnerIsAdjustingUI();
        spinnerMax.setUI(spinnerMaxUIIsAdjustingUI);
        spinnerMax.setValue(viz.getMaxValue());
        spinnerMax.setSize(new Dimension(100, 20));
        spinnerMax.setToolTipText(I18n.text("This sets max scale value."));
        ((JSpinner.NumberEditor) spinnerMax.getEditor()).getTextField().setEditable(true);
        ((JSpinner.NumberEditor) spinnerMax.getEditor()).getTextField().setBackground(Color.WHITE);
        spinnerMax.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double val = (double) spinnerMax.getValue();
                if (!spinnerMaxUIIsAdjustingUI.getValueIsAdjusting()) {
                    viz.setMaxValue(val);
                    viz.getOffScreenLayer().triggerImageRebuild();
                }
                cbp.setMaxVal(val);
            }
        });
        
        fitRangesToDataButton = new JButton(I18n.text("Fit"));
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

        colormapButton = new JButton(I18n.text("Change"));
        colormapButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPopupMenu pum = new JPopupMenu();
                JComboBox<ColorMap> combo = new JComboBox<ColorMap>(
                        ColorMap.cmaps.toArray(new ColorMap[ColorMap.cmaps.size()])) {
                    @Override
                    public Point getLocationOnScreen() {
                        return colormapButton.getLocationOnScreen();
                    }
                };
                combo.setRenderer(new ColorMapListRenderer());
                combo.setSelectedIndex(1);
                ColorMap match = ColorMap.cmaps.stream()
                        .filter(c -> viz.getColorMapVar().toString().equalsIgnoreCase(c.toString())).findFirst()
                        .orElse(null);
                if (match != null)
                    combo.setSelectedItem(viz.getColorMapVar());
                combo.addActionListener(ae -> {
                    if ((ColorMap) combo.getSelectedItem() != viz.getColorMapVar()) {
                        viz.setColorMapVar((ColorMap) combo.getSelectedItem());
                        cbp.setColormap(viz.getColorMapVar());
                        viz.getOffScreenLayer().triggerImageRebuild();
                    }
                });
                combo.showPopup();
                pum.add(combo);
                pum.show(lbl, colormapButton.getX(), colormapButton.getY());
            }
        });
        
        remButton = new JButton(I18n.text("\u00D7"));
        remButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                holder.remove(VizConfigPanel.this);
                holder.invalidate();
                holder.revalidate();
                holder.repaint();
                
                holder = null;
                viz = null;
            }
        });

        upButton = new JButton("\u2206");
        upButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                movePanel(VizConfigPanel.this, viz, UpOrDown.UP);
                saveImage(viz);
            }
        });
        downButton = new JButton("\u2207");
        downButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                movePanel(VizConfigPanel.this, viz, UpOrDown.DOWN);
            }
        });

        interpolateButton = new JCheckBox(I18n.text("Interpolate"));
        interpolateButton.setSelected(viz.isInterpolate());
        interpolateButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                viz.setInterpolate(interpolateButton.isSelected());
                viz.getOffScreenLayer().triggerImageRebuild();
            }
        });

        useLog10Button = new JCheckBox("log10");
        useLog10Button.setSelected(viz.isLogColorMap());
        useLog10Button.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                viz.setLogColorMap(useLog10Button.isSelected());
                cbp.setLog10(viz.isLogColorMap());
                viz.getOffScreenLayer().triggerImageRebuild();
            }
        });

        clampToFitButton = new JCheckBox("Clamp");
        clampToFitButton.setSelected(viz.isClampToFit());
        clampToFitButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                viz.setClampToFit(clampToFitButton.isSelected());
                cbp.setOutliersBoxFill(!viz.isClampToFit());
                viz.getOffScreenLayer().triggerImageRebuild();
            }
        });

        DoubleAccumulator savedMinValue = new DoubleAccumulator((o, n) -> n, (double) spinnerMin.getValue());
        DoubleAccumulator savedMaxValue = new DoubleAccumulator((o, n) -> n, (double) spinnerMax.getValue());
        DoubleAccumulator savedGradMinValue = new DoubleAccumulator((o, n) -> n, viz.getInfo().minGradient);
        DoubleAccumulator savedGradMaxValue = new DoubleAccumulator((o, n) -> n, viz.getInfo().maxGradient);
        ColorMap[] valAndGradColorMap = new ColorMap[2];
        Arrays.fill(valAndGradColorMap, null);
        valAndGradColorMap[1] = ColorMapFactory.getColorMapByName("CMOCEAN_amp.cpt");
        gradientButton = new JCheckBox(I18n.text("Gradient"));
        if (!viz.getInfo().validGradientData)
            gradientButton.setEnabled(false);
        gradientButton.setSelected(viz.isShowGradient());
        gradientButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                int state = e.getStateChange();
                switch (state) {
                    case ItemEvent.SELECTED:
                        savedMinValue.accumulate((double) spinnerMin.getValue());
                        savedMaxValue.accumulate((double) spinnerMax.getValue());
                        spinnerMin.setValue(savedGradMinValue.get());
                        spinnerMax.setValue(savedGradMaxValue.get());
                        valAndGradColorMap[0] = viz.getColorMapVar();
                        if (valAndGradColorMap[1]!= null) {
                            viz.setColorMapVar(valAndGradColorMap[1]);
                            cbp.setColormap(viz.getColorMapVar());
                        }
                        break;
                    case ItemEvent.DESELECTED:
                        savedGradMinValue.accumulate((double) spinnerMin.getValue());
                        savedGradMaxValue.accumulate((double) spinnerMax.getValue());
                        spinnerMin.setValue(savedMinValue.doubleValue());
                        spinnerMax.setValue(savedMaxValue.doubleValue());
                        if (valAndGradColorMap[0] != viz.getColorMapVar()) {
                            valAndGradColorMap[1] = viz.getColorMapVar();
                        }
                        else {
                            valAndGradColorMap[1] = null;
                        }
                        viz.setColorMapVar(valAndGradColorMap[0]);
                        cbp.setColormap(viz.getColorMapVar());
                        valAndGradColorMap[0] = null;
                        break;
                    default:
                        break;
                }
                viz.setShowGradient(gradientButton.isSelected());
                viz.getOffScreenLayer().triggerImageRebuild();
            }
        });

        spinnerTrans = new JSpinner(new SpinnerNumberModel(50, 5, 100, 1));
        SpinnerIsAdjustingUI spinnerTransUIIsAdjustingUI = new SpinnerIsAdjustingUI();
        spinnerTrans.setUI(spinnerTransUIIsAdjustingUI);
        spinnerTrans.setValue((int) (viz.getTransparency() / 255. * 100));
        spinnerTrans.setSize(new Dimension(50, 20));
        spinnerTrans.setToolTipText(I18n.text("This sets the transparency value."));
        ((JSpinner.NumberEditor) spinnerTrans.getEditor()).getTextField().setEditable(true);
        ((JSpinner.NumberEditor) spinnerTrans.getEditor()).getTextField().setBackground(Color.WHITE);
        spinnerTrans.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (spinnerTransUIIsAdjustingUI.getValueIsAdjusting())
                    return;
                
                int val = ((Integer) spinnerTrans.getValue()).intValue();
                viz.setTransparency((int) (255 * (val / 100.)));
                viz.getOffScreenLayer().triggerImageRebuild();
            }
        });

        showNumbersButton = new JCheckBox("Show values");
        showNumbersButton.setSelected(viz.isLogColorMap());
        showNumbersButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                viz.setShowVarLegend(showNumbersButton.isSelected());
                viz.getOffScreenLayer().triggerImageRebuild();
            }
        });
        
        long minMs = viz.getInfo().minDate.getTime();
        long maxMs = viz.getInfo().maxDate.getTime();
        JLabel timeSliderMinLabel = new JLabel(viz.getInfo().minDate.toString());
        JLabel timeSliderMaxLabel = new JLabel(viz.getInfo().maxDate.toString(), SwingConstants.RIGHT);
        double timeScale = 0.01;
        boolean validTime = viz.getInfo().minDate.getTime() != 0 && viz.getInfo().maxDate.getTime() != 0;
        timeSlider = !validTime ? new RangeSlider(0, 0) : new RangeSlider(0, (int) ((maxMs - minMs) * timeScale));
        if (!validTime)
            timeSlider.setEnabled(false);
        timeSlider.setToolTipText(String.format("<html><p>%s</p><p>%s<br/>%s<br/>%s</p>", I18n.text("Time slider"),
                I18n.text("Left/right keys for lower value change"),
                I18n.text("Shift+left/right keys for upper value change"),
                I18n.text("Control+left/right keys for window value change")));
        int timeMinV = (int) ((viz.getMinDate().getTime() - minMs) * timeScale);
        int timeMaxV = (int) ((viz.getMaxDate().getTime() - minMs) * timeScale);
        timeSlider.setUpperValue(timeMaxV);
        timeSlider.setValue(timeMinV);
        timeSlider.setMinorTickSpacing((int) ((maxMs - minMs) * timeScale * 0.02));
        timeSlider.setMajorTickSpacing((int) ((maxMs - minMs) * timeScale * 0.1));
        timeSlider.addKeyListener(new KeyAdapter() {
            RangeSlider slider = timeSlider;
            @Override
            public void keyPressed(KeyEvent e) {
                slider.setValueIsAdjusting(true);
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_KP_LEFT:
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_KP_DOWN:
                        if (e.isShiftDown())
                            slider.setUpperValue(slider.getUpperValue() - slider.getMinorTickSpacing());
                        else if (e.isControlDown()) {
                            int delta = slider.getUpperValue() - slider.getValue();
                            slider.setValue(slider.getValue() - slider.getMinorTickSpacing());
                            slider.setUpperValue(slider.getValue() + delta);
                        }
                        else
                            slider.setValue(slider.getValue() - slider.getMinorTickSpacing());
                        break;
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_KP_RIGHT:
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_KP_UP:
                        if (e.isShiftDown())
                            slider.setUpperValue(slider.getUpperValue() + slider.getMinorTickSpacing());
                        else if (e.isControlDown()) {
                            int delta = slider.getUpperValue() - slider.getValue();
                            slider.setUpperValue(slider.getUpperValue() + slider.getMinorTickSpacing());
                            slider.setValue(slider.getUpperValue() - delta);
                        }
                        else
                            slider.setValue(slider.getValue() + slider.getMinorTickSpacing());
                        break;
                    default:
                        break;
                }
                e.consume();
                super.keyPressed(e);
            }
            public void keyReleased(KeyEvent e) {
                slider.setValueIsAdjusting(false);
            }
        });
        timeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double selMin = minMs + timeSlider.getValue() / timeScale;
                double selMax = minMs + timeSlider.getUpperValue() / timeScale;
                timeSliderMinLabel.setText(new Date((long) selMin).toString());
                timeSliderMaxLabel.setText(new Date((long) selMax).toString());
                if (!((JSlider) e.getSource()).getValueIsAdjusting()) {
                    viz.setMinDate(new Date((long) selMin));
                    viz.setMaxDate(new Date((long) selMax));
                    viz.getOffScreenLayer().triggerImageRebuild();
                }
            }
        });

        double minDepth = viz.getInfo().minDepth;
        double maxDepth = viz.getInfo().maxDepth;
        JLabel depthSliderMinLabel = new JLabel(
                (Double.isFinite(minDepth) ? MathMiscUtils.round(Math.max(minDepth, viz.getMinDepth()), 1) + " m" : "n/a"));
        JLabel depthSliderMaxLabel = new JLabel(
                (Double.isFinite(maxDepth) ? MathMiscUtils.round(Math.min(maxDepth, viz.getMaxDepth()), 1) + " m" : "n/a"), SwingConstants.RIGHT);
        double depthScale = 10;
        boolean validDepth = Double.isFinite(minDepth) && Double.isFinite(maxDepth);
        depthSlider = !validDepth ? new RangeSlider(0, 0) : new RangeSlider(0, (int) ((maxDepth - minDepth) * depthScale));
        if (!validDepth)
            depthSlider.setEnabled(false);
        depthSlider.setToolTipText(String.format("<html><p>%s</p><p>%s<br/>%s<br/>%s</p>", I18n.text("Depth slider"),
                I18n.text("Left/right keys for lower value change"),
                I18n.text("Shift+left/right keys for upper value change"),
                I18n.text("Control+left/right keys for window value change")));
        int depthMinV = (int) ((viz.getMinDepth() - minDepth) * depthScale);
        int depthMaxV = (int) ((viz.getMaxDepth() - minDepth) * depthScale);
        depthSlider.setUpperValue(depthMaxV);
        depthSlider.setValue(depthMinV);
        depthSlider.setMinorTickSpacing((int) ((maxDepth - minDepth) * depthScale * 0.02));
        depthSlider.setMajorTickSpacing((int) ((maxDepth - minDepth) * depthScale * 0.1));
        depthSlider.addKeyListener(new KeyAdapter() {
            RangeSlider slider = depthSlider;
            @Override
            public void keyPressed(KeyEvent e) {
                slider.setValueIsAdjusting(true);
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_KP_LEFT:
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_KP_DOWN:
                        if (e.isShiftDown())
                            slider.setUpperValue(slider.getUpperValue() - slider.getMinorTickSpacing());
                        else if (e.isControlDown()) {
                            int delta = slider.getUpperValue() - slider.getValue();
                            slider.setValue(slider.getValue() - slider.getMinorTickSpacing());
                            slider.setUpperValue(slider.getValue() + delta);
                        }
                        else
                            slider.setValue(slider.getValue() - slider.getMinorTickSpacing());
                        break;
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_KP_RIGHT:
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_KP_UP:
                        if (e.isShiftDown())
                            slider.setUpperValue(slider.getUpperValue() + slider.getMinorTickSpacing());
                        else if (e.isControlDown()) {
                            int delta = slider.getUpperValue() - slider.getValue();
                            slider.setUpperValue(slider.getUpperValue() + slider.getMinorTickSpacing());
                            slider.setValue(slider.getUpperValue() - delta);
                        }
                        else
                            slider.setValue(slider.getValue() + slider.getMinorTickSpacing());
                        break;
                    default:
                        break;
                }
                e.consume();
                super.keyPressed(e);
            }
            public void keyReleased(KeyEvent e) {
                slider.setValueIsAdjusting(false);
            }
        });
        depthSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double selMin = minDepth + depthSlider.getValue() / depthScale;
                double selMax = minDepth + depthSlider.getUpperValue() / depthScale;
                depthSliderMinLabel.setText(MathMiscUtils.round(selMin, 1) + " m");
                depthSliderMaxLabel.setText(MathMiscUtils.round(selMax, 1) + " m");
                if (!((JSlider) e.getSource()).getValueIsAdjusting()) {
                    viz.setMinDepth(selMin);
                    viz.setMaxDepth(selMax);
                    viz.getOffScreenLayer().triggerImageRebuild();
                }
            }
        });

        // Layout
        add(vOneButton, "sg radio, spanx 6, split 3");
        add(new JLabel(LayersListPanel.VIEW_IMAGE_ICON), "");
        add(lbl, "sg name, spanx 5, grow");
        add(remButton, "align right, sg btnSmall, wrap");

        add(new JLabel(I18n.text("Min") + ":"), "sg minMaxLbl");
        add(spinnerMin, "sg minmax, w 100:100:");
        add(new JLabel(I18n.text("Max") + ":"), "sg minMaxLbl");
        add(spinnerMax, "sg minmax, w 100:100:");
        add(fitRangesToDataButton, "sg btn");
        add(showNumbersButton, "align right");
        add(upButton, "align right, sg btnSmall, wrap");
        
        add(cbp, "w 100:200:, h 40:40:, spanx 4, grow");
        add(colormapButton, "sg btn");
        add(new JLabel(I18n.text("Transp") + ":"), "split 3, align right");
        add(spinnerTrans, "align right");
        add(new JLabel("%"), "align right");
        add(downButton, "align right, sg btnSmall, wrap");
        
        add(new JLabel(I18n.text("Unit") + ":"), "spanx, split");
        add(new JLabel(viz.getInfo().unit), "w 100:100:, grow");
        add(useLog10Button, "align right");
        add(interpolateButton, "align right");
        add(gradientButton, "align right");
        add(clampToFitButton, "align right, wrap");

        if (validTime) {
            if (timeSlider.getMinimum() == timeSlider.getMaximum()) {
                timeSlider.setEnabled(false);
                timeSliderMaxLabel.setVisible(false);
            }
            add(new JLabel(I18n.text("Time") + ":"), "sg slabel, spanx, split");
            add(timeSlider, "sg sslider, growx, wrap");
            add(new JLabel(""), "sg slabel, spanx, split");
            add(timeSliderMinLabel, "sg sminmax, grow");
            add(timeSliderMaxLabel, "sg sminmax, align right, wrap");
        }
        
        if (validDepth) {
            if (depthSlider.getMinimum() == depthSlider.getMaximum()) {
                depthSlider.setEnabled(false);
                depthSliderMaxLabel.setVisible(false);
            }
            add(new JLabel(I18n.text("Depth") + ":"), "sg slabel, spanx, split");
            add(depthSlider, "sg sslider, growx, wrap");
            add(new JLabel(""), "sg slabel, spanx, split");
            add(depthSliderMinLabel, "sg sminmax, grow");
            add(depthSliderMaxLabel, "sg sminmax, align right, wrap");
        }
        
        holder.add(this, "h :30px:");
        
        this.invalidate();
        this.revalidate();
        this.repaint();
    }
    
    /**
     * @param viz2
     */
    protected void saveImage(GenericNetCDFDataPainter viz2) {
        BufferedImage image = viz.getOffScreenLayer().getCacheImg();
        if (image != null) {
            try {
                ImageIO.write(image, "PNG", new File("imgs-tmp", viz.getPlotName() + ".png"));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param hdr
     * @param viz 
     * @param upOrDown
     */
    protected void movePanel(JPanel hdr, GenericNetCDFDataPainter viz, UpOrDown upOrDown) {
        int hCount = holder.getComponentCount();
        int idxHrd = Arrays.asList(holder.getComponents()).indexOf(hdr);
        if (idxHrd < 0)
            return;
        int offsetIdx = upOrDown == UpOrDown.UP ? -1 : 1;
        holder.remove(hdr);
        int idxToReAdd = idxHrd + offsetIdx;
        idxToReAdd = Math.min(hCount - 1, idxToReAdd);
        idxToReAdd = Math.max(0, idxToReAdd);
        holder.add(hdr, idxToReAdd);
        holder.invalidate();
        holder.revalidate();
        holder.repaint();
    }
}