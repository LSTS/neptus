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
 * Author: jqcorreia
 * Mar 15, 2013
 */
package pt.lsts.neptus.plugins.sidescan;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.swing.RangeSlider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.sidescan.SidescanPanel.InteractionMode;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author jqcorreia
 *
 */
public class SidescanToolbar extends JToolBar {
    private static final long serialVersionUID = 1L;

    List<SidescanPanel> panelList = new ArrayList<>();

    ButtonGroup bgroup = new ButtonGroup();

    JToggleButton btnMeasure = new JToggleButton(I18n.text("Length"));
    JToggleButton btnMeasureHeight = new JToggleButton(I18n.text("Height"));
    JToggleButton btnInfo = new JToggleButton(I18n.text("Info"));
    JToggleButton btnZoom = new JToggleButton(I18n.text("Zoom"));
    JToggleButton btnMark = new JToggleButton(I18n.text("Mark"));
    JToggleButton btnRecord = new JToggleButton(I18n.text("Record"));

    // Normalization.
    private final JLabel lblNormalization = new JLabel(I18n.text("Normalization"));
    private final SpinnerNumberModel modelNormalization = new SpinnerNumberModel(0.0, 0.0, 100.0, 0.01);
    private final JSpinner spinNormalization = new JSpinner();

    // TVG.
    private final JLabel lblTVG = new JLabel(I18n.textc("TVG", "Time Variable Gain"));
    private final SpinnerNumberModel modelTVG = new SpinnerNumberModel(0.0, -1000.0, 1000.0, 1.0);
    private final JSpinner spinTVG = new JSpinner();

    JToggleButton btnAutoEgn = new JToggleButton(I18n.text("EGN"));
    JToggleButton btnLogarithmicDecompression = new JToggleButton(I18n.text("DEC"));
    final JSpinner spinLogarithmicDecompression = new JSpinner();
    private final SpinnerNumberModel modelLogarithmicDecompression = new SpinnerNumberModel(5.0, 0.1, 100.0, 0.1);

    RangeSlider windowSlider = new RangeSlider(0, 100);

    JButton btnConfig = new JButton(new AbstractAction(I18n.textc("Config", "Configuration")) {
        private static final long serialVersionUID = -878895322319699542L;

        @Override
        public void actionPerformed(ActionEvent e) {
            if (panelList.isEmpty()) {
                return;
            }

            SidescanPanel panel = panelList.get(0);
            PropertiesEditor.editProperties(panel.config, SwingUtilities.getWindowAncestor(panel), true);
            panel.config.saveProps();

            if (panel.config.tvgGain != (Double) spinTVG.getValue())
                spinTVG.setValue(panel.config.tvgGain);

            if (panel.config.normalization != (Double) spinNormalization.getValue())
                spinTVG.setValue(panel.config.normalization);
        }
    });

    ActionListener alMode = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            SidescanPanel.InteractionMode imode = SidescanPanel.InteractionMode.NONE;

            if (btnInfo.isSelected())
                imode = InteractionMode.INFO;
            if (btnMark.isSelected())
                imode = InteractionMode.MARK;
            if (btnMeasure.isSelected())
                imode = InteractionMode.MEASURE;
            if (btnMeasureHeight.isSelected())
                imode = InteractionMode.MEASURE_HEIGHT;

            for (SidescanPanel panel : panelList) {
                panel.setInteractionMode(imode);
                panel.setZoom(btnZoom.isSelected());
            }
        };
    };

    private final ChangeListener alGains = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            for (SidescanPanel panel : panelList) {
                panel.config.tvgGain = (Double) spinTVG.getValue();
                panel.config.normalization = (Double) spinNormalization.getValue();
                panel.record(btnRecord.isSelected());
            }
        }
    };

    private final ChangeListener autoEgnChangeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            boolean btnState = !btnAutoEgn.isSelected();
            spinNormalization.setEnabled(btnState);
            spinTVG.setEnabled(btnState);
            windowSlider.setEnabled(btnState);
            btnLogarithmicDecompression.setEnabled(btnState);
            spinLogarithmicDecompression.setEnabled(btnState);
        }
    };

    private final ChangeListener logarithmicDecompressionChangeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            boolean btnState = !btnLogarithmicDecompression.isSelected();
            spinNormalization.setEnabled(btnState);
            spinTVG.setEnabled(btnState);
            windowSlider.setEnabled(btnState);
            btnAutoEgn.setEnabled(btnState);
        }
    };

    public SidescanToolbar(SidescanPanel... panel) {
        super();
        for (SidescanPanel p : panel) {
            this.panelList.add(p);
        }
        this.spinNormalization.setModel(modelNormalization);
        this.spinTVG.setModel(modelTVG);
        this.spinLogarithmicDecompression.setModel(modelLogarithmicDecompression);
        buildToolbar();
    }

    private void buildToolbar() {
        btnInfo.setSelected(true);
        bgroup.add(btnInfo);
        bgroup.add(btnMeasure);
        bgroup.add(btnMeasureHeight);
        bgroup.add(btnMark);
        add(btnInfo);
        add(btnMeasure);
        add(btnMeasureHeight);
        add(btnMark);
        add(btnZoom);

        addSeparator();
        add(lblNormalization);
        add(spinNormalization);

        add(lblTVG);
        add(spinTVG);
        btnAutoEgn.setToolTipText("Empirical Gain Normalization");
        add(btnAutoEgn);
        add(spinLogarithmicDecompression);
        btnLogarithmicDecompression.setToolTipText("Logarithmic Decompression");
        add(btnLogarithmicDecompression);

        windowSlider.setToolTipText(String.format("<html><p>%s</p><p>%s<br/>%s<br/>%s</p>", I18n.text("Window slider"),
                I18n.text("Left/right keys for lower value change"),
                I18n.text("Shift+left/right keys for upper value change"),
                I18n.text("Control+left/right keys for window value change")));
        windowSlider.setUpperValue(100);
        windowSlider.setValue(0);
        windowSlider.setMinorTickSpacing(5);
        windowSlider.setMajorTickSpacing(20);
        windowSlider.addKeyListener(new KeyAdapter() {
            RangeSlider slider = windowSlider;
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
        windowSlider.addChangeListener(e -> {
            double selMin = windowSlider.getValue() / 100.0;
            double selMax = windowSlider.getUpperValue() / 100.0;
            if (!((JSlider) e.getSource()).getValueIsAdjusting()) {
                for (SidescanPanel panel : panelList) {
                    panel.config.sliceMinValue = selMin;
                    panel.config.sliceWindowValue = selMax - selMin;
                    panel.config.validateValues();
                }
            }
        });
        add(windowSlider);

        addSeparator();
        add(btnConfig);
        add(btnRecord);

        btnInfo.addActionListener(alMode);
        btnZoom.addActionListener(alMode);
        btnMeasure.addActionListener(alMode);
        btnMeasureHeight.addActionListener(alMode);
        btnMark.addActionListener(alMode);

        if (!panelList.isEmpty()) {
            spinNormalization.setValue(panelList.get(0).config.normalization);
        }
        spinNormalization.addChangeListener(alGains);

        if (!panelList.isEmpty()) {
            spinTVG.setValue(panelList.get(0).config.tvgGain);
        }
        spinTVG.addChangeListener(alGains);

        btnAutoEgn.addChangeListener(autoEgnChangeListener);
        btnLogarithmicDecompression.addChangeListener(logarithmicDecompressionChangeListener);
    }
}
