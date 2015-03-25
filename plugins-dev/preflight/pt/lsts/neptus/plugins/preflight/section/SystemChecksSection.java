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
 * 17 Mar 2015
 */
package pt.lsts.neptus.plugins.preflight.section;

import java.awt.Dimension;

import javax.swing.JLabel;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.FuelLevel;
import pt.lsts.imc.GpsFix;
import pt.lsts.imc.Voltage;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.preflight.PreflightSection;
import pt.lsts.neptus.plugins.preflight.Preflight;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
public class SystemChecksSection extends PreflightSection {
    private static final String GPS_DIFF = I18n.textc("DIFF", "Use a single small word");
    private static final String GPS_3D = I18n.textc("3D", "Use a single small word");
    private static final String GPS_2D = I18n.textc("2D", "Use a single small word");
    private static final String GPS_NO_FIX = I18n.textc("NoFix", "Use a single small word");
    
    private JLabel voltageLabel;    
    private JLabel rssiLabel;
    private JLabel maxSpeedLabel;
    private JLabel minSpeedLabel;
    private JLabel vehicleMediumLabel;
    private JLabel maneuverTypeLabel;
    private JLabel gpsFixLabel;
    private JLabel fuelLevelLabel;
    
    
    private double voltageValue;
    private int rssiValue;
    private double maxSpeedValue;
    private double minSpeedValue;
    private String vehicleMedium;
    private String maneuverType;
    private String gpsFixValue;
    private double fuelLevelValue;
    
    public SystemChecksSection() {
        super("Section");          
        
        initValues();
        initLabels();
        buildChecksPanel();
    }
    
    private void initValues() {
        voltageValue = 0.0;
        maxSpeedValue = 0.0;
        minSpeedValue = 0.0;
        vehicleMedium = "?";
        maneuverType = "?";
        gpsFixValue = GPS_NO_FIX;
        fuelLevelValue = 0.0;    
    }
    
    private void initLabels() {
        voltageLabel = new JLabel("<html><b>Voltage: </b>" + voltageValue + "</html>");
        rssiLabel = new JLabel("<html><b>RSSI: </b>" + rssiValue + "</html>");
        vehicleMediumLabel = new JLabel("<html><b>Medium: </b>" + vehicleMedium + "</html>");
        maxSpeedLabel = new JLabel("<html><b>Max Speed: </b>" + maxSpeedValue + "</html>");
        minSpeedLabel = new JLabel("<html><b>Min Speed: </b>" + minSpeedValue + "</html>");
        maneuverTypeLabel = new JLabel("<html><b>Maneuver: </b>" + maneuverType + "</html>");
        gpsFixLabel = new JLabel("<html><b>GPS Fix: </b>" + gpsFixValue + "</html>");
        fuelLevelLabel = new JLabel("<html><b>Fuel: </b>" + fuelLevelValue + "</html>"); 
    }

    @Override
    public void buildChecksPanel() {
        /* TEST */
        checksPanel.setPreferredSize(new Dimension(Preflight.WIDTH - 40, Preflight.HEIGHT - 400));
        
        /* LEFT */
//        addElementWithConstraints(voltageLabel, 0, 0, 0.3);
//        addElementWithConstraints(new JLabel("[OK]"), 1, 0, 0.5);
//        
//        addElementWithConstraints(rssiLabel, 0, 1, 0.3);
//        addElementWithConstraints(new JLabel("[OK]"), 1, 1, 0.5);
//        
//        addElementWithConstraints(vehicleMediumLabel, 0, 2, 0.3);
//        addElementWithConstraints(new JLabel("[OK]"), 1, 2, 0.5);
//        
//        addElementWithConstraints(gpsFixLabel, 0, 3, 0.3);
//        addElementWithConstraints(new JLabel("[OK]"), 1, 3, 0.5);
//        
//        /* RIGHT */
//        addElementWithConstraints(maxSpeedLabel, 2, 0, 0.3);
//        addElementWithConstraints(new JLabel("[OK]"), 3, 0, 0.3);
//        
//        addElementWithConstraints(minSpeedLabel, 2, 1, 0.3);
//        addElementWithConstraints(new JLabel("[OK]"), 3, 1, 0.3);
//        
//        addElementWithConstraints(maneuverTypeLabel, 2, 2, 0.3);
//        addElementWithConstraints(new JLabel("[OK]"), 3, 2, 0.3);
//        
//        addElementWithConstraints(fuelLevelLabel, 2, 3, 0.3);
//        addElementWithConstraints(new JLabel("[OK]"), 3, 3, 0.3);
    }
    
    
    @Subscribe
    public void on(GpsFix msg) {
        System.out.println("## GPS");
        System.out.println("# SOURCE " + msg.getSourceName());
        if(!msgFromMainVehicle(msg.getSourceName()))
            return;
        
        gpsFixValue = msg.getAsString("value");   
        revalidate();
    }
    
    @Subscribe
    public void on(FuelLevel msg) {
        System.out.println("# FUEL");
        System.out.println("# SOURCE " + msg.getSourceName());
        if(!msgFromMainVehicle(msg.getSourceName()))
            return;
        
        fuelLevelValue = msg.getDouble("value");
        revalidate();
    }
    
    @Subscribe
    public void on(Voltage msg) {
        System.out.println("## VOLTAGE");
        System.out.println("# SOURCE " + msg.getSourceName());
        if(!msgFromMainVehicle(msg.getSourceName()))
            return;
        
        voltageValue = msg.getDouble("value");
        revalidate();
    }
}
