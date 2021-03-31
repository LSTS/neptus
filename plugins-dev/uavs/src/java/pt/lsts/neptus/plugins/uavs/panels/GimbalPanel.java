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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: Manuel R.
 * Feb 10, 2016
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Font;
import java.util.Hashtable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.SetServoPosition;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.Popup.POSITION;

/**
 * @author Manuel R.
 *
 */
@PluginDescription(name = "Gimbal Panel", author = "maribeiro", version = "0.1", category = CATEGORY.INTERFACE)
@Popup(pos = POSITION.RIGHT, width = 300, height = 300)
public class GimbalPanel extends ConsolePanel implements MainVehicleChangeListener {

    private static final long serialVersionUID = 1L;
    private static final short PITCH_SERVO_ID = 0;
    private static final short ROLL_SERVO_ID = 1;

    // GUI
    private JPanel titlePanel = null;
    private JPanel buttonPanel = null;

    public GimbalPanel(ConsoleLayout console) {
        super(console);

        // clears all the unused initializations of the standard SimpleSubPanel
        removeAll();
    }

    @Override
    public void initSubPanel() {
        titlePanelSetup();
        buttonPanelSetup();

        // panel general layout setup
        this.setLayout(new MigLayout("gap 0 0, ins 0"));
        this.add(titlePanel, "w 100%, h 5%, wrap");
        this.add(buttonPanel, "w 100%, h 95%, wrap");

    }

    private void titlePanelSetup() {
        titlePanel = new JPanel(new MigLayout("gap 0 0, ins 0"));
        JLabel titleLabel = new JLabel(I18n.text("Gimbal Control"), SwingConstants.LEFT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9));
        titlePanel.add(titleLabel, "w 100%, h 100%");

    }

    private void buttonPanelSetup() {
        buttonPanel = new JPanel(new MigLayout("gap 0 0, ins 0"));

        JSlider rollSlider = new JSlider(JSlider.HORIZONTAL, 53, 100, 75);

        rollSlider.setMinorTickSpacing(90);
        rollSlider.setMajorTickSpacing(90);
        rollSlider.setPaintTicks(true);
        rollSlider.setPaintLabels(true);
        rollSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();

                if (!source.getValueIsAdjusting()) {
                    int pos = (int) source.getValue();
                    SetServoPosition rollMsg = new SetServoPosition();
                    rollMsg.setId(ROLL_SERVO_ID);

                    double rads = Math.toRadians(pos);
                    rollMsg.setValue(rads);
                    send(rollMsg);
                }      
            }
        });

        Hashtable<Integer, JLabel> rollLblTable = new Hashtable<>();
        rollLblTable.put( Integer.valueOf(100),  new JLabel("L"));
        rollLblTable.put( Integer.valueOf(75), new JLabel("-") );
        rollLblTable.put( Integer.valueOf(53), new JLabel("R") );
        rollSlider.setLabelTable(rollLblTable);
        rollSlider.setInverted(true);
        buttonPanel.add(rollSlider, "w 50%, h 100%");

        JSlider pitchSlider = new JSlider(JSlider.VERTICAL, 75, 100, 75);
        pitchSlider.setPaintTicks(true);

        Hashtable<Integer, JLabel> pitchLblTable = new Hashtable<>();
        pitchLblTable.put( Integer.valueOf(75), new JLabel("FWD") );
        pitchLblTable.put( Integer.valueOf(80), new JLabel("-15") );
        pitchLblTable.put( Integer.valueOf(90), new JLabel("-45") );
        pitchLblTable.put( Integer.valueOf(100),  new JLabel("DWN"));
        pitchSlider.setLabelTable( pitchLblTable );
        pitchSlider.setPaintLabels(true);
        pitchSlider.setInverted(true);

        pitchSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();

                if (!source.getValueIsAdjusting()) {
                    int pos = (int) source.getValue();
                    SetServoPosition pitchMsg = new SetServoPosition();
                    pitchMsg.setId(PITCH_SERVO_ID);
                    double rads = Math.toRadians(pos);
                    pitchMsg.setValue(rads);

                    send(pitchMsg);
                }      
            }
        });

        buttonPanel.add(pitchSlider, "w 50%, h 100%");

    }

    @Override
    public void cleanSubPanel() {
        titlePanel.removeAll();
        buttonPanel.removeAll();
        removeAll();
    }
}
