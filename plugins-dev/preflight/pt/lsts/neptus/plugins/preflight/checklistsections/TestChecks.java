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
package pt.lsts.neptus.plugins.preflight.checklistsections;

import java.awt.Dimension;

import javax.swing.JLabel;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FuelLevel;
import pt.lsts.imc.GpsFix;
import pt.lsts.imc.IndicatedSpeed;
import pt.lsts.imc.TrueSpeed;
import pt.lsts.imc.Voltage;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.preflight.ChecklistSection;
import pt.lsts.neptus.plugins.preflight.Preflight;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
public class TestChecks extends ChecklistSection {  
    private JLabel phiLabel;
    private JLabel thetaLabel;
    private JLabel psiLabel;
    private JLabel iasLabel;
    private JLabel trueSpeedLabel;
    
    private double phiValue;
    private double thetaValue;
    private double psiValue;
    private double iasValue;
    private double trueSpeedValue;
    
    public TestChecks() {
        super("Another section");
        
        initValues();
        initLabels();
        buildChecksPanel();
    }
    
    private void initValues() {
        phiValue = 0.0;
        thetaValue = 0.0;
        psiValue = 0.0;
        iasValue = 0.0;
        trueSpeedValue = 0.0;
    }
    
    private void initLabels() {
        phiLabel = new JLabel("<html><b>Phi: </b>" + phiValue + "</html>");
        thetaLabel = new JLabel("<html><b>Theta: </b>" + thetaValue + "</html>");
        psiLabel = new JLabel("<html><b>Psi: </b>" + psiValue + "</html>");
        iasLabel = new JLabel("<html><b>IAS: </b>" + iasValue+ "</html>");
        trueSpeedLabel = new JLabel("<html><b>True Speed: </b>" + trueSpeedValue + "</html>"); 
    }

    @Override
    public void buildChecksPanel() {
        /* TEST */
        checksPanel.setMaximumSize(new Dimension(Preflight.WIDTH - 40, Preflight.HEIGHT - 400));
        
        /* LEFT */
        addElementWithConstraints(phiLabel, 0, 0, 0.3);
        addElementWithConstraints(new JLabel("[OK]"), 1, 0, 0.5);
        
        addElementWithConstraints(thetaLabel, 0, 1, 0.3);
        addElementWithConstraints(new JLabel("[OK]"), 1, 1, 0.5);
        
        addElementWithConstraints(psiLabel, 0, 2, 0.3);
        addElementWithConstraints(new JLabel("[OK]"), 1, 2, 0.5);
        
        /* RIGHT */
        addElementWithConstraints(iasLabel, 2, 0, 0.3);
        addElementWithConstraints(new JLabel("[OK]"), 3, 0, 0.3);
        
        addElementWithConstraints(trueSpeedLabel, 2, 1, 0.3);
        addElementWithConstraints(new JLabel("[OK]"), 3, 1, 0.3);
    }
    
    @Subscribe
    public void on(EstimatedState msg) {
        System.out.println("# ESTIMATED STATE");
        if(!msgFromMainVehicle(msg.getSourceName()))
            return;
        System.out.println("# PIM");
        phiValue = msg.getDouble("phi");
        phiLabel.setText("" + phiValue);
        thetaValue = msg.getDouble("theta");
        thetaLabel.setText("" + thetaValue);
        revalidate();
    }
    
    @Subscribe
    public void on(TrueSpeed msg) {
        System.out.println("# TRUE SPEED");
        if(!msgFromMainVehicle(msg.getSourceName()))
            return;
        System.out.println("# PIM");
        trueSpeedValue = msg.getDouble("value");
        trueSpeedLabel.setText("" + trueSpeedValue);
        revalidate();
    }
    
    @Subscribe
    public void on(IndicatedSpeed msg) {
        System.out.println("# INDICATED SPEED");
        if(!msgFromMainVehicle(msg.getSourceName()))
            return;
        
        System.out.println("# PIM");
        iasValue = msg.getDouble("value");
        iasLabel.setText("" + iasValue);
        revalidate();
    }
}
