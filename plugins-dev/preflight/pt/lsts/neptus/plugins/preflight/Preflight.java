/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsmarques
 * 13 Mar 2015
 */
package pt.lsts.neptus.plugins.preflight;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FuelLevel;
import pt.lsts.imc.GpsFix;
import pt.lsts.imc.Voltage;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.preflight.section.AnotherTestSection;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Preflight", author = "tsmarques", version = "0.1")
@Popup(name = "Preflight", pos = POSITION.CENTER, width = 550, height = 750)
public class Preflight extends ConsolePanel {
    public static final int WIDTH = 550;
    public static final int HEIGHT = 750;
    public static final int MAX_COMPONENT_WIDTH = WIDTH - 20; /* Maximum child component width */
    
    /* Not sure if best solution */
    public static ConsoleLayout CONSOLE;
    
    
    /* Test */ 
    private JPanel contentPanel; /* main panel */
    private JScrollPane scrollMainPanel;
    
    private JPanel mainSysNamePanel; 
    private JLabel mainSysNameLabel;
    private String mainSysName; /* Gets changed when main vehicle changes */
   
    public Preflight(ConsoleLayout console) {
        super(console);
        CONSOLE = getConsole();
        setResizable(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        
        
        initMainPanel();
        initSysNamePanel();
        addNewSection(new AnotherTestSection("1"));
//        addNewSection(new AnotherTestSection("2"));
//        addNewSection(new AnotherTestSection("3"));
//        addNewSection(new AnotherTestSection("4"));
//        addNewSection(new AnotherTestSection("5"));
//        addNewSection(new AnotherTestSection("6"));
//        addNewSection(new AnotherTestSection("7"));
//        addNewSection(new AnotherTestSection("8"));
//        addNewSection(new AnotherTestSection("9"));
//        addNewSection(new AnotherTestSection("10"));
//        addNewSection(new AnotherTestSection("11"));
//        addNewSection(new AnotherTestSection("12"));
    }
    
    private void initMainPanel() {
        contentPanel = new JPanel();
        scrollMainPanel = new JScrollPane(contentPanel);
        
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        
        final Dimension d = new Dimension(MAX_COMPONENT_WIDTH, HEIGHT);
        scrollMainPanel.setMaximumSize(d);
        scrollMainPanel.setMinimumSize(d);
        scrollMainPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollMainPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMainPanel.setBorder(BorderFactory.createEmptyBorder());
        add(scrollMainPanel);
    }
    
    private void initSysNamePanel() {
        mainSysName = "?";
        mainSysNameLabel = new JLabel(mainSysName);
        
        mainSysNamePanel = new JPanel();
        final Dimension d = new Dimension(430, 20);
        mainSysNamePanel.setLayout(new GridBagLayout());
        mainSysNamePanel.setMaximumSize(d);
        mainSysNamePanel.setMinimumSize(d);
        mainSysNamePanel.setLayout(new GridBagLayout());
        mainSysNamePanel.setBackground(Color.WHITE);
        mainSysNamePanel.add(mainSysNameLabel, new GridBagConstraints());
        
        contentPanel.add(Box.createVerticalStrut(1));
        contentPanel.add(mainSysNamePanel);
        contentPanel.add(Box.createVerticalStrut(2));
    }
    
    private void addNewSection(PreflightSection section) {
        contentPanel.add(section);
    }
       
    @Subscribe
    public void on(ConsoleEventMainSystemChange ev) { /* When a diff vehicle has been selected as main Vehicle */
        mainSysName = getConsole().getMainSystem();
                
        ImcSystem sys = ImcSystemsHolder.getSystemWithName(mainSysName);
        if(sys.getTypeVehicle() != VehicleTypeEnum.UAV) {
        }
        mainSysNameLabel.setText(mainSysName);
        revalidate();
        
        /* TODO: Instead of just changing the name of the system
         * also change the Preflight Panel */
    }
    
    @Subscribe
    public void on(EstimatedState msg) {
    }
    
    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {

    }
}
