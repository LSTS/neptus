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
 * Author: jfortuna
 * Jun 20, 2014
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.AutopilotMode;
import pt.lsts.imc.AutopilotMode.AUTONOMY;
import pt.lsts.imc.Current;
import pt.lsts.imc.DevCalibrationControl;
import pt.lsts.imc.DevCalibrationControl.OP;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;

/**
 * Shows Autopilot mode and level of autonomy
 *
 * @author jfortuna
 *
 */
@PluginDescription(name = "Pre-flight Actions", author = "jfortuna", version = "0.1", category = CATEGORY.INTERFACE)
public class PreflightPanel extends ConsolePanel implements MainVehicleChangeListener {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(description="When UAV is on the ground, it is expected that the current from this entity is positive and below CurrentThreshold.")
    public String currentEntity = "Autopilot"; 
    
    @NeptusProperty(description="Current Threshold above which the UAV is considered to be flying.")
    public double CurrentThreshold = 10; 
    // GUI
    private JPanel titlePanel = null;
    private JPanel buttonPanel = null;

    /**
     * @param console
     */
    public PreflightPanel(ConsoleLayout console) {
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
        this.add(titlePanel, "w 100%, h 30%, wrap");
        this.add(buttonPanel, "w 100%, h 70%, wrap");
    }

    private void titlePanelSetup() {
        titlePanel = new JPanel(new MigLayout("gap 0 0, ins 0"));
        JLabel titleLabel = new JLabel(I18n.text("Pre-flight Actions"), SwingConstants.LEFT);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 9));
        titlePanel.add(titleLabel, "w 100%, h 100%");
    }

    private void buttonPanelSetup() {
        buttonPanel = new JPanel(new MigLayout("gap 0 0, ins 0"));

        // Calibrate
        JButton calibButton = new JButton("Calibrate");
        calibButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DevCalibrationControl calib = new DevCalibrationControl();
                calib.setOp(OP.START);
                send(calib);
            }
        });
        
        buttonPanel.add(calibButton, "w 34%, h 100%");
        // Arm
        JButton armButton = new JButton("Arm");
        armButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AutopilotMode apmode = new AutopilotMode();
                apmode.setAutonomy(AUTONOMY.MANUAL);
                apmode.setMode("ARM");
                send(apmode);
            }
        });
        buttonPanel.add(armButton, "w 33%, h 100%");

        // Disarm
        JButton disarmButton = new JButton("Disarm");
        disarmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AutopilotMode apmode = new AutopilotMode();
                apmode.setAutonomy(AUTONOMY.MANUAL);
                apmode.setMode("DISARM");
                send(apmode);
            }
        });
        buttonPanel.add(disarmButton, "w 33%, h 100%");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
    
    @Subscribe
    public void on(Current msg) {
        if (msg.getSourceName().equals(getConsole().getMainSystem())) {
            if (!currentEntity.isEmpty() && !msg.getEntityName().equals(currentEntity))
                return;
            
            double uavValue = msg.getValue();
            if(uavValue >0 && uavValue<CurrentThreshold){
                buttonPanel.setEnabled(true);                
            }
            
        }
        
    }
}
