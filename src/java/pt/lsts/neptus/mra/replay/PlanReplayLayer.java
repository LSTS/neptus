/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Jan 30, 2014
 */
package pt.lsts.neptus.mra.replay;

import java.awt.Color;
import java.awt.Graphics2D;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.PlanElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.llf.LogUtils;

/**
 * @author zp
 *
 */
@PluginDescription(icon="images/menus/plan.png")
public class PlanReplayLayer implements LogReplayLayer {
    MissionType mt = null;
    PlanType plan = null;
    PlanElement po = null;
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        po.setRenderer(renderer);
        po.paint(g, renderer);        
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source, Context context) {
        mt = LogUtils.generateMission(source);
        plan = LogUtils.generatePlan(mt, source);
        return plan != null;
    }


    @Override
    public String getName() {
        return "Plan";
    }

    @Override
    public void parse(IMraLogGroup source) {
        po = new PlanElement(MapGroup.getMapGroupInstance(mt), new MapType());
        po.setPlan(plan);
        po.setColor(new Color(0,255,255,128));
        po.setShowDistances(false);
        po.setShowManNames(false);
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] {"PlanControlState"};
    }

    @Override
    public void onMessage(IMCMessage message) {
        po.setActiveManeuver(message.getString("man_id"));
    }

    @Override
    public boolean getVisibleByDefault() {
        return false;
    }

    @Override
    public void cleanup() {

    }

}
