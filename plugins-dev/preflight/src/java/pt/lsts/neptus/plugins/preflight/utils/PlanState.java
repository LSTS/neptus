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
 * 1 Apr 2015
 */
package pt.lsts.neptus.plugins.preflight.utils;

import java.util.TreeMap;

import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBState;
import pt.lsts.neptus.plugins.preflight.Preflight;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author tsmarques
 *
 */
public final class PlanState {
    private PlanState() {}
    
    public static boolean existsLocally(String planId) {
        if(planId == null)
            return false;
        TreeMap<String, PlanType> plansList = Preflight.CONSOLE.
                getMission().
                getIndividualPlansList();

       return plansList.containsKey(planId);
    }
    
    /* This method assumes that the plan with planId exists */
    public static boolean isSynchronized(String planId) {
        if(planId == null)
            return false;
        
        ImcSystem sys = ImcSystemsHolder.getSystemWithName(Preflight.CONSOLE.getMainSystem());
        PlanType plan = Preflight.CONSOLE.
                getMission().
                    getIndividualPlansList().
                        get(planId);
        
        PlanDBState prs = sys.getPlanDBControl().getRemoteState();
        if (prs == null || !prs.matchesRemotePlan(plan))
            return false;
        return true;
    }
    
    /* This method assumes that the plan with planId exists */
    public static boolean isEmpty(String planId) {
        if(planId == null)
            return false;
        
        PlanType plan = Preflight.CONSOLE.
                getMission().
                    getIndividualPlansList().
                        get(planId);
        if(plan.isEmpty())
            return true;
        return false;
    }
}
