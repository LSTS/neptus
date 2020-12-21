/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.sidescan.SidescanPanel.InteractionMode;

/**
 * @author jqcorreia
 *
 */
public class SidescanToolbar extends JToolBar {
    private static final long serialVersionUID = 1L;

    SidescanPanel panel;

    ButtonGroup bgroup = new ButtonGroup();

    JToggleButton btnMeasure = new JToggleButton(I18n.text("Length"));
    JToggleButton btnMeasureHeight = new JToggleButton(I18n.text("Height"));
    JToggleButton btnInfo = new JToggleButton(I18n.text("Info"));
    JToggleButton btnZoom = new JToggleButton(I18n.text("Zoom"));
    JToggleButton btnMark = new JToggleButton(I18n.text("Mark"));
    JToggleButton btnRecord = new JToggleButton(I18n.text("Record"));

    // Normalization.
    private final JLabel lblNormalization = new JLabel(I18n.text("Normalization"));
    private final SpinnerNumberModel modelNormalization = new SpinnerNumberModel(0.0, 0.0, 100.0, 0.1);
    private final JSpinner spinNormalization = new JSpinner();

    // TVG.
    private final JLabel lblTVG = new JLabel(I18n.textc("TVG", "Time Variable Gain"));
    private final SpinnerNumberModel modelTVG = new SpinnerNumberModel(0.0, -1000.0, 1000.0, 1.0);
    private final JSpinner spinTVG = new JSpinner();

    JButton btnConfig = new JButton(new AbstractAction(I18n.textc("Config", "Configuration")) {
        private static final long serialVersionUID = -878895322319699542L;

        @Override
        public void actionPerformed(ActionEvent e) {
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

            panel.setInteractionMode(imode);
            panel.setZoom(btnZoom.isSelected());
        };
    };

    private final ChangeListener alGains = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            panel.config.tvgGain = (Double) spinTVG.getValue();
            panel.config.normalization = (Double) spinNormalization.getValue();
            panel.record(btnRecord.isSelected());
        }
    };

    public SidescanToolbar(SidescanPanel panel) {
        super();
        this.panel = panel;
        this.spinNormalization.setModel(modelNormalization);
        this.spinTVG.setModel(modelTVG);
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

        addSeparator();
        add(btnConfig);
        add(btnRecord);

        btnInfo.addActionListener(alMode);
        btnZoom.addActionListener(alMode);
        btnMeasure.addActionListener(alMode);
        btnMeasureHeight.addActionListener(alMode);
        btnMark.addActionListener(alMode);

        spinNormalization.setValue(panel.config.normalization);
        spinNormalization.addChangeListener(alGains);

        spinTVG.setValue(panel.config.tvgGain);
        spinTVG.addChangeListener(alGains);
    }
}
