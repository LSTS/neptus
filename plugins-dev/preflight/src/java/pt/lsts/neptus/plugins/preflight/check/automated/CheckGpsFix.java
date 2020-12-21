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
 * Author: tsmarques
 * 31 Mar 2015
 */
package pt.lsts.neptus.plugins.preflight.check.automated;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.GpsFix;
import pt.lsts.imc.GpsFix.TYPE;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.preflight.check.AutomatedCheck;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
public class CheckGpsFix extends AutomatedCheck {
    private static final String GPS_DIFF = I18n.textc("DIFF", "Use a single small word");
    private static final String GPS_3D = I18n.textc("3D", "Use a single small word");
    private static final String GPS_2D = I18n.textc("2D", "Use a single small word");
    private static final String GPS_NO_FIX = I18n.textc("NoFix", "Use a single small word");
    
    public CheckGpsFix() {
        super("GPS Fix", "Vehcile Sensors");
    }
    

    @Subscribe
    public void on(GpsFix msg) {
        if(!messageFromMainVehicle(msg.getSourceName()))
            return;
        
        GpsFix.TYPE fixType = msg.getType();
        int fixValidity = msg.getValidity();
        int nSat = msg.getSatellites();
        double vdop = msg.getVdop();
        
        BigDecimal bd = new BigDecimal(vdop);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        
        String strMsg = "(" + nSat + ")" + " | Vdop: " + bd.doubleValue();

        if (fixType == TYPE.DEAD_RECKONING) {
            setState(NOT_VALIDATED);
            setValuesLabelText(GPS_NO_FIX + strMsg);
        }
        else if (fixType == TYPE.STANDALONE) {
            setState(VALIDATED_WITH_WARNINGS);
            setValuesLabelText(GPS_2D + strMsg);
            if ((fixValidity & GpsFix.GFV_VALID_VDOP) != 0) {
                setState(VALIDATED);
                setValuesLabelText(GPS_3D + strMsg);
            }
        }
        else if (fixType == TYPE.DIFFERENTIAL) {
            setState(VALIDATED);
            setValuesLabelText(GPS_DIFF + strMsg);
        }
    }
       
    @Override
    public void validateCheck() {}
}
