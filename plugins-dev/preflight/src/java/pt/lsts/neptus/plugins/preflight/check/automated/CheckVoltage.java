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
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: tsmarques
 * 13 Apr 2015
 */
package pt.lsts.neptus.plugins.preflight.check.automated;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.FuelLevel;
import pt.lsts.imc.Voltage;
import pt.lsts.neptus.plugins.preflight.check.WithinRangeCheck;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
public class CheckVoltage extends WithinRangeCheck {
    private double minVal = Double.MIN_VALUE;
    private double maxVal = Double.MAX_VALUE;
    
    private double batteryPerc = -1;
    
    public CheckVoltage() {
        super("Voltage", "System");
    }

    @Override
    protected double getMaxValue() {
        return maxVal;
    }

    @Override
    protected double getMinValue() {
        return minVal;
    }
    
    @Override
    protected boolean isWithinRange(double value) {
        return(value >= getMinValue() && value <= getMaxValue());
    }
   
    @Subscribe
    public void on(Voltage msg) {
        if(!messageFromMainVehicle(msg.getSourceName()))
            return;
        
        double voltage = msg.getValue();
        
        if(isWithinRange(voltage))
            setState(VALIDATED);
        else
            setState(NOT_VALIDATED);
        BigDecimal bd = new BigDecimal(voltage);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        
        String displayMsg = bd.doubleValue() + "V";
        
        if(batteryPerc != -1)
            displayMsg += " (" + batteryPerc + "%)";
        
        setValuesLabelText(displayMsg);
    }
    
    @Subscribe
    public void on(FuelLevel msg) {
        if(!messageFromMainVehicle(msg.getSourceName()))
            return;
        
        batteryPerc = msg.getValue();
    }
    
    @Override
    public void validateCheck() {}
}
