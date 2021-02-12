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
 * Author: Manuel Ribeiro
 * Nov 30, 2015
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.ServoPosition;
import pt.lsts.imc.SetServoPosition;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;

/**
 * @author Manuel Ribeiro
 */
@PluginDescription(name = "Payload Manage Panel", author = "maribeiro", version = "0.1", category = CATEGORY.INTERFACE)
public class PayloadManagePanel extends ConsolePanel implements MainVehicleChangeListener {

    private static final long serialVersionUID = 1L;

    private static final double SERVO_OPEN = (Math.PI/2);
    private static final double SERVO_CLOSE = Math.PI;
    private static final short SERVO_ID = 2;
    
    private boolean isOpenState = true;
    private final String lockTxt = I18n.text("Lock");
    private final String releaseTxt = I18n.text("Release");
    private final String unknownTxt = I18n.text("UNKOWN STATE");

    // GUI
    private JPanel titlePanel = null;
    private JPanel buttonPanel = null;
    private JLabel stateLabel;
    private JToggleButton dropButton;
    /**
     * @param console
     */
    public PayloadManagePanel(ConsoleLayout console) {
        super(console);

        removeAll();
    }

    @Override
    public void initSubPanel() {
        titlePanelSetup();
        buttonPanelSetup();

        // panel general layout setup
        this.setLayout(new MigLayout("gap 0 0, ins 0"));
        this.add(titlePanel, "w 100%, h 15%, wrap");
        this.add(buttonPanel, "w 100%, h 85%, wrap");
    }

    private void titlePanelSetup() {
        titlePanel = new JPanel(new MigLayout("gap 0 0, ins 0"));
        JLabel titleLabel = new JLabel(I18n.text("Payload Action"), SwingConstants.LEFT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9));
        titlePanel.add(titleLabel, "w 100%, h 100%");
    }

    private void buttonPanelSetup() {
        buttonPanel = new JPanel(new MigLayout("gap 0 0, ins 0"));
        stateLabel = new JLabel(unknownTxt, SwingConstants.CENTER);
        stateLabel.setBackground(Color.yellow);
        stateLabel.setForeground(Color.black);
        stateLabel.setFont(new Font("Arial", Font.BOLD, 15));
        stateLabel.setOpaque(true);

        // Attach Payload
        dropButton = new JToggleButton(releaseTxt);
        dropButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isOpenState) { // is open
                    SetServoPosition servo = new SetServoPosition();
                    servo.setId(SERVO_ID);
                    servo.setValue(SERVO_CLOSE);
                    send(servo);
                    isOpenState = false;
                } 
                else {
                    SetServoPosition servo = new SetServoPosition();
                    servo.setId(SERVO_ID);
                    servo.setValue(SERVO_OPEN);
                    send(servo);
                    isOpenState = true;
                }

                stateLabel.setText(unknownTxt);
                stateLabel.setBackground(Color.YELLOW);
            }
        });
        buttonPanel.add(dropButton, "w 34%, h 100%");
        buttonPanel.add(stateLabel, "w 66%, h 100%");
    }

    @Subscribe
    public void consume(ServoPosition servo) {

        if (round(servo.getValue(), 2) == round(SERVO_OPEN, 2)) {
            dropButton.setText(releaseTxt);
            isOpenState = true;
            stateLabel.setText(I18n.text("LOCKED"));
            dropButton.setSelected(true);
            stateLabel.setBackground(new Color(255, 128, 0));
            stateLabel.repaint();
        }
        else if  (round(servo.getValue(), 2) == round(SERVO_CLOSE, 2)) {
            dropButton.setText(lockTxt);
            isOpenState = false;
            stateLabel.setText(I18n.text("OPEN"));
            dropButton.setSelected(false);
            stateLabel.setBackground(Color.green);
            stateLabel.repaint();
        }
    }

    @Override
    public void cleanSubPanel() {
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
