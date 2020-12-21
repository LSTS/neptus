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
 * 10 Apr 2015
 */
package pt.lsts.neptus.plugins.preflight.check.automated;

import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.plugins.preflight.Preflight;
import pt.lsts.neptus.plugins.preflight.check.AutomatedCheck;
import pt.lsts.neptus.plugins.preflight.utils.PlanState;
import pt.lsts.neptus.plugins.update.Periodic;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
public class CheckPlan extends AutomatedCheck {
    private String planToCheck;
    
    public CheckPlan(String planId, String planName) {
        super(planName, "Planning");
        planToCheck = planId;
    }
    
    @Override
    @Periodic(millisBetweenUpdates = 1000)
    public void validateCheck() {
        ImcSystem sys = ImcSystemsHolder.getSystemWithName(Preflight.CONSOLE.getMainSystem());
        
        if(sys == null) {
            setValuesLabelText("");
            setState(NOT_VALIDATED);
            return;
        }
        
        if(!PlanState.existsLocally(planToCheck)) {
            setValuesLabelText("No plan");
            setState(NOT_VALIDATED);
        }
        else if(!PlanState.isSynchronized(planToCheck)) {
            setValuesLabelText("Not synchronised");
            setState(NOT_VALIDATED);
        }
        else {
            if(PlanState.isEmpty(planToCheck)) {
                setValuesLabelText("Empty plan");
                setState(VALIDATED_WITH_WARNINGS);
            }
            else {
                setValuesLabelText("");
                setState(VALIDATED);
            }
        }
    }
}