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
 * Author: zp
 * Feb 24, 2014
 */
package pt.lsts.neptus.plugins.txtcmd;

import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.mp.templates.PlanCreator;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;

/**
 * @author zp
 *
 */
public class CommandGoto extends AbstractTextCommand {

    @NeptusProperty(name = "Destination")
    LocationType dest = new LocationType();
    
    @NeptusProperty
    double depth = 0;
    
    @NeptusProperty
    SpeedType speed = new SpeedType(1.2, Units.MPS);
    
    @Override
    public String getCommand() {
        return "go";
    }
    
    @Override
    public PlanType resultingPlan(MissionType mt) {
        PlanCreator planCreator = new PlanCreator(mt);
        planCreator.setSpeed(speed);
        planCreator.setLocation(dest);
        planCreator.setDepth(depth);
        planCreator.addGoto(null);
        PlanType pt = planCreator.getPlan();
        pt.setId("go");
        return pt;        
    }

    @Override
    public void setCenter(LocationType loc) {
        dest = new LocationType(loc);
    }

    public static void main(String[] args) {
        CommandGoto gt = new CommandGoto();
        PluginUtils.editPluginProperties(gt, true);
        System.out.println(gt.buildCommand());
    }

}
