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
 * Author: zp
 * Nov 15, 2013
 */
package pt.lsts.neptus.plugins.blueeye;

import java.awt.Dialog.ModalityType;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="Blue Eye Planning", description="This plugin listens for Plan Blueprints and generates respective Neptus plans")
public class BlueEyePlanner extends ConsolePanel {

    private static final long serialVersionUID = 1378142356540460721L;

    public BlueEyePlanner(ConsoleLayout cl) {
        super(cl);
    }
    
    @Subscribe
    public void on(PlanBlueprint plan) {
        if (getConsole().getMission().getIndividualPlansList().containsKey(plan.getPlanId())) {
            NeptusLog.pub().warn("Received an event for an already existing PlanBluePrint: "+plan.getPlanId());
            return;
        }
        PlanType pt = plan.generate();
        pt.setMissionType(getConsole().getMission());
        getConsole().getMission().addPlan(pt);
        getConsole().warnMissionListeners();
        getConsole().getMission().save(true);
        GuiUtils.infoMessage(getConsole(), "Plan received from web", "The plan '"+plan.getPlanId()+"' was downloaded from web and added to the mission.", ModalityType.APPLICATION_MODAL);
    }
    
    @Override
    public void initSubPanel() {
        /*
        TimerTask tt = new TimerTask() {
            
            @Override
            public void run() {
                PlanBlueprint pbp = new PlanBlueprint(26, "blueeye_plan");
                
                pbp.addPoint(41.18547713427995, -8.70566725730896, 3);
                pbp.addPoint(41.18456472894702, -8.704508543014526, 3);                
                pbp.addPoint(41.18441131441246, -8.704723119735718, 3);
                pbp.addPoint(41.18518645785469, -8.705699443817139, 3);
                pbp.addPoint(41.185081491050695, -8.705892562866211, -2);
                pbp.addPoint(41.18432249530712, -8.704948425292969, -2);
                pbp.addPoint(41.18417715469308, -8.705259561538696, -4);
                pbp.addPoint(41.1850330447767, -8.706353902816772, 3);
                pbp.addPoint(41.18548117144345, -8.705696761608124, 3);
                
                getConsole().post(pbp);
            }
        };
        
        java.util.Timer t = new java.util.Timer();
        t.schedule(tt, 30000);
        */
    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
}
