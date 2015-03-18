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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
import pt.lsts.neptus.plugins.preflight.checklistsections.SystemCheck;
import pt.lsts.neptus.plugins.preflight.checklistsections.TestChecks;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Preflight", author = "tsmarques", version = "0.1")
@Popup(name = "Preflight", pos = POSITION.CENTER, width = 450, height = 500)
public class Preflight extends ConsolePanel {
    public static final int WIDTH = 450;
    public static final int HEIGHT = 500;
    private static final String NOT_UAV_ERROR = "Main vehicle is not an UAV";
    
    
    
    /* Test */ 
    private JPanel mainSysNamePanel; 
    private JLabel mainSysNameLabel;
    private String mainSysName; /* Gets changed when main vehicle changes */
   
    public Preflight(ConsoleLayout console) {
        super(console);
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setResizable(false);
        setBackground(Color.WHITE);
        
        initNameSysNamePanel();
        add(new SystemCheck());
        add(new TestChecks());
    }
    
    private void initNameSysNamePanel() {
        mainSysName = "?";
        mainSysNameLabel = new JLabel(mainSysName);
        
        mainSysNamePanel = new JPanel();
        Dimension d = new Dimension(430, 20);
        mainSysNamePanel.setLayout(new GridBagLayout());
        mainSysNamePanel.setMaximumSize(d);
        mainSysNamePanel.setMinimumSize(d);
        mainSysNamePanel.setLayout(new GridBagLayout());
        mainSysNamePanel.setBackground(Color.WHITE);
        mainSysNamePanel.add(mainSysNameLabel, new GridBagConstraints());
        
        add(Box.createVerticalStrut(1));
        add(mainSysNamePanel);
        add(Box.createVerticalStrut(2));
    }
     

//    @Subscribe
//    public void on(EstimatedState msg) {
//        if(!msgFromMainVehicle(msg.getSourceName()))
//            return;
//    }
       
    @Subscribe
    public void on(ConsoleEventMainSystemChange ev) { /* When a diff vehicle has been selected as main Vehicle */
        mainSysName = getConsole().getMainSystem();
                
        ImcSystem sys = ImcSystemsHolder.getSystemWithName(mainSysName);
        if(sys.getTypeVehicle() != VehicleTypeEnum.UAV) {
        }
        mainSysNameLabel.setText(mainSysName);
        revalidate();
    }
    
    @Subscribe
    public void on(EstimatedState msg) {
        System.out.println("# HEY");
    }
    
    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {

    }
}
