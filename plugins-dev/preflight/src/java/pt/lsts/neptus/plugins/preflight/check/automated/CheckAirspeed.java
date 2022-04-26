/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 6 Apr 2015
 */
package pt.lsts.neptus.plugins.preflight.check.automated;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IndicatedSpeed;
import pt.lsts.neptus.plugins.preflight.check.WithinRangeCheck;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
public class CheckAirspeed extends WithinRangeCheck {
    public CheckAirspeed() {
        super("Airspeed", "Vehcile Sensors");
        setValuesLabelText("[< 2 m/s]");
    }

    @Override
    protected boolean isWithinRange(double value) {
        return((value >= getMinValue()) && (value < getMaxValue()));
    }
    
    @Override
    protected double getMaxValue() {
        return 2; /* m/s */
    }

    @Override
    protected double getMinValue() {
        return 0;
    }
    
    @Subscribe
    public void on(IndicatedSpeed msg) {
        if(!messageFromMainVehicle(msg.getSourceName()))
            return;

        double airspeed = msg.getValue();
        if(isWithinRange(airspeed)) {
            setValuesLabelText("[< 2 m/s]");
            setState(VALIDATED);
        }
        else {
            BigDecimal bd = new BigDecimal(airspeed);
            bd = bd.setScale(1, RoundingMode.HALF_UP);
            setValuesLabelText(bd.doubleValue() + " m/s");
            setState(NOT_VALIDATED);
        }
    }

    @Override
    public void validateCheck() {}

}
